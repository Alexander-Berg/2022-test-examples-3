package ru.yandex.market.logistics.nesu.controller.warehouse;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.nesu.base.AbstractShopWarehouseControllerTest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение складов DAAS-магазина")
class ShopWarehousesGetTest extends AbstractShopWarehouseControllerTest {

    @ParameterizedTest
    @DisplayName("Успешное получение всех активных складов магазина вне зависимости от статуса магазина")
    @ValueSource(longs = {1, 5})
    void getShopWarehousesSuccessful(long shopId) throws Exception {
        LogisticsPointFilter logisticsPointFilter = createLogisticsPointFilterBuilder(42L)
            .hasPartner(false)
            .build();
        LogisticsPointResponse logisticsPointResponse = createLogisticsPointResponse();

        when(lmsClient.getLogisticsPoints(logisticsPointFilter))
            .thenReturn(List.of(logisticsPointResponse));

        getWarehouses(shopId)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/warehouse/response/warehouses_successful.json"));

        verify(lmsClient).getLogisticsPoints(logisticsPointFilter);
    }

    @DisplayName("Попытка получить все активные склады несуществующего магазина")
    @Test
    void getShopWarehousesOfNonexistentShop() throws Exception {
        getWarehouses(10)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [10]"));
    }

    @DisplayName("Попытка получить все активные склады магазина (у магазина нет складов)")
    @Test
    void getEmptyShopWarehouses() throws Exception {
        LogisticsPointFilter logisticsPointFilter = createLogisticsPointFilterBuilder(42L)
            .hasPartner(false)
            .build();
        when(lmsClient.getLogisticsPoints(logisticsPointFilter))
            .thenReturn(List.of());

        getWarehouses(1)
            .andExpect(status().isOk())
            .andExpect(content().string(EMPTY_ARRAY));

        verify(lmsClient).getLogisticsPoints(logisticsPointFilter);
    }

    @Test
    @DatabaseSetup(value = "/controller/warehouse/prepare_daas.xml", type = DatabaseOperation.INSERT)
    @DisplayName("Успешное получение всех активных складов магазина")
    void getShopWarehousesSuccessfulNewLogic() throws Exception {
        LogisticsPointFilter filter = createLogisticsPointFilterBuilder(42L)
            .hasPartner(false)
            .build();
        when(lmsClient.getLogisticsPoints(filter)).thenReturn(List.of(createLogisticsPointResponse()));

        getWarehouses(6L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/warehouse/response/warehouses_successful.json"));

        verify(lmsClient).getLogisticsPoints(filter);
    }

    @DisplayName("Попытка получить не даас склад")
    @Test
    void notDaas() throws Exception {
        getWarehouses(3)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Unsupported shop role. id: 3, role: DROPSHIP"));
    }

    @Nonnull
    private ResultActions getWarehouses(long shopId) throws Exception {
        return mockMvc.perform(
            get("/back-office/warehouses")
                .param("userId", "19216801")
                .param("shopId", String.valueOf(shopId))
        );
    }
}
