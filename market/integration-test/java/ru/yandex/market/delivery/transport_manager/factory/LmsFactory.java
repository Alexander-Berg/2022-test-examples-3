package ru.yandex.market.delivery.transport_manager.factory;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiDto;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;
import ru.yandex.market.logistics.management.entity.type.ApiType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

@UtilityClass
public class LmsFactory {

    @Nonnull
    public Optional<LogisticsPointResponse> logisticsPointResponse(Long partnerId, String postCode) {
        var address = Address.newBuilder()
            .country("Россия")
            .postCode(postCode)
            .settlement("Some settlement")
            .house("13")
            .region("region")
            .build();

        ScheduleDayResponse schedule = new ScheduleDayResponse(1L, 5, LocalTime.MIDNIGHT, LocalTime.NOON);

        return Optional.of(LogisticsPointResponse.newBuilder()
            .id(99L)
            .address(address)
            .active(true)
            .partnerId(partnerId)
            .schedule(Set.of(schedule))
            .build());
    }

    @Nonnull
    public Optional<LogisticsPointResponse> pointFromMoscowRegion(Long partnerId) {
        var address = Address.newBuilder()
                .country("Россия")
                .postCode("101000")
                .settlement("Some settlement")
                .house("13")
                .locationId(213)
                // должен меняться на "Московская область"
                .region("Москва")
                .build();

        ScheduleDayResponse schedule = new ScheduleDayResponse(1L, 5, LocalTime.MIDNIGHT, LocalTime.NOON);

        return Optional.of(LogisticsPointResponse.newBuilder()
                .id(99L)
                .address(address)
                .active(true)
                .partnerId(partnerId)
                .schedule(Set.of(schedule))
                .build());
    }

    @Nonnull
    public List<SettingsMethodDto> settingsMethods() {
        return List.of(
            settingsMethodDto(1L, 4L, "createSelfExport", true),
            settingsMethodDto(2L, 4L, "createIntake", true),
            settingsMethodDto(3L, 5L, "createSelfExport", true),
            settingsMethodDto(4L, 5L, "createIntake", false),
            settingsMethodDto(5L, 8L, "createSelfExport", true),
            settingsMethodDto(6L, 9L, "createSelfExport", true),
            settingsMethodDto(6L, 9L, "createIntake", true),
            settingsMethodDto(6L, 666L, "createIntake", true)
        );
    }

    @Nonnull
    public List<SettingsApiDto> settingsApiDtos() {
        return List.of(
            settingsApiDto(1L, 4L, ApiType.DELIVERY),
            settingsApiDto(2L, 4L, ApiType.DELIVERY),
            settingsApiDto(3L, 5L, ApiType.DELIVERY),
            settingsApiDto(4L, 5L, ApiType.FULFILLMENT),
            settingsApiDto(5L, 8L, ApiType.FULFILLMENT),
            settingsApiDto(6L, 666L, ApiType.DELIVERY)
        );
    }

    @Nonnull
    public SettingsApiDto settingsApiDto(
            Long id,
            Long partnerId,
            ApiType apiType
    ) {
        return SettingsApiDto.newBuilder()
                .id(id)
                .partnerId(partnerId)
                .apiType(apiType)
                .build();
    }

    @Nonnull
    public SettingsMethodDto settingsMethodDto(
        Long settingsApiId,
        Long partnerId,
        String method,
        boolean active
    ) {
        return SettingsMethodDto.newBuilder()
            .settingsApiId(settingsApiId)
            .partnerId(partnerId)
            .method(method)
            .active(active)
            .build();
    }

    @Nonnull
    public Optional<PartnerResponse> optionalPartnerResponse(Long partnerId, Long marketId, PartnerType partnerType) {
        return Optional.of(
            PartnerResponse.newBuilder()
                .id(partnerId)
                .readableName("Partner market id#" + marketId)
                .marketId(marketId)
                .partnerType(partnerType)
                .build()
        );
    }
}
