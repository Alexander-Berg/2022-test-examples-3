package ru.yandex.market.mbo.db.modelstorage.validation;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.db.modelstorage.validation.context.CachingModelValidationContext;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author verekonn
 * @since 18.02.2021
 */
@SuppressWarnings("checkstyle:magicNumber")
public class SkuModelAliasesValidatorTest extends BaseValidatorTestClass {

    private SkuModelAliasesValidator validator;
    private ModelValidationContext context;

    @Before
    public void setup() {
        validator = new SkuModelAliasesValidator();
        context = mock(CachingModelValidationContext.class);
        when(context.getRelatedModelsForModels(anyLong(), anyList(), anyCollection(), anyCollection(), anyBoolean()))
            .thenCallRealMethod();
        when(context.getBaseModel(any(), anyList()))
            .thenCallRealMethod();
        when(context.getChildren(any(), anyList()))
            .thenCallRealMethod();
        when(context.getModel(anyLong(), anyLong(), anyList()))
            .thenCallRealMethod();
    }

    @Test
    public void testDuplicatesInNew() {
        CommonModel model = getModelBuilder()
            .modelRelation(2L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(3L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .getModel();
        CommonModel sku2 = getSku2Builder()
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();

        CommonModel sku2Modified = getSku2Builder()
            .parameterValues(1L, XslNames.ALIASES, "al1", "al2")
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();

        CommonModel sku3 = getSku3Builder()
            .parameterValues(1L, XslNames.ALIASES, "al1")
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();
        Assertions.assertThat(
            validator.isPropertyChanged(context,
                new ModelChanges(sku2, sku2Modified),
                Arrays.asList(model, sku2Modified, sku3))
        ).isTrue();
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(sku2, sku2Modified),
            Arrays.asList(model, sku2Modified, sku3));
        Assertions.assertThat(errors).containsExactlyInAnyOrder(
            validator.createModelValidationError("al1", sku3, sku2, true)
        );
    }

    @Test
    public void testDuplicatesInOld() {
        CommonModel model = getModelBuilder()
            .modelRelation(2L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(3L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .getModel();
        CommonModel sku2 = getSku2Builder()
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .parameterValues(1L, XslNames.ALIASES, "al1", "al2")
            .getModel();

        CommonModel sku2Modified = getSku2Builder()
            .parameterValues(1L, XslNames.ALIASES, "al133")
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();

        CommonModel sku3 = getSku3Builder()
            .parameterValues(1L, XslNames.ALIASES, "al1")
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .getModel();

        Assertions.assertThat(
        validator.isPropertyChanged(context,
            new ModelChanges(sku2, sku2Modified),
            Arrays.asList(model, sku2Modified, sku3))
        ).isTrue();
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(sku2, sku2Modified),
            Arrays.asList(model, sku2Modified, sku3));
        Assertions.assertThat(errors).containsExactlyInAnyOrder(
            validator.createModelValidationError("al1", sku3, sku2, false)
        );
    }

    @Test
    public void testEmptyErrors() {
        CommonModel model = getModelBuilder()
            .modelRelation(2L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(3L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .getModel();
        CommonModel sku2 = getSku2Builder()
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .parameterValues(1L, XslNames.ALIASES, "al1")
            .getModel();

        CommonModel sku2Modified = getSku2Builder()
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .parameterValues(1L, XslNames.ALIASES, "al1", "al2")
            .getModel();

        CommonModel sku3 = getSku3Builder()
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .parameterValues(1L, XslNames.ALIASES, "al3", "al4")
            .getModel();
        Assertions.assertThat(
            validator.isPropertyChanged(context,
                new ModelChanges(sku2, sku2Modified),
                Arrays.asList(model, sku2Modified, sku3))
        ).isTrue();
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(sku2, sku2Modified),
            Arrays.asList(model, sku2Modified, sku3));
        Assertions.assertThat(errors).isEmpty();
    }


    @Test
    public void testIsPropertyChangedFalse() {
        CommonModel model = getModelBuilder()
            .modelRelation(2L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(3L, 10L, ModelRelation.RelationType.SKU_MODEL)
            .getModel();
        CommonModel sku2 = getSku2Builder()
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .parameterValues(1L, XslNames.ALIASES, "al1")
            .getModel();

        CommonModel sku2Modified = getSku2Builder()
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .parameterValues(1L, XslNames.ALIASES, "al1")
            .getModel();

        CommonModel sku3 = getSku3Builder()
            .modelRelation(1L, 10L, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .parameterValues(1L, XslNames.ALIASES, "al3", "al4")
            .getModel();
        Assertions.assertThat(
            validator.isPropertyChanged(context,
                new ModelChanges(sku2, sku2Modified),
                Arrays.asList(model, sku2Modified, sku3))
        ).isFalse();
        List<ModelValidationError> errors = validator.validate(
            context,
            new ModelChanges(sku2, sku2Modified),
            Arrays.asList(model, sku2Modified, sku3));
        Assertions.assertThat(errors).isEmpty();
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

    private static CommonModelBuilder getSku2Builder() {
        return CommonModelBuilder.newBuilder().startModel()
            .id(2L)
            .category(10L)
            .title("Test SKU model")
            .currentType(CommonModel.Source.SKU)
            .vendorId(30L)
            .parameterValues(101L, "testParam2", "testValue2");
    }

    private static CommonModelBuilder getSku3Builder() {
        return CommonModelBuilder.newBuilder().startModel()
            .id(3L)
            .category(10L)
            .title("Test 2 SKU model")
            .currentType(CommonModel.Source.SKU)
            .vendorId(30L)
            .parameterValues(101L, "testParam2", "testValue2");
    }
}
