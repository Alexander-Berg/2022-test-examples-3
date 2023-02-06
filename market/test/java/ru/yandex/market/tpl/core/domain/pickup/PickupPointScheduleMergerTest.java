package ru.yandex.market.tpl.core.domain.pickup;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.order.locker.PickupPointType;
import ru.yandex.market.tpl.core.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointScheduleMergerTest {

    private final PickupPointRepository pickupPointRepository;
    private final PickupPointScheduleMerger pickupPointScheduleMerger;

    private PickupPoint pickupPoint;

    @BeforeEach
    public void init() {
        pickupPoint = new PickupPoint();
        pickupPoint.setCode("test");
        pickupPoint.setType(PickupPointType.LOCKER);
        pickupPoint.setPartnerSubType(PartnerSubType.LOCKER);
        pickupPoint.setLogisticPointId(1L);
        pickupPoint.putScheduleRecord(
                DayOfWeek.MONDAY,
                new LocalTimeInterval(LocalTime.of(9, 0), LocalTime.of(18, 0))
        );
        pickupPoint.putScheduleRecord(
                DayOfWeek.TUESDAY,
                new LocalTimeInterval(LocalTime.of(9, 0), LocalTime.of(18, 0))
        );
        pickupPointRepository.save(pickupPoint);

    }


    @Test
    void mergeSchedules() {
        assertThat(pickupPoint.getSchedules()).containsKeys(DayOfWeek.MONDAY);
        Map<DayOfWeek, LocalTimeInterval> fromYt = Map.of(
                DayOfWeek.TUESDAY,
                new LocalTimeInterval(LocalTime.of(10, 0), LocalTime.of(19, 0)),
                DayOfWeek.WEDNESDAY,
                new LocalTimeInterval(LocalTime.of(11, 0), LocalTime.of(20, 0))
        );
        pickupPointScheduleMerger.bulkMergeLogisticPointSchedules(
                Set.of(pickupPoint.getLogisticPointId()),
                Map.of(pickupPoint.getLogisticPointId(), new PickupPointExternalSchedule(fromYt, null))
        );

        assertThat(pickupPoint.getSchedules()).containsKeys(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY);
        assertThat(pickupPoint.getSchedules().get(DayOfWeek.TUESDAY).getLocalTimeInterval())
                .isEqualTo(new LocalTimeInterval(LocalTime.of(10, 0), LocalTime.of(19, 0)));
        assertThat(pickupPoint.getSchedules()).doesNotContainKey(DayOfWeek.MONDAY);
    }

    @Test
    void mergeWithCustomSchedules() {
        Map<DayOfWeek, LocalTimeInterval> fromYt = Map.of(
                DayOfWeek.TUESDAY,
                new LocalTimeInterval(LocalTime.of(10, 0), LocalTime.of(19, 0)),
                DayOfWeek.WEDNESDAY,
                new LocalTimeInterval(LocalTime.of(11, 0), LocalTime.of(20, 0))
        );

        pickupPointScheduleMerger.bulkMergeLogisticPointSchedules(
                Set.of(pickupPoint.getLogisticPointId()),
                Map.of(pickupPoint.getLogisticPointId(), new PickupPointExternalSchedule(fromYt,
                        new LocalTimeInterval(LocalTime.of(12, 0), LocalTime.of(15, 30))))
        );

        assertThat(pickupPoint.getSchedules().get(DayOfWeek.TUESDAY).getLocalTimeInterval())
                .isEqualTo(new LocalTimeInterval(LocalTime.of(12, 0), LocalTime.of(15, 30)));
        assertThat(pickupPoint.getSchedules().get(DayOfWeek.WEDNESDAY).getLocalTimeInterval())
                .isEqualTo(new LocalTimeInterval(LocalTime.of(12, 0), LocalTime.of(15, 30)));
        assertThat(pickupPoint.getSchedules()).hasSize(2);
    }

}
