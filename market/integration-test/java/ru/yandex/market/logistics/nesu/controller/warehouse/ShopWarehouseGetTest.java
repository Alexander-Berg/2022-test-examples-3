package ru.yandex.market.logistics.nesu.controller.warehouse;

import java.time.Duration;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.base.AbstractShopWarehouseControllerTest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение склада DAAS-магазина")
class ShopWarehouseGetTest extends AbstractShopWarehouseControllerTest {

    @DisplayName("Успешное получение склада с незаполненными опциональными полями")
    @Test
    void getShopWarehouseWithOnlyRequiredFieldsSuccessful() throws Exception {
        when(lmsClient.getLogisticsPoint(1L)).thenReturn(Optional.of(createMinimalLogisticsPointResponse()));

        getWarehouse(1)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/warehouse/response/warehouse_with_only_required_fields.json"));

        verify(lmsClient).getLogisticsPoint(1L);
    }

    @DisplayName("Попытка получить склад несуществующего магазина")
    @Test
    void getShopWarehouseOfNonexistentShop() throws Exception {
        getWarehouse(10)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [10]"));
    }

    @DisplayName("Попытка получить склад, который принадлежит другому партнеру")
    @Test
    void getShopWarehouseOfAnotherPartner() throws Exception {
        when(lmsClient.getLogisticsPoint(1L)).thenReturn(Optional.of(createLogisticsPointResponse()));

        getWarehouse(2)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [1]"));

        verify(lmsClient).getLogisticsPoint(1L);
    }

    @DisplayName("Попытка получить склад с другим businessId")
    @Test
    void getWarehouseWrongBusinessId() throws Exception {
        when(lmsClient.getLogisticsPoint(1L))
            .thenReturn(Optional.of(logisticsPointResponseBuilder().businessId(41L).build()));

        getWarehouse(1)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [1]"));

        verify(lmsClient).getLogisticsPoint(1L);
    }

    @DisplayName("Попытка получить несуществующий склад")
    @Test
    void getNonexistentShopWarehouse() throws Exception {
        getWarehouse(1)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [1]"));

        verify(lmsClient).getLogisticsPoint(1L);
    }

    @DisplayName("Получение склада")
    @Test
    void success() throws Exception {
        when(lmsClient.getLogisticsPoint(1L)).thenReturn(Optional.of(
            logisticsPointResponseBuilder()
                .businessId(42L)
                .partnerId(null)
                .handlingTime(Duration.ofDays(1))
                .build()
        ));

        getWarehouse(1)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/warehouse/response/warehouse_successful.json"));

        verify(lmsClient).getLogisticsPoint(1L);
    }

    @DisplayName("Попытка получить не даас склад")
    @Test
    void notDaas() throws Exception {
        getWarehouse(3)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Unsupported shop role. id: 3, role: DROPSHIP"));
    }

    @Nonnull
    private ResultActions getWarehouse(long shopId) throws Exception {
        return mockMvc.perform(
            get("/back-office/warehouses/1")
                .param("userId", "19216801")
                .param("shopId", String.valueOf(shopId))
        );
    }
}
