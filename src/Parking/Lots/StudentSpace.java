package Parking.Lots;

import Parking.Vehicles.Vehicle;

public class StudentSpace extends Space {
    
    public StudentSpace() {}

    @Override
    public boolean parkVehicle(Vehicle vehicle) {
        if (!"student".equals(vehicle.getDesignation()) || isFull) {
            return false;
        } else {
            this.isFull = true;
            this.filledBy = vehicle;
            return true;
        }
        
    }

}
