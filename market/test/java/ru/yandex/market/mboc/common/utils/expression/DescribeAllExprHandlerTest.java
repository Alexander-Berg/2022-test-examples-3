package ru.yandex.market.mboc.common.utils.expression;

import org.junit.Test;

import ru.yandex.market.mboc.common.utils.expression.DescribeExprHandler.Node;

import static org.assertj.core.api.Assertions.assertThat;

public class DescribeAllExprHandlerTest extends ExprHandlerBaseTest {

    @Test
    public void testHandler() {
        DescribeAllExprHandler handler = new DescribeAllExprHandler();
        var result = TEST_EXPRESSION.handle(0, handler);

        assertThat(result).isNull();

        assertThat(handler.getDescriptionTree().toString())
            .isEqualTo(
                new Node("cases").addChildren(
                    new Node(DescribeExprHandler.Token.CASE, "FIRST CASE").addChildren(
                        new Node(DescribeExprHandler.Token.CONDITION, "first check"),
                        new Node(DescribeExprHandler.Token.THEN, "return 1")),
                    new Node(DescribeExprHandler.Token.CASE).addChildren(
                        new Node(DescribeExprHandler.Token.CONDITION, "second check"),
                        new Node(DescribeExprHandler.Token.THEN, "return 2")),
                    new Node(DescribeExprHandler.Token.CASE).addChildren(
                        new Node(DescribeExprHandler.Token.CONDITION, "otherwise"),
                        new Node(DescribeExprHandler.Token.THEN).addChildren(
                            new Node("third check").addChildren(
                                new Node(DescribeExprHandler.Token.CASE).addChildren(
                                    new Node(DescribeExprHandler.Token.CONDITION, "((false) and (true)) or (not true)"),
                                    new Node(DescribeExprHandler.Token.THEN, "return 3")),
                                new Node(DescribeExprHandler.Token.CASE).addChildren(
                                    new Node(DescribeExprHandler.Token.CONDITION, "(false) or (true)"),
                                    new Node(DescribeExprHandler.Token.THEN).addChildren(
                                        new Node("ifElse check").addChildren(
                                            new Node(DescribeExprHandler.Token.CONDITION, "not true"),
                                            new Node(DescribeExprHandler.Token.THEN, "return 4"),
                                            new Node(DescribeExprHandler.Token.ELSE, "return 5")))),
                                new Node(DescribeExprHandler.Token.CASE).addChildren(
                                    new Node(DescribeExprHandler.Token.CONDITION, "otherwise"),
                                    new Node(DescribeExprHandler.Token.THEN, "return 6"))))))
                    .toString()
            );
    }
}
