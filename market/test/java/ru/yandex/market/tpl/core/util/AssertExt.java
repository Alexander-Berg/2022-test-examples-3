package ru.yandex.market.tpl.core.util;

import lombok.SneakyThrows;
import org.opentest4j.AssertionFailedError;

public class AssertExt {

    @SneakyThrows
    public static void awaitAssert(Runnable condition, int seconds) {
        Throwable lastThrowable = null;
        for (int count = 1; count < seconds * 20; count++) {
            lastThrowable = null;
            try {
                condition.run();
            } catch (AssertionFailedError e) {
                lastThrowable = e;
            }

            if (lastThrowable == null) {
                break;
            }
            Thread.sleep(50);
        }

        if (lastThrowable != null) {
            throw new AssertionError(lastThrowable.getMessage());
        }
    }

}
