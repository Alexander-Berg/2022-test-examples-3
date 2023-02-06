package ru.yandex.autotests.market.services.formalizer.result;

import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.autotests.market.OfferData;
import ru.yandex.autotests.market.ResponseData;
import ru.yandex.autotests.market.comparison.ComparisonItem;
import ru.yandex.autotests.market.services.formalizer.FormalizedOfferComparator;
import ru.yandex.market.ir.http.Formalizer;
import ru.yandex.market.ir.http.Formalizer.FormalizedOffer;

import static org.junit.Assert.assertEquals;

public class StatisticsCollectorTest {
    private static final Integer STATISTICS_SIZE = 1000;
    private static StatisticsCollector statisticsCollector = new StatisticsCollector();
    private static Long totalOfferCount = 0L;
    private static Long totalUnchangedOfferCount = 0L;
    private static Long totalDifferentOfferCount = 0L;
    private static Long totalSlightlyOfferCount = 0L;
    private static Double averageStableResponseTime = 0.0;
    private static Double averageTestingResponseTime = 0.0;


    @BeforeClass
    public static void fillStatistics() {
        Double totalStableResponse = 0.0;
        Double totalTestingResponse = 0.0;
        for(int i = 0; i < STATISTICS_SIZE; i++) {
            Integer categoryId = Math.toIntExact(Math.round(Math.random()* STATISTICS_SIZE / 10));
            Long stableResponseTime = Math.round(Math.random());
            Long testingResponseTime = Math.round(Math.random());
            FormalizedOfferComparator status;
            switch (i % 3) {
                case 0:
                    status = FormalizedOfferComparator.SAME;
                    totalUnchangedOfferCount++;
                    break;
                case 1:
                    status = FormalizedOfferComparator.DIFFERENT;
                    totalDifferentOfferCount++;
                    break;
                case 2:
                    status = FormalizedOfferComparator.SLIGHTLY_DIFFERENT;
                    totalSlightlyOfferCount++;
                    break;
                default:
                    throw new IllegalStateException("Unexpected state in switch");
            }
            totalOfferCount++;
            totalStableResponse += stableResponseTime;
            totalTestingResponse += testingResponseTime;
            statisticsCollector.add(comparisonItemBuilder(categoryId, stableResponseTime, testingResponseTime, status));
        }
        averageStableResponseTime = totalStableResponse / STATISTICS_SIZE / 1000000;
        averageTestingResponseTime = totalTestingResponse / STATISTICS_SIZE / 1000000;
    }


    @Test
    public void testOfferCountCalculatedCorrect() {
        assertEquals("fail to check offers count", totalOfferCount, (Long) statisticsCollector.getOffersCount());
    }

    @Test
    public void testUnchangedCountCalculatedCorrect() {
        assertEquals("fail to check offers count", totalUnchangedOfferCount, (Long)statisticsCollector.getUnchangedOffersCount());

    }

    @Test
    public void testDifferentCountCalculatedCorrect() {
        assertEquals("fail to check offers count", totalDifferentOfferCount, (Long)statisticsCollector.getChangedOffersCount());

    }

    @Test
    public void testSlightlyCountCalculatedCorrect() {
        assertEquals("fail to check offers count", totalSlightlyOfferCount, (Long)statisticsCollector.getSlightlyChangedOffersCount());

    }

    @Test
    public void testAverageResponsesCalculatedCorrect() {
        assertEquals("fail to check offers count", averageStableResponseTime, (Double)statisticsCollector.getAverageStableResponseTimeMs());
        assertEquals("fail to check offers count", averageTestingResponseTime, (Double)statisticsCollector.getAverageTestingResponseTimeMs());

    }

    private static ComparisonItem<OfferData<Formalizer.Offer>, ?, FormalizedOfferComparator> comparisonItemBuilder(
            Integer categoryId,
            Long stableResponseTime,
            Long testingResponseTime,
            FormalizedOfferComparator compareStatus
    ) {
        ResponseData<FormalizedOffer> stableResponse = new ResponseData<>(stableResponseTime, FormalizedOffer.getDefaultInstance());
        ResponseData<FormalizedOffer> testingResponse = new ResponseData<>(testingResponseTime, FormalizedOffer.getDefaultInstance());
        OfferData<Formalizer.Offer> offer = new OfferData<>("1", Formalizer.Offer.newBuilder().setCategoryId(categoryId).build());
        return new ComparisonItem<>(offer, "1", compareStatus, stableResponse, testingResponse);
    }
}
