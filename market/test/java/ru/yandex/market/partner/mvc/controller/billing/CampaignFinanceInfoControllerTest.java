package ru.yandex.market.partner.mvc.controller.billing;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты {@link CampaignFinanceInfoController}.
 */
class CampaignFinanceInfoControllerTest extends FunctionalTest {
    private static final long UID = 67282295L;
    private static final long CAMPAIGN_ID = 10774L;

    @Test
    @DbUnitDataSet(before = "csv/CampaignFinanceInfoControllerTest.shopTest.csv")
    void shopTest() {
        test("json/CampaignFinanceInfoControllerTest.shopTest.json");
    }

    @Test
    @DbUnitDataSet(before = "csv/CampaignFinanceInfoControllerTest.supplierTest.csv")
    void supplierTest() {
        test("json/CampaignFinanceInfoControllerTest.supplierTest.json");
    }

    void test(String expectedJsonFile) {
        final ResponseEntity<String> response = FunctionalTestHelper.get(
                UriComponentsBuilder.fromUriString(baseUrl + "/campaign/finance/info")
                        .queryParam("_user_id", UID)
                        .queryParam("campaign_id", CAMPAIGN_ID)
                        .queryParam("format=json")
                        .build().toString());
        JsonTestUtil.assertEquals(response, getClass(), expectedJsonFile);
    }
}
