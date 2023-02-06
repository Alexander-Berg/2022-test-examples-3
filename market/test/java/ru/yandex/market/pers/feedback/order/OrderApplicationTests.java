package ru.yandex.market.pers.feedback.order;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.collections.CollectionUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.pers.feedback.builder.OrderBuilder;
import ru.yandex.market.pers.feedback.config.AbstractPersFeedbackTest;
import ru.yandex.market.pers.feedback.helper.OrderFeedbackHelper;
import ru.yandex.market.pers.feedback.helper.OrderGradesHelper;
import ru.yandex.market.pers.feedback.helper.OrderQuestionHelper;
import ru.yandex.market.pers.feedback.order.api.OrderAnswer;
import ru.yandex.market.pers.feedback.order.api.OrderCreatable;
import ru.yandex.market.pers.feedback.order.api.OrderCreateReadable;
import ru.yandex.market.pers.feedback.order.api.OrderFeedbackQuestion;
import ru.yandex.market.pers.feedback.order.api.OrderGrade;
import ru.yandex.market.pers.feedback.order.api.OrderGradesReadable;
import ru.yandex.market.pers.feedback.order.api.OrderQuestionCreatable;
import ru.yandex.market.pers.feedback.order.api.OrderReadable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class OrderApplicationTests extends AbstractPersFeedbackTest {

    @Autowired
    private WebApplicationContext wac;
    @Autowired
    private OrderQuestionHelper orderQuestionHelper;
    @Autowired
    private OrderFeedbackHelper orderFeedbackHelper;
    @Autowired
    private OrderGradesHelper orderGradesHelper;

    @BeforeEach
    public void setup() {
        this.mockMvc = webAppContextSetup(this.wac).build();
    }

    @Test
    public void shouldSuccessfullyPutAndOverwriteFeedback() throws Exception {
        OrderFeedbackQuestion question1 = orderQuestionHelper.putQuestion(new OrderQuestionCreatable(
                "Курьер приехал точно в срок"));
        OrderFeedbackQuestion question2 = orderQuestionHelper.putQuestion(new OrderQuestionCreatable(
                "Заказ подняли на этаж"));
        OrderFeedbackQuestion question3 = orderQuestionHelper.putQuestion(new OrderQuestionCreatable(
                "Курьер был вежлив"));

        OrderCreatable orderCreatableSpacer = new OrderCreatable(
                5,
                false,
                false,
                false,
                "Мне доставка категорически понравилась",
                Arrays.asList(
                        new OrderAnswer(question1.getId()),
                        new OrderAnswer(question2.getId())
                )
        );
        checkPutFeedback(321L, orderCreatableSpacer);

        OrderCreatable orderCreatable = new OrderCreatable(
                5,
                false,
                false,
                false,
                "Мне доставка категорически понравилась",
                Arrays.asList(
                        new OrderAnswer(question1.getId()),
                        new OrderAnswer(question2.getId())
                )
        );
        checkPutFeedback(123L, orderCreatable);

        OrderCreatable orderCreatableOverwrite = new OrderCreatable(
                2,
                true,
                true,
                true,
                "Чой-то ваще не то",
                Arrays.asList(
                        new OrderAnswer(question2.getId()),
                        new OrderAnswer(question3.getId())
                )
        );
        checkPutFeedback(123L, orderCreatableOverwrite);

        OrderCreatable orderCreatableOverwriteSame = new OrderCreatable(
                2,
                true,
                true,
                true,
                "Чой-то ваще не то",
                Arrays.asList(
                        new OrderAnswer(question1.getId()),
                        new OrderAnswer(question2.getId())
                )
        );
        checkPutFeedback(123L, orderCreatableOverwriteSame);
    }

    @Test
    public void shouldSuccessfullyPutEmptyFeedback() throws Exception {
        OrderFeedbackQuestion question1 = orderQuestionHelper.putQuestion(new OrderQuestionCreatable(
                "Курьер приехал точно в срок"));
        OrderFeedbackQuestion question2 = orderQuestionHelper.putQuestion(new OrderQuestionCreatable(
                "Заказ подняли на этаж"));

        OrderCreatable orderCreatable = new OrderCreatable(
                5,
                false,
                false,
                false,
                "Мне доставка категорически понравилась",
                Arrays.asList(
                        new OrderAnswer(question1.getId()),
                        new OrderAnswer(question2.getId())
                )
        );
        long orderId = 123L;
        checkPutFeedback(orderId, orderCreatable);

        OrderCreateReadable orderCreateReadable = orderFeedbackHelper.putOrderFeedback(orderId, 1, orderCreatable, null);

        assertTrue(orderCreateReadable.isStatus());
        assertEquals(orderCreatable.getGrade(), orderCreateReadable.getFeedback().getGrade());
        assertEquals(orderCreatable.getIsCallbackRequired(), orderCreateReadable.getFeedback().getIsCallbackRequired());
        assertEquals(orderCreatable.getIsReviewDenied(), orderCreateReadable.getFeedback().getIsReviewDenied());
        assertEquals(orderCreatable.getComment(), orderCreateReadable.getFeedback().getComment());
        assertEquals(orderCreatable.getAnswers(), orderCreateReadable.getFeedback().getAnswers());
    }

    @Test
    public void shouldSuccessfullyGetGrades() throws Exception {
        OrderFeedbackQuestion question1 = orderQuestionHelper.putQuestion(new OrderQuestionCreatable(
                "Курьер приехал точно в срок"));
        OrderFeedbackQuestion question2 = orderQuestionHelper.putQuestion(new OrderQuestionCreatable(
                "Заказ подняли на этаж"));

        OrderCreatable orderCreatable = new OrderCreatable(
                5,
                false,
                true,
                true,
                "Мне доставка категорически понравилась",
                Arrays.asList(
                        new OrderAnswer(question1.getId()),
                        new OrderAnswer(question2.getId())
                )
        );

        long notExistOrderId = 321L;

        long existOrderId = 123L;
        checkPutFeedback(existOrderId, orderCreatable);

        Set<Long> orderIds = Set.of(notExistOrderId, existOrderId);
        checkouterMockConfigurer.mockPostGetAccessibleOrderIds(ClientRole.USER, 1, orderIds);

        OrderGradesReadable orderGradesReadable = orderGradesHelper.getOrderGrades(List.of(notExistOrderId, existOrderId));
        assertTrue(orderGradesReadable.isStatus());
        List<OrderGrade> grades = orderGradesReadable.getGrades();
        assertEquals(2, grades.size());


        OrderGrade orderGradeEmpty = grades.get(0);
        assertEquals(notExistOrderId, orderGradeEmpty.getOrderId());
        assertEquals(0, orderGradeEmpty.getGrade());
        assertFalse(orderGradeEmpty.getIsReviewDenied());
        assertFalse(orderGradeEmpty.getIsReviewSubmitted());

        OrderGrade orderGradeExist = grades.get(1);
        assertEquals(existOrderId, orderGradeExist.getOrderId());
        assertEquals(orderCreatable.getGrade(), orderGradeExist.getGrade());
        assertEquals(orderCreatable.getIsReviewDenied(), orderGradeExist.getIsReviewDenied());
        assertEquals(orderCreatable.getIsReviewSubmitted(), orderGradeExist.getIsReviewSubmitted());
    }



    @Test
    void shouldNotReturnDuplicatedOrdersIfGotDuplicatedOrderIds() throws Exception {
        OrderFeedbackQuestion question1 = orderQuestionHelper.putQuestion(new OrderQuestionCreatable(
                "Курьер приехал точно в срок"
        ));
        OrderFeedbackQuestion question2 = orderQuestionHelper.putQuestion(new OrderQuestionCreatable(
                "Заказ подняли на этаж"
        ));
        OrderCreatable orderCreatable = new OrderCreatable(
                5,
                false,
                true,
                true,
                "Мне доставка категорически понравилась",
                Arrays.asList(
                        new OrderAnswer(question1.getId()),
                        new OrderAnswer(question2.getId())
                )
        );

        long firstOrderId = 123L;
        long secondOrderId = 234L;

        checkPutFeedback(firstOrderId, orderCreatable);
        checkPutFeedback(secondOrderId, orderCreatable);

        Set<Long> orderIds = Set.of(firstOrderId, secondOrderId);
        checkouterMockConfigurer.mockPostGetAccessibleOrderIds(ClientRole.USER, 1, orderIds);

        OrderGradesReadable gradesReadable = orderGradesHelper.getOrderGrades(List.of(firstOrderId, firstOrderId,
                firstOrderId, secondOrderId, secondOrderId));
        MatcherAssert.assertThat(gradesReadable.isStatus(), CoreMatchers.is(true));
        MatcherAssert.assertThat(gradesReadable.getGrades(), Matchers.hasSize(2));
        MatcherAssert.assertThat(gradesReadable.getGrades(), CoreMatchers.allOf(
                CoreMatchers.hasItem(Matchers.hasProperty("orderId", CoreMatchers.is(firstOrderId))),
                CoreMatchers.hasItem(Matchers.hasProperty("orderId", CoreMatchers.is(secondOrderId)))
        ));
    }

    @Test
    void shouldReturnOrdersIfGotEmptyOrderIdsFromCheckouter() throws Exception {
        OrderFeedbackQuestion question1 = orderQuestionHelper.putQuestion(new OrderQuestionCreatable(
                "Курьер приехал точно в срок"
        ));
        OrderFeedbackQuestion question2 = orderQuestionHelper.putQuestion(new OrderQuestionCreatable(
                "Заказ подняли на этаж"
        ));
        OrderCreatable orderCreatable = new OrderCreatable(
                5,
                false,
                true,
                true,
                "Мне доставка категорически понравилась",
                Arrays.asList(
                        new OrderAnswer(question1.getId()),
                        new OrderAnswer(question2.getId())
                )
        );

        long firstOrderId = 123L;
        long secondOrderId = 234L;

        checkPutFeedback(firstOrderId, orderCreatable);
        checkPutFeedback(secondOrderId, orderCreatable);

        checkouterMockConfigurer.mockPostGetAccessibleOrderIds(ClientRole.USER, 1, Collections.emptySet());

        OrderGradesReadable gradesReadable = orderGradesHelper.getOrderGrades(List.of(firstOrderId, secondOrderId));
        MatcherAssert.assertThat(gradesReadable.isStatus(), CoreMatchers.is(true));
        MatcherAssert.assertThat(gradesReadable.getGrades(), Matchers.hasSize(2));
        MatcherAssert.assertThat(gradesReadable.getGrades(), CoreMatchers.allOf(
                CoreMatchers.hasItem(Matchers.hasProperty("orderId", CoreMatchers.is(firstOrderId))),
                CoreMatchers.hasItem(Matchers.hasProperty("orderId", CoreMatchers.is(secondOrderId)))
        ));
    }

    @Test
    public void testOverwriteNpsGrade() throws Exception {
        OrderFeedbackQuestion question1 = orderQuestionHelper.putQuestion(new OrderQuestionCreatable(
            "Курьер приехал точно в срок"
        ));
        OrderFeedbackQuestion question2 = orderQuestionHelper.putQuestion(new OrderQuestionCreatable(
            "Заказ подняли на этаж"
        ));
        OrderCreatable orderCreatable = new OrderCreatable(
            5,
            false,
            true,
            true,
            "Мне доставка категорически понравилась",
            Arrays.asList(
                new OrderAnswer(question1.getId()),
                new OrderAnswer(question2.getId())
            )
        );

        long orderId = 123L;
        checkPutFeedback(orderId, orderCreatable);

        PagedOrders pagedOrders = new PagedOrders();
        pagedOrders.setItems(List.of(
            OrderBuilder.builder().withId(orderId).build()
        ));
        checkouterMockConfigurer.mockPostGetOrders(ClientRole.USER, 1, pagedOrders);

        OrderReadable orderReadable = orderFeedbackHelper.getOrderFeedback(orderId, 1);
        assertNull(orderReadable.getFeedback().getNpsGrade());

        orderCreatable.setNpsGrade(7);
        checkPutFeedback(orderId, orderCreatable);
        orderReadable = orderFeedbackHelper.getOrderFeedback(orderId, 1);
        assertEquals(7, orderReadable.getFeedback().getNpsGrade());

    }

    @Nonnull
    private OrderCreateReadable checkPutFeedback(long orderId,
                                                 @Nonnull OrderCreatable orderCreatable) throws Exception {

        Order order = OrderBuilder.builder().withStatusUpdateDate(new Date()).build();

        checkouterMockConfigurer.mockGetOrder(orderId, ClientRole.USER, 1L, order);

        OrderCreateReadable orderCreateReadable = orderFeedbackHelper.putOrderFeedback(orderId, 1, orderCreatable, null);

        assertTrue(orderCreateReadable.isStatus());
        assertEquals(orderCreatable.getGrade(), orderCreateReadable.getFeedback().getGrade());
        assertEquals(orderCreatable.getIsCallbackRequired(), orderCreateReadable.getFeedback().getIsCallbackRequired());
        assertEquals(orderCreatable.getIsReviewDenied(), orderCreateReadable.getFeedback().getIsReviewDenied());
        assertEquals(orderCreatable.getIsReviewSubmitted(), orderCreateReadable.getFeedback().getIsReviewSubmitted());
        assertEquals(orderCreatable.getComment(), orderCreateReadable.getFeedback().getComment());
        assertTrue(orderCreatable.getAnswers() != null &&
            CollectionUtils.isEqualCollection(orderCreatable.getAnswers(), orderCreateReadable.getFeedback().getAnswers()));

        OrderReadable orderReadable = orderFeedbackHelper.getOrderFeedback(orderId, 1);
        assertTrue(orderReadable.isStatus());
        assertEquals(orderCreatable.getGrade(), orderReadable.getFeedback().getGrade());
        assertEquals(orderCreatable.getIsCallbackRequired(), orderReadable.getFeedback().getIsCallbackRequired());
        assertEquals(orderCreatable.getIsReviewDenied(), orderReadable.getFeedback().getIsReviewDenied());
        assertEquals(orderCreatable.getComment(), orderReadable.getFeedback().getComment());
        assertTrue(orderCreatable.getAnswers() != null &&
            CollectionUtils.isEqualCollection(orderCreatable.getAnswers(), orderReadable.getFeedback().getAnswers()));
        assertEquals(orderCreatable.getNpsGrade(), orderReadable.getFeedback().getNpsGrade());

        return orderCreateReadable;
    }

}
