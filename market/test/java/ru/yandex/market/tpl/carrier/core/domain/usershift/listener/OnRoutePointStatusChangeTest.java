package ru.yandex.market.tpl.carrier.core.domain.usershift.listener;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePointEvent;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePointEventsRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePointStatus;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;

@RequiredArgsConstructor(onConstructor_=@Autowired)
@CoreTestV2
class OnRoutePointStatusChangeTest {

    private final RoutePointEventsRepository routePointRepository;
    private final RunGenerator runGenerator;
    private final RunHelper runHelper;
    private final TestUserHelper testUserHelper;

    @Test
    void shouldSaveRoutePointStatusHistory() {
        CarrierAuditTracer.putSource(CarrierSource.DELIVERY);
        var user = testUserHelper.findOrCreateUser(1L);
        var transport = testUserHelper.findOrCreateTransport();

        CarrierAuditTracer.putSource(CarrierSource.TRANSPORT_MANAGER);
        Run run = runGenerator.generate();

        CarrierAuditTracer.putSource(CarrierSource.DELIVERY);
        UserShift userShift = runHelper.assignUserAndTransport(run, user, transport);

        CarrierAuditTracer.putSource(CarrierSource.COURIER);
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishCollectDropships(userShift.getFirstRoutePoint());
        testUserHelper.finishFullReturnAtEnd(userShift);

        List<RoutePointEvent> events = routePointRepository.findByUserShiftId(userShift.getId());
        Assertions.assertThat(events)
                .hasSize(8);

        List<RoutePointEvent> routePointEventList = StreamEx.of(events)
                .filterBy(RoutePointEvent::getRoutePointId, userShift.getFirstRoutePoint().getId())
                .collect(Collectors.toList());

        Assertions.assertThat(routePointEventList).hasSize(4);

        Assertions.assertThat(routePointEventList)
                .extracting(RoutePointEvent::getStatusBefore)
                .containsExactly(null, RoutePointStatus.NOT_STARTED, RoutePointStatus.IN_TRANSIT, RoutePointStatus.IN_PROGRESS);

        Assertions.assertThat(routePointEventList)
                .extracting(RoutePointEvent::getStatusAfter)
                .containsExactly(RoutePointStatus.NOT_STARTED, RoutePointStatus.IN_TRANSIT, RoutePointStatus.IN_PROGRESS, RoutePointStatus.FINISHED);

        Assertions.assertThat(routePointEventList)
                .extracting(RoutePointEvent::getSource)
                .containsExactly(CarrierSource.DELIVERY, CarrierSource.COURIER, CarrierSource.COURIER, CarrierSource.COURIER);

    }

}
