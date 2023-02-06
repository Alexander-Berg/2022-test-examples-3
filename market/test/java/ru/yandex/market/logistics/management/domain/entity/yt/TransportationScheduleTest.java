package ru.yandex.market.logistics.management.domain.entity.yt;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TransportationScheduleTest {
    final TransportationSchedule t = TransportationSchedule.builder()
        .outboundPartnerId(1L)
        .outboundLogisticsPointId(2L)
        .movingPartnerId(3L)
        .movementSegmentId(100L)
        .inboundPartnerId(4L)
        .inboundLogisticsPointId(5L)
        .transportationSchedule(List.of(new YtSchedule(1L, 1, LocalTime.of(10, 0), LocalTime.of(18, 0), false)))
        .transportationHolidays(List.of())
        .volume(1)
        .weight(2)
        .duration(3)
        .transportationType(TransportationType.ORDERS_OPERATION)
        .routingConfig(new YtRoutingConfig(
                true,
                null,
                BigDecimal.ZERO,
                0d,
                false,
                ""
        ))
        .build();

    @Test
    void hash() {
        Assertions.assertEquals("3ad47ff60aeb722d6353372b329a3f5a", t.hash());
    }

    @Test
    void nulls() {
        TransportationSchedule.builder().build().hash();
    }

    @Test
    void hashChange() {
        Assertions.assertNotEquals(t.hash(), t.toBuilder().outboundPartnerId(10L).build().hash());
        Assertions.assertNotEquals(t.hash(), t.toBuilder().outboundLogisticsPointId(10L).build().hash());
        Assertions.assertNotEquals(t.hash(), t.toBuilder().movingPartnerId(10L).build().hash());
        Assertions.assertNotEquals(t.hash(), t.toBuilder().inboundPartnerId(10L).build().hash());
        Assertions.assertNotEquals(t.hash(), t.toBuilder().inboundLogisticsPointId(10L).build().hash());
        Assertions.assertNotEquals(t.hash(), t.toBuilder().movementSegmentId(3L).build().hash());

        Assertions.assertNotEquals(t.hash(), t.toBuilder().transportationSchedule(
            Collections.singletonList(new YtSchedule(1L, 1, LocalTime.of(8, 0), LocalTime.of(18, 0), false))
        ).build().hash());

        Assertions.assertNotEquals(t.hash(), t.toBuilder().transportationSchedule(
            Collections.singletonList(new YtSchedule(1L, 2, LocalTime.of(10, 0), LocalTime.of(18, 0), false))
        ).build().hash());

        Assertions.assertNotEquals(t.hash(), t.toBuilder().volume(20).build().hash());
        Assertions.assertNotEquals(t.hash(), t.toBuilder().weight(20).build().hash());
        Assertions.assertNotEquals(t.hash(), t.toBuilder().duration(20).build().hash());
        Assertions.assertNotEquals(t.hash(), t.toBuilder().partnerCutoffData(new PartnerCutoffData()).build().hash());
    }

    @Test
    void scheduleOrder() {
        final TransportationSchedule t1 = TransportationSchedule.builder()
            .outboundPartnerId(1L)
            .outboundLogisticsPointId(2L)
            .movingPartnerId(3L)
            .movementSegmentId(100L)
            .inboundPartnerId(4L)
            .inboundLogisticsPointId(5L)
            .transportationSchedule(List.of(
                new YtSchedule(1L, 1, LocalTime.of(10, 0), LocalTime.of(18, 0), false),
                new YtSchedule(2L, 2, LocalTime.of(10, 0), LocalTime.of(18, 0), false)
            ))
            .transportationHolidays(List.of())
            .volume(1)
            .weight(2)
            .duration(3)
            .transportationType(TransportationType.ORDERS_OPERATION)
            .build();

        final TransportationSchedule t2 = t1.toBuilder()
            .transportationSchedule(List.of(
                new YtSchedule(2L, 2, LocalTime.of(10, 0), LocalTime.of(18, 0), false),
                new YtSchedule(1L, 1, LocalTime.of(10, 0), LocalTime.of(18, 0), false)
            ))
            .build();

        Assertions.assertEquals(t1.hash(), t2.hash());
    }

    @Test
    void scheduleIdsSkip() {
        final TransportationSchedule t2 = t.toBuilder()
            .transportationSchedule(Collections.singletonList(
                new YtSchedule(10000L, 1, LocalTime.of(10, 0), LocalTime.of(18, 0), false))
            )
            .build();
        Assertions.assertEquals(t.hash(), t2.hash());
    }

    @Test
    void scheduleNullAndEmptySchedules() {
        final TransportationSchedule t1 = this.t.toBuilder()
            .transportationSchedule(Collections.emptyList())
            .build();
        final TransportationSchedule t2 = this.t.toBuilder()
            .transportationSchedule(null)
            .build();

        Assertions.assertEquals(t1.hash(), t2.hash());
    }
}
