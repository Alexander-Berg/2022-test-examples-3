package ru.yandex.market.mbo.db.modelstorage.validation;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author abutusov
 * @since 13.06.2019
 */
public class ParameterXslNameValidatorTest {

    private static final long PARAM_ID_1 = 1L;
    private static final long PARAM_ID_2 = 2L;
    private static final String PARAM_XSL_NAME_1 = "Test1";
    private static final String PARAM_XSL_NAME_2 = "Test2";
    private static final Param.Type TYPE = Param.Type.NUMERIC;

    private ParameterXslNameValidator parameterXslNameValidator;
    private ModelValidationContext context;

    @Before
    public void setUp() throws Exception {
        parameterXslNameValidator = new ParameterXslNameValidator();
        context = mock(ModelValidationContext.class);
    }

    @Test
    public void providedValidValue() {
        ParameterValue paramValue1 = new ParameterValue(PARAM_ID_1, PARAM_XSL_NAME_1, TYPE);
        ParameterValue paramValue2 = new ParameterValue(PARAM_ID_2, PARAM_XSL_NAME_2, TYPE);

        CommonModel model = createModel(paramValue1, paramValue2);

        List<ModelValidationError> errors = parameterXslNameValidator.validate(context, new ModelChanges(model),
            Collections.singletonList(model));

        assertThat(errors).isEmpty();
    }

    @Test
    public void providedDuplicateParamIdValueWithDifferentXslName() {
        ParameterValue paramValue1 = new ParameterValue(PARAM_ID_1, PARAM_XSL_NAME_1, TYPE);
        ParameterValue paramValue2 = new ParameterValue(PARAM_ID_1, PARAM_XSL_NAME_2, TYPE);
        ParameterValues paramValues = new ParameterValues(PARAM_ID_1, PARAM_XSL_NAME_1, TYPE);
        paramValues.addValue(paramValue1);
        paramValues.addValue(paramValue2);

        CommonModel model = createModel();
        model.putParameterValues(paramValues);

        List<ModelValidationError> errors = parameterXslNameValidator.validate(context, new ModelChanges(model),
            Collections.singletonList(model));

        assertThat(errors).isEmpty();
    }

    @Test
    public void providedDuplicateValueXslName() {
        ParameterValue paramValue1 = new ParameterValue(PARAM_ID_1, PARAM_XSL_NAME_1, TYPE);
        ParameterValue paramValue2 = new ParameterValue(PARAM_ID_2, PARAM_XSL_NAME_1, TYPE);
        CommonModel model = createModel(paramValue1, paramValue2);

        when(context.parameterMatchesDefinition(model.getCategoryId(), paramValue1)).thenReturn(true);

        List<ModelValidationError> errors = parameterXslNameValidator.validate(context, new ModelChanges(model),
            Collections.singletonList(model));

        assertThat(errors).containsExactlyInAnyOrder(ParameterXslNameValidator
            .createDuplicateValuesXslNameError(model, PARAM_XSL_NAME_1, PARAM_ID_1, PARAM_ID_2));
    }

    private CommonModel createModel(ParameterValue... params) {
        CommonModel model = CommonModelBuilder.newBuilder()
            .id(1L)
            .currentType(CommonModel.Source.GURU)
            .category(2L)
            .getModel();
        for (ParameterValue value : params) {
            model.addParameterValue(value);
        }
        return model;
    }
}
