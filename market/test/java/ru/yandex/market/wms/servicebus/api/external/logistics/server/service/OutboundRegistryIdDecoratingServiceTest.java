package ru.yandex.market.wms.servicebus.api.external.logistics.server.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutboundRegistryIdDecoratingServiceTest {

    private OutboundRegistryIdDecoratingService decorator = new OutboundRegistryIdDecoratingService();

    @Test
    void wrap() {
        assertEquals("test_key-plan", decorator.wrap("test_key"));
    }

    @Test
    void unwrap() {
        assertEquals("test_key", decorator.unwrap("test_key-plan"));
    }
}
