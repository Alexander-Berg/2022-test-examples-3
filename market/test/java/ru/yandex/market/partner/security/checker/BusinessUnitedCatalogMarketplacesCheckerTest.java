package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.security.BusinessUidable;
import ru.yandex.market.core.security.DefaultBusinessUidable;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

@DbUnitDataSet(before = "BusinessUnitedCatalogMarketplacesCheckerTest.before.csv")
class BusinessUnitedCatalogMarketplacesCheckerTest extends FunctionalTest {

    final static Authority AUTHORITY = new Authority();

    @Autowired
    BusinessUnitedCatalogMarketplacesChecker businessUnitedCatalogMarketplacesChecker;

    private static Stream<Arguments> businessMarketplacesArgs() {
        return Stream.of(
                Arguments.of(1L, true),   // все маркетплейсы, все в Едином Каталоге
                Arguments.of(2L, false),  // нет маркетплейсов
                Arguments.of(3L, true),   // маркетплейс-DBS в Едином Каталоге
                Arguments.of(4L, false),  // нет маркетплейсов, ADV в Едином Каталоге
                Arguments.of(5L, false),  // все маркетплейсы, один не в Едином Каталоге
                Arguments.of(6L, false),  // есть маркетплейсы, в Едином Каталоге только ADV
                Arguments.of(7L, true),   // все в Едином Каталоге
                Arguments.of(8L, true),   // в бизнесе DROPSHIP - C&C в Едином Каталоге
                Arguments.of(null, false) // нет businessId
        );
    }

    @ParameterizedTest
    @MethodSource("businessMarketplacesArgs")
    void checkTyped(Long businessId, boolean expectedResult) {
        final BusinessUidable businessUidable = new DefaultBusinessUidable(businessId, -1, -1);
        Assertions.assertEquals(expectedResult,
                businessUnitedCatalogMarketplacesChecker.checkTyped(businessUidable, AUTHORITY));
    }
}
