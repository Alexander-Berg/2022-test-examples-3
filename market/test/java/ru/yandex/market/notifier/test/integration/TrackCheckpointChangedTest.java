package ru.yandex.market.notifier.test.integration;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.service.InboxService;
import ru.yandex.market.notifier.storage.InboxDao;
import ru.yandex.market.notifier.util.EventTestUtils;
import ru.yandex.market.notifier.util.providers.OrderProvider;
import ru.yandex.market.notifier.util.providers.ParcelProvider;
import ru.yandex.market.notifier.util.providers.TrackCheckpointProvider;
import ru.yandex.market.notifier.util.providers.TrackProvider;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.PersNotifyClientException;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.tms.quartz2.model.PartitionExecutor;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.TRACK_CHECKPOINT_CHANGED;
import static ru.yandex.market.checkout.test.builders.OrderHistoryEventBuilder.anOrderHistoryEvent;
import static ru.yandex.market.notifier.jobs.zk.processors.BlueEventProcessor.DELIVERY_ATTEMPT_FAIL_CHECKPOINT;
import static ru.yandex.market.notifier.jobs.zk.processors.BlueEventProcessor.DELIVERY_TRANSPORTATION_RECIPIENT_CHECKPOINT;
import static ru.yandex.market.notifier.util.NotifierTestUtils.DEFAULT_ORDER_CREATION_DATE;
import static ru.yandex.market.notifier.util.providers.TrackProvider.DELIVERY_SERVICE_ID;
import static ru.yandex.market.notifier.util.providers.TrackProvider.TRACK_CODE;

/**
 * @author musachev
 */
public class TrackCheckpointChangedTest extends AbstractServicesTestBase {

    @Autowired
    private EventTestUtils eventTestUtils;
    @Autowired
    private InboxService inboxService;
    @Autowired
    private InboxDao inboxDao;
    @Autowired
    @Qualifier("DeliveryJob")
    private Collection<PartitionExecutor> deliveryWorkerJobs;
    @Autowired
    private PersNotifyClient persNotifyClient;

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.stream(new Object[][]{
                        {
                                Color.BLUE,
                                DELIVERY_TRANSPORTATION_RECIPIENT_CHECKPOINT,
                                false
                        },
                        {
                                Color.BLUE,
                                DELIVERY_ATTEMPT_FAIL_CHECKPOINT,
                                false
                        },
                        {
                                Color.BLUE,
                                666,
                                false
                        },
                        {
                                Color.RED,
                                DELIVERY_TRANSPORTATION_RECIPIENT_CHECKPOINT,
                                false
                        },
                        {
                                Color.RED,
                                DELIVERY_ATTEMPT_FAIL_CHECKPOINT,
                                false
                        },
                }
        ).map(Arguments::of);
    }

    /**
     * ?????????????????? ???????????????? ???????????? ???????????????????????? ?????? ?????????????????? ???????? ????????????????????:
     * ??? DELIVERY_TRANSPORTATION_RECIPIENT_CHECKPOINT
     * ??? DELIVERY_ATTEMPT_FAIL_CHECKPOINT
     */
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void test(Color rgb, Integer deliveryCheckpointStatus, boolean shouldEmailToBeSend) throws PersNotifyClientException {
        OrderHistoryEvent event = getEventWithCheckpoint(rgb, deliveryCheckpointStatus);
        eventTestUtils.mockEvents(Collections.singletonList(event));

        eventTestUtils.runImport();

        List<Notification> found = inboxDao.getAllNotifications();

        if (rgb != Color.RED) {
            Assertions.assertEquals(shouldEmailToBeSend ? 1 : 0, inboxService.getDeliveryStatisticsFull().size());
            Assertions.assertEquals(shouldEmailToBeSend ? 1 : 0, found.size());
        }

        if (shouldEmailToBeSend) {
            Notification notification = found.get(0);
            assertThat(notification.getDeliveryChannels(), contains(
                    hasProperty("type", CoreMatchers.is(ChannelType.EMAIL))
            ));
            AtomicInteger stream = new AtomicInteger();
            deliveryWorkerJobs.forEach(task -> task.doJob(null, stream.incrementAndGet()));

            Mockito.verify(persNotifyClient).createEvent(MockitoHamcrest.argThat(allOf(
                    instanceOf(NotificationEventSource.class),
                    hasProperty("email", CoreMatchers.is(event.getOrderAfter().getBuyer().getEmail()))
            )));
        }

    }

    private OrderHistoryEvent getEventWithCheckpoint(Color rgb, Integer deliveryCheckpointStatus) {
        Track track = TrackProvider.createTrack(TRACK_CODE, DELIVERY_SERVICE_ID);

        Parcel shipment = ParcelProvider.createParcelWithTracks(track);
        shipment.setId(1122334455L);

        Order orderBefore = OrderProvider.getOrderWithTracking();
        orderBefore.getDelivery().setParcels(Collections.singletonList(shipment));
        orderBefore.setId(1L);
        orderBefore.setStatus(OrderStatus.DELIVERY);
        orderBefore.setCreationDate(Date.from(DEFAULT_ORDER_CREATION_DATE));
        orderBefore.setRgb(rgb);
        orderBefore.getDelivery().setValidFeatures(null);

        TrackCheckpoint trackCheckpoint = TrackCheckpointProvider.createCheckpoint(deliveryCheckpointStatus);

        Track trackAfter = TrackProvider.createTrack(TRACK_CODE, DELIVERY_SERVICE_ID);
        trackAfter.addCheckpoint(trackCheckpoint);

        Parcel shipmentAfter = ParcelProvider.createParcelWithTracks();
        shipmentAfter.setId(1122334455L);
        shipmentAfter.addTrack(trackAfter);

        Order orderAfter = orderBefore.clone();
        orderAfter.setDelivery(orderAfter.getDelivery().clone());
        orderAfter.getDelivery().setParcels(Collections.singletonList(shipmentAfter));

        return anOrderHistoryEvent()
                .withEventType(TRACK_CHECKPOINT_CHANGED)
                .withOrderBefore(orderBefore)
                .withOrderAfter(orderAfter)
                .withClientInfo(new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID))
                .build();
    }
}
