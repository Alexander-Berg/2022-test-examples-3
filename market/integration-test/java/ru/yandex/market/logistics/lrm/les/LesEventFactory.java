package ru.yandex.market.logistics.lrm.les;

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.base.EventPayload;
import ru.yandex.market.logistics.les.boxbot.CodeEvent;
import ru.yandex.market.logistics.lrm.queue.payload.LesEventPayload;

import static ru.yandex.market.logistics.lrm.config.LrmTestConfiguration.TEST_REQUEST_ID;

@ParametersAreNonnullByDefault
public final class LesEventFactory {
    private static final Instant TIMESTAMP = Instant.parse("2021-09-06T11:12:13.00Z");

    private LesEventFactory() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static LesEventPayload getDbQueuePayload(EventPayload lesEventPayload) {
        return LesEventPayload
            .builder()
            .requestId(TEST_REQUEST_ID + "/1")
            .event(new Event(
                "lom",
                "event_id_3",
                TIMESTAMP.toEpochMilli(),
                CodeEvent.EVENT_TYPE_NEW_CODE,
                lesEventPayload,
                "Тест"
            ))
            .build();
    }
}
