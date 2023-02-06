package ru.yandex.market.pers.feedback.order;

import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.pers.feedback.builder.OrderBuilder;
import ru.yandex.market.pers.feedback.builder.OrderCreatableBuilder;
import ru.yandex.market.pers.feedback.builder.OrderQuestionCreatebleBuilder;
import ru.yandex.market.pers.feedback.builder.OrderQuestionRuleCreatableBuilder;
import ru.yandex.market.pers.feedback.builder.OrderScenarioRuleCreatableBuilder;
import ru.yandex.market.pers.feedback.config.AbstractPersFeedbackTest;
import ru.yandex.market.pers.feedback.helper.OrderFeedbackHelper;
import ru.yandex.market.pers.feedback.helper.OrderQuestionHelper;
import ru.yandex.market.pers.feedback.helper.OrderQuestionRuleHelper;
import ru.yandex.market.pers.feedback.helper.OrderScenarioHelper;
import ru.yandex.market.pers.feedback.order.api.OrderAnswer;
import ru.yandex.market.pers.feedback.order.api.OrderCreateReadable;
import ru.yandex.market.pers.feedback.order.api.OrderFeedbackQuestion;
import ru.yandex.market.pers.feedback.order.api.OrderQuestionCreatable;
import ru.yandex.market.pers.feedback.order.api.OrderReadable;
import ru.yandex.market.pers.feedback.order.api.OrderScenarioRuleReadable;
import ru.yandex.market.pers.feedback.order.api.ScenarioType;
import ru.yandex.market.pers.feedback.order.model.OrderFeedbackEntity;
import ru.yandex.market.pers.feedback.order.model.SecurityData;
import ru.yandex.market.pers.feedback.order.service.OrderFeedbackDbService;
import ru.yandex.market.pers.feedback.order.service.SecurityService;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class OrderScenarioRuleTest extends AbstractPersFeedbackTest {

    private final static long ORDER_ID = 1234L;
    private final static int CLIENT_ID = 2345;

    @Autowired
    private OrderFeedbackHelper orderFeedbackHelper;
    @Autowired
    private OrderQuestionHelper orderQuestionHelper;
    @Autowired
    private OrderQuestionRuleHelper orderQuestionRuleHelper;
    @Autowired
    private OrderScenarioHelper orderScenarioHelper;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private OrderFeedbackDbService feedbackDbService;

    private OrderFeedbackQuestion question;

    @BeforeEach
    public void prepareQuestions() throws Exception {
        question =
                orderQuestionHelper.putQuestion(OrderQuestionCreatebleBuilder.builder().withTitle("question1").build());

        orderQuestionRuleHelper.postQuestionRule(OrderQuestionRuleCreatableBuilder.builder()
                .withQuestionId(question.getId())
                .withPaymentType(-1)
                .withDeliveryType(-1)
                .withShowGrade(-1)
                .withPaymentMethod(-1)
                .build());
    }

    @Test
    public void shouldReturnDefaultScenario() throws Exception {
        checkouterMockConfigurer.mockGetOrder(ORDER_ID, ClientRole.USER, CLIENT_ID, OrderBuilder.builder().build());

        OrderReadable orderFeedback = orderFeedbackHelper.getOrderFeedback(ORDER_ID, CLIENT_ID);

        OrderCreateReadable result = orderFeedbackHelper.putOrderFeedback(ORDER_ID, CLIENT_ID,
                OrderCreatableBuilder.builder()
                .withGrade(1)
                .withCallbackRequired(false)
                .withReviewDenied(false)
                .withComment("")
                .addAnswer(new OrderAnswer(question.getId()))
                .build(), null);

        MatcherAssert.assertThat(result.getScenarios(), hasSize(1));
        MatcherAssert.assertThat(result.getScenarios().get(0).getType(), equalTo(ScenarioType.THANK_YOU));
    }

    @Test
    public void shouldReturnSorryScenario() throws Exception {
        for (int grade = 1; grade <= 3; grade++) {
            orderScenarioHelper.postOrderScenarioRule(OrderScenarioRuleCreatableBuilder.builder()
                    .withGrade(grade)
                    .withPriority(1)
                    .withScenarioType(ScenarioType.SORRY)
                    .build());
        }

        for (int grade = 4; grade <= 5; grade++) {
            orderScenarioHelper.postOrderScenarioRule(OrderScenarioRuleCreatableBuilder.builder()
                    .withGrade(grade)
                    .withPriority(1)
                    .withScenarioType(ScenarioType.THANK_YOU)
                    .build());
        }

        checkouterMockConfigurer.mockGetOrder(ORDER_ID, ClientRole.USER, CLIENT_ID, OrderBuilder.builder().build());

        OrderCreateReadable result = orderFeedbackHelper.putOrderFeedback(ORDER_ID, CLIENT_ID,
                OrderCreatableBuilder.builder()
                        .withGrade(1)
                        .withCallbackRequired(false)
                        .withReviewDenied(false)
                        .withComment("")
                        .withNpsGrade(6)
                        .addAnswer(new OrderAnswer(question.getId()))
                        .build(), null);

        MatcherAssert.assertThat(result.getScenarios(), hasSize(1));
        MatcherAssert.assertThat(result.getScenarios().get(0).getType(), equalTo(ScenarioType.SORRY));
    }

    @Test
    public void shouldReturnSorryRefundScenario() throws Exception {
        for (int grade = 1; grade <= 3; grade++) {
            orderScenarioHelper.postOrderScenarioRule(OrderScenarioRuleCreatableBuilder.builder()
                    .withGrade(grade)
                    .withPriority(9999)
                    .withScenarioType(ScenarioType.SORRY)
                    .build());
        }


        OrderFeedbackQuestion question2 = orderQuestionHelper.putQuestion(new OrderQuestionCreatable("Товар поврежден"));

        orderQuestionRuleHelper.postQuestionRule(OrderQuestionRuleCreatableBuilder.builder()
                .withQuestionId(question2.getId())
                .withShowGrade(-1)
                .withPaymentType(-1)
                .withPaymentMethod(-1)
                .withDeliveryType(-1)
                .build());

        for (int grade = 1; grade <= 3; grade++) {
            orderScenarioHelper.postOrderScenarioRule(OrderScenarioRuleCreatableBuilder.builder()
                    .withGrade(grade)
                    .withPriority(1)
                    .withScenarioType(ScenarioType.SORRY_REFUND)
                    .withQuestionId(question2.getId())
                    .build()
            );
        }

        checkouterMockConfigurer.mockGetOrder(ORDER_ID, ClientRole.USER, CLIENT_ID, OrderBuilder.builder().build());

        OrderReadable orderFeedback = orderFeedbackHelper.getOrderFeedback(ORDER_ID, CLIENT_ID);

        OrderCreateReadable result = orderFeedbackHelper.putOrderFeedback(ORDER_ID, CLIENT_ID,
                OrderCreatableBuilder.builder()
                        .withGrade(1)
                        .withCallbackRequired(false)
                        .withReviewDenied(false)
                        .withComment("")
                        .addAnswer(new OrderAnswer(question2.getId()))
                        .build(), null);

        MatcherAssert.assertThat(result.getScenarios(), hasSize(1));
        MatcherAssert.assertThat(result.getScenarios().get(0).getType(), equalTo(ScenarioType.SORRY_REFUND));
    }

    @Test
    public void shouldGetAllScenarioRules() throws Exception {
        orderScenarioHelper.postOrderScenarioRule(OrderScenarioRuleCreatableBuilder.builder()
                .withQuestionId(null)
                .withScenarioType(ScenarioType.THANK_YOU)
                .withPriority(1)
                .withGrade(1)
                .build());

        List<OrderScenarioRuleReadable> orderScenarioRules = orderScenarioHelper.getOrderScenarioRules();

        MatcherAssert.assertThat(orderScenarioRules, hasSize(1));
        MatcherAssert.assertThat(orderScenarioRules.get(0).getScenarioType(), Matchers.is(ScenarioType.THANK_YOU));
    }

    @Test
    public void shouldUpdateScenarioRule() throws Exception {
        OrderScenarioRuleReadable scenario =
                orderScenarioHelper.postOrderScenarioRule(OrderScenarioRuleCreatableBuilder.builder()
                        .withQuestionId(null)
                        .withScenarioType(ScenarioType.THANK_YOU)
                        .withPriority(1)
                        .withGrade(1)
                        .build());

        orderScenarioHelper.putOrderScenarioRule(scenario.getId(), OrderScenarioRuleCreatableBuilder.builder()
                .withQuestionId(null)
                .withScenarioType(ScenarioType.THANK_YOU)
                .withPriority(1)
                .withGrade(1)
                .build());

        List<OrderScenarioRuleReadable> orderScenarioRules = orderScenarioHelper.getOrderScenarioRules();

        orderScenarioRules.stream().filter(s -> s.getId() == scenario.getId()).findAny().get();
    }

    @Test
    public void testFeedbackWithIp() throws Exception {
        checkouterMockConfigurer.mockGetOrder(ORDER_ID, ClientRole.USER, CLIENT_ID, OrderBuilder.builder().build());

        orderFeedbackHelper.putOrderFeedbackWithIp(ORDER_ID, CLIENT_ID, "127.1.1.1", null);
        OrderFeedbackEntity feedback = feedbackDbService.findFeedbackByOrderId(ORDER_ID);

        SecurityData sec = securityService.getSecurityData(feedback.getId());

        Assertions.assertEquals("127.1.1.1", sec.getIp());
        Assertions.assertNull(sec.getPort());
    }


    @Test
    public void testFeedbackWithIpPort() throws Exception {
        checkouterMockConfigurer.mockGetOrder(ORDER_ID, ClientRole.USER, CLIENT_ID, OrderBuilder.builder().build());

        orderFeedbackHelper.putOrderFeedbackWithIp(ORDER_ID, CLIENT_ID, "127.1.1.1", 4452);
        OrderFeedbackEntity feedback = feedbackDbService.findFeedbackByOrderId(ORDER_ID);

        SecurityData sec = securityService.getSecurityData(feedback.getId());

        Assertions.assertEquals("127.1.1.1", sec.getIp());
        Assertions.assertEquals(4452, sec.getPort());
    }
}
