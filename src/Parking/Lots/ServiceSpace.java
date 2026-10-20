package Parking.Lots;

import Parking.Vehicles.Vehicle;

public class ServiceSpace extends Space {
    
    public ServiceSpace() {}

    @Override
    public boolean parkVehicle(Vehicle vehicle) {
        if (vehicle.getDesignation() != "service" || isFull) {
            return false;
        } else {
            this.isFull = true;
            this.filledBy = vehicle;
            return true;
        }
        
    }

}
