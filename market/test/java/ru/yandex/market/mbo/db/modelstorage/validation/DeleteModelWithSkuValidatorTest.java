package ru.yandex.market.mbo.db.modelstorage.validation;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.validation.context.CachingModelValidationContext;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getGuruBuilder;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getSkuBuilder;

/**
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class DeleteModelWithSkuValidatorTest extends BaseValidatorTestClass {
    private DeleteModelWithSkuValidator validator;
    private ModelValidationContext context;

    @Before
    @Override
    public void before() {
        super.before();
        validator = new DeleteModelWithSkuValidator();
        context = mock(CachingModelValidationContext.class);
    }

    @Test
    public void testIsSkuModelDelete() {
        when(context.getRelatedModelsForModels(anyLong(), anyList(), anyCollection(), anyCollection(), anyBoolean()))
            .thenReturn(new RelatedModelsContainer());

        CommonModel guru = getGuruBuilder()
            .startParameterValue()
            .paramId(1L)
            .xslName(XslNames.IS_SKU)
            .booleanValue(true, 2L)
            .endParameterValue()
            .endModel();
        storage.saveModels(ModelSaveGroup.fromModels(guru), saveContext);
        guru.setDeleted(true);

        List<ModelValidationError> errors = validator.validate(
            context,
            modelChanges(guru),
            Collections.singletonList(guru)
        );

        // There is no validation for isSkuModel
        assertThat(errors).isEmpty();
    }

    @Test
    public void testModelWithSkuDelete() {
        CommonModel guru = getGuruBuilder()
            .startParameterValue()
            .paramId(1L)
            .xslName(XslNames.IS_SKU)
            .booleanValue(false, 2L)
            .endParameterValue()
            .endModel();
        CommonModel sku1 = getSkuBuilder(guru.getId()).endModel();
        CommonModel sku2 = getSkuBuilder(guru.getId()).endModel();
        RelatedModelsContainer container = new RelatedModelsContainer();
        container.addModelFromSaveRequest(sku1, ModelRelation.RelationType.SKU_MODEL);
        container.addModelFromStorage(sku2, ModelRelation.RelationType.SKU_MODEL);
        when(context.getRelatedModelsForModels(anyLong(), anyList(), anyCollection(), anyCollection(), anyBoolean()))
            .thenReturn(container);

        storage.saveModels(ModelSaveGroup.fromModels(guru, sku2), saveContext);
        guru.setDeleted(true);

        List<ModelValidationError> errors = validator.validate(
            context,
            modelChanges(guru),
            Arrays.asList(guru, sku1)
        );

        assertThat(errors).containsExactlyInAnyOrder(
            modelHasSkuError(guru.getId(), true, sku1.getId()),
            modelHasSkuError(guru.getId(), true, sku2.getId())
        );
    }

    @Test
    public void testModelWithModificationIsSKU() {
        CommonModel guru = getGuruBuilder()
            .startParameterValue()
            .paramId(1L)
            .xslName(XslNames.IS_SKU)
            .booleanValue(false, 2L)
            .endParameterValue()
            .endModel();
        CommonModel modification1 = getGuruBuilder()
            .parentModel(guru)
            .startParameterValue()
            .paramId(1L)
            .xslName(XslNames.IS_SKU)
            .booleanValue(true, 1L)
            .endParameterValue()
            .endModel();
        CommonModel modification2 = getGuruBuilder()
            .parentModel(guru)
            .endModel();

        RelatedModelsContainer container = new RelatedModelsContainer();
        when(context.getRelatedModelsForModels(anyLong(), anyList(), anyCollection(), anyCollection(), anyBoolean()))
            .thenReturn(container);
        when(context.getValidModifications(anyLong(), anyCollection()))
            .thenReturn(Arrays.asList(modification1, modification2));

        guru.setDeleted(true);

        List<ModelValidationError> errors = validator.validate(
            context,
            modelChanges(guru),
            Collections.singletonList(guru)
        );

        assertThat(errors).containsExactlyInAnyOrder(
            modelHasSkuError(guru.getId(), true, modification1.getId())
        );
    }

    private ModelValidationError modelHasSkuError(long modelId, boolean critical, long skuId) {
        return new ModelValidationError(
            modelId,
            ModelValidationError.ErrorType.MODEL_HAS_SKU,
            critical)
            .addLocalizedMessagePattern("Модель не может быть удалена поскольку в ней настроен SKU %{MODEL_ID}. " +
                "Сначала необходимо удалить связанный SKU")
            .addParam(ModelStorage.ErrorParamName.MODEL_ID, skuId);
    }
}
