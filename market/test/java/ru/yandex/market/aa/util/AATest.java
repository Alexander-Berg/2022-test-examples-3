package ru.yandex.market.aa.util;

import java.time.Duration;

/**
 * @author antipov93.
 */
public class AATest {

    private static final int DEFAULT_TIME_LIMIT_MILLIS = 1000;

    protected Duration timeLimit() {
        return Duration.ofMillis(DEFAULT_TIME_LIMIT_MILLIS);
    }
}
