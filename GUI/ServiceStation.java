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
 * 
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
import javafx.util.Duration;

import java.util.*;

/*
 * ----------------------------
 * Car Class
 * ----------------------------
 * Represents a single car entering the car wash simulation.
 * Each car is implemented as a Thread because cars operate independently
 * and interact with semaphores (synchronization) to access the waiting area
 * and pumps.
 */
class Car extends Thread
{
    private String ID;                // Unique car identifier (e.g., C0, C1)
    private CarWashGUI gui;           // Reference to GUI to update visuals
    private Queue<String> queue;      // Shared queue representing waiting area
    private Semaphore mutex, empty, full; // Semaphores for synchronization

    /*
     * Constructor initializes the car with its ID, GUI reference,
     * shared queue, and semaphores.
     */
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

    /*
     * run() method defines car behavior.
     * Each car:
     * 1. Appears at entrance
     * 2. Waits for a free slot in the waiting area
     * 3. Moves to waiting area
     * 4. Signals that a car is available for service
     */
    public void run()
    {
        try {
            gui.spawnCarAtEntrance(ID); // Show car in GUI entrance
            gui.log(ID + " arrived, waiting for a free slot...");

            empty.waitS(); // Wait if waiting area is full
            mutex.waitS(); // Lock queue for exclusive access

            queue.add(ID); // Add car to waiting area
            gui.log(ID + " moving to waiting area");
            gui.updateWaitingArea(new ArrayList<>(queue)); // Update GUI
            Thread.sleep(2000); // Optional visual delay

            full.signal(); // Signal pumps that a car is available
            mutex.signal(); // Release queue lock
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
            
    }
}

/*
 * ----------------------------
 * Pump Class
 * ----------------------------
 * Represents a single pump/service bay in the car wash.
 * Pumps operate as independent threads and process cars from the waiting area.
 */
class Pump extends Thread
{
    private int pumpID;               // Index of this pump
    private CarWashGUI gui;           // GUI reference for visuals
    private Queue<String> queue;      // Shared waiting area queue
    private Semaphore mutex, empty, full, pumps; // Synchronization semaphores

    /*
     * Constructor initializes pump with its index, GUI, shared queue,
     * and all relevant semaphores.
     */
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

    /*
     * run() method continuously:
     * 1. Waits for a car to be available
     * 2. Waits for the pump to be free
     * 3. Locks the queue
     * 4. Picks a car and updates GUI
     * 5. Simulates service duration
     * 6. Releases the pump
     */
    public void run()
    {
        while (true) {
            try 
            {
                full.waitS();  // Wait until a car is available
                pumps.waitS(); // Wait until this pump is free
                mutex.waitS(); // Lock the queue

                String car = queue.poll(); // Take the first car
                gui.log("Pump " + (pumpID + 1) + ": " + car + " begins service");
                gui.updateWaitingArea(new ArrayList<>(queue)); // Update waiting slots
                gui.updatePumpStatus(pumpID, car, true);      // Update pump visual

                mutex.signal(); // Unlock the queue
                empty.signal(); // Signal one free space in waiting area

                Thread.sleep(5000); // Simulate washing duration
                gui.log("Pump " + (pumpID + 1) + ": " + car + " finishes service");
                gui.updatePumpStatus(pumpID, car, false);     // Update GUI

                pumps.signal(); // Release pump
            } 
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }
}

/*
 * ----------------------------
 * Semaphore Class
 * ----------------------------
 * Implements a simple counting semaphore to control access to shared resources.
 * waitS() = P() operation
 * signal() = V() operation
 */
class Semaphore
{
    private int value; // Current count

    public Semaphore(int value)
    {
        this.value = value;
    }
    
    // Wait operation: blocks thread if semaphore is 0
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

    // Signal operation: increments semaphore and notifies waiting threads
    public synchronized void signal()
    {
        value++;
        notify();
    }
}

/*
 * ----------------------------
 * CarWashGUI Class
 * ----------------------------
 * This class creates the graphical interface of the car wash simulation.
 * - Displays entrance area (animationPane)
 * - Waiting area slots (GridPane)
 * - Pumps/service bays (HBox)
 * - Logging area (TextArea)
 */
class CarWashGUI extends VBox //VBox for vertical stacking
{
    private Pane animationPane;                 // Pane where cars move
    private GridPane waitingGrid;               // Grid of waiting slots
    private ScrollPane waitingScroll;           // Scroll for waiting area
    private HBox pumpBox;                       // HBox containing pumps
    private TextArea logArea;                   // Text area for logging events

    private Map<String, ImageView> carMap = new HashMap<>(); // Map carID -> ImageView
    private List<ImageView> pumpImages = new ArrayList<>();  // Images for each pump
    private List<StackPane> waitingSlots = new ArrayList<>();// StackPane for slot visuals

    private final List<String> waitingSlotAssignments = new ArrayList<>(); // carID per slot

    private int waitingCapacity; // Total slots in waiting area
    private int pumpCount;       // Total pumps
    private final String fontPath = "/fonts/robotoslab.ttf"; // Custom font

    // Images
    private final Image carImage;
    private final Image pumpIdle;
    private final Image pumpActive;
    private final Image slotImage;
    private final Image backgroundImage; // Optional background

    private final int SLOTS_PER_ROW = 5;  // Max slots per row in waiting grid
    private final int VISIBLE_ROWS = 3;   // Number of rows visible at once
    private final double SLOT_W = 120;    // Width of a slot
    private final double SLOT_H = 70;     // Height of a slot

    /*
     * Constructor sets up GUI: loads images, creates waiting slots, pumps,
     * log area, and back button.
     */
    public CarWashGUI(int waitingCapacity, int pumpCount)
    {
        this.waitingCapacity = waitingCapacity;
        this.pumpCount = pumpCount;

        // Load car and pump images
        carImage = new Image(getClass().getResource("Images/car.png").toExternalForm(), 200, 30, false, false);
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

        setSpacing(12); // vertical spacing between components
        setPadding(new Insets(12));
        setAlignment(Pos.TOP_CENTER);

        // ----------- Title
        Label title = new Label("Car Wash Simulation");
        title.setFont(Font.loadFont(getClass().getResourceAsStream(fontPath), 24));

        // -------- animationPane: base layer for moving cars + background
        animationPane = new Pane();
        animationPane.setMaxSize(720, 160);
        animationPane.setStyle("-fx-border-color: #656565ff; -fx-border-width: 10 0 10 0; -fx-background-radius: 6;");
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

        Label pumpLabel = new Label("Service Bays");
        pumpLabel.setFont(Font.loadFont(getClass().getResourceAsStream(fontPath), 24));
        VBox pumpBoxWithLabel = new VBox(6, pumpLabel, pumpBox);
        pumpBoxWithLabel.setAlignment(Pos.CENTER);
        VBox.setMargin(pumpBoxWithLabel, new Insets(-40, 0, 0, 0));
        VBox.setMargin(pumpLabel, new Insets(0, 0, 10, 0));
        VBox.setMargin(title, new Insets(25, 0, -50, 0));

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

    // -----------------------
    // Creates waiting area slots as StackPanes with labels
    private void createWaitingSlots(int capacity) {
        waitingSlots.clear();
        waitingGrid.getChildren().clear();
        waitingSlotAssignments.clear();

        for (int i = 0; i < capacity; i++) {
            StackPane slot = new StackPane();
            slot.setPrefSize(SLOT_W, SLOT_H);

            Label label = new Label("Empty");
            label.setFont(Font.loadFont(getClass().getResourceAsStream(fontPath), 16));
            label.setTextFill(Color.GRAY);

            slot.getChildren().add(label);
            StackPane.setAlignment(label, Pos.TOP_CENTER);

            if ((i + 1) % SLOTS_PER_ROW != 0) {
                slot.setStyle("-fx-border-color: transparent black transparent transparent; -fx-border-width: 0 2 0 0;");
            }

            waitingSlots.add(slot);
            waitingSlotAssignments.add(null);

            int r = i / SLOTS_PER_ROW;
            int c = i % SLOTS_PER_ROW;
            waitingGrid.add(slot, c, r);
        }
    }

    // -----------------------
    // Create pump ImageViews
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
                // Remove cars that left the waiting area
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
                    Label label = (Label) slot.getChildren().get(0);
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
            }
        );
    }

    // ================= Animation helpers =================
    public void spawnCarAtEntrance(String carId) {
        Platform.runLater(() -> {
            if (carMap.containsKey(carId)) {
                return; // Already spawned
            }
            
            // Create the car view
            ImageView carView = new ImageView(carImage);
            carView.setFitWidth(60);
            carView.setFitHeight(30);
            ColorAdjust colorAdjust = new ColorAdjust();
            
            // a random hue for each car
            double randomHue = Math.random() * 2.0 - 1.0;
            colorAdjust.setHue(randomHue);
            carView.setEffect(colorAdjust);

            // car positioning
            carView.setLayoutX(-5); 
            carView.setLayoutY(Math.random() * (animationPane.getHeight() - carView.getFitHeight()));
            carMap.put(carId, carView);
            animationPane.getChildren().add(carView);
            carView.toFront();
        });
    }

    // ----------------------------
    // Animations for Car movements in CarWashGUI
    // ----------------------------

    private void moveCarToWaiting(String carId, int slotIndex) 
    {
        // Get the ImageView object representing the car from the map using carId
        ImageView carView = carMap.get(carId);

        // If car is not found in the map, print an error and exit method
        if (carView == null) 
        {
            System.err.println("Error: Car " + carId + " not found in map to move!");
            return;
        }

        // Run the animation code on JavaFX Application Thread to update GUI safely
        Platform.runLater(() -> {
            carView.toFront();             // Bring car to front so it is visible above other elements
            animationPane.applyCss();      // Apply CSS updates (needed to recalculate layout)
            animationPane.layout();        // Recalculate layout positions

            // Get the waiting slot StackPane at the given index
            StackPane slot = waitingSlots.get(slotIndex);

            // Compute the center position of the slot in scene coordinates
            Point2D slotCenterScene = slot.localToScene(slot.getWidth() / 2.0, slot.getHeight() / 2.0);
            // Convert scene coordinates to local coordinates of animationPane
            Point2D slotCenter = animationPane.sceneToLocal(slotCenterScene);

            // Calculate target X and Y for car, centering car in slot
            double targetX = slotCenter.getX() - carView.getFitWidth() / 2.0;
            double targetY = slotCenter.getY() - carView.getFitHeight() / 4.0;

            // Create a smooth translation animation for car to move to the slot
            TranslateTransition tt = new TranslateTransition(Duration.seconds(1.2), carView);
            tt.setToX(targetX - carView.getLayoutX());  // Relative X movement
            tt.setToY(targetY - carView.getLayoutY());  // Relative Y movement

            // When animation finishes, snap car exactly to target position and reset translations
            tt.setOnFinished(ev -> {
                carView.setLayoutX(targetX);
                carView.setLayoutY(targetY);
                carView.setTranslateX(0);
                carView.setTranslateY(0);
                carView.toFront(); // Ensure car remains on top after move
            });

            tt.play(); // Start the animation
        });
    }

    // ----------------------------
    // Animation: Move car to pump
    // ----------------------------
    private void moveCarToPump(String carId, int pumpIndex) 
    {
        // Get the car ImageView
        ImageView carView = carMap.get(carId);

        // If car is missing (rare case), create a new ImageView and add to animationPane
        if (carView == null) 
        {
            carView = new ImageView(carImage);  // Default car image
            carView.setFitWidth(60);            // Set car width
            carView.setFitHeight(30);           // Set car height
            carMap.put(carId, carView);         // Add to map
            animationPane.getChildren().add(carView); // Add to pane for display
            carView.toFront();                  // Bring to front
        }

        // Make a final copy of carView for use inside lambda expression
        final ImageView carFinal = carView;

        // Run animation safely on JavaFX thread
        Platform.runLater(() -> {
            ImageView pumpView = pumpImages.get(pumpIndex); // Get pump ImageView
            // Find center position of pump in scene coordinates
            Point2D pumpCenterScene = pumpView.localToScene(
                    pumpView.getBoundsInLocal().getWidth() / 2.0,
                    pumpView.getBoundsInLocal().getHeight() / 2.0
            );
            // Convert pump center to animationPane coordinates
            Point2D pumpCenterInAnim = animationPane.sceneToLocal(pumpCenterScene);

            // Set target position slightly in front of pump (offset Y by 10 pixels)
            double targetX = pumpCenterInAnim.getX();
            double targetY = pumpCenterInAnim.getY() + 10; // Position in front of pump

            // Compute relative movement
            double dx = targetX - carFinal.getLayoutX();
            double dy = targetY - carFinal.getLayoutY();

            // Create smooth translation animation to pump
            TranslateTransition tt = new TranslateTransition(Duration.seconds(1.5), carFinal);
            tt.setByX(dx);
            tt.setByY(dy);
            tt.setOnFinished(ev -> {
                // Snap car exactly to pump spot after animation
                carFinal.setLayoutX(targetX);
                carFinal.setLayoutY(targetY);
                carFinal.setTranslateX(0);
                carFinal.setTranslateY(0);

                // Change pump image to active state to indicate usage
                pumpView.setImage(pumpActive);
            });
            tt.play(); // Start the animation
        });
    }

    // ----------------------------
    // Animation: Car leaving simulation
    // ----------------------------
    private void moveCarOut(String carId) 
    {
        // Remove car from map
        ImageView carView = carMap.remove(carId);
        if (carView == null) return; // Exit if car does not exist

        Platform.runLater(() -> {
            // Move car to right off-screen with slight upward offset
            double targetX = animationPane.getWidth() + 300; // Offscreen X
            double dx = targetX - carView.getLayoutX();
            double dy = -60; // Slight upward movement

            // Create translation animation
            TranslateTransition tt = new TranslateTransition(Duration.seconds(2.0), carView);
            tt.setByX(dx);
            tt.setByY(dy);
            tt.setOnFinished(ev -> animationPane.getChildren().remove(carView)); // Remove car after leaving
            tt.play();
        });
    }
}
// ----------------------------
// ServiceStation Class (Main)
// ----------------------------
// Main JavaFX application class managing start menu, input dialogs, and simulation initialization
public class ServiceStation extends Application
{
    private static CarWashGUI gui; // Reference to GUI for simulation display
    private Stage mainStage;       // Primary stage for the application

    public ServiceStation()
    {
        super(); // Call JavaFX Application constructor
    }
    
    // ----------------------------
    // Helper method to show information alert
    // ----------------------------
    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Notice");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait(); // Display modal alert
        });
    }

    // ----------------------------
    // Static method to restart main menu
    // ----------------------------
    public static void showMainMenuAgain() {
        Platform.runLater(() -> {
            try {
                new ServiceStation().start(new Stage()); // Open a new stage with main menu
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void start(Stage stage)
    {
        this.mainStage = stage; // Store reference to primary stage

        // ----------------------------
        // Setup Start Menu GUI
        // ----------------------------
        Image backgroundImage = new Image(getClass().getResource("Images/menu_bg2.png").toExternalForm());

        BackgroundImage bgImg = new BackgroundImage(
            backgroundImage,
            BackgroundRepeat.NO_REPEAT,
            BackgroundRepeat.NO_REPEAT,
            BackgroundPosition.CENTER,
            BackgroundSize.DEFAULT
        );

        StackPane root = new StackPane();
        root.setBackground(new Background(bgImg)); // Set menu background image

        // ----------------------------
        // Start Button
        // ----------------------------
        Button startBtn = new Button("▶ Start Simulation");
        startBtn.setStyle(
            "-fx-background-color: #ffffff; -fx-text-fill: #2196F3; -fx-font-size: 16px; " +
            "-fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 10px 20px;"
        );

        // Change style on hover for visual feedback
        startBtn.hoverProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal) {
                    startBtn.setStyle(
                        "-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 16px; " +
                        "-fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 10px 20px;"
                    );
                } else {
                    startBtn.setStyle(
                        "-fx-background-color: #ffffff; -fx-text-fill: #2196F3; -fx-font-size: 16px; " +
                        "-fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 10px 20px;"
                    );
                }
            }
        );

        // ----------------------------
        // Exit Button
        // ----------------------------
        Button exitBtn = new Button("Exit");
        exitBtn.setStyle(
            "-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8px;"
        );

        // Hover effect for Exit button
        exitBtn.hoverProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal) {
                    exitBtn.setStyle(
                        "-fx-background-color: #a72525ff; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8px;"
                    );
                } else {
                    exitBtn.setStyle(
                        "-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8px;"
                    );
                }
            }
        );
        exitBtn.setOnAction(e -> Platform.exit()); // Close application when clicked

        // ----------------------------
        // Credits Section
        // ----------------------------
        Label credits = new Label(
            "Created by: AI Section 5 — Team Project\n" +
            "Team Members:\n" +
            "Ali Radwan • Adel Hefny • Ziad Salama • Mohamed Mahmoud • Asser Ahmed"
        );
        credits.setFont(Font.font("Arial", 13));
        credits.setTextFill(Color.WHITE);
        credits.setStyle(
            "-fx-background-color: rgba(80, 80, 80, 0.4);" + 
            "-fx-text-fill: white;" +
            "-fx-font-size: 15px;" + 
            "-fx-font-weight: bold;" + 
            "-fx-effect: dropshadow(gaussian, black, 0.6, 0.5, 0, 0);" +
            "-fx-padding: 7px;" +
            "-fx-background-radius: 10;" +
            "-fx-opacity: 0.95;"
        );
        credits.setAlignment(Pos.CENTER);

        // ----------------------------
        // Fade-In Animation for Credits
        // ----------------------------
        FadeTransition fadeIn = new FadeTransition(javafx.util.Duration.seconds(3.5), credits);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setCycleCount(javafx.animation.Animation.INDEFINITE);
        fadeIn.setAutoReverse(true);
        fadeIn.play();

        // ----------------------------
        // Layout: Menu choices
        // ----------------------------
        Separator line = new Separator();
        line.setStyle("-fx-background-color: white; -fx-opacity: 0.3;");
        line.setPrefWidth(400);

        VBox menuChoices = new VBox(15, startBtn, exitBtn, line, credits);
        menuChoices.setAlignment(Pos.CENTER_LEFT);
        menuChoices.setPadding(new Insets(20));
        menuChoices.setPrefWidth(300);
        menuChoices.setTranslateY(200);

        StackPane menuLayout = new StackPane(menuChoices);
        menuLayout.setAlignment(Pos.CENTER);
        menuLayout.setPadding(new Insets(30));
        root.getChildren().add(menuLayout);

        Scene menuScene = new Scene(root, 610, 607);
        stage.setTitle("Car Wash Simulation");
        stage.setScene(menuScene);
        stage.show();

        // Open simulation input dialog when Start button clicked
        startBtn.setOnAction(e -> showInputDialog(root));
    }

    // ----------------------------
    // Input dialog for simulation settings
    // ----------------------------
    private void showInputDialog(StackPane root)
    {
        // Header label
        Label header = new Label("Simulation Settings");
        header.setFont(Font.font("Arial", 20));
        header.setTextFill(Color.WHITE);

        // Waiting area slider
        Label waitingLabel = new Label("Waiting Area Capacity: 5");
        waitingLabel.setTextFill(Color.WHITE);
        waitingLabel.setFont(Font.font("Arial", 14));
        waitingLabel.setPrefWidth(220);

        Slider waitingSlider = new Slider(1, 10, 5);
        waitingSlider.setSnapToTicks(true);
        waitingSlider.setMajorTickUnit(5);
        waitingSlider.setMinorTickCount(4);
        waitingSlider.setBlockIncrement(1);
        waitingSlider.setMaxWidth(220);
        waitingSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            waitingLabel.setText("Waiting Area Capacity: " + newVal.intValue());
        });

        // Pumps slider
        Label pumpLabel = new Label("Number of Pumps: 3");
        pumpLabel.setTextFill(Color.WHITE);
        pumpLabel.setFont(Font.font("Arial", 14));
        pumpLabel.setPrefWidth(220);

        Slider pumpSlider = new Slider(1, 5, 3);
        pumpSlider.setSnapToTicks(true);
        pumpSlider.setMajorTickUnit(1);
        pumpSlider.setBlockIncrement(1);
        pumpSlider.setMaxWidth(220);
        pumpSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            pumpLabel.setText("Number of Pumps: " + newVal.intValue());
        });

        // Cars input field
        TextField carField = new TextField();
        carField.setPromptText("Enter number of cars (default 10)");
        carField.setStyle(
            "-fx-background-color: #272a2bff;" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: #bbbbbb;" +
            "-fx-border-color: #555555;" +
            "-fx-border-radius: 5;" +
            "-fx-background-radius: 5;"
        );
        carField.setMaxWidth(220);

        // Buttons: Start / Cancel
        HBox buttons = new HBox(15);
        buttons.setAlignment(Pos.CENTER);

        // Start simulation button
        Button startSimBtn = new Button("Start");
        startSimBtn.setStyle(
            "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; " +
            "-fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8px 16px;"
        );
        startSimBtn.hoverProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal) {
                    startSimBtn.setStyle(
                        "-fx-background-color: #317a34ff; -fx-text-fill: white; -fx-font-size: 14px; " +
                        "-fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8px 16px;"
                    );
                } else {
                    startSimBtn.setStyle(
                        "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; " +
                        "-fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8px 16px;"
                    );
                }
            }
        );

        // Cancel button
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
            "-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14px; " +
            "-fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8px 16px;"
        );
        cancelBtn.hoverProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal) {
                    cancelBtn.setStyle(
                        "-fx-background-color: #a72525ff; -fx-text-fill: white; -fx-font-size: 14px; " +
                        "-fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8px 16px;"
                    );
                } else {
                    cancelBtn.setStyle(
                        "-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14px; " +
                        "-fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8px 16px;"
                    );
                }
            }
        );

        buttons.getChildren().addAll(startSimBtn, cancelBtn);

        // Dialog layout
        VBox dialogContent = new VBox(10, header, waitingLabel, waitingSlider, pumpLabel, pumpSlider, carField, buttons);
        dialogContent.setAlignment(Pos.CENTER);
        dialogContent.setPadding(new Insets(20));
        dialogContent.setPrefWidth(300);

        StackPane dialogBox = new StackPane(dialogContent);
        dialogBox.setAlignment(Pos.CENTER);
        dialogBox.setStyle(
            "-fx-background-color: rgba(0,0,0,0.5); " +
            "-fx-background-radius: 20; -fx-border-radius: 20; " +
            "-fx-border-color: white; -fx-border-width: 2;"
        );
        dialogBox.setPadding(new Insets(30));

        root.getChildren().add(dialogBox); // Add dialog to main menu

        // Cancel button closes dialog
        cancelBtn.setOnAction(e -> root.getChildren().remove(dialogBox));

        // Start button reads inputs and starts simulation
        startSimBtn.setOnAction(
            e ->{
                int waiting = 5, pumps = 3, cars = 10; // default values

                waiting = (int) waitingSlider.getValue();
                pumps = (int) pumpSlider.getValue();

                try 
                {
                    if (!carField.getText().trim().isEmpty())
                    {
                        cars = Math.max(1, Integer.parseInt(carField.getText().trim())); // Ensure at least 1 car
                    }
                } 
                catch (NumberFormatException ex) 
                {
                    showAlert("Invalid car count! Using default (10).");
                }

                root.getChildren().remove(dialogBox); // Remove dialog after input
                startSimulation(waiting, pumps, cars); // Start simulation
            }
        );
    }

    // ----------------------------
    // Initializes semaphores and threads for simulation
    // ----------------------------
    private void runSimulation(int waitingCapacity, int pumpCount, int carCount)
    {
        try 
        {
            Thread.sleep(500); // slight delay for GUI rendering

            // Semaphore for mutual exclusion
            Semaphore mutex = new Semaphore(1);
            // Semaphore representing empty slots in waiting area
            Semaphore empty = new Semaphore(waitingCapacity);
            // Semaphore representing full slots (cars waiting)
            Semaphore full = new Semaphore(0);
            // Semaphore representing available pumps
            Semaphore pumps = new Semaphore(pumpCount);

            Queue<String> queue = new LinkedList<>(); // Shared queue for waiting cars

            // Start threads for each pump
            for (int i = 0; i < pumpCount; i++) 
            {
                new Pump(i, gui, queue, mutex, empty, full, pumps).start();
            }

            // Start threads for each car
            for (int i = 0; i < carCount; i++) 
            {
                new Car("C" + i, gui, queue, mutex, empty, full).start();
                Thread.sleep(1500); // Stagger car arrivals
            }
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    // ----------------------------
    // Create GUI and start simulation threads
    // ----------------------------
    private void startSimulation(int waitingCapacity, int pumpCount, int carCount)
    {
        gui = new CarWashGUI(waitingCapacity, pumpCount); // Initialize simulation GUI

        Scene simScene = new Scene(gui, 800, 600); // Create scene
        mainStage.setScene(simScene); // Set scene to main stage

        new Thread(() -> runSimulation(waitingCapacity, pumpCount, carCount)).start(); // Start simulation in separate thread
    }

    public static void main(String[] args)
    {
        launch(args); // Launch JavaFX application
    }
}
