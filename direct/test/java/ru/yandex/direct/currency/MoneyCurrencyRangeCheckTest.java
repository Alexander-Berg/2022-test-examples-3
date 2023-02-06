package ru.yandex.direct.currency;

import java.math.BigDecimal;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.direct.currency.currencies.CurrencyYndFixed;

import static ru.yandex.direct.testing.currency.MoneyAssert.assertThat;

public class MoneyCurrencyRangeCheckTest {

    private static final Currency CURRENCY = CurrencyYndFixed.getInstance();
    private static final CurrencyCode CODE = CURRENCY.getCode();

    private static Money moneyWithMaxPrice() {
        return Money.valueOf(CURRENCY.getMaxPrice(), CODE);
    }

    private static Money moneyWithMinPrice() {
        return Money.valueOf(CURRENCY.getMinPrice(), CODE);
    }

    private static Money moneyLessThanMin() {
        return Money.valueOf(CURRENCY.getMinPrice().subtract(BigDecimal.ONE), CODE);
    }

    private static Money moneyGreaterThanMax() {
        return Money.valueOf(CURRENCY.getMaxPrice().add(BigDecimal.ONE), CODE);
    }

    @Test
    public void adjustToCurrencyRange_returnItself_whenMinPrice() throws Exception {
        Money rawMoney = moneyWithMinPrice();

        Money actual = rawMoney.adjustToCurrencyRange();
        assertThat(actual).isSameAs(rawMoney);
    }

    @Test
    public void adjustToCurrencyRange_returnItself_whenMaxPrice() throws Exception {
        Money rawMoney = moneyWithMaxPrice();

        Money actual = rawMoney.adjustToCurrencyRange();
        assertThat(actual).isSameAs(rawMoney);
    }

    @Test
    public void adjustToCurrencyRange_returnMin_whenLessThanMin() throws Exception {
        Money rawMoney = moneyLessThanMin();

        Money actual = rawMoney.adjustToCurrencyRange();
        Money expected = moneyWithMinPrice();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void adjustToCurrencyRange_returnMax_whenGreaterThanMax() throws Exception {
        Money rawMoney = moneyGreaterThanMax();

        Money actual = rawMoney.adjustToCurrencyRange();
        Money expected = moneyWithMaxPrice();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void isInCurrencyRange_returnTrue_whenMinPrice() throws Exception {
        Money rawMoney = moneyWithMinPrice();

        Assertions.assertThat(rawMoney.isInCurrencyRange()).isTrue();
    }

    @Test
    public void isInCurrencyRange_returnTrue_whenMaxPrice() throws Exception {
        Money rawMoney = moneyWithMaxPrice();

        Assertions.assertThat(rawMoney.isInCurrencyRange()).isTrue();
    }

    @Test
    public void isInCurrencyRange_returnFalse_whenLessThanMin() throws Exception {
        Money rawMoney = moneyLessThanMin();

        Assertions.assertThat(rawMoney.isInCurrencyRange()).isFalse();
    }

    @Test
    public void isInCurrencyRange_returnFalse_whenGreaterThanMax() throws Exception {
        Money rawMoney = moneyGreaterThanMax();

        Assertions.assertThat(rawMoney.isInCurrencyRange()).isFalse();
    }

}
