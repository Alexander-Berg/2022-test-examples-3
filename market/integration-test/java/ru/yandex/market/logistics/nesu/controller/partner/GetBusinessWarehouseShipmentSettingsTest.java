package ru.yandex.market.logistics.nesu.controller.partner;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.entity.request.capacity.PartnerCapacityFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.response.CutoffResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.CountingType;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType.DROPSHIP_EXPRESS;
import static ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType.ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED;
import static ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType.EXPRESS_RETURN_SORTING_CENTER_ID;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение дропшип связки")
@DatabaseSetup("/controller/partner/dropship_shop.xml")
class GetBusinessWarehouseShipmentSettingsTest extends AbstractPartnerControllerPartnerRelationTest {

    @AfterEach
    void verifyMocks() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Склад не подключен")
    @DatabaseSetup("/controller/partner/dropship_shop.xml")
    void deactivated() throws Exception {
        getShipmentSettings(1, 3)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP_PARTNER_SETTINGS] with fromPartnerId 3"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ValueSource(longs = {2L, 4L})
    @DisplayName("Некорректная роль магазина")
    @DatabaseSetup("/controller/business/before/settings_setup.xml")
    void wrongShopType(long shopId) throws Exception {
        getShipmentSettings(shopId, 1)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Shop must have role [DROPSHIP, SUPPLIER] for this operation"));
    }

    @Test
    @DisplayName("Отключенный магазин")
    @DatabaseSetup("/controller/business/before/settings_setup.xml")
    void disabledShop() throws Exception {
        getShipmentSettings(5, 1)
            .andExpect(status().isForbidden())
            .andExpect(noContent());
    }

    @Test
    @DisplayName("Несуществующий магазин")
    @DatabaseSetup("/controller/partner/dropship_shop.xml")
    void nonExistentShop() throws Exception {
        getShipmentSettings(100500, 1)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [100500]"));
    }

    @Test
    @DisplayName("Получение дропшип связки со способом отгрузки Экспресс")
    @DatabaseSetup("/controller/business/before/settings_setup.xml")
    void getWithdrawExpressRelationWithoutShipmentSchedule() throws Exception {
        PartnerRelationFilter partnerRelationFilter = createPartnerRelationFilter();
        PartnerCapacityFilter capacityFilter = createPartnerCapacityFilterBuilder()
            .partnerIds(Set.of(FROM_PARTNER_ID))
            .build();

        mockSearchPartnerRelation(
            partnerRelationFilter,
            createExpressPartnerRelation(PARTNER_RELATION_ID, ShipmentType.WITHDRAW)
        );

        mockGetPartner(
            FROM_PARTNER_ID,
            PartnerResponse.newBuilder()
                .id(FROM_PARTNER_ID)
                .marketId(1L)
                .partnerType(PartnerType.DROPSHIP)
                .params(createParams(
                    ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED,
                    DROPSHIP_EXPRESS,
                    EXPRESS_RETURN_SORTING_CENTER_ID
                ))
                .build()
        );

        mockGetPartner(
            TO_PARTNER_ID,
            PartnerResponse.newBuilder()
                .id(TO_PARTNER_ID)
                .marketId(1L)
                .partnerType(PartnerType.DELIVERY)
                .subtype(PartnerSubtypeResponse.newBuilder().id(34L).build())
                .intakeSchedule(new ArrayList<>(getSchedule()))
                .build()
        );
        mockSearchCapacity(capacityFilter, CountingType.ORDER, CAPACITY_VALUE);
        mockGetLogisticsPoint();
        mockGetWarehouseHandlingDurationMinutes(FROM_PARTNER_ID, 30);

        getRelation()
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/partner/relation/get_withdraw_express_partner_relation.json"
            ));

        verify(lmsClient).getPartner(FROM_PARTNER_ID);
        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
        verify(lmsClient).getPartner(TO_PARTNER_ID);
        verify(lmsClient).searchCapacity(capacityFilter);
        verify(lmsClient).getLogisticsPoint(TO_PARTNER_LOGISTICS_POINT_ID);
        verify(lmsClient).getWarehouseHandlingDuration(FROM_PARTNER_ID);
    }

    @ParameterizedTest
    @EnumSource(value = ShipmentType.class, names = {"WITHDRAW", "TPL"})
    @DisplayName("Получение дропшип связки со способом отгрузки Забор/TPL")
    @DatabaseSetup("/controller/business/before/settings_setup.xml")
    void getWithdrawRelationWithoutShipmentSchedule(ShipmentType shipmentType) throws Exception {
        PartnerRelationFilter partnerRelationFilter = createPartnerRelationFilter();
        PartnerCapacityFilter capacityFilter = createPartnerCapacityFilterBuilder()
            .partnerIds(Set.of(FROM_PARTNER_ID))
            .build();

        mockSearchPartnerRelation(
            partnerRelationFilter,
            createDropshipPartnerRelation(PARTNER_RELATION_ID, shipmentType)
        );
        mockGetPartner(FROM_PARTNER_ID, PartnerType.DROPSHIP);
        mockGetPartner(TO_PARTNER_ID, PartnerType.DELIVERY);
        mockSearchCapacity(capacityFilter, CountingType.ORDER, CAPACITY_VALUE);
        mockGetLogisticsPoint();
        mockGetWarehouseHandlingDuration(FROM_PARTNER_ID, 1);

        getRelation()
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/partner/relation/get_withdraw_ds_partner_relation.json"
            ));

        verify(lmsClient).getPartner(FROM_PARTNER_ID);
        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
        verify(lmsClient).getPartner(TO_PARTNER_ID);
        verify(lmsClient).searchCapacity(capacityFilter);
        verify(lmsClient).getLogisticsPoint(TO_PARTNER_LOGISTICS_POINT_ID);
        verify(lmsClient).getWarehouseHandlingDuration(FROM_PARTNER_ID);
    }

    private void mockGetPartner(Long partnerId, PartnerResponse partner) {
        when(lmsClient.getPartner(partnerId)).thenReturn(Optional.of(partner));
    }

    private void mockSearchPartnerRelation(
        PartnerRelationFilter partnerRelationFilter,
        PartnerRelationEntityDto expectedEntity
    ) {
        when(lmsClient.searchPartnerRelation(partnerRelationFilter)).thenReturn(List.of(expectedEntity));
    }

    @Nonnull
    private PartnerRelationEntityDto createExpressPartnerRelation(
        Long id,
        ShipmentType shipmentType
    ) {
        return PartnerRelationEntityDto.newBuilder()
            .id(id)
            .fromPartnerId(FROM_PARTNER_ID)
            .toPartnerId(TO_PARTNER_ID)
            .toPartnerLogisticsPointId(TO_PARTNER_LOGISTICS_POINT_ID)
            .handlingTime(0)
            .shipmentType(shipmentType)
            .enabled(true)
            .cutoffs(Set.of(
                CutoffResponse.newBuilder()
                    .locationId(225)
                    .cutoffTime(LocalTime.of(16, 00))
                    .packagingDuration(null)
                    .build()
            ))
            .inboundTime(Duration.ofHours(INBOUND_TIME))
            .intakeSchedule(getSchedule())
            .registerSchedule(PARTNER_RELATION_REGISTER_SCHEDULE)
            .build();
    }

    @Nonnull
    private PartnerRelationEntityDto createDropshipPartnerRelation(
        Long id,
        ShipmentType shipmentType
    ) {
        return PartnerRelationEntityDto.newBuilder()
            .id(id)
            .fromPartnerId(FROM_PARTNER_ID)
            .toPartnerId(TO_PARTNER_ID)
            .toPartnerLogisticsPointId(TO_PARTNER_LOGISTICS_POINT_ID)
            .handlingTime(0)
            .shipmentType(shipmentType)
            .enabled(true)
            .cutoffs(Set.of(
                CutoffResponse.newBuilder()
                    .locationId(225)
                    .cutoffTime(CUTOFF_TIME_VALUE)
                    .packagingDuration(Duration.ofHours(PACKAGING_DURATION_VALUE))
                    .build()
            ))
            .inboundTime(Duration.ofHours(INBOUND_TIME))
            .importSchedule(PARTNER_RELATION_IMPORT_SCHEDULE)
            .intakeSchedule(PARTNER_RELATION_INTAKE_SCHEDULE)
            .registerSchedule(PARTNER_RELATION_REGISTER_SCHEDULE)
            .build();
    }

    @Nonnull
    private List<PartnerExternalParam> createParams(PartnerExternalParamType... types) {
        return Stream.of(types)
            .map(type -> new PartnerExternalParam(type.name(), "", "1"))
            .collect(Collectors.toList());
    }

    @Nonnull
    private Set<ScheduleDayResponse> getSchedule() {
        return Set.of(
            new ScheduleDayResponse(null, 1, LocalTime.of(10, 0), LocalTime.of(17, 0)),
            new ScheduleDayResponse(null, 2, LocalTime.of(10, 0), LocalTime.of(17, 0)),
            new ScheduleDayResponse(null, 3, LocalTime.of(10, 0), LocalTime.of(17, 0)),
            new ScheduleDayResponse(null, 4, LocalTime.of(10, 0), LocalTime.of(17, 0)),
            new ScheduleDayResponse(null, 5, LocalTime.of(10, 0), LocalTime.of(17, 0))
        );
    }

    @Nonnull
    private ResultActions getShipmentSettings(long shopId, long warehouseId) throws Exception {
        return mockMvc.perform(
            get("/back-office/business/warehouses/{id}/shipment-settings", warehouseId)
                .param("shopId", String.valueOf(shopId))
                .param("userId", "1")
        );
    }

    @Nonnull
    @SneakyThrows
    public ResultActions getRelation() {
        return getShipmentSettings(6L, 1);
    }
}
