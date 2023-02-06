package ru.yandex.market.mbo.core.modelstorage.util;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.utils.WordProtoUtils;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:magicnumber")
public class ParametersInheritanceUtilTest {

    private ModelStorage.ParameterValue param1p;
    private ModelStorage.ParameterValue param2p;
    private ModelStorage.ParameterValue param3p;
    private ModelStorage.ParameterValue param4p;
    private ModelStorage.ParameterValue param5p;
    private ModelStorage.ParameterValue param1c;
    private ModelStorage.ParameterValue param2c;
    private ModelStorage.ParameterValue param3c;
    private ModelStorage.ParameterValueHypothesis hypo1p;
    private ModelStorage.ParameterValueHypothesis hypo1c;

    @Before
    public void setup() {
        param1p = ModelStorage.ParameterValue.newBuilder()
            .setParamId(1L)
            .setValueType(MboParameters.ValueType.STRING)
            .setXslName("1L")
            .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("strVal1").build())
            .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("strVal2").build())
            .build();
        param2p = ModelStorage.ParameterValue.newBuilder()
            .setParamId(2L)
            .setValueType(MboParameters.ValueType.STRING)
            .setXslName("2L")
            .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("strVal3").build())
            .build();
        param3p = ModelStorage.ParameterValue.newBuilder()
            .setParamId(5L)
            .setValueType(MboParameters.ValueType.STRING)
            .setXslName("5L")
            .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("strVal7").build())
            .build();
        param4p = ModelStorage.ParameterValue.newBuilder()
            .setParamId(7L)
            .setValueType(MboParameters.ValueType.STRING)
            .setXslName("7L")
            .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("strVal71").build())
            .build();
        param5p = ModelStorage.ParameterValue.newBuilder()
            .setParamId(7L)
            .setValueType(MboParameters.ValueType.STRING)
            .setXslName("7L")
            .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("strVal72").build())
            .build();

        param1c = ModelStorage.ParameterValue.newBuilder()
            .setParamId(1L)
            .setValueType(MboParameters.ValueType.STRING)
            .setXslName("1L")
            .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("strVal4").build())
            .build();
        param2c = ModelStorage.ParameterValue.newBuilder()
            .setParamId(4L)
            .setValueType(MboParameters.ValueType.STRING)
            .setXslName("4L")
            .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("strVal6").build())
            .build();
        param3c = ModelStorage.ParameterValue.newBuilder()
            .setParamId(1L)
            .setValueType(MboParameters.ValueType.STRING)
            .setXslName("1L")
            .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("strVal8").build())
            .build();

        hypo1p = ModelStorage.ParameterValueHypothesis.newBuilder()
            .setParamId(3L)
            .setValueType(MboParameters.ValueType.STRING)
            .setXslName("3L")
            .addStrValue(WordProtoUtils.defaultWord("strHypo1"))
            .build();
        hypo1c = ModelStorage.ParameterValueHypothesis.newBuilder()
            .setParamId(2L)
            .setValueType(MboParameters.ValueType.STRING)
            .setXslName("2L")
            .addStrValue(WordProtoUtils.defaultWord("strHypo5"))
            .build();
    }

    @Test
    public void paramAndHypoInheritance() {
        ModelStorage.Model parent = ModelStorage.Model.newBuilder()
            .addParameterValues(param1p)
            .addParameterValues(param2p)
            .addParameterValues(param3p)
            .addParameterValues(param4p)
            .addParameterValues(param5p)
            .addParameterValueHypothesis(hypo1p)
            .build();

        ModelStorage.Model child = ModelStorage.Model.newBuilder()
            .addParameterValues(param1c)
            .addParameterValues(param2c)
            .addParameterValues(param3c)
            .addParameterValueHypothesis(hypo1c)
            .build();

        child = ParametersInheritanceUtil.inheritParametersAndHypotheses(child, parent);

        assertThat(child.getParameterValuesList())
            .containsExactlyInAnyOrder(param3p, param1c, param2c, param3c, param4p, param5p);
        assertThat(child.getParameterValueHypothesisList()).containsExactlyInAnyOrder(hypo1p, hypo1c);
    }
}
