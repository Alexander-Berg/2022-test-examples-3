package ru.yandex.market.logistics.logistics4shops.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.logistics4shops.AbstractTest;
import ru.yandex.market.logistics.logistics4shops.api.model.PartnerType;

@DisplayName("Конвертация перечислений между внутренним представлением и внешним")
class PartnerTypeEnumConverterTest extends AbstractTest {

    private final EnumConverter enumConverter = new EnumConverter();

    @ParameterizedTest
    @EnumSource(PartnerType.class)
    @DisplayName("Конвертация PartnerType внешнее -> внутреннее")
    void partnerTypeFromExternal(PartnerType partnerType) {
        softly.assertThat(enumConverter.convert(
                partnerType,
                ru.yandex.market.logistics.logistics4shops.model.enums.PartnerType.class
            ))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.logistics4shops.model.enums.PartnerType.class)
    @DisplayName("Конвертация PartnerType внутреннее -> внешнее")
    void partnerTypeToExternal(ru.yandex.market.logistics.logistics4shops.model.enums.PartnerType partnerType) {
        softly.assertThat(enumConverter.convert(
                partnerType,
                PartnerType.class
            ))
            .isNotNull();
    }
}
