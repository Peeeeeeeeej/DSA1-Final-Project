package Parking.Vehicles;

import java.util.Random;
import java.util.HashSet;

public class Vehicle {
    private static final String[] DESIGNATIONS = {"student", "faculty", "visitor", "service"};
    private static final int[] DESIGNATION_WEIGHTS = {60, 25, 10, 5};  // percentages, must add to 100
    private String size;
    private String licencePlate;
    // HashSet instead of ArrayList: checking if a plate is taken is O(1) instead of O(n)
    private static HashSet<String> licencePlateStorage = new HashSet<>();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();
    private boolean ev;
    private String designation;
    private boolean isHandicapped;

    public Vehicle(String size) {
        this.licencePlate = generatePlate();
        this.size = size;
        this.ev = randomEv();
        this.designation = randomDesignation();
        this.isHandicapped = randomHandicapped();
    }

    // vehicle with chosen values (used by the website form)
    // a blank plate gets a random one; otherwise the plate must be valid and unused
    public Vehicle(String size, String plate, String designation, boolean ev, boolean handicapped) {
        if (!isValidDesignation(designation)) {
            throw new IllegalArgumentException("Designation must be student, faculty, visitor or service.");
        }
        if (plate == null || plate.trim().isEmpty()) {
            this.licencePlate = generatePlate();
        } else {
            String p = plate.trim().toUpperCase();
            if (!isValidPlate(p)) {
                throw new IllegalArgumentException("Plate must be exactly 6 letters or numbers (A-Z, 0-9).");
            }
            if (licencePlateStorage.contains(p)) {
                throw new IllegalArgumentException("Plate " + p + " is already in use.");
            }
            licencePlateStorage.add(p);
            this.licencePlate = p;
        }
        this.size = size;
        this.designation = designation;
        this.ev = ev;
        this.isHandicapped = handicapped;
    }

    public static boolean isValidPlate(String plate) {
        return plate != null && plate.matches("[A-Z0-9]{6}");
    }

    public static boolean isValidDesignation(String designation) {
        for (String d : DESIGNATIONS) {
            if (d.equals(designation)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPlateTaken(String plate) {
        return licencePlateStorage.contains(plate);
    }

    // claims a plate again (used when undoing a car leaving); false if someone else took it
    public static boolean reservePlate(String plate) {
        return licencePlateStorage.add(plate);
    }

    // frees a plate so it can be used again (when a car leaves or is turned away)
    public static void releasePlate(String plate) {
        licencePlateStorage.remove(plate);
    }

    public static String randomPlate() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public String generatePlate() {
        String plate;
        do {
            plate = randomPlate();
        } while (licencePlateStorage.contains(plate));
        licencePlateStorage.add(plate);
        return plate;
    }

    // 10% chance of being electric
    public static boolean randomEv() {
        return RANDOM.nextInt(100) < 10;
    }

    // 5% chance of needing a handicapped space
    public static boolean randomHandicapped() {
        return RANDOM.nextInt(100) < 5;
    }

    // 60% student, 25% faculty, 10% visitor, 5% service
    public static String randomDesignation() {
        int roll = RANDOM.nextInt(100);  // 0 to 99
        int total = 0;
        for (int i = 0; i < DESIGNATIONS.length; i++) {
            total += DESIGNATION_WEIGHTS[i];
            if (roll < total) {
                return DESIGNATIONS[i];
            }
        }
        return DESIGNATIONS[0];  // fallback, never reached if weights add to 100
    }

    public String getLicencePlate() {
        return licencePlate; }
    public String getSize() {
        return size; }
    public boolean isEv() {
        return ev; }
    public String getDesignation() {
        return designation; }
    public boolean isHandicapped() {
        return isHandicapped; }
}