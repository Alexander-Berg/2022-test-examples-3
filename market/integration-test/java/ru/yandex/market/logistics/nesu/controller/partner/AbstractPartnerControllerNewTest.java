package ru.yandex.market.logistics.nesu.controller.partner;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import one.util.streamex.IntStreamEx;
import org.junit.jupiter.api.AfterEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.capacity.PartnerCapacityFilter;
import ru.yandex.market.logistics.management.entity.request.partner.PartnerExternalParamRequest;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationCreateDto;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationUpdateDto;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.CutoffResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCapacityDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse.PartnerResponseBuilder;
import ru.yandex.market.logistics.management.entity.response.partner.PlatformClientDto;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.ProductRatingDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.CapacityType;
import ru.yandex.market.logistics.management.entity.type.CountingType;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.nesu.dto.ScheduleDayDto;
import ru.yandex.market.logistics.nesu.dto.partner.CpaPartnerInterfaceRelationRequest;
import ru.yandex.market.logistics.nesu.dto.partner.CpaPartnerInterfaceRelationRequest.CpaPartnerInterfaceRelationRequestBuilder;
import ru.yandex.market.logistics.nesu.enums.ShopShipmentType;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ErrorType;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DatabaseSetup({
    "/repository/partner-relation/before/prepare.xml",
    "/repository/validation/default_validation_settings.xml",
    "/repository/partner-relation/before/shop_partner_settings.xml",
})
abstract class AbstractPartnerControllerNewTest extends AbstractContextualTest {
    protected static final long DROPSHIP_PARTNER_ID = 1L;
    protected static final long SUPPLIER_PARTNER_ID = 2L;
    protected static final long SHOP_ID = 1L;
    protected static final long BUSINESS_ID = 41L;
    protected static final long DELIVERY_WAREHOUSE_ID = 100;
    protected static final long DELIVERY_PARTNER_ID = 200;
    protected static final long ANOTHER_DELIVERY_PARTNER_ID = 201;
    protected static final long ANOTHER_DELIVERY_WAREHOUSE_ID = 101;
    protected static final long CAPACITY_ID = 6000L;

    protected static final Duration ONE_DAY = Duration.ofDays(1L);
    protected static final Duration HALF_HOUR = Duration.ofMinutes(30L);

    protected static final Set<ScheduleDayResponse> DROPSHIP_REGISTER_SCHEDULE = IntStreamEx
        .rangeClosed(1, 7)
        .mapToEntry(i -> i, i -> IntStream.rangeClosed(0, 23))
        .flatMapValues(IntStream::boxed)
        .mapValues(hour -> LocalTime.of(hour, 0))
        .mapKeyValue((day, time) -> new ScheduleDayResponse(null, day, time, time))
        .toSet();

    protected static final Set<ScheduleDayResponse> DEFAULT_SCHEDULE = LmsFactory.createScheduleDayDtoSetWithSize(5);

    protected static final Set<ProductRatingDto> DEFAULT_PRODUCT_RATING = Set.of(
        ProductRatingDto.newBuilder().locationId(225).rating(0).build()
    );

    protected static final ErrorType ERROR_TYPE_VALID_SHIPMENT_SCHEDULE = new ErrorType(
        "Shipment schedule days must be contained within warehouse schedule",
        "ValidWarehouseShipmentSchedule"
    );

    protected static final ErrorType ERROR_TYPE_SCHEDULE_DAYS_COUNT = new ErrorType(
        "Schedule days count must be greater than or equal to 5",
        "ValidScheduleDaysCount",
        Map.of("value", 5)
    );

    @Autowired
    protected LMSClient lmsClient;

    @Autowired
    protected MbiApiClient mbiApiClient;

    @Autowired
    protected FeatureProperties featureProperties;

    @Captor
    private ArgumentCaptor<List<PartnerExternalParamRequest>> externalParamsRequestCaptor;

    @Captor
    private ArgumentCaptor<PartnerCapacityDto> partnerCapacityArgumentCaptor;

    @AfterEach
    void verifyMocks() {
        verifyNoMoreInteractions(lmsClient, mbiApiClient);
    }

    @Nonnull
    protected abstract String getUrl(long partnerId);

    private static long relationId(Long fromPartnerId, Long toPartnerId) {
        return fromPartnerId * toPartnerId;
    }

    protected void mockGetDeliveryWarehouse(boolean active, long warehouseId) {
        mockGetDeliveryWarehouse(active, warehouseId, DELIVERY_PARTNER_ID);
    }

    protected void mockGetDeliveryWarehouse(boolean active, long warehouseId, long deliveryPartnerId) {
        doReturn(Optional.of(
            LmsFactory.createLogisticsPointResponseBuilder(
                warehouseId,
                deliveryPartnerId,
                "Delivery warehouse",
                PointType.WAREHOUSE
            )
                .active(active)
                .schedule(scheduleDays().collect(Collectors.toSet()))
                .build()
        ))
            .when(lmsClient)
            .getLogisticsPoint(warehouseId);
    }

    protected void verifyGetLogisticPoint(long pointId) {
        verify(lmsClient).getLogisticsPoint(pointId);
    }

    protected void mockGetSingleWarehouse(long partnerId, int locationId) {
        long warehouseId = partnerId * 10;
        LogisticsPointResponse warehouse = LmsFactory.createLogisticsPointResponseBuilder(
            warehouseId,
            partnerId,
            "Warehouse " + warehouseId,
            PointType.WAREHOUSE
        )
            .address(LmsFactory.createAddressDto(locationId))
            .schedule(LmsFactory.createScheduleDayDtoSetWithSize(5))
            .build();
        doReturn(List.of(warehouse))
            .when(lmsClient)
            .getLogisticsPoints(partnerWarehouseFilter(partnerId));
    }

    protected void verifyGetSingleWarehouse(long partnerId) {
        verify(lmsClient).getLogisticsPoints(partnerWarehouseFilter(partnerId));
    }

    protected void mockGetPartner(long partnerId, PartnerType partnerType) {
        mockGetPartner(partnerId, partnerType, UnaryOperator.identity());
    }

    protected void mockGetPartner(
        long partnerId,
        PartnerType partnerType,
        UnaryOperator<PartnerResponseBuilder> partnerModifier
    ) {
        doReturn(Optional.of(
            partnerModifier.apply(
                LmsFactory.createPartnerResponseBuilder(partnerId, partnerType, 10000)
                    .platformClients(List.of(
                        PlatformClientDto.newBuilder()
                            .id(1L)
                            .name("Покупки.Маркет")
                            .status(PartnerStatus.ACTIVE)
                            .build()
                    ))
                    .intakeSchedule(scheduleDays().collect(Collectors.toList()))
            ).build()
        ))
            .when(lmsClient).getPartner(partnerId);
    }

    protected void mockNotFoundPartner(long partnerId) {
        doReturn(Optional.empty())
            .when(lmsClient).getPartner(partnerId);
    }

    protected void verifyGetPartner(long partnerId, int times) {
        verify(lmsClient, times(times)).getPartner(partnerId);
    }

    protected void verifyGetPartner(long partnerId) {
        verifyGetPartner(partnerId, 1);
    }

    protected void mockNoRelation(long shopPartnerId) {
        mockGetRelation(shopPartnerId, List.of());
    }

    protected void mockGetRelation(long fromPartnerId, PartnerRelationEntityDto relation) {
        mockGetRelation(fromPartnerId, List.of(relation));
    }

    protected void mockGetRelation(long fromPartnerId, List<PartnerRelationEntityDto> relations) {
        doReturn(relations)
            .when(lmsClient)
            .searchPartnerRelation(partnerRelationFilter(fromPartnerId));
    }

    protected void verifyGetRelation(long fromPartnerId, int times) {
        verify(lmsClient, times(times)).searchPartnerRelation(partnerRelationFilter(fromPartnerId));
    }

    protected void verifyGetRelation() {
        verifyGetRelation(DROPSHIP_PARTNER_ID, 1);
    }

    protected void mockGetCapacity(long shopPartnerId) {
        doReturn(List.of(capacityResponse(shopPartnerId)))
            .when(lmsClient)
            .searchCapacity(capacityFilter(shopPartnerId));
    }

    protected void mockNoCapacity(long shopPartnerId) {
        doReturn(List.of())
            .when(lmsClient)
            .searchCapacity(capacityFilter(shopPartnerId));
    }

    protected void verifyGetCapacity(long shopPartnerId) {
        verify(lmsClient).searchCapacity(capacityFilter(shopPartnerId));
    }

    protected void mockCreateCapacity() {
        when(lmsClient.createCapacity(any(PartnerCapacityDto.class)))
            .thenAnswer(i -> {
                PartnerCapacityDto request = i.getArgument(0);
                return PartnerCapacityDto.newBuilder()
                    .id(CAPACITY_ID)
                    .countingType(request.getCountingType())
                    .locationFrom(request.getLocationFrom())
                    .locationTo(request.getLocationTo())
                    .partnerId(request.getPartnerId())
                    .platformClientId(request.getPlatformClientId())
                    .type(request.getType())
                    .value(request.getValue())
                    .day(request.getDay())
                    .deliveryType(request.getDeliveryType())
                    .capacityService(request.getCapacityService())
                    .build();
            });
    }

    protected void verifyCreateCapacity(long partnerId, long value) {
        verify(lmsClient).createCapacity(partnerCapacityArgumentCaptor.capture());

        softly.assertThat(partnerCapacityArgumentCaptor.getValue())
            .usingRecursiveComparison()
            .isEqualTo(capacityRequest(partnerId, value));
    }

    protected void mockUpdateCapacity() {
        when(lmsClient.updateCapacityValue(anyLong(), anyLong()))
            .thenAnswer(i -> {
                long partnerId = i.getArgument(0);
                long newValue = i.getArgument(1);
                return capacityDtoBuilder(partnerId)
                    .id(CAPACITY_ID)
                    .value(newValue)
                    .build();
            });
    }

    protected void verifyUpdateCapacity(long value) {
        verify(lmsClient).updateCapacityValue(CAPACITY_ID, value);
    }

    protected void mockCreateRelation() {
        when(lmsClient.createPartnerRelation(any(PartnerRelationCreateDto.class)))
            .thenAnswer(i -> {
                PartnerRelationCreateDto request = i.getArgument(0);
                Long fromPartnerId = request.getFromPartnerId();
                Long toPartnerId = request.getToPartnerId();

                boolean isSupplier = fromPartnerId == SUPPLIER_PARTNER_ID;

                return PartnerRelationEntityDto.newBuilder()
                    .id(relationId(fromPartnerId, toPartnerId))
                    .fromPartnerId(fromPartnerId)
                    .toPartnerId(toPartnerId)
                    .toPartnerLogisticsPointId(request.getToPartnerLogisticsPointId())
                    .returnPartnerId(
                        Optional.ofNullable(request.getReturnPartnerId())
                            .orElse(isSupplier ? toPartnerId : fromPartnerId)
                    )
                    .enabled(request.getEnabled())
                    .handlingTime(request.getHandlingTime())
                    .shipmentType(request.getShipmentType())
                    .cutoffs(request.getCutoffs())
                    .intakeSchedule(request.getIntakeSchedule())
                    .registerSchedule(request.getRegisterSchedule())
                    .importSchedule(request.getImportSchedule())
                    .transferTime(request.getTransferTime())
                    .inboundTime(request.getInboundTime())
                    .build();
            });
    }

    protected void verifyCreateRelation(PartnerRelationCreateDto partnerRelationCreateDto) {
        verify(lmsClient).createPartnerRelation(partnerRelationCreateDto);
    }

    protected void mockUpdateRelation() {
        when(lmsClient.updatePartnerRelation(anyLong(), any(PartnerRelationUpdateDto.class)))
            .thenAnswer(i -> {
                Long relationId = i.getArgument(0);
                PartnerRelationUpdateDto request = i.getArgument(1);
                return PartnerRelationEntityDto.newBuilder()
                    .id(relationId)
                    .fromPartnerId(request.getFromPartnerId())
                    .toPartnerId(request.getToPartnerId())
                    .toPartnerLogisticsPointId(request.getToPartnerLogisticsPointId())
                    .returnPartnerId(request.getReturnPartnerId())
                    .enabled(request.getEnabled())
                    .handlingTime(request.getHandlingTime())
                    .shipmentType(request.getShipmentType())
                    .cutoffs(request.getCutoffs())
                    .intakeSchedule(request.getIntakeSchedule())
                    .registerSchedule(request.getRegisterSchedule())
                    .importSchedule(request.getImportSchedule())
                    .build();
            });
    }

    protected void verifyUpdateRelation(long shopPartnerId, long toPartner, PartnerRelationUpdateDto updateDto) {
        verify(lmsClient).updatePartnerRelation(relationId(shopPartnerId, toPartner), updateDto);
    }

    protected void verifyUpdateRelation(PartnerRelationUpdateDto updateDto) {
        verifyUpdateRelation(DROPSHIP_PARTNER_ID, DELIVERY_PARTNER_ID, updateDto);
    }

    protected void verifyUpdateHandlingTime(long shopPartnerId, Duration duration) {
        verify(lmsClient).updateWarehouseHandlingDuration(shopPartnerId, duration);
    }

    protected void verifyUpdatePartnerExternalParams(Map<PartnerExternalParamType, String> params) {
        verify(lmsClient).addOrUpdatePartnerExternalParams(
            eq(DROPSHIP_PARTNER_ID),
            externalParamsRequestCaptor.capture()
        );

        softly.assertThat(externalParamsRequestCaptor.getValue())
            .containsExactlyInAnyOrderElementsOf(createParamsRequest(params));
    }

    @Nonnull
    private static List<PartnerExternalParamRequest> createParamsRequest(Map<PartnerExternalParamType, String> params) {
        return params.entrySet().stream()
            .map(entry -> new PartnerExternalParamRequest(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    @Nonnull
    protected static CpaPartnerInterfaceRelationRequestBuilder defaultRequest() {
        return defaultRequest(ShopShipmentType.WITHDRAW);
    }

    @Nonnull
    protected static CpaPartnerInterfaceRelationRequestBuilder defaultRequest(ShopShipmentType shipmentType) {
        var builder = CpaPartnerInterfaceRelationRequest.builder().shipmentType(shipmentType);

        switch (shipmentType) {
            case IMPORT:
                builder.toPartnerLogisticsPointId(100L);
            case WITHDRAW:
                return builder.capacityValue(100)
                    .cutoffTime(LocalTime.of(17, 0))
                    .handlingTime(1)
                    .shipmentScheduleDayIds(
                        shipmentType == ShopShipmentType.WITHDRAW
                            ? Set.of(21L, 22L, 23L, 24L, 25L)
                            : Set.of(31L, 32L, 33L, 34L, 35L)
                    );
            case WITHDRAW_EXPRESS:
                return builder.useElectronicReceptionTransferAct(false)
                    .partnerSchedules(List.of(
                        scheduleDay(1),
                        scheduleDay(2),
                        scheduleDay(3),
                        scheduleDay(4),
                        scheduleDay(5)
                    ));
            default:
                throw new RuntimeException(String.format("No shipment type passed: %s", shipmentType));
        }
    }

    @Nonnull
    protected static LogisticsPointFilter partnerWarehouseFilter(long partnerId) {
        return LmsFactory.createWarehousesFilter(Set.of(partnerId));
    }

    @Nonnull
    protected static Stream<ScheduleDayResponse> scheduleDays() {
        return IntStream.rangeClosed(1, 7)
            .mapToObj(day -> LmsFactory.createScheduleDayDto(300L + day, day));
    }

    @Nonnull
    private static PartnerRelationFilter partnerRelationFilter(long fromPartnerId) {
        return PartnerRelationFilter.newBuilder().fromPartnerId(fromPartnerId).build();
    }

    @Nonnull
    protected static PartnerRelationEntityDto.Builder defaultRelation(long fromPartnerId, long toPartnerId) {
        return defaultRelation(fromPartnerId, toPartnerId, ShipmentType.WITHDRAW);
    }

    @Nonnull
    protected static PartnerRelationEntityDto.Builder defaultRelation(
        long fromPartnerId,
        long toPartnerId,
        ShipmentType shipmentType
    ) {
        boolean isSupplier = fromPartnerId == SUPPLIER_PARTNER_ID;

        var builder = PartnerRelationEntityDto.newBuilder()
            .id(relationId(fromPartnerId, toPartnerId))
            .fromPartnerId(fromPartnerId)
            .toPartnerId(toPartnerId)
            .returnPartnerId(isSupplier ? toPartnerId : fromPartnerId)
            .toPartnerLogisticsPointId(DELIVERY_WAREHOUSE_ID)
            .shipmentType(shipmentType)
            .enabled(true)
            .cutoffs(cutoffOf(17, isSupplier ? 25 : null))
            .productRatings(DEFAULT_PRODUCT_RATING)
            .handlingTime(0);

        if (shipmentType == ShipmentType.IMPORT) {
            builder.importSchedule(DEFAULT_SCHEDULE);
        } else {
            builder.intakeSchedule(DEFAULT_SCHEDULE);
        }

        if (!isSupplier) {
            builder.registerSchedule(DROPSHIP_REGISTER_SCHEDULE);
        }

        return builder;
    }

    @Nonnull
    protected static PartnerRelationCreateDto.Builder defaultCreateRelationDto(long shopPartnerId) {
        boolean isSupplier = shopPartnerId == SUPPLIER_PARTNER_ID;
        return PartnerRelationCreateDto.newBuilder()
            .fromPartnerId(shopPartnerId)
            .toPartnerId(DELIVERY_PARTNER_ID)
            .toPartnerLogisticsPointId(DELIVERY_WAREHOUSE_ID)
            .shipmentType(ShipmentType.WITHDRAW)
            .enabled(true)
            .handlingTime(0)
            .productRatings(DEFAULT_PRODUCT_RATING)
            .cutoffs(cutoffOf(17, isSupplier ? 25 : null))
            .inboundTime(isSupplier ? Duration.ofHours(12L) : null)
            .intakeSchedule(DEFAULT_SCHEDULE);
    }

    @Nonnull
    protected static PartnerRelationUpdateDto.Builder defaultUpdateRelationDto(long shopPartnerId, boolean enabled) {
        boolean isSupplier = shopPartnerId == SUPPLIER_PARTNER_ID;
        return PartnerRelationUpdateDto.newBuilder()
            .fromPartnerId(shopPartnerId)
            .toPartnerId(DELIVERY_PARTNER_ID)
            .toPartnerLogisticsPointId(DELIVERY_WAREHOUSE_ID)
            .returnPartnerId(isSupplier ? DELIVERY_PARTNER_ID : shopPartnerId)
            .shipmentType(ShipmentType.WITHDRAW)
            .enabled(enabled)
            .handlingTime(0)
            .productRatings(DEFAULT_PRODUCT_RATING)
            .cutoffs(cutoffOf(17, isSupplier ? 25 : null))
            .intakeSchedule(DEFAULT_SCHEDULE);
    }

    @Nonnull
    protected static Set<CutoffResponse> cutoffOf(int hours, @Nullable Integer packagingDurationHours) {
        return Set.of(
            CutoffResponse.newBuilder()
                .locationId(225)
                .cutoffTime(LocalTime.of(hours, 0))
                .packagingDuration(
                    Optional.ofNullable(packagingDurationHours)
                        .map(Duration::ofHours)
                        .orElse(null)
                )
                .build()
        );
    }

    @Nonnull
    protected static ScheduleDayDto scheduleDay(int dayNum, LocalTime from, LocalTime to) {
        return new ScheduleDayDto()
            .setDay(dayNum)
            .setTimeFrom(from)
            .setTimeTo(to);
    }

    @Nonnull
    protected static ScheduleDayDto scheduleDay(int day) {
        return scheduleDay(day, LocalTime.of(10, 0), LocalTime.of(18, 0));
    }

    @Nonnull
    protected static PartnerCapacityFilter capacityFilter(long partnerId) {
        var builder = PartnerCapacityFilter.newBuilder()
            .partnerIds(Set.of(partnerId))
            .countingTypes(null)
            .locationsFrom(Set.of(225L))
            .locationsTo(Set.of(225L))
            .platformClientIds(Set.of(1L))
            .types(Set.of(CapacityType.REGULAR))
            .days(Collections.singleton(null));

        if (partnerId == SUPPLIER_PARTNER_ID) {
            builder.countingTypes(Set.of(CountingType.ITEM));
        }

        return builder.build();
    }

    @Nonnull
    protected static PartnerCapacityDto.Builder capacityDtoBuilder(long shopPartnerId) {
        return PartnerCapacityDto.newBuilder()
            .partnerId(shopPartnerId)
            .locationFrom(225)
            .locationTo(225)
            .platformClientId(1L)
            .type(CapacityType.REGULAR)
            .value(100L)
            .countingType(shopPartnerId == SUPPLIER_PARTNER_ID ? CountingType.ITEM : CountingType.ORDER);
    }

    @Nonnull
    protected static PartnerCapacityDto capacityRequest(long shopPartnerId, long value) {
        return capacityDtoBuilder(shopPartnerId).value(value).build();
    }

    @Nonnull
    protected static PartnerCapacityDto capacityResponse(long shopPartnerId) {
        return capacityDtoBuilder(shopPartnerId).id(CAPACITY_ID).build();
    }

    @Nonnull
    protected ResultActions saveRelation(
        long shopId,
        long partnerId,
        CpaPartnerInterfaceRelationRequestBuilder request
    ) throws Exception {
        return mockMvc.perform(
            request(HttpMethod.PUT, getUrl(partnerId), request.build())
                .param("shopId", Long.toString(shopId))
                .param("userId", "1")
        );
    }

}
