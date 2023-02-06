package ru.yandex.market.mbo.db.modelstorage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.db.modelstorage.params.ModelParamsService;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelMergeServiceStub;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.param.ParameterView;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.LocalizedString;
import ru.yandex.market.mbo.http.ModelStorage.ModificationSource;
import ru.yandex.market.mbo.http.ModelStorage.ParameterValue;
import ru.yandex.market.mbo.http.ModelStorage.ParameterValueHypothesis;
import ru.yandex.market.mbo.utils.WordProtoUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static ru.yandex.market.mbo.http.ModelStorage.MergeType.MERGE_APPEND;
import static ru.yandex.market.mbo.http.ModelStorage.MergeType.MERGE_REPLACE;
import static ru.yandex.market.mbo.http.ModelStorage.ModificationSource.ASSESSOR;
import static ru.yandex.market.mbo.http.ModelStorage.ModificationSource.AUTO;
import static ru.yandex.market.mbo.http.ModelStorage.ModificationSource.OPERATOR_CONFIRMED;
import static ru.yandex.market.mbo.http.ModelStorage.ModificationSource.OPERATOR_FILLED;
import static ru.yandex.market.mbo.http.ModelStorage.ModificationSource.TOLOKER;

/**
 * @author york
 * @since 03.10.2017
 */
public class ModelMergeServiceTest {
    private static long idSeq = 0;
    private static final long TODAY = new Date().getTime();
    private static final long TOMORROW = new Date().getTime() + 100000000;

    private static final int PARAM_VALUE_3 = 3;
    private static final int PARAM_VALUE_11 = 11;
    private static final int PARAM_VALUE_22 = 22;

    private static final long USER1 = 100;
    private static final long USER2 = 200;

    private static final Random RANDOM = new Random(1);

    private ModelMergeServiceStub mergeService;
    private final Map<String, ParameterView> parameterMap = new HashMap<>();

    @Before
    public void setUp() {
        Stream.of(
            new ParameterView(++idSeq, "string", Param.Type.STRING, true, null),
            new ParameterView(++idSeq, "numeric", Param.Type.NUMERIC, true, 1),
            new ParameterView(++idSeq, "single_numeric", Param.Type.NUMERIC, false, 1),
            new ParameterView(++idSeq, "enum", Param.Type.ENUM, false, null),
            new ParameterView(++idSeq, "bool", Param.Type.BOOLEAN, false, null)
        )
            .forEach(pv -> parameterMap.put(pv.getXslName(), pv));

        ModelParamsService paramsService = Mockito.mock(ModelParamsService.class);
        Mockito.when(paramsService.syncModel(Mockito.any())).thenAnswer(invocation ->
            invocation.getArgument(0)
        );
        mergeService = new ModelMergeServiceStub(null);
    }

    @Test
    public void testNoToAuto1() { //normal rewrite
        ModelStorage.Model.Builder before = createModel();
        before.addAllParameterValues(createParameter("numeric", null, USER1, TODAY, 1));

        ModelStorage.Model.Builder after = createModel();
        after.addAllParameterValues(createParameter("numeric", AUTO, USER2, TOMORROW, 2));

        ModelStorage.Model result = mergeService.merge(before.build(), after, AUTO, MERGE_REPLACE);

        assertEquals("1", result.getParameterValues(0).getNumericValue());
        assertEquals(USER1, result.getParameterValues(0).getUserId());
        assertEquals(TODAY, result.getParameterValues(0).getModificationDate());
    }


    @Test
    public void testAutoToAuto1() { //normal rewrite
        ModelStorage.Model.Builder before = createModel();
        before.addAllParameterValues(createParameter("enum", AUTO, USER1, TODAY, 1));

        ModelStorage.Model.Builder after = createModel();
        after.addAllParameterValues(createParameter("enum", AUTO, USER2, TOMORROW, 2));

        ModelStorage.Model result = mergeService.merge(before.build(), after, AUTO, MERGE_REPLACE);

        assertEquals(2, result.getParameterValues(0).getOptionId());
        assertEquals(USER2, result.getParameterValues(0).getUserId());
        assertEquals(TOMORROW, result.getParameterValues(0).getModificationDate());
    }

    @Test
    public void testAutoToToloka() { //normal rewrite
        ModelStorage.Model.Builder before = createModel();
        before.addAllParameterValues(createParameter("numeric", AUTO, USER1, TODAY, 1));
        ModelStorage.Model.Builder after = createModel();
        after.addAllParameterValues(createParameter("numeric", TOLOKER, USER2, TOMORROW, 2));
        ModelStorage.Model result = mergeService.merge(before.build(), after, TOLOKER, MERGE_REPLACE);
        assertEquals("2", result.getParameterValues(0).getNumericValue());
        assertEquals(USER2, result.getParameterValues(0).getUserId());
        assertEquals(TOMORROW, result.getParameterValues(0).getModificationDate());
    }

    @Test
    public void testTolokaToAuto() { //normal rewrite
        ModelStorage.Model.Builder before = createModel();
        before.addAllParameterValues(createParameter("numeric", TOLOKER, USER1, TODAY, 1));

        ModelStorage.Model.Builder after = createModel();
        after.addAllParameterValues(createParameter("numeric", AUTO, USER2, TOMORROW, 2));

        ModelStorage.Model result = mergeService.merge(before.build(), after, AUTO, MERGE_REPLACE);
        // Toloker value has higher priority than auto
        assertEquals("1", result.getParameterValues(0).getNumericValue());
        assertEquals(USER1, result.getParameterValues(0).getUserId());
        assertEquals(TODAY, result.getParameterValues(0).getModificationDate());
    }

    @Test
    public void testOpToToloka() { //normal operator rewrite of autovalue
        ModelStorage.Model.Builder before = createModel();
        before.addAllParameterValues(createParameter("numeric", TOLOKER, USER1, TODAY, 1));
        ModelStorage.Model.Builder after = createModel();
        after.addAllParameterValues(createParameter("numeric", OPERATOR_FILLED, USER2, TOMORROW, PARAM_VALUE_3));
        ModelStorage.Model result = mergeService.merge(before.build(), after, OPERATOR_FILLED, MERGE_REPLACE);
        assertEquals("3", result.getParameterValues(0).getNumericValue());
        assertEquals(USER2, result.getParameterValues(0).getUserId());
        assertEquals(TOMORROW, result.getParameterValues(0).getModificationDate());
    }

    @Test
    public void testOperatorToAuto() { //no autorewrite op value
        ModelStorage.Model.Builder before = createModel();
        before.addAllParameterValues(createParameter("numeric", OPERATOR_FILLED, USER1, TODAY, 1));

        ModelStorage.Model.Builder after = createModel();
        after.addAllParameterValues(createParameter("numeric", AUTO, USER2, TOMORROW, 2));

        ModelStorage.Model result = mergeService.merge(before.build(), after, AUTO, MERGE_REPLACE);

        assertEquals("1", result.getParameterValues(0).getNumericValue());
        assertEquals(USER1, result.getParameterValues(0).getUserId());
        assertEquals(TODAY, result.getParameterValues(0).getModificationDate());
    }

    @Test
    public void testOpToOp1() { //normal operator rewrite of operator's value
        ModelStorage.Model.Builder before = createModel();
        before.addAllParameterValues(createParameter("enum", OPERATOR_FILLED, USER1, TODAY, 1));

        ModelStorage.Model.Builder after = createModel();
        after.addAllParameterValues(createParameter("enum", OPERATOR_FILLED, USER2, TOMORROW, 2));

        ModelStorage.Model result = mergeService.merge(before.build(), after, OPERATOR_FILLED, MERGE_REPLACE);

        assertEquals(2, result.getParameterValues(0).getOptionId());
        assertEquals(USER2, result.getParameterValues(0).getUserId());
        assertEquals(TOMORROW, result.getParameterValues(0).getModificationDate());
    }

    @Test
    public void testOpToOp2() { //normal operator rewrite of operator's value
        ModelStorage.Model.Builder before = createModel();
        before.addAllParameterValues(createParameter("numeric", OPERATOR_CONFIRMED, USER1, TODAY, 1));

        ModelStorage.Model.Builder after = createModel();
        after.addAllParameterValues(createParameter("numeric", OPERATOR_FILLED, USER2, TOMORROW, 2));

        ModelStorage.Model result = mergeService.merge(before.build(), after, OPERATOR_FILLED, MERGE_REPLACE);

        assertEquals("2", result.getParameterValues(0).getNumericValue());
        assertEquals(USER2, result.getParameterValues(0).getUserId());
        assertEquals(TOMORROW, result.getParameterValues(0).getModificationDate());
    }

    @Test
    public void testAddValue() {
        ModelStorage.Model.Builder before = createModel();

        ModelStorage.Model.Builder after = createModel();
        after.addAllParameterValues(createParameter("numeric", OPERATOR_FILLED, USER2, TOMORROW, 2));

        ModelStorage.Model result = mergeService.merge(before.build(), after, OPERATOR_FILLED, MERGE_REPLACE);

        assertEquals(1, result.getParameterValuesCount());
        assertEquals("2", result.getParameterValues(0).getNumericValue());
        assertEquals(USER2, result.getParameterValues(0).getUserId());
        assertEquals(TOMORROW, result.getParameterValues(0).getModificationDate());
    }

    //VALUES REMOVAL
    @Test
    public void testRemove1() { //auto can remove auto
        ModelStorage.Model.Builder before = createModel();
        before.addAllParameterValues(createParameter("numeric", AUTO, USER1, TODAY, 1));

        ModelStorage.Model.Builder after = createModel();

        ModelStorage.Model result = mergeService.merge(before.build(), after, OPERATOR_FILLED, MERGE_REPLACE);

        assertEquals(0, result.getParameterValuesCount());
    }

    @Test
    public void testRemove2() { //operator can remove operator
        ModelStorage.Model.Builder before = createModel();
        before.addAllParameterValues(createParameter("numeric", OPERATOR_FILLED, USER1, TODAY, 1, 2));

        ModelStorage.Model.Builder after = createModel();

        ModelStorage.Model result = mergeService.merge(before.build(), after, OPERATOR_FILLED, MERGE_REPLACE);

        assertEquals(0, result.getParameterValuesCount());
    }

    @Test
    public void testRemove4() { //auto can not remove operator
        ModelStorage.Model.Builder before = createModel();
        before.addAllParameterValues(createParameter("numeric", OPERATOR_FILLED, USER1, TODAY, 1));

        ModelStorage.Model.Builder after = createModel();

        ModelStorage.Model result = mergeService.merge(before.build(), after, AUTO, MERGE_REPLACE);

        assertEquals(1, result.getParameterValuesCount());
        assertEquals("1", result.getParameterValues(0).getNumericValue());
        assertEquals("numeric", result.getParameterValues(0).getXslName());
        assertEquals(USER1, result.getParameterValues(0).getUserId());
        assertEquals(TODAY, result.getParameterValues(0).getModificationDate());
    }

    //multi-values tests
    @Test
    public void testMultiValueAdd() {
        ModelStorage.Model.Builder before = createModel();

        ModelStorage.Model.Builder after = createModel();
        after.addAllParameterValues(createParameter("numeric", AUTO, USER1, TODAY, 1, 2));

        ModelStorage.Model result = mergeService.merge(before.build(), after, AUTO, MERGE_REPLACE);

        assertEquals(2, result.getParameterValuesCount());
        assertEquals("numeric", result.getParameterValues(0).getXslName());
        assertEquals(USER1, result.getParameterValues(0).getUserId());
        assertEquals(TODAY, result.getParameterValues(0).getModificationDate());
        assertEquals("numeric", result.getParameterValues(1).getXslName());
        assertEquals(USER1, result.getParameterValues(1).getUserId());
        assertEquals(TODAY, result.getParameterValues(1).getModificationDate());
        assertNotEquals(result.getParameterValues(0).getNumericValue(),
            result.getParameterValues(1).getNumericValue());
    }

    @Test
    public void testMultiValueUpdate() {
        ModelStorage.Model.Builder before = createModel();
        before.addAllParameterValues(createParameter("numeric", AUTO, USER1, TODAY, 1, 2));

        ModelStorage.Model.Builder after = createModel();
        after.addAllParameterValues(
            createParameter("numeric", OPERATOR_FILLED, USER2, TOMORROW, PARAM_VALUE_11, PARAM_VALUE_22));

        ModelStorage.Model result = mergeService.merge(before.build(), after, OPERATOR_FILLED, MERGE_REPLACE);

        assertEquals("numeric", result.getParameterValues(0).getXslName());
        assertEquals(USER2, result.getParameterValues(0).getUserId());
        assertEquals(TOMORROW, result.getParameterValues(0).getModificationDate());
        assertEquals("numeric", result.getParameterValues(1).getXslName());
        assertEquals(USER2, result.getParameterValues(1).getUserId());
        assertEquals(TOMORROW, result.getParameterValues(1).getModificationDate());
        assertNotEquals(result.getParameterValues(0).getNumericValue(),
            result.getParameterValues(1).getNumericValue());

        //updated params longer then before
        assertEquals(2, result.getParameterValues(0).getNumericValue().length());
        assertEquals(2, result.getParameterValues(1).getNumericValue().length());

        assertEquals(2, result.getParameterValuesCount());
    }

    @Test
    public void testMultiValueUpdateReject() {
        ModelStorage.Model.Builder before = createModel();
        before.addAllParameterValues(createParameter("numeric", OPERATOR_FILLED, USER1, TODAY, 1));
        before.addAllParameterValues(createParameter("numeric", AUTO, USER1, TODAY, 2));

        ModelStorage.Model.Builder after = createModel();
        after.addAllParameterValues(createParameter("numeric", AUTO, USER2, TOMORROW, PARAM_VALUE_11, PARAM_VALUE_22));

        ModelStorage.Model result = mergeService.merge(before.build(), after, AUTO, MERGE_REPLACE);

        assertEquals("numeric", result.getParameterValues(0).getXslName());
        assertEquals(USER1, result.getParameterValues(0).getUserId());
        assertEquals(TODAY, result.getParameterValues(0).getModificationDate());
        assertEquals("numeric", result.getParameterValues(1).getXslName());
        assertEquals(USER1, result.getParameterValues(1).getUserId());
        assertEquals(TODAY, result.getParameterValues(1).getModificationDate());
        assertNotEquals(result.getParameterValues(0).getNumericValue(),
            result.getParameterValues(1).getNumericValue());

        //updated params longer then before
        assertEquals(1, result.getParameterValues(0).getNumericValue().length());
        assertEquals(1, result.getParameterValues(1).getNumericValue().length());
        assertEquals(2, result.getParameterValuesCount());
    }

    @Test
    public void testMultiValueRemovalReject() {
        ModelStorage.Model.Builder before = createModel();
        before.addAllParameterValues(createParameter("enum", AUTO, USER1, TODAY, 1));
        before.addAllParameterValues(createParameter("enum", OPERATOR_FILLED, USER1, TODAY, 2));

        ModelStorage.Model.Builder after = createModel();

        ModelStorage.Model result = mergeService.merge(before.build(), after, AUTO, MERGE_REPLACE);

        assertEquals(before.getParameterValuesCount(), result.getParameterValuesCount());
        assertEquals("enum", result.getParameterValues(0).getXslName());
        assertEquals("enum", result.getParameterValues(1).getXslName());
    }

    @Test
    public void testMultiValueUpdateReject1() {
        ModelStorage.Model.Builder before = createModel();
        before.addAllParameterValues(createParameter("enum", AUTO, USER1, TODAY, 1));
        before.addAllParameterValues(createParameter("enum", OPERATOR_FILLED, USER1, TODAY, 2));

        ModelStorage.Model.Builder after = createModel();
        after.addAllParameterValues(createParameter("enum", AUTO, USER2, TOMORROW, 1));
        after.addAllParameterValues(createParameter("enum", OPERATOR_FILLED, USER2, TOMORROW, 2));

        ModelStorage.Model result = mergeService.merge(before.build(), after, AUTO, MERGE_REPLACE);

        assertEquals(2, result.getParameterValuesCount());
        assertEquals("enum", result.getParameterValues(0).getXslName());
        assertEquals("enum", result.getParameterValues(1).getXslName());
        assertEquals(USER1, result.getParameterValues(0).getUserId());
        assertEquals(TODAY, result.getParameterValues(0).getModificationDate());
        assertEquals(USER1, result.getParameterValues(1).getUserId());
        assertEquals(TODAY, result.getParameterValues(1).getModificationDate());
    }

    @Test
    public void testMultiValueUpdate1() {
        ModelStorage.Model.Builder before = createModel();
        before.addAllParameterValues(createParameter("enum", AUTO, USER1, TODAY, 1));
        before.addAllParameterValues(createParameter("enum", OPERATOR_FILLED, USER1, TODAY, 2));

        ModelStorage.Model.Builder after = createModel();
        after.addAllParameterValues(createParameter("enum", AUTO, USER2, TOMORROW, 1));
        after.addAllParameterValues(createParameter("enum", OPERATOR_FILLED, USER2, TOMORROW, 2));
        after.addAllParameterValues(createParameter("enum", OPERATOR_FILLED, USER2, TOMORROW, PARAM_VALUE_3));

        ModelStorage.Model result = mergeService.merge(before.build(), after, AUTO, MERGE_REPLACE);

        assertEquals(after.getParameterValuesCount(), result.getParameterValuesCount());
        assertEquals("enum", result.getParameterValues(0).getXslName());
        assertEquals("enum", result.getParameterValues(1).getXslName());
        assertEquals("enum", result.getParameterValues(2).getXslName());
        assertEquals(1, result.getParameterValues(0).getOptionId());
        assertEquals(2, result.getParameterValues(1).getOptionId());
        assertEquals(PARAM_VALUE_3, result.getParameterValues(2).getOptionId());
    }

    @Test
    public void testMergeAppend() {
        ModelStorage.Model.Builder before = createModel();
        before.addAllParameterValues(createParameter("enum", AUTO, USER1, TODAY, 1));
        before.addAllParameterValues(createParameter("numeric", OPERATOR_FILLED, USER1, TODAY, 2));

        ModelStorage.Model.Builder after = createModel();
        before.addAllParameterValues(createParameter("enum", AUTO, USER1, TODAY, 2));

        ModelStorage.Model result = mergeService.merge(before.build(), after, AUTO, MERGE_APPEND);
        final int expectedCount = 3;
        assertEquals(expectedCount, result.getParameterValuesCount());
    }


    @Test
    public void testHypotheses1() {
        Integer testValue1 = 1;
        String testValue2 = generateRandomString();
        String testValue3 = generateRandomString();
        String testValue4 = generateRandomString();
        String testValue6 = generateRandomString();

        ModelStorage.Model.Builder before = createModel();
        // 1
        before.addAllParameterValues(createParameter("string", AUTO, USER1, TODAY, ""));
        before.addParameterValueHypothesis(createHypothesis("string", USER1, ""));
        // 2
        before.addAllParameterValues(createParameter("bool", AUTO, USER1, TODAY, true));
        before.addParameterValueHypothesis(createHypothesis("bool", USER1, "true"));
        // 3
        before.addAllParameterValues(createParameter("numeric", AUTO, USER1, TODAY, ""));
        before.addParameterValueHypothesis(createHypothesis("numeric", USER1, ""));
        // 4
        before.addAllParameterValues(createParameter("enum", AUTO, USER1, TODAY, testValue1));
        before.addParameterValueHypothesis(createHypothesis("enum", USER1, testValue2));

        ModelStorage.Model.Builder after = createModel();
        // 1
        after.addAllParameterValues(createParameter("string", AUTO, USER2, TOMORROW, testValue3));
        after.addParameterValueHypothesis(createHypothesis("string", USER2, testValue4));
        // 2
        after.addAllParameterValues(createParameter("bool", AUTO, USER2, TOMORROW, false));
        // 3
        after.addParameterValueHypothesis(createHypothesis("numeric", USER2, testValue6));
        // 4 --

        ModelStorage.Model result = mergeService.merge(before.build(), after, ASSESSOR, MERGE_REPLACE);

        // 1
        assertEquals(testValue3, getStringParameterValue(result, "string"));
        assertEquals(testValue4, getHypothesisValue(result, "string"));
        // 2
        assertEquals("true", getStringParameterValue(result, "bool"));
        assertEquals("true", getHypothesisValue(result, "bool"));
        // 3
        assertEquals(testValue6, getHypothesisValue(result, "numeric"));
        assertNull(getParameter(result, "numeric"));
        // 4
        assertNull(getParameter(result, "enum"));
        assertNull(testValue2, getHypothesis(result, "enum"));
    }

    @Test
    public void testHypotheses2() {
        Integer testValue1 = 1;
        String testValue3 = generateRandomString();
        String testValue4 = generateRandomString();
        boolean testValue5 = false;
        String testValue6 = generateRandomString();

        ModelStorage.Model.Builder before = createModel();
        // 1
        before.addAllParameterValues(createParameter("string", AUTO, USER1, TODAY, ""));
        // 2
        before.addAllParameterValues(createParameter("bool", AUTO, USER1, TODAY, true));
        before.addParameterValueHypothesis(createHypothesis("bool", USER1, "true"));
        // 3
        before.addAllParameterValues(createParameter("numeric", AUTO, USER1, TODAY, ""));
        // 4
        before.addAllParameterValues(createParameter("enum", AUTO, USER1, TODAY, testValue1));

        ModelStorage.Model.Builder after = createModel();
        // 1
        after.addAllParameterValues(createParameter("string", AUTO, USER2, TOMORROW, testValue3));
        after.addParameterValueHypothesis(createHypothesis("string", USER2, testValue4));
        // 2
        after.addAllParameterValues(createParameter("bool", AUTO, USER2, TOMORROW, testValue5));
        // 3
        after.addParameterValueHypothesis(createHypothesis("numeric", USER2, testValue6));
        // 4 --

        ModelStorage.Model result = mergeService.merge(before.build(), after, ASSESSOR, MERGE_REPLACE);

        // 1
        assertEquals(testValue3, getStringParameterValue(result, "string"));
        assertEquals(testValue4, getHypothesisValue(result, "string"));
        // 2
        assertEquals("true", getStringParameterValue(result, "bool"));
        assertEquals("true", getHypothesisValue(result, "bool"));
        // 3
        assertEquals(testValue6, getHypothesisValue(result, "numeric"));
        assertNull(getParameter(result, "numeric"));
        // 4
        assertNull(getParameter(result, "enum"));
    }

    @Test
    public void testHypotheses3() {
        String testValue2 = generateRandomString();
        String testValue3 = generateRandomString();
        String testValue4 = generateRandomString();
        boolean testValue5 = false;
        String testValue6 = generateRandomString();

        ModelStorage.Model.Builder before = createModel();
        // 1
        before.addParameterValueHypothesis(createHypothesis("string", USER1, ""));
        // 2
        before.addParameterValueHypothesis(createHypothesis("bool", USER1, "true"));
        // 3
        before.addParameterValueHypothesis(createHypothesis("numeric", USER1, ""));
        // 4
        before.addParameterValueHypothesis(createHypothesis("enum", USER1, testValue2));

        ModelStorage.Model.Builder after = createModel();
        // 1
        after.addAllParameterValues(createParameter("string", ASSESSOR, USER2, TOMORROW, testValue3));
        after.addParameterValueHypothesis(createHypothesis("string", USER2, testValue4));
        // 2
        after.addAllParameterValues(createParameter("bool", ASSESSOR, USER2, TOMORROW, testValue5));
        // 3
        after.addParameterValueHypothesis(createHypothesis("numeric", USER2, testValue6));
        // 4 --

        ModelStorage.Model result = mergeService.merge(before.build(), after, ASSESSOR, MERGE_REPLACE);

        // 1
        assertEquals(testValue3, getStringParameterValue(result, "string"));
        assertEquals(testValue4, getHypothesisValue(result, "string"));
        // 2
        assertEquals(testValue5 + "", getStringParameterValue(result, "bool"));
        assertNull(getHypothesis(result, "bool"));
        // 3
        assertEquals(testValue6, getHypothesisValue(result, "numeric"));
        assertNull(getParameter(result, "numeric"));
        // 4
        assertNull(getHypothesis(result, "enum"));
    }

    @Test
    public void testHypotheses4() {
        String testValue3 = generateRandomString();
        String testValue4 = generateRandomString();
        boolean testValue5 = false;
        String testValue6 = generateRandomString();

        ModelStorage.Model.Builder before = createModel();

        ModelStorage.Model.Builder after = createModel();
        // 1
        after.addAllParameterValues(createParameter("string", AUTO, USER2, TOMORROW, testValue3));
        after.addParameterValueHypothesis(createHypothesis("string", USER2, testValue4));
        // 2
        after.addAllParameterValues(createParameter("bool", AUTO, USER2, TOMORROW, testValue5));
        // 3
        after.addParameterValueHypothesis(createHypothesis("numeric", USER2, testValue6));

        ModelStorage.Model result = mergeService.merge(before.build(), after, ASSESSOR, MERGE_REPLACE);

        // 1
        assertEquals(testValue3, getStringParameterValue(result, "string"));
        assertEquals(testValue4, getHypothesisValue(result, "string"));
        // 2
        assertEquals(testValue5 + "", getStringParameterValue(result, "bool"));
        assertNull(getHypothesis(result, "bool"));
        // 3
        assertEquals(testValue6, getHypothesisValue(result, "numeric"));
        assertNull(getParameter(result, "numeric"));

    }

    @Test
    public void testHypotheses5() {
        Integer testValue1 = 1;
        String testValue3 = generateRandomString();

        ModelStorage.Model.Builder before = createModel();
        before.addAllParameterValues(createParameter("string", AUTO, USER1, TODAY, testValue1));
        before.addParameterValueHypothesis(createHypothesis("string", USER1, testValue3));

        ModelStorage.Model.Builder after = createModel();

        ModelStorage.Model result = mergeService.merge(before.build(), after, AUTO, MERGE_REPLACE);

        assertEquals(testValue1 + "", getStringParameterValue(result, "string"));
        assertEquals(testValue3, getHypothesisValue(result, "string"));
    }

    @Test
    public void testHypotheses6() {
        String testValue2 = generateRandomString();
        String testValue3 = generateRandomString();
        String testValue4 = generateRandomString();

        ModelStorage.Model.Builder before = createModel();
        before.addParameterValueHypothesis(createHypothesis("string", USER1, testValue2));

        ModelStorage.Model.Builder after = createModel();
        after.addAllParameterValues(createParameter("string", AUTO, USER2, TOMORROW, testValue3));
        after.addParameterValueHypothesis(createHypothesis("string", USER2, testValue4));

        ModelStorage.Model result = mergeService.merge(before.build(), after, ASSESSOR, MERGE_REPLACE);

        assertEquals(testValue3, getStringParameterValue(result, "string"));
        assertEquals(testValue4, getHypothesisValue(result, "string"));

    }

    private ParameterValueHypothesis getHypothesis(ModelStorage.Model model, String hypothesisId) {
        return model.getParameterValueHypothesisList().stream()
            .filter(h -> h.getXslName().equals(hypothesisId)).findAny().orElse(null);
    }

    private String getHypothesisValue(ModelStorage.Model model, String hypothesisId) {
        ModelStorage.ParameterValueHypothesis hypothesis = getHypothesis(model, hypothesisId);
        if (hypothesis != null) {
            return hypothesis.getStrValueList().stream().map(MboParameters.Word::getName).collect(joining());
        }
        throw new UnsupportedOperationException();
    }

    private ParameterValue getParameter(ModelStorage.Model model, String parameterId) {
        return model.getParameterValuesList().stream()
            .filter(h -> h.getXslName().equals(parameterId)).findAny().orElse(null);
    }

    private String getStringParameterValue(ModelStorage.Model model, String parameterId) {
        ParameterValue parameter = getParameter(model, parameterId);
        if (parameter != null) {
            ParameterView pv = parameterMap.get(parameterId);
            switch (pv.getType()) {
                case NUMERIC_ENUM:
                case ENUM:
                    return parameter.getOptionId() + "";
                case BOOLEAN:
                    return parameter.getBoolValue() + "";
                case NUMERIC:
                    return parameter.getNumericValue();
                case STRING:
                    return parameter.getStrValueList().stream().map(LocalizedString::getValue).collect(joining());
                default:
                    throw new IllegalStateException("Can't be here");
            }
        }
        throw new UnsupportedOperationException();
    }

    private ModelStorage.Model.Builder createModel() {
        return ModelStorageTestUtil.generateModel().toBuilder().clearParameterValues();
    }

    private static String generateRandomString() {
        return Integer.toHexString(RANDOM.nextInt());
    }

    private ParameterValueHypothesis createHypothesis(String xslName, Long uid, String value) {
        ParameterView pv = parameterMap.get(xslName);
        return ParameterValueHypothesis.newBuilder()
            .setParamId(pv.getId())
            .setXslName(xslName)
            .setValueType(MboParameters.ValueType.NUMERIC)
            .setUserId(uid)
            .addStrValue(WordProtoUtils.defaultWord(value))
            .build();
    }

    private List<ParameterValue> createParameter(String xslName,
                                                 ModificationSource source,
                                                 Long uid,
                                                 Long date,
                                                 Object... values) {
        ParameterView pv = parameterMap.get(xslName);
        assert pv != null;
        assert values.length > 0;
        assert values.length == 1 || pv.isMultivalue() || pv.getType() == Param.Type.STRING;
        List<ParameterValue> result = new ArrayList<>();
        ParameterValue.Builder prototypeBuilder = ParameterValue.newBuilder()
            .setParamId(pv.getId())
            .setXslName(xslName);
        if (uid != null) {
            prototypeBuilder.setUserId(uid);
        }
        if (source != null) {
            prototypeBuilder.setValueSource(source);
        }
        if (date != null) {
            prototypeBuilder.setModificationDate(date);
        }

        ParameterValue prototype = prototypeBuilder.build();

        if (pv.getType() == Param.Type.STRING) {
            List<LocalizedString> ls = Arrays.stream(values)
                .map(v -> LocalizedString.newBuilder()
                    .setValue(v.toString()).setIsoCode("ru").build())
                .collect(Collectors.toList());
            result.add(prototype.toBuilder()
                .addAllStrValue(ls)
                .setValueType(MboParameters.ValueType.STRING)
                .build());

        } else {
            for (Object val : values) {
                ParameterValue.Builder builder = prototype.toBuilder();
                switch (pv.getType()) {
                    case NUMERIC_ENUM:
                    case ENUM:
                        builder.setOptionId((Integer) val);
                        builder.setValueType(MboParameters.ValueType.ENUM);
                        break;
                    case BOOLEAN:
                        Boolean boolVal = (Boolean) val;
                        builder.setBoolValue(boolVal);
                        builder.setOptionId(boolVal ? 2 : 1);
                        builder.setValueType(MboParameters.ValueType.BOOLEAN);
                        break;
                    case NUMERIC:
                        builder.setNumericValue(val.toString());
                        builder.setValueType(MboParameters.ValueType.NUMERIC);
                        break;
                    default:
                        throw new IllegalStateException("Can't be here");
                }
                result.add(builder.build());
            }
        }
        return result;
    }

}
