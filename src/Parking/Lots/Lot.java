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

<<<<<<< HEAD
    // queue-related methods

    public void addIncoming(Vehicle v) {
        incoming.offer(v);
    }

    public void addOutgoing(Vehicle v) {
        outgoing.offer(v);
    }

    // poll and return vehicle type
    public String removeIncoming() {
        Vehicle v = incoming.peek();
        parkVehicle(incoming.poll());
        return v.getDesignation();
    }

    public String removeOutgoing() {
        Vehicle v = outgoing.peek();
        // remove vehicle from its old space (inefficient, someone else pls implement binary search or something)
        for (int vIdx=0; vIdx<lotList.size(); vIdx++) {
            if (lotList.get(vIdx).getFilledBy()==v) {
                lotList.set(vIdx, new Space());
            }
        }
        return v.getDesignation();
    }

    public boolean parkVehicle(Vehicle v) {
        
        if (v.isHandicapped()) {
            try {
                ArrayList<Space> handicapLot = getHandicapSpaces();
                for (int spaceIdx=0; spaceIdx<handicapLot.size(); spaceIdx++) {
                    if (handicapLot.get(spaceIdx).getFilledBy()==null) {
                        handicapLot.get(spaceIdx).parkVehicle(v);
                        return true;
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
                            studentLot.get(spaceIdx).parkVehicle(v);
                            return true;
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
                            facultyLot.get(spaceIdx).parkVehicle(v);
                            return true;
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
                            serviceLot.get(spaceIdx).parkVehicle(v);
                            return true;
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
                            visitorLot.get(spaceIdx).parkVehicle(v);
                            return true;
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

=======
>>>>>>> 2345680e27fab93a0ce687a196a22fb454444bb7
}