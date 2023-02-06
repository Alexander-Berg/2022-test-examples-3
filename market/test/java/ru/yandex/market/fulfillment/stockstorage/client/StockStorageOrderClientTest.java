package ru.yandex.market.fulfillment.stockstorage.client;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;

import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemAmount;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemFreeze;
import ru.yandex.market.fulfillment.stockstorage.client.entity.enums.SSStockType;
import ru.yandex.market.fulfillment.stockstorage.client.entity.exception.StockStorageDuplicateFreezeException;
import ru.yandex.market.fulfillment.stockstorage.client.entity.exception.StockStorageFreezeNotFoundException;
import ru.yandex.market.fulfillment.stockstorage.client.entity.exception.StockStorageServerRuntimeException;
import ru.yandex.market.fulfillment.stockstorage.client.entity.exception.StockStorageUnexpectedBehaviourRuntimeException;
import ru.yandex.market.fulfillment.stockstorage.client.entity.exception.StockStorageValidationException;
import ru.yandex.market.fulfillment.stockstorage.client.entity.type.StockUnfreezingStrategyType;
import ru.yandex.market.fulfillment.stockstorage.client.entity.type.StockUpdatingStrategyType;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageOrderRestClient.FREEZES;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageOrderRestClient.GET_AVAILABLE;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageOrderRestClient.GUI;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageOrderRestClient.ORDER;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageOrderRestClient.WAREHOUSE;
import static ru.yandex.market.fulfillment.stockstorage.client.TestContextConfiguration.SERVICE_TICKET;
import static ru.yandex.market.logistics.util.client.HttpTemplate.SERVICE_TICKET_HEADER;

@ExtendWith(SpringExtension.class)
@Import({StockStorageClientConfiguration.class, TestContextConfiguration.class})
@TestPropertySource(properties = {"fulfillment.stockstorage.api.host=http://rkn.gov.ru",
        "fulfillment.stockstorage.tvm.client.id=" + SERVICE_TICKET})
public class StockStorageOrderClientTest extends BaseIntegrationTest {

    public static final String FIXTURE_ORDER_DIR = "fixture/order/";
    private final String shopSku1 = "SKU_1";
    private final String shopSku2 = "SKU_2";
    private final long vendorId1 = 100L;
    private final long vendorId2 = 200L;
    private final int warehouseId = 2;
    private final SSItem item1 = SSItem.of(shopSku1, vendorId1, warehouseId);
    private final SSItem item2 = SSItem.of(shopSku2, vendorId2, warehouseId);
    private final String orderId = "12345";
    private final List<SSItem> stocks = Arrays.asList(item1, item2);

    @Value("${fulfillment.stockstorage.api.host:}")
    private String host;

    @Autowired
    private StockStorageClientConfiguration configuration;

    private MockRestServiceServer mockServer;

    @Autowired
    private StockStorageOrderClient client;

    @BeforeEach
    public void init() {
        mockServer = MockRestServiceServer.createServer(configuration.restTemplate());
    }

    @Test
    public void getAvailable() {

        mockServer.expect(requestTo(buildPath(ORDER, GET_AVAILABLE)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent(FIXTURE_ORDER_DIR + "available_request.json")))
                .andRespond(withStatus(OK)
                        .contentType(APPLICATION_JSON)
                        .body(extractFileContent(FIXTURE_ORDER_DIR + "available_response.json")));
        List<SSItemAmount> response = client.getAvailableAmounts(stocks);
        mockServer.verify();
        Assertions
                .assertThat(response)
                .hasSize(2);
        Assert.assertEquals(getExpectedAvailableStocks(), response);
    }

    @Test
    public void getAvailableBadRequest() {
        mockServer.expect(requestTo(buildPath(ORDER, GET_AVAILABLE)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent(FIXTURE_ORDER_DIR + "available_request.json")))
                .andRespond(withStatus(BAD_REQUEST));

        assertThrows(StockStorageUnexpectedBehaviourRuntimeException.class,
                () -> client.getAvailableAmounts(stocks));

        mockServer.verify();
    }

    @Test
    public void getAvailableGatewayTimeout() {
        mockServer.expect(requestTo(buildPath(ORDER, GET_AVAILABLE)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent(FIXTURE_ORDER_DIR + "available_request.json")))
                .andRespond(withStatus(GATEWAY_TIMEOUT));

        assertThrows(StockStorageServerRuntimeException.class,
                () -> client.getAvailableAmounts(stocks));

        mockServer.verify();
    }

    @Test
    public void getFreeze() {
        mockServer.expect(requestTo(buildPath(ORDER, orderId, FREEZES)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(OK)
                        .contentType(APPLICATION_JSON)
                        .body(extractFileContent(FIXTURE_ORDER_DIR + "get_freeze_response.json")));
        List<SSItemFreeze> freeze = client.getFreezes(orderId);
        mockServer.verify();
        Assertions.assertThat(freeze).hasSize(2);
        Assert.assertEquals(getExpectedFreeze(), freeze);
    }

    @Test
    public void getFreezeEmptyResponse() {
        mockServer.expect(requestTo(buildPath(ORDER, orderId, FREEZES)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(OK)
                        .contentType(APPLICATION_JSON)
                        .body("{\"freezes\":[]}"));
        List<SSItemFreeze> freeze = client.getFreezes(orderId);
        mockServer.verify();
        Assertions.assertThat(freeze).isEmpty();
    }

    @Test
    public void getFreezeBadRequest() {
        mockServer.expect(requestTo(buildPath(ORDER, orderId, FREEZES)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(BAD_REQUEST));

        assertThrows(StockStorageUnexpectedBehaviourRuntimeException.class,
                () -> client.getFreezes(orderId));

        mockServer.verify();
    }

    @Test
    public void getFreezeGatewayTimeout() {
        mockServer.expect(requestTo(buildPath(ORDER, orderId, FREEZES)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(GATEWAY_TIMEOUT));

        assertThrows(StockStorageServerRuntimeException.class,
                () -> client.getFreezes(orderId));

        mockServer.verify();
    }

    @Test
    public void freezeSuccessful() {
        mockServer.expect(requestTo(buildPath(ORDER)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent(FIXTURE_ORDER_DIR + "freeze_request.json")))
                .andRespond(withStatus(OK));
        client.freeze(getExpectedFreezeStocks(), orderId);
        mockServer.verify();
    }

    @Test
    public void freezeFailedDueToNotEnoughStocks() {
        mockServer.expect(requestTo(buildPath(ORDER)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent(FIXTURE_ORDER_DIR + "freeze_request.json")))
                .andRespond(withStatus(BAD_REQUEST));

        assertThrows(StockStorageValidationException.class,
                () -> client.freeze(getExpectedFreezeStocks(), orderId));

        mockServer.verify();
    }

    @Test
    public void freezeFailDuplicateFreeze() {
        mockServer.expect(requestTo(buildPath(ORDER)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent(FIXTURE_ORDER_DIR + "freeze_request.json")))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThrows(StockStorageDuplicateFreezeException.class,
                () -> client.freeze(getExpectedFreezeStocks(), orderId));

        mockServer.verify();
    }

    @Test
    public void unfreeze() throws StockStorageFreezeNotFoundException {
        mockServer.expect(requestTo(buildPath(ORDER, orderId).concat("?cancel=false")))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(OK));
        client.unfreezeStocks(orderId);
        mockServer.verify();
    }

    @Test
    public void unfreezeCancel() throws StockStorageFreezeNotFoundException {
        mockServer.expect(requestTo(buildPath(ORDER, orderId).concat("?cancel=true")))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(OK));
        client.unfreezeStocks(orderId, true);
        mockServer.verify();
    }

    @Test
    public void unfreezeCancelFalse() throws StockStorageFreezeNotFoundException {
        mockServer.expect(requestTo(buildPath(ORDER, orderId).concat("?cancel=false")))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(OK));
        client.unfreezeStocks(orderId, false);
        mockServer.verify();
    }


    @Test
    public void unfreezeBadRequest() throws StockStorageFreezeNotFoundException {
        mockServer.expect(requestTo(buildPath(ORDER, orderId).concat("?cancel=false")))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(BAD_REQUEST));

        assertThrows(StockStorageUnexpectedBehaviourRuntimeException.class,
                () -> client.unfreezeStocks(orderId));

        mockServer.verify();
    }

    @Test
    public void unfreezeNoFreeze() throws StockStorageFreezeNotFoundException {
        mockServer.expect(requestTo(buildPath(ORDER, orderId).concat("?cancel=false")))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThrows(StockStorageFreezeNotFoundException.class,
                () -> client.unfreezeStocks(orderId));

        mockServer.verify();
    }

    @Test
    public void setUnfreezeStockStrategy() {
        mockServer.expect(requestTo(buildPath(GUI, WAREHOUSE)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent(FIXTURE_ORDER_DIR + "unfreeze_strategy.json")))
                .andRespond(withStatus(OK));
        client.setStockUnfreezingStrategy(123, StockUnfreezingStrategyType.CHECK_STOCK_WAS_UPDATED);
        mockServer.verify();
    }

    @Test
    public void setUpdateStockStrategy() {
        mockServer.expect(requestTo(buildPath(GUI, WAREHOUSE)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent(FIXTURE_ORDER_DIR + "update_strategy.json")))
                .andRespond(withStatus(OK));
        client.setStockUpdatingStrategy(123, StockUpdatingStrategyType.CHECK_ONLY_DATE);
        mockServer.verify();
    }


    private RequestMatcher checkBody(String expectedJson) {
        return content().string(new JsonMatcher(expectedJson));
    }

    private String buildPath(Object... parts) {
        StringBuilder path = new StringBuilder(host);
        for (Object part : parts) {
            if (!part.toString().startsWith("/")) {
                path.append("/");
            }
            path.append(part);
        }
        return path.toString();
    }

    private List<SSItemAmount> getExpectedFreezeStocks() {
        return Arrays.asList(
                SSItemAmount.of(item1, 10, true),
                SSItemAmount.of(item2, 20)
        );
    }


    private List<SSItemAmount> getExpectedAvailableStocks() {
        return Arrays.asList(
                SSItemAmount.of(item1, 10),
                SSItemAmount.of(item2, 20)
        );
    }

    private List<SSItemFreeze> getExpectedFreeze() {
        return Arrays.asList(
                SSItemFreeze.of(item1, 10, false, 123, SSStockType.FIT, false),
                SSItemFreeze.of(item2, 20, true, 456, SSStockType.DEFECT, true)
        );
    }
}
