package ru.yandex.market.logistics.lom.utils;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ru.yandex.market.logistics.lom.entity.enums.PartnerSubtype;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.legalInfo.LegalInfoResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.point.Contact;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse.LogisticsPointResponseBuilder;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PhoneType;
import ru.yandex.market.logistics.management.entity.type.PointType;

public final class LmsFactory {

    private LmsFactory() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static LogisticsPointResponseBuilder createLogisticsPointResponse(
        Long pointId,
        Long partnerId,
        String name,
        PointType type
    ) {
        return LogisticsPointResponse.newBuilder()
            .id(pointId)
            .partnerId(partnerId)
            .externalId("externalId")
            .type(type)
            .name(name)
            .address(createAddressDto())
            .phones(Set.of(createPhoneDto()))
            .active(true)
            .schedule(Set.of(createScheduleDayDto()))
            .contact(createContactDto());
    }

    @Nonnull
    private static Address createAddressDto() {
        return Address.newBuilder()
            .locationId(1)
            .settlement("Новосибирск")
            .postCode("649220")
            .street("Николаева")
            .house(String.valueOf(11))
            .housing(String.valueOf(11))
            .building("")
            .apartment("")
            .comment("")
            .region("Регион")
            .addressString("address")
            .shortAddressString("add")
            .subRegion("Округ")
            .build();
    }

    @Nonnull
    public static Address createAddressDto(int id) {
        return Address.newBuilder()
            .locationId(id)
            .settlement("Новосибирск")
            .postCode("649220")
            .street("Николаева")
            .house(String.valueOf(id))
            .housing(String.valueOf(id))
            .building("")
            .apartment("")
            .comment("")
            .region("Регион")
            .addressString("address")
            .shortAddressString("add")
            .subRegion("Округ")
            .build();
    }

    @Nonnull
    private static Contact createContactDto() {
        return new Contact(
            "Иван",
            "Иванов",
            "Иванович"
        );
    }

    @Nonnull
    private static Phone createPhoneDto() {
        return new Phone(
            "+7 923 243 5555",
            "777",
            null,
            PhoneType.PRIMARY
        );
    }

    @Nonnull
    private static ScheduleDayResponse createScheduleDayDto() {
        return new ScheduleDayResponse(
            1L,
            1,
            LocalTime.of(10, 0),
            LocalTime.of(18, 0)
        );
    }

    @Nonnull
    public static PartnerResponse createPartnerResponse(long id) {
        return createPartnerResponse(id, (Long) null);
    }

    @Nonnull
    public static PartnerResponse createPartnerResponse(long id, @Nullable Long marketId) {
        return createPartnerResponse(id, marketId, PartnerType.DELIVERY);
    }

    @Nonnull
    public static PartnerResponse createPartnerResponse(
        long id,
        @Nullable Long marketId,
        PartnerType type,
        @Nullable PartnerSubtype partnerSubtype
    ) {
        return PartnerResponse.newBuilder()
            .id(id)
            .marketId(marketId)
            .partnerType(type)
            .subtype(
                Optional.ofNullable(partnerSubtype)
                    .map(subtype -> PartnerSubtypeResponse.newBuilder().id(subtype.getId()).build())
                    .orElse(null)
            )
            .name("Partner " + id)
            .readableName("Partner readable " + id)
            .billingClientId(id * 100)
            .params(List.of(new PartnerExternalParam("RECIPIENT_UID_ENABLED", null, "1")))
            .build();
    }

    @Nonnull
    private static PartnerResponse createPartnerResponse(long id, @Nullable Long marketId, PartnerType type) {
        return createPartnerResponse(id, marketId, type, PartnerSubtype.PARTNER_SORTING_CENTER);
    }

    @Nonnull
    public static PartnerResponse createDropoffPartnerResponse(
        long id,
        @Nullable Long marketId,
        PartnerType type
    ) {
        return createPartnerResponse(
            id,
            marketId,
            type,
            new PartnerExternalParam("IS_DROPOFF", null, "1")
        );
    }

    @Nonnull
    public static PartnerResponse createDropshipExpressPartnerResponse(
        long id,
        @Nullable Long marketId,
        PartnerType type
    ) {
        return createPartnerResponse(
            id,
            marketId,
            type,
            new PartnerExternalParam("DROPSHIP_EXPRESS", null, "1")
        );
    }

    @Nonnull
    public static PartnerResponse createUpdateInstancesEnabledPartnerResponse(
        long id,
        @Nullable Long marketId,
        PartnerType type
    ) {
        return createPartnerResponse(
            id,
            marketId,
            type,
            new PartnerExternalParam(PartnerExternalParamType.CAN_UPDATE_INSTANCES.name(), null, "1")
        );
    }

    @Nonnull
    public static PartnerResponse createChangeShipmentDateEnabledPartnerResponse(
        long id,
        @Nullable Long marketId,
        PartnerType type
    ) {
        return createPartnerResponse(
            id,
            marketId,
            type,
            new PartnerExternalParam(PartnerExternalParamType.CAN_UPDATE_SHIPMENT_DATE.name(), null, "1")
        );
    }

    @Nonnull
    public static PartnerResponse createPartnerResponse(
        long id,
        @Nullable Long marketId,
        PartnerType type,
        PartnerExternalParam... params
    ) {
        return PartnerResponse.newBuilder()
            .id(id)
            .marketId(marketId)
            .partnerType(type)
            .name("Partner " + id)
            .readableName("Partner readable " + id)
            .billingClientId(id * 100)
            .params(Arrays.asList(params))
            .build();
    }

    @Nonnull
    public static ScheduleDayResponse createScheduleDayResponse(long id, int hourFrom, int hourTo) {
        return createScheduleDayResponse(id, hourFrom, hourTo, 2);
    }

    @Nonnull
    public static ScheduleDayResponse createScheduleDayResponse(long id, int hourFrom, int hourTo, int day) {
        return new ScheduleDayResponse(id, day, LocalTime.of(hourFrom, 0), LocalTime.of(hourTo, 0));
    }

    @Nonnull
    public static PartnerResponse createPartnerResponse(long id, String readableName) {
        return createPartnerResponse(id, readableName, null);
    }

    @Nonnull
    public static PartnerResponse createPartnerResponse(long id, String readableName, Long marketId) {
        return PartnerResponse.newBuilder()
            .id(id)
            .partnerType(id % 2 == 0 ? PartnerType.DELIVERY : PartnerType.SORTING_CENTER)
            .subtype(
                id % 2 == 0
                    ? PartnerSubtypeResponse.newBuilder().id(PartnerSubtype.MARKET_COURIER.getId()).build()
                    : PartnerSubtypeResponse.newBuilder().id(PartnerSubtype.PARTNER_SORTING_CENTER.getId()).build()
            )
            .name("Partner " + id)
            .readableName(readableName)
            .billingClientId(id * 100)
            .marketId(marketId)
            .build();
    }

    @Nonnull
    public static PartnerResponse.PartnerResponseBuilder createPartnerResponse(long id, PartnerType partnerType) {
        return PartnerResponse.newBuilder()
            .id(id)
            .partnerType(partnerType);
    }

    @Nonnull
    public static PartnerResponse createPartnerResponse(long id, long marketId, PartnerType partnerType) {
        return PartnerResponse.newBuilder()
            .id(id)
            .marketId(marketId)
            .partnerType(partnerType)
            .build();
    }

    @Nonnull
    public static PartnerResponse.PartnerResponseBuilder createPartnerResponseBuilder(
        long id,
        List<PartnerExternalParam> partnerExternalParams
    ) {
        return PartnerResponse.newBuilder()
            .id(id)
            .params(partnerExternalParams);
    }

    @Nonnull
    public static PartnerRelationEntityDto createPartnerRelationEntityDto(
        Long partnerIdFrom,
        Long partnerIdTo,
        Set<ScheduleDayResponse> intakeSchedule,
        Boolean enabled
    ) {
        return PartnerRelationEntityDto.newBuilder()
            .fromPartnerId(partnerIdFrom)
            .toPartnerId(partnerIdTo)
            .intakeSchedule(intakeSchedule)
            .enabled(enabled)
            .build();
    }

    @Nonnull
    public static LogisticsPointResponse createWarehouseResponse(long id, Long partnerId) {
        return LogisticsPointResponse.newBuilder()
            .id(id)
            .partnerId(partnerId)
            .type(PointType.WAREHOUSE)
            .name("WAREHOUSE")
            .build();
    }

    @Nonnull
    public static LogisticsPointResponseBuilder createWarehouseResponseBuilder(long id) {
        return LogisticsPointResponse.newBuilder()
            .id(id)
            .externalId("externalId-" + id)
            .phones(Set.of(createPhoneDto()))
            .schedule(Set.of(createScheduleDayDto()))
            .active(true)
            .isFrozen(false)
            .address(createAddressDto((int) id))
            .type(PointType.WAREHOUSE)
            .name("WAREHOUSE")
            .contact(createContactDto());
    }

    public static LogisticsPointResponseBuilder createPickupPointResponseBuilder(long id) {
        return LogisticsPointResponse.newBuilder()
            .id(id)
            .externalId("externalId-" + id)
            .phones(Set.of(createPhoneDto()))
            .schedule(Set.of(createScheduleDayDto()))
            .active(true)
            .isFrozen(false)
            .address(createAddressDto((int) id))
            .type(PointType.PICKUP_POINT)
            .name("PICKPOINT");
    }

    @Nonnull
    public static LegalInfoResponse createLegalInfo(long id) {
        return new LegalInfoResponse(
            1L + id,
            id,
            "incorporation",
            11111L,
            "http://url.to",
            "legalForm",
            "22222",
            "+79001234567",
            createAddressDto(1),
            createAddressDto(2),
            "valid@email.com",
            "112233",
            "332211",
            "account"
        );
    }
}
