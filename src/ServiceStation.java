import java.util.LinkedList;
import java.util.Queue;

class Semaphore
{
    private int value;

    public Semaphore(int initial)
    {
        this.value = initial;
    }
    
    public synchronized void demand()
    {
        value--;
        while (value < 0) { // while and not if, to ensure that it waits
            try {
                wait(); // add to queue + sleep
            } catch (InterruptedException e) {

            }
        }
    }

    public synchronized void release()
    {
        value++;
        notify(); // remove from queue (not neces. this one) + wake up
    }
}


class Car extends Thread {
    private final int id;
    private final Queue<Car> queue;
    private final Semaphore mutex, free, standby;

    public Car(int id, Queue<Car> queue, Semaphore mutex, Semaphore free, Semaphore standby) {
        this.id = id;
        this.queue = queue;
        this.mutex = mutex;
        this.free = free;
        this.standby = standby;
    }

    @Override
    public void run() 
    {
        System.out.println("Car " + id + " arrived");
        free.demand(); // attempt to join + modify queue
        mutex.demand();

        queue.add(this);
        System.out.println("Car " + id + " entered the queue (Queue size: " + queue.size() + ")");

        mutex.release();
        standby.release(); // allow a pump to serve
    }

    public int getCarId() {
        return id;
    }
}

class Pump extends Thread {
    private final int id;
    private final Queue<Car> cars_queue;
    private final Semaphore mutex, free, standby, bays;

    public Pump(int id, Queue<Car> cars_queue, Semaphore mutex, Semaphore free, Semaphore standby, Semaphore bays) {
        this.id = id;
        this.cars_queue = cars_queue;
        this.mutex = mutex;
        this.free = free;
        this.standby = standby;
        this.bays = bays;
    }

    @Override
    public void run() 
    {
        while (true) 
        {
            standby.demand(); // attempt to serve + modify queue
            mutex.demand();

            Car car = cars_queue.poll();
            System.out.println("Pump " + id + ": Car " + car.getCarId() + " taken for service");
            mutex.release();
            free.release(); // allow a new car to join the queue

            bays.demand(); // attempt to occupy a bay
            System.out.println("Pump " + id + ": Car " + car.getCarId() + " begins service");
            try {
                Thread.sleep(1500); // service time
            } catch (InterruptedException e) {

            }

            System.out.println("Pump " + id + ": Car " + car.getCarId() + " finished service");
            System.out.println("Pump " + id + ": Bay released");
            bays.release(); // allow another pump to occupy the bay
        }
    }
}

public class ServiceStation {
    public static void main(String[] args) 
    {
        int waitingAreaCapacity = 5;
        int numPumps = 3;
        int numCars = 10;

        Queue<Car> cars_queue = new LinkedList<>();

        Semaphore mutex = new Semaphore(1);
        Semaphore free = new Semaphore(waitingAreaCapacity);
        Semaphore standby = new Semaphore(0);
        Semaphore bays = new Semaphore(numPumps);

        for (int i = 1; i <= numPumps; i++) {
            new Pump(i, cars_queue, mutex, free, standby, bays).start();
        }

        for (int i = 1; i <= numCars; i++) {
            new Car(i, cars_queue, mutex, free, standby).start();
            try {
                Thread.sleep(500); // arrival delay
            } catch (InterruptedException e) {

            }
        }
    }
}
