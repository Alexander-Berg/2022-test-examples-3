package ru.yandex.market.checkout.checkouter.pay.calculators;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.pay.PaymentAgent;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItemPartition;


class PaymentPartitionCalculatorTest {

    @Test
    public void nullReceiptItem() {
        var result = PaymentPartitionCalculator.getPaymentAgentPrice(null, PaymentAgent.YANDEX_CASHBACK);
        Assertions.assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void receiptItemHasZeroCount() {
        PaymentAgent agent = PaymentAgent.SBER_SPASIBO;
        ReceiptItem receiptItem = new ReceiptItem();
        ReceiptItemPartition partition = new ReceiptItemPartition();
        receiptItem.addPartition(partition);
        partition.setPaymentAgent(agent);
        receiptItem.setCount(0);
        var result = PaymentPartitionCalculator.getPaymentAgentPrice(receiptItem,
                PaymentAgent.YANDEX_CASHBACK);
        Assertions.assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void receiptItemWithEmptyPartitions() {
        ReceiptItem receiptItem = new ReceiptItem();
        PaymentAgent agent = PaymentAgent.SBER_SPASIBO;
        var result = PaymentPartitionCalculator.getPaymentAgentPrice(receiptItem, agent);
        Assertions.assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void agentPartitionNotFound() {
        PaymentAgent agent = PaymentAgent.SBER_SPASIBO;
        ReceiptItem receiptItem = new ReceiptItem();
        receiptItem.setCount(1);
        ReceiptItemPartition partition = new ReceiptItemPartition();
        partition.setPaymentAgent(agent);
        receiptItem.addPartition(partition);
        var result = PaymentPartitionCalculator.getPaymentAgentPrice(receiptItem,
                PaymentAgent.YANDEX_CASHBACK);
        Assertions.assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void notEmptyPartitionAndValidAgent() {
        PaymentAgent agent = PaymentAgent.SBER_SPASIBO;
        ReceiptItem receiptItem = new ReceiptItem();
        receiptItem.setCount(1);
        ReceiptItemPartition partition = new ReceiptItemPartition();
        partition.setPaymentAgent(agent);
        partition.setAmount(new BigDecimal("250.0000"));
        receiptItem.addPartition(partition);
        var result = PaymentPartitionCalculator.getPaymentAgentPrice(receiptItem, agent);
        Assertions.assertEquals(new BigDecimal("250.0000"), result);
    }

    @Test
    public void validDivide() {
        PaymentAgent agent = PaymentAgent.SBER_SPASIBO;
        ReceiptItem receiptItem = new ReceiptItem();
        receiptItem.setCount(4);
        ReceiptItemPartition partition = new ReceiptItemPartition();
        partition.setPaymentAgent(agent);
        partition.setAmount(new BigDecimal("250.00"));
        receiptItem.addPartition(partition);
        var result = PaymentPartitionCalculator.getPaymentAgentPriceFromPartition(receiptItem, agent);
        Assertions.assertEquals(new BigDecimal("62.50"), result);
    }

    @Test
    public void validRound() {
        PaymentAgent agent = PaymentAgent.SBER_SPASIBO;
        ReceiptItem receiptItem = new ReceiptItem();
        receiptItem.setCount(1);
        ReceiptItemPartition partition = new ReceiptItemPartition();
        partition.setPaymentAgent(agent);
        partition.setAmount(new BigDecimal("250.0000"));
        receiptItem.addPartition(partition);
        var result = PaymentPartitionCalculator.getPaymentAgentPriceFromPartition(receiptItem, agent);
        Assertions.assertEquals(new BigDecimal("250.00"), result);
    }

    @Test
    void returnZero() {
        var result = PaymentPartitionCalculator.getPaymentAgentPriceFromPartition(null, null);
        Assertions.assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void nullPartitionsAmount() {
        PaymentAgent agent = PaymentAgent.SBER_SPASIBO;
        ReceiptItem receiptItem = new ReceiptItem();
        receiptItem.setCount(1);
        ReceiptItemPartition partition = new ReceiptItemPartition();
        partition.setAmount(null);
        partition.setPaymentAgent(agent);
        receiptItem.addPartition(partition);
        var result = PaymentPartitionCalculator.getPaymentAgentPriceFromPartition(receiptItem, agent);
        Assertions.assertEquals(BigDecimal.ZERO, result);
    }
}
