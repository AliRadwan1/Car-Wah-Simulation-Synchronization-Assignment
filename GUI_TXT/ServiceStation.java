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

/*
 * Compile command
 * & "D:\Downloads\java\openjdk-25+36_windows-x64_bin\jdk-25\bin\javac.exe" "--module-path" "C:\javafx-sdk-25.0.1\lib" "--add-modules" "javafx.controls,javafx.fxml" "ServiceStation.java"
 * Run Command
 * & "D:\Downloads\java\openjdk-25+36_windows-x64_bin\jdk-25\bin\java.exe" "--enable-native-access=javafx.graphics" "--module-path" "C:\javafx-sdk-25.0.1\lib" "--add-modules" "javafx.controls,javafx.fxml" "ServiceStation"
 * 
 */

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.animation.FadeTransition;
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

    private void checkPaused() {
        synchronized (gui) {
            while (gui.isPaused()) {
                try {
                    gui.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public void run()
    {
        try {
            checkPaused();
            empty.waitS(); // wait until there is space to add car
            checkPaused();
            mutex.waitS(); // if the queue (Waiting Area) being in use wait else take mutex
            
            queue.add(ID);
            gui.log(ID + " entered waiting area");
            gui.updateWaitingArea(new ArrayList<>(queue));
            Thread.sleep(1500);

            checkPaused();
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

    private void checkPaused() {
        synchronized (gui) {
            while (gui.isPaused()) {
                try {
                    gui.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public void run()
    {
        while (true) {
            try 
            {
                checkPaused();
                full.waitS();  // wait until there is cars waiting
                pumps.waitS(); // wait until ther is a free pump
                mutex.waitS(); // if the queue (Waiting Area) being in use wait else take mutex

                String car = queue.poll();
                gui.log("Pump " + (pumpID + 1) + ": " + car + " begins service");
                gui.updateWaitingArea(new ArrayList<>(queue));
                gui.updatePumpStatus(pumpID, car, true);

                mutex.signal(); // let go of mutex so other threads can work on the queue
                empty.signal(); // signal that there is one more space free in waiting area

                checkPaused();
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

    private List<Label> waitingLabels = new ArrayList<>();
    private List<Label> pumpLabels = new ArrayList<>();

    private int waitingCapacity;
    private int pumpCount;

    private Button pauseButton;
    private volatile boolean paused = false;

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }
    
    public CarWashGUI(int waitingCapacity, int pumpCount)
    {
        this.waitingCapacity = waitingCapacity;
        this.pumpCount = pumpCount;

        setSpacing(10);
        setPadding(new Insets(10));

        // ----------- Title
        Label title = new Label("🚗 Car Wash Simulation");
        title.setFont(Font.font("Arial", 26));
        title.setTextFill(Color.DARKBLUE);

        // ----------- Waiting Area
        waitingAreaBox = new VBox(5);
        waitingAreaBox.setPadding(new Insets(10));
        waitingAreaBox.setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(2))));
        Label waitLabel = new Label("Waiting Area:");
        waitLabel.setFont(Font.font(16));
        VBox waitLayout = new VBox(waitLabel, waitingAreaBox);
        waitLayout.setSpacing(5);
        initWaitingArea();

        // ----------- Pumps Section
        pumpBox = new HBox(10);
        pumpBox.setPadding(new Insets(10));
        pumpBox.setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(2))));
        Label pumpLabel = new Label("Service Bays:");
        pumpLabel.setFont(Font.font(16));
        VBox pumpLayout = new VBox(pumpLabel, pumpBox);
        pumpLayout.setSpacing(5);
        initPumps();

        // ----------- Log Section
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(200);

        // ----------- Pause Button
        pauseButton = new Button("⏸ Pause");
        pauseButton.setPrefWidth(200);
        pauseButton.setStyle(
            "-fx-font-size: 14px; " +
            "-fx-background-color: #FF9800; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8px;"
        );
        pauseButton.setOnAction(
            e -> 
            {
            if (!paused) 
            {
                paused = true;
                pauseButton.setText("▶ Resume");
                log("Simulation paused...");
            } 
            else 
            {
                paused = false;
                synchronized (this) { notifyAll(); } // wake all waiting threads
                pauseButton.setText("⏸ Pause");
                log("Simulation resumed...");
            }
        });

        Button backButton = new Button("⬅ Back to Menu");
        backButton.setPrefWidth(200);
        backButton.setStyle(
            "-fx-font-size: 14px; " +
            "-fx-background-color: #E91E63; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8px;"
        );
        backButton.setOnAction(
            e -> 
            {
                Platform.runLater(
                    () -> 
                    {
                        ((Stage) getScene().getWindow()).close(); // close current sim window
                        try 
                        {
                            new ServiceStation().start(new Stage()); // reopen start menu
                        } 
                        catch (Exception ex) 
                        {
                            ex.printStackTrace();
                        }
                    }
                );
            }
        );

        // getChildren().add(backButton);


        getChildren().addAll(title, waitLayout, pumpLayout, logArea, pauseButton,backButton);
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
    private Stage mainStage;

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

    public static void showMainMenuAgain() {
        Platform.runLater(() -> {
            try {
                new ServiceStation().start(new Stage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void start(Stage stage)
    {
        // default values are Waiting_capcity (5) & Pump number (3)
        
        this.mainStage = stage;
        // ------------ Start Menu
        VBox menuLayout = new VBox(15);
        menuLayout.setPadding(new Insets(30));
        menuLayout.setStyle("-fx-background-color: linear-gradient(to bottom right, #2196F3, #64B5F6);");

        Label title = new Label("🚗 Car Wash Simulation");
        title.setFont(Font.font("Arial", 28));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("AI Section 5 — Team Project");
        subtitle.setTextFill(Color.WHITE);
        subtitle.setFont(Font.font("Arial", 14));

        // ------------- Input Fields
        TextField waitingField = new TextField();
        waitingField.setPromptText("Enter waiting area capacity (default 5)");

        TextField pumpField = new TextField();
        pumpField.setPromptText("Enter number of pumbs (default 3)");

        // ------------- Start Button
        Button startBtn = new Button("▶ Start Simulation");
        startBtn.setStyle(
            "-fx-background-color: #ffffff; -fx-text-fill: #2196F3; -fx-font-size: 16px; " +
            "-fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 10px 20px;"
        );
        startBtn.setOnAction(
                e ->{
                int waitingCapacity = 5;
                int pumpCount = 3;

                try {
                    if (!waitingField.getText().trim().isEmpty())
                        waitingCapacity = Math.max(1, Integer.parseInt(waitingField.getText().trim()));
                    if (!pumpField.getText().trim().isEmpty())
                        pumpCount = Math.max(1, Integer.parseInt(pumpField.getText().trim()));
                } catch (NumberFormatException ex) {
                    showAlert("Invalid input! Using default values (waiting=5, pumps=3).");
                }

                startSimulation(waitingCapacity, pumpCount);
            }
        );  
        
        // ------------- Exit Button
        Button exitBtn = new Button("Exit");
        exitBtn.setStyle(
            "-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8px;"
        );
        exitBtn.setOnAction(e -> Platform.exit());

        // ------------- Credits Section
        Label credits = new Label(
            "Created by:\n" +
            "Ali Radwan • Adel Hefny • Ziad Salama • Mohamed Mahmoud • Asser Ahmed"
        );
        credits.setFont(Font.font("Arial", 13));
        credits.setTextFill(Color.WHITE);
        credits.setAlignment(Pos.CENTER);
        credits.setStyle("-fx-opacity: 0.9;");

        // ---- Fade-In Animation for Credits ----
        FadeTransition fadeIn = new FadeTransition(javafx.util.Duration.seconds(2.5), credits);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setCycleCount(javafx.animation.Animation.INDEFINITE);
        fadeIn.setAutoReverse(true);
        fadeIn.play();

        Separator line = new Separator();
        line.setStyle("-fx-background-color: white; -fx-opacity: 0.3;");
        line.setPrefWidth(400);

        // ---- Add all elements neatly ----
        menuLayout.getChildren().addAll(
            title,
            subtitle,
            waitingField,
            pumpField,
            startBtn,
            exitBtn,
            line,
            credits
        );
        menuLayout.setAlignment(Pos.CENTER);

        Scene menuScene = new Scene(menuLayout, 800, 600);
        stage.setTitle("Car Wash Simulation");
        stage.setScene(menuScene);
        stage.show();
    }

    private void runSimulation(int waitingCapacity, int pumpCount)
    {
        try 
        {
            Thread.sleep(500); // slight delay for GUI render
            Semaphore mutex = new Semaphore(1);
            Semaphore empty = new Semaphore(waitingCapacity);
            Semaphore full = new Semaphore(0);
            Semaphore pumps = new Semaphore(pumpCount);

            Queue<String> queue = new LinkedList<>();
            
            // Start Pump Threads
            for (int i = 0; i < pumpCount; i++) 
            {
                new Pump(i, gui, queue, mutex, empty, full, pumps).start();
            }

            // Start Car Threads
            for (int i = 0; i < 30; i++) 
            {
                new Car("C" + i, gui, queue, mutex, empty, full).start();
                Thread.sleep(1500);
            }
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    private void startSimulation(int waitingCapacity, int pumpCount)
    {
        // Create the simulation GUI inside the SAME stage
        gui = new CarWashGUI(waitingCapacity, pumpCount);

        Scene simScene = new Scene(gui, 800, 600);
        mainStage.setScene(simScene);

        new Thread(() -> runSimulation(waitingCapacity, pumpCount)).start();
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}