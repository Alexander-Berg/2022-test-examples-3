package ru.yandex.market.notifier.jobs.zk.processors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import ru.market.partner.notification.client.PartnerNotificationClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.*;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.shop.ScheduleLine;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.notifier.application.AbstractWebTestBase;
import ru.yandex.market.notifier.entity.*;
import ru.yandex.market.notifier.service.InboxService;
import ru.yandex.market.notifier.util.providers.EventsProvider;
import ru.yandex.market.notifier.util.providers.OrderProvider;
import ru.yandex.market.partner.notification.client.model.DestinationDTO;
import ru.yandex.market.partner.notification.client.model.SendNotificationRequest;
import ru.yandex.market.request.trace.RequestContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class MarketPartnerMobileEventProcessorTest extends AbstractWebTestBase {

    @Autowired
    private PartnerNotificationClient partnerNotificationClient;

    @Autowired
    private MarketPartnerMobileEventProcessor marketPartnerMobileEventProcessor;

    @Autowired
    private MbiApiClient mbiApiClient;

    @Autowired
    private NotifierProperties notifierProperties;

    @Autowired
    private InboxService inboxService;

    @BeforeEach
    public void before() {
        when(checkouterClient.shops())
                .thenReturn(Mockito.mock(CheckouterShopApi.class));
        RequestContextHolder.createNewContext();
        notifierProperties.setEnableProcessingOrderPartnerNotifications(true);
        notifierProperties.setEnableNewMarketPartnerMobileProcessor(true);
        notifierProperties.setEnableDelayedMarketPartnerMobileDbs(true);
        notifierProperties.setEnableMarketPartnerMobileDbs(true);
        notifierProperties.setMarketPartnerMobilePartnerNotificationRenderOnly(false);
        Mockito.reset(mbiApiClient, partnerNotificationClient);
    }

    @Test
    public void testDontSendExpressNewOrderNotification() {
        when(checkouterClient.shops().getSchedule(anyLong()))
                .thenReturn(List.of(new ScheduleLine(5, 720, 900))); // Пятница, 12 - 15 часов
        Order orderAfter = OrderProvider.getColorOrder(Color.BLUE);
        orderAfter.setId(123L);
        orderAfter.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        orderAfter.setStatus(OrderStatus.PROCESSING);
        orderAfter.setSubstatus(OrderSubstatus.STARTED);
        orderAfter.setStatusExpiryDate(Date.from(LocalDateTime.of(2021, Month.JANUARY, 2, 14, 30)
                .atZone(ZoneId.systemDefault()).toInstant()));
        orderAfter.setItemsTotal(BigDecimal.valueOf(520));
        orderAfter.setTotal(BigDecimal.valueOf(500));
        OrderPrices prices = orderAfter.getPromoPrices();
        prices.setSubsidyTotal(BigDecimal.valueOf(10));
        Parcel parcel = new Parcel();
        parcel.setShipmentDateTimeBySupplier(LocalDateTime.of(2021, 3, 10, 12, 0, 0));
        orderAfter.getDelivery().setParcels(Collections.singletonList(parcel));
        orderAfter.getDelivery().addFeatures(Set.of(DeliveryFeature.EXPRESS_DELIVERY));
        orderAfter.getItems().forEach(item -> item.setOfferId("test_offer_id"));

        OrderHistoryEvent substatusUpdateEvent = EventsProvider.getBlueOrderHistoryEvent();
        substatusUpdateEvent.setTranDate(new Date());
        substatusUpdateEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        substatusUpdateEvent.getOrderBefore().setStatus(OrderStatus.UNPAID);
        substatusUpdateEvent.setOrderAfter(orderAfter);

        eventTestUtils.mockEvent(substatusUpdateEvent);
        eventTestUtils.runImport();
        Mockito.verify(mbiApiClient, Mockito.never()).sendMessageToSupplier(anyInt(), eq(BlueEventProcessor.IMMEDIATE_STATUS_CHANGE_MOBILE_NOTIFICATION_ID), anyString());
        verify(partnerNotificationClient, Mockito.times(0)).sendNotification(any());
    }

    @Test
    public void testSendDbsNewOrderNotification() {
        when(checkouterClient.shops().getSchedule(anyLong()))
                .thenReturn(List.of(new ScheduleLine(5, 720, 900))); // Пятница, 12 - 15 часов
        Order orderAfter = OrderProvider.getColorOrder(Color.WHITE);
        orderAfter.setId(123L);
        orderAfter.setPaymentType(PaymentType.POSTPAID);
        orderAfter.setStatus(OrderStatus.PENDING);
        orderAfter.setSubstatus(OrderSubstatus.AWAIT_CONFIRMATION);
        orderAfter.setStatusExpiryDate(Date.from(LocalDateTime.of(2021, Month.JANUARY, 2, 14, 30)
                .atZone(ZoneId.systemDefault()).toInstant()));
        orderAfter.setItemsTotal(BigDecimal.valueOf(520));
        orderAfter.setTotal(BigDecimal.valueOf(500));
        OrderPrices prices = orderAfter.getPromoPrices();
        prices.setSubsidyTotal(BigDecimal.valueOf(10));
        Parcel parcel = new Parcel();
        parcel.setShipmentDateTimeBySupplier(LocalDateTime.of(2021, 3, 10, 12, 0, 0));
        orderAfter.getDelivery().setParcels(Collections.singletonList(parcel));
        orderAfter.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        orderAfter.getItems().forEach(item -> item.setOfferId("test_offer_id"));

        OrderHistoryEvent substatusUpdateEvent = EventsProvider.getBlueOrderHistoryEvent();
        substatusUpdateEvent.setTranDate(new Date());
        substatusUpdateEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        substatusUpdateEvent.getOrderBefore().setStatus(OrderStatus.UNPAID);
        substatusUpdateEvent.setOrderAfter(orderAfter);

        eventTestUtils.mockEvent(substatusUpdateEvent);
        eventTestUtils.runImport();
        Mockito.verify(mbiApiClient, Mockito.never()).sendMessageToSupplier(anyInt(), eq(BlueEventProcessor.IMMEDIATE_STATUS_CHANGE_MOBILE_NOTIFICATION_ID), anyString());
        var stats = inboxService.getDeliveryStatisticsFull();
        Assertions.assertEquals(2, getNotificationCountByChannelAndStatus(stats, ChannelType.PARTNER_NOTIFICATION, NotificationStatus.NEW));
    }

    @Test
    public void testCancelDbsAutoCancellationWarningNotification() {
        Notification notification = new Notification();
        notification.setOrderId(123L);
        notification.setDeliveryChannels(List.of(new DeliveryChannel(ChannelType.PARTNER_NOTIFICATION, "")));
        notification.setType(MarketPartnerMobileEventProcessor.WARNING_DBS_AUTO_CANCELLATION);
        notification.setData("");
        inboxService.saveNotification(notification);
        when(checkouterClient.shops().getSchedule(anyLong()))
                .thenReturn(List.of(new ScheduleLine(5, 720, 900))); // Пятница, 12 - 15 часов
        Order orderAfter = OrderProvider.getColorOrder(Color.WHITE);
        orderAfter.setId(123L);
        orderAfter.setStatus(OrderStatus.PROCESSING);
        orderAfter.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        orderAfter.setSubstatus(OrderSubstatus.STARTED);
        orderAfter.setStatusExpiryDate(Date.from(LocalDateTime.of(2021, Month.JANUARY, 2, 14, 30)
                .atZone(ZoneId.systemDefault()).toInstant()));
        orderAfter.setItemsTotal(BigDecimal.valueOf(520));
        orderAfter.setTotal(BigDecimal.valueOf(500));
        OrderPrices prices = orderAfter.getPromoPrices();
        prices.setSubsidyTotal(BigDecimal.valueOf(10));
        Parcel parcel = new Parcel();
        parcel.setShipmentDateTimeBySupplier(LocalDateTime.of(2021, 3, 10, 12, 0, 0));
        orderAfter.getDelivery().setParcels(Collections.singletonList(parcel));
        orderAfter.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        orderAfter.getItems().forEach(item -> item.setOfferId("test_offer_id"));

        OrderHistoryEvent substatusUpdateEvent = EventsProvider.getBlueOrderHistoryEvent();
        substatusUpdateEvent.setTranDate(new Date());
        substatusUpdateEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        substatusUpdateEvent.getOrderBefore().setStatus(OrderStatus.PENDING);
        substatusUpdateEvent.setOrderAfter(orderAfter);

        eventTestUtils.mockEvent(substatusUpdateEvent);
        eventTestUtils.runImport();
        Mockito.verify(mbiApiClient, Mockito.never()).sendMessageToSupplier(anyInt(), eq(BlueEventProcessor.IMMEDIATE_STATUS_CHANGE_MOBILE_NOTIFICATION_ID), anyString());
        verify(partnerNotificationClient, Mockito.never()).sendNotification(any());
        var stats = inboxService.getDeliveryStatisticsFull();
        Assertions.assertEquals(0, getNotificationCountByChannelAndStatus(stats, ChannelType.PARTNER_NOTIFICATION, NotificationStatus.NEW));
        Assertions.assertEquals(1, getNotificationCountByChannelAndStatus(stats, ChannelType.PARTNER_NOTIFICATION, NotificationStatus.DELETED));
    }

    @Test
    public void testSendOrderCancelledExpressNotification() {
        when(checkouterClient.shops().getSchedule(anyLong()))
                .thenReturn(List.of(new ScheduleLine(5, 720, 900))); // Пятница, 12 - 15 часов
        Order orderAfter = OrderProvider.getColorOrder(Color.BLUE);
        orderAfter.setId(123L);
        orderAfter.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        orderAfter.setStatus(OrderStatus.CANCELLED);
        orderAfter.setStatusExpiryDate(Date.from(LocalDateTime.of(2021, Month.JANUARY, 2, 14, 30)
                .atZone(ZoneId.systemDefault()).toInstant()));
        orderAfter.setItemsTotal(BigDecimal.valueOf(520));
        orderAfter.setTotal(BigDecimal.valueOf(500));
        OrderPrices prices = orderAfter.getPromoPrices();
        prices.setSubsidyTotal(BigDecimal.valueOf(10));
        Parcel parcel = new Parcel();
        parcel.setShipmentDateTimeBySupplier(LocalDateTime.of(2021, 3, 10, 12, 0, 0));
        orderAfter.getDelivery().setParcels(Collections.singletonList(parcel));
        orderAfter.getDelivery().addFeatures(Set.of(DeliveryFeature.EXPRESS_DELIVERY));
        orderAfter.getItems().forEach(item -> item.setOfferId("test_offer_id"));

        OrderHistoryEvent substatusUpdateEvent = EventsProvider.getBlueOrderHistoryEvent();
        substatusUpdateEvent.setTranDate(new Date());
        substatusUpdateEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        substatusUpdateEvent.getOrderBefore().setStatus(OrderStatus.UNPAID);
        substatusUpdateEvent.setOrderAfter(orderAfter);

        eventTestUtils.mockEvent(substatusUpdateEvent);
        eventTestUtils.runImport();
        Mockito.verify(mbiApiClient, Mockito.never()).sendMessageToSupplier(anyInt(), eq(BlueEventProcessor.IMMEDIATE_STATUS_CHANGE_MOBILE_NOTIFICATION_ID), anyString());
        var stats = inboxService.getDeliveryStatisticsFull();
        Assertions.assertEquals(1, getNotificationCountByChannelAndStatus(stats, ChannelType.PARTNER_NOTIFICATION, NotificationStatus.NEW));
    }

    @Test
    public void testSendOrderCancelledDbsNotification() {
        when(checkouterClient.shops().getSchedule(anyLong()))
                .thenReturn(List.of(new ScheduleLine(5, 720, 900))); // Пятница, 12 - 15 часов
        Order orderAfter = OrderProvider.getColorOrder(Color.WHITE);
        orderAfter.setId(123L);
        orderAfter.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        orderAfter.setStatus(OrderStatus.CANCELLED);
        orderAfter.setStatusExpiryDate(Date.from(LocalDateTime.of(2021, Month.JANUARY, 2, 14, 30)
                .atZone(ZoneId.systemDefault()).toInstant()));
        orderAfter.setItemsTotal(BigDecimal.valueOf(520));
        orderAfter.setTotal(BigDecimal.valueOf(500));
        OrderPrices prices = orderAfter.getPromoPrices();
        prices.setSubsidyTotal(BigDecimal.valueOf(10));
        Parcel parcel = new Parcel();
        parcel.setShipmentDateTimeBySupplier(LocalDateTime.of(2021, 3, 10, 12, 0, 0));
        orderAfter.getDelivery().setParcels(Collections.singletonList(parcel));
        orderAfter.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        orderAfter.getItems().forEach(item -> item.setOfferId("test_offer_id"));

        OrderHistoryEvent substatusUpdateEvent = EventsProvider.getBlueOrderHistoryEvent();
        substatusUpdateEvent.setTranDate(new Date());
        substatusUpdateEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        substatusUpdateEvent.getOrderBefore().setStatus(OrderStatus.UNPAID);
        substatusUpdateEvent.setOrderAfter(orderAfter);

        eventTestUtils.mockEvent(substatusUpdateEvent);
        eventTestUtils.runImport();
        Mockito.verify(mbiApiClient, Mockito.never()).sendMessageToSupplier(anyInt(), eq(BlueEventProcessor.IMMEDIATE_STATUS_CHANGE_MOBILE_NOTIFICATION_ID), anyString());
        var stats = inboxService.getDeliveryStatisticsFull();
        Assertions.assertEquals(1, getNotificationCountByChannelAndStatus(stats, ChannelType.PARTNER_NOTIFICATION, NotificationStatus.NEW));
    }


    private long getNotificationCountByChannelAndStatus(List<DeliveryStatisticsFull> deliveryStatisticsFull, ChannelType type, NotificationStatus status) {
        return deliveryStatisticsFull.stream().filter(s -> s.getChannelType() == type && s.getStatus() == status).count();
    }
}
