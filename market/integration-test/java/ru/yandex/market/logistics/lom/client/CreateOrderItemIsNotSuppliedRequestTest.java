package ru.yandex.market.logistics.lom.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.lom.model.dto.DtoBuilderFactory.validCreateOrderItemIsNotSuppliedRequests;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class CreateOrderItemIsNotSuppliedRequestTest extends AbstractClientTest {

    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Создание заявки о товарах, которых нет в поставке")
    void createOrderItemIsNotSuppliedRequest() {
        mock.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(uri + "/orders/itemIsNotSupplied"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json(extractFileContent("request/order/itemisnotsupplied/request.json"), true))
            .andRespond(withSuccess());
        lomClient.createOrderItemIsNotSuppliedRequest(validCreateOrderItemIsNotSuppliedRequests());
    }
}
