package ru.yandex.market.fintech.creditbroker.helper;

import java.util.concurrent.atomic.AtomicInteger;

public class PaymentIdGenerator {

    private static final AtomicInteger ID = new AtomicInteger();

    public static synchronized String getNext() {
        return "payment-" + ID.incrementAndGet();
    }
}
