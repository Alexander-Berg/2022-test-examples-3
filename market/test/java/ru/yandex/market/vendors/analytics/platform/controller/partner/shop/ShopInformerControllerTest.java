package ru.yandex.market.vendors.analytics.platform.controller.partner.shop;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "ShopInformerControllerTest.shopCategoryShare.before.csv")
@ClickhouseDbUnitDataSet(before = "ShopInformerControllerTest.shopCategoryShare.clickhouse.csv")
public class ShopInformerControllerTest extends FunctionalTest {

    private static final String PATH = "/calculate/shop/share";

    @Test
    void shopCategoryShare() {
        var actual = calcShopShare(774, 91491);
        var expected = loadFromFile("ShopInformerControllerTest.shopCategoryShare.response.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    void beruTypeCategoryShareTotal() {
        var actual = calcShopShare(775, 91491);
        var expected = loadFromFile("ShopInformerControllerTest.beruTypeCategoryShareTotal.response.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    void beruTypeCategoryShareSupplier() {
        var actual = calcShopShare(776, 91491);
        var expected = loadFromFile("ShopInformerControllerTest.beruTypeCategoryShareSupplier.response.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    void absentCategory() {
        var actual = calcShopShare(774, 999);
        var expected = loadFromFile("ShopInformerControllerTest.absentCategory.response.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }

    private String calcShopShare(long shopId, long hid) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .path(PATH)
                .queryParam("shopId", shopId)
                .queryParam("hid", hid)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }
}
