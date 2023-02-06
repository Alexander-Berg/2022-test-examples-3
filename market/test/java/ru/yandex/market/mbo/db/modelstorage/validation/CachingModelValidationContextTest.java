package ru.yandex.market.mbo.db.modelstorage.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ru.yandex.market.mbo.db.modelstorage.StatsModelQueryService;
import ru.yandex.market.mbo.db.modelstorage.StatsModelStorageService;
import ru.yandex.market.mbo.db.modelstorage.health.OperationStats;
import ru.yandex.market.mbo.db.modelstorage.validation.context.CachingModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.licensor.MboLicensors;

import static org.mockito.ArgumentMatchers.any;

/**
 * @author ayratgdl
 * @date 03.05.18
 */
public class CachingModelValidationContextTest {
    private static final long CATEGORY_ID = 101;
    private static final long MODEL_ID_1 = 201;
    private static final long MODEL_ID_2 = 202;
    private static final long MODEL_ID_3 = 203;
    private static final long PARAM_ID_1 = 301;

    private TestingPurposesCachingModelValidationContext validationContext;

    @Before
    public void setUp() throws Exception {
        validationContext = new TestingPurposesCachingModelValidationContext();
    }

    /*
     * models: empty
     * relationTypes: empty
     * updatedModels: empty
     * takeOnlyLocalModels: false
     * return: empty
     */
    @Test
    public void getRelatedModelsForModelsForEmptyData() {
        RelatedModelsContainer actualRelatedModels =
            validationContext.getRelatedModelsForModels(CATEGORY_ID, Collections.emptyList(),
                                                        Collections.emptyList(),
                                                        Collections.emptyList(), true);

        RelatedModelsContainer expectedRelatedModels = new RelatedModelsContainer();

        Assert.assertEquals(expectedRelatedModels, actualRelatedModels);
    }

    /*
     * models: MODEL_1 (SKU_MODEL: MODEL_2)
     * relationTypes: SKU_MODEL
     * updatedModels: MODEL_1, MODEL_2
     * takeOnlyLocalModels: false
     * return: MODEL_2
     */
    @Test
    public void getRelatedModelsForModelsWhenUpdatedModelsContainsRelatedModel() {
        CommonModel model1 = new CommonModel();
        model1.setId(MODEL_ID_1);
        model1.setCategoryId(CATEGORY_ID);
        model1.addRelation(new ModelRelation(MODEL_ID_2, CATEGORY_ID, ModelRelation.RelationType.SKU_MODEL));

        CommonModel model2 = new CommonModel();
        model2.setId(MODEL_ID_2);
        model2.setCategoryId(CATEGORY_ID);

        RelatedModelsContainer actualRelatedModels =
            validationContext.getRelatedModelsForModels(CATEGORY_ID, Arrays.asList(model1),
                                                        Arrays.asList(ModelRelation.RelationType.SKU_MODEL),
                                                        Arrays.asList(model1, model2), true);

        RelatedModelsContainer expectedRelatedModels = new RelatedModelsContainer();
        expectedRelatedModels.addModelFromSaveRequest(model2, ModelRelation.RelationType.SKU_MODEL);

        Assert.assertEquals(expectedRelatedModels, actualRelatedModels);
    }

    /*
     * models: новая модель MODEL_1 (SKU_MODEL: MODEL_2)
     * relationTypes: SKU_MODEL
     * updatedModels: MODEL_1, MODEL_2
     * takeOnlyLocalModels: false
     * return: MODEL_2
     */
    @Test
    public void getRelatedModelsForModelsForNewModel() {
        CommonModel model1 = new CommonModel();
        model1.setId(CommonModel.NO_ID);
        model1.setCategoryId(CATEGORY_ID);
        model1.addRelation(new ModelRelation(MODEL_ID_2, CATEGORY_ID, ModelRelation.RelationType.SKU_MODEL));

        CommonModel model2 = new CommonModel();
        model2.setId(MODEL_ID_2);
        model2.setCategoryId(CATEGORY_ID);

        RelatedModelsContainer actualRelatedModels =
            validationContext.getRelatedModelsForModels(CATEGORY_ID, Arrays.asList(model1),
                                                        Arrays.asList(ModelRelation.RelationType.SKU_MODEL),
                                                        Arrays.asList(model1, model2), true);

        RelatedModelsContainer expectedRelatedModels = new RelatedModelsContainer();
        expectedRelatedModels.addModelFromSaveRequest(model2, ModelRelation.RelationType.SKU_MODEL);

        Assert.assertEquals(expectedRelatedModels, actualRelatedModels);
    }

    /*
     * models: MODEL_1 (SKU_MODEL: MODEL_2).
     * relationTypes: SKU_MODEL
     * updatedModels: новая модель MODEL_2
     * takeOnlyLocalModels: false
     * return: MODEL_2
     */
    @Test
    public void getRelatedModelsForModelsWhenRelatedModelIsNew() {
        CommonModel model1 = new CommonModel();
        model1.setId(MODEL_ID_1);
        model1.setCategoryId(CATEGORY_ID);
        addModel(model1);

        CommonModel model2 = new CommonModel();
        model2.setId(CommonModel.NO_ID);
        model2.setCategoryId(CATEGORY_ID);
        model2.addRelation(new ModelRelation(MODEL_ID_1, CATEGORY_ID, ModelRelation.RelationType.SKU_PARENT_MODEL));

        RelatedModelsContainer actualRelatedModels =
            validationContext.getRelatedModelsForModels(CATEGORY_ID, Arrays.asList(model1),
                                                        Arrays.asList(ModelRelation.RelationType.SKU_MODEL),
                                                        Arrays.asList(model2), false);

        RelatedModelsContainer expectedRelatedModels = new RelatedModelsContainer();
        expectedRelatedModels.addModelFromSaveRequest(model2, ModelRelation.RelationType.SKU_MODEL);

        Assert.assertEquals(expectedRelatedModels, actualRelatedModels);
    }

    /*
     * models: MODEL_1 (SKU_MODEL: MODEL_2).
     * relationTypes: SKU_MODEL
     * updatedModels: empty
     * takeOnlyLocalModels: false
     * return: MODEL_2
     */
    @Test
    public void getRelatedModelsForModelsLoadModelFromStorage() {
        CommonModel model1 = new CommonModel();
        model1.setId(MODEL_ID_1);
        model1.setCategoryId(CATEGORY_ID);
        model1.addRelation(new ModelRelation(MODEL_ID_2, CATEGORY_ID, ModelRelation.RelationType.SKU_MODEL));
        addModel(model1);

        CommonModel model2 = new CommonModel();
        model2.setId(MODEL_ID_2);
        model2.setCategoryId(CATEGORY_ID);
        addModel(model2);

        RelatedModelsContainer actualRelatedModels =
            validationContext.getRelatedModelsForModels(CATEGORY_ID, Arrays.asList(model1),
                                                        Arrays.asList(ModelRelation.RelationType.SKU_MODEL),
                                                        Collections.emptyList(), false);

        RelatedModelsContainer expectedRelatedModels = new RelatedModelsContainer();
        expectedRelatedModels.addModelFromStorage(model2, ModelRelation.RelationType.SKU_MODEL);

        Assert.assertEquals(expectedRelatedModels, actualRelatedModels);
    }

    /*
     * models: MODEL_1 (ANCESTOR: MODEL_2)
     * relationTypes: SKU_MODEL
     * updatedModels: MODEL_1, MODEL_2
     * takeOnlyLocalModels: false
     * return: empty
     */
    @Test
    public void getRelatedModelsForModelsWhenRelationWithoutOpposite() {
        Assert.assertFalse(ModelRelation.RelationType.ANCESTOR.hasOpposite());

        CommonModel model1 = new CommonModel();
        model1.setId(MODEL_ID_1);
        model1.setCategoryId(CATEGORY_ID);
        model1.addRelation(new ModelRelation(MODEL_ID_2, CATEGORY_ID, ModelRelation.RelationType.ANCESTOR));

        CommonModel model2 = new CommonModel();
        model2.setId(MODEL_ID_2);
        model2.setCategoryId(CATEGORY_ID);

        RelatedModelsContainer actualRelatedModels =
            validationContext.getRelatedModelsForModels(CATEGORY_ID, Arrays.asList(model1),
                                                        Arrays.asList(ModelRelation.RelationType.SKU_MODEL),
                                                        Arrays.asList(model1, model2), false);

        RelatedModelsContainer expectedRelatedModels = new RelatedModelsContainer();

        Assert.assertEquals(expectedRelatedModels, actualRelatedModels);
    }

    /*
     * models: MODEL_1
     * relationTypes: SKU_MODEL
     * updatedModels: MODEL_1, MODEL_2 (ANCESTOR: MODEL_1)
     * takeOnlyLocalModels: false
     * return: empty
     */
    @Test
    public void getRelatedModelsForModelsWhenUpdatedModelContainsRelationWithoutOpposite() {
        CommonModel model1 = new CommonModel();
        model1.setId(MODEL_ID_1);
        model1.setCategoryId(CATEGORY_ID);

        CommonModel model2 = new CommonModel();
        model2.setId(CommonModel.NO_ID);
        model2.setCategoryId(CATEGORY_ID);
        model2.addRelation(new ModelRelation(MODEL_ID_1, CATEGORY_ID, ModelRelation.RelationType.ANCESTOR));

        RelatedModelsContainer actualRelatedModels =
            validationContext.getRelatedModelsForModels(CATEGORY_ID, Arrays.asList(model1),
                                                        Arrays.asList(ModelRelation.RelationType.SKU_MODEL),
                                                        Arrays.asList(model1, model2), false);

        RelatedModelsContainer expectedRelatedModels = new RelatedModelsContainer();

        Assert.assertEquals(expectedRelatedModels, actualRelatedModels);
    }

    /*
     * models: MODEL_1 (SKU_MODEL: MODEL_2)
     * relationTypes: SKU_MODEL
     * updatedModels: empty
     * takeOnlyLocalModels: true
     * return: empty
     */
    @Test
    public void getRelatedModelsForModelsWithTakeOnlyLocalModelsIsTrue() {
        CommonModel model1 = new CommonModel();
        model1.setId(MODEL_ID_1);
        model1.setCategoryId(CATEGORY_ID);
        model1.addRelation(new ModelRelation(MODEL_ID_2, CATEGORY_ID, ModelRelation.RelationType.SKU_MODEL));

        CommonModel model2 = new CommonModel();
        model2.setId(MODEL_ID_2);
        model2.setCategoryId(CATEGORY_ID);
        addModel(model2);

        RelatedModelsContainer actualRelatedModels =
            validationContext.getRelatedModelsForModels(CATEGORY_ID, Arrays.asList(model1),
                                                        Arrays.asList(ModelRelation.RelationType.SKU_MODEL),
                                                        Collections.emptyList(), true);

        RelatedModelsContainer expectedRelatedModels = new RelatedModelsContainer();

        Assert.assertEquals(expectedRelatedModels, actualRelatedModels);
    }

    /*
     * models: MODEL_1 (SKU_MODEL: MODEL_2)
     * relationTypes: SKU_MODEL
     * updatedModels: MODEL_1, MODEL_2 (SKU_MODEL: MODEL_1)
     * takeOnlyLocalModels: false
     * return: MODEL_2
     */
    @Test
    public void getRelatedModelsForModelsWhenModel1AndModel2ReferToEachOther() {
        CommonModel model1 = new CommonModel();
        model1.setId(MODEL_ID_1);
        model1.setCategoryId(CATEGORY_ID);
        model1.addRelation(new ModelRelation(MODEL_ID_2, CATEGORY_ID, ModelRelation.RelationType.SKU_MODEL));

        CommonModel model2 = new CommonModel();
        model2.setId(MODEL_ID_2);
        model2.setCategoryId(CATEGORY_ID);
        model2.addRelation(new ModelRelation(MODEL_ID_1, CATEGORY_ID, ModelRelation.RelationType.SKU_PARENT_MODEL));

        RelatedModelsContainer actualRelatedModels =
            validationContext.getRelatedModelsForModels(CATEGORY_ID, Arrays.asList(model1),
                                                        Arrays.asList(ModelRelation.RelationType.SKU_MODEL),
                                                        Arrays.asList(model1, model2), false);

        RelatedModelsContainer expectedRelatedModels = new RelatedModelsContainer();
        expectedRelatedModels.addModelFromSaveRequest(model2, ModelRelation.RelationType.SKU_MODEL);

        Assert.assertEquals(expectedRelatedModels, actualRelatedModels);
    }

    /*
     * models: MODEL_1 (SKU_MODEL: MODEL_2), MODEL_1 (SKU_MODEL: MODEL_3)
     * relationTypes: SKU_MODEL
     * updatedModels: MODEL_1, MODEL_2
     * takeOnlyLocalModels: false
     * return: MODEL_2, MODEL_3
     */
    @Test
    public void getRelatedModelsForModelsWhenLoadPartModelsFromStorage() {
        CommonModel model1 = new CommonModel();
        model1.setId(MODEL_ID_1);
        model1.setCategoryId(CATEGORY_ID);
        model1.addRelation(new ModelRelation(MODEL_ID_2, CATEGORY_ID, ModelRelation.RelationType.SKU_MODEL));
        model1.addRelation(new ModelRelation(MODEL_ID_3, CATEGORY_ID, ModelRelation.RelationType.SKU_MODEL));

        CommonModel model2 = new CommonModel();
        model2.setId(MODEL_ID_2);
        model2.setCategoryId(CATEGORY_ID);

        CommonModel model3 = new CommonModel();
        model3.setId(MODEL_ID_3);
        model3.setCategoryId(CATEGORY_ID);
        addModel(model3);

        RelatedModelsContainer actualRelatedModels =
            validationContext.getRelatedModelsForModels(CATEGORY_ID, Arrays.asList(model1),
                                                        Arrays.asList(ModelRelation.RelationType.SKU_MODEL),
                                                        Arrays.asList(model1, model2), false);

        RelatedModelsContainer expectedRelatedModels = new RelatedModelsContainer();
        expectedRelatedModels.addModelFromSaveRequest(model2, ModelRelation.RelationType.SKU_MODEL);
        expectedRelatedModels.addModelFromStorage(model3, ModelRelation.RelationType.SKU_MODEL);

        Assert.assertEquals(expectedRelatedModels, actualRelatedModels);
    }

    /*
     * models: MODEL_1 (id = 0), MODEL_2 (id = 0 too, it is important)
     * relationTypes: empty
     * updatedModels: MODEL_1, MODEL_2
     * takeOnlyLocalModels: false
     * return: empty
     */
    @Test
    public void getDumpNewModel() {
        CommonModel model1 = new CommonModel();
        model1.setId(0L);
        model1.setCategoryId(CATEGORY_ID);
        model1.setCurrentType(CommonModel.Source.GURU);
        model1.setSource(CommonModel.Source.BOOK);
        model1.addParameterValue(new ParameterValue(
                                PARAM_ID_1,
                                "name",
                                Param.Type.STRING,
                                new ParameterValue.ValueBuilder()
                                                  .setStringValue(new Word(Word.DEFAULT_LANG_ID, "name1"))));

        CommonModel model2 = new CommonModel();
        model2.setId(0L);
        model2.setCategoryId(CATEGORY_ID);
        model2.setCurrentType(CommonModel.Source.GURU);
        model2.setSource(CommonModel.Source.BOOK);
        model2.addParameterValue(new ParameterValue(
                                PARAM_ID_1,
                                "name",
                                Param.Type.STRING,
                                new ParameterValue.ValueBuilder()
                                                  .setStringValue(new Word(Word.DEFAULT_LANG_ID, "name2"))));

        List<CommonModel> models = Lists.newArrayList(model1, model2);

        ModelStorage.Model m1 = validationContext.getDumpModel(model1, models);
        Assert.assertEquals(m1.getTitles(0).getValue(), "name1");

        ModelStorage.Model m2 = validationContext.getDumpModel(model2, models);
        Assert.assertEquals(m2.getTitles(0).getValue(), "name2");
    }

    private void addModel(CommonModel model) {
        validationContext.addModel(model);
    }

    private static class TestingPurposesCachingModelValidationContext extends CachingModelValidationContext {
        List<CommonModel> models = new ArrayList<>();

        private StatsModelStorageService modelStorageService;

        TestingPurposesCachingModelValidationContext() {
            super(new DumpValidationServiceStub());
            modelStorageService = Mockito.mock(StatsModelStorageService.class);
            Mockito.when(modelStorageService.getModels(Mockito.anyLong(), any(Collection.class), any()))
                .thenAnswer((Answer<List<CommonModel>>) invocation -> {
                    long categoryId = invocation.getArgument(0);
                    Collection<Long> modelIds = invocation.getArgument(1);
                    return models.stream()
                        .filter(model -> categoryId == model.getCategoryId() && modelIds.contains(model.getId()))
                        .collect(Collectors.toList());
                });
        }

        public void addModel(CommonModel model) {
            models.add(model);
        }

        @Override
        public StatsModelQueryService getModelQueryService() {
            return modelStorageService;
        }

        @Override
        public boolean hasCategory(Long categoryId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<Long, String> getMandatoryForSignatureParamNames(Long categoryId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean usedInCompatibilities(long modelId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean parameterMatchesDefinition(long categoryId, ParameterValue parameter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public NumericBounds getParameterBounds(long categoryId, String xslName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<Long, String> getSkuParameterNamesWithMode(long categoryId, SkuParameterMode mode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<Long, String> getMandatoryParamNames(long categoryId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Integer getStringParamMaxLength(long categoryId, String xslName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<Long, String> getOptionNames(long categoryId, long paramId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getReadableParameterName(long categoryId, String xslName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isGroupCategory(long categoryId) {
            return false;
        }

        @Override
        public boolean isCategoryAllowNonWhiteFirstPicture(long categoryId) {
            return false;
        }

        @Override
        public OperationStats getStats() {
            return new OperationStats();
        }

        @Override
        public Map<Long, ModelValidationVendor> loadCategoryVendors(long categoryId) {
            return null;
        }

        @Override
        public String getReadableParameterName(long categoryId, long paramId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<Long> getImagePickerParamIds(long categoryId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long getParamIdByXslName(long categoryId, String xslName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<Long> getCategoryIdsUpToRoot(long categoryId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<MboLicensors.Licensor> getLicensorConstrains() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ParameterProperties getParameterProperties(long categoryId, long paramId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<Long, Long> getHidToParentHid() {
            return null;
        }

        @Override
        public void setHidToParentHid(Map<Long, Long> hidToParentHid) {

        }
    }
}
