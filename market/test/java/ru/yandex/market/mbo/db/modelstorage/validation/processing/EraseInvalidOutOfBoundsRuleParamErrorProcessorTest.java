package ru.yandex.market.mbo.db.modelstorage.validation.processing;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.ModelSaveContext;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationContextStub;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParameterBuilder;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.utils.MboAssertions;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError.ErrorSubtype.OUT_OF_BOUNDS_VALUE;
import static ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError.ErrorType.INVALID_PARAMETER_VALUE;

@SuppressWarnings("checkstyle:MagicNumber")
public class EraseInvalidOutOfBoundsRuleParamErrorProcessorTest {

    private static final long UID = 243L;

    private static final long MODEL_ID = 1L;
    private static final long PARAM_ID = 11L;
    private static final String PARAM_NAME = "numeric";

    private static final int PARAM_VALUE_1 = 5;
    private static final int PARAM_VALUE_2 = 21;
    private static final int PARAM_RULE_VALUE_1 = 3;
    private static final int PARAM_RULE_VALUE_2 = 16;

    private ModelSaveContext saveContext;
    private ModelValidationContext validationContext;
    private ValidationErrorProcessingService errorProcessingService;

    @Before
    public void setUp() throws Exception {
        saveContext = new ModelSaveContext(UID);
        validationContext = new ModelValidationContextStub(null);
        errorProcessingService = new ValidationErrorProcessingService(
            Collections.singletonList(new EraseInvalidOutOfBoundsRuleParamErrorProcessor()));
    }

    @Test
    public void withBeforeModelWithEraseAndForce() {
        CommonModel model = createModel();
        ValidationErrorProcessorResult result = process(model, createBeforeModel(), true, true);

        assertThat(result.isSuccess()).isTrue();
        MboAssertions.assertThat(model, PARAM_NAME).valuesInAnyOrder(PARAM_VALUE_1, PARAM_VALUE_2);
    }

    @Test
    public void withBeforeModelWithoutErase() {
        CommonModel model = createModel();
        ValidationErrorProcessorResult result = process(model, createBeforeModel(), false, true);

        assertThat(result.isSuccess()).isFalse();
        MboAssertions.assertThat(model, PARAM_NAME)
            .valuesInAnyOrder(PARAM_VALUE_1, PARAM_VALUE_2, PARAM_RULE_VALUE_1, PARAM_RULE_VALUE_2);
    }

    @Test
    public void withBeforeModelWithoutForce() {
        CommonModel model = createModel();
        ValidationErrorProcessorResult result = process(model, createBeforeModel(), true, false);

        assertThat(result.isSuccess()).isTrue();
        MboAssertions.assertThat(model, PARAM_NAME).valuesInAnyOrder(PARAM_VALUE_1, PARAM_VALUE_2);
    }

    @Test
    public void withBeforeModel() {
        CommonModel model = createModel();
        ValidationErrorProcessorResult result = process(model, createBeforeModel(), false, false);

        assertThat(result.isSuccess()).isFalse();
        MboAssertions.assertThat(model, PARAM_NAME)
            .valuesInAnyOrder(PARAM_VALUE_1, PARAM_VALUE_2, PARAM_RULE_VALUE_1, PARAM_RULE_VALUE_2);
    }

    private ValidationErrorProcessorResult process(CommonModel model, CommonModel beforeModel,
                                                   boolean eraseInvalidRuleParams, boolean allowForce) {
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model);
        saveGroup.addBeforeModels(Collections.singleton(beforeModel));

        ModelValidationError error = createValidationError(allowForce);
        saveContext.setEraseInvalidRuleParams(eraseInvalidRuleParams);

        return errorProcessingService.process(saveContext, validationContext, saveGroup, error);
    }

    private CommonModel createBeforeModel() {
        return CommonModelBuilder.newBuilder()
            .id(MODEL_ID)
            .parameters(Collections.singletonList(ParameterBuilder.builder()
                .id(PARAM_ID).xsl(PARAM_NAME).type(Param.Type.NUMERIC)
                .endParameter()))
            .param(PARAM_NAME).setNumeric(PARAM_RULE_VALUE_1).modificationSource(ModificationSource.RULE)
            .param(PARAM_NAME).setNumeric(PARAM_VALUE_1)
            .param(PARAM_NAME).setNumeric(PARAM_VALUE_2)
            .getModel();
    }

    private CommonModel createModel() {
        return CommonModelBuilder.newBuilder()
            .id(MODEL_ID)
            .parameters(Collections.singletonList(ParameterBuilder.builder()
                .id(PARAM_ID).xsl(PARAM_NAME).type(Param.Type.NUMERIC)
                .endParameter()))
            .param(PARAM_NAME).setNumeric(PARAM_RULE_VALUE_1).modificationSource(ModificationSource.RULE)
            .param(PARAM_NAME).setNumeric(PARAM_VALUE_1)
            .param(PARAM_NAME).setNumeric(PARAM_RULE_VALUE_2).modificationSource(ModificationSource.RULE)
            .param(PARAM_NAME).setNumeric(PARAM_VALUE_2)
            .getModel();
    }

    private ModelValidationError createValidationError(boolean allowForce) {
        return new ModelValidationError(MODEL_ID, INVALID_PARAMETER_VALUE, OUT_OF_BOUNDS_VALUE, true, allowForce)
            .addParam(ModelStorage.ErrorParamName.PARAM_XSL_NAME, PARAM_NAME)
            .addParam(ModelStorage.ErrorParamName.PARAM_ID, PARAM_ID);
    }
}
