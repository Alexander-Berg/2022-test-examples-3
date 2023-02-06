package ru.yandex.market.common.report.parser.json;

import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.SkuOffers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class SkuOffersReportJsonParserTest {
    private SkuOffersReportJsonParser parser;

    @Before
    public void setUp() {
        parser = new SkuOffersReportJsonParser();
    }

    @Test
    public void parseValid() throws IOException {
        SkuOffers skuOffers = parser.parse(
                SkuOffersReportJsonParserTest.class.getResourceAsStream("/files/sku_offers.json")
        );

        Assert.assertEquals("Parsed 2 skus", 2, skuOffers.getSkuToOffers().size());

        List<FoundOffer> firstSkuOffers = skuOffers.getOffersBy("100126185558");

        Assert.assertNotNull(firstSkuOffers);
        Assert.assertThat(firstSkuOffers, hasSize(1));

        Assert.assertThat(skuOffers.wasDeleted("100126185550"), is(true));
        Assert.assertThat(skuOffers.hasSku("100126185550"), is(false));
        Assert.assertThat(skuOffers.getSkuByDeleted("100126185550"), is("100126185558"));

        FoundOffer foundOffer = Iterables.getOnlyElement(firstSkuOffers);
        Assert.assertEquals(475690L, foundOffer.getFeedId().longValue());
        Assert.assertEquals("200344231.100126185558", foundOffer.getShopOfferId());
        Assert.assertEquals(431782L, foundOffer.getShopId().longValue());
        Assert.assertEquals
                ("PAd4m2lIcKBmjNV7_eS4pQLmBnj1widqbQ" +
                        "-T3WzBMUUI7kARSOlRjMFzX9zk0yKDN8LTFeiwLep_PT9zC9MpXdCCBdfGg_xY_B2S1OzSLAk8SbqMWUqAmuauMEU1JkTn", foundOffer.getFeeShow());

        List<FoundOffer> secondSkuOffers = skuOffers.getOffersBy("100131946306");

        Assert.assertNotNull(secondSkuOffers);
        Assert.assertThat(secondSkuOffers, hasSize(1));

        FoundOffer secondOffer = Iterables.getOnlyElement(secondSkuOffers);
        Assert.assertEquals(475690L, secondOffer.getFeedId().longValue());
        Assert.assertEquals("200344167.100131946306", secondOffer.getShopOfferId());
        Assert.assertEquals(431782L, secondOffer.getShopId().longValue());
        Assert.assertEquals
                ("OJaol_-mgzNbT8f78I1nb0vwrQeZfn5lVHAmnAVRNc72Vu-78tJnCuhm7idEtL-o99zu8xHZ0vqMnOL60RDb5bXqVA8WiYV72hA" +
                        "-IcQKTwpdSSeiXedoYKzzcJj8Vwql", secondOffer.getFeeShow());

    }

    @Test
    public void parseNoItem() throws IOException {
        SkuOffers skuOffers = parser.parse(
                SkuOffersReportJsonParserTest.class.getResourceAsStream("/files/sku_offers_noitem.json")
        );

        Assert.assertEquals("Parsed 2 skus", 2, skuOffers.getSkuToOffers().size());

        Map<String, List<FoundOffer>> skuToOffers = skuOffers.getSkuToOffers();

        List<FoundOffer> firstSkuOffers = skuToOffers.get("100126185558");

        Assert.assertNotNull(firstSkuOffers);
        Assert.assertThat(firstSkuOffers, hasSize(0));

        List<FoundOffer> secondSkuOffers = skuToOffers.get("100131946306");

        Assert.assertNotNull(secondSkuOffers);
        Assert.assertThat(secondSkuOffers, hasSize(1));

        FoundOffer secondOffer = Iterables.getOnlyElement(secondSkuOffers);
        Assert.assertEquals(475690L, secondOffer.getFeedId().longValue());
        Assert.assertEquals("200344167.100131946306", secondOffer.getShopOfferId());
        Assert.assertEquals(431782L, secondOffer.getShopId().longValue());
        Assert.assertEquals
                ("OJaol_-mgzNbT8f78I1nb0vwrQeZfn5lVHAmnAVRNc72Vu-78tJnCuhm7idEtL-o99zu8xHZ0vqMnOL60RDb5bXqVA8WiYV72hA" +
                        "-IcQKTwpdSSeiXedoYKzzcJj8Vwql", secondOffer.getFeeShow());

    }
}
