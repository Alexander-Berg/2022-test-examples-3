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

import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.FailedFreezeStock;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.OutboundItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.OutboundMeta;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemFreeze;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SimpleStock;
import ru.yandex.market.fulfillment.stockstorage.client.entity.enums.SSStockType;
import ru.yandex.market.fulfillment.stockstorage.client.entity.exception.StockStorageDuplicateFreezeException;
import ru.yandex.market.fulfillment.stockstorage.client.entity.exception.StockStorageFreezeNotFoundException;
import ru.yandex.market.fulfillment.stockstorage.client.entity.exception.StockStorageServerRuntimeException;
import ru.yandex.market.fulfillment.stockstorage.client.entity.exception.StockStorageUnexpectedBehaviourRuntimeException;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.AvailableStockResponse;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.StockFreezingResponse;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType;
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
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageOutboundRestClient.FREEZES;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageOutboundRestClient.GET_AVAILABLE;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageOutboundRestClient.GET_AVAILABLE_ITEMS;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageOutboundRestClient.OUTBOUND;
import static ru.yandex.market.fulfillment.stockstorage.client.TestContextConfiguration.SERVICE_TICKET;
import static ru.yandex.market.logistics.util.client.HttpTemplate.SERVICE_TICKET_HEADER;

@ExtendWith(SpringExtension.class)
@Import({StockStorageClientConfiguration.class, TestContextConfiguration.class})
@TestPropertySource(properties = {"fulfillment.stockstorage.api.host=http://rkn.gov.ru",
        "fulfillment.stockstorage.tvm.client.id=" + SERVICE_TICKET})
public class StockStorageOutboundClientTest extends BaseIntegrationTest {

    private static final StockType STOCK_TYPE = StockType.EXPIRED;
    private final long outboundId = 12345L;
    private final int warehouseId = 2;
    private final int amount1 = 10;
    private final int amount2 = 20;
    private final String shopSku1 = "SKU_1";
    private final String shopSku2 = "SKU_2";
    private final long vendorId1 = 100L;
    private final long vendorId2 = 200L;
    private final long refreezeId = 123L;
    private final SSItem item1 = SSItem.of(shopSku1, vendorId1, warehouseId);
    private final SSItem item2 = SSItem.of(shopSku2, vendorId2, warehouseId);

    @Value("${fulfillment.stockstorage.api.host:}")
    private String host;

    @Autowired
    private StockStorageClientConfiguration configuration;

    private MockRestServiceServer mockServer;
    @Autowired
    private StockStorageOutboundClient client;

    @BeforeEach
    public void init() {
        mockServer = MockRestServiceServer.createServer(configuration.restTemplate());
    }

    @Test
    public void getAvailable() throws Exception {
        mockServer.expect(requestTo(buildPath(OUTBOUND, GET_AVAILABLE)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent("fixture/outbound/available_request.json")))
                .andRespond(withStatus(OK)
                        .contentType(APPLICATION_JSON)
                        .body(extractFileContent("fixture/outbound/available_response.json")));
        AvailableStockResponse response = client.getAvailable(vendorId1, warehouseId, STOCK_TYPE);
        mockServer.verify();
        Assert.assertEquals(STOCK_TYPE, response.getStockType());
        Assert.assertEquals(getExpectedAvailableStocks(), response.getStocks());
    }

    @Test
    public void getAvailableBadRequest() throws Exception {
        mockServer.expect(requestTo(buildPath(OUTBOUND, GET_AVAILABLE)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent("fixture/outbound/available_request.json")))
                .andRespond(withStatus(BAD_REQUEST));

        assertThrows(StockStorageUnexpectedBehaviourRuntimeException.class,
                () -> client.getAvailable(vendorId1, warehouseId, STOCK_TYPE));

        mockServer.verify();
    }

    @Test
    public void getAvailableGatewayTimeout() throws Exception {
        mockServer.expect(requestTo(buildPath(OUTBOUND, GET_AVAILABLE)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent("fixture/outbound/available_request.json")))
                .andRespond(withStatus(GATEWAY_TIMEOUT));

        assertThrows(StockStorageServerRuntimeException.class,
                () -> client.getAvailable(vendorId1, warehouseId, STOCK_TYPE));

        mockServer.verify();
    }

    @Test
    public void getAvailableBySkuList() throws Exception {
        mockServer.expect(requestTo(buildPath(OUTBOUND, GET_AVAILABLE)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent("fixture/outbound/available_by_sku_list_request.json")))
                .andRespond(withStatus(OK)
                        .contentType(APPLICATION_JSON)
                        .body(extractFileContent("fixture/outbound/available_response.json")));
        AvailableStockResponse response = client.getAvailable(vendorId1, warehouseId, STOCK_TYPE, getShopSkuList());
        mockServer.verify();
        Assert.assertEquals(STOCK_TYPE, response.getStockType());
        Assert.assertEquals(getExpectedAvailableStocks(), response.getStocks());
    }

    @Test
    public void getAvailableBySkuListBadRequest() throws Exception {
        mockServer.expect(requestTo(buildPath(OUTBOUND, GET_AVAILABLE)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent("fixture/outbound/available_by_sku_list_request.json")))
                .andRespond(withStatus(BAD_REQUEST));
        assertThrows(StockStorageUnexpectedBehaviourRuntimeException.class,
                () -> client.getAvailable(vendorId1, warehouseId, STOCK_TYPE, getShopSkuList()));

        mockServer.verify();
    }

    @Test
    public void getAvailableBySkuListGatewayTimeout() throws Exception {
        mockServer.expect(requestTo(buildPath(OUTBOUND, GET_AVAILABLE)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent("fixture/outbound/available_by_sku_list_request.json")))
                .andRespond(withStatus(GATEWAY_TIMEOUT));
        assertThrows(StockStorageServerRuntimeException.class,
                () -> client.getAvailable(vendorId1, warehouseId, STOCK_TYPE, getShopSkuList()));

        mockServer.verify();
    }

    @Test
    public void getAvailableByItemList() throws Exception {
        mockServer.expect(requestTo(buildPath(OUTBOUND, GET_AVAILABLE_ITEMS)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent("fixture/outbound/available_by_item_list_request.json")))
                .andRespond(withStatus(OK)
                        .contentType(APPLICATION_JSON)
                        .body(extractFileContent("fixture/outbound/available_response.json")));
        AvailableStockResponse response = client.getAvailable(
                List.of(
                        SSItem.of(shopSku1, vendorId1, warehouseId),
                        SSItem.of(shopSku2, vendorId2, warehouseId)
                ),
                STOCK_TYPE);
        mockServer.verify();
        Assert.assertEquals(STOCK_TYPE, response.getStockType());
        Assert.assertEquals(getExpectedAvailableStocks(), response.getStocks());
    }

    @Test
    public void getAvailableByIteemListBadRequest() throws Exception {
        mockServer.expect(requestTo(buildPath(OUTBOUND, GET_AVAILABLE_ITEMS)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent("fixture/outbound/available_by_item_list_request.json")))
                .andRespond(withStatus(BAD_REQUEST));
        assertThrows(StockStorageUnexpectedBehaviourRuntimeException.class,
                () -> client.getAvailable(
                        List.of(
                                SSItem.of(shopSku1, vendorId1, warehouseId),
                                SSItem.of(shopSku2, vendorId2, warehouseId)
                        ),
                        STOCK_TYPE));

        mockServer.verify();
    }

    @Test
    public void getAvailableByItemListGatewayTimeout() throws Exception {
        mockServer.expect(requestTo(buildPath(OUTBOUND, GET_AVAILABLE_ITEMS)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent("fixture/outbound/available_by_item_list_request.json")))
                .andRespond(withStatus(GATEWAY_TIMEOUT));
        assertThrows(StockStorageServerRuntimeException.class,
                () -> client.getAvailable(
                        List.of(
                                SSItem.of(shopSku1, vendorId1, warehouseId),
                                SSItem.of(shopSku2, vendorId2, warehouseId)
                        ),
                        STOCK_TYPE));

        mockServer.verify();
    }

    @Test
    public void getFreeze() {
        mockServer.expect(requestTo(buildPath(OUTBOUND, outboundId, FREEZES)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(OK)
                        .contentType(APPLICATION_JSON)
                        .body(extractFileContent("fixture/outbound/get_freeze_response.json")));
        List<SSItemFreeze> freeze = client.getFreezes(String.valueOf(outboundId));
        mockServer.verify();
        Assertions.assertThat(freeze).hasSize(2);
        Assert.assertEquals(getExpectedFreeze(), freeze);
    }

    @Test
    public void getFreezeEmptyResponse() {
        mockServer.expect(requestTo(buildPath(OUTBOUND, outboundId, FREEZES)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(OK)
                        .contentType(APPLICATION_JSON)
                        .body("{\"freezes\":[]}"));
        List<SSItemFreeze> freeze = client.getFreezes(String.valueOf(outboundId));
        mockServer.verify();
        Assertions.assertThat(freeze).isEmpty();
    }

    @Test
    public void getFreezeBadRequest() {
        mockServer.expect(requestTo(buildPath(OUTBOUND, outboundId, FREEZES)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(BAD_REQUEST));
        assertThrows(StockStorageUnexpectedBehaviourRuntimeException.class,
                () -> client.getFreezes(String.valueOf(outboundId)));

        mockServer.verify();
    }

    @Test
    public void getFreezeGatewayTimeout() {
        mockServer.expect(requestTo(buildPath(OUTBOUND, outboundId, FREEZES)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(GATEWAY_TIMEOUT));
        assertThrows(StockStorageServerRuntimeException.class,
                () -> client.getFreezes(String.valueOf(outboundId)));
        mockServer.verify();
    }

    @Test
    public void freezeSuccessful() throws Exception {
        mockServer.expect(requestTo(buildPath(OUTBOUND)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent("fixture/outbound/freeze_request.json")))
                .andRespond(withStatus(OK)
                        .contentType(APPLICATION_JSON)
                        .body(extractFileContent("fixture/outbound/freeze_successful_response.json")));
        StockFreezingResponse response = client.freezeStocks(getOutboundMeta(), getOutboundItems());
        mockServer.verify();
        Assert.assertNull(response.getNotEnoughToFreeze());
        Assert.assertEquals(Long.valueOf(outboundId), response.getOutboundId());
    }

    @Test
    public void freezeSuccessfulWithInterval() throws Exception {
        mockServer.expect(requestTo(buildPath(OUTBOUND)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent("fixture/outbound/freeze_with_interval_request.json")))
                .andRespond(withStatus(OK)
                        .contentType(APPLICATION_JSON)
                        .body(extractFileContent("fixture/outbound/freeze_successful_response.json")));
        StockFreezingResponse response = client.freezeStocks(getOutboundMetaWithInterval(), getOutboundItems());
        mockServer.verify();
        Assert.assertNull(response.getNotEnoughToFreeze());
        Assert.assertEquals(Long.valueOf(outboundId), response.getOutboundId());
    }

    @Test
    public void freezeFailedDueToNotEnoughStocks() throws Exception {
        mockServer.expect(requestTo(buildPath(OUTBOUND)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent("fixture/outbound/freeze_request.json")))
                .andRespond(withStatus(OK)
                        .contentType(APPLICATION_JSON)
                        .body(extractFileContent("fixture/outbound/freeze_not_enough_response.json")));
        StockFreezingResponse response = client.freezeStocks(getOutboundMeta(), getOutboundItems());
        mockServer.verify();
        Assert.assertEquals(getExpectedFailedStocks(), response.getNotEnoughToFreeze());
        Assert.assertEquals(Long.valueOf(outboundId), response.getOutboundId());
    }

    @Test
    public void freezeBadRequest() throws Exception {
        mockServer.expect(requestTo(buildPath(OUTBOUND)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent("fixture/outbound/freeze_request.json")))
                .andRespond(withStatus(BAD_REQUEST));
        assertThrows(StockStorageUnexpectedBehaviourRuntimeException.class,
                () -> client.freezeStocks(getOutboundMeta(), getOutboundItems()));
        mockServer.verify();
    }

    @Test
    public void freezeFailDuplicateFreeze() throws Exception {
        mockServer.expect(requestTo(buildPath(OUTBOUND)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent("fixture/outbound/freeze_request.json")))
                .andRespond(withStatus(HttpStatus.CONFLICT));
        assertThrows(StockStorageDuplicateFreezeException.class,
                () -> client.freezeStocks(getOutboundMeta(), getOutboundItems()));
        mockServer.verify();
    }

    @Test
    public void freezeGatewayTimeout() throws Exception {
        mockServer.expect(requestTo(buildPath(OUTBOUND)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent("fixture/outbound/freeze_request.json")))
                .andRespond(withStatus(GATEWAY_TIMEOUT));
        assertThrows(StockStorageServerRuntimeException.class,
                () -> client.freezeStocks(getOutboundMeta(), getOutboundItems()));
        mockServer.verify();
    }

    @Test
    public void refreezeSuccessful() throws Exception {
        mockServer.expect(requestTo(buildPath(OUTBOUND)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent("fixture/outbound/refreeze_request.json")))
                .andRespond(withStatus(OK)
                        .contentType(APPLICATION_JSON)
                        .body(extractFileContent("fixture/outbound/freeze_successful_response.json")));
        StockFreezingResponse response = client.refreezeStocks(getOutboundMeta(), getOutboundItems(), refreezeId);
        mockServer.verify();
        Assert.assertNull(response.getNotEnoughToFreeze());
        Assert.assertEquals(Long.valueOf(outboundId), response.getOutboundId());
    }

    @Test
    public void refreezeFailedDueToNotEnoughStocks() throws Exception {
        mockServer.expect(requestTo(buildPath(OUTBOUND)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent("fixture/outbound/refreeze_request.json")))
                .andRespond(withStatus(OK)
                        .contentType(APPLICATION_JSON)
                        .body(extractFileContent("fixture/outbound/freeze_not_enough_response.json")));
        StockFreezingResponse response = client.refreezeStocks(getOutboundMeta(), getOutboundItems(), refreezeId);
        mockServer.verify();
        Assert.assertEquals(getExpectedFailedStocks(), response.getNotEnoughToFreeze());
        Assert.assertEquals(Long.valueOf(outboundId), response.getOutboundId());
    }

    @Test
    public void refreezeBadRequest() throws Exception {
        mockServer.expect(requestTo(buildPath(OUTBOUND)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent("fixture/outbound/refreeze_request.json")))
                .andRespond(withStatus(BAD_REQUEST));
        assertThrows(StockStorageUnexpectedBehaviourRuntimeException.class,
                () -> client.refreezeStocks(getOutboundMeta(), getOutboundItems(), refreezeId));

        mockServer.verify();
    }

    @Test
    public void refreezeGatewayTimeout() throws Exception {
        mockServer.expect(requestTo(buildPath(OUTBOUND)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent("fixture/outbound/refreeze_request.json")))
                .andRespond(withStatus(GATEWAY_TIMEOUT));
        assertThrows(StockStorageServerRuntimeException.class,
                () -> client.refreezeStocks(getOutboundMeta(), getOutboundItems(), refreezeId));

        mockServer.verify();
    }

    @Test
    public void unfreeze() throws StockStorageFreezeNotFoundException {
        mockServer.expect(requestTo(buildPath(OUTBOUND, outboundId)))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(OK));
        client.unfreezeStocks(outboundId);
        mockServer.verify();
    }

    @Test
    public void unfreezeBadRequest() throws StockStorageFreezeNotFoundException {
        mockServer.expect(requestTo(buildPath(OUTBOUND, outboundId)))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(BAD_REQUEST));
        assertThrows(StockStorageUnexpectedBehaviourRuntimeException.class,
                () -> client.unfreezeStocks(outboundId));
        mockServer.verify();
    }

    @Test
    public void unfreezeGatewayTimeout() throws StockStorageFreezeNotFoundException {
        mockServer.expect(requestTo(buildPath(OUTBOUND, outboundId)))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(GATEWAY_TIMEOUT));
        assertThrows(StockStorageServerRuntimeException.class,
                () -> client.unfreezeStocks(outboundId));
        mockServer.verify();
    }

    @Test
    public void unfreezeNoFreeze() throws StockStorageFreezeNotFoundException {
        mockServer.expect(requestTo(buildPath(OUTBOUND, outboundId)))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThrows(StockStorageFreezeNotFoundException.class,
                () -> client.unfreezeStocks(outboundId));
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

    private List<String> getShopSkuList() {
        return Arrays.asList(shopSku1, shopSku2);
    }

    private OutboundMeta getOutboundMetaWithInterval() {
        return OutboundMeta.of(
                outboundId, warehouseId, STOCK_TYPE,
                DateTimeInterval.fromFormattedValue("2017-03-29/2018-09-01T22:15:26")
        );
    }

    private OutboundMeta getOutboundMeta() {
        return OutboundMeta.of(outboundId, warehouseId, STOCK_TYPE);
    }

    private List<OutboundItem> getOutboundItems() {
        return Arrays.asList(
                OutboundItem.of(vendorId1, shopSku1, amount1),
                OutboundItem.of(vendorId2, shopSku2, amount2)
        );
    }

    private List<SimpleStock> getExpectedAvailableStocks() {
        return Arrays.asList(
                new SimpleStock(shopSku1, vendorId1, shopSku1, amount1, warehouseId),
                new SimpleStock(shopSku2, vendorId2, shopSku2, amount2, warehouseId)
        );
    }

    private List<FailedFreezeStock> getExpectedFailedStocks() {
        return Arrays.asList(
                FailedFreezeStock.of(shopSku1, vendorId1, warehouseId, amount1, 100),
                FailedFreezeStock.of(shopSku2, vendorId2, warehouseId, amount2, 100)
        );
    }

    private List<SSItemFreeze> getExpectedFreeze() {
        return Arrays.asList(
                SSItemFreeze.of(item1, 10, false, 123, SSStockType.FIT, true),
                SSItemFreeze.of(item2, 20, true, 456, SSStockType.DEFECT, false)
        );
    }
}
