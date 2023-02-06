package ru.yandex.market.common.report.parser.json;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Iterables;
import org.junit.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.common.report.model.ShopInfoSearchResult;
import ru.yandex.market.common.report.model.ShopInfoSearchResults;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

/**
 * @author ungomma
 */
public class ShopInfoSearchResultsJsonParserTest {

    @Test
    public void shopParseShopInfoOutcome() throws IOException {
        ShopInfoSearchResultsJsonParser parser = new ShopInfoSearchResultsJsonParser();
        ShopInfoSearchResults searchResults =
                parser.parse(ShopInfoMarketReportJsonParserTest.class.getResourceAsStream("/files/shop_info_2.json"));

        assertThat(searchResults, notNullValue());
        List<ShopInfoSearchResult> results = searchResults.getResults();
        assertThat(results, notNullValue());
        assertThat(results, hasSize(3));

        ShopInfoSearchResult result = Iterables.getFirst(results, null);
        assertThat(result.getShopId(), is(475177L));
        assertThat(result.getShopCurrency(), is(Currency.RUR));
        assertThat(result.getDeliveryCurrency(), is(Currency.RUR));
        assertThat(result.isIgnoreStocks(), is(true));
        assertThat(result.isCrossborder(), is(true));
        assertThat(result.getShopName(), is("Besseller"));
        assertThat(result.getShopPriorityRegion().getId().get(), is(109371));
        assertThat(result.getShopPriorityCountry().getId().get(), is(134));
    }
}
