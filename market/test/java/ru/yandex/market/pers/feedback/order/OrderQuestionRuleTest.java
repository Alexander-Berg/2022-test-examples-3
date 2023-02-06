package ru.yandex.market.pers.feedback.order;

import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.feedback.builder.OrderQuestionCreatebleBuilder;
import ru.yandex.market.pers.feedback.builder.OrderQuestionRuleCreatableBuilder;
import ru.yandex.market.pers.feedback.config.AbstractPersFeedbackTest;
import ru.yandex.market.pers.feedback.helper.OrderQuestionHelper;
import ru.yandex.market.pers.feedback.helper.OrderQuestionRuleHelper;
import ru.yandex.market.pers.feedback.order.api.OrderFeedbackQuestion;
import ru.yandex.market.pers.feedback.order.api.OrderQuestionRuleReadable;

public class OrderQuestionRuleTest extends AbstractPersFeedbackTest {
    @Autowired
    private OrderQuestionHelper orderQuestionHelper;
    @Autowired
    private OrderQuestionRuleHelper orderQuestionRuleHelper;

    @Test
    void shouldDeleteQuestionRule() throws Exception {
        OrderFeedbackQuestion question1 = orderQuestionHelper.putQuestion(OrderQuestionCreatebleBuilder.builder()
                .withTitle("question1")
                .build());

        OrderQuestionRuleReadable questionRule1 =
                orderQuestionRuleHelper.postQuestionRule(new OrderQuestionRuleCreatableBuilder()
                .withQuestionId(question1.getId())
                .withDeliveryType(1)
                .withPaymentMethod(-1)
                .withPaymentType(-1)
                .withShowGrade(-1)
                .build()
        );

        orderQuestionRuleHelper.deleteQuestionRule(questionRule1.getId());

        List<OrderQuestionRuleReadable> rules = orderQuestionRuleHelper.getQuestionRules();

        MatcherAssert.assertThat(rules, Matchers.empty());
    }

    @Test
    void shouldUpdateQuestionRule() throws Exception {
        OrderFeedbackQuestion question1 = orderQuestionHelper.putQuestion(OrderQuestionCreatebleBuilder.builder()
                .withTitle("question1")
                .build());

        OrderQuestionRuleReadable questionRule1 =
                orderQuestionRuleHelper.postQuestionRule(new OrderQuestionRuleCreatableBuilder()
                        .withQuestionId(question1.getId())
                        .withDeliveryType(1)
                        .withPaymentMethod(-1)
                        .withPaymentType(-1)
                        .withShowGrade(-1)
                        .build()
                );

        orderQuestionRuleHelper.putQuestionRule(questionRule1.getId(), new OrderQuestionRuleCreatableBuilder()
                .withQuestionId(question1.getId())
                .withDeliveryType(2)
                .withPaymentMethod(-1)
                .withPaymentType(-1)
                .withShowGrade(-1)
                .build()
        );

        List<OrderQuestionRuleReadable> rules = orderQuestionRuleHelper.getQuestionRules();

        rules.stream()
                .filter(r -> r.getId() == questionRule1.getId())
                .findAny();
    }
}
