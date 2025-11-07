/*
* Group Members:
* Ali Radwan Farouk    20231110
* Adel Hefny           20230198
* Ziad Salama          20230150
* Mohamed Mahmoud      20230354
* Asser Ahmed          20230655
* 
* Section: AI S5
* Submission to TA: Mena Asfour
 */
import java.util.Scanner;

public class ServiceStation {
    static Integer pumps_count,queue_size,cars_size;
    static String[] car_names;
    public static void take_inputs(){
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the number of pumps: ");
        pumps_count = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter the number of waiting area size: ");
        queue_size = Integer.parseInt(scanner.nextLine());
        System.out.print("Cars arriving (order): ");
        String input = scanner.nextLine();

        car_names = input.replaceAll(" ", "").split(","); 
        cars_size = car_names.length;
    }
    public static void main(String[] args) {
        take_inputs();
        BoundedBuffer<Car> buffer = new BoundedBuffer<>(queue_size);
        Semaphore bays = new Semaphore(pumps_count);
        for (int i = 1; i <= pumps_count; i++) {
            new Pump(i, buffer, bays).start();
        }

        for (int i = 0; i < cars_size; i++) {
            new Car(i,car_names[i], buffer).start();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
        }
    }
}

class Semaphore
{
    private Integer value;

    public Semaphore(int initial)
    {
        this.value = initial;
    }
    
    public synchronized void demand()
    {
        value--;
        if(value < 0){
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
    }

    public synchronized void release()
    {
        value++;
        if (value <= 0) {
            notify(); // wake one waiting thread
        }
    }
}


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
        System.out.println("Car " + this.name + " arrived");
        buffer.produce(this);
    }

    public String get_car_name() {
        return this.name;
    }
}

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
    public void run() 
    {
        while (true) 
        {
            Car car = buffer.consume();
            System.out.println("Pump " + id + ": Car " + car.get_car_name() + " taken for service " + "(Queue size: " + buffer.get_count() + ")");

            bays.demand(); // attempt to occupy a bay
            System.out.println("Pump " + id + ": Car " + car.get_car_name() + " begins service");
            try {
                Thread.sleep(1500); // service time
            } catch (InterruptedException e) {

            }

            System.out.println("Pump " + id + ": Car " + car.get_car_name() + " finished service");
            System.out.println("Pump " + id + ": Bay released");
            bays.release(); // allow another pump to occupy the bay
        }
    }
}

class BoundedBuffer<T> {
    private final Object[] buffer;
    private int in = 0;
    private int out = 0;
    private int count = 0;
    private final int size;

    private final Semaphore empty;
    private final Semaphore full;
    private final Semaphore mutex;

    public BoundedBuffer(int size) {
        this.size = size;
        buffer = new Object[size];
        empty = new Semaphore(size);
        full = new Semaphore(0);
        mutex = new Semaphore(1);
    }

    // Insert an item into the buffer
    public void produce(T item) {
        empty.demand();
        mutex.demand();

        buffer[in] = item;
        count++;
        System.out.println("Car " + ((Car) item).get_car_name() + " entered the queue (Queue size: " + count  + ")");
        in = (in + 1) % size;

        mutex.release();
        full.release();
    }

    @SuppressWarnings("unchecked")
    public T consume() {
        full.demand();
        mutex.demand();
        
        T item = (T) buffer[out];
        out = (out + 1) % size;
        count--;

        mutex.release();
        empty.release();
        return item;
    }
    public int get_count(){
        return this.count;
    }
}