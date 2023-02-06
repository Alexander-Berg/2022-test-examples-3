package ru.yandex.market.logistics.nesu.controller.shoppickuppoint;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Общие негативные сценарии для ручек CRUD ПВЗ магазинов")
@DatabaseSetup("/controller/shop-pickup-points/before/shop.xml")
@DatabaseSetup("/controller/shop-pickup-points/before/shop_pickup_point.xml")
abstract class AbstractShopPickupPointTest extends AbstractContextualTest {
    @Test
    @DisplayName("Нет магазина")
    void noShop() throws Exception {
        mockMvc.perform(requestBuilder(0, 800))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [0]"));
    }

    @Test
    @DisplayName("Нет записи по идентификатору")
    void noShopPickupPointMeta() throws Exception {
        mockMvc.perform(requestBuilder(200, 0))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP_PICKUP_POINT_META] with ids [0]"));
    }

    @Test
    @DisplayName("Удален")
    @DatabaseSetup(
        value = "/controller/shop-pickup-points/before/deleted.xml",
        type = DatabaseOperation.REFRESH
    )
    void deleted() throws Exception {
        mockMvc.perform(requestBuilder(200, 800))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP_PICKUP_POINT_META] with ids [800]"));
    }

    @Test
    @DisplayName("Не принадлежит магазину")
    @DatabaseSetup(
        value = "/controller/shop-pickup-points/before/does_not_belong_to_shop.xml",
        type = DatabaseOperation.REFRESH
    )
    void doesNotBelongToShop() throws Exception {
        mockMvc.perform(requestBuilder(200, 800))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP_PICKUP_POINT_META] with ids [800]"));
    }


    @Nonnull
    protected abstract MockHttpServletRequestBuilder requestBuilder(long shopId, long shopPickupPointMetaId);
}
