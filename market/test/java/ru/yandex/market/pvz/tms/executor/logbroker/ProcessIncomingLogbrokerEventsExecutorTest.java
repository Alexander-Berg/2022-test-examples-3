package ru.yandex.market.pvz.tms.executor.logbroker;

import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.tpl.common.logbroker.consumer.LogbrokerEventHandler;
import ru.yandex.market.tpl.common.logbroker.consumer.LogbrokerEventProcessingService;
import ru.yandex.market.tpl.common.logbroker.consumer.LogbrokerMessage;
import ru.yandex.market.tpl.common.logbroker.domain.incoming.IncomingLogbrokerEventManager;
import ru.yandex.market.tpl.common.logbroker.domain.incoming.IncomingLogbrokerEventRepository;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ProcessIncomingLogbrokerEventsExecutorTest {

    private static final String ENTITY_NAME_SUCCESS = "entity_name_success";
    private static final String ENTITY_NAME_ERROR = "entity_name_error";
    private static final String ENTITY_NAME_SLOW = "entity_name_slow";
    private static final String PAYLOAD = "payload";
    private static final int EVENT_PROCESS_TIMEOUT_SECONDS = 1;

    private final IncomingLogbrokerEventManager incomingLogbrokerEventManager;
    private final IncomingLogbrokerEventRepository repository;
    private LogbrokerEventProcessingService processingService;
    private int successCount = 0;
    private int errorCount = 0;
    private int slowEventCount = 0;

    private final LogbrokerEventHandler<?> successHandler = new LogbrokerEventHandler<>() {
        @Override
        public String getEntityName() {
            return ENTITY_NAME_SUCCESS;
        }

        @Override
        public Object parse(String message) {
            return message;
        }

        @Override
        public void handle(Object event) {
            successCount++;
        }
    };

    private final LogbrokerEventHandler<?> errorHandler = new LogbrokerEventHandler<>() {
        @Override
        public String getEntityName() {
            return ENTITY_NAME_ERROR;
        }

        @Override
        public Object parse(String message) {
            return message;
        }

        @Override
        public void handle(Object event) {
            errorCount++;
            throw new RuntimeException("An exception occurred");
        }
    };

    private final LogbrokerEventHandler<?> verySlowHandler = new LogbrokerEventHandler<>() {
        @Override
        public String getEntityName() {
            return ENTITY_NAME_SLOW;
        }

        @Override
        public Object parse(String message) {
            return message;
        }

        @Override
        @SneakyThrows
        public void handle(Object event) {
            Thread.sleep(TimeUnit.SECONDS.toMillis(EVENT_PROCESS_TIMEOUT_SECONDS * 3));
            slowEventCount++;
        }
    };

    @BeforeEach
    void setup() {
        successCount = 0;
        errorCount = 0;
        slowEventCount = 0;
        processingService = new LogbrokerEventProcessingService(
                EVENT_PROCESS_TIMEOUT_SECONDS,
                incomingLogbrokerEventManager,
                List.of(successHandler, errorHandler, verySlowHandler)
        );
        repository.deleteAll();
    }

    @Test
    void testProcessSuccessfully() {
        incomingLogbrokerEventManager.queue(List.of(new LogbrokerMessage(ENTITY_NAME_SUCCESS, PAYLOAD)));
        processingService.process();
        assertThat(successCount).isEqualTo(1);
        assertThat(errorCount).isEqualTo(0);
    }

    @Test
    void testRemoveAfterSuccessfulProcessing() {
        incomingLogbrokerEventManager.queue(List.of(new LogbrokerMessage(ENTITY_NAME_SUCCESS, PAYLOAD)));
        processingService.process();
        processingService.process();
        assertThat(successCount).isEqualTo(1);
        assertThat(errorCount).isEqualTo(0);
    }

    @Test
    void testProcessErrorOnlyThreeTimes() {
        incomingLogbrokerEventManager.queue(List.of(
                new LogbrokerMessage(ENTITY_NAME_ERROR, PAYLOAD),
                new LogbrokerMessage(ENTITY_NAME_SUCCESS, PAYLOAD)
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
        incomingLogbrokerEventManager.queue(List.of(new LogbrokerMessage(ENTITY_NAME_SLOW, PAYLOAD)));

        processingService.process();
        assertThat(slowEventCount).isEqualTo(0);
    }
}
