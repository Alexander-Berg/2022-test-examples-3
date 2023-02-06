package ru.yandex.market.delivery.transport_manager.repository.mappers.trip;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.trip.Trip;
import ru.yandex.market.delivery.transport_manager.domain.entity.trip.TripPoint;
import ru.yandex.market.delivery.transport_manager.domain.entity.trip.TripStatus;
import ru.yandex.market.delivery.transport_manager.domain.filter.trip.TripFilter;

@DatabaseSetup({
    "/repository/route/route.xml",
    "/repository/route_schedule/route_schedule.xml",
    "/repository/trip/trips.xml"
})
public class TripMapperTest extends AbstractContextualTest {

    @Autowired
    private TripMapper mapper;

    @Test
    void getByTransportationUnitIds() {
        Set<Trip> trips = mapper.getByIds(List.of(10L));

        softly.assertThat(trips).containsExactlyInAnyOrder(trip());
    }

    @Test
    void getByTransportationUnitIdsWithRunId() {
        Set<Trip> trips = mapper.getByIds(List.of(11L));

        softly.assertThat(trips).containsExactlyInAnyOrder(tripWithExternalId());
    }

    @Test
    void getByIdsNullOrEmpty() {
        softly.assertThat(mapper.getByIds(List.of())).isEmpty();
        softly.assertThat(mapper.getByIds(null)).isEmpty();
    }

    @DatabaseSetup(value = "/repository/trip/trips_different_launch_time.xml")
    @Test
    void findIdsByScheduleIdsAndStartDateFrom() {
        softly.assertThat(
            mapper.findIdsByScheduleIdsAndStartDateFrom(List.of(101L), LocalDate.of(2021, 11, 4))
        ).containsExactlyInAnyOrder(11L, 12L);
    }

    @Test
    void countByTransportationIds1() {
        softly
            .assertThat(mapper.searchCount(new TripFilter().setTransportationIds(List.of(100L))))
            .isEqualTo(1);
    }

    @Test
    void countByTransportationIds2() {
        softly
            .assertThat(mapper.searchCount(new TripFilter().setTransportationIds(List.of(100L, 110L))))
            .isEqualTo(2);
    }

    @Test
    void findByTransportationIds1() {
        softly
            .assertThat(mapper.searchIds(
                new TripFilter().setTransportationIds(List.of(100L)),
                Pageable.unpaged()
            ))
            .containsExactlyInAnyOrder(10L);
    }

    @Test
    void findByTransportationIds2() {
        softly
            .assertThat(mapper.searchIds(
                new TripFilter().setTransportationIds(List.of(100L, 110L)),
                Pageable.unpaged()
            ))
            .containsExactlyInAnyOrder(10L, 11L);
    }

    @Test
    void getStatus() {
        softly.assertThat(mapper.getStatus(11L)).isEqualTo(TripStatus.ACCEPTED);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/trip/after/status_switch.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void switchStatusReturningCount() {
        softly.assertThat(mapper.switchStatusReturningCount(
                10L,
                TripStatus.NEW,
                TripStatus.SENT
            ))
            .isEqualTo(1L);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/trip/after/status_switch.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void switchStatusReturningCountWithoutPreviousStatusCheck() {
        softly.assertThat(mapper.switchStatusReturningCount(
                10L,
                null,
                TripStatus.SENT
            ))
            .isEqualTo(1L);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/trip/trips.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void switchStatusReturningCountSkip() {
        softly.assertThat(mapper.switchStatusReturningCount(
                11L,
                TripStatus.NEW,
                TripStatus.SENT
            ))
            .isEqualTo(0L);
    }

    Trip trip() {
        return new Trip()
            .setStartDate(LocalDate.parse("2021-10-10"))
            .setId(10L)
            .setRouteScheduleId(100L)
            .setPoints(Set.of(
                point(1L, 10L, 0, 100L),
                point(2L, 10L, 1, 101L)
            ))
            .setStatus(TripStatus.NEW)
            .setCreated(LocalDateTime.of(2021, 9, 29, 20, 0, 0));
    }

    Trip tripWithExternalId() {
        return new Trip()
            .setStartDate(LocalDate.parse("2020-07-18"))
            .setId(11L)
            .setStatus(TripStatus.ACCEPTED)
            .setRouteScheduleId(100L)
            .setExternalId("10001")
            .setPoints(Set.of(
                point(3L, 11L, 0, 110L),
                point(4L, 11L, 1, 111L)
            ))
            .setCreated(LocalDateTime.of(2020, 7, 11, 21, 0, 0));
    }


    TripPoint point(long id, long tripId, int index, long unitId) {
        return TripPoint.builder()
            .id(id)
            .tripId(tripId)
            .index(index)
            .transportationUnitId(unitId)
            .build();
    }
}
