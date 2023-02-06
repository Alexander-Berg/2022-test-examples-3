package ru.yandex.market.ocrm.module.order;


import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.checkout.checkouter.pay.PaymentAgent;
import ru.yandex.market.checkout.checkouter.pay.PaymentPartition;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.AttributeEqFilter;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.domain.OrderPaymentPartition;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

import static org.mockito.ArgumentMatchers.any;

@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ModuleOrderTestConfiguration.class)
public class OrderPaymentPartitionTest {

    PaymentPartition checkouterPartitionOne;
    PaymentPartition checkouterPartitionTwo;
    Order order;

    @Inject
    OrderTestUtils orderTestUtils;

    @Inject
    EntityStorageService storageService;

    @Inject
    OrderPaymentPartitionsService partitionCheckouterService;

    @BeforeEach
    public void setUp() {
        orderTestUtils.clearCheckouterAPI();

        order = orderTestUtils.createOrder();

        checkouterPartitionOne = new PaymentPartition(PaymentAgent.DEFAULT, BigDecimal.valueOf(1000));
        checkouterPartitionTwo = new PaymentPartition(PaymentAgent.SBER_SPASIBO, BigDecimal.valueOf(500));

        Mockito.when(partitionCheckouterService.getActivePaymentPartitions(any()))
                .thenReturn(List.of());
    }

    @Test
    public void hasNoPartition() {
        Mockito.when(partitionCheckouterService.getActivePaymentPartitions(any()))
                .thenReturn(List.of());

        var partitions = getPartitions();

        Assertions.assertEquals(1, partitions.size());
        final OrderPaymentPartition orderPaymentPartition = partitions.get(0);
        final String paymentKind = orderPaymentPartition.getPaymentKind();
        Assertions.assertEquals("CASH_ON_DELIVERY", paymentKind);
        Assertions.assertNotNull(orderPaymentPartition.getAmount());
    }

    @Test
    public void twoPartitions() {
        Mockito.when(partitionCheckouterService.getActivePaymentPartitions(any()))
                .thenReturn(List.of(checkouterPartitionOne, checkouterPartitionTwo));


        var partitions = getPartitions();
        Assertions.assertEquals(2, partitions.size());

        Optional<OrderPaymentPartition> partitionOne = getPartitionByAgent(
                partitions,
                checkouterPartitionOne.getAmount());
        Optional<OrderPaymentPartition> partitionTwo = getPartitionByAgent(
                partitions,
                checkouterPartitionTwo.getAmount());

        Assertions.assertTrue(partitionOne.isPresent());
        Assertions.assertTrue(partitionTwo.isPresent());
    }

    private List<OrderPaymentPartition> getPartitions() {
        var q = Query
                .of(OrderPaymentPartition.FQN)
                .withFilters(List.of(
                        new AttributeEqFilter(OrderPaymentPartition.PARENT, List.of(order.getTitle()))
                ));

        return storageService.list(q);
    }

    private Optional<OrderPaymentPartition> getPartitionByAgent(List<OrderPaymentPartition> partitions,
                                                                BigDecimal amount) {
        return partitions.stream()
                .filter(p -> amount.equals(p.getAmount()))
                .findAny();
    }
}
