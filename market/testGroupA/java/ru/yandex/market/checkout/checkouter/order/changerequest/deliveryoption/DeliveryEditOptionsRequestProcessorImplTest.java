package ru.yandex.market.checkout.checkouter.order.changerequest.deliveryoption;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.DeliveryOption;
import ru.yandex.market.checkout.checkouter.order.TimeInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeliveryEditOptionsRequestProcessorImplTest extends AbstractWebTestBase {

    @Autowired
    private DeliveryEditOptionsRequestProcessorImpl sut;

    @Test
    public void mergeTimeIntervalsForDeliveryOptions() {
        final var day1 = LocalDate.now();
        final var day2 = day1.plusDays(1);
        final var day3 = day2.plusDays(1);
        final var input = Stream.of(
                option(1L, day1, day1, Set.of(timeInterval(10, 18))),
                option(1L, day1, day1, Set.of(timeInterval(8, 9))),
                option(1L, day1, day1, Set.of(timeInterval(9, 10))),
                option(1L, day1, day1, Set.of(timeInterval(11, 12), timeInterval(12, 13))),

                option(1L, day2, day2, Set.of(timeInterval(10, 18))),
                option(1L, day2, day2, Set.of(timeInterval(8, 9))),
                option(1L, day2, day2, Set.of(timeInterval(9, 10))),
                option(1L, day2, day2, Set.of(timeInterval(11, 12), timeInterval(12, 13))),
                option(1L, day2, day2, null),

                option(1L, day3, day3, Set.of(timeInterval(10, 18))),

                option(2L, day1, day1, Set.of(timeInterval(10, 18))),
                option(2L, day1, day1, Set.of(timeInterval(8, 9))),
                option(2L, day1, day1, Set.of(timeInterval(9, 10))),
                option(2L, day1, day1, Set.of(timeInterval(11, 12), timeInterval(12, 13))),

                option(2L, day2, day2, null),
                option(2L, day2, day2, Set.of(timeInterval(10, 18)))
        );

        final var expectedResult = List.of(
                option(1L, day1, day1, Set.of(
                        timeInterval(10, 18),
                        timeInterval(8, 9),
                        timeInterval(9, 10),
                        timeInterval(11, 12),
                        timeInterval(12, 13))),

                option(1L, day2, day2, Set.of(
                        timeInterval(10, 18),
                        timeInterval(8, 9),
                        timeInterval(9, 10),
                        timeInterval(11, 12),
                        timeInterval(12, 13))),

                option(1L, day3, day3, Set.of(timeInterval(10, 18))),

                option(2L, day1, day1, Set.of(
                        timeInterval(10, 18),
                        timeInterval(8, 9),
                        timeInterval(9, 10),
                        timeInterval(11, 12),
                        timeInterval(12, 13))),

                option(2L, day2, day2, Set.of(timeInterval(10, 18)))
        );

        final var actualResult = sut.mergeTimeIntervalsForDeliveryOptions(input)
                .collect(Collectors.toList());

        assertEquals(expectedResult, actualResult);
    }

    private static DeliveryOption option(Long deliveryServiceId,
                         LocalDate fromDate, LocalDate toDate,
                         Set<TimeInterval> timeIntervalOptions) {
        var option = new DeliveryOption();
        option.setDeliveryServiceId(deliveryServiceId);
        option.setFromDate(fromDate);
        option.setToDate(toDate);
        option.setTimeIntervalOptions(timeIntervalOptions);
        return option;
    }

    private static TimeInterval timeInterval(int fromHour, int toHour) {
        return new TimeInterval(LocalTime.of(fromHour, 0), LocalTime.of(toHour, 0));
    }



}
