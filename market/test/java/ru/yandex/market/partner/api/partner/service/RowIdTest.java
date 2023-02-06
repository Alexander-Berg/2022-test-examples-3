package ru.yandex.market.partner.api.partner.service;

import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ru.yandex.market.partner.api.partner.RowId;

import static org.junit.jupiter.api.Assertions.*;

class RowIdTest {

    @Test
    void parseRowId() {
        Random random = new Random();
        Long partnerId = random.nextLong();
        Long requestDate = random.nextLong();
        String traceId = UUID.randomUUID().toString();

        RowId rowId = new RowId(partnerId, requestDate, traceId);

        assertEquals(partnerId, rowId.getPartnerId());
        assertEquals(requestDate, rowId.getRequestDate());
        assertEquals(traceId, rowId.getTraceId());
    }

    @Test
    void parseRowId_withNullPartnerId() {
        Random random = new Random();
        Long requestDate = random.nextLong();
        String traceId = UUID.randomUUID().toString();

        RowId rowId = new RowId(null, requestDate, traceId);

        assertNull(rowId.getPartnerId());
        assertEquals(requestDate, rowId.getRequestDate());
        assertEquals(traceId, rowId.getTraceId());
    }

    @Test
    void parseRowId_withNullRequestDate() {
        Random random = new Random();
        Long partnerId = random.nextLong();
        String traceId = UUID.randomUUID().toString();

        RowId rowId = new RowId(partnerId, null, traceId);

        assertEquals(partnerId, rowId.getPartnerId());
        assertNull(rowId.getRequestDate());
        assertEquals(traceId, rowId.getTraceId());
    }

    @Test
    void parseRowId_withEmptyTraceId() {
        Random random = new Random();
        Long partnerId = random.nextLong();
        Long requestDate = random.nextLong();

        RowId rowId = new RowId(partnerId, requestDate, "");

        assertEquals(partnerId, rowId.getPartnerId());
        assertEquals(requestDate, rowId.getRequestDate());
        assertEquals("", rowId.getTraceId());
    }
}
