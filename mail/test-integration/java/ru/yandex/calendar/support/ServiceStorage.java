package ru.yandex.calendar.support;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNullElseGet;

public class ServiceStorage {
    private final Map<String, Long> services = new HashMap<>();

    public void register(String name, long tvmId) {
        services.put(name, tvmId);
    }

    public long find(String name) {
        return requireNonNullElseGet(services.get(name), () -> { throw new RuntimeException("Service " + name + " not found"); });
    }
}
