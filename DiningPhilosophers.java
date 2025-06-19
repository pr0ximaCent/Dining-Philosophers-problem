import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class DiningPhilosophers {

    private static final int NUM_TABLES = 6;
    private static final int SEATS_PER_TABLE = 5;

    // forks[table][seat] is the semaphore for the fork to the philosopher's right;
    // his left fork is forks[table][(seat + SEATS_PER_TABLE - 1) % SEATS_PER_TABLE]
    private final Semaphore[][] forks = new Semaphore[NUM_TABLES][SEATS_PER_TABLE];

    // track whether a philosopher is sitting at [table][seat]
    private final AtomicBoolean[][] occupied = new AtomicBoolean[NUM_TABLES][SEATS_PER_TABLE];

    // all running philosophers
    private final List<Philosopher> philosophers = Collections.synchronizedList(new ArrayList<>());

    // last philosopher moved into table 6
    private final AtomicReference<Philosopher> lastMoved = new AtomicReference<>();

    // schedule deadlock checks once per second
    private final ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor();

    private final AtomicBoolean simulationEnded = new AtomicBoolean(false);

    public DiningPhilosophers() {
        for (int t = 0; t < NUM_TABLES; t++) {
            for (int s = 0; s < SEATS_PER_TABLE; s++) {
                forks[t][s] = new Semaphore(1);
                occupied[t][s] = new AtomicBoolean(false);
            }
        }
    }

    public void startDining() {
        // spawn philosophers at tables 0–4
        for (int t = 0; t < NUM_TABLES - 1; t++) {
            for (int s = 0; s < SEATS_PER_TABLE; s++) {
                char label = (char)('A' + t * SEATS_PER_TABLE + s);
                Philosopher p = new Philosopher(t, s, String.valueOf(label));
                philosophers.add(p);
                occupied[t][s].set(true);
                new Thread(p, "Philosopher-" + label).start();
            }
        }

        // start deadlock monitor
        monitor.scheduleAtFixedRate(this::checkAllTables, 1, 1, TimeUnit.SECONDS);
    }

    private void checkAllTables() {
        // skip if simulation already ended
        if (simulationEnded.get()) return;

        // check tables 0–4 for the first deadlock
        for (int t = 0; t < NUM_TABLES - 1; t++) {
            if (isDeadlocked(t)) {
                moveOnePhilosopher(t, NUM_TABLES - 1);
                break;  // only move one per cycle
            }
        }

        // if table 5 (index NUM_TABLES–1) deadlocks, we're done
        if (isDeadlocked(NUM_TABLES - 1)) {
            simulationEnded.set(true);
            restoreThreadsInterrupt();
            System.out.println("🛑 Simulation ended: table " + NUM_TABLES + " has deadlocked.");
            System.out.println("Last philosopher to move was: " + lastMoved.get().label);
            monitor.shutdown();
        }
    }

    // detect deadlock at a table:  
    // no one is eating, and every occupied philosopher holds exactly one fork
    private boolean isDeadlocked(int table) {
        int waitingOne = 0, holdingOne = 0, eating = 0;
        for (Philosopher p : philosophers) {
            if (p.table != table) continue;
            switch (p.state) {
                case EATING:          eating++; break;
                case WAITING_SECOND:
                case WAITING_FIRST:   waitingOne++; break;
                default:              /* THINKING or MOVING */ break;
            }
        }
        // deadlock = at least 1 waiting, no one eating, and count(waiting+eating) == occupied seats
        int occupiedSeats = (int) Arrays.stream(occupied[table]).filter(AtomicBoolean::get).count();
        return eating == 0 && waitingOne > 0 && waitingOne == occupiedSeats;
    }

    private void moveOnePhilosopher(int fromTable, int toTable) {
        // pick one waiting philosopher at random
        List<Philosopher> candidates = new ArrayList<>();
        for (Philosopher p : philosophers) {
            if (p.table == fromTable && (p.state == State.WAITING_FIRST || p.state == State.WAITING_SECOND)) {
                candidates.add(p);
            }
        }
        if (candidates.isEmpty()) return;
        Philosopher mover = candidates.get(new Random().nextInt(candidates.size()));
        occupied[fromTable][mover.seat].set(false);
        mover.moveToTable(toTable);
        occupied[toTable][mover.seat].set(true);
        lastMoved.set(mover);
        System.out.println("⏩ Philosopher " + mover.label + " moved from table " + (fromTable+1)
                           + " to table " + (toTable+1));
    }

    private void restoreThreadsInterrupt() {
        // politely interrupt all philosopher threads so they can exit
        for (Philosopher p : philosophers) {
            p.stop();
        }
    }

    public static void main(String[] args) {
        new DiningPhilosophers().startDining();
    }

    // Philosopher states
    private enum State { THINKING, WAITING_FIRST, WAITING_SECOND, EATING, MOVING }

    private class Philosopher implements Runnable {
        private volatile int table;
        private final int seat;
        private final String label;
        private volatile State state = State.THINKING;
        private volatile boolean running = true;
        private final Random rnd = new Random();

        Philosopher(int table, int seat, String label) {
            this.table = table;
            this.seat = seat;
            this.label = label;
        }

        void moveToTable(int newTable) {
            state = State.MOVING;
            table = newTable;
            // after moving, go back to THINKING so monitor can detect any new deadlock
            state = State.THINKING;
        }

        void stop() {
            running = false;
        }

        @Override
        public void run() {
            while (running && !simulationEnded.get()) {
                think();
                if (!running || simulationEnded.get()) break;
                tryEat();
            }
        }

        private void think() {
            state = State.THINKING;
            sleepRandom(200, 500);
        }

        private void tryEat() {
            state = State.WAITING_FIRST;
            Semaphore first = forks[table][seat];
            Semaphore second = forks[table][(seat + SEATS_PER_TABLE - 1) % SEATS_PER_TABLE];

            try {
                // pick up first fork
                if (!first.tryAcquire(300, TimeUnit.MILLISECONDS)) return;
                state = State.WAITING_SECOND;

                // pick up second fork
                if (!second.tryAcquire(300, TimeUnit.MILLISECONDS)) {
                    first.release();
                    return;
                }

                state = State.EATING;
                eat();

                // put down forks
                second.release();
                first.release();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void eat() {
            sleepRandom(200, 400);
        }

        private void sleepRandom(int minMs, int maxMs) {
            try {
                Thread.sleep(minMs + rnd.nextInt(maxMs - minMs));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
