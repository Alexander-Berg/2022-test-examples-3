package ru.yandex.market.pers.feedback.order;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.pers.feedback.builder.OrderBuilder;
import ru.yandex.market.pers.feedback.config.AbstractPersFeedbackTest;
import ru.yandex.market.pers.feedback.helper.OrderFeedbackHelper;
import ru.yandex.market.pers.feedback.order.api.OrderCreatable;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderFeedbackValidationTest extends AbstractPersFeedbackTest {
    @Autowired
    private OrderFeedbackHelper orderFeedbackHelper;

    private final long orderId = 1234L;
    private final int clientId = 2345;
    private final String comment = "a";

    @Test
    public void shouldFailWhenCommentIsTooLong() throws Exception {
        String comment = "a".repeat(4001);

        OrderCreatable body = new OrderCreatable(
                1,
                false,
                false,
                false,
                comment,
                List.of()
        );

        orderFeedbackHelper.putOrderFeedbackForActions(orderId, clientId, body, null)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldFailOnNegativeGrade() throws Exception {
        OrderCreatable body = new OrderCreatable(
                -1,
                false,
                false,
                false,
                comment,
                List.of()
        );

        orderFeedbackHelper.putOrderFeedbackForActions(orderId, clientId, body, null)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldFailOnTooBigGrade() throws Exception {
        OrderCreatable body = new OrderCreatable(
                6,
                false,
                false,
                false,
                comment,
                List.of()
        );

        orderFeedbackHelper.putOrderFeedbackForActions(orderId, clientId, body, null)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldNotFailOnEmptyGrade() throws Exception {
        OrderCreatable body = new OrderCreatable(
                null,
                false,
                false,
                false,
                comment,
                List.of()
        );

        Order order = OrderBuilder.builder()
                .build();

        checkouterMockConfigurer.mockGetOrder(orderId, ClientRole.USER, clientId, order);

        orderFeedbackHelper.putOrderFeedbackForActions(orderId, clientId, body, null)
                .andExpect(status().isOk());
    }

    @Test
    public void shouldFailIfOrderIsNotFound() throws Exception {
        OrderCreatable body = new OrderCreatable(
                null,
                false,
                false,
                false,
                comment,
                List.of()
        );

        checkouterMockConfigurer.mockGetOrderNotFound(orderId, ClientRole.USER, clientId);

        orderFeedbackHelper.putOrderFeedbackForActions(orderId, clientId, body, null)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldFailIfNoOrderFound() throws Exception {
        checkouterMockConfigurer.mockGetOrder(orderId, ClientRole.USER, clientId, null);
        ResultMatcher reason = status().reason("No Order found for given orderId");
        orderFeedbackHelper.getOrderFeedback(orderId, clientId, status().is4xxClientError(), reason);
    }

    @Test
    public void shouldFailOnInvalidNpsGrade() throws Exception {
        OrderCreatable body = new OrderCreatable(
            1,
            false,
            false,
            false,
            comment,
            List.of()
        ).setNpsGrade(11);

        orderFeedbackHelper.putOrderFeedbackForActions(orderId, clientId, body, null)
            .andExpect(status().isBadRequest());
    }
}
