package ru.yandex.market.logistics.lom.jobs.processor;

import java.math.BigDecimal;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.embedded.OrderHistoryEventAuthor;
import ru.yandex.market.logistics.lom.jobs.model.ProcessSegmentCheckpointsPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;

@ParametersAreNonnullByDefault
@DisplayName("Обработка необработанных чекпоинтов сегмента")
@DatabaseSetup({
    "/billing/before/billing_service_products.xml",
    "/jobs/processor/process_segment_checkpoints/before/setup.xml",
})
class ProcessSegmentCheckpointsProcessorTest extends AbstractContextualTest {

    @Autowired
    private ProcessSegmentCheckpointsProcessor processSegmentCheckpointsProcessor;

    @Test
    @DisplayName("У сегмента нет необработанных статусов в истории")
    @ExpectedDatabase(
        value = "/jobs/processor/process_segment_checkpoints/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noUnprocessedStatuses() {
        processPayloadAndCheckResult(
            1L,
            100L,
            "No unprocessed checkpoints found."
        );
    }

    @Test
    @DisplayName("Необработанные чекпоинты есть только на неактивных сегментах")
    @ExpectedDatabase(
        value = "/jobs/processor/process_segment_checkpoints/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unprocessedOnlyOnInactiveSegment() {
        processPayloadAndCheckResult(
            3L,
            102L,
            "Unprocessed null checkpointId null for inactive segment 3. "
                + "Unprocessed DELIVERY_LOADED checkpointId 3333 for inactive segment 3."
        );
    }

    @Test
    @DisplayName("У заказа есть активный возврат в ЛРМ")
    @ExpectedDatabase(
        value = "/jobs/processor/process_segment_checkpoints/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void hasReturnInLrm() {
        processPayloadAndCheckResult(
            22L,
            201L,
            "Checkpoint RETURN_PREPARING with id 222 skipped. Active order returns in LRM exist in order 2."
        );
    }

    private void processPayloadAndCheckResult(
        long waybillSegmentId,
        long trackerId,
        String resultMessage
    ) {
        softly.assertThat(processSegmentCheckpointsProcessor.processPayload(payload(waybillSegmentId, trackerId)))
            .usingRecursiveComparison()
            .isEqualTo(ProcessingResult.success(
                "Processing result: " + resultMessage
            ));
    }

    @Nonnull
    private ProcessSegmentCheckpointsPayload payload(
        long waybillSegmentId,
        long trackerId
    ) {
        return new ProcessSegmentCheckpointsPayload(
            REQUEST_ID,
            waybillSegmentId,
            trackerId,
            new OrderHistoryEventAuthor().setTvmServiceId(1L).setYandexUid(BigDecimal.TEN)
        );
    }
}
