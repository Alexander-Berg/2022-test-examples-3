package ru.yandex.market.tpl.core.test.factory;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import javax.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommand;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommandService;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.usershift.CollectDropshipTaskFactory;
import ru.yandex.market.tpl.core.domain.usershift.DeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.CargoReference;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.warehouse.OrderWarehouseGenerator;

@Service
@RequiredArgsConstructor
public class TestTplDropoffFactory {

    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final MovementGenerator movementGenerator;
    private final Clock clock;
    private final UserShiftCommandService userShiftCommandService;
    private final DropoffCargoCommandService dropoffCargoCommandService;

    public Movement generateReturnMovement(Shift shift, Long deliveryServiceId, PickupPoint pickupPoint) {
        return movementGenerator.generate(MovementCommand.Create.builder()
                .deliveryServiceId(deliveryServiceId)
                .orderWarehouseTo(
                        orderWarehouseGenerator.generateWarehouse(
                                wh -> wh.setYandexId("" + pickupPoint.getLogisticPointId())
                        )
                )
                .deliveryIntervalFrom(shift.getShiftDate().atTime(LocalTime.of(14, 30)).toInstant(ZoneOffset.of("+03" +
                        ":00")))
                .deliveryIntervalFrom(shift.getShiftDate().atTime(LocalTime.of(14, 35)).toInstant(ZoneOffset.of("+03" +
                        ":00")))
                .tags(List.of(Movement.TAG_DROPOFF_CARGO_RETURN))
                .build()
        );
    }

    public Movement generateDirectMovement(Shift shift, Long deliveryServiceId, PickupPoint pickupPoint) {
        return movementGenerator.generate(MovementCommand.Create.builder()
                .deliveryServiceId(deliveryServiceId)
                .orderWarehouseTo(
                        orderWarehouseGenerator.generateWarehouse(
                                wh -> wh.setYandexId("" + pickupPoint.getLogisticPointId() + "_FROM")
                        )
                )
                .orderWarehouse(
                        orderWarehouseGenerator.generateWarehouse(
                                wh -> wh.setYandexId("" + pickupPoint.getLogisticPointId())
                        )
                )
                .deliveryIntervalFrom(shift.getShiftDate().atTime(LocalTime.of(14, 30)).toInstant(ZoneOffset.of("+03" +
                        ":00")))
                .deliveryIntervalFrom(shift.getShiftDate().atTime(LocalTime.of(14, 35)).toInstant(ZoneOffset.of("+03" +
                        ":00")))
                .build()
        );
    }

    public DeliveryTask addDropoffTask(UserShift userShift, Movement movement, @Nullable Long cargoId,
                                       PickupPoint pickupPoint) {
        return userShiftCommandService.addDeliveryTask(
                null,
                new UserShiftCommand.AddDeliveryTask(
                        userShift.getId(),
                        NewDeliveryRoutePointData.builder()
                                .cargoReference(
                                        CargoReference.builder()
                                                .movementId(movement.getId())
                                                .dropOffCargoId(cargoId)
                                                .isReturn(movement.isDropOffReturn())
                                                .build()
                                )
                                .address(CollectDropshipTaskFactory
                                        .fromWarehouseAddress(movement.getWarehouseTo().getAddress()))
                                .name(movement.getWarehouseTo().getAddress().getAddress())
                                .expectedArrivalTime(DateTimeUtil.todayAtHour(12, clock))
                                .type(RoutePointType.LOCKER_DELIVERY)
                                .pickupPointId(pickupPoint.getId())
                                .build(),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                )
        );
    }

    public DropoffCargo generateCargo(String barcode, String referenceId, Movement movement) {
        return dropoffCargoCommandService.createOrGet(
                DropoffCargoCommand.Create.builder()
                        .barcode(barcode)
                        .referenceId(referenceId)
                        .logisticPointIdFrom(movement.getWarehouse().getYandexId())
                        .logisticPointIdTo(movement.getWarehouseTo().getYandexId())
                        .build());
    }
}
