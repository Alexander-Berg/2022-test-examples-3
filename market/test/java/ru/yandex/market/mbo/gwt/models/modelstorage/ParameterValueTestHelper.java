package ru.yandex.market.mbo.gwt.models.modelstorage;

import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.visual.Word;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 22.03.2017
 */
public class ParameterValueTestHelper {
    private ParameterValueTestHelper() {
    }

    public static ParameterValue string(long id, String xslName, List<Word> words) {
        return new ParameterValue(
            id, xslName, Param.Type.STRING, null, null, null, words, null
        );
    }

    public static ParameterValue numeric() {
        return numeric(1L, "value");
    }

    public static ParameterValue numeric(long id, String xslName) {
        return numeric(id, xslName, 1);
    }

    public static ParameterValue numeric(long id, String xslName, int value) {
        return new ParameterValue(id, xslName, Param.Type.NUMERIC, BigDecimal.valueOf(value), null,
            null, null, null);
    }

    public static ParameterValue bool(long id, String xslName, boolean value, long optionId) {
        return new ParameterValue(id, xslName, Param.Type.BOOLEAN, null, value, optionId, null, null);
    }

    public static ParameterValue enumValue(long id, String xslName, long optionId, List<Word> words) {
        return new ParameterValue(id, xslName, Param.Type.ENUM, null, null, optionId, words, null);
    }
}
