/*
* Group Members:
* Ali Radwan Farouk    20231110
* Adel Hefny           20230198
* Mohamed Mahmoud      20230354
* Asser Ahmed          20230655
* 
* Section: AI S5
* Submission to TA: Mena Asfour
 */

import java.util.Scanner;

public class ServiceStation {
    // Shared configuration variables for pumps, queue, and cars
    static Integer pumps_count, queue_size, cars_size;
    static String[] car_names;

    // Method to take input from the user (interactive setup)
    public static void take_inputs() {
        Scanner scanner = new Scanner(System.in);

        // Input number of pumps available for servicing
        System.out.print("Enter the number of pumps: ");
        pumps_count = Integer.parseInt(scanner.nextLine());

        // Input maximum number of cars that can wait in the queue
        System.out.print("Enter the number of waiting area size: ");
        queue_size = Integer.parseInt(scanner.nextLine());

        // Input arriving cars in order (comma separated)
        System.out.print("Cars arriving (order): ");
        String input = scanner.nextLine();

        // Clean input (remove spaces) and split into an array of car names
        car_names = input.replaceAll(" ", "").split(","); 
        cars_size = car_names.length;

        scanner.close();
    }

    public static void main(String[] args) {
        // Step 1: Gather inputs from user
        take_inputs();

        // Step 2: Initialize the shared bounded buffer (waiting area)
        BoundedBuffer<Car> buffer = new BoundedBuffer<>(queue_size);

        // Step 3: Initialize semaphore representing available pumps (service bays)
        Semaphore bays = new Semaphore(pumps_count);

        // Step 4: Create and start pump threads — each representing a service pump
        for (int i = 1; i <= pumps_count; i++) {
            new Pump(i, buffer, bays).start();
        }

        // Step 5: Create and start car threads — each representing an arriving car
        for (int i = 0; i < cars_size; i++) {
            new Car(i, car_names[i], buffer).start();
            try {
                Thread.sleep(100); // small delay between car arrivals
            } catch (InterruptedException ignored) {}
        }
    }
}

// ============================================================================
// Custom Semaphore class (manual implementation of semaphore behavior)
// Used for synchronization and managing resource availability
// ============================================================================
class Semaphore {
    private Integer value;

    // Initialize semaphore with a given count (e.g., number of available pumps)
    public Semaphore(int initial) {
        this.value = initial;
    }

    // demand(): Acquire a permit. If none are available, the thread waits.
    public synchronized void demand() {
        value--;
        if (value < 0) {
            try {
                wait(); // block the thread until a permit is released
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // release(): Free a permit and wake up one waiting thread (if any)
    public synchronized void release() {
        value++;
        if (value <= 0) {
            notify(); // wake up one waiting thread
        }
    }
}

// ============================================================================
// Car class — represents a car arriving to the service station
// Each car acts as a producer that tries to enter the waiting queue
// ============================================================================
class Car extends Thread {
    private final int id;
    private final String name;
    private final BoundedBuffer<Car> buffer;

    public Car(int id, String name, BoundedBuffer<Car> buffer) {
        this.id = id;
        this.buffer = buffer;
        this.name = name;
    }

    @Override
    public void run() {
        // When a car arrives, it prints a message and tries to enter the queue
        System.out.println("Car " + this.name + " arrived");
        buffer.produce(this); // Add car to waiting area
    }

    // Getter method to return car name
    public String get_car_name() {
        return this.name;
    }
}

// ============================================================================
// Pump class — represents a pump that continuously serves cars
// Each pump acts as a consumer thread pulling cars from the waiting buffer
// ============================================================================
class Pump extends Thread {
    private final int id;
    private final BoundedBuffer<Car> buffer;
    private final Semaphore bays;

    public Pump(int id, BoundedBuffer<Car> buffer, Semaphore bays) {
        this.id = id;
        this.buffer = buffer;
        this.bays = bays;
    }

    @Override
    public void run() {
        while (true) {
            // Take the next car from the waiting queue (consume operation)
            Car car = buffer.consume();
            System.out.println("Pump " + id + ": Car " + car.get_car_name() + " taken for service " +
                    "(Queue size: " + buffer.get_count() + ")");

            // Try to occupy a service bay (if all are full, wait)
            bays.demand();
            System.out.println("Pump " + id + ": Car " + car.get_car_name() + " begins service");

            try {
                Thread.sleep(1500); // Simulate time required for servicing
            } catch (InterruptedException e) {
                // If interrupted, simply continue (not critical for simulation)
            }

            // Service completed, release the bay and log the status
            System.out.println("Pump " + id + ": Car " + car.get_car_name() + " finished service");
            System.out.println("Pump " + id + ": Bay released");
            bays.release(); // Make the bay available for another car
        }
    }
}

// ============================================================================
// BoundedBuffer class — Shared buffer implementing Producer-Consumer model
// Manages concurrent access using semaphores to avoid race conditions
// ============================================================================
class BoundedBuffer<T> {
    private final Object[] buffer; // storage array for queued items
    private int in = 0;            // next insertion index
    private int out = 0;           // next removal index
    private int count = 0;         // number of current items in the buffer
    private final int size;        // maximum buffer capacity

    // Semaphores for controlling access and capacity
    private final Semaphore empty; // counts available empty slots
    private final Semaphore full;  // counts filled slots
    private final Semaphore mutex; // ensures mutual exclusion

    // Initialize the buffer and semaphores
    public BoundedBuffer(int size) {
        this.size = size;
        buffer = new Object[size];
        empty = new Semaphore(size); // initially all slots are empty
        full = new Semaphore(0);     // initially no items are full
        mutex = new Semaphore(1);    // only one thread can modify buffer at a time
    }

    // ------------------------------------------------------------------------
    // produce(): Called by Car threads to add themselves to the waiting area
    // ------------------------------------------------------------------------
    public void produce(T item) {
        empty.demand();  // wait for an empty slot
        mutex.demand();  // enter critical section

        buffer[in] = item; // add car to queue
        count++;
        System.out.println("Car " + ((Car) item).get_car_name() +
                " entered the queue (Queue size: " + count + ")");
        in = (in + 1) % size; // circular increment

        mutex.release(); // exit critical section
        full.release();  // signal that a new item is available
    }

    // ------------------------------------------------------------------------
    // consume(): Called by Pump threads to take the next car for service
    // ------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public T consume() {
        full.demand();   // wait until a car is available
        mutex.demand();  // enter critical section

        T item = (T) buffer[out]; // remove car from queue
        out = (out + 1) % size;   // circular increment
        count--;

        mutex.release(); // exit critical section
        empty.release(); // signal an empty slot is free
        return item;     // return the car object
    }

    // Get the current number of cars waiting in the queue
    public int get_count() {
        return this.count;
    }
}
