package ru.yandex.market.vendor.controllers;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

import static net.javacrumbs.jsonunit.JsonAssert.when;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

/**
 * Тест для {@link ShopInfoController}.
 */
@DbUnitDataSet(before = "/ru/yandex/market/vendor/controllers/ShopInfoControllerFunctionalTest/before.csv")
class ShopInfoControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    /**
     * Тест проверяет, что запрос к ресурсу {@code GET /brands} ({@link ShopInfoController#getBrands(String, Integer, Integer)})
     * возвращает корректный результат.
     */
    @Test
    void testGetBrandsWithFilterByName() {
        String response = FunctionalTestHelper.get(baseUrl + "/brands?name=Hansa");
        String expected = getStringResource( "/testGetBrandsWithFilterByName/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что запрос к ресурсу {@code GET /brands} ({@link ShopInfoController#getBrands(String, Integer, Integer)})
     * возвращает корректный результат при запросе по которому ничего не находится.
     */
    @Test
    void testGetBrandsWithFilterByNameWithEmptyResult() {
        String response = FunctionalTestHelper.get(baseUrl + "/brands?name=Hanso");
        String expected = getStringResource( "/testGetBrandsWithFilterByNameWithEmptyResult/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/ShopInfoControllerFunctionalTest/testGetCategoryBrands/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetCategoryBrands() {
        String response = FunctionalTestHelper.get(baseUrl + "/brands?categoryIds=1");
        String expected = getStringResource( "/testGetCategoryBrands/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testGetBrandsByIds() {
        String response = FunctionalTestHelper.get(baseUrl + "/brands?brandIds=10766590&brandIds=10766591&brandIds=10766592");
        String expected = getStringResource( "/testGetBrandsByIds/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }
}
