package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

@DbUnitDataSet(before = "CanEnableWithoutModerationCheckerTest.before.csv")
public class CanEnableWithoutModerationCheckerTest extends FunctionalTest {
    @Autowired
    CanEnableWithoutModerationChecker canEnableWithoutModerationChecker;


    private static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("Реплицированный магазин, у которого родитель в белом списке", 4102102, true),
                Arguments.of("Реплицированный магазин, у которого родитель не в белом списке", 4102107, false),
                Arguments.of("Родительский магазин в белом списке", 4102, false),
                Arguments.of("Несуществующий магазин", 99999, false)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    void testCheck(String description, long campaignId, boolean expected) {
        assertThat(canEnableWithoutModerationChecker.checkTyped(
                new MockPartnerRequest(0, 0, 0, campaignId), null))
                .isEqualTo(expected);
    }
}
