package Parking.Vehicles;

import java.util.Random;
import java.util.ArrayList;

public class Vehicle {
    private static final String[] DESIGNATIONS = {"student", "faculty", "visitor", "service"};
    private static final int[] DESIGNATION_WEIGHTS = {60, 25, 10, 5};  // percentages, must add to 100
    private String size;
    private String licencePlate;
    private static ArrayList<String> licencePlateStorage = new ArrayList<>();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();
    private boolean ev;
    private String designation;
    private boolean isHandicapped;

    public Vehicle(String size) {
        this.licencePlate = generatePlate();
        this.size = size;
        this.ev = randomEv();
        this.designation = randomDesignation();
        this.isHandicapped = randomHandicapped();
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

    // 10% chance of being electric
    private static boolean randomEv() {
        return RANDOM.nextInt(100) < 10;
    }

    // 5% chance of needing a handicapped space
    private static boolean randomHandicapped() {
        return RANDOM.nextInt(100) < 5;
    }

    // 60% student, 25% faculty, 10% visitor, 5% service
    private static String randomDesignation() {
        int roll = RANDOM.nextInt(100);  // 0 to 99
        int total = 0;
        for (int i = 0; i < DESIGNATIONS.length; i++) {
            total += DESIGNATION_WEIGHTS[i];
            if (roll < total) {
                return DESIGNATIONS[i];
            }
        }
        return DESIGNATIONS[0];  // fallback, never reached if weights add to 100
    }

    public String getLicencePlate() {
        return licencePlate; }
    public String getSize() {
        return size; }
    public boolean isEv() {
        return ev; }
    public String getDesignation() {
        return designation; }
    public boolean isHandicapped() {
        return isHandicapped; }
}