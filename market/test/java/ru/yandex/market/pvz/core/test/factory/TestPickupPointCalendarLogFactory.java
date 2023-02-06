package ru.yandex.market.pvz.core.test.factory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.pickup_point.calendar.log.PickupPointCalendarLog;
import ru.yandex.market.pvz.core.domain.pickup_point.calendar.log.PickupPointCalendarLogCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.calendar.log.PickupPointCalendarLogRepository;

@Transactional
public class TestPickupPointCalendarLogFactory {

    @Autowired
    private PickupPointCalendarLogCommandService calendarLogCommandService;

    @Autowired
    private PickupPointCalendarLogRepository pickupPointCalendarLogRepository;

    public PickupPointCalendarLog startShiftAtDate(long pickupPointId,
                                                   LocalDateTime dateTime,
                                                   ZoneOffset zone,
                                                   TestableClock clock) {
        Instant prevClock = clock.instant();
        clock.setFixed(dateTime.atZone(zone).toInstant(), zone);
        calendarLogCommandService.startShift(pickupPointId);
        var calendarLog = pickupPointCalendarLogRepository.findByPickupPointIdAndDate(
                pickupPointId, LocalDate.now(clock)
        ).get();
        clock.setFixed(prevClock, zone);
        return calendarLog;
    }

    public PickupPointCalendarLog logDayAsNotWorking(long pickupPointId, LocalDate date) {
        return calendarLogCommandService.logDayAsNotWorking(pickupPointId, date);
    }

}
