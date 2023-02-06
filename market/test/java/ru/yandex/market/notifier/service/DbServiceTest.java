package ru.yandex.market.notifier.service;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.notifier.ResourceLoadUtil;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.criteria.EvictionSearch;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.DeliveryChannel;
import ru.yandex.market.notifier.entity.DeliveryStatisticsFull;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.entity.NotificationStatus;
import ru.yandex.market.notifier.storage.RegisteredId;
import ru.yandex.market.notifier.util.providers.EventsProvider;
import ru.yandex.market.notifier.util.providers.OrderProvider;
import ru.yandex.market.request.trace.RequestContextHolder;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.notifier.entity.NotifierPropertiesImpl.AGGREGATE_PROCESSING_EMAIL;
import static ru.yandex.market.notifier.service.NotificationDeliveryRetriever.takeNotificationDeliveries;

/**
 * @author kukabara
 */
public class DbServiceTest extends AbstractServicesTestBase {

    private static final Logger log = LoggerFactory.getLogger(DbServiceTest.class);

    @Autowired
    protected InboxService inboxService;

    @Autowired
    protected PersistentIdService idsService;
    @Autowired
    private ResourceLoadUtil resourceLoadUtil;
    @Autowired
    private NotifierPropertiesHolder notifierPropertiesHolder;

    @Test
    public void shouldUpdateProperties() {
        notifierPropertiesHolder.updateValue(AGGREGATE_PROCESSING_EMAIL, "true");
        assertTrue(notifierPropertiesHolder.getValue().getAggregateProcessingEmail());
        notifierPropertiesHolder.updateValue(AGGREGATE_PROCESSING_EMAIL, "false");
        assertFalse(notifierPropertiesHolder.getValue().getAggregateProcessingEmail());
    }

    @Test
    public void testInboxService() throws Exception {
        EvictionSearch es = new EvictionSearch(new Date(System.currentTimeMillis() + 10000), NotificationStatus.NEW,
                100);
        List<Notification> notes = inboxService.evictionSearch(es);
        int size = notes.size() + fillInbox();
        notes = inboxService.evictionSearch(es);
        assertEquals(size, notes.size());
    }

    public int fillInbox() throws IOException {
        List<Notification> notifications = resourceLoadUtil.getSampleNotifications();
        assertFalse(notifications.isEmpty());
        int size = notifications.size();
        log.debug("Load " + size + " samples");
        RequestContextHolder.createNewContext();
        Multiset<Integer> stream2Cnt = HashMultiset.create();
        for (Notification note : notifications) {
            Notification n = inboxService.saveNotification(note);
            n.getDeliveryChannels().forEach(d -> stream2Cnt.add(d.getStream(), 1));
        }

        for (int stream : stream2Cnt.elementSet()) {
            int count = stream2Cnt.count(stream);
            List<Notification> forSending = takeNotificationDeliveries(inboxService, NotificationStatus.NEW, stream);
            assertTrue(count <= forSending.size());
        }

        DeliveryChannel dc = notifications.get(0).getDeliveryChannels().get(0);
        dc.setSendAfterTs(DateUtil.addHour(new Date(), 1));
        inboxService.updateDelivery(dc);

        List<DeliveryStatisticsFull> stat = inboxService.getDeliveryStatisticsFull();
        log.debug("Finding " + stat.size() + " rows ");
        return size;
    }

    @Test
    public void testIdsService() {
        Long fromId = idsService.getLongValue(RegisteredId.LAST_CHECKOUT_EVENT_ID);
        if (fromId == null) {
            fromId = 1L;
            idsService.initLongValue(RegisteredId.LAST_CHECKOUT_EVENT_ID, fromId);
        }
        log.debug("LAST_CHECKOUT_EVENT_ID = " + fromId);

        assertTrue(idsService.compareAndSetLongValue(RegisteredId.LAST_CHECKOUT_EVENT_ID, fromId, fromId + 1));
        assertEquals(Long.valueOf(fromId + 1L), idsService.getLongValue(RegisteredId.LAST_CHECKOUT_EVENT_ID));
    }

    @Test
    public void requestIdStored() {
        Order order = OrderProvider.getBlueOrder();
        OrderHistoryEvent event = EventsProvider.orderStatusUpdated(order,
                OrderStatus.DELIVERY,
                OrderStatus.DELIVERED);

        eventTestUtils.mockEvent(event);
        eventTestUtils.runImport();
        eventTestUtils.deliverNotifications();

        List<DeliveryChannel> deliveriesUpdated = eventTestUtils.getNotifications(ChannelType.EMAIL)
                .stream().map(Notification::getDeliveryChannels).flatMap(Collection::stream).collect(toList());
        assertThat(deliveriesUpdated, everyItem(hasProperty("requestId", notNullValue())));
    }

    @Test
    public void testMarkedDelete() throws IOException {
        EvictionSearch es = new EvictionSearch(new Date(System.currentTimeMillis() + 10000), NotificationStatus.NEW,
                100);

        fillInbox();
        List<Notification> notes = inboxService.evictionSearch(es);

        assertEquals(2, notes.size());

        inboxService.findNotificationsAndMarkDeleted(es);

        notes = inboxService.evictionSearch(es);

        assertEquals(0, notes.size());

        notes = eventTestUtils.getAllNotifications();

        assertEquals(2, notes.size());
        assertEquals(1, notes.get(0).getDeliveryChannels().size());
        assertEquals(NotificationStatus.DELETED, notes.get(0).getDeliveryChannels().get(0).getStatus());
        assertEquals(1, notes.get(1).getDeliveryChannels().size());
        assertEquals(NotificationStatus.DELETED, notes.get(1).getDeliveryChannels().get(0).getStatus());
    }
}
