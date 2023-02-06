package ru.yandex.market.gutgin.tms.utils;

import io.qameta.allure.Issue;
import org.junit.Test;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;

import static org.assertj.core.api.Assertions.assertThat;


public class ParameterCreatorTest {
    private static final long PARAM_ID = 100500;
    private static final String XSL_NAME = "XslName";
    private static final int OPTION_ID = 1000500;

    private ParameterCreator parameterCreator = new ParameterCreator();

    @Test
    @Issue("MARKETIR-8785")
    public void createNumericParam() {
        assertNumeric("100", "100");
        assertNumeric("100.500", "100.500");
        assertNumeric("100.500", "100,500");
    }

    @Test
    @Issue("MARKETIR-8785")
    public void createNumericEnumParam() {
        assertNumericEnum("100", "100");
        assertNumericEnum("100.500", "100.500");
        assertNumericEnum("100.500", "100,500");
    }

    private void assertNumeric(String expected, String actual) {
        final ModelStorage.ParameterValue numericParam = parameterCreator
            .createNumericParam(PARAM_ID, XSL_NAME, actual, -1);

        assertThat(numericParam).isEqualTo(ModelStorage.ParameterValue.newBuilder()
            .setParamId(PARAM_ID)
            .setXslName(XSL_NAME)
            .setValueType(MboParameters.ValueType.NUMERIC)
            .setValueSource(ModelStorage.ModificationSource.VENDOR_OFFICE)
            .setTypeId(ModelStorage.ParameterValueType.NUMERIC_VALUE)
            .setUserId(ModelStorageHelper.AUTO_USER_ID)
            .setNumericValue(expected)
            .build()
        );
    }

    private void assertNumericEnum(String expected, String actual) {
        final ModelStorage.ParameterValue numericParam = parameterCreator
            .createNumericEnumParam(PARAM_ID, XSL_NAME, OPTION_ID, actual, -1);

        assertThat(numericParam).isEqualTo(ModelStorage.ParameterValue.newBuilder()
            .setParamId(PARAM_ID)
            .setXslName(XSL_NAME)
            .setOptionId(OPTION_ID)
            .setValueType(MboParameters.ValueType.NUMERIC_ENUM)
            .setValueSource(ModelStorage.ModificationSource.VENDOR_OFFICE)
            .setTypeId(ModelStorage.ParameterValueType.NUMERIC_ENUM_VALUE)
            .setUserId(ModelStorageHelper.AUTO_USER_ID)
            .setNumericValue(expected)
            .build()
        );
    }
}