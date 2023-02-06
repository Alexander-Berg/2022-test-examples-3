package ru.yandex.market.logistics.lom;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.logistics.lom.entity.enums.PartnerSubtype;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.response.point.Contact;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PhoneType;
import ru.yandex.market.logistics.management.entity.type.PointType;

@ParametersAreNonnullByDefault
public final class LmsModelFactory {
    private LmsModelFactory() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static Address createLmsAddress(int locationId) {
        return Address.newBuilder()
            .locationId(locationId)
            .settlement("locality")
            .postCode("zipCode")
            .latitude(new BigDecimal("10.1"))
            .longitude(new BigDecimal("100.2"))
            .street("street")
            .house("house")
            .housing("housing")
            .building("building")
            .apartment("room")
            .comment("comment")
            .region("region")
            .addressString("addressString")
            .shortAddressString("shortAddressString")
            .build();
    }

    @Nonnull
    public static Address createReturnLmsAddress(int locationId) {
        return Address.newBuilder()
            .locationId(locationId)
            .settlement("return-locality")
            .postCode("return-zipCode")
            .latitude(new BigDecimal("10.1"))
            .longitude(new BigDecimal("100.2"))
            .street("return-street")
            .house("return-house")
            .housing("return-housing")
            .building("return-building")
            .apartment("return-room")
            .comment("return-comment")
            .region("return-region")
            .addressString("return-addressString")
            .shortAddressString("return-shortAddressString")
            .build();
    }

    @Nonnull
    public static PartnerResponse createPartnerResponse(
        long id,
        PartnerType partnerType,
        @Nullable PartnerSubtype partnerSubtype
    ) {
        return PartnerResponse.newBuilder()
            .id(id)
            .partnerType(partnerType)
            .subtype(
                partnerSubtype != null
                    ? PartnerSubtypeResponse.newBuilder().id(partnerSubtype.getId()).build()
                    : null
            )
            .build();
    }

    @Nonnull
    public static PartnerResponse createPartnerResponse(long id, Long marketId, PartnerType partnerType) {
        return PartnerResponse.newBuilder()
            .id(id)
            .marketId(marketId)
            .partnerType(partnerType)
            .name("Partner " + id)
            .readableName("Partner readable " + id)
            .billingClientId(id * 100)
            .build();
    }

    @Nonnull
    public static LogisticsPointResponse.LogisticsPointResponseBuilder createPickupPointResponseBuilder(long id) {
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
    public static LogisticsPointResponse.LogisticsPointResponseBuilder createLogisticsPointResponse(
        Long pointId,
        @Nullable Long partnerId,
        @Nullable String name,
        PointType type
    ) {
        return LogisticsPointResponse.newBuilder()
            .id(pointId)
            .partnerId(partnerId)
            .externalId("externalId")
            .type(type)
            .name(name)
            .address(createAddressDto(1))
            .phones(Set.of(createPhoneDto()))
            .active(true)
            .schedule(Set.of(createScheduleDayDto()))
            .contact(createContactDto());
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
    private static Address createAddressDto(int id) {
        return Address.newBuilder()
            .locationId(id)
            .settlement("Новосибирск")
            .postCode("649220")
            .street("Николаева")
            .house(String.valueOf(id))
            .housing(String.valueOf(id))
            .building("")
            .apartment("")
            .comment("Комментарий, как проехать")
            .region("Регион")
            .addressString("address")
            .shortAddressString("add")
            .build();
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
}
