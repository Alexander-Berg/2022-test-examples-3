package ru.yandex.market.logistics.cte.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static java.lang.ClassLoader.getSystemResourceAsStream;

public class FulfillmentCteClientWebClientTest extends BaseFulfillmentCteClientTest {

    private FulfillmentCteClientApi clientApi;

    private static MockWebServer server;

    @BeforeAll
    public static void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterAll
    public static void tearDown() throws IOException {
        server.shutdown();
    }

    @BeforeEach
    public void init() {
        String url = server.url("/").toString();
        clientApi = new FulfillmentCteClientWebClient(cteWebClient(url), url);
    }

    @Test
    public void evaluateResupplyItem() {
        server.enqueue(new MockResponse()
            .setBody(getCteResponse("put_evaluate_resupply_item.json"))
            .addHeader("Content-Type", "application/json")
        );

        evaluateResupplyItemAndCheckResult(clientApi);
    }

    @Test
    void evaluateResupplyItemForNullValues() {
        server.enqueue(new MockResponse()
            .setBody(getCteResponse("put_evaluate_resupply_item_with_null_values.json"))
            .addHeader("Content-Type", "application/json")
        );

        evaluateResupplyItemForNullValuesAndCheck(clientApi);
    }

    @Test
    void updateResupplyItems() {
        server.enqueue(new MockResponse()
                .setBody(getCteResponse("put_update_resupply_items.json"))
                .addHeader("Content-Type", "application/json"));
        updateResupplyItemsAndCheck(clientApi);

    }

    @Test
    void resolveQualityAttributes() {
        server.enqueue(new MockResponse()
                .setBody(getCteResponse("get_resolve_quality_attributes.json"))
                .addHeader("Content-Type", "application/json")
        );

        resolveQualityAttributesAndCheck(clientApi);
    }

    @Test
    void resolveQualityAttributesByUnitType() {
        server.enqueue(new MockResponse()
                .setBody(getCteResponse("get_resolve_quality_attributes_by_unit_type.json"))
                .addHeader("Content-Type", "application/json")
        );

        resolveQualityAttributesByUnitTypeAndCheck(clientApi);
    }

    @Test
    void getQualityAttributesForUnitLabels() {
        server.enqueue(new MockResponse()
            .setBody(getCteResponse("get_quality_attributes_for_unit_labels_response.json"))
            .addHeader("Content-Type", "application/json")
        );

        getQualityAttributesForUnitLabels(clientApi);
    }

    @Test
    void getSupplyItemsBySupplyId() {
        server.enqueue(new MockResponse()
            .setBody(getCteResponse("get_items_with_attributes_by_supply_id_response.json"))
            .addHeader("Content-Type", "application/json")
        );

        getSupplyItemsBySupplyId(clientApi);
    }

    @Test
    void updateItemsStockType() {
        server.enqueue(new MockResponse()
                .setBody(getCteResponse("put_update_items_stock_empty_result.json"))
                .addHeader("Content-Type", "application/json"));

        updateItemsStockTypeAndCheck(clientApi);
    }

    @Test
    void getAscStockItems() {
        server.enqueue(new MockResponse()
                .setBody(getCteResponse("get_items_on_asc_stock.json"))
                .addHeader("Content-Type", "application/json"));

        getAscStockItemsAndCheck(clientApi);
    }

    private String getCteResponse(String fileName) {
        try {
            return IOUtils.toString(
                Objects.requireNonNull(
                    getSystemResourceAsStream(fileName)),
                StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public WebClient cteWebClient(String url) {
        WebClient.Builder builder = WebClient.builder()
            .baseUrl(url);

        return builder.build();
    }
}
