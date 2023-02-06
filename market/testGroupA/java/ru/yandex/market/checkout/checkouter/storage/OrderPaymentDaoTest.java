package ru.yandex.market.checkout.checkouter.storage;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.storage.payment.OrderPaymentDao;
import ru.yandex.market.checkout.checkouter.storage.payment.PaymentWritingDao;
import ru.yandex.market.checkout.helpers.OrderInsertHelper;
import ru.yandex.market.checkout.providers.PaymentProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkouter.entities.OrderPaymentEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author musachev
 * Created on: 15.07.18
 */
public class OrderPaymentDaoTest extends AbstractServicesTestBase {

    @Autowired
    private OrderPaymentDao orderPaymentDao;
    @Autowired
    private OrderInsertHelper orderInsertHelper;
    @Autowired
    private PaymentWritingDao paymentWritingDao;

    private long orderId1;
    private long orderId2;
    private long orderId3;
    private long orderId4;
    private long payment1;
    private long payment2;

    @BeforeEach
    public void setUp() {
        final Order order = OrderProvider.getBlueOrder();
        orderId1 = orderInsertHelper.insertOrder(order);
        orderId2 = orderInsertHelper.insertOrder(order);
        orderId3 = orderInsertHelper.insertOrder(order);
        orderId4 = orderInsertHelper.insertOrder(order);
        payment1 = insertPayment(orderId1);
        payment2 = insertPayment(orderId2);
    }

    @Test
    public void insertBatchTest() {
        List<OrderPaymentEntity> orderPaymentEntityList = Arrays.asList(
                (new OrderPaymentEntityBuilder(getClock())).setOrderId(orderId1).setPaymentId(payment1).build(),
                (new OrderPaymentEntityBuilder(getClock())).setOrderId(orderId2).setPaymentId(payment1).build(),
                (new OrderPaymentEntityBuilder(getClock())).setOrderId(orderId3).setPaymentId(payment1).build(),
                (new OrderPaymentEntityBuilder(getClock())).setOrderId(orderId4).setPaymentId(payment2).build()

        );
        transactionTemplate.execute(
                (txStatus) -> {
                    orderPaymentDao.insertOrderPayments(orderPaymentEntityList);
                    return null;
                }
        );

        List<OrderPaymentEntity> entitySelectedList;
        entitySelectedList = orderPaymentDao.findByPaymentId(payment1);
        assertEquals(3, entitySelectedList.size());
    }

    @Test
    public void insertTest() {
        OrderPaymentEntity entity = (new OrderPaymentEntityBuilder(getClock()))
                .setOrderId(orderId1)
                .setPaymentId(payment1)
                .build();
        Integer result = transactionTemplate.execute((txStatus) -> orderPaymentDao.insertOrderPayment(entity));
        assertNotNull(result);

        OrderPaymentEntity entitySelected;
        entitySelected = orderPaymentDao.findByOrderId(entity.getOrderId()).get();
        assertEquals(entity, entitySelected);

        List<OrderPaymentEntity> entitySelectedList;
        entitySelectedList = orderPaymentDao.findByPaymentId(payment2);
        Assertions.assertTrue(entitySelectedList.isEmpty());

        entitySelectedList = orderPaymentDao.findByPaymentId(payment1);
        Assertions.assertSame(1, entitySelectedList.size());
        assertEquals(entity, entitySelectedList.get(0));
    }

    private Long insertPayment(final long orderId) {
        return transactionTemplate.execute(ts -> paymentWritingDao.insertPayment(
                ClientInfo.SYSTEM,
                PaymentProvider.createPayment(orderId)
        ));
    }

    static class OrderPaymentEntityBuilder {

        private OrderPaymentEntity orderPaymentEntity;

        OrderPaymentEntityBuilder(Clock clock) {
            orderPaymentEntity = new OrderPaymentEntity();
            orderPaymentEntity.setCreatedAt(LocalDateTime.now(clock));
        }

        public OrderPaymentEntityBuilder setPaymentId(long paymentId) {
            orderPaymentEntity.setPaymentId(paymentId);
            return this;
        }

        public OrderPaymentEntityBuilder setOrderId(long orderId) {
            orderPaymentEntity.setOrderId(orderId);
            return this;
        }

        public OrderPaymentEntity build() {
            return orderPaymentEntity;
        }
    }
}
