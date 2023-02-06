package ru.yandex.market.mbo.db.modelstorage.validation.processing;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Collection;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author abutusov
 * @since 13.06.2019
 */
public class DuplicateValuesXslNameErrorProcessorTest {

    private static final long MODEL_ID = 1L;
    private static final long CATEGORY_ID = 2L;
    private static final long PARAM_ID_1 = 11L;
    private static final long PARAM_ID_2 = 12L;
    private static final String PARAM_XSL_NAME = "Test";
    private static final Param.Type TYPE = Param.Type.NUMERIC;


    private DuplicateValuesXslNameErrorProcessor processor;
    private ModelValidationContext context;

    @Before
    public void setup() {
        processor = new DuplicateValuesXslNameErrorProcessor();
        context = mock(ModelValidationContext.class);
    }

    @Test
    public void incorrectErrorType() {
        ModelValidationError error = new ModelValidationError(1L, ModelValidationError.ErrorType.UNKNOWN_ERROR);
        ValidationErrorProcessorResult result = processor
            .process(context, ModelSaveGroup.fromModels(getModel()), error);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void modelNotFound() {
        ModelValidationError error = error(0L, 2L);
        ValidationErrorProcessorResult result = processor
            .process(context, ModelSaveGroup.fromModels(getModel()), error);

        Assertions.assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void paramValuesNotFound() {
        CommonModel model = getModel();

        ModelValidationError error = error(model.getId(), 2L);
        ValidationErrorProcessorResult result = processor.process(context, ModelSaveGroup.fromModels(model), error);

        Assertions.assertThat(result.isSuccess()).isTrue();
        Assertions.assertThat(result.getUpdatedModels().isEmpty()).isTrue();
    }

    @Test
    public void duplicatedParamValueNotFound() {
        ParameterValues duplicatedParam = new ParameterValues(PARAM_ID_1, PARAM_XSL_NAME, TYPE);
        CommonModel model = getModel(duplicatedParam);

        ModelValidationError error = error(model.getId(), duplicatedParam.getParamId());
        ValidationErrorProcessorResult result = processor.process(context, ModelSaveGroup.fromModels(model), error);

        Assertions.assertThat(result.isSuccess()).isTrue();
        Assertions.assertThat(result.getUpdatedModels().isEmpty()).isTrue();
    }

    @Test
    public void duplicatedParamValuesRemove() {
        ParameterValues duplicatedParams = new ParameterValues(PARAM_ID_2, PARAM_XSL_NAME, TYPE);
        ParameterValue duplicatedParam = new ParameterValue(PARAM_ID_2, PARAM_XSL_NAME, TYPE);
        duplicatedParams.addValue(duplicatedParam);
        CommonModel model = getModel(duplicatedParams);

        ModelValidationError error = error(model.getId(), duplicatedParam.getParamId());
        ValidationErrorProcessorResult result = processor.process(context, ModelSaveGroup.fromModels(model), error);

        Assertions.assertThat(result.isSuccess()).isTrue();
        Assertions.assertThat(result.getUpdatedModels().isEmpty()).isFalse();
        assertModelUpdated(result, duplicatedParam);
    }

    private ModelValidationError error(long modelId, long paramIdForRemove) {
        return new ModelValidationError(modelId, processor.getErrorType().getType(),
            processor.getErrorType().getSubtype(), true, false)
            .addParam(ModelStorage.ErrorParamName.PARAM_XSL_NAME, PARAM_XSL_NAME)
            .addParam(ModelStorage.ErrorParamName.PARAM_ID, 1L)
            .addParam(ModelStorage.ErrorParamName.PARAM_ID_FOR_REMOVE, paramIdForRemove);
    }

    private void assertModelUpdated(ValidationErrorProcessorResult result, ParameterValue duplicate) {
        Assertions
            .assertThat(result.getUpdatedModels().stream()
                .map(ModelChanges::getAfter)
                .map(CommonModel::getParameterValues)
                .flatMap(Collection::stream)
                .map(ParameterValues::getValues)
                .flatMap(Collection::stream))
            .doesNotContain(duplicate);
    }

    private CommonModel getModel(ParameterValues... values) {
        CommonModel model = CommonModelBuilder.newBuilder(MODEL_ID, CATEGORY_ID).getModel();
        Stream.of(values).forEach(model::putParameterValues);
        return model;
    }
}
