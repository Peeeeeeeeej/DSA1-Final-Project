package Parking.Lots;

import java.util.*;

import Parking.Vehicles.Vehicle;

public class Lot {

    // we can add vehicles to spaces by calling indices of lotList (which are
    // spaces)
    // and then calling methods defined in space class

    private int size;
    private List<Space> lotList = new ArrayList<>();

    private int nHandicapSpaces;
    private int nFacultySpaces;
    private int nServiceSpaces;
    private int nStudentSpaces;
    private int nVisitorSpaces;


    private int filledHandicap = 0;
    private int filledFaculty = 0;
    private int filledService = 0;
    private int filledStudent = 0;
    private int filledVisitor = 0;

    // general spaces: the left-over spaces any vehicle can use as overflow
    private int nGeneralSpaces;
    private int filledGeneral = 0;

    private Queue<Vehicle> incoming;
    private Queue<Vehicle> outgoing;

    public Lot(int sizeInit) {

        this.size = sizeInit;
        this.lotList = new ArrayList<>(sizeInit);

        incoming = new LinkedList<>();
        outgoing = new LinkedList<>();

        // based on designations in Vehicle.java (+percentages for EV and handicap
        // spaces)
        this.nFacultySpaces = (int) Math.floor((double) sizeInit * 0.25);
        this.nServiceSpaces = (int) Math.floor((double) sizeInit * 0.05);
        this.nStudentSpaces = (int) Math.floor((double) sizeInit * 0.45); // was 0.60 before EV and handicap
        this.nVisitorSpaces = (int) Math.floor((double) sizeInit * 0.10);
        this.nHandicapSpaces = (int) Math.floor((double) sizeInit * 0.05);
        this.nGeneralSpaces = size - (nFacultySpaces + nServiceSpaces + nStudentSpaces + nVisitorSpaces + nHandicapSpaces);

        // account for left-over spaces from truncation of ceiling function
        for (int spaceIdx = 0; spaceIdx < (size
                - (nFacultySpaces + nServiceSpaces + nStudentSpaces + nVisitorSpaces + nHandicapSpaces)); spaceIdx++) {
            lotList.add(new Space());
        }

        // append faculty spaces
        for (int spaceIdx = 0; spaceIdx < nFacultySpaces; spaceIdx++) {
            lotList.add(new FacultySpace());
        }

        // append service spaces
        for (int spaceIdx = 0; spaceIdx < nServiceSpaces; spaceIdx++) {
            lotList.add(new ServiceSpace());
        }

        // append student spaces
        for (int spaceIdx = 0; spaceIdx < nStudentSpaces; spaceIdx++) {
            lotList.add(new StudentSpace());
        }

        // append visitor spaces
        for (int spaceIdx = 0; spaceIdx < nVisitorSpaces; spaceIdx++) {
            lotList.add(new VisitorSpace());
        }

        // append handicap spaces
        for (int spaceIdx = 0; spaceIdx < nHandicapSpaces; spaceIdx++) {
            lotList.add(new HandicapSpace());
        }
    }

    public int getSize() {
        return size;
    }

    public ArrayList<Space> getFacultySpaces() {
        ArrayList<Space> facultySpaces = new ArrayList<Space>();
        for (int spaceIdx = 0; spaceIdx < size; spaceIdx++) {
            if (lotList.get(spaceIdx) instanceof FacultySpace) {
                facultySpaces.add(lotList.get(spaceIdx));
            }
        }
        return facultySpaces;
    }

    public ArrayList<Space> getServiceSpaces() {
        ArrayList<Space> serviceSpaces = new ArrayList<Space>();
        for (int spaceIdx = 0; spaceIdx < size; spaceIdx++) {
            if (lotList.get(spaceIdx) instanceof ServiceSpace) {
                serviceSpaces.add(lotList.get(spaceIdx));
            }
        }
        return serviceSpaces;
    }

    public ArrayList<Space> getStudentSpaces() {
        ArrayList<Space> studentSpaces = new ArrayList<Space>();
        for (int spaceIdx = 0; spaceIdx < size; spaceIdx++) {
            if (lotList.get(spaceIdx) instanceof StudentSpace) {
                studentSpaces.add(lotList.get(spaceIdx));
            }
        }
        return studentSpaces;
    }

    public ArrayList<Space> getVisitorSpaces() {
        ArrayList<Space> visitorSpaces = new ArrayList<Space>();
        for (int spaceIdx = 0; spaceIdx < size; spaceIdx++) {
            if (lotList.get(spaceIdx) instanceof VisitorSpace) {
                visitorSpaces.add(lotList.get(spaceIdx));
            }
        }
        return visitorSpaces;
    }

    public ArrayList<Space> getHandicapSpaces() {
        ArrayList<Space> handicapSpaces = new ArrayList<Space>();
        for (int spaceIdx = 0; spaceIdx < size; spaceIdx++) {
            if (lotList.get(spaceIdx) instanceof HandicapSpace) {
                handicapSpaces.add(lotList.get(spaceIdx));
            }
        }
        return handicapSpaces;
    }

    public List<Space> getSpaces() {
        return lotList;
    }

    // queue-related methods

    public void addIncoming(Vehicle v) {
        incoming.offer(v);
    }

    public void addOutgoing(Vehicle v) {
        outgoing.offer(v);
    }

    // poll and return vehicle type
    public String removeIncoming() throws Exception {
        try {
            Vehicle v = incoming.peek();
            parkVehicle(incoming.poll());
            return v.getDesignation();
        } catch (Exception e) {
            throw new Exception("warning: empty incoming queue!!!");
        }
    }

    public String removeOutgoing() throws Exception {
        try {
            Vehicle v = outgoing.poll();
            
            // remove vehicle from its old space (inefficient, someone else pls implement binary search or something)
            for (int vIdx=0; vIdx<lotList.size(); vIdx++) {
                
                if (lotList.get(vIdx).getFilledBy()==v) {

                    // lower the counter for the section this car was actually parked in
                    Space oldSpace = lotList.get(vIdx);
                    if (oldSpace instanceof HandicapSpace) {
                        filledHandicap--;
                    } else if (oldSpace instanceof FacultySpace) {
                        filledFaculty--;
                    } else if (oldSpace instanceof ServiceSpace) {
                        filledService--;
                    } else if (oldSpace instanceof StudentSpace) {
                        filledStudent--;
                    } else if (oldSpace instanceof VisitorSpace) {
                        filledVisitor--;
                    } else {
                        filledGeneral--;
                    }

                    // replace it with an empty space of the SAME type it was
                    // (a student leaving a general space must leave a general space behind)
                    if (oldSpace instanceof HandicapSpace) {
                        lotList.set(vIdx, new HandicapSpace());
                    } else if (oldSpace instanceof FacultySpace) {
                        lotList.set(vIdx, new FacultySpace());
                    } else if (oldSpace instanceof ServiceSpace) {
                        lotList.set(vIdx, new ServiceSpace());
                    } else if (oldSpace instanceof StudentSpace) {
                        lotList.set(vIdx, new StudentSpace());
                    } else if (oldSpace instanceof VisitorSpace) {
                        lotList.set(vIdx, new VisitorSpace());
                    } else {
                        lotList.set(vIdx, new Space());
                    }
                    break; // plates are unique, so stop once the car is found
                }
            }
            return v.getDesignation();
        } catch (Exception e) {
            throw new Exception("warning: empty outgoing queue!!!");
        }
    }

    public boolean parkVehicle(Vehicle v) {
        
        if (v.isHandicapped()) {
            try {
                ArrayList<Space> handicapLot = getHandicapSpaces();
                for (int spaceIdx=0; spaceIdx<handicapLot.size(); spaceIdx++) {
                    if (handicapLot.get(spaceIdx).getFilledBy()==null) {
                        if (handicapLot.get(spaceIdx).parkVehicle(v)) {
                            filledHandicap++;
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return false;
            }
        } else {

        String desig = v.getDesignation();
        
        switch (desig) {
            
            case "student":
                
                try {
                    ArrayList<Space> studentLot = getStudentSpaces();
                    for (int spaceIdx=0; spaceIdx<studentLot.size(); spaceIdx++) {
                        if (studentLot.get(spaceIdx).getFilledBy()==null) {
                            if (studentLot.get(spaceIdx).parkVehicle(v)) {
                                filledStudent++;
                                return true;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    return false;
                }
                break;
            
            case "faculty":

                try {
                    ArrayList<Space> facultyLot = getFacultySpaces();
                    for (int spaceIdx=0; spaceIdx<facultyLot.size(); spaceIdx++) {
                        if (facultyLot.get(spaceIdx).getFilledBy()==null) {
                            if (facultyLot.get(spaceIdx).parkVehicle(v)) {
                                filledFaculty++;
                                return true;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    return false;
                }
                break;

            case "service":

                try {
                    ArrayList<Space> serviceLot = getServiceSpaces();
                    for (int spaceIdx=0; spaceIdx<serviceLot.size(); spaceIdx++) {
                        if (serviceLot.get(spaceIdx).getFilledBy()==null) {
                            if (serviceLot.get(spaceIdx).parkVehicle(v)) {
                                filledService++;
                                return true;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    return false;
                }
                break;

            case "visitor":

                try {
                    ArrayList<Space> visitorLot = getVisitorSpaces();
                    for (int spaceIdx=0; spaceIdx<visitorLot.size(); spaceIdx++) {
                        if (visitorLot.get(spaceIdx).getFilledBy()==null) {
                            if (visitorLot.get(spaceIdx).parkVehicle(v)) {
                                filledVisitor++;
                                return true;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    return false;
                }
                break;

            default:
                return false;
        }
        
    }
        return false;
}



    public boolean isFull(Vehicle vehicle) {

        // handicapped overrides designation, so turn both into one section name
        String section = vehicle.isHandicapped() ? "handicap" : vehicle.getDesignation();

        switch (section) {
            case "handicap":
                return filledHandicap >= nHandicapSpaces;
            case "student":
                return filledStudent >= nStudentSpaces;
            case "faculty":
                return filledFaculty >= nFacultySpaces;
            case "service":
                return filledService >= nServiceSpaces;
            case "visitor":
                return filledVisitor >= nVisitorSpaces;
            default:
                return true; // unknown designation: no section it can use
        }
    }

    public String searchByPlate(Vehicle vehicle) {
        String searchPlate = vehicle.getLicencePlate();
        String found = "Vehicle has not been found.";
        for(int i = 0; i<lotList.size(); i++) {
            if(lotList.get(i).getFilledBy() != null) {
                String testPlate = lotList.get(i).getFilledBy().getLicencePlate();
                if(searchPlate.equals(testPlate)) {
                    found = "Vehicle found in space " + i;
                }
            }
        }
        return found;
    }

    // ---------- methods used by the website (Parking.server) ----------

    // general spaces are plain Space objects (not one of the subclasses)
    public ArrayList<Space> getGeneralSpaces() {
        ArrayList<Space> generalSpaces = new ArrayList<Space>();
        for (int spaceIdx = 0; spaceIdx < size; spaceIdx++) {
            if (lotList.get(spaceIdx).getClass() == Space.class) {
                generalSpaces.add(lotList.get(spaceIdx));
            }
        }
        return generalSpaces;
    }

    // which section a space belongs to
    public static String sectionOf(Space space) {
        if (space instanceof HandicapSpace) return "handicap";
        if (space instanceof FacultySpace) return "faculty";
        if (space instanceof ServiceSpace) return "service";
        if (space instanceof StudentSpace) return "student";
        if (space instanceof VisitorSpace) return "visitor";
        return "general";
    }

    // open spots left in a section, O(1) using the counters
    public int openSpaces(String section) {
        switch (section) {
            case "handicap": return nHandicapSpaces - filledHandicap;
            case "student":  return nStudentSpaces - filledStudent;
            case "faculty":  return nFacultySpaces - filledFaculty;
            case "service":  return nServiceSpaces - filledService;
            case "visitor":  return nVisitorSpaces - filledVisitor;
            case "general":  return nGeneralSpaces - filledGeneral;
            default:         return 0;
        }
    }

    public int totalSpaces(String section) {
        switch (section) {
            case "handicap": return nHandicapSpaces;
            case "student":  return nStudentSpaces;
            case "faculty":  return nFacultySpaces;
            case "service":  return nServiceSpaces;
            case "visitor":  return nVisitorSpaces;
            case "general":  return nGeneralSpaces;
            default:         return 0;
        }
    }

    public boolean isSectionFull(String section) {
        return openSpaces(section) <= 0;
    }

    // parks the vehicle in a specific section of this lot
    // returns the space number it parked in, or -1 if that section had no space for it
    public int parkInSection(Vehicle v, String section) {
        if (isSectionFull(section)) {
            return -1;
        }
        for (int spaceIdx = 0; spaceIdx < lotList.size(); spaceIdx++) {
            Space space = lotList.get(spaceIdx);
            if (space.getFilledBy() == null && sectionOf(space).equals(section) && space.parkVehicle(v)) {
                switch (section) {
                    case "handicap": filledHandicap++; break;
                    case "student":  filledStudent++;  break;
                    case "faculty":  filledFaculty++;  break;
                    case "service":  filledService++;  break;
                    case "visitor":  filledVisitor++;  break;
                    default:         filledGeneral++;
                }
                return spaceIdx;
            }
        }
        return -1;
    }

    public int countParked() {
        return filledHandicap + filledStudent + filledFaculty + filledService + filledVisitor + filledGeneral;
    }

}