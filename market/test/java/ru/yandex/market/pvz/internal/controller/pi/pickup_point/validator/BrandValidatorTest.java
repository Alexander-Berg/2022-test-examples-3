package ru.yandex.market.pvz.internal.controller.pi.pickup_point.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.pvz.internal.controller.pi.pickup_point.dto.LegalPartnerPickupPointDto;
import ru.yandex.market.pvz.internal.controller.pi.pickup_point.dto.PickupPointBrandingTypeDto;
import ru.yandex.market.tpl.common.util.configuration.ConfigurationProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.BRAND_DATE_AND_REGION_VALIDATION_ENABLED;
import static ru.yandex.market.pvz.core.test.factory.TestBrandRegionFactory.DEFAULT_REGIONS;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_BRAND_DATE;

@ExtendWith(SpringExtension.class)
class BrandValidatorTest {

    @Mock
    private ConfigurationProvider configurationProvider;

    @InjectMocks
    private BrandValidator validator;

    @BeforeEach
    void enableValidation() {
        when(configurationProvider.isBooleanEnabled(BRAND_DATE_AND_REGION_VALIDATION_ENABLED)).thenReturn(true);
        validator.setConfigurationProvider(configurationProvider);
    }

    @Test
    void validBrand() {
        LegalPartnerPickupPointDto dto = LegalPartnerPickupPointDto.builder()
                .brandType(PickupPointBrandingTypeDto.FULL)
                .brandRegion(DEFAULT_REGIONS.get(0).getRegion())
                .brandDate(DEFAULT_BRAND_DATE)
                .build();

        validateBrand(dto, true);
    }

    @Test
    void validNotBrand() {
        LegalPartnerPickupPointDto dto = LegalPartnerPickupPointDto.builder()
                .brandType(PickupPointBrandingTypeDto.NONE)
                .build();

        validateBrand(dto, true);
    }

    @Test
    void invalidNullDto() {
        validateBrand(null, false);
    }

    @Test
    void invalidNullRegion() {
        LegalPartnerPickupPointDto dto = LegalPartnerPickupPointDto.builder()
                .brandType(PickupPointBrandingTypeDto.FULL)
                .brandDate(DEFAULT_BRAND_DATE)
                .build();

        validateBrand(dto, false);
    }

    @Test
    void invalidBlankRegion() {
        LegalPartnerPickupPointDto dto = LegalPartnerPickupPointDto.builder()
                .brandType(PickupPointBrandingTypeDto.FULL)
                .brandRegion("   ")
                .brandDate(DEFAULT_BRAND_DATE)
                .build();

        validateBrand(dto, false);
    }

    @Test
    void invalidNullBrandDate() {
        LegalPartnerPickupPointDto dto = LegalPartnerPickupPointDto.builder()
                .brandType(PickupPointBrandingTypeDto.FULL)
                .brandRegion(DEFAULT_REGIONS.get(0).getRegion())
                .build();

        validateBrand(dto, false);
    }

    private void validateBrand(LegalPartnerPickupPointDto dto, boolean expectedValid) {
        assertThat(validator.isValid(dto, null)).isEqualTo(expectedValid);
    }
}
