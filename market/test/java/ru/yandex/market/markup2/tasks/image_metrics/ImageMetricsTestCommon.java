package ru.yandex.market.markup2.tasks.image_metrics;

import com.google.common.collect.Lists;
import org.mockito.Mockito;
import ru.yandex.market.markup2.utils.ModelTestUtils;
import ru.yandex.market.markup2.utils.model.ModelStorageService;
import ru.yandex.market.markup2.utils.param.ParamUtils;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * @author inenakhov
 */
public class ImageMetricsTestCommon {

    private ImageMetricsTestCommon() {

    }

    public static List<ModelStorage.Model> generateDummyMboModels(int modelsCount, int imagePerModel) {
        String baseUrl = "http://main_";
        String otherUrl = "http://other_";
        ArrayList<ModelStorage.Model> models = Lists.newArrayList();
        for (int id = 0; id < modelsCount; id++) {
            ArrayList<String> otherImageUrls = Lists.newArrayList();
            for (int index = 0; index < imagePerModel - 1; index++) {
                otherImageUrls.add(createOtherUrl(otherUrl, id, index));
            }
            models.add(ModelTestUtils.createDummyGuruModel(id, true,
                                                           baseUrl + id,
                                                           otherImageUrls));
        }

        return models;
    }

    private static String createOtherUrl(String otherUrl, int id, int index) {
        return otherUrl + id + "_" + index;
    }

    public static boolean isMainPictureForModel(String url, ModelStorage.Model model) {
        return url.equals(ParamUtils.getStringParamValue(ParamUtils.XL_PICTURE_XLS_NAME,
                                                         model.getParameterValuesList()));
    }

    public static boolean isPictureInOtherModelPictures(String url, ModelStorage.Model model) {
        return ParamUtils.getStringParamValues(ParamUtils.NOT_MAIN_PICTURES_PATTERN, model.getParameterValuesList())
            .contains(url);
    }

    public static boolean isModelPicture(String url, ModelStorage.Model model) {
        return ParamUtils.getStringParamValues(ParamUtils.XL_PICTURE_PATTERN, model.getParameterValuesList())
            .contains(url);
    }

    public static void setUpModelStorageMock(List<ModelStorage.Model> genModels,
                                             ModelStorageService modelStorageService) {
        Map<Long, ModelStorage.Model> idToModel = genModels.stream()
            .collect(Collectors.toMap(ModelStorage.Model::getId, model -> model));

        doAnswer(i -> {
            Consumer<ModelStorage.Model> argument = i.getArgument(2);
            genModels.forEach(argument);

            return null;
        }).when(modelStorageService).processModelsOfType(Mockito.anyLong(),
                                                         Mockito.any(),
                                                         Mockito.any(Consumer.class));

        when(modelStorageService.getGURU(anyInt(), anyCollection())).thenAnswer(i -> {
            Set<Long> argument = i.getArgument(1);
            return argument.stream().map(idToModel::get).collect(Collectors.toList());
        });
    }
}
