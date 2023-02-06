package ru.yandex.ir.common.features.extractors.image;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImageFeaturesV6ExtractorTest {

    @Test
    void calcLogRegPairwiseSum() {
        float weights[] = ImageFeaturesExtractorWeightsLoadingUtil.readWeights(ImageFeaturesExtractorWeightsLoadingUtil.V6_WEIGHTS_FILE);
        float result = -36.966885f;
        float first[] = new float[96];
        Arrays.fill(first,1);
        float second[] = new float[96];
        Arrays.fill(second,2);
        float logRegProduct = ImageFeaturesExtractorHelper.calcLogRegPairwiseMultSum(first, second, weights);
        assertEquals(logRegProduct, result);
    }
}
