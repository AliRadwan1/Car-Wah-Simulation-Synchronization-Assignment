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

/**
 * Represents a car that arrives at the car wash station
 * Each car runs in its own thread and follows the car wash process
 */
class Car extends Thread
{
    private String ID;              // Unique identifier for the car (e.g., "C1", "C2")
    private CarWashGUI gui;         // Reference to the GUI for visual updates
    private Queue<String> queue;    // Shared queue representing the waiting area
    private Semaphore mutex;        // Mutual exclusion semaphore for queue access
    private Semaphore empty;        // Semaphore tracking empty slots in waiting area
    private Semaphore full;         // Semaphore tracking occupied slots in waiting area

    /**
     * Constructor to initialize a car with its dependencies
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

    /**
     * The main execution logic for each car thread
     * Simulates the car's journey from arrival to entering waiting area
     */
    public void run()
    {
        try {
            // Visual: Show car arriving at entrance
            gui.spawnCarAtEntrance(ID);
            gui.log(ID + " arrived, waiting for a free slot...");
            
            // Wait for an available slot in waiting area (producer-consumer pattern)
            empty.waitS(); // Decrements empty count, blocks if no slots available
            
            // Acquire mutual exclusion to safely modify the shared queue
            mutex.waitS(); 
            
            // Critical Section: Add car to waiting area queue
            queue.add(ID);
            gui.log(ID + " moving to waiting area");
            gui.updateWaitingArea(new ArrayList<>(queue));
            Thread.sleep(2000); // Simulate time to move to waiting area

            // Signal that waiting area now has one more car
            full.signal();
            // Release mutual exclusion lock
            mutex.signal();
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
            
    }
}

/**
 * Represents a washing pump that services cars
 * Each pump runs in its own thread and continuously processes cars
 */
class Pump extends Thread
{
    private int pumpID;             // Unique identifier for the pump
    private CarWashGUI gui;         // Reference to GUI for visual updates
    private Queue<String> queue;    // Shared queue of waiting cars
    private Semaphore mutex;        // Mutual exclusion semaphore
    private Semaphore empty;        // Tracks empty waiting slots
    private Semaphore full;         // Tracks occupied waiting slots
    private Semaphore pumps;        // Semaphore for available pumps

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

    /**
     * Main execution loop for pump threads
     * Continuously takes cars from waiting area and services them
     */
    public void run()
    {
        while (true) {
            try 
            {
                // Wait until there are cars in the waiting area
                full.waitS();
                // Wait until this pump becomes available
                pumps.waitS();
                // Acquire lock to safely access the shared queue
                mutex.waitS();

                // Critical Section: Remove car from waiting area
                String car = queue.poll();
                gui.log("Pump " + (pumpID + 1) + ": " + car + " begins service");
                gui.updateWaitingArea(new ArrayList<>(queue));
                gui.updatePumpStatus(pumpID, car, true);

                // Release lock and signal that waiting area has empty slot
                mutex.signal();
                empty.signal();

                // Simulate the car washing process (5 seconds)
                Thread.sleep(5000);
                gui.log("Pump " + (pumpID + 1) + ": " + car + " finishes service");
                gui.updatePumpStatus(pumpID, car, false);

                // Signal that this pump is now available again
                pumps.signal();
            } 
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }
}

/**
 * Implements a counting semaphore for thread synchronization
 * Used to control access to shared resources in the car wash simulation
 */
class Semaphore
{
    private int value;  // The semaphore counter value

    public Semaphore(int value)
    {
        this.value = value;
    }
    
    /**
     * Wait (acquire) operation - decrements semaphore value
     * If value is 0, thread blocks until signaled
     */
    public synchronized void waitS()
    {
        while (value == 0) {
            try {
                wait();  // Thread waits until notified
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        value--;  // Decrement semaphore value
    }

    /**
     * Signal (release) operation - increments semaphore value
     * Wakes up one waiting thread if any
     */
    public synchronized void signal()
    {
        value++;  // Increment semaphore value
        notify(); // Wake up one waiting thread
    }
}

/**
 * Main GUI class for the Car Wash Simulation
 * Handles all visual elements and animations using JavaFX
 */
class CarWashGUI extends VBox
{
    // GUI Components
    private Pane animationPane;                 // Main area where cars move and animate
    private GridPane waitingGrid;               // Grid layout for waiting area slots
    private ScrollPane waitingScroll;           // Scrollable container for waiting area
    private HBox pumpBox;                       // Horizontal box containing pump visuals
    private TextArea logArea;                   // Text area for simulation logs

    // Data structures for tracking visual elements
    private Map<String, ImageView> carMap = new HashMap<>();      // Maps car IDs to their visual representations
    private List<ImageView> pumpImages = new ArrayList<>();       // List of pump image views
    private List<StackPane> waitingSlots = new ArrayList<>();     // List of waiting slot visual containers

    // Tracks which car is in which waiting slot (null if empty)
    private final List<String> waitingSlotAssignments = new ArrayList<>();

    // Simulation parameters
    private int waitingCapacity;    // Maximum cars in waiting area
    private int pumpCount;          // Number of washing pumps
    private final Font font = Font.loadFont(getClass().getResourceAsStream("/fonts/robotoslab.ttf"), 24);
    
    // Image resources
    private final Image carImage;       // Car visual representation
    private final Image pumpIdle;       // Pump when not in use
    private final Image pumpActive;     // Pump when washing a car
    private final Image slotImage;      // Waiting slot background
    private final Image backgroundImage; // Optional background image

    // Layout constants
    private final int SLOTS_PER_ROW = 5;    // Number of slots per row in waiting area
    private final int VISIBLE_ROWS = 3;     // Number of initially visible rows
    private final double SLOT_W = 120;      // Slot width in pixels
    private final double SLOT_H = 70;       // Slot height in pixels

    /**
     * Constructor - initializes the main GUI layout and components
     */
    public CarWashGUI(int waitingCapacity, int pumpCount)
    {
        this.waitingCapacity = waitingCapacity;
        this.pumpCount = pumpCount;

        // Load images from resources - these are used for visual representation
        carImage = new Image(getClass().getResource("Images/car.png").toExternalForm(), 200, 30, false, false);
        pumpIdle = new Image(getClass().getResource("Images/pump_idle.png").toExternalForm(), 100, 100, true, true);
        pumpActive = new Image(getClass().getResource("Images/pump_active.png").toExternalForm(), 100, 100, true, true);
        slotImage = new Image(getClass().getResource("Images/slot.png").toExternalForm(), (int)SLOT_W, (int)SLOT_H, true, true);
        
        // Load background image with error handling
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

        // Set up main layout properties
        setSpacing(12);
        setPadding(new Insets(12));
        setAlignment(Pos.TOP_CENTER);

        // ----------- Title Section
        Label title = new Label("Car Wash Simulation");
        title.setFont(font);
        title.setTextFill(Color.DARKBLUE);

        // -------- Animation Pane: Main area for car movements
        animationPane = new Pane();
        animationPane.setPrefSize(760, 280);
        animationPane.setStyle("-fx-border-color: #90CAF9; -fx-border-width: 2; -fx-background-radius: 6;");
        
        // Set background image or fallback color
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

        // ---------- Visual Waiting Area (slots grid)
        waitingGrid = new GridPane();
        waitingGrid.setHgap(12);
        waitingGrid.setVgap(12);
        waitingGrid.setPadding(new Insets(8));
        waitingGrid.setAlignment(Pos.CENTER);

        // Create all waiting slot visual elements
        createWaitingSlots(waitingCapacity);

        // Wrap waiting grid in scroll pane for large capacities
        waitingScroll = new ScrollPane(waitingGrid);
        waitingScroll.setPrefViewportWidth(SLOTS_PER_ROW * (SLOT_W + 12) + 20);
        double visibleHeight = VISIBLE_ROWS * (SLOT_H + 12) + 30;
        waitingScroll.setPrefViewportHeight(Math.min(visibleHeight, waitingCapacity * (SLOT_H + 12) + 30));
        waitingScroll.setFitToWidth(true);
        waitingScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        waitingScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox waitingBox = new VBox(6, new Label("Waiting Area (slots)"), waitingScroll);
        waitingBox.setAlignment(Pos.CENTER);

        // -------- Pump Area - visual representation of service bays
        pumpBox = new HBox(30);
        pumpBox.setAlignment(Pos.CENTER);
        createPumps(pumpCount);

        VBox pumpBoxWithLabel = new VBox(6, new Label("Service Bays"), pumpBox);
        pumpBoxWithLabel.setAlignment(Pos.CENTER);

        // -------- Log Area - displays simulation events
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(120);

        // -------- Navigation Controls
        Button backButton = new Button("⬅ Back to Menu");
        backButton.setPrefWidth(180);
        backButton.setStyle(
                "-fx-font-size: 13px; -fx-background-color: #E91E63; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 8px;");
        backButton.setOnAction(e -> Platform.runLater(ServiceStation::showMainMenuAgain));

        HBox controls = new HBox(backButton);
        controls.setAlignment(Pos.CENTER);

        // Combine animation pane and waiting grid in stack layout
        StackPane waitingVisualPane = new StackPane(waitingGrid,animationPane);
        waitingVisualPane.setPrefHeight(300);

        // Add all components to main layout
        getChildren().addAll(title, waitingVisualPane, pumpBoxWithLabel, logArea, controls);
    }

    /**
     * Creates visual slots for the waiting area grid
     * @param capacity Number of slots to create
     */
    private void createWaitingSlots(int capacity) {
        waitingSlots.clear();
        waitingGrid.getChildren().clear();

        for (int i = 0; i < capacity; i++) {
            StackPane slot = new StackPane();
            slot.setPrefSize(SLOT_W, SLOT_H);

            Label label = new Label("Empty");
            label.setFont(Font.font(12));
            label.setTextFill(Color.GRAY);

            slot.getChildren().add(label);
            StackPane.setAlignment(label, Pos.TOP_CENTER);

            // Add visual separator between slots (except last in each row)
            if ((i + 1) % SLOTS_PER_ROW != 0) {
                slot.setStyle("-fx-border-color: transparent black transparent transparent; -fx-border-width: 0 2 0 0;");
            }

            waitingSlots.add(slot);
            waitingSlotAssignments.add(null); // Initialize all slots as empty

            // Calculate grid position and add to layout
            int r = i / SLOTS_PER_ROW;
            int c = i % SLOTS_PER_ROW;
            waitingGrid.add(slot, c, r);
        }
    }

    /**
     * Creates visual representations for all pumps
     * @param count Number of pumps to create
     */
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

    // ---------- Logging Methods ----------

    /**
     * Appends a message to the log area in a thread-safe manner
     * @param message The message to log
     */
    public void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    // ---------- Pump Status Updates ----------

    /**
     * Updates the visual status of a pump (idle/active) and manages car movement
     * Called by Pump threads when they start/finish servicing a car
     */
    public void updatePumpStatus(int pumpIndex, String carId, boolean occupied) {
        Platform.runLater(
            () -> {
                ImageView pumpView = pumpImages.get(pumpIndex);
                if (occupied) {
                    // Car is starting service - remove from waiting area and move to pump
                    freeWatingSlot(carId);
                    moveCarToPump(carId, pumpIndex);
                } else {
                    // Car finished service - reset pump to idle state
                    pumpView.setImage(pumpIdle);
                    moveCarOut(carId);
                }
            }
        );
    }
    
    /**
     * Removes a car from its waiting slot assignment
     * @param carId The ID of the car to remove
     */
    private void freeWatingSlot(String carId)
    {
        int index = waitingSlotAssignments.indexOf(carId);
        if (index != -1)
        {
            waitingSlotAssignments.set(index, null);
        }
    }

    /**
     * Updates the waiting area display based on current queue state
     * Manages both visual updates and car animations
     * @param cars List of car IDs currently in the waiting area
     */
    public void updateWaitingArea(List<String> cars) {
        Platform.runLater(
            () -> {
                // Remove cars that have left the waiting area (moved to pumps)
                for (int i = 0; i < waitingSlotAssignments.size(); i++) 
                {
                    String carAtSlot = waitingSlotAssignments.get(i);
                    if (carAtSlot != null && !cars.contains(carAtSlot)) 
                    {
                        // Car has left the waiting area for service
                        waitingSlotAssignments.set(i, null);
                    }
                }

                // Add new cars to empty slots in first-come-first-served order
                for (String carID : cars) 
                {
                    boolean alreadyAssigned = waitingSlotAssignments.contains(carID);
                    if (!alreadyAssigned) 
                    {
                        // Find first empty slot and assign the car
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

                // Update all slot labels to show current car assignments
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

    // ================= Animation Methods =================

    /**
     * Creates and displays a new car at the entrance of the car wash
     * @param carId Unique identifier for the car
     */
    public void spawnCarAtEntrance(String carId) {
        Platform.runLater(() -> {
            if (carMap.containsKey(carId)) {
                return; // Car already exists, prevent duplicates
            }
            
            // Create visual representation of the car
            ImageView carView = new ImageView(carImage);
            carView.setFitWidth(60);
            carView.setFitHeight(30);
            
            // Apply random color variation for visual distinction
            ColorAdjust colorAdjust = new ColorAdjust();
            double randomHue = Math.random() * 2.0 - 1.0;
            colorAdjust.setHue(randomHue);
            carView.setEffect(colorAdjust);

            // Position car at entrance (left side)
            carView.setLayoutX(-5); 
            carView.setLayoutY(Math.random() * (animationPane.getHeight() - carView.getFitHeight()));
            carMap.put(carId, carView);
            animationPane.getChildren().add(carView);
            carView.toFront(); // Ensure car is visible above other elements
        });
    }

    /**
     * Animates a car moving from entrance to its assigned waiting slot
     * @param carId The car to move
     * @param slotIndex The destination slot index
     */
    private void moveCarToWaiting(String carId, int slotIndex) 
    {
        ImageView carView = carMap.get(carId);
        if (carView == null) 
        {
            System.err.println("Error: Car " + carId + " not found in map to move!");
            return;
        }

        Platform.runLater(() -> {
            carView.toFront();

            // Ensure layout is calculated before getting coordinates
            animationPane.applyCss();
            animationPane.layout();

            StackPane slot = waitingSlots.get(slotIndex);

            // Calculate target position (center of the slot)
            Point2D slotCenterScene = slot.localToScene(slot.getWidth() / 2.0, slot.getHeight() / 2.0);
            Point2D slotCenter = animationPane.sceneToLocal(slotCenterScene);

            double targetX = slotCenter.getX() - carView.getFitWidth() / 2.0;
            double targetY = slotCenter.getY() - carView.getFitHeight() / 4.0;

            // Create smooth translation animation
            TranslateTransition tt = new TranslateTransition(Duration.seconds(1.2), carView);
            tt.setToX(targetX - carView.getLayoutX());
            tt.setToY(targetY - carView.getLayoutY());

            // Finalize position after animation completes
            tt.setOnFinished(ev -> {
                carView.setLayoutX(targetX);
                carView.setLayoutY(targetY);
                carView.setTranslateX(0);
                carView.setTranslateY(0);
                carView.toFront();
            });
            tt.play();
        });
    }
    
    /**
     * Animates a car moving from waiting area to a pump for service
     * @param carId The car to move
     * @param pumpIndex The destination pump index
     */
    private void moveCarToPump(String carId, int pumpIndex) 
    {
        ImageView carView = carMap.get(carId);
        if (carView == null) 
        {
            // If car visual is missing (shouldn't happen), create it near the pump
            carView = new ImageView(carImage);
            carView.setFitWidth(60);
            carView.setFitHeight(30);
            carMap.put(carId, carView);
            animationPane.getChildren().add(carView);
            carView.toFront();
        }

        final ImageView carFinal = carView;

        Platform.runLater(() -> {
            ImageView pumpView = pumpImages.get(pumpIndex);
            
            // Calculate pump center position in animation pane coordinates
            Point2D pumpCenterScene = pumpView.localToScene(
                    pumpView.getBoundsInLocal().getWidth() / 2.0,
                    pumpView.getBoundsInLocal().getHeight() / 2.0
            );
            Point2D pumpCenterInAnim = animationPane.sceneToLocal(pumpCenterScene);

            // Target position slightly in front of the pump visual
            double targetX = pumpCenterInAnim.getX();
            double targetY = pumpCenterInAnim.getY() + 10;

            double dx = targetX - carFinal.getLayoutX();
            double dy = targetY - carFinal.getLayoutY();

            // Animate car movement to pump
            TranslateTransition tt = new TranslateTransition(Duration.seconds(1.5), carFinal);
            tt.setByX(dx);
            tt.setByY(dy);
            tt.setOnFinished(ev -> {
                // Snap to exact position after animation
                carFinal.setLayoutX(targetX);
                carFinal.setLayoutY(targetY);
                carFinal.setTranslateX(0);
                carFinal.setTranslateY(0);

                // Change pump visual to active state
                pumpView.setImage(pumpActive);
            });
            tt.play();
        });
    }

    /**
     * Animates a car leaving the car wash after service completion
     * @param carId The car to remove
     */
    private void moveCarOut(String carId) 
    {
        ImageView carView = carMap.remove(carId);
        if (carView == null) return;

        Platform.runLater(() -> {
            // Animate car moving off-screen to the right
            double targetX = animationPane.getWidth() + 300;
            double dx = targetX - carView.getLayoutX();
            double dy = -60; // Slight upward curve for natural exit

            TranslateTransition tt = new TranslateTransition(Duration.seconds(2.0), carView);
            tt.setByX(dx);
            tt.setByY(dy);
            tt.setOnFinished(ev -> animationPane.getChildren().remove(carView));
            tt.play();
        });
    }
}

/**
 * Main application class for the Car Wash Simulation
 * Handles application lifecycle, menu navigation, and simulation setup
 */
public class ServiceStation extends Application
{
    private static CarWashGUI gui;  // Static reference to GUI for global access
    private Stage mainStage;         // Primary application window

    public ServiceStation()
    {
        super();
    }
    
    // ----------- Utility Methods -----------

    /**
     * Displays an informational alert dialog to the user
     * @param message The message to display
     */
    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Notice");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Returns to the main menu from any point in the application
     * Used for navigation from simulation back to menu
     */
    public static void showMainMenuAgain() {
        Platform.runLater(() -> {
            try {
                new ServiceStation().start(new Stage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * JavaFX application entry point - initializes and displays the main menu
     * @param stage The primary stage provided by JavaFX
     */
    @Override
    public void start(Stage stage)
    {
        this.mainStage = stage;
        
        // ------------ Main Menu Setup
        // Load and set background image
        Image backgroundImage = new Image(getClass().getResource("Images/menu_bg2.png").toExternalForm());

        BackgroundImage bgImg = new BackgroundImage(
            backgroundImage,
            BackgroundRepeat.NO_REPEAT,
            BackgroundRepeat.NO_REPEAT,
            BackgroundPosition.CENTER,
            BackgroundSize.DEFAULT
        );

        StackPane root = new StackPane();
        root.setBackground(new Background(bgImg));

        // ------------- Start Button - begins the simulation
        Button startBtn = new Button("▶ Start Simulation");
        startBtn.setStyle(
            "-fx-background-color: #ffffff; -fx-text-fill: #2196F3; -fx-font-size: 16px; " +
            "-fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 10px 20px;"
        );
        // Hover effect for better user interaction
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
        
        // ------------- Exit Button - closes the application
        Button exitBtn = new Button("Exit");
        exitBtn.setStyle(
            "-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8px;"
        );
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
        exitBtn.setOnAction(e -> Platform.exit());        

        // ------------- Credits Section - displays team information
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

        // ---- Fade-In Animation for Credits ----
        FadeTransition fadeIn = new FadeTransition(javafx.util.Duration.seconds(3.5), credits);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setCycleCount(javafx.animation.Animation.INDEFINITE);
        fadeIn.setAutoReverse(true);
        fadeIn.play();

        Separator line = new Separator();
        line.setStyle("-fx-background-color: white; -fx-opacity: 0.3;");
        line.setPrefWidth(400);

        // Arrange menu elements vertically
        VBox menuChoices = new VBox(15, startBtn, exitBtn, line, credits);
        menuChoices.setAlignment(Pos.CENTER_LEFT);
        menuChoices.setPadding(new Insets(20));
        menuChoices.setPrefWidth(300);
        menuChoices.setTranslateY(200); // Position slightly below center

        StackPane menuLayout = new StackPane(menuChoices);
        menuLayout.setAlignment(Pos.CENTER);
        menuLayout.setPadding(new Insets(30));
        root.getChildren().add(menuLayout);
        
        Scene menuScene = new Scene(root, 610, 607);
        stage.setTitle("Car Wash Simulation");
        stage.setScene(menuScene);
        stage.show();

        // When Start button is clicked, show configuration dialog
        startBtn.setOnAction(e -> showInputDialog(root));
    }

    // ----------- Configuration Dialog -----------

    /**
     * Displays the simulation configuration dialog where users set parameters
     * @param root The root pane to overlay the dialog on
     */
    private void showInputDialog(StackPane root)
    {
        Label header = new Label("Simulation Settings");
        header.setFont(Font.font("Arial", 20));
        header.setTextFill(Color.WHITE);

        // Waiting Area Capacity Slider
        Label waitingLabel = new Label("Waiting Area Capacity: 5");
        waitingLabel.setTextFill(Color.WHITE);
        waitingLabel.setFont(Font.font("Arial", 14));
        waitingLabel.setPrefWidth(220);

        Slider waitingSlider = new Slider(1, 10, 5); // min=1, max=10, default=5
        waitingSlider.setSnapToTicks(true);
        waitingSlider.setMajorTickUnit(5);
        waitingSlider.setMinorTickCount(4);
        waitingSlider.setBlockIncrement(1);
        waitingSlider.setMaxWidth(220);

        // Update label when slider changes
        waitingSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            waitingLabel.setText("Waiting Area Capacity: " + newVal.intValue());
        });

        // Pump Count Slider
        Label pumpLabel = new Label("Number of Pumps: 3");
        pumpLabel.setTextFill(Color.WHITE);
        pumpLabel.setFont(Font.font("Arial", 14));
        pumpLabel.setPrefWidth(220);

        Slider pumpSlider = new Slider(1, 5, 3); // min=1, max=5, default=3
        pumpSlider.setSnapToTicks(true);
        pumpSlider.setMajorTickUnit(1);
        pumpSlider.setBlockIncrement(1);
        pumpSlider.setMaxWidth(220);

        pumpSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            pumpLabel.setText("Number of Pumps: " + newVal.intValue());
        });

        // Car Count Input Field
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

        // Action Buttons
        HBox buttons = new HBox(15);
        buttons.setAlignment(Pos.CENTER);

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

        buttons.getChildren().addAll(startSimBtn,cancelBtn);
        
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

        root.getChildren().add(dialogBox);

        // Cancel button removes the dialog
        cancelBtn.setOnAction(e -> root.getChildren().remove(dialogBox));

        // Start simulation with configured parameters
        startSimBtn.setOnAction(
            e ->{
                int waiting = 5, pumps = 3, cars = 10;

                // Get values from sliders
                waiting = (int) waitingSlider.getValue();
                pumps = (int) pumpSlider.getValue();

                // Parse car count with error handling
                try 
                {
                    if (!carField.getText().trim().isEmpty())
                    {
                        cars = Math.max(1, Integer.parseInt(carField.getText().trim()));
                    }
                } 
                catch (NumberFormatException ex) 
                {
                    showAlert("Invalid car count! Using default (10).");
                }

                // Remove dialog and start simulation
                root.getChildren().remove(dialogBox);
                startSimulation(waiting, pumps, cars);
            }
        );
    }

    /**
     * Runs the core simulation logic in a separate thread
     * Manages thread creation and synchronization semaphores
     */
    private void runSimulation(int waitingCapacity, int pumpCount, int carCount)
    {
        try 
        {
            Thread.sleep(500); // Brief delay for GUI to render completely
            
            // Initialize synchronization semaphores
            Semaphore mutex = new Semaphore(1);        // Mutual exclusion for queue access
            Semaphore empty = new Semaphore(waitingCapacity); // Tracks empty waiting slots
            Semaphore full = new Semaphore(0);         // Tracks occupied waiting slots  
            Semaphore pumps = new Semaphore(pumpCount); // Tracks available pumps

            Queue<String> queue = new LinkedList<>();  // Shared queue for waiting cars
            
            // Start all pump worker threads
            for (int i = 0; i < pumpCount; i++) 
            {
                new Pump(i, gui, queue, mutex, empty, full, pumps).start();
            }

            // Start car threads with delays to simulate staggered arrivals
            for (int i = 0; i < carCount; i++) 
            {
                new Car("C" + i, gui, queue, mutex, empty, full).start();
                Thread.sleep(1500); // 1.5 second delay between car arrivals
            }
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    /**
     * Initializes and starts the car wash simulation
     * Sets up the GUI and begins the simulation thread
     */
    private void startSimulation(int waitingCapacity, int pumpCount, int carCount)
    {
        // Create the simulation GUI scene
        gui = new CarWashGUI(waitingCapacity, pumpCount);
        Scene simScene = new Scene(gui, 800, 600);
        mainStage.setScene(simScene);

        // Start simulation in background thread to avoid blocking UI
        new Thread(() -> runSimulation(waitingCapacity, pumpCount, carCount)).start();
    }

    /**
     * Main application entry point
     * Launches the JavaFX application
     */
    public static void main(String[] args)
    {
        launch(args);
    }
}
