package ru.yandex.market.logistics.nesu.model;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import ru.yandex.market.logistics.lom.model.enums.PlatformClient;
import ru.yandex.market.logistics.management.entity.page.PageResult;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.UpdateBusinessWarehouseDto;
import ru.yandex.market.logistics.management.entity.request.partner.CreatePartnerDto;
import ru.yandex.market.logistics.management.entity.request.partner.PartnerExternalParamRequest;
import ru.yandex.market.logistics.management.entity.request.partner.PlatformClientPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseResponse;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentDto;
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentServiceDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCourierDayScheduleResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PlatformClientDto;
import ru.yandex.market.logistics.management.entity.response.point.Contact;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.point.Service;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiDto;
import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiUpdateDto;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodCreateDto;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;
import ru.yandex.market.logistics.management.entity.type.ApiType;
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PhoneType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName;
import ru.yandex.market.logistics.nesu.service.lms.PlatformClientId;

public final class LmsFactory {

    private LmsFactory() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static LogisticsPointResponse createLogisticsPointResponse(
        Long pointId,
        @Nullable Long partnerId,
        String name,
        PointType type
    ) {
        return createLogisticsPointResponse(pointId, pointId, partnerId, name, type);
    }

    @Nonnull
    public static LogisticsPointResponse createLogisticsPointResponse(
        Long pointId,
        @Nullable Long partnerId,
        String name,
        @Nullable Boolean active,
        PointType type
    ) {
        if (active == null) {
            return createLogisticsPointResponse(pointId, partnerId, name, type);
        }
        return createLogisticsPointResponseBuilder(pointId, partnerId, name, type)
            .active(active)
            .build();
    }

    @Nonnull
    public static LogisticsPointResponse createLogisticsPointResponse(
        Long pointId,
        Long businessId,
        @Nullable Long partnerId,
        String name,
        PointType type
    ) {
        return createLogisticsPointResponseBuilder(pointId, partnerId, name, type)
            .businessId(businessId)
            .build();
    }

    @Nonnull
    public static LogisticsPointResponse.LogisticsPointResponseBuilder createLogisticsPointResponseBuilder(
        Long pointId,
        @Nullable Long partnerId,
        String name,
        PointType type
    ) {
        return LogisticsPointResponse.newBuilder()
            .id(pointId)
            .partnerId(partnerId)
            .externalId("externalId")
            .type(type)
            .pickupPointType(null)
            .name(name)
            .address(createAddressDto())
            .phones(Set.of(createPhoneDto()))
            .active(true)
            .schedule(Set.of(createScheduleDayDto(1)))
            .cardAllowed(false)
            .prepayAllowed(false)
            .cashAllowed(true)
            .photos(null)
            .instruction("test_instructions")
            .returnAllowed(false)
            .services(Set.of(new Service(
                ServiceCodeName.CASH_SERVICE,
                true,
                "test_service_name",
                "description"
            )))
            .storagePeriod(3)
            .maxWeight(20.0)
            .maxLength(20)
            .maxWidth(10)
            .maxHeight(15)
            .maxSidesSum(100)
            .isFrozen(false)
            .businessId(41L)
            .contact(createContactDto());
    }

    @Nonnull
    public static Address createAddressDto() {
        return createAddressDto(1);
    }

    @Nonnull
    public static Address createAddressDto(int locationId) {
        return createAddressDto(locationId, null);
    }

    @Nonnull
    public static Address createAddressDto(int locationId, Integer exactLocationId) {
        return createShortAddressDtoBuilder(locationId)
            .subRegion("Новосибирский округ")
            .addressString("address")
            .shortAddressString("short address string")
            .exactLocationId(exactLocationId)
            .build();
    }

    @Nonnull
    public static Address.AddressBuilder createShortAddressDtoBuilder(int locationId) {
        return Address.newBuilder()
            .locationId(locationId)
            .settlement("Новосибирск")
            .postCode("649220")
            .latitude(new BigDecimal(1))
            .longitude(new BigDecimal(2))
            .street("Николаева")
            .house("11")
            .housing("1/2")
            .building("2a")
            .apartment("314")
            .comment("как проехать")
            .region("Новосибирская область");
    }

    @Nonnull
    public static Contact createContactDto() {
        return new Contact(
            "Иван",
            "Иванов",
            "Иванович"
        );
    }

    @Nonnull
    public static Phone createPhoneDto() {
        return new Phone(
            "+7 923 243 5555",
            "777",
            "test_phone",
            PhoneType.PRIMARY
        );
    }

    @Nonnull
    public static ScheduleDayResponse createScheduleDayDto(int day) {
        return new ScheduleDayResponse(
            1L,
            day,
            LocalTime.of(10, 0),
            LocalTime.of(18, 0)
        );
    }

    @Nonnull
    public static ScheduleDayResponse createScheduleDayDto(@Nullable Long id, Integer day) {
        return new ScheduleDayResponse(
            id,
            day,
            LocalTime.of(10, 0),
            LocalTime.of(18, 0)
        );
    }

    @Nonnull
    public static Set<ScheduleDayResponse> createScheduleDayDtoSetWithSize(int daysCount) {
        return IntStream.range(1, daysCount + 1)
            .mapToObj(num -> createScheduleDayDto(null, num))
            .collect(Collectors.toSet());
    }

    @Nonnull
    public static PartnerCourierDayScheduleResponse createCourierSchedule(
        long partnerId,
        int locationId,
        int day
    ) {
        return createCourierSchedule(partnerId, locationId, Set.of(DayOfWeek.of(day)));
    }

    @Nonnull
    public static PartnerCourierDayScheduleResponse createCourierSchedule(
        long partnerId,
        int locationId,
        Set<DayOfWeek> daysOfWeek
    ) {
        return PartnerCourierDayScheduleResponse.builder()
            .locationId(locationId)
            .partnerId(partnerId)
            .schedule(
                daysOfWeek.stream()
                    .map(dayOfWeek -> createScheduleDayDto(dayOfWeek.getValue()))
                    .collect(Collectors.toList())
            )
            .build();
    }

    @Nonnull
    public static PartnerResponse createPartner(long partnerId, PartnerType partnerType) {
        return createPartner(partnerId, 100L, partnerType);
    }

    @Nonnull
    public static PartnerResponse createPartner(long partnerId, long marketId, PartnerType partnerType) {
        return createPartner(partnerId, partnerType, "Sample Readable Partner", marketId);
    }

    @Nonnull
    public static PartnerResponse createPartner(
        long partnerId,
        long marketId,
        String name,
        String readableName,
        PartnerType partnerType,
        boolean withPlatformClients
    ) {
        return createPartnerResponseBuilder(partnerId, partnerType, marketId)
            .platformClients(withPlatformClients ? List.of(PlatformClientDto.newBuilder().id(1L).build()) : null)
            .name(name)
            .readableName(readableName)
            .build();
    }

    @Nonnull
    public static PartnerResponse createPartner(
        long partnerId,
        long marketId,
        String name,
        String readableName,
        PartnerType partnerType,
        boolean withPlatformClients,
        Long businessId
    ) {
        return createPartnerResponseBuilder(partnerId, partnerType, marketId)
            .platformClients(withPlatformClients ? List.of(PlatformClientDto.newBuilder().id(1L).build()) : null)
            .name(name)
            .readableName(readableName)
            .businessId(businessId)
            .build();
    }

    @Nonnull
    public static PartnerResponse createPartner(
        long partnerId,
        PartnerType partnerType,
        String readableName,
        long marketId
    ) {
        return createPartnerResponseBuilder(partnerId, partnerType, marketId)
            .readableName(readableName)
            .intakeSchedule(List.of(
                createScheduleDayDto(1),
                createScheduleDayDto(2),
                createScheduleDayDto(3),
                createScheduleDayDto(4),
                createScheduleDayDto(5)
            ))
            .build();
    }

    @Nonnull
    public static PartnerResponse createPartner(
        long partnerId,
        PartnerType partnerType,
        PartnerStatus partnerStatus
    ) {
        return createPartner(partnerId, partnerType, partnerStatus, null);
    }

    @Nonnull
    public static PartnerResponse createPartner(
        long partnerId,
        PartnerType partnerType,
        PartnerStatus partnerStatus,
        @Nullable List<PartnerExternalParam> params
    ) {
        return createPartnerResponseBuilder(partnerId, partnerType, 1L)
            .readableName("Sample Readable Partner")
            .status(partnerStatus)
            .params(params)
            .build();
    }

    @Nonnull
    public static PartnerResponse.PartnerResponseBuilder createPartnerResponseBuilder(
        long partnerId,
        PartnerType partnerType,
        long marketId
    ) {
        return PartnerResponse.newBuilder()
            .id(partnerId)
            .marketId(marketId)
            .partnerType(partnerType)
            .name("Sample Partner")
            .status(PartnerStatus.ACTIVE)
            .billingClientId(1L)
            .trackingType("Sample Tracking Type")
            .locationId(1)
            .rating(5)
            .logoUrl("http://test_logo_url/" + partnerId)
            .businessId(41L)
            .platformClients(List.of(PlatformClientDto.newBuilder().id(3L).status(PartnerStatus.ACTIVE).build()));
    }

    @Nonnull
    public static CreatePartnerDto createDropshipPartnerDto(
        long marketId,
        String name,
        String readableName,
        Long businessId
    ) {
        return CreatePartnerDto.newBuilder()
            .name(name)
            .readableName(readableName)
            .partnerType(PartnerType.DROPSHIP)
            .marketId(marketId)
            .businessId(businessId)
            .build();
    }

    @Nonnull
    public static SettingsApiUpdateDto createSettingsApiUpdateDto() {
        return SettingsApiUpdateDto.newBuilder()
            .apiType(ApiType.FULFILLMENT)
            .format("XML")
            .version("3.*")
            .build();
    }

    @Nonnull
    public static SettingsApiDto createSettingsApiDto(long partnerId) {
        return SettingsApiDto.newBuilder()
            .id(184L)
            .format("XML")
            .partnerId(partnerId)
            .version("3.*")
            .token("token")
            .build();
    }

    @Nonnull
    public static List<SettingsMethodCreateDto> createSettingsMethodsCreateDtos(List<Pair<String, String>> methods) {
        return createSettingsMethodsCreateDtos(methods, false);
    }

    @Nonnull
    public static List<SettingsMethodCreateDto> createSettingsMethodsCreateDtos(
        List<Pair<String, String>> methods,
        boolean useL4S
    ) {
        return methods.stream()
            .map(method -> {
                    String apiUrl = useL4S ? "https://l4s.net/external/" : "https://ff4shops.net/";
                    return SettingsMethodCreateDto.newBuilder()
                        .apiType(ApiType.FULFILLMENT)
                        .method(method.getKey())
                        .url(apiUrl + method.getValue())
                        .active(true)
                        .build();
                }
            )
            .collect(Collectors.toList());
    }

    @Nonnull
    public static List<SettingsMethodDto> createSettingsMethodDtos(List<String> methods) {
        return methods.stream()
            .map(method -> SettingsMethodDto.newBuilder()
                .url("https://ff4shops.net/" + method)
                .method(method)
                .active(true)
                .build()
            )
            .collect(Collectors.toList());
    }

    @Nonnull
    public static SearchPartnerFilter createPartnerFilter(@Nullable PartnerType partnerType) {
        return createPartnerFilter(null, partnerType, Set.of(PartnerStatus.ACTIVE));
    }

    @Nonnull
    public static SearchPartnerFilter createPartnerFilter(
        @Nullable PartnerType partnerType,
        Set<PartnerStatus> platformClientStatuses
    ) {
        return createPartnerFilter(null, partnerType, platformClientStatuses);
    }

    @Nonnull
    public static SearchPartnerFilter createPartnerFilter(@Nullable Set<Long> ids, @Nullable PartnerType partnerType) {
        return createPartnerFilter(ids, partnerType, Set.of(PartnerStatus.ACTIVE));
    }

    @Nonnull
    public static SearchPartnerFilter createPartnerFilter(
        @Nullable Set<Long> ids,
        @Nullable PartnerType partnerType,
        Set<PartnerStatus> platformClientStatuses
    ) {
        return createPartnerFilter(
            ids,
            platformClientStatuses,
            Optional.ofNullable(partnerType).map(Set::of).orElse(null)
        );
    }

    @Nonnull
    public static SearchPartnerFilter createPartnerFilter(
        @Nullable Set<Long> ids,
        @Nullable Set<PartnerStatus> platformClientStatuses,
        Set<PartnerType> types
    ) {
        return SearchPartnerFilter.builder()
            .setIds(ids)
            .setPlatformClientIds(Set.of(PlatformClientId.YANDEX_DELIVERY.getId()))
            .setStatuses(Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING))
            .setPlatformClientStatuses(platformClientStatuses)
            .setTypes(types)
            .build();
    }

    @Nonnull
    public static SearchPartnerFilter createPartnerFilter(
        PartnerType partnerType,
        PartnerExternalParamType partnerExternalParamType
    ) {
        return SearchPartnerFilter.builder()
            .setTypes(Set.of(partnerType))
            .setExternalParamsIntersection(Set.of(
                new PartnerExternalParamRequest(partnerExternalParamType, "1")
            ))
            .setStatuses(Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING))
            .build();
    }

    @Nonnull
    public static LogisticsPointFilter createLogisticsPointsFilter(Set<Long> ids, Boolean active) {
        return createLogisticsPointsFilter(ids, null, null, active);
    }

    @Nonnull
    public static LogisticsPointFilter createWarehousesFilter(Set<Long> partnerIds) {
        return createLogisticsPointsFilter(null, partnerIds, PointType.WAREHOUSE, true);
    }

    @Nonnull
    public static LogisticsPointFilter createLogisticsPointsFilter(
        @Nullable Set<Long> ids,
        @Nullable Set<Long> partnerIds,
        @Nullable PointType pointType,
        @Nullable Boolean active
    ) {
        return LogisticsPointFilter.newBuilder()
            .ids(ids)
            .partnerIds(partnerIds)
            .type(pointType)
            .active(active)
            .build();
    }

    @Nonnull
    public static PlatformClientPartnerFilter defaultPartnerPlatformSettingsFilter(Set<Long> partnerIds) {
        return PlatformClientPartnerFilter.newBuilder()
            .partnerIds(partnerIds)
            .platformClientIds(Set.of(PlatformClient.YANDEX_DELIVERY.getId()))
            .build();
    }

    @Nonnull
    public static PartnerExternalParam enabledPartnerExternalParam(PartnerExternalParamType type) {
        return new PartnerExternalParam(type.name(), "", "1");
    }

    @Nonnull
    public static Address.AddressBuilder addressWithoutCalculatedFields() {
        return createShortAddressDtoBuilder(2)
            .housing("")
            .building("")
            .apartment("")
            .latitude(null)
            .longitude(null)
            .subRegion(null);
    }

    @Nonnull
    public static BusinessWarehouseResponse.Builder businessWarehouseBuilder(Long partnerId) {
        return BusinessWarehouseResponse.newBuilder()
            .businessId(42L)
            .marketId(100L)
            .logisticsPointId(partnerId)
            .logisticsPointName("Warehouse name")
            .partnerId(partnerId)
            .externalId("external-id-" + partnerId)
            .name("Warehouse name")
            .readableName("Warehouse name")
            .schedule(createScheduleDayDtoSetWithSize(5))
            .phones(Set.of(createPhoneDto()))
            .contact(createContactDto())
            .address(createAddressDto());
    }

    @Nonnull
    public static UpdateBusinessWarehouseDto.Builder updateBusinessWarehouseDtoBuilder() {
        return UpdateBusinessWarehouseDto.newBuilder()
            .name("new name")
            .readableName("new name")
            .address(createShortAddressDtoBuilder(2).build())
            .phones(Set.of(new Phone(
                "+7 923 243 5555",
                "777",
                null,
                PhoneType.PRIMARY
            )))
            .contact(createContactDto())
            .schedule(createScheduleDayDtoSetWithSize(5));
    }

    @Nonnull
    public static PageResult<BusinessWarehouseResponse> businessWarehousesPageResult(
        List<BusinessWarehouseResponse> result
    ) {
        return businessWarehousesPageResult(result, result.size());
    }

    @Nonnull
    public static PageResult<BusinessWarehouseResponse> businessWarehousesPageResult(
        List<BusinessWarehouseResponse> result,
        int pageSize
    ) {
        return new PageResult<BusinessWarehouseResponse>()
            .setData(result)
            .setPage(0)
            .setSize(pageSize)
            .setTotalElements(result.size())
            .setTotalPages(1);
    }

    @Nonnull
    public static LogisticSegmentDto logisticSegmentDto(
        Long id,
        Long partnerId,
        LogisticSegmentType type
    ) {
        return new LogisticSegmentDto()
            .setId(id)
            .setPartnerId(partnerId)
            .setType(type);
    }

    @Nonnull
    public static LogisticSegmentDto logisticSegmentDto(
        Long id,
        Long partnerId,
        LogisticSegmentType type,
        List<LogisticSegmentServiceDto> services
    ) {
        return new LogisticSegmentDto()
            .setId(id)
            .setPartnerId(partnerId)
            .setType(type)
            .setServices(services);
    }
}
