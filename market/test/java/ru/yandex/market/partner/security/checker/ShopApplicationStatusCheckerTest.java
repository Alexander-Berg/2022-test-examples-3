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
 * @author dpastukhov
 */
@DbUnitDataSet(before = "partnerApplicationStatusCheckerTest.csv")
class ShopApplicationStatusCheckerTest extends FunctionalTest {

    @Autowired
    private ShopApplicationStatusChecker applicationStatusChecker;

    public static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of("Should permit if NEW", 4L, "NEW,COMPLETED", Boolean.TRUE),
                Arguments.of("Should permit if any", 4L, "*", Boolean.TRUE),
                Arguments.of("Should prohibit if -NEW", 4L, "-NEW,", Boolean.FALSE),
                Arguments.of("Should permit if NEW", 4L, "-COMPLETED,CANCELLED", Boolean.TRUE),
                Arguments.of("Should prohibit if campaign not exist", 404L, "NEW,COMPLETED", Boolean.FALSE),
                Arguments.of("Should prohibit if campaign not exist", 404L, "*", Boolean.FALSE),
                Arguments.of("Should permit if INIT", 1L, "INIT,", Boolean.TRUE),
                Arguments.of("Should prohibit if -INIT", 1L, "-INIT", Boolean.FALSE),
                Arguments.of("Should permit if INIT", 1L, "IN_PROGRESS,INIT,NEW", Boolean.TRUE),
                Arguments.of("Should permit if -CLOSED,FROZEN", 1L, "-CLOSED,FROZEN", Boolean.TRUE),
                Arguments.of("Should permit if IN_PROGRESS", 2L, "INIT,IN_PROGRESS,NEW", Boolean.TRUE),
                Arguments.of("Should prohibit if allow list is empty", 1L, "", Boolean.FALSE),
                Arguments.of("Should prohibit if allow list is empty", 4L, "", Boolean.FALSE),
                Arguments.of("Should prohibit if INIT not in allow list", 1L, "FROZEN,NEW", Boolean.FALSE),
                Arguments.of("Should prohibit if request id is null", 3L, "COMPLETED,NEW", Boolean.FALSE),
                Arguments.of("Should prohibit if -COMPLETED", 5L, "-COMPLETED,NEW", Boolean.FALSE),
                Arguments.of("Should permit if COMPLETED", 5L, "COMPLETED,NEW", Boolean.TRUE)
        );
    }

    @ParameterizedTest
    @MethodSource("args")
    void test(String desc, long partnerId, String permittedStatues, boolean expected) {
        Authority authority = new Authority("test", permittedStatues);
        Assertions.assertEquals(
                expected,
                applicationStatusChecker.checkTyped(new MockPartnerRequest(0L, 0, partnerId, partnerId), authority),
                desc
        );
    }
}
