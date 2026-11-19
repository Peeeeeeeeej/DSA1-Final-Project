package Parking.server;

import Parking.Lots.Lot;
import Parking.Lots.Space;
import Parking.Vehicles.*;

import java.util.*;

// Holds every lot on campus and runs the parking rules for the website.
// All parking goes through Lot methods so the Lot counters stay correct.
public class ParkingSystem {

    public static final int[] LOT_SIZES = {100, 80, 70, 60, 60, 50, 40, 40}; // 500 spaces
    public static final String[] SECTIONS = {"student", "faculty", "visitor", "service", "handicap", "general"};
    private static final String[] GROUPS = {"student", "faculty", "visitor", "service", "handicap"};

    private final Random random = new Random();
    private List<Lot> lots = new ArrayList<>();

    // plate -> where the car is, so finding a car is O(1) instead of scanning every lot
    private final HashMap<String, Vehicle> vehiclesByPlate = new HashMap<>();
    private final HashMap<String, Integer> lotByPlate = new HashMap<>();
    private final HashMap<String, String> sectionByPlate = new HashMap<>();

    // newest first, capped so it doesn't grow forever
    private final LinkedList<String> log = new LinkedList<>();
    private static final int LOG_LIMIT = 200;

    // undo: a stack (last in, first out) of the most recent actions
    private final Deque<Action> undoStack = new ArrayDeque<>();
    private static final int UNDO_LIMIT = 50;

    // stats: cars that couldn't be parked anywhere, by group (handicapped cars count as "handicap")
    private final Map<String, Integer> turnedAway = new LinkedHashMap<>();

    // occupancy history for the chart: total cars parked after each change
    private final List<Integer> history = new ArrayList<>();
    private static final int HISTORY_LIMIT = 400;

    // goes up on every change so the page knows when to redraw
    private int version = 0;

    public ParkingSystem() {
        buildLots();
        clearStats();
        changed();
    }

    private void buildLots() {
        lots = new ArrayList<>();
        for (int size : LOT_SIZES) {
            lots.add(new Lot(size));
        }
    }

    private void clearStats() {
        for (String g : GROUPS) turnedAway.put(g, 0);
        history.clear();
    }

    private void changed() {
        version++;
        history.add(totalParked());
        if (history.size() > HISTORY_LIMIT) history.remove(0);
    }

    private static String group(Vehicle v) {
        return v.isHandicapped() ? "handicap" : v.getDesignation();
    }

    private void countTurnedAway(Vehicle v) {
        turnedAway.merge(group(v), 1, Integer::sum);
    }

    // ---------- undo actions ----------

    private static class LeftCar {
        Vehicle v; int lot; String section;
        LeftCar(Vehicle v, int lot, String section) { this.v = v; this.lot = lot; this.section = section; }
    }

    private static class Action {
        String label;
        List<String> parked = new ArrayList<>(); // undo = remove these cars
        List<LeftCar> left = new ArrayList<>();  // undo = put these cars back
        Map<String, Integer> turnedAway = new HashMap<>(); // undo = take these back out of the stats
        Action(String label) { this.label = label; }
    }

    private void pushUndo(Action a) {
        if (a.parked.isEmpty() && a.left.isEmpty() && a.turnedAway.isEmpty()) return;
        undoStack.push(a);
        if (undoStack.size() > UNDO_LIMIT) undoStack.removeLast();
    }

    // ---------- results sent back to the website ----------

    public static class Result {
        public String status;   // parked, full, handicapFull, error, left, found, notFound, undone
        public String message;
        public int lot = -1;    // lot index the car parked in
        public String section;
        public String plate;
        public boolean canParkHere;              // handicapFull: another section in this lot is open
        public List<int[]> suggestions = new ArrayList<>(); // {lotIndex, openSpots}
    }

    // ---------- parking ----------

    // mode "" = normal, "here" = handicapped car chose to park elsewhere in the same lot
    public synchronized Result park(int lotIdx, String type, String plate, String designation,
                                    boolean ev, boolean handicapped, String mode) {
        Result r = new Result();
        if (lotIdx < 0 || lotIdx >= lots.size()) {
            return error(r, "That lot doesn't exist.");
        }
        Vehicle v;
        try {
            v = makeVehicle(type, plate, designation, ev, handicapped);
        } catch (IllegalArgumentException e) {
            return error(r, e.getMessage());
        }
        r.plate = v.getLicencePlate();
        Lot lot = lots.get(lotIdx);

        if (handicapped && !"here".equals(mode)) {
            // the team's queue + parkVehicle send handicapped cars to handicap spaces
            if (queuePark(v, lotIdx, r)) {
                return r;
            }
            // handicap spaces full here: let the user choose what to do
            Vehicle.releasePlate(v.getLicencePlate());
            r.status = "handicapFull";
            r.canParkHere = !lot.isSectionFull(designation) || !lot.isSectionFull("general");
            r.message = "Lot " + (lotIdx + 1) + "'s handicap spaces are full.";
            for (int i = 0; i < lots.size(); i++) {
                if (i != lotIdx && !lots.get(i).isSectionFull("handicap")) {
                    r.suggestions.add(new int[]{i, lots.get(i).openSpaces("handicap")});
                }
            }
            if (!r.canParkHere && r.suggestions.isEmpty()) countTurnedAway(v);
            return r;
        }

        // own section first (through the incoming queue), then overflow into a general space.
        // "here" mode is a handicapped car using its designation's section, which parkVehicle
        // can't do (it always sends handicapped cars to handicap spaces), so it uses parkInSection.
        boolean parked = "here".equals(mode)
                ? tryPark(v, lotIdx, designation, r)
                : queuePark(v, lotIdx, r);
        if (parked || tryPark(v, lotIdx, "general", r)) {
            return r;
        }

        Vehicle.releasePlate(v.getLicencePlate());
        r.status = "full";
        r.message = "Lot " + (lotIdx + 1) + " is full for " + designation + " vehicles.";
        for (int i = 0; i < lots.size(); i++) {
            int open = lots.get(i).openSpaces(designation) + lots.get(i).openSpaces("general");
            if (i != lotIdx && open > 0) {
                r.suggestions.add(new int[]{i, open});
            }
        }
        if (r.suggestions.isEmpty()) countTurnedAway(v);
        return r;
    }

    // "Park me anywhere": picks the lot with the most room for this car, then parks normally
    public synchronized Result parkBest(String type, String plate, String designation,
                                        boolean ev, boolean handicapped) {
        int best = -1, bestOpen = 0;
        String mode = "";
        if (handicapped) {
            for (int i = 0; i < lots.size(); i++) {
                int open = lots.get(i).openSpaces("handicap");
                if (open > bestOpen) { best = i; bestOpen = open; }
            }
        }
        if (best < 0) {
            if (handicapped) mode = "here"; // no handicap space anywhere: use designation or general
            for (int i = 0; i < lots.size(); i++) {
                int open = lots.get(i).openSpaces(designation) + lots.get(i).openSpaces("general");
                if (open > bestOpen) { best = i; bestOpen = open; }
            }
        }
        if (best < 0) {
            // nowhere on campus: still validate the form, then report full
            Result r = park(0, type, plate, designation, ev, handicapped, mode);
            if (!"error".equals(r.status) && !"parked".equals(r.status)) {
                r.status = "full";
                r.message = "Every lot on campus is full for this car.";
                r.suggestions.clear();
            }
            return r;
        }
        return park(best, type, plate, designation, ev, handicapped, mode);
    }

    // parks through the team's Lot methods: isFull -> addIncoming -> removeIncoming (-> parkVehicle)
    // returns the space number, or -1 if the car's section in that lot is full
    private int enqueueAndPark(Vehicle v, Lot lot) {
        if (lot.isFull(v)) {
            return -1;
        }
        int before = lot.countParked();
        lot.addIncoming(v);
        try {
            lot.removeIncoming();
        } catch (Exception e) {
            return -1;
        }
        if (lot.countParked() == before) {
            return -1; // parkVehicle didn't take it
        }
        List<Space> spaces = lot.getSpaces();
        for (int i = 0; i < spaces.size(); i++) {
            if (spaces.get(i).getFilledBy() == v) {
                return i;
            }
        }
        return -1;
    }

    private boolean queuePark(Vehicle v, int lotIdx, Result r) {
        Lot lot = lots.get(lotIdx);
        int space = enqueueAndPark(v, lot);
        if (space < 0) {
            return false;
        }
        recordParked(v, lotIdx, Lot.sectionOf(lot.getSpaces().get(space)), space, r);
        return true;
    }

    // parks in one chosen section (used for general overflow and the handicap "here" choice)
    private boolean tryPark(Vehicle v, int lotIdx, String section, Result r) {
        int space = lots.get(lotIdx).parkInSection(v, section);
        if (space < 0) {
            return false;
        }
        recordParked(v, lotIdx, section, space, r);
        return true;
    }

    private void recordParked(Vehicle v, int lotIdx, String section, int space, Result r) {
        String plate = v.getLicencePlate();
        remember(v, lotIdx, section);
        r.status = "parked";
        r.lot = lotIdx;
        r.section = section;
        r.message = plate + " parked in Lot " + (lotIdx + 1) + ", " + section + " space " + space + ".";
        addLog(plate + " parked in Lot " + (lotIdx + 1) + ", " + section + " space");
        Action a = new Action(plate + " parked in Lot " + (lotIdx + 1));
        a.parked.add(plate);
        pushUndo(a);
        changed();
    }

    private void remember(Vehicle v, int lotIdx, String section) {
        String plate = v.getLicencePlate();
        vehiclesByPlate.put(plate, v);
        lotByPlate.put(plate, lotIdx);
        sectionByPlate.put(plate, section);
    }

    private Vehicle makeVehicle(String type, String plate, String designation, boolean ev, boolean handicapped) {
        switch (type == null ? "" : type) {
            case "Car":        return new Car(plate, designation, ev, handicapped);
            case "Truck":      return new Truck(plate, designation, ev, handicapped);
            case "Motorcycle": return new Motorcycle(plate, designation, ev, handicapped);
            default: throw new IllegalArgumentException("Type must be Car, Truck or Motorcycle.");
        }
    }

    private Result error(Result r, String message) {
        r.status = "error";
        r.message = message;
        return r;
    }

    // ---------- leaving and searching ----------

    // takes a car out through the lot's outgoing queue; returns what's needed to undo it, or null
    private LeftCar removeCar(String plate) {
        Vehicle v = vehiclesByPlate.get(plate);
        if (v == null) return null;
        int lotIdx = lotByPlate.get(plate);
        String section = sectionByPlate.get(plate);
        Lot lot = lots.get(lotIdx);
        try {
            lot.addOutgoing(v);
            lot.removeOutgoing();
        } catch (Exception e) {
            return null;
        }
        vehiclesByPlate.remove(plate);
        lotByPlate.remove(plate);
        sectionByPlate.remove(plate);
        Vehicle.releasePlate(plate);
        return new LeftCar(v, lotIdx, section);
    }

    public synchronized Result leave(String plate) {
        Result r = new Result();
        String p = plate == null ? "" : plate.trim().toUpperCase();
        LeftCar gone = removeCar(p);
        if (gone == null) {
            return error(r, "No parked car has plate " + p + ".");
        }
        r.status = "left";
        r.lot = gone.lot;
        r.plate = p;
        r.message = p + " left Lot " + (gone.lot + 1) + ".";
        addLog(p + " left Lot " + (gone.lot + 1));
        Action a = new Action(p + " left Lot " + (gone.lot + 1));
        a.left.add(gone);
        pushUndo(a);
        changed();
        return r;
    }

    public synchronized Result search(String plate) {
        Result r = new Result();
        String p = plate == null ? "" : plate.trim().toUpperCase();
        Vehicle v = vehiclesByPlate.get(p);
        r.plate = p;
        if (v == null) {
            r.status = "notFound";
            r.message = p + " is not parked on campus.";
            return r;
        }
        int lotIdx = lotByPlate.get(p);
        r.status = "found";
        r.lot = lotIdx;
        r.section = sectionByPlate.get(p);
        // uses the team's Lot.searchByPlate for the space number
        r.message = p + " is in Lot " + (lotIdx + 1) + " (" + r.section + "). "
                + lots.get(lotIdx).searchByPlate(v) + ".";
        return r;
    }

    // ---------- undo ----------

    public synchronized Result undo() {
        Result r = new Result();
        Action a = undoStack.poll();
        if (a == null) {
            return error(r, "Nothing to undo.");
        }
        int removed = 0, restored = 0, failed = 0;
        for (Map.Entry<String, Integer> e : a.turnedAway.entrySet()) {
            turnedAway.merge(e.getKey(), -e.getValue(), Integer::sum);
        }
        for (String plate : a.parked) {
            if (removeCar(plate) != null) removed++;
        }
        for (LeftCar c : a.left) {
            String plate = c.v.getLicencePlate();
            if (!Vehicle.reservePlate(plate)) { failed++; continue; }
            if (lots.get(c.lot).parkInSection(c.v, c.section) >= 0) {
                remember(c.v, c.lot, c.section);
                restored++;
            } else {
                Vehicle.releasePlate(plate); // that space was taken since
                failed++;
            }
        }
        r.status = "undone";
        r.message = "Undid: " + a.label + (failed > 0 ? " (" + failed + " car(s) couldn't go back, their spaces were taken)" : "");
        addLog(r.message);
        changed();
        return r;
    }

    // ---------- Dev tab ----------

    // adds random cars: 'total' fully random cars, plus forced counts per designation / handicapped
    // typeMix = {car%, truck%, motorcycle%}; each car goes to a random lot that has room for it
    public synchronized int[] devAdd(int total, Map<String, Integer> forced, int handicappedCount, int[] typeMix) {
        List<String[]> wanted = new ArrayList<>(); // {designation, handicapped}
        for (int i = 0; i < total; i++) {
            wanted.add(new String[]{Vehicle.randomDesignation(), String.valueOf(Vehicle.randomHandicapped())});
        }
        for (Map.Entry<String, Integer> e : forced.entrySet()) {
            for (int i = 0; i < e.getValue(); i++) {
                wanted.add(new String[]{e.getKey(), "false"});
            }
        }
        for (int i = 0; i < handicappedCount; i++) {
            wanted.add(new String[]{Vehicle.randomDesignation(), "true"});
        }
        Collections.shuffle(wanted, random);

        // every new car joins one arrivals queue (FIFO), then they're parked in arrival order
        Queue<Vehicle> arrivals = new LinkedList<>();
        for (String[] w : wanted) {
            arrivals.offer(makeVehicle(randomType(typeMix), "", w[0], Vehicle.randomEv(), Boolean.parseBoolean(w[1])));
        }

        Action a = new Action("Dev added " + wanted.size() + " cars");
        int parked = 0, away = 0;
        while (!arrivals.isEmpty()) {
            Vehicle v = arrivals.poll();
            if (devPlace(v)) {
                parked++;
                a.parked.add(v.getLicencePlate());
            } else {
                Vehicle.releasePlate(v.getLicencePlate());
                countTurnedAway(v);
                a.turnedAway.merge(group(v), 1, Integer::sum);
                away++;
            }
        }
        addLog("Dev: added " + wanted.size() + " cars (" + parked + " parked, " + away + " turned away)");
        pushUndo(a);
        changed();
        return new int[]{wanted.size(), parked, away};
    }

    // random lot with room. First choice goes through the team's methods:
    // pick a random lot where isFull(v) is false, then addIncoming/removeIncoming (parkVehicle).
    // If no lot has room there: handicapped cars try their designation's section, then everyone tries general.
    private boolean devPlace(Vehicle v) {
        List<Integer> open = new ArrayList<>();
        for (int i = 0; i < lots.size(); i++) {
            if (!lots.get(i).isFull(v)) open.add(i);
        }
        if (!open.isEmpty()) {
            int lotIdx = open.get(random.nextInt(open.size()));
            Lot lot = lots.get(lotIdx);
            int space = enqueueAndPark(v, lot);
            if (space >= 0) {
                remember(v, lotIdx, Lot.sectionOf(lot.getSpaces().get(space)));
                return true;
            }
        }

        List<String> order = new ArrayList<>();
        if (v.isHandicapped()) order.add(v.getDesignation());
        order.add("general");
        for (String section : order) {
            List<Integer> candidates = new ArrayList<>();
            for (int i = 0; i < lots.size(); i++) {
                if (!lots.get(i).isSectionFull(section)) candidates.add(i);
            }
            if (!candidates.isEmpty()) {
                int lotIdx = candidates.get(random.nextInt(candidates.size()));
                int space = lots.get(lotIdx).parkInSection(v, section);
                if (space >= 0) {
                    remember(v, lotIdx, section);
                    return true;
                }
            }
        }
        return false;
    }

    // removes 'count' random parked cars, each through its lot's outgoing queue
    public synchronized int devLeave(int count) {
        List<String> plates = new ArrayList<>(vehiclesByPlate.keySet());
        Collections.shuffle(plates, random);
        int n = Math.min(count, plates.size());
        Action a = new Action("Dev removed " + n + " cars");
        for (int i = 0; i < n; i++) {
            LeftCar gone = removeCar(plates.get(i));
            if (gone != null) a.left.add(gone);
        }
        addLog("Dev: " + a.left.size() + " random cars left");
        pushUndo(a);
        changed();
        return a.left.size();
    }

    private String randomType(int[] mix) {
        int sum = mix[0] + mix[1] + mix[2];
        if (sum <= 0) return "Car";
        int roll = random.nextInt(sum);
        if (roll < mix[0]) return "Car";
        if (roll < mix[0] + mix[1]) return "Truck";
        return "Motorcycle";
    }

    public synchronized void reset() {
        for (String plate : vehiclesByPlate.keySet()) {
            Vehicle.releasePlate(plate);
        }
        vehiclesByPlate.clear();
        lotByPlate.clear();
        sectionByPlate.clear();
        undoStack.clear(); // a reset can't be undone
        buildLots();
        clearStats();
        addLog("All lots reset to empty");
        changed();
    }

    private void addLog(String line) {
        log.addFirst(line);
        if (log.size() > LOG_LIMIT) log.removeLast();
    }

    private int totalParked() {
        int n = 0;
        for (Lot lot : lots) n += lot.countParked();
        return n;
    }

    // ---------- state for the website, as JSON ----------

    public synchronized int getVersion() {
        return version;
    }

    public synchronized String stateJson() {
        StringBuilder sb = new StringBuilder("{\"version\":").append(version).append(",\"lots\":[");
        for (int i = 0; i < lots.size(); i++) {
            Lot lot = lots.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"id\":").append(i)
              .append(",\"size\":").append(lot.getSize())
              .append(",\"parked\":").append(lot.countParked())
              .append(",\"sections\":{");
            for (int s = 0; s < SECTIONS.length; s++) {
                if (s > 0) sb.append(',');
                sb.append(Json.str(SECTIONS[s])).append(":{\"open\":").append(lot.openSpaces(SECTIONS[s]))
                  .append(",\"total\":").append(lot.totalSpaces(SECTIONS[s])).append('}');
            }
            // one letter per space, in order, for the stall view
            StringBuilder layout = new StringBuilder();
            List<Space> spaces = lot.getSpaces();
            for (Space sp : spaces) {
                String sec = Lot.sectionOf(sp);
                layout.append(sec.equals("service") ? 'r' : sec.charAt(0));
            }
            sb.append("},\"layout\":").append(Json.str(layout.toString())).append(",\"cars\":[");
            boolean first = true;
            for (int sp = 0; sp < spaces.size(); sp++) {
                Vehicle v = spaces.get(sp).getFilledBy();
                if (v == null) continue;
                if (!first) sb.append(',');
                first = false;
                sb.append("{\"plate\":").append(Json.str(v.getLicencePlate()))
                  .append(",\"type\":").append(Json.str(v.getClass().getSimpleName()))
                  .append(",\"designation\":").append(Json.str(v.getDesignation()))
                  .append(",\"ev\":").append(v.isEv())
                  .append(",\"handicapped\":").append(v.isHandicapped())
                  .append(",\"section\":").append(Json.str(Lot.sectionOf(spaces.get(sp))))
                  .append(",\"space\":").append(sp).append('}');
            }
            sb.append("]}");
        }
        sb.append("],\"log\":[");
        int n = 0;
        for (String line : log) {
            if (n++ > 0) sb.append(',');
            sb.append(Json.str(line));
        }
        sb.append("],\"undo\":").append(Json.str(undoStack.isEmpty() ? null : undoStack.peek().label));

        // stats
        Map<String, Integer> parkedBy = new LinkedHashMap<>();
        for (String g : GROUPS) parkedBy.put(g, 0);
        int ev = 0;
        for (Vehicle v : vehiclesByPlate.values()) {
            parkedBy.merge(group(v), 1, Integer::sum);
            if (v.isEv()) ev++;
        }
        sb.append(",\"stats\":{\"parked\":").append(totalParked())
          .append(",\"capacity\":").append(capacity())
          .append(",\"ev\":").append(ev)
          .append(",\"parkedBy\":").append(mapJson(parkedBy))
          .append(",\"turnedAway\":").append(mapJson(turnedAway))
          .append("},\"history\":[");
        for (int h = 0; h < history.size(); h++) {
            if (h > 0) sb.append(',');
            sb.append(history.get(h));
        }
        sb.append("]}");
        return sb.toString();
    }

    private int capacity() {
        int n = 0;
        for (int s : LOT_SIZES) n += s;
        return n;
    }

    private static String mapJson(Map<String, Integer> m) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            if (i++ > 0) sb.append(',');
            sb.append(Json.str(e.getKey())).append(':').append(e.getValue());
        }
        return sb.append('}').toString();
    }

    public static String resultJson(Result r) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"status\":").append(Json.str(r.status))
          .append(",\"message\":").append(Json.str(r.message))
          .append(",\"lot\":").append(r.lot)
          .append(",\"section\":").append(Json.str(r.section))
          .append(",\"plate\":").append(Json.str(r.plate))
          .append(",\"canParkHere\":").append(r.canParkHere)
          .append(",\"suggestions\":[");
        for (int i = 0; i < r.suggestions.size(); i++) {
            if (i > 0) sb.append(',');
            int[] s = r.suggestions.get(i);
            sb.append("{\"lot\":").append(s[0]).append(",\"open\":").append(s[1]).append('}');
        }
        sb.append("]}");
        return sb.toString();
    }
}
