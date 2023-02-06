package ru.yandex.market.ir.nirvana.modelpublisher;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorageService;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author inenakhov
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ModelStorageHelperTest {
    public static final String RU_ISO_CODE = "ru";
    public static final long VENDOR_ID = 7893318L;
    public static final long NAME_ID = 7351771L;
    public static final long URL_ID = 7351726L;
    public static final String GURU = "GURU";

    @Mock
    private ModelStorageService modelStorageService;

    private Map<Long, ModelStorage.Model> models = new HashMap<>();


    @Before
    public void setUp() throws Exception {
        models.put(1L, createModelBuilder(1, 1L, 1L, GURU, true).build());
        models.put(2L, createModelBuilder(1, 1L, 2L, GURU, false).build());
        models.put(3L, createModelBuilder(2, 1L, 3L, GURU, false).build());


        when(modelStorageService.getModels(any())).thenAnswer(i -> {
            ModelStorage.GetModelsRequest request = (ModelStorage.GetModelsRequest) i.getArgument(0);
            ModelStorage.GetModelsResponse.Builder builder = ModelStorage.GetModelsResponse.newBuilder();

            request.getModelIdsList().forEach(id -> {
                if (models.containsKey(id)) {
                    builder.addModels(models.get(id));
                }
            });

            return builder.build();
        });

        when(modelStorageService.saveModelsGroup(any())).thenAnswer(i -> {
            ModelCardApi.SaveModelsGroupRequest request = (ModelCardApi.SaveModelsGroupRequest) i.getArgument(0);

            ModelCardApi.SaveModelsGroupResponse.Builder groupResponseBuilder =
                ModelCardApi.SaveModelsGroupResponse.newBuilder();

            request.getModelsRequestList().stream().flatMap(r -> r.getModelsList().stream()).forEach(model -> {
                models.put(model.getId(), model);

                ModelCardApi.SaveModelsGroupOperationResponse.Builder groupOperationResponseBuilder =
                    ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.OK)
                        .addRequestedModelsStatuses(
                            ModelStorage.OperationStatus.newBuilder()
                                .setModelId(model.getId())
                                .setStatus(ModelStorage.OperationStatusType.NO_OP)
                                .setType(ModelStorage.OperationType.CHANGE)
                                .build()
                        );

                groupResponseBuilder.addResponse(groupOperationResponseBuilder.build());
            });

            return groupResponseBuilder.build();
        });
    }

    @Test
    public void publishGuruModels() throws Exception {
        ModelStorageHelper modelStorageHelper = new ModelStorageHelper(modelStorageService, modelStorageService);
            Stats stats = modelStorageHelper.publishGuruModels(Lists.newArrayList(new Model(1L, 1L),
                                                                                  new Model(1L, 2L),
                                                                                  new Model(2L, 3L),
                                                                                  new Model(3L, 4L)));

        Assert.assertTrue(models.get(2L).getPublished());
        Assert.assertTrue(models.get(3L).getPublished());
        Assert.assertEquals(Sets.newHashSet(1L, 2L, 3L, 4L), stats.getModelIdsToFind());
        Assert.assertEquals(Sets.newHashSet(2L, 3L), stats.getPublishedModels());
        Assert.assertEquals(Sets.newHashSet(1L), stats.getAlreadyPublishedModels());
        Assert.assertEquals(Sets.newHashSet(4L), stats.getNotFoundModels());
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
                VENDOR_ID, MboParameters.ValueType.ENUM, "vendor").setOptionId(vendorId.intValue()));
        }
        if (name != null) {
            result.addTitles(ModelStorage.LocalizedString.newBuilder()
                                 .setIsoCode(RU_ISO_CODE)
                                 .setValue(name)
                                 .build());
            result.addParameterValues(createParameterValue(
                NAME_ID, MboParameters.ValueType.STRING, "name").addStrValue(
                createLocalizedString(name)));
        }
        if (published != null) {
            result.setPublished(published);
        }
        if (site != null) {
            result.addParameterValues(createParameterValue(
                URL_ID, MboParameters.ValueType.STRING, "url").addStrValue(
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

    public static ModelStorage.LocalizedString createLocalizedString(String str) {
        ModelStorage.LocalizedString.Builder builder = ModelStorage.LocalizedString.newBuilder();
        builder.setValue(str);
        builder.setIsoCode(RU_ISO_CODE);
        return builder.build();
    }
}