package ru.yandex.market.logistics.nesu.controller.partner;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.service.lms.PlatformClientId;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/controller/partner/sorting_centers_warehouse_setup.xml")
@DisplayName("Получение списка всех доступных сортировочных центров")
class PartnerControllerSortingCentersWarehousesTest extends AbstractContextualTest {
    @Autowired
    private LMSClient lmsClient;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getSortingCentersWarehousesArguments")
    @DisplayName("Успех")
    void getSortingCentersWarehouses(
        @SuppressWarnings("unused") String displayName,
        Set<Long> warehouseIds,
        Long shopId,
        String resultPath
    ) throws Exception {
        mockGetPartners();
        mockGetPartnersWarehouses(warehouseIds);

        mockMvc.perform(
            get("/back-office/partner/sorting-centers/warehouses")
                .param("locationId", "213")
                .param("shopId", shopId.toString())
                .param("userId", "123")
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(resultPath));
    }

    private static Stream<Arguments> getSortingCentersWarehousesArguments() {
        return Stream.of(
            Arguments.of(
                "Поиск для магазина, не находящегося в вайт-листе",
                Set.of(1L, 6L),
                100L,
                "controller/partner/sorting_centers_warehouse_response.json"
            ),
            Arguments.of(
                "Поиск для магазина, находящегося в вайт-листе",
                Set.of(1L, 2L, 6L),
                200L,
                "controller/partner/sorting_centers_warehouse_response_white_list.json"
            )
        );
    }

    private void mockGetPartners() {
        SearchPartnerFilter partnerFilter = SearchPartnerFilter.builder()
            .setPlatformClientIds(Set.of(PlatformClientId.YANDEX_DELIVERY.getId()))
            .setStatuses(Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING))
            .setPlatformClientStatuses(Set.of(PartnerStatus.ACTIVE))
            .setTypes(Set.of(PartnerType.SORTING_CENTER))
            .build();
        when(lmsClient.searchPartners(partnerFilter)).thenReturn(List.of(
            LmsFactory.createPartnerResponseBuilder(10L, PartnerType.SORTING_CENTER, 1000L)
                .name("SC_CODE")
                .readableName("Сортировочный центр")
                .build(),
            LmsFactory.createPartnerResponseBuilder(20L, PartnerType.SORTING_CENTER, 2000L)
                .name("SC_CODE_2")
                .readableName("Сортировочный центр 2")
                .build()
        ));
    }

    private void mockGetPartnersWarehouses(Set<Long> warehouseIds) {
        LogisticsPointFilter logisticsPointFilter = LogisticsPointFilter.newBuilder()
            .ids(warehouseIds)
            .type(PointType.WAREHOUSE)
            .partnerIds(Set.of(10L, 20L))
            .active(true)
            .build();

        when(lmsClient.getLogisticsPoints(logisticsPointFilter)).thenReturn(
            warehouseIds.stream()
                //отфильтровываем неактивный склад
                .filter(id -> !id.equals(6L))
                .map(
                    id -> LmsFactory.createLogisticsPointResponse(
                        id,
                        id * 10,
                        String.format("Склад СЦ %d", id),
                        PointType.WAREHOUSE
                    )
                )
                .collect(Collectors.toList())
        );
    }
}
