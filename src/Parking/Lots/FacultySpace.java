package Parking.Lots;

import Parking.Vehicles.Vehicle;

public class FacultySpace extends Space {
    
    public FacultySpace() {}

    @Override
    public boolean parkVehicle(Vehicle vehicle) {
        if (vehicle.getDesignation() != "faculty" || isFull) {
            return false;
        } else {
            this.isFull = true;
            this.filledBy = vehicle;
            return true;
        }
        
    }

}
