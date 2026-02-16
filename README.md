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

## 5. Prompts Log

The following table documents the interaction history with AI tooling used to build this project.

| Phase | Title | Formalized Prompt Description |
|------:|-------|-------------------------------|
| 1 | Initialization | Provided the assignment constraints for building a Native Durable Execution Engine. Requested an initial architectural strategy, technology stack recommendations, and a step-by-step implementation plan. |
| 2 | Environment Setup | Selected Java as the implementation language. Requested a comprehensive guide to initialize the local development environment, set up the Maven project structure, and configure version control with Git/GitHub. |
| 3 | Project Scaffolding | Confirmed successful completion of the initial Maven workspace setup and package creation. Requested the next steps for implementing the core database schema and dependencies. |
| 4 | DB Configuration | Clarified the target JDK version is Java 21. Requested a test implementation for the SQLite database connection and initialization to ensure the persistence layer is functioning correctly. |
| 5 | Troubleshooting Builds | Encountered classpath and compilation errors (package does not exist) while attempting to execute the multi-package Java application. Provided the error logs and requested troubleshooting assistance for build execution. |
| 6 | IDE Integration | Inquired about configuring VS Code to properly recognize the Maven project lifecycle, specifically seeking a way to build and run the application directly via the IDE UI rather than the command line. |
| 7 | Warning Resolution | Successfully generated the SQLite database file but encountered SLF4J binding warnings. Provided the console output to verify correct execution and requested guidance on resolving the logger warnings. |
| 8 | Core Engine Logic | Approved the core `WorkflowEngine` implementation. Requested a test harness within `App.java` to simulate side-effects and verify the engine’s memoization and durability characteristics upon application restart. |
| 9 | Concurrency | Confirmed the durability test successfully skipped previously executed steps. Requested the next phase of implementation, specifically addressing the concurrency requirements (parallel steps) and the employee onboarding workflow. |
| 10 | Final Verification | Successfully validated the parallel execution and fault-recovery simulation. Requested the final project deliverables, including required documentation, sequence tracking methodology, and explanations of thread-safety design decisions. |
| 11 | Documentation Update | Provided a new rubric image requiring specific README sections (Setup, Architecture, Design/Backpressure, Assumptions, Prompts) and requested an updated README excluding the architecture diagram. |
| 12 | Consolidation | Requested that all required sections, including the AI tooling prompts, be consolidated strictly into a single README file block. |
