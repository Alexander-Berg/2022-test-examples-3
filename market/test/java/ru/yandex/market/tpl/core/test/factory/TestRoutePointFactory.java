package ru.yandex.market.tpl.core.test.factory;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.usershift.CollectDropshipTaskFactory;
import ru.yandex.market.tpl.core.domain.usershift.commands.CargoReference;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewCollectDropshipRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;

@Service
@RequiredArgsConstructor
public class TestRoutePointFactory {
    private final Clock clock;

    public NewDeliveryRoutePointData buildDropoffReturnRoutePointData(Movement movement, Long pickupPointId) {
        return buildDropoffRoutePointData(movement, pickupPointId, true);
    }

    public NewDeliveryRoutePointData buildDropoffDirectRoutePointData(Movement movement, Long pickupPointId) {
        return buildDropoffRoutePointData(movement, pickupPointId, false);
    }

    public NewCollectDropshipRoutePointData buildCollectDropshipRoutePointData(Movement movement) {
        return NewCollectDropshipRoutePointData.builder()
                .movement(movement)
                .address(CollectDropshipTaskFactory.fromWarehouseAddress(movement.getWarehouseTo().getAddress()))
                .name(movement.getWarehouse().getAddress().getAddress())
                .expectedArrivalTime(DateTimeUtil.todayAtHour(12, clock))
                .build();
    }

    private NewDeliveryRoutePointData buildDropoffRoutePointData(Movement movement, Long pickupPointId,
                                                                 boolean isReturn) {
        return NewDeliveryRoutePointData.builder()
                .cargoReference(
                        CargoReference.builder()
                                .movementId(movement.getId())
                                .isReturn(isReturn)
                                .build()
                )
                .address(CollectDropshipTaskFactory.fromWarehouseAddress(movement.getWarehouseTo().getAddress()))
                .name(movement.getWarehouse().getAddress().getAddress())
                .expectedArrivalTime(DateTimeUtil.todayAtHour(12, clock))
                .type(RoutePointType.LOCKER_DELIVERY)
                .pickupPointId(pickupPointId)
                .build();
    }
}
