package Parking.Lots;

import Parking.Vehicles.Vehicle;

public class VisitorSpace extends Space {
    
    public VisitorSpace() {}

    @Override
    public boolean parkVehicle(Vehicle vehicle) {
        if (vehicle.getDesignation() != "visitor" || isFull) {
            return false;
        } else {
            this.isFull = true;
            this.filledBy = vehicle;
            return true;
        }
        
    }

}
