package ru.yandex.market.cashier.dao;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.market.cashier.AbstractApplicationTest;
import ru.yandex.market.cashier.entities.ExternalPaymentId;
import ru.yandex.market.cashier.entities.PayDataEntity;
import ru.yandex.market.cashier.entities.PayOrderEntity;
import ru.yandex.market.cashier.entities.PayOrderItemEntity;
import ru.yandex.market.cashier.entities.PayOrderItemId;
import ru.yandex.market.cashier.entities.PaymentEntity;
import ru.yandex.market.cashier.entities.PrepayPaymentEntity;
import ru.yandex.market.cashier.model.Client;
import ru.yandex.market.cashier.model.payment.PayOrderItemType;
import ru.yandex.market.cashier.model.payment.PaymentStatus;
import ru.yandex.market.cashier.utils.TestableClock;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PaymentDaoTest extends AbstractApplicationTest {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentDaoTest.class);
    @Autowired
    private TestableClock clock;

    @Autowired
    private PaymentDao paymentDao;

    @Transactional
    @Test
    public void testCreate() {
        Long generatedId = paymentDao.create(entity());
        paymentDao.flush();

        PaymentEntity entity = paymentDao.get(generatedId);
        assertEquals(generatedId, entity.getId());
        assertEquals(Long.valueOf(1337L), entity.getExternalPaymentId().getExternalId());
        assertEquals(PaymentStatus.INIT, entity.getPaymentStatus());
    }

    @Transactional
    @Test
    public void CreatePrepayPayment() {
        PrepayPaymentEntity prepayPayment = new PrepayPaymentEntity();
        prepayPayment.setExternalPaymentId(new ExternalPaymentId(1337L, Client.BLUE_MARKETPLACE));
        PayOrderEntity payOrder = new PayOrderEntity();
        payOrder.setOrderId(666L);
        payOrder.setOrderTotalAmount(BigDecimal.TEN);

        PayDataEntity payDataEntity = new PayDataEntity();
        payDataEntity.setClientId(1);
        payDataEntity.setCampaignId(2);

        payOrder.setShopInfo(payDataEntity);

        PayOrderItemEntity item = new PayOrderItemEntity();
        item.setOrderItemId(new PayOrderItemId(777L, PayOrderItemType.FIRST_PARTY));
        item.setItemShopInfo(payDataEntity);
        item.setPayOrder(payOrder);
        payOrder.getItems().add(item);

        payOrder.setPrepayPayment(prepayPayment);
        prepayPayment.getOrders().add(payOrder);

        Long generatedId = paymentDao.create(prepayPayment);
        LOG.info("payment =" + prepayPayment);

        paymentDao.flush();

        PaymentEntity entity = paymentDao.get(generatedId);
        PrepayPaymentEntity loaded = (PrepayPaymentEntity) entity;
        assertEquals(1, loaded.getOrders().size());
    }

    @Transactional
    @Test
    public void testUpdateDate() {
        Long generatedId = paymentDao.create(entity());
        paymentDao.flush();

        PaymentEntity entity = paymentDao.get(generatedId);
        entity.setPaymentStatus(PaymentStatus.IN_PROGRESS);

        Instant current = clock.instant();
        clock.setFixed(current, ZoneId.systemDefault());
        paymentDao.flush();

        entity = paymentDao.get(generatedId);
        assertEquals(current, entity.getUpdateDate());
    }

    private PaymentEntity entity() {
        PaymentEntity payment = new PaymentEntity();
        payment.setExternalPaymentId(new ExternalPaymentId(1337L, Client.BLUE_MARKETPLACE));
        return payment;
    }
}
