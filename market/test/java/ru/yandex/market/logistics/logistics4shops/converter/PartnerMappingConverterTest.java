package ru.yandex.market.logistics.logistics4shops.converter;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.logistics4shops.AbstractTest;
import ru.yandex.market.logistics.logistics4shops.api.model.CreatePartnerMappingRequest;
import ru.yandex.market.logistics.logistics4shops.api.model.PartnerMappingDto;
import ru.yandex.market.logistics.logistics4shops.api.model.PartnerType;
import ru.yandex.market.logistics.logistics4shops.model.entity.PartnerMapping;

@DisplayName("Конвертация перечислений между внутренним представлением и внешним")
class PartnerMappingConverterTest extends AbstractTest {
    private static final long MBI_PARTNER_ID = 123L;
    private static final long LMS_PARTNER_ID = 456L;

    private final PartnerMappingConverter partnerMappingConverter = new PartnerMappingConverter(new EnumConverter());

    @Test
    @DisplayName("Конвертация запроса на создание во внутреннее представление")
    void createRequestToMapping() {
        softly.assertThat(partnerMappingConverter.fromCreateRequest(createRequest()))
            .usingRecursiveComparison()
            .isEqualTo(partnerMapping());
    }

    @Test
    @DisplayName("Конвертация внутреннего представления во внешнее")
    void internalToExternal() {
        softly.assertThat(partnerMappingConverter.toDto(partnerMapping()))
            .usingRecursiveComparison()
            .isEqualTo(partnerMappingDto());
    }

    @Nonnull
    private CreatePartnerMappingRequest createRequest() {
        return new CreatePartnerMappingRequest()
            .mbiPartnerId(MBI_PARTNER_ID)
            .lmsPartnerId(LMS_PARTNER_ID)
            .partnerType(PartnerType.DROPSHIP);
    }

    @Nonnull
    private PartnerMapping partnerMapping() {
        return new PartnerMapping()
            .setMbiPartnerId(MBI_PARTNER_ID)
            .setLmsPartnerId(LMS_PARTNER_ID)
            .setPartnerType(ru.yandex.market.logistics.logistics4shops.model.enums.PartnerType.DROPSHIP);
    }

    @Nonnull
    private PartnerMappingDto partnerMappingDto() {
        return new PartnerMappingDto()
            .mbiPartnerId(MBI_PARTNER_ID)
            .lmsPartnerId(LMS_PARTNER_ID)
            .partnerType(PartnerType.DROPSHIP);
    }
}
