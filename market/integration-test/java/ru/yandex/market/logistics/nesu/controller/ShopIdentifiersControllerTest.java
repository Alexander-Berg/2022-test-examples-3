package ru.yandex.market.logistics.nesu.controller;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createLogisticsPointResponseBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/controller/shop-identifiers/prepare.xml")
class ShopIdentifiersControllerTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @Test
    @DisplayName("Получить все идентификаторы магазина отсортированными")
    void getAllShopIdentifiersSorted() throws Exception {
        when(lmsClient.getLogisticsPoints(any(LogisticsPointFilter.class))).thenAnswer(
            invocation -> List.of(
                shopWarehouse(3L, null, 1L),
                shopWarehouse(2L, 41L, 2L),
                shopWarehouse(4L, 41L, 8L),
                shopWarehouse(1L, null, 8L),
                shopWarehouse(5L, 42L, 8L)
            ));
        mockMvc.perform(
            get("/back-office/shop-identifiers")
                .param("userId", "1")
                .param("shopId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop-identifiers/response/get_all_information.json"));
    }

    @Test
    @DisplayName("Получить все идентификаторы магазина без складов")
    void getSendersShopIdentifiers() throws Exception {
        when(lmsClient.getLogisticsPoints(any(LogisticsPointFilter.class))).thenAnswer(
            invocation -> List.of());
        mockMvc.perform(
            get("/back-office/shop-identifiers")
                .param("userId", "1")
                .param("shopId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop-identifiers/response/get_senders_information.json"));
    }

    @Test
    @DisplayName("Получить все идентификаторы магазина без сендеров")
    void getWarehousesShopIdentiers() throws Exception {
        when(lmsClient.getLogisticsPoints(any(LogisticsPointFilter.class))).thenAnswer(
            invocation -> List.of(
                shopWarehouse(3L, null, 1L),
                shopWarehouse(2L, 41L, 2L),
                shopWarehouse(4L, 41L, 8L),
                shopWarehouse(1L, null, 8L)
            ));
        mockMvc.perform(
            get("/back-office/shop-identifiers")
                .param("userId", "2")
                .param("shopId", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop-identifiers/response/get_warehouses_information.json"));
    }

    @Test
    @DisplayName("Получить все идентификаторы магазина без сендеров и складов")
    void getNoShopIdentifiers() throws Exception {
        when(lmsClient.getLogisticsPoints(any(LogisticsPointFilter.class))).thenAnswer(
            invocation -> List.of());
        mockMvc.perform(
            get("/back-office/shop-identifiers")
                .param("userId", "2")
                .param("shopId", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop-identifiers/response/get_no_information.json"));
    }

    @Test
    @DisplayName("Получить идентификаторы несуществующего магазина")
    void getInvalidShopIdentifiers() throws Exception {
        when(lmsClient.getLogisticsPoints(any(LogisticsPointFilter.class))).thenAnswer(
            invocation -> List.of());
        mockMvc.perform(
            get("/back-office/shop-identifiers")
                .param("userId", "2")
                .param("shopId", "10"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [10]"));
    }

    @Nonnull
    private LogisticsPointResponse shopWarehouse(long pointId,
        @Nullable Long businessId,
        @Nullable Long partnerId
    ) {
        return createLogisticsPointResponseBuilder(
            pointId,
            partnerId,
            "Warehouse " + pointId,
            PointType.WAREHOUSE
        )
            .businessId(businessId)
            .build();
    }
}
