package Parking.Vehicles;

public class Motorcycle extends Vehicle {

    // random designation, EV, handicapped and plate
    public Motorcycle() {
        super("S");
    }

    // chosen values (blank plate = random plate)
    public Motorcycle(String plate, String designation, boolean ev, boolean handicapped) {
        super("S", plate, designation, ev, handicapped);
    }
}
