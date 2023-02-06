package ru.yandex.market.notifier.test.integration;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.DeliveryChannel;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.entity.NotificationStatus;
import ru.yandex.market.notifier.storage.InboxDao;
import ru.yandex.market.notifier.util.EventTestUtils;
import ru.yandex.market.notifier.util.PersNotifyVerifier;
import ru.yandex.market.notifier.util.providers.OrderProvider;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.PersNotifyClientException;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.notifier.util.NotifierTestUtils.DEFAULT_ORDER_CREATION_DATE;
import static ru.yandex.market.notifier.util.providers.EventsProvider.makeOrderForMock;
import static ru.yandex.market.notifier.util.providers.EventsProvider.orderStatusUpdated;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.BLUE_ORDER_PROCESSING;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.PUSH_STORE_PREPAID_DELAY;

public class BlueOrderStatusUnpaidUpdateTest extends AbstractServicesTestBase {

    @Autowired
    private EventTestUtils eventTestUtils;
    @Autowired
    private InboxDao inboxDao;
    @Autowired
    private PersNotifyClient persNotifyClient;
    @Autowired
    private PersNotifyVerifier persNotifyVerifier;

    @Test
    public void shouldNotSendEmailForFakeOrder() {
        OrderHistoryEvent event = orderStatusUpdated(
                OrderStatus.RESERVED,
                OrderStatus.UNPAID,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                order -> {
                    order.setRgb(Color.BLUE);
                    order.setFake(true);
                }
        );
        when(checkouterClient.getOrder(event.getOrderAfter().getId(), ClientRole.SYSTEM, 1L))
                .thenReturn(makeOrderForMock(event, true));

        eventTestUtils.mockEvents(singletonList(event));
        eventTestUtils.assertHasNewNotifications(0);
        verify(checkouterClient)
                .getOrder(event.getOrderAfter().getId(), ClientRole.SYSTEM, 1L);
    }

    @Test
    public void shouldScheduleEmailAndPushForOrderForBuyerWithoutUuid() throws PersNotifyClientException {
        OrderHistoryEvent event = orderStatusUpdated(
                OrderStatus.RESERVED,
                OrderStatus.UNPAID,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                order -> {
                    order.setRgb(Color.BLUE);
                    order.setFake(false);
                    order.getBuyer().setUuid(null);
                }
        );
        deliverAndCheckEmailAndPush(event);
    }

    @Test
    public void shouldScheduleEmailAndPushForOrderForBuyerWithUuid() throws PersNotifyClientException {
        OrderHistoryEvent event = orderStatusUpdated(
                OrderStatus.RESERVED,
                OrderStatus.UNPAID,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                order -> {
                    order.setRgb(Color.BLUE);
                    order.setFake(false);
                }
        );
        deliverAndCheckEmailAndPush(event);
    }

    private void deliverAndCheckEmailAndPush(OrderHistoryEvent event) throws PersNotifyClientException {
        when(checkouterClient.getOrder(event.getOrderAfter().getId(), ClientRole.SYSTEM, 1L))
                .thenReturn(makeOrderForMock(event, true));

        eventTestUtils.mockEvents(singletonList(event));

        eventTestUtils.runImport();
        List<Notification> allNotifications = inboxDao.getAllNotifications();
        assertThat(allNotifications, hasSize(2));

        Notification emailNotification = allNotifications.get(0);
        assertThat(emailNotification.getDeliveryChannels(), hasSize(1));
        DeliveryChannel emailDeliveryChannel =
                CollectionUtils.expectedSingleResult(emailNotification.getDeliveryChannels());
        assertEquals(ChannelType.EMAIL, emailDeliveryChannel.getType());
        assertThat(
                emailDeliveryChannel.getSendAfterTs().toInstant().truncatedTo(ChronoUnit.MINUTES),
                is(DEFAULT_ORDER_CREATION_DATE.plus(16, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES))
        );

        Notification mobilePushNotification = allNotifications.get(1);
        assertThat(mobilePushNotification.getDeliveryChannels(), hasSize(1));
        DeliveryChannel mobilePushDeliveryChannel =
                CollectionUtils.expectedSingleResult(mobilePushNotification.getDeliveryChannels());
        assertEquals(ChannelType.MOBILE_PUSH, mobilePushDeliveryChannel.getType());
        assertThat(
                mobilePushDeliveryChannel.getSendAfterTs().toInstant().truncatedTo(ChronoUnit.MINUTES),
                is(DEFAULT_ORDER_CREATION_DATE.plus(16, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES))
        );

        verify(checkouterClient)
                .getOrder(event.getOrderAfter().getId(), ClientRole.SYSTEM, 1L);
        eventTestUtils.deliverNotifications();
        persNotifyVerifier.verifyMobilePushSent();

        InOrder inOrder = inOrder(persNotifyClient);
        inOrder.verify(persNotifyClient)
                .createEvent(
                        MockitoHamcrest.argThat(allOf(instanceOf(NotificationEventSource.class),
                                        hasProperty("email", is(event.getOrderAfter().getBuyer().getEmail())),
                                        hasProperty("sendTime", is(emailDeliveryChannel.getSendAfterTs()))
                                )
                        )
                );
        verify(persNotifyClient, Mockito.atLeastOnce())
                .createEvent(
                        MockitoHamcrest.argThat(allOf(instanceOf(NotificationEventSource.class),
                                        hasProperty("notificationSubtype", is(PUSH_STORE_PREPAID_DELAY)),
                                        hasProperty("uid", is(event.getOrderAfter().getBuyer().getUid())),
                                        hasProperty("uuid", is(event.getOrderAfter().getBuyer().getUuid()))
                                )
                        )
                );
    }

    @Test
    public void shouldRemoveScheduledEmailForOrderWhenPaid() throws PersNotifyClientException {
        // 1. Создаем событие перехода в UNPAID
        OrderHistoryEvent event = orderStatusUpdated(
                OrderStatus.RESERVED,
                OrderStatus.UNPAID,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                order -> {
                    order.setRgb(Color.BLUE);
                    order.setFake(false);
                    order.setFulfilment(true);
                }
        );
        when(checkouterClient.getOrder(event.getOrderAfter().getId(), ClientRole.SYSTEM, 1L))
                .thenReturn(makeOrderForMock(event, true));

        eventTestUtils.mockEvents(singletonList(event));

        eventTestUtils.runImport();

        // 2. Проверяем наличие события перехода в UNPAID
        List<Notification> allNotifications = inboxDao.getAllNotifications();
        assertThat(allNotifications, hasSize(2));

        Notification notification = allNotifications.get(0);
        assertThat(notification.getDeliveryChannels(), hasSize(1));
        DeliveryChannel emailDeliveryChannel = CollectionUtils.expectedSingleResult(notification.getDeliveryChannels());
        assertEquals(ChannelType.EMAIL, emailDeliveryChannel.getType());
        assertThat(
                emailDeliveryChannel.getSendAfterTs().toInstant().truncatedTo(ChronoUnit.MINUTES),
                is(DEFAULT_ORDER_CREATION_DATE.plus(16, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES))
        );
        verify(checkouterClient)
                .getOrder(event.getOrderAfter().getId(), ClientRole.SYSTEM, 1L);

        // 3. Создаем событие перехода в PROCESSING
        event = orderStatusUpdated(
                OrderStatus.UNPAID,
                OrderStatus.PROCESSING,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                order -> {
                    order.setRgb(Color.BLUE);
                    order.setFake(false);
                    order.setFulfilment(true);
                }
        );
        when(checkouterClient.getOrder(event.getOrderAfter().getId(), ClientRole.SYSTEM, 1L))
                .thenReturn(makeOrderForMock(event, true));

        eventTestUtils.mockEvents(singletonList(event));

        // 4. Проверяем, что событие перехода в UNPAID помечено как удаленное
        eventTestUtils.runImport();
        allNotifications = inboxDao.getAllNotifications();
        assertThat(allNotifications, hasSize(4));
        List<DeliveryChannel> deletedChannels = allNotifications.stream()
                .flatMap(n -> n.getDeliveryChannels().stream())
                .filter(dc -> dc.getStatus() == NotificationStatus.DELETED)
                .collect(Collectors.toList());
        assertThat(deletedChannels, hasSize(2));
        Set<Long> deletedIds = deletedChannels
                .stream()
                .map(DeliveryChannel::getId)
                .collect(Collectors.toSet());
        assertThat(emailDeliveryChannel.getId(), in(deletedIds));

        // 5. Вызываем доставку событий и проверяем, что отправилось только BLUE_ORDER_PROCESSING
        eventTestUtils.deliverNotifications();
        verify(persNotifyClient, never())
                .createEvent(
                        MockitoHamcrest.argThat(allOf(instanceOf(NotificationEventSource.class),
                                        hasProperty("email", is(event.getOrderAfter().getBuyer().getEmail())),
                                        hasProperty("sendTime", is(emailDeliveryChannel.getSendAfterTs()))
                                )
                        )
                );
        verify(persNotifyClient, times(1))
                .createEvent(
                        MockitoHamcrest.argThat(allOf(instanceOf(NotificationEventSource.class),
                                        hasProperty("email", is(event.getOrderAfter().getBuyer().getEmail())),
                                        hasProperty("notificationSubtype", is(BLUE_ORDER_PROCESSING))
                                )
                        )
                );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void checkSendNotificationForUnpaidOrderToShop(boolean sendUnpaidToShop) {
        notifierProperties.setSkipSendingUnpaidStatusToShop(!sendUnpaidToShop);
        OrderHistoryEvent event = orderStatusUpdated(
                OrderStatus.RESERVED,
                OrderStatus.UNPAID,
                new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID),
                order -> {
                    order.setRgb(Color.BLUE);
                    order.setFake(false);
                    order.setFulfilment(Boolean.FALSE);
                    order.setAcceptMethod(OrderAcceptMethod.PUSH_API);
                }
        );
        when(checkouterClient.getOrder(event.getOrderAfter().getId(), ClientRole.SYSTEM, 1L))
                .thenReturn(makeOrderForMock(event, true));

        eventTestUtils.mockEvents(singletonList(event));
        eventTestUtils.assertHasNewNotifications(sendUnpaidToShop ? 2 : 1);
        verify(checkouterClient).getOrder(event.getOrderAfter().getId(), ClientRole.SYSTEM, 1L);
    }
}
