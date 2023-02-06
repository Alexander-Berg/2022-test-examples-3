package ru.yandex.market.logistics.lom.service.order.route;

import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.repository.OrderRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ParametersAreNonnullByDefault
@DisplayName("Обновление истории маршрутов заказа")
class UpdateOrderCombinedRouteHistoryTest extends AbstractOrderCombinedRouteHistoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("Функциональность выключена, ничего не происходит")
    void featureDisabled() {
        updateRouteHistory(1L, MOCKED_UUID);
    }

    @Test
    @DisplayName("Успешное обновление истории заказа")
    @ExpectedDatabase(
        value = "/service/order/route/after/success_updating.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void sucessUpdating() {
        updateRouteHistory(1L, UUID.fromString("6303da55-580e-4470-8b54-58503a2cbe79"));
        updateRouteHistory(2L, MOCKED_UUID);
    }

    @Test
    @DatabaseSetup(
        value = "/service/order/route/before/prepare_illegal_state.xml",
        type = DatabaseOperation.INSERT
    )
    @DisplayName("Невалидное состояние базы данных: маршрут уже привязан к другому заказу")
    void anotherOrderWithSameRouteUuidExist() {
        assertThrows(
            IllegalStateException.class,
            () -> updateRouteHistory(1L, MOCKED_UUID),
            "Trying to save order 2 route e11c5e64-3694-40c9-b9b4-126efedaa098 to order 1"
        );
    }

    void updateRouteHistory(long orderId, UUID routeUuid) {
        transactionTemplate.execute(ts -> {
            orderCombinedRouteHistoryService.updateRouteHistory(orderRepository.getById(orderId), routeUuid);
            return null;
        });
    }

    @Test
    @DatabaseSetup(
        value = "/service/order/route/before/already_in_history.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/order/route/after/order_route_uuid_updated_no_new_route_history.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Обновление истории, в истории уже есть запись с этим заказом")
    void routeAlreadyInHistory() {
        updateRouteHistory(1L, MOCKED_UUID);
    }
}
