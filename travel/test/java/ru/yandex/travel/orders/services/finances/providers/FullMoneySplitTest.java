package ru.yandex.travel.orders.services.finances.providers;

import org.javamoney.moneta.Money;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FullMoneySplitTest {
    @Test
    public void extractNegativeValues() {
        FullMoneySplit input = new FullMoneySplit(
                new MoneySplit(Money.of(100, "RUB"), Money.of(-50, "RUB")),
                new MoneySplit(Money.of(-70, "RUB"), Money.of(0, "RUB"))
        );
        FullMoneySplit expected = new FullMoneySplit(
                new MoneySplit(Money.of(0, "RUB"), Money.of(-50, "RUB")),
                new MoneySplit(Money.of(-70, "RUB"), Money.of(0, "RUB"))
        );
        assertThat(input.extractNegativeValues()).isEqualTo(expected);
    }

    @Test
    public void subtract() {
        FullMoneySplit first = new FullMoneySplit(
                new MoneySplit(Money.of(100, "RUB"), Money.of(-50, "RUB")),
                new MoneySplit(Money.of(-70, "RUB"), Money.of(0, "RUB"))
        );
        FullMoneySplit second = new FullMoneySplit(
                new MoneySplit(Money.of(0, "RUB"), Money.of(-50, "RUB")),
                new MoneySplit(Money.of(-70, "RUB"), Money.of(0, "RUB"))
        );
        FullMoneySplit expected = new FullMoneySplit(
                new MoneySplit(Money.of(100, "RUB"), Money.of(0, "RUB")),
                new MoneySplit(Money.of(0, "RUB"), Money.of(0, "RUB"))
        );
        assertThat(first.subtract(second)).isEqualTo(expected);
    }

    @Test
    public void negate() {
        FullMoneySplit input = new FullMoneySplit(
                new MoneySplit(Money.of(100, "RUB"), Money.of(-50, "RUB")),
                new MoneySplit(Money.of(-70, "RUB"), Money.of(0, "RUB"))
        );
        FullMoneySplit expected = new FullMoneySplit(
                new MoneySplit(Money.of(-100, "RUB"), Money.of(50, "RUB")),
                new MoneySplit(Money.of(70, "RUB"), Money.of(0, "RUB"))
        );
        assertThat(input.negate()).isEqualTo(expected);
    }
}
