package ru.yandex.market.crm.operatorwindow;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnDecisionType;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.returns.ReturnStatus;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.query.SortingOrder;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.InternalComment;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.ocrm.module.complaints.BeruComplaintsTicket;
import ru.yandex.market.ocrm.module.order.OrderReturnSource;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.domain.OrderItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@Transactional
public class UpdateReturnTicketOnReturnTest extends AbstractBeruComplaintsMailProcessingTest {

    private static final Long TEST_ORDER_NUMBER = Randoms.positiveLongValue();
    private static final Long TEST_ORDER_RETURN_ID = Randoms.positiveLongValue();
    private static final Long ORDER_ITEM_ID_1 = Randoms.positiveLongValue();
    private static final String ORDER_ITEM_TITLE_1 = Randoms.string();
    private static final Long ORDER_ITEM_ID_2 = ORDER_ITEM_ID_1 + 1;
    private static final String ORDER_ITEM_TITLE_2 = Randoms.string();

    private static final ReturnDecisionType RETURN_ITEM_DECISION_TYPE = ReturnDecisionType.REFUND_MONEY;
    private static final String RETURN_ITEM_DECISION_TYPE_TITLE = "Вернуть деньги покупателю";
    private static final String RETURN_ITEM_DECISION_COMMENT = Randoms.string();

    private static final String TEST_BUYER_EMAIL = Randoms.email();
    private static final String TEST_BUYER_PHONE = Randoms.phoneNumber();
    private static final String TEST_CLIENT_EMAIL = Randoms.email();
    private static final String TEST_CLIENT_FULL_NAME = Randoms.string();


    private static final String COMMENT_TEMPLATE = "" +
            "Магазин принял решения по позициям для возврата:<br />\n" +
            "<ul><li>[%s] - %s\n" +
            "  <ul><li>Решение по возврату: %s</li><li>Комментарий магазина: %s</li></ul>\n" +
            "</li></ul>";

    @Inject
    private OrderReturnSource orderReturnSource;

    @Test
    public void testTicketUpdatedWIthDecision() {
        Order order = createOrder();
        Return orderReturn = createOrderReturn();

        Return returnWithDecision = createOrderReturnWithDecision();

        when(orderReturnSource.getReturn(TEST_ORDER_RETURN_ID, false))
                .thenReturn(orderReturn)
                .thenReturn(returnWithDecision);

        // Создание тикета
        orderTestUtils.fireOrderImportedEvent(order, ClientRole.USER, HistoryEventType.ORDER_RETURN_CREATED,
                TEST_ORDER_RETURN_ID);

        // Изменение статуса решения
        orderTestUtils.fireOrderImportedEvent(order, ClientRole.SHOP_USER, HistoryEventType.ORDER_RETURN_STATUS_UPDATED,
                TEST_ORDER_RETURN_ID);

        BeruComplaintsTicket actual = getSingleOpenedBeruComplaintsTicket();

        assertComment(actual, String.format(COMMENT_TEMPLATE, ORDER_ITEM_ID_1, ORDER_ITEM_TITLE_1,
                RETURN_ITEM_DECISION_TYPE_TITLE, RETURN_ITEM_DECISION_COMMENT));
    }

    private Order createOrder() {
        Map<String, Object> props = new HashMap<>(Map.of(
                Order.NUMBER, TEST_ORDER_NUMBER,
                Order.PAYMENT_METHOD, PaymentMethod.GOOGLE_PAY.name(),
                Order.PAYMENT_TYPE, PaymentType.PREPAID.name(),
                Order.BUYER_EMAIL, TEST_BUYER_EMAIL,
                Order.BUYER_PHONE, TEST_BUYER_PHONE,
                Order.BUYER_LAST_NAME, Randoms.string(),
                Order.BUYER_FIRST_NAME, "",
                Order.BUYER_MIDDLE_NAME, ""
        ));

        Order order = orderTestUtils.createOrder(props);
        createOrderItem(order, ORDER_ITEM_ID_1, ORDER_ITEM_TITLE_1);
        createOrderItem(order, ORDER_ITEM_ID_2, ORDER_ITEM_TITLE_2);
        return order;
    }

    private Return createOrderReturn() {
        Return orderReturn = new Return();
        orderReturn.setId(TEST_ORDER_RETURN_ID);
        orderReturn.setStatus(ReturnStatus.STARTED_BY_USER);
        orderReturn.setFullName(TEST_CLIENT_FULL_NAME);
        orderReturn.setUserEmail(TEST_CLIENT_EMAIL);
        orderReturn.setComment(Randoms.string());

        ReturnItem item = new ReturnItem();
        item.setItemId(ORDER_ITEM_ID_1);
        item.setItemTitle(ORDER_ITEM_TITLE_1);
        item.setCount(1);

        orderReturn.setItems(List.of(item));

        return orderReturn;
    }

    private Return createOrderReturnWithDecision() {
        Return orderReturn = new Return();
        orderReturn.setId(TEST_ORDER_RETURN_ID);
        orderReturn.setStatus(ReturnStatus.DECISION_MADE);
        orderReturn.setFullName(TEST_CLIENT_FULL_NAME);
        orderReturn.setUserEmail(TEST_CLIENT_EMAIL);
        orderReturn.setComment(Randoms.string());

        ReturnItem item = new ReturnItem();
        item.setItemId(ORDER_ITEM_ID_1);
        item.setItemTitle(ORDER_ITEM_TITLE_1);

        item.setDecisionType(RETURN_ITEM_DECISION_TYPE);
        item.setDecisionComment(RETURN_ITEM_DECISION_COMMENT);
        item.setCount(1);

        orderReturn.setItems(List.of(item));

        return orderReturn;
    }

    private void createOrderItem(Order order, Long checkouterId, String title) {
        Map<String, Object> props = new HashMap<>(Map.of(
                OrderItem.CHECKOUTER_ID, checkouterId,
                OrderItem.TITLE, title
        ));

        props.putAll(Map.of(
                OrderItem.BUYER_PRICE, new BigDecimal("2001"),
                OrderItem.COUNT, 2
        ));

        orderTestUtils.mockOrderItem(order, props);
    }

    private void assertComment(Ticket ticket, String expectedComment) {
        List<Comment> comments = dbService.list(Query.of(InternalComment.FQN)
                .withFilters(Filters.eq(Comment.ENTITY, ticket))
                .withSortingOrder(SortingOrder.desc("creationTime")));

        assertEquals(expectedComment, comments.get(0).getBody());
    }
}
