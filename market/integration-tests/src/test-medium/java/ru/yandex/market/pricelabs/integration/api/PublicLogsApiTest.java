package ru.yandex.market.pricelabs.integration.api;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PublicLogsApiTest extends AbstractApiTests {

    @Test
    void shopLogsGetEmpty() {
        assertEquals(List.of(), getShopLogs());
    }

    @Test
    void jobLogsGetEmpty() {
        assertEquals(List.of(), getJobLogs(1));
    }
}
