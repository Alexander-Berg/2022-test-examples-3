package ru.yandex.market.logistics.lom.converter.lms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.converter.EnumConverter;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

@DisplayName("PartnerTypeLmsConverter юнит тест")
public class PartnerTypeLmsConverterTest extends AbstractTest {

    private final PartnerTypeLmsConverter partnerTypeLmsConverter = new PartnerTypeLmsConverter(new EnumConverter());

    @DisplayName("Проверяем конвертацию типов партнеров из LMS")
    @ParameterizedTest
    @EnumSource(
        value = PartnerType.class,
        names = {
            "XDOC",
            "DROPSHIP_BY_SELLER",
            "LINEHAUL",
            "DISTRIBUTION_CENTER",
            "FIRST_PARTY_SUPPLIER",
            "SCRAP_DISPOSER",
            "RETAIL",
        },
        mode = EnumSource.Mode.EXCLUDE
    )
    void checkPartnerTypeConversionFromLms(PartnerType partnerType) {
        softly.assertThat(partnerTypeLmsConverter.fromExternal(partnerType)).isNotNull();
    }
}
