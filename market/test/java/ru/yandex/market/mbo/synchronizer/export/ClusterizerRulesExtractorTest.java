package ru.yandex.market.mbo.synchronizer.export;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.export.CategoryPublishingRules;
import ru.yandex.market.mbo.gwt.models.rules.clusterizer.Condition;
import ru.yandex.market.mbo.gwt.models.rules.clusterizer.Expression;

@SuppressWarnings("checkstyle:magicNumber")
public class ClusterizerRulesExtractorTest {

    ClusterizerRulesExtractor.ConvertToProtobufVisitor visitor;

    @Before
    public void before() {
        visitor = new ClusterizerRulesExtractor.ConvertToProtobufVisitor();
    }

    @Test
    public void testConvertToProtobufVisitor() {
        Condition secondCondition1 = createCondition(11, 12);
        Condition secondCondition2 = createCondition(21, 22);

        Condition firstCondition1 = createCondition(31, 32);
        Condition firstCondition2 = createCondition(41, 42);

        Expression secondExpression = new Expression(Expression.Operator.AND, secondCondition1, secondCondition2);
        Expression firstExpression = new Expression(
                Expression.Operator.OR, secondExpression, firstCondition1, firstCondition2
        );

        visitor.enterNode(firstExpression);
            visitor.enterExpression(firstExpression);

                visitor.enterNode(secondExpression);
                    visitor.enterExpression(secondExpression);

                        visitor.enterNode(secondCondition1);
                            visitor.visitCondition(secondCondition1);
                        visitor.exitNode(secondCondition1);

                        visitor.enterNode(secondCondition2);
                            visitor.visitCondition(secondCondition2);
                        visitor.exitNode(secondCondition2);

                    visitor.exitExpression(secondExpression);
                visitor.exitNode(secondExpression);

                visitor.enterNode(firstCondition1);
                    visitor.visitCondition(firstCondition1);
                visitor.exitNode(firstCondition1);

                visitor.enterNode(firstCondition2);
                    visitor.visitCondition(firstCondition2);
                visitor.exitNode(firstCondition2);

            visitor.exitExpression(firstExpression);
        visitor.exitNode(firstExpression);

        CategoryPublishingRules.Node node = visitor.getRootNode().build();

        System.out.print(node);

        Assert.assertEquals(
                firstExpression.getOperator().toString(),
                node.getExpression().getOperator().toString()
        );
        Assert.assertEquals(
                secondExpression.getOperator().toString(),
                node.getExpression().getNode(0).getExpression().getOperator().toString()
        );

        Assert.assertEquals(
                secondCondition1.getParameterId(),
                node.getExpression().getNode(0).getExpression().getNode(0).getCondition().getParamId()
        );
        Assert.assertEquals(
                secondCondition1.getOptionId(),
                node.getExpression().getNode(0).getExpression().getNode(0).getCondition().getValueId()
        );

        Assert.assertEquals(
                secondCondition2.getParameterId(),
                node.getExpression().getNode(0).getExpression().getNode(1).getCondition().getParamId()
        );
        Assert.assertEquals(
                secondCondition2.getOptionId(),
                node.getExpression().getNode(0).getExpression().getNode(1).getCondition().getValueId()
        );

        Assert.assertEquals(
                firstCondition1.getParameterId(),
                node.getExpression().getNode(1).getCondition().getParamId()
        );
        Assert.assertEquals(
                firstCondition1.getOptionId(),
                node.getExpression().getNode(1).getCondition().getValueId()
        );

        Assert.assertEquals(
                firstCondition2.getParameterId(),
                node.getExpression().getNode(2).getCondition().getParamId()
        );
        Assert.assertEquals(
                firstCondition2.getOptionId(),
                node.getExpression().getNode(2).getCondition().getValueId()
        );
    }

    private Condition createCondition(long paramId, long optionId) {
        Condition condition = new Condition();
        condition.setParameterId(paramId);
        condition.setOptionId(optionId);
        return condition;
    }
}
