package ru.yandex.market.delivery.transport_manager.repository.mappers.trip;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.trip.TripPointFlatInfo;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;

@DatabaseSetup({
    "/repository/route/route.xml",
    "/repository/route_schedule/route_schedule.xml",
    "/repository/trip/trips.xml"
})
class TripPointFlatInfoMapperTest extends AbstractContextualTest {
    @Autowired
    private TripPointFlatInfoMapper tripPointFlatInfoMapper;

    @Test
    void getByTrip() {
        softly
            .assertThat(tripPointFlatInfoMapper.getByTrip(10L))
            .containsExactlyInAnyOrder(
                new TripPointFlatInfo(1L, 0, 100, 100, TransportationUnitType.OUTBOUND),
                new TripPointFlatInfo(2L, 1, 100, 200, TransportationUnitType.INBOUND)
            );
    }
}
