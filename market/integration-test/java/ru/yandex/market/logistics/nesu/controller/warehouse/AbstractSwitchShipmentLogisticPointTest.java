package ru.yandex.market.logistics.nesu.controller.warehouse;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.capacity.PartnerCapacityFilter;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationCreateDto;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationUpdateDto;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.CutoffResponse;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseResponse;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCapacityDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.CapacityType;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.nesu.jobs.producer.RemoveDropoffShopBannerProducer;
import ru.yandex.market.logistics.nesu.service.lms.PlatformClientId;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DatabaseSetup({
    "/service/shop/prepare_database.xml",
    "/jobs/consumer/switch_shipment_logistic_point/shop_partner_settings.xml",
    "/jobs/consumer/switch_shipment_logistic_point/logistic_point_availability.xml",
})
@DisplayName("Переключение партнера на СЦ")
public class AbstractSwitchShipmentLogisticPointTest extends AbstractContextualTest {

    protected static final Instant NOW = Instant.parse("2021-06-10T14:00:00.00Z");
    protected static final Long SHOP_ID = 3L;
    protected static final Long DROPOFF_LOGISTICS_POINT_ID = 2101L;
    protected static final Long SORTING_CENTER_ID = 3001L;
    protected static final Long SORTING_CENTER_LOGISTICS_POINT_ID = 3101L;
    protected static final Long CLOSEST_SORTING_CENTER_LOGISTICS_POINT_ID = 5101L;
    protected static final Long SORTING_CENTER_ANOTHER_REGION_ID = 4001L;
    protected static final Long SORTING_CENTER_ANOTHER_REGION_LOGISTICS_POINT_ID = 4101L;
    protected static final Long PARTNER_CAPACITY_ID = 1L;
    protected static final Long PARTNER_ID = 1001L;
    protected static final Long DROPOFF_ID = 2001L;
    protected static final Long PARTNER_RELATION_ID = 1L;

    /**
     * Дефолтные параметры капасити, по которым создается/обновляется значение.
     */
    protected static final Integer DEFAULT_LOCATION = 225;
    protected static final LocalDate DEFAULT_CAPACITY_DAY = null;
    protected static final Long DEFAULT_PLATFORM_CLIENT_ID = PlatformClientId.BERU.getId();
    protected static final CapacityType DEFAULT_CAPACITY_TYPE = CapacityType.REGULAR;

    protected static final Long BUSINESS_WAREHOUSE_ID = 1101L;
    private static final Long BUSINESS_ID = 41L;

    @Autowired
    protected LMSClient lmsClient;
    @Autowired
    protected MbiApiClient mbiApiClient;
    @Autowired
    protected FeatureProperties featureProperties;
    @Autowired
    protected RemoveDropoffShopBannerProducer removeDropoffShopBannerProducer;

    protected void assertPartnerRelations(
        PartnerRelationUpdateDto partnerRelationUpdateDto,
        PartnerRelationCreateDto partnerRelationCreateDto
    ) {
        softly.assertThat(partnerRelationUpdateDto.getEnabled())
            .isFalse();

        softly.assertThat(partnerRelationUpdateDto.getFromPartnerId())
            .isEqualTo(partnerRelationCreateDto.getFromPartnerId());
        softly.assertThat(partnerRelationUpdateDto.getShipmentType())
            .isEqualTo(partnerRelationCreateDto.getShipmentType());

        softly.assertThat(partnerRelationUpdateDto.getToPartnerLogisticsPointId())
            .isNotEqualTo(partnerRelationCreateDto.getToPartnerLogisticsPointId());
        softly.assertThat(partnerRelationUpdateDto.getToPartnerId())
            .isNotEqualTo(partnerRelationCreateDto.getToPartnerId());
    }

    protected void mockGetPartnerRelation() {
        when(lmsClient.getPartner(PARTNER_ID)).thenReturn(Optional.of(
            PartnerResponse.newBuilder()
                .id(PARTNER_ID)
                .partnerType(PartnerType.DROPSHIP)
                .businessId(BUSINESS_ID)
                .build()
        ));

        when(lmsClient.searchPartnerRelation(
            PartnerRelationFilter.newBuilder()
                .fromPartnerId(PARTNER_ID)
                .build()
        )).thenReturn(List.of(createPartnerRelation(ShipmentType.IMPORT)));

        when(lmsClient.getPartner(DROPOFF_ID)).thenReturn(Optional.of(
            PartnerResponse.newBuilder()
                .id(DROPOFF_ID)
                .build()
        ));

        when(lmsClient.searchCapacity(
            PartnerCapacityFilter.newBuilder()
                .partnerIds(Set.of(PARTNER_ID))
                .locationsFrom(Set.of(DEFAULT_LOCATION.longValue()))
                .locationsTo(Set.of(DEFAULT_LOCATION.longValue()))
                .platformClientIds(Set.of(DEFAULT_PLATFORM_CLIENT_ID))
                .days(Collections.singleton(DEFAULT_CAPACITY_DAY))
                .types(Set.of(DEFAULT_CAPACITY_TYPE))
                .build()
        )).thenReturn(List.of(
            PartnerCapacityDto.newBuilder()
                .id(PARTNER_CAPACITY_ID)
                .partnerId(PARTNER_ID)
                .value(50L)
                .build()
        ));

        when(lmsClient.getWarehouseHandlingDuration(PARTNER_ID)).thenReturn(Duration.ofMinutes(40));

        when(lmsClient.getLogisticsPoint(DROPOFF_LOGISTICS_POINT_ID)).thenReturn(Optional.of(
            LogisticsPointResponse.newBuilder()
                .id(DROPOFF_LOGISTICS_POINT_ID)
                .partnerId(DROPOFF_ID)
                .address(
                    Address.newBuilder()
                        //Зеленоград
                        .locationId(216)
                        .latitude(BigDecimal.valueOf(55.969330))
                        .longitude(BigDecimal.valueOf(37.176122))
                        .build()
                )
                .build()
        ));
    }

    @Nonnull
    protected PartnerRelationEntityDto createPartnerRelation(ShipmentType shipmentType) {
        return PartnerRelationEntityDto.newBuilder()
            .id(PARTNER_RELATION_ID)
            .fromPartnerId(PARTNER_ID)
            .toPartnerId(DROPOFF_ID)
            .toPartnerLogisticsPointId(DROPOFF_LOGISTICS_POINT_ID)
            .importSchedule(Set.of(
                new ScheduleDayResponse(
                    1L,
                    1,
                    LocalTime.of(11, 0),
                    LocalTime.of(19, 0)
                )
            ))
            .cutoffs(Set.of(
                CutoffResponse.newBuilder()
                    .locationId(225)
                    .cutoffTime(LocalTime.of(10, 0))
                    .build()
            ))
            .shipmentType(shipmentType)
            .enabled(Boolean.TRUE)
            .build();
    }

    protected void mockSearchAvailableShipmentOptions() {
        when(lmsClient.getBusinessWarehouseForPartner(PARTNER_ID)).thenReturn(Optional.of(
            BusinessWarehouseResponse.newBuilder()
                .logisticsPointId(BUSINESS_WAREHOUSE_ID)
                .partnerId(PARTNER_ID)
                .address(
                    Address.newBuilder()
                        .locationId(213)
                        .build()
                )
                .build()
        ));

        when(lmsClient.getLogisticsPoints(
            LogisticsPointFilter.newBuilder()
                .ids(Set.of(
                    DROPOFF_LOGISTICS_POINT_ID,
                    CLOSEST_SORTING_CENTER_LOGISTICS_POINT_ID,
                    SORTING_CENTER_LOGISTICS_POINT_ID,
                    SORTING_CENTER_ANOTHER_REGION_LOGISTICS_POINT_ID
                ))
                .active(true)
                .build()
        )).thenReturn(List.of(
            LogisticsPointResponse.newBuilder()
                .id(DROPOFF_LOGISTICS_POINT_ID)
                .partnerId(DROPOFF_ID)
                .address(
                    Address.newBuilder()
                        .locationId(216)
                        .build()
                )
                .build(),
            LogisticsPointResponse.newBuilder()
                .id(CLOSEST_SORTING_CENTER_LOGISTICS_POINT_ID)
                .partnerId(SORTING_CENTER_ID)
                .address(
                    Address.newBuilder()
                        .locationId(213)
                        .latitude(BigDecimal.valueOf(56))
                        .longitude(BigDecimal.valueOf(37))
                        .build()
                )
                .build(),
            LogisticsPointResponse.newBuilder()
                .id(SORTING_CENTER_LOGISTICS_POINT_ID)
                .partnerId(SORTING_CENTER_ID)
                .address(
                    Address.newBuilder()
                        .locationId(213)
                        .latitude(BigDecimal.valueOf(56))
                        .longitude(BigDecimal.valueOf(40))
                        .build()
                )
                .build(),
            LogisticsPointResponse.newBuilder()
                .id(SORTING_CENTER_ANOTHER_REGION_LOGISTICS_POINT_ID)
                .partnerId(SORTING_CENTER_ANOTHER_REGION_ID)
                .address(
                    Address.newBuilder()
                        //Саранск
                        .locationId(42)
                        .build()
                )
                .build()
        ));

        when(lmsClient.searchPartners(
            SearchPartnerFilter.builder()
                .setIds(Set.of(DROPOFF_ID, SORTING_CENTER_ID, SORTING_CENTER_ANOTHER_REGION_ID))
                .build()
        )).thenReturn(List.of(
            PartnerResponse.newBuilder()
                .id(DROPOFF_ID)
                .params(List.of(
                    new PartnerExternalParam(PartnerExternalParamType.IS_DROPOFF.name(), "", "1")
                ))
                .build(),
            PartnerResponse.newBuilder()
                .id(SORTING_CENTER_ID)
                .build(),
            PartnerResponse.newBuilder()
                .id(SORTING_CENTER_ANOTHER_REGION_ID)
                .build()
        ));
    }

    protected void mockSetPartnerRelation() {
        when(lmsClient.getLogisticsPoints(
            LogisticsPointFilter.newBuilder()
                .type(PointType.WAREHOUSE)
                .partnerIds(Set.of(PARTNER_ID))
                .active(true)
                .build()
        )).thenReturn(List.of(
            LogisticsPointResponse.newBuilder()
                .id(BUSINESS_WAREHOUSE_ID)
                .partnerId(PARTNER_ID)
                .address(
                    Address.newBuilder()
                        .locationId(213)
                        .build()
                )
                .build()
        ));

        when(lmsClient.getLogisticsPoint(CLOSEST_SORTING_CENTER_LOGISTICS_POINT_ID)).thenReturn(Optional.of(
            LogisticsPointResponse.newBuilder()
                .id(CLOSEST_SORTING_CENTER_LOGISTICS_POINT_ID)
                .partnerId(SORTING_CENTER_ID)
                .active(true)
                .address(
                    Address.newBuilder()
                        .locationId(213)
                        .build()
                )
                .build()
        ));

        when(lmsClient.getPartner(SORTING_CENTER_ID)).thenReturn(Optional.of(
            PartnerResponse.newBuilder()
                .id(SORTING_CENTER_ID)
                .build()
        ));

        when(lmsClient.createPartnerRelation(any(PartnerRelationCreateDto.class)))
            .thenReturn(
                PartnerRelationEntityDto.newBuilder()
                    .fromPartnerId(PARTNER_ID)
                    .toPartnerId(SORTING_CENTER_ID)
                    .toPartnerLogisticsPointId(CLOSEST_SORTING_CENTER_LOGISTICS_POINT_ID)
                    .importSchedule(Set.of(
                        new ScheduleDayResponse(
                            1L,
                            1,
                            LocalTime.of(11, 0),
                            LocalTime.of(19, 0)
                        )
                    ))
                    .cutoffs(Set.of(
                        CutoffResponse.newBuilder()
                            .locationId(225)
                            .cutoffTime(LocalTime.of(10, 0))
                            .build()
                    ))
                    .shipmentType(ShipmentType.IMPORT)
                    .enabled(Boolean.TRUE)
                    .build()
            );
    }

    protected void mockNotFoundPoint() {
        doReturn(List.of(
            LogisticsPointResponse.newBuilder()
                .id(DROPOFF_LOGISTICS_POINT_ID)
                .partnerId(DROPOFF_ID)
                .address(
                    Address.newBuilder()
                        //Зеленоград
                        .locationId(216)
                        .build()
                )
                .build(),
            LogisticsPointResponse.newBuilder()
                .id(SORTING_CENTER_ANOTHER_REGION_LOGISTICS_POINT_ID)
                .partnerId(SORTING_CENTER_ANOTHER_REGION_ID)
                .address(
                    Address.newBuilder()
                        //Саранск
                        .locationId(42)
                        .build()
                )
                .build()
        )).when(lmsClient).getLogisticsPoints(
            LogisticsPointFilter.newBuilder()
                .ids(Set.of(
                    DROPOFF_LOGISTICS_POINT_ID,
                    CLOSEST_SORTING_CENTER_LOGISTICS_POINT_ID,
                    SORTING_CENTER_LOGISTICS_POINT_ID,
                    SORTING_CENTER_ANOTHER_REGION_LOGISTICS_POINT_ID
                ))
                .active(true)
                .build()
        );
    }

    protected void mockNotFoundPartners() {
        when(lmsClient.searchPartners(
            SearchPartnerFilter.builder()
                .setIds(Set.of(DROPOFF_ID, SORTING_CENTER_ANOTHER_REGION_ID))
                .build()
        )).thenReturn(List.of(
            PartnerResponse.newBuilder()
                .id(DROPOFF_ID)
                .params(List.of(
                    new PartnerExternalParam(PartnerExternalParamType.IS_DROPOFF.name(), "", "1")
                ))
                .build(),
            PartnerResponse.newBuilder()
                .id(SORTING_CENTER_ANOTHER_REGION_ID)
                .build()
        ));
    }

    protected void verifyLmsScNotFound() {
        verify(lmsClient).getPartner(PARTNER_ID);
        verify(lmsClient, times(2)).searchPartnerRelation(
            PartnerRelationFilter.newBuilder()
                .fromPartnerId(PARTNER_ID)
                .build()
        );
        verify(lmsClient).getPartner(DROPOFF_ID);
        verify(lmsClient).searchPartners(
            SearchPartnerFilter.builder().setIds(Set.of(DROPOFF_ID, PARTNER_ID)).build()
        );
        verify(lmsClient).searchCapacity(
            PartnerCapacityFilter.newBuilder()
                .partnerIds(Set.of(PARTNER_ID))
                .locationsFrom(Set.of(DEFAULT_LOCATION.longValue()))
                .locationsTo(Set.of(DEFAULT_LOCATION.longValue()))
                .platformClientIds(Set.of(DEFAULT_PLATFORM_CLIENT_ID))
                .days(Collections.singleton(DEFAULT_CAPACITY_DAY))
                .types(Set.of(DEFAULT_CAPACITY_TYPE))
                .build()
        );
        verify(lmsClient).getWarehouseHandlingDuration(PARTNER_ID);
        verify(lmsClient).getLogisticsPoint(DROPOFF_LOGISTICS_POINT_ID);

        verify(lmsClient).getBusinessWarehouseForPartner(PARTNER_ID);
        verify(lmsClient).getLogisticsPoints(
            LogisticsPointFilter.newBuilder()
                .ids(Set.of(
                    DROPOFF_LOGISTICS_POINT_ID,
                    CLOSEST_SORTING_CENTER_LOGISTICS_POINT_ID,
                    SORTING_CENTER_LOGISTICS_POINT_ID,
                    SORTING_CENTER_ANOTHER_REGION_LOGISTICS_POINT_ID
                ))
                .active(true)
                .build()
        );
        verify(lmsClient).searchPartners(
            SearchPartnerFilter.builder()
                .setIds(Set.of(DROPOFF_ID, SORTING_CENTER_ANOTHER_REGION_ID))
                .build()
        );
    }

    protected void verifySwitchLogisticPoint() {
        verify(lmsClient, times(2)).getPartner(PARTNER_ID);
        verify(lmsClient, times(4)).searchPartnerRelation(
            PartnerRelationFilter.newBuilder()
                .fromPartnerId(PARTNER_ID)
                .build()
        );
        verify(lmsClient, times(2)).getPartner(DROPOFF_ID);
        verify(lmsClient).searchPartners(
            SearchPartnerFilter.builder().setIds(Set.of(DROPOFF_ID, PARTNER_ID)).build()
        );
        verify(lmsClient, times(2)).searchCapacity(
            PartnerCapacityFilter.newBuilder()
                .partnerIds(Set.of(PARTNER_ID))
                .locationsFrom(Set.of(DEFAULT_LOCATION.longValue()))
                .locationsTo(Set.of(DEFAULT_LOCATION.longValue()))
                .platformClientIds(Set.of(DEFAULT_PLATFORM_CLIENT_ID))
                .days(Collections.singleton(DEFAULT_CAPACITY_DAY))
                .types(Set.of(DEFAULT_CAPACITY_TYPE))
                .build()
        );
        verify(lmsClient).getWarehouseHandlingDuration(PARTNER_ID);
        verify(lmsClient).getLogisticsPoint(DROPOFF_LOGISTICS_POINT_ID);

        verify(lmsClient).getBusinessWarehouseForPartner(PARTNER_ID);
        verify(lmsClient).getLogisticsPoints(
            LogisticsPointFilter.newBuilder()
                .ids(Set.of(
                    DROPOFF_LOGISTICS_POINT_ID,
                    CLOSEST_SORTING_CENTER_LOGISTICS_POINT_ID,
                    SORTING_CENTER_LOGISTICS_POINT_ID,
                    SORTING_CENTER_ANOTHER_REGION_LOGISTICS_POINT_ID
                ))
                .active(true)
                .build()
        );
        verify(lmsClient).searchPartners(
            SearchPartnerFilter.builder()
                .setIds(Set.of(DROPOFF_ID, SORTING_CENTER_ID, SORTING_CENTER_ANOTHER_REGION_ID))
                .build()
        );

        verify(lmsClient).getLogisticsPoints(
            LogisticsPointFilter.newBuilder()
                .type(PointType.WAREHOUSE)
                .partnerIds(Set.of(PARTNER_ID))
                .active(true)
                .build()
        );
        verify(lmsClient).getLogisticsPoint(CLOSEST_SORTING_CENTER_LOGISTICS_POINT_ID);
        verify(lmsClient).getPartner(SORTING_CENTER_ID);

        ArgumentCaptor<PartnerRelationUpdateDto> partnerRelationUpdateDtoCaptor = ArgumentCaptor.forClass(
            PartnerRelationUpdateDto.class
        );
        verify(lmsClient).updatePartnerRelation(eq(PARTNER_RELATION_ID), partnerRelationUpdateDtoCaptor.capture());
        PartnerRelationUpdateDto partnerRelationUpdateDto = partnerRelationUpdateDtoCaptor.getValue();

        ArgumentCaptor<PartnerRelationCreateDto> partnerRelationCreateDtoCaptor = ArgumentCaptor.forClass(
            PartnerRelationCreateDto.class
        );
        verify(lmsClient).createPartnerRelation(partnerRelationCreateDtoCaptor.capture());
        PartnerRelationCreateDto partnerRelationCreateDto = partnerRelationCreateDtoCaptor.getValue();

        assertPartnerRelations(partnerRelationUpdateDto, partnerRelationCreateDto);

        verify(lmsClient).updateWarehouseHandlingDuration(PARTNER_ID, Duration.ZERO);
    }

    protected void mockBannerRemoving() {
        doNothing().when(removeDropoffShopBannerProducer).produceTask(anyLong());
    }

}
