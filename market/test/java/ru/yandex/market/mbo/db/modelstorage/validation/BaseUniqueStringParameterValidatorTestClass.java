package ru.yandex.market.mbo.db.modelstorage.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModelUtils;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelQuality;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

/**
 * @author s-ermakov
 * @author danfertev
 */
@SuppressWarnings("checkstyle:magicNumber")
public abstract class BaseUniqueStringParameterValidatorTestClass extends BaseValidatorTestClass {

    protected UniqueStringParameterValidator validator;
    protected ModelValidationContext context;

    private CommonModel baseModel1;
    private CommonModel baseModel2;
    private CommonModel baseModel3;
    private CommonModel baseSku1;
    private CommonModel baseSku2;
    private CommonModel baseSku3;
    private CommonModel baseDummy1;

    protected void baseSetup(UniqueStringParameterValidator validator) {
        this.validator = validator;
        context = mock(ModelValidationContext.class);

        baseModel1 = createGuruWithValue(1, "a");
        baseModel2 = createGuruWithValue(2, "b");
        baseModel3 = createGuruWithValue(3, "c", "d");
        baseSku1 = createSkuWithValue(11, 1, "aa");
        baseSku2 = createSkuWithValue(22, 2, "bb");
        baseSku3 = createSkuWithValue(33, 3, "cc", "dd");
        baseDummy1 = createModelWithValue(100, CommonModel.Source.GURU_DUMMY, "eee");

        storage.saveModels(
            ModelSaveGroup.fromModels(baseModel1, baseModel2, baseModel3, baseSku1, baseSku2, baseSku3, baseDummy1),
            saveContext
        );
    }

    @Test
    public void testValidationInEmptyNewModel() {
        CommonModel model = createModel(0);

        List<ModelValidationError> errors = validator.validate(context, modelChanges(model), Arrays.asList(model));
        Assertions.assertThat(errors).isEmpty();
    }

    @Test
    public void testValidationNewModel() {
        CommonModel model = createGuruWithValue(0, "e");

        List<ModelValidationError> errors = validator.validate(context, modelChanges(model), Arrays.asList(model));
        Assertions.assertThat(errors).isEmpty();
    }

    @Test
    public void testValidationNewModelWithDuplicateValueShouldFail() {
        CommonModel model = createGuruWithValue(0, "a");

        List<ModelValidationError> errors = validator.validate(context, modelChanges(model), Arrays.asList(model));
        ModelValidationError error = error("a", model, baseModel1, true, true);
        Assertions.assertThat(errors).containsExactlyInAnyOrder(error);
    }

    @Test
    public void testValidationNewModelWithDuplicateValuesShouldFail() {
        CommonModel model = createGuruWithValue(0, "a", "d");

        List<ModelValidationError> errors = validator.validate(context, modelChanges(model), Arrays.asList(model));
        ModelValidationError error1 = error("a", model, baseModel1, true, false);
        ModelValidationError error2 = error("d", model, baseModel3, true, false);
        Assertions.assertThat(errors).containsExactlyInAnyOrder(error1, error2);
    }

    @Test
    public void testValidationInEmptyUpdatedModel() {
        CommonModel model = createModel(1);

        List<ModelValidationError> errors = validator.validate(context, modelChanges(model), Arrays.asList(model));
        Assertions.assertThat(errors).isEmpty();
    }

    @Test
    public void testValidationInUpdatedModel() {
        CommonModel model = createGuruWithValue(2, "e");

        List<ModelValidationError> errors = validator.validate(context, modelChanges(model), Arrays.asList(model));
        Assertions.assertThat(errors).isEmpty();
    }

    @Test
    public void testValidationInUpdatedModelWithDuplicateValueShouldFail() {
        CommonModel model = createGuruWithValue(2, "a");

        List<ModelValidationError> errors = validator.validate(context, modelChanges(model), Arrays.asList(model));
        ModelValidationError error = error("a", model, baseModel1, true);
        Assertions.assertThat(errors).containsExactlyInAnyOrder(error);
    }

    @Test
    public void testValidationInUpdatedModelWithDuplicateValuesShouldFail() {
        CommonModel model = createGuruWithValue(2, "a", "c");

        List<ModelValidationError> errors = validator.validate(context, modelChanges(model), Arrays.asList(model));
        ModelValidationError error1 = error("a", model, baseModel1, true, false);
        ModelValidationError error2 = error("c", model, baseModel3, true, false);
        Assertions.assertThat(errors).containsExactlyInAnyOrder(error1, error2);
    }

    @Test
    public void testValidationNewModelInMultiValue() {
        CommonModel model = createGuruWithValue(0, "ы", "ы");

        List<ModelValidationError> errors = validator.validate(context, modelChanges(model), Arrays.asList(model));
        ModelValidationError error = error("ы", model, model, true, false);
        Assertions.assertThat(errors).containsExactlyInAnyOrder(error);
    }

    @Test
    public void testValidationUpdatedInMultiValue() {
        CommonModel beforeModel = createGuruWithValue(1, "ы");
        storage.saveModels(ModelSaveGroup.fromModels(beforeModel), saveContext);
        CommonModel model = createModelWithValue(beforeModel, "ы", "ы");

        List<ModelValidationError> errors = validator.validate(context, modelChanges(model), Arrays.asList(model));
        ModelValidationError error = error("ы", model, beforeModel, true, false);
        Assertions.assertThat(errors).containsExactlyInAnyOrder(error);
    }

    @Test
    public void testCriticalErrorIfModelContainsDuplicateValuesAndModelWasChanged() {
        CommonModel beforeModel = createGuruWithValue(4, "ы", "ы");
        storage.saveModel(beforeModel, saveContext);

        CommonModel model = createModelWithValue(beforeModel, "ы", "ы", "э");

        List<ModelValidationError> errors = validator.validate(context, modelChanges(model), Arrays.asList(model));
        ModelValidationError error = error("ы", model, beforeModel, true, false);
        Assertions.assertThat(errors).containsExactlyInAnyOrder(error);
    }

    @Test
    public void testNoErrorIfValuesDontChange() {
        // специально устанавливаем модели в неконсистентное состояние
        CommonModel model1 = createGuruWithValue(1, "a");
        CommonModel model2 = createGuruWithValue(2, "a");
        storage.saveModels(ModelSaveGroup.fromModels(model1, model2), saveContext);

        CommonModel model = createModelWithValue(model2, "a");

        assertFalse(validator.isPropertyChanged(context, modelChanges(model), Arrays.asList(model)));
    }

    @Test
    public void testErrorIfValuesDontChangeButChangePosition() {
        // специально устанавливаем модели в неконсистентное состояние
        CommonModel model1 = createGuruWithValue(1, "a", "b");
        CommonModel model2 = createGuruWithValue(2, "a", "b");
        storage.saveModels(ModelSaveGroup.fromModels(model1, model2), saveContext);

        CommonModel model = createModelWithValue(model2, "b", "a");

        List<ModelValidationError> errors = validator.validate(context, modelChanges(model), Arrays.asList(model));
        ModelValidationError error1 = error("a", model, model1, false);
        ModelValidationError error2 = error("b", model, model1, false);
        Assertions.assertThat(errors).containsExactlyInAnyOrder(error1, error2);
    }

    @Test
    public void testCriticalErrorIfAddDuplicateValueToExistOne() {
        CommonModel beforeModel = createGuruWithValue(7, "мягкий знак");
        storage.saveModels(ModelSaveGroup.fromModels(beforeModel), saveContext);

        CommonModel model = createModelWithValue(beforeModel, "мягкий знак", "мягкий знак");

        List<ModelValidationError> errors = validator.validate(context, modelChanges(model), Arrays.asList(model));
        ModelValidationError error = error("мягкий знак", model, beforeModel, true, false);
        Assertions.assertThat(errors).containsExactlyInAnyOrder(error);
    }

    @Test
    public void testCriticalErrorIfAdd2DuplicateValueToExistOne() {
        CommonModel beforeModel = createGuruWithValue(7, "мягкий знак");
        storage.saveModels(ModelSaveGroup.fromModels(beforeModel), saveContext);

        CommonModel model = createModelWithValue(beforeModel, "мягкий знак", "мягкий знак", "мягкий знак");

        List<ModelValidationError> errors = validator.validate(context, modelChanges(model), Arrays.asList(model));
        ModelValidationError error = error("мягкий знак", model, beforeModel, true, false);
        Assertions.assertThat(errors).containsExactlyInAnyOrder(error);
    }

    @Test
    public void testCriticalErrorIfDeleteOnlyOneDuplicateFromTree() {
        CommonModel beforeModel = createGuruWithValue(7, "мягкий знак", "мягкий знак", "мягкий знак");
        storage.saveModels(ModelSaveGroup.fromModels(beforeModel), saveContext);

        CommonModel model = createModelWithValue(beforeModel, "мягкий знак", "мягкий знак");

        List<ModelValidationError> errors = validator.validate(context, modelChanges(model), Arrays.asList(model));
        ModelValidationError error = error("мягкий знак", model, beforeModel, true, false);
        Assertions.assertThat(errors).containsExactlyInAnyOrder(error);
    }

    @Test
    public void testModelsSaveFromInconsistentState() {
        CommonModel model1 = createGuruWithValue(1, "a");
        CommonModel model2 = createGuruWithValue(2, "a");
        storage.saveModels(ModelSaveGroup.fromModels(model1, model2), saveContext);

        CommonModel model = createGuruWithValue(2, "b");

        List<ModelValidationError> errors = validator.validate(context, modelChanges(model), Arrays.asList(model));
        Assertions.assertThat(errors).isEmpty();
    }

    @Test
    public void testNoCriticalErrorIfValuesAlreadyWasInInconsistentState() {
        // специально устанавливаем модели в неконсистентное состояние
        CommonModel beforeModel = createGuruWithValue(2, "c");
        storage.saveModels(ModelSaveGroup.fromModels(beforeModel), saveContext);

        CommonModel model = createModelWithValue(beforeModel, "c", "e");

        List<ModelValidationError> errors = validator
            .validate(context, modelChanges(model), Collections.singletonList(model));
        ModelValidationError error1 = error("c", model, baseModel3, false);
        Assertions.assertThat(errors).containsExactlyInAnyOrder(error1);
    }

    @Test
    public void testErrorIfCategoryChanges() {
        // специально устанавливаем модели в неконсистентное состояние
        CommonModel beforeModel = createGuruWithValue(2, "c");
        storage.saveModels(ModelSaveGroup.fromModels(beforeModel), saveContext);

        CommonModel model = createModelWithValue(beforeModel, "b", "c");
        model.setCategoryId(123456L);

        List<ModelValidationError> errors = validator
            .validate(context, modelChanges(model), Collections.singletonList(model));
        ModelValidationError error1 = error("c", model, baseModel3, true);
        Assertions.assertThat(errors).containsExactlyInAnyOrder(error1);
    }

    @Test
    public void testValidationInContext() {
        CommonModel model1 = createGuruWithValue(1, "a", "b", "d");
        CommonModel model2 = createGuruWithValue(2, "a", "c");
        CommonModel model0 = createGuruWithValue(0, "b", "e");

        List<ModelValidationError> errors = validator.validate(context, modelChanges(model1),
            Arrays.asList(model1, model2, model0));
        ModelValidationError error1 = error("b", model1, model0, true, false);
        ModelValidationError error2 = error("a", model1, model2, false, false);
        ModelValidationError error3 = error("d", model1, baseModel3, true, false);
        Assertions.assertThat(errors).containsExactlyInAnyOrder(error1, error2, error3);
    }

    @Test
    public void testValidationOnlySupportedModelTypes() {
        CommonModel guruModel = createGuruWithValue(0, "e");
        List<CommonModel> models = createModelWithDuplicateValueOfAllTypes("e");

        // test model validation
        for (CommonModel model : models) {
            if (validator.getSupportedModelTypes().contains(model.getCurrentType())) {
                List<ModelValidationError> errors = validator.validate(context, modelChanges(model),
                    Arrays.asList(model, guruModel));
                Assertions.assertThat(errors.size()).isEqualTo(1);
            }
        }

        // test validation context using updatedModel
        List<ModelValidationError> errorsFromUpdateContext = validator.validate(context, modelChanges(guruModel),
            models);
        List<ModelValidationError> errors = models.stream()
            .filter(m -> validator.getSupportedModelTypes().contains(m.getCurrentType()))
            .map(m -> error("e", guruModel.getId(), m.getId(), true, false))
            .collect(Collectors.toList());
        Assertions.assertThat(errorsFromUpdateContext).containsExactlyInAnyOrderElementsOf(errors);

        // test validation using models from storage
        storage.saveModels(ModelSaveGroup.fromModels(models), saveContext);

        List<ModelValidationError> errorsFromStorage = validator.validate(context, modelChanges(guruModel), models);
        Assertions.assertThat(errorsFromStorage).containsExactlyInAnyOrderElementsOf(errors);
    }

    @Test
    public void testDuplicateInParent() {
        CommonModel sku = createSkuWithValue(11, 1, "a");

        List<ModelValidationError> errors = validator.validate(context, modelChanges(sku), Arrays.asList(sku));
        ModelValidationError error = error("a", sku, baseModel1, true, true, true);
        Assertions.assertThat(errors).containsExactlyInAnyOrder(error);
    }

    @Test
    public void testDuplicateInOther() {
        CommonModel guru = createGuruWithValue(2, "a");

        List<ModelValidationError> errors = validator.validate(context, modelChanges(guru), Arrays.asList(guru));
        ModelValidationError error = error("a", guru, baseModel1, true, true, false);
        Assertions.assertThat(errors).containsExactlyInAnyOrder(error);
    }

    @Test
    public void testShouldForceError() {
        CommonModel guru = createGuruWithValue(2, "eee");

        List<ModelValidationError> errors = validator.validate(context, modelChanges(guru), Arrays.asList(guru));
        if (validator.getSupportedModelTypes().contains(CommonModel.Source.GURU_DUMMY)) {
            ModelValidationError error = error("eee", guru, baseDummy1, true, true, false)
                .setShouldForce(true);
            Assertions.assertThat(errors).containsExactlyInAnyOrder(error);
        } else {
            Assertions.assertThat(errors).isEmpty();
        }
    }

    @Test
    public void testMainOperatorModelVSDuplicatePartnerModel() {
        CommonModel guru = createGuruWithValue(2, "a");
        storage.getAllModels().stream()
            .filter(m -> m.getId() != 2)
            .forEach(this::makeModelPartner);
        List<ModelValidationError> errors = validator.validate(context, modelChanges(guru), Arrays.asList(guru));
        ModelValidationError error = error("a", guru, baseModel1, false, false, false);
        Assertions.assertThat(errors).containsExactlyInAnyOrder(error);
        assertFalse(errors.iterator().next().isCritical());
    }

    private void makeModelPartner(CommonModel commonModel) {
        List<Word> words = Collections.singletonList(new Word(255, "a"));
        ParameterValue parameterValue = new ParameterValue(KnownIds.MODEL_QUALITY_PARAM_ID, XslNames.MODEL_QUALITY,
            Param.Type.ENUM, null, null, ModelQuality.PARTNER.getOptionId(), words, null);
        commonModel.addParameterValue(parameterValue);
        assertFalse(CommonModelUtils.isOperatorQuality(commonModel));
    }

    protected ModelValidationError error(String value, long baseModelId, long relatedModelId,
                                         boolean isCritical, boolean allowForce, boolean parent) {
        String message = null;
        String xslName;
        switch (validator.getErrorType()) {
            case DUPLICATE_BARCODE:
                if (allowForce) {
                    message = "Баркод '%{PARAM_VALUE}' дублируется в модели %{MODEL_ID}. " +
                        "Если сохранить принудительно, баркод будет удален из %{MODEL_RELATION_TYPE} модели.";
                } else if (!isCritical) {
                    message = "Баркод '%{PARAM_VALUE}' дублируется в модели %{MODEL_ID}. " +
                        "Баркод был перенесён из %{MODEL_RELATION_TYPE} модели.";
                } else {
                    message = "Баркод '%{PARAM_VALUE}' дублируется в модели %{MODEL_ID}. " +
                        "Баркод не может быть удален из %{MODEL_RELATION_TYPE} модели.";
                }
                xslName = XslNames.BAR_CODE;
                break;
            case DUPLICATE_VENDOR_CODE:
                if (allowForce) {
                    message = "Код вендора '%{PARAM_VALUE}' дублируется в модели %{MODEL_ID}. " +
                        "Если сохранить принудительно, код вендора будет удален из %{MODEL_RELATION_TYPE} модели.";
                } else if (!isCritical) {
                    message = "Код вендора '%{PARAM_VALUE}' дублируется в модели %{MODEL_ID}. " +
                        "Код вендора был перенесён из %{MODEL_RELATION_TYPE} модели.";
                } else {
                    message = "Код вендора '%{PARAM_VALUE}' дублируется в модели %{MODEL_ID}. " +
                        "Код вендора не может быть удален из %{MODEL_RELATION_TYPE} модели.";
                }
                xslName = XslNames.VENDOR_CODE;
                break;
            default:
                xslName = null;
        }
        return new ModelValidationError(baseModelId,
            validator.getErrorType(), isCritical, allowForce)
            .addLocalizedMessagePattern(message)
            .addParam(ModelStorage.ErrorParamName.PARAM_VALUE, value)
            .addParam(ModelStorage.ErrorParamName.MODEL_ID, relatedModelId)
            .addParam(ModelStorage.ErrorParamName.MODEL_RELATION_TYPE,
                parent ? UniqueStringParameterValidator.PARENT_RELATION_TYPE
                    : UniqueStringParameterValidator.NO_RELATION_TYPE)
            .addParam(ModelStorage.ErrorParamName.PARAM_XSL_NAME, xslName);
    }

    protected ModelValidationError error(String value, long baseModelId, long relatedModelId,
                                         boolean isCritical, boolean allowForce) {
        return error(value, baseModelId, relatedModelId, isCritical, isCritical && allowForce, false);
    }

    protected ModelValidationError error(String value, long baseModelId, long relatedModelId,
                                         boolean isCritical) {
        return error(value, baseModelId, relatedModelId, isCritical, isCritical, false);
    }

    protected ModelValidationError error(String value, CommonModel baseModel, CommonModel relatedModel,
                                         boolean isCritical, boolean allowForce, boolean parent) {
        return error(value, baseModel.getId(), relatedModel.getId(), isCritical, allowForce, parent);
    }

    protected ModelValidationError error(String value, CommonModel baseModel, CommonModel relatedModel,
                                         boolean isCritical, boolean allowForce) {
        return error(value, baseModel, relatedModel, isCritical, isCritical && allowForce, false);
    }

    protected ModelValidationError error(String value, CommonModel baseModel, CommonModel relatedModel,
                                         boolean isCritical) {
        return error(value, baseModel, relatedModel, isCritical, isCritical);
    }

    protected CommonModel createModelWithValue(long id, CommonModel.Source type, String... values) {
        return createModel(id, type, b ->
            b.startParameterValue()
                .paramId(1)
                .xslName(validator.getParamName())
                .words(values)
                .endParameterValue()
        );
    }

    protected CommonModel createModelWithValue(CommonModel beforeModel, String... values) {
        return createModelWithValue(beforeModel.getId(), beforeModel.getCurrentType(), values);
    }

    protected CommonModel createGuruWithValue(long id, String... values) {
        return createModelWithValue(id, CommonModel.Source.GURU, values);
    }

    protected CommonModel createSkuWithValue(long id, long parentId, String... values) {
        return createModel(id, CommonModel.Source.SKU, b ->
            b.startParameterValue()
                .paramId(1)
                .xslName(validator.getParamName())
                .words(values)
                .endParameterValue()
                .startModelRelation()
                .id(parentId).categoryId(CATEGORY_ID).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
                .endModelRelation()
        );
    }

    private List<CommonModel> createModelWithDuplicateValueOfAllTypes(String... values) {
        AtomicInteger index = new AtomicInteger(10);
        return Arrays.stream(CommonModel.Source.values())
            .sorted()
            .map(type -> {
                CommonModel modelWithValues = createGuruWithValue(index.getAndIncrement(), values);
                modelWithValues.setCurrentType(type);
                return modelWithValues;
            })
            .collect(Collectors.toList());
    }
}
