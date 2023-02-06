package ru.yandex.market.pers.shopinfo.controller.supplier;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.pers.shopinfo.test.context.FunctionalTest;

import static ru.yandex.market.common.test.util.StringTestUtil.getString;

/**
 * @author stani on 12.02.18.
 * Функциональные тесты на {@link ru.yandex.market.pers.shopinfo.controller.SupplierInfoController}.
 */
@DbUnitDataSet(before = "suppliers.csv")
class SupplierInfoControllerTest extends FunctionalTest {

    private static String supplierInfo(String urlBasePrefix, String... supplierIds) {
        return FunctionalTestHelper.get(urlBasePrefix + "/supplierInfo?" + paramString(supplierIds)).getBody();
    }

    private static String supplierNames(String urlBasePrefix, String... supplierIds) {
        return FunctionalTestHelper.get(urlBasePrefix + "/supplierNames?" + paramString(supplierIds)).getBody();
    }

    private static String supplierNames(String urlBasePrefix) {
        return FunctionalTestHelper.get(urlBasePrefix + "/supplierNames").getBody();
    }

    private static String paramString(String[] supplierIds) {
        return Arrays.stream(supplierIds).map(id -> "supplier-id" + "=" + id).collect(Collectors.joining("&"));
    }

    @Test
    @DisplayName("GET /supplierInfo.")
    void testSupplierInfoSuccess() throws JSONException {
        String response = supplierInfo(urlBasePrefix, "774", "775", "777");
        String expected = getString(getClass(), "supplierInfoSuccess.json");
        JSONAssert.assertEquals(expected, response, JSONCompareMode.LENIENT);
    }

    @Test
    @DisplayName("GET /supplierInfo. Проверяет ситуацию, когда такого поставщика не существует.")
    void testSupplierInfoNotFound() throws JSONException {
        String response = supplierInfo(urlBasePrefix, "404");
        JsonTestUtil.assertEquals("[]", response);
    }

    @Test
    @DisplayName("GET /supplierNames.")
    void testSupplierNamesSuccess() throws JSONException {
        String response = supplierNames(urlBasePrefix, "774", "775");
        JsonTestUtil.assertEquals(
                // language=json
                "[\n" +
                        "  {\n" +
                        "    \"id\": 774,\n" +
                        "    \"name\": \"bus2\",\n" +
                        "    \"slug\": \"bus2\"\n" +
                        "  },\n" +
                        "  {\n" +
                        "    \"id\": 775,\n" +
                        "    \"name\": \"bus1\",\n" +
                        "    \"slug\": \"bus1\"\n" +
                        "  }\n" +
                        "]",
                response
        );
    }

    @DisplayName("GET /supplierNames. Проверяет ситуацию, когда supplier-id не указан")
    @Test
    void supplierNamesWrongId() {
        HttpServerErrorException e = Assertions.assertThrows(HttpServerErrorException.class,
                () -> supplierNames(urlBasePrefix));
        Assertions.assertEquals("{\"message\":\"wrong-supplier-id\"}", e.getResponseBodyAsString());
        Assertions.assertNotEquals(200, e.getRawStatusCode());
    }

}
