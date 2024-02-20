package ru.jsk0.tinkoff;

import ru.jsk0.tinkoff.api.Client;
import ru.jsk0.tinkoff.impl.ApplicationStatusHandler;
import ru.jsk0.tinkoff.impl.DelayedClient;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        Client client = new DelayedClient();

//        int threads = Runtime.getRuntime().availableProcessors();

        ApplicationStatusHandler handler = new ApplicationStatusHandler(client, 4,24);

        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Timer thread was interrupted");
            }

            handler.shutdown();
            System.out.println("Processing stopped, task completed: " + handler.getTaskCompletedCount());
        }).start();
    }
}

