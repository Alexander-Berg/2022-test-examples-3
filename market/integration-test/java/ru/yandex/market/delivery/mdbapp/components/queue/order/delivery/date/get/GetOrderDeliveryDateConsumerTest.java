package ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.date.get;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterOrderService;
import ru.yandex.market.delivery.mdbapp.configuration.LatchTaskListenerConfig;
import ru.yandex.market.delivery.mdbapp.configuration.queue.GetOrdersDeliveryDateQueue;
import ru.yandex.market.delivery.mdbapp.util.DeliveryDateUpdateReason;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DirtiesContext
public class GetOrderDeliveryDateConsumerTest extends AllMockContextualTest {

    public static final Long ORDER_ID = 100500L;

    @Autowired
    private TaskLifecycleListener taskListener;

    @Autowired
    private DeliveryClient deliveryClient;

    private CountDownLatch countDownLatch;

    @MockBean
    private CheckouterOrderService checkouterOrderService;

    @Autowired
    @Qualifier(value = GetOrdersDeliveryDateQueue.QUEUE_PRODUCER)
    private QueueProducer<OrderDeliveryDateRequestDto> getOrdersDeliveryDateQueueProducer;

    @Before
    public void setUp() {
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
    }

    @Test
    public void testRunSuccess() throws InterruptedException, GatewayApiException {
        countDownLatch = new CountDownLatch(1);
        when(checkouterOrderService.getOrder(ORDER_ID))
            .thenReturn(getOrder());
        getOrdersDeliveryDateQueueProducer.enqueue(EnqueueParams.create(getQueueDto()));
        countDownLatch.await(2, TimeUnit.SECONDS);
        verify(deliveryClient).getOrdersDeliveryDateAsync(anyList(), any(Partner.class), any(ClientRequestMeta.class));
    }

    @Test
    public void testFailedWith5xxAndRetried() throws InterruptedException, GatewayApiException {
        countDownLatch = new CountDownLatch(2);
        doThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "some server error"))
            .when(deliveryClient)
            .getOrdersDeliveryDateAsync(anyList(), any(Partner.class), any(ClientRequestMeta.class));
        when(checkouterOrderService.getOrder(ORDER_ID))
            .thenReturn(getOrder());
        getOrdersDeliveryDateQueueProducer.enqueue(EnqueueParams.create(getQueueDto()));
        countDownLatch.await(2, TimeUnit.SECONDS);
        verify(deliveryClient, atLeast(2))
            .getOrdersDeliveryDateAsync(anyList(), any(Partner.class), any(ClientRequestMeta.class));
    }

    @Test
    public void testFailedWith4xxAndFinished() throws InterruptedException, GatewayApiException {
        countDownLatch = new CountDownLatch(2);
        doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, null, "some error".getBytes(), null))
            .when(deliveryClient)
            .getOrdersDeliveryDateAsync(anyList(), any(Partner.class), any(ClientRequestMeta.class));
        when(checkouterOrderService.getOrder(ORDER_ID))
            .thenReturn(getOrder());
        getOrdersDeliveryDateQueueProducer.enqueue(EnqueueParams.create(getQueueDto()));
        countDownLatch.await(2, TimeUnit.SECONDS);
        verify(deliveryClient, times(1))
            .getOrdersDeliveryDateAsync(anyList(), any(Partner.class), any(ClientRequestMeta.class));
    }

    @Test
    public void testOrderCancelled() throws InterruptedException {
        countDownLatch = new CountDownLatch(2);
        when(checkouterOrderService.getOrder(ORDER_ID))
            .thenReturn(getCancelledOrder());
        getOrdersDeliveryDateQueueProducer.enqueue(EnqueueParams.create(getQueueDto()));
        countDownLatch.await(2, TimeUnit.SECONDS);
        verify(deliveryClient, never()).getOrdersDeliveryDate(anyList(), any(Partner.class));
    }

    private OrderDeliveryDateRequestDto getQueueDto() {
        return new OrderDeliveryDateRequestDto(
            ORDER_ID.toString(), 1003937L, DeliveryDateUpdateReason.UNKNOWN
        );
    }

    Order getOrder() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setStatus(OrderStatus.DELIVERY);
        return order;
    }

    Order getCancelledOrder() {
        Order order = getOrder();
        order.setStatus(OrderStatus.CANCELLED);
        return order;
    }
}
