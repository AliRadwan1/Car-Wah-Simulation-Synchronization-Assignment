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

/*
 * Compile command
 * "D:\Downloads\java\openjdk-25+36_windows-x64_bin\jdk-25\bin\javac.exe" "--module-path" "C:\javafx-sdk-25.0.1\lib" "--add-modules" "javafx.controls,javafx.fxml" "ServiceStation.java"
 * Run Command
 * "D:\Downloads\java\openjdk-25+36_windows-x64_bin\jdk-25\bin\java.exe" "--enable-native-access=javafx.graphics" "--module-path" "C:\javafx-sdk-25.0.1\lib" "--add-modules" "javafx.controls,javafx.fxml" "ServiceStation"
 * 
 */

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.application.*;
import javafx.stage.*;

import java.util.*;



class Car extends Thread
{
    private String ID;
    private CarWashGUI gui;
    private Queue<String> queue;
    private Semaphore mutex, empty, full;

    public Car(String ID, CarWashGUI gui, Queue<String> queue,
                Semaphore mutex, Semaphore empty, Semaphore full)
    {
        this.ID = ID;
        this.gui = gui;
        this.queue = queue;
        this.mutex = mutex;
        this.empty = empty;
        this.full = full;
    }

    public void run()
    {
        try {
            
            // gui.log(ID + " arrived");
            empty.waitS();
            mutex.waitS();
            
            queue.add(ID);
            gui.log(ID + " entered waiting area");
            gui.updateWaitingArea(new ArrayList<>(queue));
            Thread.sleep(1500);
            
            full.signal();
            mutex.signal();
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
            
    }
}

class Pump extends Thread
{
    private int pumpID;
    private CarWashGUI gui;
    private Queue<String> queue;
    private Semaphore mutex, empty, full, pumps;

    public Pump(int pumpID, CarWashGUI gui, Queue<String> queue,
                Semaphore mutex, Semaphore empty, Semaphore full, Semaphore pumps)
    {
        this.pumpID = pumpID;
        this.gui = gui;
        this.queue = queue;
        this.mutex = mutex;
        this.empty = empty;
        this.full = full;
        this.pumps = pumps;
    }

    public void run()
    {
        while (true) {
            try 
            {
                full.waitS();  // wait until there is cars waiting
                pumps.waitS(); // wait until there is a free pump
                mutex.waitS(); // if the queue being in use wait else take the mutex

                String car = queue.poll();
                gui.log("Pump " + (pumpID + 1) + ": " + car + " begins service");
                gui.updateWaitingArea(new ArrayList<>(queue));
                gui.updatePumpStatus(pumpID, car, true);
                

                mutex.signal(); // let go of mutex so other threads can work on the queue
                empty.signal(); // signal that there is one more space free in waiting area

                Thread.sleep(5000); // simulate service duration
                gui.log("Pump " + (pumpID + 1) + ": " + car + " finishes service");
                gui.updatePumpStatus(pumpID, car, false);
                

                pumps.signal();
            } 
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }
}

class Semaphore
{
    private int value;

    public Semaphore(int value)
    {
        this.value = value;
    }
    
    public synchronized void waitS()
    {
        while (value == 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                
                Thread.currentThread().interrupt();
                return;
            }
        }
        value--;

    }

    public synchronized void signal()
    {
        value++;
        notify();
    }
}


class CarWashGUI extends VBox
{
    private VBox waitingAreaBox;
    private HBox pumpBox;
    private TextArea logArea;
    private Button startButton;

    private List<Label> waitingLabels = new ArrayList<>();
    private List<Label> pumpLabels = new ArrayList<>();

    private int waitingCapacity;
    private int pumpCount;

    // Callback for when "Start Simulation" button is pressed
    private Runnable onStartClicked;

    public void setOnStartClicked(Runnable action) {
        this.onStartClicked = action;
    }

    public CarWashGUI(int waitingCapacity, int pumpCount)
    {
        this.waitingCapacity = waitingCapacity;
        this.pumpCount = pumpCount;

        setSpacing(10);
        setPadding(new Insets(10));

        // ----------- Title
        Label title = new Label("🚗 Car Wash Simulation");
        title.setFont(Font.font(22));
        getChildren().add(title);

        // ----------- Waiting Area
        waitingAreaBox = new VBox(5);
        waitingAreaBox.setPadding(new Insets(10));
        waitingAreaBox.setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(2))));
        Label waitLabel = new Label("Waiting Area:");
        waitLabel.setFont(Font.font(16));
        VBox waitLayout = new VBox(waitLabel, waitingAreaBox);
        waitLayout.setSpacing(5);
        getChildren().add(waitLayout);
        initWaitingArea();

        // ----------- Pumps Section
        pumpBox = new HBox(10);
        pumpBox.setPadding(new Insets(10));
        pumpBox.setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(2))));
        Label pumpLabel = new Label("Service Bays:");
        pumpLabel.setFont(Font.font(16));
        VBox pumpLayout = new VBox(pumpLabel, pumpBox);
        pumpLayout.setSpacing(5);
        getChildren().add(pumpLayout);
        initPumps();

        // ----------- Log Section
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(200);
        getChildren().add(logArea);

        // ----------- Start Button
        startButton = new Button("▶ Start Simulation");
        startButton.setPrefWidth(200);
        startButton.setStyle(
            "-fx-font-size: 14px; " +
            "-fx-background-color: #2196F3; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8px;"
        );
        startButton.setOnAction(e -> {
            if (onStartClicked != null) {
                onStartClicked.run();
            }
            startButton.setDisable(true); // prevent double start
            log("Simulation started...");
        });
        getChildren().add(startButton);

        // ----------- Create Scene & Show Stage
        Scene scene = new Scene(this, 800, 600);
        Stage stage = new Stage();
        stage.setTitle("Car Wash GUI");
        stage.setScene(scene);
        stage.show();
    }

    // -------- Initialize Waiting Area --------
    private void initWaitingArea() {
        for (int i = 0; i < waitingCapacity; i++) {
            Label carLabel = new Label("Empty");
            carLabel.setPrefSize(120, 30);
            carLabel.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
            carLabel.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(1))));
            waitingAreaBox.getChildren().add(carLabel);
            waitingLabels.add(carLabel);
        }
    }

     // -------- Initialize Pumps --------
    private void initPumps() {
        for (int i = 0; i < pumpCount; i++) {
            Label pumpLabel = new Label("Pump " + (i + 1) + ": FREE");
            pumpLabel.setPrefSize(150, 50);
            pumpLabel.setBackground(new Background(new BackgroundFill(Color.LIGHTGRAY, null, null)));
            pumpLabel.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(1))));
            pumpBox.getChildren().add(pumpLabel);
            pumpLabels.add(pumpLabel);
        }
    }

    // -------- Log Updates --------
    public void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    // -------- Update Pump Status --------
    public void updatePumpStatus(int pumpIndex, String carId, boolean occupied) {
        Platform.runLater(() -> {
            Label pumpLabel = pumpLabels.get(pumpIndex);
            if (occupied) {
                pumpLabel.setText("Pump " + (pumpIndex + 1) + ": " + carId);
                pumpLabel.setBackground(new Background(new BackgroundFill(Color.LIGHTGREEN, null, null)));
            } else {
                pumpLabel.setText("Pump " + (pumpIndex + 1) + ": FREE");
                pumpLabel.setBackground(new Background(new BackgroundFill(Color.LIGHTGRAY, null, null)));
            }
        });
    }

    // -------- Update Waiting Area --------
    public void updateWaitingArea(List<String> cars) {
        Platform.runLater(() -> {
            for (int i = 0; i < waitingLabels.size(); i++) {
                Label label = waitingLabels.get(i);
                if (i < cars.size()) {
                    label.setText(cars.get(i));
                    label.setBackground(new Background(new BackgroundFill(Color.LIGHTYELLOW, null, null)));
                } else {
                    label.setText("Empty");
                    label.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
                }
            }
        });
    }



}

public class ServiceStation extends Application
{
    private static CarWashGUI gui;

    public ServiceStation()
    {
        super();
    }
    
    // ----------- Alert Helper -----------
    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Notice");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    @Override
    public void start(Stage stage)
    {
        // default values
        int waitingCapacity = 5;
        int pumpCount = 3;
        
        // --------- Prompt 1: Waiting Area Capacity
        TextInputDialog waitDialog = new TextInputDialog(String.valueOf(waitingCapacity));
        waitDialog.setTitle("Car Wash Setup");
        waitDialog.setHeaderText("Enter Waiting Area Capacity");
        waitDialog.setContentText("Number Cars That Can Wait:");

        Optional<String> waitResutl = waitDialog.showAndWait();
        if (waitResutl.isPresent() && !waitResutl.get().trim().isEmpty()) 
        {
          try 
          {
            waitingCapacity = Integer.parseInt(waitResutl.get().trim());
            if (waitingCapacity < 1) 
            {
                showAlert("Waiting capacity must be at least 1. Using defualt (5).");
                waitingCapacity = 5;
            }
          } 
          catch (NumberFormatException e) 
          {
            showAlert("Invalid input! Using default waiting capacity (5).");
            waitingCapacity = 5;
          }
        }

        // --------- Prompt 2: Number of pumps
        TextInputDialog pumpDialog = new TextInputDialog(String.valueOf(pumpCount));
        pumpDialog.setTitle("Car Wash Setup");
        pumpDialog.setHeaderText("Enter Number Of Pumps");
        pumpDialog.setContentText("Number of service bays:");

        Optional<String> pumbResult = pumpDialog.showAndWait();

        if (pumbResult.isPresent() && !pumbResult.get().trim().isEmpty())
        {
            try 
            {
                pumpCount = Integer.parseInt(pumbResult.get().trim());
                if (pumpCount < 1) 
                {
                    showAlert("Pump count must be at least 1. Using default (3).");
                    pumpCount = 3;
                }
            }
            catch (NumberFormatException e) 
            {
                showAlert("Invalid input! Using default pump count (3).");
                pumpCount = 3;
            }
        }

        // ----------- Create GUI and Start Simulation
        int finalWaitingCapacity = waitingCapacity;
        int finalPumpCount = pumpCount;

        gui = new CarWashGUI(finalWaitingCapacity, finalPumpCount);

        new Thread(() -> runSimulation(finalWaitingCapacity, finalPumpCount)).start();
    }


    private void runSimulation(int waitingCapacity, int pumpCount)
    {
        try 
        {
            Semaphore mutex = new Semaphore(1);
            Semaphore empty = new Semaphore(waitingCapacity);
            Semaphore full = new Semaphore(0);
            Semaphore pumps = new Semaphore(pumpCount);

            Queue<String> queue = new LinkedList<>();

            // Start Pump Thread
            for (int i = 0; i < pumpCount; i++) 
            {
                new Pump(i, gui, queue, mutex, empty, full, pumps).start();
            }

            // Start car Thread (30 Cars Total)
            for (int i = 0; i < 30; i++) 
            {
                new Car("C" + i, gui, queue, mutex, empty, full).start();
                Thread.sleep(1200); // Display between arrivals
            }
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}