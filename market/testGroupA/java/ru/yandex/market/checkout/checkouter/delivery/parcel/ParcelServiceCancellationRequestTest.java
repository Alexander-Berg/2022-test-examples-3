package ru.yandex.market.checkout.checkouter.delivery.parcel;

import java.util.Collections;
import java.util.Optional;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryUtils;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.NotificationResultStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.NotificationResults;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.edit.OrderEditService;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.ParcelHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.test.builders.ParcelBuilder;
import ru.yandex.market.checkout.test.providers.DeliveryUpdateProvider;
import ru.yandex.market.checkout.test.providers.TrackProvider;
import ru.yandex.market.checkout.util.tracker.MockTrackerHelper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class ParcelServiceCancellationRequestTest extends AbstractWebTestBase {

    private Order order;
    private Parcel cancellingParcel;

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private WireMockServer trackerMock;
    @Autowired
    private NotifyTracksHelper notifyTracksHelper;
    @Autowired
    private ParcelHelper parcelHelper;
    @Autowired
    private OrderEditService orderEditService;

    @BeforeEach
    public void setUp() throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .buildParameters();
        order = orderCreateHelper.createOrder(parameters);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        order = orderService.getOrder(order.getId());

        assertTrue(CollectionUtils.isNonEmpty(order.getDelivery().getParcels()));
        assertSame(1, order.getDelivery().getParcels().size());

        cancellingParcel = Optional.ofNullable(order.getDelivery().getParcels())
                .orElseThrow(() -> new ParcelNotFoundException(0L))
                .iterator()
                .next();

        order = orderDeliveryHelper.updateOrderDelivery(order.getId(),
                DeliveryUpdateProvider.createDeliveryUpdateWithParcels(
                        ParcelBuilder.instance()
                                .withId(cancellingParcel.getId())
                                .withTracks(Collections.singletonList(
                                        TrackProvider.createTrack(MOCK_DELIVERY_SERVICE_ID)))
                                .build()
                ));
        assertThat(order.getDelivery().getParcels().get(0).getTracks(), hasSize(1));

        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
        MockTrackerHelper.mockGetDeliveryServices(MOCK_DELIVERY_SERVICE_ID, trackerMock);

        tmsTaskHelper.runRegisterDeliveryTrackTaskV2();

        order = orderService.getOrder(order.getId());
        assertThat(order.getDelivery().getParcels().get(0).getTracks().get(0).getTrackerId(),
                CoreMatchers.equalTo(MockTrackerHelper.TRACKER_ID));

        CancellationRequest cancellationRequest = new CancellationRequest(
                OrderSubstatus.USER_PLACED_OTHER_ORDER,
                "some sad writings..."
        );

        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setCancellationRequest(
                new ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequest(
                        cancellationRequest.getSubstatus(),
                        cancellationRequest.getNotes()
                )
        );
        orderEditService.editOrder(order.getId(), new ClientInfo(ClientRole.CALL_CENTER_OPERATOR, 1121L),
                Color.ALL_COLORS, orderEditRequest);

        Order updatedOrder = orderService.getOrder(order.getId());
        cancellingParcel = DeliveryUtils.requireParcel(cancellingParcel.getId(), updatedOrder);
    }

    @Test
    public void confirmCancellationRequestTest() throws Exception {
        parcelHelper.updateCancellationRequestStatus(
                order.getId(),
                cancellingParcel.getId(),
                CancellationRequestStatus.CONFIRMED,
                ClientInfo.SYSTEM
        );

        Order updatedOrder = orderService.getOrder(order.getId());
        assertSame(CANCELLED, updatedOrder.getStatus());
    }

    @Test
    public void shouldNotCancelIfInDeliveryService() throws Exception {
        NotificationResults notificationResults =
                notifyTracksHelper.notifyTracksForResult(DeliveryTrackProvider.getDeliveryTrack(
                        MockTrackerHelper.TRACKER_ID, 10));

        assertThat(notificationResults.getResults().get(0).getStatus(), is(NotificationResultStatus.OK));

        parcelHelper.updateCancellationRequestStatus(
                order.getId(),
                cancellingParcel.getId(),
                CancellationRequestStatus.CONFIRMED,
                ClientInfo.SYSTEM
        );

        Order updatedOrder = orderService.getOrder(order.getId());
        assertThat(updatedOrder.getStatus(), CoreMatchers.not(CANCELLED));
    }
}
