package ru.yandex.market.vendors.analytics.platform.controller.sales.growth.model;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.calculate.growth.request.model.TopModelsGrowthRequest;
import ru.yandex.market.vendors.analytics.core.model.dto.common.LanguageDTO;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.controller.sales.CalculateFunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.sales.growth.GrowthDriversController;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Functional tests for {@link GrowthDriversController#topModelsGrowth(TopModelsGrowthRequest, long, Long, LanguageDTO)}
 *
 * @author antipov93.
 */
@DbUnitDataSet(before = "TopModelsGrowthTest.before.csv")
@ClickhouseDbUnitDataSet(before = {"TopModelsGrowthTest.ch.before.csv", "TopModelsGrowth.csv"})
public class TopModelsGrowthTest extends CalculateFunctionalTest {

    private static final long DASHBOARD_ID = 100;
    private static final String TOP_MODELS_GROWTH_PATH = "/growthDrivers/topModels";

    @Test
    @DisplayName("Рост топовых моделей в категории")
    void topModelsGrowth() {
        String expected = loadFromFile("TopModelsGrowthTest.response.json");

        String body =
                //language=json
                "{\n"
                        + "  \"categoryFilter\": {\n"
                        + "    \"hid\": 91491\n"
                        + "  },\n"
                        + "  \"interval\": {\n"
                        + "    \"startDate\": \"2019-02-01\",\n"
                        + "    \"endDate\": \"2019-02-28\"\n"
                        + "  },\n"
                        + "  \"timeDetailing\": \"MONTH\",\n"
                        + "  \"sortBy\": \"MONEY\",\n"
                        + "  \"selectionInfo\": {\n"
                        + "    \"limit\": 10,\n"
                        + "    \"strategy\": \"MONEY\"\n"
                        + "  },\n"
                        + "  \"visualization\": \"BUBBLE\",\n"
                        + "  \"measure\": \"MONEY\"\n"
                        + "}";
        String actual = getTopModelsGrowth(body);
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Рост топовых моделей в категории с новыми скрытиями")
    @DbUnitDataSet(before = "TopModelsGrowthNewHidings.before.csv")
    @ClickhouseDbUnitDataSet(before = {"TopModelsGrowthNewHidings.ch.before.csv"})
    void topModelsGrowthNewHidings() {
        String expected = loadFromFile("TopModelGrowthTestNewHidings.response.json");

        String body =
                //language=json
                "{\n"
                        + "  \"categoryFilter\": {\n"
                        + "    \"hid\": 91491\n"
                        + "  },\n"
                        + "  \"interval\": {\n"
                        + "    \"startDate\": \"2019-02-01\",\n"
                        + "    \"endDate\": \"2019-02-28\"\n"
                        + "  },\n"
                        + "  \"timeDetailing\": \"MONTH\",\n"
                        + "  \"sortBy\": \"MONEY\",\n"
                        + "  \"selectionInfo\": {\n"
                        + "    \"limit\": 10,\n"
                        + "    \"strategy\": \"MONEY\"\n"
                        + "  },\n"
                        + "  \"visualization\": \"BUBBLE\",\n"
                        + "  \"measure\": \"MONEY\"\n"
                        + "}";
        String actual = getTopModelsGrowth(body);
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Рост топовых моделей в категории, нет данных")
    void topModelsGrowthNoSales() {
        String body =
                "{\n"
                        + "  \"categoryFilter\": {\n"
                        + "    \"hid\": 91491\n"
                        + "  },\n"
                        + "  \"interval\": {\n"
                        + "    \"startDate\": \"2019-03-01\",\n"
                        + "    \"endDate\": \"2019-03-31\"\n"
                        + "  },\n"
                        + "  \"timeDetailing\": \"MONTH\",\n"
                        + "  \"sortBy\": \"MONEY\",\n"
                        + "  \"selectionInfo\": {\n"
                        + "    \"limit\": 10,\n"
                        + "    \"strategy\": \"MONEY\"\n"
                        + "  },\n"
                        + "  \"visualization\": \"BUBBLE\",\n"
                        + "  \"measure\": \"MONEY\"\n"
                        + "}";
        HttpClientErrorException clientException = assertThrows(
                HttpClientErrorException.class,
                () -> getTopModelsGrowth(body)
        );

        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                clientException.getStatusCode()
        );
        JsonTestUtil.assertEquals(
                "{\"code\":\"NO_DATA\",\"message\":\"There are no sales for this periods\"}",
                clientException.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Неподдерживаемый таймдетейлинг")
    void badTimeDetailing() {
        String body =
                //language=json
                "{\n"
                        + "  \"categoryFilter\": {\n"
                        + "    \"hid\": 91491\n"
                        + "  },\n"
                        + "  \"interval\": {\n"
                        + "    \"startDate\": \"2019-02-01\",\n"
                        + "    \"endDate\": \"2019-02-28\"\n"
                        + "  },\n"
                        + "  \"timeDetailing\": \"NONE\",\n"
                        + "  \"sortBy\": \"MONEY\",\n"
                        + "  \"selectionInfo\": {\n"
                        + "    \"limit\": 10,\n"
                        + "    \"strategy\": \"MONEY\"\n"
                        + "  },\n"
                        + "  \"visualization\": \"BUBBLE\",\n"
                        + "  \"measure\": \"MONEY\"\n"
                        + "}";
        assertThrows(
                HttpClientErrorException.class,
                () -> getTopModelsGrowth(body),
                "400 Bad Request"
        );
    }


    private String getTopModelsGrowth(String body) {
        return FunctionalTestHelper.postForJson(getFullWidgetUrl(TOP_MODELS_GROWTH_PATH, DASHBOARD_ID), body);
    }
}
