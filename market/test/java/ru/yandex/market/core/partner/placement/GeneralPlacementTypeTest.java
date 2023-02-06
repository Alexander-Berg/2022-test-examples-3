package ru.yandex.market.core.partner.placement;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.business.MarketServiceType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Проверяем группировку типов размещения для отображения в бизнесе.
 */
class GeneralPlacementTypeTest {

    private static Stream<Arguments> checkData() {
        return Stream.of(
                Arguments.of(MarketServiceType.SHOP, PartnerPlacementProgramType.DROPSHIP_BY_SELLER,
                        GeneralPlacementType.MARKETPLACE),
                Arguments.of(MarketServiceType.SHOP, PartnerPlacementProgramType.CPC,
                        GeneralPlacementType.ADV),
                Arguments.of(MarketServiceType.SUPPLIER, null, GeneralPlacementType.MARKETPLACE),
                Arguments.of(MarketServiceType.SUPPLIER, PartnerPlacementProgramType.DROPSHIP,
                        GeneralPlacementType.MARKETPLACE),
                Arguments.of(MarketServiceType.DELIVERY, null, GeneralPlacementType.DELIVERY),
                Arguments.of(MarketServiceType.SHOP, null, GeneralPlacementType.ADV)
        );
    }

    private static Stream<Arguments> checkThrowExceptionData() {
        return Stream.of(
                Arguments.of(MarketServiceType.DATACAMP, null)
        );
    }

    @ParameterizedTest
    @MethodSource("checkData")
    void check(MarketServiceType serviceType, PartnerPlacementProgramType placementProgramType,
               GeneralPlacementType expected) {
        assertThat(GeneralPlacementType.getGeneralPlacementType(serviceType, placementProgramType)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("checkThrowExceptionData")
    void checkThrowException(MarketServiceType serviceType, PartnerPlacementProgramType placementProgramType) {
        assertThatThrownBy(() -> GeneralPlacementType.getGeneralPlacementType(serviceType, placementProgramType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("General type is not supported for service type %s and placement %s",
                        serviceType, placementProgramType);
    }

}
