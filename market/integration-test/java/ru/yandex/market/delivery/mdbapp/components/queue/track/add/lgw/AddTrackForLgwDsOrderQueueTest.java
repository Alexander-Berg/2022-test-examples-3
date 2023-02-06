package ru.yandex.market.delivery.mdbapp.components.queue.track.add.lgw;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.track.add.dto.AddTrackForLgwDsOrder;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterOrderService;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterParcelService;
import ru.yandex.market.delivery.mdbapp.configuration.LatchTaskListenerConfig;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class AddTrackForLgwDsOrderQueueTest extends AllMockContextualTest {

    @Autowired
    private QueueProducer<AddTrackForLgwDsOrder> producer;

    @Autowired
    private TaskLifecycleListener taskListener;

    private CountDownLatch countDownLatch;

    @MockBean
    private CheckouterOrderService checkouterOrderService;

    @MockBean
    private CheckouterParcelService checkouterParcelService;

    private static final long DELIVERY_SERVICE_ID = 1005525L;
    private static final long ORDER_ID = 2L;
    private static final long PARCEL_ID = 3L;
    private static final String TRACK_ID = "123456";

    @Before
    public void setUp() {
        countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);

        doReturn(new Track())
                .when(checkouterParcelService).addTrack(ORDER_ID, PARCEL_ID, TRACK_ID, DELIVERY_SERVICE_ID, null);
        doReturn(orderWithDelivery())
                .when(checkouterOrderService).getOrder(ORDER_ID);
    }

    @After
    public void after() {
        verifyNoMoreInteractions(checkouterOrderService, checkouterParcelService);
    }

    @Test
    @DisplayName("Проверка вызова метода регистрации трека в чекаутере")
    public void addTrackForLgwDsOrder() throws InterruptedException {
        producer.enqueue(EnqueueParams.create(addTrackParams()));
        countDownLatch.await(2, TimeUnit.SECONDS);

        verify(checkouterOrderService).getOrder(ORDER_ID);
        verify(checkouterParcelService).addTrack(ORDER_ID, PARCEL_ID, TRACK_ID, DELIVERY_SERVICE_ID, null);
    }

    private AddTrackForLgwDsOrder addTrackParams() {
        return AddTrackForLgwDsOrder.builder()
                .orderId(ORDER_ID)
                .trackId(TRACK_ID)
                .build();
    }

    private Order orderWithDelivery() {
        Order order = new Order();
        order.setId(ORDER_ID);

        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(DELIVERY_SERVICE_ID);
        Parcel parcel = new Parcel();
        parcel.setId(PARCEL_ID);
        delivery.setParcels(List.of(parcel));
        order.setDelivery(delivery);
        return order;
    }
}
