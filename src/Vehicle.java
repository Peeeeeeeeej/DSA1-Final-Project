import java.util.Random;
import java.util.ArrayList;

public class Vehicle {
    private String size;
    private final String licencePlate;
    private ArrayList<String> licencePlateStorage = new ArrayList();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();

    public Vehicle(String size) {
        this.licencePlate = generatePlate();
        this.size = size;
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



}
