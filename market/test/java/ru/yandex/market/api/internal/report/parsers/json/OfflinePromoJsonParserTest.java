package ru.yandex.market.api.internal.report.parsers.json;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.ShopInfoV2;
import ru.yandex.market.api.domain.v2.promo.shop.PromoShopInfo;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.report.parsers.json.filters.FilterFactory;
import ru.yandex.market.api.util.CommonCollections;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 18.10.2018
 */
public class OfflinePromoJsonParserTest extends UnitTestBase {

    @Test
    public void testWorksOnExpectedReportResponse() {
        List<PromoShopInfo> shops = parse("offline-promo.json");

        Map<Long, PromoShopInfo> data = toMap(shops);
        assertShopsData(shops, data);
    }

    @Test
    public void testWorksOnExpectedReportResponse2() {
        // result filters rearranged
        // some data is other filters is not in expected format
        List<PromoShopInfo> shops = parse("offline-promo-2.json");

        Map<Long, PromoShopInfo> data = toMap(shops);
        assertShopsData(shops, data);
    }

    private void assertShopsData(List<PromoShopInfo> shops, Map<Long, PromoShopInfo> data) {
        long firstShopId = 479558L;
        long secondShopId = 10275457L;
        long thirdShopId = 138471986L;

        assertTrue(data.containsKey(firstShopId));
        assertTrue(data.containsKey(secondShopId));
        assertTrue(data.containsKey(thirdShopId));
        assertEquals(3, data.size());
        assertEquals(3, shops.size());

        assertEquals("test1", data.get(firstShopId).getName());
        assertEquals(583, data.get(firstShopId).getPromos().getPromoCount());

        assertEquals("Some other shop", data.get(secondShopId).getName());
        assertEquals(423, data.get(secondShopId).getPromos().getPromoCount());
    }

    @NotNull
    private Map<Long, PromoShopInfo> toMap(List<PromoShopInfo> shops) {
        return CommonCollections.index(shops, ShopInfoV2::getId);
    }

    private List<PromoShopInfo> parse(String filename) {
        return new OfflinePromoJsonParser(new FilterFactory())
            .parse(ResourceHelpers.getResource(filename));
    }
}
