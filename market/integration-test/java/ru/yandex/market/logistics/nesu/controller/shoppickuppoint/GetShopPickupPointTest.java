package ru.yandex.market.logistics.nesu.controller.shoppickuppoint;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение ПВЗ магазина и его тарифа")
class GetShopPickupPointTest extends AbstractShopPickupPointTest {

    @Test
    @DisplayName("Успешное получение")
    void getSuccess() throws Exception {
        mockMvc.perform(requestBuilder(200, 800))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop-pickup-points/response/get-shop-pickup-point-meta.json"));
    }

    @Nonnull
    @Override
    protected MockHttpServletRequestBuilder requestBuilder(long shopId, long shopPickupPointMetaId) {
        return MockMvcRequestBuilders.request(
            HttpMethod.GET,
            String.format("/internal/shop/%d/pickup-point-meta/%d", shopId, shopPickupPointMetaId)
        );
    }
}
