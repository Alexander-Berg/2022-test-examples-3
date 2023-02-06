package ru.yandex.direct.logviewercore.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.logviewercore.service.LogViewerService.normalizeOperators;

public class LogViewerServiceNormalizeOperatorsTest {
    private static final String OPERATOR1 = ">";
    private static final String OPERATOR2 = "<";
    private static final String VALUE1 = "10";
    private static final String VALUE2 = "1000";
    private static final String VALUE3 = "248";

    @Test
    public void normalizeOperators_EmptyList() {
        List<String> operatorsAndValues = new ArrayList<>();
        List<String> actualResult = normalizeOperators(operatorsAndValues);
        assertThat(actualResult).isEqualTo(operatorsAndValues);
    }

    @Test
    public void normalizeOperators_SingleValueOnly() {
        List<String> operatorsAndValues = List.of(VALUE1);
        List<String> actualResult = normalizeOperators(operatorsAndValues);
        assertThat(actualResult).isEqualTo(operatorsAndValues);
    }

    @Test
    public void normalizeOperators_MultipleValues() {
        List<String> operatorsAndValues = List.of(VALUE1, VALUE2, VALUE3);
        List<String> actualResult = normalizeOperators(operatorsAndValues);
        assertThat(actualResult).isEqualTo(operatorsAndValues);
    }

    @Test
    public void normalizeOperators_OperatorWithValue() {
        List<String> operatorsAndValues = List.of(OPERATOR1 + VALUE1);
        List<String> actualResult = normalizeOperators(operatorsAndValues);
        assertThat(actualResult).isEqualTo(operatorsAndValues);
    }

    @Test
    public void normalizeOperators_OperatorAndValue() {
        List<String> operatorsAndValues = List.of(OPERATOR1, VALUE1);
        List<String> actualResult = normalizeOperators(operatorsAndValues);
        List<String> expectedResult = List.of(OPERATOR1 + VALUE1);
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test
    public void normalizeOperators_MultipleOperatorsAndValues() {
        List<String> operatorsAndValues = List.of(OPERATOR1, VALUE1, OPERATOR2 + VALUE2, VALUE3, OPERATOR1, VALUE3);
        List<String> actualResult = normalizeOperators(operatorsAndValues);
        List<String> expectedResult = List.of(OPERATOR1 + VALUE1, OPERATOR2 + VALUE2, VALUE3, OPERATOR1 + VALUE3);
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test(expected = IllegalArgumentException.class)
    public void normalizeOperators_SingleOperatorOnly() {
        List<String> operatorsAndValues = List.of(OPERATOR1);
        List<String> actualResult = normalizeOperators(operatorsAndValues);
    }

    @Test(expected = IllegalArgumentException.class)
    public void normalizeOperators_ValueAndHangingOperator() {
        List<String> operatorsAndValues = List.of(VALUE1, OPERATOR1);
        List<String> actualResult = normalizeOperators(operatorsAndValues);
    }

    @Test(expected = IllegalArgumentException.class)
    public void normalizeOperators_OperatorAndValueAndHangingOperator() {
        List<String> operatorsAndValues = List.of(OPERATOR1, VALUE1, OPERATOR2);
        List<String> actualResult = normalizeOperators(operatorsAndValues);
    }

    @Test(expected = IllegalArgumentException.class)
    public void normalizeOperators_OperatorsInRow() {
        List<String> operatorsAndValues = List.of(OPERATOR1, OPERATOR2);
        List<String> actualResult = normalizeOperators(operatorsAndValues);
    }

    @Test(expected = IllegalArgumentException.class)
    public void normalizeOperators_OperatorAndOperatorWithValue() {
        List<String> operatorsAndValues = List.of(OPERATOR1, OPERATOR2 + VALUE1);
        List<String> actualResult = normalizeOperators(operatorsAndValues);
    }

    @Test(expected = IllegalArgumentException.class)
    public void normalizeOperators_OperatorsInRowAndValue() {
        List<String> operatorsAndValues = List.of(OPERATOR1, OPERATOR2, VALUE1);
        List<String> actualResult = normalizeOperators(operatorsAndValues);
    }
}
