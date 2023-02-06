package ru.yandex.market.partner.outlet;


import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Unit тесты для {@link ShowOutletInfosServantlet}.
 *
 * @author stani
 */
@DbUnitDataSet(before = "ShowOutletInfosServantletFunctionalTest.before.csv")
class ShowOutletInfosServantletFunctionalTest extends FunctionalTest {

    private static final String SHOP_OUTLETS = "/mvc/outlet/outlet_by_shop_leniest.json";
    private static final String SUPPLIER_OUTLETS = "/mvc/outlet/outlet_by_supplier_leniest.json";
    private static final String SUPPLIER_OUTLETS_WITH_LEGAL = "/mvc/outlet/outlet_by_supplier_with_legal_info_leniest.json";
    private static final String NO_DATA = "/mvc/outlet/outlet_no_data.json";

    @Test
    @DisplayName("Получить информацию по точкам магазина")
    void testShopGetShowOutletInfos() throws JSONException {
        final ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/showOutletInfos?id={campaignId}&format=json", 101L);
        checkResponse(response, SHOP_OUTLETS);
    }

    @Test
    @DisplayName("Получить информацию по точкам поставщика")
    void testSupplierGetShowOutletInfos() throws JSONException {
        final ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/showOutletInfos?id={campaignId}&format=json", 201L);
        checkResponse(response, SUPPLIER_OUTLETS);
    }

    @Test
    @DisplayName("Получить информацию по точкам с юридической информацией")
    void testShopGetShowOutletInfosWithLegalInfo() throws JSONException {
        final ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/showOutletInfos?id={campaignId}&format=json", 202L);
        checkResponse(response, SUPPLIER_OUTLETS_WITH_LEGAL);
    }

    /**
     * Для понимания, координаты на карте мира через 0 меридиан:
     * (-, +) | (+, +) - северное полушарие
     * (-, -) | (+, -) - южное полушарие
     * | - нулевой мередиан
     */
    @Test
    @DisplayName("Проверить фильтрацию по GPS координатам")
    void testGpsCoordinatesFilterPositive() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/showOutletInfos?id={campaignId}&format=json&topLeft={topLeft}&bottomRight={bottomRight}",
                101L, "-85.5,82.5", "160.5,-20.5"
        );
        checkResponse(response, SHOP_OUTLETS);
    }

    @Test
    @DisplayName("Проверить отсутствие данных при фильтрации по GPS координатам")
    void testGpsCoordinatesFilterNegative() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/showOutletInfos?id={campaignId}&format=json&topLeft={topLeft}&bottomRight={bottomRight}",
                101L, "25.5,26.5", "60.5,61.5"
        );
        checkResponse(response, NO_DATA);
    }

    @Test
    @DisplayName("Получить только алкогольные точки магазина")
    void testOnlyAlcoholOutlets() throws JSONException {
        final ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/showOutletInfos?id={campaignId}&hasAlcohol=true&format=json", 101L);
        checkResponse(response, NO_DATA);
    }

    @Test
    @DisplayName("Получить только не алкогольные точки магазина")
    void testNotAlcoholOutlets() throws JSONException {
        final ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/showOutletInfos?id={campaignId}&hasAlcohol=false&format=json", 101L);
        checkResponse(response, SHOP_OUTLETS);
    }

    private void checkResponse(final ResponseEntity<String> response, final String fileName) {
        final String data = StringTestUtil.getString(ShowOutletInfosServantletFunctionalTest.class, fileName);
        final String result = new JSONObject(response.getBody()).getJSONArray("result").toString();

        JSONAssert.assertEquals(data, result, JSONCompareMode.LENIENT);
    }

}
