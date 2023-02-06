package ru.yandex.market.global.checkout.domain.queue.task.notification;

import java.time.Duration;
import java.time.LocalTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static ru.yandex.market.global.checkout.domain.queue.task.notification.SendPushProducer.getSleepTimeDuration;

public class SendPushProducerTimeTest {

    @Test
    public void testIsInSleepTime() {

        Assertions.assertThat(getSleepTimeDuration(
                        LocalTime.parse("02:00:00"),
                        LocalTime.parse("06:00:00"),
                        LocalTime.parse("08:00:00")))
                .usingRecursiveComparison()
                .isEqualTo(null);

        Assertions.assertThat(getSleepTimeDuration(
                        LocalTime.parse("00:00:00"),
                        LocalTime.parse("06:00:00"),
                        LocalTime.parse("23:00:00")))
                .usingRecursiveComparison()
                .isEqualTo(null);

        Assertions.assertThat(getSleepTimeDuration(
                        LocalTime.parse("22:30:00"),
                        LocalTime.parse("23:00:00"),
                        LocalTime.parse("08:00:00")))
                .usingRecursiveComparison()
                .isEqualTo(null);

        Assertions.assertThat(getSleepTimeDuration(
                        LocalTime.parse("23:00:00"),
                        LocalTime.parse("23:00:00"),
                        LocalTime.parse("08:00:00")))
                .usingRecursiveComparison()
                .isEqualTo(null);

        Assertions.assertThat(getSleepTimeDuration(
                        LocalTime.parse("08:00:00"),
                        LocalTime.parse("23:00:00"),
                        LocalTime.parse("08:00:00")))
                .usingRecursiveComparison()
                .isEqualTo(null);

        Assertions.assertThat(getSleepTimeDuration(
                        LocalTime.parse("23:30:00"),
                        LocalTime.parse("23:00:00"),
                        LocalTime.parse("08:00:00")))
                .usingRecursiveComparison()
                .isEqualTo(Duration.parse("PT8H30M"));

        Assertions.assertThat(getSleepTimeDuration(
                        LocalTime.parse("02:00:00"),
                        LocalTime.parse("23:00:00"),
                        LocalTime.parse("08:00:00")))
                .usingRecursiveComparison()
                .isEqualTo(Duration.parse("PT6H"));
    }
}
