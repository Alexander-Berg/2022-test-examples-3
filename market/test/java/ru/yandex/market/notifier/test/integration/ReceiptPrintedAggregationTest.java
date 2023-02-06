package ru.yandex.market.notifier.test.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.test.builders.OrderHistoryEventBuilder;
import ru.yandex.market.notifier.application.AbstractWebTestBase;
import ru.yandex.market.notifier.storage.InboxDao;
import ru.yandex.market.notifier.util.EventTestUtils;
import ru.yandex.market.notifier.util.providers.EventsProvider;
import ru.yandex.market.notifier.util.providers.OrderProvider;
import ru.yandex.market.notifier.util.providers.ReceiptProvider;
import ru.yandex.market.pers.notify.PersNotifyClientException;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

public class ReceiptPrintedAggregationTest extends AbstractWebTestBase {

    @Autowired
    private EventTestUtils eventTestUtils;
    @Autowired
    private InboxDao inboxDao;

    @BeforeEach
    public void setUpEach() throws Exception {
        mockMvc.perform(
                put("/properties/aggregateReceiptPrintedEmail")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("true")
        );
    }

    @AfterEach
    public void tearDownEach() throws Exception {
        mockMvc.perform(
                put("/properties/aggregateReceiptPrintedEmail")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("false")
        );
    }

    @Test
    public void shouldAggregateReceiptPrintedEmail() throws PersNotifyClientException {
        ReceiptType receiptType = ReceiptType.INCOME;

        List<OrderHistoryEvent> events = LongStream.rangeClosed(1, 5)
                .mapToObj(orderId -> {
                    Order order = OrderProvider.getBlueOrder();
                    order.setId(orderId);
                    order.setProperty(OrderPropertyType.MARKET_REQUEST_ID, "my_multi_order_id");
                    order.setProperty(OrderPropertyType.MULTI_ORDER_ID, "my_multi_order_id");
                    order.setProperty(OrderPropertyType.MULTI_ORDER_SIZE, 5);
                    order.setStatus(OrderStatus.PROCESSING);

                    OrderHistoryEvent event = OrderHistoryEventBuilder.anOrderHistoryEvent()
                            .withEventType(HistoryEventType.RECEIPT_PRINTED)
                            .withOrder(order)
                            .withClientInfo(ClientInfo.SYSTEM)
                            .build();
                    Receipt receipt = ReceiptProvider.getReceipt(receiptType);
                    event.setReceipt(receipt);
                    return event;
                })
                .collect(Collectors.toList());

        eventTestUtils.mockEvents(events);
        eventTestUtils.runImport();

        inboxDao.getAllNotifications();

        eventTestUtils.assertEmailsWereSent(events.size(), 1, List.of(
                Matchers.hasProperty("data", Matchers.allOf(
                        Matchers.not(Matchers.hasEntry(Matchers.is("receipt_id"), Matchers.any(String.class))),
                        Matchers.hasEntry("receipt_ids", "123"),
                        Matchers.hasEntry(Matchers.is(NotificationEventDataName.ORDER_IDS), Matchers.allOf(
                                LongStream.rangeClosed(1, 5)
                                        .mapToObj(String::valueOf)
                                        .map(Matchers::containsString)
                                        .collect(Collectors.toList())
                        )),
                        Matchers.hasEntry(NotificationEventDataName.MULTIORDER_ID, "my_multi_order_id")
                )),
                Matchers.hasProperty("data", Matchers.not(Matchers.hasEntry(NotificationEventDataName.ORDER_ID,
                        CoreMatchers.any(String.class))))
        ));
    }

    @Test
    public void shouldAggregateReceiptPrintedEmailWithDifferentReceipts() throws PersNotifyClientException {
        ReceiptType receiptType = ReceiptType.INCOME;

        List<OrderHistoryEvent> events = LongStream.rangeClosed(1, 5)
                .mapToObj(orderId -> {
                    Order order = OrderProvider.getBlueOrder();
                    order.setId(orderId);
                    order.setProperty(OrderPropertyType.MARKET_REQUEST_ID, "my_multi_order_id");
                    order.setProperty(OrderPropertyType.MULTI_ORDER_ID, "my_multi_order_id");
                    order.setProperty(OrderPropertyType.MULTI_ORDER_SIZE, 5);
                    order.setStatus(OrderStatus.PROCESSING);

                    OrderHistoryEvent event = OrderHistoryEventBuilder.anOrderHistoryEvent()
                            .withEventType(HistoryEventType.RECEIPT_PRINTED)
                            .withOrder(order)
                            .withClientInfo(ClientInfo.SYSTEM)
                            .build();
                    Receipt receipt = ReceiptProvider.getReceipt(receiptType);
                    event.setReceipt(receipt);
                    receipt.setId(receipt.getId() + orderId);
                    return event;
                })
                .collect(Collectors.toList());

        eventTestUtils.mockEvents(events);
        eventTestUtils.runImport();

        inboxDao.getAllNotifications();

        eventTestUtils.assertEmailsWereSent(events.size(), 1, List.of(
                Matchers.hasProperty("data", Matchers.allOf(
                        Matchers.not(Matchers.hasEntry(Matchers.is("receipt_id"), Matchers.any(String.class))),
                        Matchers.hasEntry("receipt_ids", "124,125,126,127,128"),
                        Matchers.hasEntry(Matchers.is(NotificationEventDataName.ORDER_IDS), Matchers.allOf(
                                LongStream.rangeClosed(1, 5)
                                        .mapToObj(String::valueOf)
                                        .map(Matchers::containsString)
                                        .collect(Collectors.toList())
                        )),
                        Matchers.hasEntry(NotificationEventDataName.MULTIORDER_ID, "my_multi_order_id")
                )),
                Matchers.hasProperty("data", Matchers.not(Matchers.hasEntry(NotificationEventDataName.ORDER_ID,
                        CoreMatchers.any(String.class))))
        ));
    }


    @Test
    void shouldAggregateReceiptPrintedIfPartiallyPostpaid() throws PersNotifyClientException {
        ReceiptType receiptType = ReceiptType.INCOME;

        String multiOrderId = "my_multi_order_id";
        List<OrderHistoryEvent> events = new ArrayList<>();

        events.add(EventsProvider.orderStatusUpdated(
                OrderStatus.RESERVED,
                OrderStatus.PROCESSING,
                ClientInfo.SYSTEM,
                o -> {
                    o.setProperty(OrderPropertyType.MULTI_ORDER_ID, multiOrderId);
                    o.setProperty(OrderPropertyType.MULTI_ORDER_SIZE, 5);
                    o.setPaymentType(PaymentType.POSTPAID);
                    o.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
                }
        ));
        events.addAll(LongStream.rangeClosed(2, 5)
                .mapToObj(orderId -> {
                    Order order = OrderProvider.getBlueOrder();
                    order.setId(orderId);
                    order.setProperty(OrderPropertyType.MARKET_REQUEST_ID, multiOrderId);
                    order.setProperty(OrderPropertyType.MULTI_ORDER_ID, multiOrderId);
                    order.setProperty(OrderPropertyType.MULTI_ORDER_SIZE, 5);
                    order.setStatus(OrderStatus.PROCESSING);

                    OrderHistoryEvent event = OrderHistoryEventBuilder.anOrderHistoryEvent()
                            .withEventType(HistoryEventType.RECEIPT_PRINTED)
                            .withOrder(order)
                            .withClientInfo(ClientInfo.SYSTEM)
                            .build();
                    Receipt receipt = ReceiptProvider.getReceipt(receiptType);
                    event.setReceipt(receipt);
                    return event;
                })
                .collect(Collectors.toList()));

        eventTestUtils.mockEvents(events);
        eventTestUtils.runImport();

        inboxDao.getAllNotifications();

        eventTestUtils.assertEmailsWereSent(events.size(), 1, List.of(
                Matchers.hasProperty("data", Matchers.allOf(
                        Matchers.not(Matchers.hasEntry(Matchers.is("receipt_id"), Matchers.any(String.class))),
                        Matchers.hasEntry("receipt_ids", "123"),
                        Matchers.hasEntry(Matchers.is(NotificationEventDataName.ORDER_IDS), Matchers.allOf(
                                LongStream.rangeClosed(2, 5)
                                        .mapToObj(String::valueOf)
                                        .map(Matchers::containsString)
                                        .collect(Collectors.toList())
                        )),
                        Matchers.hasEntry(NotificationEventDataName.MULTIORDER_ID, multiOrderId)
                )),
                Matchers.hasProperty("data", Matchers.not(Matchers.hasEntry(NotificationEventDataName.ORDER_ID,
                        CoreMatchers.any(String.class))))
        ));
    }
}
