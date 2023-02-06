package ru.yandex.ir.modelsclusterizer.core.classify.features;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author mkrasnoperov 
 */
public class ArticleFeaturesExtractorTest {
    private static final double DELTA = 0.00001;

    @Test
    public void getCommonTokensFraction() throws Exception {
        assertEquals(0, ArticleFeaturesExtractor.getCommonTokensFraction(null, null), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.getCommonTokensFraction(null, new int[]{}), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.getCommonTokensFraction(null, new int[]{1, 2}), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.getCommonTokensFraction(new int[]{}, null), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.getCommonTokensFraction(new int[]{}, new int[]{}), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.getCommonTokensFraction(new int[]{}, new int[]{1, 2}), DELTA);
        assertEquals(1, ArticleFeaturesExtractor.getCommonTokensFraction(new int[]{1}, new int[]{1, 2}), DELTA);
        assertEquals(1, ArticleFeaturesExtractor.getCommonTokensFraction(new int[]{1, 5}, new int[]{1}), DELTA);
        assertEquals(0.5, ArticleFeaturesExtractor.getCommonTokensFraction(new int[]{1, 5}, new int[]{1, 2}), DELTA);
        assertEquals(1, ArticleFeaturesExtractor.getCommonTokensFraction(new int[]{1, 2}, new int[]{1, 2}), DELTA);

        assertEquals(1, ArticleFeaturesExtractor.getCommonTokensFraction(new int[]{1, 2}, new int[]{1, 2, 3}), DELTA);
        assertEquals(1, ArticleFeaturesExtractor.getCommonTokensFraction(new int[]{1, 2}, new int[]{1, 2, 3}), DELTA);
        assertEquals(2d / 3, ArticleFeaturesExtractor.getCommonTokensFraction(new int[]{1, 2, 4}, new int[]{1, 2, 3}), DELTA);
        assertEquals(3d / 4, ArticleFeaturesExtractor.getCommonTokensFraction(new int[]{1, 2, 11, 12}, new int[]{1, 2, 11, 13}), DELTA);
        assertEquals(3d / 4, ArticleFeaturesExtractor.getCommonTokensFraction(new int[]{1, 2, 11, 12}, new int[]{1, 2, 11, 13, 14}), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.getCommonTokensFraction(new int[]{10, 1, 2}, new int[]{11, 1, 2}), DELTA);
    }


    @Test
    public void getCommonPrefixLength() {
        assertEquals(0, ArticleFeaturesExtractor.getCommonPrefixLength(null, null), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.getCommonPrefixLength(null, ""), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.getCommonPrefixLength(null, "AB-120"), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.getCommonPrefixLength("", null), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.getCommonPrefixLength("", ""), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.getCommonPrefixLength("", "AB-120"), DELTA);
        assertEquals(2d / 6, ArticleFeaturesExtractor.getCommonPrefixLength("AB", "AB-120"), DELTA);
        assertEquals(2d / 7, ArticleFeaturesExtractor.getCommonPrefixLength("AB-9999", "AB"), DELTA);
        assertEquals(3d / 7, ArticleFeaturesExtractor.getCommonPrefixLength("AB-9999", "AB-120"), DELTA);
        assertEquals(1.0, ArticleFeaturesExtractor.getCommonPrefixLength("AB-120", "AB-120"), DELTA);
    }


    @Test
    public void isOnlyPrefixOfAnother() {
        assertEquals(0, ArticleFeaturesExtractor.isOnlyPrefixOfAnother(null, null), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.isOnlyPrefixOfAnother(null, ""), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.isOnlyPrefixOfAnother(null, "AB-120"), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.isOnlyPrefixOfAnother("", null), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.isOnlyPrefixOfAnother("", ""), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.isOnlyPrefixOfAnother("", "AB-120"), DELTA);
        assertEquals(1, ArticleFeaturesExtractor.isOnlyPrefixOfAnother("AB", "AB-120"), DELTA);
        assertEquals(1, ArticleFeaturesExtractor.isOnlyPrefixOfAnother("AB-9999", "AB"), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.isOnlyPrefixOfAnother("AB-9999", "AB-120"), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.isOnlyPrefixOfAnother("AB-120", "AB-120"), DELTA);
    }
    
    @Test
    public void areBothArticlesEmpty() {
        assertEquals(1, ArticleFeaturesExtractor.areBothStringsEmpty(null, null), DELTA);
        assertEquals(1, ArticleFeaturesExtractor.areBothStringsEmpty(null, ""), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.areBothStringsEmpty(null, "AB-120"), DELTA);
        assertEquals(1, ArticleFeaturesExtractor.areBothStringsEmpty("", null), DELTA);
        assertEquals(1, ArticleFeaturesExtractor.areBothStringsEmpty("", ""), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.areBothStringsEmpty("", "AB-120"), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.areStringsEqual("AB-9999", "AB-120"), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.areBothStringsEmpty("AB-120", "AB-120"), DELTA);
    }


    @Test
    public void areArticlesEqual() {
        assertEquals(0, ArticleFeaturesExtractor.areStringsEqual(null, null), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.areStringsEqual(null, ""), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.areStringsEqual(null, "AB-120"), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.areStringsEqual("", null), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.areStringsEqual("", ""), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.areStringsEqual("", "AB-120"), DELTA);
        assertEquals(0, ArticleFeaturesExtractor.areStringsEqual("AB-9999", "AB-120"), DELTA);
        assertEquals(1, ArticleFeaturesExtractor.areStringsEqual("AB-120", "AB-120"), DELTA);
    }
}