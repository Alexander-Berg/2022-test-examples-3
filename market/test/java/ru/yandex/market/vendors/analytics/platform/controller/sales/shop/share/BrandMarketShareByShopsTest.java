package ru.yandex.market.vendors.analytics.platform.controller.sales.shop.share;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.calculate.assortment.request.share.BrandMarketShareByShopsCategoryRequest;
import ru.yandex.market.vendors.analytics.core.calculate.assortment.request.share.BrandMarketShareByShopsRegionRequest;
import ru.yandex.market.vendors.analytics.core.dao.clickhouse.sales.shops.ShopSalesDAO;
import ru.yandex.market.vendors.analytics.core.model.common.GeoFilters;
import ru.yandex.market.vendors.analytics.core.model.common.StartEndDate;
import ru.yandex.market.vendors.analytics.core.model.common.socdem.SocdemFilter;
import ru.yandex.market.vendors.analytics.core.model.dto.common.LanguageDTO;
import ru.yandex.market.vendors.analytics.core.model.sales.common.CategoryPriceSegmentsFilter;
import ru.yandex.market.vendors.analytics.core.model.sales.shops.share.RawMarketShare;
import ru.yandex.market.vendors.analytics.core.service.sales.common.DBDatesInterval;
import ru.yandex.market.vendors.analytics.core.service.strategies.TimeDetailing;
import ru.yandex.market.vendors.analytics.platform.controller.sales.CalculateFunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.sales.shop.ShopsAssortmentController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static ru.yandex.market.vendors.analytics.core.model.enums.RegionType.COUNTRY;
import static ru.yandex.market.vendors.analytics.core.service.SalesTestUtils.createRawMarketShare;
import static ru.yandex.market.vendors.analytics.core.service.sales.shop.ShopAssortmentTestUtils.createCategoryPriceSegmentsFilter;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "MarketShareByShopsTest.before.csv")
public class BrandMarketShareByShopsTest extends CalculateFunctionalTest {

    private static final String BASE_PATH = "/shops/assortment/share/brand";
    private static final String BY_REGION_PATH = BASE_PATH + "/byRegions";
    private static final String BY_CATEGORY_PATH = BASE_PATH + "/byCategories";

    @Autowired
    private ShopSalesDAO shopSalesDAO;

    /**
     * Functional tests for {@link ShopsAssortmentController#brandMarketShareByRegion(
     *BrandMarketShareByShopsRegionRequest, LanguageDTO)}
     */
    @Test
    @DisplayName("Группировка по всей России")
    void brandMarketShareByCountry() {
        mockClickhouseServices(List.of(
                createRawMarketShare(556, 1000, "2019-01-01", 225),
                createRawMarketShare(900, 2000, "2019-02-01", 225)
        ));

        String expected = loadFromFile("BrandRegionMarketShareByShops.response.json");

        String body = "{\n"
                + "  \"brandId\": 153043,\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-01-01\",\n"
                + "    \"endDate\": \"2019-02-28\"\n"
                + "  },\n"
                + "  \"regionType\": \"COUNTRY\",\n"
                + "  \"shareType\": \"PERCENT\",\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"categoriesFilter\": [\n"
                + "    {\n"
                + "      \"hid\": 91491,\n"
                + "      \"priceSegments\": [8]\n"
                + "    },\n"
                + "    {\n"
                + "      \"hid\": 91013\n"
                + "    }\n"
                + "  ],\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        String actual = getBrandShopsShareByRegion(body);
        JsonAssert.assertJsonEquals(expected, actual);
    }

    /**
     * Functional tests for {@link ShopsAssortmentController#brandMarketShareByCategory(
     *BrandMarketShareByShopsCategoryRequest, LanguageDTO)}
     */
    @Test
    @DisplayName("Группировка по категориям")
    void brandMarketShareByCategory() {
        mockClickhouseServices(List.of(
                createRawMarketShare(600, 1000, "2019-01-01", 91491),
                createRawMarketShare(150, 1000, "2019-01-01", 91013),

                createRawMarketShare(550, 1100, "2019-02-01", 91491)
        ));

        String expected = loadFromFile("BrandCategoryMarketShareByShops.response.json");

        String body = "{\n"
                + "  \"brandId\": 153043,\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-01-01\",\n"
                + "    \"endDate\": \"2019-02-28\"\n"
                + "  },\n"
                + "  \"shareType\": \"PERCENT\",\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"categoriesFilter\": [\n"
                + "    {\n"
                + "      \"hid\": 91491,\n"
                + "      \"priceSegments\": [8]\n"
                + "    },\n"
                + "    {\n"
                + "      \"hid\": 91013\n"
                + "    }\n"
                + "  ],\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        String actual = getBrandShopsShareByCategory(body);
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Неизвестный бренд")
    void unknownBrand() {
        String body = "{\n"
                + "  \"brandId\": 1,\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-01-01\",\n"
                + "    \"endDate\": \"2019-02-28\"\n"
                + "  },\n"
                + "  \"regionType\": \"COUNTRY\",\n"
                + "  \"shareType\": \"PERCENT\",\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"categoriesFilter\": [\n"
                + "    {\n"
                + "      \"hid\": 91491,\n"
                + "      \"priceSegments\": [8]\n"
                + "    }],\n"
                + "  \"visualization\": \"PIE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";

        HttpClientErrorException clientException = assertThrows(
                HttpClientErrorException.class,
                () -> getBrandShopsShareByRegion(body)
        );
        assertEquals(HttpStatus.NOT_FOUND, clientException.getStatusCode());
        JsonAssert.assertJsonEquals("{\n" +
                        "  \"code\" : \"ENTITY_NOT_FOUND\",\n" +
                        "  \"message\": \"${json-unit.ignore}\",\n" +
                        "   \"entityId\": 1,\n" +
                        "  \"entityType\": \"BRAND\"\n" +
                        "}",
                clientException.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Не указаны категории")
    void unknownCategories() {
        String body =
                //language=json
                "{\n" +
                        "  \"brandId\": 153043,\n" +
                        "  \"interval\": {\n" +
                        "    \"startDate\": \"2019-01-01\",\n" +
                        "    \"endDate\": \"2019-02-28\"\n" +
                        "  },\n" +
                        "  \"regionType\": \"COUNTRY\",\n" +
                        "  \"shareType\": \"PERCENT\",\n" +
                        "  \"timeDetailing\": \"MONTH\"\n," +
                        "  \"categoriesFilter\": []" +
                        "}";

        HttpClientErrorException clientException = assertThrows(
                HttpClientErrorException.class,
                () -> getBrandShopsShareByRegion(body)
        );
        assertEquals(HttpStatus.BAD_REQUEST, clientException.getStatusCode());
        assertTrue(clientException.getResponseBodyAsString().contains("categories should not be empty"));
    }

    @Test
    @DisplayName("Нет продаж товаров бренда в указанных категориях")
    void brandHasNoSalesInCategories() {
        String body =
                //language=json
                "{\n" +
                        "  \"brandId\": 153043,\n" +
                        "  \"interval\": {\n" +
                        "    \"startDate\": \"2019-01-01\",\n" +
                        "    \"endDate\": \"2019-02-28\"\n" +
                        "  },\n" +
                        "  \"shareType\": \"PERCENT\",\n" +
                        "  \"timeDetailing\": \"MONTH\",\n" +
                        "  \"categoriesFilter\": [\n" +
                        "    {\n" +
                        "      \"hid\": 88888\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";

        HttpClientErrorException clientException = assertThrows(
                HttpClientErrorException.class,
                () -> getBrandShopsShareByRegion(body)
        );
        assertEquals(HttpStatus.BAD_REQUEST, clientException.getStatusCode());
    }

    @Test
    @DisplayName("Нельзя передавать более 1 категории и репорт фильтры одновременно")
    void errorMultiCategoriesAndReportFilters() {
        String body = "{\n"
                + "  \"brandId\": 153043,\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-01-01\",\n"
                + "    \"endDate\": \"2019-02-28\"\n"
                + "  },\n"
                + "  \"shareType\": \"PERCENT\",\n"
                + "  \"timeDetailing\": \"MONTH\",\n"
                + "  \"categoriesFilter\": [\n"
                + "    {\n"
                + "      \"hid\": 91491,\n"
                + "      \"priceSegments\": [8]\n"
                + "    },\n"
                + "    {\n"
                + "      \"hid\": 91013\n"
                + "    }\n"
                + "  ],\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\",\n"
                + "  \"reportFilters\": {\n"
                + "    \"glFilters\": {\n"
                + "      \"201\": [1, 2],\n"
                + "      \"202\": [100]\n"
                + "    }\n"
                + "  }\n"
                + "}";

        HttpClientErrorException clientException = assertThrows(
                HttpClientErrorException.class,
                () -> getBrandShopsShareByRegion(body)
        );
        assertEquals(HttpStatus.BAD_REQUEST, clientException.getStatusCode());
    }

    private void mockClickhouseServices(List<RawMarketShare> rawShares) {
        long brandId = 153043L;
        StartEndDate interval = new StartEndDate("2019-01-01", "2019-02-28");

        reset(shopSalesDAO);

        DBDatesInterval dbInterval = new DBDatesInterval(interval, TimeDetailing.MONTH);
        GeoFilters geoFilters = GeoFilters.empty();
        SocdemFilter socdemFilter = SocdemFilter.empty();
        Set<CategoryPriceSegmentsFilter> categoriesFilter = Set.of(
                createCategoryPriceSegmentsFilter(91491, Set.of(8)),
                createCategoryPriceSegmentsFilter(91013, Collections.emptySet())
        );
        Set<Long> modelIds = Collections.emptySet();

        when(shopSalesDAO.loadBrandRawMarketSharesByRegion(
                eq(brandId),
                eq(modelIds),
                eq(COUNTRY),
                any(),
                eq(geoFilters),
                eq(categoriesFilter),
                eq(socdemFilter)
        )).thenReturn(rawShares);

        when(shopSalesDAO.loadBrandRawMarketSharesByCategory(
                eq(brandId),
                eq(modelIds),
                eq(dbInterval),
                eq(geoFilters),
                eq(categoriesFilter),
                eq(socdemFilter)
        )).thenReturn(rawShares);
    }

    private String getBrandShopsShareByRegion(String body) {
        return FunctionalTestHelper.postForJson(getFullWidgetUrl(BY_REGION_PATH), body);
    }

    private String getBrandShopsShareByCategory(String body) {
        return FunctionalTestHelper.postForJson(getFullWidgetUrl(BY_CATEGORY_PATH), body);
    }
}
