package ru.yandex.market.logistics.logistics4shops.queue.processor;

import java.time.Instant;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.queue.payload.OrderPayload;

@DisplayName("Пуш чекпоинта в трекер")
@DatabaseSetup(value = "/service/les/pushReadyToShipOnDeadline/before/prepare.xml")
class PushReadyToShipOnDeadlineProcessorTest extends AbstractIntegrationTest {
    private static final Instant FIXED_TIME = Instant.parse("2021-12-12T00:00:00Z");

    @Autowired
    private PushReadyToShipOnDeadlineProcessor processor;


    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Нет заказа")
    @ExpectedDatabase(
        value = "/service/les/pushReadyToShipOnDeadline/no_cp.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/les/pushReadyToShipOnDeadline/after/no_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void orderNotFound() {
        softly.assertThat(processor.execute(OrderPayload.builder().externalId("100101").build()))
            .isEqualTo(TaskExecutionResult.finish());
    }

    @Test
    @DisplayName("Был настоящий чекпоинт")
    @DatabaseSetup(
        value = "/service/les/pushReadyToShipOnDeadline/real_ready_to_ship.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/les/pushReadyToShipOnDeadline/real_ready_to_ship.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/les/pushReadyToShipOnDeadline/after/no_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void wasRealReadyToShip() {
        softly.assertThat(processor.execute(OrderPayload.builder().externalId("100100").build()))
            .isEqualTo(TaskExecutionResult.finish());
    }

    @Test
    @DisplayName("Был автоматический чекпоинт")
    @DatabaseSetup(
        value = "/service/les/pushReadyToShipOnDeadline/automatic_ready_to_ship.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/les/pushReadyToShipOnDeadline/automatic_ready_to_ship.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/les/pushReadyToShipOnDeadline/after/push_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void wasAutomaticReadyToShip() {
        softly.assertThat(processor.execute(OrderPayload.builder().externalId("100100").build()))
            .isEqualTo(TaskExecutionResult.finish());
    }

    @Test
    @DisplayName("Не было чекпоинтов")
    @DatabaseSetup(
        value = "/service/les/pushReadyToShipOnDeadline/no_cp.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/les/pushReadyToShipOnDeadline/after/new_automatic_ready_to_ship.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/les/pushReadyToShipOnDeadline/after/push_task_new.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noCheckpointsBefore() {
        softly.assertThat(processor.execute(OrderPayload.builder().externalId("100100").build()))
            .isEqualTo(TaskExecutionResult.finish());
    }
}
