package ru.yandex.market.markup2.utils;

import org.mockito.stubbing.Answer;
import ru.yandex.market.markup2.utils.model.ModelStorageService;
import ru.yandex.market.markup2.utils.param.ParamUtils;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static ru.yandex.market.markup2.utils.ParameterTestUtils.createLocalizedString;

/**
 * @author anmalysh
 */
public class ModelTestUtils {

    public static final long NEW_MODEL_ID_FIRST = 1000;

    private ModelTestUtils() {

    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public static ModelStorage.Model createModel(Integer categoryId, Long vendorId, Long id, String type,
                                                 String name, String site, Boolean published,
                                                 ModelStorage.ParameterValue... parameters) {
        ModelStorage.Model.Builder result = createModelBuilder(categoryId, vendorId, id, type, name, site, published);
        result.addAllParameterValues(Arrays.asList(parameters));
        return result.build();
    }

    public static ModelStorage.Model createModel(Integer categoryId, Long vendorId, Long id, String type,
                                                 Boolean published, ModelStorage.ParameterValue... parameters) {
        ModelStorage.Model.Builder result = createModelBuilder(categoryId, vendorId, id, type, published);
        result.addAllParameterValues(Arrays.asList(parameters));
        return result.build();
    }

    public static ModelStorage.Model createModelWithPictures(Integer categoryId, Long vendorId, Long id, String type,
                                                             Boolean published, ModelStorage.Picture... pictures) {
        ModelStorage.Model.Builder result = createModelBuilder(categoryId, vendorId, id, type, published);
        result.addAllPictures(Arrays.asList(pictures));
        return result.build();
    }

    public static ModelStorage.Model createModelWithPictures(Integer categoryId, Long vendorId, Long id, String type,
                                                             Boolean published, List<ModelStorage.Picture> pictures) {
        ModelStorage.Model.Builder result =
                ModelTestUtils.createModelBuilder(categoryId, vendorId, id, type, published);
        result.addAllPictures(pictures);
        return result.build();
    }

    public static ModelStorage.Model.Builder createModelBuilder(Integer categoryId, Long vendorId, Long id, String type,
                                                                Boolean published) {
        return createModelBuilder(categoryId, vendorId, id, type, "Model" + id, "http://url" + id, published);
    }

    public static ModelStorage.Model.Builder createModelBuilder(Integer categoryId, Long vendorId, Long id, String type,
                                                                String name, String site, Boolean published) {
        ModelStorage.Model.Builder result = ModelStorage.Model.newBuilder()
            .setId(id)
            .setCurrentType(type)
            .setCreatedDate(System.currentTimeMillis())
            .setModifiedTs(System.currentTimeMillis());
        if (categoryId != null) {
            result.setCategoryId(categoryId);
        }
        if (vendorId != null) {
            result.setVendorId(vendorId);
            result.addParameterValues(createParameterValue(
                ParamUtils.VENDOR_ID, MboParameters.ValueType.ENUM, "vendor").setOptionId(vendorId.intValue()));
        }
        if (name != null) {
            result.addTitles(ModelStorage.LocalizedString.newBuilder()
                .setIsoCode(Utils.RU_ISO_CODE)
                .setValue(name)
                .build());
            result.addParameterValues(createParameterValue(
                ParamUtils.NAME_ID, MboParameters.ValueType.STRING, "name").addStrValue(
                createLocalizedString(name)));
        }
        if (published != null) {
            result.setPublished(published);
        }
        if (site != null) {
            result.addParameterValues(createParameterValue(
                ParamUtils.URL_ID, MboParameters.ValueType.STRING, "url").addStrValue(
                createLocalizedString(site)));
        }
        return result;
    }

    public static ModelStorage.ParameterValue.Builder createParameterValue(Long id, MboParameters.ValueType type,
                                                                    String xslName) {
        return ModelStorage.ParameterValue.newBuilder()
            .setParamId(id)
            .setValueType(type)
            .setTypeId(type.ordinal())
            .setXslName(xslName);
    }

    public static ModelStorage.ParameterValue.Builder createParameterValue(MboParameters.Parameter parameter) {
        return createParameterValue(parameter.getId(), parameter.getValueType(), parameter.getXslName());
    }

    public static ModelStorage.ParameterValue.Builder createStringValue(MboParameters.Parameter param, String value) {
        return createParameterValue(param).addStrValue(createLocalizedString(value));
    }

    public static ModelStorage.ParameterValue.Builder createNumericValue(MboParameters.Parameter param, String value) {
        return createParameterValue(param).setNumericValue(value);
    }

    public static ModelStorage.ParameterValue.Builder createOptionValue(MboParameters.Parameter param, int optionId) {
        return createParameterValue(param).setOptionId(optionId);
    }

    public static ModelStorage.Picture.Builder createPicture(String xslName, String url,
                                                             ModelStorage.ParameterValue... parameters) {
        return ModelStorage.Picture.newBuilder()
            .setXslName(xslName)
            .setUrl(url)
            .addAllParameterValues(Arrays.asList(parameters));
    }

    public static String getStringValue(ModelStorage.Model model, Long paramId) {
        for (ModelStorage.ParameterValue value : model.getParameterValuesList()) {
            if (value.getParamId() == paramId) {
                return value.getStrValue(0).getValue();
            }
        }
        return null;
    }

    public static void mockModelStorageGetModel(ModelStorageService service, Collection<ModelStorage.Model> models) {
        when(service.getModel(anyLong(), anyLong())).thenAnswer(i -> {
            Long modelId = i.getArgument(1);
            return Optional.ofNullable(models.stream()
                .filter(m -> modelId.equals(m.getId()))
                .findFirst().orElse(null));
        });
    }

    public static void mockModelStorageGetModels(ModelStorageService service, Collection<ModelStorage.Model> models) {
        when(service.getModels(anyLong(), anyCollection())).thenAnswer(i -> {
            Set<Long> modelIds = new HashSet<>(i.getArgument(1));
            return models.stream()
                .filter(m -> modelIds.contains(m.getId()))
                .collect(Collectors.toList());
        });
    }

    public static void mockModelStorageSaveModels(ModelStorageService service, Set<Long> successIds) {
        when(service.saveModels(anyList())).thenAnswer(mockSaveModelsAnswer(successIds));
    }

    public static void mockModelStorageSaveModelsWithForce(ModelStorageService service, Set<Long> successIds) {
        when(service.saveModels(anyList(), anyBoolean())).thenAnswer(mockSaveModelsAnswer(successIds));
    }

    private static Answer<Object> mockSaveModelsAnswer(Set<Long> successIds) {
        AtomicLong newModelIdStart = new AtomicLong(NEW_MODEL_ID_FIRST);
        return i -> {
            List<ModelStorage.Model> models = i.getArgument(0);
            List<ModelStorage.OperationStatus> statuses = new ArrayList<>();
            for (ModelStorage.Model model : models) {
                ModelStorage.OperationStatus.Builder status = ModelStorage.OperationStatus.newBuilder()
                    .setModelId(model.getId())
                    .setType(model.getId() == 0 ? ModelStorage.OperationType.CREATE :
                            ModelStorage.OperationType.CHANGE);
                if (successIds.contains(model.getId())) {
                    if (!model.hasId() || model.getId() == 0) {
                        status.setModelId(newModelIdStart.getAndIncrement());
                    }
                    status.setStatus(ModelStorage.OperationStatusType.OK);
                } else {
                    status.setStatus(ModelStorage.OperationStatusType.INTERNAL_ERROR);
                    status.setFailureModelId(model.getId());
                }
                statuses.add(status.build());
            }
            return statuses;
        };
    }

    public static void mockModelStorageRemoveWithTransitionsModels(ModelStorageService service) {
        when(service.removeModelWithTransitions(anyLong(), anyLong(), any(), anyList(),
            anyBoolean(), anyBoolean(), anyBoolean())).thenAnswer(i -> {
            Long modelId = i.getArgument(0);
            ModelStorage.OperationStatus.Builder status = ModelStorage.OperationStatus.newBuilder()
                .setModelId(modelId)
                .setStatus(ModelStorage.OperationStatusType.OK)
                .setType(ModelStorage.OperationType.REMOVE);
            return status.build();
        });
    }

    public static void mockModelStorageProcessModels(ModelStorageService service,
                                                     Collection<ModelStorage.Model> models) {
        doAnswer(i -> {
            ModelStorage.ModelType type = i.getArgument(1);
            Consumer<ModelStorage.Model> consumer = i.getArgument(2);
            for (ModelStorage.Model model : models) {
                if (type.name().equals(model.getCurrentType())) {
                    consumer.accept(model);
                }
            }
            return null;
        }).when(service)
            .processModelsOfType(anyLong(), any(ModelStorage.ModelType.class), any(Consumer.class));
    }

    public static void mockModelStorageConditionalProcessModels(ModelStorageService service,
                                                                Collection<ModelStorage.Model> models) {
        doAnswer(i -> {
            ModelStorage.ModelType type = i.getArgument(1);
            Function<ModelStorage.Model, Boolean> function = i.getArgument(2);
            for (ModelStorage.Model model : models) {
                if (type.name().equals(model.getCurrentType())) {
                    if (!function.apply(model)) {
                        return null;
                    }
                }
            }
            return null;
        }).when(service)
            .processModelsOfType(anyLong(), any(ModelStorage.ModelType.class), any(Function.class));
    }

    public static ModelStorage.Model createDummyGuruModel(long id, boolean published,
                                                    String mainPicUrl,
                                                    ArrayList<String> otherPicUrls) {


        ModelStorage.ParameterValue mainPicParam = createMainPicParam(mainPicUrl);
        List<ModelStorage.ParameterValue> allPicUrls = createOtherPicParams(otherPicUrls);
        allPicUrls.add(mainPicParam);

        return ModelTestUtils.createModel(1, 1L, id,
                                          ModelStorage.ModelType.GURU.name(),
                                          published,
                                          allPicUrls.toArray(new ModelStorage.ParameterValue[otherPicUrls.size()]));
    }

    public static ModelStorage.ParameterValue createMainPicParam(String mainPicUrl) {
        ModelStorage.ParameterValue.Builder builder = ModelStorage.ParameterValue.newBuilder();
        builder.setXslName(ParamUtils.XL_PICTURE_XLS_NAME);
        builder.addStrValue(ParameterTestUtils.createLocalizedString(mainPicUrl));
        return builder.build();
    }


    public static List<ModelStorage.ParameterValue> createOtherPicParams(ArrayList<String> otherPicUrls) {
        ArrayList<ModelStorage.ParameterValue> result = new ArrayList<>();

        for (int i = 0; i < otherPicUrls.size(); i++) {
            ModelStorage.ParameterValue.Builder builder = ModelStorage.ParameterValue.newBuilder();
            builder.setXslName(ParamUtils.XL_PICTURE_XLS_NAME + "_" + (i + 1));
            builder.addStrValue(ParameterTestUtils.createLocalizedString(otherPicUrls.get(i)));
            result.add(builder.build());
        }

        return result;
    }

    public static ModelStorage.Model createDummyGuruModel(boolean published) {
        return ModelTestUtils.createModel(1, 1L, 1L,
                                          ModelStorage.ModelType.GURU.name(), published);
    }
}
