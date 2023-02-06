package ru.yandex.market.logistics.nesu.controller.shipment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.request.schedule.CalendarsFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.CalendarHolidaysResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.service.lms.PlatformClientId;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createLogisticsPointResponse;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createLogisticsPointResponseBuilder;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createPartner;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createPartnerResponseBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение доступных вариантов отгрузки")
@DatabaseSetup("/repository/shipments/available/common.xml")
@ParametersAreNonnullByDefault
class GetAvailableShipmentsTest extends AbstractContextualTest {

    private static final long SHOP_WAREHOUSE_ID = 100;
    private static final long SHOP_ID = 10;
    private static final long SC_PARTNER_ID = 20;

    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setupMocks() {
        clock.setFixed(Instant.parse("2020-12-01T12:00:00.00Z"), ZoneId.systemDefault());

        when(lmsClient.getLogisticsPoint(SHOP_WAREHOUSE_ID))
            .thenReturn(Optional.of(shopWarehouse().build()));

        when(lmsClient.getLogisticsPoints(
            LogisticsPointFilter.newBuilder()
                .type(PointType.WAREHOUSE)
                .partnerIds(Set.of(SC_PARTNER_ID))
                .active(true)
                .build()
        )).thenReturn(List.of(scWarehouse(200)));
    }

    @Test
    @DisplayName("Неизвестный магазин")
    void unknownShop() throws Exception {
        getShipments(2, SHOP_WAREHOUSE_ID)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [2]"));
    }

    @Test
    @DisplayName("Неизвестный склад")
    void unknownWarehouse() throws Exception {
        getShipments(SHOP_ID, 200)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [200]"));
    }

    @Test
    @DisplayName("Склад с другим businessId")
    void warehouseWrongBusinessId() throws Exception {
        when(lmsClient.getLogisticsPoint(SHOP_WAREHOUSE_ID))
            .thenReturn(Optional.of(shopWarehouse().businessId(42L).build()));
        getShipments()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [100]"));
    }

    @Test
    @DisplayName("Неактивный склад")
    void inactiveWarehouse() throws Exception {
        doReturn(Optional.of(shopWarehouse().active(false).build()))
            .when(lmsClient).getLogisticsPoint(SHOP_WAREHOUSE_ID);

        getShipments()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [100]"));
    }

    @Test
    @DisplayName("Недоступный склад")
    void noWarehouseAccess() throws Exception {
        doReturn(Optional.of(shopWarehouse().businessId(null).build()))
            .when(lmsClient).getLogisticsPoint(SHOP_WAREHOUSE_ID);

        getShipments()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WAREHOUSE] with ids [100]"));
    }

    @Test
    @DisplayName("Неизвестная локация склада")
    void invalidLocation() throws Exception {
        doReturn(Optional.of(
            shopWarehouse()
                .address(
                    Address.newBuilder()
                        .locationId(-1000)
                        .build()
                )
                .build()
        )).when(lmsClient).getLogisticsPoint(SHOP_WAREHOUSE_ID);

        getShipments()
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage("no such region id in tree -1000"));
    }

    @Test
    @DisplayName("Нет настроек служб доставки")
    void noDeliverySettings() throws Exception {
        getShipments()
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY, true));
    }

    @Test
    @DisplayName("Нет доступных для отгрузки складов")
    @DatabaseSetup("/repository/shipments/available/regional_settings.xml")
    void noAvailableWarehouses() throws Exception {
        mockNonScWarehouses();

        getShipments()
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY, true));
    }

    @Test
    @DisplayName("Нет доступного способа отгрузки")
    @DatabaseSetup("/repository/shipments/available/regional_settings_sc.xml")
    @DatabaseSetup("/repository/shipments/available/warehouses_availability_limited.xml")
    void noAvailableShipmentType() throws Exception {
        mockGetWarehouses(
            LogisticsPointFilter.newBuilder()
                .active(true)
                .type(PointType.WAREHOUSE)
                .ids(Set.of(110L, 200L))
                .build(),
            createLogisticsPointResponse(110L, 9L, "Warehouse 110", PointType.WAREHOUSE),
            scWarehouse(200)
        );
        mockGetPartners(
            createPartner(9L, PartnerType.DELIVERY),
            createPartner(SC_PARTNER_ID, PartnerType.SORTING_CENTER)
        );

        getShipments()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/available/delivery_service_and_sc_limited.json"));
    }

    @Test
    @DisplayName("Региональные настройки служб доставки, без СЦ")
    @DatabaseSetup("/repository/shipments/available/regional_settings.xml")
    @DatabaseSetup("/repository/shipments/available/warehouses_availability.xml")
    void regionalSettingsWithoutSortingCenter() throws Exception {
        mockNonScWarehouses();

        getShipments()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/available/delivery_service.json"));
    }

    @Test
    @DisplayName("Региональные настройки служб доставки, СЦ")
    @DatabaseSetup("/repository/shipments/available/regional_settings_sc.xml")
    @DatabaseSetup("/repository/shipments/available/warehouses_availability.xml")
    void regionalSettingsWithSortingCenter() throws Exception {
        mockGetWarehouses(
            createLogisticsPointResponse(110L, 9L, "Warehouse 110", PointType.WAREHOUSE),
            scWarehouse(200),
            scWarehouse(210)
        );
        mockGetPartners(
            createPartner(9L, PartnerType.DELIVERY),
            createPartner(SC_PARTNER_ID, PartnerType.SORTING_CENTER)
        );

        getShipments()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/available/delivery_service_and_sc.json"));
    }

    @Test
    @DisplayName("Региональные настройки служб доставки, два сендера")
    @DatabaseSetup("/repository/shipments/available/regional_settings.xml")
    @DatabaseSetup("/repository/shipments/available/warehouses_availability.xml")
    @DatabaseSetup(
        value = "/repository/shipments/available/regional_second_sender.xml",
        type = DatabaseOperation.INSERT
    )
    void regionalSettingsMultipleSenders() throws Exception {
        mockMultipleWarehouses();

        getShipments()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/available/two_delivery_services.json"));
    }

    @Test
    @DisplayName("Региональные настройки служб доставки, два сендера с разными складами СЦ")
    @DatabaseSetup("/repository/shipments/available/regional_settings_sc.xml")
    @DatabaseSetup("/repository/shipments/available/warehouses_availability.xml")
    @DatabaseSetup(
        value = "/repository/shipments/available/regional_second_sender_sc.xml",
        type = DatabaseOperation.INSERT
    )
    void regionalSettingsMultipleSendersDifferentSc() throws Exception {
        mockGetWarehouses(
            createLogisticsPointResponse(110L, 9L, "Warehouse 110", PointType.WAREHOUSE),
            createLogisticsPointResponse(120L, 19L, "Warehouse 120", PointType.WAREHOUSE),
            scWarehouse(200),
            scWarehouse(210)
        );
        mockGetPartners(
            createPartner(9L, PartnerType.DELIVERY),
            createPartner(19L, PartnerType.DELIVERY),
            createPartner(SC_PARTNER_ID, PartnerType.SORTING_CENTER)
        );

        getShipments()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/available/two_delivery_services_two_sc.json"));
    }

    @Test
    @DisplayName("Выходные дни партнёра")
    @DatabaseSetup("/repository/shipments/available/regional_settings.xml")
    @DatabaseSetup("/repository/shipments/available/warehouses_availability.xml")
    void partnerHolidays() throws Exception {
        long calendarId = 900;

        mockGetWarehouses(createLogisticsPointResponse(110L, 9L, "Warehouse 110", PointType.WAREHOUSE));
        mockGetPartners(
            createPartnerResponseBuilder(9L, PartnerType.DELIVERY, 9000L)
                .readableName("Sample Readable Partner")
                .calendarId(calendarId)
                .build()
        );

        when(lmsClient.getHolidays(refEq(
            CalendarsFilter.builder()
                .calendarIds(List.of(calendarId))
                .dateFrom(LocalDate.of(2020, 12, 1))
                .dateTo(LocalDate.of(2021, 1, 1))
                .build()
        ))).thenReturn(List.of(
            CalendarHolidaysResponse.builder()
                .id(calendarId)
                .days(List.of(LocalDate.of(2020, 12, 25)))
                .build()
        ));

        getShipments()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/available/delivery_service_holidays.json"));
    }

    @Test
    @DisplayName("Собственная служба доставки")
    void ownDelivery() throws Exception {
        mockGetPartners(
            SearchPartnerFilter.builder()
                .setPlatformClientIds(Set.of(PlatformClientId.YANDEX_DELIVERY.getId()))
                .setStatuses(Set.of(PartnerStatus.ACTIVE))
                .setTypes(Set.of(PartnerType.OWN_DELIVERY))
                .setMarketIds(Set.of(1000L))
                .build(),
            createPartnerResponseBuilder(9L, PartnerType.OWN_DELIVERY, 200)
                .businessId(41L)
                .readableName("Sample Readable Partner")
                .build()
        );

        getShipments()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/available/own_delivery_service.json"));
    }

    @Test
    @DisplayName("Неактивный склад в настройках доступности")
    @DatabaseSetup("/repository/shipments/available/regional_settings.xml")
    @DatabaseSetup(
        value = "/repository/shipments/available/regional_second_sender.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup("/repository/shipments/available/warehouses_availability.xml")
    void inactiveWarehouseAvailability() throws Exception {
        mockNonScWarehouses();

        getShipments()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/available/delivery_service.json"));
    }

    @Test
    @DisplayName("Склад неактивного партнёра в настройках доступности")
    @DatabaseSetup("/repository/shipments/available/regional_settings.xml")
    @DatabaseSetup(
        value = "/repository/shipments/available/regional_second_sender.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup("/repository/shipments/available/warehouses_availability.xml")
    void inactivePartnerWarehouseAvailability() throws Exception {
        mockGetWarehouses(
            createLogisticsPointResponse(110L, 9L, "Warehouse 110", PointType.WAREHOUSE),
            createLogisticsPointResponse(120L, 19L, "Warehouse 120", PointType.WAREHOUSE)
        );
        mockGetPartners(
            getValidPartnersFilter(Set.of(9L, 19L)),
            createPartner(9L, PartnerType.DELIVERY)
        );

        getShipments()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/available/delivery_service.json"));
    }

    @Test
    @DisplayName("Точка с DAAS и WITHDRAW_EXPRESS приводит к 500")
    @DatabaseSetup("/repository/shipments/available/broken_warehouses_availability.xml")
    void invalidCombinationDaasAndWithdrawExpress() throws Exception {
        when(lmsClient.getLogisticsPoint(SHOP_WAREHOUSE_ID))
            .thenReturn(Optional.of(shopWarehouse().businessId(42L).build()));
        mockGetWarehouses(
            LogisticsPointFilter.newBuilder()
                .active(true)
                .type(PointType.WAREHOUSE)
                .ids(Set.of(500L))
                .build(),
            createLogisticsPointResponseBuilder(500L, 50L, "Warehouse 500", PointType.WAREHOUSE)
                .businessId(42L)
                .build()
        );

        mockGetPartners(
            createPartnerResponseBuilder(50L, PartnerType.DELIVERY, 100L).businessId(42L).build()
        );

        getShipments(50, 100)
            .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Магазин в вайтлисте")
    @DatabaseSetup("/repository/shipments/available/regional_settings.xml")
    @DatabaseSetup("/repository/shipments/available/warehouses_availability.xml")
    @DatabaseSetup("/repository/shipments/available/warehouse_available_for_shop.xml")
    void warehouseAvailableForShop() throws Exception {
        mockNonScWarehouses();

        getShipments()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shipment/available/delivery_service.json"));
    }

    @Test
    @DisplayName("Магазин не в вайтлисте")
    @DatabaseSetup(
        value = "/repository/shipments/available/whitelist_shop.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup("/repository/shipments/available/regional_settings.xml")
    @DatabaseSetup("/repository/shipments/available/warehouses_availability.xml")
    @DatabaseSetup("/repository/shipments/available/warehouse_unavailable_for_shop.xml")
    void warehouseUnavailableForShop() throws Exception {
        mockNonScWarehouses();

        getShipments()
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY));
    }

    private void mockNonScWarehouses() {
        mockGetWarehouses(createLogisticsPointResponse(110L, 9L, "Warehouse 110", PointType.WAREHOUSE));
        mockGetPartners(createPartner(9L, PartnerType.DELIVERY));
    }

    private void mockMultipleWarehouses() {
        mockGetWarehouses(
            createLogisticsPointResponse(110L, 9L, "Warehouse 110", PointType.WAREHOUSE),
            createLogisticsPointResponse(120L, 19L, "Warehouse 120", PointType.WAREHOUSE)
        );
        mockGetPartners(
            createPartner(9L, PartnerType.DELIVERY),
            createPartner(19L, PartnerType.DELIVERY)
        );
    }

    private void mockGetWarehouses(LogisticsPointResponse... warehouses) {
        mockGetWarehouses(
            LogisticsPointFilter.newBuilder()
                .active(true)
                .type(PointType.WAREHOUSE)
                .ids(Set.of(110L, 120L, 200L, 210L))
                .build(),
            warehouses
        );
    }

    private void mockGetWarehouses(LogisticsPointFilter filter, LogisticsPointResponse... warehouses) {
        when(lmsClient.getLogisticsPoints(filter)).thenReturn(List.of(warehouses));
    }

    private void mockGetPartners(PartnerResponse... partners) {
        mockGetPartners(
            getValidPartnersFilter(Stream.of(partners).map(PartnerResponse::getId).collect(Collectors.toSet())),
            partners
        );
    }

    @Nonnull
    private SearchPartnerFilter getValidPartnersFilter(Set<Long> partnerIds) {
        return SearchPartnerFilter.builder()
            .setStatuses(Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING))
            .setPlatformClientIds(Set.of(PlatformClientId.YANDEX_DELIVERY.getId()))
            .setPlatformClientStatuses(Set.of(PartnerStatus.ACTIVE))
            .setIds(partnerIds)
            .build();
    }

    private void mockGetPartners(SearchPartnerFilter filter, PartnerResponse... partners) {
        when(lmsClient.searchPartners(filter)).thenReturn(List.of(partners));
    }

    @Nonnull
    private LogisticsPointResponse.LogisticsPointResponseBuilder shopWarehouse() {
        return createLogisticsPointResponseBuilder(
            SHOP_WAREHOUSE_ID,
            null,
            "Shop warehouse",
            PointType.WAREHOUSE
        )
            .address(Address.newBuilder().locationId(213).build());
    }

    @Nonnull
    private LogisticsPointResponse scWarehouse(long id) {
        return createLogisticsPointResponse(id, SC_PARTNER_ID, "SC warehouse", PointType.WAREHOUSE);
    }

    @Nonnull
    private ResultActions getShipments() throws Exception {
        return getShipments(SHOP_ID, SHOP_WAREHOUSE_ID);
    }

    @Nonnull
    private ResultActions getShipments(long shopId, long warehouseId) throws Exception {
        return mockMvc.perform(
            get("/back-office/shipments/available")
                .param("userId", "1")
                .param("shopId", String.valueOf(shopId))
                .param("warehouseId", String.valueOf(warehouseId))
        );
    }

}
