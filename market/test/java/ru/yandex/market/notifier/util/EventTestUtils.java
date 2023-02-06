package ru.yandex.market.notifier.util;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.annotation.Nonnull;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.notifier.entity.Barrier;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.DeliveryStatisticsFull;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.jobs.zk.CheckoutImportUtils;
import ru.yandex.market.notifier.jobs.zk.processors.BlueEventProcessor;
import ru.yandex.market.notifier.jobs.zk.processors.MarketPartnerMobileEventProcessor;
import ru.yandex.market.notifier.service.InboxService;
import ru.yandex.market.notifier.storage.BarrierDao;
import ru.yandex.market.notifier.storage.InboxDao;
import ru.yandex.market.notifier.util.providers.OrderProvider;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.PersNotifyClientException;
import ru.yandex.market.pers.notify.model.NotificationTransportType;
import ru.yandex.market.pers.notify.model.event.EventAddressType;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.request.trace.RequestContextHolder;
import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.market.tms.quartz2.model.PartitionExecutor;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PENDING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.notifier.util.NotifierTestUtils.generateOrderHistoryEvent;
import static ru.yandex.market.notifier.util.providers.EventsProvider.orderStatusUpdated;

@Component
public class EventTestUtils {

    private static final Logger LOG = LoggerFactory.getLogger(EventTestUtils.class);

    @Autowired
    private InboxService inboxService;
    @Autowired
    private InboxDao inboxDao;
    @Autowired
    private BarrierDao barrierDao;
    @Autowired
    @Qualifier("DeliveryJob")
    private Collection<PartitionExecutor> deliveryWorkerJobs;
    @Autowired
    @Qualifier("FailedDeliveryJob")
    private Collection<PartitionExecutor> failedDeliveryWorkerJobs;
    @Autowired
    private Executor barrierUnblockingTmsJob;
    @Autowired
    private PersNotifyClient persNotifyClient;
    @Autowired
    private RestTemplate mbiRestTemplate;
    @Autowired
    private BlueEventProcessor blueEventProcessor;
    @Autowired
    private MarketPartnerMobileEventProcessor marketPartnerMobileEventProcessor;

    private Collection<OrderHistoryEvent> events;

    public void clean() {
        events = null;
    }

    public static OrderHistoryEvent modifyEventAsDeliveryUpdated(
            OrderHistoryEvent event,
            DeliveryPartnerType deliveryPartnerType

    ) {
        Delivery deliveryBefore = new Delivery();
        deliveryBefore.setDeliveryPartnerType(deliveryPartnerType);
        deliveryBefore.setValidFeatures(new HashSet<>(deliveryBefore.getValidFeatures())); // обмани xstream !

        Date from = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(from);
        calendar.add(Calendar.DATE, 5);
        from = calendar.getTime();
        calendar.add(Calendar.DATE, 5);
        Date to = calendar.getTime();
        deliveryBefore.setDeliveryDates(new DeliveryDates(from, to));
        AddressImpl baseAddress = new AddressImpl();
        baseAddress.setCity("Москва");
        deliveryBefore.setAddress(baseAddress);

        event.getOrderBefore().setDelivery(deliveryBefore);
        Delivery deliveryAfter = deliveryBefore.clone();

        AddressImpl newAddress = new AddressImpl();
        newAddress.setCity("Санкт-Петербург");
        deliveryAfter.setAddress(newAddress);

        event.getOrderAfter().setDelivery(deliveryAfter);

        if (deliveryPartnerType == DeliveryPartnerType.YANDEX_MARKET) {
            Parcel orderShipmentBefore = new Parcel();
            event.getOrderBefore().getDelivery().setShipment(orderShipmentBefore);

            Parcel orderShipmentAfter = new Parcel();
            event.getOrderAfter().getDelivery().setShipment(orderShipmentAfter);
        }
        return event;
    }

    @Nonnull
    public static OrderHistoryEvent generateDeliveryUpdatedEvent(ClientInfo clientInfo,
                                                                 boolean isGlobal,
                                                                 DeliveryPartnerType deliveryPartnerType
    ) {
        OrderHistoryEvent event = generateOrderHistoryEvent(1L, OrderStatus.PROCESSING, null);
        event.setType(HistoryEventType.ORDER_DELIVERY_UPDATED);
        event.setAuthor(clientInfo);
        event.getOrderAfter().setGlobal(isGlobal);
        event.getOrderAfter().setAcceptMethod(OrderAcceptMethod.PUSH_API);
        event.getOrderAfter().setFake(false);
        event.setOrderBefore(event.getOrderAfter().clone());

        return modifyEventAsDeliveryUpdated(event, deliveryPartnerType);
    }

    public void mockEvents(Collection<OrderHistoryEvent> events) {
        this.events = events;
    }

    public void mockEvent(OrderHistoryEvent event) {
        mockEvents(Collections.singletonList(event));
    }

    public void mockGetOrders(CheckouterClient checkouterClient, Order currentOrder, int count) {
        when(checkouterClient.getOrdersCount(any(OrderSearchRequest.class), eq(ClientRole.USER),
                eq(currentOrder.getUid())))
                .thenReturn(count);
    }

    public void assertHasNewNotifications(int count) {
        assertHasNewNotifications(null, count);
    }

    private void assertHasNewNotifications(String message, int count) {
        assertTrue(inboxService.getDeliveryStatisticsFull().isEmpty());
        runImport();
        List<DeliveryStatisticsFull> statisticsFull = inboxService.getDeliveryStatisticsFull();
        long countWithoutMobilePush = statisticsFull.stream()
                .filter(deliveryStatisticsFull -> deliveryStatisticsFull
                        .getChannelType() != ChannelType.MOBILE_PUSH)
                .count();
        assertEquals(count, countWithoutMobilePush, message);
    }

    public void runImport() {
        try {
            RequestContextHolder.createNewContext();
            CheckoutImportUtils.prepareImportList(events).forEach(e -> CheckoutImportUtils.processEvent(
                    new HashSet<>(), e, List.of(blueEventProcessor, marketPartnerMobileEventProcessor)
            ));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    public Notification getSingleEmailNotification() {
        return getSingleNotification(ChannelType.EMAIL);
    }

    public Notification getSingleNotification(ChannelType type) {
        return getSingleNotification(type, n -> true);
    }

    private Notification getSingleNotification(ChannelType type, Predicate<Notification> additionalFiltering) {
        List<Notification> notifications = inboxDao.getAllNotifications()
                .stream()
                .filter(n -> n.getDeliveryChannels()
                        .stream()
                        .anyMatch(dc -> dc.getType() == type)
                )
                .filter(additionalFiltering)
                .collect(Collectors.toList());
        return CollectionUtils.expectedSingleResult(notifications);
    }

    public void assertEmailWasSent(OrderHistoryEvent event) throws PersNotifyClientException {
        Notification notification = getSingleEmailNotification();
        assertEmailWasSent(
                notification,
                Arrays.asList(
                        instanceOf(NotificationEventSource.class),
                        hasProperty("email", is(event.getOrderAfter().getBuyer().getEmail()))
                )
        );
    }

    public void assertEmailsWereSent(int notificationCount,
                                     int emailCount,
                                     List<Matcher<? super NotificationEventSource>> additionalMatchers) throws PersNotifyClientException {
        List<Notification> notifications = getNotifications(ChannelType.EMAIL);
        MatcherAssert.assertThat(notifications, hasSize(notificationCount));

        deliverNotifications();

        List<Matcher<? super NotificationEventSource>> matchers = new ArrayList<>();
        matchers.add(Matchers.instanceOf(NotificationEventSource.class));
        matchers.add(Matchers.hasProperty("eventAddressType", is(EventAddressType.MAIL)));
        matchers.addAll(additionalMatchers);

        verify(persNotifyClient, times(emailCount)).createEvent(MockitoHamcrest.argThat(
                Matchers.allOf(matchers)
        ));
    }

    private void assertEmailWasSent(Notification notification,
                                    List<Matcher<? super NotificationEventSource>> matchers)
            throws PersNotifyClientException {
        assertThat(notification.getDeliveryChannels(), contains(
                hasProperty("type", is(ChannelType.EMAIL))
        ));

        deliverNotifications();
        verify(persNotifyClient).createEvent(MockitoHamcrest.argThat(Matchers.allOf(matchers)));
    }

    public List<NotificationEventSource> getSentEmails() {
        ArgumentCaptor<NotificationEventSource> eventSourceArgumentCaptor =
                ArgumentCaptor.forClass(NotificationEventSource.class);

        try {
            verify(persNotifyClient, Mockito.atLeastOnce()).createEvent(eventSourceArgumentCaptor.capture());
        } catch (PersNotifyClientException pex) {
            throw new RuntimeException(pex);
        }

        return eventSourceArgumentCaptor.getAllValues()
                .stream()
                .filter(nes -> nes.getEventAddressType() == EventAddressType.MAIL)
                .collect(Collectors.toList());
    }

    public void deliverNotifications() {
        barrierUnblockingTmsJob.doJob(null);

        List<Barrier> allBarriers = barrierDao.findAllBarriers();
        LOG.info("Remaining barriers: {}", allBarriers);

        List<Notification> allNotifications = inboxDao.getAllNotifications();
        LOG.info("Pending notifications: {}", allNotifications);

        AtomicInteger stream = new AtomicInteger();
        deliveryWorkerJobs.forEach(task -> task.doJob(null, stream.getAndIncrement()));

        AtomicInteger stream2 = new AtomicInteger();
        failedDeliveryWorkerJobs.forEach(task -> task.doJob(null, stream2.getAndIncrement()));
    }

    public List<Notification> getNotifications(ChannelType channelType) {
        return inboxDao.getAllNotifications()
                .stream()
                .filter(n -> n.getDeliveryChannels()
                        .stream()
                        .anyMatch(dc -> dc.getType() == channelType)
                )
                .collect(Collectors.toList());
    }

    public List<Notification> getAllNotifications() {
        return inboxDao.getAllNotifications();
    }

    public List<OrderHistoryEvent> getAggregatedOrderHistoryEvents(int eventsNumber) {
        return getAggregatedOrderHistoryEvents(eventsNumber, order -> {
            order.setRgb(Color.BLUE);
            order.setFulfilment(true);
        });
    }

    public List<OrderHistoryEvent> getAggregatedOrderHistoryEvents(int eventsNumber, Consumer<Order> orderModifier) {
        return getAggregatedOrderHistoryEvents(null, eventsNumber, orderModifier);
    }

    public List<OrderHistoryEvent> getAggregatedOrderHistoryEvents(Buyer buyer, int eventsNumber) {
        return LongStream.range(1, eventsNumber + 1)
                .mapToObj(id -> orderStatusUpdated(
                        PENDING,
                        PROCESSING,
                        new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                        order -> {
                            order.setId(id);
                            order.setRgb(Color.BLUE);
                            order.setFulfilment(true);
                            if (buyer != null) {
                                order.setBuyer(buyer);
                            }
                        }))
                .collect(toList());
    }

    public List<OrderHistoryEvent> getAggregatedOrderHistoryEvents(Buyer buyer,
                                                                   int eventsNumber,
                                                                   Consumer<Order> orderModifier) {
        return LongStream.range(1, eventsNumber + 1)
                .mapToObj(id -> orderStatusUpdated(
                        PENDING,
                        PROCESSING,
                        new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                        order -> {
                            order.setId(id);
                            if (buyer != null) {
                                order.setBuyer(buyer);
                            }
                            orderModifier.accept(order);
                        }))
                .collect(toList());
    }

    public List<OrderHistoryEvent> getOneOrderHistoryEvents(long orderId, int eventsNumber) {
        return LongStream.range(1, eventsNumber + 1)
                .mapToObj(id -> orderStatusUpdated(
                        PENDING,
                        PROCESSING,
                        new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                        order -> {
                            order.setId(orderId);
                            order.setRgb(Color.BLUE);
                            order.setFulfilment(true);
                        }))
                .collect(toList());
    }

    public void assertMbiNoteWasSent() throws IOException {
        ClientHttpRequestFactory requestMock = mbiRestTemplate.getRequestFactory();

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(requestMock, times(1)).createRequest(uriCaptor.capture(), eq(HttpMethod.POST));

        String expectedQuery = "shop_id=123&template_id=1714360125";

        assertEquals(expectedQuery, uriCaptor.getValue().getQuery());
    }

    public List<NotificationEventSource> getSentPushes() {
        ArgumentCaptor<NotificationEventSource> eventSourceArgumentCaptor =
                ArgumentCaptor.forClass(NotificationEventSource.class);

        try {
            verify(persNotifyClient, Mockito.atLeastOnce()).createEvent(eventSourceArgumentCaptor.capture());
        } catch (PersNotifyClientException pex) {
            throw new RuntimeException(pex);
        }

        return eventSourceArgumentCaptor.getAllValues()
                .stream()
                .filter(nes -> nes.getNotificationSubtype().getTransportType() == NotificationTransportType.PUSH)
                .collect(Collectors.toList());
    }

    public void assertNoPushesSent() {
        try {
            verify(persNotifyClient, Mockito.never()).createEvent(Mockito.any());
        } catch (PersNotifyClientException ex) {
            throw new RuntimeException(ex);
        }
    }
}
