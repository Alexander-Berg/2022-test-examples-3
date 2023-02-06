package ru.yandex.market.partner.mvc.controller.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

@DbUnitDataSet(before = "LogisticPartnerControllerTest.before.csv")
public class LogisticPartnerControllerTest extends FunctionalTest {

    private static final long CAMPAIGN_ID = 21581225;

    private static final long NO_CUTOFF_CAMPAIGN_ID = 25135795;

    @Test
    void getPointSwitchState() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(
                        baseUrl + String.format("/campaigns/%s/logistic/point-switch-state", CAMPAIGN_ID));

        JsonTestUtil.assertEquals(
                response,
                "{\n" +
                        "  \"id\": 56374,\n" +
                        "  \"name\": \"Я Точка\",\n" +
                        "  \"address\": \"Льва Толстого д. 18б\",\n" +
                        "  \"shipmentType\": \"IMPORT\"\n" +
                        "}");
    }

    @Test
    void noCutoffEmptyResponse() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(
                        baseUrl + String.format("/campaigns/%s/logistic/point-switch-state", NO_CUTOFF_CAMPAIGN_ID));

        JsonTestUtil.assertEquals(response, "{}");
    }
}
