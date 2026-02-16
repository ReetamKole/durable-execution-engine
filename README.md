# Native Durable Execution Engine (Java)

A lightweight durable execution engine built in Java that allows standard code to survive process crashes and power losses. It supports sequential execution, parallel branching, and transparent state recovery without re-executing completed side effects.

---

## 1. Setup Instructions

### Prerequisites

- Java Development Kit (JDK): 21  
- Apache Maven: 3.6+

### Build and Run

Navigate to the core project directory:

```bash
cd engine-core
```

Compile and run the application using Maven:

```bash
mvn clean compile exec:java -Dexec.mainClass="com.durableengine.app.App"
```

---

## 2. Testing Durability (Crash Simulation)

The application includes a built-in crash simulator in `App.java`.

### First Run (Crash)

1. Ensure the following is set in `App.java`:

```java
boolean simulateCrash = true;
```

2. Run the application.

The engine will execute Step 1 and then intentionally crash/exit.

### Second Run (Recovery)

1. Change the variable in `App.java` to:

```java
boolean simulateCrash = false;
```

2. Run the application again.

### Expected Observation

- Step 1 is skipped instantly (its result is recovered from SQLite).
- The engine immediately begins parallel execution of Steps 2 and 3.

---

## 3. Design Decisions

### Concurrency Model

Parallel execution is implemented using Java’s `CompletableFuture`, enabling concurrent branching such as provisioning a laptop and granting access simultaneously.

**Why `CompletableFuture`**
- Native to Java and idiomatic for async computation
- Non-blocking and lightweight
- Supports structural concurrency patterns using `allOf(...)`

---

### Thread Safety and SQLite (Handling `SQLITE_BUSY`)

SQLite is a single-file database and can encounter locking issues when multiple threads write simultaneously. To ensure safe concurrent execution, the engine uses two strategies.

#### WAL Mode

The database is initialized using:

```sql
PRAGMA journal_mode=WAL;
```

This improves concurrency by allowing multiple readers and one writer.

#### Application-Level Locking

A `synchronized(dbLock)` section is used for:

- Memoization reads
- Checkpoint writes

**Why this works**
- The heavy work of each step (API calls, computations) runs in parallel
- Database operations are short and serialized
- Prevents `SQLITE_BUSY` errors entirely

---

### Backpressure Handling

Backpressure is handled through structural concurrency.

By using:

```java
CompletableFuture.allOf(task1, task2).join();
```

the workflow naturally pauses until all tasks in the parallel batch complete. This prevents:

- Unlimited task spawning
- Thread pool exhaustion
- System resource overload

---

### Sequence Tracking (Bonus)

To support repeated invocations of the same step ID (e.g., calling `send_email` multiple times), the engine implements automatic sequence generation using:

- `ConcurrentHashMap<String, AtomicInteger>`

Each invocation increments a counter for that step ID and produces unique internal keys:

- `send_email_1`
- `send_email_2`

This keeps the user-facing API clean while ensuring database uniqueness.

---

## 4. Assumptions

### Data Serialization

All step return values are assumed to be standard Java objects that can be serialized/deserialized using Jackson’s `ObjectMapper`.

- Complex object graphs with circular dependencies are not supported.

---

### Idempotency (Zombie Steps)

External systems called by steps are assumed to be idempotent.

**Scenario**  
If the process crashes after an external side-effect occurs (e.g., an email is sent), but before the engine writes `COMPLETED` to SQLite, the step will execute again after restart.

**Guarantee**
- The engine provides **At-Least-Once** execution.
- Exactly-once must be enforced by external services, typically using unique request IDs.

---

### Environment

The local environment must have write permissions to the project directory in order to create the durable database file:

- `durable_engine.db`

---

