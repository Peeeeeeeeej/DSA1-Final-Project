package Parking.Lots;
import Parking.Vehicles.Vehicle;

public class Space {

    Boolean isFull = false;
    Vehicle filledBy = null;

    public Space(Vehicle vehicle) {
        isFull = true;
        this.filledBy = vehicle;
    }

    // empty constructor - isFull remains false and filledBy remains null
    public Space() {}

    public boolean parkVehicle(Vehicle vehicle) {
        if (isFull!=false) {
            return false;
        } else {
            isFull = true;
            this.filledBy = vehicle;
            return true;
        }
    }

    public Vehicle getFilledBy() {
        return filledBy;
    }

}
