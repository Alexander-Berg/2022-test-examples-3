package ru.yandex.market.mbo.db.modelstorage.validation;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.validation.context.CachingModelValidationContext;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getDefaultSkuBuilder;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getGuruBuilder;

/**
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class IsSkuValidatorTest {
    private IsSkuValueValidator validator;
    private ModelValidationContext context;

    @Before
    public void setup() {
        validator = new IsSkuValueValidator();
        context = mock(CachingModelValidationContext.class);
        when(context.getReadableParameterName(anyLong(), anyLong()))
            .thenReturn("Модель является SKU");
    }

    @Test
    public void testValidationNotRunOnSku() {
        CommonModel sku = getDefaultSkuBuilder().getModel();

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(sku),
            Collections.singletonList(sku)
        );

        assertTrue(errors.isEmpty());
    }

    @Test
    public void testEmptyModelIsSku() {
        CommonModel model = getGuruBuilder()
            .param(XslNames.IS_SKU).setBoolean(true)
            .endModel();

        when(context.getValidModifications(anyLong(), anyList()))
            .thenReturn(Collections.emptyList());
        when(context.getRelatedModelsForModels(anyLong(), anyList(), anyCollection(), anyCollection(), anyBoolean()))
            .thenReturn(new RelatedModelsContainer(Collections.emptyList(), Collections.emptyList(),
                ModelRelation.RelationType.SKU_MODEL));

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(model),
            Collections.singletonList(model)
        );

        assertEquals(0, errors.size());
    }

    @Test
    public void testModelWithModificationsIsSku() {
        CommonModel model = getGuruBuilder()
            .param(XslNames.IS_SKU).setBoolean(true)
            .endModel();

        when(context.getValidModifications(anyLong(), anyList()))
            .thenReturn(Collections.singletonList(getGuruBuilder().endModel()));

        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(model),
            Collections.singletonList(model)
        );

        assertEquals(1, errors.size());

        ModelValidationError expected = new ModelValidationError(
            model.getId(),
            ModelValidationError.ErrorType.INVALID_PARAMETER_VALUE,
            ModelValidationError.ErrorSubtype.INVALID_IS_SKU_MODEL,
            true)
            .addLocalizedMessagePattern(
                "Модель с модификациями имеет выставленный флаг '%{PARAMETER_NAME}'(%{PARAM_ID}).")
            .addParam(ModelStorage.ErrorParamName.PARAM_ID, "5")
            .addParam(ModelStorage.ErrorParamName.PARAMETER_NAME, "Модель является SKU")
            .addParam(ModelStorage.ErrorParamName.PARAM_XSL_NAME, XslNames.IS_SKU);

        assertEquals(expected, errors.get(0));
    }

    @Test
    public void testModelWithSkusIsSku() {
        CommonModel model = getGuruBuilder()
            .param(XslNames.IS_SKU).setBoolean(true)
            .endModel();

        when(context.getValidModifications(anyLong(), anyList()))
            .thenReturn(Collections.emptyList());
        when(context.getRelatedModelsForModels(anyLong(), anyList(), anyCollection(), anyCollection(), anyBoolean()))
            .thenReturn(new RelatedModelsContainer(
                Collections.singletonList(getDefaultSkuBuilder().endModel()), Collections.emptyList(),
                    ModelRelation.RelationType.SKU_MODEL));


        List<ModelValidationError> errors = validator.validate(
            context,
            getModelChanges(model),
            Collections.singletonList(model)
        );

        assertEquals(1, errors.size());

        ModelValidationError expected = new ModelValidationError(
            model.getId(),
            ModelValidationError.ErrorType.INVALID_PARAMETER_VALUE,
            ModelValidationError.ErrorSubtype.INVALID_IS_SKU_MODEL,
            true, true)
            .addLocalizedMessagePattern("Модель с SKU имеет выставленный флаг '%{PARAMETER_NAME}'(%{PARAM_ID}). " +
                "После завершения трансформаций не забудьте удалить ненужные SKU.")
            .addParam(ModelStorage.ErrorParamName.PARAM_ID, "5")
            .addParam(ModelStorage.ErrorParamName.PARAMETER_NAME, "Модель является SKU")
            .addParam(ModelStorage.ErrorParamName.PARAM_XSL_NAME, XslNames.IS_SKU);

        assertEquals(expected, errors.get(0));

        when(context.isForcedIsSku()).thenReturn(true);

        errors = validator.validate(
            context,
            getModelChanges(model),
            Collections.singletonList(model)
        );

        assertEquals(0, errors.size());
    }

    private ModelChanges getModelChanges(CommonModel after) {
        return new ModelChanges(after, after, ModelChanges.Operation.UPDATE);
    }
}
