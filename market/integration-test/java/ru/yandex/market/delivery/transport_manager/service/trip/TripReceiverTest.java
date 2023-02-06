package ru.yandex.market.delivery.transport_manager.service.trip;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteScheduleType;
import ru.yandex.market.delivery.transport_manager.domain.entity.trip.TripShortcut;
import ru.yandex.market.delivery.transport_manager.domain.filter.trip.TripFilter;

@DatabaseSetup({
    "/repository/route/full_routes.xml",
    "/repository/route_schedule/full_schedules.xml",
    "/repository/trip/before/trips_and_transportations.xml"
})
class TripReceiverTest extends AbstractContextualTest {

    @Autowired
    private TripReceiver tripReceiver;

    @Test
    void getCommonTrip() {
        softly.assertThat(
            tripReceiver.getCommonTrip(5L, 6L).get().getId()
        ).isEqualTo(2);
    }

    @Test
    void getCommonTripError() {
        softly.assertThatThrownBy(() ->
            tripReceiver.getCommonTrip(1L, 6L)
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Transportation units [1, 6] must belong to a common trip");
    }

    @Test
    void searchByTransportation() {
        TripFilter filter = new TripFilter().setTransportationIds(List.of(3L));
        assertContainsExactlyInAnyOrder(
            tripReceiver.search(filter, Pageable.unpaged()),
            tripShortcut(2, 102, Set.of(2L, 3L), "2021-11-27", "2021-11-27T10:30", null)
        );
    }

    @Test
    void searchPaged() {
        Pageable pageable = PageRequest.of(2, 1);
        assertContainsExactlyInAnyOrder(
            tripReceiver.search(new TripFilter(), pageable),
            tripShortcut(1, 100, Set.of(1L), "2021-11-26", "2021-11-26T10:30", "testname")
        );
    }

    @Test
    void searchByStartDate() {
        TripFilter filter = new TripFilter().setStartDate(List.of(LocalDate.of(2021, 11, 26)));
        assertContainsExactlyInAnyOrder(
            tripReceiver.search(filter, Pageable.unpaged()),
            tripShortcut(1, 100, Set.of(1L), "2021-11-26", "2021-11-26T10:30", "testname")
        );

        TripFilter filterWithTwoDates = new TripFilter().setStartDate(
            List.of(
                LocalDate.of(2021, 11, 26),
                LocalDate.of(2021, 11, 27)
            )
        );
        assertContainsExactlyInAnyOrder(
            tripReceiver.search(filterWithTwoDates, Pageable.unpaged()),
            tripShortcut(1, 100, Set.of(1L), "2021-11-26", "2021-11-26T10:30", "testname"),
            tripShortcut(2, 102, Set.of(2L, 3L), "2021-11-27", "2021-11-27T10:30", null)
        );
    }


    private TripShortcut tripShortcut(
        long id,
        long scheduleId,
        Set<Long> transportationIds,
        String startDate,
        String created,
        String name
    ) {
        return TripShortcut.builder()
            .id(id)
            .routeScheduleId(scheduleId)
            .routeScheduleType(RouteScheduleType.LINEHAUL)
            .transportationIds(transportationIds)
            .startDate(LocalDate.parse(startDate))
            .created(LocalDateTime.parse(created))
            .routeName(name)
            .build();
    }
}
