package ru.yandex.ir.modelsclusterizer.core.classify.features;

import org.junit.Test;
import ru.yandex.ir.modelsclusterizer.be.FormalizedOffer;

import static org.junit.Assert.*;

/**
 * @author mkrasnoperov 
 */
public class TitleMakerFeaturesExtractorTest {
    private static final double DELTA = 0.00001;

    @Test
    public void testRefactoringCorrectnes(){
        FormalizedOffer.FormalizedOfferBuilder formalizedOfferBuilder = FormalizedOffer.newBuilder();
        FormalizedOffer firstOffer = formalizedOfferBuilder
            .setTitleMakerModel("model1")
            .setTitleMakerModelTokens(new int[]{1, 2, 3})
            .setTitleMakerModelProbability(0.42f)
            .build();
        FormalizedOffer secondOffer = formalizedOfferBuilder
            .clear()
            .setTitleMakerModel("model2")
            .setTitleMakerModelTokens(new int[]{1, 3})
            .setTitleMakerModelProbability(0.44f)
            .build();
        PairToClassify pairToClassify = new PairToClassify(null, firstOffer, secondOffer, false);
        TitleMakerFeaturesExtractor titleMakerFeaturesExtractor = new TitleMakerFeaturesExtractor();

        float[] newResult = titleMakerFeaturesExtractor.calculateFeatures(pairToClassify);
        float[] oldResult = titleMakerFeaturesExtractor.calculateFeaturesOldWay(pairToClassify);
        for (int i = 0; i < newResult.length; i++) {
            assertEquals(newResult[i], oldResult[i], DELTA);
        }
    }

    @Test
    public void areCountCommonTokens() {
        assertEquals(0, getCommonTokens(new int[]{}, new int[]{}), DELTA);
        assertEquals(1, getCommonTokens(new int[]{1}, new int[]{1}), DELTA);
        assertEquals(2, getCommonTokens(new int[]{1, 2}, new int[]{1, 2}), DELTA);
        assertEquals(3, getCommonTokens(new int[]{1, 2, 3}, new int[]{1, 2, 3}), DELTA);

        assertEquals(0, getCommonTokens(new int[]{1, 2, 3}, new int[]{}), DELTA);
        assertEquals(1, getCommonTokens(new int[]{1, 2, 3}, new int[]{1}), DELTA);
        assertEquals(2, getCommonTokens(new int[]{1, 2, 3}, new int[]{1, 2}), DELTA);
        assertEquals(3, getCommonTokens(new int[]{1, 2, 3}, new int[]{1, 2, 3}), DELTA);

        assertEquals(0, getCommonTokens(new int[]{1, 2, 3, 10}, new int[]{}), DELTA);
        assertEquals(1, getCommonTokens(new int[]{1, 2, 3, 10}, new int[]{1}), DELTA);
        assertEquals(2, getCommonTokens(new int[]{1, 2, 3, 10}, new int[]{1, 2}), DELTA);
        assertEquals(3, getCommonTokens(new int[]{1, 2, 3, 10}, new int[]{1, 2, 3}), DELTA);

        assertEquals(0, getCommonTokens(new int[]{1, 2, 3}, new int[]{20}), DELTA);
        assertEquals(1, getCommonTokens(new int[]{1, 2, 3}, new int[]{1, 20}), DELTA);
        assertEquals(2, getCommonTokens(new int[]{1, 2, 3}, new int[]{1, 2, 20}), DELTA);
        assertEquals(3, getCommonTokens(new int[]{1, 2, 3}, new int[]{1, 2, 3, 20}), DELTA);

        assertEquals(0, getCommonTokens(new int[]{1, 2, 3, 10}, new int[]{20}), DELTA);
        assertEquals(1, getCommonTokens(new int[]{1, 2, 3, 10}, new int[]{1, 20}), DELTA);
        assertEquals(2, getCommonTokens(new int[]{1, 2, 3, 10}, new int[]{1, 2, 20}), DELTA);
        assertEquals(3, getCommonTokens(new int[]{1, 2, 3, 10}, new int[]{1, 2, 3, 20}), DELTA);
    }
    
    private float getCommonTokens(int[] first, int[] second) {
        return TokensSetFeatures.build(first, second).getCommonTokensNumber();
    }


    @Test
    public void areArticlesEqual() {
        assertEquals(0, TitleMakerFeaturesExtractor.areStringsEqual(null, null), DELTA);
        assertEquals(0, TitleMakerFeaturesExtractor.areStringsEqual(null, ""), DELTA);
        assertEquals(0, TitleMakerFeaturesExtractor.areStringsEqual(null, "AB-120"), DELTA);
        assertEquals(0, TitleMakerFeaturesExtractor.areStringsEqual("", null), DELTA);
        assertEquals(0, TitleMakerFeaturesExtractor.areStringsEqual("", ""), DELTA);
        assertEquals(0, TitleMakerFeaturesExtractor.areStringsEqual("", "AB-120"), DELTA);
        assertEquals(0, TitleMakerFeaturesExtractor.areStringsEqual("AB-9999", "AB-120"), DELTA);
        assertEquals(1, TitleMakerFeaturesExtractor.areStringsEqual("AB-120", "AB-120"), DELTA);
    }

}