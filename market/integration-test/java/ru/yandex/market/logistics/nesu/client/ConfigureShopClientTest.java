package ru.yandex.market.logistics.nesu.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.yandex.market.logistics.nesu.client.model.ConfigureShopDto;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

@DisplayName("Метод настройки магазина")
class ConfigureShopClientTest extends AbstractClientTest {

    @Test
    @DisplayName("Успешная операция")
    void success() {
        mock.expect(requestTo(uri + "/internal/shops/1/configure"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonRequestContent("request/configure_shop.json"))
            .andRespond(withStatus(HttpStatus.OK));

        softly.assertThatCode(
            () -> client.configureShop(1L, ConfigureShopDto.builder().marketId(1L).build())
        ).doesNotThrowAnyException();
    }
}
