package ru.yandex.market.api.common.currency;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import ru.yandex.market.api.geo.GeoUtils;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ApiStrings;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class CurrencyDataParserTest extends UnitTestBase {

    private static final double DELTA = 1E-10;

    @Test
    public void shouldParseFile() throws Exception {
        CurrencyData currencyData = new CurrencyDataParser().parse(ResourceHelpers.getResource("test_currency_rates.xml"));

        assertEquals(Currency.RUR, currencyData.getCountryCurrency(GeoUtils.Country.RUSSIA));
        assertEquals(0.213_669, currencyData.getRate(Currency.KZT, Currency.RUR), DELTA);
        assertEquals(0.003_823_31, currencyData.getRate(Currency.BYR, Currency.RUR), DELTA);
    }

    @Test
    public void shouldReturnBynAsDefaultBelarussianCurrency() throws Exception {
        CurrencyData currencyData = new CurrencyDataParser().parse(ResourceHelpers.getResource("currency_rates_byn_default.xml"));

        assertEquals(Currency.BYN, currencyData.getCountryCurrency(GeoUtils.Country.BELARUS));
    }

    @Test
    public void shouldReturnRateForAlias() throws Exception {
        CurrencyData currencyData = new CurrencyDataParser().parse(ResourceHelpers.getResource("currency_rates_byn_default.xml"));

        assertEquals(0.003_071_93, currencyData.getRate(Currency.BYR, Currency.RUR), DELTA);
        assertEquals(325.95, currencyData.getRate(Currency.RUR, Currency.BYR), DELTA);
    }

    @Test
    public void shouldParseAliasRateElement() throws Exception {
        CurrencyDataParser.AliasRate parsed = new CurrencyDataParser.AliasRateParser().parse(
            "<alias rate_to_primary=\"0.0001\">BYR</alias>".getBytes(ApiStrings.UTF8)
        );

        assertEquals(Currency.BYR, parsed.getCurrency());
        assertEquals(0.0001d, parsed.getRate(), DELTA);
    }

    @Test
    public void shouldParseAliasRateList() throws Exception {
        Pair<Currency, List<CurrencyDataParser.AliasRate>> parsed = new CurrencyDataParser.AliasesParser().parse(
            ("<currency name=\"BYN\">\n" +
                "<alias rate_to_primary=\"0.0001\">BYR</alias>\n" +
                "<alias>RUR</alias>\n" +
                "</currency>"
            ).getBytes(ApiStrings.UTF8)
        );
        assertEquals(Currency.BYN, parsed.getKey());
        assertEquals(2, parsed.getValue().size());
        assertEquals(Currency.BYR, parsed.getValue().get(0).getCurrency());
        assertEquals(0.0001d, parsed.getValue().get(0).getRate(), DELTA);
        assertEquals(Currency.RUR, parsed.getValue().get(1).getCurrency());
        assertEquals(1d, parsed.getValue().get(1).getRate(), DELTA);
    }
}
