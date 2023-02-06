package ru.yandex.market.pvz.tms.executor.calendar;

import java.time.LocalDate;
import java.time.ZoneOffset;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.calendar.log.PickupPointCalendarLogRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@Import({CaptureActualWorkingDaysExecutor.class})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CaptureActualWorkingDaysExecutorTest {

    private static final ZoneOffset SYSTEM_OFFSET = DateTimeUtil.DEFAULT_ZONE_ID;

    private static final LocalDate DAY_1 = LocalDate.of(2021, 1, 10);
    private static final LocalDate DAY_2 = LocalDate.of(2021, 1, 11);

    private final TestableClock clock;
    private final TestPickupPointFactory pickupPointFactory;
    private final CaptureActualWorkingDaysExecutor executor;
    private final PickupPointCalendarLogRepository calendarLogRepository;

    @Test
    void testDaysRecorded() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .active(true)
                        .build()
        );

        doJobAt(DAY_1, 0, SYSTEM_OFFSET);
        doJobAt(DAY_2, 0, SYSTEM_OFFSET);

        checkCalendar(pickupPoint, DAY_1.minusDays(1));
        checkCalendar(pickupPoint, DAY_2.minusDays(1));
    }

    @Test
    void testDayNotRecordedTheSameDay() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .active(true)
                        .build()
        );

        doJobAt(DAY_1, 0, ZoneOffset.ofHours(+6));
        assertThat(calendarLogRepository.findAll()).isEmpty();
    }

    @Test
    void testNoCrashOnRepeatRun() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .active(true)
                        .build()
        );

        doJobAt(DAY_1, 0, SYSTEM_OFFSET);
        doJobAt(DAY_1, 0, SYSTEM_OFFSET);

        checkCalendar(pickupPoint, DAY_1.minusDays(1));
        checkCalendar(pickupPoint, DAY_1.minusDays(1));
    }

    @Test
    void testDayIsRecordedIfRunLater() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .active(true)
                        .build()
        );

        doJobAt(DAY_1, 12, SYSTEM_OFFSET);
        checkCalendar(pickupPoint, DAY_1.minusDays(1));
    }

    private void doJobAt(LocalDate date, int hour, ZoneOffset zoneOffset) {
        clock.setFixed(date.atTime(hour, 0).toInstant(zoneOffset), zoneOffset);
        executor.doRealJob(null);
    }

    private void checkCalendar(PickupPoint pickupPoint, LocalDate date) {
        var recordO = calendarLogRepository.findByPickupPointIdAndDate(pickupPoint.getId(), date);
        assertThat(recordO).isPresent();
        assertThat(recordO.get().isWorkingDay()).isEqualTo(false);
    }

}
