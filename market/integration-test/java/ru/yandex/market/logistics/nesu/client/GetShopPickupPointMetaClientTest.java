package ru.yandex.market.logistics.nesu.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointMetaDto;
import ru.yandex.market.logistics.nesu.client.utils.ShopPickupPointFactory;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Получение ПВЗ и тарифа")
class GetShopPickupPointMetaClientTest extends AbstractClientTest {

    @Test
    void getShopPickupPointMeta() {
        mock.expect(requestTo(uri + "/internal/shop/1/pickup-point-meta/2"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withStatus(OK)
                    .body(extractFileContent("response/shop_pickup_point/response.json"))
                    .contentType(APPLICATION_JSON)
            );

        ShopPickupPointMetaDto meta = client.getShopPickupPointMeta(1, 2);
        softly.assertThat(meta)
            .usingRecursiveComparison()
            .isEqualTo(ShopPickupPointFactory.shopPickupPointMeta());
    }
}
