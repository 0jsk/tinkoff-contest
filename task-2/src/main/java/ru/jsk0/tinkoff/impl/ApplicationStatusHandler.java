package ru.jsk0.tinkoff.impl;

import ru.jsk0.tinkoff.api.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class ApplicationStatusHandler implements Handler {
    private static final Logger logger = Logger.getLogger(ApplicationStatusHandler.class.getName());

    private static final int TIMEOUT_DURATION_MILLIS = 500;
    private static final int MAX_RETIRES_FOR_RECIPIENT = 3;

    private static final int QUEUE_CAPACITY = 1_024;
    private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final ExecutorService consumerExecutorService;
    private final ExecutorService processExecutorService;

    private final Client client;

    private final AtomicInteger taskCompletedCount = new AtomicInteger(0);

    public ApplicationStatusHandler(Client client, int consumerThreads, int processThreads) {
        this.consumerExecutorService = Executors.newFixedThreadPool(consumerThreads);
        this.processExecutorService = Executors.newFixedThreadPool(processThreads);
        this.client = client;

        runEventProducer();
        runEventConsumers(consumerThreads);
    }

    @Override
    public Duration timeout() {
        return Duration.ofMillis(TIMEOUT_DURATION_MILLIS);
    }

    public AtomicInteger getTaskCompletedCount() {
        return taskCompletedCount;
    }

    @Override
    public void performOperation() {
    }

    private void runEventProducer() {
        Thread producer = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Event event = client.readData();
                    eventQueue.put(event);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.severe("Event producer was interrupted");
                }
            }
        });

        producer.start();
    }

    private void runEventConsumers(int consumers) {
        for (int i = 0; i < consumers; i += 1) {
            consumerExecutorService.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Event event = eventQueue.take();
                        processEvent(event);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.severe("Event consumer was interrupted");
                    }
                }
            });
        }
    }

    private void processEvent(Event event) {
        for (Address recipient : event.recipients()) {
            CompletableFuture.runAsync(() -> sendData(recipient, event.payload()), processExecutorService);
        }
    }

    private void sendData(Address recipient, Payload payload) {
        for (int i = 0; i < MAX_RETIRES_FOR_RECIPIENT; i += 1) {
            Result result = client.sendData(recipient, payload);

            if (result == Result.ACCEPTED) {
                taskCompletedCount.incrementAndGet();
                return;
            }

            try {
                TimeUnit.MILLISECONDS.sleep(timeout().toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.severe("Retry was interrupted");

                return;
            }
        }

        logger.severe("Max retries reached for recipient: " + recipient);
    }


    private void shutdownAndAwaitTermination(ExecutorService pool, long timeout, TimeUnit unit) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(timeout, unit)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        shutdownAndAwaitTermination(consumerExecutorService, 5, TimeUnit.SECONDS);
        shutdownAndAwaitTermination(processExecutorService, 5, TimeUnit.SECONDS);

        logger.info("Processing stopped, task completed: " + taskCompletedCount.get());
    }
}
