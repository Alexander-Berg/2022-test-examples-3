package ru.yandex.market.delivery.transport_manager.service.trip;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.trip_included_outbounds.Interval;
import ru.yandex.market.delivery.transport_manager.domain.dto.trip_included_outbounds.TripAggregatedInfo;
import ru.yandex.market.delivery.transport_manager.util.TimeUtil;

@DatabaseSetup({
    "/repository/route/full_routes.xml",
    "/repository/route_schedule/full_schedules.xml",
    "/repository/trip/before/trips_and_transportations.xml"
})
class TripAggregatedInfoReceiverTest extends AbstractContextualTest {

    @Autowired
    private TripAggregatedInfoReceiver tripAggregatedInfoReceiver;

    @Test
    void getTripAggregatedInfoInternal() {
        softly.assertThat(tripAggregatedInfoReceiver.getTripAggregatedInfoInternal(2L))
            .isEqualTo(new TripAggregatedInfo(
                1L,
                33,
                LocalDateTime.of(2021, 11, 25, 21, 10),
                Map.of(
                    10L, 1L,
                    20L, 2L,
                    30L, 3L
                ),
                Map.of(
                    10L, new Interval(
                        OffsetDateTime.of(2021, 11, 26, 10, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET),
                        OffsetDateTime.of(2021, 11, 26, 11, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET)
                    ),
                    20L, new Interval(
                        OffsetDateTime.of(2021, 11, 26, 12, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET),
                        OffsetDateTime.of(2021, 11, 26, 13, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET)
                    ),
                    30L, new Interval(
                        OffsetDateTime.of(2021, 11, 26, 15, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET),
                        OffsetDateTime.of(2021, 11, 26, 17, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET)
                    )
                )
            ));
    }
}
