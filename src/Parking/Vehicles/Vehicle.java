package Parking.Vehicles;

import java.util.Random;
import java.util.ArrayList;

public class Vehicle {
    private String size;
    private String licencePlate;
    private ArrayList<String> licencePlateStorage = new ArrayList();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();
    private boolean ev;
    private String priority;

    public Vehicle(String size, boolean ev, String priority) {
        this.licencePlate = generatePlate();
        this.size = size;
        this.ev = ev;
        this.priority = priority;
    }

    public static String randomPlate() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public String generatePlate() {
        String plate;
        do {
            plate = randomPlate();
        } while (licencePlateStorage.contains(plate));
        licencePlateStorage.add(plate);
        return plate;
    }

    public String getLicencePlate() {
        return licencePlate;
    }
}
