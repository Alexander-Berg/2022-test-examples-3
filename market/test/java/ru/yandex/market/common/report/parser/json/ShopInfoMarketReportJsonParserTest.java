package ru.yandex.market.common.report.parser.json;

import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.common.report.model.ShopInfoSearchResult;
import ru.yandex.market.common.report.model.ShopInfoSearchResults;

import java.io.IOException;
import java.util.List;

public class ShopInfoMarketReportJsonParserTest {
    @Test
    public void shopParseShopInfoOutcome() throws IOException {
        ShopInfoMarketReportJsonParserSettings settings = new ShopInfoMarketReportJsonParserSettings();
        settings.setResults("results");
        settings.setDeliveryCurrency("deliveryCurrency");
        settings.setShopCurrency("shopCurrency");
        settings.setShopId("id");
        ShopInfoMarketReportJsonParser parser = new ShopInfoMarketReportJsonParser(settings);
        parser.parse(ShopInfoMarketReportJsonParserTest.class.getResourceAsStream("/files/shop_info.json"));

        ShopInfoSearchResults searchResults = parser.getSearchResults();
        Assert.assertNotNull(searchResults);
        List<ShopInfoSearchResult> results = searchResults.getResults();
        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.size());
        ShopInfoSearchResult result = Iterables.getOnlyElement(results);
        Assert.assertEquals(6102L, result.getShopId());
        Assert.assertEquals(Currency.RUR, result.getShopCurrency());
        Assert.assertEquals(Currency.RUR, result.getDeliveryCurrency());
    }
}
