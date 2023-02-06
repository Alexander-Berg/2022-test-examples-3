package ru.yandex.ir.modelsclusterizer.core.classify.features;

import org.junit.Test;
import ru.yandex.ir.modelsclusterizer.be.FormalizedOffer;

import static org.junit.Assert.assertEquals;

/**
 * @author mkrasnoperov
 */
public class BagOfWordsFeaturesExtractorTest {
    private static final double DELTA = 0.00001;

    @Test
    public void testRefactoringCorrectness() {
        FormalizedOffer.FormalizedOfferBuilder formalizedOfferBuilder = FormalizedOffer.newBuilder();
        FormalizedOffer firstOffer = formalizedOfferBuilder
            .setTitle("offer1")
            .setStemmedTitleTokens(new int[]{1, 2, 3})
            .setStemmedDescrTokens(new int[]{2})
            .setOriginalTitleTokens(new int[]{2, 3, 4, 5})
            .setOriginalTitleTokens(new int[]{3, 4, 5})
            .build();
        FormalizedOffer secondOffer = formalizedOfferBuilder
            .setTitle("offer2")
            .setStemmedTitleTokens(new int[]{})
            .setStemmedDescrTokens(new int[]{2})
            .setOriginalTitleTokens(new int[]{7, 9})
            .setOriginalTitleTokens(new int[]{6, 4, 5})
            .build();
        PairToClassify pairToClassify = new PairToClassify(null, firstOffer, secondOffer, false);
        BagOfWordsFeaturesExtractor bagOfWordsFeaturesExtractor = new BagOfWordsFeaturesExtractor();

        float[] newResult = bagOfWordsFeaturesExtractor.calculateFeatures(pairToClassify);
        float[] oldResult = bagOfWordsFeaturesExtractor.calculateFeaturesOldWay(pairToClassify);
        for (int i = 0; i < newResult.length; i++) {
            assertEquals(newResult[i], oldResult[i], DELTA);
        }
    }

    @Test
    public void areCountCommonTokens() {
        assertEquals(0 / 1d, getCommonTokens(new int[]{}, new int[]{}), DELTA);
        assertEquals(1 / 2d, getCommonTokens(new int[]{1}, new int[]{1}), DELTA);
        assertEquals(4 / 5d, getCommonTokens(new int[]{1, 2}, new int[]{1, 2}), DELTA);
        assertEquals(9 / 10d, getCommonTokens(new int[]{1, 2, 3}, new int[]{1, 2, 3}), DELTA);

        assertEquals(0 / 1d, getCommonTokens(new int[]{1, 2, 3}, new int[]{}), DELTA);
        assertEquals(1 / 4d, getCommonTokens(new int[]{1, 2, 3}, new int[]{1}), DELTA);
        assertEquals(4 / 7d, getCommonTokens(new int[]{1, 2, 3}, new int[]{1, 2}), DELTA);

        assertEquals(0 / 1d, getCommonTokens(new int[]{1, 2, 3, 10}, new int[]{}), DELTA);
        assertEquals(1 / 5d, getCommonTokens(new int[]{1, 2, 3, 10}, new int[]{1}), DELTA);
        assertEquals(4 / 9d, getCommonTokens(new int[]{1, 2, 3, 10}, new int[]{1, 2}), DELTA);
        assertEquals(9 / 13d, getCommonTokens(new int[]{1, 2, 3, 10}, new int[]{1, 2, 3}), DELTA);

        assertEquals(0 / 4d, getCommonTokens(new int[]{1, 2, 3}, new int[]{20}), DELTA);
        assertEquals(1 / 7d, getCommonTokens(new int[]{1, 2, 3}, new int[]{1, 20}), DELTA);
        assertEquals(4 / 10d, getCommonTokens(new int[]{1, 2, 3}, new int[]{1, 2, 20}), DELTA);
        assertEquals(9 / 13d, getCommonTokens(new int[]{1, 2, 3}, new int[]{1, 2, 3, 20}), DELTA);

        assertEquals(0 / 5d, getCommonTokens(new int[]{1, 2, 3, 10}, new int[]{20}), DELTA);
        assertEquals(1 / 9d, getCommonTokens(new int[]{1, 2, 3, 10}, new int[]{1, 20}), DELTA);
        assertEquals(4 / 13d, getCommonTokens(new int[]{1, 2, 3, 10}, new int[]{1, 2, 20}), DELTA);
        assertEquals(9 / 17d, getCommonTokens(new int[]{1, 2, 3, 10}, new int[]{1, 2, 3, 20}), DELTA);
    }

    private float getCommonTokens(int[] first, int[] second) {
        return TokensSetFeatures.build(first, second).countNormalizedCommonTokes();
    }

}