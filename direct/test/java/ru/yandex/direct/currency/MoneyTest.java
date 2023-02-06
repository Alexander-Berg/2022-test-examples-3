package ru.yandex.direct.currency;

import java.math.BigDecimal;

import org.junit.Test;

import ru.yandex.direct.currency.currencies.CurrencyYndFixed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.direct.currency.CurrencyCode.RUB;
import static ru.yandex.direct.currency.CurrencyCode.USD;
import static ru.yandex.direct.currency.CurrencyCode.YND_FIXED;
import static ru.yandex.direct.testing.currency.MoneyAssert.assertThat;

public class MoneyTest {
    private static final Currency CURRENCY = CurrencyYndFixed.getInstance();

    // Money.valueOfMicros & Money.valueOf

    @Test
    public void valueOfMicrosLong_equalToValueOfBigDecimal() throws Exception {
        Money actual = Money.valueOfMicros(1_000_000, RUB);
        Money expected = Money.valueOf(BigDecimal.ONE, RUB);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void valueOfDouble_equalToValueOfBigDecimal() throws Exception {
        Money actual = Money.valueOf(1.0, RUB);
        Money expected = Money.valueOf(BigDecimal.ONE, RUB);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void valueOfMicrosLong_enoughPrecision() throws Exception {
        //
        Money actual = Money.valueOfMicros(123_456789_123456L, RUB);
        BigDecimal expected = new BigDecimal("123456789.123456");

        assertThat(actual.bigDecimalValue()).isEqualByComparingTo(expected);
    }

    @Test
    public void valueOfDouble_enoughPrecision() throws Exception {
        Money actual = Money.valueOf(123_456_789.123_456, RUB);
        BigDecimal expected = new BigDecimal("123456789.123456");

        assertThat(actual.bigDecimalValue()).isEqualByComparingTo(expected);
    }

    @Test
    public void microsWorks() throws Exception {
        Money actual = Money.valueOf(12.123_456_812, RUB);

        assertThat(actual.micros()).isEqualTo(12_123_456);
    }


    // Money.unreachableBid
    @Test
    public void unreachableBidWorks() {
        Money actual = Money.unreachableBid(RUB.getCurrency());

        // Ожидаем MAX_PRICE (25000) + AUCTION_STEP (0.1)
        assertThat(actual.micros()).isEqualTo(25_000_100_000L);
    }


    // Money.add/subtract

    @Test
    public void add_Money_successCase() throws Exception {
        Money moneyOne = Money.valueOf(BigDecimal.ONE, RUB);
        Money moneyTwo = Money.valueOf(BigDecimal.ONE, RUB);
        Money actual = moneyOne.add(moneyTwo);
        Money expected = Money.valueOf(BigDecimal.valueOf(2), RUB);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void subtract_Money_successCase() throws Exception {
        Money moneyOne = Money.valueOf(BigDecimal.ONE, RUB);
        Money moneyTwo = Money.valueOf(BigDecimal.ONE, RUB);
        Money actual = moneyOne.subtract(moneyTwo);
        Money expected = Money.valueOf(BigDecimal.ZERO, RUB);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void add_Percent_successCase() throws Exception {
        Money moneyOne = Money.valueOf(BigDecimal.ONE, RUB);
        Percent oneHundredPercent = Percent.fromRatio(BigDecimal.ONE);
        Money actual = moneyOne.add(oneHundredPercent);
        Money expected = Money.valueOf(BigDecimal.valueOf(2), RUB);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void subtract_Percent_successCase() throws Exception {
        Money moneyOne = Money.valueOf(BigDecimal.ONE, RUB);
        Percent oneHundredPercent = Percent.fromRatio(BigDecimal.ONE);
        Money actual = moneyOne.subtract(oneHundredPercent);
        Money expected = Money.valueOf(BigDecimal.ZERO, RUB);

        assertThat(actual).isEqualTo(expected);
    }

    // Money.add/subtract negative scenarios

    @Test
    public void add_Money_fail_onDifferentCurrencies() throws Exception {
        Money moneyOne = Money.valueOf(BigDecimal.ONE, RUB);
        Money moneyTwo = Money.valueOf(BigDecimal.ONE, USD);
        assertThatThrownBy(() -> moneyOne.add(moneyTwo))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void subtract_Money_fail_onDifferentCurrencies() throws Exception {
        Money moneyOne = Money.valueOf(BigDecimal.ONE, RUB);
        Money moneyTwo = Money.valueOf(BigDecimal.ONE, USD);
        assertThatThrownBy(() -> moneyOne.subtract(moneyTwo))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // Money.multiply

    @Test
    public void multiplyDouble_successCase() throws Exception {
        Money money = Money.valueOf(BigDecimal.ONE, RUB);
        Money actual = money.multiply(10.0);
        Money expected = Money.valueOf(BigDecimal.TEN, RUB);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void multiplyBigDecimal_successCase() throws Exception {
        Money money = Money.valueOf(BigDecimal.ONE, RUB);
        Money actual = money.multiply(BigDecimal.TEN);
        Money expected = Money.valueOf(BigDecimal.TEN, RUB);

        assertThat(actual).isEqualTo(expected);
    }

    // Money.divide

    @Test
    public void divideDouble_successCase() throws Exception {
        Money money = Money.valueOf(BigDecimal.TEN, RUB);
        Money actual = money.divide(10.0);
        Money expected = Money.valueOf(BigDecimal.ONE, RUB);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void divideBigDecimal_successCase() throws Exception {
        Money money = Money.valueOf(BigDecimal.TEN, RUB);
        Money actual = money.divide(BigDecimal.TEN);
        Money expected = Money.valueOf(BigDecimal.ONE, RUB);

        assertThat(actual).isEqualTo(expected);
    }

    // Money.abs

    @Test
    public void abs_negativeValue_successCase() {
        Money money = Money.valueOf(-1e-8, RUB);
        Money actual = money.abs();
        Money expected = Money.valueOf(1e-8, RUB);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void abs_positiveValue_successCase() {
        Money money = Money.valueOf(1e-8, RUB);
        Money actual = money.abs();
        Money expected = Money.valueOf(1e-8, RUB);

        assertThat(actual).isEqualTo(expected);
    }

    // Money.lessThan

    @Test
    public void lessThan_true_whenObjectLessThanArgument() throws Exception {
        Money little = Money.valueOf(1.0, RUB);
        Money big = Money.valueOf(1.1, RUB);

        assertThat(little.lessThan(big)).isTrue();
    }

    @Test
    public void lessThan_false_whenObjectEqualsToArgument() throws Exception {
        Money little = Money.valueOf(1.0, RUB);
        Money big = Money.valueOf(1.0, RUB);

        assertThat(little.lessThan(big)).isFalse();
    }

    @Test
    public void lessThan_false_whenObjectGreaterThanArgument() throws Exception {
        Money little = Money.valueOf(1.1, RUB);
        Money big = Money.valueOf(1.0, RUB);

        assertThat(little.lessThan(big)).isFalse();
    }


    // Money.lessThanOrEqual

    @Test
    public void lessThanOrEqual_true_whenObjectLessThanArgument() throws Exception {
        Money little = Money.valueOf(1.0, RUB);
        Money big = Money.valueOf(1.1, RUB);

        assertThat(little.lessThanOrEqual(big)).isTrue();
    }

    @Test
    public void lessThanOrEqual_true_whenObjectEqualsToArgument() throws Exception {
        Money little = Money.valueOf(1.0, RUB);
        Money big = Money.valueOf(1.0, RUB);

        assertThat(little.lessThanOrEqual(big)).isTrue();
    }

    @Test
    public void lessThanOrEqual_false_whenObjectGreaterThanArgument() throws Exception {
        Money little = Money.valueOf(1.1, RUB);
        Money big = Money.valueOf(1.0, RUB);

        assertThat(little.lessThanOrEqual(big)).isFalse();
    }

    // Money.lessThanZero

    @Test
    public void lessThanZero_true_whenObjectLessThanZero() throws Exception {
        Money little = Money.valueOf(-1e-8, RUB);

        assertThat(little.lessThanZero()).isTrue();
    }

    @Test
    public void lessThanZero_false_whenObjectEqualsToZero() throws Exception {
        Money little = Money.valueOf(BigDecimal.ZERO, RUB);

        assertThat(little.lessThanZero()).isFalse();
    }

    @Test
    public void lessThanZero_false_whenObjectGreaterThanZero() throws Exception {
        Money little = Money.valueOf(1e-8, RUB);

        assertThat(little.lessThanZero()).isFalse();
    }

    // Money.greaterThanOrEqualEpsilon

    @Test
    public void greaterThanOrEqualEpsilon_false_whenObjectLessThanEpsilon() throws Exception {
        Money little = Money.valueOf(0.5e-7, RUB);

        assertThat(little.greaterThanOrEqualEpsilon()).isFalse();
    }

    @Test
    public void greaterThanOrEqualEpsilon_false_whenObjectEqualsToZero() throws Exception {
        Money little = Money.valueOf(BigDecimal.ZERO, RUB);

        assertThat(little.greaterThanOrEqualEpsilon()).isFalse();
    }

    @Test
    public void greaterThanOrEqualEpsilon_true_whenObjectGreaterThanEpsilon() throws Exception {
        Money little = Money.valueOf(1.5e-7, RUB);

        assertThat(little.greaterThanOrEqualEpsilon()).isTrue();
    }

    // Money.lessThanOrEqualEpsilon

    @Test
    public void lessThanOrEqualEpsilon_true_whenObjectLessThanEpsilon() throws Exception {
        Money little = Money.valueOf(0.5e-7, RUB);

        assertThat(little.lessThanOrEqualEpsilon()).isTrue();
    }

    @Test
    public void lessThanOrEqualEpsilon_true_whenObjectEqualsToZero() throws Exception {
        Money little = Money.valueOf(BigDecimal.ZERO, RUB);

        assertThat(little.lessThanOrEqualEpsilon()).isTrue();
    }

    @Test
    public void lessThanOrEqualEpsilon_false_whenObjectGreaterThanEpsilon() throws Exception {
        Money little = Money.valueOf(1.5e-7, RUB);

        assertThat(little.lessThanOrEqualEpsilon()).isFalse();
    }

    // Money.*Nds

    @Test
    public void addNds_success() {
        Money money = Money.valueOf(100, RUB);
        BigDecimal ndsPercentage = BigDecimal.valueOf(0.13);
        Money actual = money.addNds(Percent.fromRatio(ndsPercentage));
        Money expected = Money.valueOf(113, RUB);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void subtractNds_success() {
        Money money = Money.valueOf(113, RUB);
        BigDecimal ndsPercentage = BigDecimal.valueOf(0.13);
        Money actual = money.subtractNds(Percent.fromRatio(ndsPercentage));
        Money expected = Money.valueOf(100, RUB);
        assertThat(actual).isEqualTo(expected);
    }


    // Money.*Nds with Percent

    @Test
    public void addNdsPercent_success() {
        Money money = Money.valueOf(100, RUB);
        Percent ndsPercentage = Percent.fromRatio(BigDecimal.valueOf(0.13));
        Money actual = money.addNds(ndsPercentage);
        Money expected = Money.valueOf(113, RUB);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void addNdsPercent_success_withRounding() {
        Money money = Money.valueOf(111.11, RUB);
        Percent ndsPercentage = Percent.fromRatio(BigDecimal.valueOf(0.18));
        Money actual = money.addNds(ndsPercentage);
        Money expected = Money.valueOf(131.10, RUB);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void subtractNdsPercent_success() {
        Money money = Money.valueOf(113, RUB);
        Percent ndsPercentage = Percent.fromRatio(BigDecimal.valueOf(0.13));
        Money actual = money.subtractNds(ndsPercentage);
        Money expected = Money.valueOf(100, RUB);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void subtractNdsPercent_success_withRounding() {
        Money money = Money.valueOf(1000, RUB);
        Percent ndsPercentage = Percent.fromRatio(BigDecimal.valueOf(0.18));
        Money actual = money.subtractNds(ndsPercentage);
        Money expected = Money.valueOf(847.45, RUB);
        assertThat(actual).isEqualTo(expected);
    }


    // Money.roundToCent*

    @Test
    public void roundToCentUp_success() {
        Money money = Money.valueOf(1234.5678, RUB);
        Money actual = money.roundToCentUp();
        Money expected = Money.valueOf(1234.57, RUB);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void roundToCentDown_success() {
        Money money = Money.valueOf(1234.5678, RUB);
        Money actual = money.roundToCentDown();
        Money expected = Money.valueOf(1234.56, RUB);
        assertThat(actual).isEqualTo(expected);
    }

    // Money.roundToAuctionStepDown
    @Test
    public void roundToAuctionStepDown_success_roundFromAlmostFullAuctionStep() {
        BigDecimal roundedValue = CURRENCY.getAuctionStep().multiply(BigDecimal.valueOf(5));
        BigDecimal valueToRound = roundedValue.add(CURRENCY.getAuctionStep()).subtract(BigDecimal.valueOf(0.000_001));

        Money moneyToRound = Money.valueOf(valueToRound, YND_FIXED);

        Money roundedMoney = moneyToRound.roundToAuctionStepDown();

        assertThat(roundedMoney).isEqualTo(Money.valueOf(roundedValue, YND_FIXED));
    }

    @Test
    public void roundToAuctionStepDown_success_roundFromFullAuctionStep() {
        BigDecimal roundedValue = CURRENCY.getAuctionStep().multiply(BigDecimal.valueOf(5));

        Money moneyToRound = Money.valueOf(roundedValue, YND_FIXED);

        Money roundedMoney = moneyToRound.roundToAuctionStepDown();

        assertThat(roundedMoney).isEqualTo(Money.valueOf(roundedValue, YND_FIXED));
    }

    // Money.roundToAuctionStepUp
    @Test
    public void roundToAuctionStepUp_success_roundFromAlmostFullAuctionStep() {
        BigDecimal roundedValue = CURRENCY.getAuctionStep().multiply(BigDecimal.valueOf(5));
        BigDecimal valueToRound = roundedValue.subtract(CURRENCY.getAuctionStep()).add(BigDecimal.valueOf(0.000_001));

        Money moneyToRound = Money.valueOf(valueToRound, YND_FIXED);

        Money roundedMoney = moneyToRound.roundToAuctionStepUp();

        assertThat(roundedMoney).isEqualTo(Money.valueOf(roundedValue, YND_FIXED));
    }

    @Test
    public void roundToAuctionStepUp_success_roundFromFullAuctionStep() {
        BigDecimal roundedValue = CURRENCY.getAuctionStep().multiply(BigDecimal.valueOf(5));

        Money moneyToRound = Money.valueOf(roundedValue, YND_FIXED);

        Money roundedMoney = moneyToRound.roundToAuctionStepUp();

        assertThat(roundedMoney).isEqualTo(Money.valueOf(roundedValue, YND_FIXED));
    }
}
