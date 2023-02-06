package ru.yandex.market.partner.mvc.controller.partner.ucat;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;


@DbUnitDataSet(before = "TestUcatPartnerSwitch.csv")
public class PartnerUcatControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Партнер не в екате")
    void notUcat() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/partner/ucat/status?campaignId={id}", String.class, 10555L);
        JSONAssert.assertEquals("{\"switched\":false}",
                new JSONObject(response.getBody()).getJSONObject("result"), JSONCompareMode.LENIENT);
    }

    @Test
    @DisplayName("Партнер в екате")
    void testUcat() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/partner/ucat/status?campaignId={id}", String.class, 10777L);
        System.out.println(response);

        JSONAssert.assertEquals("{\"switched\":true}",
                new JSONObject(response.getBody()).getJSONObject("result"), JSONCompareMode.LENIENT);

    }

}
