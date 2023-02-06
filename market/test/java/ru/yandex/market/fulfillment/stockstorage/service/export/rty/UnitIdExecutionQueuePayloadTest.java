package ru.yandex.market.fulfillment.stockstorage.service.export.rty;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

public class UnitIdExecutionQueuePayloadTest {

    @Test
    public void getUuid() {
        UnitIdPayload unitIdPayload = new UnitIdPayload("To be, or not to be, that is the question",
                123456789L, 9999);

        UnitIdExecutionQueuePayload payload = new UnitIdExecutionQueuePayload(Set.of(unitIdPayload));
        assertEquals("7f13889ebe9550bcc61ef3f7ca1923bce2e30a29b1d3476f311b88fc6abbd3a2", payload.getUUID());
    }
}
