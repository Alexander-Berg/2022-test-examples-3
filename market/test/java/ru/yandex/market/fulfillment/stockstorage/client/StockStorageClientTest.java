package ru.yandex.market.fulfillment.stockstorage.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.client.match.MockRestRequestMatchers;

import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Pager;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SimpleStock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageClientTests.checkBody;
import static ru.yandex.market.fulfillment.stockstorage.client.TestContextConfiguration.SERVICE_TICKET;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.util.client.HttpTemplate.SERVICE_TICKET_HEADER;

@ExtendWith(SpringExtension.class)
@Import({StockStorageClientConfiguration.class, TestContextConfiguration.class})
@TestPropertySource(properties = {"fulfillment.stockstorage.api.host=http://rkn.gov.ru",
        "fulfillment.stockstorage.tvm.client.id=" + SERVICE_TICKET})
public class StockStorageClientTest {

    public static final String FIXTURE_STOCK_DIR = "fixture/stock/";

    @Value("${fulfillment.stockstorage.api.host:}")
    private String uri;

    private DefaultResourceLoader resourceLoader;

    @Autowired
    private StockStorageClientConfiguration configuration;

    private MockRestServiceServer mockServer;

    @Autowired
    private StockStorageClient client;

    @BeforeEach
    public void init() {
        resourceLoader = new DefaultResourceLoader();
        mockServer = MockRestServiceServer.createServer(configuration.restTemplate());
    }

    @Test
    public void checkStocksAvailable() {
        String path = "/stocks/checkAvailable";
        ResponseCreator responseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(buildResponse(ResponseType.CHECK_AVAILABLE));
        mockServer.expect(requestTo(uri + path))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(responseCreator);

        List<SimpleStock> response = client.checkStocksAvailable(Collections.emptyList());

        mockServer.verify();
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(buildSimpleStock(), response.get(0));
    }

    @Test
    public void freezeStocks() {
        String path = "/stocks/freeze";
        ResponseCreator responseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON);
        mockServer.expect(requestTo(uri + path))
                .andExpect(method(HttpMethod.POST))
                .andExpect(checkBody(extractFileContent(FIXTURE_STOCK_DIR + "freeze_request.json")))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(responseCreator);

        client.freezeStocks(Arrays.asList(
                new SimpleStock("sku", 10L, "shop", 1, 1, true),
                new SimpleStock("sku2", 11L, "shop2", 1, 1)
        ), "orderId");

        mockServer.verify();
    }

    @Test
    public void updateFrozenStocks() {
        String path = "/stocks/refreeze";
        ResponseCreator responseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON);
        mockServer.expect(requestTo(uri + path))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(responseCreator);

        client.refreezeStocks(Collections.emptyList(), "orderId", 123);

        mockServer.verify();
    }

    @Test
    public void unfreezeStocks() {
        String path = "/stocks/unfreeze";
        ResponseCreator responseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON);
        mockServer.expect(requestTo(uri + path))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(responseCreator);

        client.unfreezeStocks(Collections.emptyList(), "orderId");

        mockServer.verify();
    }

    @Test
    public void unfreezeStocksWithDelay() {
        String path = "/stocks/unfreeze";
        ResponseCreator responseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON);
        mockServer.expect(requestTo(uri + path))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(responseCreator);

        client.unfreezeStocks(Collections.emptyList(), "orderId", 0);

        mockServer.verify();
    }

    @Test
    public void forceUnfreezeStocks() {
        String path = "/stocks/force-unfreeze";
        ResponseCreator responseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON);
        mockServer.expect(requestTo(uri + path))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(responseCreator);

        client.forceUnfreezeStocks("1", "comment");
        mockServer.verify();
    }

    @Test
    public void getCurrentStocksByVendors() {
        String path = "/stocks/byVendors";
        ResponseCreator responseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(buildResponse(ResponseType.GET_CURRENT_STOCKS_BY_VENDOR));
        mockServer.expect(requestTo(uri + path))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(responseCreator);

        client.getCurrentStocksByVendors(Arrays.asList(110L, 98L), new Pager(900, 0));

        mockServer.verify();
    }

    @Test
    public void getAllCurrentStocksByVendors() {
        String path = "/stocks/allByVendors";
        ResponseCreator responseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(buildResponse(ResponseType.GET_CURRENT_STOCKS_BY_VENDOR));
        mockServer.expect(requestTo(uri + path))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(responseCreator);

        client.getAllCurrentStocksByVendors(Arrays.asList(110L, 98L));

        mockServer.verify();
    }

    private SimpleStock buildSimpleStock() {
        return new SimpleStock("omfg", 988L, "sparc_me", 300, 1);
    }

    private Resource buildResponse(ResponseType responseType) {
        String path = getClass().getResource("/fixture/client/response/" + responseType + ".json").toString();
        return resourceLoader.getResource(path);
    }

    private enum ResponseType {
        CHECK_AVAILABLE,
        GET_CURRENT_STOCKS_BY_VENDOR;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

}
