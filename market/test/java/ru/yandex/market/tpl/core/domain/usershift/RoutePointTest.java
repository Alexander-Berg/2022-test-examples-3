package ru.yandex.market.tpl.core.domain.usershift;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;

import static org.assertj.core.api.Assertions.assertThat;

class RoutePointTest {

    @Nested
    class CanSkipLocationTest {
        @Test
        void statusIsInTransit() {
            RoutePoint rp = getRoutePoint(RoutePointStatus.IN_TRANSIT, RoutePointType.ORDER_PICKUP);

            assertThat(rp.canSkipLocation()).isTrue();
        }

        @Test
        void statusIsInProgressWithoutOrderPickupTaskInStatusMissedArrival() {
            RoutePoint rp = getRoutePoint(RoutePointStatus.IN_PROGRESS, RoutePointType.ORDER_PICKUP);
            rp.setTasks(List.of(
                    getOrderPickupTask(OrderPickupTaskStatus.NOT_STARTED)
            ));

            assertThat(rp.canSkipLocation()).isFalse();
        }

        @Test
        void statusIsInProgressWithOrderPickupTaskInStatusMissedArrival() {
            RoutePoint rp = getRoutePoint(RoutePointStatus.IN_PROGRESS, RoutePointType.ORDER_PICKUP);
            rp.setTasks(List.of(
                    getOrderPickupTask(OrderPickupTaskStatus.MISSED_ARRIVAL)
            ));

            assertThat(rp.canSkipLocation()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = RoutePointStatus.class, names = {"NOT_STARTED", "UNFINISHED", "FINISHED"})
        void statusIsOther(RoutePointStatus status) {
            RoutePoint rp = getRoutePoint(status, RoutePointType.ORDER_PICKUP);

            assertThat(rp.canSkipLocation()).isFalse();
        }
    }

    private RoutePoint getRoutePoint(RoutePointStatus status, RoutePointType type) {
        RoutePoint result = new RoutePoint();
        result.setStatus(status);
        result.setType(type);
        result.setTasks(List.of());
        return result;
    }

    private OrderPickupTask getOrderPickupTask(OrderPickupTaskStatus status) {
        OrderPickupTask result = new OrderPickupTask();
        result.setStatus(status.name());
        return result;
    }

}
