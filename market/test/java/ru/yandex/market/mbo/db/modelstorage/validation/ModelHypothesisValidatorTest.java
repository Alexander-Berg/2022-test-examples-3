package ru.yandex.market.mbo.db.modelstorage.validation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValueHypothesis;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.visual.Word;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getGuruBuilder;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class ModelHypothesisValidatorTest extends BaseValidatorTestClass {
    private ModelHypothesisValidator validator;
    private CommonModel guruModel;
    private ModelValidationContext context;

    @Before
    public void setUp() {
        this.validator = new ModelHypothesisValidator();
        this.guruModel = getGuruBuilder()
            .id(1)
            .endModel();
        this.context = mock(ModelValidationContext.class);
        when(context.getParameterProperties(anyLong(), anyLong())).thenReturn(new ParameterProperties(false));
    }

    @Test
    public void okCase() {
        assertEquals(Collections.emptyList(), validator.validate(context, null, Collections.singletonList(guruModel)));
    }


    @Test
    public void errorIfparametersHasHypothesisToo() {
        List<ParameterValueHypothesis> hypotheses = guruModel.getParameterValues().stream()
            .map(pv -> {
                Word word = new Word(255, pv.getParamId() + "");
                return new ParameterValueHypothesis(pv, Collections.singletonList(word));
            })
            .collect(Collectors.toList());
        guruModel.setParameterValueHypotheses(hypotheses);
        assertEquals(guruModel.getParameterValues().size(),
            validator.validate(context, null, Collections.singletonList(guruModel)).size());
    }

    @Test
    public void modelHasHypothesisWithoutMainValues() {
        Word word = new Word(255, "01212121");
        ParameterValueHypothesis hypotheses = new ParameterValueHypothesis(12121L, "xsl-name", Param.Type.ENUM,
            Collections.singletonList(word), null);
        guruModel.setParameterValueHypotheses(Collections.singletonList(hypotheses));
        assertEquals(Collections.emptyList(), validator.validate(context, null, Collections.singletonList(guruModel)));
    }

    @Test
    public void notEnumParameterHasHypothesis() {
        Word word = new Word(255, "01212121");
        ParameterValueHypothesis hypotheses = new ParameterValueHypothesis(12121L, "xsl-name", Param.Type.NUMERIC,
            Collections.singletonList(word), null);
        guruModel.setParameterValueHypotheses(Collections.singletonList(hypotheses));
        List<ModelValidationError> result = validator.validate(context, null, Collections.singletonList(guruModel));
        assertEquals(1, result.size());
    }

    @Test
    public void dontValidateHypothesisConflictOnMultivalueParameter() {
        List<ParameterValueHypothesis> hypotheses = guruModel.getParameterValues().stream()
            .map(pv -> {
                Word word = new Word(255, pv.getParamId() + "");
                return new ParameterValueHypothesis(pv, Collections.singletonList(word));
            })
            .collect(Collectors.toList());
        guruModel.setParameterValueHypotheses(hypotheses);
        when(context.getParameterProperties(anyLong(), anyLong()))
            .thenReturn(new ParameterProperties(true));
        List<ModelValidationError> result = validator.validate(context, null, Collections.singletonList(guruModel));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void evenMultivalueParameterShouldHaveOnlyEnumHypothesis() {
        Word word = new Word(255, "01212121");
        ParameterValueHypothesis hypotheses = new ParameterValueHypothesis(12121L, "xsl-name", Param.Type.NUMERIC,
            Collections.singletonList(word), null);
        guruModel.setParameterValueHypotheses(Collections.singletonList(hypotheses));
        List<ModelValidationError> result = validator.validate(context, null, Collections.singletonList(guruModel));
        assertEquals(1, result.size());
    }
}
