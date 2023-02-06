package ru.yandex.market.ir.autogeneration_api.http.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.ir.http.AutoGenerationApi;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.MboParameters.ValueType;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.ModificationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ayratgdl
 * @date 14.09.17
 */
public class ParamValueBuilderTest {
    private ParamValueBuilder builder;

    @Before
    public void setUp() {
        builder = new ParamValueBuilder(0L);
    }

    @Test
    public void addVendorParamValueTestAddParamWithoutValue() {
        MboParameters.Parameter.Builder parameterBuilder = MboParameters.Parameter.newBuilder();

        AutoGenerationApi.ParameterValue.Builder agParamValueBuilder = AutoGenerationApi.ParameterValue.newBuilder();
        agParamValueBuilder.setParamId(1);
        agParamValueBuilder.setType(AutoGenerationApi.ParameterType.TEXT);

        builder.addVendorParamValue(parameterBuilder.build(), agParamValueBuilder.build());
    }

    @Test
    public void testChoosingParamValueNumeric() {
        MboParameters.Parameter param = MboParameters.Parameter.newBuilder()
            .setValueType(MboParameters.ValueType.NUMERIC)
            .build();
        List<ModelStorage.ParameterValue> guruValues = Arrays.asList(
            ModelStorage.ParameterValue.newBuilder()
                .setParamId(1)
                .setNumericValue("0.5")
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                .build()
        );
        AutoGenerationApi.ParameterValue vendorValue = AutoGenerationApi.ParameterValue.newBuilder()
            .setParamId(1)
            .setType(AutoGenerationApi.ParameterType.BOOLEAN)
            .setNumericValue(0.5)
            .build();

        builder.chooseAndAddParamValue(param, Collections.singletonList(vendorValue), guruValues);
        List<ModelStorage.ParameterValue> parameterValues = builder.build();
        Assert.assertEquals(1, parameterValues.size());
        ModelStorage.ParameterValue addedValue = parameterValues.get(0);
        Assert.assertEquals(ModelStorage.ModificationSource.OPERATOR_FILLED, addedValue.getValueSource());
        Assert.assertEquals("0.5", addedValue.getNumericValue());

        builder.clear();
        vendorValue = vendorValue.toBuilder().setNumericValue(1.5).build();
        builder.chooseAndAddParamValue(param, Collections.singletonList(vendorValue), guruValues);
        parameterValues = builder.build();
        Assert.assertEquals(1, parameterValues.size());
        addedValue = parameterValues.get(0);
        Assert.assertEquals(ModelStorage.ModificationSource.VENDOR_OFFICE, addedValue.getValueSource());
        Assert.assertEquals("1.5", addedValue.getNumericValue());
    }

    @Test
    public void testChoosingParamValueEnum() {
        MboParameters.Parameter param = MboParameters.Parameter.newBuilder()
            .setValueType(MboParameters.ValueType.ENUM)
            .build();
        List<ModelStorage.ParameterValue> guruValues = Arrays.asList(
            ModelStorage.ParameterValue.newBuilder()
                .setParamId(1)
                .setOptionId(1)
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                .build()
        );
        AutoGenerationApi.ParameterValue vendorValue = AutoGenerationApi.ParameterValue.newBuilder()
            .setParamId(1)
            .setType(AutoGenerationApi.ParameterType.ENUM)
            .setOptionId(1)
            .build();

        builder.chooseAndAddParamValue(param, Collections.singletonList(vendorValue), guruValues);
        List<ModelStorage.ParameterValue> parameterValues = builder.build();
        Assert.assertEquals(1, parameterValues.size());
        ModelStorage.ParameterValue addedValue = parameterValues.get(0);
        Assert.assertEquals(ModelStorage.ModificationSource.OPERATOR_FILLED, addedValue.getValueSource());
        Assert.assertEquals(1, addedValue.getOptionId());

        builder.clear();
        vendorValue = vendorValue.toBuilder().setOptionId(2).build();
        builder.chooseAndAddParamValue(param, Collections.singletonList(vendorValue), guruValues);
        parameterValues = builder.build();
        Assert.assertEquals(1, parameterValues.size());
        addedValue = parameterValues.get(0);
        Assert.assertEquals(ModelStorage.ModificationSource.VENDOR_OFFICE, addedValue.getValueSource());
        Assert.assertEquals(2, addedValue.getOptionId());
    }

    @Test
    public void testChoosingParamValueBoolean() {
        MboParameters.Parameter param = MboParameters.Parameter.newBuilder()
            .setValueType(MboParameters.ValueType.BOOLEAN)
            .addOption(MboParameters.Option.newBuilder()
                .setId(1)
                .addName(MboParameters.Word.newBuilder().setName("true").build())
                .build())
            .addOption(MboParameters.Option.newBuilder()
                .setId(2)
                .addName(MboParameters.Word.newBuilder().setName("false").build())
                .build())
            .build();
        List<ModelStorage.ParameterValue> guruValues = Arrays.asList(
            ModelStorage.ParameterValue.newBuilder()
                .setParamId(1)
                .setBoolValue(true)
                .setOptionId(1)
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                .build()
        );
        AutoGenerationApi.ParameterValue vendorValue = AutoGenerationApi.ParameterValue.newBuilder()
            .setParamId(1)
            .setType(AutoGenerationApi.ParameterType.BOOLEAN)
            .setBoolValue(true)
            .build();

        builder.chooseAndAddParamValue(param, Collections.singletonList(vendorValue), guruValues);
        List<ModelStorage.ParameterValue> parameterValues = builder.build();
        Assert.assertEquals(1, parameterValues.size());
        ModelStorage.ParameterValue addedValue = parameterValues.get(0);
        Assert.assertEquals(ModelStorage.ModificationSource.OPERATOR_FILLED, addedValue.getValueSource());
        Assert.assertEquals(true, addedValue.getBoolValue());
        Assert.assertEquals(1, addedValue.getOptionId());

        builder.clear();
        vendorValue = vendorValue.toBuilder().setBoolValue(false).build();
        builder.chooseAndAddParamValue(param, Collections.singletonList(vendorValue), guruValues);
        parameterValues = builder.build();
        Assert.assertEquals(1, parameterValues.size());
        addedValue = parameterValues.get(0);
        Assert.assertEquals(ModelStorage.ModificationSource.VENDOR_OFFICE, addedValue.getValueSource());
        Assert.assertEquals(false, addedValue.getBoolValue());
        Assert.assertEquals(2, addedValue.getOptionId());
    }

    @Test
    public void testChoosingParamValueString() {
        MboParameters.Parameter param = MboParameters.Parameter.newBuilder()
            .setValueType(MboParameters.ValueType.STRING)
            .build();
        List<ModelStorage.ParameterValue> guruValues = Arrays.asList(
            ModelStorage.ParameterValue.newBuilder()
                .setParamId(1)
                .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("test").build())
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                .build()
        );
        AutoGenerationApi.ParameterValue vendorValue = AutoGenerationApi.ParameterValue.newBuilder()
            .setParamId(1)
            .setType(AutoGenerationApi.ParameterType.TEXT)
            .addText(AutoGenerationApi.LocalizedString.newBuilder().setText("test").build())
            .build();

        builder.chooseAndAddParamValue(param, Collections.singletonList(vendorValue), guruValues);
        List<ModelStorage.ParameterValue> parameterValues = builder.build();
        Assert.assertEquals(1, parameterValues.size());
        ModelStorage.ParameterValue addedValue = parameterValues.get(0);
        Assert.assertEquals(ModelStorage.ModificationSource.OPERATOR_FILLED, addedValue.getValueSource());
        Assert.assertEquals("test", addedValue.getStrValue(0).getValue());

        builder.clear();
        vendorValue = vendorValue.toBuilder()
            .setText(0, AutoGenerationApi.LocalizedString.newBuilder().setText("qwerty").build())
            .build();
        builder.chooseAndAddParamValue(param, Collections.singletonList(vendorValue), guruValues);
        parameterValues = builder.build();
        Assert.assertEquals(1, parameterValues.size());
        addedValue = parameterValues.get(0);
        Assert.assertEquals(ModelStorage.ModificationSource.VENDOR_OFFICE, addedValue.getValueSource());
        Assert.assertEquals("qwerty", addedValue.getStrValue(0).getValue());
    }

    @Test
    public void testChoosingParamValueStringMultivalue() {
        MboParameters.Parameter param = MboParameters.Parameter.newBuilder()
            .setValueType(MboParameters.ValueType.STRING)
            .build();
        List<ModelStorage.ParameterValue> guruValues = Arrays.asList(
            ModelStorage.ParameterValue.newBuilder()
                .setParamId(1)
                .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("test1").build())
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                .build(),
            ModelStorage.ParameterValue.newBuilder()
                .setParamId(1)
                .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("test2").build())
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                .build(),
            ModelStorage.ParameterValue.newBuilder()
                .setParamId(1)
                .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("test3").build())
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                .build()
        );

        AutoGenerationApi.ParameterValue vendorValue = AutoGenerationApi.ParameterValue.newBuilder()
            .setParamId(1)
            .setType(AutoGenerationApi.ParameterType.TEXT)
            .addText(AutoGenerationApi.LocalizedString.newBuilder().setText("test2").build())
            .build();

        builder.chooseAndAddParamValue(param, Collections.singletonList(vendorValue), guruValues);
        List<ModelStorage.ParameterValue> parameterValues = builder.build();
        Assert.assertEquals(1, parameterValues.size());
        ModelStorage.ParameterValue addedValue = parameterValues.get(0);
        Assert.assertEquals(ModificationSource.VENDOR_OFFICE, addedValue.getValueSource());
        Assert.assertEquals("test2", addedValue.getStrValue(0).getValue());

        builder.clear();
        vendorValue = vendorValue.toBuilder()
            .setText(0, AutoGenerationApi.LocalizedString.newBuilder().setText("qwerty").build())
            .build();
        builder.chooseAndAddParamValue(param, Collections.singletonList(vendorValue), guruValues);
        parameterValues = builder.build();
        Assert.assertEquals(1, parameterValues.size());
        addedValue = parameterValues.get(0);
        Assert.assertEquals(ModelStorage.ModificationSource.VENDOR_OFFICE, addedValue.getValueSource());
        Assert.assertEquals("qwerty", addedValue.getStrValue(0).getValue());
    }

    @Test
    public void preferVendorValuesWhenDifferenceInMultivalueParam() {
        MboParameters.Parameter param = MboParameters.Parameter.newBuilder()
            .setValueType(ValueType.ENUM)
            .setId(1L)
            .build();
        List<ModelStorage.ParameterValue> guruValues = new ArrayList<>(Arrays.asList(
            ModelStorage.ParameterValue.newBuilder()
                .setParamId(1)
                .setOptionId(1)
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                .build(),
            ModelStorage.ParameterValue.newBuilder()
                .setParamId(1)
                .setOptionId(3)
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                .build()
        ));
        List<AutoGenerationApi.ParameterValue> vendorValues = Arrays.asList(
                AutoGenerationApi.ParameterValue.newBuilder()
                        .setParamId(1)
                        .setType(AutoGenerationApi.ParameterType.ENUM)
                        .setOptionId(1)
                        .build(),
                AutoGenerationApi.ParameterValue.newBuilder()
                        .setParamId(1)
                        .setType(AutoGenerationApi.ParameterType.ENUM)
                        .setOptionId(2)
                        .build(),
                AutoGenerationApi.ParameterValue.newBuilder()
                        .setParamId(1)
                        .setType(AutoGenerationApi.ParameterType.ENUM)
                        .setOptionId(3)
                        .build()
        );

        builder.chooseAndAddParamValue(param, vendorValues, guruValues);
        List<ModelStorage.ParameterValue> parameterValues = builder.build();
        Assert.assertEquals(3, parameterValues.size());
        parameterValues.forEach(pv -> Assert.assertEquals(pv.getValueSource(), ModificationSource.VENDOR_OFFICE));

        guruValues.add(
                ModelStorage.ParameterValue.newBuilder()
                        .setParamId(1)
                        .setOptionId(4)
                        .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                        .build()
        );
        builder.clear();
        builder.chooseAndAddParamValue(param, vendorValues, guruValues);
        parameterValues = builder.build();
        Assert.assertEquals(3, parameterValues.size());
        parameterValues.forEach(pv -> Assert.assertEquals(pv.getValueSource(), ModificationSource.VENDOR_OFFICE));
        Assert.assertEquals(
                parameterValues.stream()
                        .map(ModelStorage.ParameterValue::getOptionId)
                        .sorted()
                        .collect(Collectors.toList()),
                Arrays.asList(1, 2, 3));
    }

    @Test
    public void preferGuruValuesWhenSameValuesInMultivalueParam() {
        MboParameters.Parameter param = MboParameters.Parameter.newBuilder()
                .setValueType(ValueType.ENUM)
                .setId(1L)
                .build();
        List<ModelStorage.ParameterValue> guruValues = Arrays.asList(
                ModelStorage.ParameterValue.newBuilder()
                        .setParamId(1)
                        .setOptionId(1)
                        .setValueType(ValueType.ENUM)
                        .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                        .build(),
                ModelStorage.ParameterValue.newBuilder()
                        .setParamId(1)
                        .setOptionId(3)
                        .setValueType(ValueType.ENUM)
                        .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                        .build(),
                ModelStorage.ParameterValue.newBuilder()
                        .setParamId(1)
                        .setOptionId(2)
                        .setValueType(ValueType.ENUM)
                        .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                        .build()
        );
        List<AutoGenerationApi.ParameterValue> vendorValues = Arrays.asList(
                AutoGenerationApi.ParameterValue.newBuilder()
                        .setParamId(1)
                        .setType(AutoGenerationApi.ParameterType.ENUM)
                        .setOptionId(1)
                        .build(),
                AutoGenerationApi.ParameterValue.newBuilder()
                        .setParamId(1)
                        .setType(AutoGenerationApi.ParameterType.ENUM)
                        .setOptionId(2)
                        .build(),
                AutoGenerationApi.ParameterValue.newBuilder()
                        .setParamId(1)
                        .setType(AutoGenerationApi.ParameterType.ENUM)
                        .setOptionId(3)
                        .build()
        );

        builder.chooseAndAddParamValue(param, vendorValues, guruValues);
        List<ModelStorage.ParameterValue> parameterValues = builder.build();
        Assert.assertEquals(3, parameterValues.size());
        parameterValues.forEach(pv -> Assert.assertEquals(pv.getValueSource(), ModificationSource.OPERATOR_FILLED));
    }
}