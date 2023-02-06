package ru.yandex.market.clab;

import org.junit.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 22.10.2018
 */
public class ControlledClockTest {

    @Test
    public void pauseUnpause() throws InterruptedException {
        Clock real = Clock.systemUTC();
        ControlledClock clock = new ControlledClock(real);
        assertThat(clock.isPaused()).isFalse();
        Instant timeInThePast = clock.instant();
        sleepFewMilliseconds();
        assertThat(clock.instant()).isAfter(timeInThePast);

        clock.pause();
        assertThat(clock.isPaused()).isTrue();
        Instant afterPause = clock.instant();
        sleepFewMilliseconds();
        assertThat(clock.instant()).isEqualTo(afterPause);
        sleepFewMilliseconds();
        assertThat(clock.instant()).isEqualTo(afterPause);

        clock.unpause();
        assertThat(clock.isPaused()).isFalse();
        sleepFewMilliseconds();
        Instant unpaused = clock.instant();
        assertThat(unpaused).isBefore(real.instant()).isAfter(afterPause);
        sleepFewMilliseconds();
        assertThat(clock.instant()).isAfter(unpaused);

        Instant someTimeInThePast = clock.instant();
        clock.unpause();
        sleepFewMilliseconds();
        clock.unpause();
        clock.unpause();
        sleepFewMilliseconds();
        clock.unpause();
        assertThat(clock.instant()).isAfter(someTimeInThePast);
    }


    @Test
    public void tickMinutePaused() throws InterruptedException {
        ControlledClock clock = new ControlledClock(Clock.systemUTC());
        clock.pause();
        Instant fixedTime = clock.instant();
        clock.tickMinute();
        sleepFewMilliseconds();
        assertThat(clock.instant()).isAfter(fixedTime);
        assertThat(Duration.between(fixedTime, clock.instant())).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    public void tickMinuteUnpaused() {
        Clock real = Clock.systemUTC();
        ControlledClock clock = new ControlledClock(real);
        clock.tickMinute();
        assertThat(Duration.between(real.instant(), clock.instant()))
            .isBetween(Duration.ofMinutes(1).minusSeconds(1), Duration.ofMinutes(1).plusSeconds(1));
    }


    @Test
    public void withTimezone() throws InterruptedException {
        ControlledClock clock = new ControlledClock(Clock.systemUTC());
        assertThat(clock.getZone()).isEqualTo(Clock.systemUTC().getZone());
        ZoneId zoneId = ZoneId.of("Europe/Paris");
        assertThat(clock.withZone(zoneId).getZone()).isEqualTo(Clock.systemUTC().withZone(zoneId).getZone());

        assertThat(clock.withZone(zoneId)).isNotInstanceOf(ControlledClock.class);
        Clock controlledWithZone = clock.withZone(zoneId).withZone(ZoneId.systemDefault()).withZone(zoneId);
        assertThat(controlledWithZone.getZone()).isEqualTo(zoneId);
        clock.pause();

        Instant afterPause = controlledWithZone.instant();
        sleepFewMilliseconds();
        assertThat(controlledWithZone.instant()).withFailMessage("clock with zone should be controlled by main")
            .isEqualTo(afterPause);

        clock.tickMinute();
        assertThat(Duration.between(afterPause, controlledWithZone.instant())).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    public void sequentialPauseUnpause() throws InterruptedException {
        Clock real = Clock.systemUTC();
        ControlledClock clock = new ControlledClock(real);
        clock.pause();
        Instant afterPause = clock.instant();

        sleepFewMilliseconds();
        clock.pause();
        clock.pause();
        sleepFewMilliseconds();
        clock.pause();

        assertThat(clock.instant()).isEqualTo(afterPause);


        clock.unpause();
        sleepFewMilliseconds();
        assertThat(clock.instant()).isAfter(afterPause);
    }

    private static void sleepFewMilliseconds() throws InterruptedException {
        Thread.sleep(20);
    }

}
