package ru.yandex.market.notifier.jobs.zk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.criteria.EvictionSearch;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.entity.NotificationStatus;
import ru.yandex.market.notifier.jobs.zk.processors.AbstractEventProcessor;
import ru.yandex.market.notifier.service.InboxService;
import ru.yandex.market.notifier.util.EventTestUtils;
import ru.yandex.market.notifier.util.NotifierTestUtils;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.market.notifier.util.EventTestUtils.generateDeliveryUpdatedEvent;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 23.12.15
 */
public class CheckoutImportWorkerJobStubTest extends AbstractServicesTestBase {


    @Autowired
    private InboxService inboxService;
    @Autowired
    private EventTestUtils eventTestUtils;

    /**
     * На изменение условий доставки НЕ глобал заказа c доставкой маркета не создается нотификация
     */
    @Test
    public void testNoDeliveryUpdateForYandexMarketDelivery() {
        OrderHistoryEvent event = generateDeliveryUpdatedEvent(new ClientInfo(ClientRole.SYSTEM, 0L),
                false, DeliveryPartnerType.YANDEX_MARKET);
        eventTestUtils.mockEvent(event);
        eventTestUtils.assertHasNewNotifications(0);
    }

    @Test
    public void testOrderShipmentEmailDeliverySingleShipment() {
        OrderHistoryEvent event = generateTrackCheckpointChangedEvent(new ClientInfo(ClientRole.SYSTEM, 0L),
                OrderStatus.DELIVERY, 1,
                AbstractEventProcessor.DELIVERY_TRANSPORTATION_RECIPIENT);

        eventTestUtils.mockEvent(event);
        eventTestUtils.assertHasNewNotifications(0);
    }

    @Test
    public void testOrderShipmentEmailDeliveryMultiShipment() {
        OrderHistoryEvent event = generateTrackCheckpointChangedEvent(new ClientInfo(ClientRole.SYSTEM, 0L),
                OrderStatus.DELIVERY, 2,
                AbstractEventProcessor.DELIVERY_TRANSPORTATION_RECIPIENT);

        eventTestUtils.mockEvent(event);
        eventTestUtils.assertHasNewNotifications(0);
    }

    /**
     * Delivery-check письма
     */
    @Test
    public void testDeliveryExpiryEvent() {
        OrderHistoryEvent event = generateDeliveryExpiryEvent(1L);
        eventTestUtils.mockEvents(Collections.singletonList(event));
        eventTestUtils.runImport();
        List<Notification> notifications = getNewNotifications(event.getOrderAfter().getId());
        assertThat(notifications.size(), equalTo(1));
        List<String> notificationTypes = notifications.stream().map(Notification::getType).distinct().collect(toList());
        assertThat(notificationTypes, hasItem(AbstractEventProcessor.PUSH_API_STATUS_CHANGE_TYPE));
    }

    @Test
    public void testDeleteDeliveryExpiryNotification() {
        OrderHistoryEvent event1 = generateDeliveryExpiryEvent(1L);
        OrderHistoryEvent event2 = generateOrderReceivedEvent(2L);
        eventTestUtils.mockEvents(Arrays.asList(event1, event2));
        eventTestUtils.runImport();
        List<Notification> notifications = getNewNotifications(event1.getOrderAfter().getId());
        assertThat(notifications.size(), equalTo(1));
        List<String> notificationTypes = notifications.stream().map(Notification::getType).distinct().collect(toList());
        assertThat(notificationTypes, hasItem(AbstractEventProcessor.PUSH_API_STATUS_CHANGE_TYPE));
    }

    private List<Notification> getNewNotifications(Long orderId) {
        EvictionSearch es = new EvictionSearch(NotificationStatus.NEW, Collections.singletonList(orderId));
        return inboxService.evictionSearch(es);
    }

    @Nonnull
    private static OrderHistoryEvent generateDeliveryExpiryEvent(Long id) {
        OrderHistoryEvent event = generateDeliveryUpdatedEvent(ClientInfo.SYSTEM,
                false, DeliveryPartnerType.YANDEX_MARKET);
        event.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        event.getOrderAfter().setStatus(OrderStatus.DELIVERY);
        event.getOrderAfter().setPaymentType(PaymentType.PREPAID);
        event.setId(id);
        event.getOrderAfter().setGlobal(false);
        event.getOrderAfter().getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        return event;
    }

    @Nonnull
    private static OrderHistoryEvent generateOrderReceivedEvent(Long id) {
        OrderHistoryEvent event = generateDeliveryUpdatedEvent(new ClientInfo(ClientRole.SYSTEM, 0L),
                false, DeliveryPartnerType.YANDEX_MARKET);
        event.setType(HistoryEventType.USER_RECEIVED_ORDER);
        event.setId(id);
        return event;
    }

    @Nonnull
    public static OrderHistoryEvent generateTrackCheckpointChangedEvent(ClientInfo clientInfo,
                                                                        OrderStatus status,
                                                                        int shipmentCount,
                                                                        int... checkPoints) {
        OrderHistoryEvent event = NotifierTestUtils.generateOrderHistoryEvent(1L, status, null);
        event.setType(HistoryEventType.TRACK_CHECKPOINT_CHANGED);
        event.setAuthor(clientInfo);
        event.setOrderBefore(event.getOrderAfter().clone());

        Delivery deliveryBefore = new Delivery();
        deliveryBefore.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        deliveryBefore.setValidFeatures(new HashSet<>(deliveryBefore.getValidFeatures())); // обмани xstream !
        Date from = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(from);
        calendar.add(Calendar.DATE, 5);
        from = calendar.getTime();
        calendar.add(Calendar.DATE, 5);
        Date to = calendar.getTime();
        deliveryBefore.setDeliveryDates(new DeliveryDates(from, to));
        event.getOrderBefore().setDelivery(deliveryBefore);

        List<Parcel> shipments = generateShipments(status, shipmentCount, checkPoints);
        shipments.get(0).getTracks().get(0).getCheckpoints().clear();
        deliveryBefore.setParcels(shipments);

        Delivery deliveryAfter = deliveryBefore.clone();
        event.getOrderAfter().setDelivery(deliveryAfter);

        deliveryAfter.setParcels(generateShipments(status, shipmentCount, checkPoints));
        return event;
    }

    public static List<Parcel> generateShipments(OrderStatus status, int count, int... checkPoints) {
        List<Parcel> shipments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Parcel shipment = new Parcel();
            shipment.setId((long) i);
            Track track = new Track("track_code", 0L);
            if (status == OrderStatus.PICKUP) {
                TrackCheckpoint checkpoint = new TrackCheckpoint();
                checkpoint.setDeliveryCheckpointStatus(AbstractEventProcessor.DELIVERY_ARRIVED_PICKUP_POINT);
                track.addCheckpoint(checkpoint);
            } else {
                for (int checkPoint : checkPoints) {
                    TrackCheckpoint checkpoint = new TrackCheckpoint();
                    checkpoint.setDeliveryCheckpointStatus(checkPoint);
                    track.addCheckpoint(checkpoint);
                }
            }
            shipment.setTracks(Collections.singletonList(track));
            shipments.add(shipment);
        }
        return shipments;
    }

    public static OrderHistoryEvent generateCashReceiptPrintedEvent() {
        OrderHistoryEvent event = NotifierTestUtils.generateOrderHistoryEvent(1L, OrderStatus.DELIVERED, null);
        event.setType(HistoryEventType.CASH_REFUND_RECEIPT_PRINTED);
        event.setAuthor(ClientInfo.SYSTEM);
        event.setOrderBefore(event.getOrderAfter().clone());
        Receipt receipt = new Receipt();
        receipt.setId(321L);
        receipt.setType(ReceiptType.INCOME_RETURN);
        event.setReceipt(receipt);
        return event;
    }

    public static OrderHistoryEvent generateReturnCreatedEvent() {
        OrderHistoryEvent event = NotifierTestUtils.generateOrderHistoryEvent(1L, OrderStatus.DELIVERED, null);
        event.setType(HistoryEventType.ORDER_RETURN_CREATED);
        event.setAuthor(ClientInfo.SYSTEM);
        event.setOrderBefore(event.getOrderAfter().clone());
        event.setReturnId(1234L);
        return event;
    }

    public static OrderHistoryEvent generateReturnRefund() {
        OrderHistoryEvent event = NotifierTestUtils.generateOrderHistoryEvent(1L, OrderStatus.DELIVERED, null);
        event.setType(HistoryEventType.ORDER_RETURN_STATUS_UPDATED);
        event.setAuthor(ClientInfo.SYSTEM);
        event.setOrderBefore(event.getOrderAfter().clone());
        event.setReturnId(1234L);
        return event;
    }

    public static OrderHistoryEvent generateSubstusUpdatedWaitngBankDecision() {
        OrderHistoryEvent event = NotifierTestUtils.generateOrderHistoryEvent(1L, OrderStatus.UNPAID, null);
        event.getOrderAfter().setSubstatus(OrderSubstatus.WAITING_BANK_DECISION);
        event.setType(HistoryEventType.ORDER_SUBSTATUS_UPDATED);
        event.setAuthor(ClientInfo.SYSTEM);
        event.setOrderBefore(event.getOrderAfter().clone());
        event.getOrderBefore().setSubstatus(OrderSubstatus.WAITING_USER_INPUT);
        event.getOrderBefore().setRgb(Color.BLUE);
        event.getOrderAfter().setRgb(Color.BLUE);
        event.getOrderAfter().setStatusUpdateDate(new Date());
        return event;
    }

    public static OrderHistoryEvent generateUnpaidEventForCredit() {
        OrderHistoryEvent event = NotifierTestUtils.generateOrderHistoryEvent(1L, OrderStatus.UNPAID, null);
        event.getOrderAfter().setSubstatus(OrderSubstatus.WAITING_USER_INPUT);
        event.getOrderAfter().setPaymentMethod(PaymentMethod.CREDIT);
        event.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        event.setAuthor(ClientInfo.SYSTEM);
        event.setOrderBefore(event.getOrderAfter().clone());
        event.getOrderBefore().setRgb(Color.BLUE);
        event.getOrderAfter().setRgb(Color.BLUE);
        event.getOrderAfter().setStatusUpdateDate(new Date());
        return event;
    }

    public static OrderHistoryEvent generateSubstatusUpdatedWaitingUserDeliveryInput() {
        OrderHistoryEvent event = NotifierTestUtils.generateOrderHistoryEvent(1L, OrderStatus.UNPAID, null);
        event.getOrderAfter().setSubstatus(OrderSubstatus.WAITING_USER_DELIVERY_INPUT);
        event.setType(HistoryEventType.ORDER_SUBSTATUS_UPDATED);
        event.setAuthor(ClientInfo.SYSTEM);
        event.setOrderBefore(event.getOrderAfter().clone());
        event.getOrderBefore().setSubstatus(OrderSubstatus.WAITING_TINKOFF_DECISION);
        event.getOrderBefore().setRgb(Color.BLUE);
        event.getOrderAfter().setRgb(Color.BLUE);
        event.getOrderAfter().setStatusUpdateDate(new Date());
        return event;
    }

    public static OrderHistoryEvent generateUnpaidEventForTinkoffCredit() {
        OrderHistoryEvent event = NotifierTestUtils.generateOrderHistoryEvent(1L, OrderStatus.UNPAID, null);
        event.getOrderAfter().setSubstatus(OrderSubstatus.WAITING_TINKOFF_DECISION);
        event.getOrderAfter().setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);
        event.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        event.setAuthor(ClientInfo.SYSTEM);
        event.setOrderBefore(event.getOrderAfter().clone());
        event.getOrderBefore().setRgb(Color.BLUE);
        event.getOrderAfter().setRgb(Color.BLUE);
        event.getOrderAfter().setStatusUpdateDate(new Date());
        return event;
    }
}
