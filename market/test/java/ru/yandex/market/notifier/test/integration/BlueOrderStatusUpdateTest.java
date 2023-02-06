package ru.yandex.market.notifier.test.integration;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.OrderTypeUtils;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.util.EventTestUtils;
import ru.yandex.market.notifier.util.PersNotifyVerifier;
import ru.yandex.market.notifier.util.providers.OrderProvider;
import ru.yandex.market.pers.notify.PersNotifyClientException;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PENDING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PICKUP;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PLACING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.RESERVED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.UNKNOWN;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.ASYNC_PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.AWAIT_CONFIRMATION;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.PREORDER;
import static ru.yandex.market.notifier.util.providers.EventsProvider.orderStatusUpdated;

public class BlueOrderStatusUpdateTest extends AbstractServicesTestBase {

    @Autowired
    private EventTestUtils eventTestUtils;
    @Autowired
    private PersNotifyVerifier persNotifyVerifier;
    @Autowired
    private TestableClock testableClock;

    private Instant testTime;

    @BeforeEach
    public void setUp() {
        testTime = ZonedDateTime.of(2020, 9, 29, 18, 32, 0, 0, ZoneId.systemDefault())
                .toInstant();

        testableClock.setFixed(testTime, ZoneId.systemDefault());
    }

    public static Stream<Arguments> parameterizedTestData() {

        return Stream.concat(
                Stream.of(
                        new Object[]{"PENDING", PENDING, null, true, false, false},
                        new Object[]{"PENDING.ASYNC", PENDING, ASYNC_PROCESSING, true, false, false},
                        new Object[]{"PENDING.AWAIT_CONFIRMATION", PENDING, AWAIT_CONFIRMATION, true, true, false},
                        new Object[]{"PENDING.PREORDER", PENDING, PREORDER, true, true, false},
                        new Object[]{"PROCESSING", PROCESSING, null, true, false, true},
                        new Object[]{"DELIVERY", DELIVERY, null, false, false, true},
                        new Object[]{"PICKUP", PICKUP, null, false, false, true},
                        new Object[]{"DELIVERED", DELIVERED, null, true, false, true},
                        new Object[]{"PLACING", PLACING, null, false, false, false},
                        new Object[]{"RESERVED", RESERVED, null, false, false, false},
                        new Object[]{"UNKNOWN", UNKNOWN, null, false, false, false}
                ),
                Arrays.stream(OrderSubstatus.values())
                        .filter(os -> os.getStatus() == CANCELLED)
                        .filter(oss -> oss != OrderSubstatus.USER_REFUSED_TO_PROVIDE_PERSONAL_DATA)
                        .filter(oss -> oss != OrderSubstatus.RESERVATION_FAILED)
                        .map(substatus -> new Object[]{
                                CANCELLED.name() + " - " + substatus.name(),
                                CANCELLED,
                                substatus,
                                false,
                                false,
                                true
                        })
        ).collect(toList()).stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldSendEmailAndPushForStatusWithoutBuyerUuid(String caseName, OrderStatus orderAfterStatus,
                                                                OrderSubstatus orderAfterSubstatus,
                                                                boolean shouldSendEmail, boolean shouldSendPush,
                                                                boolean shouldNotifyShop) throws PersNotifyClientException {
        OrderHistoryEvent event = orderStatusUpdated(
                PENDING,
                orderAfterStatus,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                order -> {
                    order.setRgb(Color.BLUE);
                    order.setFulfilment(true);
                    order.getBuyer().setUuid(null);
                    order.getItems().forEach(item -> item.setSupplierDescription(""));
                }
        );
        deliverAndCheckNotifications(orderAfterStatus, orderAfterSubstatus, shouldSendEmail, shouldSendPush,
                shouldNotifyShop, event);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldSendEmailAndPushWhenHasInvalidXmlCharacters(String caseName, OrderStatus orderAfterStatus,
                                                                  OrderSubstatus orderAfterSubstatus,
                                                                  boolean shouldSendEmail, boolean shouldSendPush,
                                                                  boolean shouldNotifyShop) throws PersNotifyClientException {
        OrderHistoryEvent event = orderStatusUpdated(
                PENDING,
                orderAfterStatus,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                order -> {
                    order.setRgb(Color.BLUE);
                    order.setFulfilment(true);
                    order.getItems().forEach(item -> item.setSupplierDescription(
                            "Строка с невалидными символами \u000B  \u000B2 \u000B4"
                    ));
                }
        );
        deliverAndCheckNotifications(orderAfterStatus, orderAfterSubstatus, shouldSendEmail, shouldSendPush,
                shouldNotifyShop, event);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldSendEmailAndPushForStatus(String caseName, OrderStatus orderAfterStatus,
                                                OrderSubstatus orderAfterSubstatus, boolean shouldSendEmail,
                                                boolean shouldSendPush, boolean shouldNotifyShop) throws PersNotifyClientException {
        OrderHistoryEvent event = orderStatusUpdated(
                PENDING,
                orderAfterStatus,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                order -> {
                    order.setRgb(Color.BLUE);
                    order.setFulfilment(true);
                }
        );
        deliverAndCheckNotifications(orderAfterStatus, orderAfterSubstatus, shouldSendEmail, shouldSendPush,
                shouldNotifyShop, event);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldNotSendSmsForFake(String caseName, OrderStatus orderAfterStatus,
                                        OrderSubstatus orderAfterSubstatus, boolean shouldSendEmail,
                                        boolean shouldSendPush, boolean shouldNotifyShop) throws PersNotifyClientException {
        OrderHistoryEvent event = orderStatusUpdated(
                PENDING,
                orderAfterStatus,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                order -> {
                    order.setRgb(Color.BLUE);
                    order.setFulfilment(true);
                    order.setFake(true);
                }
        );
        deliverAndCheckNotifications(orderAfterStatus, orderAfterSubstatus, shouldSendEmail, shouldSendPush,
                shouldNotifyShop, event);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldSendSmsForNotFulfilmentShopDelivery(String caseName, OrderStatus orderAfterStatus,
                                                          OrderSubstatus orderAfterSubstatus, boolean shouldSendEmail,
                                                          boolean shouldSendPush, boolean shouldNotifyShop)
            throws PersNotifyClientException {
        OrderHistoryEvent event = orderStatusUpdated(
                PENDING,
                orderAfterStatus,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                order -> {
                    order.setRgb(Color.BLUE);
                    order.setFulfilment(false);
                    order.setFake(false);
                    order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
                    order.setAcceptMethod(OrderAcceptMethod.PUSH_API);
                }
        );
        deliverAndCheckNotifications(orderAfterStatus, orderAfterSubstatus, shouldSendEmail, shouldSendPush,
                shouldNotifyShop, event);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldSendSmsForNotFulfilmentMarDo(String caseName, OrderStatus orderAfterStatus,
                                                   OrderSubstatus orderAfterSubstatus, boolean shouldSendEmail,
                                                   boolean shouldSendPush, boolean shouldNotifyShop) throws PersNotifyClientException {
        OrderHistoryEvent event = orderStatusUpdated(
                PENDING,
                orderAfterStatus,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                order -> {
                    order.setRgb(Color.BLUE);
                    order.setFulfilment(false);
                    order.setFake(false);
                    order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
                    order.setAcceptMethod(OrderAcceptMethod.PUSH_API);
                }
        );
        deliverAndCheckNotifications(orderAfterStatus, orderAfterSubstatus, shouldSendEmail, shouldSendPush,
                shouldNotifyShop, event);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldNotSendSmsForFakeContext(String caseName, OrderStatus orderAfterStatus,
                                               OrderSubstatus orderAfterSubstatus, boolean shouldSendEmail,
                                               boolean shouldSendPush, boolean shouldNotifyShop) throws PersNotifyClientException {
        OrderHistoryEvent event = orderStatusUpdated(
                PENDING,
                orderAfterStatus,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                order -> {
                    order.setRgb(Color.BLUE);
                    order.setFulfilment(true);
                    order.setContext(Context.SANDBOX);
                }
        );
        deliverAndCheckNotifications(orderAfterStatus, orderAfterSubstatus, shouldSendEmail, shouldSendPush,
                shouldNotifyShop, event);
    }

    @Test
    public void shouldNotSendPushForPickupOutlet() {
        OrderHistoryEvent event = orderStatusUpdated(
                DELIVERY,
                PICKUP,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                order -> {
                    order.setRgb(Color.BLUE);
                    Delivery delivery = order.getDelivery();
                    delivery.setBuyerAddress(null);
                    delivery.setShopAddress(null);

                    ShopOutlet outlet = new ShopOutlet();
                    outlet.setCity("Москва");
                    outlet.setStreet("Тверская");
                    outlet.setHouse("10");
                    outlet.setBlock("3");
                    delivery.setOutlet(outlet);
                }
        );

        eventTestUtils.mockEvents(singletonList(event));
        eventTestUtils.runImport();
        eventTestUtils.deliverNotifications();
        assertMobilePushNotSent();
    }

    @Test
    public void shouldNotSendSmsForBeruPickupOutlet() {
        OrderHistoryEvent event = orderStatusUpdated(
                DELIVERY,
                PICKUP,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                order -> {
                    order.setRgb(Color.BLUE);
                    Delivery delivery = order.getDelivery();
                    delivery.setBuyerAddress(null);
                    delivery.setShopAddress(null);
                    delivery.setDeliveryServiceId(198L);

                    ShopOutlet outlet = new ShopOutlet();
                    outlet.setCity("Москва");
                    outlet.setStreet("Тверская");
                    outlet.setHouse("10");
                    outlet.setBlock("3");
                    delivery.setOutlet(outlet);
                }
        );

        eventTestUtils.mockEvents(singletonList(event));
        eventTestUtils.runImport();
    }

    @Test
    public void shouldNotSendEmailIfTracksIsNull() {
        OrderHistoryEvent event = orderStatusUpdated(
                PROCESSING,
                DELIVERY,
                ClientInfo.SYSTEM,
                order -> {
                    order.setRgb(Color.BLUE);

                    Parcel parcel = new Parcel();
                    parcel.setTracks(null);

                    Delivery delivery = order.getDelivery();
                    delivery.setParcels(List.of(parcel));
                    delivery.setDeliveryServiceId(19L);
                }
        );

        eventTestUtils.mockEvent(event);
        eventTestUtils.runImport();

        List<Notification> notifications = eventTestUtils.getAllNotifications();
        assertTrue(notifications.isEmpty());
    }

    private void deliverAndCheckNotifications(OrderStatus orderAfterStatus, OrderSubstatus orderAfterSubstatus,
                                              boolean shouldSendEmail, boolean shouldSendPush,
                                              boolean shouldNotifyShop, OrderHistoryEvent event) throws PersNotifyClientException {
        event.getOrderAfter().setSubstatus(orderAfterSubstatus);

        eventTestUtils.mockEvents(singletonList(event));

        boolean preorder = event.getOrderAfter().isPreorder();

        int totalNotifications = 0;
        if (shouldSendEmail) {
            totalNotifications++;
        }
        if (shouldNotifyShop && Boolean.FALSE.equals(event.getOrderAfter().isFulfilment())) {
            totalNotifications++;
        }
        eventTestUtils.assertHasNewNotifications(totalNotifications);
        if (shouldSendEmail) {
            eventTestUtils.assertEmailWasSent(event);
        }

        if (shouldSendPush && !preorder && (orderAfterStatus != DELIVERY || OrderTypeUtils.isMarketDelivery(event.getOrderAfter()))) {
            assertMobilePushSent();
        }
    }

    private void assertMobilePushSent() {
        try {
            persNotifyVerifier.verifyMobilePushSent(times(1));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void assertMobilePushNotSent() {
        try {
            persNotifyVerifier.verifyMobilePushSent(times(0));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
