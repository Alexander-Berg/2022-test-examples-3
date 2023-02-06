package ru.yandex.market.delivery.deliveryintegrationtests.tool;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class UniqueId {

    private static AtomicInteger counter = new AtomicInteger(0);

    private UniqueId() { }

    public static String getString() {
        return String.valueOf(get());
    }

    public static long get() { return System.currentTimeMillis() * 1000 + counter.incrementAndGet(); }

    public static String getStringUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
