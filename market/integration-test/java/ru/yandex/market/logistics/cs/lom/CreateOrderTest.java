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
import ru.yandex.market.logistics.lom.model.enums.PlatformClient;

@DisplayName("Обработка ивентов из лома о создании заказов")
class CreateOrderTest extends AbstractLomTest {

    @Test
    @DisplayName("Создался заказ в ломе")
    @DatabaseSetup("/repository/lom/capacity_setup.xml")
    @ExpectedDatabase(
        value = "/repository/lom/create_order/after/service_counters.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/lom/create_order/after/new_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrder() {
        Mockito.when(lomClient.getRouteByUuid(LOM_ROUTE_UUID))
            .thenReturn(Optional.of(new CombinatorRoute().setRoute(getRealRoute())));
        lomEventConsumer.accept(createLomEvent(PlatformClient.YANDEX_GO, createDiff()));

        Awaitility.await()
            .atMost(Duration.ofMinutes(2))
            .until(() -> queueTaskRepository.thereIsNoTask());
    }

    @Test
    @DisplayName("Заказ уже был создан ранее, игнорируем")
    @DatabaseSetup("/repository/lom/create_order/before/duplicate_event.xml")
    @ExpectedDatabase(
        value = "/repository/lom/create_order/before/duplicate_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderDuplicate() {
        lomEventConsumer.accept(createLomEvent(PlatformClient.YANDEX_GO, createDiff()));

        Awaitility.await()
            .atMost(Duration.ofMinutes(2))
            .until(() -> queueTaskRepository.thereIsNoTask());
    }

    @Test
    @DisplayName("Пришел ивент о создании чекаутерного заказа, игнорируем")
    @ExpectedDatabase(
        value = "/repository/lom/no_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderBeru() {
        lomEventConsumer.accept(createLomEvent(PlatformClient.BERU, createDiff()));

        Awaitility.await()
            .atMost(Duration.ofMinutes(2))
            .until(() -> queueTaskRepository.thereIsNoTask());
    }

    @Nonnull
    @SneakyThrows
    private JsonNode createDiff() {
        return objectMapper.readTree(String.format(
            "[{\"op\": \"replace\", \"path\": \"/barcode\", \"value\": \"%s\", \"fromValue\": null}]",
            BARCODE
        ));
    }
}
