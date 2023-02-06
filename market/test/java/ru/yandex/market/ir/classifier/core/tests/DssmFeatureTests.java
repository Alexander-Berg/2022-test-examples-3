package ru.yandex.market.ir.classifier.core.tests;

import org.junit.Test;

import ru.yandex.market.ir.classifier.dssm.DssmFeature;

import static org.junit.Assert.assertEquals;

public class DssmFeatureTests {
    @Test(expected = IllegalArgumentException.class) // that's why we need fromCode() method
    public void dssmFeature_valueOf_throwsIllegalArgumentException_forKnownCode() {
        DssmFeature.valueOf(DssmFeature.Title_To_TitleCenterByCategory.getCodes()[0]);
    }

    @Test
    public void dssmFeature_fromCode_shouldReturnValidFeature_forEveryKnownCode() {
        for (var feature : DssmFeature.values()) {
            var featureCode = feature.getCodes()[0];
            assertEquals(featureCode, DssmFeature.fromCode(featureCode).getCodes()[0]);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void dssmFeature_fromCode_shouldThrowIllegalArgumentException_forUnknownCode() {
        DssmFeature.fromCode("unknown");
    }
}
