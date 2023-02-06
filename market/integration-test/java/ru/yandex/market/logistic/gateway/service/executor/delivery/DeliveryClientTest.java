package ru.yandex.market.logistic.gateway.service.executor.delivery;

import java.io.IOException;
import java.nio.charset.Charset;
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
import ru.yandex.market.logistic.api.model.common.PartnerMethod;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.UpdateOrderRequest;
import ru.yandex.market.logistic.gateway.exceptions.PartnerInteractionException;
import ru.yandex.market.logistic.gateway.service.executor.delivery.sync.UpdateOrderRequestExecutor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class DeliveryClientTest extends AbstractIntegrationTest {

    private final static String UNIQ = "64c9579de53cb19afe9f96edb8c59fca";

    @Autowired
    private UpdateOrderRequestExecutor executor;

    private final static long TEST_PARTNER_ID = 145L;

    private MockRestServiceServer mockServer;

    @Before
    public void setup() throws Exception {
        when(uniqService.generate()).thenReturn(UNIQ);
        mockServer = createMockServerByRequest(PartnerMethod.UPDATE_ORDER_DS);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void testResponseValidationFailed() throws Exception {
        prepareMockServerXmlScenario(mockServer, GATEWAY_URL,
            "fixtures/request/delivery/update_order/delivery_update_order.xml",
            "fixtures/response/delivery/delivery_not_valid_response.xml");

        assertThatThrownBy(() -> executor.tryExecute(getRequest(), Collections.emptySet()))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    public void testResponseContentTypeTextHtmlSuccess() throws Exception {
        ResponseCreator taskResponseCreator = withStatus(OK)
            .contentType(TEXT_HTML)
            .body(getFileContent("fixtures/response/delivery/update_order/delivery_update_order.xml"));

        mockServer.expect(requestTo(GATEWAY_URL)).andRespond(taskResponseCreator);
        executor.tryExecute(getRequest(), Collections.emptySet());
    }

    @Test
    public void testResponseSimilarContentTypeSuccess() throws Exception {
        ResponseCreator taskResponseCreator = withStatus(OK)
            .contentType(new MediaType("application", "xml", Charset.forName("utf-8")))
            .body(getFileContent("fixtures/response/delivery/update_order/delivery_update_order.xml"));

        mockServer.expect(requestTo(GATEWAY_URL)).andRespond(taskResponseCreator);

        executor.tryExecute(getRequest(), Collections.emptySet());
    }

    @Test
    public void testErrorResponseInvalidContentTypeIgnored() throws Exception {
        ResponseCreator taskResponseCreator = withStatus(INTERNAL_SERVER_ERROR)
            .contentType(APPLICATION_JSON)
            .body(getFileContent("fixtures/response/delivery/update_order/delivery_update_order.xml"));

        mockServer.expect(requestTo(GATEWAY_URL)).andRespond(taskResponseCreator);

        assertThatThrownBy(() -> executor.tryExecute(getRequest(), Collections.emptySet()))
            .isInstanceOf(PartnerInteractionException.class);
    }

    private UpdateOrderRequest getRequest() throws IOException {
        return jsonMapper.readValue(
            getFileContent("fixtures/executors/update_order/update_order_task_message.json"),
            UpdateOrderRequest.class);
    }
}
