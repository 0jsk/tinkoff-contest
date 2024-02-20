package ru.jsk0.tinkoff;

import ru.jsk0.tinkoff.api.ApplicationStatusResponse;
import ru.jsk0.tinkoff.impl.ApplicationStatusHandler;
import ru.jsk0.tinkoff.impl.RandomDelayedClient;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Main {
    public static void main(String[] args) {
        RandomDelayedClient client = new RandomDelayedClient(ThreadLocalRandom.current());
        ApplicationStatusHandler handler = new ApplicationStatusHandler(client);

        for (int i = 0; i < 3; i++) {
            String applicationId = "app" + i;
            ApplicationStatusResponse response = handler.performOperation(applicationId);

            if (response instanceof ApplicationStatusResponse.Success successResponse) {
                System.out.println("Success: " + successResponse.id() + " - " + successResponse.status());
            } else if (response instanceof ApplicationStatusResponse.Failure failureResponse) {
                System.out.println(
                    "[" + applicationId + "] " +
                    "Failure: " + failureResponse.retriesCount() + " retries, last attempt was " + failureResponse.lastRequestTime().toMillis() + "ms ago");
            }
        }
    }
}
