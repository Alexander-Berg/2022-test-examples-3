package ru.yandex.market.pvz.core.domain.pickup_point.calendar.log;

import java.time.LocalDate;
import java.time.ZoneOffset;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.calendar.override.PickupPointCalendarManager;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointCalendarLogCommandServiceTest {

    private static final LocalDate DAY = LocalDate.of(2021, 1, 10);

    private final TestableClock clock;
    private final TestPickupPointFactory pickupPointFactory;
    private final PickupPointCalendarLogCommandService pickupPointCalendarLogCommandService;
    private final PickupPointCalendarLogRepository pickupPointCalendarLogRepository;

    @MockBean
    private PickupPointCalendarManager calendarManager;

    @Test
    void startShiftOnWorkingDay() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .active(true)
                        .build()
        );

        when(calendarManager.isHoliday(any(), eq(DAY))).thenReturn(false);
        var offset = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        clock.setFixed(DAY.atTime(8, 0).toInstant(offset), offset);

        pickupPointCalendarLogCommandService.startShift(pickupPoint.getId());
        var pickupPointCalendarLog = pickupPointCalendarLogRepository.findByPickupPointIdAndDate(
                pickupPoint.getId(), LocalDate.now(clock)
        ).get();

        assertThat(pickupPointCalendarLog.getDate()).isEqualTo(DAY);
        assertThat(pickupPointCalendarLog.isWorkingDay()).isTrue();
        assertThat(pickupPointCalendarLog.getShiftStartedAt()).isEqualTo(pickupPoint.getTimeWithOffset(clock));
        assertThat(pickupPointCalendarLog.getIsWorkingDayBySchedule()).isTrue();
    }

    @Test
    void startShiftOnHoliday() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .active(true)
                        .build()
        );

        when(calendarManager.isHoliday(any(), eq(DAY))).thenReturn(true);
        var offset = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        clock.setFixed(DAY.atTime(13, 0).toInstant(offset), offset);

        pickupPointCalendarLogCommandService.startShift(pickupPoint.getId());
        var pickupPointCalendarLog = pickupPointCalendarLogRepository.findByPickupPointIdAndDate(
                pickupPoint.getId(), LocalDate.now(clock)
        ).get();

        assertThat(pickupPointCalendarLog.getDate()).isEqualTo(DAY);
        assertThat(pickupPointCalendarLog.isWorkingDay()).isFalse();
        assertThat(pickupPointCalendarLog.getShiftStartedAt()).isEqualTo(pickupPoint.getTimeWithOffset(clock));
        assertThat(pickupPointCalendarLog.getIsWorkingDayBySchedule()).isFalse();
    }

    @Test
    void startShiftOnWorkingDayTwice() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .active(true)
                        .build()
        );

        when(calendarManager.isHoliday(any(), eq(DAY))).thenReturn(false);
        var offset = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        clock.setFixed(DAY.atTime(8, 0).toInstant(offset), offset);

        pickupPointCalendarLogCommandService.startShift(pickupPoint.getId());

        clock.setFixed(DAY.atTime(9, 0).toInstant(offset), offset);

        pickupPointCalendarLogCommandService.startShift(pickupPoint.getId());
        var pickupPointCalendarLog = pickupPointCalendarLogRepository.findByPickupPointIdAndDate(
                pickupPoint.getId(), LocalDate.now(clock)
        ).get();

        assertThat(pickupPointCalendarLog.getDate()).isEqualTo(DAY);
        assertThat(pickupPointCalendarLog.isWorkingDay()).isTrue();
        clock.setFixed(DAY.atTime(8, 0).toInstant(offset), offset);
        assertThat(pickupPointCalendarLog.getShiftStartedAt()).isEqualTo(pickupPoint.getTimeWithOffset(clock));
        assertThat(pickupPointCalendarLog.getIsWorkingDayBySchedule()).isTrue();
    }

    @Test
    void startShiftForNotExistentPickupPoint() {
        assertThatThrownBy(() -> pickupPointCalendarLogCommandService.startShift(100L))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
    }

    @Test
    void logDayAsNotWorking() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .active(true)
                        .build()
        );

        when(calendarManager.isHoliday(any(), eq(DAY))).thenReturn(false);
        var offset = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        clock.setFixed(DAY.atTime(8, 0).toInstant(offset), offset);

        var actual = pickupPointCalendarLogCommandService.logDayAsNotWorking(pickupPoint.getId(), DAY);

        assertThat(actual.getDate()).isEqualTo(DAY);
        assertThat(actual.isWorkingDay()).isFalse();
        assertThat(actual.getShiftStartedAt()).isNull();
        assertThat(actual.getIsWorkingDayBySchedule()).isTrue();
    }

    @Test
    void logDayAsNotWorkingForExistentLog() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .active(true)
                        .build()
        );

        when(calendarManager.isHoliday(any(), eq(DAY))).thenReturn(false);
        var offset = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        clock.setFixed(DAY.atTime(8, 0).toInstant(offset), offset);

        pickupPointCalendarLogCommandService.startShift(pickupPoint.getId());

        var actual = pickupPointCalendarLogCommandService.logDayAsNotWorking(pickupPoint.getId(), DAY);

        assertThat(actual.getDate()).isEqualTo(DAY);
        assertThat(actual.isWorkingDay()).isFalse();
        assertThat(actual.getShiftStartedAt()).isEqualTo(pickupPoint.getTimeWithOffset(clock));
        assertThat(actual.getIsWorkingDayBySchedule()).isTrue();
    }

    @Test
    void logDayAsNotWorkingForNotExistentPickupPoint() {
        assertThatThrownBy(() -> pickupPointCalendarLogCommandService.logDayAsNotWorking(100L, DAY))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
    }

}
