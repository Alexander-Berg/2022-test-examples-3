package ru.yandex.market.common.report.parser.json;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.common.report.model.CurrencyConvertResult;

import java.io.IOException;
import java.math.BigDecimal;

public class CurrencyConvertMarketReportJsonParserTest {

    @Test
    public void shouldParseCurrencyConvertPlace() throws IOException {
        CurrencyConvertMarketReportJsonParser parser = new CurrencyConvertMarketReportJsonParser(new CurrencyConvertMarketReportJsonParserSettings());
        parser.parse(CurrencyConvertMarketReportJsonParserTest.class.getResourceAsStream("/files/currency_convert.json"));
        CurrencyConvertResult result = parser.getResult();

        Assert.assertNotNull(result);
        Assert.assertEquals(BigDecimal.valueOf(35), result.getValue());
        Assert.assertEquals(BigDecimal.valueOf(2017.197), result.getConvertedValue());
        Assert.assertEquals(BigDecimal.valueOf(35), result.getRenderedValue());
        Assert.assertEquals(BigDecimal.valueOf(2017), result.getRenderedConvertedValue());
        Assert.assertEquals(Currency.RUR, result.getCurrencyTo());
        Assert.assertEquals(Currency.USD, result.getCurrencyFrom());
    }
}
