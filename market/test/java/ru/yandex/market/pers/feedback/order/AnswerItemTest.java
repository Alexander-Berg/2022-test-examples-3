package ru.yandex.market.pers.feedback.order;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.pers.feedback.builder.OrderBuilder;
import ru.yandex.market.pers.feedback.builder.OrderCreatableBuilder;
import ru.yandex.market.pers.feedback.builder.OrderQuestionCreatebleBuilder;
import ru.yandex.market.pers.feedback.config.AbstractPersFeedbackTest;
import ru.yandex.market.pers.feedback.helper.OrderFeedbackHelper;
import ru.yandex.market.pers.feedback.helper.OrderQuestionHelper;
import ru.yandex.market.pers.feedback.helper.OrderQuestionRuleHelper;
import ru.yandex.market.pers.feedback.order.api.FeedbackType;
import ru.yandex.market.pers.feedback.order.api.OrderAnswer;
import ru.yandex.market.pers.feedback.order.api.OrderAnswerItem;
import ru.yandex.market.pers.feedback.order.api.OrderCreatable;
import ru.yandex.market.pers.feedback.order.api.OrderCreateReadable;
import ru.yandex.market.pers.feedback.order.api.OrderFeedbackQuestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

public class AnswerItemTest  extends AbstractPersFeedbackTest {
    public static final long ORDER_ID = 123L;
    public static final int CLIENT_ID = 321;
    @Autowired
    private WebApplicationContext wac;
    @Autowired
    private OrderQuestionHelper orderQuestionHelper;
    @Autowired
    private OrderFeedbackHelper orderFeedbackHelper;
    @Autowired
    private OrderQuestionRuleHelper orderQuestionRuleHelper;

    @BeforeEach
    public void setup() {
        this.mockMvc = webAppContextSetup(this.wac).build();
    }

    @Test
    public void testItemsSave() throws Exception {
        OrderFeedbackQuestion question1 = orderQuestionHelper.putQuestion(
            OrderQuestionCreatebleBuilder.builder().withTitle("question1").build());

        orderQuestionRuleHelper.addRuleWithFeedbackType(question1, FeedbackType.BLUE);

        OrderAnswerItem item1 = new OrderAnswerItem(111L, 1111L);
        OrderAnswerItem item2 = new OrderAnswerItem(222L, 2222L);
        OrderAnswer answer = new OrderAnswer(question1.getId(), List.of(item1, item2));
        OrderCreatable orderCreatable = OrderCreatableBuilder.builder()
            .withGrade(5)
            .withAnswers(List.of(answer))
            .build();
        Order order = OrderBuilder.builder().build();
        checkouterMockConfigurer.mockGetOrder(ORDER_ID, ClientRole.USER, CLIENT_ID, order);

        OrderCreateReadable ocr =
            orderFeedbackHelper.putOrderFeedback(ORDER_ID, CLIENT_ID, orderCreatable, FeedbackType.BLUE);
        assertEquals(List.of(item1, item2), ocr.getFeedback().getAnswers().get(0).getItems());
    }
}
