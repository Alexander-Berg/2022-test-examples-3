package ru.yandex.market.mboc.common.utils.expression;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EvaluateExprHandlerTest extends ExprHandlerBaseTest {

    @Test
    public void testHandler() {
        EvaluateExprHandler handler = new EvaluateExprHandler();
        Integer result = TEST_EXPRESSION.handle(0, handler);

        assertThat(result)
            .isNotNull()
            .isEqualTo(5);
    }
}
