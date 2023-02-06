package ru.yandex.market.mbo.db.modelstorage.validation.processing;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.db.modelstorage.validation.SkuHasParamValueAliasValidator;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.EnumValueAlias;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getDefaultSkuBuilder;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getGuru;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getGuruBuilder;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getSkuBuilder;

/**
 * @author danfertev
 * @since 05.04.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SkuHasParamValueAliasErrorProcessorTest {
    private SkuHasParamValueAliasValidator validator;
    private SkuHasParamValueAliasErrorProcessor processor;
    private ModelValidationContext context;
    private CommonModel parent;

    @Before
    public void setUp() throws Exception {
        validator = new SkuHasParamValueAliasValidator();
        processor = new SkuHasParamValueAliasErrorProcessor();
        context = mock(ModelValidationContext.class);

        parent = getGuruBuilder()
            .enumAlias(3L)
                .xslName("param3").optionId(1L).aliasOptionId(11L)
            .end()
            .enumAlias(3L)
                .xslName("param3").optionId(1L).aliasOptionId(12L)
            .end()
            .enumAlias(3L)
                .xslName("param3").optionId(2L).aliasOptionId(22L)
            .end()
            .endModel();

        when(context.getReadableParameterName(anyLong(), anyLong()))
            .thenReturn("param3");
    }

    @Test
    public void incorrectErrorType() {
        ModelValidationError error = new ModelValidationError(1L, ModelValidationError.ErrorType.UNKNOWN_ERROR);
        ValidationErrorProcessorResult result = processor.process(context, ModelSaveGroup.fromModels(getGuru()), error);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void incorrectErrorSubtype() {
        ModelValidationError error = new ModelValidationError(1L,
            ModelValidationError.ErrorType.SKU_HAS_DEPENDENT_DATA,
            ModelValidationError.ErrorSubtype.MISSING_ID);
        ValidationErrorProcessorResult result = processor.process(context, ModelSaveGroup.fromModels(getGuru()), error);

        assertThat(result.isSuccess()).isTrue();
        Assertions.assertThat(result.getUpdatedModels()).isEmpty();
    }

    @Test
    public void skuModelNotFound() {
        CommonModel sku = getDefaultSkuBuilder()
            .id(1L)
            .deleted(true)
            .endModel();
        CommonModel sku2 = getDefaultSkuBuilder()
            .id(2L)
            .endModel();
        ModelValidationError error = validator.createError(context, sku2, 3L, "param3",
            11L, "11");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(parent, sku), error);

        Assertions.assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void parentModelIdNotFound() {
        CommonModel sku = getDefaultSkuBuilder()
            .id(1L)
            .deleted(true)
            .endModel();
        sku.setRelations(Collections.emptyList());
        ModelValidationError error = validator.createError(context, sku, 3L, "param3",
            11L, "11");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(parent, sku), error);

        Assertions.assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void parentModelNotFound() {
        CommonModel sku = getSkuBuilder(333L)
            .id(1L)
            .deleted(true)
            .endModel();
        ModelValidationError error = validator.createError(context, sku, 3L, "param3",
            11L, "11");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(parent, sku), error);

        Assertions.assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void aliasNotFound() {
        CommonModel sku = getDefaultSkuBuilder()
            .id(1L)
            .deleted(true)
            .endModel();
        ModelValidationError error = validator.createError(context, sku, 3L, "param3",
            33L, "33");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(parent, sku), error);

        Assertions.assertThat(result.isSuccess()).isTrue();
        Assertions.assertThat(result.getUpdatedModels()).isEmpty();
    }

    @Test
    public void aliasRemoved() {
        CommonModel sku = getDefaultSkuBuilder()
            .id(1L)
            .deleted(true)
            .endModel();
        ModelValidationError error = validator.createError(context, sku, 3L, "param3",
            11L, "11");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(parent, sku), error);

        Assertions.assertThat(result.isSuccess()).isTrue();
        Assertions.assertThat(result.getUpdatedModels().stream().map(ModelChanges::getAfter))
            .containsExactlyInAnyOrder(parent);
        Assertions.assertThat(parent.getEnumValueAliases().stream().map(EnumValueAlias::getAliasOptionId))
            .containsExactlyInAnyOrder(12L, 22L);
    }
}
