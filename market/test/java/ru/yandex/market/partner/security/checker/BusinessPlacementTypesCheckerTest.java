package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.security.DefaultBusinessUidable;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Порверяем работу проверку размещения партнеров в бизнесе
 */
@DbUnitDataSet(before = "BusinessPlacementTypesCheckerTest.before.csv")
class BusinessPlacementTypesCheckerTest extends FunctionalTest {

    @Autowired
    private BusinessPlacementTypesChecker businessPlacementTypesChecker;

    private static Stream<Arguments> checkTypedTestData() {
        return Stream.of(
                //business does not exist
                Arguments.of(11100L, "DROPSHIP", false),
                //empty filter, business has not got dbs
                Arguments.of(20001L, "", false),
                //empty filter, business has got dbs
                Arguments.of(11101L, "", true),
                //self-exclusive filter
                Arguments.of(11101L, "DROPSHIP,-DROPSHIP", false),
                //empty business
                Arguments.of(null, "DROPSHIP_BY_SELLER", false),
                //business has dbs
                Arguments.of(11101L, "DROPSHIP_BY_SELLER", true),
                //business hasn't got dropship
                Arguments.of(11101L, "-DROPSHIP", true),
                //business has dbs, and hasn't dropship
                Arguments.of(11101L, "DROPSHIP_BY_SELLER,-DROPSHIP", true),
                //business hasn't got dbs
                Arguments.of(11201L, "DROPSHIP_BY_SELLER", false),
                //business hasn't got dbs
                Arguments.of(11201L, "-DROPSHIP_BY_SELLER", true),
                //business hasn't got dbs, but has delivery
                Arguments.of(11301L, "DROPSHIP_BY_SELLER", false),
                //business has got C&C
                Arguments.of(20001L, "CLICK_AND_COLLECT", true),
                //business has got C&C
                Arguments.of(20001L, "CLICK_AND_COLLECT,-DROPSHIP_BY_SELLER", true),
                //business has not got C&C
                Arguments.of(20011L, "CLICK_AND_COLLECT", false),
                //business has not got C&C
                Arguments.of(20011L, "-CLICK_AND_COLLECT", true)
        );
    }

    @ParameterizedTest
    @MethodSource("checkTypedTestData")
    void checkTypedTest(Long businessId, String authorityValue, boolean expectedResponse) {
        Authority authority = new Authority("test", authorityValue);
        DefaultBusinessUidable businessUidable = new DefaultBusinessUidable(businessId, 111L, 222L);
        assertThat(businessPlacementTypesChecker.checkTyped(businessUidable, authority))
                .isEqualTo(expectedResponse);
    }
}
