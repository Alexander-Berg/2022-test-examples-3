package ru.yandex.market.logistics.nesu.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@DisplayName("Удаление ПВЗ и тарифа")
class DeleteShopPickupPointMetaClientTest extends AbstractClientTest {

    @Test
    void deleteShopPickupPointMeta() {
        mock.expect(requestTo(uri + "/internal/shop/1/pickup-point-meta/2"))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(withStatus(OK));

        softly.assertThatCode(() -> client.deleteShopPickupPointMeta(1, 2))
            .doesNotThrowAnyException();
    }
}
