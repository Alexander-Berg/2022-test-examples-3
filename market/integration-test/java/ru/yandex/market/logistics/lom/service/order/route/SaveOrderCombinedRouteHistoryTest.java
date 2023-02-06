package ru.yandex.market.logistics.lom.service.order.route;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.repository.OrderRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ParametersAreNonnullByDefault
@DisplayName("Тест на сохранение истории комбинированных маршрутов заказов")
class SaveOrderCombinedRouteHistoryTest extends AbstractOrderCombinedRouteHistoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @ExpectedDatabase(
        value = "/service/order/route/after/first_order_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Маршрут успешно сохранен в postgres, ydb, в заказе заполнено поле routeUuid")
    void successSavingIfNotExists() {
        saveRoute(1L);

        verifyOrderRouteHistorySaved(1L);
    }

    @Test
    @ExpectedDatabase(
        value = "/service/order/route/after/second_order_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Маршрут успешно сохранен в postgres, ydb, в заказе обновлено поле routeUuid")
    void successSavingIfExists() {
        saveRoute(2L);

        verifyOrderRouteHistorySaved(2L);
    }

    @Test
    @DisplayName("Маршрут не сохраняется в postgres, если произошла ошибка при сохранении в ydb, "
        + "routeUuid заказа не обновляется")
    @ExpectedDatabase(
        value = "/service/order/route/before/prepare_orders.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noSavingInPostgresIfHasYdbErrors() {
        doThrow(new RuntimeException())
            .when(ydbRepository).saveRoute(any(), any());

        assertThrows(RuntimeException.class, () -> saveRoute(1L));

        verify(uuidGenerator).randomUuid();
        verify(ydbRepository).saveRoute(refEq(expectedRouteHistory(1L)), refEq(combinedRoute()));
        verifyUidsInYdb(List.of());
    }

    private void verifyOrderRouteHistorySaved(long orderId) {
        verify(uuidGenerator).randomUuid();
        verify(ydbRepository).saveRoute(refEq(expectedRouteHistory(orderId), "id"), refEq(combinedRoute()));
        verifyUidsInYdb(List.of(MOCKED_UUID));
    }

    private void saveRoute(long orderId) {
        transactionTemplate.execute(ts -> {
            orderCombinedRouteHistoryService.saveRoute(orderRepository.getById(orderId), combinedRoute());
            return null;
        });
    }
}
