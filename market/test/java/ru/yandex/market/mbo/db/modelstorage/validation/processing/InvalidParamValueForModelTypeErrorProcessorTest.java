package ru.yandex.market.mbo.db.modelstorage.validation.processing;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.db.modelstorage.validation.SkuParametersValidator;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.utils.MboAssertions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getDefaultSkuBuilder;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getGuru;

/**
 * @author danfertev
 * @since 05.04.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class InvalidParamValueForModelTypeErrorProcessorTest {
    private SkuParametersValidator validator;
    private InvalidParamValueForModelTypeErrorProcessor processor;
    private ModelValidationContext context;

    @Before
    public void setUp() throws Exception {
        validator = new SkuParametersValidator();
        processor = new InvalidParamValueForModelTypeErrorProcessor();
        context = mock(ModelValidationContext.class);
        when(context.getSkuParameterNamesWithMode(anyLong(), eq(SkuParameterMode.SKU_NONE)))
            .thenReturn(ImmutableMap.of(
                KnownIds.NAME_PARAM_ID, XslNames.NAME,
                KnownIds.VENDOR_PARAM_ID, XslNames.VENDOR,
                2L, "param2",
                3L, "param3"
            ));
        when(context.getReadableParameterName(anyLong(), anyLong()))
            .thenReturn("param2");
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
            ModelValidationError.ErrorType.INVALID_PARAMETER_VALUE,
            ModelValidationError.ErrorSubtype.MISSING_ID);
        ValidationErrorProcessorResult result = processor.process(context, ModelSaveGroup.fromModels(getGuru()), error);

        assertThat(result.isSuccess()).isTrue();
        Assertions.assertThat(result.getUpdatedModels()).isEmpty();
    }

    @Test
    public void modelNotFound() {
        CommonModel sku = getDefaultSkuBuilder()
            .id(333L)
            .endModel();
        CommonModel sku2 = getDefaultSkuBuilder()
            .id(1L)
            .endModel();
        ModelValidationError error = validator.createInvalidParamForModelTypeError(
            context, sku2, null, 2L, "param2", "");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(sku), error);

        Assertions.assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void noInvalidParam() {
        CommonModel sku = getDefaultSkuBuilder()
            .id(1L)
            .param(1L).setNumeric(1)
            .endModel();
        ModelValidationError error = validator.createInvalidParamForModelTypeError(
            context, sku, null, 2L, "param2", "");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(sku), error);

        Assertions.assertThat(result.isSuccess()).isTrue();
        Assertions.assertThat(result.getUpdatedModels()).isEmpty();
    }

    @Test
    public void removeInvalidParam() {
        CommonModel sku = getDefaultSkuBuilder()
            .id(1L)
            .param(2L).setString("2")
            .endModel();
        ModelValidationError error = validator.createInvalidParamForModelTypeError(
            context, sku, null, 2L, "param2", "");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(sku), error);

        Assertions.assertThat(result.isSuccess()).isTrue();
        MboAssertions.assertThat(sku, 2L).notExists();
    }

    @Test
    public void removeInvalidHypothesis() {
        CommonModel sku = getDefaultSkuBuilder()
            .id(1L)
            .parameterValueHypothesis(2L, "param2", Param.Type.ENUM, "2")
            .endModel();
        ModelValidationError error = validator.createInvalidParamForModelTypeError(
            context, sku, null, 2L, "param2", "");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(sku), error);

        Assertions.assertThat(result.isSuccess()).isTrue();
        MboAssertions.assertThat(sku, 2L).notExists();
    }

    @Test
    public void removeMultivalueInvalidParam() {
        CommonModel sku = getDefaultSkuBuilder()
            .id(1L)
            .startParameterValue()
                .paramId(3L).optionId(31)
            .endParameterValue()
            .startParameterValue()
                .paramId(3L).optionId(32)
            .endParameterValue()
            .endModel();
        ModelValidationError error = validator.createInvalidParamForModelTypeError(
            context, sku, null, 2L, "param2", "");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(sku), error);

        Assertions.assertThat(result.isSuccess()).isTrue();
        MboAssertions.assertThat(sku, 3L).notExists();
    }

    @Test
    public void removeMultipleInvalidParam() {
        CommonModel sku = getDefaultSkuBuilder()
            .id(1L)
            .param(2L).setString("21")
            .param(3L).setOption(31)
            .endModel();
        ModelValidationError error = validator.createInvalidParamForModelTypeError(
            context, sku, null, 2L, "param2", "");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(sku), error);

        Assertions.assertThat(result.isSuccess()).isTrue();
        MboAssertions.assertThat(sku, 2L).notExists();
        MboAssertions.assertThat(sku, 3L).notExists();
    }

    @Test
    public void removeOnlyInvalidParam() {
        CommonModel sku = getDefaultSkuBuilder()
            .id(1L)
            .param(1L).setNumeric(11)
            .param(3L).setOption(31)
            .endModel();
        ModelValidationError error = validator.createInvalidParamForModelTypeError(
            context, sku, null, 2L, "param2", "");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(sku), error);

        Assertions.assertThat(result.isSuccess()).isTrue();
        MboAssertions.assertThat(sku, 1L).valuesInAnyOrder(11);
        MboAssertions.assertThat(sku, 3L).notExists();
    }

    @Test
    public void multipleValidationErrorAllValueRemovedByTheFirstProcessor() {
        CommonModel sku = getDefaultSkuBuilder()
            .id(1L)
            .param(1L).setNumeric(11)
            .param(2L).setString("21")
            .param(3L).setOption(31)
            .endModel();
        ModelValidationError error1 = validator.createInvalidParamForModelTypeError(
            context, sku, null, 2L, "param2", "");
        ModelValidationError error2 = validator.createInvalidParamForModelTypeError(
            context, sku, null, 3L, "param3", "");

        ValidationErrorProcessorResult result1 = processor.process(
            context, ModelSaveGroup.fromModels(sku), error1);

        Assertions.assertThat(result1.isSuccess()).isTrue();
        MboAssertions.assertThat(sku, 1L).valuesInAnyOrder(11);
        MboAssertions.assertThat(sku, 2L).notExists();
        MboAssertions.assertThat(sku, 3L).notExists();

        ValidationErrorProcessorResult result2 = processor.process(
            context, ModelSaveGroup.fromModels(sku), error2);

        Assertions.assertThat(result2.isSuccess()).isTrue();
        Assertions.assertThat(result2.getUpdatedModels()).isEmpty();
    }
}
