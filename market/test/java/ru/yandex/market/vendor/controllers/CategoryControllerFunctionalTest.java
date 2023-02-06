package ru.yandex.market.vendor.controllers;

import java.util.List;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.vendor.brand.BrandInfoService;
import ru.yandex.vendor.category.Category;

import static net.javacrumbs.jsonunit.JsonAssert.when;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

/**
 * Тест для {@link CategoryController}.
 */
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/CategoryControllerFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/CategoryControllerFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
class CategoryControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {


    public final BrandInfoService brandInfoService;

    @Autowired
    CategoryControllerFunctionalTest(BrandInfoService brandInfoService) {
        this.brandInfoService = brandInfoService;
    }

    /**
     * Тест проверяет, что запрос к ресурсу {@code GET /categories}
     * ({@link CategoryController#getCategories(String, Long, Category.Type, List,
     * Boolean, Boolean, Boolean, Boolean, Boolean, NavigationTreeType, Integer, Integer)})
     * возвращает корректный результат.
     */
    @Test
    void testGetAllCategories() {
        String response = FunctionalTestHelper.get(baseUrl + "/categories?uid=100500");
        String expected = getStringResource("/testGetAllCategories/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(before = "/ru/yandex/market/vendor/controllers/CategoryControllerFunctionalTest/testGetParentCategories/before.csv")
    void testGetParentCategories() {
        String response = FunctionalTestHelper.get(baseUrl +
                "/categories?relativeCategoryId=13041460&uid=100500&collectorType=PARENT");
        String expected = getStringResource("/testGetParentCategories/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(before = "/ru/yandex/market/vendor/controllers/CategoryControllerFunctionalTest/testGetSiblingCategories/before.csv")
    void getSiblingCategories() {
        String response = FunctionalTestHelper.get(baseUrl +
                "/categories?relativeCategoryId=13041460&uid=100500&collectorType=SIBLING");
        String expected = getStringResource("/testGetSiblingCategories/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что поведение при acceptGoodContent=false такое же, как и без параметра
     */
    @Test
    void testNotAcceptGoodContent() {
        String response = FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&acceptGoodContent=false");
        String expected = getStringResource("/testGetAllCategories/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет работоспособность фильтра по типу выдачи при запросе категорий
     */
    @Test
    void testGetCategoriesWithFilterByOutputType() {
        String response = FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&outputType=GURU,CLUSTER");
        String expected = getStringResource("/testGetCategoriesWithFilterByOutputType/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет работоспособность фильтра по групповым при запросе категорий
     */
    @Test
    void testGetCategoriesWithFilterByGrouped() {
        String response = FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&grouped=true");
        String expected = getStringResource("/testGetCategoriesWithFilterByGrouped/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет работоспособность фильтра по возможности создавать SKU при запросе категорий
     */
    @Test
    void testGetCategoriesWithFilterByCanCreateSku() {
        String response = FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&canCreateSku=true");
        String expected = getStringResource("/testGetCategoriesWithFilterByCanCreateSku/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет работоспособность фильтра по имени при запросе категорий
     */
    @Test
    void testGetCategoriesWithFilterByName() {
        String response = FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&name=1");
        String expected = getStringResource("/testGetCategoriesWithFilterByName/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }


    /**
     * Тест проверяет работоспособность фильтра по вендору при запросе категорий
     */
    @Test
    public void testGetCategoriesWithFilterByVendorId() {
        String response = FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&vendorId=321");
        String expected = getStringResource("/testGetCategoriesWithFilterByVendorId/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет работоспособность фильтра по типу выдачи при запросе категорий
     */
    @Test
    void testGetCategoriesWithFilterByType() {
        String response = FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&type=GROUP");
        String expected = getStringResource("/testGetCategoriesWithFilterByType/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет работоспособность всех фильтров при запросе категорий
     */
    @Test
    void testGetCategoriesWithAllFilters() {
        String response = FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&outputType=SIMPLE&grouped=false" +
                "&canCreateSku=false&name=3&vendorId=654&type=LEAF");
        String expected = getStringResource("/testGetCategoriesWithAllFilters/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет работоспособность пагинации при указании корректных параметров
     */
    @Test
    void testGetCategoriesWithCorrectPagerParams() {
        String response = FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&vendorId=321&page=1&pageSize=1");
        String expected = getStringResource("/testGetCategoriesWithCorrectPagerParams/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет работоспособность пагинации при пропуске параметров
     */
    @Test
    void testGetCategoriesWithNoPagerParams() {
        String response = FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&vendorId=321");
        String expected = getStringResource("/testGetCategoriesWithNoPagerParams/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что кол-во элементов на 2-ой странице будет равно 1
     */
    @Test
    void testGetCategoriesFromSecondPageWithPageSizeEqualsTwo() {
        String response = FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&vendorId=321&pageSize=2&page=2");
        String expected = getStringResource("/testGetCategoriesFromSecondPageWithPageSizeEqualsTwo/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Тест проверяет, что кол-во элементов на 1-ой странице будет равно 2
     */
    @Test
    void testGetCategoriesFromFirstPageWithPageSizeEqualsTwo() {
        String response = FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&vendorId=321&pageSize=2&page=1");
        String expected = getStringResource("/testGetCategoriesFromFirstPageWithPageSizeEqualsTwo/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    void testGetVendorCategoriesWithModelsWithPriorityCategoryId() {
        String response = FunctionalTestHelper.get(baseUrl + "/categories?uid=100500&vendorId=321&pageSize=2&page=1" +
                "&type=LEAF&priorityCategoryId=666&onlyWithModels=true");
        String expected = getStringResource("/testGetVendorCategoriesWithModelsWithPriorityCategoryId/expected.json");
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    void testGetCategoriesByIds() {
        String response = FunctionalTestHelper.get(baseUrl +
                "/categories?uid=100500&categoryIds=555&categoryIds=777&categoryIds=444");
        String expected = getStringResource("/testGetCategoriesByIds/expected.json");
        JsonAssert.assertJsonEquals(expected, response);
    }

}
