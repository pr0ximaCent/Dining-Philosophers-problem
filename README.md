# Extended Dining Philosophers Simulation

This Java project simulates an extended version of the classic Dining Philosophers problem using semaphores and multithreading. Philosophers sit at up to six circular tables, each requiring two forks (semaphores) to eat. Whenever a deadlock is detected at tables 1–5, one waiting philosopher is moved to the sixth table. The simulation terminates when the sixth table deadlocks, printing the last moved philosopher.

---

## Features

* **Semaphore-based forks**: Each fork is represented by a `Semaphore(1)`.
* **Deadlock detection**: A monitor thread checks for deadlocks every second.
* **Dynamic philosopher movement**: On deadlock, one philosopher is moved to the sixth table.
* **Graceful shutdown**: The simulation ends cleanly when table six deadlocks, interrupting all threads.

---

## Requirements

* Java 8 or higher
* No external dependencies (uses `java.util.concurrent`)

---

## Project Structure

```
src/
└── DiningPhilosophers.java   # Main simulation code
README.md                     # This documentation file
```

---

## Compilation & Running

1. **Compile**

   ```bash
   javac DiningPhilosophers.java
   ```

2. **Run**

   ```bash
   java DiningPhilosophers
   ```

During execution, you will see console output indicating when philosophers are moved and when the simulation ends:

```
⏩ Philosopher C moved from table 2 to table 6
🛑 Simulation ended: table 6 has deadlocked.
Last philosopher to move was: C
```

---

## Configuration

The following constants in `DiningPhilosophers.java` can be adjusted to tune the simulation:

```java
private static final int NUM_TABLES = 6;         // Total tables
private static final int SEATS_PER_TABLE = 5;    // Philosophers per table

// Deadlock monitor interval (seconds)
monitor.scheduleAtFixedRate(this::checkAllTables, 1, 1, TimeUnit.SECONDS);

// Philosopher think/eat timeouts (milliseconds)
sleepRandom(200, 500);  // thinking
eat: sleepRandom(200, 400);

// Fork acquisition timeout
first.tryAcquire(300, TimeUnit.MILLISECONDS);
```

Feel free to modify these values to increase tables, change timeouts, or alter check frequency.

---

## How It Works

1. **Initialization**: Creates semaphores for each fork and flags for occupied seats.
2. **Philosopher Threads**: Spawns philosophers at tables 1–5, each repeatedly thinking and attempting to eat.
3. **Fork Acquisition**: Each philosopher acquires their right fork, then left fork with timeouts to avoid blocking indefinitely.
4. **Deadlock Monitor**: Periodically scans each table for a state where every seated philosopher holds one fork and none are eating.
5. **Movement**: On detecting deadlock at tables 1–5, moves a random waiting philosopher to table 6.
6. **Termination**: When table 6 deadlocks, interrupts all threads and prints the last moved philosopher.

---

## Extending the Simulation

* **More Tables/Philosophers**: Adjust `NUM_TABLES` and `SEATS_PER_TABLE`.
* **Alternate Strategies**: Replace timeouts with ordered fork-picking or resource hierarchies.
* **Logging**: Integrate a logging framework (e.g., SLF4J) for more detailed traces.

---

## Author

Neon Bytes Workspace Simulation Module

---

## License

This project is licensed under the MIT License. Feel free to use and modify for educational or research purposes.
