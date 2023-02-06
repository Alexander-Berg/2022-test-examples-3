package ru.yandex.market.mbo.gwt.models.modelstorage;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue.ValueBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.visual.Word;

/**
 * @author ayratgdl
 * @date 07.09.18
 */
public class ParameterValuesTest {
    private static final long PARAM_1_ID = 101;
    private static final String PARAM_1_XSL_NAME = "xsl_name_101";

    @Test
    public void valueEqualsConsiderWordsWithoutTakingWordIdIntoConsideration() {
        ParameterValues paramValues1 = new ParameterValues(PARAM_1_ID, PARAM_1_XSL_NAME, Param.Type.STRING);
        paramValues1.addValue(
            new ParameterValue(PARAM_1_ID, PARAM_1_XSL_NAME, Param.Type.STRING,
                               ValueBuilder.newBuilder()
                                   .setStringValue(new Word(0, Language.RUSSIAN.getId(), "Значение"))
            )
        );

        ParameterValues paramValues2 = new ParameterValues(PARAM_1_ID, PARAM_1_XSL_NAME, Param.Type.STRING);
        paramValues2.addValue(
            new ParameterValue(PARAM_1_ID, PARAM_1_XSL_NAME, Param.Type.STRING,
                               ValueBuilder.newBuilder()
                                   .setStringValue(new Word(1, Language.RUSSIAN.getId(), "Значение"))
            )
        );

        Assert.assertTrue(paramValues1.valueEquals(paramValues2));
    }
}
