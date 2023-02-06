package ru.yandex.market.partner.mvc.controller.delivery;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@DbUnitDataSet(before = "CommonDeliveryData.csv")
public class IntakeCostControllerTest extends FunctionalTest {
    private static final String BY_SHIPMENT_ID_JSON = "{\"cost\":123,\"billed\":true}";
    private static final String BY_PARAMS_JSON = "{\"cost\":4321,\"billed\":false}";
    private static final String BY_PARAMS_JSON_EMPTY = "{\"billed\":false}";

    @Test
    @DbUnitDataSet(before = "IntakeCostControllerTest.findCostByShipmentId.before.csv")
    public void findCostByShipmentId() throws JSONException {
        test("1", "0", "0", "0", "0", "0", BY_SHIPMENT_ID_JSON);
    }

    @Test
    @DbUnitDataSet(before = "IntakeCostControllerTest.findCostByParams.before.csv")
    public void findCostByParams() throws JSONException {
        test("-1", "0", "123", "100", "100", "100", BY_PARAMS_JSON);
        test("-1", "0", "123", "100", "100", "101", BY_PARAMS_JSON_EMPTY);
        test("", "0", "123", "10", "10", "10", BY_PARAMS_JSON);
    }

    public void test(final String shipmentId, final String shopId, final String deliveryServiceId, final String width, final String height, final String depth, String resultJson) throws JSONException {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/intakeCost?_user_id=268255595&shipmentId="
                        + shipmentId + "&shopId=" + shopId + "&deliveryServiceId=" + deliveryServiceId
                        + "&width=" + width + "&height=" + height + "&depth=" + depth);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        String json = response.getBody();
        JSONAssert.assertEquals(resultJson, new JSONObject(json).getJSONObject("result").toString(), true);
    }

}
