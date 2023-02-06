package ru.yandex.market.logistics.logistics4shops.queue.processor;

import java.time.Instant;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.les.OrderNewCheckpointEvent;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.les.tracker.enums.ApiVersion;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.queue.payload.OrderCheckpointPayload;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Пуш чекпоинта в трекер")
@DatabaseSetup(value = "/service/les/pushCheckpointToTracker/prepare.xml")
class PushCheckpointToTrackerProcessorTest extends AbstractIntegrationTest {
    private static final Instant FIXED_TIME = Instant.parse("2021-12-12T00:00:00Z");
    private static final Instant DATETIME = Instant.parse("2022-02-21T11:30:00Z");

    @Autowired
    private PushCheckpointToTrackerProcessor processor;
    @Autowired
    private LesProducer lesProducer;

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lesProducer);
    }

    @Test
    @DisplayName("Запушить чекпоинт в трекер")
    void success() {
        softly.assertThat(processor.execute(OrderCheckpointPayload.builder().orderCheckpointId(100).build()))
            .isEqualTo(TaskExecutionResult.finish());
        verify(lesProducer).send(buildEvent(), "logistics4shops_out");
    }

    @Test
    @DisplayName("Чекпоинт не существует")
    void checkpointDoesNotExist() {
        softly.assertThatCode(() -> processor.execute(OrderCheckpointPayload.builder().orderCheckpointId(200).build()))
            .doesNotThrowAnyException();
    }

    @Nonnull
    private Event buildEvent() {
        return new Event(
            "logistics4shops",
            "1",
            FIXED_TIME.toEpochMilli(),
            "ORDER_NEW_CHECKPOINT",
            new OrderNewCheckpointEvent(
                null,
                300100L,
                "100100",
                "100100",
                ApiVersion.FF,
                101,
                DATETIME,
                null
            ),
            ""
        );
    }
}
