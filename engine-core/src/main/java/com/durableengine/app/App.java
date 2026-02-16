package com.durableengine.app;

import com.durableengine.engine.DatabaseHelper;
import com.durableengine.engine.WorkflowEngine;
import com.durableengine.examples.onboarding.OnboardingWorkflow;

public class App {
    public static void main(String[] args) {
        DatabaseHelper.initializeDatabase();
        
        WorkflowEngine engine = new WorkflowEngine("hire_bob_002");
        OnboardingWorkflow onboarding = new OnboardingWorkflow();

        // Change this to 'false' after your first run to prove it recovers!
        boolean simulateCrash = false; 

        System.out.println("\n--- Starting Employee Onboarding Workflow ---");
        if (simulateCrash) {
            System.out.println("Note: We are going to intentionally crash halfway through.");
        }

        onboarding.run(engine, "Bob The Builder", simulateCrash);
    }
}