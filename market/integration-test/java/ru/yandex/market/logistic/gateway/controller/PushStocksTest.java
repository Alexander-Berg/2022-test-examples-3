package ru.yandex.market.logistic.gateway.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.service.util.RequestTvmServiceTicketChecker;
import ru.yandex.market.logistic.gateway.service.util.ServicePropertiesService;
import ru.yandex.market.logistic.gateway.utils.MockServerUtils;
import ru.yandex.market.logistics.test.integration.matchers.XmlMatcher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistic.gateway.processing.LogisticApiRequestProcessingService.PARTNER_ID_HEADER;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class PushStocksTest extends AbstractIntegrationTest {

    private static final String STOCKS = "/stocks";
    private static final long EXISTING_PARTNER_ID = 145;
    private static final long NOT_EXISTING_PARTNER_ID = 22222222;

    @Autowired
    private RestTemplate stockStorageRestTemplate;

    @SpyBean
    private RequestTvmServiceTicketChecker requestTvmServiceTicketChecker;

    @SpyBean
    private ServicePropertiesService servicePropertiesService;

    private MockRestServiceServer mockStockStorageServer;

    @Before
    public void setup() {
        mockStockStorageServer = MockServerUtils.createMockRestServiceServer(stockStorageRestTemplate);
    }

    @After
    public void tearDown() {
        mockStockStorageServer.verify();
    }

    @Test
    public void positiveScenario() throws Exception {

        assertPositiveScenario(
            post("/fulfillment/query-gateway")
                .contentType(MediaType.TEXT_XML)
                .content(getFileContent("fixtures/request/fulfillment/push_stocks/fulfillment_push_stocks.xml"))
        );

        verifyNoInteractions(requestTvmServiceTicketChecker);
    }

    @Test
    public void positiveScenarioFallingBackToTokenWhenBadTvm() throws Exception {
        doReturn(false)
            .when(requestTvmServiceTicketChecker)
            .isValid(any());

        assertPositiveScenario(
            post("/fulfillment/query-gateway")
                .contentType(MediaType.TEXT_XML)
                .content(getFileContent("fixtures/request/fulfillment/push_stocks/fulfillment_push_stocks.xml"))
                .header(PARTNER_ID_HEADER, Long.toString(EXISTING_PARTNER_ID))
        );
    }

    @Test
    public void positiveScenarioPartnerHeader() throws Exception {
        doRequestWithPartnerIdHeader(EXISTING_PARTNER_ID);
    }

    @Test
    public void positiveScenarioPartnerHeaderNoPartnerInLgw() throws Exception {
        doRequestWithPartnerIdHeader(NOT_EXISTING_PARTNER_ID);
    }

    @Test
    public void anyAcceptHeaderReturnsXml() throws Exception {
        mockMvc.perform(post("/fulfillment/query-gateway")
            .contentType(MediaType.TEXT_XML)
            .accept(MediaType.APPLICATION_JSON)
            .content(getFileContent("fixtures/request/fulfillment/push_stocks/fulfillment_push_stocks.xml")))
            .andExpect(content().contentType(MediaType.TEXT_XML));
    }

    @Test
    public void negativeScenarioBadRequest() throws Exception {

        mockStockStorageServer.expect(
            once(),
            requestTo(stockStorageHost + STOCKS)
        ).andRespond(withStatus(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
        );

        String xmlResponse = mockMvc.perform(post("/fulfillment/query-gateway")
            .contentType(MediaType.TEXT_XML)
            .content(getFileContent("fixtures/request/fulfillment/push_stocks/fulfillment_push_stocks_with_duplicate_types.xml")))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String expectedXml = getFileContent("fixtures/response/fulfillment/push_stocks/fulfillment_push_stocks_with_duplicate_types.xml");

        softAssert.assertThat(new XmlMatcher(expectedXml).matches(xmlResponse))
            .as("Asserting that the XML response is correct")
            .isTrue();

    }

    @Test
    public void negativeScenarioInvalidToken() throws Exception {
        String xmlResponse = mockMvc.perform(post("/fulfillment/query-gateway")
            .contentType(MediaType.TEXT_XML)
            .content(getFileContent("fixtures/request/fulfillment/push_stocks/fulfillment_push_stocks_invalid_token.xml")))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String expectedXml = getFileContent("fixtures/response/fulfillment/push_stocks/fulfillment_push_stocks_invalid_token.xml");

        softAssert.assertThat(new XmlMatcher(expectedXml).matches(xmlResponse))
            .as("Asserting that the XML response is correct")
            .isTrue();

    }

    private void doRequestWithPartnerIdHeader(long partnerId) throws Exception {
        doReturn(true)
            .when(requestTvmServiceTicketChecker)
            .isValid(any());

        assertPositiveScenario(
            post("/fulfillment/query-gateway")
                .contentType(MediaType.TEXT_XML)
                .content(
                    getFileContent(
                        "fixtures/request/fulfillment/push_stocks/fulfillment_push_stocks_invalid_token.xml"
                    )
                )
                .header(PARTNER_ID_HEADER, Long.toString(partnerId))
        );

        verifyNoInteractions(servicePropertiesService);
    }

    private void assertPositiveScenario(RequestBuilder requestBuilder) throws Exception {
        prepareMockServerJsonScenario(
            mockStockStorageServer,
            once(),
            stockStorageHost + STOCKS,
            null
        );

        String xmlResponse = mockMvc.perform(requestBuilder)
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String expectedXml = getFileContent("fixtures/response/fulfillment/push_stocks/fulfillment_push_stocks.xml");

        softAssert.assertThat(new XmlMatcher(expectedXml).matches(xmlResponse))
            .as("Asserting that the XML response is correct")
            .isTrue();
    }
}
