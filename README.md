Native Durable Execution Engine (Java)

A lightweight, durable execution engine built in Java that allows standard code to survive process crashes and power losses. It natively supports sequential execution, parallel branching, and transparent state recovery without re-executing completed side effects.

1. Setup Instructions

Prerequisites

Java Development Kit (JDK): Version 21

Apache Maven: 3.6+

How to Build and Run

Navigate to the core project directory:

cd engine-core


Compile and run the application using Maven:

mvn clean compile exec:java -Dexec.mainClass="com.durableengine.app.App"


Testing Durability (Simulation)

The application includes a built-in crash simulator in App.java.

First Run (Crash): Ensure boolean simulateCrash = true; is set in App.java. Run the app. It will execute Step 1 and then intentionally crash/exit.

Second Run (Recovery): Change the variable to boolean simulateCrash = false; in App.java. Run the app again.

Observation: You will see the engine instantly skip Step 1 (recovering the result from SQLite) and immediately begin the parallel execution of Steps 2 and 3.

2. Design Decisions

Concurrency Model

We utilized Java's CompletableFuture framework to handle parallel execution (e.g., provisioning a laptop and granting access simultaneously).

Reasoning: CompletableFuture is native to Java, non-blocking, and allows for clean structural concurrency (waiting for multiple tasks with allOf). It avoids the overhead of external thread management libraries while keeping the API idiomatic.

Thread Safety & RDBMS (Handling SQLITE_BUSY)

SQLite is a single-file database that struggles with concurrent writes. To handle parallel steps without locking errors:

WAL Mode: The database is initialized with PRAGMA journal_mode=WAL; (Write-Ahead Logging), allowing simultaneous readers and one writer.

Application-Level Locking: We implemented a synchronized(dbLock) block specifically for the Memoization (Read) and Checkpoint (Write) phases.

Why: This ensures that while the "heavy lifting" of a step (API calls, computations) runs in parallel on multiple threads, the split-second database interactions are serialized. This prevents SQLITE_BUSY errors completely.

Handling Backpressure

We manage backpressure via Structural Concurrency. By using CompletableFuture.allOf(task1, task2).join(), the main workflow thread pauses until the current batch of parallel tasks is complete. This acts as a natural barrier, preventing the engine from spawning an infinite number of threads or overwhelming the system resources.

Sequence Tracking (Bonus)

To handle loops and reuse step IDs (e.g., calling send_email multiple times), we implemented Automatic Sequence Generation using ConcurrentHashMap<String, AtomicInteger>.

Every time a step is invoked, we atomically increment a counter for that specific step ID.

This generates unique keys (e.g., send_email_1, send_email_2) internally, keeping the user API clean while ensuring database uniqueness.

3. Assumptions

Data Serialization: We assume that all step return values are standard Java objects that can be serialized/deserialized via Jackson's ObjectMapper. Complex circular dependencies in objects are not supported.

Idempotency (Zombie Steps): We assume the external systems called by the steps are idempotent.

Scenario: If the process crashes after an API call sends an email but before the engine writes "COMPLETED" to the DB, the step will run again on restart (Zombie Step).

Mitigation: The engine guarantees "At-Least-Once" execution. "Exactly-Once" must be handled by the external service (e.g., passing a unique request ID).

Environment: We assume the local environment has write access to the project directory to create the durable_engine.db file.

4. Prompts Log

The following table documents the interaction history with AI tooling used to build this project.

Phase

Formalized Prompt Description

1. Initialization

Provided the assignment constraints for building a Native Durable Execution Engine. Requested an initial architectural strategy, technology stack recommendations, and a step-by-step implementation plan.

2. Environment Setup

Selected Java as the implementation language. Requested a comprehensive guide to initialize the local development environment, set up the Maven project structure, and configure version control with Git/GitHub.

3. Project Scaffolding

Confirmed successful completion of the initial Maven workspace setup and package creation. Requested the next steps for implementing the core database schema and dependencies.

4. DB Configuration

Clarified the target JDK version is Java 21. Requested a test implementation for the SQLite Database connection and initialization to ensure the persistence layer is functioning correctly.

5. Troubleshooting Builds

Encountered classpath and compilation errors (package does not exist) while attempting to execute the multi-package Java application. Provided the error logs and requested troubleshooting assistance for the build execution.

6. IDE Integration

Inquired about configuring the VS Code IDE to properly recognize the Maven project lifecycle, specifically seeking a way to build and run the application directly via the IDE UI rather than the command line.

7. Warning Resolution

Successfully generated the SQLite database file but encountered SLF4J binding warnings. Provided the console output to verify correct execution and requested guidance on resolving the logger warnings.

8. Core Engine Logic

Approved the core WorkflowEngine implementation. Requested a test harness within App.java to simulate side-effects and verify the engine's memoization and durability characteristics upon application restart.

9. Concurrency

Confirmed the durability test successfully skipped previously executed steps. Requested the next phase of implementation, specifically addressing the concurrency requirements (parallel steps) and the employee onboarding workflow.

10. Final Verification

Successfully validated the parallel execution and fault-recovery simulation. Requested the final project deliverables, including required documentation, sequence tracking methodology, and explanations of thread-safety design decisions.

11. Documentation Update

Provided a new rubric image requiring specific README sections (Setup, Architecture, Design/Backpressure, Assumptions, Prompts) and requested an updated README excluding the architecture diagram.

12. Consolidation

Requested that all required sections, including the AI tooling prompts, be consolidated strictly into a single README file block.