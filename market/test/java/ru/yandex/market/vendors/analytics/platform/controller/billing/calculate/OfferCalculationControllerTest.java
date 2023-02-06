package ru.yandex.market.vendors.analytics.platform.controller.billing.calculate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "OfferCalculationControllerTest.before.csv")
class OfferCalculationControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Одна категория без заморозки")
    @DbUnitDataSet(before = "../OfferClock.2020-04-10.before.csv")
    void oneCategoryWithoutFreeze() {
        String request = ""
                + "{\n"
                + "  \"categories\": [\n"
                + "    {\n"
                + "      \"hid\": 91491,\n"
                + "      \"tariffType\": \"BASIC\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        String response = calculateAdd(request);
        String expected = loadFromFile("OfferCalculationControllerTest.oneCategoryWithoutFreeze.response.json");
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Одна категория без заморозки, новая логика")
    @DbUnitDataSet(before = "../OfferClock.2020-04-10.new.before.csv")
    void oneCategoryWithoutFreezeNew() {
        String request = ""
                + "{\n"
                + "  \"categories\": [\n"
                + "    {\n"
                + "      \"hid\": 91491,\n"
                + "      \"tariffType\": \"BASIC\",\n"
                + "      \"monthCount\": 6\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        String response = calculateAdd(request);
        String expected = loadFromFile("OfferCalculationControllerTest.new.response.json");
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Три категории со фризом")
    @DbUnitDataSet(before = "../OfferClock.2020-04-11.before.csv")
    void threeCategoriesWithFreeze() {
        String request = ""
                + "{\n"
                + "  \"categories\": [\n"
                + "    {\n"
                + "      \"hid\": 91491, \"tariffType\": \"BASIC\"\n"
                + "    },\n"
                + "    {\n"
                + "       \"hid\":91011, \"tariffType\": \"FULL\"\n"
                + "    },\n"
                + "    {\n"
                + "       \"hid\":6290268, \"tariffType\": \"FULL\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        String response = calculateAdd(request);
        String expected = loadFromFile("OfferCalculationControllerTest.threeCategoriesWithFreeze.response.json");
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Запрос с категориями, для которых не задана цена")
    void unknownCategoryPrice() {
        String request = ""
                + "{\n"
                + "  \"categories\": [\n"
                + "    {\n"
                + "      \"hid\": 91491, \"tariffType\": \"BASIC\"\n"
                + "    },\n"
                + "    {\n"
                + "       \"hid\":94334, \"tariffType\": \"FULL\"\n"
                + "    },\n"
                + "    {\n"
                + "       \"hid\":10001, \"tariffType\": \"FULL\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        var exception = assertThrows(HttpClientErrorException.class, () -> calculateAdd(request));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        var expected = "{  \n" +
                "   \"code\":\"ENTITY_NOT_FOUND\",\n" +
                "   \"message\":\"${json-unit.ignore}\",\n" +
                "   \"entityId\": 10001,\n" +
                "   \"entityType\":\"CATEGORY_PRICE\"\n" +
                "}";
        assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Расчёт суммарной стоимости категорий за несколько месяцев")
    void costForMonth() {
        String request = ""
                + "{\n"
                + "  \"categories\": [\n"
                + "    {\n"
                + "      \"hid\": 91491, \"tariffType\": \"BASIC\"\n"
                + "    },\n"
                + "    {\n"
                + "       \"hid\":91011, \"tariffType\": \"FULL\"\n"
                + "    },\n"
                + "    {\n"
                + "       \"hid\":6290268, \"tariffType\": \"FULL\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        String response = calculateAddForMonth(request, 3);
        String expected = "3618000";
        assertJsonEquals(expected, response);
    }

    @Test
    @DbUnitDataSet(before = "../OfferClock.2020-04-10.new.before.csv")
    @DisplayName("Новый расчёт суммарной стоимости категорий за несколько месяцев")
    void costForMonthNew() {
        String request = ""
                + "{\n"
                + "  \"categories\": [\n"
                + "    {\n"
                + "      \"hid\": 91491, \"tariffType\": \"BASIC\", \"monthCount\": 3\n"
                + "    },\n"
                + "    {\n"
                + "       \"hid\":91011, \"tariffType\": \"FULL\"\n"
                + "    },\n"
                + "    {\n"
                + "       \"hid\":6290268, \"tariffType\": \"FULL\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        String response = calculateAddPrice(request);
        String expected = "2500000";
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Запрос с неизвестной категорией")
    void unknownCategory() {
        String request = ""
                + "{\n"
                + "  \"categories\": [\n"
                + "    {\n"
                + "      \"hid\": 49013, \"tariffType\": \"BASIC\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        var exception = assertThrows(HttpClientErrorException.class, () -> calculateAdd(request));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        var expected = ""
                + "{\n"
                + "     \"code\":\"ENTITY_NOT_FOUND\",\n"
                + "     \"message\": \"${json-unit.ignore}\","
                + "     \"entityId\": 49013,\n"
                + "     \"entityType\": \"CATEGORY\"\n"
                + "}";
        assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Апгрейд до полного в середине месяца")
    @DbUnitDataSet(before = "../OfferClock.2020-04-11.before.csv")
    void upgradeWithFreeze() {
        String response = calculateUpgrade(91122);
        String expected = loadFromFile("OfferCalculationControllerTest.upgradeWithFreeze.response.json");
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Апгрейд до полного в начале месяца")
    @DbUnitDataSet(before = "../OfferClock.2020-04-10.before.csv")
    void upgradeWithoutFreeze() {
        String response = calculateUpgrade(91491);
        String expected = loadFromFile("OfferCalculationControllerTest.upgradeWithoutFreeze.response.json");
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Запрос на апгрейд категории, для которой не задана цена")
    void upgradeUnknownCategory() {
        var exception = assertThrows(HttpClientErrorException.class, () -> calculateUpgrade(32032));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        var expected = "{  \n" +
                "   \"code\":\"ENTITY_NOT_FOUND\",\n" +
                "   \"message\":\"${json-unit.ignore}\",\n" +
                "   \"entityId\": 32032,\n" +
                "   \"entityType\":\"CATEGORY_PRICE\"\n" +
                "}";
        assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    private String calculateAdd(String body) {
        return FunctionalTestHelper.postForJson(baseUrl() + "/offer/calculate", body);
    }

    private String calculateAddPrice(String body) {
        return FunctionalTestHelper.postForJson(baseUrl() + "/offer/calculate/add/price", body);
    }

    private String calculateAddForMonth(String body, long monthCount) {
        return FunctionalTestHelper
                .postForJson(baseUrl() + "/offer/calculate/add/months?monthCount=" + monthCount, body);
    }

    private String calculateUpgrade(long categoryId) {
        return FunctionalTestHelper.get(baseUrl() + "/offer/calculate/upgrade/" + categoryId).getBody();
    }
}