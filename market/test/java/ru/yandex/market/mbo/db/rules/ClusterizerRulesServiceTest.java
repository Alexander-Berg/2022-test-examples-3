package ru.yandex.market.mbo.db.rules;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.rules.clusterizer.Condition;
import ru.yandex.market.mbo.gwt.models.rules.clusterizer.Expression;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 01.08.2016
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ClusterizerRulesServiceTest {
    ClusterizerRulesService rulesService;

    @Before
    public void setUp() throws Exception {
        rulesService = new ClusterizerRulesService();
    }

    @Test
    public void getNodesCount() throws Exception {
        Expression expression = new Expression();
        expression.setOperator(Expression.Operator.AND);

        for (int i = 0; i < 3; i++) {
            Condition condition = new Condition();
            condition.setParameterId(i + 100);
            condition.setOptionId(i + 4000);
            expression.getNodes().add(condition);
        }

        Expression childExpr = new Expression();
        childExpr.setOperator(Expression.Operator.OR);

        Condition condition = new Condition();
        condition.setParameterId(155);
        condition.setOptionId(4055);
        childExpr.getNodes().add(condition);

        condition = new Condition();
        condition.setParameterId(188);
        condition.setOptionId(4088);
        childExpr.getNodes().add(condition);

        expression.getNodes().add(childExpr);

        System.out.println(expression);
        assertThat(rulesService.getNodesCount(expression), is(7));
    }

}
