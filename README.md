# DSA1-Final-Project
Group 3's final Project for Stockton DSA1


First Class is a Java Compact file that will create all of the necessary data points for each thing. Those data outputs will then be put into CSV files and exported

## Project Structure

| File | Description |
|------|-------------|
| `Vehicle.java` | Base class that holds the shared data and the licence plate logic for every vehicle. |
| `Car.java` | Subclass of `Vehicle` with size `"M"` (medium). |
| `Motorcylce.java` | Subclass of `Vehicle` with size `"S"` (small). |
| `Truck.java` | Subclass of `Vehicle` with size `"L"` (large). |
| `ParkingDataCreator.java` | Entry point (`main`). Loops 100 times to create vehicle data. |

## Vehicle Class

The parent class that every vehicle type extends.

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `size` | `String` | Vehicle size: `"S"`, `"M"` or `"L"`. Set by the subclass. |
| `licencePlate` | `String` | A 6-character plate generated when the vehicle is created. |
| `licencePlateStorage` | `ArrayList<String>` | The list of plates that have already been used. |
| `ev` | `boolean` | `true` if the vehicle is electric. |
| `priority` | `String` | The vehicle's parking priority. |
| `CHARS` | `static final String` | The characters a plate can contain (A-Z, 0-9). |
| `RANDOM` | `static final Random` | The random number generator used to build plates. |

### Constructor

```java
public Vehicle(String size, boolean ev, String priority)
```

The constructor generates a licence plate automatically and stores the size, EV status and priority.

### Methods

| Method | Description |
|--------|-------------|
| `randomPlate()` | Builds a random 6-character string from `CHARS`, for example `K7Q2ZP`. |
| `generatePlate()` | Calls `randomPlate()` repeatedly in a `do-while` loop until it gets a plate that isn't in `licencePlateStorage`, then stores that plate and returns it. |
| `getLicencePlate()` | Returns the vehicle's licence plate. Every subclass inherits this method. |

## Subclasses

Each subclass passes its own fixed size to `Vehicle` through `super(...)`. The caller only supplies the EV status and the priority.

```java
new Car(true, "high");        // size "M"
new Motorcylce(false, "low"); // size "S"
new Truck(false, "normal");   // size "L"
```

Because `licencePlate` is `private`, subclasses read it through `getLicencePlate()` rather than accessing the field directly.

## How Licence Plates Are Generated

1. `randomPlate()` picks 6 random characters from A-Z and 0-9.
2. `generatePlate()` checks whether that plate is already in `licencePlateStorage`.
3. If the plate is taken, the loop generates a new one. If it's free, the plate is added to the storage list and returned.

With 36 possible characters in 6 positions, there are over 2 billion possible plates, so a repeat is rare. The loop guarantees uniqueness for the cases where one does happen.

## Running

```bash
javac *.java
java ParkingDataCreator
```

`ParkingDataCreator` currently contains an empty loop that runs 100 times, intended for creating vehicles.
