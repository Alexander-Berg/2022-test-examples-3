package ru.yandex.market.delivery.transport_manager.facade.trip;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.dto.trip.TripsRefresherInputData;
import ru.yandex.market.delivery.transport_manager.repository.mappers.route.RouteMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.route.RouteShortcutMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.route_schedule.RouteScheduleMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.trip.TripMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.trip.TripPointMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.trip.TripShortcutMapper;
import ru.yandex.market.delivery.transport_manager.service.TransportationService;
import ru.yandex.market.delivery.transport_manager.service.route.RouteReceiver;
import ru.yandex.market.delivery.transport_manager.service.route_schedule.RouteScheduleReceiver;
import ru.yandex.market.delivery.transport_manager.service.trip.TripReceiver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.transport_manager.util.RouteScheduleTripFactory.point;
import static ru.yandex.market.delivery.transport_manager.util.RouteScheduleTripFactory.route;
import static ru.yandex.market.delivery.transport_manager.util.RouteScheduleTripFactory.schedule;
import static ru.yandex.market.delivery.transport_manager.util.RouteScheduleTripFactory.transportation;
import static ru.yandex.market.delivery.transport_manager.util.RouteScheduleTripFactory.trip;

class TripsRefresherDataReceiverTest {

    private TripsRefresherDataReceiver dataReceiver;

    private RouteScheduleMapper routeScheduleMapper;
    private RouteMapper routeMapper;
    private TripMapper tripMapper;
    private TransportationService transportationService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        routeScheduleMapper = mock(RouteScheduleMapper.class);
        routeMapper = mock(RouteMapper.class);
        RouteShortcutMapper routeShortcutMapper = mock(RouteShortcutMapper.class);
        tripMapper = mock(TripMapper.class);
        var tripShortcutMapper = mock(TripShortcutMapper.class);
        TripPointMapper tripPointMapper = mock(TripPointMapper.class);
        transportationService = mock(TransportationService.class);
        clock = mock(Clock.class);

        dataReceiver = new TripsRefresherDataReceiver(
            new RouteScheduleReceiver(routeScheduleMapper),
            new RouteReceiver(routeMapper, routeShortcutMapper, clock),
            new TripReceiver(tripPointMapper, tripMapper, tripShortcutMapper),
            transportationService
        );
    }

    @Test
    void receive() {
        LocalDateTime now = LocalDateTime.of(2021, 11, 23, 10, 0);

        var route1 = route(1);
        var schedule11 = schedule(11, 1L);
        var trip111 = trip(111L, 11, "2021-12-01", 1, 2);
        var transportation1111 = transportation(1111L, 3, 4);
        var trip112 = trip(112L, 11, "2021-12-02", 3, 4);
        var transportation1121 = transportation(1121L, 1, 2);

        var route2 = route(2);
        var schedule21 = schedule(21, 2);
        var trip211 = trip(211L, 21, LocalDate.parse("2021-12-03"), point(0, 5), point(1, 6), point(2, 7), point(3, 8));
        var transportation2111 = transportation(2111L, 5, 6);
        var transportation2112 = transportation(2112L, 7, 8);

        when(routeScheduleMapper.getByIds(Set.of(11L, 21L)))
            .thenReturn(Set.of(schedule11, schedule21));
        when(routeMapper.getByIds(Set.of(1L, 2L)))
            .thenReturn(Set.of(route1, route2));
        when(tripMapper.findIdsByScheduleIdsAndStartDateFrom(Set.of(11L, 21L), now.toLocalDate()))
            .thenReturn(Set.of(111L, 112L, 121L));
        when(tripMapper.getByIds(Set.of(111L, 112L, 121L)))
            .thenReturn(Set.of(trip111, trip112, trip211));
        when(transportationService.getByUnitIds(Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L)))
            .thenReturn(Set.of(transportation1111, transportation1121, transportation2111, transportation2112));

        TripsRefresherInputData result = dataReceiver.receive(Set.of(11L, 21L), now);

        Assertions.assertEquals(result.getRoutes(), Set.of(route1, route2));
        Assertions.assertEquals(result.getSchedules(), Set.of(schedule11, schedule21));
        Assertions.assertEquals(result.getTrips(), Set.of(trip111, trip112, trip211));
        Assertions.assertEquals(
            result.getTransportations(),
            Set.of(transportation1111, transportation1121, transportation2111, transportation2112)
        );
    }
}
