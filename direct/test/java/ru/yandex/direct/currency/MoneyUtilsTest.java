package ru.yandex.direct.currency;

import java.math.BigDecimal;

import org.junit.Test;

import static ru.yandex.direct.currency.CurrencyCode.RUB;
import static ru.yandex.direct.currency.CurrencyCode.USD;
import static ru.yandex.direct.testing.currency.MoneyAssert.assertThat;

public class MoneyUtilsTest {
    // MoneyUtils.min

    @Test
    public void min_returnsLeft_whenLeftLessThanRight() throws Exception {
        Money left = Money.valueOf(1, RUB);
        Money right = Money.valueOf(1.01, RUB);
        assertThat(MoneyUtils.min(left, right)).isSameAs(left);
    }

    @Test
    public void min_returnsLeft_whenLeftEqualsToRight() throws Exception {
        Money left = Money.valueOf(1, RUB);
        Money right = Money.valueOf(BigDecimal.ONE, RUB);
        assertThat(MoneyUtils.min(left, right)).isSameAs(left);
    }

    @Test
    public void min_returnsRight_whenLeftGreaterThanRight() throws Exception {
        Money left = Money.valueOf(1.01, RUB);
        Money right = Money.valueOf(1, RUB);
        assertThat(MoneyUtils.min(left, right)).isSameAs(right);
    }


    // MoneyUtils.max

    @Test
    public void max_returnsRight_whenLeftLessThanRight() throws Exception {
        Money left = Money.valueOf(1, RUB);
        Money right = Money.valueOf(1.01, RUB);
        assertThat(MoneyUtils.max(left, right)).isSameAs(right);
    }

    @Test
    public void max_returnsLeft_whenLeftEqualsToRight() throws Exception {
        Money left = Money.valueOf(1, RUB);
        Money right = Money.valueOf(BigDecimal.ONE, RUB);
        assertThat(MoneyUtils.max(left, right)).isSameAs(left);
    }

    @Test
    public void max_returnsLeft_whenLeftGreaterThanRight() throws Exception {
        Money left = Money.valueOf(1.01, RUB);
        Money right = Money.valueOf(1, RUB);
        assertThat(MoneyUtils.max(left, right)).isSameAs(left);
    }

    // MoneyUtils.checkMoneyCurrencyCodesAreSame

    @Test(expected = IllegalArgumentException.class)
    public void checkMoneyCurrencyCodesAreSame_fail_whenCurrenciesDiffer() throws Exception {
        Money oneRubble = Money.valueOf(1, RUB);
        Money oneDollar = Money.valueOf(1, USD);

        MoneyUtils.checkMoneyCurrencyCodesAreSame(oneRubble, oneDollar);
    }

}
