package ru.yandex.market.logistics.logistics4shops.utils;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.logistics4shops.client.api.model.Outbound;
import ru.yandex.market.logistics.logistics4shops.client.api.model.PartnerMappingDto;
import ru.yandex.market.logistics.logistics4shops.client.api.model.PartnerType;

@UtilityClass
@ParametersAreNonnullByDefault
public final class ModelFactory {
    @Nonnull
    public PartnerMappingDto dropshipPartnerMappingDto(Long mbiPartnerId, Long lmsPartnerId) {
        return partnerMappingDto(mbiPartnerId, lmsPartnerId, PartnerType.DROPSHIP);
    }

    @Nonnull
    public PartnerMappingDto partnerMappingDto(Long mbiPartnerId, Long lmsPartnerId, PartnerType partnerType) {
        return new PartnerMappingDto()
            .mbiPartnerId(mbiPartnerId)
            .lmsPartnerId(lmsPartnerId)
            .partnerType(partnerType);
    }

    @Nonnull
    public Outbound outbound(long id, String yandexId, String externalId, List<String> orderIds) {
        return new Outbound()
            .id(id)
            .yandexId(yandexId)
            .externalId(externalId)
            .created(Instant.parse("2022-02-21T10:30:00.00Z"))
            .intervalFrom(Instant.parse("2022-02-21T11:30:00.00Z"))
            .intervalTo(Instant.parse("2022-02-21T13:30:00.00Z"))
            .orderIds(orderIds);
    }
}
