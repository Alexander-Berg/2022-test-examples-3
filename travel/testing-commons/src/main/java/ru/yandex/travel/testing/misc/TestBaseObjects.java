package ru.yandex.travel.testing.misc;

import java.util.UUID;

import org.javamoney.moneta.Money;

public class TestBaseObjects {
    /**
     * Simplified static uuids in the following form:
     * 00000000-0000-0000-0000-0000...&lt;YOUR_NUMBER&gt;
     */
    public static UUID uuid(int value) {
        return UUID.fromString("0-0-0-0-" + value);
    }

    public static Money rub(Number value) {
        return Money.of(value, "RUB");
    }
}
