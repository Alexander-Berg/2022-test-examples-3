package ru.yandex.direct.logviewercore.service;

import org.junit.Test;

import ru.yandex.direct.logviewercore.service.LogViewerService.ConditionWithOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.logviewercore.service.LogViewerService.ConditionWithOperator.fromString;

public class LogViewerServiceConditionWithOperatorTest {
    private static final String OPERATOR = ">";
    private static final String VALUE = "42";
    private static final String EMPTY = "";

    @Test
    public void fromString_HasOperatorAndValue() {
        String stringForParse = OPERATOR + VALUE;
        ConditionWithOperator actualResult = fromString(stringForParse);
        ConditionWithOperator expectedResult = new ConditionWithOperator(OPERATOR, VALUE);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);
    }

    @Test
    public void fromString_HasOperatorOnly() {
        ConditionWithOperator actualResult = fromString(OPERATOR);
        ConditionWithOperator expectedResult = new ConditionWithOperator(OPERATOR, EMPTY);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);
    }

    @Test
    public void fromString_HasValueOnly() {
        ConditionWithOperator actualResult = fromString(VALUE);
        ConditionWithOperator expectedResult = new ConditionWithOperator(EMPTY, VALUE);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);
    }

    @Test
    public void fromString_EmptyString() {
        ConditionWithOperator actualResult = fromString(EMPTY);
        ConditionWithOperator expectedResult = new ConditionWithOperator(EMPTY, EMPTY);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);
    }
}
