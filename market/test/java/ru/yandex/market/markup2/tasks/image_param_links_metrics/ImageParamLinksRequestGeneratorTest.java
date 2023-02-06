package ru.yandex.market.markup2.tasks.image_param_links_metrics;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.markup2.entries.group.MetricType;
import ru.yandex.market.markup2.entries.group.TaskConfigGroupInfo;
import ru.yandex.market.markup2.tasks.image_metrics.RequestContextMock;
import ru.yandex.market.markup2.utils.ModelTestUtils;
import ru.yandex.market.markup2.utils.ParameterTestUtils;
import ru.yandex.market.markup2.utils.model.ModelStorageService;
import ru.yandex.market.markup2.utils.param.ParamUtils;
import ru.yandex.market.markup2.utils.tovarTree.TovarTreeProvider;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ImageParamLinksRequestGeneratorTest {
    private static ModelStorage.ParameterValue customParamValue =
            createEnumParameterValue(1L, "vendor_color", 1).build();
    private static MboParameters.Parameter customMboParamValue = ParameterTestUtils.createParameterBuilder(1L,
        MboParameters.ValueType.ENUM, "vendor_color").build();

    @Mock
    private ModelStorageService modelStorageService;

    @Mock
    private ParamUtils paramUtils;

    @Mock
    private TovarTreeProvider tovarTreeProvider;

    @Mock
    private TaskConfigGroupInfo groupInfo;

    private final String categoryName = "Название категории";

    @Before
    public void setUp() throws Exception {
        when(tovarTreeProvider.getCategoryName(anyInt())).thenAnswer(i -> categoryName);

        when(paramUtils.getAllParams(anyInt())).thenAnswer(i -> new ArrayList<>(Arrays.asList(customMboParamValue)));

        when(groupInfo.getParameterValueOrDefault(any(), any()))
            .thenAnswer(i -> MetricType.ACCEPTANCE);
    }

    @Test
    public void generateRequests() throws Exception {
        int modelsInCategory = 500;
        int linksPerModel = 2;
        int tasksToGenerate = 100;

        List<ModelStorage.Model> genModels = generateDummyMboModelsWithPictures(modelsInCategory, linksPerModel);

        ImageParamLinksRequestGenerator generator = new ImageParamLinksRequestGenerator();
        generator.setModelStorageService(modelStorageService);
        generator.setParamUtils(paramUtils);
        generator.setTovarTreeProvider(tovarTreeProvider);

        setUpModelStorageMock(genModels, modelStorageService);

        RequestContextMock<ImageParamLinksTaskIdentity, ImageParamLinksTaskPayload,
            ImageParamLinksHitmanResponse> ctx =  RequestContextMock.create(tasksToGenerate, groupInfo);

        generator.generateRequests(ctx);
        assertEquals(tasksToGenerate, ctx.getPayloads().size());
    }

    public static void setUpModelStorageMock(List<ModelStorage.Model> genModels,
                                             ModelStorageService modelStorageService) {
        doAnswer(i -> {
            Consumer<ModelStorage.Model> argument = i.getArgument(2);
            genModels.forEach(argument);

            return null;
        }).when(modelStorageService).processModelsOfType(Mockito.anyLong(), any(), any(Consumer.class));

        when(modelStorageService.getModels(anyLong(), anyCollection())).thenAnswer(i -> {
            Set<Long> modelIds = new HashSet<>(i.getArgument(1));
            return genModels.stream()
                .filter(m -> modelIds.contains(m.getId()))
                .collect(Collectors.toList());
        });
    }

    public static List<ModelStorage.Model> generateDummyMboModelsWithPictures(int modelsCount, int linksPerModel) {
        ArrayList<ModelStorage.Model> models = Lists.newArrayList();
        for (int id = 0; id < modelsCount; id++) {
            ArrayList<ModelStorage.Picture> modelPictures = Lists.newArrayList();
            for (int index = 0; index < linksPerModel; index++) {
                modelPictures.add(
                    ModelTestUtils.createPicture(
                        ParamUtils.XL_PICTURE_XLS_NAME + "_" + Integer.toString(index + 1),
                        "http://url_" + Integer.toString(index + 1),
                        customParamValue
                    ).build()
                );
            }
            models.add(ModelTestUtils.createModelWithPictures(1, 2L, (long) id, "GURU", true, modelPictures));
        }
        return models;
    }

    public static ModelStorage.ParameterValue.Builder createEnumParameterValue(Long id, String xslName, int optionId) {
        return ModelTestUtils.createParameterValue(id, MboParameters.ValueType.ENUM, xslName).setOptionId(optionId);
    }

}
