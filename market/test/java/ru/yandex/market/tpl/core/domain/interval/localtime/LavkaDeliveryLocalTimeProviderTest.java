package ru.yandex.market.tpl.core.domain.interval.localtime;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;

import static org.assertj.core.api.Assertions.assertThat;

class LavkaDeliveryLocalTimeProviderTest {

    @Test
    void getForLocalDate() {
        var defaultDeliveryLocalTimeIntervalProvider = Mockito.mock(DefaultDeliveryLocalTimeIntervalProvider.class);
        LavkaDeliveryLocalTimeProvider lavkaDeliveryLocalTimeProvider = new LavkaDeliveryLocalTimeProvider(
                Map.of(DayOfWeek.MONDAY, LocalTimeInterval.valueOf("09:00-15:00")),
                LocalTimeInterval.valueOf("10:00-12:00"),
                defaultDeliveryLocalTimeIntervalProvider
        );
        LocalDate monday = LocalDate.of(2021, Month.APRIL, 26);
        assertThat(lavkaDeliveryLocalTimeProvider.getForLocalDate(monday))
                .isNotEmpty()
                .contains(LocalTimeInterval.valueOf("10:00-12:00"));
        LocalDate tuesday = monday.plusDays(1L);
        assertThat(lavkaDeliveryLocalTimeProvider.getForLocalDate(tuesday))
                .isEmpty();
    }
}
