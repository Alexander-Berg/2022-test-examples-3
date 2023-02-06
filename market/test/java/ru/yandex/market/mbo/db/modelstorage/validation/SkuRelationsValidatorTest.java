package ru.yandex.market.mbo.db.modelstorage.validation;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.validation.context.CachingModelValidationContext;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.CATEGORY_ID;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.PARENT_MODEL_ID;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getDefaultSkuBuilder;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getGuru;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getParentWithRelation;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SkuRelationsValidatorTest {

    private SkuRelationsValidator validator;
    private ModelValidationContext context;

    @Before
    public void setup() {
        validator = new SkuRelationsValidator();
        context = mock(CachingModelValidationContext.class);
    }

    @Test
    public void testNoErrorOnGuru() {
        CommonModel guru = getGuru();

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(guru),
            Collections.singletonList(guru)
        );

        assertThat(errors).isEmpty();
    }

    @Test
    public void testDuplicateSkuParentRelation() {
        CommonModel parent = getParentWithRelation(10L);
        CommonModel sku = getDefaultSkuBuilder()
            .id(10L)
            .startModelRelation()
            .id(99L).categoryId(CATEGORY_ID).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .endModel();

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(sku),
            Arrays.asList(parent, sku)
        );

        ModelValidationError expected = new ModelValidationError(
            sku.getId(),
            ModelValidationError.ErrorType.INVALID_RELATION,
            ModelValidationError.ErrorSubtype.DUPLICATE_SKU_PARENT_RELATION,
            true)
            .addLocalizedMessagePattern("Связь с родительской моделью дублируется.")
            .addParam(ModelStorage.ErrorParamName.MODEL_ID, 10L);

        assertThat(errors).containsExactlyInAnyOrder(expected);
    }

    @Test
    public void testSaveWithoutParent() {
        CommonModel sku = getDefaultSkuBuilder().id(10L).endModel();

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(sku),
            Collections.singletonList(sku)
        );

        ModelValidationError expected = new ModelValidationError(
            sku.getId(),
            ModelValidationError.ErrorType.INVALID_RELATION,
            ModelValidationError.ErrorSubtype.MISSING_SKU_PARENT_MODEL, true)
            .addLocalizedMessagePattern("Родительская модель %{MODEL_ID} для SKU не найдена")
            .addParam(ModelStorage.ErrorParamName.MODEL_ID, PARENT_MODEL_ID);

        assertThat(errors).containsExactlyInAnyOrder(expected);
    }

    @Test
    public void testInvalidRelationForGuru() {
        CommonModel sku = getDefaultSkuBuilder()
            .currentType(CommonModel.Source.GURU)
            .id(10L)
            .endModel();
        CommonModel parent = getParentWithRelation(10L);

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(sku),
            Arrays.asList(parent, sku)
        );

        ModelValidationError expected = new ModelValidationError(
            sku.getId(),
            ModelValidationError.ErrorType.INVALID_RELATION,
            ModelValidationError.ErrorSubtype.INVALID_RELATION_FOR_MODEL_TYPE,
            true)
            .addLocalizedMessagePattern("Связь не соответствует типу модели. Тип связи: %{MODEL_RELATION_TYPE}.")
            .addParam(ModelStorage.ErrorParamName.MODEL_RELATION_TYPE,
                ModelRelation.RelationType.SKU_PARENT_MODEL.name());

        assertThat(errors).containsExactlyInAnyOrder(expected);
    }

    private ModelChanges getModelChanges(CommonModel after) {
        return new ModelChanges(after, after, ModelChanges.Operation.UPDATE);
    }
}
