package ru.yandex.market.partner.mvc.controller.campaign;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

public class LegacyCampaignControllerFunctionalTest extends FunctionalTest {
    private String getUrl(String requestOfCampaignId, long id) {
        return baseUrl + "/getCampaign?" + requestOfCampaignId + "=" + id;
    }

    /**
     * {@link javax.servlet.http.HttpServletResponse#SC_NOT_FOUND 404} если кампания не найдена.
     */
    @ParameterizedTest
    @DbUnitDataSet
    @CsvSource({
            "campaign_id",
            "id",
            "campaignId"
    })
    public void getCampaignNotFoundTest(String request) {
        HttpClientErrorException error = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(getUrl(request, 10774), String.class)
        );
        Assertions.assertEquals(error.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @ParameterizedTest
    @DbUnitDataSet(before = "getCampaignTest.csv")
    @CsvSource({
            "Shop,10774,campaign_id",
            "Shop,10774,id",
            "Shop,10774,campaignId",
            "Supplier,10775,campaign_id",
            "Supplier,10775,id",
            "Supplier,10775,campaignId",
            "Fmcg,10776,campaign_id",
            "Fmcg,10776,id",
            "Fmcg,10776,campaignId"
    })
    public void getCampaignTest(String type, long id, String request) {
        ResponseEntity<String> responseShop = FunctionalTestHelper.get(getUrl(request, id));
        JsonTestUtil.assertEquals(responseShop, getClass(), "testLegacyGetCampaign" + type + ".json");
    }
}
