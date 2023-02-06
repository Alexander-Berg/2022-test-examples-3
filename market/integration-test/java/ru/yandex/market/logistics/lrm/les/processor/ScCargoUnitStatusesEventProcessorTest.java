package ru.yandex.market.logistics.lrm.les.processor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.sc.ScCargoUnit;
import ru.yandex.market.logistics.les.sc.ScCargoUnitStatus;
import ru.yandex.market.logistics.les.sc.ScSegmentStatusesEvent;
import ru.yandex.market.logistics.les.sc.StatusHistory;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.queue.processor.AsyncLesEventProcessor;

import static ru.yandex.market.logistics.lrm.les.LesEventFactory.getDbQueuePayload;

@DisplayName("Обработка получения события: Изменение статуса сегмента из СЦ")
class ScCargoUnitStatusesEventProcessorTest extends AbstractIntegrationTest {
    @Autowired
    private AsyncLesEventProcessor processor;

    @Test
    @DisplayName("Успех")
    @ExpectedDatabase(
        value = "/database/les/sc-segment-history/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void success() {
        Instant timestamp = Instant.parse("2021-11-20T12:00:00.00Z");
        processor.execute(getDbQueuePayload(
            new ScSegmentStatusesEvent(List.of(new ScCargoUnit(
                "cargo_id",
                "e11c5e64-3694-40c9-b9b4-126efedaa098",
                List.of(
                    new StatusHistory(ScCargoUnitStatus.CANCELLED, timestamp.minus(1, ChronoUnit.DAYS)),
                    new StatusHistory(ScCargoUnitStatus.SORTED_RETURN, timestamp),
                    new StatusHistory(ScCargoUnitStatus.ACCEPTED_RETURN, timestamp.plus(1, ChronoUnit.DAYS)),
                    new StatusHistory(ScCargoUnitStatus.DELETED, timestamp.plus(2, ChronoUnit.DAYS))
                )
            )))
        ));
    }
}
