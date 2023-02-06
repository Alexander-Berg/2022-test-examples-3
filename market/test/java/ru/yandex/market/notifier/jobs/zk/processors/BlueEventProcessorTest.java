package ru.yandex.market.notifier.jobs.zk.processors;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Iterables;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import ru.yandex.common.util.region.RegionService;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.DeliveryDatesChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.shop.ScheduleLine;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.notification.context.OrderContextParams;
import ru.yandex.market.notifier.application.AbstractWebTestBase;
import ru.yandex.market.notifier.core.NotifierMapperService;
import ru.yandex.market.notifier.criteria.EvictionSearch;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.DeliveryStatisticsFull;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.entity.NotificationStatus;
import ru.yandex.market.notifier.entity.NotifierProperties;
import ru.yandex.market.notifier.mock.NotifierTestMockFactory;
import ru.yandex.market.notifier.service.BarrierService;
import ru.yandex.market.notifier.service.InboxService;
import ru.yandex.market.notifier.service.MbiNotificationService;
import ru.yandex.market.notifier.service.MobilePushService;
import ru.yandex.market.notifier.service.ShopMetaService;
import ru.yandex.market.notifier.storage.InboxDao;
import ru.yandex.market.notifier.util.EventTestUtils;
import ru.yandex.market.notifier.util.NotifierTestUtils;
import ru.yandex.market.notifier.util.providers.DeliveryProvider;
import ru.yandex.market.notifier.util.providers.EventsProvider;
import ru.yandex.market.notifier.util.providers.OrderItemProvider;
import ru.yandex.market.notifier.util.providers.OrderProvider;
import ru.yandex.market.notifier.xml.ShopsOutletSaxHandlerTest;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.request.trace.RequestContextHolder;
import ru.yandex.market.sdk.userinfo.service.UidConstants;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.xmlunit.matchers.CompareMatcher.isIdenticalTo;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.USER;
import static ru.yandex.market.checkout.checkouter.order.promo.ItemPromo.cashbackPromo;
import static ru.yandex.market.notifier.entity.ChannelType.MOBILE_PUSH;
import static ru.yandex.market.notifier.jobs.zk.processors.BlueEventProcessor.IMMEDIATE_CANCELLED_ORDER_NOTIFICATION_ID;
import static ru.yandex.market.notifier.jobs.zk.processors.BlueEventProcessor.IMMEDIATE_NEW_ORDER_NOTIFICATION_ID;
import static ru.yandex.market.notifier.jobs.zk.processors.BlueEventProcessor.IMMEDIATE_STATUS_CHANGE_MOBILE_NOTIFICATION_ID;
import static ru.yandex.market.notifier.jobs.zk.processors.BlueEventProcessor.NOTIFY_SHOP_PI_ORDER_STATUS_PENDING;
import static ru.yandex.market.notifier.jobs.zk.processors.BlueEventProcessor.NOTIFY_SHOP_PI_ORDER_STATUS_PENDING_WHITE;
import static ru.yandex.market.notifier.util.NotifierTestUtils.DEFAULT_EVENT_DATE;
import static ru.yandex.market.notifier.util.providers.EventsProvider.makeOrderForMock;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.PUSH_YAPLUS_DELIVERED_CASH_BACK;

public class BlueEventProcessorTest extends AbstractWebTestBase {

    public static List<ScheduleLine> MOCK_SCHEDULE = List.of(
            new ScheduleLine(1, 0, 60 * 24),
            new ScheduleLine(2, 0, 60 * 24),
            new ScheduleLine(3, 0, 60 * 24),
            new ScheduleLine(4, 0, 60 * 24),
            new ScheduleLine(5, 0, 60 * 24),
            new ScheduleLine(6, 0, 60 * 24),
            new ScheduleLine(7, 0, 60 * 24)
    );
    public final static String DEFAULT_CASHBACK_PROMO_KEY = "1";
    private final static Instant WHEN_PICKUP_IN_TRIGGER_PLATFORM_DATE = Instant.parse("2021-08-16T11:12:55.00Z");
    private final static Instant WHEN_LAST_MILE_STARTED_IN_TRIGGER_PLATFORM_DATE = Instant.parse("2021-08-16T11:12:55" +
            ".00Z");
    private final static Set<String> IGNORED_IN_COMPARISON_DBS_MAIL_FIELDS = Set.of(
            "hours-for-confirmation",
            "minutes-for-confirmation"
    );

    private final MobilePushMessageSerializer mobilePushMessageSerializer = new MobilePushMessageSerializer();

    private OrderHistoryEvent goldenEvent;
    private OrderHistoryEvent deliveryReceiptEvent;

    @Autowired
    private MobilePushService mobilePushService;
    @Autowired
    private InboxService inboxService;
    @Autowired
    private RegionService regionService;
    @Autowired
    private ShopMetaService shopMetaService;
    @Autowired
    private EventTestUtils eventTestUtils;
    @Autowired
    private NotifierMapperService jacksonHelper;
    @Autowired
    private InboxDao inboxDao;
    @Autowired
    private NotifierProperties notifierProperties;
    @Autowired
    private BarrierService barrierService;
    @Autowired
    private MbiNotificationService mbiNotificationService;
    @Autowired
    private MbiApiClient mbiApiClient;

    private BlueEventProcessor blueEventProcessor;

    @BeforeEach
    public void before() {
        blueEventProcessor = new BlueEventProcessor(checkouterClient, shopMetaService,
                inboxService, regionService, mobilePushService, 3,
                jacksonHelper, barrierService, mbiNotificationService,
                notifierProperties,
                Clock.systemDefaultZone(), 30, 48);

        goldenEvent = EventsProvider.getGoldenOrderHistoryEvent();
        deliveryReceiptEvent = EventsProvider.getDeliveryReceiptEvent();
        when(checkouterClient.shops())
                .thenReturn(Mockito.mock(CheckouterShopApi.class));
        RequestContextHolder.createNewContext();
        notifierProperties.setEnableProcessingOrderPartnerNotifications(true);
        Mockito.reset(mbiApiClient);
    }

    @Test
    public void runJobGoldenTest() {
        eventTestUtils.mockEvent(goldenEvent);
        assertTrue(inboxService.getDeliveryStatisticsFull().isEmpty());
        eventTestUtils.runImport();

        List<DeliveryStatisticsFull> statisticsFullList = inboxService.getDeliveryStatisticsFull();
        Assertions.assertEquals(1, statisticsFullList.size());
        Assertions.assertEquals(1, getPushToShopNotificationCount(statisticsFullList, ChannelType.PUSH));
    }

    @Test
    public void goldenMetadataError() {
        OrderHistoryEvent statusUpdatedEvent = EventsProvider.orderStatusUpdated(
                OrderStatus.PENDING,
                OrderStatus.CANCELLED,
                new ClientInfo(USER, goldenEvent.getOrderAfter().getBuyer().getUid())
        );
        statusUpdatedEvent.getOrderAfter().setRgb(Color.BLUE);
        statusUpdatedEvent.getOrderAfter().setShopId(NotifierTestMockFactory.INVALID_SHOP_ID);
        statusUpdatedEvent.getOrderBefore().setShopId(NotifierTestMockFactory.INVALID_SHOP_ID);

        Assertions.assertNotNull(blueEventProcessor.getOrderData(statusUpdatedEvent, false));
    }

    @Test
    public void sendPushToShopOnChangeStatus() {
        OrderHistoryEvent statusUpdatedEvent = EventsProvider.getGoldenOrderHistoryEvent();
        assertTrue(inboxService.getDeliveryStatisticsFull().isEmpty());

        int i = 0;
        for (ClientRole role : ClientRole.values()) {
            processEvent(statusUpdatedEvent, role);
            Assertions.assertEquals(++i, getPushToShopNotificationCount(
                    inboxService.getDeliveryStatisticsFull(), ChannelType.PUSH));
        }
    }

    @Test
    public void testNotSendPushIfOrderCancelledByReservation() {
        Order orderAfter = OrderProvider.getColorOrder(Color.BLUE);
        orderAfter.setStatus(OrderStatus.CANCELLED);
        orderAfter.setSubstatus(OrderSubstatus.RESERVATION_FAILED);
        orderAfter.setId(123L);

        OrderHistoryEvent substatusUpdateEvent = EventsProvider.getBlueOrderHistoryEvent();
        substatusUpdateEvent.setTranDate(new Date());
        substatusUpdateEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        substatusUpdateEvent.getOrderBefore().setStatus(OrderStatus.PENDING);
        substatusUpdateEvent.getOrderBefore().setSubstatus(OrderSubstatus.PENDING_CANCELLED);
        substatusUpdateEvent.setOrderAfter(orderAfter);

        assertTrue(inboxService.getDeliveryStatisticsFull().isEmpty());

        eventTestUtils.mockEvent(substatusUpdateEvent);
        eventTestUtils.runImport();
        assertTrue(inboxService.getDeliveryStatisticsFull().isEmpty());

    }

    @Test
    public void testAvoidingSendingNotificationToSupplierIfToggleIsOff() throws Exception {
        notifierProperties.setDisableSupplierNotificationNewOrderInMbi(true);
        when(checkouterClient.shops().getSchedule(anyLong()))
                .thenReturn(List.of(new ScheduleLine(5, 720, 900))); // Пятница, 12 - 15 часов
        Order orderAfter = OrderProvider.getColorOrder(Color.BLUE);
        orderAfter.setId(321L);
        orderAfter.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        orderAfter.setStatus(OrderStatus.PENDING);
        orderAfter.setSubstatus(OrderSubstatus.AWAIT_CONFIRMATION);
        Parcel parcel = new Parcel();
        parcel.setShipmentDateTimeBySupplier(LocalDateTime.of(2021, 3, 10, 12, 0, 0));
        orderAfter.getDelivery().setParcels(Collections.singletonList(parcel));

        OrderHistoryEvent substatusUpdateEvent = EventsProvider.getBlueOrderHistoryEvent();
        substatusUpdateEvent.setTranDate(Date.from(LocalDateTime.of(2021, Month.JUNE, 18, 0, 0)
                .atZone(ZoneId.systemDefault()).toInstant()));
        substatusUpdateEvent.setType(HistoryEventType.ORDER_SUBSTATUS_UPDATED);
        substatusUpdateEvent.getOrderBefore().setStatus(OrderStatus.PENDING);
        substatusUpdateEvent.getOrderBefore().setSubstatus(OrderSubstatus.ANTIFRAUD);
        substatusUpdateEvent.setOrderAfter(orderAfter);

        eventTestUtils.mockEvent(substatusUpdateEvent);
        assertThat(inboxService.getDeliveryStatisticsFull(), is(empty()));
        eventTestUtils.runImport();

        verify(mbiApiClient, Mockito.times(0))
                .sendMessageToSupplier(
                        Mockito.anyLong(),
                        Mockito.anyInt(),
                        Mockito.anyString(),
                        Mockito.any()
                );
    }

    @Test
    public void testAvoidingSendingNotificationToSupplierIfOrderSubstatusIsAntifraud() {
        OrderHistoryEvent statusUpdateEvent = EventsProvider.getBlueOrderHistoryEvent();
        statusUpdateEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        statusUpdateEvent.getOrderAfter().setId(444L);
        statusUpdateEvent.getOrderAfter().setStatus(OrderStatus.PENDING);
        statusUpdateEvent.getOrderAfter().setSubstatus(OrderSubstatus.ANTIFRAUD);
        processEvent(statusUpdateEvent, ClientRole.SYSTEM);

        verify(mbiApiClient, Mockito.times(0))
                .sendMessageToSupplier(Mockito.anyLong(),
                        Mockito.anyInt(),
                        Mockito.anyString(),
                        Mockito.eq(new OrderContextParams(444L, OrderStatus.PENDING.getId())));
    }

    @Test
    public void testSendingNotificationToSupplierIfOrderSubstatusIsAwaitConfirmationAfterAntifraud() {

        when(checkouterClient.shops().getSchedule(anyLong()))
                .thenReturn(List.of(new ScheduleLine(5, 720, 900))); // Пятница, 12 - 15 часов
        Order orderAfter = OrderProvider.getColorOrder(Color.BLUE);
        orderAfter.setId(321L);
        orderAfter.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        orderAfter.setStatus(OrderStatus.PENDING);
        orderAfter.setSubstatus(OrderSubstatus.AWAIT_CONFIRMATION);
        Parcel parcel = new Parcel();
        parcel.setShipmentDateTimeBySupplier(LocalDateTime.of(2021, 3, 10, 12, 0, 0));
        orderAfter.getDelivery().setParcels(Collections.singletonList(parcel));
        orderAfter.getItems().forEach(item -> item.setOfferId("test_offer_id"));

        OrderHistoryEvent substatusUpdateEvent = EventsProvider.getBlueOrderHistoryEvent();
        substatusUpdateEvent.setTranDate(Date.from(LocalDateTime.of(2021, Month.JUNE, 18, 0, 0)
                .atZone(ZoneId.systemDefault()).toInstant()));
        substatusUpdateEvent.setType(HistoryEventType.ORDER_SUBSTATUS_UPDATED);
        substatusUpdateEvent.getOrderBefore().setStatus(OrderStatus.PENDING);
        substatusUpdateEvent.getOrderBefore().setSubstatus(OrderSubstatus.ANTIFRAUD);
        substatusUpdateEvent.setOrderAfter(orderAfter);

        eventTestUtils.mockEvent(substatusUpdateEvent);
        assertThat(inboxService.getDeliveryStatisticsFull(), is(empty()));
        eventTestUtils.runImport();

        verify(mbiApiClient, Mockito.times(1))
                .sendMessageToSupplier(Mockito.anyLong(),
                        Mockito.eq(IMMEDIATE_NEW_ORDER_NOTIFICATION_ID),
                        Mockito.argThat(data -> {
                            assertMailData(data, "/files/email-data.xml");
                            return true;
                        }),
                        Mockito.eq(new OrderContextParams(321L, OrderStatus.PENDING.getId())));
    }

    @Test
    public void testDBSNewOrderNotification() throws IOException {
        /*
        Given
         */
        ArgumentCaptor<String> messageData = ArgumentCaptor.forClass(String.class);

        when(checkouterClient.shops().getSchedule(anyLong()))
                .thenReturn(List.of(new ScheduleLine(5, 720, 900))); // Пятница, 12 - 15 часов
        /*
        When
         */
        eventTestUtils.mockEvent(createNewDbsOrderEvent());
        eventTestUtils.runImport();
        /*
        Then
         */
        verify(mbiApiClient, Mockito.times(1))
                .sendMessageToShop(Mockito.anyLong(),
                        Mockito.eq(IMMEDIATE_NEW_ORDER_NOTIFICATION_ID),
                        messageData.capture());
        assertNotNull(messageData.getValue());

        String expected = IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream("/files/dbs-email-data.xml")),
                StandardCharsets.UTF_8
        );
        Diff diff = DiffBuilder.compare(expected).withTest(messageData.getValue())
                .ignoreWhitespace()
                .withNodeFilter(node -> !IGNORED_IN_COMPARISON_DBS_MAIL_FIELDS.contains(node.getNodeName()))
                .build();
        assertFalse(diff.hasDifferences(), "Has differences: " + diff.getDifferences());
    }

    @Test
    public void testAvoidingSendingNotificationToSupplierIfOrderSubstatusIsAwaitConfirmationNotAfterAntifraud() {

        when(checkouterClient.shops().getSchedule(anyLong()))
                .thenReturn(List.of(new ScheduleLine(5, 720, 900))); // Пятница, 12 - 15 часов
        Order orderAfter = OrderProvider.getColorOrder(Color.BLUE);
        orderAfter.setId(123L);
        orderAfter.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        orderAfter.setStatus(OrderStatus.PENDING);
        orderAfter.setSubstatus(OrderSubstatus.AWAIT_CONFIRMATION);
        Parcel parcel = new Parcel();
        parcel.setShipmentDateTimeBySupplier(LocalDateTime.of(2021, 3, 10, 12, 0, 0));
        orderAfter.getDelivery().setParcels(Collections.singletonList(parcel));

        OrderHistoryEvent substatusUpdateEvent = EventsProvider.getBlueOrderHistoryEvent();
        substatusUpdateEvent.setTranDate(new Date());
        substatusUpdateEvent.setType(HistoryEventType.ORDER_SUBSTATUS_UPDATED);
        substatusUpdateEvent.getOrderBefore().setStatus(OrderStatus.PENDING);
        substatusUpdateEvent.setOrderAfter(orderAfter);

        eventTestUtils.mockEvent(substatusUpdateEvent);
        assertThat(inboxService.getDeliveryStatisticsFull(), is(empty()));
        eventTestUtils.runImport();

        verify(mbiApiClient, Mockito.times(0))
                .sendMessageToSupplier(Mockito.anyLong(),
                        Mockito.anyInt(),
                        Mockito.anyString(),
                        Mockito.eq(new OrderContextParams(123L, OrderStatus.PENDING.getId())));
    }

    @Test
    public void testSendPartnerMobileAppNotificationToSupplierIfOrderCancelled() {
        Order orderAfter = OrderProvider.getColorOrder(Color.BLUE);
        orderAfter.setId(123L);
        orderAfter.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        orderAfter.setStatus(OrderStatus.CANCELLED);
        Parcel parcel = new Parcel();
        parcel.setShipmentDateTimeBySupplier(LocalDateTime.of(2021, 3, 10, 12, 0, 0));
        orderAfter.getDelivery().addFeatures(Set.of(DeliveryFeature.EXPRESS_DELIVERY));

        OrderHistoryEvent statusUpdateEvent = EventsProvider.getBlueOrderHistoryEvent();
        statusUpdateEvent.setTranDate(new Date());
        statusUpdateEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        statusUpdateEvent.getOrderBefore().setStatus(OrderStatus.UNPAID);
        statusUpdateEvent.setOrderAfter(orderAfter);

        eventTestUtils.mockEvent(statusUpdateEvent);
        assertThat(inboxService.getDeliveryStatisticsFull(), is(empty()));
        eventTestUtils.runImport();
        verify(mbiApiClient, Mockito.times(1))
                .sendMessageToSupplier(Mockito.anyLong(),
                        Mockito.eq(IMMEDIATE_STATUS_CHANGE_MOBILE_NOTIFICATION_ID),
                        Mockito.anyString(),
                        Mockito.any()
                );
    }

    @Test
    public void testSendPartnerMobileAppAndPINotificationToSupplierIfOrderCancelledByUser() {
        Order orderAfter = OrderProvider.getColorOrder(Color.BLUE);
        orderAfter.setId(123L);
        orderAfter.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        orderAfter.setStatus(OrderStatus.CANCELLED);
        orderAfter.setSubstatus(OrderSubstatus.USER_CHANGED_MIND);
        Parcel parcel = new Parcel();
        parcel.setShipmentDateTimeBySupplier(LocalDateTime.of(2021, 3, 10, 12, 0, 0));
        orderAfter.getDelivery().addFeatures(Set.of(DeliveryFeature.EXPRESS_DELIVERY));

        OrderHistoryEvent statusUpdateEvent = EventsProvider.getBlueOrderHistoryEvent();
        statusUpdateEvent.setTranDate(new Date());
        statusUpdateEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        statusUpdateEvent.getOrderBefore().setStatus(OrderStatus.PROCESSING);
        statusUpdateEvent.setOrderAfter(orderAfter);

        eventTestUtils.mockEvent(statusUpdateEvent);
        assertThat(inboxService.getDeliveryStatisticsFull(), is(empty()));
        eventTestUtils.runImport();
        verify(mbiApiClient, Mockito.times(1))
                .sendMessageToSupplier(Mockito.anyLong(),
                        Mockito.eq(IMMEDIATE_STATUS_CHANGE_MOBILE_NOTIFICATION_ID),
                        Mockito.anyString(),
                        Mockito.any()
                );
        verify(mbiApiClient, Mockito.times(1))
                .sendMessageToSupplier(Mockito.anyLong(),
                        Mockito.eq(IMMEDIATE_CANCELLED_ORDER_NOTIFICATION_ID),
                        Mockito.anyString(),
                        Mockito.any()
                );
    }

    @Test
    public void testSendPartnerMobileAppAndNotSendCancelledOrderNotificationToPI() {
        Order orderAfter = OrderProvider.getColorOrder(Color.BLUE);
        orderAfter.setId(123L);
        orderAfter.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        orderAfter.setStatus(OrderStatus.CANCELLED);
        orderAfter.setSubstatus(OrderSubstatus.USER_CHANGED_MIND);
        Parcel parcel = new Parcel();
        parcel.setShipmentDateTimeBySupplier(LocalDateTime.of(2021, 3, 10, 12, 0, 0));
        orderAfter.getDelivery().addFeatures(Set.of(DeliveryFeature.EXPRESS_DELIVERY));

        OrderHistoryEvent statusUpdateEvent = EventsProvider.getBlueOrderHistoryEvent();
        statusUpdateEvent.setTranDate(new Date());
        statusUpdateEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        statusUpdateEvent.getOrderBefore().setStatus(OrderStatus.PROCESSING);
        statusUpdateEvent.getOrderBefore().setSubstatus(OrderSubstatus.SHIPPED);
        statusUpdateEvent.setOrderAfter(orderAfter);

        eventTestUtils.mockEvent(statusUpdateEvent);
        assertThat(inboxService.getDeliveryStatisticsFull(), is(empty()));
        eventTestUtils.runImport();
        verify(mbiApiClient, Mockito.times(1))
                .sendMessageToSupplier(Mockito.anyLong(),
                        Mockito.eq(IMMEDIATE_STATUS_CHANGE_MOBILE_NOTIFICATION_ID),
                        Mockito.anyString(),
                        Mockito.any()
                );
        verify(mbiApiClient, Mockito.times(0))
                .sendMessageToSupplier(Mockito.anyLong(),
                        Mockito.eq(IMMEDIATE_CANCELLED_ORDER_NOTIFICATION_ID),
                        Mockito.anyString(),
                        Mockito.any()
                );
    }

    @Test
    public void testAvoidingSendPINotificationToSupplierIfOrderCancelledNotByUser() {
        Order orderAfter = OrderProvider.getColorOrder(Color.BLUE);
        orderAfter.setId(123L);
        orderAfter.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        orderAfter.setStatus(OrderStatus.CANCELLED);
        orderAfter.setSubstatus(OrderSubstatus.SHOP_FAILED);
        Parcel parcel = new Parcel();
        parcel.setShipmentDateTimeBySupplier(LocalDateTime.of(2021, 3, 10, 12, 0, 0));
        orderAfter.getDelivery().addFeatures(Set.of(DeliveryFeature.EXPRESS_DELIVERY));

        OrderHistoryEvent statusUpdateEvent = EventsProvider.getBlueOrderHistoryEvent();
        statusUpdateEvent.setTranDate(new Date());
        statusUpdateEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        statusUpdateEvent.getOrderBefore().setStatus(OrderStatus.PROCESSING);
        statusUpdateEvent.setOrderAfter(orderAfter);

        eventTestUtils.mockEvent(statusUpdateEvent);
        assertThat(inboxService.getDeliveryStatisticsFull(), is(empty()));
        eventTestUtils.runImport();
        verify(mbiApiClient, Mockito.times(1))
                .sendMessageToSupplier(Mockito.anyLong(),
                        Mockito.eq(IMMEDIATE_STATUS_CHANGE_MOBILE_NOTIFICATION_ID),
                        Mockito.anyString(),
                        Mockito.any()
                );
        verify(mbiApiClient, Mockito.times(0))
                .sendMessageToSupplier(Mockito.anyLong(),
                        Mockito.eq(IMMEDIATE_CANCELLED_ORDER_NOTIFICATION_ID),
                        Mockito.anyString(),
                        Mockito.any()
                );
    }

    @Test
    public void testAvoidingSendPINotificationToSupplierIfFakeOrder() {
        Order orderAfter = OrderProvider.getColorOrder(Color.BLUE);
        orderAfter.setId(123L);
        orderAfter.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        orderAfter.setStatus(OrderStatus.CANCELLED);
        orderAfter.setSubstatus(OrderSubstatus.USER_CHANGED_MIND);
        Parcel parcel = new Parcel();
        parcel.setShipmentDateTimeBySupplier(LocalDateTime.of(2021, 3, 10, 12, 0, 0));
        orderAfter.getDelivery().addFeatures(Set.of(DeliveryFeature.EXPRESS_DELIVERY));

        OrderHistoryEvent statusUpdateEvent = EventsProvider.getBlueOrderHistoryEvent();
        statusUpdateEvent.setTranDate(new Date());
        statusUpdateEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        statusUpdateEvent.getOrderBefore().setStatus(OrderStatus.PROCESSING);
        statusUpdateEvent.getOrderBefore().setFake(true);
        statusUpdateEvent.setOrderAfter(orderAfter);

        eventTestUtils.mockEvent(statusUpdateEvent);
        assertThat(inboxService.getDeliveryStatisticsFull(), is(empty()));
        eventTestUtils.runImport();
        verify(mbiApiClient, Mockito.times(1))
                .sendMessageToSupplier(Mockito.anyLong(),
                        Mockito.eq(IMMEDIATE_STATUS_CHANGE_MOBILE_NOTIFICATION_ID),
                        Mockito.anyString(),
                        Mockito.any()
                );
        verify(mbiApiClient, Mockito.times(0))
                .sendMessageToSupplier(Mockito.anyLong(),
                        Mockito.eq(IMMEDIATE_CANCELLED_ORDER_NOTIFICATION_ID),
                        Mockito.anyString(),
                        Mockito.any()
                );
    }

    @Test
    public void testAvoidingSendingNotificationShootingOrder() {
        Order orderAfter = OrderProvider.getColorOrder(Color.BLUE);
        orderAfter.setId(123L);
        orderAfter.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        orderAfter.setStatus(OrderStatus.PROCESSING);
        orderAfter.getBuyer().setUid(UidConstants.NO_SIDE_EFFECT_UID);
        Parcel parcel = new Parcel();
        parcel.setShipmentDateTimeBySupplier(LocalDateTime.of(2021, 3, 10, 12, 0, 0));
        orderAfter.getDelivery().addFeatures(Set.of(DeliveryFeature.EXPRESS_DELIVERY));

        OrderHistoryEvent statusUpdateEvent = EventsProvider.getBlueOrderHistoryEvent();
        statusUpdateEvent.setTranDate(new Date());
        statusUpdateEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        statusUpdateEvent.getOrderBefore().setStatus(OrderStatus.PENDING);
        statusUpdateEvent.getOrderBefore().setSubstatus(OrderSubstatus.ANTIFRAUD);
        statusUpdateEvent.setOrderAfter(orderAfter);

        eventTestUtils.mockEvent(statusUpdateEvent);
        assertThat(inboxService.getDeliveryStatisticsFull(), is(empty()));
        eventTestUtils.runImport();

        verifyNoInteractions(mbiApiClient);
    }

    @Test
    public void testPartnerMobileAppNotificationOrderPendingToProcessingNotSent() {
        Order orderAfter = OrderProvider.getColorOrder(Color.BLUE);
        orderAfter.setId(123L);
        orderAfter.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        orderAfter.setStatus(OrderStatus.PROCESSING);
        Parcel parcel = new Parcel();
        parcel.setShipmentDateTimeBySupplier(LocalDateTime.of(2021, 3, 10, 12, 0, 0));
        orderAfter.getDelivery().addFeatures(Set.of(DeliveryFeature.EXPRESS_DELIVERY));

        OrderHistoryEvent statusUpdateEvent = EventsProvider.getBlueOrderHistoryEvent();
        statusUpdateEvent.setTranDate(new Date());
        statusUpdateEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        statusUpdateEvent.getOrderBefore().setStatus(OrderStatus.PENDING);
        statusUpdateEvent.setOrderAfter(orderAfter);

        eventTestUtils.mockEvent(statusUpdateEvent);
        assertThat(inboxService.getDeliveryStatisticsFull(), is(empty()));
        eventTestUtils.runImport();
        verify(mbiApiClient, Mockito.times(0))
                .sendMessageToSupplier(Mockito.anyLong(),
                        Mockito.eq(IMMEDIATE_STATUS_CHANGE_MOBILE_NOTIFICATION_ID),
                        Mockito.anyString(),
                        Mockito.any()
                );
    }

    @Test
    public void testAvoidSendingNotificationToSupplierIfOrderManuallyAccepted() {

        when(checkouterClient.shops().getSchedule(anyLong()))
                .thenReturn(List.of(new ScheduleLine(5, 720, 900))); // Пятница, 12 - 15 часов
        Order orderAfter = OrderProvider.getColorOrder(Color.BLUE);
        orderAfter.setId(123L);
        orderAfter.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        orderAfter.setStatus(OrderStatus.PROCESSING);
        orderAfter.setSubstatus(OrderSubstatus.STARTED);
        Parcel parcel = new Parcel();
        parcel.setShipmentDateTimeBySupplier(LocalDateTime.of(2021, 3, 10, 12, 0, 0));
        orderAfter.getDelivery().setParcels(Collections.singletonList(parcel));

        OrderHistoryEvent substatusUpdateEvent = EventsProvider.getBlueOrderHistoryEvent();
        substatusUpdateEvent.setTranDate(new Date());
        substatusUpdateEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        substatusUpdateEvent.getOrderBefore().setStatus(OrderStatus.PENDING);
        substatusUpdateEvent.setOrderAfter(orderAfter);

        eventTestUtils.mockEvent(substatusUpdateEvent);
        assertThat(inboxService.getDeliveryStatisticsFull(), is(empty()));
        eventTestUtils.runImport();

        verify(mbiApiClient, Mockito.times(0))
                .sendMessageToSupplier(Mockito.anyLong(),
                        Mockito.anyInt(),
                        Mockito.anyString(),
                        Mockito.any());
    }

    @Test
    public void eventWithoutStatusExpiryAtShouldWorkCorrectly() {
        OrderHistoryEvent event = EventsProvider.orderStatusUpdated(
                OrderStatus.PLACING,
                OrderStatus.UNPAID,
                new ClientInfo(USER, goldenEvent.getOrderAfter().getBuyer().getUid())
        );
        event.getOrderAfter().setRgb(Color.BLUE);
        event.getOrderAfter().setShopId(NotifierTestMockFactory.INVALID_SHOP_ID);
        event.getOrderAfter().setStatusExpiryDate(null);
        event.getOrderBefore().setShopId(NotifierTestMockFactory.INVALID_SHOP_ID);
        when(checkouterClient.getOrder(anyLong(), any(), any()))
                .thenReturn(makeOrderForMock(event, true));

        Assertions.assertEquals(ProcessEventResult.ok(),
                blueEventProcessor.process(event, null));
        Assertions.assertNotNull(blueEventProcessor.getOrderData(event, false));

        // Повторим, но уже без StatusExpiryDate из Чекаутера
        event.getOrderAfter().setStatusExpiryDate(null);
        when(checkouterClient.getOrder(anyLong(), any(), any()))
                .thenReturn(makeOrderForMock(event, false));
        // Не должны падать
        Assertions.assertNotNull(blueEventProcessor.getOrderData(event, false));
        Assertions.assertNull(event.getOrderAfter().getStatusExpiryDate());
    }

    @Test
    public void deliveryReceiptEmailTest() {
        eventTestUtils.mockEvent(deliveryReceiptEvent);
        assertTrue(inboxService.getDeliveryStatisticsFull().isEmpty());
        eventTestUtils.runImport();

        List<DeliveryStatisticsFull> statisticsFullList = inboxService.getDeliveryStatisticsFull();
        Assertions.assertEquals(1, statisticsFullList.size());
        Assertions.assertEquals(1, getPushToShopNotificationCount(statisticsFullList, ChannelType.EMAIL));
    }

    @Test
    public void testPushCheckouterEventCreatedTsExist() {

        blueEventProcessor.process(EventsProvider.orderStatusUpdated(
                OrderStatus.PLACING,
                OrderStatus.UNPAID,
                new ClientInfo(ClientRole.USER, 12345L),
                order -> {
                }
        ), Set.of());

        List<Notification> notifications = inboxDao.getAllNotifications();

        Notification mobilePushNotification = notifications.stream()
                .filter(n -> n.getDeliveryChannels().stream().anyMatch(c -> c.getType() == MOBILE_PUSH))
                .findFirst()
                .orElseThrow();
        NotificationEventSource eventSource = mobilePushMessageSerializer.deserialize(
                mobilePushNotification.getData().getBytes());
        assertEquals(String.valueOf(DEFAULT_EVENT_DATE.toEpochMilli()),
                eventSource.getData().get(NotificationEventDataName.CHECKOUTER_EVENT_CREATED_TS));
    }

    static Stream<Arguments> scheduleShopNotificationArgs() {
        return Stream.of(
                Arguments.of(Color.WHITE, ChannelType.MBI),
                Arguments.of(Color.BLUE, ChannelType.MBI_API)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scheduleShopNotificationArgs")
    public void scheduleShopNotification(Color color, ChannelType channelType) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        when(checkouterClient.shops().getSchedule(anyLong()))
                .thenReturn(List.of(new ScheduleLine(5, 720, 900))); // Пятница, 12 - 15 часов
        Order order = OrderProvider.getColorOrder(color);
        order.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        Parcel parcel = new Parcel();
        parcel.setShipmentDateTimeBySupplier(LocalDateTime.of(2021, 3, 10, 12, 0, 0));
        order.getDelivery().setParcels(Collections.singletonList(parcel));
        OrderHistoryEvent event = EventsProvider.orderStatusUpdated(order, OrderStatus.RESERVED, OrderStatus.PENDING);

        eventTestUtils.mockEvent(event);
        assertThat(inboxService.getDeliveryStatisticsFull(), is(empty()));
        eventTestUtils.runImport();

        EvictionSearch es = new EvictionSearch(NotificationStatus.NEW, Collections.singletonList(order.getId()));
        List<Notification> mbiNotifications = inboxService.evictionSearch(es)
                .stream()
                .filter(n -> List.of(NOTIFY_SHOP_PI_ORDER_STATUS_PENDING, NOTIFY_SHOP_PI_ORDER_STATUS_PENDING_WHITE)
                        .contains(n.getType()))
                .filter(this::nonNullSendAfter)
                .filter(n -> containsDeliveryChannelType(n, channelType))
                .collect(Collectors.toList());
        assertThat(mbiNotifications, hasSize(1));
        Date expectedDate = Date.from(LocalDateTime.parse("2016-12-09 13:00", formatter).toInstant(ZoneOffset.UTC));
        assertThat(mbiNotifications.iterator().next().getInboxTs(),
                greaterThanOrEqualTo(expectedDate));

        if (color == Color.BLUE) {
            verify(mbiApiClient).sendMessageToSupplier(
                    Mockito.eq(774L),
                    Mockito.eq(1612872409),
                    Mockito.anyString(),
                    Mockito.argThat(params -> {
                        OrderContextParams orderParams = (OrderContextParams) params;
                        return orderParams.getOrderId() == 1
                                && orderParams.getOrderStatusId() == OrderStatus.PENDING.getId();
                    })
            );
        } else if (color == Color.WHITE) {
            verify(mbiApiClient).sendMessageToShop(
                    Mockito.eq(774L),
                    Mockito.eq(1612872409),
                    Mockito.anyString()
            );
        }
    }

    private boolean nonNullSendAfter(Notification n) {
        return n.getDeliveryChannels().stream().anyMatch(dc -> dc.getSendAfterTs() != null);
    }

    private boolean containsDeliveryChannelType(Notification n, ChannelType type) {
        return n.getDeliveryChannels().stream().anyMatch(dc -> dc.getType() == type);
    }

    @Test
    public void scheduledShopNotificationIsDeleted() {
        Order order = OrderProvider.getBlueOrder();
        order.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        OrderHistoryEvent event = EventsProvider.orderStatusUpdated(order, OrderStatus.RESERVED, OrderStatus.PENDING);
        when(checkouterClient.shops().getSchedule(anyLong()))
                .thenReturn(MOCK_SCHEDULE);

        eventTestUtils.mockEvent(event);
        assertThat(inboxService.getDeliveryStatisticsFull(), is(empty()));
        eventTestUtils.runImport();

        event = EventsProvider.orderStatusUpdated(order, OrderStatus.PENDING, OrderStatus.PROCESSING);
        eventTestUtils.mockEvent(event);
        eventTestUtils.runImport();

        EvictionSearch es = new EvictionSearch(NotificationStatus.DELETED, Collections.singletonList(order.getId()));

        List<Notification> mbiNotifications = inboxService.evictionSearch(es)
                .stream()
                .filter(n -> n.getType()
                        .equals(NOTIFY_SHOP_PI_ORDER_STATUS_PENDING))
                .filter(this::nonNullSendAfter)
                .filter(n -> containsDeliveryChannelType(n, ChannelType.MBI_API))
                .collect(Collectors.toList());
        assertThat(mbiNotifications, hasSize(1));
    }

    private Order createDSBSOrder(long id, LocalDate shipmentDate, Date deliveryDate) {
        return createDSBSOrder(id, shipmentDate, deliveryDate, null);
    }

    private Order createDSBSOrder(long id, LocalDate shipmentDate, Date deliveryDate, OrderStatus status) {
        Order order = OrderProvider.getColorOrder(Color.WHITE);
        order.setId(id);
        order.setStatus(status);
        order.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        order.setDelivery(DeliveryProvider.getShopDelivery());

        Parcel parcel = new Parcel();
        parcel.setShipmentDate(shipmentDate);
        order.getDelivery().setParcels(Collections.singletonList(parcel));

        order.getDelivery().setDeliveryDates(new DeliveryDates(deliveryDate, deliveryDate));

        return order;
    }

    @Test
    public void avoidingPushToShopForDsbsCancellationByShop() throws Exception {
        var order = createDSBSOrder(123L, LocalDate.now(),
                new GregorianCalendar(2021, Calendar.DECEMBER, 31).getTime(), OrderStatus.PROCESSING);
        order.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        order.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        var event = EventsProvider.orderStatusUpdated(order,
                OrderStatus.PROCESSING,
                OrderStatus.CANCELLED,
                OrderSubstatus.USER_CHANGED_MIND,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID));

        eventTestUtils.mockEvent(event);
        eventTestUtils.runImport();

        var notifications = eventTestUtils.getAllNotifications();
        Assertions.assertNotNull(notifications);
        Assertions.assertEquals(0, notifications
                .stream()
                .flatMap(n -> n.getDeliveryChannels().stream())
                .filter(dc -> dc.getType() == ChannelType.PUSH)
                .count());
    }

    @Test
    public void testSendingPushToShopForDsbsCancellationByUser() throws Exception {
        var order = createDSBSOrder(123L, LocalDate.now(),
                new GregorianCalendar(2021, Calendar.DECEMBER, 31).getTime(), OrderStatus.PROCESSING);
        order.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        var event = EventsProvider.orderStatusUpdated(order,
                OrderStatus.PROCESSING,
                OrderStatus.CANCELLED,
                OrderSubstatus.USER_CHANGED_MIND,
                new ClientInfo(USER, order.getBuyer().getUid()));

        eventTestUtils.mockEvent(event);
        eventTestUtils.runImport();

        var notifications = eventTestUtils.getAllNotifications();
        Assertions.assertNotNull(notifications);
        Assertions.assertEquals(1, notifications
                .stream()
                .flatMap(n -> n.getDeliveryChannels().stream())
                .filter(dc -> dc.getType() == ChannelType.PUSH)
                .count());
    }

    @Test
    public void testDeliveredPushIsSentForOrderWithoutCashbackEmit() throws Exception {
        notifierProperties.setMergeDeliveredAndCashbackPush(true);
        var orderHistoryEvent = EventsProvider.orderStatusUpdated(
                OrderStatus.DELIVERY,
                OrderStatus.DELIVERED,
                OrderSubstatus.DELIVERY_SERVICE_DELIVERED,
                new ClientInfo(USER, 12345L),
                order -> {
                }
        );
        notifierProperties.setMergeDeliveredAndCashbackPush(false);
        var processEventResult = blueEventProcessor.process(orderHistoryEvent, Set.of());
        assertThat(processEventResult, is(ProcessEventResult.ok()));
        var pushNotification = eventTestUtils.getSingleNotification(MOBILE_PUSH);
        var emailNotification = eventTestUtils.getSingleEmailNotification();
        assertThat(pushNotification.getType(), is("blue.market.checkout.order.status.delivered"));
        assertThat(emailNotification.getType(), is("blue.market.checkout.order.status.delivered"));
    }

    @Test
    public void testDeliveredPushAndEmailAreNotSentForOrderWithCashbackEmit() throws Exception {
        notifierProperties.setMergeDeliveredAndCashbackPush(true);
        var orderHistoryEvent = EventsProvider.orderStatusUpdated(
                OrderStatus.DELIVERY,
                OrderStatus.DELIVERED,
                OrderSubstatus.DELIVERY_SERVICE_DELIVERED,
                new ClientInfo(USER, 12345L),
                order -> order.getItems().forEach(oi -> oi.addOrReplacePromo(cashbackPromo(ONE, TEN,
                        DEFAULT_CASHBACK_PROMO_KEY)))
        );
        notifierProperties.setMergeDeliveredAndCashbackPush(true);
        var processEventResult = blueEventProcessor.process(orderHistoryEvent, Set.of());
        assertThat(processEventResult, is(ProcessEventResult.ok()));
        var pushNotification = eventTestUtils.getSingleNotification(MOBILE_PUSH);
        var emailNotification = eventTestUtils.getSingleEmailNotification();
        assertThat(pushNotification, is(nullValue()));
        assertThat(emailNotification, is(nullValue()));
    }

    @Test
    public void testCashbackEmitPushIsSent() throws Exception {
        notifierProperties.setMergeDeliveredAndCashbackPush(true);
        var orderHistoryEvent = EventsProvider.orderCashbackEmissionCleared(new ClientInfo(USER, 12345L));
        var processEventResult = blueEventProcessor.process(orderHistoryEvent, Set.of());
        assertThat(processEventResult, is(ProcessEventResult.ok()));
        var mobilePushNotification = eventTestUtils.getSingleNotification(MOBILE_PUSH);
        var notificationEventSource = mobilePushMessageSerializer.deserialize(
                mobilePushNotification.getData().getBytes());
        var emailNotification = eventTestUtils.getSingleEmailNotification();
        assertThat(Iterables.getOnlyElement(mobilePushNotification.getDeliveryChannels()).getType(), is(MOBILE_PUSH));
        assertThat(notificationEventSource.getNotificationSubtype(), is(PUSH_YAPLUS_DELIVERED_CASH_BACK));
        assertThat(emailNotification.getType(), is("blue.market.checkout.order.status.delivered"));
    }

    @Test
    public void testCashbackEmitPushIsNotSentWhenTheToggleIsOff() {
        var orderHistoryEvent = EventsProvider.orderCashbackEmissionCleared(new ClientInfo(USER, 12345L));
        var processEventResult = blueEventProcessor.process(orderHistoryEvent, Set.of());
        assertThat(processEventResult, is(ProcessEventResult.skipped()));
    }

    @Test
    @DisplayName("Отправлять уведомления по PushApi и MbiApi при получении ивента ORDER_CHANGE_REQUEST_CREATED" +
            " с типом CANCELLATION созданным пользователем по DSBS заказу в статусе DELIVERY или PICKUP")
    public void createPushApiAndMbiApiNotificationsWhenCancellationRequestCreated() {
        var order = createDSBSOrder(123L, LocalDate.now(),
                new GregorianCalendar(2021, Calendar.DECEMBER, 31).getTime());
        order.setStatus(OrderStatus.DELIVERY);
        order.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        var event = EventsProvider.dsbsOrderChangeRequestCreated(
                new ClientInfo(ClientRole.USER, 123L),
                order);

        when(checkouterClient.getChangeRequestsByEventId(Mockito.eq(order.getId()), Mockito.eq(event.getId()),
                Mockito.any()))
                .thenReturn(List.of(new ChangeRequest(
                        1L,
                        order.getId(),
                        new CancellationRequestPayload(
                                OrderSubstatus.USER_CHANGED_MIND,
                                null,
                                null,
                                null),
                        ChangeRequestStatus.NEW,
                        Instant.now(),
                        null,
                        USER)));

        eventTestUtils.mockEvent(event);
        eventTestUtils.runImport();

        var notifications = eventTestUtils.getAllNotifications();
        assertNotNull(notifications);
        assertThat(notifications, hasSize(2));
        Assertions.assertNotNull(notifications
                .stream()
                .filter(n -> n.getType().equals(AbstractEventProcessor.PUSH_API_ORDER_CANCELLATION_CREATED_BY_USER_TYPE)
                        && n.getDeliveryChannels().stream().anyMatch(dc -> dc.getType() == ChannelType.PUSH))
                .findAny()
                .orElse(null));
        Assertions.assertNotNull(notifications
                .stream()
                .filter(n -> n.getType().equals(AbstractEventProcessor.MBI_ORDER_CANCELLATION_CREATED_BY_USER_TYPE)
                        && n.getDeliveryChannels().stream().anyMatch(dc -> dc.getType() == ChannelType.MBI))
                .findAny()
                .orElse(null));
    }

    @Test
    @DisplayName("Не отправлять уведомления по PushApi и MbiApi при получении ивента ORDER_CHANGE_REQUEST_CREATED" +
            " с типом CANCELLATION созданным не пользователем по DSBS заказу в статусе DELIVERY или PICKUP")
    public void avoidCreatePushApiAndMbiApiNotificationsWhenCancellationRequestCreated() {
        var order = createDSBSOrder(123L, LocalDate.now(),
                new GregorianCalendar(2021, Calendar.DECEMBER, 31).getTime());
        order.setStatus(OrderStatus.DELIVERY);
        order.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        var event = EventsProvider.dsbsOrderChangeRequestCreated(
                new ClientInfo(ClientRole.SHOP, 123L),
                order);

        when(checkouterClient.getChangeRequestsByEventId(Mockito.eq(order.getId()), Mockito.eq(event.getId()),
                Mockito.any()))
                .thenReturn(List.of(new ChangeRequest(
                        1L,
                        order.getId(),
                        new CancellationRequestPayload(
                                OrderSubstatus.USER_CHANGED_MIND,
                                null,
                                null,
                                null),
                        ChangeRequestStatus.NEW,
                        Instant.now(),
                        null,
                        USER)));

        eventTestUtils.mockEvent(event);
        eventTestUtils.runImport();

        var notifications = eventTestUtils.getAllNotifications();
        assertNotNull(notifications);
        assertThat(notifications, hasSize(0));
    }

    @Test
    @DisplayName("Не отправлять уведомления по PushApi и MbiApi при получении ивента ORDER_CHANGE_REQUEST_CREATED" +
            " с типом не CANCELLATION созданным пользователем по DSBS заказу в статусе DELIVERY или PICKUP")
    public void avoidCreatePushApiAndMbiApiNotificationsWhenNotCancellationRequestCreated() {
        var order = createDSBSOrder(123L, LocalDate.now(),
                new GregorianCalendar(2021, Calendar.DECEMBER, 31).getTime());
        order.setStatus(OrderStatus.DELIVERY);
        order.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        var event = EventsProvider.dsbsOrderChangeRequestCreated(
                new ClientInfo(ClientRole.USER, 123L),
                order);

        when(checkouterClient.getChangeRequestsByEventId(Mockito.eq(order.getId()), Mockito.eq(event.getId()),
                Mockito.any()))
                .thenReturn(List.of(new ChangeRequest(
                        1L,
                        order.getId(),
                        new DeliveryDatesChangeRequestPayload(),
                        ChangeRequestStatus.NEW,
                        Instant.now(),
                        null,
                        USER)));

        eventTestUtils.mockEvent(event);
        eventTestUtils.runImport();

        var notifications = eventTestUtils.getAllNotifications();
        assertNotNull(notifications);
        assertThat(notifications, hasSize(0));
    }

    private long getPushToShopNotificationCount(List<DeliveryStatisticsFull> deliveryStatisticsFull, ChannelType push) {
        return deliveryStatisticsFull.stream().filter(s -> s.getChannelType() == push).count();
    }

    private void processEvent(OrderHistoryEvent statusUpdatedEvent, ClientRole role) {
        statusUpdatedEvent.setAuthor(new ClientInfo(role, 0L));
        eventTestUtils.mockEvent(statusUpdatedEvent);
        eventTestUtils.runImport();
    }

    private void assertMailData(String data, String expectedXmlPath) {
        try (InputStream inputStream = ShopsOutletSaxHandlerTest.class.getResourceAsStream(
                expectedXmlPath)) {
            assertThat(data, isIdenticalTo(Input.fromStream(inputStream)).ignoreWhitespace());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private OrderHistoryEvent createNewDbsOrderEvent() {
        Order orderAfter = OrderProvider.getColorOrder(Color.WHITE);
        orderAfter.setId(321L);
        orderAfter.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        orderAfter.setStatus(OrderStatus.PENDING);
        orderAfter.setSubstatus(OrderSubstatus.AWAIT_CONFIRMATION);
        orderAfter.setPaymentType(PaymentType.POSTPAID);
        orderAfter.setItems(List.of(OrderItemProvider.buildOrderItem("1231", 1)));

        OrderHistoryEvent substatusUpdateEvent = NotifierTestUtils.generateOrderHistoryEvent(1L, OrderStatus.PENDING,
                OrderSubstatus.AWAIT_CONFIRMATION);
        substatusUpdateEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        substatusUpdateEvent.setAuthor(ClientInfo.SYSTEM);
        substatusUpdateEvent.setTranDate(Date.from(LocalDateTime.of(2021, Month.JUNE, 18, 0, 0)
                .atZone(ZoneId.systemDefault()).toInstant()));
        substatusUpdateEvent.setOrderAfter(orderAfter);
        return substatusUpdateEvent;
    }
}
