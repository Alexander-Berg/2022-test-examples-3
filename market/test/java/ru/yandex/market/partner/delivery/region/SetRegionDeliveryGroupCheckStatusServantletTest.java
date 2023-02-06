package ru.yandex.market.partner.delivery.region;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;

public class SetRegionDeliveryGroupCheckStatusServantletTest extends FunctionalTest {

    private static final Long SHOP_ID = 1L;

    @Autowired
    private WireMockServer tarifficatorWireMockServer;

    @BeforeEach
    private void beforeEach() {
        tarifficatorWireMockServer.resetMappings();
        tarifficatorWireMockServer.removeServeEventsMatching(RequestPattern.everything());
    }

    @Test
    @DisplayName("Успешное обновление статуса группы регионов")
    @DbUnitDataSet(before = "csv/SetRegionDeliveryGroupCheckStatusServantletTest.testSaveStatus.before.csv")
    void testSaveStatus() {
        mockTarifficatorRequest("json/saveStatus.tarifficator.request.json");

        FunctionalTestHelper.post(
                createUrl(),
                getString(this.getClass(), "json/saveStatus.request.json")
        );
    }

    private String createUrl() {
        return baseUrl + "setRegionDeliveryGroupCheckStatus?datasource_id=" + SHOP_ID;
    }

    private void mockTarifficatorRequest(String expectedRequestPath) {
        tarifficatorWireMockServer.stubFor(
                post("/v2/shops/" + SHOP_ID + "/region-groups/statuses?_user_id=0")
                        .withRequestBody(equalToJson(getString(this.getClass(), expectedRequestPath)))
                        .willReturn(aResponse().withStatus(200)));
    }
}
