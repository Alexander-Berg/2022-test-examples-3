package ru.yandex.market.logistics.cs.lom;

import java.time.Duration;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PlatformClient;

@DisplayName("Обработка ивентов из лома об отмене заказов")
class CancelOrderTest extends AbstractLomTest {

    @Test
    @DisplayName("Отмена заказа в ломе")
    @DatabaseSetup("/repository/lom/cancel_order/before/new_event.xml")
    @DatabaseSetup("/repository/lom/capacity_setup.xml")
    @ExpectedDatabase(
        value = "/repository/lom/cancel_order/after/service_counters.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/lom/cancel_order/after/cancelled_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cancelOrder() {
        Mockito.when(lomClient.getRouteByUuid(LOM_ROUTE_UUID))
            .thenReturn(Optional.of(new CombinatorRoute().setRoute(getRealRoute())));
        lomEventConsumer.accept(createLomEvent(
            PlatformClient.YANDEX_GO,
            createDiff(CancellationOrderStatus.PROCESSING, CancellationOrderStatus.SUCCESS)
        ));

        Awaitility.await()
            .atMost(Duration.ofMinutes(2))
            .until(() -> queueTaskRepository.thereIsNoTask());
    }

    @Test
    @DisplayName("Переход заявки на отмену в промежуточный статус, игнорируем")
    @DatabaseSetup("/repository/lom/cancel_order/before/new_event.xml")
    @ExpectedDatabase(
        value = "/repository/lom/cancel_order/before/new_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processingCancelOrder() {
        lomEventConsumer.accept(createLomEvent(
            PlatformClient.YANDEX_GO,
            createDiff(CancellationOrderStatus.CREATED, CancellationOrderStatus.PROCESSING)
        ));

        Awaitility.await()
            .atMost(Duration.ofMinutes(2))
            .until(() -> queueTaskRepository.thereIsNoTask());
    }

    @Test
    @DisplayName("Отмена заказа в ломе, отсутствует заявка на создание")
    @DatabaseSetup("/repository/lom/no_events.xml")
    @ExpectedDatabase(
        value = "/repository/lom/no_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cancelOrderWithoutNewOrderEvent() {
        lomEventConsumer.accept(createLomEvent(
            PlatformClient.YANDEX_GO,
            createDiff(CancellationOrderStatus.PROCESSING, CancellationOrderStatus.SUCCESS)
        ));

        Awaitility.await()
            .atMost(Duration.ofMinutes(2))
            .until(() -> queueTaskRepository.thereIsNoTask());
    }

    @Test
    @DisplayName("Отмена чекаутерного заказа, игнорируем")
    @DatabaseSetup("/repository/lom/cancel_order/before/new_event.xml")
    @ExpectedDatabase(
        value = "/repository/lom/cancel_order/before/new_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cancelBeruOrder() {
        lomEventConsumer.accept(createLomEvent(
            PlatformClient.BERU,
            createDiff(CancellationOrderStatus.PROCESSING, CancellationOrderStatus.SUCCESS)
        ));

        Awaitility.await()
            .atMost(Duration.ofMinutes(2))
            .until(() -> queueTaskRepository.thereIsNoTask());
    }

    @Test
    @DisplayName("Заказ уже был отменен, игнорируем")
    @DatabaseSetup("/repository/lom/cancel_order/before/duplicate_event.xml")
    @ExpectedDatabase(
        value = "/repository/lom/cancel_order/before/duplicate_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void duplicateCancelOrder() {
        lomEventConsumer.accept(createLomEvent(
            PlatformClient.YANDEX_GO,
            createDiff(CancellationOrderStatus.PROCESSING, CancellationOrderStatus.SUCCESS)
        ));

        Awaitility.await()
            .atMost(Duration.ofMinutes(2))
            .until(() -> queueTaskRepository.thereIsNoTask());
    }

    @Nonnull
    @SneakyThrows
    private JsonNode createDiff(CancellationOrderStatus from, CancellationOrderStatus to) {
        return objectMapper.readTree(String.format(
            "[{\"op\": \"replace\","
                + "\"path\": \"/cancellationOrderRequests/0/status\", "
                + "\"fromValue\": \"%s\","
                + "\"value\": \"%s\"}]",
            from.name(),
            to.name()
        ));
    }
}
