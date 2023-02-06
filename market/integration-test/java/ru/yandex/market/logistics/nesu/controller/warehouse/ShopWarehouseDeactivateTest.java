package ru.yandex.market.logistics.nesu.controller.warehouse;

import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.nesu.base.AbstractShopWarehouseControllerTest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Деактивация DAAS-склада")
class ShopWarehouseDeactivateTest extends AbstractShopWarehouseControllerTest {

    @DisplayName("Успешное отключение склада магазина")
    @Test
    void deactivateShopWarehousesSuccessful() throws Exception {
        LogisticsPointFilter logisticsPointFilter = logisticsPointFilter();
        LogisticsPointResponse logisticsPointResponse = createLogisticsPointResponse();

        when(lmsClient.getLogisticsPoints(logisticsPointFilter))
            .thenReturn(List.of(logisticsPointResponse, logisticsPointResponse));

        when(lmsClient.deactivateLogisticsPoint(1L))
            .thenReturn(logisticsPointResponse);

        deactivateWarehouse(1)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/warehouse/response/warehouse_successful.json"));

        verify(lmsClient).getLogisticsPoints(logisticsPointFilter);
        verify(lmsClient).deactivateLogisticsPoint(1L);
    }

    @DisplayName("Неуспешное отключение последнего склада магазина")
    @Test
    void deactivateShopLastWarehouse() throws Exception {
        LogisticsPointFilter logisticsPointFilter = logisticsPointFilter();

        when(lmsClient.getLogisticsPoints(logisticsPointFilter))
            .thenReturn(List.of(createLogisticsPointResponse()));

        deactivateWarehouse(1)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Last warehouse 1 of shop 1 cannot be deleted"));

        verify(lmsClient).getLogisticsPoints(logisticsPointFilter);
    }

    @DisplayName("Попытка отключить склад несуществующего магазина")
    @Test
    void deactivateShopWarehouseOfNonexistentShop() throws Exception {
        deactivateWarehouse(10)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [10]"));
    }

    @DisplayName("Попытка отключить несуществующий склад")
    @Test
    void deactivateNonexistentShopWarehouse() throws Exception {
        deactivateWarehouse(1)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [1]"));

        verify(lmsClient).getLogisticsPoints(logisticsPointFilter());
    }

    @DisplayName("Попытка деактивировать не даас склад")
    @Test
    void notDaas() throws Exception {
        deactivateWarehouse(3)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Unsupported shop role. id: 3, role: DROPSHIP"));
    }

    @Nonnull
    private LogisticsPointFilter logisticsPointFilter() {
        return createLogisticsPointFilterBuilder(42L)
            .hasPartner(false)
            .build();
    }

    @Nonnull
    private ResultActions deactivateWarehouse(long shopId) throws Exception {
        return mockMvc.perform(
            delete("/back-office/warehouses/1")
                .param("userId", "19216801")
                .param("shopId", String.valueOf(shopId))
        );
    }
}
