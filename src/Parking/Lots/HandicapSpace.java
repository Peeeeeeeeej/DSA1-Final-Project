package Parking.Lots;

import Parking.Vehicles.Vehicle;

public class HandicapSpace extends Space {
    
    public HandicapSpace() {}

    @Override
    public boolean parkVehicle(Vehicle vehicle) {
        if (vehicle.isHandicapped() == false || isFull) {
            return false;
        } else {
            this.isFull = true;
            this.filledBy = vehicle;
            return true;
        }
        
    }

}
