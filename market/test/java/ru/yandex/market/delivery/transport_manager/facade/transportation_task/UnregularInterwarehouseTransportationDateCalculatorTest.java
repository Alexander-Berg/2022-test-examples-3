package ru.yandex.market.delivery.transport_manager.facade.transportation_task;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.delivery.transport_manager.util.IntRange;

class UnregularInterwarehouseTransportationDateCalculatorTest {
    private static final LocalDate TODAY = LocalDate.of(2020, 10, 20);
    private static final LocalTime NOW = LocalTime.of(15, 0);
    private UnregularInterwarehouseTransportationDateCalculator dateCalculator;

    @BeforeEach
    void setUp() {
        TestableClock clock = new TestableClock();
        clock.setFixed(
            TODAY.atTime(NOW).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );
        dateCalculator = new UnregularInterwarehouseTransportationDateCalculator(clock);
        dateCalculator.setOutboundIntervalDays(new IntRange(2, 3));
        dateCalculator.setMaxDaysOffset(10);
        dateCalculator.setMovementIntervalLengthHours(2);
        dateCalculator.afterPropertiesSet();
    }

    @Test
    void initOutboundInterval() {
        InterwarehouseTransportationDates dates = new InterwarehouseTransportationDates();
        dateCalculator.initOutboundInterval(
            dates,
            Set.of(
                TODAY, //working day 0
                TODAY.plusDays(3), //working day 1
                TODAY.plusDays(4), //working day 2
                TODAY.plusDays(7), //working day 3
                TODAY.plusDays(8), //working day 4
                TODAY.plusDays(9)  //working day 5
            ),
            NOW);

        Assertions.assertEquals(TODAY.plusDays(4).atTime(NOW), dates.outboundIntervalStart);
        Assertions.assertEquals(TODAY.plusDays(7).atTime(NOW), dates.outboundIntervalEnd);
    }

    @Test
    void noSutableOutboundWorkingDays() {
        InterwarehouseTransportationDates dates = new InterwarehouseTransportationDates();
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> dateCalculator.initOutboundInterval(
                dates,
                Set.of(
                    TODAY, //working day 0
                    TODAY.plusDays(3) //working day 1
                ),
                NOW)
        );
    }

    @Test
    void initMovingInterval() {
        InterwarehouseTransportationDates dates = new InterwarehouseTransportationDates();
        dates.outboundIntervalEnd = TODAY.plusDays(7).atTime(NOW);
        dateCalculator.initMovementInterval(dates);

        Assertions.assertEquals(TODAY.plusDays(7).atTime(NOW), dates.movementIntervalStart);
        Assertions.assertEquals(TODAY.plusDays(7).atTime(NOW).plusHours(2), dates.movementIntervalEnd);
    }

    @Test
    void initInboundInterval() {
        InterwarehouseTransportationDates dates = new InterwarehouseTransportationDates();
        dates.movementIntervalEnd = TODAY.plusDays(7).atTime(NOW).plusHours(2);
        dateCalculator.initInboundInterval(dates);

        Assertions.assertEquals(TODAY.plusDays(7).atTime(NOW).plusHours(2), dates.inboundIntervalStart);
        Assertions.assertEquals(TODAY.plusDays(8).atTime(NOW).plusHours(2), dates.inboundIntervalEnd);
    }
}
