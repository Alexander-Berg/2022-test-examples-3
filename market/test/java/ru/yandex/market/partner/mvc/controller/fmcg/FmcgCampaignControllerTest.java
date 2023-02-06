package ru.yandex.market.partner.mvc.controller.fmcg;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты для {@link FmcgCampaignController}.
 */
@DbUnitDataSet(before = "data/csv/FmcgCampaignsControllerTest.csv")
class FmcgCampaignControllerTest extends FunctionalTest {

    private static final long CLIENT_ID = 111;

    @Test
    void testGetAllUserFmcgCampaigns() {
        ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(CLIENT_ID, null));
        JsonTestUtil.assertEquals(response, getClass(), "data/json/get-all-fmcg-campaigns-response.json");
    }

    @Test
    void testQueryCampaignsWithShopName() {
        ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(CLIENT_ID, "shop"));
        JsonTestUtil.assertEquals(response, getClass(), "data/json/get-shop-1-only-campaign.json");
    }

    @Test
    void testQueryCampaignsWithShopNameIgnoreCase() {
        ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(CLIENT_ID, "sHOp"));
        JsonTestUtil.assertEquals(response, getClass(), "data/json/get-shop-1-only-campaign.json");
    }

    @Test
    void testQueryCampaignsWithPartnerId() {
        ResponseEntity<String> response = FunctionalTestHelper.get(partnersUrl(CLIENT_ID, "47"));
        JsonTestUtil.assertEquals(response, getClass(), "data/json/get-shop-1-only-campaign.json");
    }

    private String partnersUrl(final long euid, String query) {
        return baseUrl + "/fmcg/campaigns?euid=" + euid + (query == null ? "" : "&query=" + query);
    }
}
