package ru.yandex.market.checkout.common.util;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RetrierTest {

    private static final String EXCEPTION_MESSAGE_TEMPLATE = "Index too low";

    @Test
    public void catchExceptionDuringExecution() {
        AtomicInteger counter = new AtomicInteger(0);
        StringBuilder exceptionDuringExecution = new StringBuilder();

        try {
            Retrier.retry(() -> doAction(counter))
                    .onException(exception -> exceptionDuringExecution.append(exception.getMessage()))
                    .times(2)
                    .withDelay(10)
                    .execute();
        } catch (Exception e) {
            Assertions.fail("Unpredictable problem with Retrier", e);
        }

        Assertions.assertEquals(2, counter.get());
        Assertions.assertEquals(EXCEPTION_MESSAGE_TEMPLATE, exceptionDuringExecution.toString());
    }

    private void doAction(AtomicInteger counter) {
        counter.getAndIncrement();
        if (counter.get() == 1) {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE_TEMPLATE);
        }
    }
}
