package ru.yandex.market.checkout.checkouter.order.getOrder;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.backbone.order.reservation.OrderCompletionService;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.market.checkout.test.providers.BuyerProvider.UID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID;
import static ru.yandex.market.checkout.util.matching.NumberMatcher.numberEqualsTo;

/**
 * @author : poluektov
 * date: 25.07.17.
 */
public class OrderDeliveryReceivedFlagTest extends AbstractWebTestBase {
    @Autowired
    private OrderCompletionService orderCompletionService;

    private long orderId;
    private final boolean received = true;

    @BeforeEach
    public void createOrder() {
        orderId = OrderServiceHelper.createPostPaidOrder(orderCreateService, orderUpdateService,
                orderCompletionService);
        orderStatusHelper.updateOrderStatus(orderId, OrderStatus.PROCESSING);
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS)
    @DisplayName("Фильтрация по userReceived")
    @Test
    public void testSetDeliveryReceived() throws Exception {
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.DELIVERY);
        orderUpdateService.updateDeliveryReceived(orderId, new ClientInfo(ClientRole.USER, UID), received);
        Order order = orderService.getOrder(orderId);
        assertThat(order.getDelivery().getUserReceived(), equalTo(received));
        checkGetOrdersFilter();
    }

    private void checkGetOrdersFilter() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/orders?shopId=" + SHOP_ID +
                "&clientRole=SYSTEM&userReceived=" + received)
                .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name()))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.orders[*].id").
                        value(hasItem(numberEqualsTo(orderId))));

        mockMvc.perform(MockMvcRequestBuilders.get("/orders?shopId=" + SHOP_ID +
                "&clientRole=SYSTEM&userReceived=" + false)
                .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name()))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(containsString("\"orders\":[]")));
    }
}
