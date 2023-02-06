package ru.yandex.market.logistics.nesu.controller.partner;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.entity.request.capacity.PartnerCapacityFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.response.CutoffResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.type.CountingType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/controller/business/before/settings_crossdock.xml")
class GetBusinessWarehouseShipmentSettingsCrossdockTest extends AbstractPartnerControllerPartnerRelationTest {

    @BeforeEach
    void setup() {
        mockGetPartner(FROM_PARTNER_ID, PartnerType.SUPPLIER);
    }

    @Test
    @DisplayName("Попытка получения кроссдок связки cо способом отгрузки забор, когда связка отсутствует в LMS")
    void getRelationNotFoundInLms() throws Exception {
        PartnerRelationFilter partnerRelationFilter = createPartnerRelationFilter();

        getRelation()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PARTNER_RELATION] with fromPartnerId 1"));

        verify(lmsClient).getPartner(FROM_PARTNER_ID);
        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
    }

    @Test
    @DisplayName("Получение кроссдок связки со способом отгрузки забор")
    void getWithdrawRelationWithoutShipmentSchedule() throws Exception {
        PartnerRelationFilter partnerRelationFilter = createPartnerRelationFilter();
        PartnerCapacityFilter capacityFilter = createDefaultItemCapacityFilter();

        mockSearchSupplierFulfillmentPartnerRelation(partnerRelationFilter, ShipmentType.WITHDRAW, PARTNER_RELATION_ID);
        mockGetFulfillmentPartner();
        mockSearchCapacity(capacityFilter, CountingType.ITEM, CAPACITY_VALUE);
        mockGetLogisticsPoint();
        mockGetWarehouseHandlingDuration(FROM_PARTNER_ID, 1);

        getRelation()
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/partner/relation/get_withdraw_ff_partner_relation_shipment_schedule_response.json"
            ));

        verify(lmsClient).getPartner(FROM_PARTNER_ID);
        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
        verify(lmsClient).getPartner(TO_PARTNER_ID);
        verify(lmsClient).searchCapacity(capacityFilter);
        verify(lmsClient).getLogisticsPoint(TO_PARTNER_LOGISTICS_POINT_ID);
        verify(lmsClient).getWarehouseHandlingDuration(FROM_PARTNER_ID);
    }

    @Test
    @DisplayName("Получение кроссдок связки со способом отгрузки самопривоз без расписания отгрузок")
    void getWithdrawRelationWithShipmentSchedule() throws Exception {
        PartnerRelationFilter partnerRelationFilter = createPartnerRelationFilter();
        PartnerCapacityFilter capacityFilter = createDefaultItemCapacityFilter();

        mockSearchSupplierFulfillmentPartnerRelation(partnerRelationFilter, ShipmentType.IMPORT, PARTNER_RELATION_ID);
        mockGetFulfillmentPartner();
        mockSearchCapacity(capacityFilter, CountingType.ITEM, CAPACITY_VALUE);
        mockGetLogisticsPoint();
        mockGetWarehouseHandlingDuration(FROM_PARTNER_ID, 1);

        getRelation()
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/partner/relation/get_import_ff_partner_relation_shipment_schedule_response.json"
            ));

        verify(lmsClient).getPartner(FROM_PARTNER_ID);
        verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
        verify(lmsClient).getPartner(TO_PARTNER_ID);
        verify(lmsClient).searchCapacity(capacityFilter);
        verify(lmsClient).getLogisticsPoint(TO_PARTNER_LOGISTICS_POINT_ID);
        verify(lmsClient).getWarehouseHandlingDuration(FROM_PARTNER_ID);
    }

    @Nonnull
    @SneakyThrows
    protected ResultActions getRelation() {
        return mockMvc.perform(
            get("/back-office/business/warehouses/1/shipment-settings")
                .param("shopId", String.valueOf(1L))
                .param("userId", "1")
        );
    }

    @Nonnull
    private PartnerRelationEntityDto createSupplierFulfillmentPartnerRelationForSearch(
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

    private void mockSearchSupplierFulfillmentPartnerRelation(
        PartnerRelationFilter partnerRelationFilter,
        ShipmentType shipmentType,
        Long... returnedIds
    ) {
        when(lmsClient.searchPartnerRelation(partnerRelationFilter))
            .thenReturn(
                Stream.of(returnedIds)
                    .map(id -> createSupplierFulfillmentPartnerRelationForSearch(id, shipmentType))
                    .collect(Collectors.toList())
            );
    }

}
