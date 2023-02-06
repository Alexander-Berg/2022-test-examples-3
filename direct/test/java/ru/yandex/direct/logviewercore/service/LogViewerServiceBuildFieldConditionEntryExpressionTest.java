package ru.yandex.direct.logviewercore.service;

import org.junit.Test;

import ru.yandex.direct.clickhouse.SqlBuilder.Column;
import ru.yandex.direct.clickhouse.SqlBuilder.ExpressionWithBinds;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.logviewercore.service.LogViewerService.buildFieldConditionEntryExpression;

public class LogViewerServiceBuildFieldConditionEntryExpressionTest {
    private static final Column COLUMN = new Column("column");
    private static final String OPERATOR_EQUALS = "=";
    private static final String OPERATOR_NOT = "!";
    private static final String OPERATOR_GREATER = ">";

    private static final int INT_VALUE = 17;
    private static final String STRING_VALUE = "abc";
    private static final String STRING_PERCENT_WILDCARD = "abc%";
    private static final String STRING_UNDERSCORE_WILDCARD = "abc_";
    private static final int[] ARRAY_VALUE = {100};

    @Test
    public void buildFieldConditionEntryExpression_EqualityOperatorAndNumberValue() {
        ExpressionWithBinds actualResult = buildFieldConditionEntryExpression(
                COLUMN, int.class, OPERATOR_EQUALS, Integer.toString(INT_VALUE));
        ExpressionWithBinds expectedResult = new ExpressionWithBinds("`column` = ?", INT_VALUE);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);
    }

    @Test
    public void buildFieldConditionEntryExpression_NegativeOperatorAndNumberValue() {
        ExpressionWithBinds actualResult = buildFieldConditionEntryExpression(
                COLUMN, int.class, OPERATOR_NOT, Integer.toString(INT_VALUE));
        ExpressionWithBinds expectedResult = new ExpressionWithBinds("`column` != ?", INT_VALUE);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);
    }

    @Test
    public void buildFieldConditionEntryExpression_GreaterOperatorAndNumberValue() {
        ExpressionWithBinds actualResult = buildFieldConditionEntryExpression(
                COLUMN, int.class, OPERATOR_GREATER, Integer.toString(INT_VALUE));
        ExpressionWithBinds expectedResult = new ExpressionWithBinds("`column` > ?", INT_VALUE);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);
    }

    @Test
    public void buildFieldConditionEntryExpression_EqualityOperatorAndStringValue() {
        ExpressionWithBinds actualResult = buildFieldConditionEntryExpression(
                COLUMN, STRING_VALUE.getClass(), OPERATOR_EQUALS, STRING_VALUE);
        ExpressionWithBinds expectedResult = new ExpressionWithBinds("`column` = ?", STRING_VALUE);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);
    }

    @Test
    public void buildFieldConditionEntryExpression_NegativeOperatorAndStringValue() {
        ExpressionWithBinds actualResult = buildFieldConditionEntryExpression(
                COLUMN, STRING_VALUE.getClass(), OPERATOR_NOT, STRING_VALUE);
        ExpressionWithBinds expectedResult = new ExpressionWithBinds("`column` != ?", STRING_VALUE);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);
    }

    @Test
    public void buildFieldConditionEntryExpression_GreaterOperatorAndStringValue() {
        ExpressionWithBinds actualResult = buildFieldConditionEntryExpression(
                COLUMN, STRING_VALUE.getClass(), OPERATOR_GREATER, STRING_VALUE);
        ExpressionWithBinds expectedResult = new ExpressionWithBinds("`column` > ?", STRING_VALUE);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);
    }

    @Test
    public void buildFieldConditionEntryExpression_EqualityOperatorAndStringPercentWildcardValue() {
        ExpressionWithBinds actualResult = buildFieldConditionEntryExpression(
                COLUMN, STRING_PERCENT_WILDCARD.getClass(), OPERATOR_EQUALS, STRING_PERCENT_WILDCARD);
        ExpressionWithBinds expectedResult = new ExpressionWithBinds(
                "like(`column`, ?)", STRING_PERCENT_WILDCARD);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);
    }

    @Test
    public void buildFieldConditionEntryExpression_EqualityOperatorAndStringUnderscoreWildcardValue() {
        ExpressionWithBinds actualResult = buildFieldConditionEntryExpression(
                COLUMN, STRING_UNDERSCORE_WILDCARD.getClass(), OPERATOR_EQUALS, STRING_UNDERSCORE_WILDCARD);
        ExpressionWithBinds expectedResult = new ExpressionWithBinds(
                "like(`column`, ?)", STRING_UNDERSCORE_WILDCARD);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);
    }

    @Test
    public void buildFieldConditionEntryExpression_NegativeOperatorAndStringPercentWildcardValue() {
        ExpressionWithBinds actualResult = buildFieldConditionEntryExpression(
                COLUMN, STRING_PERCENT_WILDCARD.getClass(), OPERATOR_NOT, STRING_PERCENT_WILDCARD);
        ExpressionWithBinds expectedResult = new ExpressionWithBinds(
                "not like(`column`, ?)", STRING_PERCENT_WILDCARD);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);
    }

    @Test
    public void buildFieldConditionEntryExpression_NegativeOperatorAndStringUnderscoreWildcardValue() {
        ExpressionWithBinds actualResult = buildFieldConditionEntryExpression(
                COLUMN, STRING_UNDERSCORE_WILDCARD.getClass(), OPERATOR_NOT, STRING_UNDERSCORE_WILDCARD);
        ExpressionWithBinds expectedResult = new ExpressionWithBinds(
                "not like(`column`, ?)", STRING_UNDERSCORE_WILDCARD);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);
    }

    @Test
    public void buildFieldConditionEntryExpression_GreaterOperatorAndStringPercentWildcardValue() {
        ExpressionWithBinds actualResult = buildFieldConditionEntryExpression(
                COLUMN, STRING_PERCENT_WILDCARD.getClass(), OPERATOR_GREATER, STRING_PERCENT_WILDCARD);
        ExpressionWithBinds expectedResult = new ExpressionWithBinds(
                "`column` > ?", STRING_PERCENT_WILDCARD);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);
    }

    @Test
    public void buildFieldConditionEntryExpression_GreaterOperatorAndStringUnderscoreWildcardValue() {
        ExpressionWithBinds actualResult = buildFieldConditionEntryExpression(
                COLUMN, STRING_UNDERSCORE_WILDCARD.getClass(), OPERATOR_GREATER, STRING_UNDERSCORE_WILDCARD);
        ExpressionWithBinds expectedResult = new ExpressionWithBinds(
                "`column` > ?", STRING_UNDERSCORE_WILDCARD);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);
    }

    @Test
    public void buildFieldConditionEntryExpression_EqualityOperatorAndArrayValue() {
        ExpressionWithBinds actualResult = buildFieldConditionEntryExpression(
                COLUMN, ARRAY_VALUE.getClass(), OPERATOR_EQUALS, Integer.toString(ARRAY_VALUE[0]));
        ExpressionWithBinds expectedResult = new ExpressionWithBinds(
                "has(`column`, ?)", ARRAY_VALUE[0]);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);
    }

    @Test
    public void buildFieldConditionEntryExpression_NegativeOperatorAndArrayValue() {
        ExpressionWithBinds actualResult = buildFieldConditionEntryExpression(
                COLUMN, ARRAY_VALUE.getClass(), OPERATOR_NOT, Integer.toString(ARRAY_VALUE[0]));
        ExpressionWithBinds expectedResult = new ExpressionWithBinds(
                "not has(`column`, ?)", ARRAY_VALUE[0]);
        assertThat(actualResult).isEqualToComparingFieldByField(expectedResult);
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildFieldConditionEntryExpression_GreaterOperatorAndArrayValue() {
        ExpressionWithBinds actualResult = buildFieldConditionEntryExpression(
                COLUMN, ARRAY_VALUE.getClass(), OPERATOR_GREATER, Integer.toString(ARRAY_VALUE[0]));
    }
}
