package ru.yandex.market.tpl.core.domain.interval.localtime;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PickupPointDeliveryLocalTimeProviderTest {

    @Test
    void getForLocalDate() {
        var pickupPointDeliveryLocalTimeIntervalCalculator = Mockito.mock(PickupPointDeliveryLocalTimeIntervalCalculator.class);
        var pickupPointDeliveryLocalTimeProvider =
                new PickupPointDeliveryLocalTimeProvider(
                        pickupPointDeliveryLocalTimeIntervalCalculator,
                Map.of(DayOfWeek.MONDAY, LocalTimeInterval.valueOf("09:00-13:00")),
                LocalTimeInterval.valueOf("10:00-15:00")
        );
        Mockito.when(pickupPointDeliveryLocalTimeIntervalCalculator.calculateForPickupPoint(
                LocalTimeInterval.valueOf("09:00-13:00"),
                LocalTimeInterval.valueOf("10:00-15:00")
        )).thenReturn(LocalTimeInterval.valueOf("11:00-13:00"));
        LocalDate monday = LocalDate.of(2021, Month.APRIL, 26);
        assertThat(pickupPointDeliveryLocalTimeProvider.getForLocalDate(monday))
                .isNotEmpty()
                .contains(LocalTimeInterval.valueOf("11:00-13:00"));
        LocalDate tuesday = LocalDate.of(2021, Month.APRIL, 26).plusDays(1L);
        assertThat(pickupPointDeliveryLocalTimeProvider.getForLocalDate(tuesday))
                .isEmpty();
        assertThat(pickupPointDeliveryLocalTimeProvider.getFallbackInterval())
                .isEqualTo(LocalTimeInterval.valueOf("10:00-15:00"));
    }

}
