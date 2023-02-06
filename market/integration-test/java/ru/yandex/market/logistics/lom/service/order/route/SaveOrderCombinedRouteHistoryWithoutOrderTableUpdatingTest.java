package ru.yandex.market.logistics.lom.service.order.route;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ParametersAreNonnullByDefault
@DisplayName("Сохранение истории маршрутов заказа без обновления таблицы заказов")
class SaveOrderCombinedRouteHistoryWithoutOrderTableUpdatingTest extends AbstractOrderCombinedRouteHistoryTest {

    private static final long ORDER_ID = 1;

    @Test
    @ExpectedDatabase(
        value = "/service/order/route/after/route_saved_order_not_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Маршрут успешно сохранен в postgres, ydb, в заказе не заполнено поле routeUuid")
    void successSaving() {
        saveRoute(MOCKED_UUID);

        verifyOrderRouteHistorySaved();
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

        softly.assertThatCode(() -> saveRoute(null)).isInstanceOf(RuntimeException.class);

        verify(uuidGenerator).randomUuid();
        verify(ydbRepository).saveRoute(refEq(expectedRouteHistory(ORDER_ID)), refEq(combinedRoute()));
        verifyUidsInYdb(List.of());
    }

    private void saveRoute(@Nullable UUID expectedUuid) {
        transactionTemplate.execute(ts -> {
            softly.assertThat(orderCombinedRouteHistoryService.saveRoute(ORDER_ID, combinedRoute()))
                .isEqualTo(expectedUuid);

            return null;
        });
    }

    private void verifyOrderRouteHistorySaved() {
        verify(uuidGenerator).randomUuid();
        verify(ydbRepository).saveRoute(refEq(expectedRouteHistory(ORDER_ID), "id"), refEq(combinedRoute()));
        verifyUidsInYdb(List.of(MOCKED_UUID));
    }
}
