package ru.yandex.market.logistics.lom.service.order.route;

import java.util.List;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;

@DisplayName("Сохранение маршрута только в ydb")
class SaveOrderCombinedRouteHistoryToYdbTest extends AbstractOrderCombinedRouteHistoryTest {

    @Test
    @DisplayName("Успешное сохранение")
    @ExpectedDatabase(
        value = "/service/order/route/before/prepare_orders.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void savedSuccess() {
        softly.assertThat(orderCombinedRouteHistoryService.saveRouteToYdb(combinedRoute()))
            .isEqualTo(MOCKED_UUID);

        verifyUidsInYdb(List.of(MOCKED_UUID));
        verify(uuidGenerator).randomUuid();
        verify(ydbRepository).saveRoute(
            refEq(expectedRouteHistory(null)),
            refEq(combinedRoute())
        );
    }
}
