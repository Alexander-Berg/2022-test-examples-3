package ru.yandex.market.fulfillment.stockstorage.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.ResponseCreator;

import ru.yandex.market.fulfillment.stockstorage.client.entity.request.Source;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemStocks;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.fulfillment.stockstorage.client.TestContextConfiguration.SERVICE_TICKET;
import static ru.yandex.market.logistics.util.client.HttpTemplate.SERVICE_TICKET_HEADER;

@ExtendWith(SpringExtension.class)
@Import({StockStorageClientConfiguration.class, TestContextConfiguration.class})
@TestPropertySource(properties = {"fulfillment.stockstorage.api.host=http://rkn.gov.ru",
        "fulfillment.stockstorage.tvm.client.id=" + SERVICE_TICKET})
public class StockStorageStocksClientTest extends BaseIntegrationTest {

    @Value("${fulfillment.stockstorage.api.host:}")
    private String uri;

    @Autowired
    private StockStorageClientConfiguration configuration;

    @Autowired
    private StockStorageStocksClient client;
    private MockRestServiceServer mockServer;

    @BeforeEach
    public void init() {
        mockServer = MockRestServiceServer.createServer(configuration.restTemplate());
    }

    @Test
    public void pushStocks() {
        String path = "/stocks";
        ResponseCreator responseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON);
        mockServer.expect(requestTo(uri + path))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent("fixture/push_stocks/available_request.json")))
                .andRespond(responseCreator);

        client.pushStocks(new Source(1), new ItemStocks[]{});

        mockServer.verify();
    }

    private RequestMatcher checkBody(String expectedJson) {
        return content().string(new JsonMatcher(expectedJson));
    }
}
