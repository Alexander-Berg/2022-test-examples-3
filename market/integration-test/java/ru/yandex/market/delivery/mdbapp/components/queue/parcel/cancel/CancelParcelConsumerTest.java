package ru.yandex.market.delivery.mdbapp.components.queue.parcel.cancel;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import steps.orderSteps.OrderEventSteps;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.health.HealthManager;
import ru.yandex.market.delivery.mdbapp.components.queue.parcel.CancelParcelDto;
import ru.yandex.market.delivery.mdbapp.components.queue.parcel.OrderParcelDto;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterServiceClient;
import ru.yandex.market.delivery.mdbapp.configuration.LatchTaskListenerConfig;
import ru.yandex.market.delivery.mdbapp.configuration.queue.CancelParcelQueue;
import ru.yandex.market.delivery.mdbapp.integration.gateway.LgwGateway;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.delivery.parcel.CancellationRequestStatus.CONFIRMED;

@RunWith(Parameterized.class)
@DirtiesContext
public class CancelParcelConsumerTest extends AllMockContextualTest {

    private static final Long ORDER_ID = 1L;

    private static final Long PARCEL_ID = 100L;

    @Autowired
    private HealthManager healthManager;

    @MockBean
    private CheckouterServiceClient checkouterServiceClient;

    @MockBean
    private LgwGateway lgwGateway;

    @SpyBean
    private CancelParcelConsumer cancelParcelConsumer;

    @Autowired
    @Qualifier(CancelParcelQueue.QUEUE_PRODUCER)
    private QueueProducer<CancelParcelDto> cancelParcelProducer;

    @Autowired
    private TaskLifecycleListener taskListener;

    private CountDownLatch countDownLatch;

    @Parameter
    public Order order;

    @Parameter(1)
    public Parcel parcel;

    @Parameter(2)
    public boolean creationError;

    @Parameters()
    public static Collection<Object[]> data() {
        return Arrays.asList(
            //Отмена заказа через дропшип через СЦ
            buildDropshipSortingCenterOrder(),

            //обычный заказ с треком от СЦ
            buildFulfillmentOrder(OrderStatus.PROCESSING, DeliveryServiceType.SORTING_CENTER, buildParcel(), false),
            buildFulfillmentOrder(OrderStatus.PROCESSING, DeliveryServiceType.SORTING_CENTER, buildParcel(), true),

            //обычный заказ с треком от ФФ
            buildFulfillmentOrder(OrderStatus.PROCESSING, DeliveryServiceType.FULFILLMENT, buildParcel(), false),
            buildFulfillmentOrder(OrderStatus.PROCESSING, DeliveryServiceType.FULFILLMENT, buildParcel(), true),

            //Почтовые заказы с треками от СЦ и ФФ
            buildPostOrder(OrderStatus.PROCESSING, DeliveryServiceType.SORTING_CENTER, buildParcel(), false),
            buildPostOrder(OrderStatus.PROCESSING, DeliveryServiceType.FULFILLMENT, buildParcel(), false),
            buildPostOrder(OrderStatus.PROCESSING, DeliveryServiceType.SORTING_CENTER, buildParcel(), true),
            buildPostOrder(OrderStatus.PROCESSING, DeliveryServiceType.FULFILLMENT, buildParcel(), true),

            //Cross Dock заказ
            buildCrossdockOrder(OrderStatus.PROCESSING, buildParcel(), false),
            buildCrossdockOrder(OrderStatus.PROCESSING, buildParcel(), true),

            //Заказ уже отменён
            buildFulfillmentOrder(OrderStatus.CANCELLED, DeliveryServiceType.SORTING_CENTER, buildParcel(), false),
            buildPostOrder(OrderStatus.CANCELLED, DeliveryServiceType.SORTING_CENTER, buildParcel(), false),
            buildCrossdockOrder(OrderStatus.CANCELLED, buildParcel(), false)
        );
    }

    @Before
    public void beforeTest() {
        when(healthManager.isHealthyEnough()).thenReturn(true);

        Mockito.when(checkouterServiceClient.getOrder(Mockito.anyLong())).thenReturn(order);
        countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);

        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            countDownLatch.countDown();
            return result;
        }).when(cancelParcelConsumer).execute(any());
    }

    @Test
    public void cancelParcel() throws Exception {
        OrderParcelDto orderParcelDto = new OrderParcelDto(order, parcel);
        CancelParcelDto cancelParcelDto = new CancelParcelDto(ORDER_ID, PARCEL_ID);
        cancelParcelProducer.enqueue(EnqueueParams.create(cancelParcelDto));

        countDownLatch.await();

        Mockito.verify(checkouterServiceClient).getOrder(Mockito.anyLong());

        if (order.getStatus() == OrderStatus.CANCELLED) {
            // Cancelled
            Mockito.verify(lgwGateway, Mockito.never()).cancelParcel(orderParcelDto);
            Mockito.verify(lgwGateway, Mockito.never()).cancelParcelFF(orderParcelDto);
        } else if (creationError) {
            // Error
            Mockito.verify(lgwGateway, Mockito.never()).cancelParcel(orderParcelDto);
            Mockito.verify(lgwGateway, Mockito.never()).cancelParcelFF(orderParcelDto);

            Mockito.verify(checkouterServiceClient).cancelParcel(ORDER_ID, PARCEL_ID, CONFIRMED);
        } else if (DeliveryType.POST.equals(order.getDelivery().getType())) {
            // POST
            Mockito.verify(lgwGateway).cancelParcel(orderParcelDto);
            Mockito.verify(lgwGateway, Mockito.never()).cancelParcelFF(orderParcelDto);
        } else if (order.isFulfilment()) {
            // Fulfillment
            Mockito.verify(lgwGateway).cancelParcelFF(orderParcelDto);
            Mockito.verify(lgwGateway).cancelParcel(orderParcelDto);
        } else {
            // Crossdock
            Mockito.verify(lgwGateway).cancelParcel(orderParcelDto);
            Mockito.verify(lgwGateway, Mockito.never()).cancelParcelFF(orderParcelDto);
        }
    }

    private static Order buildOrder(OrderStatus status, Parcel parcel, boolean creationError) {
        Track trackDS = buildTrack(DeliveryServiceType.CARRIER, 106L, "track-code");

        Delivery delivery = new Delivery();
        delivery.addParcel(parcel);

        // Another parcels
        delivery.addParcel(buildParcel(301L));
        delivery.addParcel(buildParcel(303L));
        delivery.addParcel(buildParcel(303L));

        Order order = new Order();
        order.setId(ORDER_ID);
        order.setStatus(status);
        order.setDelivery(delivery);

        if (creationError) {
            parcel.setStatus(ParcelStatus.ERROR);
        } else {
            parcel.addTrack(trackDS);
        }

        return order;
    }

    private static Object[] buildCrossdockOrder(OrderStatus status, Parcel parcel, boolean creationError) {
        return new Object[]{buildOrder(status, parcel, creationError), parcel, creationError};
    }

    private static Object[] buildDropshipSortingCenterOrder() {
        Order order = OrderEventSteps.buildBeruDropshipOrderWithCancelRequest(
            ORDER_ID,
            1L,
            PARCEL_ID,
            1L
        );
        return new Object[]{
            order,
            CollectionUtils.firstOrNull(order.getDelivery().getParcels()),
            false
        };
    }

    private static Object[] buildFulfillmentOrder(
        OrderStatus status,
        DeliveryServiceType type,
        Parcel parcel,
        boolean creationError
    ) {
        Order order = buildOrder(status, parcel, creationError);
        Track trackFF = buildTrack(type, 110L, "track-code-from-FF");
        parcel.addTrack(trackFF);
        order.setFulfilment(true);

        return new Object[]{order, parcel, creationError};
    }

    private static Object[] buildPostOrder(
        OrderStatus status,
        DeliveryServiceType type,
        Parcel parcel,
        boolean creationError
    ) {
        Order order = buildOrder(status, parcel, creationError);
        order.getDelivery().setType(DeliveryType.POST);

        Track trackFF = buildTrack(type, 111L, "track-code-from-FF");
        parcel.addTrack(trackFF);

        return new Object[]{order, parcel, creationError};
    }

    private static Parcel buildParcel() {
        return buildParcel(PARCEL_ID);
    }

    private static Parcel buildParcel(Long parcelId) {
        Parcel parcel = new Parcel();
        parcel.setId(parcelId);
        return parcel;
    }

    private static Track buildTrack(DeliveryServiceType type, Long deliveryServiceId, String code) {
        Track track = new Track();
        track.setDeliveryServiceType(type);
        track.setDeliveryServiceId(deliveryServiceId);
        track.setTrackCode(code);
        return track;
    }
}
