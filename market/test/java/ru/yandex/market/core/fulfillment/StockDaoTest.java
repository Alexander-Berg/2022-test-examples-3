package ru.yandex.market.core.fulfillment;

import java.math.BigDecimal;
import java.util.List;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.fulfillment.matchers.ShopSkuInfoMatchers;
import ru.yandex.market.core.fulfillment.model.ShopSkuInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;


/**
 * @author Vadim Lyalin
 */
class StockDaoTest extends FunctionalTest {
    private static final Matcher<ShopSkuInfo> EXPECTED_SHOP_SKU_INFO_1_MATCHER = allOf(
            ShopSkuInfoMatchers.hasSupplierId(1L),
            ShopSkuInfoMatchers.hasShopSku("teabag1"),
            ShopSkuInfoMatchers.hasLength(1),
            ShopSkuInfoMatchers.hasWidth(2),
            ShopSkuInfoMatchers.hasHeight(3),
            ShopSkuInfoMatchers.hasWeight(new BigDecimal("0.999"))
    );

    @Autowired
    private StockDao stockDao;

    @Test
    @DbUnitDataSet(before = "StockDaoTest.before.csv")
    void testGetShopSkuInfos() {
        List<ShopSkuInfo> shopSkuInfos = stockDao.getShopSkuInfos(1L);

        assertThat(shopSkuInfos).hasSize(3);
        MatcherAssert.assertThat(shopSkuInfos, hasItem(EXPECTED_SHOP_SKU_INFO_1_MATCHER));
    }

    @Test
    void clearTable() {
        // simple smoke test
        stockDao.clearTable("shops_web.imported_stocks_alt");
    }
}
