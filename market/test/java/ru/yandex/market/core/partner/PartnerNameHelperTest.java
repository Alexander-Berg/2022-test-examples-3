package ru.yandex.market.core.partner;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

/**
 * Тесты для {@link PartnerNameHelper}.
 */
@DbUnitDataSet(before = "PartnerNameHelperTest.csv")
public class PartnerNameHelperTest extends FunctionalTest {
    @Autowired
    PartnerNameHelper partnerNameHelper;

    @ParameterizedTest
    @MethodSource("args")
    void getNameTest(long partnerId, String expectedName) {
        Assertions.assertEquals(expectedName, partnerNameHelper.getPartnerName(partnerId));
    }

    private static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(1, "bus1"),
                Arguments.of(2, "bus2"),
                Arguments.of(3, "shop3"),
                Arguments.of(4, null)
        );
    }

}
