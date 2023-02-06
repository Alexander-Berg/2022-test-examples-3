package ru.yandex.market.forecastint.utils;

import java.time.LocalDateTime;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.forecastint.service.TimeService;
import ru.yandex.market.forecastint.service.yt.YtTableService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class TestUtils {

    private TestUtils() {
        // NO_OP
    }

    public static void setMockedTimeServiceWithNowDateTime(@NotNull Object object, LocalDateTime nowDateTime) {
        setMockedTimeServiceWithNowDateTime(object, nowDateTime, nowDateTime);
    }

    public static void setMockedTimeServiceWithNowDateTime(
            @NotNull Object object,
            LocalDateTime nowDateTime,
            LocalDateTime nowDateTimeUTC
    ) {
        TimeService timeService = Mockito.mock(TimeService.class);
        when(timeService.getNowDate()).thenReturn(nowDateTime.toLocalDate());
        when(timeService.getNowDateTime()).thenReturn(nowDateTime);
        when(timeService.getNowDateTimeUTC()).thenReturn(nowDateTimeUTC);
        ReflectionTestUtils.setField(object, "timeService", timeService);
    }

    public static void setMockedYtTableExistsCheckService(
            @NotNull Object object,
            @NotNull Map<String, Boolean> tableExistsFlags
    ) {
        final YtTableService ytTableService = mock(YtTableService.class);
        tableExistsFlags.forEach((path, exists) -> {
            when(ytTableService.checkYtTableExists(path)).thenReturn(exists);
            when(ytTableService.checkYtTableNotExists(path)).thenReturn(!exists);
        });

        ReflectionTestUtils.setField(object, "ytTableService", ytTableService);
    }
}
