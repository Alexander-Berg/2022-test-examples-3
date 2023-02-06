package ru.yandex.travel.commons.lang;

import java.time.Duration;
import java.time.LocalDate;

import org.javamoney.moneta.Money;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComparatorUtilsTest {
    @Test
    public void testLessThat() {
        assertThat(ComparatorUtils.isLessThan(1, 2)).isTrue();
        assertThat(ComparatorUtils.isLessThan(1, 1)).isFalse();
        assertThat(ComparatorUtils.isLessThan(1, 0)).isFalse();

        assertThat(ComparatorUtils.isLessThan(Duration.ofSeconds(1), Duration.ofSeconds(2))).isTrue();
        assertThat(ComparatorUtils.isLessThan(LocalDate.parse("2021-06-25"), LocalDate.parse("2021-06-24"))).isFalse();
    }

    @Test
    public void testMin() {
        assertThat(ComparatorUtils.min(1, 2)).isEqualTo(1);
        assertThat(ComparatorUtils.min(1, 1)).isEqualTo(1);
        assertThat(ComparatorUtils.min(1, 0)).isEqualTo(0);

        assertThat(ComparatorUtils.min(Duration.ofSeconds(1), Duration.ofSeconds(2))).isEqualTo(Duration.ofSeconds(1));
        assertThat(ComparatorUtils.min(LocalDate.parse("2021-06-25"), LocalDate.parse("2021-06-24")))
                .isEqualTo(LocalDate.parse("2021-06-24"));
    }

    @Test
    public void testMax() {
        assertThat(ComparatorUtils.max(1, 2)).isEqualTo(2);
        assertThat(ComparatorUtils.max(1, 1)).isEqualTo(1);
        assertThat(ComparatorUtils.max(1, 0)).isEqualTo(1);

        assertThat(ComparatorUtils.max(Duration.ofSeconds(1), Duration.ofSeconds(2))).isEqualTo(Duration.ofSeconds(2));
        assertThat(ComparatorUtils.max(LocalDate.parse("2021-06-25"), LocalDate.parse("2021-06-24")))
                .isEqualTo(LocalDate.parse("2021-06-25"));
    }

    @Test
    public void testStrangeHierarchies() {
        Money a = Money.of(10, "RUB");
        Money b = Money.of(20, "RUB");
        assertThat(ComparatorUtils.min(a, b)).isEqualTo(a);
    }

    @Test
    public void testLimit() {
        assertThat(ComparatorUtils.limit(1, -5, 10)).isEqualTo(1);
        assertThat(ComparatorUtils.limit(1, 0, 10)).isEqualTo(1);
        assertThat(ComparatorUtils.limit(1, 1, 10)).isEqualTo(1);
        assertThat(ComparatorUtils.limit(1, 2, 10)).isEqualTo(2);
        assertThat(ComparatorUtils.limit(1, 5, 10)).isEqualTo(5);
        assertThat(ComparatorUtils.limit(1, 9, 10)).isEqualTo(9);
        assertThat(ComparatorUtils.limit(1, 10, 10)).isEqualTo(10);
        assertThat(ComparatorUtils.limit(1, 11, 10)).isEqualTo(10);
        assertThat(ComparatorUtils.limit(1, 15, 10)).isEqualTo(10);
    }
}
