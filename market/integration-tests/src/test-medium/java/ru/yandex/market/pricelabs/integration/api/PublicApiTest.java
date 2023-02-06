package ru.yandex.market.pricelabs.integration.api;

import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.api.api.PublicApi;
import ru.yandex.market.pricelabs.api.api.PublicApiInterfaces;
import ru.yandex.market.pricelabs.generated.server.pub.model.SearchCategoriesResponse;
import ru.yandex.market.pricelabs.generated.server.pub.model.SearchCategoriesResponseCount;
import ru.yandex.market.pricelabs.generated.server.pub.model.SearchOffersResponse;
import ru.yandex.market.pricelabs.generated.server.pub.model.SearchParametersResponse;
import ru.yandex.market.pricelabs.generated.server.pub.model.SearchVendorsResponse;
import ru.yandex.market.pricelabs.model.ShopCategory;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.SHOP_ID;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.SHOP_ID_2;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.SHOP_ID_N;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.SHOP_ID_N2;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.UNKNOWN_SHOP;
import static ru.yandex.market.pricelabs.integration.api.PublicPartnerApiTest.RELATIVE_ABOVE_AVG_PRICE_FILTER;
import static ru.yandex.market.pricelabs.integration.api.PublicPartnerApiTest.RELATIVE_BELOW_AVG_PRICE_FILTER;
import static ru.yandex.market.pricelabs.integration.api.PublicPartnerApiTest.RELATIVE_MIN_PRICE_FILTER;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.filter;

public class PublicApiTest extends AbstractApiTests {

    @Autowired
    private PublicApi publicApiBean;
    private PublicApiInterfaces publicApi;

    static Object[][] searchOffersPostOffsetFilterByPriceData() {
        return new Object[][]{
                {"offer.price <= model.min", RELATIVE_MIN_PRICE_FILTER, (Predicate<SearchOffersResponse>)
                        (offer -> offer.getPrice() <= offer.getModelDetails().getMinPrice())},
                {"offer.price <= model.avg", RELATIVE_BELOW_AVG_PRICE_FILTER, (Predicate<SearchOffersResponse>)
                        (offer -> offer.getPrice() <= offer.getModelDetails().getAvgPrice())},
                {"offer.price > model.avg", RELATIVE_ABOVE_AVG_PRICE_FILTER, (Predicate<SearchOffersResponse>)
                        (offer -> offer.getPrice() > offer.getModelDetails().getAvgPrice())}
        };
    }

    private static SearchVendorsResponse vendor(String vendor) {
        return new SearchVendorsResponse().vendorName(vendor);
    }

    private static SearchParametersResponse param(String param) {
        return new SearchParametersResponse().parameterName(param);
    }

    @BeforeEach
    void init() {
        publicApi = buildProxy(PublicApiInterfaces.class, publicApiBean);
        super.init();
    }

    @Test
    void searchCategoriesGet() {
        var ret = publicApi.searchCategoriesGet(List.of(SHOP_ID, SHOP_ID_2), List.of(45L, 46L), "Test",
                List.of(-1L, 49L, 0L), null, null, null, null);
        checkResponse(ret);
    }

    @Test
    void searchCategoriesGetWithOffers() {
        var ret = publicApi.searchCategoriesGet(List.of(SHOP_ID, SHOP_ID_2), List.of(45L, 46L), "Test",
                List.of(-1L, 49L, 0L), null, null, null, true);
        checkResponse(ret);
    }

    @Test
    void searchCategoriesGetUnique() {
        var ret = publicApi.searchCategoriesGet(List.of(SHOP_ID, SHOP_ID_2), List.of(45L, 46L), "Test",
                List.of(-1L, 49L, 0L), true, null, null, null);
        checkResponse(ret);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void searchCategoriesGetExact(boolean withOffersOnly) {
        var ret = publicApi.searchCategoriesGet(List.of(SHOP_ID_N, UNKNOWN_SHOP),
                null, null, null, false, null, null, withOffersOnly);
        var list = checkResponse(ret);
        assertEquals(List.of(
                new SearchCategoriesResponse()
                        .categoryId((long) SHOP_ID_N + 1).categoryName("name 1")
                        .parentCategoryId(ShopCategory.NO_CATEGORY).offersCount(1).childrenCount(10).categoryCount(1)
                        .level(1),
                new SearchCategoriesResponse()
                        .categoryId((long) SHOP_ID_N + 2).categoryName("name 2")
                        .parentCategoryId(ShopCategory.NO_CATEGORY).offersCount(21).childrenCount(2).categoryCount(1)
                        .level(1),
                new SearchCategoriesResponse()
                        .categoryId((long) SHOP_ID_N + 3).categoryName("name 3")
                        .parentCategoryId(ShopCategory.NO_CATEGORY).offersCount(199).childrenCount(299).categoryCount(1)
                        .level(1),
                new SearchCategoriesResponse()
                        .categoryId((long) SHOP_ID_N + 1).categoryName("name 1.1")
                        .parentCategoryId((long) SHOP_ID_N + 1).offersCount(2).childrenCount(20).categoryCount(1)
                        .level(2),
                new SearchCategoriesResponse()
                        .categoryId((long) SHOP_ID_N + 3).categoryName("name 3")
                        .parentCategoryId((long) SHOP_ID_N + 3).offersCount(399).childrenCount(499).categoryCount(1)
                        .level(2),
                new SearchCategoriesResponse()
                        .categoryId((long) SHOP_ID_N + 1).categoryName("name 1.2")
                        .parentCategoryId((long) SHOP_ID_N + 1).offersCount(3).childrenCount(30).categoryCount(1)
                        .level(3),
                new SearchCategoriesResponse()
                        .categoryId((long) SHOP_ID_N + 4).categoryName("name 4")
                        .parentCategoryId((long) SHOP_ID_N + 1).offersCount(3).childrenCount(4).categoryCount(1)
                        .level(3),
                new SearchCategoriesResponse()
                        .categoryId((long) SHOP_ID_N + 4).categoryName("name 4")
                        .parentCategoryId((long) SHOP_ID_N + 3).offersCount(1).childrenCount(2).categoryCount(1)
                        .level(3),
                new SearchCategoriesResponse()
                        .categoryId((long) SHOP_ID_N + 5).categoryName("name 4.1")
                        .parentCategoryId((long) SHOP_ID_N + 4).offersCount(19).childrenCount(1).categoryCount(1)
                        .level(4)), list);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void searchCategoriesGetUniqueExact(boolean withOffersOnly) {
        var ret = publicApi.searchCategoriesGet(List.of(SHOP_ID_N, UNKNOWN_SHOP),
                null, null, null, true, null, null, withOffersOnly);
        var list = checkResponse(ret);
        assertEquals(List.of(
                new SearchCategoriesResponse()
                        .categoryId((long) SHOP_ID_N + 1)
                        .categoryName("name 1, name 1.1, name 1.2")
                        .parentCategoryId(ShopCategory.NO_CATEGORY)
                        .offersCount(1 + 2 + 3).childrenCount(10 + 20 + 30).categoryCount(3)
                        .level(1),
                new SearchCategoriesResponse()
                        .categoryId((long) SHOP_ID_N + 2)
                        .categoryName("name 2")
                        .parentCategoryId(ShopCategory.NO_CATEGORY)
                        .offersCount(21).childrenCount(2).categoryCount(1)
                        .level(1),
                new SearchCategoriesResponse()
                        .categoryId((long) SHOP_ID_N + 3)
                        .categoryName("name 3") // одно название (уникальное)
                        .parentCategoryId(ShopCategory.NO_CATEGORY)
                        .offersCount(199 + 399).childrenCount(299 + 499).categoryCount(2)
                        .level(1),
                new SearchCategoriesResponse()
                        .categoryId((long) SHOP_ID_N + 4)
                        .categoryName("name 4") // одно название (уникальное)
                        .parentCategoryId((long) SHOP_ID_N + 1)
                        .offersCount(1 + 3).childrenCount(2 + 4).categoryCount(2)
                        .level(3),
                new SearchCategoriesResponse()
                        .categoryId((long) SHOP_ID_N + 5)
                        .categoryName("name 4.1")
                        .parentCategoryId((long) SHOP_ID_N + 4)
                        .offersCount(19).childrenCount(1).categoryCount(1)
                        .level(4)), list);
    }

    @Test
    void searchCategoriesGetExactN2() {
        var ret = publicApi.searchCategoriesGet(List.of(SHOP_ID_N2, UNKNOWN_SHOP),
                null, null, null, false, null, null, false);
        var list = checkResponse(ret);
        assertEquals(List.of(
                new SearchCategoriesResponse()
                        .categoryId((long) SHOP_ID_N + 1)
                        .categoryName("name 1")
                        .parentCategoryId(ShopCategory.NO_CATEGORY)
                        .offersCount(1).childrenCount(2).categoryCount(1)
                        .level(1),
                new SearchCategoriesResponse()
                        .categoryId((long) SHOP_ID_N + 2)
                        .categoryName("name 2")
                        .parentCategoryId((long) (SHOP_ID_N + 1))
                        .offersCount(1).childrenCount(1).categoryCount(1)
                        .level(2),
                new SearchCategoriesResponse()
                        .categoryId((long) SHOP_ID_N + 3)
                        .categoryName("name 3")
                        .parentCategoryId((long) (SHOP_ID_N + 2))
                        .offersCount(0).childrenCount(0).categoryCount(1)
                        .level(3)), list);
    }

    @Test
    void searchCategoriesGetExactN2WithOffersOnly() {
        var ret = publicApi.searchCategoriesGet(List.of(SHOP_ID_N2, UNKNOWN_SHOP),
                null, null, null, false, null, null, true);
        var list = checkResponse(ret);
        assertEquals(List.of(
                new SearchCategoriesResponse()
                        .categoryId((long) SHOP_ID_N + 1)
                        .categoryName("name 1")
                        .parentCategoryId(ShopCategory.NO_CATEGORY)
                        .offersCount(1)
                        .childrenCount(2)
                        .categoryCount(1)
                        .level(1),
                new SearchCategoriesResponse()
                        .categoryId((long) SHOP_ID_N + 2)
                        .categoryName("name 2")
                        .parentCategoryId((long) (SHOP_ID_N + 1))
                        .offersCount(1)
                        .childrenCount(1)
                        .categoryCount(1)
                        .level(2)), list);
    }

    @Test
    void searchCategoriesGetExactN2WithOffersOnlyAndUniquieId() {
        var ret = publicApi.searchCategoriesGet(List.of(SHOP_ID_N2, UNKNOWN_SHOP),
                null, null, null, true, null, null, true);
        var list = checkResponse(ret);
        assertEquals(List.of(
                new SearchCategoriesResponse()
                        .categoryId((long) SHOP_ID_N + 1)
                        .categoryName("name 1")
                        .parentCategoryId(ShopCategory.NO_CATEGORY)
                        .offersCount(1)
                        .childrenCount(2)
                        .categoryCount(1)
                        .level(1),
                new SearchCategoriesResponse()
                        .categoryId((long) SHOP_ID_N + 2)
                        .categoryName("name 2")
                        .parentCategoryId((long) (SHOP_ID_N + 1))
                        .offersCount(1)
                        .childrenCount(1)
                        .categoryCount(1)
                        .level(2)), list);
    }

    @Test
    void searchCategoriesGetUnknownShop() {
        var ret = publicApi.searchCategoriesGet(List.of(UNKNOWN_SHOP), List.of(45L, 46L), "Test",
                List.of(-1L, 49L, 0L), null, null, null, null);
        checkResponse(ret);
    }

    @Test
    void searchCategoriesCountGet() {
        var ret = publicApi.searchCategoriesCountGet(List.of(SHOP_ID, SHOP_ID_2), List.of(45L, 46L), "Test",
                List.of(-1L, 49L, 0L), null);
        assertEquals(new SearchCategoriesResponseCount(), checkResponse(ret));
    }

    @Test
    void searchCategoriesCountGetWithOffers() {
        var ret = publicApi.searchCategoriesCountGet(List.of(SHOP_ID, SHOP_ID_2), List.of(45L, 46L), "Test",
                List.of(-1L, 49L, 0L), true);
        assertEquals(new SearchCategoriesResponseCount(), checkResponse(ret));
    }

    @Test
    void searchCategoriesCountGetUnknownShop() {
        var ret = publicApi.searchCategoriesCountGet(List.of(UNKNOWN_SHOP), List.of(45L, 46L), "Test",
                List.of(-1L, 49L, 0L), null);
        assertEquals(new SearchCategoriesResponseCount(), checkResponse(ret));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void searchCategoriesCountExact(boolean withOffersOnly) {
        var ret = publicApi.searchCategoriesCountGet(List.of(SHOP_ID_N, UNKNOWN_SHOP), null, null, null,
                withOffersOnly);
        assertEquals(new SearchCategoriesResponseCount()
                .childrenCount(10 + 20 + 30 + 2 + 299 + 499 + 2 + 4 + 1)
                .offersCount(1 + 2 + 3 + 21 + 199 + 399 + 1 + 3 + 19)
                .categoryCount(9), checkResponse(ret));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void searchCategoriesCountFilterExact(boolean withOffersOnly) {
        var ret = publicApi.searchCategoriesCountGet(List.of(SHOP_ID_N), null, null,
                singletonList(ShopCategory.NO_CATEGORY), withOffersOnly);
        assertEquals(new SearchCategoriesResponseCount()
                .childrenCount(10 + 2 + 299)
                .offersCount(1 + 21 + 199)
                .categoryCount(3), checkResponse(ret));
    }

    @Test
    void searchCategoriesCountExactN2() {
        var ret = publicApi.searchCategoriesCountGet(List.of(SHOP_ID_N2, UNKNOWN_SHOP), null, null,
                List.of(ShopCategory.NO_CATEGORY), false);
        assertEquals(new SearchCategoriesResponseCount()
                .childrenCount(2)
                .offersCount(1)
                .categoryCount(1), checkResponse(ret));
    }

    @Test
    void searchCategoriesCountExactN2WithOffersOnly() {
        var ret = publicApi.searchCategoriesCountGet(List.of(SHOP_ID_N2, UNKNOWN_SHOP), null, null,
                List.of(ShopCategory.NO_CATEGORY), true);
        assertEquals(new SearchCategoriesResponseCount()
                .childrenCount(2)
                .offersCount(1)
                .categoryCount(1), checkResponse(ret));
    }

    @Test
    void searchCategoriesCountFilterExact0() {
        var ret = publicApi.searchCategoriesCountGet(List.of(SHOP_ID_N), null, null, singletonList(999L), null);
        assertEquals(new SearchCategoriesResponseCount(), checkResponse(ret));
    }

    @ParameterizedTest
    @EnumSource(AutostrategyTarget.class)
    void searchVendorsGet(AutostrategyTarget target) {
        var ret = publicApi.searchVendorsGet(List.of(SHOP_ID, SHOP_ID_2, UNKNOWN_SHOP), "test", null, null,
                target.name());
        checkResponse(ret);
    }

    @ParameterizedTest
    @EnumSource(AutostrategyTarget.class)
    void searchVendorsGetUnknownShop(AutostrategyTarget target) {
        var ret = publicApi.searchVendorsGet(List.of(UNKNOWN_SHOP), "test", null, null, target.name());
        checkResponse(ret);
    }

    @Test
    void searchVendorsGetWhiteAll() {
        var ret = checkResponse(publicApi.searchVendorsGet(List.of(SHOP_ID, SHOP_ID_2, UNKNOWN_SHOP),
                "v", null, null, null));
        assertEquals(List.of(vendor("v1"), vendor("v2"), vendor("v3")), ret);
    }

    @Test
    void searchVendorsGetWhiteExact() {
        var ret = checkResponse(publicApi.searchVendorsGet(List.of(SHOP_ID, SHOP_ID_2, UNKNOWN_SHOP),
                "v1", null, null, null));
        assertEquals(List.of(vendor("v1")), ret);
    }

    @Test
    void searchVendorsGetWhiteSingleShop() {
        var ret = checkResponse(publicApi.searchVendorsGet(List.of(SHOP_ID_2), "v", null, null, null));
        assertEquals(List.of(vendor("v3")), ret);
    }

    @Test
    void searchVendorsGetBlueAll() {
        var ret = checkResponse(publicApi.searchVendorsGet(List.of(SHOP_ID, SHOP_ID_2, UNKNOWN_SHOP),
                "bv", null, null, "blue"));
        assertEquals(List.of(vendor("bv1"), vendor("bv2"), vendor("bv3")), ret);
    }

    @Test
    void searchVendorsGetBlueExact() {
        var ret = checkResponse(publicApi.searchVendorsGet(List.of(SHOP_ID, SHOP_ID_2, UNKNOWN_SHOP),
                "bv1", null, null, "blue"));
        assertEquals(List.of(vendor("bv1")), ret);
    }

    @Test
    void searchVendorsGetBlueSingleShop() {
        var ret = checkResponse(publicApi.searchVendorsGet(List.of(SHOP_ID_2), "bv", null, null, "blue"));
        assertEquals(List.of(vendor("bv3")), ret);
    }

    @Test
    void searchStrategiesPost() {
        var ret = publicApi.searchStrategiesPost(SHOP_ID, null, null,
                filter(1, f -> f.setCategory("test")).toJsonString());
        checkResponse(ret);
    }

    @Test
    void searchStrategiesPostUnknownShop() {
        var ret = publicApi.searchStrategiesPost(UNKNOWN_SHOP, null, null,
                filter(1, f -> f.setCategory("test")).toJsonString());
        checkResponse(ret);
    }

    @Test
    void searchStrategiesPostFastPath() {
        var ret = publicApi.searchStrategiesPost(SHOP_ID, null, null, null);
        checkResponse(ret);
    }

    @Test
    void searchStrategiesPostFastPathUnknownShop() {
        var ret = publicApi.searchStrategiesPost(UNKNOWN_SHOP, null, null, null);
        checkResponse(ret);
    }

}
