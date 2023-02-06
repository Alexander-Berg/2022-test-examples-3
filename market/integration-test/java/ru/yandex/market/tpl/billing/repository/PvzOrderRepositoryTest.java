package ru.yandex.market.tpl.billing.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.model.PaymentMethod;
import ru.yandex.market.tpl.billing.model.PaymentStatus;
import ru.yandex.market.tpl.billing.model.entity.PvzOrder;

public class PvzOrderRepositoryTest extends AbstractFunctionalTest {

    @Autowired
    private PvzOrderRepository pvzOrderRepository;

    @Test
    @DisplayName("Скрипт в методе findOrdersForPartners работает")
    @DbUnitDataSet(before = "/database/repository/pvzorderrepository/pvz_order.csv")
    void testFindOrdersForPartners() {
        PvzOrder expectedOrder = createTestPvzOrder();
        PvzOrder actualOrder = pvzOrderRepository.findOrdersForPartners(
                List.of(1L),
                OffsetDateTime.of(2021, 9, 20, 0, 0, 0, 0, ZoneOffset.ofHours(3)),
                Set.of(PaymentMethod.CARD.name())
        ).get(0);

        Assertions.assertEquals(expectedOrder.getId(), actualOrder.getId());
        Assertions.assertEquals(expectedOrder.getPvzOrderId(), actualOrder.getPvzOrderId());
        Assertions.assertEquals(expectedOrder.getMarketOrderId(), actualOrder.getMarketOrderId());
        Assertions.assertEquals(expectedOrder.getDeliveryServiceId(), actualOrder.getDeliveryServiceId());
        Assertions.assertEquals(expectedOrder.getPickupPointId(), actualOrder.getPickupPointId());
        Assertions.assertEquals(expectedOrder.getPaymentMethod(), actualOrder.getPaymentMethod());
        Assertions.assertEquals(expectedOrder.getPaymentStatus(), actualOrder.getPaymentStatus());
        Assertions.assertEquals(
                expectedOrder.getDeliveredAt().toInstant(),
                actualOrder.getDeliveredAt().toInstant()
        );
        Assertions.assertEquals(expectedOrder.getPaymentSum(), actualOrder.getPaymentSum());
        Assertions.assertEquals(
                expectedOrder.getCreatedAt().toInstant(),
                actualOrder.getCreatedAt().toInstant()
        );
        Assertions.assertEquals(expectedOrder.getItemsSum(), actualOrder.getItemsSum());

    }

    private PvzOrder createTestPvzOrder() {
        return new PvzOrder()
                .setId(1L)
                .setPvzOrderId(1L)
                .setMarketOrderId("or1")
                .setDeliveryServiceId(1L)
                .setPickupPointId(1L)
                .setPaymentMethod(PaymentMethod.CARD)
                .setPaymentStatus(PaymentStatus.PAID)
                .setDeliveredAt(OffsetDateTime.of(2021, 9, 10, 0, 0, 0, 0, ZoneOffset.ofHours(3)))
                .setPaymentSum(BigDecimal.TEN)
                .setCreatedAt(OffsetDateTime.of(2021, 8, 1, 0, 0, 0, 0, ZoneOffset.ofHours(3)))
                .setItemsSum(BigDecimal.valueOf(1000000));
    }
}
