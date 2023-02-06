package ru.yandex.market.delivery.transport_manager.repository.mappers.trip;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

public class TripPointMapperTest extends AbstractContextualTest {

    @Autowired
    private TripPointMapper mapper;

    @DatabaseSetup({
        "/repository/route/route.xml",
        "/repository/route_schedule/route_schedule.xml",
        "/repository/trip/trips.xml"
    })
    @Test
    void getByTransportationUnitIds() {
        List<Long> points = mapper.getTripIdsByTransportationUnitIds(Set.of(100L, 101L));

        softly.assertThat(points).containsExactlyInAnyOrder(10L);
    }

    @DatabaseSetup({
        "/repository/route/route.xml",
        "/repository/route_schedule/route_schedule.xml",
        "/repository/trip/trips.xml"
    })
    @Test
    void getTripByMovementId() {
        List<Long> tripIds = mapper.getTripIdsByMovementId(100L);

        softly.assertThat(tripIds).containsExactlyInAnyOrder(10L);
    }

    @DatabaseSetup({
        "/repository/route/full_routes.xml",
        "/repository/route_schedule/full_schedules.xml",
        "/repository/trip/before/trips_and_transportations.xml"
    })
    @ExpectedDatabase(
        value = "/repository/trip/after/index_incremented.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void incrementIndexFromPosition() {
        mapper.incrementIndexFromPosition(2L, 1);
    }
}
