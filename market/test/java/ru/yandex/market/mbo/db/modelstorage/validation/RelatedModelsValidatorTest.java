package ru.yandex.market.mbo.db.modelstorage.validation;

import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.validation.context.CachingModelValidationContext;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.utils.WordUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author dmserebr
 * @date 28.03.18
 */
@SuppressWarnings("checkstyle:magicNumber")
public class RelatedModelsValidatorTest {
    private RelatedModelsValidator validator;
    private ModelValidationContext context;

    @Before
    public void setup() {
        validator = new RelatedModelsValidator();
        context = mock(CachingModelValidationContext.class);
        when(context.getRelatedModelsForModels(anyLong(), anyList(), anyCollection(), anyCollection(), anyBoolean()))
            .thenCallRealMethod();
    }

    @Test
    public void testNoErrorOnNewModelWithoutRelations() {
        CommonModel model = getModelBuilder()
            .id(0L)
            .getModel();
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, model),
            Collections.singletonList(model));
        Assert.assertThat(errors, Matchers.empty());
    }

    @Test
    public void testNoErrorOnUnchangedModelWithoutRelations() {
        CommonModel model = getModelBuilder().getModel();
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(model, model),
            Collections.singletonList(model));
        Assert.assertThat(errors, Matchers.empty());
    }

    @Test
    public void testNoErrorOnUnchangedModelWithSkuRelation() {
        CommonModel model = getModelBuilder()
            .modelRelation(2L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .getModel();
        CommonModel sku = getSkuBuilder()
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(model, model),
            Arrays.asList(model, sku));
        Assert.assertThat(errors, Matchers.empty());
    }

    @Test
    public void testNoErrorOnModelChangedCategoryWithSkuRelation() {
        // change category id both in model and SKU
        CommonModel model = getModelBuilder()
            .modelRelation(2L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .getModel();
        CommonModel changedModel = getModelBuilder()
            .category(11L)
            .modelRelation(2L, 11L, ModelRelation.RelationType.SKU_MODEL)
            .getModel();
        CommonModel changedSku = getSkuBuilder()
            .category(11L)
            .modelRelation(1L, 11L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();

        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(model, changedModel),
            Arrays.asList(changedModel, changedSku));
        Assert.assertThat(errors, Matchers.empty());
    }

    @Test
    public void testErrorOnModelChangedCategoryWithSkuRelation() {
        // change category id in model, forget about SKU
        CommonModel model = getModelBuilder()
            .modelRelation(2L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .getModel();
        CommonModel sku = getSkuBuilder()
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();
        CommonModel changedModel = getModelBuilder()
            .category(11L)
            .modelRelation(2L, 11L, ModelRelation.RelationType.SKU_MODEL)
            .getModel();

        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(model, changedModel),
            Arrays.asList(changedModel, sku));
        Assertions.assertThat(errors).containsExactlyInAnyOrder(
            RelatedModelsValidator.createInconsistentCategoryIdError(model.getId(), 11L, 2L,
                ModelRelation.RelationType.SKU_MODEL, true));
    }

    @Test
    public void testNoErrorOnNewSkuWithParentSkuRelation() {
        CommonModel model = getModelBuilder().getModel();
        CommonModel sku = getSkuBuilder()
            .id(0L)
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(null, sku),
            Arrays.asList(sku, model));
        Assert.assertThat(errors, Matchers.empty());
    }

    @Test
    public void testErrorOnSkuChangedCategoryWithParentSkuRelation() {
        // change category id in SKU, forget about parent model
        CommonModel model = getModelBuilder()
            .modelRelation(2L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .getModel();
        CommonModel sku = getSkuBuilder()
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();
        CommonModel changedSku = getSkuBuilder()
            .category(12L)
            .modelRelation(1L, 12L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();

        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(sku, changedSku),
            Arrays.asList(model, changedSku));
        Assertions.assertThat(errors).containsExactlyInAnyOrder(
            RelatedModelsValidator.createInconsistentCategoryIdError(sku.getId(), 12L, 1L,
                ModelRelation.RelationType.SKU_PARENT_MODEL, true));
    }

    @Test
    public void testErrorOnSkuChangedCategoryAndVendorWithParentSkuRelation() {
        // change both category id and vendor id in SKU, forget about parent model
        CommonModel model = getModelBuilder()
            .modelRelation(2L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .getModel();
        CommonModel sku = getSkuBuilder()
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();
        CommonModel changedSku = getSkuBuilder()
            .category(12L)
            .vendorId(31L)
            .modelRelation(1L, 12L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();

        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(sku, changedSku),
            Arrays.asList(model, changedSku));
        Assertions.assertThat(errors).containsExactlyInAnyOrder(
            RelatedModelsValidator.createInconsistentCategoryIdError(sku.getId(), 12L, 1L,
                ModelRelation.RelationType.SKU_PARENT_MODEL, true),
            RelatedModelsValidator.createInconsistentVendorIdError(sku.getId(), 31L, 1L,
                ModelRelation.RelationType.SKU_PARENT_MODEL, true));
    }

    @Test
    public void testErrorOnModelChangedVendorWithTwoSkuRelations() {
        // change vendor id in model, forget about both SKUs
        CommonModel model = getModelBuilder()
            .modelRelation(2L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(3L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .getModel();
        CommonModel sku1 = getSkuBuilder()
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();
        CommonModel sku2 = getSkuBuilder()
            .id(3L)
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();
        CommonModel changedModel = getModelBuilder()
            .vendorId(31L)
            .modelRelation(2L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(3L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .getModel();

        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(model, changedModel),
            Arrays.asList(changedModel, sku1, sku2));
        Assertions.assertThat(errors).containsExactlyInAnyOrder(
            RelatedModelsValidator.createInconsistentVendorIdError(model.getId(), 31L, 2L,
                ModelRelation.RelationType.SKU_MODEL, true),
            RelatedModelsValidator.createInconsistentVendorIdError(model.getId(), 31L, 3L,
                ModelRelation.RelationType.SKU_MODEL, true));
    }

    @Test
    public void testErrorOnModelChangedCategoryWithExperimentalRelation() {
        // change category id in model, forget about experimental model
        CommonModel model = getModelBuilder()
            .modelRelation(5L, 10L, ModelRelation.RelationType.EXPERIMENTAL_MODEL)
            .getModel();
        CommonModel experimental = getExperimentalBuilder()
            .modelRelation(1L, 10L, ModelRelation.RelationType.EXPERIMENTAL_BASE_MODEL)
            .getModel();
        CommonModel changedModel = getModelBuilder()
            .category(11L)
            .modelRelation(5L, 11L, ModelRelation.RelationType.EXPERIMENTAL_MODEL)
            .getModel();

        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(model, changedModel),
            Arrays.asList(changedModel, experimental));
        Assertions.assertThat(errors).containsExactlyInAnyOrder(
            RelatedModelsValidator.createInconsistentCategoryIdError(model.getId(), 11L, 5L,
                ModelRelation.RelationType.EXPERIMENTAL_MODEL, true));
    }

    @Test
    public void testNoErrorOnModelChangedVendorWithExperimentalRelation() {
        // change vendor id in model, don't change in experimental one - validation passes
        CommonModel model = getModelBuilder()
            .modelRelation(5L, 10L, ModelRelation.RelationType.EXPERIMENTAL_MODEL)
            .getModel();
        CommonModel experimental = getExperimentalBuilder()
            .modelRelation(1L, 10L, ModelRelation.RelationType.EXPERIMENTAL_BASE_MODEL)
            .getModel();
        CommonModel changedModel = getModelBuilder()
            .vendorId(32L)
            .modelRelation(5L, 10L, ModelRelation.RelationType.EXPERIMENTAL_MODEL)
            .getModel();

        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(model, changedModel),
            Arrays.asList(changedModel, experimental));
        Assert.assertThat(errors, Matchers.empty());
    }

    @Test
    public void testNonCriticalErrorOnChangedModelWithSkuRelation() {
        // have model with SKU in another category
        // change model but don't change category or vendor, get non-critical error
        CommonModel model = getModelBuilder()
            .modelRelation(2L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .getModel();
        CommonModel sku = getSkuBuilder()
            .category(11L)
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();
        CommonModel changedModel = new CommonModel(model);
        changedModel.putParameterValues(new ParameterValues(11, "testParam4",
            Param.Type.STRING, WordUtil.defaultWord("testValue4")));

        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(model, changedModel),
            Arrays.asList(changedModel, sku));
        Assertions.assertThat(errors).containsExactlyInAnyOrder(
            RelatedModelsValidator.createInconsistentCategoryIdError(model.getId(), 10L, 2L,
                ModelRelation.RelationType.SKU_MODEL, false));
    }

    @Test
    public void testGetRelatedModelFromStorageAndValidate() {
        // Emulate the situation when SKU is not passed in updatedModels and thus retrieved from storage
        // change category id in model, forget about SKU - the validation should fail
        CommonModel model = getModelBuilder()
            .modelRelation(2L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .getModel();
        CommonModel sku = getSkuBuilder()
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();
        CommonModel changedModel = getModelBuilder()
            .category(11L)
            .modelRelation(2L, 11L, ModelRelation.RelationType.SKU_MODEL)
            .getModel();

        when(context.getModels(anyLong(), anyList())).thenReturn(Collections.singletonList(sku));

        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(model, changedModel),
            Collections.singletonList(changedModel));
        Assertions.assertThat(errors).containsExactlyInAnyOrder(
            RelatedModelsValidator.createInconsistentCategoryIdError(model.getId(), 11L, 2L,
                ModelRelation.RelationType.SKU_MODEL, true));
    }

    @Test
    public void testErrorOnModelChangedVendorWithTwoNewSkus() {
        // change vendor id in model, forget about both SKUs (which are newly added)
        CommonModel model = getModelBuilder()
            .modelRelation(2L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(3L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .getModel();
        CommonModel sku1 = getSkuBuilder()
            .id(0L)
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();
        CommonModel sku2 = getSkuBuilder()
            .id(0L)
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();
        CommonModel changedModel = getModelBuilder()
            .vendorId(31L)
            .modelRelation(2L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(3L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .getModel();

        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(model, changedModel),
            Arrays.asList(changedModel, sku1, sku2));
        Assertions.assertThat(errors).containsExactlyInAnyOrder(
            RelatedModelsValidator.createInconsistentVendorIdError(model.getId(), 31L, 0L,
                ModelRelation.RelationType.SKU_MODEL, true),
            RelatedModelsValidator.createInconsistentVendorIdError(model.getId(), 31L, 0L,
                ModelRelation.RelationType.SKU_MODEL, true));
    }

    @Test
    public void testNoErrorOnModelChangedVendorWithDeletedSku() {
        // change vendor id in model, don't change in deleted SKU - no error
        CommonModel model = getModelBuilder()
            .modelRelation(2L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(3L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .getModel();
        CommonModel sku = getSkuBuilder()
            .deleted(true)
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();
        CommonModel changedModel = getModelBuilder()
            .vendorId(31L)
            .modelRelation(2L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(3L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .getModel();

        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(model, changedModel),
            Arrays.asList(changedModel, sku));
        Assert.assertThat(errors, Matchers.empty());
    }

    private static CommonModelBuilder getModelBuilder() {
        return CommonModelBuilder.newBuilder().startModel()
            .id(1L)
            .category(10L)
            .title("Test guru model")
            .currentType(CommonModel.Source.GURU)
            .vendorId(30L)
            .parameterValues(100L, "testParam1", "testValue");
    }

    private static CommonModelBuilder getSkuBuilder() {
        return CommonModelBuilder.newBuilder().startModel()
            .id(2L)
            .category(10L)
            .title("Test SKU model")
            .currentType(CommonModel.Source.SKU)
            .vendorId(30L)
            .parameterValues(101L, "testParam2", "testValue2");
    }

    private static CommonModelBuilder getExperimentalBuilder() {
        return CommonModelBuilder.newBuilder().startModel()
            .id(5L)
            .category(10L)
            .title("Test experimental model")
            .currentType(CommonModel.Source.EXPERIMENTAL)
            .vendorId(30L)
            .parameterValues(102L, "testParam3", "testValue3");
    }
}
