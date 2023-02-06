package ru.yandex.market.partner.mvc.controller.fulfillment;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;
import static ru.yandex.market.partner.util.FunctionalTestHelper.get;

/**
 * Функциональные тесты для {@link FulfillmentServiceController}.
 *
 * @author avetokhin 22.08.18.
 */
@DbUnitDataSet(before = "FulfillmentServiceControllerTest.before.csv")
class FulfillmentServiceControllerTest extends FunctionalTest {

    private static final int CAMPAIGN_ID_1 = 1;
    private static final int CAMPAIGN_ID_2 = 2;
    private static final int CAMPAIGN_ID_3 = 3;
    private static final int CAMPAIGN_ID_4 = 4;

    @Test
    void getAll() {
        final ResponseEntity<String> response = get(baseUrl + "/fulfillment/services");
        assertResponseWithFile(response, "FulfillmentServiceControllerTest.allServices.json");
    }

    @Test
    void getAvailable() {
        testAvailable(CAMPAIGN_ID_1, "FulfillmentServiceControllerTest.available1.json");
        testAvailable(CAMPAIGN_ID_2, "FulfillmentServiceControllerTest.available2.json");
        testAvailable(CAMPAIGN_ID_3, "FulfillmentServiceControllerTest.available3.json");
        testAvailable(CAMPAIGN_ID_4, "FulfillmentServiceControllerTest.available3.json");
    }

    @DisplayName("Без фильтра по типу сервиса должны находиться все FULFILLMENT склады")
    @Test
    void getServiceWithoutTypeFilter() {
        ResponseEntity<String> response = get(baseUrl + "/fulfillment/services/available?id=5");
        assertResponseWithFile(response, "FulfillmentServiceControllerTest.available5.json");
    }

    @DisplayName("Фильтр по типу сервиса")
    @Test
    void getServiceWithTypeFilter() {
        ResponseEntity<String> response =
                get(baseUrl + "/fulfillment/services/available?id=5&serviceTypes=CROSSDOCK");

        assertResponse(response, "[{\"id\":505,\"name\":\"Unnamed\",\"serviceType\":\"crossdock\"}]");
    }

    @DisplayName("Фильтр по типу сервиса dropship by seller")
    @Test
    void getServiceWithTypeFilterDSBS() {
        ResponseEntity<String> response =
                get(baseUrl + "/fulfillment/services/available?id=16&serviceTypes=DROPSHIP_BY_SELLER");

        assertResponse(response, "[{\"id\":1000,\"name\":\"Unnamed\",\"serviceType\":\"dropship_by_seller\"}]");
    }

    @DisplayName("Пустой список при отсутствии у поставщика такого типа склада")
    @Test
    void getEmptyForPartnerWithoutFilteredType() {
        ResponseEntity<String> response =
                get(baseUrl + "/fulfillment/services/available?id=5&serviceTypes=DROPSHIP");

        assertResponse(response, "[]");
    }

    @DisplayName("Поиск складов по нескольким фильтрам типов")
    @Test
    void getMultiplyServicesWithTypeFilter() {
        ResponseEntity<String> response =
                get(baseUrl + "/fulfillment/services/available?id=5&serviceTypes=CROSSDOCK,FULFILLMENT");

        assertResponseWithFile(response, "FulfillmentServiceControllerTest.available6.json");
    }

    @DisplayName("Поиск складов по настройкам")
    @Test
    void getByCapability() {
        ResponseEntity<String> response =
                get(baseUrl + "/fulfillment/services/available?id=5&capabilities=SALES_DYNAMICS");

        assertResponseWithFile(response, "FulfillmentServiceControllerTest.available7.json");
    }

    private void testAvailable(final long campaignId, final String jsonFileName) {
        final ResponseEntity<String> response =
                get(baseUrl + "/fulfillment/services/available?id={campaignId}", campaignId);

        assertResponseWithFile(response, jsonFileName);
    }

    private void assertResponseWithFile(final ResponseEntity<String> response, final String fileName) {
        final String json = getString(getClass(), fileName);
        assertResponse(response, json);
    }

    private void assertResponse(final ResponseEntity<String> response, final String json) {
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));

        final String result = new JSONObject(response.getBody()).getJSONArray("result").toString();
        JSONAssert.assertEquals(json, result, JSONCompareMode.NON_EXTENSIBLE);
    }

}
