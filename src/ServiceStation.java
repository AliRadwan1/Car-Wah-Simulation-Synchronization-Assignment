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
import java.util.*;
import java.lang.*;
public class ServiceStation {
    static Integer pumps_count,queue_size;
    public static void take_inputs(){
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the number of pumps: ");
        pumps_count = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter the number of waiting area size: ");
        queue_size = Integer.parseInt(scanner.nextLine());
    }
    public static void main(String[] args) {
        take_inputs();
    }
}

class BoundedBuffer<T> {
    private final Object[] buffer;
    private int in = 0;
    private int out = 0;
    private final int size;

    private final Semaphore empty;
    private final Semaphore full;
    private final Semaphore mutex;

    public BoundedBuffer(int size) {
 
    }

    // Insert an item into the buffer
    public void produce(T item) {

    }

    @SuppressWarnings("unchecked")
    public T consume() {

    }
}

class Pump implements Runnable {
    public void run() {

    }
}

class Car implements Runnable {
    public void run() {

    }
}

class Semaphore {
    private int value;
    public Semaphore(int value) {this.value = value;}
    public synchronized void waitSem() {}
    public synchronized void signalSem() {}
}