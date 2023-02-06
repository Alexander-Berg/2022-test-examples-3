package ru.yandex.market.logistics.nesu.controller.warehouse;

import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.nesu.base.AbstractShopWarehouseControllerTest;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение регионов, в которых присутствуют склады DAAS-магазина")
class ShopWarehouseGetRegionsTest extends AbstractShopWarehouseControllerTest {
    @DisplayName("Получение регионов складов магазина с проверкой сортировки по региону и складов внутри региона")
    @Test
    void getWarehousesRegions() throws Exception {
        LogisticsPointFilter logisticsPointFilter = logisticsPointFilter();

        when(lmsClient.getLogisticsPoints(refEq(logisticsPointFilter))).thenReturn(getLogisticPoints());

        getRegions("1")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/warehouse/response/regions.json"));

        verify(lmsClient).getLogisticsPoints(refEq(logisticsPointFilter));
    }

    @DisplayName("Не найден магазин")
    @Test
    void getWarehousesRegionsShopNotFound() throws Exception {
        getRegions("-1")
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [-1]"));
    }

    @DisplayName("Не удалось определить субъект федерации  по региону склада")
    @Test
    void getWarehousesRegionsRegionNotFound() throws Exception {
        LogisticsPointFilter logisticsPointFilter = logisticsPointFilter();

        when(lmsClient.getLogisticsPoints(refEq(logisticsPointFilter))).thenReturn(List.of(
            logisticsPointResponseBuilder()
                .address(
                    addressBuilder()
                        .locationId(3)
                        .build()
                )
                .build()
        ));

        getRegions("1")
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Не удалось определить субъект федерации для региона \"Центральный федеральный округ\" (id = 3)."
            ));

        verify(lmsClient).getLogisticsPoints(refEq(logisticsPointFilter));
    }

    @DisplayName("Попытка получить регионы не даас склада")
    @Test
    void notDaas() throws Exception {
        getRegions("3")
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Unsupported shop role. id: 3, role: DROPSHIP"));
    }

    /**
     * Список складов в регионах с неупорядоченными идентификаторами регионов и идентификаторами складов внутри региона.
     * Порядок следующий:
     * - locationId = 1,     warehouseId = 3
     * - locationId = 11117, warehouseId = 2
     * - locationId = 1,     warehouseId = 1
     */
    @Nonnull
    private List<LogisticsPointResponse> getLogisticPoints() {
        LogisticsPointResponse logisticsPoint1 = logisticsPointResponseBuilder()
            .id(3L)
            .name("Склад 3")
            .address(
                addressBuilder()
                    .locationId(213)
                    .build()
            )
            .build();

        LogisticsPointResponse logisticsPoint2 = logisticsPointResponseBuilder()
            .id(2L)
            .name("Склад 2")
            .address(
                addressBuilder()
                    .locationId(11117)
                    .build()
            )
            .build();

        LogisticsPointResponse logisticsPoint3 = logisticsPointResponseBuilder()
            .id(1L)
            .name("Склад 1")
            .address(
                addressBuilder()
                    .locationId(1)
                    .build()
            )
            .build();

        return List.of(logisticsPoint1, logisticsPoint2, logisticsPoint3);
    }

    @Nonnull
    private LogisticsPointFilter logisticsPointFilter() {
        return createLogisticsPointFilterBuilder(42L).hasPartner(false).build();
    }

    @Nonnull
    private ResultActions getRegions(String shopId) throws Exception {
        return mockMvc.perform(
            get("/back-office/warehouses/regions")
                .param("userId", "19216801")
                .param("shopId", shopId)
        );
    }
}
