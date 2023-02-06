package ru.yandex.market.mbo.db.modelstorage.validation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.db.modelstorage.validation.context.CachingModelValidationContext;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:MagicNumber")
public class SkuDefParamsFilledValidatorTest {

    private SkuDefiningParamsFilledValidator validator;
    private ModelValidationContext context;

    private static final List<Long> FILLED_DEFINING_IDS = asList(114L, 116L, 118L);
    private static final List<Long> UNFILLED_DEFINING_IDS = asList(294L, 295L, 297L);
    private static final List<Long> NON_DEFINING_IDS = asList(1062L, 1431L, 1476L);
    private static final long CATEGORY_ID_WITH_PARAMS = 9100592L;
    private static final long CATEGORY_ID_WITHOUT_PARAMS = 23306L;

    @Before
    public void setup() {
        validator = new SkuDefiningParamsFilledValidator();
        context = mock(CachingModelValidationContext.class);
        when(context.getSkuParameterNamesWithMode(CATEGORY_ID_WITH_PARAMS, SkuParameterMode.SKU_DEFINING))
            .thenReturn(ImmutableMap.<Long, String>builder()
                .put(FILLED_DEFINING_IDS.get(0), "")
                .put(FILLED_DEFINING_IDS.get(1), "")
                .put(FILLED_DEFINING_IDS.get(2), "")
                .put(UNFILLED_DEFINING_IDS.get(0), "")
                .put(UNFILLED_DEFINING_IDS.get(1), "")
                .put(UNFILLED_DEFINING_IDS.get(2), "")
                .build()
            );
        when(context.getRootModel(any(CommonModel.class), anyList()))
            .thenAnswer(i -> ((List<CommonModel>) i.getArgument(1)).stream()
                .filter(m -> m.getId() == 1)
                .findFirst().orElse(null));

        when(context.getSkuParameterNamesWithMode(CATEGORY_ID_WITHOUT_PARAMS, SkuParameterMode.SKU_DEFINING))
            .thenReturn(emptyMap());
    }

    @Test
    public void testAllDefiningFilled() {
        CommonModel guru = createGuruModel(KnownIds.MODEL_QUALITY_OPERATOR);
        CommonModel sku = createModelWithAllDefiningFilled();

        List errors = validator.validate(context, new ModelChanges(sku, sku, ModelChanges.Operation.UPDATE),
            ImmutableList.of(guru, sku));
        assertTrue(errors.isEmpty());
        errors = validator.validate(context, new ModelChanges(sku, sku, ModelChanges.Operation.CREATE),
            ImmutableList.of(guru, sku));
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testNotAllDefiningFilled() {
        CommonModel guru = createGuruModel(KnownIds.MODEL_QUALITY_OPERATOR);
        CommonModel sku = createModelWithNotAllDefiningFilled();

        List errors = validator.validate(context, new ModelChanges(sku, sku, ModelChanges.Operation.UPDATE),
            ImmutableList.of(guru, sku));
        assertTrue(errors.isEmpty());
        errors = validator.validate(context, new ModelChanges(sku, sku, ModelChanges.Operation.CREATE),
            ImmutableList.of(guru, sku));
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testOnlyOneDefiningFilled() {
        CommonModel guru = createGuruModel(KnownIds.MODEL_QUALITY_OPERATOR);
        CommonModel sku = createModelWithOneDefiningFilled();

        List errors = validator.validate(context, new ModelChanges(sku, sku, ModelChanges.Operation.UPDATE),
            ImmutableList.of(guru, sku));
        assertTrue(errors.isEmpty());
        errors = validator.validate(context, new ModelChanges(sku, sku, ModelChanges.Operation.CREATE),
            ImmutableList.of(guru, sku));
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testNoDefiningFilled() {
        CommonModel guru = createGuruModel(KnownIds.MODEL_QUALITY_OPERATOR);
        CommonModel sku = createModelWithNoDefiningFilled();

        List<ModelValidationError> errors = validator.validate(context,
            new ModelChanges(null, sku, ModelChanges.Operation.UPDATE),
            ImmutableList.of(guru, sku));
        assertFalse(errors.isEmpty());
        ModelValidationError error = errors.get(0);
        assertEquals(ModelValidationError.ErrorType.INVALID_PARAMETER_VALUE, error.getType());
        assertEquals(ModelValidationError.ErrorSubtype.NO_DEFINING_PARAM_FILLED_SKU, error.getSubtype());
        assertTrue(error.isCritical());

        errors = validator.validate(context, new ModelChanges(null, sku, ModelChanges.Operation.CREATE),
            ImmutableList.of(guru, sku));
        assertFalse(errors.isEmpty());
        error = errors.get(0);
        assertEquals(ModelValidationError.ErrorType.INVALID_PARAMETER_VALUE, error.getType());
        assertEquals(ModelValidationError.ErrorSubtype.NO_DEFINING_PARAM_FILLED_SKU, error.getSubtype());
        assertTrue(error.isCritical());
    }

    @Test
    public void testNoDefiningFilledOnBothBeforeAndAfterGivesWarning() {
        CommonModel guru = createGuruModel(KnownIds.MODEL_QUALITY_OPERATOR);
        CommonModel sku = createModelWithNoDefiningFilled();

        List<ModelValidationError> errors = validator.validate(context,
            new ModelChanges(sku, sku, ModelChanges.Operation.UPDATE),
            ImmutableList.of(guru, sku));
        assertFalse(errors.isEmpty());
        ModelValidationError error = errors.get(0);
        assertEquals(ModelValidationError.ErrorType.INVALID_PARAMETER_VALUE, error.getType());
        assertEquals(ModelValidationError.ErrorSubtype.NO_DEFINING_PARAM_FILLED_SKU, error.getSubtype());
        assertFalse(error.isCritical());

        errors = validator.validate(context, new ModelChanges(sku, sku, ModelChanges.Operation.CREATE),
            ImmutableList.of(guru, sku));
        assertFalse(errors.isEmpty());
        error = errors.get(0);
        assertEquals(ModelValidationError.ErrorType.INVALID_PARAMETER_VALUE, error.getType());
        assertEquals(ModelValidationError.ErrorSubtype.NO_DEFINING_PARAM_FILLED_SKU, error.getSubtype());
        assertFalse(error.isCritical());
    }

    @Test
    public void testNoDefiningParamsInCategory() {
        CommonModel guru = createGuruModel(KnownIds.MODEL_QUALITY_OPERATOR);
        CommonModel sku = createModelWithoutParams();

        List<ModelValidationError> errors = validator.validate(context,
            new ModelChanges(null, sku, ModelChanges.Operation.CREATE),
            ImmutableList.of(guru, sku));

        ModelValidationError expected = SkuDefiningParamsFilledValidator.createNoDefiningParamInCategoryError(
            sku.getId(), sku.getCategoryId());

        Assertions.assertThat(errors).containsExactlyInAnyOrder(expected);
    }

    @Test
    public void testNoDefiningFilledNotOperator() {
        CommonModel guru = createGuruModel(KnownIds.MODEL_QUALITY_OFFER);
        CommonModel sku = createModelWithNoDefiningFilled();

        List<ModelValidationError> errors = validator.validate(context,
            new ModelChanges(null, sku, ModelChanges.Operation.UPDATE),
            ImmutableList.of(guru, sku));
        assertTrue(errors.isEmpty());

        errors = validator.validate(context, new ModelChanges(null, sku, ModelChanges.Operation.CREATE),
            ImmutableList.of(guru, sku));
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testNoDefiningFilledOnBothBeforeAndAfterNotOperator() {
        CommonModel guru = createGuruModel(KnownIds.MODEL_QUALITY_OFFER);
        CommonModel sku = createModelWithNoDefiningFilled();

        List<ModelValidationError> errors = validator.validate(context,
            new ModelChanges(sku, sku, ModelChanges.Operation.UPDATE),
            ImmutableList.of(guru, sku));
        assertTrue(errors.isEmpty());

        errors = validator.validate(context, new ModelChanges(sku, sku, ModelChanges.Operation.CREATE),
            ImmutableList.of(guru, sku));
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testNoDefiningParamsInCategoryNotOperator() {
        CommonModel guru = createGuruModel(KnownIds.MODEL_QUALITY_OFFER);
        CommonModel sku = createModelWithoutParams();

        List<ModelValidationError> errors = validator.validate(context,
            new ModelChanges(null, sku, ModelChanges.Operation.CREATE),
            ImmutableList.of(guru, sku));

        assertTrue(errors.isEmpty());
    }

    private CommonModel createModelWithAllDefiningFilled() {
        CommonModelBuilder builder = CommonModelBuilder.newBuilder()
            .id(2)
            .category(CATEGORY_ID_WITH_PARAMS)
            .currentType(CommonModel.Source.SKU)
            .modelRelation(1, CATEGORY_ID_WITH_PARAMS, ModelRelation.RelationType.SKU_PARENT_MODEL);
        createAndFillParams(builder, FILLED_DEFINING_IDS);
        createEmptyParams(builder, NON_DEFINING_IDS);
        return builder.getModel();
    }

    private CommonModel createModelWithNotAllDefiningFilled() {
        CommonModelBuilder builder = CommonModelBuilder.newBuilder()
            .id(2)
            .category(CATEGORY_ID_WITH_PARAMS)
            .currentType(CommonModel.Source.SKU)
            .modelRelation(1, CATEGORY_ID_WITH_PARAMS, ModelRelation.RelationType.SKU_PARENT_MODEL);
        createAndFillParams(builder, FILLED_DEFINING_IDS);
        createEmptyParams(builder, NON_DEFINING_IDS);
        createEmptyParams(builder, UNFILLED_DEFINING_IDS);
        return builder.getModel();
    }

    private CommonModel createModelWithOneDefiningFilled() {
        CommonModelBuilder builder = CommonModelBuilder.newBuilder()
            .id(2)
            .category(CATEGORY_ID_WITH_PARAMS)
            .currentType(CommonModel.Source.SKU)
            .modelRelation(1, CATEGORY_ID_WITH_PARAMS, ModelRelation.RelationType.SKU_PARENT_MODEL);
        createAndFillParams(builder, singletonList(FILLED_DEFINING_IDS.get(0)));
        createEmptyParams(builder, NON_DEFINING_IDS);
        createEmptyParams(builder, UNFILLED_DEFINING_IDS);
        return builder.getModel();
    }

    private CommonModel createModelWithNoDefiningFilled() {
        CommonModelBuilder builder = CommonModelBuilder.newBuilder()
            .id(2)
            .category(CATEGORY_ID_WITH_PARAMS)
            .currentType(CommonModel.Source.SKU)
            .modelRelation(1, CATEGORY_ID_WITH_PARAMS, ModelRelation.RelationType.SKU_PARENT_MODEL);
        createEmptyParams(builder, UNFILLED_DEFINING_IDS);
        createEmptyParams(builder, NON_DEFINING_IDS);
        return builder.getModel();
    }

    private CommonModel createModelWithoutParams() {
        CommonModelBuilder builder = CommonModelBuilder.newBuilder()
            .id(2)
            .category(CATEGORY_ID_WITHOUT_PARAMS)
            .currentType(CommonModel.Source.SKU)
            .modelRelation(1, CATEGORY_ID_WITHOUT_PARAMS, ModelRelation.RelationType.SKU_PARENT_MODEL);
        return builder.getModel();
    }

    private CommonModel createGuruModel(Long modelQuality) {
        CommonModelBuilder builder = CommonModelBuilder.newBuilder()
            .id(1)
            .category(CATEGORY_ID_WITH_PARAMS)
            .currentType(CommonModel.Source.GURU)
            .quality(modelQuality)
            .modelRelation(2, CATEGORY_ID_WITH_PARAMS, ModelRelation.RelationType.SKU_MODEL);
        createEmptyParams(builder, NON_DEFINING_IDS);
        createEmptyParams(builder, UNFILLED_DEFINING_IDS);
        return builder.getModel();
    }

    private void createAndFillParams(CommonModelBuilder builder, List<Long> ids) {
        ids.forEach(id ->
            builder.startParameterValue().paramId(id).type(Param.Type.STRING).words("Fur Elise").endParameterValue()
        );
    }

    private void createEmptyParams(CommonModelBuilder builder, List<Long> ids) {
        ids.forEach(id ->
            builder.startParameterValue().paramId(id).type(Param.Type.STRING).endParameterValue()
        );
    }
}
