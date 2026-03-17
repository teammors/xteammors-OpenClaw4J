package com.xteammors.openclaw;

import com.xteammors.openclaw.manager.AgentManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

@SpringBootApplication
public class OpenClaw4JApplication implements CommandLineRunner, ApplicationListener<ContextClosedEvent> {




    @Autowired
    AgentManager agentManager;



    public static void main(String[] args) {
        try {
            SpringApplication.run(OpenClaw4JApplication.class, args);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {

    }

    @Override
    public boolean supportsAsyncExecution() {
        return ApplicationListener.super.supportsAsyncExecution();
    }

    @Override
    public void run(String... args) throws Exception {
        agentManager.startAgent();
    }


}
