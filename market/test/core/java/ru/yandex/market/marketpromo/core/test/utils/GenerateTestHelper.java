package ru.yandex.market.marketpromo.core.test.utils;

import java.util.UUID;

import javax.annotation.Nonnull;

public final class GenerateTestHelper {

    private GenerateTestHelper() {
    }

    @Nonnull
    public static String someString() {
        return UUID.randomUUID().toString();
    }
}
