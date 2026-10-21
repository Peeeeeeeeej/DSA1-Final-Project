# DSA1-Final-Project
Group 3's final Project for Stockton DSA1


First Class is a Java Compact file that will create all of the necessary data points for each thing. Those data outputs will then be put into CSV files and exported

## Project Structure

```
src/
└── Parking/
    ├── data/
    │   └── ParkingDataCreator.java
    ├── Lots/
    │   ├── Lot.java
    │   ├── Space.java
    │   ├── FacultySpace.java
    │   ├── StudentSpace.java
    │   ├── VisitorSpace.java
    │   ├── ServiceSpace.java
    │   └── HandicapSpace.java
    └── Vehicles/
        ├── Vehicle.java
        ├── Car.java
        ├── Motorcycle.java
        └── Truck.java
```

| Package | Purpose |
|---------|---------|
| `Parking.data` | Generates the simulated dataset and exports it to CSV files. |
| `Parking.Lots` | Where vehicles park: parking lots and the different types of spaces. |
| `Parking.Vehicles` | The vehicles being parked: the `Vehicle` base class and its subclasses. |

## Vehicles

### Vehicle

The parent class that every vehicle type extends. All of a vehicle's data except its size is generated automatically when the vehicle is created.

```java
public Vehicle(String size)
```

| Field | Type | Description |
|-------|------|-------------|
| `size` | `String` | Vehicle size: `"S"`, `"M"` or `"L"`. Set by the subclass. |
| `licencePlate` | `String` | A unique 6-character plate. |
| `ev` | `boolean` | `true` if the vehicle is electric. |
| `designation` | `String` | `"student"`, `"faculty"`, `"visitor"` or `"service"`. |
| `isHandicapped` | `boolean` | `true` if the vehicle needs a handicapped space. |
| `licencePlateStorage` | `static ArrayList<String>` | Every plate used so far, shared by all vehicles. |
| `DESIGNATIONS` | `static final String[]` | The list of valid designations. |
| `DESIGNATION_WEIGHTS` | `static final int[]` | The percentage chance of each designation. |
| `CHARS` | `static final String` | The characters a plate can contain (A-Z, 0-9). |
| `RANDOM` | `static final Random` | The random number generator used for all random values. |

| Method | Description |
|--------|-------------|
| `randomPlate()` | Builds a random 6-character string from `CHARS`, for example `K7Q2ZP`. |
| `generatePlate()` | Calls `randomPlate()` in a `do-while` loop until it gets an unused plate, then stores that plate and returns it. |
| `randomEv()` | Returns `true` 10% of the time. |
| `randomHandicapped()` | Returns `true` 5% of the time. |
| `randomDesignation()` | Picks a designation using `DESIGNATION_WEIGHTS`. |
| `getLicencePlate()` | Returns the licence plate. |
| `getSize()` | Returns the size. |
| `isEv()` | Returns whether the vehicle is electric. |
| `getDesignation()` | Returns the designation. |
| `isHandicapped()` | Returns whether the vehicle needs a handicapped space. |

### Subclasses

| Class | Size |
|-------|------|
| `Car` | `"M"` (medium) |
| `Motorcycle` | `"S"` (small) |
| `Truck` | `"L"` (large) |

Each subclass passes its size to `Vehicle` through `super(...)`, so creating a vehicle is just:

```java
Vehicle v = new Car();
```

Subclasses inherit all of the getters from `Vehicle`.

## How Random Values Are Generated

### Licence Plates

1. `randomPlate()` picks 6 random characters from A-Z and 0-9.
2. `generatePlate()` checks whether that plate is already in `licencePlateStorage`.
3. If the plate is taken, the loop generates a new one. If it's free, the plate is added to the storage list and returned.

`licencePlateStorage` is `static`, so every vehicle checks against the same list and no two vehicles can share a plate. With 36 possible characters in 6 positions, there are over 2 billion possible plates, so a repeat is rare.

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

## Lots

### Space

The base class for a single parking space. A plain `Space` accepts any vehicle.

| Field | Type | Description |
|-------|------|-------------|
| `isFull` | `Boolean` | `true` if a vehicle is parked here. Starts as `false`. |
| `filledBy` | `Vehicle` | The vehicle parked here, or `null` if empty. |

| Constructor / Method | Description |
|--------|-------------|
| `Space()` | Creates an empty space. |
| `Space(Vehicle vehicle)` | Creates a space that's already filled by the given vehicle. |
| `parkVehicle(Vehicle vehicle)` | Parks the vehicle and returns `true` if the space is empty, otherwise returns `false`. |
| `getFilledBy()` | Returns the vehicle parked here, or `null`. |

### Space Subclasses

Each subclass overrides `parkVehicle()` so that only the right kind of vehicle can park there. If the vehicle doesn't qualify, or the space is already full, `parkVehicle()` returns `false`.

| Class | Who can park |
|-------|--------------|
| `FacultySpace` | Vehicles with designation `"faculty"` |
| `StudentSpace` | Vehicles with designation `"student"` |
| `VisitorSpace` | Vehicles with designation `"visitor"` |
| `ServiceSpace` | Vehicles with designation `"service"` |
| `HandicapSpace` | Vehicles where `isHandicapped()` is `true` |

### Lot

A parking lot made up of a list of spaces.

```java
public Lot(int sizeInit)
```

The constructor takes the total number of spaces and splits them by type:

| Space Type | Share of Lot |
|------------|--------------|
| Student | 45% |
| Faculty | 25% |
| Visitor | 10% |
| Service | 5% |
| Handicap | 5% |
| General (`Space`) | Remainder (about 10%) |

Each count is rounded down with `Math.floor`. Any spaces left over, from the remaining percentage plus rounding, become general `Space` objects that accept any vehicle.

| Method | Description |
|--------|-------------|
| `getSize()` | Returns the total number of spaces in the lot. |
| `getFacultySpaces()` | Returns a list of all `FacultySpace`s in the lot. |
| `getStudentSpaces()` | Returns a list of all `StudentSpace`s in the lot. |
| `getVisitorSpaces()` | Returns a list of all `VisitorSpace`s in the lot. |
| `getServiceSpaces()` | Returns a list of all `ServiceSpace`s in the lot. |
| `getHandicapSpaces()` | Returns a list of all `HandicapSpace`s in the lot. |

The getters use `instanceof` to check each space's type.

## Data

`ParkingDataCreator` is the program's entry point. It currently contains a loop that runs 100 times, which will be used to generate the dataset.

## Running

Open the project in IntelliJ, open `ParkingDataCreator`, and click the green run arrow next to `main`.
