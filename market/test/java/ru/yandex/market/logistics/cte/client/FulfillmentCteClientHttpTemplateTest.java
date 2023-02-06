package ru.yandex.market.logistics.cte.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Config.class)
public class FulfillmentCteClientHttpTemplateTest extends BaseFulfillmentCteClientTest {

    @Autowired
    private FulfillmentCteClientApi clientApi;

    @Autowired
    private MockRestServiceServer mockServer;

    @Value("${fulfillment.cte.api.host}")
    private String host;

    @AfterEach
    void verifyAndResetServerMock() {
        try {
            mockServer.verify();
        } finally {
            mockServer.reset();
        }
    }

    @Test
    void evaluateResupplyItem() throws IOException {
        ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("put_evaluate_resupply_item.json")), StandardCharsets.UTF_8));

        mockServer.expect(requestTo(host + "/logistic_services/supplies/" + YANDEX_SUPPLY_ID
                + "/items/" + UUID))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(returnResponseCreator);

        evaluateResupplyItemAndCheckResult(clientApi);
    }

    @Test
    void evaluateResupplyItemForNullValues() throws IOException {
        ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("put_evaluate_resupply_item_with_null_values.json")),
                        StandardCharsets.UTF_8));

        mockServer.expect(requestTo(host + "/logistic_services/supplies/" + YANDEX_SUPPLY_ID
                + "/items/" + UUID))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(returnResponseCreator);

        evaluateResupplyItemForNullValuesAndCheck(clientApi);
    }

    @Test
    void updateResupplyItems() throws IOException {
        ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("put_update_resupply_items.json")), StandardCharsets.UTF_8));

        mockServer.expect(requestTo(host + "/logistic_services/supplies/items"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(returnResponseCreator);

        updateResupplyItemsAndCheck(clientApi);
    }

    @Test
    void resolveQualityAttributes() throws IOException {
        ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("get_resolve_quality_attributes.json")),
                        StandardCharsets.UTF_8));

        mockServer.expect(requestTo(host +
                "/logistic_services/quality-attributes/find-by-supply_item" +
                "?marketShopSku=marketShopSku1&vendorId=10264169&categoryId=3"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(returnResponseCreator);

        resolveQualityAttributesAndCheck(clientApi);
    }

    @Test
    void resolveQualityAttributesWithMatrixType() throws IOException {
        ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("get_resolve_quality_attributes.json")),
                        StandardCharsets.UTF_8));

        mockServer.expect(requestTo(host +
                "/logistic_services/quality-attributes/find-by-supply_item" +
                "?marketShopSku=marketShopSku1&vendorId=10264169&matrixType=FULFILLMENT&categoryId=3"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(returnResponseCreator);

        resolveQualityAttributesWithMatrixTypeAndCheck(clientApi);
    }

    @Test
    void resolveQualityAttributesByUnitType() throws IOException {
        ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("get_resolve_quality_attributes_by_unit_type.json")),
                        StandardCharsets.UTF_8));

        mockServer.expect(requestTo(host +
                "/logistic_services/quality-attributes/find-by-unit_type" +
                "?unitType=BOX"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(returnResponseCreator);

        resolveQualityAttributesByUnitTypeAndCheck(clientApi);
    }

    @Test
    void resolveQualityAttributesByUnitTypeAndMatrixType() throws IOException {
        ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("get_resolve_quality_attributes_by_unit_type.json")),
                        StandardCharsets.UTF_8));

        mockServer.expect(requestTo(host +
                "/logistic_services/quality-attributes/find-by-unit_type" +
                "?unitType=BOX&matrixType=FULFILLMENT"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(returnResponseCreator);

        resolveQualityAttributesByUnitTypeAndMatrixTypeAndCheck(clientApi);
    }

    @Test
    void evaluateTransportationUnitMinimalRequest() throws IOException {

        String expectedRequestBody = IOUtils.toString(Objects.requireNonNull(
                getSystemResourceAsStream("evaluateTransportationUnit/minimal_request.json")), StandardCharsets.UTF_8);
        mockServer.expect(requestTo(host + "/logistic_services/supplies/" + YANDEX_SUPPLY_ID
                + "/units"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(content().json(expectedRequestBody, true))
                .andRespond(withStatus(OK));

        evaluateTransportationUnitMinimalRequest(clientApi);
    }

    @Test
    void evaluateTransportationUnitLessThanMinimalRequest() {

        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            evaluateTransportationUnitLessThanMinimalRequest(clientApi);
        });

        assertEquals("Parameter specified as non-null is null:" +
                " method ru.yandex.market.logistics.cte.client.dto.TransportationUnitRequestDTO.<init>," +
                " parameter label", exception.getMessage());
    }

    @Test
    void evaluateTransportationUnitNormalRequest() throws IOException {

        String expectedRequestBody = IOUtils.toString(Objects.requireNonNull(
                getSystemResourceAsStream("evaluateTransportationUnit/normal_request.json")), StandardCharsets.UTF_8);
        mockServer.expect(requestTo(host + "/logistic_services/supplies/" + YANDEX_SUPPLY_ID
                + "/units"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(content().json(expectedRequestBody, true))
                .andRespond(withStatus(OK));

        evaluateTransportationUnitNormalRequest(clientApi);
    }

    @Test
    void getQualityAttributesForUnitLabels() throws IOException {
        ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("get_quality_attributes_for_unit_labels_response.json")),
                        StandardCharsets.UTF_8));

        String expectedRequestBody = IOUtils.toString(Objects.requireNonNull(
                getSystemResourceAsStream("get_quality_attributes_for_unit_labels_request.json")),
                StandardCharsets.UTF_8);

        mockServer.expect(requestTo(host + "/logistic_services/supplies/"
                + YANDEX_SUPPLY_ID + "/units"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(content().json(expectedRequestBody, true))
                .andRespond(returnResponseCreator);

        getQualityAttributesForUnitLabels(clientApi);
    }

    @Test
    void updateItemsStockType() throws IOException {
        ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("put_update_items_stock_empty_result.json")),
                        StandardCharsets.UTF_8));
        String expectedRequestBody = IOUtils.toString(Objects.requireNonNull(
                getSystemResourceAsStream("put_update_items_stock.json")), StandardCharsets.UTF_8);
        mockServer.expect(requestTo(host + "/logistic_services/supplies/items/stock"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(content().json(expectedRequestBody, true))
                .andRespond(returnResponseCreator);

        updateItemsStockTypeAndCheck(clientApi);
    }

    @Test
    void getAscStockItems() throws IOException {
        ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("get_items_on_asc_stock.json")),
                        StandardCharsets.UTF_8));
        mockServer.expect(requestTo(host + "/logistic_services/supplies/items/stock"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(content().contentType(APPLICATION_JSON))
                .andRespond(returnResponseCreator);

        getAscStockItemsAndCheck(clientApi);
    }


    @Test
    void getSupplyItemsByUuids() throws IOException {
        ResponseCreator returnResponseCreator = withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(IOUtils.toString(Objects.requireNonNull(
                        getSystemResourceAsStream("get_items_with_attributes_by_supply_id_response.json")),
                        StandardCharsets.UTF_8));

        mockServer.expect(requestTo(host + "/logistic_services/supplies/"
                + YANDEX_SUPPLY_ID + "/item/list"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(content().contentType(APPLICATION_JSON))
                .andRespond(returnResponseCreator);

        getSupplyItemsBySupplyId(clientApi);
    }
}
