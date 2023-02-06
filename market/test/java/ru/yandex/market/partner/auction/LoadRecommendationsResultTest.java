package ru.yandex.market.partner.auction;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class LoadRecommendationsResultTest {

    /**
     * По умолчанию флаг об ошибке не взведен!
     */
    @Test
    public void test_build_when_notSet_should_returnNoWarning() {
        LoadRecommendationsResult res = LoadRecommendationsResult.builder().build();
        assertFalse(res.hasExceptionsDuringProcessing());
    }

    /**
     * Если не установлены в билдере, то возвращаемые коллекцие пустые а не null.
     */
    @Test
    public void test_build_when_notSet_should_returnEmptyCollectionsInsteadOfNull() {
        LoadRecommendationsResult res = LoadRecommendationsResult.builder().build();
        assertNotNull(res.getWarnings());
        assertNotNull(res.getOffersData());
    }
}