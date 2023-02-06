package ru.yandex.market.notifier.test.integration;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.checkout.checkouter.client.CheckouterReturnClient;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnDelivery;
import ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.DeliveryChannel;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.jobs.zk.processors.BlueEventProcessor;
import ru.yandex.market.notifier.senders.MbiMessageSender;
import ru.yandex.market.notifier.storage.InboxDao;
import ru.yandex.market.notifier.util.providers.ReturnProvider;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.notifier.util.providers.EventsProvider.getOrderReturnCreatedEvent;
import static ru.yandex.market.notifier.util.providers.EventsProvider.getOrderReturnDeliveryStatusUpdatedEvent;
import static ru.yandex.market.notifier.util.providers.EventsProvider.getWhiteOrderReturnCreated;

class MbiApiNotificationTest extends AbstractServicesTestBase {

    @Mock
    private CheckouterReturnClient checkouterReturnClient;
    @Autowired
    private InboxDao inboxDao;
    @MockBean
    private MbiApiClient mbiApiClient;
    @MockBean
    private MbiMessageSender mbiMessageSender;

    @Test
    void shouldNotifySupplierOnReturnCreateEvent() {
        Long returnId = 7777L;
        OrderHistoryEvent event = getOrderReturnCreatedEvent(returnId);
        Order order = event.getOrderAfter();
        Return ret = ReturnProvider.makeReturn(order);

        mockCheckouter(returnId, order, ret);
        processEvent(event);

        checkMbiApiNotificationInInbox(ChannelType.MBI_API);
        Mockito.verify(mbiApiClient, times(1)).sendMessageToSupplier(eq(123L), Mockito.anyInt(),
                and(and(Mockito.contains("title=\"OfferName\""), Mockito.contains("sku=\"shop_sku_test\"")),
                        Mockito.contains(ReturnProvider.RETURN_REASON)));
    }

    @Test
    void shouldNotifyShopOnOrderReturnCreated() throws Exception {
        Long returnId = 7777L;
        OrderHistoryEvent event = getWhiteOrderReturnCreated(returnId);
        Order order = event.getOrderAfter();
        Return ret = ReturnProvider.makeReturn(order);

        mockCheckouter(returnId, order, ret);
        processEvent(event);

        checkMbiApiNotificationInInbox(ChannelType.MBI);
        Mockito.verify(mbiMessageSender, times(1))
                .send(eq(BlueEventProcessor.NOTIFY_ORDER_RETURN_CREATED), any(), any());
    }

    @Test
    void shouldNotifyShopOnReturnDeliveryStatusUpdatedReadyForPickup() {
        notifierProperties.setReturnStatusUpdatedNotificationEnabled(true);

        Long returnId = 7777L;
        OrderHistoryEvent event = getOrderReturnDeliveryStatusUpdatedEvent(returnId);
        Order order = event.getOrderAfter();

        Return ret = ReturnProvider.makeReturn(order);
        ReturnDelivery returnDelivery = ret.getDelivery();
        returnDelivery.setType(DeliveryType.POST);
        returnDelivery.setStatus(ReturnDeliveryStatus.READY_FOR_PICKUP);

        mockCheckouter(returnId, order, ret);
        processEvent(event);

        checkMbiApiNotificationInInbox(ChannelType.MBI_API);
        Mockito.verify(mbiApiClient, times(1))
                .sendMessageToSupplier(eq(123L), Mockito.anyInt(), Mockito.anyString());
    }

    @Test
    void shouldNotifyShopOnReturnDeliveryStatusUpdatedSenderSent() {
        notifierProperties.setReturnStatusUpdatedNotificationEnabled(true);

        Long returnId = 7777L;
        OrderHistoryEvent event = getOrderReturnDeliveryStatusUpdatedEvent(returnId);
        Order order = event.getOrderAfter();

        Return ret = ReturnProvider.makeReturn(order);
        ReturnDelivery returnDelivery = ret.getDelivery();
        returnDelivery.setType(DeliveryType.POST);
        returnDelivery.setStatus(ReturnDeliveryStatus.SENDER_SENT);

        mockCheckouter(returnId, order, ret);
        processEvent(event);

        checkMbiApiNotificationInInbox(ChannelType.MBI_API);
        Mockito.verify(mbiApiClient, times(1))
                .sendMessageToSupplier(eq(123L), Mockito.anyInt(), Mockito.anyString());
    }

    @Test
    void shouldNotNotifyShopOnReturnDeliveryStatusUpdated() {
        notifierProperties.setReturnStatusUpdatedNotificationEnabled(true);

        Long returnId = 7777L;
        OrderHistoryEvent event = getOrderReturnDeliveryStatusUpdatedEvent(returnId);
        Order order = event.getOrderAfter();
        Return ret = ReturnProvider.makeReturn(order);
        ReturnDelivery returnDelivery = ret.getDelivery();
        returnDelivery.setType(DeliveryType.POST);
        returnDelivery.setStatus(ReturnDeliveryStatus.CANCELLED);

        mockCheckouter(returnId, order, ret);
        processEvent(event);
        checkZeroNotification(ChannelType.MBI_API);
    }

    @Test
    void shouldFilterItemsForNotifySupplierOnReturnCreateEvent() {
        Long returnId = 7778L;
        OrderHistoryEvent event = getOrderReturnCreatedEvent(returnId);
        Order order = event.getOrderAfter();

        List<ReturnItem> returnItems = new ArrayList<>();
        OrderItem item = order.getItems().stream().findAny().orElseThrow(() -> new RuntimeException("No items"));
        returnItems.add(ReturnProvider.toReturnItemWithReasonAndCount(item, ReturnReasonType.BAD_QUALITY, 1));
        returnItems.add(ReturnProvider.toReturnItemWithReasonAndCount(item, ReturnReasonType.WRONG_ITEM, 1));
        Return ret = ReturnProvider.makeReturn(order.getId(), returnItems);

        mockCheckouter(returnId, order, ret);
        processEvent(event);

        checkMbiApiNotificationInInbox(ChannelType.MBI_API);
        Mockito.verify(mbiApiClient, times(1)).sendMessageToSupplier(eq(123L), Mockito.anyInt(),
                and(
                        and(
                                and(
                                        Mockito.contains("title=\"OfferName\""),
                                        Mockito.contains("sku=\"shop_sku_test\"")
                                ),
                                Mockito.contains(ReturnReasonType.BAD_QUALITY.name())
                        ),
                        not(Mockito.contains(ReturnReasonType.WRONG_ITEM.name()))
                )
        );
    }

    @Test
    void shouldNotNotifySupplierOnReturnWithWrongItemCreateEvent() {
        Long returnId = 7779L;
        OrderHistoryEvent event = getOrderReturnCreatedEvent(returnId);
        Order order = event.getOrderAfter();
        Return ret = ReturnProvider.makeReturnWithReasonType(order, ReturnReasonType.WRONG_ITEM);
        mockCheckouter(returnId, order, ret);

        processEvent(event);

        checkZeroNotification(ChannelType.MBI_API);
    }

    private void processEvent(OrderHistoryEvent event) {
        eventTestUtils.mockEvents(singletonList(event));
        eventTestUtils.runImport();
    }

    private void checkZeroNotification(ChannelType channelType) {
        List<Notification> allNotifications = inboxDao.getAllNotifications();
        Notification notification = getNotificationWithDeliveryChannel(channelType, allNotifications);

        assertThat(notification, nullValue());
        verifyNoInteractions(mbiApiClient);
    }

    private void checkMbiApiNotificationInInbox(ChannelType channelType) {
        List<Notification> allNotifications = inboxDao.getAllNotifications();
        Notification notification = getNotificationWithDeliveryChannel(channelType, allNotifications);

        assertThat(notification, notNullValue());
        assertThat(notification.getDeliveryChannels(), hasSize(1));
        DeliveryChannel deliveryChannel = CollectionUtils.expectedSingleResult(notification.getDeliveryChannels());
        assertThat(deliveryChannel.getType(), equalTo(channelType));
        eventTestUtils.deliverNotifications();
    }

    private void mockCheckouter(Long returnId, Order order, Return ret) {
        when(checkouterClient.getOrder(order.getId(), ClientRole.SYSTEM, 1L)).thenReturn(order);
        when(checkouterClient.returns()).thenReturn(checkouterReturnClient);
        when(checkouterReturnClient.getReturn(
                MockitoHamcrest.argThat(
                        Matchers.allOf(
                                Matchers.hasProperty("clientRole", Matchers.is(ClientRole.SYSTEM)),
                                Matchers.hasProperty("clientId", Matchers.is(1L))
                        )
                ),
                MockitoHamcrest.argThat(
                        Matchers.allOf(
                                Matchers.hasProperty("returnId", Matchers.is(returnId)),
                                Matchers.hasProperty("includeBankDetails", Matchers.is(false))
                        )
                )
        )).thenReturn(ret);
    }
}
