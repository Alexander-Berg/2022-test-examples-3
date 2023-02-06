package ru.yandex.market.fulfillment.stockstorage.util;

import java.util.Arrays;
import java.util.Collection;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.Assert.assertEquals;

public class ExecutionDelayCalculatorTest {

    private final ExecutionDelayCalculator executionDelayCalculator = new ExecutionDelayCalculator(
            ImmutableMap.of(0, 15,
                    15, 30,
                    30, 60,
                    60, 180,
                    120, 300
            )
    );

    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][]{
                        {0, 15},
                        {1, 15},
                        {10, 15},
                        {20, 30},
                        {30, 60},
                        {40, 60},
                        {50, 60},
                        {59, 60},
                        {50, 60},
                        {60, 180},
                        {110, 180},
                        {120, 300},
                        {130, 300},
                        {270, 300},
                }
        );
    }

    @MethodSource("data")
    @ParameterizedTest
    public void delayCalculation(int attemptNumber, int expectedDelay) {
        int actualDelay = executionDelayCalculator.calculateDelayInSeconds(attemptNumber);

        assertEquals(
                "Asserting that actual delay is equal to expected delay",
                expectedDelay,
                actualDelay
        );
    }
}
