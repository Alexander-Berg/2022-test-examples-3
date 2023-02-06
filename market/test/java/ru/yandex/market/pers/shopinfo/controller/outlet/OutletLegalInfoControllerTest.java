package ru.yandex.market.pers.shopinfo.controller.outlet;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.pers.shopinfo.test.context.FunctionalTest;

/**
 * @author stani on 13.09.18.
 */
@DbUnitDataSet(before = "outletLegalInfo.csv")
class OutletLegalInfoControllerTest extends FunctionalTest {

    private static ResponseEntity<String> outletLegalInfo(String urlBasePrefix, String... outletIds) {
        return FunctionalTestHelper.get(urlBasePrefix + "/outletLegalInfo?" + paramString(outletIds));
    }

    private static String paramString(String[] outletIds) {
        return Arrays.stream(outletIds).map(id -> "outlet-id" + "=" + id).collect(Collectors.joining("&"));
    }

    @Test
    @DisplayName("GET /outletLegalInfo")
    void testSupplierInfoSuccess() throws JSONException {
        final ResponseEntity<String> response = outletLegalInfo(urlBasePrefix, "101", "102");
        JSONAssert.assertEquals("[\n" +
                "  {\n" +
                "    \"outletId\": \"101\",\n" +
                "    \"organizationName\": \"chto_ugodno_retail_group\",\n" +
                "    \"organizationType\": \"1\",\n" +
                "    \"registrationNumber\": \"5077746887312\",\n" +
                "    \"juridicalAddress\": \"Kemerovo ul Lenina 1\",\n" +
                "    \"factAddress\": \"Kemerovo ul Lenina 2\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"outletId\": \"102\",\n" +
                "    \"organizationName\": \"x6_retail_group\",\n" +
                "    \"organizationType\": \"1\",\n" +
                "    \"registrationNumber\": \"5077746887312\",\n" +
                "    \"juridicalAddress\": \"Kemerovo ul Ilona Maska 14\",\n" +
                "    \"factAddress\": \"Kemerovo ul Eskobara 21\"\n" +
                "  }\n" +
                "]", response.getBody(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DisplayName("GET /outletLegalInfo. Юридической информации точки продаж не существует.")
    void testSupplierInfoNotFound() throws JSONException {
        final ResponseEntity<String> response = outletLegalInfo(urlBasePrefix, "404");
        JSONAssert.assertEquals("[]", response.getBody(), JSONCompareMode.NON_EXTENSIBLE);
    }
}
