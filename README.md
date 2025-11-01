# Car Wash Simulation

## Project Description

This project is a Java-based simulation of a car wash and gas station, built to solve a classic concurrency challenge the Producer-Consumer Problem. The goal is to manage a shared, limited set of resources (service bays and waiting spots) without causing race conditions or deadlocks.

In this simulation, Cars (Producers) continuously arrive at the station seeking service. Theay must first enter a waiting area (a bounded bufferqueue) of a fixed size. If the queue is full, the car must wait.

The Pumps (Consumers) represent the service bays. A pump can only service a car if there is one waiting in the queue and if a service bay is free. The entire process is managed using custom-built semaphores and mutexes to ensure that

 Only one thread (car or pump) modifies the waiting queue at a time.
 Cars don't try to enter a full queue.
 Pumps don't try to service cars from an empty queue.
 The number of cars being serviced never exceeds the number of available pumps.

## Implementation Details

The simulation is built in Java and uses custom-built `Semaphore` objects to manage concurrency.

### Core Java Classes

1.  `ServiceStation` (Main Class)
     Initializes all shared resources the bounded buffer (Queue), Mutex for the queue, and Semaphores (`Empty`, `Full`, `Pumps`).
     Takes user input for waiting area capacity and the number of service bays.
     Creates and starts the thread pool for `Pump` (Consumers).
     Creates and starts a continuous stream of `Car` (Producer) threads.

2.  `Car` (Producer)
     Implements `Runnable`.
     Represents a car arriving at the station.
     Interacts with the `Empty` and `Full` semaphores and the `Mutex` to safely add itself to the waiting queue.

3.  `Pump` (Consumer)
     Implements `Runnable`.
     Represents a service bay.
     Interacts with the `Empty`, `Full`, and `Pumps` semaphores, as well as the `Mutex`.
     Waits for a car to be in the queue (`Full`).
     Waits for a service bay to be available (`Pumps`).
     Removes the car from the queue, services it, and then releases the service bay.

4.  `Semaphore`
     A custom implementation of a counting semaphore with `wait()` (P) and `signal()` (V) operations.

## How to Run

1.  Compile all `.java` files.
2.  Run the `ServiceStation` class
    ```bash
    java ServiceStation
    ```
3.  The program will prompt you to enter
     The capacity of the waiting area (queue).
     The number of available service bays (pumps).

## Output

The console will log all system activities, showing the state of each car and pump.

### Sample Output

Input
 Waiting area capacity 5
 Number of service bays (pumps) 3

Log Sequence
   ```
   C1 arrived
   C2 arrived
   C3 arrived
   C4 arrived
   Pump 1 C1 Occupied
   Pump 2 C2 Occupied
   Pump 3 C3 Occupied
   C4 arrived and waiting
   C5 arrived
   C5 arrived and waiting
   Pump 1 C1 login
   Pump 1 C1 begins service at Bay 1
   Pump 2 C2 login
   Pump 2 C2 begins service at Bay 2
   Pump 3 C3 login
   Pump 3 C3 begins service at Bay 3
   Pump 1 C1 finishes service
   Pump 1 Bay 1 is now free
   Pump 2 C2 finishes service
   Pump 2 Bay 2 is now free
   Pump 1 C4 login
   Pump 1 C4 begins service at Bay 1
   Pump 3 C3 finishes service
   Pump 3 Bay 3 is now free
   Pump 2 C5 login
   Pump 2 C5 begins service at Bay 2
   Pump 1 C4 finishes service
   Pump 1 Bay 1 is now free
   Pump 3 C3 finishes service
   Pump 3 Bay 3 is now free
   Pump 2 C5 finishes service
   Pump 2 Bay 2 is now free
   All cars processed; simulation ends
   ```

## Acknowledgements
This project was completed as an assignment for the CS241 Operating System - 1 course at the Faculty of Computers and Artificial Intelligence, Cairo University.

