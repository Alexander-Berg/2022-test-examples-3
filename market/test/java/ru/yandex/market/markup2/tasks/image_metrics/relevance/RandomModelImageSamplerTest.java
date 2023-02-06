package ru.yandex.market.markup2.tasks.image_metrics.relevance;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.markup2.entries.group.PublishingValue;
import ru.yandex.market.markup2.tasks.image_metrics.ImageMetricsTestCommon;
import ru.yandex.market.markup2.utils.model.ModelStorageService;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;

/**
 * @author inenakhov
 */
@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class RandomModelImageSamplerTest {
    @Mock
    private ModelStorageService modelStorageService;

    private int categoryId = 1;

    @Test
    public void sampleImageLowModelsCount() throws Exception {
        int modelsCount = 50;
        int picturesPerModel = 4;
        int sampleSize = 500;

        List<ModelStorage.Model> genModels =
            ImageMetricsTestCommon.generateDummyMboModels(modelsCount, picturesPerModel);
        ImageMetricsTestCommon.setUpModelStorageMock(genModels, modelStorageService);

        RandomModelImageSampler randomModelImageSampler = new RandomModelImageSampler(modelStorageService,
                                                                                      categoryId,
                                                                                      PublishingValue.ALL);
        Map<String, ModelStorage.Model> firstImageSample = randomModelImageSampler.sampleImage(sampleSize);

        assertEquals(modelsCount * picturesPerModel, firstImageSample.size());
    }

    @Test
    public void sampleImage() throws Exception {
        int modelsCount = 500;
        int picturesPerModel = 4;
        int sampleSize = 500;

        List<ModelStorage.Model> genModels =
            ImageMetricsTestCommon.generateDummyMboModels(modelsCount, picturesPerModel);
        ImageMetricsTestCommon.setUpModelStorageMock(genModels, modelStorageService);

        RandomModelImageSampler randomModelImageSampler = new RandomModelImageSampler(modelStorageService,
                                                                                      categoryId,
                                                                                      PublishingValue.ALL);
        Map<String, ModelStorage.Model> firstImageSample = randomModelImageSampler.sampleImage(sampleSize);

        assertEquals(sampleSize, firstImageSample.size());
        Map<String, ModelStorage.Model> secondImageSample = randomModelImageSampler.sampleImage(sampleSize);

        int diffCounter = 0;
        for (String imageUrl : firstImageSample.keySet()) {
            if (!secondImageSample.containsKey(imageUrl)) {
                diffCounter += 1;
            }
        }
        assertNotSame(diffCounter, 0);
    }
}
