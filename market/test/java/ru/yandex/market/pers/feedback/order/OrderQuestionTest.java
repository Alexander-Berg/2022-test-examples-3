package ru.yandex.market.pers.feedback.order;

import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.pers.feedback.builder.OrderBuilder;
import ru.yandex.market.pers.feedback.builder.OrderQuestionCreatebleBuilder;
import ru.yandex.market.pers.feedback.builder.OrderQuestionRuleCreatableBuilder;
import ru.yandex.market.pers.feedback.config.AbstractPersFeedbackTest;
import ru.yandex.market.pers.feedback.helper.OrderFeedbackHelper;
import ru.yandex.market.pers.feedback.helper.OrderQuestionHelper;
import ru.yandex.market.pers.feedback.helper.OrderQuestionRuleHelper;
import ru.yandex.market.pers.feedback.mock.CheckouterMockConfigurer;
import ru.yandex.market.pers.feedback.order.api.OrderFeedback;
import ru.yandex.market.pers.feedback.order.api.OrderFeedbackQuestion;
import ru.yandex.market.pers.feedback.order.api.OrderQuestionRuleReadable;
import ru.yandex.market.pers.feedback.order.api.OrderReadable;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class OrderQuestionTest extends AbstractPersFeedbackTest {
    @Autowired
    private OrderQuestionHelper orderQuestionHelper;
    @Autowired
    private OrderQuestionRuleHelper orderQuestionRuleHelper;
    @Autowired
    private OrderFeedbackHelper orderFeedbackHelper;

    @Test
    public void shouldOfferQuestionsByGrade() throws Exception {
        OrderFeedbackQuestion question =
                orderQuestionHelper.putQuestion(OrderQuestionCreatebleBuilder.builder().withTitle("question1").build());
        OrderQuestionRuleReadable questionRule =
                orderQuestionRuleHelper.postQuestionRule(new OrderQuestionRuleCreatableBuilder()
                        .withQuestionId(question.getId())
                        .withShowGrade(1)
                        .withDeliveryType(-1)
                        .withOutletPurpose(-1)
                        .withPaymentMethod(-1)
                        .withPaymentType(-1)
                        .build());

        Assertions.assertNotNull(questionRule);

        checkouterMockConfigurer.mockGetOrder(1234, ClientRole.USER, 1, OrderBuilder.builder()
                .build());

        OrderReadable orderFeedback = orderFeedbackHelper.getOrderFeedback(1234, 1);
        MatcherAssert.assertThat(orderFeedback.getFeedback().getQuestions(), hasSize(1));
        MatcherAssert.assertThat(orderFeedback.getFeedback().getQuestionsByGrade().get(1), hasSize(1));
        MatcherAssert.assertThat(orderFeedback.getFeedback().getQuestionsByGrade().get(2), hasSize(0));
    }

    @Test
    public void shouldOfferQuestionsByDeliveryType() throws Exception {
        OrderFeedbackQuestion question =
                orderQuestionHelper.putQuestion(OrderQuestionCreatebleBuilder.builder().withTitle("question1").build());
        OrderQuestionRuleReadable questionRule =
                orderQuestionRuleHelper.postQuestionRule(new OrderQuestionRuleCreatableBuilder()
                        .withQuestionId(question.getId())
                        .withShowGrade(1)
                        .withDeliveryType(DeliveryType.DELIVERY.getId())
                        .withOutletPurpose(-1)
                        .withPaymentMethod(-1)
                        .withPaymentType(-1)
                        .build());

        Assertions.assertNotNull(questionRule);

        OrderBuilder orderBuilder = OrderBuilder.builder()
                .withDeliveryType(DeliveryType.DELIVERY);


        checkouterMockConfigurer.mockGetOrder(1234, ClientRole.USER, 1, orderBuilder.build());

        OrderReadable orderFeedback = orderFeedbackHelper.getOrderFeedback(1234, 1);
        MatcherAssert.assertThat(orderFeedback.getFeedback().getQuestions(), hasSize(1));
        MatcherAssert.assertThat(orderFeedback.getFeedback().getQuestionsByGrade().get(1), hasSize(1));
        MatcherAssert.assertThat(orderFeedback.getFeedback().getQuestionsByGrade().get(2), hasSize(0));

    }

    @Test
    public void shouldOfferQuestionByPaymentType() throws Exception {
        OrderFeedbackQuestion question =
                orderQuestionHelper.putQuestion(OrderQuestionCreatebleBuilder.builder().withTitle("question1").build());
        OrderQuestionRuleReadable questionRule =
                orderQuestionRuleHelper.postQuestionRule(new OrderQuestionRuleCreatableBuilder()
                        .withQuestionId(question.getId())
                        .withShowGrade(1)
                        .withDeliveryType(-1)
                        .withOutletPurpose(-1)
                        .withPaymentMethod(-1)
                        .withPaymentType(PaymentType.POSTPAID.getId())
                        .build());

        Assertions.assertNotNull(questionRule);

        OrderBuilder orderBuilder = OrderBuilder.builder()
                .withPaymentType(PaymentType.POSTPAID);


        checkouterMockConfigurer.mockGetOrder(1234, ClientRole.USER, 1, orderBuilder.build());

        OrderReadable orderFeedback = orderFeedbackHelper.getOrderFeedback(1234, 1);
        MatcherAssert.assertThat(orderFeedback.getFeedback().getQuestions(), hasSize(1));
        MatcherAssert.assertThat(orderFeedback.getFeedback().getQuestionsByGrade().get(1), hasSize(1));
        MatcherAssert.assertThat(orderFeedback.getFeedback().getQuestionsByGrade().get(2), hasSize(0));
    }

    @Test
    public void shouldOfferQuestionByPaymentMethod() throws Exception {
        OrderFeedbackQuestion question =
                orderQuestionHelper.putQuestion(OrderQuestionCreatebleBuilder.builder().withTitle("question1").build());
        OrderQuestionRuleReadable questionRule =
                orderQuestionRuleHelper.postQuestionRule(new OrderQuestionRuleCreatableBuilder()
                        .withQuestionId(question.getId())
                        .withShowGrade(1)
                        .withDeliveryType(-1)
                        .withOutletPurpose(-1)
                        .withPaymentMethod(PaymentMethod.YANDEX.getId())
                        .withPaymentType(-1)
                        .build());

        Assertions.assertNotNull(questionRule);

        OrderBuilder orderBuilder = OrderBuilder.builder()
                .withPaymentMethod(PaymentMethod.YANDEX);


        checkouterMockConfigurer.mockGetOrder(1234, ClientRole.USER, 1, orderBuilder.build());

        OrderReadable orderFeedback = orderFeedbackHelper.getOrderFeedback(1234, 1);
        MatcherAssert.assertThat(orderFeedback.getFeedback().getQuestions(), hasSize(1));
        MatcherAssert.assertThat(orderFeedback.getFeedback().getQuestionsByGrade().get(1), hasSize(1));
        MatcherAssert.assertThat(orderFeedback.getFeedback().getQuestionsByGrade().get(2), hasSize(0));
    }

    @Test
    public void shouldReturnQuestions() throws Exception {
        OrderFeedbackQuestion question =
                orderQuestionHelper.putQuestion(OrderQuestionCreatebleBuilder.builder().withTitle("question1").build());

        List<OrderFeedbackQuestion> questions = orderQuestionHelper.getQuestions();

        MatcherAssert.assertThat(questions, hasSize(1));
        MatcherAssert.assertThat(questions.get(0).getId(), equalTo(question.getId()));
        MatcherAssert.assertThat(questions.get(0).getTitle(), equalTo(question.getTitle()));
    }

    @Test
    public void shouldDeleteQuestions() throws Exception {
        orderQuestionHelper.putQuestion(OrderQuestionCreatebleBuilder.builder().withTitle("question1").build());
        OrderFeedbackQuestion question2 =
                orderQuestionHelper.putQuestion(OrderQuestionCreatebleBuilder.builder().withTitle("question2").build());

        orderQuestionHelper.deleteQuestion(question2.getId());

        List<OrderFeedbackQuestion> questionsByTitle2 = orderQuestionHelper.getQuestions().stream()
                .filter(q -> "question2".equals(q.getTitle()))
                .collect(Collectors.toList());

        MatcherAssert.assertThat(questionsByTitle2, empty());
    }

    @Test
    public void shouldNotReturnDuplicatedQuestions() throws Exception {
        OrderFeedbackQuestion question =
                orderQuestionHelper.putQuestion(OrderQuestionCreatebleBuilder.builder().withTitle("question1").build());
        orderQuestionRuleHelper.postQuestionRule(OrderQuestionRuleCreatableBuilder.builder()
            .withQuestionId(question.getId())
            .withShowGrade(1)
            .withDeliveryType(-1)
            .withOutletPurpose(-1)
            .withPaymentType(-1)
            .withPaymentMethod(-1)
            .build());

        orderQuestionRuleHelper.postQuestionRule(OrderQuestionRuleCreatableBuilder.builder()
            .withQuestionId(question.getId())
            .withShowGrade(2)
            .withDeliveryType(-1)
            .withOutletPurpose(-1)
            .withPaymentType(-1)
            .withPaymentMethod(-1)
            .build());

        orderQuestionRuleHelper.postQuestionRule(OrderQuestionRuleCreatableBuilder.builder()
            .withQuestionId(question.getId())
            .withShowGrade(3)
            .withDeliveryType(-1)
            .withOutletPurpose(-1)
            .withPaymentType(-1)
            .withPaymentMethod(-1)
            .build());

        long orderId = 1234;
        int clientId = 3456;
        Order order = OrderBuilder.builder().build();
        checkouterMockConfigurer.mockGetOrder(orderId, ClientRole.USER, clientId, order);

        OrderReadable orderFeedback = orderFeedbackHelper.getOrderFeedback(orderId, clientId);
        MatcherAssert.assertThat(orderFeedback.getFeedback().getQuestions(), hasSize(1));
    }

    @Test
    public void shouldReturnQuestionsWithGradeMinus1InEveryGrade() throws Exception {
        OrderFeedbackQuestion question =
                orderQuestionHelper.putQuestion(OrderQuestionCreatebleBuilder.builder().withTitle("question1").build());
        orderQuestionRuleHelper.postQuestionRule(OrderQuestionRuleCreatableBuilder.builder()
                .withQuestionId(question.getId())
                .withShowGrade(-1)
                .withDeliveryType(-1)
                .withOutletPurpose(-1)
                .withPaymentType(-1)
                .withPaymentMethod(-1)
                .build());

        long orderId = 1234;
        int clientId = 3456;
        Order order = OrderBuilder.builder().build();
        checkouterMockConfigurer.mockGetOrder(orderId, ClientRole.USER, clientId, order);

        OrderFeedback feedback = orderFeedbackHelper.getOrderFeedback(orderId, clientId).getFeedback();
        MatcherAssert.assertThat(feedback.getQuestionsByGrade().get(-1), nullValue());
        MatcherAssert.assertThat(feedback.getQuestionsByGrade().get(1), hasSize(1));
        MatcherAssert.assertThat(feedback.getQuestionsByGrade().get(2), hasSize(1));
        MatcherAssert.assertThat(feedback.getQuestionsByGrade().get(3), hasSize(1));
        MatcherAssert.assertThat(feedback.getQuestionsByGrade().get(4), hasSize(1));
        MatcherAssert.assertThat(feedback.getQuestionsByGrade().get(5), hasSize(1));

    }

    @Test
    public void shouldUpdateQuestion() throws Exception {
        OrderFeedbackQuestion question =
                orderQuestionHelper.putQuestion(OrderQuestionCreatebleBuilder.builder().withTitle("question1").build());

        orderQuestionHelper.updateQuestion(question.getId(), OrderQuestionCreatebleBuilder.builder().withTitle("question2").build());

        List<OrderFeedbackQuestion> questions = orderQuestionHelper.getQuestions();

        OrderFeedbackQuestion updated = questions.stream()
                .filter(q -> q.getId() == question.getId())
                .findAny()
                .get();

        MatcherAssert.assertThat(updated.getTitle(), is("question2"));
    }
}
