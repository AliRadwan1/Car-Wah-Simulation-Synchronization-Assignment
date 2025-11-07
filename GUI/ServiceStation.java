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
            gui.spawnCarAtEntrance(ID);
            gui.log(ID + " arrived, waiting for a free slot...");
            // checkPaused();
            empty.waitS(); // wait until there is space to add car
            // checkPaused();
            mutex.waitS(); // if the queue (Waiting Area) being in use wait else take mutex
            
            queue.add(ID);
            gui.log(ID + " moving to waiting area");
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

    // Maps slot index → carId (or null)
    private final List<String> waitingSlotAssignments = new ArrayList<>();

    private int waitingCapacity;
    private int pumpCount;
    private final Font font = Font.loadFont(getClass().getResourceAsStream("/fonts/robotoslab.ttf"), 24);
    
    private final Image carImage;
    private final Image pumpIdle;
    private final Image pumpActive;
    private final Image slotImage;
    private final Image backgroundImage; // optional

    private final int SLOTS_PER_ROW = 5;
    private final int VISIBLE_ROWS = 3;     // 3 x 5 = 15 visible slots
    private final double SLOT_W = 120;
    private final double SLOT_H = 70;

    public CarWashGUI(int waitingCapacity, int pumpCount)
    {
        this.waitingCapacity = waitingCapacity;
        this.pumpCount = pumpCount;

        // load images from classpath (GUI/Images/)
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

        setSpacing(12);
        setPadding(new Insets(12));
        setAlignment(Pos.TOP_CENTER);

        // ----------- Title
        Label title = new Label("Car Wash Simulation");
        title.setFont(font);
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

    for (int i = 0; i < capacity; i++) {
        StackPane slot = new StackPane();
        slot.setPrefSize(SLOT_W, SLOT_H);

        Label label = new Label("Empty");
        label.setFont(Font.font(12));
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
    // move a newly arrived car onto its slot index
    private void moveCarToWaiting(String carId, int slotIndex) 
    {
        ImageView carView = carMap.get(carId);
        if (carView == null) 
        {
            System.err.println("Error: Car " + carId + " not found in map to move!");
            return;
        }

        // Add to pane and animate to slot after layout pass
        Platform.runLater(() -> {
            carView.toFront();

            // ensure layout pass to have correct slot coordinates
            animationPane.applyCss();
            animationPane.layout();

            StackPane slot = waitingSlots.get(slotIndex);

            // car positioning
            Point2D slotCenterScene = slot.localToScene(slot.getWidth() / 2.0, slot.getHeight() / 2.0);
            Point2D slotCenter = animationPane.sceneToLocal(slotCenterScene);

            double targetX = slotCenter.getX() - carView.getFitWidth() / 2.0;
            double targetY = slotCenter.getY() - carView.getFitHeight() / 4.0;

            TranslateTransition tt = new TranslateTransition(Duration.seconds(1.2), carView);
            tt.setToX(targetX - carView.getLayoutX());
            tt.setToY(targetY - carView.getLayoutY());

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
        // Background Image
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

        // ------------- Start Button
        Button startBtn = new Button("▶ Start Simulation");
        startBtn.setStyle(
            "-fx-background-color: #ffffff; -fx-text-fill: #2196F3; -fx-font-size: 16px; " +
            "-fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 10px 20px;"
        );
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
        // ------------- Exit Button
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

        // ------------- Credits Section
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

        VBox menuChoices = new VBox(15, startBtn, exitBtn, line, credits);
        menuChoices.setAlignment(Pos.CENTER_LEFT);
        menuChoices.setPadding(new Insets(20));
        menuChoices.setPrefWidth(300);
        menuChoices.setTranslateY(200); // move down a bit from center for better layout

        StackPane menuLayout = new StackPane(menuChoices);
        menuLayout.setAlignment(Pos.CENTER);
        menuLayout.setPadding(new Insets(30));
        root.getChildren().add(menuLayout);
        
        Scene menuScene = new Scene(root, 610, 607);
        stage.setTitle("Car Wash Simulation");
        stage.setScene(menuScene);
        stage.show();

        // When Start button is clicked
        startBtn.setOnAction(e -> showInputDialog(root));
    }

    // ----------- Input Dialog -----------
    private void showInputDialog(StackPane root)
    {
        Label header = new Label("Simulation Settings");
        header.setFont(Font.font("Arial", 20));
        header.setTextFill(Color.WHITE);

        // waiting area slider
        Label waitingLabel = new Label("Waiting Area Capacity: 5");
        waitingLabel.setTextFill(Color.WHITE);
        waitingLabel.setFont(Font.font("Arial", 14));
        waitingLabel.setPrefWidth(220);

        Slider waitingSlider = new Slider(1, 10, 5); // (min, max, default)
        waitingSlider.setSnapToTicks(true);
        waitingSlider.setMajorTickUnit(5);
        waitingSlider.setMinorTickCount(4);
        waitingSlider.setBlockIncrement(1);
        waitingSlider.setMaxWidth(220);

        // label update
        waitingSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            waitingLabel.setText("Waiting Area Capacity: " + newVal.intValue());
        });

        // pumps slider
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

        // cars text field
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

        // Buttons
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
        
        // dialog update
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

        cancelBtn.setOnAction(e -> root.getChildren().remove(dialogBox));

        // start sim
        startSimBtn.setOnAction(
            e ->{
                int waiting = 5, pumps = 3, cars = 10;

                waiting = (int) waitingSlider.getValue();
                pumps = (int) pumpSlider.getValue();

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

                root.getChildren().remove(dialogBox);
                startSimulation(waiting, pumps, cars);
            }
        );
    }

    private void runSimulation(int waitingCapacity, int pumpCount, int carCount)
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
            for (int i = 0; i < carCount; i++) 
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

    private void startSimulation(int waitingCapacity, int pumpCount, int carCount)
    {
        // Create the simulation GUI inside the SAME stage
        gui = new CarWashGUI(waitingCapacity, pumpCount);

        Scene simScene = new Scene(gui, 800, 600);
        mainStage.setScene(simScene);

        new Thread(() -> runSimulation(waitingCapacity, pumpCount, carCount)).start();
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}