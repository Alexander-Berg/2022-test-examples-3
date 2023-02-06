package ru.yandex.market.sc.api.resttest.infra;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import lombok.Value;

/**
 * @author valter
 */
public class RestTestContext {

    private static final AtomicReference<Values> VALUES = new AtomicReference<>();

    public static Values get() {
        return VALUES.get();
    }

    public static void set(Values context) {
        VALUES.set(context);
    }

    @Value
    public static class Values {

        Clock clock;
        long sortingCenterId;
        long stockmanUid;

        @Nullable
        String stockmanToken;

    }

}
