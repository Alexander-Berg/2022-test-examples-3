package ru.yandex.market.tpl.core.query;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.task.TaskGroupingKey;
import ru.yandex.market.tpl.api.model.task.TaskGroupingType;
import ru.yandex.market.tpl.common.web.exception.TplInvalidDataException;
import ru.yandex.market.tpl.core.domain.base.property.TplPropertyType;
import ru.yandex.market.tpl.core.domain.order.CargoType;
import ru.yandex.market.tpl.core.domain.order.property.TplOrderProperties;
import ru.yandex.market.tpl.core.domain.usershift.routepoint.projection.RoutePointClientDeliveryProjection;
import ru.yandex.market.tpl.core.query.usershift.ClientTaskGroupingService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredArgsConstructor
public class ClientTaskGroupingServiceUnitTest extends TplAbstractTest {
    private final ClientTaskGroupingService clientTaskGroupingService;

    @Test
    void groupingStandardOrder() {
        TaskGroupingKey taskGroupingKey = clientTaskGroupingService.groupingBy(
                RoutePointClientDeliveryProjection.Task.builder()
                        .multiOrderId("1")
                        .order(RoutePointClientDeliveryProjection.Order.builder().build())
                        .build()
        );
        assertThat(taskGroupingKey.getType()).isEqualTo(TaskGroupingType.STANDARD_ORDER);
    }

    @Test
    void groupingClientReturnOrder() {
        TaskGroupingKey taskGroupingKey = clientTaskGroupingService.groupingBy(
                RoutePointClientDeliveryProjection.Task.builder()
                        .multiOrderId("1")
                        .clientReturn(RoutePointClientDeliveryProjection.ClientReturn.builder().build())
                        .build()
        );
        assertThat(taskGroupingKey.getType()).isEqualTo(TaskGroupingType.CLIENT_RETURN);
    }

    @Test
    void groupingFashionOrder() {
        TaskGroupingKey taskGroupingKey = clientTaskGroupingService.groupingBy(
                RoutePointClientDeliveryProjection.Task.builder()
                        .multiOrderId("1")
                        .order(RoutePointClientDeliveryProjection.Order.builder()
                                .items(List.of(
                                        RoutePointClientDeliveryProjection.OrderItem.builder()
                                                .cargoTypeCodes(List.of(CargoType.FASHION.getCode()))
                                                .build())
                                )
                                .properties(
                                        Map.of(
                                                TplOrderProperties.IS_TRYING_AVAILABLE.getName(),
                                                RoutePointClientDeliveryProjection.OrderProperty.builder()
                                                        .type(TplPropertyType.BOOLEAN)
                                                        .value(String.valueOf(true))
                                                        .name(TplOrderProperties.IS_TRYING_AVAILABLE.getName())
                                                        .build()
                                        )
                                )
                                .build())
                        .build()
        );
        assertThat(taskGroupingKey.getType()).isEqualTo(TaskGroupingType.FASHION_ORDER);
    }

    @Test
    void groupingSomeThingWithoutOrderOrClientReturn() {
        assertThatThrownBy(
                () -> clientTaskGroupingService.groupingBy(
                        RoutePointClientDeliveryProjection.Task.builder()
                                .multiOrderId("1")
                                .build()
                )
        ).isInstanceOf(TplInvalidDataException.class);
    }
}
