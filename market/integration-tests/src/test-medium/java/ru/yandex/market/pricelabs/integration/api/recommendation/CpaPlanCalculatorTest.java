package ru.yandex.market.pricelabs.integration.api.recommendation;


import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.pricelabs.model.types.RecommendationType;
import ru.yandex.market.pricelabs.processing.cpa.CpaPlanCalculator;
import ru.yandex.market.pricelabs.processing.cpa.CpaPlanCalculatorImpl;
import ru.yandex.market.pricelabs.processing.cpa.CpaRecommendation;

public class CpaPlanCalculatorTest {

    private final CpaPlanCalculator calculator = new CpaPlanCalculatorImpl();

    public static Stream<Arguments> recommendations() {
        //
        return Stream.of(
                // ---------- меньше оптимума
                // меньше оптимум-1 то минимум
                Arguments.of(540, RecommendationType.MINIMUM, new CpaRecommendation(0, 0, 700, 650, 0, 700, 650)),
                // равна оптимум-1 то оптимум
                Arguments.of(540, RecommendationType.OPTIMUM, new CpaRecommendation(0, 0, 700, 640, 0, 700, 640)),
                // больше оптимум-1 то оптимум
                Arguments.of(540, RecommendationType.OPTIMUM, new CpaRecommendation(0, 0, 700, 600, 0, 700, 600)),

                //--------- между оптимумом и максимумом
                // меньше максимум-1 то оптимум
                Arguments.of(640, RecommendationType.OPTIMUM, new CpaRecommendation(0, 0, 800, 600, 0, 800, 600)),
                // равна максимум-1 то максимум
                Arguments.of(700, RecommendationType.MAXIMUM, new CpaRecommendation(0, 0, 800, 600, 0, 800, 600)),
                // больше максимум-1 то максимум
                Arguments.of(710, RecommendationType.MAXIMUM, new CpaRecommendation(0, 0, 800, 600, 0, 800, 600)),

                //--------- выше или равна максимума
                Arguments.of(800, RecommendationType.MAXIMUM, new CpaRecommendation(0, 0, 800, 600, 0, 800, 600)),
                Arguments.of(810, RecommendationType.MAXIMUM, new CpaRecommendation(0, 0, 800, 600, 0, 800, 600)),

                // --- нет обоих
                Arguments.of(800, RecommendationType.DEFAULT, new CpaRecommendation(0, 0, 0, 0, 0, 0, 0))

        );
    }

    @ParameterizedTest
    @MethodSource("recommendations")
    public void testPlanCalculation(long currentBid, RecommendationType plan, CpaRecommendation recommendation) {
        Assertions.assertEquals(plan, calculator.getRecommendationPlan(currentBid, recommendation));
    }
}
