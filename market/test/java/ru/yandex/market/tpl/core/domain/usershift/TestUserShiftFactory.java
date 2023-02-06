package ru.yandex.market.tpl.core.domain.usershift;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;

import lombok.experimental.UtilityClass;

import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.core.domain.usershift.commands.CargoReference;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;

@UtilityClass
public class TestUserShiftFactory {

    public static UserShift buildWithDropOffTask(long existedMovementId) {
        UserShift userShift = new UserShift();
        userShift.setId(1L);
        userShift.setStatus(UserShiftStatus.ON_TASK);
        userShift.setRoutePoints(new ArrayList<>());

        userShift.addLockerDeliverySubtask(NewDeliveryRoutePointData.builder()
                        .pickupPointId(1L)
                        .type(RoutePointType.LOCKER_DELIVERY)
                        .expectedArrivalTime(Instant.now())
                        .address(new RoutePointAddress("street", "", GeoPoint.ofLatLon(BigDecimal.valueOf(55.733969),
                                BigDecimal.valueOf(37.720388))))
                        .cargoReference(CargoReference.builder()
                                .movementId(existedMovementId)
                                .build())
                        .build(),
                null,
                false);
        return userShift;
    }
}
