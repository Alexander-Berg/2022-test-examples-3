package ru.yandex.market.common.excel.out.impl.content;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.market.common.excel.content.CsvUtils;

@SuppressWarnings("unused")
class CsvUtilsTest {

    @CsvSource({
            "foobar2000,foobar2000,Только буквы и цифры",
            "\"\"\"foobar2000\"\" media player\",\"foobar2000\" media player, Кавычки",
            "'\"foobar2000 supported formats:\nmp3\nflac\"','foobar2000 supported formats:\nmp3\nflac', Перенос строки"
    })
    @DisplayName("Корректное экранирование записей")
    @ParameterizedTest(name = "expected = {2}")
    void escapeString_parametrized_equalToExpected(String expected, String stringForEscape, String name) {
        Assertions.assertThat(CsvUtils.escapeString(stringForEscape))
                .isEqualTo(expected);
    }
}
