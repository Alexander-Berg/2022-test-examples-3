package ru.yandex.market.mbi.core;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.excel.MarketTemplate;
import ru.yandex.market.core.feed.model.FeedFileType;

/**
 * Unit-тесты для {@link Xls2CsvOutputParser}.
 *
 * @author Vladislav Bauer
 */
class Xls2CsvOutputParserTest {

    @DisplayName("Корректный разбор stdout-данных, полученных от компонента mbi-xls2csv")
    @Test
    void Xls2CsvOutputParser_correctInput_correctResult() {
        String src = "" +
                "X-MARKET-TEMPLATE: PRICE\n" +
                "X-RETURN-CODE: 0\n" +
                "X-FEED-FILE-TYPE: CSV\n";

        Xls2CsvOutputParser xls2CsvOutputParser = new Xls2CsvOutputParser(src);

        Assertions.assertThat(xls2CsvOutputParser.getMarketTemplate())
                .isEqualTo(MarketTemplate.PRICE);
        Assertions.assertThat(xls2CsvOutputParser.getFeedFileType())
                .isEqualTo(FeedFileType.CSV);
    }

    @DisplayName("Корректный разбор пустых stdout-данных, полученных от компонента mbi-xls2csv")
    @Test
    void Xls2CsvOutputParser_emptyInput_correctResult() {
        String src = "";

        Xls2CsvOutputParser xls2CsvOutputParser = new Xls2CsvOutputParser(src);

        Assertions.assertThat(xls2CsvOutputParser.getMarketTemplate())
                .isEqualTo(MarketTemplate.NONE);
        Assertions.assertThat(xls2CsvOutputParser.getFeedFileType())
                .isNull();
    }
}
