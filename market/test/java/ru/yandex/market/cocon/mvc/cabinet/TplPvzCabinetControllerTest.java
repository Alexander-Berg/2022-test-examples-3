package ru.yandex.market.cocon.mvc.cabinet;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.cocon.FunctionalTest;
import ru.yandex.market.cocon.model.CabinetType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Functional tests for {@link CabinetController}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "tplPvzPages.csv")
class TplPvzCabinetControllerTest extends FunctionalTest {

    private static final long UID = 1000L;
    private static final long CAMPAIGN_ID_DROPOFF = 1001L;
    private static final long CAMPAIGN_ID_DELIVERY = 1002L;
    private static final String DROPOFF_PAGE = "market-partner:html:tpl-outlet-dropoff-order:get";

    @Autowired
    private ClientAndServer mockServer;

    @BeforeEach
    void initMockServer() {
        String tplPvzResponseBodyDropOff = "" +
                "{" +
                "  \"id\": " + CAMPAIGN_ID_DROPOFF + "," +
                "  \"mbiCampaignId\": " + CAMPAIGN_ID_DROPOFF + "," +
                "  \"name\": \"test pvz\"," +
                "  \"active\": true," +
                "  \"features\": {" +
                "    \"delivery\": false," +
                "    \"refund\": false," +
                "    \"sorting\": false," +
                "    \"dropOff\": true" +
                "  }" +
                "}";
        mockServer.when(
                HttpRequest.request()
                .withMethod("GET")
                .withPath("/v1/pi/hub")
                .withQueryStringParameters(
                        Parameter.param("campaignId", CAMPAIGN_ID_DROPOFF + "")
                )
        ).respond(
                HttpResponse.response()
                .withStatusCode(HttpStatus.OK.value())
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(JsonBody.json(tplPvzResponseBodyDropOff))
        );

        String tplPvzResponseBodyDelivery = "" +
                "{" +
                "  \"id\": " + CAMPAIGN_ID_DELIVERY + "," +
                "  \"mbiCampaignId\": " + CAMPAIGN_ID_DELIVERY + "," +
                "  \"name\": \"test pvz\"," +
                "  \"active\": true," +
                "  \"features\": {" +
                "    \"delivery\": true," +
                "    \"refund\": false," +
                "    \"sorting\": false," +
                "    \"dropOff\": false" +
                "  }" +
                "}";
        mockServer.when(
                HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/v1/pi/hub")
                        .withQueryStringParameters(
                                Parameter.param("campaignId", CAMPAIGN_ID_DROPOFF + "")
                        )
        ).respond(
                HttpResponse.response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(tplPvzResponseBodyDelivery))
        );

        String mbiResponseBody = "{\n" +
                "    \"authorities\": [\n" +
                "        {\n" +
                "            \"id\": 1002,\n" +
                "            \"name\": \"CAMPAIGN_TYPE\",\n" +
                "            \"checker\": \"campaignTypeChecker\",\n" +
                "            \"params\": \"TPL_OUTLET\",\n" +
                "            \"domain\": \"MBI-PARTNER\",\n" +
                "            \"checkResult\": true\n" +
                "        }\n" +
                "    ]\n" +
                "}\n";
        mockServer.when(
                HttpRequest.request()
                        .withMethod("POST")
                        .withPath("/checker/result")
                        .withQueryStringParameters(
                                Parameter.param("uid", UID + "")
                        )
        ).respond(
                HttpResponse.response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(mbiResponseBody))
        );
    }

    @Test
    @DisplayName("Получение конфига для разрешенной страницы")
    void allowedPage() {
        String response = getPage(CabinetType.TPL_OUTLET, UID, CAMPAIGN_ID_DROPOFF, DROPOFF_PAGE);

        assertThat(response,
                hasJsonPath(
                        "$.result" +
                                ".pages[?(@.name=='" + DROPOFF_PAGE + "')]" +
                                ".states.result",
                        is(List.of(true))
                ));
    }

    @Test
    @DisplayName("Получение конфига для запрещенной страницы")
    void restrictedPage() {
        String response = getPage(CabinetType.TPL_OUTLET, UID, CAMPAIGN_ID_DELIVERY, DROPOFF_PAGE);

        assertThat(response,
                hasJsonPath(
                        "$.result" +
                                ".pages[?(@.name=='" + DROPOFF_PAGE + "')]" +
                                ".states.result",
                        is(List.of(false))
                ));
    }

    private String pageUrl(CabinetType cabinetType, long uid, long campaignId, String page) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path("/cabinet/{cabinet}/page?uid={uid}&page={page}&id={id}")
                .buildAndExpand(cabinetType.getId(), uid, page, campaignId)
                .toUriString();
    }

    private String getPage(CabinetType cabinetType, long uid, long campaignId, String page) {
        String pagesUrl = pageUrl(cabinetType, uid, campaignId, page);
        return FunctionalTestHelper.get(pagesUrl).getBody();
    }
}
