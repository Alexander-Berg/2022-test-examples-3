package ru.yandex.market.logistics.tarifficator.service.pricelist.spreadsheet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.tarifficator.AbstractUnitTest;
import ru.yandex.market.logistics.tarifficator.converter.SpreadsheetConverter;

@DisplayName("Unit-тест конвертера SpreadsheetConverter")
class SpreadsheetConverterTest extends AbstractUnitTest {

    private SpreadsheetConverter converter = new SpreadsheetConverter();

    @Test
    @DisplayName("Конвертация строки в boolean значение для ячейки 'возможность предоставления'")
    void toIsEnabled() {
        softly.assertThat(converter.toIsEnabled("ДА")).isEqualTo(Boolean.TRUE);
        softly.assertThat(converter.toIsEnabled("да")).isEqualTo(Boolean.TRUE);
        softly.assertThat(converter.toIsEnabled("НЕТ")).isEqualTo(Boolean.FALSE);
        softly.assertThat(converter.toIsEnabled("нет")).isEqualTo(Boolean.FALSE);
        softly.assertThat(converter.toIsEnabled("ВОЗМОЖНО")).isNull();
        softly.assertThat(converter.toIsEnabled("может быть")).isNull();
    }
}
