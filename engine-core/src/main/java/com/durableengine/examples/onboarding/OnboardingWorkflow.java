package com.durableengine.examples.onboarding;

import com.durableengine.engine.WorkflowEngine;
import java.util.concurrent.CompletableFuture;

public class OnboardingWorkflow {

    public void run(WorkflowEngine engine, String employeeName, boolean simulateCrash) {
        try {
            // Step 1: Create Record (Sequential)
            String record = engine.step("create_record", () -> {
                System.out.println("    [Sequential] Creating HR record for " + employeeName + "...");
                Thread.sleep(1000);
                return "HR_Record_Created";
            }, String.class);

            // SIMULATING A CRASH AFTER STEP 1 
            if (simulateCrash) {
                System.err.println("!!! BOOM !!! Power loss simulated. Exiting process.");
                System.exit(1); 
            }

            // Step 2 & 3: Provision Laptop & Access (PARALLEL)
            CompletableFuture<String> laptopTask = CompletableFuture.supplyAsync(() -> {
                try {
                    return engine.step("provision_laptop", () -> {
                        System.out.println("    [Parallel] Ordering MacBook Pro...");
                        Thread.sleep(2000); // Takes 2 seconds
                        return "MacBook_Shipped";
                    }, String.class);
                } catch (Exception e) { throw new RuntimeException(e); }
            });

            CompletableFuture<String> accessTask = CompletableFuture.supplyAsync(() -> {
                try {
                    return engine.step("provision_access", () -> {
                        System.out.println("    [Parallel] Granting AWS and GitHub Access...");
                        Thread.sleep(1500); // Takes 1.5 seconds
                        return "Access_Granted";
                    }, String.class);
                } catch (Exception e) { throw new RuntimeException(e); }
            });

            // Wait for BOTH parallel steps to finish before moving on
            CompletableFuture.allOf(laptopTask, accessTask).join();
            System.out.println("    >> Both parallel provisioning tasks completed.");

            // Step 4: Send Welcome Email (Sequential)
            String email = engine.step("send_email", () -> {
                System.out.println("    [Sequential] Sending Welcome Email to " + employeeName + "...");
                Thread.sleep(500);
                return "Email_Sent";
            }, String.class);

            System.out.println("\n*** Employee Onboarding Complete! Final Status: " + email + " ***\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}