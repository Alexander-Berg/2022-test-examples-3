package ru.yandex.market.tpl.carrier.core.domain;

import java.time.Instant;
import java.util.List;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.carrier.core.domain.movement.Movement;
import ru.yandex.market.tpl.carrier.core.domain.usershift.CollectDropshipTask;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePointAddress;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.carrier.core.domain.usershift.routepoint.command.NewCollectDropshipRoutePointData;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

/**
 * @author kukabara
 */
@Service
@RequiredArgsConstructor
public class TestDataFactory {

    private final UserShiftCommandService commandService;

    private final EntityManager entityManager;


    @Transactional
    public CollectDropshipTask addDropshipTask(long userShiftId, Movement movement) {
        UserShift userShift = entityManager.find(UserShift.class, userShiftId);
        OrderWarehouse warehouse = movement.getWarehouse();
        String address = warehouse.getAddress().getAddress();
        Instant expectedArrivalTime = userShift.getStartDateTime()
                .atZone(DateTimeUtil.DEFAULT_ZONE_ID)
                .with(NewCollectDropshipRoutePointData.COLLECT_DROPSHIP_TIME)
                .toInstant();
        Instant expectedDepartureTime = userShift.getEndDateTime()
                .atZone(DateTimeUtil.DEFAULT_ZONE_ID)
                .with(NewCollectDropshipRoutePointData.COLLECT_DROPSHIP_TIME)
                .toInstant();


        NewCollectDropshipRoutePointData data = new NewCollectDropshipRoutePointData(
                warehouse.getRoutePointName(),
                new RoutePointAddress(address, warehouse.getAddress().getGeoPoint(),
                        warehouse.getRegionId(), warehouse.getTimezone()),
                expectedArrivalTime,
                expectedDepartureTime,
                0,
                List.of(),
                warehouse.getYandexId()
        );
        UserShiftCommand.AddCollectDropshipTask command = new UserShiftCommand.AddCollectDropshipTask(
                userShiftId,
                data
        );
        commandService.addCollectDropshipTask(userShift.getUser(), command);

        return userShift.streamCollectDropshipTasks()
                .findFirst()
                .orElseThrow(IllegalStateException::new);

    }


}
