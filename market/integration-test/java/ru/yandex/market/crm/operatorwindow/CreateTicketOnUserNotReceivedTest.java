package ru.yandex.market.crm.operatorwindow;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.crm.operatorwindow.jmf.entity.MarketTicket;
import ru.yandex.market.crm.operatorwindow.utils.ExpectedTicket;
import ru.yandex.market.crm.operatorwindow.utils.TestOrder;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.InternalComment;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.TicketCategory;
import ru.yandex.market.jmf.module.ticket.TicketTag;
import ru.yandex.market.ocrm.module.order.domain.Order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CreateTicketOnUserNotReceivedTest extends AbstractTicketOnUserNotReceivedTest {

    private static final Long DEFAULT_SERVICE_PRIORITY = 50L;

    private final TestOrder testOrder;
    private final ExpectedTicket expectedTicket;

    public CreateTicketOnUserNotReceivedTest() {
        this.testOrder = new TestOrder()
                .setStatus("DELIVERY")
                .setSubstatus("DELIVERY_USER_NOT_RECEIVED")
                .setDropshipping(false)
                .setPaymentType(PaymentType.PREPAID.name())
                .setPaymentMethod(PaymentMethod.APPLE_PAY.name());
        this.expectedTicket = new ExpectedTicket()
                .setTitle("Клиент не получил заказ, расчетная дата доставки просрочена - заказ "
                        + TestOrder.TEST_ORDER_NUMBER)
                .setClientName(TestOrder.TEST_BUYER_FULL_NAME)
                .setClientEmail(TestOrder.TEST_BUYER_EMAIL)
                .setClientPhone(TestOrder.TEST_BUYER_PHONE)
                .setServiceCode("marketQuestion")
                .setOrderNumber(TestOrder.TEST_ORDER_NUMBER)
                .setPriority(DEFAULT_SERVICE_PRIORITY)
                .setTags(Set.of())
                .setCategories(Set.of("marketDeliveryUserNotReceived"))
                .setComment(String.format("" +
                                "<p>Обращение создано по кнопке &#34;Заказ у меня&#34;.</p>\n" +
                                "<p>Клиент не получил заказ №%d</p>\n" +
                                "<p>Расчетная дата доставки просрочена.</p>\n" +
                                "<p>Нужно обратиться в магазин и уточнить, когда будет доставлен товар до " +
                                "клиента.</p>\n" +
                                "<p>Получили ответ с новой датой доставки, направь ответ клиенту по СМС.</p>",
                        TestOrder.TEST_ORDER_NUMBER));
    }

    /**
     * В тесте проверяется сценарий:
     * При получении заказа с статусом DELIVERY и подстатусом DELIVERY_USER_NOT_RECEIVED должно создаться обращение в
     * очереди marketQuestion с целью обратиться в магазин и уточнить, когда будет доставлен товар до
     * клиента.
     * <p>
     * Включает себя кейс из https://testpalm.yandex-team.ru/testcase/ocrm-1338
     */
    @Test
    public void testCreateTicket() {
        Order order = testOrderUtils.createOrder(testOrder);
        orderTestUtils.fireOrderImportedEvent(order, ClientRole.USER, HistoryEventType.ORDER_STATUS_UPDATED);
        MarketTicket actual = getSingleOpenedMarketTicket();

        assertEquals(expectedTicket.getTitle(), actual.getTitle());
        assertEquals(expectedTicket.getClientName(), actual.getClientName());
        assertEquals(expectedTicket.getClientEmail(), actual.getClientEmail());
        assertEquals(expectedTicket.getClientPhone(), actual.getClientPhone().getMain());

        assertNotNull(actual.getService());
        assertEquals(expectedTicket.getServiceCode(), actual.getService().getCode());

        assertNotNull(actual.getOrder());
        assertEquals(expectedTicket.getOrderNumber(), actual.getOrder().getTitle());

        assertEquals(expectedTicket.getPriority(), actual.getPriorityLevel());
        assertEquals(expectedTicket.getTags(), actual.getTags().stream()
                .map(TicketTag::getCode)
                .collect(Collectors.toSet())
        );
        assertEquals(expectedTicket.getCategories(), actual.getCategories().stream()
                .map(TicketCategory::getCode)
                .collect(Collectors.toSet())
        );
        assertSingleComment(actual, expectedTicket.getComment());
    }

    private void assertSingleComment(Ticket ticket, String expectedBody) {
        List<Comment> comments = dbService.list(Query.of(InternalComment.FQN)
                .withFilters(Filters.eq(Comment.ENTITY, ticket)));
        assertEquals(1, comments.size());
        if (null != expectedBody) {
            Comment comment = comments.get(0);
            assertEquals(expectedBody, comment.getBody());
        }
    }
}
