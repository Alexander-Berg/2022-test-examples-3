package ru.yandex.market.pvz.tms.executor.sqs;

import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.base.EntityKey;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.base.EventPayload;
import ru.yandex.market.pvz.core.domain.sqs.entity.IncomingSqsEventManager;
import ru.yandex.market.pvz.core.domain.sqs.entity.IncomingSqsEventRepository;
import ru.yandex.market.pvz.core.domain.sqs.processing.SqsEventHandler;
import ru.yandex.market.pvz.core.domain.sqs.processing.SqsEventProcessingService;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ProcessIncomingSqsEventsExecutorTest {

    private static final String EVENT_TYPE_SUCCESS = "entity_name_success";
    private static final String EVENT_TYPE_ERROR = "entity_name_error";
    private static final String EVENT_TYPE_SLOW = "entity_name_slow";
    private static final EventPayload PAYLOAD = new DummyPayload();
    private static final int EVENT_PROCESS_TIMEOUT_SECONDS = 3;

    private final IncomingSqsEventManager incomingSqsEventManager;
    private final IncomingSqsEventRepository repository;
    private SqsEventProcessingService processingService;
    private int successCount = 0;
    private int errorCount = 0;
    private int slowEventCount = 0;

    private final SqsEventHandler<?> successHandler = new SqsEventHandler<>() {

        @Override
        public String getEventType() {
            return EVENT_TYPE_SUCCESS;
        }

        @Override
        public void handle(Event event, Object payload) {
            successCount++;
        }

    };

    private final SqsEventHandler<?> errorHandler = new SqsEventHandler<>() {

        @Override
        public String getEventType() {
            return EVENT_TYPE_ERROR;
        }

        @Override
        public void handle(Event event, Object payload) {
            errorCount++;
            throw new RuntimeException("An exception occurred");
        }

    };

    private final SqsEventHandler<?> verySlowHandler = new SqsEventHandler<>() {

        @Override
        public String getEventType() {
            return EVENT_TYPE_SLOW;
        }

        @Override
        @SneakyThrows
        public void handle(Event event, Object payload) {
            Thread.sleep(TimeUnit.SECONDS.toMillis(EVENT_PROCESS_TIMEOUT_SECONDS * 2));
            slowEventCount++;
        }

    };

    @Data
    private static class DummyPayload implements EventPayload {
        @NotNull
        @Override
        public List<EntityKey> getEntityKeys() {
            return List.of();
        }
    }

    @BeforeEach
    void setup() {
        successCount = 0;
        errorCount = 0;
        processingService = new SqsEventProcessingService(
                EVENT_PROCESS_TIMEOUT_SECONDS,
                incomingSqsEventManager,
                List.of(successHandler, errorHandler, verySlowHandler)
        );
        repository.deleteAll();
    }

    @Test
    void testProcessSuccessfully() {
        incomingSqsEventManager.queue(List.of(buildEvent(EVENT_TYPE_SUCCESS, PAYLOAD)));
        processingService.process();
        assertThat(successCount).isEqualTo(1);
        assertThat(errorCount).isEqualTo(0);
    }

    @Test
    void testRemoveAfterSuccessfulProcessing() {
        incomingSqsEventManager.queue(List.of(buildEvent(EVENT_TYPE_SUCCESS, PAYLOAD)));
        processingService.process();
        processingService.process();
        assertThat(successCount).isEqualTo(1);
        assertThat(errorCount).isEqualTo(0);
    }

    @Test
    void testProcessErrorOnlyThreeTimes() {
        incomingSqsEventManager.queue(List.of(
                buildEvent(EVENT_TYPE_ERROR, PAYLOAD),
                buildEvent(EVENT_TYPE_SUCCESS, PAYLOAD)
        ));

        processingService.process();
        assertThat(successCount).isEqualTo(1);
        assertThat(errorCount).isEqualTo(1);

        processingService.process();
        assertThat(successCount).isEqualTo(1);
        assertThat(errorCount).isEqualTo(2);

        processingService.process();
        assertThat(successCount).isEqualTo(1);
        assertThat(errorCount).isEqualTo(3);

        processingService.process();
        assertThat(successCount).isEqualTo(1);
        assertThat(errorCount).isEqualTo(3);
    }

    @Test
    void testTimeoutExceptionWhenProcessingIsTooLong() {
        incomingSqsEventManager.queue(List.of(buildEvent(EVENT_TYPE_SLOW, PAYLOAD)));

        processingService.process();
        assertThat(slowEventCount).isEqualTo(0);
    }

    private Event buildEvent(String type, EventPayload payload) {
        return new Event(
                "source", "event_id", 123456789L, type, payload, "description"
        );
    }

}
