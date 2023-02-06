package ru.yandex.market.logistics.nesu.admin;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.page.PageRequest;
import ru.yandex.market.logistics.management.entity.page.PageResult;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.admin.model.request.AdminShopWarehouseFilter;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.test.integration.utils.QueryParamUtils;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение складов магазина через админку")
@DatabaseSetup("/controller/admin/shop-warehouse/prepare.xml")
public class ShopWarehousesSearchTest extends AbstractContextualTest {
    private static final PageRequest PAGE_REQUEST = new PageRequest(0, 20);

    @Autowired
    private LMSClient lmsClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("LMS вернул пустую страницу складов")
    void noWarehouses() throws Exception {
        LogisticsPointFilter filter = LogisticsPointFilter.newBuilder()
            .businessIds(Set.of(30004L))
            .hasPartner(false)
            .type(PointType.WAREHOUSE)
            .build();

        when(lmsClient.getLogisticsPoints(filter, PAGE_REQUEST)).thenReturn(new PageResult<>());

        getShopWarehouses(50004L, new AdminShopWarehouseFilter())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/shop-warehouse/response/no_warehouses.json"));

        verify(lmsClient).getLogisticsPoints(filter, PAGE_REQUEST);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Получение складов магазина")
    void getShopWithFilteredWarehouses(
        @SuppressWarnings("unused") String name,
        Long shopId,
        String responsePath,
        LogisticsPointFilter filter,
        AdminShopWarehouseFilter warehouseFilter
    )
        throws Exception {
        when(lmsClient.getLogisticsPoints(filter, PAGE_REQUEST)).thenReturn(
            new PageResult<LogisticsPointResponse>()
                .setData(
                    List.of(
                        LmsFactory.createLogisticsPointResponse(
                            1L,
                            20L,
                            "name",
                            filter.getActive(),
                            PointType.WAREHOUSE
                        ))
                )
                .setTotalElements(2000)
        );

        getShopWarehouses(shopId, warehouseFilter).andExpect(status().isOk()).andExpect(jsonContent(responsePath));

        verify(lmsClient).getLogisticsPoints(filter, PAGE_REQUEST);
    }

    @Nonnull
    private static Stream<Arguments> getShopWithFilteredWarehouses() {
        return Stream.of(
            Arguments.of(
                "DAAS-магазин",
                50004L,
                "controller/admin/shop-warehouse/response/daas_new_logic.json",
                LogisticsPointFilter.newBuilder()
                    .businessIds(Set.of(30004L))
                    .hasPartner(false)
                    .type(PointType.WAREHOUSE)
                    .build(),
                new AdminShopWarehouseFilter()
            ),
            Arguments.of(
                "Синий магазин",
                50002L,
                "controller/admin/shop-warehouse/response/blue_new_logic.json",
                LogisticsPointFilter.newBuilder()
                    .businessIds(Set.of(30002L))
                    .partnerIds(Set.of(290L))
                    .type(PointType.WAREHOUSE)
                    .build(),
                new AdminShopWarehouseFilter()
            ),
            Arguments.of(
                "DAAS-магазин, с фильтром active = false",
                50004L,
                "controller/admin/shop-warehouse/response/daas_warehouses_active_false.json",
                LogisticsPointFilter.newBuilder()
                    .businessIds(Set.of(30004L))
                    .hasPartner(false)
                    .type(PointType.WAREHOUSE)
                    .active(false)
                    .build(),
                new AdminShopWarehouseFilter()
                    .setActive(false)
            ),
            Arguments.of(
                "DAAS-магазин, с фильтром active = true",
                50004L,
                "controller/admin/shop-warehouse/response/daas_warehouses_active_true.json",
                LogisticsPointFilter.newBuilder()
                    .businessIds(Set.of(30004L))
                    .hasPartner(false)
                    .type(PointType.WAREHOUSE)
                    .active(true)
                    .build(),
                new AdminShopWarehouseFilter()
                    .setActive(true)
            ),
            Arguments.of(
                "Синий магазин, с фильтром active = false",
                50002L,
                "controller/admin/shop-warehouse/response/blue_warehouses_active_false.json",
                LogisticsPointFilter.newBuilder()
                    .businessIds(Set.of(30002L))
                    .partnerIds(Set.of(290L))
                    .type(PointType.WAREHOUSE)
                    .active(false)
                    .build(),
                new AdminShopWarehouseFilter()
                    .setActive(false)
            ),
            Arguments.of(
                "Синий магазин, с фильтром active = true",
                50002L,
                "controller/admin/shop-warehouse/response/blue_warehouses_active_true.json",
                LogisticsPointFilter.newBuilder()
                    .businessIds(Set.of(30002L))
                    .partnerIds(Set.of(290L))
                    .type(PointType.WAREHOUSE)
                    .active(true)
                    .build(),
                new AdminShopWarehouseFilter()
                    .setActive(true)
            )
        );
    }

    @Nonnull
    private ResultActions getShopWarehouses(Long shopId, AdminShopWarehouseFilter filter) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get("/admin/shops/warehouses")
            .param("shopId", shopId.toString())
            .params(QueryParamUtils.toParams(filter)));
    }
}
