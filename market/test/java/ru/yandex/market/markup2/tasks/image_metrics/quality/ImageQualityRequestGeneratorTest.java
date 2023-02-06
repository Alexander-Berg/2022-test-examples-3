package ru.yandex.market.markup2.tasks.image_metrics.quality;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.markup2.entries.group.PublishingValue;
import ru.yandex.market.markup2.entries.group.TaskConfigGroupInfo;
import ru.yandex.market.markup2.tasks.image_metrics.ImageMetricsTestCommon;
import ru.yandex.market.markup2.tasks.image_metrics.RequestContextMock;
import ru.yandex.market.markup2.utils.StatUtils;
import ru.yandex.market.markup2.utils.model.ModelStorageService;
import ru.yandex.market.markup2.utils.param.ParamUtils;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author inenakhov
 */
@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ImageQualityRequestGeneratorTest {

    @Mock
    private ModelStorageService modelStorageService;

    @Mock
    private TaskConfigGroupInfo groupInfo;

    @Before
    public void setUp() throws Exception {
        when(groupInfo.getParameterValueOrDefault(any(), any())).thenAnswer(i -> PublishingValue.PUBLISHED);
    }

    @Test
    public void generateRequestsLowTaskGenerateCount() throws Exception {
        int modelsInCategory = 1000;
        int picturesPerModel = 4;
        int tasksToGenerate = 500;

        List<ModelStorage.Model> genModels = ImageMetricsTestCommon.generateDummyMboModels(modelsInCategory,
                                                                                           picturesPerModel);

        ImageMetricsTestCommon.setUpModelStorageMock(genModels, modelStorageService);

        RequestContextMock<ImageQualityTaskIdentity, ImageQualityTaskPayload, ImageQualityHitmanResponse> ctx =
            RequestContextMock.create(tasksToGenerate, groupInfo);

        ImageQualityRequestGenerator generator = new ImageQualityRequestGenerator();
        generator.setModelStorageService(modelStorageService);

        generator.generateRequests(ctx);

        assertEquals(tasksToGenerate, ctx.getPayloads().size());
        long count = ctx.getPayloads().stream().filter(p -> p.getAttributes().isMain()).count();
        assertEquals(tasksToGenerate, count);

        checkGeneratedTaskUrlModelConsistency(genModels, ctx);
    }

    @Test
    public void generateRequests() throws Exception {
        int modelsInCategory = 300;
        int picturesPerModel = 4;
        int tasksToGenerate = 1000;

        List<ModelStorage.Model> genModels = ImageMetricsTestCommon.generateDummyMboModels(modelsInCategory,
                                                                                           picturesPerModel);
        ImageMetricsTestCommon.setUpModelStorageMock(genModels, modelStorageService);

        RequestContextMock<ImageQualityTaskIdentity, ImageQualityTaskPayload, ImageQualityHitmanResponse> ctx =
            RequestContextMock.create(tasksToGenerate, groupInfo);

        ImageQualityRequestGenerator generator = new ImageQualityRequestGenerator();
        generator.setModelStorageService(modelStorageService);

        generator.generateRequests(ctx);

        assertEquals(tasksToGenerate, ctx.getPayloads().size());
        long mainPicturesCount = ctx.getPayloads().stream().filter(p -> p.getAttributes().isMain()).count();
        assertEquals(StatUtils.sampleSize(modelsInCategory,
                                          ImageQualityRequestGenerator.P,
                                          ImageQualityRequestGenerator.EPSILON,
                                          ImageQualityRequestGenerator.Z_ALPHA), mainPicturesCount);

        checkGeneratedTaskUrlModelConsistency(genModels, ctx);
    }

    @Test
    public void generateRequestsContextIsFullOfMainCards() throws Exception {
        int modelsInCategory = 300;
        int picturesPerModel = 4;
        int tasksToGenerate = 1000;
        int taskWithMainPicAlreadyStored = 232;

        List<ModelStorage.Model> genModels = ImageMetricsTestCommon.generateDummyMboModels(modelsInCategory,
                                                                                           picturesPerModel);
        ImageMetricsTestCommon.setUpModelStorageMock(genModels, modelStorageService);

        RequestContextMock<ImageQualityTaskIdentity, ImageQualityTaskPayload, ImageQualityHitmanResponse> ctx =
            RequestContextMock.create(tasksToGenerate, groupInfo,
                                      createMainPicDataItems(genModels).subList(0, taskWithMainPicAlreadyStored));

        ImageQualityRequestGenerator generator = new ImageQualityRequestGenerator();
        generator.setModelStorageService(modelStorageService);

        generator.generateRequests(ctx);

        assertEquals(tasksToGenerate, ctx.getPayloads().size());
        long mainPicturesCount = ctx.getPayloads().stream().filter(p -> p.getAttributes().isMain()).count();
        assertEquals(StatUtils.sampleSize(modelsInCategory,
                                          ImageQualityRequestGenerator.P,
                                          ImageQualityRequestGenerator.EPSILON,
                                          ImageQualityRequestGenerator.Z_ALPHA), mainPicturesCount);

        checkGeneratedTaskUrlModelConsistency(genModels, ctx);
    }

    @Test
    public void generateRequestsFewCardsInCategory() throws Exception {
        int modelsInCategory = 150;
        int picturesPerModel = 4;
        int tasksToGenerate = 1000;

        List<ModelStorage.Model> genModels = ImageMetricsTestCommon.generateDummyMboModels(modelsInCategory,
                                                                                           picturesPerModel);
        ImageMetricsTestCommon.setUpModelStorageMock(genModels, modelStorageService);

        RequestContextMock<ImageQualityTaskIdentity, ImageQualityTaskPayload, ImageQualityHitmanResponse> ctx =
            RequestContextMock.create(tasksToGenerate, groupInfo);

        ImageQualityRequestGenerator generator = new ImageQualityRequestGenerator();
        generator.setModelStorageService(modelStorageService);

        generator.generateRequests(ctx);

        assertEquals(modelsInCategory * picturesPerModel, ctx.getPayloads().size());
        long mainPicturesCount = ctx.getPayloads().stream().filter(p -> p.getAttributes().isMain()).count();
        assertEquals(modelsInCategory, mainPicturesCount);

        checkGeneratedTaskUrlModelConsistency(genModels, ctx);
    }

    private List<ImageQualityTaskPayload> createMainPicDataItems(List<ModelStorage.Model> models) {
        ArrayList<ImageQualityTaskPayload> result = new ArrayList<>();
        models.forEach(model -> {
            String mainPicUrl = ParamUtils.getStringParamValue(ParamUtils.XL_PICTURE_XLS_NAME,
                                                               model.getParameterValuesList());

            result.add(new ImageQualityTaskPayload(new ImageQualityTaskIdentity(mainPicUrl),
                                                   new ImageQualityDataAttributes(model.getId(), true)));

        });

        return result;
    }

    private void checkGeneratedTaskUrlModelConsistency(List<ModelStorage.Model> models,
                                                       RequestContextMock<ImageQualityTaskIdentity,
                                                           ImageQualityTaskPayload, ImageQualityHitmanResponse> ctx) {
        Map<Long, ModelStorage.Model> idToModel = models.stream().collect(Collectors.toMap(ModelStorage.Model::getId,
                                                                                           model -> model));
        for (ImageQualityTaskPayload payload : ctx.getPayloads()) {
            long modelId = payload.getAttributes().getModelId();
            boolean isMain = payload.getAttributes().isMain();
            ModelStorage.Model model = idToModel.get(modelId);
            String imageUrl = payload.getDataIdentifier().getImageUrl();

            if (isMain) {
                assertTrue(ImageMetricsTestCommon.isMainPictureForModel(imageUrl, model));
            } else {
                assertTrue(ImageMetricsTestCommon.isPictureInOtherModelPictures(imageUrl, model));
            }
        }
    }
}
