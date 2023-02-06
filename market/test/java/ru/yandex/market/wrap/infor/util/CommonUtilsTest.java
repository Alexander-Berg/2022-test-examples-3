package ru.yandex.market.wrap.infor.util;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommonUtilsTest {

    @Test
    void getIntegerOrZero() {
        Integer nullValue = CommonUtils.getIntegerOrZero(null);
        Integer one = CommonUtils.getIntegerOrZero(BigDecimal.ONE);
        Integer maxvalue = CommonUtils.getIntegerOrZero(BigDecimal.valueOf(Integer.MAX_VALUE));

        assertEquals(Integer.valueOf(0), nullValue);
        assertEquals(Integer.valueOf(1), one);
        assertEquals(Integer.MAX_VALUE, (int) maxvalue);
    }

    @Test
    void getIntegerOrDefault() {
        Integer nullValue = CommonUtils.getIntegerOrDefault(null, BigDecimal.TEN);
        Integer one = CommonUtils.getIntegerOrDefault(BigDecimal.ONE, BigDecimal.TEN);
        Integer maxvalue = CommonUtils.getIntegerOrDefault(BigDecimal.valueOf(Integer.MAX_VALUE), BigDecimal.TEN);

        assertEquals(Integer.valueOf(10), nullValue);
        assertEquals(Integer.valueOf(1), one);
        assertEquals(Integer.MAX_VALUE, (int) maxvalue);
    }

    @Test
    void getIntegerOrDefaultNPE() {
        assertThatThrownBy(() -> CommonUtils.getIntegerOrDefault(null, null))
            .isInstanceOf(NullPointerException.class);
    }


    @Test
    void illegalSymbolsReplacement() {
        String value = "O'Lor. O^Lor. O\"Lor. O`Lor.";

        assertEquals(CommonUtils.replaceIllegalSymbols(value), "O Lor. O Lor. O Lor. O Lor.");
    }

    @Test
    void illegalSymbolsReplacementForNulls() {
        assertNull(CommonUtils.replaceIllegalSymbols(null));
    }
}
