package ru.yandex.market.mbi.api.controller.cpa;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.collections.CollectionUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.api.client.entity.shops.CpaShopInfo;
import ru.yandex.market.mbi.api.client.entity.shops.PagedCpaShops;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author fbokovikov
 */
class GetCpaShopsFunctionalTest extends FunctionalTest {

    private static final int PAGE_NUMBER = 1;
    private static final int PAGE_SIZE = 10;
    private static final long SHOP_ID = 774L;

    /**
     * Проверяем результат работы ручки при запросе по "несуществующему" datasource_id.
     */
    @Test
    @DbUnitDataSet
    void testNonExistentDatasourceId() {
        PagedCpaShops cpaShops = mbiApiClient.getCpaShops(Arrays.asList(SHOP_ID), PAGE_NUMBER, PAGE_SIZE, false);
        assertTrue(CollectionUtils.isEmpty(cpaShops.getCpaShopInfos()));
    }

    /**
     * Проверяем формирование полного вывода ручки (проверяется, что в случае размещения по CPA через ПИ в
     * <cpa-regions><cpa-regions/> возвращается только локальный регион доставки, а <all-cpa-regions></all-cpa-regions>
     * возвращаются все настроенные регионы доставки)
     */
    @Test
    @DbUnitDataSet(before = "testFullOutput.before.csv")
    void testFullOutput() {
        PagedCpaShops cpaShops = mbiApiClient.getCpaShops(Arrays.asList(SHOP_ID), PAGE_NUMBER, PAGE_SIZE, false);
        Collection<CpaShopInfo> shopInfos = cpaShops.getCpaShopInfos();
        assertTrue(shopInfos.size() == 1);
        CpaShopInfo shopInfo = shopInfos.iterator().next();
        assertCpaShopInfo(shopInfo);

        assertThat(shopInfo.getCutoffsForTesting(), nullValue());
    }

    private void assertCpaShopInfo(CpaShopInfo shopInfo) {
        assertTrue(shopInfo.getCpaRegions().size() == 1);
        assertTrue(shopInfo.getCpaRegions().iterator().next().longValue() == 213L);
        assertFalse(CollectionUtils.isEmpty(shopInfo.getAllCpaRegions()));

        MatcherAssert.assertThat(shopInfo.getAllCpaRegions(),
                Matchers.containsInAnyOrder(1L, 2L, 3L, 4L, 5L, 6L));
    }
}
