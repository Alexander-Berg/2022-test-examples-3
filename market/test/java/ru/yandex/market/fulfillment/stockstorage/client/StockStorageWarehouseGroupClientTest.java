package ru.yandex.market.fulfillment.stockstorage.client;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.fulfillment.stockstorage.client.entity.request.GroupWarehouse;
import ru.yandex.market.fulfillment.stockstorage.client.entity.request.SaveStocksWarehouseGroupRequest;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.fulfillment.stockstorage.client.TestContextConfiguration.SERVICE_TICKET;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.util.client.HttpTemplate.SERVICE_TICKET_HEADER;

@ExtendWith(SpringExtension.class)
@Import({StockStorageClientConfiguration.class, TestContextConfiguration.class})
@TestPropertySource(properties = {"fulfillment.stockstorage.api.host=http://rkn.gov.ru",
        "fulfillment.stockstorage.tvm.client.id=" + SERVICE_TICKET})
public class StockStorageWarehouseGroupClientTest {
    private static final String JSONS_DIR = "fixture/stocks_warehouse_group/";
    @Autowired
    private StockStorageWarehouseGroupClient client;
    @Autowired
    private RestTemplate restTemplate;
    @Value("${fulfillment.stockstorage.api.host:}")
    private String uri;

    private MockRestServiceServer mockServer;

    @BeforeEach
    public void init() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void testSaveGroupSuccess() {
        mockServer.expect(requestTo(uri + "/stocks-warehouse-groups"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(content().string(new JsonMatcher(
                        extractFileContent(JSONS_DIR + "save_request.json"), JSONCompareMode.NON_EXTENSIBLE)))
                .andRespond(withStatus(OK));
        client.saveGroup(new SaveStocksWarehouseGroupRequest(1, 10, List.of(
                new GroupWarehouse(100, 10), new GroupWarehouse(200, 20), new GroupWarehouse(300, 30))));
        mockServer.verify();
    }

    @Test
    void testDeleteGroupSuccess() {
        long groupId = 1L;
        mockServer.expect(requestTo(uri + "/stocks-warehouse-groups/" + groupId))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(OK));
        client.deleteGroup(groupId);
        mockServer.verify();
    }

    @Test
    void testAddWarehouseToGroupSuccess() {
        long groupId = 1L;
        mockServer.expect(requestTo(uri + "/stocks-warehouse-groups/" + groupId + "/add-warehouse"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(content().string(new JsonMatcher(
                        extractFileContent(JSONS_DIR + "add_warehouse.json"), JSONCompareMode.NON_EXTENSIBLE)))
                .andRespond(withStatus(OK));
        client.addWarehouseToGroup(1, new GroupWarehouse(300, 30));
        mockServer.verify();
    }
}
