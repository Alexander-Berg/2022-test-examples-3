package ru.yandex.market.sc.internal.util.les.builder;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.base.EventPayload;

/**
 * @author: dbryndin
 * @date: 3/21/22
 */
public class EventBuilder {


    /**
     * @see Event
     */
    @Builder
    @Getter
    public static class EventParams {

        @Builder.Default
        private String source = "lrm";

        @Builder.Default
        private String eventId = UUID.randomUUID().toString();

        @Builder.Default
        private Long timestamp = System.currentTimeMillis();

        private String eventType;

        private EventPayload payload;

        @Builder.Default
        private String description = "event";
    }
}
