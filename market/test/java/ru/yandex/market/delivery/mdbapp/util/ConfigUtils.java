package ru.yandex.market.delivery.mdbapp.util;

import ru.yandex.market.delivery.mdbapp.configuration.SenderConfiguration;

public final class ConfigUtils {

    private ConfigUtils() {
        throw new AssertionError();
    }

    public static SenderConfiguration getSenderConfiguration() {
        return new SenderConfiguration().setName("Беру");
    }
}
