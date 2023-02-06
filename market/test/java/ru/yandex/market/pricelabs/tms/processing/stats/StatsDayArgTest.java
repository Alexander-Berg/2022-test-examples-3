package ru.yandex.market.pricelabs.tms.processing.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatsDayArgTest {
    @Test
    void testSerialization() {
        assertEquals("{\"value\":\"2019-12-01\"}", new StatsDayArg(null, "2019-12-01").toJsonString());
    }
}
