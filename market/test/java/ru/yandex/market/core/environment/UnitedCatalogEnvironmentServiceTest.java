package ru.yandex.market.core.environment;

import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

/**
 * Date: 27.10.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@SuppressWarnings("unused")
@DbUnitDataSet
class UnitedCatalogEnvironmentServiceTest extends FunctionalTest {

    @Autowired
    private UnitedCatalogEnvironmentService unitedCatalogEnvironmentService;

    @DisplayName("Проверка флагов, которые должны быть во включенном состоянии")
    @ParameterizedTest(name = "{0}")
    @MethodSource("testFlagsData")
    @DbUnitDataSet(before = "UnitedCatalogEnvironmentService/enabled.before.csv")
    void testEnabledFlags(String name, Function<UnitedCatalogEnvironmentService, Boolean> checker) {
        Assertions.assertTrue(checker.apply(unitedCatalogEnvironmentService));
    }

    @DisplayName("Проверка флагов, которые должны быть в отключенном состоянии")
    @ParameterizedTest(name = "{0}")
    @MethodSource("testFlagsData")
    @DbUnitDataSet(before = "UnitedCatalogEnvironmentService/disabled.before.csv")
    void testDisabledFlags(String name, Function<UnitedCatalogEnvironmentService, Boolean> checker) {
        Assertions.assertFalse(checker.apply(unitedCatalogEnvironmentService));
    }

    @DisplayName("Проверка дефолтного состояния флагов")
    @ParameterizedTest(name = "{0}")
    @MethodSource("testFlagsData")
    void testDefaultFlags(String name, Function<UnitedCatalogEnvironmentService, Boolean> checker) {
        Assertions.assertFalse(checker.apply(unitedCatalogEnvironmentService));
    }

    private static Stream<Arguments> testFlagsData() {
        return Stream.of(
                Arguments.of(
                        "isEnabledHiddenOffersViaDatacamp",
                        (Function<UnitedCatalogEnvironmentService, Boolean>)
                                UnitedCatalogEnvironmentService::isEnabledHiddenOffersViaDatacamp
                ),
                Arguments.of(
                        "isHiddenOfferDuplicateFixEnabled",
                        (Function<UnitedCatalogEnvironmentService, Boolean>)
                                UnitedCatalogEnvironmentService::isHiddenOfferDuplicateFixEnabled
                ),
                Arguments.of(
                        "isValidationCpaStatusEnabled",
                        (Function<UnitedCatalogEnvironmentService, Boolean>)
                                UnitedCatalogEnvironmentService::isValidationCpaStatusEnabled
                ),
                Arguments.of(
                        "isAboShopHiddenOfferDisabled",
                        (Function<UnitedCatalogEnvironmentService, Boolean>)
                                UnitedCatalogEnvironmentService::isAboShopHiddenOfferDisabled
                )
        );
    }
}
