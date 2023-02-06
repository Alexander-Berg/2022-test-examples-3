package ru.yandex.market.logistics.logistics4shops.queue.processor;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.model.exception.ResourceNotFoundException;
import ru.yandex.market.logistics.logistics4shops.queue.payload.OrderPayload;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DisplayName("Обработка потерянных чекпоинтов для заказа")
@DatabaseSetup("/queue/processor/process_lost_checkpoints/prepare.xml")
class LostCheckpointsProcessorTest extends AbstractIntegrationTest {
    private static final String ORDER_WITHOUT_ACCEPTED = "100100";
    private static final String ORDER_WITH_ACCEPTED = "100101";

    @Autowired
    private LostCheckpointsProcessor processor;

    @Test
    @DisplayName("Заказ не существует")
    @ExpectedDatabase(
        value = "/queue/processor/process_lost_checkpoints/prepare.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void orderNotExist() {
        softly.assertThatCode(() -> processor.execute(OrderPayload.of("400")))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [ORDER] with id [400]");
    }

    @Test
    @DisplayName("ACCEPTED")
    @DatabaseSetup(
        value = "/queue/processor/process_lost_checkpoints/accepted.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/queue/processor/process_lost_checkpoints/after/accepted.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/queue/processor/process_lost_checkpoints/after/push_task.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/queue/processor/process_lost_checkpoints/after/empty_lost_cp.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void accepted() {
        processor.execute(OrderPayload.of(ORDER_WITHOUT_ACCEPTED));
    }

    @Test
    @DisplayName("PACKAGING")
    @DatabaseSetup(
        value = "/queue/processor/process_lost_checkpoints/packaging.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/queue/processor/process_lost_checkpoints/after/packaging.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/queue/processor/process_lost_checkpoints/after/push_task.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/queue/processor/process_lost_checkpoints/after/empty_lost_cp.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void packaging() {
        processor.execute(OrderPayload.of(ORDER_WITH_ACCEPTED));
    }

    @Test
    @DisplayName("READY_TO_SHIP")
    @DatabaseSetup(
        value = "/queue/processor/process_lost_checkpoints/ready_to_ship.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/queue/processor/process_lost_checkpoints/after/ready_to_ship.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/queue/processor/process_lost_checkpoints/after/push_task.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/queue/processor/process_lost_checkpoints/after/empty_lost_cp.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void readyToShip() {
        processor.execute(OrderPayload.of(ORDER_WITH_ACCEPTED));
    }

    @Test
    @DisplayName("Удаление товаров")
    @DatabaseSetup(
        value = "/queue/processor/process_lost_checkpoints/items_removed.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/queue/processor/process_lost_checkpoints/after/items_removed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/queue/processor/process_lost_checkpoints/after/push_task.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/queue/processor/process_lost_checkpoints/after/empty_lost_cp.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void itemsRemoved() {
        processor.execute(OrderPayload.of(ORDER_WITH_ACCEPTED));
    }

    @Test
    @DisplayName("Изменение коробок")
    @DatabaseSetup(
        value = {
            "/queue/processor/process_lost_checkpoints/places_changed.xml",
            "/queue/processor/process_lost_checkpoints/ready_to_ship_cp.xml",
        },
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/queue/processor/process_lost_checkpoints/after/places_changed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/queue/processor/process_lost_checkpoints/after/push_task.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/queue/processor/process_lost_checkpoints/after/empty_lost_cp.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void placesChanged() {
        processor.execute(OrderPayload.of(ORDER_WITH_ACCEPTED));
    }

    @Test
    @DisplayName("Чп обрабатываются в правильном порядке")
    @DatabaseSetup(
        value = "/queue/processor/process_lost_checkpoints/all_lost_cp_kinds.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/queue/processor/process_lost_checkpoints/after/all_lost.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void correctOrder() {
        processor.execute(OrderPayload.of(ORDER_WITHOUT_ACCEPTED));
    }
}
