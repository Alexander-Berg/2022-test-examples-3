package ru.yandex.market.fulfillment.wrap.marschroute.service.calculator;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.fulfillment.wrap.marschroute.entity.service.MarschrouteService;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.service.MarschrouteServiceDiff;
import ru.yandex.market.fulfillment.wrap.marschroute.service.services.MarschrouteServiceDiffCalculator;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.market.fulfillment.wrap.marschroute.service.calculator.MarschrouteServiceDiffCalculatorTest.MARSCHROUTE_SERVICE_KEY;

class MarschrouteServiceDiffMathCalculatorTest {

    private final MarschrouteServiceDiffCalculator calculator = new MarschrouteServiceDiffCalculator();

    @Nonnull
    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(
                BigDecimal.valueOf(100),
                new MarschrouteServiceDiff().setSum(BigDecimal.valueOf(200)).setVersion(5),
                new MarschrouteServiceDiff()
                    .setDiff(BigDecimal.valueOf(-100))
                    .setSum(BigDecimal.valueOf(100))
                    .setVersion(6)
            ),
            Arguments.of(
                new BigDecimal("100.00"),
                new MarschrouteServiceDiff().setSum(BigDecimal.valueOf(100)).setVersion(5),
                null
            ),
            Arguments.of(
                new BigDecimal("100.00"),
                new MarschrouteServiceDiff().setSum(new BigDecimal("100.000")).setVersion(5),
                null
            ),
            Arguments.of(
                new BigDecimal("0.000"),
                new MarschrouteServiceDiff().setSum(new BigDecimal("-150")).setVersion(5),
                new MarschrouteServiceDiff()
                    .setDiff(BigDecimal.valueOf(150))
                    .setSum(BigDecimal.valueOf(0))
                    .setVersion(6)
            )
        );
    }

    /**
     * Проверяет корректность операций сравнения/калькуляции и пр. в рамках генерации разницы между
     * существующими в обоих источниках услугами.
     */
    @MethodSource("data")
    @ParameterizedTest
    void mathCorrectness(
        BigDecimal marschrouteSum,
        MarschrouteServiceDiff wrapDiff,
        MarschrouteServiceDiff expectedDiff
    ) {
        MarschrouteService wrapService = new MarschrouteService()
            .setServiceKey(MARSCHROUTE_SERVICE_KEY)
            .setServiceDiff(wrapDiff);

        Optional<MarschrouteServiceDiff> actualDiff = calculator.calculateDiff(
            new TestRawService(marschrouteSum, MARSCHROUTE_SERVICE_KEY),
            wrapService
        );

        Optional<MarschrouteServiceDiff> expectedDiffOpt = Optional.ofNullable(expectedDiff);

        assertSoftly(assertions -> {
            assertions.assertThat(actualDiff.isPresent())
                .as("Assert that diffs are either both exist or both do not")
                .isEqualTo(expectedDiffOpt.isPresent());

            if (actualDiff.isPresent() && expectedDiffOpt.isPresent()) {
                MarschrouteServiceDiff actual = actualDiff.get();
                MarschrouteServiceDiff expected = expectedDiffOpt.get();

                assertMatch(assertions, actual, expected);
            }
        });

    }

    private void assertMatch(SoftAssertions assertions,
                             MarschrouteServiceDiff actual,
                             MarschrouteServiceDiff expected) {
        assertions.assertThat(actual.getSum())
            .as("Asserting sum value")
            .isEqualByComparingTo(expected.getSum());

        assertions.assertThat(actual.getDiff())
            .as("Asserting diff value")
            .isEqualByComparingTo(expected.getDiff());

        assertions.assertThat(actual.getVersion())
            .as("Asserting version value")
            .isEqualTo(expected.getVersion());
    }
}
