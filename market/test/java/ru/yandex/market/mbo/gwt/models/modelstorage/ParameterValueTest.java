package ru.yandex.market.mbo.gwt.models.modelstorage;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.utils.WordUtil;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * @author s-ermakov
 */
public class ParameterValueTest {
    private static final Parameter STRING_CATEGORY_PARAM = CategoryParamBuilder.newBuilder(1, "test")
        .setType(Param.Type.STRING)
        .build();

    private static final Parameter NUMERIC_CATEGORY_PARAM = CategoryParamBuilder.newBuilder(1, "test")
        .setType(Param.Type.NUMERIC)
        .build();

    @Test
    public void testStringValuesWillBeSerializedIfInitializedFromVararg() {
        ParameterValue parameterValue = new ParameterValue(STRING_CATEGORY_PARAM,
            WordUtil.defaultWords("val1", "var2"));
        // на самом деле Arrays.asList тоже возвращает сериализуемый список
        // проблема в том, что его gwt не может десериализовать
        // поэтому проверяем, что образуется массив, а не Serializable
        Assert.assertTrue(parameterValue.getStringValue() instanceof ArrayList);
    }

    @Test
    public void testNumericEqualsAndHashCode() {
        ParameterValue intValue = createNumericParameterValue("400");
        ParameterValue doubleValue = createNumericParameterValue("400.0000");
        Assert.assertEquals(intValue, doubleValue);
        Assert.assertEquals(intValue.hashCode(), doubleValue.hashCode());

        //  hashcode has 100000 precision
        doubleValue = createNumericParameterValue("400.000001");
        Assert.assertNotEquals(intValue, doubleValue);
        Assert.assertEquals(intValue.hashCode(), doubleValue.hashCode());

        doubleValue = createNumericParameterValue("400.1");
        Assert.assertNotEquals(intValue, doubleValue);
        Assert.assertNotEquals(intValue.hashCode(), doubleValue.hashCode());

        intValue.setNumericValue(null);
        doubleValue.setNumericValue(null);
        Assert.assertEquals(intValue, doubleValue);
        Assert.assertEquals(intValue.hashCode(), doubleValue.hashCode());
    }

    private ParameterValue createNumericParameterValue(String bigDecimal) {
        ParameterValue value = new ParameterValue(NUMERIC_CATEGORY_PARAM);
        value.setNumericValue(new BigDecimal(bigDecimal));
        return value;
    }
}
