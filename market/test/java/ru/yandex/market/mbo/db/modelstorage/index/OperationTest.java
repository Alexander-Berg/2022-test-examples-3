package ru.yandex.market.mbo.db.modelstorage.index;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.common.utils.LocalizedStringUtils;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.ParamValueSearch;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.ParameterValueBuilder;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.http.ModelStorage;

import static org.junit.Assert.assertEquals;

/**
 * @author apluhin
 * @created 11/10/20
 */
@SuppressWarnings("checkstyle:magicnumber")
public class OperationTest {

    @Test
    public void testSaasConvertEq() {
        OperationContainer container = new OperationContainer(
            GenericField.MODEL_ID,
            10000,
            Operation.EQ
        );
        StringBuilder sb = new StringBuilder();
        Operation.EQ.convertToSaas(container).mkString(sb);
        assertEquals("s_id:000000000000010000", sb.toString());
    }

    @Test
    public void testSaasConvertGt() {
        OperationContainer container = new OperationContainer(
            GenericField.VENDOR_ID,
            10000,
            Operation.GT
        );
        StringBuilder sb = new StringBuilder();
        Operation.GT.convertToSaas(container).mkString(sb);
        assertEquals("s_vendor_id:>000000000000010000", sb.toString());
    }

    @Test
    public void testSaasConvertAttribute() {
        ParameterValues p1 = new ParameterValues(100L, "name", Param.Type.HYPOTHESIS);
        p1.addValue(ParameterValueBuilder.newBuilder().paramId(100).optionId(1).build());
        p1.addValue(ParameterValueBuilder.newBuilder().paramId(100).optionId(2).build());

        ParameterValues p2 = new ParameterValues(150L, "type", Param.Type.HYPOTHESIS);
        p2.addValue(ParameterValueBuilder.newBuilder().paramId(150).words(new Word(255, "check")).build());

        OperationContainer container = new OperationContainer(
            GenericField.ATTRIBUTE,
            Arrays.asList(new ParamValueSearch(p1), new ParamValueSearch(p2)),
            Operation.ATTRIBUTE
        );
        StringBuilder sb = new StringBuilder();
        Operation.ATTRIBUTE.convertToSaas(container).mkString(sb);
        assertEquals("(  ( sz_param_id:000000000000000100 /0  ( sz_option_id:1 | sz_option_id:2 )  )  &&" +
                "  ( sz_param_id:000000000000000150 /0  ( sz_string_values:check )  )  )",
            sb.toString().trim());
    }

    @Test
    public void testSaasConvertInvertedAttributes() {
        ParameterValues p1 = new ParameterValues(100L, "name", Param.Type.HYPOTHESIS);
        p1.addValue(ParameterValueBuilder.newBuilder().paramId(100).optionId(1).build());
        p1.addValue(ParameterValueBuilder.newBuilder().paramId(100).optionId(2).build());

        ParameterValues p2 = new ParameterValues(150L, "type", Param.Type.HYPOTHESIS);
        p2.addValue(ParameterValueBuilder.newBuilder().paramId(150).words(new Word(255, "check")).build());

        OperationContainer container = new OperationContainer(
            GenericField.ATTRIBUTE,
            Arrays.asList(new ParamValueSearch(p1, true), new ParamValueSearch(p2)),
            Operation.ATTRIBUTE
        );
        StringBuilder sb = new StringBuilder();
        Operation.ATTRIBUTE.convertToSaas(container).mkString(sb);
        assertEquals("(  ( sz_param_id:000000000000000150 /0  ( sz_string_values:check )  )  ~~  " +
                "( sz_param_id:000000000000000100 /0  ( sz_option_id:1 | sz_option_id:2 )  )  )",
            sb.toString().trim());
    }

    @Test
    public void testSaasConvertTwoInvertedAttributes() {
        ParameterValues p1 = new ParameterValues(100L, "name", Param.Type.HYPOTHESIS);
        p1.addValue(ParameterValueBuilder.newBuilder().paramId(100).optionId(1).build());
        p1.addValue(ParameterValueBuilder.newBuilder().paramId(100).optionId(2).build());

        ParameterValues p2 = new ParameterValues(150L, "type", Param.Type.HYPOTHESIS);
        p2.addValue(ParameterValueBuilder.newBuilder().paramId(150).words(new Word(255, "check")).build());

        OperationContainer container = new OperationContainer(
            GenericField.ATTRIBUTE,
            Arrays.asList(new ParamValueSearch(p1, true), new ParamValueSearch(p2, true)),
            Operation.ATTRIBUTE
        );
        StringBuilder sb = new StringBuilder();
        Operation.ATTRIBUTE.convertToSaas(container).mkString(sb);
        assertEquals("~~  (  ( sz_param_id:000000000000000100 /0  ( sz_option_id:1 | sz_option_id:2 )  ) " +
                " |  ( sz_param_id:000000000000000150 /0  ( sz_string_values:check )  )  )",
            sb.toString().trim());
    }

    //One inversed attribute convert without base andTerm
    @Test
    public void testSaasConvertInvertedAttribute() {
        ParameterValues p2 = new ParameterValues(150L, "type", Param.Type.HYPOTHESIS);
        p2.addValue(ParameterValueBuilder.newBuilder().paramId(150).words(new Word(255, "check")).build());

        OperationContainer container = new OperationContainer(
            GenericField.ATTRIBUTE,
            Arrays.asList(new ParamValueSearch(p2, true)),
            Operation.ATTRIBUTE
        );
        StringBuilder sb = new StringBuilder();
        Operation.ATTRIBUTE.convertToSaas(container).mkString(sb);
        assertEquals("~~  ( sz_param_id:000000000000000150 /0  ( sz_string_values:check )  )",
            sb.toString().trim());
    }

    @Test
    public void testInSaas() {
        List<Long> list = new ArrayList<Long>();
        list.add(1L);
        list.add(2L);
        OperationContainer container = new OperationContainer(
            GenericField.VENDOR_ID,
            list,
            Operation.IN
        );
        StringBuilder sb = new StringBuilder();
        Operation.IN.convertToSaas(container).mkString(sb);
        assertEquals("( s_vendor_id:000000000000000001 " +
            "| s_vendor_id:000000000000000002 )", sb.toString().trim());
    }

    @Test
    public void testFullTextInSaas() {
        OperationContainer container = new OperationContainer(
            GenericField.TITLE,
            "test kek",
            Operation.FULL_TEXT
        );
        StringBuilder sb = new StringBuilder();
        Operation.FULL_TEXT.convertToSaas(container).mkString(sb);
        assertEquals("z_titles:(test kek)", sb.toString().trim());
    }

    @Test
    public void testInModelCheck() {
        boolean result = Operation.IN.checkOperation(
            new OperationContainer(GenericField.CATEGORY_ID, Collections.singletonList(150L), Operation.IN), model());
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testInModelFailedCheck() {
        boolean result = Operation.IN.checkOperation(
            new OperationContainer(GenericField.CATEGORY_ID, Collections.singletonList(151L), Operation.IN), model());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testEqModelCheck() {
        boolean result = Operation.EQ.checkOperation(
            new OperationContainer(GenericField.DELETED, false, Operation.IN), model());
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testEqModelFailedCheck() {
        boolean result = Operation.EQ.checkOperation(
            new OperationContainer(GenericField.DELETED, true, Operation.IN), model());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testExistModelCheck() {
        boolean result = Operation.EXIST.checkOperation(
            new OperationContainer(GenericField.PARENT_ID, true, Operation.EXIST), model());
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testExistModelFailedCheck() {
        boolean result = Operation.EXIST.checkOperation(
            new OperationContainer(GenericField.DOUBTFUL, true, Operation.EXIST), model());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testGtModelCheck() {
        boolean result = Operation.GT.checkOperation(
            new OperationContainer(GenericField.CATEGORY_ID, 50, Operation.GT), model());
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testGtModelFailedCheck() {
        boolean result = Operation.GT.checkOperation(
            new OperationContainer(GenericField.CATEGORY_ID, 250, Operation.GT), model());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testMatchByAll() {
        ParameterValues stringMatch = new ParameterValues(
            100L,
            null,
            Param.Type.STRING,
            ParameterValue.ValueBuilder.newBuilder().setStringValue(new Word(255, "barcode1"))
        );
        ParameterValues booleanMatch = new ParameterValues(
            0L,
            "someBooleanXslName",
            Param.Type.BOOLEAN,
            ParameterValue.ValueBuilder.newBuilder().setBooleanValue(true)
        );
        ParameterValues numericMatch = new ParameterValues(
            0L,
            "someNumberXslName",
            Param.Type.NUMERIC,
            ParameterValue.ValueBuilder.newBuilder().setNumericValue(BigDecimal.valueOf(Double.parseDouble("100.5")))
        );
        ParameterValues hypothesisMatch = new ParameterValues(
            200L,
            null,
            Param.Type.HYPOTHESIS,
            ParameterValue.ValueBuilder.newBuilder().setOptionId(300L)
        );
        List<ParamValueSearch> values = Arrays.asList(
            new ParamValueSearch(stringMatch),
            new ParamValueSearch(booleanMatch),
            new ParamValueSearch(numericMatch),
            new ParamValueSearch(hypothesisMatch)
        );
        boolean result = Operation.ATTRIBUTE.checkOperation(
            new OperationContainer(GenericField.ATTRIBUTE, values, Operation.ATTRIBUTE), model());
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testWrongNumeric() {
        ParameterValues numericMatch = new ParameterValues(
            0L,
            "someNumberBadXslName",
            Param.Type.NUMERIC,
            ParameterValue.ValueBuilder.newBuilder().setNumericValue(BigDecimal.valueOf(Double.parseDouble("100.5")))
        );
        List<ParamValueSearch> values = Arrays.asList(new ParamValueSearch(numericMatch));
        boolean result = Operation.ATTRIBUTE.checkOperation(
            new OperationContainer(GenericField.ATTRIBUTE, values, Operation.ATTRIBUTE), model());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testWrongStringParameters() {
        ParameterValues stringMatch = new ParameterValues(
            100L,
            null,
            Param.Type.STRING,
            ParameterValue.ValueBuilder.newBuilder().setStringValue(new Word(255, "barcode5"))
        );
        List<ParamValueSearch> values = Arrays.asList(new ParamValueSearch(stringMatch));
        boolean result = Operation.ATTRIBUTE.checkOperation(
            new OperationContainer(GenericField.ATTRIBUTE, values, Operation.ATTRIBUTE), model());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testCheckExistParameterParameters() {
        ParameterValues stringMatch = new ParameterValues(
            100L,
            null,
            Param.Type.STRING,
            ParameterValue.ValueBuilder.newBuilder()
        );
        List<ParamValueSearch> values = Arrays.asList(new ParamValueSearch(stringMatch));
        boolean result = Operation.ATTRIBUTE.checkOperation(
            new OperationContainer(GenericField.ATTRIBUTE, values, Operation.ATTRIBUTE), model());
        Assertions.assertThat(result).isTrue();
    }


    @Test
    public void testFindByArrayValueOptionParam() {
        ParameterValues stringMatch = new ParameterValues(
            200L,
            null,
            Param.Type.HYPOTHESIS,
            ParameterValue.ValueBuilder.newBuilder().setOptionId(299L),
            ParameterValue.ValueBuilder.newBuilder().setOptionId(305L)
        );
        List<ParamValueSearch> values = Arrays.asList(new ParamValueSearch(stringMatch));
        boolean result = Operation.ATTRIBUTE.checkOperation(
            new OperationContainer(GenericField.ATTRIBUTE, values, Operation.ATTRIBUTE), model());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testFindByArrayValueStringParam() {
        ParameterValues stringMatch = new ParameterValues(
            100L,
            null,
            Param.Type.STRING,
            ParameterValue.ValueBuilder.newBuilder().setStringValue(new Word(255, "barcode2")),
            ParameterValue.ValueBuilder.newBuilder().setStringValue(new Word(255, "barcode4"))
        );
        List<ParamValueSearch> values = Arrays.asList(new ParamValueSearch(stringMatch));
        boolean result = Operation.ATTRIBUTE.checkOperation(
            new OperationContainer(GenericField.ATTRIBUTE, values, Operation.ATTRIBUTE), model());
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testCheckExistParameterParametersWithInvertedResult() {
        ParameterValues stringMatch = new ParameterValues(
            100L,
            null,
            Param.Type.STRING,
            ParameterValue.ValueBuilder.newBuilder()
        );
        List<ParamValueSearch> values = Arrays.asList(new ParamValueSearch(stringMatch, true));
        boolean result = Operation.ATTRIBUTE.checkOperation(
            new OperationContainer(GenericField.ATTRIBUTE, values, Operation.ATTRIBUTE), model());
        Assertions.assertThat(result).isFalse();
    }


    @Test
    public void testNonCheckExistParameterParameters() {
        ParameterValues stringMatch = new ParameterValues(
            99L,
            "radnomXslName",
            Param.Type.STRING,
            ParameterValue.ValueBuilder.newBuilder().setStringValue(new Word(255, "barcode5"))
        );
        List<ParamValueSearch> values = Arrays.asList(new ParamValueSearch(stringMatch));
        boolean result = Operation.ATTRIBUTE.checkOperation(
            new OperationContainer(GenericField.ATTRIBUTE, values, Operation.ATTRIBUTE), model());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testNonCheckExistParameterParametersWithInvertedParam() {
        ParameterValues stringMatch = new ParameterValues(
            99L,
            "radnomXslName",
            Param.Type.STRING,
            ParameterValue.ValueBuilder.newBuilder().setStringValue(new Word(255, "barcode5"))
        );
        List<ParamValueSearch> values = Arrays.asList(new ParamValueSearch(stringMatch, true));
        boolean result = Operation.ATTRIBUTE.checkOperation(
            new OperationContainer(GenericField.ATTRIBUTE, values, Operation.ATTRIBUTE), model());
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testMatchByAllInvertedAndNonInvertedParams() {
        ParameterValues stringMatch = new ParameterValues(
            100L,
            null,
            Param.Type.STRING,
            ParameterValue.ValueBuilder.newBuilder().setStringValue(new Word(255, "barcode1"))
        );
        ParameterValues booleanMatch = new ParameterValues(
            0L,
            "someBooleanXslName",
            Param.Type.BOOLEAN,
            ParameterValue.ValueBuilder.newBuilder().setBooleanValue(false)
        );
        ParameterValues numericMatch = new ParameterValues(
            -100L,
            "unrealXslName",
            Param.Type.NUMERIC,
            ParameterValue.ValueBuilder.newBuilder().setNumericValue(BigDecimal.valueOf(Double.parseDouble("100.5")))
        );
        List<ParamValueSearch> values = Arrays.asList(
            new ParamValueSearch(stringMatch),
            new ParamValueSearch(booleanMatch, true),
            new ParamValueSearch(numericMatch, true)
        );
        boolean result = Operation.ATTRIBUTE.checkOperation(
            new OperationContainer(GenericField.ATTRIBUTE, values, Operation.ATTRIBUTE), model());
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void testNonExistInverted() {
        ParameterValues stringMatch = new ParameterValues(
            100L,
            null,
            Param.Type.STRING,
            ParameterValue.ValueBuilder.newBuilder().setStringValue(new Word(255, "barcode1"))
        );
        List<ParamValueSearch> values = Arrays.asList(
            new ParamValueSearch(stringMatch, true)
        );
        boolean result = Operation.ATTRIBUTE.checkOperation(
            new OperationContainer(GenericField.ATTRIBUTE, values, Operation.ATTRIBUTE), model());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testNonExistInvertedByParamId() {
        ParameterValues stringMatch = new ParameterValues(
            100L,
            null,
            Param.Type.STRING,
            ParameterValue.ValueBuilder.newBuilder()
        );
        List<ParamValueSearch> values = Arrays.asList(
            new ParamValueSearch(stringMatch, true)
        );
        boolean result = Operation.ATTRIBUTE.checkOperation(
            new OperationContainer(GenericField.ATTRIBUTE, values, Operation.ATTRIBUTE), model());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testNonExistInvertedByParamIdForOptionId() {
        ParameterValues stringMatch = new ParameterValues(
            200L,
            null,
            Param.Type.HYPOTHESIS,
            ParameterValue.ValueBuilder.newBuilder()
        );
        List<ParamValueSearch> values = Arrays.asList(
            new ParamValueSearch(stringMatch, true)
        );
        boolean result = Operation.ATTRIBUTE.checkOperation(
            new OperationContainer(GenericField.ATTRIBUTE, values, Operation.ATTRIBUTE), model());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void testExistParamByParamIdByWithoutValue() {
        ParameterValues stringMatch = new ParameterValues(
            200L,
            null,
            Param.Type.HYPOTHESIS,
            ParameterValue.ValueBuilder.newBuilder()
        );
        List<ParamValueSearch> values = Arrays.asList(
            new ParamValueSearch(stringMatch)
        );
        boolean result = Operation.ATTRIBUTE.checkOperation(
            new OperationContainer(GenericField.ATTRIBUTE, values, Operation.ATTRIBUTE), model());
        Assertions.assertThat(result).isTrue();
    }


    private ModelStorage.Model model() {
        return ModelStorage.Model.newBuilder()
            .setId(100L)
            .setCategoryId(150L)
            .setDeleted(false)
            .setParentId(100L)
            .addAllParameterValues(Arrays.asList(
                ModelStorage.ParameterValue.newBuilder()
                    .setXslName("someStringXslName")
                    .setParamId(100L)
                    .setValueType(MboParameters.ValueType.STRING)
                    .setValueSource(ModelStorage.ModificationSource.AUTO)
                    .addAllStrValue(Arrays.asList(
                        LocalizedStringUtils.defaultString("barcode1"),
                        LocalizedStringUtils.defaultString("barcode2"),
                        LocalizedStringUtils.defaultString("barcode3")))
                    .build(),
                ModelStorage.ParameterValue.newBuilder()
                    .setXslName("someBooleanXslName")
                    .setParamId(150L)
                    .setValueType(MboParameters.ValueType.BOOLEAN)
                    .setBoolValue(true)
                    .build(),
                ModelStorage.ParameterValue.newBuilder()
                    .setXslName("someNumberXslName")
                    .setParamId(-2)
                    .setValueType(MboParameters.ValueType.NUMERIC)
                    .setNumericValue("100.5")
                    .build(),
                ModelStorage.ParameterValue.newBuilder()
                    .setParamId(-1)
                    .setXslName("someNumberBadXslName")
                    .setValueType(MboParameters.ValueType.NUMERIC)
                    .setNumericValue("")
                    .build(),
                ModelStorage.ParameterValue.newBuilder()
                    .setParamId(200L)
                    .setValueType(MboParameters.ValueType.HYPOTHESIS)
                    .setOptionId(300)
                    .build()
                )
            ).build();
    }
}
