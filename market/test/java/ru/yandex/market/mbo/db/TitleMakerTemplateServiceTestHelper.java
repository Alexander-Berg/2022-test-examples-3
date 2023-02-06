package ru.yandex.market.mbo.db;

import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.utils.WordUtil;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author dmserebr
 * @date 19.11.18
 */
public class TitleMakerTemplateServiceTestHelper {

    private TitleMakerTemplateServiceTestHelper() { }

    public static CategoryParam createParameter(long id, String xslName, Param.Type type, boolean iseService,
                                          boolean isGuru, boolean isGuruLight) {
        Parameter result = new Parameter();
        result.setId(id);
        result.addName(WordUtil.defaultWord(xslName));
        result.setType(type);
        result.setXslName(xslName);
        result.setService(iseService);
        result.setUseForGuru(isGuru);
        result.setUseForGurulight(isGuruLight);
        return result;
    }

    public static ParameterValue createParameterValue(long id, String xslName, Param.Type type,
                                                Long optionId, String strValue) {
        ParameterValue result = new ParameterValue();
        result.setParamId(id);
        result.setXslName(xslName);
        result.setType(type);
        if (optionId != null) {
            result.setOptionId(optionId);
        }
        if (strValue != null) {
            result.setStringValue(WordUtil.defaultWords(strValue));
        }
        return result;
    }

    public static ParameterValue createParameterValue(long id, String xslName, Param.Type type,
                                                      BigDecimal numericValue) {
        ParameterValue result = new ParameterValue();
        result.setParamId(id);
        result.setXslName(xslName);
        result.setType(type);
        if (numericValue != null) {
            result.setNumericValue(numericValue);
        }
        return result;
    }

    public static void assertParams(List<CategoryParam> params, String... xslNames) {
        assertEquals(xslNames.length, params.size());
        for (String xslName : xslNames) {
            boolean found = false;
            for (CategoryParam param : params) {
                if (param.getXslName().equals(xslName)) {
                    found = true;
                    break;
                }
            }
            assertTrue("Parameter " + xslName + " not found", found);
        }
    }
}
