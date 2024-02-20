package ru.jsk0.tinkoff.impl;

import ru.jsk0.tinkoff.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DelayedClient implements Client {

    private final Random random = new Random();

    @Override
    public Event readData() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during read data", e);
        }

        List<Address> recipients = new ArrayList<>();
        recipients.add(new Address("d1", "n1"));
        recipients.add(new Address("D2", "n2"));
        Payload payload = new Payload("O1", new byte[]{4, 2});

        return new Event(recipients, payload);
    }

    @Override
    public Result sendData(Address dest, Payload payload) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during send data", e);
        }

        int result = random.nextInt(10);
        if (result < 8) {
            return Result.ACCEPTED;
        } else {
            return Result.REJECTED;
        }
    }

}
