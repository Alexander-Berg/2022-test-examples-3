package ru.yandex.market.delivery.trust.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.yandex.market.delivery.trust.client.model.request.CreateOrderRequest;
import ru.yandex.market.delivery.trust.client.model.request.CreateProductRequest;

import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class TrustClientTest extends AbstractTest {

    @Test
    @DisplayName("Контекст поднимается")
    void context() {
        softly.assertThat(trustClient).isNotNull();
    }

    @Test
    @DisplayName("Создание продукта")
    void createProduct() {
        expectRequestTo(HttpMethod.POST, "products")
            .andExpect(jsonRequest("product/create.json"))
            .andExpect(serviceToken("abcdef"))
            .andRespond(withStatus(HttpStatus.OK));

        trustClient.createProduct(
            "abcdef",
            CreateProductRequest.builder()
                .productId("product-id-12378")
                .name("Product name")
                .partnerId(567L)
                .build()
        );
    }

    @Test
    @DisplayName("Создание заказа")
    void createOrder() {
        expectRequestTo(HttpMethod.POST, "orders")
            .andExpect(jsonRequest("order/request/create.json"))
            .andExpect(serviceToken("zxcvbn"))
            .andRespond(jsonResponse("order/response/create.json"));

        Long orderId = trustClient.createOrder(
            "zxcvbn",
            CreateOrderRequest.builder()
                .productId("product-id-12378")
                .commission(322)
                .build()
        );

        softly.assertThat(orderId).isEqualTo(104677998);
    }

}
