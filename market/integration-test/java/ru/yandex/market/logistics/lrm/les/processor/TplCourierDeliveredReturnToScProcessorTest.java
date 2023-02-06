package ru.yandex.market.logistics.lrm.les.processor;

import java.time.Instant;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.les.tpl.TplCourierDeliveredReturnToScEvent;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.model.exception.ModelResourceNotFoundException;
import ru.yandex.market.logistics.lrm.queue.processor.AsyncLesEventProcessor;

import static ru.yandex.market.logistics.lrm.les.LesEventFactory.getDbQueuePayload;

@DatabaseSetup("/database/les/tpl-return-delivered-to-sc/before/common.xml")
class TplCourierDeliveredReturnToScProcessorTest extends AbstractIntegrationTest {

    @Autowired
    private AsyncLesEventProcessor processor;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2022-05-06T07:08:09.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Коробка не найдена")
    void boxNotFound() {
        softly.assertThatThrownBy(() -> process("unknown-box-id"))
            .isInstanceOf(ModelResourceNotFoundException.class)
            .hasMessage("Failed to find RETURN_BOX with id unknown-box-id");
    }

    @Test
    @DisplayName("Больше одного курьерского сегмента")
    @DatabaseSetup(
        value = "/database/les/tpl-return-delivered-to-sc/before/multiple_courier_segments.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void multipleCourierSegments() {
        softly.assertThatThrownBy(() -> process("box-external-id"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Return box 1 must have at most one COURIER segment");
    }

    @Test
    @DisplayName("Сегмент не найден")
    @DatabaseSetup(
        value = "/database/les/tpl-return-delivered-to-sc/before/no_courier_segments.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/database/les/tpl-return-delivered-to-sc/after/no_courier_segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void segmentNotFound() {
        process("box-external-id");
    }

    @Test
    @DisplayName("Успех")
    @ExpectedDatabase(
        value = "/database/les/tpl-return-delivered-to-sc/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void success() {
        process("box-external-id");
    }

    private void process(String boxExternalId) {
        processor.execute(getDbQueuePayload(new TplCourierDeliveredReturnToScEvent(
            "1",
            boxExternalId,
            "100"
        )));
    }

}
