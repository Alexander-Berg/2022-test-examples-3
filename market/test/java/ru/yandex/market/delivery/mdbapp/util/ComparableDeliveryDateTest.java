package ru.yandex.market.delivery.mdbapp.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

public class ComparableDeliveryDateTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void testCompareDates() {
        ComparableDeliveryDate winner = new ComparableDeliveryDate(
            LocalDate.of(2018, 2, 11),
            LocalDate.of(2018, 2, 11)
        )
            .setTimeFrom(LocalTime.MIDNIGHT)
            .setTimeTo(LocalTime.MIDNIGHT);

        ComparableDeliveryDate looser = new ComparableDeliveryDate(
            LocalDate.of(2018, 2, 12),
            LocalDate.of(2018, 2, 12)
        )
            .setTimeFrom(LocalTime.MIDNIGHT)
            .setTimeTo(LocalTime.MIDNIGHT);

        List<ComparableDeliveryDate> dates = Stream.of(winner, looser)
            .sorted(ComparableDeliveryDate::compareTo)
            .collect(Collectors.toList());

        softly.assertThat(winner).as("The first element should be the earliest date").isEqualTo(dates.get(0));
        softly.assertThat(looser).as("The second element should be the latest date").isEqualTo(dates.get(1));
    }

    @Test
    public void testCompareIntervals() {
        ComparableDeliveryDate winner = new ComparableDeliveryDate(
            LocalDate.of(2018, 2, 12),
            LocalDate.of(2018, 2, 12)
        )
            .setTimeFrom(LocalTime.of(13, 0))
            .setTimeTo(LocalTime.of(18, 0));

        ComparableDeliveryDate looser = new ComparableDeliveryDate(
            LocalDate.of(2018, 2, 12),
            LocalDate.of(2018, 2, 12)
        )
            .setTimeFrom(LocalTime.of(13, 0))
            .setTimeTo(LocalTime.of(14, 0));

        List<ComparableDeliveryDate> dates = Stream.of(winner, looser)
            .sorted(ComparableDeliveryDate::compareTo)
            .collect(Collectors.toList());

        softly.assertThat(winner).as("The first element should have the widest time interval").isEqualTo(dates.get(0));
        softly.assertThat(looser).as("The second element should have lower time interval").isEqualTo(dates.get(1));
    }

    @Test
    public void testCompareIntervals2() {
        ComparableDeliveryDate winner = new ComparableDeliveryDate(
            LocalDate.of(2018, 2, 12),
            LocalDate.of(2018, 2, 12)
        )
            .setTimeFrom(LocalTime.of(13, 0))
            .setTimeTo(LocalTime.of(18, 0));

        ComparableDeliveryDate looser = new ComparableDeliveryDate(
            LocalDate.of(2018, 2, 12),
            LocalDate.of(2018, 2, 12)
        )
            .setTimeFrom(LocalTime.of(13, 0))
            .setTimeTo(LocalTime.of(14, 0));

        List<ComparableDeliveryDate> dates = Stream.of(looser, winner)
            .sorted(ComparableDeliveryDate::compareTo)
            .collect(Collectors.toList());

        softly.assertThat(winner).as("The first element should have the widest time interval").isEqualTo(dates.get(0));
        softly.assertThat(looser).as("The second element should have lower time interval").isEqualTo(dates.get(1));
    }

    @Test
    public void testCompareDatesNoIntervals() {
        ComparableDeliveryDate winner = new ComparableDeliveryDate(
            LocalDate.of(2018, 2, 11),
            LocalDate.of(2018, 2, 11)
        );

        ComparableDeliveryDate looser = new ComparableDeliveryDate(
            LocalDate.of(2018, 2, 12),
            LocalDate.of(2018, 2, 12)
        );

        List<ComparableDeliveryDate> dates = Stream.of(winner, looser)
            .sorted(ComparableDeliveryDate::compareTo)
            .collect(Collectors.toList());

        softly.assertThat(winner).as("The first element should be the earliest date").isEqualTo(dates.get(0));
        softly.assertThat(looser).as("The second element should be the latest date").isEqualTo(dates.get(1));
    }

    @Test
    public void testCompareDatesRanges() {
        ComparableDeliveryDate winner = new ComparableDeliveryDate(
            LocalDate.of(2018, 2, 11),
            LocalDate.of(2018, 2, 12)
        );

        ComparableDeliveryDate looser = new ComparableDeliveryDate(
            LocalDate.of(2018, 2, 11),
            LocalDate.of(2018, 2, 13)
        );

        List<ComparableDeliveryDate> dates = Stream.of(winner, looser)
            .sorted(ComparableDeliveryDate::compareTo)
            .collect(Collectors.toList());

        softly.assertThat(winner).as("The first element should be the shortest earliest range").isEqualTo(dates.get(0));
        softly.assertThat(looser).as("The second element should larger range").isEqualTo(dates.get(1));
    }

    @Test
    public void testCompareDatesRanges2() {
        ComparableDeliveryDate winner = new ComparableDeliveryDate(
            LocalDate.of(2018, 2, 11),
            LocalDate.of(2018, 2, 14)
        );

        ComparableDeliveryDate looser = new ComparableDeliveryDate(
            LocalDate.of(2018, 2, 13),
            LocalDate.of(2018, 2, 14)
        );

        List<ComparableDeliveryDate> dates = Stream.of(winner, looser)
            .sorted(ComparableDeliveryDate::compareTo)
            .collect(Collectors.toList());

        softly.assertThat(winner).as("The first element should be earliest but longer range").isEqualTo(dates.get(0));
        softly.assertThat(looser).as("The second element should the shortest but later range").isEqualTo(dates.get(1));
    }
}
