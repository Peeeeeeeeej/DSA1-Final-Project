package Parking.Lots;

import java.util.*;

public class Lot {

    private int size;
    private List<Space> lotList = new ArrayList<>();

    private int nHandicapSpaces;
    private int nFacultySpaces;
    private int nServiceSpaces;
    private int nStudentSpaces;
    private int nVisitorSpaces;

    public Lot(int sizeInit) {
        
        this.size = sizeInit;
        this.lotList = new ArrayList<>(sizeInit);

        // based on designations in Vehicle.java (+percentages for EV and handicap spaces)
        this.nFacultySpaces = (int) Math.floor((double) sizeInit * 0.25);
        this.nServiceSpaces = (int) Math.floor((double) sizeInit * 0.05);
        this.nStudentSpaces = (int) Math.floor((double) sizeInit * 0.45); // was 0.60 before EV and handicap
        this.nVisitorSpaces = (int) Math.floor((double) sizeInit * 0.10);
        this.nHandicapSpaces = (int) Math.floor((double) sizeInit * 0.05);

        // account for left-over spaces from truncation of ceiling function
        for (int spaceIdx = 0; spaceIdx < (size-(nFacultySpaces+nServiceSpaces+nStudentSpaces+nVisitorSpaces+nHandicapSpaces)); spaceIdx++) {
            lotList.add(new Space());
        }

        // append faculty spaces
        for (int spaceIdx=0; spaceIdx<nFacultySpaces; spaceIdx++) {
            lotList.add(new FacultySpace());
        }

        // append service spaces
        for (int spaceIdx=0; spaceIdx<nServiceSpaces; spaceIdx++) {
            lotList.add(new ServiceSpace());
        }

        // append student spaces
        for (int spaceIdx=0; spaceIdx<nStudentSpaces; spaceIdx++) {
            lotList.add(new StudentSpace());
        }

        // append visitor spaces
        for (int spaceIdx=0; spaceIdx<nVisitorSpaces; spaceIdx++) {
            lotList.add(new VisitorSpace());
        }
        
        // append handicap spaces
        for (int spaceIdx=0; spaceIdx<nHandicapSpaces; spaceIdx++) {
            lotList.add(new HandicapSpace());
        }
    }

    public int getSize() {
        return size;
    }

    public ArrayList<Space> getFacultySpaces() {
        ArrayList<Space> facultySpaces = new ArrayList<Space>();
        for (int spaceIdx = 0; spaceIdx<size; spaceIdx++) {
            if (lotList.get(spaceIdx) instanceof FacultySpace) {
                facultySpaces.add(lotList.get(spaceIdx));
            }
        }
        return facultySpaces;
    }

    public ArrayList<Space> getServiceSpaces() {
        ArrayList<Space> serviceSpaces = new ArrayList<Space>();
        for (int spaceIdx = 0; spaceIdx<size; spaceIdx++) {
            if (lotList.get(spaceIdx) instanceof ServiceSpace) {
                serviceSpaces.add(lotList.get(spaceIdx));
            }
        }
        return serviceSpaces;
    }

    public ArrayList<Space> getStudentSpaces() {
        ArrayList<Space> studentSpaces = new ArrayList<Space>();
        for (int spaceIdx = 0; spaceIdx<size; spaceIdx++) {
            if (lotList.get(spaceIdx) instanceof StudentSpace) {
                studentSpaces.add(lotList.get(spaceIdx));
            }
        }
        return studentSpaces;
    }

    public ArrayList<Space> getVisitorSpaces() {
        ArrayList<Space> visitorSpaces = new ArrayList<Space>();
        for (int spaceIdx = 0; spaceIdx<size; spaceIdx++) {
            if (lotList.get(spaceIdx) instanceof VisitorSpace) {
                visitorSpaces.add(lotList.get(spaceIdx));
            }
        }
        return visitorSpaces;
    }

    public ArrayList<Space> getHandicapSpaces() {
        ArrayList<Space> handicapSpaces = new ArrayList<Space>();
        for (int spaceIdx = 0; spaceIdx<size; spaceIdx++) {
            if (lotList.get(spaceIdx) instanceof HandicapSpace) {
                handicapSpaces.add(lotList.get(spaceIdx));
            }
        }
        return handicapSpaces;
    }

    public List<Space> getSpaces() {
        return lotList;
    }

}