package ru.yandex.market.logistics.cs.service;

import java.util.List;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.cs.AbstractIntegrationTest;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static org.assertj.core.api.Assertions.assertThat;

@DatabaseSetup("/service/partner_cargo_type_factor.xml")
class PartnerCargoTypeFactorServiceTest extends AbstractIntegrationTest {

    private static final Double DEFAULT_FACTOR = 1.0;
    private static final Long PARTNER_ID_1 = 100L;
    private static final Long PARTNER_ID_2 = 101L;
    private static final List<Integer> CARGO_TYPE_1 = List.of(1);
    private static final List<Integer> CARGO_TYPE_2 = List.of(2);
    private static final Double FACTOR_VALUE_2_4 = 2.4;
    private static final Double FACTOR_VALUE_1_4 = 1.4;

    @Autowired
    private PartnerCargoTypeFactorService service;

    @Test
    @DisplayName("Ходим в кеш за всеми коэффициентами один раз")
    @JpaQueriesCount(1)
    void useCacheForGetFactor() {
        softly.assertThat(service.getMaxFactor(PARTNER_ID_1, CARGO_TYPE_1)).isEqualTo(FACTOR_VALUE_2_4);
        softly.assertThat(service.getMaxFactor(PARTNER_ID_1, CARGO_TYPE_2)).isEqualTo(FACTOR_VALUE_1_4);
        softly.assertThat(service.getMaxFactor(PARTNER_ID_2, CARGO_TYPE_1)).isEqualTo(FACTOR_VALUE_2_4);
        softly.assertThat(service.getMaxFactor(PARTNER_ID_1, CARGO_TYPE_1)).isEqualTo(FACTOR_VALUE_2_4);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getFactorSource")
    void testGetFactor(
        String displayName,
        Long partnerId,
        List<Integer> cargoTypes,
        Double expectedFactor
    ) {
        Double actualFactor = service.getMaxFactor(partnerId, cargoTypes);
        assertThat(actualFactor).isEqualTo(expectedFactor);
    }

    private static Stream<Arguments> getFactorSource() {
        return Stream.of(
            Arguments.of("коэффициент из БД", PARTNER_ID_1, CARGO_TYPE_1, FACTOR_VALUE_2_4),
            Arguments.of("стандартный коэффициент, если нет настроек для партнёра", 999L, CARGO_TYPE_1, DEFAULT_FACTOR),
            Arguments.of(
                "стандартный коэффициент, если у партнёра нет настроек для карготипа",
                PARTNER_ID_1,
                List.of(999),
                DEFAULT_FACTOR
            ),
            Arguments.of(
                "Стандартный коэффициент, если список карготипов пуст",
                PARTNER_ID_1,
                List.of(),
                DEFAULT_FACTOR
            ),
            Arguments.of(
                "Стандартный коэффициент, если списка карготипов нет",
                PARTNER_ID_1,
                null,
                DEFAULT_FACTOR
            )
        );
    }
}
