package ru.yandex.market.notifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsClient;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.shop.MarketplaceFeature;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.util.EventTestUtils;
import ru.yandex.market.notifier.util.PersNotifyVerifier;
import ru.yandex.market.notifier.util.providers.DeliveryProvider;
import ru.yandex.market.notifier.util.providers.OrderProvider;
import ru.yandex.market.notifier.util.providers.TrackProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static ru.yandex.market.notifier.util.NotifierTestUtils.DEFAULT_EVENT_DATE;
import static ru.yandex.market.notifier.util.NotifierTestUtils.generateOrderHistoryEvent;

public class UserNotificationScenariosTest extends AbstractServicesTestBase {

    //2019-06-13 21:26:40 MSK
    private static final Instant DELIVERY_DATE_TODAY = Instant.ofEpochSecond(1560450400L);

    @Mock
    private CheckouterOrderHistoryEventsClient orderHistoryEventsClient;

    @Autowired
    private PersNotifyVerifier persNotifyVerifier;

    @Autowired
    private EventTestUtils eventTestUtils;

    @Test
    void onlyMailOnPending() {
        processEventsAndDeliverNormalNotifications(OrderStatus.PENDING, null);
        verifyOnlyMailSent();
    }

    @Test
    void noMailAndNoPushOnTrackedAndGlobalDelivery() {
        processBlueEventsAndDeliverNotifications(OrderStatus.DELIVERY, null,
                DEFAULT_EVENT_DATE, true, "my_track", PaymentType.PREPAID,
                TrackProvider.DELIVERY_SERVICE_ID);
        verifyNoMailAndNoPushSent();
    }

    @Test
    void noMailAndNoPushOnTrackedAndGlobalDeliveryWitDS() throws Exception {
        processBlueEventsAndDeliverNotifications(OrderStatus.DELIVERY, null,
                DEFAULT_EVENT_DATE, true, "my_track", PaymentType.PREPAID,
                19L);
        verifyNoMailAndNoPushSent();
    }

    @Test
    void noMailAndNoPushSentOnNoTrackedAndNoGlobalDelivery() {
        processBlueEventsAndDeliverNotifications(OrderStatus.DELIVERY, null,
                DEFAULT_EVENT_DATE, false, null, PaymentType.PREPAID,
                TrackProvider.DELIVERY_SERVICE_ID);
        verifyNoMailAndNoPushSent();
    }

    @Test
    void mailOnProcessingDontCallFulfilment() {
        processBlueEventsAndDeliverNotifications(true, OrderStatus.PROCESSING, null,
                DELIVERY_DATE_TODAY, DELIVERY_DATE_TODAY, null, null, false, null, true, PaymentType.PREPAID,
                TrackProvider.DELIVERY_SERVICE_ID);
        persNotifyVerifier.verifyMailSent(1);
    }

    @Test
    void mailOnProcessingDontCallFulfilmentWithDateInterval() {
        processBlueEventsAndDeliverNotifications(true, OrderStatus.PROCESSING, null,
                DELIVERY_DATE_TODAY.truncatedTo(ChronoUnit.DAYS),
                DELIVERY_DATE_TODAY.plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                null, null,
                false, null, true, PaymentType.PREPAID,
                TrackProvider.DELIVERY_SERVICE_ID);
        persNotifyVerifier.verifyMailSent(1);
    }

    @Test
    void mailOnProcessingDontCallFulfilmentWithDateAndTimeInterval() {
        LocalTime fromTime = LocalTime.of(14, 30);
        processBlueEventsAndDeliverNotifications(true, OrderStatus.PROCESSING, null,
                DELIVERY_DATE_TODAY,
                DELIVERY_DATE_TODAY.plus(1, ChronoUnit.DAYS),
                fromTime,
                fromTime.plus(4, ChronoUnit.HOURS),
                false, null, true, PaymentType.PREPAID,
                TrackProvider.DELIVERY_SERVICE_ID);
        persNotifyVerifier.verifyMailSent(1);
    }

    @Test
    void mailOnProcessingDontCallFulfilmentWithSingleDateAndTimeInterval() {
        LocalTime fromTime = LocalTime.of(14, 30);
        processBlueEventsAndDeliverNotifications(true, OrderStatus.PROCESSING, null,
                DELIVERY_DATE_TODAY,
                DELIVERY_DATE_TODAY,
                fromTime,
                fromTime.plus(4, ChronoUnit.HOURS),
                false, null, true, PaymentType.PREPAID,
                TrackProvider.DELIVERY_SERVICE_ID);
        persNotifyVerifier.verifyMailSent(1);
    }

    @Test
    void mailOnExpressProcessing() {
        LocalTime fromTime = LocalTime.of(14, 30);
        processBlueEventsAndDeliverNotifications(true, OrderStatus.PROCESSING, null,
                DELIVERY_DATE_TODAY,
                DELIVERY_DATE_TODAY,
                fromTime,
                fromTime.plus(2, ChronoUnit.HOURS),
                false, null, true, PaymentType.PREPAID,
                TrackProvider.DELIVERY_SERVICE_ID);
        persNotifyVerifier.verifyMailSent(1);
    }

    @Test
    void mailOnProcessingDontCallFulfilmentWithDateIntervalOnSameTime() {
        LocalTime fromTime = LocalTime.of(14, 30);
        processBlueEventsAndDeliverNotifications(true, OrderStatus.PROCESSING, null,
                DELIVERY_DATE_TODAY,
                DELIVERY_DATE_TODAY.plus(1, ChronoUnit.DAYS),
                fromTime,
                fromTime.plus(4, ChronoUnit.HOURS),
                false, null, true, PaymentType.PREPAID,
                TrackProvider.DELIVERY_SERVICE_ID);
        persNotifyVerifier.verifyMailSent(1);
    }

    @Test
    void smsNotSendOnSecondOrder() {
        Order order = OrderProvider.orderBuilder()
                .id(0)
                .buyer(new Buyer(0))
                .color(Color.BLUE)
                .build();

        eventTestUtils.mockGetOrders(checkouterClient, order, 1);

        processBlueEventsAndDeliverNotifications(true, OrderStatus.PROCESSING, null,
                DELIVERY_DATE_TODAY, DELIVERY_DATE_TODAY, null, null, false, null, true, PaymentType.PREPAID,
                TrackProvider.DELIVERY_SERVICE_ID, Platform.ANDROID);
        verifyMailAndPushSent();
    }

    private void processEventsAndDeliverNormalNotifications(OrderStatus status, OrderSubstatus substatus) {
        processBlueEventsAndDeliverNotifications(status, substatus,
                DEFAULT_EVENT_DATE, false, null, PaymentType.PREPAID,
                TrackProvider.DELIVERY_SERVICE_ID);
    }

    private void processBlueEventsAndDeliverNotifications(OrderStatus status, OrderSubstatus substatus,
                                                          Instant deliveryDate, boolean global,
                                                          String trackingId, PaymentType paymentType,
                                                          Long deliveryServiceId) {
        processBlueEventsAndDeliverNotifications(
                false, status, substatus,
                deliveryDate, deliveryDate, null, null,
                global, trackingId, false, paymentType,
                deliveryServiceId);
    }

    private void processBlueEventsAndDeliverNotifications(boolean dontCall,
                                                          OrderStatus status,
                                                          OrderSubstatus substatus,
                                                          Instant fromDeliveryDate,
                                                          Instant toDeliveryDate,
                                                          LocalTime fromDeliveryTime,
                                                          LocalTime toDeliveryTime,
                                                          boolean global,
                                                          String trackingId,
                                                          boolean fulfilment,
                                                          PaymentType paymentType,
                                                          Long deliveryServiceId) {
        processBlueEventsAndDeliverNotifications(dontCall, status, substatus, fromDeliveryDate, toDeliveryDate,
                fromDeliveryTime, toDeliveryTime, global, trackingId, fulfilment, paymentType, deliveryServiceId, null);
    }

    private void processBlueEventsAndDeliverNotifications(boolean dontCall,
                                                          OrderStatus status,
                                                          OrderSubstatus substatus,
                                                          Instant fromDeliveryDate,
                                                          Instant toDeliveryDate,
                                                          LocalTime fromDeliveryTime,
                                                          LocalTime toDeliveryTime,
                                                          boolean global,
                                                          String trackingId,
                                                          boolean fulfilment,
                                                          PaymentType paymentType,
                                                          Long deliveryServiceId, Platform platform) {
        processBlueEventsAndDeliverNotifications(
                dontCall, status, substatus,
                order -> {
                    Delivery delivery = order.getDelivery();
                    delivery.setDeliveryServiceId(deliveryServiceId);
                    delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
                    if (trackingId != null) {
                        Track track = new Track();
                        track.setTrackCode(trackingId);
                        track.setDeliveryServiceType(DeliveryServiceType.CARRIER);
                        track.setDeliveryServiceId(deliveryServiceId);

                        List<Track> tracks = Collections.singletonList(track);
                        delivery.setTracks(tracks);

                        Parcel parcel = new Parcel();
                        delivery.setParcels(Collections.singletonList(parcel));
                        parcel.setTracks(tracks);
                    }
                    delivery.setDeliveryDates(
                            new DeliveryDates(
                                    Date.from(fromDeliveryDate.truncatedTo(ChronoUnit.DAYS)),
                                    Date.from(toDeliveryDate.truncatedTo(ChronoUnit.DAYS)),
                                    fromDeliveryTime,
                                    toDeliveryTime
                            )
                    );
                    delivery.setValidFeatures(Collections.singleton(MarketplaceFeature.PLAINCPA));
                },
                global, fulfilment, paymentType, platform);
    }

    private void processBlueEventsAndDeliverNotifications(boolean dontCall,
                                                          OrderStatus status,
                                                          OrderSubstatus substatus,
                                                          Consumer<Order> orderModifier,
                                                          boolean global,
                                                          boolean fulfilment,
                                                          PaymentType paymentType, Platform platform) {
        OrderHistoryEvent event = generateOrderHistoryEvent(0, status, substatus);
        event.getOrderAfter().setFulfilment(fulfilment);
        event.getOrderAfter().setGlobal(global);
        event.getOrderAfter().setRgb(Color.BLUE);
        event.getOrderAfter().setBuyerTotal(BigDecimal.valueOf(2000L));
        event.getOrderAfter().setPaymentType(paymentType);
        event.getOrderAfter().getBuyer().setDontCall(dontCall);
        event.getOrderAfter().getBuyer().setPhone("+79169024225");
        event.getOrderAfter().getBuyer().setNormalizedPhone("79169024225");
        event.getOrderAfter().getBuyer().setUuid("abcdefgh");
        event.getOrderAfter().setStatusUpdateDate(Date.from(DELIVERY_DATE_TODAY));
        event.getOrderAfter().setDelivery(DeliveryProvider.getShopDelivery());
        if (platform != null) {
            event.getOrderAfter().setProperty(OrderPropertyType.PLATFORM, platform);
        }

        orderModifier.accept(event.getOrderAfter());

        when(checkouterClient.orderHistoryEvents())
                .thenReturn(orderHistoryEventsClient);
        eventTestUtils.mockEvent(event);
        when(checkouterClient.getOrder(anyLong(), any(), any())).thenReturn(event.getOrderAfter());
        eventTestUtils.runImport();
        eventTestUtils.deliverNotifications();
    }

    private void verifyOnlyMailSent() {
        persNotifyVerifier.verifyMailSent();
        persNotifyVerifier.verifyMobilePushSent(never());
    }

    private void verifyMailAndPushSent() {
        persNotifyVerifier.verifyMailSent();
        persNotifyVerifier.verifyMobilePushSent();
    }

    private void verifyNoMailAndNoPushSent() {
        persNotifyVerifier.verifyMailSent(never());
        persNotifyVerifier.verifyMobilePushSent(never());
    }
}
