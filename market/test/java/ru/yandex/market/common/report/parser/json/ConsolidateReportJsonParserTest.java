package ru.yandex.market.common.report.parser.json;

import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.common.report.model.ConsolidatedOffers;
import ru.yandex.market.common.report.model.FoundOffer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

public class ConsolidateReportJsonParserTest {

    private ConsolidateReportJsonParser parser;

    @Before
    public void setUp() {
        parser = new ConsolidateReportJsonParser();
    }

    @Test
    public void parseValid() throws IOException {
        ConsolidatedOffers consolidatedOffers = parser.parse(
                ConsolidateReportJsonParser.class.getResourceAsStream("/files/consolidate.json")
        );

        Assert.assertEquals("Parsed 2 skus", 2, consolidatedOffers.getSkuToOffers().size());

        Map<String, List<FoundOffer>> skuToOffers = consolidatedOffers.getSkuToOffers();

        List<FoundOffer> firstSkuOffers = skuToOffers.get("100131945735");

        Assert.assertNotNull(firstSkuOffers);
        Assert.assertThat(firstSkuOffers, hasSize(1));

        FoundOffer foundOffer = Iterables.getOnlyElement(firstSkuOffers);
        Assert.assertEquals(200377618L, foundOffer.getFeedId().longValue());
        Assert.assertEquals("cross-100131945735", foundOffer.getShopOfferId());
        Assert.assertEquals(10283952L, foundOffer.getShopId().longValue());
        Assert.assertEquals
                ("OJaol_-mgzOBnbZqZWpH8gFOqYMseibh0ftKczPV9omgFgx8Oo1zQb6VjI9IycQobS3Lin27ev9h6RCVReuUQ-" +
                        "1MS3tBjVJM4kk-Qj7OsaDVzeyA3jkXTiZBl6KN0S-cWcvWsO3pNqQ,", foundOffer.getFeeShow());

        List<FoundOffer> secondSkuOffers = skuToOffers.get("100131945734");

        Assert.assertNotNull(secondSkuOffers);
        Assert.assertThat(secondSkuOffers, hasSize(1));

        FoundOffer secondOffer = Iterables.getOnlyElement(secondSkuOffers);
        Assert.assertEquals(200377618L, secondOffer.getFeedId().longValue());
        Assert.assertEquals("cross-100131945734", secondOffer.getShopOfferId());
        Assert.assertEquals(10283952L, secondOffer.getShopId().longValue());
        Assert.assertEquals
                ("OJaol_-mgzOdJqanHbsdv92HNgfpTS8241Fwb9YzbBYMbfzC6eR84puQfyfhzbDVDZe3_oirlKcGt3QonHyP2SPdPTl4Iyafq-" +
                        "zkc0Wi0cjDdKlxWHS_yxqkR37l7tIA", secondOffer.getFeeShow());

        Assert.assertEquals(2, consolidatedOffers.getTotal().intValue());
        Assert.assertEquals(2, consolidatedOffers.getTotalOffers().intValue());
        Assert.assertEquals(1, consolidatedOffers.getTotalWarehouses().intValue());
        Assert.assertEquals(true, consolidatedOffers.isReplaced());

    }

    @Test
    public void parseWithErrors() throws IOException {
        ConsolidatedOffers consolidatedOffers = parser.parse(
                ConsolidateReportJsonParser.class.getResourceAsStream("/files/consolidate_errors.json")
        );

        Assert.assertEquals("Parsed 1 sku", 1, consolidatedOffers.getSkuToOffers().size());
        List<FoundOffer> skuOffers = consolidatedOffers.getSkuToOffers().get("228153968");
        Assert.assertNotNull(skuOffers);
        Assert.assertThat(skuOffers, hasSize(0));

        Assert.assertEquals(1, consolidatedOffers.getTotal().intValue());
        Assert.assertEquals(0, consolidatedOffers.getTotalOffers().intValue());
        Assert.assertEquals(0, consolidatedOffers.getTotalWarehouses().intValue());
        Assert.assertEquals(false, consolidatedOffers.isReplaced());
        Assert.assertThat(consolidatedOffers.getErrors(), hasSize(1));
        Assert.assertThat(consolidatedOffers.getErrors(), contains("No warehouses for specified region"));


    }
}
