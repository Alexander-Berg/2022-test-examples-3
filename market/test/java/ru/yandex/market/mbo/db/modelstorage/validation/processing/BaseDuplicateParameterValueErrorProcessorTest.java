package ru.yandex.market.mbo.db.modelstorage.validation.processing;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.db.modelstorage.ModelSaveContext;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.db.modelstorage.validation.UniqueStringParameterValidator;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.utils.MboAssertions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getGuru;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getGuruBuilder;

/**
 * @author danfertev
 * @since 15.03.2018
 */
public abstract class BaseDuplicateParameterValueErrorProcessorTest {

    private static final long FORMER_PARAM_ID = 13L;

    private static final String FORMER_VAL_1 = "123456";
    private static final String FORMER_VAL_2 = "654321";
    private static final String DUPLICATE_VAL = "76543";

    protected DuplicateParameterValueErrorProcessor processor;
    protected ModelValidationContext context;

    public void init(DuplicateParameterValueErrorProcessor processor) {
        this.processor = processor;
        context = mock(ModelValidationContext.class);

        Mockito.when(context.getParamIdByXslName(anyLong(), anyString())).thenReturn(FORMER_PARAM_ID);
    }

    @Test
    public void incorrectErrorType() {
        ModelValidationError error = new ModelValidationError(1L, ModelValidationError.ErrorType.UNKNOWN_ERROR);
        ValidationErrorProcessorResult result = processor.process(context, ModelSaveGroup.fromModels(getGuru()), error);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void baseModelIsNewModel() {
        CommonModel base = getGuru();
        base.setId(0L);
        ModelValidationError error = error(base, 1L, true, true, "");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(base), error);

        Assertions.assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void baseModelNotFound() {
        CommonModel base = getGuru();
        base.setId(2L);
        ModelValidationError error = error(base, 1L, true, true, "");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(base), error);

        Assertions.assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void duplicateModelIdNotFound() {
        CommonModel base = getGuru();
        ModelValidationError error = new ModelValidationError(base.getId(), processor.getErrorType().getType());
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(base), error);

        Assertions.assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void duplicateModelNotFound() {
        CommonModel base = getGuru();
        CommonModel duplicate = getGuruBuilder()
            .id(1L)
            .endModel();
        ModelValidationError error = error(base, duplicate.getId(), true, true, "");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(base), error);

        Assertions.assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void modelsNotChanged() {
        CommonModel base = getGuru();
        CommonModel duplicate = getGuruBuilder()
            .id(1L)
            .endModel();
        ModelValidationError error = error(base, duplicate.getId(), true, true, "");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(base, duplicate), error);

        Assertions.assertThat(result.isSuccess()).isTrue();
        Assertions.assertThat(result.getUpdatedModels()).isEmpty();
    }

    @Test
    public void allDuplicateParameterRemoved() {
        CommonModel base = getGuruBuilder()
            .startParameterValue()
            .paramId(1L).xslName(processor.getParameterName()).type(Param.Type.ENUM)
            .optionId(1)
            .endParameterValue()
            .endModel();
        CommonModel duplicate = getGuruBuilder()
            .id(1L)
            .startParameterValue()
            .paramId(1L).xslName(processor.getParameterName()).type(Param.Type.ENUM)
            .optionId(1)
            .endParameterValue()
            .startParameterValue()
            .paramId(1L).xslName(processor.getParameterName()).type(Param.Type.ENUM)
            .optionId(1)
            .endParameterValue()
            .endModel();
        ModelValidationError error = error(base, duplicate.getId(), true, true, "");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(base, duplicate), error);

        Assertions.assertThat(result.isSuccess()).isTrue();
        assertDuplicateUpdated(result, duplicate);
        MboAssertions.assertThat(duplicate, processor.getParameterName()).notExists();
    }

    @Test
    public void allDuplicateParameterRemovedWrongModel() {
        // @formatter:off
        CommonModel base = getGuruBuilder()
            .startParameterValue()
                .paramId(1L).xslName(processor.getParameterName()).type(Param.Type.ENUM)
                .optionId(1)
            .endParameterValue()
            .endModel();
        CommonModel duplicate = getGuruBuilder()
            .id(1L)
            .startParameterValue()
                .paramId(1L).xslName(processor.getParameterName()).type(Param.Type.ENUM)
                .optionId(1)
            .endParameterValue()
            .startParameterValue()
                .paramId(1L).xslName(processor.getParameterName()).type(Param.Type.ENUM)
                .optionId(1)
            .endParameterValue()
            .startParameterValue()
                .paramId(FORMER_PARAM_ID).xslName(processor.getParameterNameForFormerValues()).type(Param.Type.STRING)
                .words(FORMER_VAL_1)
            .endParameterValue()
            .startParameterValue()
                .paramId(FORMER_PARAM_ID).xslName(processor.getParameterNameForFormerValues()).type(Param.Type.STRING)
                .words(FORMER_VAL_2)
            .endParameterValue()
            .endModel();
        // @formatter:on

        ModelValidationError error = error(base, duplicate.getId(), true, true, DUPLICATE_VAL);
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(base, duplicate), error);

        Assertions.assertThat(result.isSuccess()).isTrue();
        assertDuplicateUpdated(result, duplicate);
        MboAssertions.assertThat(duplicate, processor.getParameterName()).notExists();

        assertThat(duplicate.getSingleParameterValue(FORMER_PARAM_ID).getStringValue()).containsExactlyInAnyOrder(
            WordUtil.defaultWord(FORMER_VAL_1),
            WordUtil.defaultWord(FORMER_VAL_2),
            WordUtil.defaultWord(DUPLICATE_VAL)
        );
    }

    @Test
    public void duplicateParameterRemoved() {
        CommonModel base = getGuruBuilder()
            .startParameterValue()
            .paramId(1L).xslName(processor.getParameterName()).type(Param.Type.ENUM)
            .optionId(1)
            .endParameterValue()
            .endModel();
        CommonModel duplicate = getGuruBuilder()
            .id(1L)
            .startParameterValue()
            .paramId(1L).xslName(processor.getParameterName()).type(Param.Type.ENUM)
            .optionId(1)
            .endParameterValue()
            .startParameterValue()
            .paramId(1L).xslName(processor.getParameterName()).type(Param.Type.ENUM)
            .optionId(2)
            .endParameterValue()
            .endModel();
        ModelValidationError error = error(base, duplicate.getId(), true, true, "");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(base, duplicate), error);

        Assertions.assertThat(result.isSuccess()).isTrue();
        assertDuplicateUpdated(result, duplicate);
        MboAssertions.assertThat(duplicate, processor.getParameterName()).values(2L);
    }

    @Test
    public void allDuplicateStringParameterRemoved() {
        CommonModel base = getGuruBuilder()
            .startParameterValue()
            .paramId(1L).xslName(processor.getParameterName())
            .words("1")
            .endParameterValue()
            .endModel();
        CommonModel duplicate = getGuruBuilder()
            .id(1L)
            .startParameterValue()
            .paramId(1L).xslName(processor.getParameterName())
            .words("1", "1")
            .endParameterValue()
            .endModel();
        ModelValidationError error = error(base, duplicate.getId(), true, true, "");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(base, duplicate), error);

        Assertions.assertThat(result.isSuccess()).isTrue();
        assertDuplicateUpdated(result, duplicate);
        MboAssertions.assertThat(duplicate, processor.getParameterName()).notExists();
    }

    @Test
    public void duplicateStringParameterRemoved() {
        CommonModel base = getGuruBuilder()
            .startParameterValue()
            .paramId(1L).xslName(processor.getParameterName())
            .words("1")
            .endParameterValue()
            .endModel();
        CommonModel duplicate = getGuruBuilder()
            .id(1L)
            .startParameterValue()
            .paramId(1L).xslName(processor.getParameterName())
            .words("1", "2")
            .endParameterValue()
            .endModel();
        ModelValidationError error = error(base, duplicate.getId(), true, true, "");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(base, duplicate), error);

        Assertions.assertThat(result.isSuccess()).isTrue();
        assertDuplicateUpdated(result, duplicate);
        MboAssertions.assertThat(duplicate, processor.getParameterName()).values("2");
    }

    @Test
    public void multipleBaseDuplicateParameterRemoved() {
        CommonModel base = getGuruBuilder()
            .startParameterValue()
            .paramId(1L).xslName(processor.getParameterName()).type(Param.Type.ENUM)
            .optionId(1)
            .endParameterValue()
            .startParameterValue()
            .paramId(1L).xslName(processor.getParameterName()).type(Param.Type.ENUM)
            .optionId(2)
            .endParameterValue()
            .endModel();
        CommonModel duplicate = getGuruBuilder()
            .id(1L)
            .startParameterValue()
            .paramId(1L).xslName(processor.getParameterName()).type(Param.Type.ENUM)
            .optionId(1)
            .endParameterValue()
            .startParameterValue()
            .paramId(1L).xslName(processor.getParameterName()).type(Param.Type.ENUM)
            .optionId(2)
            .endParameterValue()
            .endModel();
        ModelValidationError error = error(base, duplicate.getId(), true, true, "");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(base, duplicate), error);

        Assertions.assertThat(result.isSuccess()).isTrue();
        assertDuplicateUpdated(result, duplicate);
        MboAssertions.assertThat(duplicate, processor.getParameterName()).notExists();
    }

    @Test
    public void multipleStringBaseDuplicateParameterRemoved() {
        CommonModel base = getGuruBuilder()
            .startParameterValue()
            .paramId(1L).xslName(processor.getParameterName())
            .words("1", "2")
            .endParameterValue()
            .endModel();
        CommonModel duplicate = getGuruBuilder()
            .id(1L)
            .startParameterValue()
            .paramId(1L).xslName(processor.getParameterName())
            .words("1", "2")
            .endParameterValue()
            .endModel();
        ModelValidationError error = error(base, duplicate.getId(), true, true, "");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(base, duplicate), error);

        Assertions.assertThat(result.isSuccess()).isTrue();
        assertDuplicateUpdated(result, duplicate);
        MboAssertions.assertThat(duplicate, processor.getParameterName()).notExists();
    }

    @Test
    public void shouldForceRepairable() {
        CommonModel base = getGuruBuilder()
            .endModel();
        CommonModel duplicate = getGuruBuilder()
            .id(1L)
            .endModel();
        ModelValidationError error = error(base, duplicate.getId(), true, true, "")
            .setShouldForce(true);
        boolean isRepairable = processor.isRepairable(new ModelSaveContext(1L), error);

        Assertions.assertThat(isRepairable).isTrue();
    }

    protected ModelValidationError error(CommonModel baseModel, long duplicateModelId, boolean isCritical,
                                         boolean parent, String value) {
        return new ModelValidationError(baseModel, processor.getErrorType().getType(), null, isCritical, isCritical)
            .addParam(ModelStorage.ErrorParamName.PARAM_VALUE, value)
            .addParam(ModelStorage.ErrorParamName.MODEL_ID, duplicateModelId)
            .addParam(ModelStorage.ErrorParamName.MODEL_RELATION_TYPE, parent
                ? UniqueStringParameterValidator.PARENT_RELATION_TYPE
                : UniqueStringParameterValidator.NO_RELATION_TYPE);
    }

    private void assertDuplicateUpdated(ValidationErrorProcessorResult result, CommonModel duplicate) {
        Assertions.assertThat(result.getUpdatedModels().stream().map(ModelChanges::getAfter)).contains(duplicate);
    }
}
