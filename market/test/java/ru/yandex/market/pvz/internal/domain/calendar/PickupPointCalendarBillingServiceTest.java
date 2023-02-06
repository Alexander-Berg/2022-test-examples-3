package ru.yandex.market.pvz.internal.domain.calendar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointCalendarLogFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleDayTestParams.DEFAULT_TIME_FROM;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointCalendarBillingServiceTest {

    private static final LocalDate FROM = LocalDate.of(2021, 1, 1);
    private static final LocalDate TO = LocalDate.of(2021, 1, 31);

    private final TestableClock clock;

    private final TestPickupPointFactory pickupPointFactory;
    private final PickupPointCalendarBillingService calendarBillingService;
    private final TestPickupPointCalendarLogFactory pickupPointCalendarLogFactory;

    @Test
    void testGetWorkingDaysMap() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());

        assertThat(calendarBillingService.getWorkingDays(FROM, TO).getPickupPointWorkingDays()).isEmpty();

        pickupPointCalendarLogFactory.startShiftAtDate(pickupPoint.getId(), LocalDateTime.of(2021, 1, 1,
                DEFAULT_TIME_FROM.getHour() + 1, 0), zone, clock);
        pickupPointCalendarLogFactory.startShiftAtDate(pickupPoint.getId(), LocalDateTime.of(2021, 1, 15,
                DEFAULT_TIME_FROM.getHour() + 1, 0), zone, clock);
        pickupPointCalendarLogFactory.startShiftAtDate(pickupPoint.getId(), LocalDateTime.of(2021, 1, 31,
                DEFAULT_TIME_FROM.getHour() + 1, 0), zone, clock);

        pickupPointCalendarLogFactory.startShiftAtDate(pickupPoint.getId(), LocalDateTime.of(2021, 12, 31,
                DEFAULT_TIME_FROM.getHour() + 1, 0), zone, clock);
        pickupPointCalendarLogFactory.logDayAsNotWorking(pickupPoint.getId(), LocalDate.of(2021, 1, 16));
        pickupPointCalendarLogFactory.startShiftAtDate(pickupPoint.getId(), LocalDateTime.of(2021, 2, 1,
                DEFAULT_TIME_FROM.getHour() + 1, 0), zone, clock);

        assertThat(calendarBillingService.getWorkingDays(FROM, TO).getPickupPointWorkingDays()).containsOnly(
                Map.entry(pickupPoint.getId(), Set.of(
                        LocalDate.of(2021, 1, 1),
                        LocalDate.of(2021, 1, 15),
                        LocalDate.of(2021, 1, 31)
                ))
        );

    }

}
