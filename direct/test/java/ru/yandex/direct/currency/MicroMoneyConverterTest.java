package ru.yandex.direct.currency;

import java.math.BigDecimal;

import org.junit.Test;

import static ru.yandex.direct.currency.CurrencyCode.YND_FIXED;
import static ru.yandex.direct.testing.currency.MoneyAssert.assertThat;

public class MicroMoneyConverterTest {
    private static final CurrencyCode CURRENCY_CODE = YND_FIXED;
    private static final Currency CURRENCY = CURRENCY_CODE.getCurrency();

    @Test
    public void MicroMoneyConverterRoundingUp_success_roundFromAlmostFullAuctionStep() {
        BigDecimal roundedValue = CURRENCY.getAuctionStep().multiply(BigDecimal.valueOf(5));
        Money expected = Money.valueOf(roundedValue, CURRENCY_CODE);

        BigDecimal valueToRound = roundedValue.subtract(CURRENCY.getAuctionStep()).add(BigDecimal.valueOf(0.000_001));
        Long microValueToRound = Money.valueOf(valueToRound, CURRENCY_CODE).micros();

        MicroMoneyConverter moneyConverter = MicroMoneyConverter.roundingUp(CURRENCY_CODE);
        Money actual = moneyConverter.apply(microValueToRound);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void MicroMoneyConverterRoundingUp_success_roundFromFullAuctionStep() {
        BigDecimal roundedValue = CURRENCY.getAuctionStep().multiply(BigDecimal.valueOf(5));
        Money expected = Money.valueOf(roundedValue, CURRENCY_CODE);

        Long microValueToRound = Money.valueOf(roundedValue, CURRENCY_CODE).micros();

        MicroMoneyConverter moneyConverter = MicroMoneyConverter.roundingUp(CURRENCY_CODE);
        Money actual = moneyConverter.apply(microValueToRound);

        assertThat(actual).isEqualTo(expected);
    }
}
