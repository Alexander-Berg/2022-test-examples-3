package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

/**
 * @author stani on 22.02.18.
 */
@DbUnitDataSet(before = "supplierApplicationStatusCheckerTest.csv")
public class SupplierApplicationStatusCheckerTest extends FunctionalTest {

    @Autowired
    SupplierApplicationStatusChecker supplierApplicationStatusChecker;

    public static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of("Should permit if INIT", 1L, "INIT,", Boolean.TRUE),
                Arguments.of("Should prohibit if -INIT", 1L, "-INIT", Boolean.FALSE),
                Arguments.of("Should permit if INIT", 1L, "IN_PROGRESS,INIT,NEW", Boolean.TRUE),
                Arguments.of("Should permit if -CLOSED,FROZEN", 1L, "-CLOSED,FROZEN", Boolean.TRUE),
                Arguments.of("Should permit if IN_PROGRESS", 2L, "INIT,IN_PROGRESS,NEW", Boolean.TRUE),
                Arguments.of("Should prohibit if allow list is empty", 1L, "", Boolean.FALSE),
                Arguments.of("Should prohibit if INIT not in allow list", 1L, "FROZEN,NEW", Boolean.FALSE),
                Arguments.of("Should prohibit if campaign not exist", 404L, "COMPLETED,NEW", Boolean.FALSE),
                Arguments.of("Should prohibit if request id is null", 3L, "COMPLETED,NEW", Boolean.FALSE)
        );
    }

    @ParameterizedTest
    @MethodSource("args")
    public void test(String desc, long campaignId, String permittedStatues, boolean expected) {
        MockPartnerRequest mockPartnerRequest = new MockPartnerRequest(-1, -1L, -1L, campaignId);
        Authority authority = new Authority("test", permittedStatues);
        Assertions.assertEquals(
                expected,
                supplierApplicationStatusChecker.checkTyped(mockPartnerRequest, authority),
                desc
        );
    }
}
