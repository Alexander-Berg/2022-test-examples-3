package ru.yandex.market.pers.shopinfo.controller.shopinfo;

import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.pers.shopinfo.test.context.FunctionalTest;

import static ru.yandex.market.common.test.util.StringTestUtil.getString;

/**
 * @author ogonek on 03.09.18.
 * Функциональные тесты на {@link ru.yandex.market.pers.shopinfo.controller.ShopInfoController}.
 */
@DbUnitDataSet(before = "shops.csv")
class ShopInfoControllerTest extends FunctionalTest {

    private static ResponseEntity<String> getShopInfoFull(String urlBasePrefix, String shopJurId, String... shopIds) {
        String url = urlBasePrefix + "/shopInfo?shop-jur-id={shopJurId}&shop-id={shopIds}";
        return FunctionalTestHelper.get(url, shopJurId, paramString(shopIds));
    }

    private static ResponseEntity<String> getShopInfoIds(String urlBasePrefix, String... shopIds) {
        return FunctionalTestHelper.get(urlBasePrefix + "/shopInfo?shop-id={shopIds}", paramString(shopIds));
    }

    private static ResponseEntity<String> getShopInfoJur(String urlBasePrefix, String shopJurId) {
        return FunctionalTestHelper.get(urlBasePrefix + "/shopInfo?shop-jur-id={shopJurId}", shopJurId);
    }

    private static ResponseEntity<String> getShopReturnAddress(String urlBasePrefix, String shopId) {
        return FunctionalTestHelper.get(urlBasePrefix + "/shop/{shopId}/returnAddress", shopId);
    }

    private static ResponseEntity<String> getShopSchedule(String urlBasePrefix, String... shopIds) {
        return FunctionalTestHelper.get(urlBasePrefix + "/shopSchedule?shop-id={shopIds}", paramString(shopIds));
    }

    private static ResponseEntity<String> getShopNames(String urlBasePrefix, String... shopIds) {
        return FunctionalTestHelper.get(urlBasePrefix + "/shopNames?shop-id={shopIds}", paramString(shopIds));
    }

    private static ResponseEntity<String> getShopNames(String urlBasePrefix) {
        return FunctionalTestHelper.get(urlBasePrefix + "/shopNames");
    }

    private static String paramString(String[] shopIds) {
        return String.join(",", shopIds);
    }

    @DisplayName(
            "GET /shopnames. " +
                    "Проверяет ситуации, когда название магазина есть в таблице Datasource, и когда его там нет."
    )
    @Test
    void shopNames() throws JSONException {
        ResponseEntity<String> response = getShopNames(urlBasePrefix, "774", "775", "776", "777");
        JSONAssert.assertEquals(
                // language=json
                "[\n" +
                        "  {\n" +
                        "    \"id\": 774,\n" +
                        "    \"name\": \"business1774\",\n" +
                        "    \"slug\": \"business1774\"\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"id\": 775,\n" +
                        "    \"name\": \"business1775\",\n" +
                        "    \"slug\": \"business1775\"\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"id\": 776,\n" +
                        "    \"name\": \"business1776\",\n" +
                        "    \"slug\": \"business1776\"\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"id\": 777,\n" +
                        "    \"name\": null,\n" +
                        "    \"slug\": null\n" +
                        "  }\n" +
                        "]",
                response.getBody(),
                JSONCompareMode.NON_EXTENSIBLE);
    }

    @DisplayName("GET /shopNames. Проверяет ситуацию, когда shop-id не указан")
    @Test
    void shopNamesWrongId() {
        HttpServerErrorException e = Assertions.assertThrows(HttpServerErrorException.class,
                () -> getShopNames(urlBasePrefix));
        Assertions.assertEquals("{\"message\":\"wrong-shop-id\"}", e.getResponseBodyAsString());
        Assertions.assertNotEquals(200, e.getRawStatusCode());
    }

    @DisplayName(
            "GET /shopSchedule. Проверяет ситуации, когда у магазина 0, 1 или несколько расписаний."
    )
    @Test
    void shopSchedule() throws JSONException {
        ResponseEntity<String> response = getShopSchedule(urlBasePrefix, "774", "775", "404");
        String expected = getString(getClass(), "shopScheduleSuccess.json");
        JSONAssert.assertEquals(expected, response.getBody(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @DisplayName(
            "GET /getShopReturnAddress."
    )
    @Test
    void shopReturnAddress() throws JSONException {
        ResponseEntity<String> response = getShopReturnAddress(urlBasePrefix, "774");
        JSONAssert.assertEquals(
                // language=json
                "{\n" +
                        "  \"value\": \"Ростов на дону ул. Победы 42\"\n" +
                        "}",
                response.getBody(),
                JSONCompareMode.NON_EXTENSIBLE);
    }

    @DisplayName(
            "GET /shopInfo. Проверяет работу shopInfo с параметрами shopJurId."
    )
    @Test
    void shopInfoJur() throws JSONException {
        ResponseEntity<String> response = getShopInfoJur(urlBasePrefix, "17743");
        String expected = getString(getClass(), "shopInfo774.json");
        JSONAssert.assertEquals(expected, response.getBody(), JSONCompareMode.NON_EXTENSIBLE);

        response = getShopInfoJur(urlBasePrefix, "1775");
        expected = getString(getClass(), "shopInfo775.json");
        JSONAssert.assertEquals(expected, response.getBody(), JSONCompareMode.NON_EXTENSIBLE);

        response = getShopInfoJur(urlBasePrefix, "1776");
        Assertions.assertEquals("[]", response.getBody());
    }

    @DisplayName(
            "GET /shopInfo. Проверяет работу shopInfo с параметрами shopIds."
    )
    @Test
    void shopInfoIds() throws JSONException {
        ResponseEntity<String> response = getShopInfoIds(urlBasePrefix, "774");
        String expected = getString(getClass(), "shopInfo774.json");
        JSONAssert.assertEquals(expected, response.getBody(), JSONCompareMode.NON_EXTENSIBLE);

        response = getShopInfoIds(urlBasePrefix, "775", "776");
        expected = getString(getClass(), "shopInfo775.json");
        JSONAssert.assertEquals(expected, response.getBody(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @DisplayName(
            "GET /shopInfo. Проверяет работу shopInfo с параметрами shopJurId и shopIds."
    )
    @Test
    void shopInfoFull() throws JSONException {
        ResponseEntity<String> response = getShopInfoFull(urlBasePrefix, "17742", "775", "776");
        String expected = getString(getClass(), "shopInfo774_history.json");
        JSONAssert.assertEquals(expected, response.getBody(), JSONCompareMode.NON_EXTENSIBLE);
    }

}
