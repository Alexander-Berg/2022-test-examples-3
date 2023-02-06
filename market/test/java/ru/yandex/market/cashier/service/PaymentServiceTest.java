package ru.yandex.market.cashier.service;


import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PaymentServiceTest extends AbstractApplicationTest {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentServiceTest.class);
    @Autowired
    private PaymentService paymentService;

    @Test
    public void testCreatePrepayPayment() {
        ExternalPaymentId externalPaymentId = getExternalPaymentId();

        PrepayPaymentEntity payment = paymentService.createNewPayment(() -> {
            PrepayPaymentEntity paymentEntity = new PrepayPaymentEntity();
            paymentEntity.setExternalPaymentId(externalPaymentId);

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

            payOrder.setPrepayPayment(paymentEntity);
            paymentEntity.getOrders().add(payOrder);

            return paymentEntity;
        });

        PrepayPaymentEntity loaded = paymentService.loadPaymentEntity(externalPaymentId, PrepayPaymentEntity.class);
        LOG.info("loaded = " + loaded);
        assertEquals(1, loaded.getOrders().size());
        assertEquals(1, loaded.getOrders().iterator().next().getItems().size());
    }

    @Test
    public void testStateChange() {
        ExternalPaymentId externalPaymentId = getExternalPaymentId();

        PaymentEntity payment = paymentService.createNewPayment(createPaymentEntity(externalPaymentId));
        paymentService.changePaymentStatus(payment.getId(), PaymentStatus.IN_PROGRESS);

        PaymentEntity loaded = paymentService.loadPaymentEntity(externalPaymentId, PaymentEntity.class);
        LOG.info("loaded = " + loaded);
        assertEquals(PaymentStatus.IN_PROGRESS, loaded.getPaymentStatus());
    }

    @Test
    public void testPaymentHistory() {
        ExternalPaymentId externalPaymentId = getExternalPaymentId();

        PaymentEntity payment = paymentService.createNewPayment(createPaymentEntity(externalPaymentId));
        paymentService.changePaymentStatus(payment.getId(), PaymentStatus.IN_PROGRESS);

        Collection<PaymentEntity> historyEntities = paymentService.loadPaymentHistory(payment.getId(), PaymentEntity.class);
        assertEquals(2, historyEntities.size());
    }

    private ExternalPaymentId getExternalPaymentId() {
        return new ExternalPaymentId(1337L, Client.BLUE_MARKETPLACE);
    }

    private Supplier<PaymentEntity> createPaymentEntity(ExternalPaymentId externalPaymentId) {
        return () -> {
            PaymentEntity paymentEntity = new PaymentEntity();
            paymentEntity.setExternalPaymentId(externalPaymentId);
            return paymentEntity;
        };
    }
}
