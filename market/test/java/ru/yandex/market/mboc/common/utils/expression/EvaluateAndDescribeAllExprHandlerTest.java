package ru.yandex.market.mboc.common.utils.expression;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EvaluateAndDescribeAllExprHandlerTest extends ExprHandlerBaseTest {

    @Test
    public void testHandler() {
        EvaluateAndDescribeAllExprHandler handler = new EvaluateAndDescribeAllExprHandler();
        var result = TEST_EXPRESSION.handle(0, handler);

        assertThat(result)
            .isNotNull()
            .isEqualTo(5);

        assertThat(handler.getDescriptionTree().toString())
            .isEqualTo(
                nodeEvaluated("cases", 5).addChildren(
                    node("case: first check").addChildren(
                        nodeEvaluated("condition", false).addChildren(
                            nodeEvaluated("first check", false)),
                        node("if true").addChildren(
                            node("return 1"))),
                    node("case: second check").addChildren(
                        nodeEvaluated("condition", false).addChildren(
                            nodeEvaluated("second check", false)),
                        node("if true").addChildren(
                            node("return 2"))),
                    nodeEvaluated("case: otherwise", 5).addChildren(
                        nodeEvaluated("condition", true).addChildren(
                            nodeEvaluated("otherwise", true)),
                        nodeEvaluated("if true", 5).addChildren(
                            nodeEvaluated("third check", 5).addChildren(
                                node("case: ((false) and (true)) or (not true)").addChildren(
                                    nodeEvaluated("condition", false).addChildren(
                                        nodeEvaluated("((false) and (true)) or (not true)", false).addChildren(
                                            nodeEvaluated("(false) and (true)", false).addChildren(
                                                nodeEvaluated("false", false),
                                                node("true")),
                                            nodeEvaluated("not true", false).addChildren(
                                                nodeEvaluated("true", true)))),
                                    node("if true").addChildren(
                                        node("return 3"))),
                                nodeEvaluated("case: (false) or (true)", 5).addChildren(
                                    nodeEvaluated("condition", true).addChildren(
                                        nodeEvaluated("(false) or (true)", true).addChildren(
                                            nodeEvaluated("false", false),
                                            nodeEvaluated("true", true))),
                                    nodeEvaluated("if true", 5).addChildren(
                                        nodeEvaluated("ifElse check", 5).addChildren(
                                            nodeEvaluated("condition", false).addChildren(
                                                nodeEvaluated("not true", false).addChildren(
                                                    nodeEvaluated("true", true))),
                                            nodeEvaluated("branches", 5).addChildren(
                                                node("if true").addChildren(
                                                    node("return 4")),
                                                nodeEvaluated("else", 5).addChildren(
                                                    nodeEvaluated("return 5", 5)))))),
                                node("case: otherwise").addChildren(
                                    node("condition").addChildren(
                                        node("otherwise")),
                                    node("if true").addChildren(
                                        node("return 6")))))))
                    .toString()
            );
    }
}
