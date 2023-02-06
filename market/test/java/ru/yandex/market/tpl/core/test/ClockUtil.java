package ru.yandex.market.tpl.core.test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import lombok.experimental.UtilityClass;
import org.mockito.Mockito;

import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static org.mockito.Mockito.lenient;

/**
 * @author valter
 */
@UtilityClass
public class ClockUtil {

    public static Clock initFixed(Clock clock) {
        return initFixed(clock, defaultDateTime());
    }

    // monday
    public static LocalDateTime defaultDateTime() {
        return LocalDateTime.of(1990, 1, 1, 0, 0, 0);
    }

    public static Clock initFixed(Clock clock, LocalDateTime dateTime) {
        ZoneOffset zoneOffset = DateTimeUtil.DEFAULT_ZONE_ID;
        lenient().doReturn(dateTime.toInstant(zoneOffset))
                .when(clock).instant();
        lenient().doReturn(zoneOffset).when(clock).getZone();
        return clock;
    }

    public static Clock reset(Clock clock) {
        Mockito.doCallRealMethod().when(clock).instant();
        Mockito.doCallRealMethod().when(clock).getZone();
        return clock;
    }
}
