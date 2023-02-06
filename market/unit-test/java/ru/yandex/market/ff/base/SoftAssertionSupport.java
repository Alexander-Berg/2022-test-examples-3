package ru.yandex.market.ff.base;

import java.time.ZoneId;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

import ru.yandex.market.ff.service.CalendaringClientCachingService;
import ru.yandex.market.ff.service.implementation.calendaring.CalendaringClientCachingServiceHolder;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class SoftAssertionSupport {
    protected final SoftAssertions assertions = new SoftAssertions();

    @AfterEach
    public final void triggerSoftAssertions() {
        assertions.assertAll();
    }

    @BeforeAll
    static void initMockedCalendaringService() {
        final CalendaringClientCachingService calendaringCachedService = mock(
                CalendaringClientCachingService.class);
        final CalendaringClientCachingServiceHolder calendaringServiceHolder =
                new CalendaringClientCachingServiceHolder(calendaringCachedService);

        when(calendaringCachedService.getZoneId(anyLong())).thenReturn(ZoneId.of("Europe/Moscow"));
        when(calendaringCachedService.getZoneId(300L)).thenReturn(ZoneId.of("Asia/Yekaterinburg"));
    }
}
