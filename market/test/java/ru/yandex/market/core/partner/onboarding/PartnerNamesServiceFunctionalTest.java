package ru.yandex.market.core.partner.onboarding;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для {@link PartnerNamesService}.
 */
class PartnerNamesServiceFunctionalTest extends FunctionalTest {

    @Autowired
    PartnerNamesService partnerNamesService;

    @DbUnitDataSet(before = "names/PartnerNamesServiceFunctionalTest.before.csv")
    @DisplayName("Генерация названия магазина")
    @ParameterizedTest(name = "{0}")
    @MethodSource(value = "namesGenerationTestArguments")
    void checkNamesGenerations(String testDesc, Long businessId, String expectedName) {
        assertThat(partnerNamesService.generatePartnerName(businessId))
                .isEqualTo(expectedName);
    }

    static Stream<Arguments> namesGenerationTestArguments() {
        return Stream.of(
                Arguments.of("нет магазинов на бизнесе, берем имя бизнеса", 100L, "business"),
                Arguments.of("на бизнесе есть магазины, но не содержащие имя бизнеса, берем имя бизнеса", 200L, "Rediska"),
                Arguments.of("на бизесе уже есть шопы с такими именами, проверяем счетчик", 300L, "Arbuzik 3"),
                Arguments.of("Корректная работа счетчика при удаленных магазинах их бизнеса", 400L, "Pomidorka 4")
        );
    }

}
