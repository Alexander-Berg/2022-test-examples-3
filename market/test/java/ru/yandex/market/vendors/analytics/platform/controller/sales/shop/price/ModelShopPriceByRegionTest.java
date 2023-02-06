package ru.yandex.market.vendors.analytics.platform.controller.sales.shop.price;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.calculate.assortment.request.price.ModelShopsPricesByRegionRequest;
import ru.yandex.market.vendors.analytics.core.dao.clickhouse.sales.shops.ShopSalesDAO;
import ru.yandex.market.vendors.analytics.core.model.common.socdem.SocdemFilter;
import ru.yandex.market.vendors.analytics.core.model.dto.common.LanguageDTO;
import ru.yandex.market.vendors.analytics.core.model.enums.RegionType;
import ru.yandex.market.vendors.analytics.platform.controller.sales.CalculateFunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.sales.shop.ShopsAssortmentController;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static ru.yandex.market.vendors.analytics.core.service.sales.shop.ShopAssortmentTestUtils.createRawSales;

/**
 * Functional test for {@link ShopsAssortmentController#modelPricesByRegion(
 * ModelShopsPricesByRegionRequest, LanguageDTO)}
 *
 * @author antipov93.
 */
@DbUnitDataSet(before = "ModelShopPriceByRegionTest.before.csv")
public class ModelShopPriceByRegionTest extends CalculateFunctionalTest {

    private static final String MODEL_SHOP_PRICE_PATH = "/shops/assortment/modelsPricesByRegion";

    @Autowired
    private ShopSalesDAO shopSalesDAO;

    @Test
    @DisplayName("Группировка по дням")
    void dailyModelShopsPriceByFederalDistrict() {
        reset(shopSalesDAO);

        when(shopSalesDAO.loadRegionsShopsSales(
                eq(10L),
                eq(91491L),
                any(RegionType.class),
                any(),
                eq(SocdemFilter.empty())
        )).thenReturn(List.of(
                // 2019-01-01
                createRawSales("apple.com", 1, "2019-01-01", 200, 2),
                createRawSales("beru.ru", 1, "2019-01-01", 100, 10),
                createRawSales("pleer.ru", 1, "2019-01-01", 20, 20),

                createRawSales("beru.ru", 2, "2019-01-01", 96, 12),
                createRawSales("pleer.ru", 2, "2019-01-01", 5, 5),

                createRawSales("pleer.ru", 3, "2019-01-01", 100, 100),

                createRawSales("beru.ru", 4, "2019-01-01", 200, 20),

                createRawSales("pleer.ru", 5, "2019-01-01", 20, 500),

                // 2019-01-02
                createRawSales("beru.ru", 1, "2019-01-02", 121, 11),
                createRawSales("apple.com", 1, "2019-01-02", 100, 1)
        ));

        String expected = loadFromFile("ModelShopPriceByRegionDailyTest.response.json");

        String body = ""
                + "{\n"
                + "  \"modelId\": 10,\n"
                + "  \"hid\": 91491,\n"
                + "  \"regionType\": \"FEDERAL_DISTRICT\",\n"
                + "  \"topSelectionStrategy\": \"MONEY\",\n"
                + "  \"topRegionsCount\": 3,\n"
                + "  \"interestingRegions\": [3, 4],\n"
                + "  \"topShopsCount\": 2,\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-01-01\",\n"
                + "    \"endDate\": \"2019-01-02\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"DAY\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";
        String actual = getModelShopsPrices(body);
        JsonTestUtil.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Неправильный интервал: конечная дата раньше начальной")
    void invalidInterval() {
        String body = ""
                + "{\n"
                + "  \"modelId\": 10,"
                + "  \"hid\": 91491,\n"
                + "  \"regionType\": \"FEDERAL_DISTRICT\",\n"
                + "  \"topSelectionStrategy\": \"MONEY\",\n"
                + "  \"topRegionsCount\": 3,\n"
                + "  \"interestingRegions\": [3, 4],\n"
                + "  \"topShopsCount\": 2,\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-01-02\",\n"
                + "    \"endDate\": \"2019-01-01\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"DAY\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";

        HttpClientErrorException clientException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getModelShopsPrices(body)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                clientException.getStatusCode()
        );

        String expected = ""
                + "{\n"
                + "   \"message\" : \"startDate should be before endDate\",\n"
                + "   \"code\" : \"BAD_REQUEST\"\n"
                + "}";
        JsonTestUtil.assertEquals(expected, clientException.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Неизвестный регион")
    void invalidRegions() {
        String body = ""
                + "{\n"
                + "  \"modelId\": 10,"
                + "  \"hid\": 91491,\n"
                + "  \"regionType\": \"FEDERAL_DISTRICT\",\n"
                + "  \"topSelectionStrategy\": \"MONEY\",\n"
                + "  \"topRegionsCount\": 3,\n"
                + "  \"interestingRegions\": [6, 7],\n"
                + "  \"topShopsCount\": 2,\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-01-01\",\n"
                + "    \"endDate\": \"2019-01-02\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"DAY\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";

        HttpClientErrorException clientException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getModelShopsPrices(body)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                clientException.getStatusCode()
        );
        String expected =
                //language=json
                "{\n" +
                        "   \"message\" : \"2 unknown interesting regions\",\n" +
                        "   \"code\" : \"BAD_REQUEST\"\n" +
                        "}";
        JsonTestUtil.assertEquals(expected, clientException.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Неправильный тип региона")
    void invalidRegionType() {
        String body = ""
                + "{\n"
                + "  \"modelId\": 10,"
                + "  \"hid\": 91491,\n"
                + "  \"regionType\": \"FEDERAL_SUBJECT\",\n"
                + "  \"topSelectionStrategy\": \"MONEY\",\n"
                + "  \"topRegionsCount\": 3,\n"
                + "  \"interestingRegions\": [1],\n"
                + "  \"topShopsCount\": 2,\n"
                + "  \"interval\": {\n"
                + "    \"startDate\": \"2019-01-01\",\n"
                + "    \"endDate\": \"2019-01-02\"\n"
                + "  },\n"
                + "  \"timeDetailing\": \"DAY\",\n"
                + "  \"visualization\": \"LINE\",\n"
                + "  \"measure\": \"MONEY\"\n"
                + "}";

        HttpClientErrorException clientException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getModelShopsPrices(body)
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                clientException.getStatusCode()
        );
        String expected = ""
                + "{\n"
                + "   \"message\" : \"Not all interesting regions have type FEDERAL_SUBJECT\",\n"
                + "   \"code\" : \"BAD_REQUEST\"\n"
                + "}";
        JsonTestUtil.assertEquals(expected, clientException.getResponseBodyAsString());
    }

    private String getModelShopsPrices(String body) {
        return FunctionalTestHelper.postForJson(getFullWidgetUrl(MODEL_SHOP_PRICE_PATH), body);
    }
}
