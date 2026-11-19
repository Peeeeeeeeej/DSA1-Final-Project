package Parking.Vehicles;

public class Car extends Vehicle {

    // random designation, EV, handicapped and plate
    public Car() {
        super("M");
    }

    // chosen values (blank plate = random plate)
    public Car(String plate, String designation, boolean ev, boolean handicapped) {
        super("M", plate, designation, ev, handicapped);
    }
}
