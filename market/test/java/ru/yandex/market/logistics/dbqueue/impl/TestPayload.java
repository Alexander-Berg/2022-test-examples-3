package ru.yandex.market.logistics.dbqueue.impl;

import java.util.List;

public class TestPayload implements DbQueueTaskPayloadInterface {
    private final List<Long> bookingIds;

    public TestPayload(List<Long> bookingIds) {
        this.bookingIds = bookingIds;
    }

    public List<Long> getBookingIds() {
        return bookingIds;
    }
}
