package ru.yandex.market.mbo.db.modelstorage.validation.processing;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParameterBuilder;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.utils.MboAssertions;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author galaev@yandex-team.ru
 * @since 30/08/2018.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class OutOfBoundsParamValueErrorProcessorTest {
    private OutOfBoundsParamValueErrorProcessor processor;
    private ModelValidationContext context;

    @Before
    public void setUp() throws Exception {
        processor = new OutOfBoundsParamValueErrorProcessor();
        context = null;
    }

    @Test
    public void incorrectErrorType() {
        ModelValidationError error = new ModelValidationError(1L, ModelValidationError.ErrorType.UNKNOWN_ERROR);
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(createModel(1));
        ValidationErrorProcessorResult result = processor.process(context, saveGroup, error);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void incorrectErrorSubtype() {
        ModelValidationError error = new ModelValidationError(1L,
            ModelValidationError.ErrorType.INVALID_PARAMETER_VALUE,
            ModelValidationError.ErrorSubtype.DUPLICATE_PICTURES);
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(createModel(1));
        ValidationErrorProcessorResult result = processor.process(context, saveGroup, error);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUpdatedModels()).isEmpty();
    }

    @Test
    public void modelNotFound() {
        ModelValidationError error = createValidationError();
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(createModel(2));
        ValidationErrorProcessorResult result = processor.process(context, saveGroup, error);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void noXslName() {
        ModelValidationError error = createValidationError();
        error.removeParam(ModelStorage.ErrorParamName.PARAM_XSL_NAME);
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(createModel(2));
        ValidationErrorProcessorResult result = processor.process(context, saveGroup, error);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void noBeforeModel() {
        ModelValidationError error = createValidationError();
        CommonModel model = createModel(1);
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model);
        ValidationErrorProcessorResult result = processor.process(context, saveGroup, error);

        assertThat(result.isSuccess()).isTrue();
        MboAssertions.assertThat(model, "numeric").notExists();
    }

    @Test
    public void noParameterInBeforeModel() {
        ModelValidationError error = createValidationError();
        CommonModel model = createModel(1);
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model);
        CommonModel beforeModel = createModel(1);
        beforeModel.removeAllParameterValues("numeric");
        saveGroup.addBeforeModels(Collections.singleton(beforeModel));
        ValidationErrorProcessorResult result = processor.process(context, saveGroup, error);

        assertThat(result.isSuccess()).isTrue();
        MboAssertions.assertThat(model, "numeric").notExists();
    }

    @Test
    public void withBeforeModel() {
        ModelValidationError error = createValidationError();
        CommonModel model = createModel(1, 10);
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model);
        saveGroup.addBeforeModels(Collections.singleton(createModel(1, 5)));
        ValidationErrorProcessorResult result = processor.process(context, saveGroup, error);

        assertThat(result.isSuccess()).isTrue();
        MboAssertions.assertThat(model, "numeric").valuesInAnyOrder(5, 15);
    }

    private CommonModel createModel(long modelId) {
        return createModel(modelId, 10);
    }

    private CommonModel createModel(long modelId, int numericValue) {
        return CommonModelBuilder.newBuilder()
            .id(modelId)
            .parameters(Collections.singletonList(ParameterBuilder.builder()
                .id(1).xsl("numeric").type(Param.Type.NUMERIC)
                .endParameter()))
            .param("numeric").setNumeric(numericValue)
            .param("numeric").setNumeric(numericValue + 10) // make it multivalue
            .getModel();
    }

    private ModelValidationError createValidationError() {
        return new ModelValidationError(1L, ModelValidationError.ErrorType.INVALID_PARAMETER_VALUE,
            ModelValidationError.ErrorSubtype.OUT_OF_BOUNDS_VALUE, true)
            .addParam(ModelStorage.ErrorParamName.PARAM_XSL_NAME, "numeric");
    }
}
