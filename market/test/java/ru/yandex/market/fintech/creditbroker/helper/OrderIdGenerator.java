package ru.yandex.market.fintech.creditbroker.helper;

import java.util.concurrent.atomic.AtomicInteger;

public class OrderIdGenerator {

    private static final AtomicInteger ID = new AtomicInteger();

    public static String getNext() {
        return "order-" + ID.incrementAndGet();
    }

}
