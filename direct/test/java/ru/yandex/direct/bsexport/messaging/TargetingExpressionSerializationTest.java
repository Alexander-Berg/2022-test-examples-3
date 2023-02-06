package ru.yandex.direct.bsexport.messaging;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ru.yandex.adv.direct.expression.TargetingExpression;
import ru.yandex.direct.bsexport.testing.data.TestTargetingExpression;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.bsexport.messaging.FeedJsonSerializer.expressionOldStyleSerializer;
import static ru.yandex.direct.bsexport.testing.Util.getFromClasspath;

class TargetingExpressionSerializationTest extends BaseSerializationTest {

    private class ObjectWithTargetingExpressionTest {
        private final TargetingExpression expression;
        private final String object;

        ObjectWithTargetingExpressionTest(TargetingExpression expression, String resoursename) {
            this.expression = expression;
            this.object = getFromClasspath(resoursename);
        }

        @Test
        void newFormatTest() {
            serialize(expression);
            assertThat(json).isSubstringOf(object);
        }

        @Test
        void oldFormatTest() {
            String oldStyle = expressionOldStyleSerializer(expression);
            assertThat(oldStyle).isSubstringOf(object);
        }
    }

    @Nested
    class ContextWithTargetingExpression1 extends ObjectWithTargetingExpressionTest {
        ContextWithTargetingExpression1() {
            super(TestTargetingExpression.CONTEXT_TARGETING_EXPRESSION_1, "json/targeting_expression1_in_context.json");
        }
    }

    @Nested
    class ContextWithTargetingExpression2 extends ObjectWithTargetingExpressionTest {
        ContextWithTargetingExpression2() {
            super(TestTargetingExpression.CONTEXT_TARGETING_EXPRESSION_2, "json/targeting_expression3_in_context.json");
        }
    }


    @Nested
    class ContextWithTargetingExpression3 extends ObjectWithTargetingExpressionTest {
        ContextWithTargetingExpression3() {
            super(TestTargetingExpression.CONTEXT_TARGETING_EXPRESSION_3, "json/targeting_expression4_in_context.json");
        }
    }

    @Nested
    class OrderWithTargetingExpression1 extends ObjectWithTargetingExpressionTest {
        OrderWithTargetingExpression1() {
            super(TestTargetingExpression.ORDER_TARGETING_EXPRESSION_1, "json/targeting_expression2_in_order.json");
        }
    }

    @Nested
    class TargetingExpression5 extends ObjectWithTargetingExpressionTest {
        TargetingExpression5() {
            super(TestTargetingExpression.WEATHER_TARGETING_EXPRESSION_1, "json/targeting_expression5.json");
        }
    }
}
