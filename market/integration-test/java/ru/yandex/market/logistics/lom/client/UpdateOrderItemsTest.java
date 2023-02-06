package ru.yandex.market.logistics.lom.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.lom.model.dto.DtoBuilderFactory.validUpdateOrderItemsRequestBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class UpdateOrderItemsTest extends AbstractClientTest {
    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Отправить запрос на обновление товаров заказа")
    void updateOrderItems() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/updateItems"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(extractFileContent("request/order/update/items/request.json"), true))
            .andRespond(withSuccess());
        lomClient.updateOrderItems(validUpdateOrderItemsRequestBuilder().build());
    }

    @Test
    @DisplayName("Отправить запрос на обновление товаров заказа с внешним идентификатором")
    void updateOrderItemWithExternalId() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/updateItems"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(
                extractFileContent("request/order/update/items/request_with_external_id.json"),
                true
            ))
            .andRespond(withSuccess());
        lomClient.updateOrderItems(
            validUpdateOrderItemsRequestBuilder()
                .externalRequestId("external-request-id")
                .build()
        );
    }

    @Test
    @DisplayName("Отправить запрос на обновление товаров заказа, обновление разрешено")
    void updateOrderItemsAllowed() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/updateItems"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(extractFileContent("request/order/update/items/request.json"), true))
            .andRespond(withSuccess());
        softly.assertThat(lomClient.updateOrderItemsIfAllowed(validUpdateOrderItemsRequestBuilder().build()))
            .isTrue();
    }

    @Test
    @DisplayName("Отправить запрос на обновление товаров заказа, обновление запрещено")
    void updateOrderItemsNotAllowed() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/updateItems"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(extractFileContent("request/order/update/items/request.json"), true))
            .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));
        softly.assertThat(lomClient.updateOrderItemsIfAllowed(validUpdateOrderItemsRequestBuilder().build()))
            .isFalse();
    }
}
