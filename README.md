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

