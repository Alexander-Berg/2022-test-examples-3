package ru.yandex.market.logistics.nesu.controller.partner;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.capacity.PartnerCapacityFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCapacityDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.CapacityType;
import ru.yandex.market.logistics.management.entity.type.CountingType;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.model.LmsFactory;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AbstractPartnerControllerPartnerRelationTest extends AbstractContextualTest {

    static final long PARTNER_RELATION_ID = 1L;
    static final long FROM_PARTNER_ID = 1L;
    static final long TO_PARTNER_ID = 2L;
    static final long TO_PARTNER_LOGISTICS_POINT_ID = 4L;

    static final Set<ScheduleDayResponse> WAREHOUSE_SCHEDULE = Set.of(createScheduleDay(1L, 1));
    static final Set<ScheduleDayResponse> PARTNER_RELATION_IMPORT_SCHEDULE = Set.of(createScheduleDay(3L, 3));
    static final Set<ScheduleDayResponse> PARTNER_RELATION_INTAKE_SCHEDULE = Set.of(createScheduleDay(1L, 1));
    static final Set<ScheduleDayResponse> PARTNER_RELATION_REGISTER_SCHEDULE = Set.of(createScheduleDay(4L, 4));

    static final long CAPACITY_VALUE = 100L;
    static final long INBOUND_TIME = 6;
    static final int PACKAGING_DURATION_VALUE = 24;
    static final LocalTime CUTOFF_TIME_VALUE = LocalTime.of(10, 0);

    @Autowired
    protected LMSClient lmsClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    void mockGetPartner(Long partnerId, @Nullable PartnerType partnerType) {
        when(lmsClient.getPartner(partnerId))
            .thenReturn(Optional.of(createPartner(partnerId, partnerType)));
    }

    void mockGetFulfillmentPartner() {
        mockGetPartner(TO_PARTNER_ID, PartnerType.FULFILLMENT);
    }

    void mockGetLogisticsPoint() {
        when(lmsClient.getLogisticsPoint(TO_PARTNER_LOGISTICS_POINT_ID))
            .thenReturn(Optional.of(createWarehouse()));
    }

    void mockSearchCapacity(PartnerCapacityFilter capacityFilter, CountingType countingType, Long... returnedIds) {
        when(lmsClient.searchCapacity(capacityFilter))
            .thenReturn(
                Stream.of(returnedIds)
                    .map(id -> createCapacity(id, countingType))
                    .collect(Collectors.toList())
            );
    }

    void mockGetWarehouseHandlingDuration(long partnerId, int handlingDurationInDays) {
        when(lmsClient.getWarehouseHandlingDuration(partnerId))
            .thenReturn(Duration.ofDays(handlingDurationInDays));
    }

    void mockGetWarehouseHandlingDurationMinutes(long partnerId, int handlingDurationInMinutes) {
        when(lmsClient.getWarehouseHandlingDuration(partnerId))
            .thenReturn(Duration.ofMinutes(handlingDurationInMinutes));
    }

    @Nonnull
    static ScheduleDayResponse createScheduleDay(Long id, Integer day) {
        return LmsFactory.createScheduleDayDto(id, day);
    }

    @Nonnull
    PartnerRelationFilter createPartnerRelationFilter() {
        return PartnerRelationFilter.newBuilder()
            .fromPartnerId(FROM_PARTNER_ID)
            .build();
    }

    @Nonnull
    LogisticsPointResponse createWarehouse() {
        return LmsFactory.createLogisticsPointResponseBuilder(
            TO_PARTNER_LOGISTICS_POINT_ID,
            TO_PARTNER_ID,
            "warehouse",
            PointType.WAREHOUSE
        )
            .schedule(WAREHOUSE_SCHEDULE)
            .build();
    }

    @Nonnull
    PartnerCapacityFilter createDefaultItemCapacityFilter() {
        return createPartnerCapacityFilterBuilder()
            .countingTypes(Set.of(CountingType.ITEM))
            .build();
    }

    @Nonnull
    PartnerCapacityFilter.Builder createPartnerCapacityFilterBuilder() {
        return PartnerCapacityFilter.newBuilder()
            .partnerIds(Collections.singleton(FROM_PARTNER_ID))
            .locationsFrom(Set.of(225L))
            .locationsTo(Set.of(225L))
            .platformClientIds(Set.of(1L))
            .days(Collections.singleton(null))
            .types(Set.of(CapacityType.REGULAR));
    }

    @Nonnull
    PartnerCapacityDto createCapacity(Long value, CountingType countingType) {
        return PartnerCapacityDto.newBuilder()
            .id(1L)
            .partnerId(FROM_PARTNER_ID)
            .locationFrom(225)
            .locationTo(225)
            .deliveryType(DeliveryType.PICKUP)
            .type(CapacityType.REGULAR)
            .countingType(countingType)
            .platformClientId(1L)
            .day(LocalDate.of(2019, 1, 1))
            .value(value)
            .build();
    }

    @Nonnull
    PartnerResponse createPartner(long partnerId, @Nullable PartnerType partnerType) {
        return PartnerResponse.newBuilder()
            .id(partnerId)
            .marketId(1L)
            .partnerType(Optional.ofNullable(partnerType).orElse(null))
            .intakeSchedule(List.of(
                createScheduleDay(1L, 1),
                createScheduleDay(2L, 2),
                createScheduleDay(3L, 3),
                createScheduleDay(4L, 4),
                createScheduleDay(5L, 5)
            ))
            .build();
    }
}
