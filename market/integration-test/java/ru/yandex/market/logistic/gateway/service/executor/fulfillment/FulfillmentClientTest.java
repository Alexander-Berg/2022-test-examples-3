package ru.yandex.market.logistic.gateway.service.executor.fulfillment;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;

import ru.yandex.market.logistic.api.exceptions.ValidationException;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetInboundsStatusRequest;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.sync.GetInboundsStatusRequestExecutor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.GET_INBOUNDS_STATUS_FF;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class FulfillmentClientTest extends AbstractIntegrationTest {

    @Autowired
    private GetInboundsStatusRequestExecutor executor;

    private final static long TEST_PARTNER_ID = 145L;

    private final static String PARTNER_URL =
        "http://partner-api-mock.tst.vs.market.yandex.net:80/1P-getInboundsStatus/mock/check";

    private MockRestServiceServer mockServer;

    @Before
    public void setup() throws Exception {
        mockServer = createMockServerByRequest(GET_INBOUNDS_STATUS_FF);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void testRequestValidationFailed() throws Exception {
        GetInboundsStatusRequest getInboundDetailsRequest = new GetInboundsStatusRequest(
            Collections.emptyList(),
            new Partner(TEST_PARTNER_ID));

        assertThatThrownBy(() -> executor.tryExecute(getInboundDetailsRequest, Collections.emptySet()))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    public void testResponseValidationFailed() throws Exception {
        prepareMockServerXmlScenario(mockServer, PARTNER_URL,
            "fixtures/request/fulfillment/get_inbound_status/fulfillment_get_inbound_status.xml",
            "fixtures/response/fulfillment/fulfillment_not_valid_response.xml");

        assertThatThrownBy(() -> executor.tryExecute(getRequest(), Collections.emptySet()))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    public void testResponseContentTypeHtmlSuccess() throws Exception {
        ResponseCreator taskResponseCreator = withStatus(OK)
            .contentType(MediaType.TEXT_HTML)
            .body(getFileContent("fixtures/response/fulfillment/get_inbound_status/fulfillment_get_inbound_status.xml"));

        mockServer.expect(requestTo(PARTNER_URL)).andRespond(taskResponseCreator);

        executor.tryExecute(getRequest(), Collections.emptySet());
    }

    @Test
    public void testResponseSimilarContentTypeSuccess() throws Exception {
        ResponseCreator taskResponseCreator = withStatus(OK)
            .contentType(new MediaType("application", "xml", Charset.forName("utf-8")))
            .body(getFileContent("fixtures/response/fulfillment/get_inbound_status/fulfillment_get_inbound_status.xml"));

        mockServer.expect(requestTo(PARTNER_URL)).andRespond(taskResponseCreator);

        executor.tryExecute(getRequest(), Collections.emptySet());
    }

    private GetInboundsStatusRequest getRequest() {
        return new GetInboundsStatusRequest(
            Arrays.asList(
                ResourceId.builder().setYandexId("543242").setPartnerId("AD156DF").build(),
                ResourceId.builder().setYandexId("543243").setPartnerId("AD156DD").build()),
            new Partner(TEST_PARTNER_ID));
    }
}
