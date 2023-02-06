package ru.yandex.market.delivery.trust.client;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.yandex.market.delivery.trust.client.model.request.BasketOrder;
import ru.yandex.market.delivery.trust.client.model.request.CreateBasketRequest;
import ru.yandex.market.delivery.trust.client.model.response.BasketStatus;
import ru.yandex.market.delivery.trust.client.model.response.GetBasketResponse;

import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class BasketTest extends AbstractTest {

    private static final String TOKEN = "qwerty";

    @Test
    @DisplayName("Создание корзины")
    void createBasket() {
        expectRequestTo(HttpMethod.POST, "payments")
            .andExpect(jsonRequest("basket/request/create.json"))
            .andExpect(serviceToken(TOKEN))
            .andRespond(jsonResponse("basket/response/create.json"));

        String basketId = trustClient.createBasket(
            "qwerty",
            CreateBasketRequest.builder()
                .productId("product-id-12378")
                .payMethodId("cash-890")
                .order(BasketOrder.builder().orderId(4561L).price(new BigDecimal("10.30")).build())
                .build()
        );

        softly.assertThat(basketId).isEqualTo("asdvbn");
    }

    @Test
    @DisplayName("Оплата корзины")
    void payBasket() {
        expectRequestTo(HttpMethod.POST, "payments/321-qwe-poi-098/start")
            .andExpect(serviceToken(TOKEN))
            .andRespond(withStatus(HttpStatus.OK));

        trustClient.payBasket(TOKEN, "321-qwe-poi-098");
    }

    @Test
    @DisplayName("Получение корзины")
    void getBasket() {
        expectRequestTo(HttpMethod.GET, "payments/098-poi-lkj")
            .andExpect(serviceToken(TOKEN))
            .andRespond(jsonResponse("basket/response/get.json"));

        GetBasketResponse response = trustClient.getBasket(TOKEN, "098-poi-lkj");

        softly.assertThat(response.getBasketId()).isEqualTo("098-poi-lkj");
        softly.assertThat(response.getStatus()).isEqualTo(BasketStatus.AUTHORIZED);
    }

}
