package Parking.Lots;

import Parking.Vehicles.Vehicle;

public class EVSpace extends Space {
    
    public EVSpace() {}

    @Override
    public boolean parkVehicle(Vehicle vehicle) {
        if (vehicle.isEv() == false || isFull) {
            return false;
        } else {
            this.isFull = true;
            this.filledBy = vehicle;
            return true;
        }
        
    }

}
