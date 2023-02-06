package ru.yandex.mail.micronaut.micrometer;

import lombok.val;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import ru.yandex.mail.micronaut.micrometer.unistat.UnistatSlaConfiguration;

import java.time.Duration;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofNanos;
import static org.assertj.core.api.Assertions.assertThat;

class SlaGeneratorTest {
    @Test
    void generationTest() {
        val generator = new UnistatSlaConfiguration.Generator();
        generator.setLeft(ofMillis(1));
        generator.setPivot(ofMillis(10));
        generator.setRight(ofMillis(40));
        generator.setTimeout(ofMillis(500));

        val actual = StreamEx.of(generator.generate())
            .mapToLong(Duration::toNanos)
            .map(v -> v / 1000)
            .toArray();

        assertThat(actual)
            .containsExactly(200L, 400L, 600L, 800L, 1000L, 1450L, 1900L, 2350L, 2800L, 3250L, 3700L, 4150L, 4600L,
                5050L, 5500L, 5950L, 6400L, 6850L, 7300L, 7750L, 8200L, 8650L, 9100L, 9550L, 10000L, 12000L, 14000L,
                16000L, 18000L, 20000L, 22000L, 24000L, 26000L, 28000L, 30000L, 32000L, 34000L, 36000L, 38000L, 40000L,
                132000L, 224000L, 316000L, 408000L, 500000L, 600000L, 700000L, 800000L, 900000L);
    }

    @Test
    void denseRangeTest() {
        val generator = new UnistatSlaConfiguration.Generator();
        generator.setLeft(ofNanos(1));
        generator.setPivot(ofNanos(2));
        generator.setRight(ofNanos(3));
        generator.setTimeout(ofNanos(4));

        val actual = StreamEx.of(generator.generate())
            .mapToLong(Duration::toNanos)
            .toArray();

        assertThat(actual)
            .containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L);
    }
}
