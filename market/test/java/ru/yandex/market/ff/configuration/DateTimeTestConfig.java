package ru.yandex.market.ff.configuration;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.ff.service.DateTimeService;
import ru.yandex.market.ff.service.util.dateTime.DateTimePeriod;
import ru.yandex.market.logistic.api.utils.TimeZoneUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author avetokhin 11/01/18.
 */
public class DateTimeTestConfig {
    public static final LocalDateTime FIXED_NOW = LocalDateTime.of(2018, 1, 1, 10, 10, 10);
    public static final LocalDateTime FIXED_SUPPLY_DT = LocalDateTime.of(2018, 1, 5, 10, 10, 10);
    public static final Instant FIXED_SUPPLY_INSTANT = FIXED_SUPPLY_DT.toInstant(TimeZoneUtil.DEFAULT_OFFSET);

    public static final Instant FIXED_NOW_INSTANT = FIXED_NOW.toInstant(TimeZoneUtil.DEFAULT_OFFSET);
    public static final Instant FIXED_SUPPLY_FROM = LocalDateTime.of(2018, 1, 5, 0, 0, 0)
            .toInstant(TimeZoneUtil.DEFAULT_OFFSET);


    public static final LocalDateTime FIXED_WITHDRAW_FROM_DATE_TIME =
            LocalDateTime.of(2018, 1, 2, 10, 0, 0);
    public static final Instant FIXED_WITHDRAW_FROM =
            FIXED_WITHDRAW_FROM_DATE_TIME.toInstant(TimeZoneUtil.DEFAULT_OFFSET);

    public static final LocalDateTime FIXED_WITHDRAW_TO_DATE_TIME =
            LocalDateTime.of(2018, 1, 15, 10, 0, 0);
    public static final Instant FIXED_WITHDRAW_TO = FIXED_WITHDRAW_TO_DATE_TIME.toInstant(TimeZoneUtil.DEFAULT_OFFSET);

    private static final LocalDateTime FIXED_IN_TRANSIT_FROM_DATE = LocalDateTime.of(2017, Month.MARCH, 1, 0, 0);

    @Bean
    @Primary
    public DateTimeService dateTimeService(DateTimeService dateTimeService) {
        final DateTimeService service = spy(dateTimeService);
        when(service.localDateTimeNow()).thenReturn(FIXED_NOW);
        when(service.localDateNow()).thenReturn(FIXED_NOW.toLocalDate());
        when(service.customerReturnSupplyDateTime()).thenReturn(FIXED_SUPPLY_DT);
        when(service.nearestWithdrawPeriod()).thenReturn(Pair.of(FIXED_WITHDRAW_FROM, FIXED_WITHDRAW_TO));
        when(service.inTransitFromDate()).thenReturn(FIXED_IN_TRANSIT_FROM_DATE);
        when(service.nearestCalendaringWithdrawDateTimePeriod(any(), any()))
                .thenReturn(new DateTimePeriod(FIXED_WITHDRAW_FROM_DATE_TIME, FIXED_WITHDRAW_TO_DATE_TIME));
        return service;
    }

}
