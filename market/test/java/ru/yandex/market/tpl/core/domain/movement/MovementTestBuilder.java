package ru.yandex.market.tpl.core.domain.movement;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import lombok.Builder;

import ru.yandex.market.tpl.api.model.movement.MovementStatus;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseTestBuilder;

@Builder
public class MovementTestBuilder {

    @Builder.Default
    private Long id = null;
    @Builder.Default
    private String externalId = "123";
    @Builder.Default
    private long deliveryServiceId = 1;
    @Builder.Default
    private OrderWarehouse warehouse = OrderWarehouseTestBuilder.builder()
            .yandexId("ya-1")
            .build().get();
    @Builder.Default
    private OrderWarehouse warehouseTo = OrderWarehouseTestBuilder.builder()
            .yandexId("ya-2")
            .build().get();
    @Builder.Default
    private Instant deliveryIntervalFrom = Instant.now().minus(1, ChronoUnit.HOURS);
    @Builder.Default
    private Instant deliveryIntervalTo = Instant.now().plus(1, ChronoUnit.HOURS);
    @Builder.Default
    private MovementStatus status = MovementStatus.CREATED;
    @Builder.Default
    private List<String> tags = null;

    public Movement get() {
        Movement movement = new Movement();
        movement.setId(id);
        movement.setExternalId(externalId);
        movement.setDeliveryServiceId(deliveryServiceId);
        movement.setWarehouse(warehouse);
        movement.setWarehouseTo(warehouseTo);
        movement.setDeliveryIntervalFrom(deliveryIntervalFrom);
        movement.setDeliveryIntervalTo(deliveryIntervalTo);
        movement.setStatus(status);
        movement.setTags(tags);
        return movement;
    }

}
