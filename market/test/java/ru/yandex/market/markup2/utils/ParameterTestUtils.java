package ru.yandex.market.markup2.utils;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author anmalysh
 */
public class ParameterTestUtils {
    public static final int RU = 225;

    private ParameterTestUtils() {

    }

    public static MboParameters.Parameter.Builder createParameterBuilder(
        long id, MboParameters.ValueType type, String xslName) {
        return createParameterBuilder(id, type, xslName, false, true, false, false);
    }

    public static MboParameters.Parameter createParameter(long id, MboParameters.ValueType type,
                                                          MboParameters.Option... options) {
        return createParameter(id, type, false, false, false, false, options);
    }

    public static MboParameters.Parameter createParameter(long id, MboParameters.ValueType type,
                                                          boolean multivalue, boolean formalizaton,
                                                          boolean clusterVariable, boolean clusterBone,
                                                          MboParameters.Option... options) {
        return createParameterBuilder(id, type, "xsl_name_" + String.valueOf(id),
            multivalue, formalizaton, clusterVariable, clusterBone)
            .addAllOption(Arrays.asList(options)).build();
    }

    public static MboParameters.Parameter.Builder createParameterBuilder(
        long id, MboParameters.ValueType type, String xslName, boolean multivalue, boolean formalizaton,
        boolean clusterVariable, boolean clusterBone) {

        MboParameters.Parameter.Builder result = MboParameters.Parameter.newBuilder()
            .setId(id)
            .setValueType(type)
            .setXslName(xslName)
            .setMultivalue(multivalue)
            .setUseForGurulight(true)
            .setUseFormalization(formalizaton);

        if (clusterVariable) {
            result.setFormalizerTag(MboParameters.Tag.newBuilder().setXslName("cluster-variable"));
        } else if (clusterBone) {
            result.setFormalizerTag(MboParameters.Tag.newBuilder().setXslName("cluster-bone"));
        }
        return result;
    }

    public static MboParameters.Option createOption(long id, String... names) {
        MboParameters.Option.Builder builder = MboParameters.Option.newBuilder().setId(id);
        for (String name : names) {
            builder.addName(createWord(name));
        }
        return builder.build();
    }

    public static void verifyModelParameter(ModelStorage.Model model, MboParameters.Parameter parameter,
                                     Object valueObject) {
        verifyModelParameter(model, parameter.getId(), parameter.getXslName(), parameter.getValueType(), valueObject);
    }

    public static void verifyModelParameter(ModelStorage.Model model, Long paramId, String xslName,
                                     MboParameters.ValueType type, Object valueObject) {
        for (ModelStorage.ParameterValue value : model.getParameterValuesList()) {
            if (value.getParamId() == paramId) {
                assertEquals(xslName, value.getXslName());
                assertEquals(type, value.getValueType());
                assertEquals(type.ordinal(), value.getTypeId());
                switch (type) {
                    case ENUM:
                        assertTrue(value.hasOptionId());
                        if (valueObject.equals(value.getOptionId())) {
                            assertFalse(value.hasNumericValue());
                            assertFalse(value.hasBoolValue());
                            assertTrue(value.getStrValueList().isEmpty());
                        }
                        return;
                    case NUMERIC_ENUM:
                        assertTrue(value.hasOptionId());
                        assertTrue(value.hasNumericValue());
                        if (valueObject.equals(value.getOptionId())) {
                            assertFalse(value.hasBoolValue());
                            assertTrue(value.getStrValueList().isEmpty());
                        }
                        return;
                    case BOOLEAN:
                        assertTrue(value.hasOptionId());
                        assertTrue(value.hasBoolValue());
                        if (valueObject.equals(value.getOptionId())) {
                            assertFalse(value.hasNumericValue());
                            assertTrue(value.getStrValueList().isEmpty());
                        }
                        return;
                    case NUMERIC:
                        assertTrue(value.hasNumericValue());
                        if (valueObject.equals(value.hasNumericValue())) {
                            assertFalse(value.hasOptionId());
                            assertFalse(value.hasBoolValue());
                            assertTrue(value.getStrValueList().isEmpty());
                        }
                        return;
                    case STRING:
                        assertFalse(value.getStrValueList().isEmpty());
                        Set<String> values = value.getStrValueList().stream()
                            .map(ModelStorage.LocalizedString::getValue)
                            .collect(Collectors.toSet());
                        if (values.contains(valueObject)) {
                            assertFalse(value.hasNumericValue());
                            assertFalse(value.hasOptionId());
                            assertFalse(value.hasBoolValue());
                        }
                        return;
                    default:
                }
            }
        }
        assertTrue("Parameter " + paramId + " with value " + valueObject + " not found in model " + model, false);
    }

    public static void verifyNoParameter(ModelStorage.Model model, Long paramId) {
        for (ModelStorage.ParameterValue value : model.getParameterValuesList()) {
            if (value.getParamId() == paramId) {
                assertTrue("Parameter " + paramId + " is not expected in model " + model, false);
            }
        }
    }

    public static MboParameters.Word createWord(String str) {
        MboParameters.Word.Builder builder = MboParameters.Word.newBuilder();
        builder.setName(str);
        builder.setLangId(RU);
        return builder.build();
    }

    public static ModelStorage.LocalizedString createLocalizedString(String str) {
        ModelStorage.LocalizedString.Builder builder = ModelStorage.LocalizedString.newBuilder();
        builder.setValue(str);
        builder.setIsoCode(Utils.RU_ISO_CODE);
        return builder.build();
    }

    public static ModelStorage.ParameterValue createStringParamValue(long id, String value) {
        ModelStorage.ParameterValue.Builder paramValueBuilder = ModelStorage.ParameterValue.newBuilder();
        paramValueBuilder.addStrValue(createLocalizedString(value));
        paramValueBuilder.setParamId(id);

        return paramValueBuilder.build();
    }

    public static ModelStorage.ParameterValue createNumericParamValue(long id, String value) {
        ModelStorage.ParameterValue.Builder paramValueBuilder = ModelStorage.ParameterValue.newBuilder();
        paramValueBuilder.setNumericValue(value);
        paramValueBuilder.setParamId(id);

        return paramValueBuilder.build();
    }

    public static MboParameters.Measure measure(long id, String... names) {
        MboParameters.Measure.Builder builder = MboParameters.Measure.newBuilder().setId(id);
        for (String name : names) {
            builder.addName(createWord(name));
        }
        return builder.build();
    }

    public static MboParameters.Unit unit(long id, MboParameters.Measure measure, String... names) {
        MboParameters.Unit.Builder builder = MboParameters.Unit.newBuilder()
            .setId(id)
            .setMeasure(measure);

        for (String name : names) {
            builder.addName(createWord(name));
        }
        return builder.build();
    }
}
