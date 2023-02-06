package ru.yandex.market.mbo.gwt.client.pages.model.editor.model;

import org.junit.Assert;
import org.junit.Before;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.converter.NumericConverter;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.converter.ValueConverter;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ViewFactoryStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ParamMeta;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

/**
 * @author s-ermakov
 */
public class BaseParameterValuesTest {
    protected ParamMeta paramMeta;
    protected CommonModel model;
    protected CategoryParam categoryParam;
    protected ValueConverter<String> valueConverter;

    @Before
    public void setUp() throws Exception {
        categoryParam = CategoryParamBuilder.newBuilder(1, "test")
            .setType(Param.Type.NUMERIC)
            .build();
        model = CommonModelBuilder.newBuilder(1, 1, 1).getModel();
        paramMeta = new ViewFactoryStub().createParamMeta(categoryParam, model);

        valueConverter = new NumericConverter();
        valueConverter.setFormatApi(new ViewFactoryStub().createFormatApi());
    }

    protected static ParameterValue createParameterValue(CategoryParam categoryParam, int value) {
        return new ParameterValue(categoryParam, new BigDecimal(value));
    }

    protected static ParameterValues createParameterValues(CategoryParam categoryParam, int... values) {
        List<ParameterValue> parameterValues = Arrays.stream(values)
            .mapToObj(v -> new ParameterValue(categoryParam, new BigDecimal(v)))
            .collect(Collectors.toList());
        return new ParameterValues(categoryParam, parameterValues);
    }

    protected static void assertParameterValue(ParameterValue parameterValue, int value) {
        Assert.assertEquals(new BigDecimal(value), parameterValue.getNumericValue());
    }

    protected static void assertParameterValues(ParameterValues parameterValues, Integer... expectedValues) {
        List<Integer> values = parameterValues.getValues().stream()
            .map(ParameterValue::getNumericValue)
            .map(BigDecimal::toBigInteger)
            .map(BigInteger::intValue)
            .collect(Collectors.toList());

        Assert.assertEquals(Arrays.asList(expectedValues), values);
    }

    protected static void assertDifferentObjects(ParameterValues values1, ParameterValues values2) {
        Assert.assertEquals(values1, values2);
        Assert.assertFalse(values1 == values2);
        for (int i = 0, n = values1.size(); i < n; i++) {
            ParameterValue value1 = values1.getValue(i);
            ParameterValue value2 = values2.getValue(i);
            Assert.assertFalse(value1 == value2);
        }
    }
}
