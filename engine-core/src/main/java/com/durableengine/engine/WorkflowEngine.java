package com.durableengine.engine;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkflowEngine {
    
    private final String workflowId;
    // Tracks sequences PER STEP NAME to perfectly handle loops and parallel execution
    private final ConcurrentHashMap<String, AtomicInteger> stepCounts = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    
    // A lock to prevent SQLITE_BUSY errors during parallel database writes
    private static final Object dbLock = new Object();

    public WorkflowEngine(String workflowId) {
        this.workflowId = workflowId;
    }

    public <T> T step(String id, Callable<T> fn, Class<T> returnType) throws Exception {
        // Atomic thread-safe sequence generation
        int sequenceId = stepCounts.computeIfAbsent(id, k -> new AtomicInteger(0)).incrementAndGet();
        String stepKey = id + "_" + sequenceId;
        
        System.out.println("Executing Step: " + stepKey);

        // 1. THREAD-SAFE DB READ: Check if we already did this step
        synchronized (dbLock) {
            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT status, output FROM steps WHERE workflow_id = ? AND step_key = ?")) {
                
                stmt.setString(1, this.workflowId);
                stmt.setString(2, stepKey);
                
                ResultSet rs = stmt.executeQuery();
                if (rs.next() && "COMPLETED".equals(rs.getString("status"))) {
                    System.out.println("  -> Skipping! Recovered [" + stepKey + "] from DB.");
                    return mapper.readValue(rs.getString("output"), returnType);
                }
            }
        }

        // 2. PARALLEL EXECUTION: The actual work happens outside the lock!
        T result = fn.call();

        // 3. THREAD-SAFE DB WRITE: Checkpoint the result
        synchronized (dbLock) {
            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT OR REPLACE INTO steps (workflow_id, step_key, status, output) VALUES (?, ?, ?, ?)")) {
                
                stmt.setString(1, this.workflowId);
                stmt.setString(2, stepKey);
                stmt.setString(3, "COMPLETED");
                stmt.setString(4, mapper.writeValueAsString(result));
                
                stmt.executeUpdate();
                System.out.println("  -> Checkpointed [" + stepKey + "] to DB.");
            }
        }

        return result;
    }
}