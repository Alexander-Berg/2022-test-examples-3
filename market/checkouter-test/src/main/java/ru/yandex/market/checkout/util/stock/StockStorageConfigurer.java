package ru.yandex.market.checkout.util.stock;


import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Lists;
import org.apache.curator.shaded.com.google.common.base.Throwables;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemAmount;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.GetStocksAmountResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

@TestComponent
public class StockStorageConfigurer {

    private final WireMockServer stockStorageMock;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StockStorageConfigurer(WireMockServer stockStorageMock) {
        this.stockStorageMock = stockStorageMock;
    }

    public void mockOkForFreeze() {
        stockStorageMock.stubFor(post(urlPathEqualTo("/order"))
                .willReturn(ResponseDefinitionBuilder.okForEmptyJson()));
    }

    public void mockOkForFreeze(List<SSItem> items) {
        MappingBuilder builder = post(urlPathEqualTo("/order"));
        for (SSItem item : items) {
            builder
                    .withRequestBody(matchingJsonPath(
                            String.format("$.items.[?(@.item.shopSku=='%s')].item.shopSku", item.getShopSku()),
                            containing(item.getShopSku())))
                    .withRequestBody(matchingJsonPath(
                            String.format("$.items[?(@.item.vendorId=='%d')].item.vendorId", item.getVendorId()),
                            containing(String.valueOf(item.getVendorId()))))
                    .withRequestBody(matchingJsonPath(
                            String.format("$.items[?(@.item.warehouseId=='%d')].item.warehouseId",
                                    item.getWarehouseId()),
                            containing(String.valueOf(item.getWarehouseId()))));
        }
        builder.willReturn(ResponseDefinitionBuilder.okForEmptyJson());
        stockStorageMock.stubFor(builder);
    }

    public void mockNoStocksForFreeze() {
        stockStorageMock.stubFor(post(urlPathEqualTo("/order"))
                .willReturn(ResponseDefinitionBuilder.responseDefinition().withStatus(400)));
    }

    public void mockRequestTimeoutForFreeze() {
        stockStorageMock.stubFor(post(urlPathEqualTo("/order"))
                .willReturn(ResponseDefinitionBuilder.responseDefinition().withFixedDelay(100).withStatus(503)));
    }

    public void mockOkForPreorderFreeze() {
        stockStorageMock.stubFor(post(urlPathEqualTo("/preorder"))
                .willReturn(ResponseDefinitionBuilder.okForEmptyJson()));
    }

    public void mockErrorForFreeze() {
        stockStorageMock.stubFor(post(urlPathEqualTo("/order"))
                .willReturn(ResponseDefinitionBuilder.responseDefinition().withStatus(500)));
    }

    public void mockErrorForPreorderFreeze() {
        stockStorageMock.stubFor(post(urlPathEqualTo("/preorder"))
                .willReturn(ResponseDefinitionBuilder.responseDefinition().withStatus(500)));
    }

    public void mockOkForRefreeze() {
        stockStorageMock.stubFor(post(urlPathEqualTo("/order"))
                .willReturn(ResponseDefinitionBuilder.okForEmptyJson()));
    }

    public void mockOkForUnfreezePreorder() {
        stockStorageMock.stubFor(delete(urlPathMatching("/preorder/\\d+"))
                .willReturn(ResponseDefinitionBuilder.okForEmptyJson()));
    }

    public void mockOkForUnfreeze() {
        stockStorageMock.stubFor(delete(urlPathMatching("/order/\\d+"))
                .willReturn(ResponseDefinitionBuilder.okForEmptyJson()));
    }

    public void mockOkForForceUnfreeze() {
        stockStorageMock.stubFor(post(urlPathMatching("/stocks/force-unfreeze"))
                .willReturn(ResponseDefinitionBuilder.okForEmptyJson()));
    }

    public void mockOkForUnfreeze(long orderId) {
        stockStorageMock.stubFor(delete(urlPathEqualTo("/order/" + orderId))
                .willReturn(ResponseDefinitionBuilder.okForEmptyJson()));

    }

    public void mockErrorForUnfreeze(long orderId) {
        stockStorageMock.stubFor(delete(urlPathEqualTo("/order/" + orderId))
                .willReturn(ResponseDefinitionBuilder.responseDefinition().withStatus(500)));

    }

    public void mockOkForCertificateUnfreeze() {
        stockStorageMock.stubFor(delete(urlPathMatching("/order/certificate-\\d+"))
                .willReturn(ResponseDefinitionBuilder.okForEmptyJson()));

    }

    public void mockErrorForCertificateUnfreeze() {
        stockStorageMock.stubFor(delete(urlPathMatching("/order/certificate-\\d+"))
                .willReturn(ResponseDefinitionBuilder.responseDefinition().withStatus(500)));

    }

    public void mockGetAvailableCount(SSItemAmount response, SSItemAmount... responses) {
        try {
            var builder = post("/order/getAvailableAmounts");
            var items = Lists.asList(response, responses);
            for (SSItemAmount item : items) {
                builder
                        .withRequestBody(matchingJsonPath(
                                String.format("$.items[?(@.shopSku=='%s')].shopSku", item.getItem().getShopSku()),
                                containing(item.getItem().getShopSku())))
                        .withRequestBody(matchingJsonPath(
                                String.format("$.items[?(@.vendorId=='%d')].vendorId", item.getItem().getVendorId()),
                                containing(String.valueOf(item.getItem().getVendorId()))))
                        .withRequestBody(matchingJsonPath(
                                String.format("$.items[?(@.warehouseId=='%d')].warehouseId",
                                        item.getItem().getWarehouseId()),
                                containing(String.valueOf(item.getItem().getWarehouseId()))));
            }
            stockStorageMock.stubFor(builder.willReturn(ResponseDefinitionBuilder.responseDefinition()
                    .withBody(objectMapper.writeValueAsString(GetStocksAmountResponse.of(items)))
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
            ));
        } catch (JsonProcessingException e) {
            throw Throwables.propagate(e);
        }
    }

    public void mockGetAvailableCount(List<SSItem> items, boolean preorder, List<SSItemAmount> response) {
        String url = (preorder ? "/preorder" : "/order") + "/getAvailableAmounts";

        try {
            MappingBuilder builder = post(url);
            for (SSItem item : items) {
                builder
                        .withRequestBody(matchingJsonPath(
                                String.format("$.items[?(@.shopSku=='%s')].shopSku", item.getShopSku()),
                                containing(item.getShopSku())))
                        .withRequestBody(matchingJsonPath(
                                String.format("$.items[?(@.vendorId=='%d')].vendorId", item.getVendorId()),
                                containing(String.valueOf(item.getVendorId()))))
                        .withRequestBody(matchingJsonPath(
                                String.format("$.items[?(@.warehouseId=='%d')].warehouseId", item.getWarehouseId()),
                                containing(String.valueOf(item.getWarehouseId()))));
            }
            builder.willReturn(ResponseDefinitionBuilder.responseDefinition()
                    .withBody(objectMapper.writeValueAsString(GetStocksAmountResponse.of(response)))
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
            );
            stockStorageMock.stubFor(builder);
        } catch (JsonProcessingException e) {
            throw Throwables.propagate(e);
        }
    }

    public void mockErrorForGetAvailableCount() {
        stockStorageMock.stubFor(post(urlPathEqualTo("/order/getAvailableAmounts"))
                .willReturn(ResponseDefinitionBuilder.responseDefinition().withStatus(500)));
    }

    public List<ServeEvent> getServeEvents() {
        return stockStorageMock.getServeEvents().getServeEvents();
    }

    public void resetRequests() {
        stockStorageMock.resetRequests();
    }

    public void resetMappings() {
        stockStorageMock.resetMappings();
    }
}
