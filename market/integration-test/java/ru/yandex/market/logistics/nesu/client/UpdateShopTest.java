package ru.yandex.market.logistics.nesu.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.yandex.market.logistics.nesu.client.model.shop.UpdateShopDto;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

@DisplayName("Обновление магазина")
class UpdateShopTest extends AbstractClientTest {

    @Test
    @DisplayName("Успех")
    void success() {
        mock.expect(requestTo(uri + "/internal/shops/42"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(jsonRequestContent("request/update_shop.json"))
            .andRespond(withStatus(HttpStatus.OK));

        softly.assertThatCode(
            () -> client.updateShop(
                42,
                UpdateShopDto.builder().name("new name").externalId("new-external-id").localDeliveryRegion(213).build()
            )
        ).doesNotThrowAnyException();
    }

}
