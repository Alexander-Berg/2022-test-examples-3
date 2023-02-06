package ru.yandex.market.mboc.common.utils.expression;

import org.junit.Test;

import static ru.yandex.market.mboc.common.utils.expression.Expr.Cases.cases;
import static ru.yandex.market.mboc.common.utils.expression.Expr.Cases.otherwise;
import static ru.yandex.market.mboc.common.utils.expression.Expr.Cases.when;
import static ru.yandex.market.mboc.common.utils.expression.Expr.IfElse.ifElse;
import static ru.yandex.market.mboc.common.utils.expression.Expr.Not.negate;
import static ru.yandex.market.mboc.common.utils.expression.Expr.Value.value;

public abstract class ExprHandlerBaseTest {

    protected static final Expr<Integer, Integer> TEST_EXPRESSION =
        cases("cases",
            when("FIRST CASE", bool("first check", false), value("return 1", 1)),
            when(bool("second check", false), value("return 2", 2)),
            otherwise(
                cases("third check",
                    when(bool("false", false)
                            .and(bool("true", true))
                            .or(negate(bool("true", true))),
                        value("return 3", 3)),
                    when(bool("false", false).or(value("true", true)),
                        ifElse("ifElse check", negate(bool("true", true)),
                            value("return 4", 4),
                            value("return 5", 5))),
                    otherwise(value("return 6", 6))
                )
            )
        );

    protected static Expr.BoolExpr<Integer> bool(String descr, boolean result) {
        return value(descr, result);
    }

    @Test
    public abstract void testHandler();


    protected <T> DescribeExprHandler.Node nodeEvaluated(String name, T result) {
        return node(name)
            .addMark(new EvaluateAndDescribeExprHandler.EvaluatedMark<>(result));
    }

    protected DescribeExprHandler.Node node(String name) {
        return new DescribeExprHandler.Node(name);
    }
}
