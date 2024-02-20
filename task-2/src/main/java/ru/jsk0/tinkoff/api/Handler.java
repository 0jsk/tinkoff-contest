package ru.jsk0.tinkoff.api;

import java.time.Duration;

public interface Handler {
    Duration timeout();

    void performOperation();
}

