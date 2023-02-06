package ru.yandex.market.logistics.lom.client;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class RebindOrderClientTest extends AbstractClientTest {

    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Метод успешно вызван")
    void callOk() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(content().json(extractFileContent("request/order/shipment/rebind-order.json")))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(requestTo(uri + "/orders/rebind-order"))
            .andRespond(withSuccess());

        lomClient.rebindOrders(2L, Set.of(1L));
    }

    @Test
    @DisplayName("Метод вернул ошибку")
    void callFail() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(content().json(extractFileContent("request/order/shipment/rebind-order-fail.json")))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(requestTo(uri + "/orders/rebind-order"))
            .andRespond(withBadRequest().body("{\"message\": \"fail\" }"));

        softly.assertThatThrownBy(() -> lomClient.rebindOrders(3L, Set.of(1L)))
            .isInstanceOf(HttpTemplateException.class)
            .hasMessage("Http request exception: status <400>, response body <{\"message\": \"fail\" }>.");
    }
}
