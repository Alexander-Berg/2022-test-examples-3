package ru.yandex.market.logistics.nesu.controller.business;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.nesu.controller.partner.AbstractGetAvailableShipmentOptionsTest;
import ru.yandex.market.logistics.nesu.dto.business.AvailableShipmentOptionsFilter;
import ru.yandex.market.logistics.nesu.enums.ShipmentPointType;
import ru.yandex.market.logistics.nesu.utils.CommonsConstants;
import ru.yandex.market.logistics.test.integration.utils.QueryParamUtils;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение доступных вариантов отгрузки со склада")
@DatabaseSetup("/controller/business/before/setup.xml")
@DatabaseSetup("/controller/business/before/shop_partner_settings.xml")
class GetAvailableShipmentOptionsTest extends AbstractGetAvailableShipmentOptionsTest {

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2021-01-01T00:00:00Z"), CommonsConstants.MSK_TIME_ZONE);
    }

    @Override
    protected MockHttpServletRequestBuilder createRequestBuilder() {
        return get("/back-office/business/warehouses/1/available-shipment-options");
    }

    @Test
    @DisplayName("Не найдена локация склада магазина")
    void warehouseRegionNotFound() throws Exception {
        long businessWarehouseId = SHOP_PARTNER_ID;
        try (var ignored = mockGetBusinessWarehouse(businessWarehouseId, -100)) {
            getAvailableShipmentOptions(1, businessWarehouseId)
                .andExpect(status().isInternalServerError())
                .andExpect(errorMessage("Warehouse 10000001000 has invalid location"));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
    }

    @Test
    @DisplayName("Нет доступных складов")
    void emptyAvailability() throws Exception {
        long businessWarehouseId = SHOP_PARTNER_ID;
        try (var ignored = mockGetBusinessWarehouse(businessWarehouseId, 213)) {
            getAvailableShipmentOptions(1, businessWarehouseId)
                .andExpect(status().isOk())
                .andExpect(content().json(EMPTY_ARRAY));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetActiveRelation();
        verifyGetPartners(Set.of(SHOP_PARTNER_ID));
    }

    @Test
    @DisplayName("Отключена доступность склада")
    @DatabaseSetup("/repository/logistic-point-availability/before/disabled_availability.xml")
    void disabledAvailability() throws Exception {
        long businessWarehouseId = SHOP_PARTNER_ID;
        try (var ignored = mockGetBusinessWarehouse(businessWarehouseId, 213)) {
            getAvailableShipmentOptions(1, businessWarehouseId)
                .andExpect(status().isOk())
                .andExpect(content().json(EMPTY_ARRAY));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetActiveRelation();
        verifyGetPartners(Set.of(SHOP_PARTNER_ID));
    }

    @Test
    @DisplayName("Доступен склад для родительского региона")
    @DatabaseSetup("/repository/logistic-point-availability/before/parent_location.xml")
    void parentRegion() throws Exception {
        long businessWarehouseId = SHOP_PARTNER_ID;
        Set<Long> availableWarehouses = Set.of(100L);
        try (
            var ignored1 = mockGetBusinessWarehouse(businessWarehouseId, 213);
            var ignored2 = mockGetWarehouses(availableWarehouses, 1);
            var ignored3 = mockPartnerTo()
        ) {
            getAvailableShipmentOptions(1, businessWarehouseId)
                .andExpect(status().isOk())
                .andExpect(jsonContent("controller/partner/available-warehouses/search_result_parent.json"));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetActiveRelation();
        verifyGetPartners(Set.of(SHOP_PARTNER_ID));
    }

    @Test
    @DisplayName("Неактивная логистическая точка")
    @DatabaseSetup("/repository/logistic-point-availability/before/parent_location.xml")
    void inactiveLogisticPoint() throws Exception {
        long businessWarehouseId = SHOP_PARTNER_ID;
        try (var ignored = mockGetBusinessWarehouse(businessWarehouseId, 1)) {
            getAvailableShipmentOptions(1, businessWarehouseId)
                .andExpect(status().isOk())
                .andExpect(content().json(EMPTY_ARRAY));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetWarehouses(Set.of(100L));
        verifyGetActiveRelation();
        verifyGetPartners(Set.of(SHOP_PARTNER_ID));
    }

    @Test
    @DisplayName("Расписание заборов")
    @DatabaseSetup("/repository/logistic-point-availability/before/dropship_withdraw.xml")
    void withdrawSchedule() throws Exception {
        long businessWarehouseId = SHOP_PARTNER_ID;
        Set<Long> availableWarehouses = Set.of(100L);
        try (
            var ignored1 = mockGetBusinessWarehouse(businessWarehouseId, 11117);
            var ignored2 = mockGetWarehouses(availableWarehouses, 11117)
        ) {
            getAvailableShipmentOptions(1, businessWarehouseId)
                .andExpect(status().isOk())
                .andExpect(jsonContent("controller/partner/available-warehouses/search_result_withdraw.json"));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetActiveRelation();
        verifyGetPartners(Set.of(SHOP_PARTNER_ID));
    }

    @Test
    @DisplayName("Склад доступен для нескольких локаций, возвращается ближайший")
    @DatabaseSetup("/repository/logistic-point-availability/before/multiple_locations.xml")
    void multipleLocations() throws Exception {
        long businessWarehouseId = SHOP_PARTNER_ID;
        Set<Long> availableWarehouses = Set.of(200L);

        try (
            var ignored1 = mockGetBusinessWarehouse(businessWarehouseId, 121908);
            var ignored2 = mockGetWarehouses(availableWarehouses, 121905);
            var ignored3 = mockPartnerTo()
        ) {
            getAvailableShipmentOptions(1, businessWarehouseId)
                .andExpect(status().isOk())
                .andExpect(jsonContent(
                    "controller/partner/available-warehouses/search_result_multiple_locations.json"
                ));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetActiveRelation();
        verifyGetPartners(Set.of(SHOP_PARTNER_ID));
    }

    @Test
    @DisplayName("Несколько вариантов самопривоза, возвращаются все")
    @DatabaseSetup("/repository/logistic-point-availability/before/multiple_import.xml")
    void multipleImport() throws Exception {
        long businessWarehouseId = SHOP_PARTNER_ID;
        Set<Long> availableWarehouses = Set.of(100L, 200L);

        try (
            var ignored1 = mockGetBusinessWarehouse(businessWarehouseId, 121908);
            var ignored2 = mockGetWarehouses(availableWarehouses, 121905);
            var ignored3 = mockPartnerTo()
        ) {
            getAvailableShipmentOptions(1, businessWarehouseId)
                .andExpect(status().isOk())
                .andExpect(jsonContent("controller/partner/available-warehouses/search_result_multiple_import.json"));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetActiveRelation();
        verifyGetPartners(Set.of(SHOP_PARTNER_ID));
    }

    @Test
    @DisplayName("Несколько вариантов забора, возвращается лучший")
    @DatabaseSetup("/repository/logistic-point-availability/before/multiple_withdraw.xml")
    void multipleWithdraw() throws Exception {
        long businessWarehouseId = SHOP_PARTNER_ID;
        Set<Long> availableWarehouses = Set.of(200L);
        try (
            var ignored1 = mockGetBusinessWarehouse(businessWarehouseId, 121908);
            var ignored2 = mockGetWarehouses(availableWarehouses, 121908)
        ) {
            getAvailableShipmentOptions(1, businessWarehouseId)
                .andExpect(status().isOk())
                .andExpect(jsonContent("controller/partner/available-warehouses/search_result_multiple_withdraw.json"));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetActiveRelation();
        verifyGetPartners(Set.of(SHOP_PARTNER_ID));
    }

    @Test
    @DisplayName("Нет свободных мест для подключения")
    @DatabaseSetup("/repository/logistic-point-availability/before/no_vacancy.xml")
    void noVacancy() throws Exception {
        long businessWarehouseId = SHOP_PARTNER_ID;
        try (var ignored = mockGetBusinessWarehouse(businessWarehouseId, 121908)) {
            getAvailableShipmentOptions(1, businessWarehouseId)
                .andExpect(status().isOk())
                .andExpect(content().json(EMPTY_ARRAY));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetActiveRelation();
        verifyGetPartners(Set.of(SHOP_PARTNER_ID));
    }

    @Test
    @DisplayName("Нет свободных мест, но существует связка")
    @DatabaseSetup("/repository/logistic-point-availability/before/no_vacancy.xml")
    void noVacancyExistingRelation() throws Exception {
        long businessWarehouseId = SHOP_PARTNER_ID;
        long availableWarehouse = 100L;
        Set<Long> availableWarehouses = Set.of(availableWarehouse);
        try (
            var ignored1 = mockGetBusinessWarehouse(businessWarehouseId, 11117);
            var ignored2 = mockGetWarehouses(availableWarehouses, 11117);
            var ignored3 = mockGetActiveRelation(availableWarehouse)
        ) {
            getAvailableShipmentOptions(1, businessWarehouseId)
                .andExpect(status().isOk())
                .andExpect(jsonContent("controller/partner/available-warehouses/search_result_withdraw_selected.json"));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetPartners(Set.of(TO_PARTNER_ID, SHOP_PARTNER_ID));
    }

    @Test
    @DisplayName("Недоступно из-за превышения порогового капасити по заказам")
    @DatabaseSetup("/repository/logistic-point-availability/before/no_order_vacancy.xml")
    void noOrderVacancy() throws Exception {
        long businessWarehouseId = SHOP_PARTNER_ID;
        try (var ignored = mockGetBusinessWarehouse(businessWarehouseId, 121908)) {
            getAvailableShipmentOptions(1, businessWarehouseId)
                .andExpect(status().isOk())
                .andExpect(content().json(EMPTY_ARRAY));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetActiveRelation();
        verifyGetPartners(Set.of(SHOP_PARTNER_ID));
    }

    @Test
    @DisplayName("Существует связка для доступного варианта")
    @DatabaseSetup("/repository/logistic-point-availability/before/dropship_withdraw.xml")
    void existingRelation() throws Exception {
        long businessWarehouseId = SHOP_PARTNER_ID;
        long availableWarehouse = 100L;
        Set<Long> availableWarehouses = Set.of(availableWarehouse);
        try (
            var ignored1 = mockGetBusinessWarehouse(businessWarehouseId, 11117);
            var ignored2 = mockGetWarehouses(availableWarehouses, 11117);
            var ignored3 = mockGetActiveRelation(availableWarehouse)
        ) {
            getAvailableShipmentOptions(1, businessWarehouseId)
                .andExpect(status().isOk())
                .andExpect(jsonContent("controller/partner/available-warehouses/search_result_withdraw_selected.json"));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetPartners(Set.of(TO_PARTNER_ID, SHOP_PARTNER_ID));
    }

    @Test
    @DisplayName("Существует связка для доступного варианта")
    @DatabaseSetup("/repository/logistic-point-availability/before/dropship_withdraw_unavailable_returns.xml")
    void existingRelationUnavailableReturns() throws Exception {
        long businessWarehouseId = SHOP_PARTNER_ID;
        long availableWarehouse = 100L;
        try (
            var ignored1 = mockGetBusinessWarehouse(businessWarehouseId, 11117);
            var ignored2 = mockGetActiveRelation(availableWarehouse)
        ) {
            getAvailableShipmentOptions(
                1,
                businessWarehouseId,
                new AvailableShipmentOptionsFilter().setShowOnlyReturnEnabled(true)
            )
                .andExpect(status().isOk())
                .andExpect(jsonContent(
                    "controller/partner/available-warehouses/search_result_withdraw_selected_unavailable_returns.json"
                ));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetPartners(Set.of(TO_PARTNER_ID, SHOP_PARTNER_ID));
    }

    @Test
    @DisplayName("Существует связка без указания склада для доступного варианта")
    @DatabaseSetup("/repository/logistic-point-availability/before/dropship_withdraw.xml")
    void existingRelationWithoutLogisticPoint() throws Exception {
        long businessWarehouseId = SHOP_PARTNER_ID;
        long availableWarehouse = 100L;
        Set<Long> availableWarehouses = Set.of(availableWarehouse);
        try (
            var ignored1 = mockGetBusinessWarehouse(businessWarehouseId, 11117);
            var ignored2 = mockGetWarehouses(availableWarehouses, 11117);
            var ignored3 = mockGetActiveRelation(null);
            var ignored4 = mockGetPartnerWarehouse(availableWarehouse)
        ) {
            getAvailableShipmentOptions(1, businessWarehouseId)
                .andExpect(status().isOk())
                .andExpect(jsonContent("controller/partner/available-warehouses/search_result_withdraw_selected.json"));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetPartners(Set.of(TO_PARTNER_ID, SHOP_PARTNER_ID));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Существует связка с экспрессом и вариант экспресса")
    @DatabaseSetup("/repository/logistic-point-availability/before/withdraw_express.xml")
    void existingRelationWithWithdrawExpress(
        @SuppressWarnings("unused") String name,
        Integer defaultHandlingTime,
        List<Integer> availableHandlingTimes,
        List<PartnerExternalParam> params,
        String responsePath
    ) throws Exception {
        long businessWarehouseId = SHOP_PARTNER_ID;
        long availableWarehouse = 100L;
        Set<Long> availableWarehouses = Set.of(availableWarehouse, 101L);
        featureProperties.setExpressDefaultHandlingTimeMinutes(defaultHandlingTime);
        featureProperties.setExpressAvailableHandlingTimesMinutes(availableHandlingTimes);
        try (
            var ignored1 = mockGetBusinessWarehouse(businessWarehouseId, 11117);
            var ignored2 = mockGetWarehouses(availableWarehouses, 11117);
            var ignored3 = mockGetActiveRelation(availableWarehouse);
            var ignored4 = mockGetPartnersExpress(TO_PARTNER_ID, SHOP_PARTNER_ID, params)
        ) {
            getAvailableShipmentOptions(1, businessWarehouseId)
                .andExpect(status().isOk())
                .andExpect(jsonContent(responsePath));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
    }

    @Nonnull
    private static Stream<Arguments> existingRelationWithWithdrawExpress() {
        return Stream.of(
            Arguments.of(
                "Параметр false, отдаем пустой список и дефолтное время",
                30,
                EXPRESS_AVAILABLE_HANDLING_TIME_MINUTES,
                List.of(
                    new PartnerExternalParam(
                        "EXPRESS_CUSTOM_HANDLING_TIME_ENABLED",
                        "Разрешено кастомное время",
                        "0"
                    )
                ),
                "controller/partner/available-warehouses/search_result_multiple_express.json"
            ),
            Arguments.of(
                "Валидный партнер с параметром null, отдаем заполненный список",
                30,
                EXPRESS_AVAILABLE_HANDLING_TIME_MINUTES,
                List.of(),
                "controller/partner/available-warehouses/search_result_multiple_express_custom_handling.json"
            ),
            Arguments.of(
                "Валидный партнер, отдаем заполненный список",
                30,
                EXPRESS_AVAILABLE_HANDLING_TIME_MINUTES,
                List.of(
                    new PartnerExternalParam(
                        "EXPRESS_CUSTOM_HANDLING_TIME_ENABLED",
                        "Разрешено кастомное время",
                        "1"
                    )
                ),
                "controller/partner/available-warehouses/search_result_multiple_express_custom_handling.json"
            )
        );
    }

    @Test
    @DisplayName("Варианты экспресса, выбирается лучший")
    @DatabaseSetup("/repository/logistic-point-availability/before/withdraw_express.xml")
    void existingRelationsWithWithdrawExpress() throws Exception {
        long businessWarehouseId = SHOP_PARTNER_ID;
        Set<Long> availableWarehouses = Set.of(101L);
        try (
            var ignored1 = mockGetBusinessWarehouse(businessWarehouseId, 11117);
            var ignored2 = mockGetWarehouses(availableWarehouses, 11117)
        ) {
            getAvailableShipmentOptions(1, businessWarehouseId)
                .andExpect(status().isOk())
                .andExpect(jsonContent("controller/partner/available-warehouses/search_result_withdraw_express.json"));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetActiveRelation();
        verifyGetPartners(Set.of(SHOP_PARTNER_ID));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Фильтр по типу точки сдачи")
    @DatabaseSetup({
        "/repository/logistic-point-availability/before/multiple_import.xml",
        "/repository/logistic-point-availability/before/another_partner_import.xml",
    })
    void pointTypeFilter(
        List<ShipmentPointType> pointTypes,
        String responsePath
    ) throws Exception {
        int locationId = 121905;
        Set<Long> warehouseIds = Set.of(200L, 210L);
        long dropoffPartner = 3000L;
        when(lmsClient.getLogisticsPoints(warehousesFilter(warehouseIds)))
            .thenReturn(List.of(
                warehouse(200L, locationId, TO_PARTNER_ID).build(),
                warehouse(210L, locationId, dropoffPartner).build()
            ));
        SearchPartnerFilter filter = SearchPartnerFilter.builder()
            .setIds(Set.of(TO_PARTNER_ID, dropoffPartner))
            .build();
        when(lmsClient.searchPartners(filter))
            .thenReturn(List.of(
                shipmentPartner(TO_PARTNER_ID, false),
                shipmentPartner(dropoffPartner, true)
            ));
        try (
            var ignored1 = mockGetBusinessWarehouse(SHOP_PARTNER_ID, locationId)
        ) {
            getAvailableShipmentOptions(
                1,
                SHOP_PARTNER_ID,
                new AvailableShipmentOptionsFilter().setPointTypes(Set.copyOf(pointTypes))
            )
                .andExpect(status().isOk())
                .andExpect(jsonContent(responsePath));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetWarehouses(warehouseIds);
        verifyGetActiveRelation();
        verify(lmsClient).searchPartners(filter);
        verifyGetPartners(Set.of(SHOP_PARTNER_ID));
    }

    @Nonnull
    private static Stream<Arguments> pointTypeFilter() {
        return Stream.of(
            Arguments.of(
                List.of(ShipmentPointType.SORTING_CENTER),
                "controller/partner/available-warehouses/search_result_sc.json"
            ),
            Arguments.of(
                List.of(ShipmentPointType.DROPOFF),
                "controller/partner/available-warehouses/search_result_dropoff.json"
            ),
            Arguments.of(
                List.of(ShipmentPointType.SORTING_CENTER, ShipmentPointType.DROPOFF),
                "controller/partner/available-warehouses/search_result_sc_and_dropoff.json"
            )
        );
    }

    @Test
    @DisplayName("Фильтр по капасити")
    @DatabaseSetup("/repository/logistic-point-availability/before/multiple_import.xml")
    @DatabaseSetup(
        value = "/repository/logistic-point-availability/before/multiple_import_capacity.xml",
        type = DatabaseOperation.UPDATE
    )
    void capacityFilter() throws Exception {
        try (
            var ignored1 = mockGetBusinessWarehouse(SHOP_PARTNER_ID, 121908);
            var ignored2 = mockGetWarehouses(Set.of(200L), 121905);
            var ignored3 = mockPartnerTo()
        ) {
            getAvailableShipmentOptions(
                1,
                SHOP_PARTNER_ID,
                new AvailableShipmentOptionsFilter().setCapacityValue(75)
            )
                .andExpect(status().isOk())
                .andExpect(jsonContent("controller/partner/available-warehouses/search_result_sc_capacity.json"));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetActiveRelation();
        verifyGetPartners(Set.of(SHOP_PARTNER_ID));
    }

    @Test
    @DisplayName("Фильтр по карго-типам")
    @DatabaseSetup({
        "/repository/logistic-point-availability/before/multiple_import.xml",
        "/repository/logistic-point-availability/before/multiple_import_cargo_types.xml",
    })
    void cargoTypesFilter() throws Exception {
        try (
            var ignored1 = mockGetBusinessWarehouse(SHOP_PARTNER_ID, 121908);
            var ignored2 = mockGetWarehouses(Set.of(200L), 121905);
            var ignored3 = mockPartnerTo()
        ) {
            getAvailableShipmentOptions(
                1,
                SHOP_PARTNER_ID,
                new AvailableShipmentOptionsFilter().setCargoTypes(Set.of(900, 1000, 1100))
            )
                .andExpect(status().isOk())
                .andExpect(jsonContent("controller/partner/available-warehouses/search_result_sc_cargo_types.json"));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetActiveRelation();
        verifyGetPartners(Set.of(SHOP_PARTNER_ID));
    }

    @MethodSource
    @SneakyThrows
    @ParameterizedTest(name = "{0}")
    @DisplayName("Фильтр по дропоффам выдающим невыкупы")
    @DatabaseSetup({
        "/repository/logistic-point-availability/before/multiple_import.xml",
        "/repository/logistic-point-availability/before/return_sc.xml"
    })
    void returnSortingCenterFilter(
        @SuppressWarnings("unused") String name,
        Boolean hideWithReturnDisabled,
        String expectedJsonPath,
        Set<Long> mockLogisticPoints
    ) {
        try (
            var ignored1 = mockGetBusinessWarehouse(SHOP_PARTNER_ID, 121908);
            var ignored2 = mockGetWarehouses(mockLogisticPoints, 121905);
            var ignored3 = mockPartnerTo()
        ) {
            getAvailableShipmentOptions(
                1,
                SHOP_PARTNER_ID,
                new AvailableShipmentOptionsFilter().setShowOnlyReturnEnabled(hideWithReturnDisabled)
            )
                .andExpect(status().isOk())
                .andExpect(jsonContent(expectedJsonPath));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetActiveRelation();
        verifyGetPartners(Set.of(SHOP_PARTNER_ID));
    }

    @Nonnull
    private static Stream<Arguments> returnSortingCenterFilter() {
        return Stream.of(
            Arguments.of(
                "Показывать только дропоффы выдающие невыкупы",
                true,
                "controller/partner/available-warehouses/search_result_sc.json",
                Set.of(200L)
            ),
            Arguments.of(
                "Показывать все дропоффы",
                false,
                "controller/partner/available-warehouses/search_result_with_return_sc.json",
                Set.of(100L, 200L, 543L)
            )
        );
    }

    @Test
    @DisplayName("Фильтр для поиска точек с описанием причин недоступности")
    @DatabaseSetup({
        "/repository/logistic-point-availability/before/multiple_import.xml",
        "/repository/logistic-point-availability/before/multiple_import_cargo_types.xml",
    })
    @DatabaseSetup(
        value = "/repository/logistic-point-availability/before/multiple_import_capacity.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/repository/logistic-point-availability/before/multiple_import_no_vacancy.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/repository/logistic-point-availability/before/multiple_import_no_order_vacancy.xml",
        type = DatabaseOperation.UPDATE
    )
    void disabledReasonsFilter() throws Exception {
        when(dataCampClient.getCargoTypes(SHOP_BUSINESS_ID, 1)).thenReturn(List.of(1200));
        try (
            var ignored1 = mockGetBusinessWarehouse(SHOP_PARTNER_ID, 121908);
            var ignored2 = mockGetWarehouses(Set.of(100L, 200L), 121905);
            var ignored3 = mockPartnerTo()
        ) {
            getAvailableShipmentOptions(
                1,
                SHOP_PARTNER_ID,
                new AvailableShipmentOptionsFilter()
                    .setCargoTypes(Set.of(900, 1000, 1100))
                    .setCapacityValue(75)
                    .setShowDisabled(true)
                    .setMaxOrdersCapacityPercent(50)
            )
                .andExpect(status().isOk())
                .andExpect(jsonContent(
                    "controller/partner/available-warehouses/search_result_disabled_reasons.json"
                ));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetActiveRelation();
        verifyGetPartners(Set.of(SHOP_PARTNER_ID));
    }

    @Test
    @DisplayName("Не падаем, если в cargoTypes пришла пустая коллекция")
    void cargoTypesEmptyCollection() throws Exception {
        try (var ignored = mockGetBusinessWarehouse(SHOP_PARTNER_ID, 213)) {
            mockMvc.perform(
                    get("/back-office/business/warehouses/{id}/available-shipment-options", SHOP_PARTNER_ID)
                        .param("shopId", String.valueOf(1))
                        .param("userId", "1")
                        .param("cargoTypes", "")
                )
                .andExpect(status().isOk())
                .andExpect(content().json(EMPTY_ARRAY));

        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetActiveRelation();
        verifyGetPartners(Set.of(SHOP_PARTNER_ID));
    }

    @Test
    @DisplayName("Выходные дни")
    @DatabaseSetup("/repository/logistic-point-availability/before/multiple_import.xml")
    @DatabaseSetup("/repository/logistic-point-availability/before/dayoffs.xml")
    void dayoffs() throws Exception {
        long businessWarehouseId = SHOP_PARTNER_ID;
        Set<Long> availableWarehouses = Set.of(100L, 200L);

        try (
            var ignored1 = mockGetBusinessWarehouse(businessWarehouseId, 121908);
            var ignored2 = mockGetWarehouses(availableWarehouses, 121905);
            var ignored3 = mockPartnerTo()
        ) {
            getAvailableShipmentOptions(1, businessWarehouseId)
                .andExpect(status().isOk())
                .andExpect(jsonContent("controller/partner/available-warehouses/search_result_with_dayoffs.json"));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetActiveRelation();
        verifyGetPartners(Set.of(SHOP_PARTNER_ID));
    }

    @Test
    @DisplayName("С возвратным СЦ")
    @DatabaseSetup("/repository/logistic-point-availability/before/multiple_import.xml")
    @DatabaseSetup(
        value = "/repository/logistic-point-availability/before/return_sc.xml",
        type = DatabaseOperation.INSERT
    )
    void withReturnSortingCenter() throws Exception {
        long businessWarehouseId = SHOP_PARTNER_ID;
        long returnSortingCenterId = 543;
        Set<Long> availableWarehouses = Set.of(100L, 200L, returnSortingCenterId);
        try (
            var ignored1 = mockGetBusinessWarehouse(businessWarehouseId, 121908);
            var ignored2 = mockGetWarehouses(availableWarehouses, 121905);
            var ignored3 = mockPartnerTo()
        ) {
            getAvailableShipmentOptions(1, businessWarehouseId)
                .andExpect(status().isOk())
                .andExpect(jsonContent("controller/partner/available-warehouses/search_result_with_return_sc.json"));
        }
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
        verifyGetActiveRelation();
        verifyGetPartners(Set.of(SHOP_PARTNER_ID));
    }

    @Nonnull
    @Override
    protected ResultActions getAvailableShipmentOptions(long shopId, long warehouseId) throws Exception {
        return getAvailableShipmentOptions(shopId, warehouseId, null);
    }

    @Nonnull
    private ResultActions getAvailableShipmentOptions(
        long shopId,
        long warehouseId,
        @Nullable AvailableShipmentOptionsFilter filter
    ) throws Exception {
        var request = get("/back-office/business/warehouses/{id}/available-shipment-options", warehouseId)
            .param("shopId", String.valueOf(shopId))
            .param("userId", "1");
        if (filter != null) {
            request.params(QueryParamUtils.toParams(filter));
        }
        return mockMvc.perform(request);
    }
}
