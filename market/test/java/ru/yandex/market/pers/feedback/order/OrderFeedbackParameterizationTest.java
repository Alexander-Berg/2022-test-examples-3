package ru.yandex.market.pers.feedback.order;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.common.util.IOUtils;
import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.outlet.OutletPurpose;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.pers.author.client.PersAuthorClient;
import ru.yandex.market.pers.author.client.api.model.AgitationType;
import ru.yandex.market.pers.author.client.api.model.AgitationUserType;
import ru.yandex.market.pers.feedback.builder.OrderBuilder;
import ru.yandex.market.pers.feedback.builder.OrderCreatableBuilder;
import ru.yandex.market.pers.feedback.builder.OrderQuestionCreatebleBuilder;
import ru.yandex.market.pers.feedback.config.AbstractPersFeedbackTest;
import ru.yandex.market.pers.feedback.helper.OrderFeedbackHelper;
import ru.yandex.market.pers.feedback.helper.OrderQuestionHelper;
import ru.yandex.market.pers.feedback.helper.OrderQuestionRuleHelper;
import ru.yandex.market.pers.feedback.order.api.DeliveredOnTime;
import ru.yandex.market.pers.feedback.order.api.FeedbackType;
import ru.yandex.market.pers.feedback.order.api.OrderCreatable;
import ru.yandex.market.pers.feedback.order.api.OrderCreateReadable;
import ru.yandex.market.pers.feedback.order.api.OrderFeedbackQuestion;
import ru.yandex.market.pers.feedback.order.api.OrderReadable;
import ru.yandex.market.pers.feedback.order.api.QuestionCategory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

public class OrderFeedbackParameterizationTest extends AbstractPersFeedbackTest {
    private static final int CLIENT_ID = 321;
    private static final long ORDER_1 = 123L;
    private static final long ORDER_2 = 234L;
    private static final long ORDER_3 = 345L;
    private static final long ORDER_4 = 456L;

    @Autowired
    private WebApplicationContext wac;
    @Autowired
    private OrderQuestionHelper orderQuestionHelper;
    @Autowired
    private OrderFeedbackHelper orderFeedbackHelper;
    @Autowired
    private OrderQuestionRuleHelper orderQuestionRuleHelper;
    @Autowired
    private TestableClock clock;
    @MockBean
    private PersAuthorClient authorClient;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setup() {
        this.mockMvc = webAppContextSetup(this.wac).build();
    }

    @Test
    public void testFeedbackTypes() throws Exception {
        OrderFeedbackQuestion question1 = orderQuestionHelper.putQuestion(
            OrderQuestionCreatebleBuilder.builder().withTitle("question1").build());
        OrderFeedbackQuestion question2 = orderQuestionHelper.putQuestion(
            OrderQuestionCreatebleBuilder.builder().withTitle("question2").build());

        orderQuestionRuleHelper.addRuleWithFeedbackType(question1, FeedbackType.BLUE);
        orderQuestionRuleHelper.addRuleWithFeedbackType(question2, FeedbackType.DSBS);

        OrderCreatable orderCreatable = OrderCreatableBuilder.builder().withGrade(5).build();
        List<Long> orderIds = List.of(ORDER_1, ORDER_2, ORDER_3);
        mockOrders(orderIds, CLIENT_ID);

        OrderCreateReadable ocr1 = orderFeedbackHelper.putOrderFeedback(orderIds.get(0), CLIENT_ID,
            orderCreatable, null);
        OrderCreateReadable ocr2 = orderFeedbackHelper.putOrderFeedback(orderIds.get(1), CLIENT_ID,
            orderCreatable, FeedbackType.BLUE);
        OrderCreateReadable ocr3 = orderFeedbackHelper.putOrderFeedback(orderIds.get(2), CLIENT_ID,
            orderCreatable, FeedbackType.DSBS);
        assertEquals(List.of(question1), ocr1.getFeedback().getQuestions());
        assertEquals(List.of(question1), ocr2.getFeedback().getQuestions());
        assertEquals(List.of(question2), ocr3.getFeedback().getQuestions());
    }

    @Test
    public void testFeedbackDeliveredOnTime() throws Exception {
        OrderFeedbackQuestion question1 = orderQuestionHelper.putQuestion(
            OrderQuestionCreatebleBuilder.builder().withTitle("question1").build());
        OrderFeedbackQuestion question2 = orderQuestionHelper.putQuestion(
            OrderQuestionCreatebleBuilder.builder().withTitle("question2").build());
        OrderFeedbackQuestion question3 = orderQuestionHelper.putQuestion(
            OrderQuestionCreatebleBuilder.builder().withTitle("question3").build());

        orderQuestionRuleHelper.addRuleWithDeliveredOnTime(question1, DeliveredOnTime.UNKNOWN);
        orderQuestionRuleHelper.addRuleWithDeliveredOnTime(question2, DeliveredOnTime.LATE);
        orderQuestionRuleHelper.addRuleWithDeliveredOnTime(question3, DeliveredOnTime.ON_TIME);

        OrderCreatable orderCreatable = OrderCreatableBuilder.builder().withGrade(5).build();
        Date dateInPast = Date.from(clock.instant().minus(5, ChronoUnit.DAYS));
        Order order1 = OrderBuilder.builder().withStatusUpdateDate(dateInPast).build();
        checkouterMockConfigurer.mockGetOrder(ORDER_1, ClientRole.USER, CLIENT_ID, order1);
        Order order2 = OrderBuilder.builder().withDeliveryDates(dateInPast, dateInPast).build();
        checkouterMockConfigurer.mockGetOrder(ORDER_2, ClientRole.USER, CLIENT_ID, order2);


        OrderCreateReadable ocr1 = orderFeedbackHelper.putOrderFeedback(ORDER_1, CLIENT_ID, orderCreatable, null);
        OrderCreateReadable ocr2 = orderFeedbackHelper.putOrderFeedback(ORDER_2, CLIENT_ID, orderCreatable, null);
        assertEquals(List.of(question1, question3), ocr1.getFeedback().getQuestions());
        assertEquals(List.of(question1, question2), ocr2.getFeedback().getQuestions());
    }

    @Test
    public void testFeedbackWithCategories() throws Exception {
        OrderFeedbackQuestion question1 = orderQuestionHelper.putQuestion(
            OrderQuestionCreatebleBuilder.builder().withTitle("question1").build());
        OrderFeedbackQuestion question2 = orderQuestionHelper.putQuestion(
            OrderQuestionCreatebleBuilder.builder().withTitle("question2").build());
        OrderFeedbackQuestion question3 = orderQuestionHelper.putQuestion(
            OrderQuestionCreatebleBuilder.builder().withTitle("question3").build());

        orderQuestionRuleHelper.addRuleWithCategory(question1, QuestionCategory.UNKNOWN);
        orderQuestionRuleHelper.addRuleWithCategory(question2, QuestionCategory.DELIVERY);
        orderQuestionRuleHelper.addRuleWithCategory(question3, QuestionCategory.PRODUCT);

        OrderCreatable orderCreatable = OrderCreatableBuilder.builder().withGrade(5).build();
        Order order = OrderBuilder.builder().build();
        checkouterMockConfigurer.mockGetOrder(ORDER_1, ClientRole.USER, CLIENT_ID, order);

        OrderCreateReadable ocr1 = orderFeedbackHelper.putOrderFeedback(ORDER_1, CLIENT_ID, orderCreatable, null);
        List<OrderFeedbackQuestion> actualQuestions = ocr1.getFeedback().getQuestions();
        assertEquals(3, actualQuestions.size());
        assertEquals(QuestionCategory.UNKNOWN, actualQuestions.get(0).getCategory());
        assertEquals(QuestionCategory.DELIVERY, actualQuestions.get(1).getCategory());
        assertEquals(QuestionCategory.PRODUCT, actualQuestions.get(2).getCategory());
    }

    @Test
    public void testFeedbackWithOutletPurpose() throws Exception {
        OrderFeedbackQuestion question1 = orderQuestionHelper.putQuestion(
            OrderQuestionCreatebleBuilder.builder().withTitle("question1").build());
        OrderFeedbackQuestion question2 = orderQuestionHelper.putQuestion(
            OrderQuestionCreatebleBuilder.builder().withTitle("question2").build());
        OrderFeedbackQuestion question3 = orderQuestionHelper.putQuestion(
            OrderQuestionCreatebleBuilder.builder().withTitle("question3").build());

        orderQuestionRuleHelper.addRuleWithOutletPurpose(question1, OutletPurpose.UNKNOWN);
        orderQuestionRuleHelper.addRuleWithOutletPurpose(question2, OutletPurpose.PICKUP);
        orderQuestionRuleHelper.addRuleWithOutletPurpose(question3, OutletPurpose.STORE);

        OrderCreatable orderCreatable = OrderCreatableBuilder.builder().withGrade(5).build();
        Order order1 = OrderBuilder.builder().withOutletPurpose(OutletPurpose.UNKNOWN).build();
        checkouterMockConfigurer.mockGetOrder(ORDER_1, ClientRole.USER, CLIENT_ID, order1);
        Order order2 = OrderBuilder.builder().withOutletPurpose(OutletPurpose.PICKUP).build();
        checkouterMockConfigurer.mockGetOrder(ORDER_2, ClientRole.USER, CLIENT_ID, order2);
        Order order3 = OrderBuilder.builder().withOutletPurpose(OutletPurpose.STORE).build();
        checkouterMockConfigurer.mockGetOrder(ORDER_3, ClientRole.USER, CLIENT_ID, order3);


        OrderCreateReadable ocr1 = orderFeedbackHelper.putOrderFeedback(ORDER_1, CLIENT_ID, orderCreatable, null);
        OrderCreateReadable ocr2 = orderFeedbackHelper.putOrderFeedback(ORDER_2, CLIENT_ID, orderCreatable, null);
        OrderCreateReadable ocr3 = orderFeedbackHelper.putOrderFeedback(ORDER_3, CLIENT_ID, orderCreatable, null);
        assertEquals(List.of(question1), ocr1.getFeedback().getQuestions());
        assertEquals(List.of(question1, question2), ocr2.getFeedback().getQuestions());
        assertEquals(List.of(question1, question3), ocr3.getFeedback().getQuestions());
    }

    @Test
    public void testFeedbackNullOutletPurpose() throws Exception {
        OrderFeedbackQuestion question1 = orderQuestionHelper.putQuestion(
            OrderQuestionCreatebleBuilder.builder().withTitle("question1").build());
        OrderFeedbackQuestion question2 = orderQuestionHelper.putQuestion(
            OrderQuestionCreatebleBuilder.builder().withTitle("question2").build());

        orderQuestionRuleHelper.addRuleWithOutletPurpose(question1, OutletPurpose.UNKNOWN);
        orderQuestionRuleHelper.addRuleWithOutletPurpose(question2, OutletPurpose.PICKUP);

        OrderCreatable orderCreatable = OrderCreatableBuilder.builder().withGrade(5).build();
        Order order1 = OrderBuilder.builder().build(); // No OutletPurpose in Checkouter response
        checkouterMockConfigurer.mockGetOrder(ORDER_1, ClientRole.USER, CLIENT_ID, order1);


        OrderCreateReadable ocr1 = orderFeedbackHelper.putOrderFeedback(ORDER_1, CLIENT_ID, orderCreatable, null);
        assertEquals(List.of(question1), ocr1.getFeedback().getQuestions());
    }

    @Test
    public void testAgitationComplition() throws Exception {
        OrderCreatable orderCreatable = OrderCreatableBuilder.builder().withGrade(5).build();
        Order order = OrderBuilder.builder().build();
        checkouterMockConfigurer.mockGetOrder(ORDER_1, ClientRole.USER, CLIENT_ID, order);

        OrderCreateReadable ocr =
            orderFeedbackHelper.putOrderFeedback(ORDER_1, CLIENT_ID, orderCreatable, FeedbackType.ORDER);

        Mockito.verify(authorClient).completeAgitation(AgitationUserType.UID,
            String.valueOf(ocr.getFeedback().getUserId()),
            AgitationType.ORDER_FEEDBACK,
            String.valueOf(ocr.getFeedback().getOrderId()));
    }

    @Test
    public void testEnumLoading() throws Exception {
        int clientId = 321;
        long orderId1 = 123L;
        jdbcTemplate.update("INSERT INTO order_feedback (id, order_id, grade, is_callback_required, " +
                "is_review_denied, created_at, updated_at, comment, " +
                "is_review_submitted, user_id, feedback_type, nps_grade) " +
                "VALUES (nextval('order_feedback_seq'), ?, 5, false, false, current_timestamp, current_timestamp, " +
                "'comment', true, ?, ?, ?)",
            orderId1, clientId, FeedbackType.UNKNOWN.value(), 0);
        long orderId2 = 234L;
        jdbcTemplate.update("INSERT INTO order_feedback (id, order_id, grade, is_callback_required, " +
                "is_review_denied, created_at, updated_at, comment, " +
                "is_review_submitted, user_id, feedback_type, nps_grade) " +
                "VALUES (nextval('order_feedback_seq'), ?, 5, false, false, current_timestamp, current_timestamp, " +
                "'comment', true, ?, ?, ?)",
            orderId2, clientId, FeedbackType.ORDER.value(), 0);
        mockOrders(List.of(orderId1, orderId2), clientId);

        OrderReadable orderFeedback1 = orderFeedbackHelper.getOrderFeedback(orderId1, clientId);
        assertEquals(FeedbackType.UNKNOWN, orderFeedback1.getFeedback().getFeedbackType());

        OrderReadable orderFeedback2 = orderFeedbackHelper.getOrderFeedback(orderId2, clientId);
        assertEquals(FeedbackType.ORDER, orderFeedback2.getFeedback().getFeedbackType());
    }

    @Test
    public void testEnumSaving() throws Exception {
        int clientId = 321;
        long orderId1 = 123L;
        long orderId2 = 234L;
        mockOrders(List.of(orderId1, orderId2), clientId);
        OrderCreatable orderCreatable = OrderCreatableBuilder.builder().withGrade(5).withNpsGrade(5).build();

        orderFeedbackHelper.putOrderFeedback(orderId1, clientId, orderCreatable, FeedbackType.UNKNOWN);
        orderFeedbackHelper.putOrderFeedback(orderId2, clientId, orderCreatable, FeedbackType.ORDER);

        Integer feedbackType1 = jdbcTemplate.queryForObject("SELECT feedback_type FROM order_feedback WHERE order_id " +
                "= ?",
            Integer.class, orderId1);
        assertEquals(FeedbackType.UNKNOWN, FeedbackType.byValue(feedbackType1));
        Integer feedbackType2 = jdbcTemplate.queryForObject("SELECT feedback_type FROM order_feedback WHERE order_id " +
                "= ?",
            Integer.class, orderId2);
        assertEquals(FeedbackType.ORDER, FeedbackType.byValue(feedbackType2));
    }

    @Test
    public void testNpsGrade() throws Exception {
        int clientId = 321;
        long orderId1 = 123L;
        long orderId2 = 234L;
        mockOrders(List.of(orderId1, orderId2), clientId);
        OrderCreatable orderCreatable1 = OrderCreatableBuilder.builder().withGrade(5).build();
        OrderCreatable orderCreatable2 = OrderCreatableBuilder.builder().withGrade(5).withNpsGrade(6).build();

        assertNull(orderFeedbackHelper
            .putOrderFeedback(orderId1, clientId, orderCreatable1, FeedbackType.UNKNOWN)
            .getFeedback()
            .getNpsGrade());
        assertEquals(6, orderFeedbackHelper
            .putOrderFeedback(orderId2, clientId, orderCreatable2, FeedbackType.ORDER)
            .getFeedback()
            .getNpsGrade());

    }

    // test for handling old entities without userId
    @Test
    public void testNullUserId() throws Exception {
        int clientId = 321;
        long orderId = 123L;
        jdbcTemplate.update("INSERT INTO order_feedback " +
                "VALUES (nextval('order_feedback_seq'), ?, 5, false, false, current_timestamp, current_timestamp, " +
                "'comment', true, ?, ?)",
            orderId, clientId, FeedbackType.ORDER.value());
        mockOrders(List.of(orderId), clientId);

        // returns correct userId because takes it from request
        OrderReadable orderFeedback1 = orderFeedbackHelper.getOrderFeedback(orderId, clientId);
        assertEquals(clientId, orderFeedback1.getFeedback().getUserId());
    }

    @Test
    public void testAnswerWithItemSaving() throws Exception {
        OrderFeedbackQuestion question1 = orderQuestionHelper.putQuestion(
            OrderQuestionCreatebleBuilder.builder().withTitle("question3").build());
        orderQuestionRuleHelper.addRuleWithFeedbackType(question1, FeedbackType.ORDER);
        mockOrders(List.of(ORDER_1, ORDER_2, ORDER_3, ORDER_4), CLIENT_ID);
        String template = file("/data/order_feedback_with_answer.json");

        String good = String.format(template, 1, 1);
        orderFeedbackHelper.putRawOrderFeedbackForActions(ORDER_1, CLIENT_ID, good,
            FeedbackType.ORDER)
            .andExpect(status().isOk());

        String skuIdIsNull = String.format(template, null, 1);
        orderFeedbackHelper.putRawOrderFeedbackForActions(ORDER_2, CLIENT_ID, skuIdIsNull, FeedbackType.ORDER)
            .andExpect(status().is4xxClientError());

        String supplierIdIsStringNull = String.format(template, "1", "null");
        orderFeedbackHelper.putRawOrderFeedbackForActions(ORDER_3, CLIENT_ID, supplierIdIsStringNull, FeedbackType.ORDER)
            .andExpect(status().is4xxClientError());

        String skuIdIsUndefined = String.format(template, "undefined", "123");
        orderFeedbackHelper.putRawOrderFeedbackForActions(ORDER_4, CLIENT_ID, skuIdIsUndefined, FeedbackType.ORDER)
            .andExpect(status().is4xxClientError());
    }

    @Test
    public void testAnswerWithoutItemSaving() throws Exception {
        OrderFeedbackQuestion question1 = orderQuestionHelper.putQuestion(
            OrderQuestionCreatebleBuilder.builder().withTitle("question3").build());
        orderQuestionRuleHelper.addRuleWithFeedbackType(question1, FeedbackType.ORDER);
        mockOrders(List.of(ORDER_1), CLIENT_ID);
        String template = file("/data/order_feedback_with_answer_without_item.json");

        orderFeedbackHelper.putRawOrderFeedbackForActions(ORDER_1, CLIENT_ID, template, FeedbackType.ORDER)
            .andExpect(status().isOk());
    }

    private void mockOrders(List<Long> orderIds, long clientId) throws Exception {
        Order order = OrderBuilder.builder().build();
        for (Long orderId : orderIds) {
            checkouterMockConfigurer.mockGetOrder(orderId, ClientRole.USER, clientId, order);
        }
    }

    private String file(String file) throws IOException {
        return IOUtils.readInputStream(
            getClass().getResourceAsStream(file));
    }
}
