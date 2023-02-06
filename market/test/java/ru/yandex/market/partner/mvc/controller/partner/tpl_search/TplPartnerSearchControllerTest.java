package ru.yandex.market.partner.mvc.controller.partner.tpl_search;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты для {@link TplPartnerSearchController}
 */
public class TplPartnerSearchControllerTest extends FunctionalTest {
    private static final long CAMPAIGN_ID_QUERY = 10101;
    private static final long ID_QUERY = 3003;
    private static final String TPL_TYPE_1_QUERY = CampaignType.TPL.getId();
    private static final String TPL_TYPE_2_QUERY = CampaignType.LOGISTRATOR.getId();
    private static final String NON_TPL_TYPE_QUERY = CampaignType.SHOP.getId();
    private static final String CAMPAIGN_NAME_QUERY = "tESt SHop";
    private static final String ABSENT_NAME_QUERY = "%bad name%";

    @Test
    @DbUnitDataSet(before = "TplPartnerSearchControllerTest.testGetCampaignsAllValuesQuery.before.csv")
    void testGetCampaignsAllValuesQuery() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/campaigns/3pl/search?q={name}&type={type1}&type={type2}&page=1&size=10",
                CAMPAIGN_NAME_QUERY, TPL_TYPE_1_QUERY, TPL_TYPE_2_QUERY
        );
        JsonTestUtil.assertEquals(response, TplPartnerSearchControllerTest.class,
                "TplPartnerSearchControllerTest.testGetCampaignsAllValuesQuery.response.json");
    }

    @Test
    @DbUnitDataSet(before = "TplPartnerSearchControllerTest.testGetCampaignsDefaultValuesQuery.before.csv")
    void testGetCampaignsDefaultValuesQuery() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/campaigns/3pl/search");
        JsonTestUtil.assertEquals(response, TplPartnerSearchControllerTest.class,
                "TplPartnerSearchControllerTest.testGetCampaignsDefaultValuesQuery.response.json");
    }

    @Test
    @DbUnitDataSet(before = "TplPartnerSearchControllerTest.testGetCampaignsCampaignIdQuery.before.csv")
    void testGetCampaignCampaignIdQuery() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/campaigns/3pl/search?q={campaignId}", CAMPAIGN_ID_QUERY);
        JsonTestUtil.assertEquals(response, TplPartnerSearchControllerTest.class,
                "TplPartnerSearchControllerTest.testGetCampaignsCampaignIdQuery.response.json");
    }

    @Test
    @DbUnitDataSet(before = "TplPartnerSearchControllerTest.testGetCampaignsIdQuery.before.csv")
    void testGetCampaignsIdQuery() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/campaigns/3pl/search?q={id}", ID_QUERY);
        JsonTestUtil.assertEquals(response, TplPartnerSearchControllerTest.class,
                "TplPartnerSearchControllerTest.testGetCampaignsIdQuery.response.json");
    }

    @Test
    @DbUnitDataSet(before = "TplPartnerSearchControllerTest.testGetCampaignsTypeQuery.before.csv")
    void testGetCampaignsTypeQuery() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/campaigns/3pl/search?type={type}", TPL_TYPE_2_QUERY);
        JsonTestUtil.assertEquals(response, TplPartnerSearchControllerTest.class,
                "TplPartnerSearchControllerTest.testGetCampaignsTypeQuery.response.json");
    }

    @Test
    @DbUnitDataSet(before = "TplPartnerSearchControllerTest.testGetCampaignsDefaultValuesQuery.before.csv")
    void testGetCampaignsInvalidCampaignTypeQuery() {
        Assertions.assertThrows(
                HttpClientErrorException.class, () -> FunctionalTestHelper.get(
                        baseUrl + "/campaigns/3pl/search?type={type}", NON_TPL_TYPE_QUERY));
    }

    @Test
    @DbUnitDataSet(before = "TplPartnerSearchControllerTest.testGetCampaignsEmptyResultQuery.before.csv")
    void testGetCampaignsEmptyResultQuery() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/campaigns/3pl/search?q={absentName}", ABSENT_NAME_QUERY);
        JsonTestUtil.assertEquals(response, TplPartnerSearchControllerTest.class,
                "TplPartnerSearchControllerTest.testGetCampaignsEmptyResultQuery.response.json");
    }

    @Test
    @DbUnitDataSet(before = "TplPartnerSearchControllerTest.testGetCampaignsTwoPagesQuery.before.csv")
    void testGetCampaignsTwoPagesQuery() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/campaigns/3pl/search?size=1");
        JsonTestUtil.assertEquals(response, TplPartnerSearchControllerTest.class,
                "TplPartnerSearchControllerTest.testGetCampaignsTwoPagesQuery.response.json");
    }
}
