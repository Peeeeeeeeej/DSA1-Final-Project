# DSA1-Final-Project
Group 3's final Project for Stockton DSA1


First Class is a Java Compact file that will create all of the necessary data points for each thing. Those data outputs will then be put into CSV files and exported

## Project Structure

```
src/
└── Parking/
    ├── Vehicles/
    │   ├── Vehicle.java
    │   ├── Car.java
    │   ├── Motorcylce.java
    │   └── Truck.java
    ├── Lots/
    │   └── FacultySpace.java
    │   └── HandicapSpace.java
    │   └── Lot.java
    │   └── ServiceSpace.java
    │   └── Space.java
    │   └── StudentSpace.java
    │   └── VisitorSpace.java
    └── Data/
        └── ParkingDataCreator.java
```

| Package | Purpose |
|---------|---------|
| `Parking.Vehicles` | The vehicles being parked: the `Vehicle` base class and its subclasses. |
| `Parking.Lots` | Where vehicles park: lots, spaces, and space subclasses. |
| `Parking.Data` | Generates the simulated dataset and exports it to CSV files. |

## Vehicles

### Vehicle

The parent class that every vehicle type extends.

| Field | Type | Description |
|-------|------|-------------|
| `size` | `String` | Vehicle size: `"S"`, `"M"` or `"L"`. Set by the subclass. |
| `licencePlate` | `String` | A unique 6-character plate generated when the vehicle is created. |
| `licencePlateStorage` | `ArrayList<String>` | The list of plates that have already been used. |
| `ev` | `boolean` | `true` if the vehicle is electric. |
| `designation` | `String` | `"student"`, `"faculty"`, `"visitor"` or `"service"`. |
| `isHandicapped` | `boolean` | `true` if the vehicle needs a handicapped space. |
| `DESIGNATIONS` | `static final String[]` | The list of valid designations. |
| `DESIGNATION_WEIGHTS` | `static final int[]` | The percentage chance of each designation. |
| `CHARS` | `static final String` | The characters a plate can contain (A-Z, 0-9). |
| `RANDOM` | `static final Random` | The random number generator used for all random values. |

| Method | Description |
|--------|-------------|
| `randomPlate()` | Builds a random 6-character string from `CHARS`, for example `K7Q2ZP`. |
| `generatePlate()` | Calls `randomPlate()` in a `do-while` loop until it gets a plate that isn't in `licencePlateStorage`, then stores that plate and returns it. |
| `randomEv()` | Returns `true` 10% of the time. |
| `randomHandicapped()` | Returns `true` 5% of the time. |
| `randomDesignation()` | Picks a designation using `DESIGNATION_WEIGHTS`. |
| `getLicencePlate()` | Returns the vehicle's licence plate. Every subclass inherits this method. |

### Subclasses

| Class | Size |
|-------|------|
| `Car` | `"M"` (medium) |
| `Motorcylce` | `"S"` (small) |
| `Truck` | `"L"` (large) |

Each subclass passes its own fixed size to `Vehicle` through `super(...)`. Because the fields are `private`, subclasses read them through the inherited getters.

## How Random Values Are Generated

### Licence Plates

1. `randomPlate()` picks 6 random characters from A-Z and 0-9.
2. `generatePlate()` checks whether that plate is already in `licencePlateStorage`.
3. If the plate is taken, the loop generates a new one. If it's free, the plate is added to the storage list and returned.

With 36 possible characters in 6 positions, there are over 2 billion possible plates, so a repeat is rare. The loop guarantees uniqueness for the cases where one does happen.

### EV and Handicapped

`RANDOM.nextInt(100)` gives a number from 0 to 99. The vehicle is EV if the number is below 10, giving a 10% chance, and handicapped if it's below 5, giving a 5% chance.

### Designation

| Designation | Chance |
|-------------|--------|
| Student | 60% |
| Faculty | 25% |
| Visitor | 10% |
| Service | 5% |

`randomDesignation()` rolls a number from 0 to 99 and adds up the weights in order. The first designation whose running total is higher than the roll is chosen. To change the mix, edit `DESIGNATION_WEIGHTS` and keep the total at 100.

## Running

Open the project in IntelliJ, open `ParkingDataCreator`, and click the green run arrow next to `main`.
