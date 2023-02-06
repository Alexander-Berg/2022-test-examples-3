package ru.yandex.market.mbo.excel.formatting;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import static ru.yandex.market.mbo.excel.formatting.SpecialType.BOOLEAN;
import static ru.yandex.market.mbo.excel.formatting.SpecialType.COMBINED;
import static ru.yandex.market.mbo.excel.formatting.SpecialType.FORMULA;
import static ru.yandex.market.mbo.excel.formatting.SpecialType.NUMERIC_BIG;
import static ru.yandex.market.mbo.excel.formatting.SpecialType.NUMERIC_SMALL;
import static ru.yandex.market.mbo.excel.formatting.SpecialType.SKIP;
import static ru.yandex.market.mbo.excel.formatting.SpecialType.STRING;
import static ru.yandex.market.mbo.excel.formatting.SpecialType.URL;

public class SpecialTypeTest {

    @Test
    public void testGetTypeFor() {
        Long longValue = null;
        Assertions.assertThat(SpecialType.getTypeFor(longValue)).isEqualTo(SKIP);
        Assertions.assertThat(SpecialType.getTypeFor(Long.MIN_VALUE)).isEqualTo(NUMERIC_SMALL);
        Assertions.assertThat(SpecialType.getTypeFor(5L)).isEqualTo(NUMERIC_SMALL);
        Assertions.assertThat(SpecialType.getTypeFor(99_999_999_999L)).isEqualTo(NUMERIC_SMALL);
        Assertions.assertThat(SpecialType.getTypeFor(100_000_000_000L)).isEqualTo(NUMERIC_BIG);
        Assertions.assertThat(SpecialType.getTypeFor(Long.MAX_VALUE)).isEqualTo(NUMERIC_BIG);

        Double doubleValue = null;
        Assertions.assertThat(SpecialType.getTypeFor(doubleValue)).isEqualTo(SKIP);
        Assertions.assertThat(SpecialType.getTypeFor(Double.MIN_VALUE)).isEqualTo(NUMERIC_SMALL);
        Assertions.assertThat(SpecialType.getTypeFor(5.0)).isEqualTo(NUMERIC_SMALL);
        Assertions.assertThat(SpecialType.getTypeFor(99_999_999_999.0)).isEqualTo(NUMERIC_SMALL);
        Assertions.assertThat(SpecialType.getTypeFor(100_000_000_000.0)).isEqualTo(NUMERIC_BIG);
        Assertions.assertThat(SpecialType.getTypeFor(Double.MAX_VALUE)).isEqualTo(NUMERIC_BIG);

        Integer integerValue = null;
        Assertions.assertThat(SpecialType.getTypeFor(integerValue)).isEqualTo(SKIP);
        Assertions.assertThat(SpecialType.getTypeFor(Integer.MIN_VALUE)).isEqualTo(NUMERIC_SMALL);
        Assertions.assertThat(SpecialType.getTypeFor(5)).isEqualTo(NUMERIC_SMALL);
        Assertions.assertThat(SpecialType.getTypeFor(Integer.MAX_VALUE)).isEqualTo(NUMERIC_SMALL);

        Boolean booleanValue = null;
        Assertions.assertThat(SpecialType.getTypeFor(booleanValue)).isEqualTo(SKIP);
        Assertions.assertThat(SpecialType.getTypeFor(Boolean.FALSE)).isEqualTo(BOOLEAN);
        Assertions.assertThat(SpecialType.getTypeFor(Boolean.TRUE)).isEqualTo(BOOLEAN);

        String formula = "=FALSE()";
        String url = "https://ya.ru";
        String empty = "";
        String someString = "just some text";
        String nullValue = null;
        Assertions.assertThat(SpecialType.getTypeFor(nullValue)).isEqualTo(SKIP);
        Assertions.assertThat(SpecialType.getTypeFor(formula)).isEqualTo(FORMULA);
        Assertions.assertThat(SpecialType.getTypeFor(url)).isEqualTo(URL);
        Assertions.assertThat(SpecialType.getTypeFor(empty)).isEqualTo(STRING);
        Assertions.assertThat(SpecialType.getTypeFor(someString)).isEqualTo(STRING);
    }

    @Test
    public void testCombine() {

        Assertions.assertThat(BOOLEAN.combine(BOOLEAN)).isEqualTo(BOOLEAN);
        Assertions.assertThat(BOOLEAN.combine(NUMERIC_SMALL)).isEqualTo(COMBINED);
        Assertions.assertThat(BOOLEAN.combine(NUMERIC_BIG)).isEqualTo(COMBINED);
        Assertions.assertThat(BOOLEAN.combine(FORMULA)).isEqualTo(COMBINED);
        Assertions.assertThat(BOOLEAN.combine(STRING)).isEqualTo(COMBINED);
        Assertions.assertThat(BOOLEAN.combine(URL)).isEqualTo(COMBINED);
        Assertions.assertThat(BOOLEAN.combine(COMBINED)).isEqualTo(COMBINED);

        Assertions.assertThat(NUMERIC_SMALL.combine(NUMERIC_SMALL)).isEqualTo(NUMERIC_SMALL);
        Assertions.assertThat(NUMERIC_SMALL.combine(NUMERIC_BIG)).isEqualTo(NUMERIC_BIG);
        Assertions.assertThat(NUMERIC_SMALL.combine(FORMULA)).isEqualTo(COMBINED);
        Assertions.assertThat(NUMERIC_SMALL.combine(STRING)).isEqualTo(COMBINED);
        Assertions.assertThat(NUMERIC_SMALL.combine(URL)).isEqualTo(COMBINED);
        Assertions.assertThat(NUMERIC_SMALL.combine(COMBINED)).isEqualTo(COMBINED);

        Assertions.assertThat(NUMERIC_BIG.combine(NUMERIC_BIG)).isEqualTo(NUMERIC_BIG);
        Assertions.assertThat(NUMERIC_BIG.combine(FORMULA)).isEqualTo(COMBINED);
        Assertions.assertThat(NUMERIC_BIG.combine(STRING)).isEqualTo(COMBINED);
        Assertions.assertThat(NUMERIC_BIG.combine(URL)).isEqualTo(COMBINED);
        Assertions.assertThat(NUMERIC_BIG.combine(COMBINED)).isEqualTo(COMBINED);

        Assertions.assertThat(FORMULA.combine(FORMULA)).isEqualTo(FORMULA);
        Assertions.assertThat(FORMULA.combine(STRING)).isEqualTo(COMBINED);
        Assertions.assertThat(FORMULA.combine(URL)).isEqualTo(COMBINED);
        Assertions.assertThat(FORMULA.combine(COMBINED)).isEqualTo(COMBINED);

        Assertions.assertThat(STRING.combine(STRING)).isEqualTo(STRING);
        Assertions.assertThat(STRING.combine(URL)).isEqualTo(COMBINED);
        Assertions.assertThat(STRING.combine(COMBINED)).isEqualTo(COMBINED);

        Assertions.assertThat(URL.combine(URL)).isEqualTo(URL);
        Assertions.assertThat(URL.combine(COMBINED)).isEqualTo(COMBINED);

        Assertions.assertThat(COMBINED.combine(COMBINED)).isEqualTo(COMBINED);
    }


    @SuppressWarnings("checkstyle:LineLength")
    @Test
    public void testValidateUrl() {
        Assertions.assertThat(SpecialType.validateUrl("http://ya.ru")).isTrue();
        Assertions.assertThat(SpecialType.validateUrl("HTTP://ya.ru")).isTrue();
        Assertions.assertThat(SpecialType.validateUrl("ya.ru")).isFalse();
        Assertions.assertThat(SpecialType.validateUrl("https://www.akusherstvo.ru/magaz" +
                ".php?action=show_tovar&tovar_id=286036&utm_medium=cpc&utm_source=yandex&utm_campaign" +
                "=Generator_Igrovie_kovriki|16736915&utm_content=3911929355_коврик%20Y8"))
            .isFalse();
        Assertions.assertThat(SpecialType.validateUrl("https://www.akusherstvo.ru/magaz" +
                ".php?action=show_tovar&tovar_id=286036&utm_medium=cpc&utm_source=yandex&utm_campaign" +
                "=Generator_Igrovie_kovriki%7C16736915&utm_content=3911929355_%7C%D0%BA%D0%BE%D0%B2%D1%80%D0%B8%D0%BA" +
                "%20Y8"))
            .isTrue();
    }

    @Test
    public void testValidateFormula() {
        Assertions.assertThat(SpecialType.validateFormula("http://ya.ru")).isFalse();
        Assertions.assertThat(SpecialType.validateFormula("")).isFalse();
        Assertions.assertThat(SpecialType.validateFormula("-1")).isFalse();
        Assertions.assertThat(SpecialType.validateFormula("=1")).isTrue();
    }
}
