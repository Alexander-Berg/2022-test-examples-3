package ru.yandex.market.checkout.checkouter.delivery;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.DeliveryTrack;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.tracker.MockTrackerHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Is.is;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class SortCheckpointsTest extends AbstractWebTestBase {

    private static final AtomicLong COUNTER = new AtomicLong(789L);

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private WireMockServer trackerMock;
    @Autowired
    private NotifyTracksHelper notifyTracksHelper;
    @Autowired
    private OrderGetHelper orderGetHelper;

    private static Date date(String date) {
        return Date.from(LocalDateTime.parse(date).atZone(ZoneId.systemDefault()).toInstant());
    }

    @Test
    public void sortCheckpoints() throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .build();

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        orderDeliveryHelper.addTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                new Track("iddqd", MOCK_DELIVERY_SERVICE_ID) {{
                    setDeliveryServiceType(DeliveryServiceType.CARRIER);
                }},
                ClientInfo.SYSTEM);

        MockTrackerHelper.mockGetDeliveryServices(MOCK_DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
        tmsTaskHelper.runRegisterDeliveryTrackTaskV2();

        Date firstDate = date("2019-07-06T10:15:30");
        notifyTrack(46, firstDate);
        Date secondDate = date("2019-06-06T10:15:30");
        notifyTrack(50, secondDate);
        Date thirdDate = date("2019-10-06T10:15:30");
        notifyTrack(30, thirdDate);

        order = client.getOrder(order.getId(), ClientRole.SYSTEM, 1L);
        assertThat(
                order.getDelivery()
                        .getParcels().iterator().next()
                        .getTracks().iterator().next()
                        .getCheckpoints(),
                contains(
                        allOf(
                                hasProperty("deliveryCheckpointStatus", is(50)),
                                hasProperty("checkpointDate", is(secondDate))
                        ),
                        allOf(
                                hasProperty("deliveryCheckpointStatus", is(46)),
                                hasProperty("checkpointDate", is(firstDate))
                        ),
                        allOf(
                                hasProperty("deliveryCheckpointStatus", is(30)),
                                hasProperty("checkpointDate", is(thirdDate))
                        )
                )
        );

        order = orderGetHelper.getOrder(order.getId(), new ClientInfo(ClientRole.USER, BuyerProvider.UID));
        assertThat(
                order.getDelivery().getParcels().get(0).getTracks().get(0).getCheckpoints(),
                contains(
                        hasProperty("deliveryCheckpointStatus", is(50)),
                        hasProperty("deliveryCheckpointStatus", is(46)),
                        hasProperty("deliveryCheckpointStatus", is(30))
                )
        );
    }

    private void notifyTrack(int status, Date date) throws Exception {
        DeliveryTrack deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID, status,
                COUNTER.incrementAndGet());
        deliveryTrack.getDeliveryTrackCheckpoints().get(0).setCheckpointDate(date);
        notifyTracksHelper.notifyTracks(deliveryTrack);
    }
}
