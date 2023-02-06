package ru.yandex.market.markup2.tasks.image_metrics.relevance;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.markup2.entries.group.PublishingValue;
import ru.yandex.market.markup2.entries.group.TaskConfigGroupInfo;
import ru.yandex.market.markup2.tasks.image_metrics.ImageMetricsTestCommon;
import ru.yandex.market.markup2.tasks.image_metrics.RequestContextMock;
import ru.yandex.market.markup2.utils.model.ModelStorageService;
import ru.yandex.market.markup2.utils.param.ParamUtils;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * @author inenakhov
 */
@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ImageRelevanceRequestGeneratorTest {

    @Mock
    private ModelStorageService modelStorageService;

    @Mock
    private ParamUtils paramUtils;

    @Mock
    private TaskConfigGroupInfo groupInfo;

    @Before
    public void setUp() throws Exception {
        when(paramUtils.getAllParams(anyInt())).thenAnswer(i -> new ArrayList<MboParameters.Parameter>());

        when(groupInfo.getParameterValueOrDefault(any(), any()))
            .thenAnswer(i -> PublishingValue.PUBLISHED);
    }

    @Test
    public void generateRequests() throws Exception {
        int modelsInCategory = 1000;
        int picturesPerModel = 4;
        int tasksToGenerate = 200;

        List<ModelStorage.Model> genModels =
            ImageMetricsTestCommon.generateDummyMboModels(modelsInCategory, picturesPerModel);

        ImageRelevanceRequestGenerator generator = new ImageRelevanceRequestGenerator();
        generator.setModelStorageService(modelStorageService);
        generator.setParamUtils(paramUtils);

        ImageMetricsTestCommon.setUpModelStorageMock(genModels, modelStorageService);
        RequestContextMock<ImageRelevanceTaskIdentity,
                    ImageRelevanceTaskPayload, ImageRelevanceHitmanResponse> ctx =
            RequestContextMock.create(tasksToGenerate, groupInfo);

        generator.generateRequests(ctx);
        assertEquals(tasksToGenerate, ctx.getPayloads().size());

        checkGeneratedTaskUrlModelConsistency(genModels, ctx);
    }

    @Test
    public void generateRequestsLowCardsInCategory() throws Exception {
        int modelsInCategory = 200;
        int picturesPerModel = 4;
        int tasksToGenerate = 1000;
        List<ModelStorage.Model> genModels =
            ImageMetricsTestCommon.generateDummyMboModels(modelsInCategory, picturesPerModel);

        ImageMetricsTestCommon.setUpModelStorageMock(genModels, modelStorageService);

        ImageRelevanceRequestGenerator generator = new ImageRelevanceRequestGenerator();
        generator.setModelStorageService(modelStorageService);
        generator.setParamUtils(paramUtils);

        RequestContextMock<ImageRelevanceTaskIdentity,
            ImageRelevanceTaskPayload, ImageRelevanceHitmanResponse> ctx =
            RequestContextMock.create(tasksToGenerate, groupInfo);

        generator.generateRequests(ctx);
        assertEquals(modelsInCategory * picturesPerModel, ctx.getPayloads().size());

        checkGeneratedTaskUrlModelConsistency(genModels, ctx);
    }

    @Test
    public void generateRequestsNotEmptyContext() throws Exception {
        int modelsInCategory = 200;
        int picturesPerModel = 4;
        int tasksToGenerate = 1000;
        int alreadyGeneratedCount = 100;
        List<ModelStorage.Model> genModels =
            ImageMetricsTestCommon.generateDummyMboModels(modelsInCategory, picturesPerModel);

        ImageMetricsTestCommon.setUpModelStorageMock(genModels, modelStorageService);

        ImageRelevanceRequestGenerator generator = new ImageRelevanceRequestGenerator();
        generator.setModelStorageService(modelStorageService);
        generator.setParamUtils(paramUtils);

        RequestContextMock<ImageRelevanceTaskIdentity,
            ImageRelevanceTaskPayload, ImageRelevanceHitmanResponse> ctx =
            RequestContextMock.create(tasksToGenerate, groupInfo,
                                      createDataItems(genModels.subList(0, alreadyGeneratedCount)));

        generator.generateRequests(ctx);
        assertEquals(modelsInCategory * picturesPerModel, ctx.getPayloads().size());

        checkGeneratedTaskUrlModelConsistency(genModels, ctx);
        assertTrue(ctx.getPayloads().size() == new HashSet<>(ctx.getPayloads()).size());
    }

    private List<ImageRelevanceTaskPayload> createDataItems(List<ModelStorage.Model> models) {
        ArrayList<ImageRelevanceTaskPayload> result = new ArrayList<>();
        models.forEach(model -> {

            String modelUrl = ParamUtils.getUrl(model);
            String modelTitle = ParamUtils.getName(model);
            String description = "description";

            ParamUtils.getStringParamValues(ParamUtils.XL_PICTURE_PATTERN, model.getParameterValuesList())
                .forEach(url -> {
                    ImageRelevanceTaskIdentity imageRelevanceTaskIdentity = new ImageRelevanceTaskIdentity(url);

                    ImageRelevanceDataAttributes imageRelevanceDataAttributes =
                        new ImageRelevanceDataAttributes(model.getId(), modelUrl, modelTitle, description);
                    ImageRelevanceTaskPayload imageRelevanceTaskPayload =
                        new ImageRelevanceTaskPayload(imageRelevanceTaskIdentity, imageRelevanceDataAttributes);

                    result.add(imageRelevanceTaskPayload);
            });
        });

        return result;
    }

    private void checkGeneratedTaskUrlModelConsistency(List<ModelStorage.Model> models,
                                                       RequestContextMock<ImageRelevanceTaskIdentity,
                                                           ImageRelevanceTaskPayload,
                                                               ImageRelevanceHitmanResponse> ctx) {
        Map<Long, ModelStorage.Model> idToModel = models.stream().collect(Collectors.toMap(ModelStorage.Model::getId,
                                                                                           model -> model));
        for (ImageRelevanceTaskPayload payload : ctx.getPayloads()) {
            long modelId = payload.getAttributes().getModelId();
            ModelStorage.Model model = idToModel.get(modelId);
            String imageUrl = payload.getDataIdentifier().getImageUrl();

            assertTrue(ImageMetricsTestCommon.isModelPicture(imageUrl, model));
        }
    }
}
