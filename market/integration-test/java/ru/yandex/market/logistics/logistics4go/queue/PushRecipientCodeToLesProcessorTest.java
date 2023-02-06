package ru.yandex.market.logistics.logistics4go.queue;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.les.logistics4go.RecipientCode;
import ru.yandex.market.logistics.logistics4go.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4go.queue.payload.RecipientCodePayload;
import ru.yandex.market.logistics.logistics4go.queue.processor.PushRecipientCodeToLesProcessor;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Пуш событий заказов в LES")
public class PushRecipientCodeToLesProcessorTest extends AbstractIntegrationTest {
    private static final Instant FIXED_TIME = Instant.parse("2022-01-02T03:04:05Z");

    @Autowired
    private PushRecipientCodeToLesProcessor processor;

    @Autowired
    private LesProducer lesProducer;

    @BeforeEach
    void setUp() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lesProducer);
    }

    @Test
    @DisplayName("Успешный пуш")
    void success() {
        RecipientCodePayload payload = RecipientCodePayload.builder()
            .requestId("test-request-id/1")
            .eventId(10)
            .externalId("aa872c9c-afd4-4bf2-a8d9-df8b84852c04")
            .lomId(1L)
            .created(FIXED_TIME)
            .code("9999")
            .build();
        softly.assertThat(processor.execute(payload))
            .isEqualTo(TaskExecutionResult.finish());

        Event lesEvent = new Event(
            "logistics4go",
            "10",
            FIXED_TIME.toEpochMilli(),
            "GO_ORDER_RECIPIENT_CODE",
            new RecipientCode(
                "aa872c9c-afd4-4bf2-a8d9-df8b84852c04",
                1L,
                FIXED_TIME,
                "9999"
            ),
            ""
        );
        verify(lesProducer).send(lesEvent, "logistics4go_out");
    }
}
