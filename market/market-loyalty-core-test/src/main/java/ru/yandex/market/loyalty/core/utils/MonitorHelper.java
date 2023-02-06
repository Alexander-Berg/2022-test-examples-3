package ru.yandex.market.loyalty.core.utils;

import ru.yandex.market.monitoring.ComplicatedMonitoring;
import ru.yandex.market.monitoring.MonitoringStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MonitorHelper {
    public static void assertMonitor(MonitoringStatus expected, ComplicatedMonitoring.Result result) {
        assertEquals(result.getMessage(), expected, result.getStatus());
    }

    public static void assertMonitor(MonitoringStatus expected, String expectedMessage,
                                     ComplicatedMonitoring.Result result) {
        assertEquals(result.getMessage(), expected, result.getStatus());
        assertTrue(result.getMessage(), result.getMessage().contains(expectedMessage));
    }
}
