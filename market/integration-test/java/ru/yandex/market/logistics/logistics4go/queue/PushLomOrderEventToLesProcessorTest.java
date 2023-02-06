package ru.yandex.market.logistics.logistics4go.queue;

import java.time.Instant;

import javax.annotation.Nonnull;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.JmsException;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.les.logistics4go.GoOrderSnapshot;
import ru.yandex.market.logistics.logistics4go.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4go.api.model.GetOrderResponse;
import ru.yandex.market.logistics.logistics4go.api.model.OrderStatus;
import ru.yandex.market.logistics.logistics4go.queue.payload.PushLomOrderEventToLesPayload;
import ru.yandex.market.logistics.logistics4go.queue.processor.PushLomOrderEventToLesProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Пуш событий заказов в LES")
public class PushLomOrderEventToLesProcessorTest extends AbstractIntegrationTest {

    private static final GetOrderResponse SNAPSHOT_OBJECT = new GetOrderResponse()
        .id(10L)
        .externalId("order-external-id")
        .status(OrderStatus.DRAFT);

    private static final String QUEUE_NAME = "logistics4go_out";

    private static final String SUCCESS_BACKLOG = "level=INFO\t"
        + "format=plain\t"
        + "code=LES_PUSH\t"
        + "payload=Event pushed to LES\t"
        + "request_id=test-request-id\t"
        + "extra_keys=eventId,lomOrderId\t"
        + "extra_values=1,10";

    private static final String NULL_SNAPSHOT_BACKLOG = "level=ERROR\t"
        + "format=plain\t"
        + "code=LES_PUSH\t"
        + "payload=Event not pushed - order snapshot is null\t"
        + "request_id=test-request-id\t"
        + "extra_keys=eventId\t"
        + "extra_values=1";

    private static final String SERIALIZATION_ERROR_BACKLOG_PREFIX = "level=ERROR\t"
        + "format=json-exception\t"
        + "code=LES_PUSH\t"
        + "payload={"
        + "\\\"eventMessage\\\":\\\"Event not pushed - cannot serialize order snapshot: This snapshot is broken "
        + "(through reference chain: ru.yandex.market.logistics.logistics4go.queue."
        + "PushLomOrderEventToLesProcessorTest$BrokenOrderResponse[\\\\\\\"externalId\\\\\\\"])\\\","
        + "\\\"exceptionMessage\\\":\\\"RuntimeException: This snapshot is broken\\\","
        + "\\\"stackTrace\\\":";

    private static final String LES_EXCEPTION_BACKLOG_PREFIX = "level=ERROR\t"
        + "format=json-exception\t"
        + "code=LES_PUSH\t"
        + "payload={"
        + "\\\"eventMessage\\\":\\\"Event not pushed - error while pushing event to LES: LES IS ON FIRE!\\\","
        + "\\\"exceptionMessage\\\":\\\"PushLomOrderEventToLesProcessorTest.TestJmsException: LES IS ON FIRE!\\\","
        + "\\\"stackTrace\\\":";

    private static final String EXCEPTION_BACKLOG_SUFFIX = "}\t"
        + "request_id=test-request-id\t"
        + "extra_keys=eventId,lomOrderId\t"
        + "extra_values=1,10";

    @Autowired
    private PushLomOrderEventToLesProcessor processor;

    @Autowired
    private LesProducer lesProducer;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2022-01-02T03:04:05Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lesProducer);
    }

    @Test
    @DisplayName("Успешный пуш")
    void success() {
        softly.assertThat(processor.execute(goodPayload())).isEqualTo(TaskExecutionResult.finish());
        softly.assertThat(backLogCaptor.getResults().toString().contains(SUCCESS_BACKLOG)).isTrue();
        verify(lesProducer).send(lesEvent(), QUEUE_NAME);
    }

    @Test
    @DisplayName("Снапшот заказа == null")
    void nullSnapshot() {
        softly
            .assertThat(
                processor.execute(PushLomOrderEventToLesPayload.builder().eventId(1L).orderSnapshot(null).build())
            )
            .isEqualTo(TaskExecutionResult.fail());

        softly.assertThat(backLogCaptor.getResults().toString().contains(NULL_SNAPSHOT_BACKLOG)).isTrue();
    }

    @Test
    @DisplayName("Ошибка сериализации снапшота заказа")
    void cannotSerializeSnapshot() {
        softly.assertThat(processor.execute(brokenPayload())).isEqualTo(TaskExecutionResult.fail());

        String backLogResults = backLogCaptor.getResults().toString();
        softly.assertThat(backLogResults.contains(SERIALIZATION_ERROR_BACKLOG_PREFIX)).isTrue();
        softly.assertThat(backLogResults.contains(EXCEPTION_BACKLOG_SUFFIX)).isTrue();
    }

    @Test
    @DisplayName("Исключение при отправке в LES")
    void lesException() {
        doThrow(new TestJmsException("LES IS ON FIRE!"))
            .when(lesProducer)
            .send(any(Event.class), eq(QUEUE_NAME));

        softly.assertThat(processor.execute(goodPayload())).isEqualTo(TaskExecutionResult.fail());

        String backLogResults = backLogCaptor.getResults().toString();
        softly.assertThat(backLogResults.contains(LES_EXCEPTION_BACKLOG_PREFIX)).isTrue();
        softly.assertThat(backLogResults.contains(EXCEPTION_BACKLOG_SUFFIX)).isTrue();
        verify(lesProducer).send(lesEvent(), QUEUE_NAME);
    }

    @Nonnull
    private PushLomOrderEventToLesPayload goodPayload() {
        return PushLomOrderEventToLesPayload.builder()
            .eventId(1L)
            .orderSnapshot(SNAPSHOT_OBJECT)
            .build();
    }

    @Nonnull
    private PushLomOrderEventToLesPayload brokenPayload() {
        return PushLomOrderEventToLesPayload.builder()
            .eventId(1L)
            .orderSnapshot(new BrokenOrderResponse())
            .build();
    }

    @Nonnull
    @SneakyThrows
    private Event lesEvent() {
        return new Event(
            "logistics4go",
            "1",
            1641092645000L,
            "GO_ORDER_SNAPSHOT",
            new GoOrderSnapshot(
                "order-external-id",
                objectMapper.valueToTree(SNAPSHOT_OBJECT)
            ),
            ""
        );
    }

    private static class BrokenOrderResponse extends GetOrderResponse {
        @Override
        public Long getId() {
            return 10L;
        }

        @Override
        public String getExternalId() {
            throw new RuntimeException("This snapshot is broken");
        }
    }

    private static class TestJmsException extends JmsException {
        TestJmsException(String message) {
            super(message);
        }

        @Override
        public String getMessage() {
            return super.getMessage();
        }
    }
}
