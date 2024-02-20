package ru.jsk0.tinkoff.impl;

import ru.jsk0.tinkoff.api.Client;
import ru.jsk0.tinkoff.api.Response;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.*;

public class RandomDelayedClient implements Client {
    private final Random random;

    public RandomDelayedClient(Random random) {
        this.random = random;
    }

    @Override
    public Response getApplicationStatus1(String id) {
        return simulateServiceResponseWithDelay(id);
    }

    @Override
    public Response getApplicationStatus2(String id) {
        return simulateServiceResponseWithDelay(id);
    }

    private Response simulateServiceResponseWithDelay(String id) {
        int delay = random.nextInt(500, 2000);

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Response.Failure(e);
        }

        return generateRandomResponse(id);
    }

    private Response generateRandomResponse(String id) {
        int responseType = random.nextInt(3);

        return switch (responseType) {
            case 0 -> new Response.Success("Success", id);
            case 1 -> new Response.RetryAfter(Duration.ofSeconds(random.nextInt(1, 5)));
            case 2 -> new Response.Failure(new RuntimeException("Failure " + id));
            default -> throw new IllegalStateException("Unexpected response type: " + responseType);
        };
    }
}
