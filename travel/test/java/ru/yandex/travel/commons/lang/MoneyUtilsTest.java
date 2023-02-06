package ru.yandex.travel.commons.lang;

import java.math.BigDecimal;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.travel.commons.lang.MoneyUtils.ensureDecimalScale;
import static ru.yandex.travel.commons.lang.MoneyUtils.ensureIntegerScale;
import static ru.yandex.travel.commons.lang.MoneyUtils.roundDownToInteger;
import static ru.yandex.travel.commons.lang.MoneyUtils.roundUpToInteger;
import static ru.yandex.travel.commons.lang.MoneyUtils.safeAdd;
import static ru.yandex.travel.testing.misc.TestBaseObjects.rub;

public class MoneyUtilsTest {
    @Test
    public void testSafeAdd() {
        assertThat(safeAdd(rub(1), rub(2))).isEqualTo(rub(3));
        assertThat(safeAdd(rub(1), null)).isEqualTo(rub(1));
        assertThat(safeAdd(null, rub(2))).isEqualTo(rub(2));
        assertThat(safeAdd(null, null)).isEqualTo(null);
    }

    @Test
    public void testEnsureIntegerScale() {
        ensureIntegerScale(rub(1));
        ensureIntegerScale(rub(10));
        ensureIntegerScale(rub(1e9));
        ensureIntegerScale(rub(10e-1));
        ensureIntegerScale(rub(0));
        ensureIntegerScale(rub(0.0));
        ensureIntegerScale(rub(-0.0));
        ensureIntegerScale(rub(-2));
        ensureIntegerScale(rub(-10e-1));
        ensureIntegerScale(rub(new BigDecimal("0.0")));
        ensureIntegerScale(rub(new BigDecimal("0")));
        ensureIntegerScale(rub(new BigDecimal("1.0")));
        ensureIntegerScale(rub(new BigDecimal("1.00")));
        ensureIntegerScale(rub(new BigDecimal("-1.00")));

        assertThatThrownBy(() -> ensureIntegerScale(rub(1e-1)))
                .hasMessageContaining("Unexpected decimal money");
        assertThatThrownBy(() -> ensureIntegerScale(rub(101e-1)))
                .hasMessageContaining("Unexpected decimal money");
        assertThatThrownBy(() -> ensureIntegerScale(rub(0.1)))
                .hasMessageContaining("Unexpected decimal money");
        assertThatThrownBy(() -> ensureIntegerScale(rub(new BigDecimal("0.1"))))
                .hasMessageContaining("Unexpected decimal money");
        assertThatThrownBy(() -> ensureIntegerScale(rub(new BigDecimal("1.001"))))
                .hasMessageContaining("Unexpected decimal money");
        assertThatThrownBy(() -> ensureIntegerScale(rub(new BigDecimal("-1.001"))))
                .hasMessageContaining("Unexpected decimal money");
    }

    @Test
    public void testEnsureDecimalScale() {
        ensureDecimalScale(rub(1));
        ensureDecimalScale(rub(1.23));
        ensureDecimalScale(rub(1e9));
        ensureDecimalScale(rub(10e-3));
        ensureDecimalScale(rub(0.0000));
        ensureDecimalScale(rub(-2.34));
        ensureDecimalScale(rub(new BigDecimal("1.2300")));
        ensureDecimalScale(rub(new BigDecimal("-1.2300")));

        assertThatThrownBy(() -> ensureDecimalScale(rub(1e-3)))
                .hasMessageContaining("Unexpected decimal money value part");
        assertThatThrownBy(() -> ensureDecimalScale(rub(101e-3)))
                .hasMessageContaining("Unexpected decimal money value part");
        assertThatThrownBy(() -> ensureDecimalScale(rub(0.001)))
                .hasMessageContaining("Unexpected decimal money value part");
        assertThatThrownBy(() -> ensureDecimalScale(rub(new BigDecimal("0.001"))))
                .hasMessageContaining("Unexpected decimal money value part");
        assertThatThrownBy(() -> ensureDecimalScale(rub(new BigDecimal("1.001"))))
                .hasMessageContaining("Unexpected decimal money value part");
        assertThatThrownBy(() -> ensureDecimalScale(rub(new BigDecimal("-1.001"))))
                .hasMessageContaining("Unexpected decimal money value part");
    }

    @Test
    public void testRoundDownToInteger() {
        assertThat(roundDownToInteger(rub(0))).isEqualTo(rub(0));
        assertThat(roundDownToInteger(rub(1.234))).isEqualTo(rub(1));
        assertThat(roundDownToInteger(rub(-1.234))).isEqualTo(rub(-2)); // just be aware ;)
        assertThat(roundDownToInteger(rub(1.9))).isEqualTo(rub(1));
        assertThat(roundDownToInteger(rub(2.0))).isEqualTo(rub(2));
    }

    @Test
    public void testRoundUpToInteger() {
        assertThat(roundUpToInteger(rub(0))).isEqualTo(rub(0));
        assertThat(roundUpToInteger(rub(1.234))).isEqualTo(rub(2));
        assertThat(roundUpToInteger(rub(-1.234))).isEqualTo(rub(-1)); // just be aware ;)
        assertThat(roundUpToInteger(rub(1.9))).isEqualTo(rub(2));
        assertThat(roundUpToInteger(rub(2.0))).isEqualTo(rub(2));
    }
}
