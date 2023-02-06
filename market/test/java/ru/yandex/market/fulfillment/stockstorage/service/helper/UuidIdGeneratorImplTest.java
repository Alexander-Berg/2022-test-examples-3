package ru.yandex.market.fulfillment.stockstorage.service.helper;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class UuidIdGeneratorImplTest {

    private UuidIdGeneratorImpl subject = new UuidIdGeneratorImpl();

    @Test
    void getReturnsUuid() {
        String result = subject.get();

        UUID.fromString(result);
    }

}
