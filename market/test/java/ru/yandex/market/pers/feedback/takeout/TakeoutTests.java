package ru.yandex.market.pers.feedback.takeout;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.pers.feedback.builder.OrderBuilder;
import ru.yandex.market.pers.feedback.config.AbstractPersFeedbackTest;
import ru.yandex.market.pers.feedback.helper.OrderFeedbackHelper;
import ru.yandex.market.pers.feedback.helper.OrderQuestionHelper;
import ru.yandex.market.pers.feedback.helper.TakeoutHelper;
import ru.yandex.market.pers.feedback.order.api.OrderAnswer;
import ru.yandex.market.pers.feedback.order.api.OrderCreatable;
import ru.yandex.market.pers.feedback.order.api.OrderFeedbackQuestion;
import ru.yandex.market.pers.feedback.order.api.OrderQuestionCreatable;
import ru.yandex.market.pers.feedback.order.api.TakeoutDataWrapper;
import ru.yandex.market.pers.feedback.order.api.TakeoutOrderFeedback;
import ru.yandex.market.pers.service.common.dto.TakeoutStatusDto;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

public class TakeoutTests extends AbstractPersFeedbackTest {

    @Autowired
    private WebApplicationContext wac;
    @Autowired
    private OrderFeedbackHelper orderFeedbackHelper;
    @Autowired
    private OrderQuestionHelper orderQuestionHelper;
    @Autowired
    private TakeoutHelper takeoutHelper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setup() {
        this.mockMvc = webAppContextSetup(this.wac).build();
    }

    @Test
    public void shouldReturnFeedbacksForTakeout() throws Exception {
        String question1Title = "Курьер приехал точно в срок";
        OrderFeedbackQuestion question1 = orderQuestionHelper.putQuestion(new OrderQuestionCreatable(question1Title));
        String question2Title = "Заказ подняли на этаж";
        OrderFeedbackQuestion question2 = orderQuestionHelper.putQuestion(new OrderQuestionCreatable(question2Title));
        String comment = "Мне доставка категорически понравилась";
        int grade = 5;
        OrderCreatable orderCreatable = new OrderCreatable(
                grade,
                false,
                true,
                true,
                comment,
                Arrays.asList(
                        new OrderAnswer(question1.getId()),
                        new OrderAnswer(question2.getId())
                )
        );

        long orderId = 123L;
        long clientId = 1L;
        createFeedback(orderId, orderCreatable);
        PagedOrders pagedOrders = new PagedOrders();
        pagedOrders.setItems(List.of(
                OrderBuilder.builder().withId(orderId).build()
        ));
        checkouterMockConfigurer.mockPostGetOrders(ClientRole.USER, clientId, pagedOrders);

        TakeoutDataWrapper data = takeoutHelper.getData(clientId);
        assertNotNull(data);
        MatcherAssert.assertThat(data.getFeedbacks(), Matchers.hasSize(1));

        TakeoutOrderFeedback feedback = data.getFeedbacks().get(0);
        MatcherAssert.assertThat(feedback.getOrderId(), CoreMatchers.is(orderId));
        MatcherAssert.assertThat(feedback.getComment(), CoreMatchers.is(comment));
        MatcherAssert.assertThat(feedback.getGrade(), CoreMatchers.is(grade));
        MatcherAssert.assertThat(feedback.getAnswers(), Matchers.hasSize(2));
        MatcherAssert.assertThat(feedback.getAnswers(), CoreMatchers.allOf(
                CoreMatchers.hasItem(Matchers.hasProperty("question", CoreMatchers.is(question1Title))),
                CoreMatchers.hasItem(Matchers.hasProperty("question", CoreMatchers.is(question2Title)))
        ));
    }

    @Test
    public void shouldReturnStatusForTakeout() throws Exception {
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
        long clientId = 1L;
        createFeedback(orderId, orderCreatable);
        PagedOrders pagedOrders = new PagedOrders();
        pagedOrders.setItems(List.of(
                OrderBuilder.builder().withId(orderId).build()
        ));
        checkouterMockConfigurer.mockPostGetOrders(ClientRole.USER, clientId, pagedOrders);

        TakeoutStatusDto status = takeoutHelper.getStatus(clientId);
        assertNotNull(status);
        MatcherAssert.assertThat(status.getTypes(), Matchers.arrayContaining(takeoutHelper.getFeedbackType()));
    }

    @Test
    public void shouldReturnEmptyStatusForTakeout() throws Exception {
        long clientId = 1L;
        PagedOrders pagedOrders = new PagedOrders();
        pagedOrders.setItems(List.of());
        checkouterMockConfigurer.mockPostGetOrders(ClientRole.USER, clientId, pagedOrders);
        TakeoutStatusDto status = takeoutHelper.getStatus(clientId);
        assertNotNull(status);
        MatcherAssert.assertThat(status.getTypes(), CoreMatchers.is(Matchers.emptyArray()));
    }

    @Test
    public void shouldDeleteHardFeedback() throws Exception {
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
        long clientId = 1L;
        createFeedback(orderId, orderCreatable);
        PagedOrders pagedOrders = new PagedOrders();
        pagedOrders.setItems(List.of(
                OrderBuilder.builder().withId(orderId).build(), OrderBuilder.builder().withId(orderId + 1).build()
        ));
        checkouterMockConfigurer.mockPostGetOrders(ClientRole.USER, clientId, pagedOrders);

        takeoutHelper.deleteHardFeedback(
                clientId, Set.of(takeoutHelper.getFeedbackType()), Instant.now().plusSeconds(10).toEpochMilli());
        Integer count = getFeedbacksCount(clientId, orderId);
        MatcherAssert.assertThat(count, CoreMatchers.is(0));
    }

    private void createFeedback(long orderId, @Nonnull OrderCreatable orderCreatable) throws Exception {
        Order order = OrderBuilder.builder().withStatusUpdateDate(new Date()).build();
        checkouterMockConfigurer.mockGetOrder(orderId, ClientRole.USER, 1L, order);
        orderFeedbackHelper.putOrderFeedback(orderId, 1, orderCreatable, null);
    }

    private Integer getFeedbacksCount(long clientId, long orderId) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM order_feedback WHERE user_id = ? AND order_id = ?",
                Integer.class,
                clientId,
                orderId);
    }
}
