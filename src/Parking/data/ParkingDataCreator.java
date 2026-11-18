package Parking.data;

import Parking.Lots.*;
import Parking.Vehicles.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParkingDataCreator {

    private static final Random RANDOM = new Random();
    private static final int NUM_VEHICLES = 300;
    private static final int[] LOT_SIZES = {80, 70, 60, 50, 50, 45, 45, 40, 30, 30}; // adds to 500

    public static void main(String[] args) {
        // create vehicles
        ArrayList<Vehicle> vehicles = new ArrayList<>();
        for (int i = 0; i < NUM_VEHICLES; i++) {
            vehicles.add(randomVehicle());
        }

        // create lots
        ArrayList<Lot> lots = new ArrayList<>();
        for (int size : LOT_SIZES) {
            lots.add(new Lot(size));
        }

        writeVehiclesCsv(vehicles, "vehicles.csv");
        writeLotsCsv(lots, "lots.csv");

        System.out.println("Created " + vehicles.size() + " vehicles and " + lots.size() + " lots.");
    }

    // 75% car, 15% truck, 10% motorcycle
    private static Vehicle randomVehicle() {
        int roll = RANDOM.nextInt(100);
        if (roll < 75) {
            return new Car();
        } else if (roll < 90) {
            return new Truck();
        } else {
            return new Motorcycle();
        }
    }

    private static void writeVehiclesCsv(List<Vehicle> vehicles, String fileName) {
        try (PrintWriter out = new PrintWriter(new FileWriter(fileName))) {
            out.println("vehicleId,licencePlate,type,size,designation,ev,handicapped");
            for (int i = 0; i < vehicles.size(); i++) {
                Vehicle v = vehicles.get(i);
                out.println((i + 1) + ","
                        + v.getLicencePlate() + ","
                        + v.getClass().getSimpleName() + ","
                        + v.getSize() + ","
                        + v.getDesignation() + ","
                        + v.isEv() + ","
                        + v.isHandicapped());
            }
        } catch (IOException e) {
            System.out.println("Could not write " + fileName + ": " + e.getMessage());
        }
    }

    private static void writeLotsCsv(List<Lot> lots, String fileName) {
        try (PrintWriter out = new PrintWriter(new FileWriter(fileName))) {
            out.println("lotId,lotSize,spaceId,spaceType");
            for (int lotNum = 0; lotNum < lots.size(); lotNum++) {
                Lot lot = lots.get(lotNum);
                List<Space> spaces = lot.getSpaces();
                for (int spaceNum = 0; spaceNum < spaces.size(); spaceNum++) {
                    String type = spaces.get(spaceNum).getClass().getSimpleName();
                    if (type.equals("Space")) {
                        type = "GeneralSpace";
                    }
                    out.println("Lot" + (lotNum + 1) + ","
                            + lot.getSize() + ","
                            + String.format("L%d-%03d", lotNum + 1, spaceNum + 1) + ","
                            + type);
                }
            }
        } catch (IOException e) {
            System.out.println("Could not write " + fileName + ": " + e.getMessage());
        }
    }
}