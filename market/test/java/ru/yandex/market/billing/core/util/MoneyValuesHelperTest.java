package ru.yandex.market.billing.core.util;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.billing.util.MoneyValuesHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyValuesHelperTest {

    @Test
    void testConvertToUe() {
        assertThat(MoneyValuesHelper.convertToUe(new BigDecimal("628100"), Currency.RUR), equalTo(new BigDecimal(
                "20936.66666667")));
    }

    @Test
    void testConvertToUeFromNotRur() {
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> MoneyValuesHelper.convertToUe(new BigDecimal("100"), Currency.USD));
        assertEquals("Cannot convert to UE not from RUR", exception.getMessage());
    }
}
