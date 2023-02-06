package ru.yandex.market.common.excel.out.impl;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.market.common.excel.Strings;

@SuppressWarnings("unused")
class StringsTest {

    @CsvSource({
            "'slsjflsdkj',' slsjflsdkj'",
            "'slsjflsdkj','slsjflsdkj '",
            "'slsjflsdkj',' slsjflsdkj '",
            "'slsjflsdkj','slsjflsdkj    '",
            "'slsjflsdkj','    slsjflsdkj    '",
            "'slsjflsdkj','\n   \u00A0slsjflsdkj'",
            "'slsjflsdkj','slsjflsdkj\n   \u00A0'",
            "'slsjflsdkj','\n   \u00A0slsjflsdkj\n   \u00A0'",
            "'slsjf\n   \u00A0lsdkj','slsjf\n   \u00A0lsdkj'",
            "'slsjf\n   \u00A0lsdkj','\n   \u00A0slsjf\n   \u00A0lsdkj'",
            "'slsjf\n   \u00A0lsdkj','slsjf\n   \u00A0lsdkj\n   \u00A0'",
            "'slsjf\n   \u00A0lsdkj','\n   \u00A0slsjf\n   \u00A0lsdkj\n   \u00A0'",
    })
    @DisplayName("Корректное экранирование записей")
    @ParameterizedTest(name = "stringToTrim = ({1})")
    void unicodeTrim_parametrized_equalToExpected(String expected, String stringToTrim) {
        Assertions.assertThat(Strings.unicodeTrim(stringToTrim))
                .isEqualTo(expected);
    }
}
