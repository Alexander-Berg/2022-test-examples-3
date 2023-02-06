package ru.yandex.market.stat.dicts.parsers;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.market.stat.dicts.records.CurrencyRatesDictionaryRecord;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.stat.dicts.utils.ParserTestUtil.loadRecords;

/**
 * @author Alexander Gavrikov <agavrikov@yandex-team.ru>
 */
@RunWith(DataProviderRunner.class)
public class CurrencyRatesParserTest {
    @Test
    public void test() throws IOException {
        // Given
        DictionaryParser<CurrencyRatesDictionaryRecord> currencyRatesParser = new CurrencyRatesDictionaryRecord.CurrencyRatesParser();

        // When
        List<CurrencyRatesDictionaryRecord> recordList = loadRecords(currencyRatesParser, "/parsers/currency_rates.xml.gz");

        // Then
        assertThat(recordList.size(), equalTo(43));
        assertThat(recordList.get(0), equalTo(new CurrencyRatesDictionaryRecord("RUR", 225, "RUR", 225, Double.valueOf("1.0"))));
        assertThat(recordList.get(1), equalTo(new CurrencyRatesDictionaryRecord("USD", 201, "RUR", 225, Double.valueOf("58.9325"))));
        assertThat(recordList.get(2), equalTo(new CurrencyRatesDictionaryRecord("EUR", 315, "RUR", 225, Double.valueOf("68.6623"))));
        assertThat(recordList.get(3), equalTo(new CurrencyRatesDictionaryRecord("UAH", 187, "RUR", 225, Double.valueOf("2.27539"))));

    }
}
