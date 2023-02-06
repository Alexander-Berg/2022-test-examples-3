package ru.yandex.market.logistics.lrm.tasks.return_segment;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.les.dto.CargoUnitDeleteSegmentDto;
import ru.yandex.market.logistics.les.tpl.StorageUnitDeleteSegmentRequestEvent;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.model.entity.enums.BusinessProcessType;
import ru.yandex.market.logistics.lrm.queue.payload.CancelScSegmentPayload;
import ru.yandex.market.logistics.lrm.queue.payload.CourierDto;
import ru.yandex.market.logistics.lrm.queue.processor.CancelScSegmentProcessor;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.logistics.lrm.config.LocalsConfiguration.TEST_UUID;
import static ru.yandex.market.logistics.lrm.config.LrmTestConfiguration.TEST_REQUEST_ID;

@DisplayName("Удаление сегмента СЦ")
@ParametersAreNonnullByDefault
@DatabaseSetup("/database/tasks/return-segment/delete-in-sc/before/prepare.xml")
class CancelScSegmentProcessorTest extends AbstractIntegrationTest {

    private static final Instant FIXED_TIME = Instant.parse("2021-11-11T11:12:13Z");

    @Autowired
    private CancelScSegmentProcessor cancelScSegmentProcessor;

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
    @DisplayName("Сегмент в неподходящем статусе")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void segmentInIncorrectStatus() {
        softly.assertThat(cancelScSegmentProcessor.execute(payload(3L)))
            .isEqualTo(TaskExecutionResult.finish());
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            """
                level=ERROR\t\
                format=plain\t\
                payload=Segment 3 already has an active CANCELLATION change\
                """
        );
    }

    @Test
    @DisplayName("Передан идентификатор не СЦ")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void incorrectSegmentId() {
        softly.assertThatThrownBy(() -> cancelScSegmentProcessor.execute(payload(1L)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Segment 1 has invalid type PICKUP");
    }

    @Test
    @DisplayName("Успешная отправка события в LES")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/delete-in-sc/after/task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void lesEventSent() {
        cancelScSegmentProcessor.execute(payload(2L));

        verify(lesProducer).send(expectedLesEvent(), OUT_LES_QUEUE);
    }

    @Test
    @DisplayName("Успех, сегмент дропоффа")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/delete-in-sc/before/dropoff.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/delete-in-sc/after/task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void dropoffSegment() {
        cancelScSegmentProcessor.execute(payload(2L));

        verify(lesProducer).send(expectedLesEvent(), OUT_LES_QUEUE);
    }

    @Test
    @DisplayName("Неуспешная отправка события в LES")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void errorSendingEventToLes() {
        doThrow(new RuntimeException("Some LES error"))
            .when(lesProducer).send(expectedLesEvent(), OUT_LES_QUEUE);

        softly.assertThatThrownBy(() -> cancelScSegmentProcessor.execute(payload(2L)))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Some LES error");

        verify(lesProducer).send(expectedLesEvent(), OUT_LES_QUEUE);
    }

    @Nonnull
    private CancelScSegmentPayload payload(long segmentId) {
        return CancelScSegmentPayload.builder()
            .scSegmentId(segmentId)
            .courierDto(
                CourierDto.builder()
                    .carNumber("123456789")
                    .deliveryServiceId(123L)
                    .courierName("Super courier")
                    .build()
            )
            .build();
    }

    @Nonnull
    private Event expectedLesEvent() {
        return new Event(
            SOURCE_FOR_LES,
            TEST_UUID,
            FIXED_TIME.toEpochMilli(),
            BusinessProcessType.DELETE_SEGMENT_IN_SC.name(),
            requestEvent(),
            ""
        );
    }

    @Nonnull
    private StorageUnitDeleteSegmentRequestEvent requestEvent() {
        return new StorageUnitDeleteSegmentRequestEvent(
            TEST_REQUEST_ID + "/1",
            List.of(
                new CargoUnitDeleteSegmentDto("123098", "e11c5e64-3694-40c9-b9b4-126efedaa098")
            )
        );
    }
}
