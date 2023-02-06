package ru.yandex.market.notifier.util.providers;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.test.builders.OrderHistoryEventBuilder;
import ru.yandex.market.notifier.util.NotifierTestUtils;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.SHOP;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.YANDEX_MARKET;
import static ru.yandex.market.notifier.util.NotifierTestUtils.DEFAULT_EVENT_DATE;
import static ru.yandex.market.notifier.util.NotifierTestUtils.DEFAULT_ORDER_CREATION_DATE;

/**
 * @author mmetlov
 */
public abstract class EventsProvider {

    public final static String DEFAULT_CASHBACK_PROMO_KEY = "1";

    public static OrderHistoryEvent orderStatusUpdated(OrderStatus statusBefore,
                                                       OrderStatus statusAfter,
                                                       ClientInfo clientInfo) {
        return orderStatusUpdated(statusBefore, statusAfter, null, clientInfo, order -> {
        });
    }

    public static OrderHistoryEvent orderStatusUpdated(OrderStatus statusBefore,
                                                       OrderStatus statusAfter,
                                                       ClientInfo clientInfo,
                                                       Consumer<Order> orderModifier) {
        return orderStatusUpdated(statusBefore, statusAfter, null, clientInfo, orderModifier);
    }

    public static OrderHistoryEvent orderStatusUpdated(OrderStatus statusBefore,
                                                       OrderStatus statusAfter,
                                                       OrderSubstatus substatusAfter,
                                                       ClientInfo clientInfo,
                                                       Consumer<Order> orderModifier
    ) {
        Order order = OrderProvider.getBlueOrder();
        order.setId(1L);
        order.setStatus(OrderStatus.DELIVERY);
        order.setCreationDate(Date.from(DEFAULT_ORDER_CREATION_DATE));
        order.setDelivery(DeliveryProvider.yandexDelivery()
                .build());
        order.getDelivery().setValidFeatures(new HashSet<>(order.getDelivery().getValidFeatures())); // обмани
        order.getBuyer().setUuid("asdasde");
        order.setPaymentType(PaymentType.PREPAID);
        order.setFulfilment(true);
        orderModifier.accept(order);
        return OrderHistoryEventBuilder.anOrderHistoryEvent()
                .withClientInfo(clientInfo)
                .withId(1L)
                .withFromDate(Date.from(DEFAULT_EVENT_DATE))
                .withEventType(HistoryEventType.ORDER_STATUS_UPDATED)
                .withOrder(order)
                .withOrderAfterModifier(o -> {
                    o.setStatus(statusAfter);
                    o.setSubstatus(substatusAfter);
                })
                .withOrderBeforeModifier(o -> o.setStatus(statusBefore))
                .build();
    }

    public static OrderHistoryEvent orderSubstatusUpdated(OrderSubstatus substatusBefore,
                                                          OrderSubstatus substatusAfter,
                                                          ClientInfo clientInfo,
                                                          Consumer<Order> orderModifier) {
        Order order = OrderProvider.getBlueOrder();
        order.setId(1L);
        order.setStatus(OrderStatus.DELIVERY);
        order.setCreationDate(Date.from(DEFAULT_ORDER_CREATION_DATE));
        order.getDelivery().setValidFeatures(new HashSet<>(order.getDelivery().getValidFeatures())); // обмани
        order.getBuyer().setUuid("asdasd");
        order.getBuyer().setUid(12345L);
        order.setPaymentType(PaymentType.PREPAID);
        order.setFulfilment(true);
        orderModifier.accept(order);
        return OrderHistoryEventBuilder.anOrderHistoryEvent()
                .withClientInfo(clientInfo)
                .withId(1L)
                .withFromDate(Date.from(DEFAULT_EVENT_DATE))
                .withEventType(HistoryEventType.ORDER_SUBSTATUS_UPDATED)
                .withOrder(order)
                .withOrderAfterModifier(o -> {
                    o.setStatus(substatusAfter.getStatus());
                    o.setSubstatus(substatusAfter);
                })
                .withOrderBeforeModifier(o -> {
                    o.setStatus(substatusBefore.getStatus());
                    o.setSubstatus(substatusBefore);
                })
                .build();
    }

    public static OrderHistoryEvent orderStatusUpdated(Order order,
                                                       OrderStatus statusBefore,
                                                       OrderStatus statusAfter) {
        order.setId(order.getId() != null ? order.getId() : 1L);
        order.setStatus(OrderStatus.DELIVERY);
        order.setCreationDate(Date.from(DEFAULT_ORDER_CREATION_DATE));
        order.getDelivery().setValidFeatures(new HashSet<>(order.getDelivery().getValidFeatures())); // обмани
        order.getBuyer().setUuid("asdasd");
        order.setPaymentType(PaymentType.PREPAID);
        return OrderHistoryEventBuilder.anOrderHistoryEvent()
                .withClientInfo(ClientInfo.SYSTEM)
                .withId(1L)
                .withFromDate(Date.from(DEFAULT_EVENT_DATE))
                .withEventType(HistoryEventType.ORDER_STATUS_UPDATED)
                .withOrder(order)
                .withOrderAfterModifier(o -> o.setStatus(statusAfter))
                .withOrderBeforeModifier(o -> o.setStatus(statusBefore))
                .build();
    }

    public static OrderHistoryEvent orderStatusUpdated(Order order,
                                                       OrderStatus statusBefore,
                                                       OrderStatus statusAfter,
                                                       OrderSubstatus substatusAfter,
                                                       ClientInfo clientInfo) {
        order.setId(order.getId() != null ? order.getId() : 1L);
        order.setStatus(OrderStatus.DELIVERY);
        order.setCreationDate(Date.from(DEFAULT_ORDER_CREATION_DATE));
        order.getDelivery().setValidFeatures(new HashSet<>(order.getDelivery().getValidFeatures())); // обмани
        order.getBuyer().setUuid("asdasd");
        order.setPaymentType(PaymentType.PREPAID);
        return OrderHistoryEventBuilder.anOrderHistoryEvent()
                .withClientInfo(clientInfo)
                .withId(1L)
                .withFromDate(Date.from(DEFAULT_EVENT_DATE))
                .withEventType(HistoryEventType.ORDER_STATUS_UPDATED)
                .withOrder(order)
                .withOrderAfterModifier(o -> {
                    o.setStatus(statusAfter);
                    o.setSubstatus(substatusAfter);
                })
                .withOrderBeforeModifier(o -> o.setStatus(statusBefore))
                .build();
    }

    public static OrderHistoryEvent getBlueOrderHistoryEvent() {
        OrderHistoryEvent event = NotifierTestUtils.generateOrderHistoryEvent(1L, OrderStatus.CANCELLED,
                OrderSubstatus.USER_NOT_PAID);
        event.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        event.setAuthor(ClientInfo.SYSTEM);
        event.getOrderAfter().setGlobal(false);
        event.getOrderAfter().setAcceptMethod(OrderAcceptMethod.PUSH_API);
        event.getOrderAfter().setFake(false);
        event.getOrderAfter().setRgb(Color.BLUE);
        event.getOrderAfter().setFulfilment(true);
        event.setOrderBefore(event.getOrderAfter().clone());
        return event;
    }

    public static OrderHistoryEvent getGoldenOrderHistoryEvent() {
        OrderHistoryEvent event = NotifierTestUtils.generateOrderHistoryEvent(1L, OrderStatus.CANCELLED,
                OrderSubstatus.USER_NOT_PAID);
        event.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        event.setAuthor(ClientInfo.SYSTEM);
        event.getOrderAfter().setGlobal(false);
        event.getOrderAfter().setAcceptMethod(OrderAcceptMethod.PUSH_API);
        event.getOrderAfter().setFake(false);
        event.getOrderAfter().setRgb(Color.BLUE);
        event.getOrderAfter().setFulfilment(false);
        event.getOrderAfter().setShopId(1337L);
        event.setOrderBefore(event.getOrderAfter().clone());
        return event;
    }

    public static OrderHistoryEvent getDeliveryReceiptEvent() {
        OrderHistoryEvent event = NotifierTestUtils.generateOrderHistoryEvent(1L, OrderStatus.DELIVERED,
                null);
        event.setType(HistoryEventType.RECEIPT_PRINTED);
        event.setAuthor(ClientInfo.SYSTEM);
        event.getOrderAfter().setGlobal(false);
        event.getOrderAfter().setAcceptMethod(OrderAcceptMethod.PUSH_API);
        event.getOrderAfter().setFake(false);
        event.getOrderAfter().setRgb(Color.BLUE);
        event.getOrderAfter().setFulfilment(false);
        event.getOrderAfter().setShopId(1337L);
        event.setOrderBefore(event.getOrderAfter().clone());

        event.setReceipt(ReceiptProvider.getReceipt(ReceiptType.OFFSET_ADVANCE_ON_DELIVERED));

        return event;
    }

    public static OrderHistoryEvent getOrderReturnCreatedEvent(Long returnId) {
        Order order = OrderProvider.getBlueOrder();
        order.getItems().forEach(i -> {
            i.setId(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
            i.setCount(2);
        });
        order.setId(123L);
        order.setStatus(OrderStatus.DELIVERED);
        order.setCreationDate(Date.from(DEFAULT_ORDER_CREATION_DATE));
        order.getDelivery().setValidFeatures(new HashSet<>(order.getDelivery().getValidFeatures()));
        order.setRgb(Color.BLUE);
        return OrderHistoryEventBuilder.anOrderHistoryEvent()
                .withClientInfo(ClientInfo.SYSTEM)
                .withId(1L)
                .withFromDate(Date.from(DEFAULT_EVENT_DATE))
                .withEventType(HistoryEventType.ORDER_RETURN_CREATED)
                .withReturnId(returnId)
                .withOrder(order)
                .build();
    }

    public static OrderHistoryEvent getWhiteOrderReturnCreated(Long returnId) {
        Order order = OrderProvider.getColorOrder(Color.WHITE);
        order.getItems().forEach(i -> {
            i.setId(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
            i.setCount(2);
        });
        order.setId(123L);
        order.setStatus(OrderStatus.DELIVERED);
        order.setCreationDate(Date.from(DEFAULT_ORDER_CREATION_DATE));
        order.getDelivery().setValidFeatures(new HashSet<>(order.getDelivery().getValidFeatures()));
        order.setRgb(Color.WHITE);
        return OrderHistoryEventBuilder.anOrderHistoryEvent()
                .withClientInfo(ClientInfo.SYSTEM)
                .withId(1L)
                .withFromDate(Date.from(DEFAULT_EVENT_DATE))
                .withEventType(HistoryEventType.ORDER_RETURN_CREATED)
                .withReturnId(returnId)
                .withOrder(order)
                .build();
    }

    public static OrderHistoryEvent getOrderDeliveryUpdated(Order orderBefore, Order orderAfer) {
        return OrderHistoryEventBuilder.anOrderHistoryEvent()
                .withClientInfo(ClientInfo.SYSTEM)
                .withId(1L)
                .withFromDate(Date.from(DEFAULT_EVENT_DATE))
                .withEventType(HistoryEventType.ORDER_DELIVERY_UPDATED)
                .withOrderBefore(orderBefore)
                .withOrderAfter(orderAfer)
                .build();

    }

    public static OrderHistoryEvent getOrderReturnDeliveryUpdatedEvent(Long returnId) {
        Order order = OrderProvider.getBlueOrder();
        order.getItems().forEach(i -> {
            i.setId(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
            i.setCount(2);
        });
        order.setId(123L);
        order.setStatus(OrderStatus.DELIVERED);
        order.setCreationDate(Date.from(DEFAULT_ORDER_CREATION_DATE));
        order.getDelivery().setValidFeatures(new HashSet<>(order.getDelivery().getValidFeatures()));
        order.setRgb(Color.BLUE);
        return OrderHistoryEventBuilder.anOrderHistoryEvent()
                .withClientInfo(ClientInfo.SYSTEM)
                .withId(1L)
                .withFromDate(Date.from(DEFAULT_EVENT_DATE))
                .withEventType(HistoryEventType.ORDER_RETURN_DELIVERY_UPDATED)
                .withReturnId(returnId)
                .withOrder(order)
                .build();
    }

    public static OrderHistoryEvent getOrderReturnDeliveryStatusUpdatedEvent(Long returnId) {
        Order order = OrderProvider.getBlueOrder();
        order.getItems().forEach(i -> {
            i.setId(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
            i.setCount(2);
        });
        order.setId(123L);
        order.setStatus(OrderStatus.PROCESSING);
        order.setCreationDate(Date.from(DEFAULT_ORDER_CREATION_DATE));
        order.getDelivery().setValidFeatures(new HashSet<>(order.getDelivery().getValidFeatures()));
        order.setRgb(Color.BLUE);
        return OrderHistoryEventBuilder.anOrderHistoryEvent()
                .withClientInfo(ClientInfo.SYSTEM)
                .withId(1L)
                .withFromDate(Date.from(DEFAULT_EVENT_DATE))
                .withEventType(HistoryEventType.ORDER_RETURN_DELIVERY_STATUS_UPDATED)
                .withReturnId(returnId)
                .withOrder(order)
                .build();
    }

    public static OrderHistoryEvent orderItemsUpdated(ClientInfo clientInfo,
                                                      Consumer<Order> orderModifier) {
        Order order = OrderProvider.getBlueOrder();
        order.setId(1L);
        order.setStatus(OrderStatus.PENDING);
        order.setCreationDate(Date.from(DEFAULT_ORDER_CREATION_DATE));
        order.getDelivery().setValidFeatures(new HashSet<>(order.getDelivery().getValidFeatures()));
        orderModifier.accept(order);
        return OrderHistoryEventBuilder.anOrderHistoryEvent()
                .withClientInfo(clientInfo)
                .withId(1L)
                .withFromDate(Date.from(DEFAULT_EVENT_DATE))
                .withEventType(HistoryEventType.ITEMS_UPDATED)
                .withOrder(order)
                .build();
    }

    public static Order makeOrderForMock(OrderHistoryEvent event, boolean withDate) {
        Order fromCheckouter = event.getOrderAfter().clone();
        fromCheckouter.setStatusExpiryDate(withDate ? DateUtil.addDay(DateUtil.getToday(), 5) : null);
        return fromCheckouter;
    }

    public static OrderHistoryEvent ffBlueOrderItemsUpdated() {
        OrderHistoryEvent event = NotifierTestUtils.generateOrderHistoryEvent(1L, OrderStatus.PROCESSING, null);
        event.setType(HistoryEventType.ITEMS_UPDATED);
        event.setReason(HistoryEventReason.ITEMS_NOT_FOUND);
        event.setAuthor(new ClientInfo(ClientRole.SYSTEM, null));
        event.getOrderBefore().setCurrency(Currency.RUR);
        event.getOrderBefore().setBuyerCurrency(Currency.RUR);
        event.getOrderBefore().setExchangeRate(BigDecimal.ONE);
        event.getOrderBefore().setBuyerItemsTotal(new BigDecimal("1000"));
        event.getOrderBefore().setItemsTotal(new BigDecimal("1000"));
        convertToFulfilment(event.getOrderBefore());
        convertToFulfilment(event.getOrderAfter());

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setBuyerPrice(new BigDecimal("500"));
        item.setQuantPrice(new BigDecimal("500"));
        item.setCount(2);
        item.setQuantity(BigDecimal.valueOf(2));

        event.getOrderBefore().addItem(item);

        event.getOrderAfter().setBuyerItemsTotal(new BigDecimal("500"));
        event.getOrderAfter().setItemsTotal(new BigDecimal("500"));
        event.getOrderAfter().setPaymentType(PaymentType.POSTPAID);

        OrderItem itemAfter = new OrderItem();
        itemAfter.setId(1L);
        itemAfter.setBuyerPrice(new BigDecimal("500"));
        itemAfter.setQuantPrice(new BigDecimal("500"));
        itemAfter.setCount(1);
        itemAfter.setQuantity(ONE);
        event.getOrderAfter().addItem(itemAfter);
        return event;
    }

    public static OrderHistoryEvent fbsBlueOrderItemsUpdated() {
        OrderHistoryEvent event = ffBlueOrderItemsUpdated();
        convertToFbs(event.getOrderBefore());
        convertToFbs(event.getOrderAfter());
        return event;
    }

    public static OrderHistoryEvent dbsBlueOrderItemsUpdated() {
        return dbsBlueOrderItemsUpdated(HistoryEventReason.ITEMS_NOT_FOUND);
    }

    public static OrderHistoryEvent dbsBlueOrderItemsUpdated(HistoryEventReason reason) {
        OrderHistoryEvent event = ffBlueOrderItemsUpdated();
        convertToDbs(event.getOrderBefore());
        convertToDbs(event.getOrderAfter());
        event.setReason(reason);
        return event;
    }

    public static OrderHistoryEvent orderCashbackEmissionCleared(ClientInfo clientInfo) {
        var order = OrderProvider.getBlueOrder();
        order.setId(1L);
        order.setStatus(OrderStatus.DELIVERED);
        order.setCreationDate(Date.from(DEFAULT_ORDER_CREATION_DATE));
        order.getDelivery().setValidFeatures(new HashSet<>(order.getDelivery().getValidFeatures()));
        order.getBuyer().setUuid("asdasd");
        order.setPaymentType(PaymentType.PREPAID);
        order.setFulfilment(true);
        order.getItems().forEach(item -> item.addOrReplacePromo(ItemPromo.cashbackPromo(ONE, TEN,
                DEFAULT_CASHBACK_PROMO_KEY)));
        return OrderHistoryEventBuilder.anOrderHistoryEvent()
                .withClientInfo(clientInfo)
                .withId(1L)
                .withFromDate(Date.from(DEFAULT_EVENT_DATE))
                .withEventType(HistoryEventType.ORDER_CASHBACK_EMISSION_CLEARED)
                .withOrder(order)
                .build();
    }

    public static OrderHistoryEvent dsbsOrderChangeRequestCreated(ClientInfo clientInfo, Order order) {
        return OrderHistoryEventBuilder.anOrderHistoryEvent()
                .withClientInfo(clientInfo)
                .withId(1L)
                .withFromDate(Date.from(DEFAULT_EVENT_DATE))
                .withEventType(HistoryEventType.ORDER_CHANGE_REQUEST_CREATED)
                .withOrder(order)
                .build();
    }

    public static OrderHistoryEvent dsbsOrderChangeRequestStatusUpdated(ClientInfo clientInfo, Order order) {
        return OrderHistoryEventBuilder.anOrderHistoryEvent()
                .withClientInfo(clientInfo)
                .withId(1L)
                .withFromDate(Date.from(DEFAULT_EVENT_DATE))
                .withEventType(HistoryEventType.ORDER_CHANGE_REQUEST_STATUS_UPDATED)
                .withOrder(order)
                .build();
    }

    private static void convertToFulfilment(Order order) {
        order.setFulfilment(true);
        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(YANDEX_MARKET);
        order.setDelivery(delivery);
    }

    private static void convertToFbs(Order order) {
        convertToFulfilment(order);
        order.setFulfilment(false);
    }

    private static void convertToDbs(Order order) {
        order.setFulfilment(false);
        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(SHOP);
        order.setDelivery(delivery);
    }
}
