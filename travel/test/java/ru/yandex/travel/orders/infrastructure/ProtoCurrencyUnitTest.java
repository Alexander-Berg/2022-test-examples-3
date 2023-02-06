package ru.yandex.travel.orders.infrastructure;


import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.UnknownCurrencyException;

import org.javamoney.moneta.FastMoney;
import org.javamoney.moneta.Money;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ProtoCurrencyUnitTest {
    @Test
    public void testCurrenciesRegistered() {
        CurrencyUnit unit = ProtoCurrencyUnit.RUB;
        CurrencyUnit rubUnit = Monetary.getCurrency("RUB");
        assertThat(unit).isEqualTo(rubUnit);
    }

    @Test
    public void testNewCurrenciesRegistered() {
        assertThat(Monetary.getCurrency("JPY")).isSameAs(ProtoCurrencyUnit.JPY);
    }

    @Test
    public void testUnregisteredCurrenciesNotPresent() {
        assertThatExceptionOfType(UnknownCurrencyException.class).isThrownBy(
                () -> Monetary.getCurrency("ZWL")
        );
    }

    @Test
    public void testCurrenciesCreationViaFastMoneyOF() {
        FastMoney.of(1000L, ProtoCurrencyUnit.RUB);
    }

    @Test
    public void testCurrenciesCreationViaMoneyOf() {
        Money.of(1000L, ProtoCurrencyUnit.USD);
    }
}
