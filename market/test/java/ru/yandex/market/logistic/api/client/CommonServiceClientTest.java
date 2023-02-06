package ru.yandex.market.logistic.api.client;

import java.io.IOException;
import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestTemplate;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.matchers.CompareMatcher;

import ru.yandex.market.logistic.api.model.common.OrderTransferCode;
import ru.yandex.market.logistic.api.model.common.OrderTransferCodes;
import ru.yandex.market.logistic.api.model.fulfillment.request.entities.restricted.CreateOrderRestrictedData;
import ru.yandex.market.logistic.api.model.fulfillment.request.entities.restricted.UpdateOrderRestrictedData;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.ContentType;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    ServicesClientConf.class
})
@TestPropertySource("classpath:test.properties")
public abstract class CommonServiceClientTest {

    protected static final String PARTNER_URL = "https://localhost/query-gateway";

    protected static final String INTERNAL_PARTNER_URL = "https://localhost.market.yandex.net/query-gateway";

    private static final String TOKEN = "xxxxxxxxxxxxxxxxxxxxxxmarschrouteT.T.Tokenxxxxxxxxxxxxxxxxxxxxxxxxxx";


    protected static final long SERVICE_ID = 174;

    protected final ObjectMapper xmlMapper;

    @Autowired
    @Qualifier("getXmlRestTemplate")
    protected RestTemplate restXmlTemplate;

    @Autowired
    @Qualifier("getJsonRestTemplate")
    protected RestTemplate restJsonTemplate;

    @Autowired
    protected DeliveryServiceClient deliveryServiceClient;

    @Autowired
    protected FulfillmentClient fulfillmentClient;

    protected MockRestServiceServer mockService;

    protected MockRestServiceServer mockPropertiesService;

    protected SoftAssertions assertions = new SoftAssertions();

    public CommonServiceClientTest() {
        xmlMapper = LogisticApiClientFactory.createXmlMapper();
    }

    @BeforeEach
    public void before() {
        this.mockService = MockRestServiceServer.createServer(restXmlTemplate);
        this.mockPropertiesService = MockRestServiceServer.createServer(restJsonTemplate);
    }

    @AfterEach
    public void after() {
        mockService.verify();
        assertions.assertAll();
    }

    protected ObjectMapper getXmlMapper() {
        return xmlMapper;
    }

    protected final String getFileContent(String filename) throws IOException {
        return IOUtils.toString(getSystemResourceAsStream(filename),
            Charset.forName("UTF-8"));
    }

    protected final <T> T getObjectFromXml(String filename, Class<T> type) throws IOException {
        return getXmlMapper().readValue(getFileContent(filename), type);
    }

    protected String getResponseBody(String response, ContentType contentType) throws IOException {
        return getFileContent("fixture/response/" + response + "." + contentType.toString().toLowerCase());
    }

    protected String getRequestBody(String request, ContentType contentType) throws IOException {
        return getFileContent("fixture/request/" + request + "." + contentType.toString().toLowerCase());
    }

    private ResponseCreator getTaskResponseCreator(String methodName) throws IOException {
        return withStatus(OK)
            .contentType(APPLICATION_XML)
            .body(getResponseBody(methodName, ContentType.XML));
    }

    protected void prepareMockServiceNormalized(String methodName, String url) throws IOException {
        this.prepareMockServiceNormalized(methodName, methodName, url);
    }

    protected void prepareMockServiceNormalized(
        String methodNameRequest, String methodNameResponse, String url) throws IOException {
        mockService.expect(requestTo(url))
            .andExpect(content().string(CompareMatcher.isSimilarTo(getRequestBody(methodNameRequest, ContentType.XML))
                .ignoreWhitespace()
                .normalizeWhitespace()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))))
            .andRespond(getTaskResponseCreator(methodNameResponse));
    }

    protected void prepareMockService(String methodName, String url) throws IOException {
        mockService.expect(requestTo(url))
            .andExpect(content().string(CompareMatcher.isSimilarTo(getRequestBody(methodName, ContentType.XML))
                .ignoreWhitespace()
                .normalizeWhitespace()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))))
            .andRespond(getTaskResponseCreator(methodName));
    }

    protected PartnerProperties getPartnerProperties() {
        return new PartnerProperties(TOKEN, PARTNER_URL);
    }

    protected PartnerProperties getInternalPartnerProperties() {
        return new PartnerProperties(TOKEN, INTERNAL_PARTNER_URL);
    }

    protected CreateOrderRestrictedData getCreateOrderRestrictedData() {
        return new CreateOrderRestrictedData(
            new OrderTransferCodes.OrderTransferCodesBuilder()
                .setInbound(new OrderTransferCode.OrderTransferCodeBuilder().setVerification("123456").build())
                .setOutbound(new OrderTransferCode.OrderTransferCodeBuilder().setVerification("654321").build())
                .setReturnOutbound(new OrderTransferCode.OrderTransferCodeBuilder().setVerification("333333").build())
                .build()
        );
    }

    protected UpdateOrderRestrictedData getUpdateOrderRestrictedData() {
        return new UpdateOrderRestrictedData(
            new OrderTransferCodes.OrderTransferCodesBuilder()
                .setInbound(new OrderTransferCode.OrderTransferCodeBuilder().setVerification("123456").build())
                .setOutbound(new OrderTransferCode.OrderTransferCodeBuilder().setVerification("654321").build())
                .setReturnOutbound(new OrderTransferCode.OrderTransferCodeBuilder().setVerification("333333").build())
                .build()
        );
    }
}
