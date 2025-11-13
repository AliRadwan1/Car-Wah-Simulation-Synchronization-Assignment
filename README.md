# 🚗 Car Wash Simulation

![Java](https://img.shields.io/badge/Java-25-blue?logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-25-8A2BE2?logo=javafx&logoColor=white)
![Course](https://img.shields.io/badge/CS241-Operating_Systems_1-orange)
![License](https://img.shields.io/badge/License-Academic-blueviolet)

---

## 📖 Overview
This project is a **Java 25 simulation** of a **Car Wash and Gas Station**, built to model the **Producer–Consumer Problem** using **custom semaphores and mutexes**.  
It demonstrates synchronization and safe concurrent access to shared resources in multithreaded environments.

- **Cars (Producers)** continuously arrive seeking service.  
- **Pumps (Consumers)** represent service bays working in parallel.  
- Synchronization ensures cars wait when the queue is full and pumps operate only when cars are available.

---

## 🧩 Repository Structure
```
Car-Wash-Simulation/
│
├── GUI/
│   └── ServiceStation.java        # JavaFX 25 GUI version with animations
│
├── GUI_TXT/
│   └── ServiceStation.java        # JavaFX 25 GUI version (text-based, no animations)
│
├── src/
│   └── ServiceStation.java        # Console version (terminal-based)
│
├── .gitignore
└── README.md
```

🧠 **Note:**  
Each version includes **only one file named `ServiceStation.java`** (as required by the assignment).  
All synchronization logic (Semaphores, Cars, Pumps) is contained in that file.

---

## 🧠 Concept & Objective
The project applies **Operating-Systems synchronization** principles—particularly the **Bounded Buffer pattern**—to simulate limited waiting space and concurrent servicing.

It ensures:
- Cars cannot enter a full waiting area.  
- Pumps only serve cars when available.  
- Only one thread modifies the queue at a time.  
- The number of serviced cars never exceeds the number of pumps.

---

## ⚙️ Implementation Details

### 🧱 `ServiceStation` (Main Controller)
Initializes shared resources (`Queue`, `Mutex`, `Empty`, `Full`, `Pumps` semaphores), reads user input, and launches producer and consumer threads.

### 🚗 `Car` (Producer)
- Represents an arriving car.  
- Waits if the queue is full (`Empty.wait()`).  
- Adds itself safely under mutex protection.  
- Signals a pump that a car is available (`Full.signal()`).

### ⛽ `Pump` (Consumer)
- Waits for available cars (`Full.wait()`) and bays (`Pumps.wait()`).  
- Removes a car, services it, and releases the bay.

### 🔒 `Semaphore`
Custom counting semaphore with `wait()` (P) and `signal()` (V), preventing race conditions and ensuring proper synchronization.

---

## 🖥️ Versions & Execution

### 🧾 Console Version — `src/ServiceStation.java`
Run from terminal:

```bash
cd src
javac ServiceStation.java
java ServiceStation
```

**Example Input**
```
Enter waiting area capacity: 5
Enter number of service bays: 3
```

**Example Output**
```
C1 arrived
C2 arrived
C3 arrived
Pump 1: C1 occupied
Pump 2: C2 occupied
Pump 3: C3 occupied
C4 arrived and waiting
C5 arrived and waiting
Pump 1: C1 finishes service
Pump 1: Bay 1 is now free
Pump 1: C4 begins service
Pump 2: C5 begins service
All cars processed; simulation ends
```

---

### 🎨 GUI Version — `GUI/ServiceStation.java`
- **JavaFX 25 animated** version showing cars moving between waiting areas and pumps.  
- Real-time visual updates for car movement and pump usage.

**Run**
1. Open the folder in VS Code or IntelliJ with JavaFX configured.  
2. Run `ServiceStation.java` inside the `GUI` folder.

---

### 💬 Text-Based GUI Version — `GUI_TXT/ServiceStation.java`
- Simplified **JavaFX 25** version with textual logs only (no animations).  
- Useful for lightweight visualization.

**Run**
1. Open in your IDE with JavaFX configured.  
2. Run `ServiceStation.java` inside the `GUI_TXT` folder.

---

## 🧭 Getting Started — JavaFX 25 Setup in VS Code

### 🪜 Step 1: Install Required Extensions
1. Open VS Code.  
2. Go to **Extensions (Ctrl + Shift + X)**.  
3. Install:
   - ✅ **Extension Pack for Java** (Microsoft)  
   - ✅ *Optional:* **JavaFX Support**

---

### 🪜 Step 2: Download JavaFX 25 SDK
1. Visit [https://openjfx.io](https://openjfx.io).  
2. Download **JavaFX 25 SDK** for your OS.  
3. Extract it, e.g.:
   ```
   C:\javafx-sdk-25\
   ```

---

### 🪜 Step 3: Configure VS Code Launch Settings
Add VM arguments to include JavaFX libraries.

#### For Windows
```
--module-path "C:\javafx-sdk-25\lib" --add-modules javafx.controls,javafx.fxml
```

#### For macOS/Linux
```
--module-path "/path/to/javafx-sdk-25/lib" --add-modules javafx.controls,javafx.fxml
```

#### Example `.vscode/launch.json`
```json
{
  "configurations": [
    {
      "type": "java",
      "name": "Run GUI (JavaFX 25)",
      "request": "launch",
      "mainClass": "ServiceStation",
      "vmArgs": "--module-path \"C:\\javafx-sdk-25\\lib\" --add-modules javafx.controls,javafx.fxml"
    }
  ]
}
```

---

### 🪜 Step 4: Run
- Open `GUI/ServiceStation.java` or `GUI_TXT/ServiceStation.java`.  
- Click **Run ▶️** in VS Code.  
- The JavaFX 25 window will launch showing the simulation.

---

## 🧰 Technologies Used
- **Java 25**
- **JavaFX 25**
- **Multithreading & Concurrency**
- **Custom Semaphores & Mutexes**
- **Producer–Consumer (Bounded Buffer)**

---

## 🧑‍💻 Contributors

| Name | Student ID |
|------|-------------|
| Ali Radwan Farouk | 20231110 |
| Adel Hefny | 20230198 |
| Ziad Salama | 20230150 |
| Mohamed Mahmoud | 20230354 |
| Asser Ahmed | 20230655 |

**Section:** AI S5  
**Course:** CS241 – Operating Systems 1  
**Faculty:** Computers and Artificial Intelligence, Cairo University

---

## 🏁 Acknowledgements
Developed for **CS241 Operating Systems (Synchronization Assignment)** under **TA Mena Asfour**.  
Demonstrates **thread synchronization** and **bounded-buffer concurrency** using **Java 25** and **JavaFX 25**.
