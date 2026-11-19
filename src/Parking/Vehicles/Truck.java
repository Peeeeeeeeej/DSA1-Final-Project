package Parking.Vehicles;

public class Truck extends Vehicle {

    // random designation, EV, handicapped and plate
    public Truck() {
        super("L");
    }

    // chosen values (blank plate = random plate)
    public Truck(String plate, String designation, boolean ev, boolean handicapped) {
        super("L", plate, designation, ev, handicapped);
    }
}
