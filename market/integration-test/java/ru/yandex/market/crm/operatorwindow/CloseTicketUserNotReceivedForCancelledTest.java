package ru.yandex.market.crm.operatorwindow;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.crm.operatorwindow.jmf.entity.MarketTicket;
import ru.yandex.market.crm.operatorwindow.utils.TestOrder;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.module.chat.Ticket;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.ocrm.module.order.domain.Order;

public class CloseTicketUserNotReceivedForCancelledTest extends AbstractTicketOnUserNotReceivedTest {
    @Inject
    private BcpService bcpService;

    private TestOrder testOrder;

    private Order order;


    @BeforeEach
    public void setUp() {
        testOrder = new TestOrder()
                .setStatus("DELIVERY")
                .setSubstatus("DELIVERY_USER_NOT_RECEIVED")
                .setDropshipping(false)
                .setPaymentType(PaymentType.PREPAID.name())
                .setPaymentMethod(PaymentMethod.APPLE_PAY.name());
    }

    /**
     * В тесте проверяется сценарий:
     * Есть обращение, созданное по заказу order со статусом DELIVERY и подстатусом DELIVERY_USER_NOT_RECEIVED.
     * Это обращение должно закрыться при получении статуса CANCELLED по заказу order
     * <p>
     * Является одним из кейсов https://testpalm.yandex-team.ru/testcase/ocrm-1339
     */
    @Test
    public void shouldCloseTicketUserNotReceivedOnCancelled() {
        shouldCloseTicketUserNotReceived("CANCELLED");
    }

    /**
     * В тесте проверяется сценарий:
     * Есть обращение, созданное по заказу order со статусом DELIVERY и подстатусом DELIVERY_USER_NOT_RECEIVED.
     * Это обращение должно закрыться при получении статуса DELIVERED по заказу order
     * <p>
     * Является одним из кейсов https://testpalm.yandex-team.ru/testcase/ocrm-1339
     */
    @Test
    public void shouldCloseTicketUserNotReceivedOnDelivered() {
        shouldCloseTicketUserNotReceived("DELIVERED");
    }

    /**
     * В тесте проверяется сценарий:
     * Есть обращение, созданное по заказу order со статусом DELIVERY и подстатусом DELIVERY_USER_NOT_RECEIVED.
     * У обращения поменяли категорию обращения.
     * Это обращение НЕ должно закрыться при получении статуса DELIVERED по заказу order
     * <p>
     * Является одним из кейсов https://testpalm.yandex-team.ru/testcase/ocrm-1340
     */
    @Test
    public void shouldNotCloseTicketUserNotReceivedOnDelivered() {
        shouldNotCloseTicketUserNotReceived("DELIVERED");
    }

    /**
     * В тесте проверяется сценарий:
     * Есть обращение, созданное по заказу order со статусом DELIVERY и подстатусом DELIVERY_USER_NOT_RECEIVED.
     * У обращения поменяли категорию обращения
     * Это обращение НЕ должно закрыться при получении статуса CANCELLED по заказу order
     * <p>
     * Является одним из кейсов https://testpalm.yandex-team.ru/testcase/ocrm-1340
     */
    @Test
    public void shouldNotCloseTicketUserNotReceivedOnCancelled() {
        shouldNotCloseTicketUserNotReceived("CANCELLED");
    }

    private void shouldCloseTicketUserNotReceived(String newOrderStatus) {
        order = testOrderUtils.createOrder(testOrder);
        orderTestUtils.fireOrderImportedEvent(order);
        MarketTicket ticket = getSingleOpenedMarketTicket();

        updateOrderStatus(newOrderStatus);
        orderTestUtils.fireOrderImportedEvent(order, ClientRole.SYSTEM, HistoryEventType.ORDER_STATUS_UPDATED);

        List<MarketTicket> tickets = getActiveTickets();

        MatcherAssert.assertThat(tickets, Matchers.empty());
    }

    private void shouldNotCloseTicketUserNotReceived(String newOrderStatus) {
        order = testOrderUtils.createOrder(testOrder);
        orderTestUtils.fireOrderImportedEvent(order);
        MarketTicket ticket = getSingleOpenedMarketTicket();
        bcpService.edit(ticket, Maps.of(Ticket.CATEGORIES, null));

        updateOrderStatus(newOrderStatus);
        orderTestUtils.fireOrderImportedEvent(order, ClientRole.SYSTEM, HistoryEventType.ORDER_STATUS_UPDATED);

        getSingleOpenedMarketTicket();
    }

    private void updateOrderStatus(String status) {
        bcpService.edit(order, Map.of(
                Order.STATUS, status
        ));
    }
}
