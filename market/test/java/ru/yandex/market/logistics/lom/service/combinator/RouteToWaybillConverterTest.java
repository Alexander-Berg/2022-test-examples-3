package ru.yandex.market.logistics.lom.service.combinator;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.exception.ConvertRouteException;
import ru.yandex.market.logistics.lom.service.order.combinator.converter.PointsConvertingContext;
import ru.yandex.market.logistics.lom.service.order.combinator.converter.WarehouseMovementPointsToWaybillSegmentConverter;

@DisplayName("Конвертер комбинированного маршрута в вейбилл")
public class RouteToWaybillConverterTest extends AbstractTest {
    private final WarehouseMovementPointsToWaybillSegmentConverter converter =
        new WarehouseMovementPointsToWaybillSegmentConverter();

    @Test
    @DisplayName("Точки не подходят под шаблон конвертера")
    void pointsDoNotMatchPattern() {
        softly.assertThatThrownBy(
                () -> converter.convertRoute(PointsConvertingContext.builder().points(List.of()).build())
            )
            .isInstanceOf(ConvertRouteException.class)
            .hasMessage(
                "Points [] do not match pattern. " +
                    "Expected point types [[WAREHOUSE], [MOVEMENT]], " +
                    "partner types [DELIVERY, SORTING_CENTER, DROPSHIP, SUPPLIER]"
            );
    }
}
