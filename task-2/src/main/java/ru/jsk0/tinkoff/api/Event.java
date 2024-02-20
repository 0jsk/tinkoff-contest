package ru.jsk0.tinkoff.api;

import java.util.List;

public record Event(List<Address> recipients, Payload payload) {}

