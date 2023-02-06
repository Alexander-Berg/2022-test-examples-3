package ru.yandex.market.logistics.lrm.les.processor;

import java.time.Instant;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.dto.PvzReturnStatusType;
import ru.yandex.market.logistics.les.tpl.PvzReturnStatuses;
import ru.yandex.market.logistics.les.tpl.PvzReturnStatusesEvent;
import ru.yandex.market.logistics.les.tpl.StatusHistory;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.queue.processor.AsyncLesEventProcessor;

import static ru.yandex.market.logistics.lrm.les.LesEventFactory.getDbQueuePayload;

@DisplayName("Обработка статусов возвратов от ПВЗ")
class PvzReturnStatusesEventProcessorTest extends AbstractIntegrationTest {
    @Autowired
    private AsyncLesEventProcessor processor;

    private static final Instant BASE = Instant.parse("2021-12-23T12:11:10Z");

    @Test
    @DisplayName("Успех")
    @DatabaseSetup("/database/les/pvz-return-statuses/before/minimal.xml")
    @ExpectedDatabase(
        value = "/database/les/pvz-return-statuses/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void success() {
        processor.execute(getDbQueuePayload(
            new PvzReturnStatusesEvent(List.of(
                new PvzReturnStatuses(
                    "box-external-id-1",
                    "1",
                    List.of(
                        new StatusHistory(PvzReturnStatusType.NEW, BASE),
                        new StatusHistory(PvzReturnStatusType.RECEIVED, BASE.plusSeconds(1))
                    )
                ),
                new PvzReturnStatuses(
                    "box-external-id-2",
                    "2",
                    List.of()
                )
            ))
        ));
    }

    @Test
    @DisplayName("Несколько коробок одного возврата")
    @DatabaseSetup("/database/les/pvz-return-statuses/before/multiple_boxes.xml")
    @ExpectedDatabase(
        value = "/database/les/pvz-return-statuses/after/multiple_boxes.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void multipleBoxes() {
        processor.execute(getDbQueuePayload(
            new PvzReturnStatusesEvent(List.of(
                new PvzReturnStatuses(
                    "box-external-id-1",
                    "1",
                    List.of(
                        new StatusHistory(PvzReturnStatusType.NEW, BASE),
                        new StatusHistory(PvzReturnStatusType.RECEIVED, BASE.plusSeconds(1))
                    )
                ),
                new PvzReturnStatuses(
                    "box-external-id-2",
                    "1",
                    List.of(
                        new StatusHistory(PvzReturnStatusType.NEW, BASE)
                    )
                )
            ))
        ));
    }

    @Test
    @DisplayName("Нет сегмента ПВЗ")
    @DatabaseSetup("/database/les/pvz-return-statuses/before/no_pickup_segment.xml")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void noPickupSegment() {
        processor.execute(getDbQueuePayload(
            new PvzReturnStatusesEvent(List.of(
                new PvzReturnStatuses(
                    "box-external-id-1",
                    "2",
                    List.of()
                )
            ))
        ));

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                """
                    level=WARN\t\
                    format=plain\t\
                    payload=Not found PICKUP segment for return box 1 (box-external-id-1)\t\
                    request_id=test-request-id
                    """
            );
    }

    @Test
    @DisplayName("Неизвестный возврат")
    @DatabaseSetup("/database/les/pvz-return-statuses/before/minimal.xml")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void unknownReturn() {
        processor.execute(getDbQueuePayload(
            new PvzReturnStatusesEvent(List.of(new PvzReturnStatuses(
                "box-id",
                "3",
                List.of()
            )))
        ));
    }
}
