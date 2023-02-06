package ru.yandex.market.delivery.command;

import java.io.PrintWriter;
import java.util.Collections;

import javax.annotation.Nonnull;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;

@ExtendWith(MockitoExtension.class)
@DbUnitDataSet(before = "testSyncDeliveryWithTarifficator.before.csv")
public class SyncDeliveryWithTarifficatorCommandTest extends FunctionalTest {

    @Autowired
    private SyncDeliveryWithTarifficatorCommand tested;
    @Autowired
    private Terminal terminal;
    @Autowired
    private WireMockServer tarificatorWireMockServer;

    @BeforeEach
    private void beforeEach() {
        tarificatorWireMockServer.resetMappings();
        tarificatorWireMockServer.removeServeEventsMatching(RequestPattern.everything());
        when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
    }

    @Test
    @DbUnitDataSet(after = "testSyncDeliveryWithTarifficator.after.csv")
    void testOneShopExport() {
        /*
        Given
         */
        prepareMockForShop1000();
        /*
        When
         */
        tested.executeCommand(createCommandInvocation(new String[]{"1000"}), terminal);
    }

    @Test
    void testAllShopsExported() {
        /*
        Given
         */
        prepareMockForShop1000();
        prepareMockForShop2000();
        /*
        When
         */
        tested.executeCommand(createCommandInvocation(new String[]{}), terminal);
    }

    @Nonnull
    private CommandInvocation createCommandInvocation(String[] shopIds) {
        return new CommandInvocation(SyncDeliveryWithTarifficatorCommand.COMMAND_NAME,
                shopIds,
                Collections.emptyMap());
    }

    private void prepareMockForShop1000() {
        /*
        Common
         */
        tarificatorWireMockServer.stubFor(post("/v2/shops/1000/meta?_user_id=11")
                .withRequestBody(equalToJson(getString(this.getClass(), "json/metaDataForShop1000.request.json")))
                .willReturn(aResponse().withStatus(200).withBody(getString(this.getClass(), "json/metaDataForShop1000.response.json"))));
        tarificatorWireMockServer.stubFor(get("/v2/shops/1000/region-groups")
                .willReturn(aResponse().withStatus(200).withBody(getString(this.getClass(), "json/regionGroupsForShop1000.response.json"))));
        tarificatorWireMockServer.stubFor(post("/v2/shops/1000/region-groups/statuses?_user_id=11")
                .withRequestBody(equalToJson(getString(this.getClass(), "json/statusesForShop100.request.json")))
                .willReturn(aResponse()
                        .withStatus(200)));
        tarificatorWireMockServer.stubFor(delete("/v2/shops/1000/region-groups?_user_id=11&regionId=9000")
                .willReturn(aResponse()
                        .withStatus(200)));
        /*
        New group 11
         */
        tarificatorWireMockServer.stubFor(post("/v2/shops/1000/region-groups?_user_id=11")
                .withRequestBody(equalToJson(getString(this.getClass(), "json/newRegionGroupForShop1000.request.json")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(getString(this.getClass(), "json/newRegionGroupForShop1000.response.json"))));
        tarificatorWireMockServer.stubFor(post("/v2/shops/1000/region-groups/11/tariff?_user_id=11")
                .withRequestBody(equalToJson(getString(this.getClass(), "json/tariffForShop100regionGroup11.request.json")))
                .willReturn(aResponse()
                        .withStatus(200)));
        tarificatorWireMockServer.stubFor(post("/v2/shops/1000/region-groups/11/payment-types?_user_id=11")
                .withRequestBody(equalToJson("[]"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(getString(this.getClass(), "json/paymentsUpdateResponse.json"))));
        tarificatorWireMockServer.stubFor(get("/v2/shops/1000/region-groups/11/delivery-services?_user_id=11")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("[]")));
        /*
        New group 12
         */
        tarificatorWireMockServer.stubFor(post("/v2/shops/1000/region-groups?_user_id=11")
                .withRequestBody(equalToJson(getString(this.getClass(), "json/newRegion12GroupForShop1000.request.json")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(getString(this.getClass(), "json/newRegion12GroupForShop1000.response.json"))));
        tarificatorWireMockServer.stubFor(post("/v2/shops/1000/region-groups/12/tariff?_user_id=11")
                .withRequestBody(equalToJson(getString(this.getClass(), "json/tariffForShop100regionGroup11.request.json")))
                .willReturn(aResponse()
                        .withStatus(200)));
        tarificatorWireMockServer.stubFor(post("/v2/shops/1000/region-groups/12/payment-types?_user_id=11")
                .withRequestBody(equalToJson("[]"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(getString(this.getClass(), "json/paymentsUpdateResponse.json"))));
        tarificatorWireMockServer.stubFor(get("/v2/shops/1000/region-groups/12/delivery-services?_user_id=11")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("[]")));
        /*
        Updated group 8000
         */
        tarificatorWireMockServer.stubFor(post("/v2/shops/1000/region-groups/8000?_user_id=11")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(getString(this.getClass(), "json/regionGroupForShop1000regionGroup8000.response.json"))));
        tarificatorWireMockServer.stubFor(post("/v2/shops/1000/region-groups/8000/tariff?_user_id=11")
                .withRequestBody(equalToJson(getString(this.getClass(), "json/tariffForShop100regionGroup8000.request.json")))
                .willReturn(aResponse()
                        .withStatus(200)));
        tarificatorWireMockServer.stubFor(post("/v2/shops/1000/region-groups/8000/payment-types?_user_id=11")
                .withRequestBody(equalToJson(getString(this.getClass(), "json/paymentTypeForShop1000regionGroup8000.request.json")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(getString(this.getClass(), "json/paymentsUpdateResponse.json"))));
        tarificatorWireMockServer.stubFor(post("/v2/shops/1000/region-groups/8000/delivery-services?_user_id=11")
                .withRequestBody(equalToJson(getString(this.getClass(), "json/deliveryServicesSaveForShop1000.request.json")))
                .willReturn(aResponse()
                        .withStatus(200)));
        tarificatorWireMockServer.stubFor(get("/v2/shops/1000/region-groups/8000/delivery-services?_user_id=11")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("[]")));
    }

    private void prepareMockForShop2000() {
        /*
        Common
         */
        tarificatorWireMockServer.stubFor(post("/v2/shops/2000/meta?_user_id=11")
                .withRequestBody(equalToJson(getString(this.getClass(), "json/metaDataForShop2000.request.json")))
                .willReturn(aResponse().withStatus(200).withBody(getString(this.getClass(), "json/metaDataForShop2000.response.json"))));
        tarificatorWireMockServer.stubFor(get("/v2/shops/2000/region-groups")
                .willReturn(aResponse().withStatus(200).withBody(getString(this.getClass(), "json/regionGroupsForShop2000.response.json"))));
        tarificatorWireMockServer.stubFor(post("/v2/shops/2000/region-groups/statuses?_user_id=11")
                .withRequestBody(equalToJson(getString(this.getClass(), "json/statusesForShop2000.request.json")))
                .willReturn(aResponse()
                        .withStatus(200)));
        /*
        New group 22
         */
        tarificatorWireMockServer.stubFor(post("/v2/shops/2000/region-groups?_user_id=11")
                .withRequestBody(equalToJson(getString(this.getClass(), "json/newRegionGroupForShop2000.request.json")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(getString(this.getClass(), "json/newRegionGroupForShop2000.response.json"))));
        tarificatorWireMockServer.stubFor(post("/v2/shops/2000/region-groups/22/tariff?_user_id=11")
                .withRequestBody(equalToJson(getString(this.getClass(), "json/tariffForShop2000regionGroup22.request.json")))
                .willReturn(aResponse()
                        .withStatus(200)));
        tarificatorWireMockServer.stubFor(post("/v2/shops/2000/region-groups/22/payment-types?_user_id=11")
                .withRequestBody(equalToJson("[\"COURIER_CARD\"]"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(getString(this.getClass(), "json/paymentsUpdateResponse.json"))));
        tarificatorWireMockServer.stubFor(get("/v2/shops/2000/region-groups/22/delivery-services?_user_id=11")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("[]")));
    }
}
