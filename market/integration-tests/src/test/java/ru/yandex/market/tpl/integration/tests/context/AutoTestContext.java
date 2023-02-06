package ru.yandex.market.tpl.integration.tests.context;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Data;

import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointListDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointSummaryDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.shift.ShiftDto;
import ru.yandex.market.tpl.api.model.shift.UserShiftDto;
import ru.yandex.market.tpl.integration.tests.configuration.DeliveryTestConfiguration;
import ru.yandex.market.tpl.integration.tests.configuration.TestConfiguration;

/**
 * Контекст, который содержит последние/актуальные dto-шки, полученные из АПИ.
 * Создаётся на каждый тест.
 */
@Data
public class AutoTestContext {
    Long userId;
    Long uid;
    RoutePointDto routePoint;
    ShiftDto shift;
    UserShiftDto userShift;
    String trackingId;
    Map<Long, RoutePointSummaryDto> routePointsMap;
    String courierTkn;
    String orderId;
    DeliveryTestConfiguration deliveryTestConfiguration = new DeliveryTestConfiguration();
    TestConfiguration testConfiguration = TestConfiguration.getDefaultConfiguration();

    public Long getRoutePointId() {
        return routePoint.getId();
    }

    public Long getShiftId() {
        return shift.getId();
    }

    public Long getSortingCenterId() {
        return shift.getSortingCenterId();
    }

    public Long getUserShiftId() {
        return userShift.getId();
    }

    public void setRoutePoints(RoutePointListDto routePoints) {
        this.routePointsMap = routePoints.getRoutePoints().stream()
                .collect(Collectors.toMap(RoutePointSummaryDto::getId, Function.identity()));
    }

    public RoutePointSummaryDto findRoutePointById(Long routePointId) {
        return routePointsMap.get(routePointId);
    }

    public RoutePointSummaryDto findFirstRoutePointByType(RoutePointType routePointType) {
        return routePointsMap.values().stream()
                .filter(x -> x.getType() == routePointType)
                .findFirst()
                .orElseThrow();
    }


    public RoutePointSummaryDto getCurrentRoutePoint() {
        return routePointsMap.get(userShift.getCurrentRoutePointId());
    }
}
