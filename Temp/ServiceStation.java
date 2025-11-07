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
 * & "D:\Downloads\java\openjdk-25+36_windows-x64_bin\jdk-25\bin\javac.exe" "--module-path" "C:\javafx-sdk-25.0.1\lib" "--add-modules" "javafx.controls,javafx.fxml" "ServiceStation.java"
 * Run Command
 * & "D:\Downloads\java\openjdk-25+36_windows-x64_bin\jdk-25\bin\java.exe" "--enable-native-access=javafx.graphics" "--module-path" "C:\javafx-sdk-25.0.1\lib" "--add-modules" "javafx.controls,javafx.fxml" "ServiceStation"
 */

import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.*;
import javafx.stage.*;
import javafx.application.Platform;
import javafx.util.Duration;

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
            // checkPaused();
            empty.waitS(); // wait until there is space to add car
            // checkPaused();
            mutex.waitS(); // if the queue (Waiting Area) being in use wait else take mutex
            
            queue.add(ID);
            gui.log(ID + " entered waiting area");
            gui.updateWaitingArea(new ArrayList<>(queue));
            Thread.sleep(2000);

            // checkPaused();
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
                // checkPaused();
                full.waitS();  // wait until there is cars waiting
                pumps.waitS(); // wait until ther is a free pump
                mutex.waitS(); // if the queue (Waiting Area) being in use wait else take mutex

                String car = queue.poll();
                gui.log("Pump " + (pumpID + 1) + ": " + car + " begins service");
                gui.updateWaitingArea(new ArrayList<>(queue));
                gui.updatePumpStatus(pumpID, car, true);

                mutex.signal(); // let go of mutex so other threads can work on the queue
                empty.signal(); // signal that there is one more space free in waiting area

                // checkPaused();
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


class CarWashGUI extends VBox //Pane
{
    private Pane animationPane; // where cars move
    private GridPane waitingGrid;               // visual grid (slots inside a ScrollPane)
    private ScrollPane waitingScroll;
    private HBox pumpBox;
    private TextArea logArea;

    // private List<ImageView> waitingSpots = new ArrayList<>();
    private Map<String, ImageView> carMap = new HashMap<>();
    private List<ImageView> pumpImages = new ArrayList<>();
    private List<StackPane> waitingSlots = new ArrayList<>();
    private final Set<String> currentWaitingCars = Collections.synchronizedSet(new HashSet<>());

    // Maps slot index → carId (or null)
    private final List<String> waitingSlotAssignments = new ArrayList<>();

    private int waitingCapacity;
    private int pumpCount;
    
    private final Image carImage;
    private final Image pumpIdle;
    private final Image pumpActive;
    private final Image slotImage;
    private final Image backgroundImage; // optional

    private final int SLOTS_PER_ROW = 3;
    private final int VISIBLE_ROWS = 5;     // 3 x 5 = 15 visible slots
    private final double SLOT_W = 120;
    private final double SLOT_H = 70;

    public CarWashGUI(int waitingCapacity, int pumpCount)
    {
        this.waitingCapacity = waitingCapacity;
        this.pumpCount = pumpCount;

        // load images from classpath (GUI/Images/)
        carImage = new Image(getClass().getResource("Images/car.png").toExternalForm(), 60, 30, true, true);
        pumpIdle = new Image(getClass().getResource("Images/pump_idle.png").toExternalForm(), 100, 100, true, true);
        pumpActive = new Image(getClass().getResource("Images/pump_active.png").toExternalForm(), 100, 100, true, true);
        slotImage = new Image(getClass().getResource("Images/slot.png").toExternalForm(), (int)SLOT_W, (int)SLOT_H, true, true);
        Image tmpBg;
        try 
        {
            tmpBg = new Image(getClass().getResource("Images/background.png").toExternalForm());
        } 
        catch (Exception ex) 
        {
            tmpBg = null;
        }
        backgroundImage = tmpBg;

        setSpacing(12);
        setPadding(new Insets(12));
        setAlignment(Pos.TOP_CENTER);

        // ----------- Title
        Label title = new Label("🚗 Car Wash Simulation");
        title.setFont(Font.font("Arial", 22));
        title.setTextFill(Color.DARKBLUE);

        // -------- animationPane: base layer for moving cars + background
        animationPane = new Pane();
        animationPane.setPrefSize(760, 280);
        animationPane.setStyle("-fx-border-color: #90CAF9; -fx-border-width: 2; -fx-background-radius: 6;");
        if (backgroundImage != null) 
        {
            BackgroundImage bimg = new BackgroundImage(backgroundImage, BackgroundRepeat.REPEAT,
                                         BackgroundRepeat.REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT);
            animationPane.setBackground(new Background(bimg));
        } 
        else 
        {
            animationPane.setBackground(
                new Background(new BackgroundFill(Color.web("#E3F2FD"), null, null)));
        }

        // ---------- Visual Waiting Area (slots)
        waitingGrid = new GridPane();
        waitingGrid.setHgap(12);
        waitingGrid.setVgap(12);
        waitingGrid.setPadding(new Insets(8));
        waitingGrid.setAlignment(Pos.CENTER);

        // create all slot nodes (we will add them to the grid)
        createWaitingSlots(waitingCapacity);

        waitingScroll = new ScrollPane(waitingGrid);
        waitingScroll.setPrefViewportWidth(SLOTS_PER_ROW * (SLOT_W + 12) + 20);
        // compute visible height for VISIBLE_ROWS rows
        double visibleHeight = VISIBLE_ROWS * (SLOT_H + 12) + 30;
        waitingScroll.setPrefViewportHeight(Math.min(visibleHeight, waitingCapacity * (SLOT_H + 12) + 30));
        waitingScroll.setFitToWidth(true);
        waitingScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        waitingScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox waitingBox = new VBox(6, new Label("Waiting Area (slots)"), waitingScroll);
        waitingBox.setAlignment(Pos.CENTER);

        // -------- pump area
        pumpBox = new HBox(30);
        pumpBox.setAlignment(Pos.CENTER);
        createPumps(pumpCount);

        VBox pumpBoxWithLabel = new VBox(6, new Label("Service Bays"), pumpBox);
        pumpBoxWithLabel.setAlignment(Pos.CENTER);

        // -------- log
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(120);

        // -------- back button only
        Button backButton = new Button("⬅ Back to Menu");
        backButton.setPrefWidth(180);
        backButton.setStyle(
                "-fx-font-size: 13px; -fx-background-color: #E91E63; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 8px;");
        backButton.setOnAction(e -> Platform.runLater(ServiceStation::showMainMenuAgain));

        HBox controls = new HBox(backButton);
        controls.setAlignment(Pos.CENTER);

        StackPane waitingVisualPane = new StackPane(waitingGrid,animationPane);
        waitingVisualPane.setPrefHeight(300);

        getChildren().addAll(title, waitingVisualPane, pumpBoxWithLabel, logArea, controls);
    }

    // create waiting slot nodes and populate grid
    private void createWaitingSlots(int capacity) {
        waitingSlots.clear();
        waitingGrid.getChildren().clear();

        // int rows = (int) Math.ceil((double) capacity / SLOTS_PER_ROW);
        for (int i = 0; i < capacity; i++) 
        {
            StackPane slot = new StackPane();
            slot.setPrefSize(SLOT_W, SLOT_H);

            ImageView slotBg = new ImageView(slotImage);
            slotBg.setFitWidth(SLOT_W);
            slotBg.setFitHeight(SLOT_H);

            Label label = new Label("Empty");
            label.setFont(Font.font(12));
            label.setTextFill(Color.GRAY);

            slot.getChildren().addAll(slotBg, label);
            waitingSlots.add(slot);
            waitingSlotAssignments.add(null); // initialize as empty

            int r = i / SLOTS_PER_ROW;
            int c = i % SLOTS_PER_ROW;
            waitingGrid.add(slot, c, r);
        }
    }

    private void createPumps(int count) {
        pumpBox.getChildren().clear();
        pumpImages.clear();
        
        for (int i = 0; i < count; i++) {
            ImageView p = new ImageView(pumpIdle);
            p.setFitWidth(100);
            p.setFitHeight(100);
            pumpImages.add(p);
            pumpBox.getChildren().add(p);
        }
    }

    // ---------- Logging
    public void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    // ---------- Update Pump Status

    // called by Pump threads to toggle pump's visual and move car
    // occupied==true means car moves to pump and pump becomes active
    public void updatePumpStatus(int pumpIndex, String carId, boolean occupied) {
        Platform.runLater(
            () -> {
                ImageView pumpView = pumpImages.get(pumpIndex);
                if (occupied) {
                    // pumpView.setImage(pumpActive);
                    freeWatingSlot(carId);
                    moveCarToPump(carId, pumpIndex);
                } else {
                    pumpView.setImage(pumpIdle);
                    moveCarOut(carId);
                }
            }
        );
    }
    
    private void freeWatingSlot(String carId)
    {
        int index = waitingSlotAssignments.indexOf(carId);
        if (index != -1)
        {
            waitingSlotAssignments.set(index, null);
        }
    }

    // update waiting area labels and animate new cars into the grid
    public void updateWaitingArea(List<String> cars) {
        Platform.runLater(
            () -> {
                // Set<String> newWaitingCars = new HashSet<>(cars);

                // Remove cars that left the waiting area (now at pump)               
                for (int i = 0; i < waitingSlotAssignments.size(); i++) 
                {
                    String carAtSlot = waitingSlotAssignments.get(i);
                    if (carAtSlot != null && !cars.contains(carAtSlot)) 
                    {
                        // car has left the waiting area
                        waitingSlotAssignments.set(i, null);
                    }
                }

                // Add new cars to empty slots
                for (String carID : cars) 
                {
                    boolean alreadyAssigned = waitingSlotAssignments.contains(carID);
                    if (!alreadyAssigned) 
                    {
                        // find first empty slot
                        for (int i = 0; i < waitingSlotAssignments.size(); i++) 
                        {
                            if (waitingSlotAssignments.get(i) == null) 
                            {
                                waitingSlotAssignments.set(i, carID);
                                moveCarToWaiting(carID, i);
                                break;
                            }
                        }
                    }
                }

                // Update slot labels
                for (int i = 0; i < waitingSlots.size(); i++) 
                {
                    StackPane slot = waitingSlots.get(i);
                    Label label = (Label) slot.getChildren().get(1);
                    String carAtSlot = waitingSlotAssignments.get(i);

                    if(carAtSlot != null) 
                    {
                        label.setText(carAtSlot);
                        label.setTextFill(Color.BLACK);
                    } 
                    else 
                    {
                        label.setText("Empty");
                        label.setTextFill(Color.GRAY);
                    }
                }

                // ✅ make sure all cars are drawn above slots
                for (ImageView carView : carMap.values()) {
                    carView.toFront();
                }
            }
        );
    }

    // ================= Animation helpers =================

    // move a newly arrived car onto its slot index
    private void moveCarToWaiting(String carId, int slotIndex) 
    {
        if (carMap.containsKey(carId)) 
        {
            // already exists — just return
            return;
        }
        // create view
        ImageView carView = new ImageView(carImage);
        carView.setFitWidth(60);
        carView.setFitHeight(30);

        // small random tint for variety
        carView.setEffect(new ColorAdjust(0, 0, Math.random() * 0.35 - 0.175, 0));

        // place off-screen left as start
        carView.setLayoutX(-120);
        carView.setLayoutY(animationPane.getHeight() / 2.0 - 20);

        // put into maps immediately so updateWaitingArea won't create a duplicate
        carMap.put(carId, carView);
        currentWaitingCars.add(carId);  // mark as present in waiting area

        // Add to pane and animate to slot after layout pass
        Platform.runLater(() -> {
            animationPane.getChildren().add(carView);
            carView.toFront();

            // ensure layout pass to have correct slot coordinates
            animationPane.applyCss();
            animationPane.layout();

            StackPane slot = waitingSlots.get(slotIndex);

            // compute center of slot in animationPane coordinates
            Point2D slotCenterScene = slot.localToScene(slot.getWidth() / 2.0, slot.getHeight() / 2.0);
            Point2D target = animationPane.sceneToLocal(slotCenterScene);

            // reset any previous translate so we compute absolute translation
            carView.setTranslateX(0);
            carView.setTranslateY(0);

            double translateX = target.getX() - carView.getLayoutX();
            double translateY = target.getY() - carView.getLayoutY();

            TranslateTransition tt = new TranslateTransition(Duration.seconds(1.2), carView);
            // use setToX/Y relative to current translate (we reset it to 0)
            tt.setToX(translateX);
            tt.setToY(translateY);

            tt.setOnFinished(ev -> {
                // snap to exact slot coords and clear translate to avoid accumulation
                carView.setLayoutX(target.getX());
                carView.setLayoutY(target.getY());
                carView.setTranslateX(0);
                carView.setTranslateY(0);
                carView.toFront();
            });
            tt.play();
        });
    }
    
    // ---------- Animation: Move car to pump ----------d
    // move car to pump area (center in front of pump)
    private void moveCarToPump(String carId, int pumpIndex) 
    {
        ImageView carView = carMap.get(carId);
        if (carView == null) 
        {
            // if missing (rare), create and fade in near pump
            carView = new ImageView(carImage);
            carView.setFitWidth(60);
            carView.setFitHeight(30);
            carMap.put(carId, carView);
            animationPane.getChildren().add(carView);
            carView.toFront();
        }

        // make a final copy for lambda use
        final ImageView carFinal = carView;

        Platform.runLater(() -> {
            ImageView pumpView = pumpImages.get(pumpIndex);
            Point2D pumpCenterScene = pumpView.localToScene(
                    pumpView.getBoundsInLocal().getWidth() / 2.0,
                    pumpView.getBoundsInLocal().getHeight() / 2.0
            );
            Point2D pumpCenterInAnim = animationPane.sceneToLocal(pumpCenterScene);

            // choose a target slightly in front of pump (lower Y)
            double targetX = pumpCenterInAnim.getX();
            double targetY = pumpCenterInAnim.getY() + 10; // adjust to appear in front of pump

            double dx = targetX - carFinal.getLayoutX();
            double dy = targetY - carFinal.getLayoutY();

            TranslateTransition tt = new TranslateTransition(Duration.seconds(1.5), carFinal);
            tt.setByX(dx);
            tt.setByY(dy);
            tt.setOnFinished(ev -> {
                // snap to pump spot
                carFinal.setLayoutX(targetX);
                carFinal.setLayoutY(targetY);
                carFinal.setTranslateX(0);
                carFinal.setTranslateY(0);

                pumpView.setImage(pumpActive);
            });
            tt.play();
        });
    }

    // animate car leaving
    private void moveCarOut(String carId) 
    {
        ImageView carView = carMap.remove(carId);
        if (carView == null) return;

        Platform.runLater(() -> {
            // move to right off-screen (x = animationPane.width + 200)
            double targetX = animationPane.getWidth() + 300;
            double dx = targetX - carView.getLayoutX();
            double dy = -60; // a little upward as it leaves

            TranslateTransition tt = new TranslateTransition(Duration.seconds(2.0), carView);
            tt.setByX(dx);
            tt.setByY(dy);
            tt.setOnFinished(ev -> animationPane.getChildren().remove(carView));
            // carView.toFront();
            tt.play();
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
            for (int i = 0; i < 10; i++) 
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