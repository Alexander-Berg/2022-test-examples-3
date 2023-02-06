package ru.yandex.market.ff.tvm;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.ff.base.TvmIntegrationTest;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.ff.util.FileContentUtils.getFileContent;

public class UsingTvmInIntegrationWithLgwTest extends TvmIntegrationTest {

    private static final Long REQ_ID_1 = 1L;
    private static final String REQ_EXT_ID_1 = "11";
    private static final Long SERVICE_ID_1 = 555L;
    private static final ResourceId RES_ID_1 = ResourceId.builder()
        .setYandexId(REQ_ID_1.toString())
        .setFulfillmentId(REQ_EXT_ID_1)
        .setPartnerId(REQ_EXT_ID_1)
        .build();
    private static final Partner PARTNER_1 = new Partner(SERVICE_ID_1);

    @Autowired
    @Qualifier("fulfillmentClient")
    private FulfillmentClient fulfillmentClient;

    @Autowired
    private TvmClient tvmClient;

    @Autowired
    @Qualifier("lgwApiRestTemplate")
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    public void init() {
        Mockito.when(tvmClient.getServiceTicketFor(2011232)).thenReturn("ServiceTicketLgw");
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    /**
     * Проверка, что при запросе к LGW передаются TVM тикеты.
     */
    @Test
    void tvmTicketsSentInLgwRequest() throws IOException {
        ResponseActions responseActions = mockServer.expect(
            requestTo("https://lgw.vs.market.yandex.net/fulfillment/getOutboundDetails"));

        String expectedRequest = getFileContent("tvm/lgw-integration/lgw-request.json");
        responseActions
            .andExpect(content().string(expectedRequest))
            .andExpect(header("X-Ya-Service-Ticket", "ServiceTicketLgw"));

        ResponseCreator response = withStatus(OK)
            .contentType(APPLICATION_JSON)
            .body(getFileContent("tvm/lgw-integration/lgw-response.json"));

        responseActions.andRespond(response);

        fulfillmentClient.getOutboundDetails(RES_ID_1, PARTNER_1);

    }
}
