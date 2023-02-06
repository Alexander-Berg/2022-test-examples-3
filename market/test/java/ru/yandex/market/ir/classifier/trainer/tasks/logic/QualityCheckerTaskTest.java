package ru.yandex.market.ir.classifier.trainer.tasks.logic;

import org.junit.Test;
import ru.yandex.market.ir.classifier.ml.MarketColor;

import java.util.EnumMap;
import java.util.Random;

import static org.junit.Assert.*;

public class QualityCheckerTaskTest {

    @Test
    public void calculateIfCheckPassedTest1() {
        EnumMap<MarketColor, QualityCheckerTask.CheckResult> prevQuality = buildQulaity(
            0.5,0.5, 0.5,0,1
        );
        EnumMap<MarketColor, QualityCheckerTask.CheckResult> currentQuality = buildQulaity(
            0.5,0.5, 0.5,0,1
        );
        EnumMap<MarketColor, Double> acceptableDropRate = buildExpectedDropRateMap(0.0, 0.0);
        assertTrue(QualityCheckerTask.calculateIfCheckPassed(prevQuality, currentQuality, acceptableDropRate));
    }

    @Test
    public void calculateIfCheckPassedTest2() {
        EnumMap<MarketColor, QualityCheckerTask.CheckResult> prevQuality = buildQulaity(
            0.5,0.5, 0.5,0,1
        );
        EnumMap<MarketColor, QualityCheckerTask.CheckResult> currentQuality = buildQulaity(
            0.4,0.5, 0.5,0,1
        );
        EnumMap<MarketColor, Double> acceptableDropRate = buildExpectedDropRateMap(0.05, 0.0);
        assertFalse(QualityCheckerTask.calculateIfCheckPassed(prevQuality, currentQuality, acceptableDropRate));
    }

    @Test
    public void calculateIfCheckPassedTest3() {
        Random random = new Random(4071505);
        int okCount = 0, notOkCount = 0;
        for (int i = 0; i < 10000; ++i) {
            double prevWhite = random.nextDouble();
            double prevGroupedWhite = random.nextDouble();
            double prevBlue = random.nextDouble();
            EnumMap<MarketColor, QualityCheckerTask.CheckResult> prevQuality = buildQulaity(
                prevWhite, prevGroupedWhite, prevBlue,0,1
            );
            double acceptWhite = random.nextDouble() / 5;
            double acceptBlue = random.nextDouble() / 5;
            EnumMap<MarketColor, Double> acceptableDropRate = buildExpectedDropRateMap(
                acceptWhite, acceptBlue
            );
            double currentWhite = random.nextDouble();
            double currentGroupedWhite = random.nextDouble();
            double currentBlue = random.nextDouble();
            boolean shouldBeOk = true;
            shouldBeOk &= currentWhite >= prevWhite - acceptWhite;
            shouldBeOk &= currentGroupedWhite >= prevGroupedWhite - acceptWhite;
            shouldBeOk &= currentBlue >= prevBlue - acceptBlue;

            EnumMap<MarketColor, QualityCheckerTask.CheckResult> currentQuality = buildQulaity(
                currentWhite, currentGroupedWhite, currentBlue,0,1
            );
            if (shouldBeOk) {
                okCount++;
                assertTrue(QualityCheckerTask.calculateIfCheckPassed(prevQuality, currentQuality, acceptableDropRate));
            } else {
                notOkCount++;
                assertFalse(QualityCheckerTask.calculateIfCheckPassed(prevQuality, currentQuality, acceptableDropRate));
            }
            System.out.println("OK count: " + okCount + " notOkCount: " + notOkCount);
        }
    }

    @Test
    public void calculateIfCheckPassedTest4() {
        EnumMap<MarketColor, QualityCheckerTask.CheckResult> prevQuality = buildQulaity(
                0.5,0.5, 0.5,0,1
        );
        EnumMap<MarketColor, QualityCheckerTask.CheckResult> currentQuality = buildQulaity(
                0.5,0.5, 0.5,0.4,0.3
        );
        EnumMap<MarketColor, Double> acceptableDropRate = buildExpectedDropRateMap(0.0, 0.0);
        assertFalse(QualityCheckerTask.calculateIfCheckPassed(prevQuality, currentQuality, acceptableDropRate));
    }

    @Test
    public void calculateIfCheckPassedTest5() {
        EnumMap<MarketColor, QualityCheckerTask.CheckResult> prevQuality = buildQulaity(
                0.5,0.5, 0.5,0.4,1
        );
        EnumMap<MarketColor, QualityCheckerTask.CheckResult> currentQuality = buildQulaity(
                0.5,0.5, 0.5,0.5,0.3
        );
        EnumMap<MarketColor, Double> acceptableDropRate = buildExpectedDropRateMap(0.0, 0.0);
        assertFalse(QualityCheckerTask.calculateIfCheckPassed(prevQuality, currentQuality, acceptableDropRate));
    }

    @Test
    public void checkResultLog2Test() {
        double zeroLog2 = QualityCheckerTask.CheckResult.Builder.log2(1.0);
        assertTrue(zeroLog2 == 0);
        double twoLog2 = QualityCheckerTask.CheckResult.Builder.log2(2);
        assertTrue(twoLog2 == 1);
    }

    @Test
    public void checkResultLogLossComponentTest() {
        double truePred = QualityCheckerTask.CheckResult.Builder.calcLogLossComponent(0,0);
        assertEquals(0, truePred, 0.0);
        double falsePred = QualityCheckerTask.CheckResult.Builder.calcLogLossComponent(0,1);
        // Check log lower bound
        assertEquals(falsePred, -15, 0.0);
        double somePred = QualityCheckerTask.CheckResult.Builder.calcLogLossComponent(1,0.8);
        double someLogloss = -0.3219280948873623;
        assertEquals(someLogloss, somePred, 0.001);
    }

    private EnumMap<MarketColor, Double> buildExpectedDropRateMap(double white, double blue) {
        EnumMap<MarketColor, Double> acceptableDropRate = new EnumMap<>(MarketColor.class);
        acceptableDropRate.put(MarketColor.WHITE, white);
        acceptableDropRate.put(MarketColor.BLUE, blue);
        return acceptableDropRate;
    }

    private EnumMap<MarketColor, QualityCheckerTask.CheckResult> buildQulaity(
            double white, double groupedWhite, double blue,
            double confidenceLogLoss, double bestConstantConfidenceLogLoss
    ) {
        EnumMap<MarketColor, QualityCheckerTask.CheckResult> result = new EnumMap<>(MarketColor.class);
        result.put(MarketColor.WHITE, new QualityCheckerTask.CheckResult(
            null, null, null, white, groupedWhite, 0
                ,confidenceLogLoss, bestConstantConfidenceLogLoss
        ));
        result.put(MarketColor.BLUE, new QualityCheckerTask.CheckResult(
            null, null, null, blue, 0, 0,
                confidenceLogLoss, bestConstantConfidenceLogLoss
        ));
        return result;
    }
}