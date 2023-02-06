package ru.yandex.travel.hotels.searcher.partners;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@Profile("fake-clock")
public class FakeClockConfiguration {

    @Bean
    public Clock getClock() {
        final ZoneId fakeZone = ZoneId.systemDefault();
        final Instant fakeTime = ZonedDateTime.of(
                LocalDate.of(3018, Month.AUGUST, 8),
                LocalTime.of(0, 0),
                fakeZone
        ).toInstant();
        return Clock.fixed(fakeTime, fakeZone);
    }
}
