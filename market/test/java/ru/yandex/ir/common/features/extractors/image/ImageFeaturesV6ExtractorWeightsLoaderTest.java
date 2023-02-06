package ru.yandex.ir.common.features.extractors.image;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImageFeaturesV6ExtractorWeightsLoaderTest {

    @Test
    void getWeights() {
        float weights[] = ImageFeaturesExtractorWeightsLoadingUtil.readWeights(ImageFeaturesExtractorWeightsLoadingUtil.V6_WEIGHTS_FILE);
        assertEquals(weights.length, 96*96+1);
    }

}
