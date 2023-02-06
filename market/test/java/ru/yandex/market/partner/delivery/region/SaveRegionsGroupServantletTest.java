package ru.yandex.market.partner.delivery.region;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;

class SaveRegionsGroupServantletTest extends FunctionalTest {
    private static final Long SHOP_ID = 774L;
    private static final Long TARIFFICATOR_REGION_GROUP_ID = 1000L;

    @Autowired
    private WireMockServer tarifficatorWireMockServer;

    @BeforeEach
    private void beforeEach() {
        tarifficatorWireMockServer.resetMappings();
        tarifficatorWireMockServer.removeServeEventsMatching(RequestPattern.everything());
    }

    @Test
    @DisplayName("Успешное сохранение группы регионов")
    @DbUnitDataSet(
            before = "csv/SaveRegionsGroupServantletTest.testSuccessCreation.before.csv"
    )
    void testSuccessCreationDbs() {
        mockTarifficatorCreateRequest();

        HttpEntity request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "json/SaveRegionsGroupServantletTest.testSuccessCreation.request.json"
        );

        ResponseEntity<String> response = FunctionalTestHelper.post(getUrl(SHOP_ID), request);

        JsonTestUtil.assertEquals(
                response,
                this.getClass(),
                "json/SaveRegionsGroupServantletTest.testSuccessCreation.response.json"
        );
    }

    @DisplayName("Успешное обновление группы регионов")
    @Test
    @DbUnitDataSet(
            before = "csv/SaveRegionsGroupServantletTest.testSuccessfulUpdateWithMapping.before.csv"
    )
    void testSuccessfulUpdate() {
        mockTarifficatorUpdateRequest(TARIFFICATOR_REGION_GROUP_ID);

        HttpEntity request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "json/SaveRegionsGroupServantletTest.testSuccessUpdate.request.json"
        );

        ResponseEntity<String> response = FunctionalTestHelper.post(
                getUrl(SHOP_ID) + "&regionGroupId=" + TARIFFICATOR_REGION_GROUP_ID,
                request
        );

        JsonTestUtil.assertEquals(
                response,
                this.getClass(),
                "json/SaveRegionsGroupServantletTest.testSuccessUpdate.response.json"
        );
    }

    private void mockTarifficatorCreateRequest() {
        tarifficatorWireMockServer.stubFor(
                post("/v2/shops/" + SHOP_ID + "/region-groups?_user_id=0")
                        .withRequestBody(equalToJson(getString(this.getClass(), "json/SaveRegionsGroup.tarifficator.creation.request.json")))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody(getString(this.getClass(), "json/SaveRegionsGroup.tarifficator.creation.response.json"))));
    }

    private void mockTarifficatorUpdateRequest(long regionGroupId) {
        tarifficatorWireMockServer.stubFor(
                post("/v2/shops/" + SHOP_ID + "/region-groups/" + regionGroupId + "?_user_id=0")
                        .withRequestBody(equalToJson(getString(this.getClass(), "json/SaveRegionsGroup.tarifficator.request.json")))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody(getString(this.getClass(), "json/SaveRegionsGroup.tarifficator.response.json"))));
    }

    private String getUrl(long shopId) {
        return String.format("%s/saveRegionsGroup?format=json&datasourceId=%d", baseUrl, shopId);
    }
}
