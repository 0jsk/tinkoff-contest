package ru.jsk0.tinkoff.impl;

import ru.jsk0.tinkoff.api.ApplicationStatusResponse;
import ru.jsk0.tinkoff.api.Client;
import ru.jsk0.tinkoff.api.Handler;
import ru.jsk0.tinkoff.api.Response;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Logger;

public class ApplicationStatusHandler implements Handler {
    private static final Logger logger = Logger.getLogger(ApplicationStatusHandler.class.getName());

    private static final Integer PERFORM_ACTION_TIMEOUT = 15;

    private final Client client;

    public ApplicationStatusHandler(Client client) {
        this.client = client;
    }

    @Override
    public ApplicationStatusResponse performOperation(String id) {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        AtomicInteger retries = new AtomicInteger(0);

        CompletableFuture<ApplicationStatusResponse> future1 = CompletableFuture.supplyAsync(() -> makeRequest(client::getApplicationStatus1, id, retries), executor);
        CompletableFuture<ApplicationStatusResponse> future2 = CompletableFuture.supplyAsync(() -> makeRequest(client::getApplicationStatus2, id, retries), executor);

        CompletableFuture<ApplicationStatusResponse> combinedFuture = CompletableFuture.anyOf(future1, future2)
            .thenApply(response -> (ApplicationStatusResponse) response);

        try {
            return combinedFuture.get(PERFORM_ACTION_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException e) {
            logger.severe("Caught exception while doing requests, returning an empty Failure status.");
        } catch (TimeoutException e) {
            logger.severe("Operation timed out after " + PERFORM_ACTION_TIMEOUT + " seconds.");
        } finally {
            executor.shutdownNow();
        }

        return new ApplicationStatusResponse.Failure(Duration.ZERO, -1);
    }

    private ApplicationStatusResponse makeRequest(Function<String, Response> call, String id, AtomicInteger retries) {
        Instant lastRequestTime;

        /*
          В задании отсутствует условие о прекращении работы вызовов при достижении определенного кол-ва попыток,
          поэтому расчитываем только на PERFORM_ACTION_TIMEOUT
         */
        while (true) {
            retries.incrementAndGet();

            lastRequestTime = Instant.now();
            Response response = call.apply(id);

            if (response instanceof Response.Success success) {
                logger.info("Request for ID " + id + " succeeded.");

                return new ApplicationStatusResponse.Success(success.applicationId(), success.applicationStatus());
            } else if (response instanceof Response.Failure failure) {
                logger.severe("Request for ID " + id + " failed: " + failure.ex().getMessage());

                return new ApplicationStatusResponse.Failure(
                    durationBetweenNow(lastRequestTime),
                    retries.get()
                );
            } else if (response instanceof Response.RetryAfter retryAfter) {
                logger.info("Retrying request for ID " + id + " after " + retryAfter.delay().toMillis() + "ms.");

                try {
                    TimeUnit.MILLISECONDS.sleep(retryAfter.delay().toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.severe("Interrupted during retry delay for ID " + id + ", aborting retries.");

                    break;
                }
            } else {
                logger.severe("Unexpected response type for ID " + id + ", aborting retries.");

                break;
            }
        }

        return new ApplicationStatusResponse.Failure(
            durationBetweenNow(lastRequestTime),
            retries.get()
        );
    }

    private Duration durationBetweenNow(Instant time) {
        return Duration.between(time, Instant.now());
    }
}
