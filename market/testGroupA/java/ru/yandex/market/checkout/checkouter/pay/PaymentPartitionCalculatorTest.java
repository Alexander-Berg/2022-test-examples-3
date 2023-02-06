package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.checkout.checkouter.balance.trust.model.BasketLineMarkup;
import ru.yandex.market.checkout.checkouter.balance.trust.model.BasketMarkup;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.pay.calculators.PartitionLimits;
import ru.yandex.market.checkout.checkouter.pay.calculators.PaymentPartitionCalculator;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItemPartition;
import ru.yandex.market.common.report.model.FeedOfferId;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.pay.calculators.PaymentPartitionCalculator.calculatePaymentPartitions;
import static ru.yandex.market.checkout.checkouter.pay.calculators.PaymentPartitionCalculator.createBasketMarkup;
import static ru.yandex.market.checkout.checkouter.pay.calculators.PaymentPartitionCalculator.getTotalByAgent;

public class PaymentPartitionCalculatorTest {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentPartitionCalculatorTest.class);

    private final PaymentPartitionCalculator calculator = new PaymentPartitionCalculator();

    @Test
    public void testSimplePartition() throws Exception {
        List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(10)));
            add(createReceiptItem(2, 2, new BigDecimal(20)));
            add(createReceiptItem(3, 3, new BigDecimal(30)));
        }};

        List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
            add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("10")));
        }};


        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);

        ReceiptItem item1 = receiptItems.stream().filter(i -> 1 == i.getOrderId()).findFirst().orElse(null);
        ReceiptItem item2 = receiptItems.stream().filter(i -> 2 == i.getOrderId()).findFirst().orElse(null);
        ReceiptItem item3 = receiptItems.stream().filter(i -> 3 == i.getOrderId()).findFirst().orElse(null);

        Assertions.assertEquals(new BigDecimal("2"), item1.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("8"), item1.amountByAgent(PaymentAgent.DEFAULT));

        Assertions.assertEquals(new BigDecimal("3"), item2.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("17"), item2.amountByAgent(PaymentAgent.DEFAULT));

        Assertions.assertEquals(new BigDecimal("5"), item3.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("25"), item3.amountByAgent(PaymentAgent.DEFAULT));


        validateParttitionTotal(receiptItems);
    }

    @Test
    public void testPartitionMinusTwo() throws Exception {
        List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(10)));
            add(createReceiptItem(2, 2, new BigDecimal(10)));
        }};

        List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
            add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("18")));
        }};


        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);
        ReceiptItem item1 = receiptItems.stream().filter(i -> 1 == i.getOrderId()).findFirst().orElse(null);
        ReceiptItem item2 = receiptItems.stream().filter(i -> 2 == i.getOrderId()).findFirst().orElse(null);

        Assertions.assertEquals(new BigDecimal("9"), item1.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("1"), item1.amountByAgent(PaymentAgent.DEFAULT));

        Assertions.assertEquals(new BigDecimal("9"), item2.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("1"), item2.amountByAgent(PaymentAgent.DEFAULT));

        //Проверяем, что все расспределилось
        Assertions.assertEquals(new BigDecimal("18"),
                receiptItems.stream().map(ri -> ri.amountByAgent(PaymentAgent.SBER_SPASIBO)).reduce(BigDecimal::add)
                        .orElse(BigDecimal.ZERO));
        validateParttitionTotal(receiptItems);
    }

    @Test
    public void testPartitionTwoOneIsBig() throws Exception {
        List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(1000)));
            add(createReceiptItem(2, 2, new BigDecimal(5)));
        }};

        List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
            add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("1003")));
        }};


        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);
        ReceiptItem item1 = receiptItems.stream().filter(i -> 1 == i.getOrderId()).findFirst().orElse(null);
        ReceiptItem item2 = receiptItems.stream().filter(i -> 2 == i.getOrderId()).findFirst().orElse(null);

        Assertions.assertEquals(new BigDecimal("999"), item1.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("1"), item1.amountByAgent(PaymentAgent.DEFAULT));

        Assertions.assertEquals(new BigDecimal("4"), item2.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("1"), item2.amountByAgent(PaymentAgent.DEFAULT));

        //Проверяем, что все расспределилось
        Assertions.assertEquals(new BigDecimal("1003"),
                receiptItems.stream().map(ri -> ri.amountByAgent(PaymentAgent.SBER_SPASIBO)).reduce(BigDecimal::add)
                        .orElse(BigDecimal.ZERO));
        validateParttitionTotal(receiptItems);
    }

    @Test
    public void testPartitionTwoOneIsBig2() throws Exception {
        List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(10000)));
            add(createReceiptItem(2, 2, new BigDecimal(5)));
            add(createReceiptItem(3, 2, new BigDecimal(5)));
            add(createReceiptItem(4, 2, new BigDecimal(700)));
        }};

        List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
            add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("10706")));
        }};


        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);
        ReceiptItem item1 = receiptItems.stream().filter(i -> 1 == i.getOrderId()).findFirst().orElse(null);
        ReceiptItem item2 = receiptItems.stream().filter(i -> 2 == i.getOrderId()).findFirst().orElse(null);

        Assertions.assertEquals(new BigDecimal("9999"), item1.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("1"), item1.amountByAgent(PaymentAgent.DEFAULT));

        Assertions.assertEquals(new BigDecimal("4"), item2.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("1"), item2.amountByAgent(PaymentAgent.DEFAULT));

        //Проверяем, что все расспределилось
        Assertions.assertEquals(new BigDecimal("10706"),
                receiptItems.stream().map(ri -> ri.amountByAgent(PaymentAgent.SBER_SPASIBO)).reduce(BigDecimal::add)
                        .orElse(BigDecimal.ZERO));
        validateParttitionTotal(receiptItems);
    }

    @Test
    public void testPartitionMinusFour() throws Exception {
        List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(40)));
            add(createReceiptItem(2, 2, new BigDecimal(10)));
            add(createReceiptItem(3, 3, new BigDecimal(10)));
            add(createReceiptItem(4, 4, new BigDecimal(10)));
        }};

        List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
            add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("66")));
        }};


        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);
        ReceiptItem item1 = receiptItems.stream().filter(i -> 1 == i.getOrderId()).findFirst().orElse(null);
        ReceiptItem item2 = receiptItems.stream().filter(i -> 2 == i.getOrderId()).findFirst().orElse(null);
        ReceiptItem item3 = receiptItems.stream().filter(i -> 3 == i.getOrderId()).findFirst().orElse(null);

        Assertions.assertEquals(new BigDecimal("39"), item1.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("1"), item1.amountByAgent(PaymentAgent.DEFAULT));

        Assertions.assertEquals(new BigDecimal("9"), item2.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("1"), item2.amountByAgent(PaymentAgent.DEFAULT));

        Assertions.assertEquals(new BigDecimal("9"), item3.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("1"), item3.amountByAgent(PaymentAgent.DEFAULT));

        //Проверяем, что все расспределилось
        Assertions.assertEquals(new BigDecimal("66"),
                receiptItems.stream().map(ri -> ri.amountByAgent(PaymentAgent.SBER_SPASIBO)).reduce(BigDecimal::add)
                        .orElse(BigDecimal.ZERO));
        validateParttitionTotal(receiptItems);
    }


    @Test
    public void testPartitionMinusFour2() throws Exception {
        List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(10)));
            add(createReceiptItem(2, 2, new BigDecimal(10)));
            add(createReceiptItem(3, 3, new BigDecimal(30)));
            add(createReceiptItem(4, 4, new BigDecimal(40)));
        }};

        List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
            add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("86")));
        }};

        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);
        ReceiptItem item1 = receiptItems.stream().filter(i -> 1 == i.getOrderId()).findFirst().orElse(null);
        ReceiptItem item2 = receiptItems.stream().filter(i -> 2 == i.getOrderId()).findFirst().orElse(null);
        ReceiptItem item3 = receiptItems.stream().filter(i -> 3 == i.getOrderId()).findFirst().orElse(null);
        ReceiptItem item4 = receiptItems.stream().filter(i -> 4 == i.getOrderId()).findFirst().orElse(null);

        Assertions.assertEquals(new BigDecimal("9"), item1.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("1"), item1.amountByAgent(PaymentAgent.DEFAULT));

        Assertions.assertEquals(new BigDecimal("9"), item2.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("1"), item2.amountByAgent(PaymentAgent.DEFAULT));

        Assertions.assertEquals(new BigDecimal("29"), item3.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("1"), item3.amountByAgent(PaymentAgent.DEFAULT));

        Assertions.assertEquals(new BigDecimal("39"), item4.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("1"), item4.amountByAgent(PaymentAgent.DEFAULT));

        //Проверяем, что все расспределилось
        Assertions.assertEquals(new BigDecimal("86"),
                receiptItems.stream().map(ri -> ri.amountByAgent(PaymentAgent.SBER_SPASIBO)).reduce(BigDecimal::add)
                        .orElse(BigDecimal.ZERO));
        validateParttitionTotal(receiptItems);
    }


    @Test
    public void testImpossiblePartitionMinusOne() {
        Assertions.assertThrows(PaymentException.class, () -> {
            List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
                add(createReceiptItem(1, 1, new BigDecimal(10)));
                add(createReceiptItem(2, 2, new BigDecimal(10)));
            }};

            List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
                add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("19")));
            }};


            calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);
        });
    }

    @Test
    public void testImpossiblePartitionLast() {
        Assertions.assertThrows(PaymentException.class, () -> {
            List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
                add(createReceiptItem(1, 1, new BigDecimal(2)));
                add(createReceiptItem(2, 2, new BigDecimal(2)));
                add(createReceiptItem(3, 3, new BigDecimal(1)));
            }};

            List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
                add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("2")));
            }};


            calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);
        });
    }

    @Test
    public void testImpossiblePartition() {
        Assertions.assertThrows(PaymentException.class, () -> {
            List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
                add(createReceiptItem(1, 1, new BigDecimal(2)));
                add(createReceiptItem(2, 2, new BigDecimal(1)));
                add(createReceiptItem(3, 3, new BigDecimal(1)));
            }};

            List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
                add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("2")));
            }};


            calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);
        });
    }

    @Test
    public void testSimplePartitionFractionAmounts() throws Exception {
        List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(10.5)));
            add(createReceiptItem(2, 2, new BigDecimal(19.5)));
            add(createReceiptItem(3, 3, new BigDecimal(30)));
        }};

        List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
            add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("10")));
        }};


        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);

        ReceiptItem item1 = receiptItems.stream().filter(i -> 1 == i.getOrderId()).findFirst().orElse(null);
        ReceiptItem item2 = receiptItems.stream().filter(i -> 2 == i.getOrderId()).findFirst().orElse(null);
        ReceiptItem item3 = receiptItems.stream().filter(i -> 3 == i.getOrderId()).findFirst().orElse(null);

        Assertions.assertEquals(new BigDecimal("2"), item1.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("8.5"), item1.amountByAgent(PaymentAgent.DEFAULT));

        Assertions.assertEquals(new BigDecimal("3"), item2.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("16.5"), item2.amountByAgent(PaymentAgent.DEFAULT));

        Assertions.assertEquals(new BigDecimal("5"), item3.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("25"), item3.amountByAgent(PaymentAgent.DEFAULT));


        validateParttitionTotal(receiptItems);
    }

    @Test
    public void testSimplePartitionFractionAgentAmount() throws Exception {
        List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(10)));
            add(createReceiptItem(2, 2, new BigDecimal(20)));
            add(createReceiptItem(3, 3, new BigDecimal(30)));
        }};

        List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
            add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("10.99")));
        }};


        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);

        ReceiptItem item1 = receiptItems.stream().filter(i -> 1 == i.getOrderId()).findFirst().orElse(null);
        ReceiptItem item2 = receiptItems.stream().filter(i -> 2 == i.getOrderId()).findFirst().orElse(null);
        ReceiptItem item3 = receiptItems.stream().filter(i -> 3 == i.getOrderId()).findFirst().orElse(null);

        Assertions.assertEquals(new BigDecimal("2"), item1.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("8"), item1.amountByAgent(PaymentAgent.DEFAULT));

        Assertions.assertEquals(new BigDecimal("4"), item2.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("16"), item2.amountByAgent(PaymentAgent.DEFAULT));

        Assertions.assertEquals(new BigDecimal("4.99"), item3.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("25.01"), item3.amountByAgent(PaymentAgent.DEFAULT));


        validateParttitionTotal(receiptItems);
    }

    @Test
    public void testPartitionOneItem() throws Exception {
        List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(10)));
        }};

        List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
            add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("9.00")));
        }};


        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);

        ReceiptItem item1 = receiptItems.stream().filter(i -> 1 == i.getOrderId()).findFirst().orElse(null);

        Assertions.assertEquals(new BigDecimal("9.00"), item1.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("1.00"), item1.amountByAgent(PaymentAgent.DEFAULT));

        validateParttitionTotal(receiptItems);
    }

    @Test
    public void testPartitionWithFilter() throws Exception {
        List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, null, 111L, new BigDecimal("42.00")));
            add(createReceiptItem(2, 2, new BigDecimal(10)));
        }};

        List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
            add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("5.00")));
        }};

        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, ReceiptItem::isOrderItem);

        ReceiptItem item1 = receiptItems.stream().filter(i -> 2 == i.getOrderId()).findFirst().orElse(null);

        Assertions.assertEquals(new BigDecimal("5.00"), item1.amountByAgent(PaymentAgent.SBER_SPASIBO));
        Assertions.assertEquals(new BigDecimal("5.00"), item1.amountByAgent(PaymentAgent.DEFAULT));

        ReceiptItem deliveryItem =
                receiptItems.stream().filter(i -> 111L == i.getDeliveryId()).findFirst().orElse(null);
        Assertions.assertEquals(new BigDecimal("42.00"), deliveryItem.amountByAgent(PaymentAgent.DEFAULT));

        validateParttitionTotal(receiptItems);
    }

    @Test
    public void testPartitionEmpty() throws Exception {
        List<PaymentPartition> paymentPartitions = List.of(
                new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("5.00")));

        List<ReceiptItem> receiptItems = calculatePaymentPartitions(List.of(), paymentPartitions, r -> true);

        ReceiptItem item1 = receiptItems.stream().filter(i -> 1 == i.getOrderId()).findFirst().orElse(null);
        Assertions.assertNull(item1);
    }

    @Test
    public void testCalculateTotal() throws Exception {
        List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(10)));
            add(createReceiptItem(2, 2, new BigDecimal(20)));
            add(createReceiptItem(3, 3, new BigDecimal(30)));
        }};

        List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
            add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("10.33")));
        }};

        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);

        Map<PaymentAgent, BigDecimal> totals = getTotalByAgent(receiptItems);

        Assertions.assertEquals(new BigDecimal("49.67"), totals.get(PaymentAgent.DEFAULT));
        Assertions.assertEquals(new BigDecimal("10.33"), totals.get(PaymentAgent.SBER_SPASIBO));

        Assertions.assertEquals(new BigDecimal("60.00"), totals.values().stream()
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ONE)
        );
    }

    /**
     * Тест для случаев когда кол-во спасиб равно числу айтемов в заказе
     * https://st.yandex-team.ru/MARKETCHECKOUT-17995
     */
    @Test
    public void testCalculateSpasiboWhenAmountEqualsItemsNumber() {
        var receiptItems = List.of(
                createReceiptItem(1, 1, new BigDecimal(10)),
                createReceiptItem(1, 2, new BigDecimal(20)),
                createReceiptItem(1, 3, new BigDecimal(30)),
                createReceiptItem(1, 4, new BigDecimal(40)),
                createReceiptItem(1, 5, new BigDecimal(900)),
                createReceiptItem(1, 6, new BigDecimal(1337)),
                createReceiptItem(1, 7, new BigDecimal(5000))
        );

        var paymentPartitions = Collections.singletonList(
                new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("7")));

        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);

        var totals = getTotalByAgent(receiptItems);

        assertThat(totals.get(PaymentAgent.DEFAULT), is(new BigDecimal("7330")));
        assertThat(totals.get(PaymentAgent.SBER_SPASIBO), is(new BigDecimal("7")));
        receiptItems.forEach(item -> assertThat(item.amountByAgent(PaymentAgent.SBER_SPASIBO), is(BigDecimal.ONE)));
        assertThat(totals.values().stream().reduce(BigDecimal::add).orElse(BigDecimal.ONE), is(new BigDecimal("7337")));
    }

    @Test
    public void testRefundPartition() {
        List<ReceiptItem> paymentReceiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItemWithPartitions(1, 1, new BigDecimal(10)));
            add(createReceiptItemWithPartitions(1, 2, new BigDecimal(13)));
            add(createReceiptItemWithPartitions(1, 3, new BigDecimal(17)));
            add(createReceiptItemWithPartitions(1, 4, new BigDecimal(25)));
            add(addPartition(createReceiptItem(1, null, 99L, new BigDecimal(25))));
        }};

        List<ReceiptItem> draftRefundReceiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(5)));
            add(createReceiptItem(1, 3, new BigDecimal(11)));
            add(createReceiptItem(1, 4, new BigDecimal(25)));
            add(createReceiptItem(1, null, 99L, new BigDecimal(25)));
        }};

        List<ReceiptItem> result = calculator.calculateRefundPartitionsByPaymentReceipt(draftRefundReceiptItems,
                paymentReceiptItems);

        validateParttitionTotal(result);
        ReceiptItem first = result.stream().filter(i -> i.getItemId() == 1).findAny().get();
        assertThat(first.getPartitions(), hasSize(2));

        ReceiptItem delivery = result.stream().filter(ReceiptItem::isDelivery).findAny().get();
        assertThat(delivery.getPartitions(), hasSize(2));
    }

    @Test
    public void testRefundPartition2() {
        List<ReceiptItem> paymentReceiptItems = new ArrayList<ReceiptItem>() {{
            add(addPartition(createReceiptItem(1, 1, new BigDecimal(3), 3)));
        }};

        List<ReceiptItem> draftRefundReceiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(2), 2));
        }};

        List<ReceiptItem> refundReceiptItems =
                calculator.calculateRefundPartitionsByPaymentReceipt(draftRefundReceiptItems, paymentReceiptItems);


        Collection<ReceiptItem> itemsAfterReduce = calculator.subtractReceiptItems(
                paymentReceiptItems, refundReceiptItems);

        //Сумма всех партиций в чеках совпадает с рефандоами и дельтой
        assertThat(extractTotalPartitionAmount(itemsAfterReduce.iterator().next()), equalTo(new BigDecimal("1.00")));
        assertThat(extractTotalPartitionAmount(refundReceiptItems.iterator().next()), equalTo(new BigDecimal("2.00")));
    }

    private BigDecimal extractTotalPartitionAmount(ReceiptItem receiptItem) {
        return receiptItem.getPartitions().stream()
                .map(ReceiptItemPartition::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    @Test
    public void testCreateBasketMarkup() {
        List<Order> orders = new ArrayList<Order>() {{
            add(createOrderForMarkup(() -> {
                Order order = new Order();
                order.setId(1L);
                List<OrderItem> items = new ArrayList<>();

                LongStream.of(1L, 2L).forEach(id -> {
                    createOrderItem(items, id);
                });

                order.setItems(items);
                return order;
            }));


            add(createOrderForMarkup(() -> {
                Order order = new Order();
                order.setId(2L);
                List<OrderItem> items = new ArrayList<>();

                LongStream.of(3L, 4L).forEach(id -> {
                    createOrderItem(items, id);
                });

                order.setItems(items);
                return order;
            }));
        }};

        List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(10)));
            add(createReceiptItem(1, 2, new BigDecimal(10)));
            add(createReceiptItem(2, 3, new BigDecimal(30)));
            add(createReceiptItem(2, 4, new BigDecimal(40)));
        }};

        List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
            add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("10.33")));
        }};

        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);
        BasketMarkup markup = createBasketMarkup(orders, receiptItems);

        Assertions.assertEquals(4, markup.getBasketLinesMarkup().values().size());
        Assertions.assertEquals(new BigDecimal("90.00"),
                markup.getBasketLinesMarkup()
                        .values().stream()
                        .flatMap(m -> m.getLineMarkup().values().stream())
                        .reduce(BigDecimal::add).orElse(BigDecimal.ONE));

    }

    @Test
    public void freeLiftingTest() {
        List<Order> orders = new ArrayList<>() {{
            add(createOrderForMarkup(() -> {
                Order order = new Order();
                order.setId(666L);
                Delivery delivery = new Delivery(666L);
                delivery.setLiftPrice(BigDecimal.ZERO);
                delivery.setPrice(BigDecimal.ZERO);
                delivery.setBalanceOrderId("666");

                order.setDelivery(delivery);

                return order;
            }));
            add(createOrderForMarkup(() -> {
                Order order = new Order();
                order.setId(777L);
                Delivery delivery = new Delivery(777L);
                delivery.setLiftPrice(BigDecimal.ZERO);
                delivery.setPrice(BigDecimal.ONE);

                delivery.setBalanceOrderId("777");
                order.setDelivery(delivery);

                return order;
            }));
        }};

        ReceiptItem freeReceipt = new ReceiptItem();
        freeReceipt.setOrderId(777L);
        freeReceipt.setAmount(BigDecimal.ONE);
        freeReceipt.setDeliveryId(1L);

        BasketMarkup markup = createBasketMarkup(orders, List.of(freeReceipt));

        Map<String, BasketLineMarkup> lineMarkupMap = markup.getBasketLinesMarkup();
        Assertions.assertEquals(lineMarkupMap.size(), 1);

        Assertions.assertTrue(lineMarkupMap.containsKey("777"));
    }

    @Test
    public void testCreateBasketMarkupNullPartition() {
        List<Order> orders = new ArrayList<Order>() {{
            add(createOrderForMarkup(() -> {
                Order order = new Order();
                order.setId(1L);
                List<OrderItem> items = new ArrayList<>();

                LongStream.of(1L, 2L).forEach(id -> {
                    createOrderItem(items, id);
                });

                order.setItems(items);
                return order;
            }));


            add(createOrderForMarkup(() -> {
                Order order = new Order();
                order.setId(2L);
                List<OrderItem> items = new ArrayList<>();

                LongStream.of(3L, 4L).forEach(id -> {
                    createOrderItem(items, id);
                });

                order.setItems(items);
                return order;
            }));
        }};

        List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(10)));
            add(createReceiptItem(1, 2, new BigDecimal(10)));
            add(createReceiptItem(2, 3, new BigDecimal(30)));
            add(createReceiptItem(2, 4, new BigDecimal(40)));
        }};

        List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
            add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("10.33")));
        }};

        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);

        //imitate null partitions from DB (delivery for exaple)
        receiptItems.stream().filter(r -> r.getOrderId() == 2).forEach(r -> r.setPartitions(null));


        BasketMarkup markup = createBasketMarkup(orders, receiptItems);

        //ok, we have markup anyway
        Assertions.assertEquals(4, markup.getBasketLinesMarkup().values().size());
    }

    @Test
    public void testSpasiboPriceCalculation1() {
        ReceiptItem item = createReceiptItem(1, 1, new BigDecimal(20));
        addPartition(item);
        item.setCount(1);

        BigDecimal spasiboPrice = PaymentPartitionCalculator.getPaymentAgentPriceFromPartition(item,
                PaymentAgent.SBER_SPASIBO);
        assertThat(spasiboPrice, equalTo(new BigDecimal("13.33"))); //2/3
    }

    @Test
    public void testSpasiboPriceCalculation2() {
        ReceiptItem item = createReceiptItem(1, 1, new BigDecimal(20));
        addPartition(item);
        item.setCount(2);

        BigDecimal spasiboPrice = PaymentPartitionCalculator.getPaymentAgentPriceFromPartition(item,
                PaymentAgent.SBER_SPASIBO);
        assertThat(spasiboPrice, equalTo(new BigDecimal("6.66"))); //2/6
    }

    @Test
    public void testSpasiboPriceCalculation3() {
        ReceiptItem item = createReceiptItem(1, 1, new BigDecimal(20));
        addPartition(item);
        item.setCount(3);

        BigDecimal spasiboPrice = PaymentPartitionCalculator.getPaymentAgentPriceFromPartition(item,
                PaymentAgent.DEFAULT);
        assertThat(spasiboPrice, equalTo(new BigDecimal("2.22"))); //1/9
    }


    @Test
    public void testCreateBasketMarkupWithDelivery() {
        List<Order> orders = new ArrayList<Order>() {{
            add(createOrderForMarkup(() -> {
                Order order = new Order();
                order.setId(1L);
                List<OrderItem> items = new ArrayList<>();

                LongStream.of(1L, 2L).forEach(id -> {
                    createOrderItem(items, id);
                });

                order.setItems(items);

                createOrderDelivery(order, 100L, BigDecimal.TEN);
                return order;
            }));


            add(createOrderForMarkup(() -> {
                Order order = new Order();
                order.setId(2L);
                List<OrderItem> items = new ArrayList<>();

                LongStream.of(3L, 4L).forEach(id -> {
                    createOrderItem(items, id);
                });

                order.setItems(items);

                createOrderDelivery(order, 200L, BigDecimal.TEN);
                return order;
            }));
        }};

        List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(10)));
            add(createReceiptItem(1, 2, new BigDecimal(10)));
            add(createReceiptItem(2, 3, new BigDecimal(30)));
            add(createReceiptItem(2, 4, new BigDecimal(40)));

            add(createReceiptItem(1, null, 100L, new BigDecimal(50)));
            add(createReceiptItem(2, null, 200L, new BigDecimal(50)));
        }};

        List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
            add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("86.00")));
        }};

        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, ReceiptItem::isOrderItem);
        BasketMarkup markup = createBasketMarkup(orders, receiptItems);

        Assertions.assertEquals(6, markup.getBasketLinesMarkup().values().size());
        Assertions.assertEquals(new BigDecimal("190.00"),
                markup.getBasketLinesMarkup()
                        .values().stream()
                        .flatMap(m -> m.getLineMarkup().values().stream())
                        .reduce(BigDecimal::add).orElse(BigDecimal.ONE));

    }

    @Test
    public void testCreateBasketMarkupWithBigDelivery() {
        List<Order> orders = new ArrayList<Order>() {{
            add(createOrderForMarkup(() -> {
                Order order = new Order();
                order.setId(1L);
                List<OrderItem> items = new ArrayList<>();

                LongStream.of(1L, 2L).forEach(id -> {
                    createOrderItem(items, id);
                });

                order.setItems(items);

                createOrderDelivery(order, 100L, BigDecimal.TEN);
                return order;
            }));
        }};

        List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(10)));
            add(createReceiptItem(1, 2, new BigDecimal(10)));
            add(createReceiptItem(1, null, 100L, new BigDecimal(50)));
        }};

        List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
            add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("18")));
        }};

        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, ReceiptItem::isOrderItem);
        BasketMarkup markup = createBasketMarkup(orders, receiptItems);

        Assertions.assertEquals(3, markup.getBasketLinesMarkup().values().size());
        markup.getBasketLinesMarkup().values().forEach(
                i -> {
                    BigDecimal spasiboAmount = i.getLineMarkup().get(PaymentAgent.SBER_SPASIBO.getTrustPaymentMethod());
                    if (spasiboAmount != null) {
                        Assertions.assertTrue(spasiboAmount.compareTo(BigDecimal.ZERO) > 0);
                    }
                    BigDecimal cartAmount = i.getLineMarkup().get(PaymentAgent.DEFAULT.getTrustPaymentMethod());
                    Assertions.assertTrue(cartAmount.compareTo(BigDecimal.ZERO) > 0);
                }
        );

        Assertions.assertEquals(new BigDecimal("70"),
                markup.getBasketLinesMarkup()
                        .values().stream()
                        .flatMap(m -> m.getLineMarkup().values().stream())
                        .reduce(BigDecimal::add).orElse(BigDecimal.ONE));

    }


    @Test
    public void testCreateBasketMarkupWithFreeDelivery() {
        List<Order> orders = new ArrayList<Order>() {{
            add(createOrderForMarkup(() -> {
                Order order = new Order();
                order.setId(1L);
                List<OrderItem> items = new ArrayList<>();

                LongStream.of(1L, 2L).forEach(id -> {
                    createOrderItem(items, id);
                });

                order.setItems(items);

                createOrderDelivery(order, 100L, BigDecimal.TEN);
                return order;
            }));


            add(createOrderForMarkup(() -> {
                Order order = new Order();
                order.setId(2L);
                List<OrderItem> items = new ArrayList<>();

                LongStream.of(3L, 4L).forEach(id -> {
                    createOrderItem(items, id);
                });

                order.setItems(items);

                createOrderDelivery(order, 200L, BigDecimal.ZERO).setBalanceOrderId(null);

                return order;
            }));
        }};

        List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(10)));
            add(createReceiptItem(1, 2, new BigDecimal(10)));
            add(createReceiptItem(2, 3, new BigDecimal(30)));
            add(createReceiptItem(2, 4, new BigDecimal(40)));

            add(createReceiptItem(1, null, 100L, new BigDecimal(50)));
        }};

        List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
            add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("10.33")));
        }};

        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);
        BasketMarkup markup = createBasketMarkup(orders, receiptItems);

        Assertions.assertEquals(5, markup.getBasketLinesMarkup().values().size());
        Assertions.assertEquals(new BigDecimal("140.00"),
                markup.getBasketLinesMarkup()
                        .values().stream()
                        .flatMap(m -> m.getLineMarkup().values().stream())
                        .reduce(BigDecimal::add).orElse(BigDecimal.ONE));

    }

    @Test
    public void testIncomingPartitionsWithDefault() {
        Assertions.assertThrows(PaymentException.class, () -> {
            List<PaymentPartition> partitions = new ArrayList<PaymentPartition>() {{
                add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, BigDecimal.TEN));
                add(new PaymentPartition(PaymentAgent.DEFAULT, BigDecimal.TEN));
            }};

            PaymentPartitionCalculator.checkIncomingPartitions(partitions);
        });
    }

    @Test
    public void testIncomingPartitionsDuplicates() {
        Assertions.assertThrows(PaymentException.class, () -> {
            List<PaymentPartition> partitions = new ArrayList<PaymentPartition>() {{
                add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, BigDecimal.TEN));
                add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, BigDecimal.TEN));
                add(new PaymentPartition(PaymentAgent.DEFAULT, BigDecimal.TEN));
            }};

            PaymentPartitionCalculator.checkIncomingPartitions(partitions);
        });
    }

    @Test
    public void testMaxSpasiboAmount() {
        List<OrderItem> items = new ArrayList<OrderItem>() {{
            add(createOrderItem(1L, "10", 2));
            add(createOrderItem(2L, "20", 1));
            add(createOrderItem(2L, "20", 1));
        }};

        BigDecimal maxAmount = PaymentPartitionCalculator.calculatePartitionMaxAmountForOrderItems(items);
        Assertions.assertEquals(new BigDecimal("57"), maxAmount);
    }

    @Test
    public void testMaxSpasiboAmountOneRuble() {
        List<OrderItem> items = new ArrayList<OrderItem>() {{
            add(createOrderItem(1L, "1", 1));
        }};

        BigDecimal maxAmount = PaymentPartitionCalculator.calculatePartitionMaxAmountForOrderItems(items);
        Assertions.assertEquals(new BigDecimal("0"), maxAmount);
    }

    @Test
    public void testMaxSpasiboAmountZero() {
        List<OrderItem> items = new ArrayList<>();

        BigDecimal maxAmount = PaymentPartitionCalculator.calculatePartitionMaxAmountForOrderItems(items);
        Assertions.assertEquals(new BigDecimal("0"), maxAmount);
    }

    @Test
    public void testCheckSpasiboMaxAmountTooBig() {
        Assertions.assertThrows(PaymentException.class, () -> {
            List<OrderItem> items = new ArrayList<OrderItem>() {{
                add(createOrderItem(1L, "10", 2));
                add(createOrderItem(2L, "20", 1));
                add(createOrderItem(2L, "20", 1));
            }};

            PaymentPartitionCalculator.checkPartitionAmount(items, new BigDecimal("58.00"));
        });
    }

    @Test
    public void testCheckSpasiboMaxAmountTooSmall() {
        Assertions.assertThrows(PaymentException.class, () -> {
            List<OrderItem> items = new ArrayList<OrderItem>() {{
                add(createOrderItem(1L, "10", 2));
                add(createOrderItem(2L, "20", 1));
                add(createOrderItem(2L, "20", 1));
            }};

            PaymentPartitionCalculator.checkPartitionAmount(items, BigDecimal.ZERO);
        });
    }

    @Test
    public void testCanUsePaymentAgentsPartitioning() {
        assertTrue(PaymentPartitionCalculator.canUsePaymentAgentsPartitioning(newArrayList(
                createOrderItem(1L, "1", 2),
                createOrderItem(2L, "2", 1),
                createOrderItem(3L, "0.25", 8)
        )));

        assertFalse(PaymentPartitionCalculator.canUsePaymentAgentsPartitioning(newArrayList(
                createOrderItem(1L, "1", 2),
                createOrderItem(2L, "2", 1),
                createOrderItem(3L, "0.25", 8),
                createOrderItem(4L, "0.3", 6)
        )));

        assertFalse(PaymentPartitionCalculator.canUsePaymentAgentsPartitioning(newArrayList(
                createOrderItem(1L, "1", 2),
                createOrderItem(2L, null, Integer.valueOf(1)),
                createOrderItem(3L, "0.25", 8)
        )));

        assertFalse(PaymentPartitionCalculator.canUsePaymentAgentsPartitioning(newArrayList(
                createOrderItem(1L, "1", 2),
                createOrderItem(2L, "2", 1),
                createOrderItem(3L, BigDecimal.TEN, null)
        )));
    }


    @Test
    public void testCheckSpasiboMaxAmountOk() {
        List<OrderItem> items = new ArrayList<OrderItem>() {{
            add(createOrderItem(1L, "10", 2));
            add(createOrderItem(2L, "20", 1));
            add(createOrderItem(2L, "20", 1));
        }};

        PaymentPartitionCalculator.checkPartitionAmount(items, new BigDecimal("50.00"));
    }

    @Test
    public void testCheckSpasiboMaxAmountOkExactly() {
        List<OrderItem> items = new ArrayList<OrderItem>() {{
            add(createOrderItem(1L, "10", 2));
            add(createOrderItem(2L, "20", 1));
            add(createOrderItem(2L, "20", 1));
        }};

        PaymentPartitionCalculator.checkPartitionAmount(items, new BigDecimal("57.00"));
    }

    @Test
    public void testPartitionReduction() {
        ReceiptItem baseItem = createReceiptItem(1, 1, new BigDecimal(30));
        addPartition(baseItem);
        baseItem.setCount(3);

        ReceiptItem reductionItem = createReceiptItem(1, 1, new BigDecimal(10));
        addPartition(reductionItem);
        reductionItem.setCount(1);

        List<PaymentPartition> reducedPartitions =
                calculator.calculatePaymentPartitionAfterReduction(singletonList(baseItem),
                        singletonList(reductionItem));
        assertThat(reducedPartitions, hasSize(2));

        BigDecimal newTotal = reducedPartitions.stream()
                .map(PaymentPartition::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal spasiboTotal =
                reducedPartitions.stream().filter(p -> p.getPaymentAgent() == PaymentAgent.SBER_SPASIBO).findAny().
                        get().getAmount();
        BigDecimal yandexTotal =
                reducedPartitions.stream().filter(p -> p.getPaymentAgent() == PaymentAgent.DEFAULT).findAny()
                        .get().getAmount();

        assertThat(yandexTotal, equalTo(new BigDecimal("6.67")));
        assertThat(spasiboTotal, equalTo(new BigDecimal("13.33")));
        assertThat(newTotal, equalTo(new BigDecimal("20.00")));
    }

    @Test
    public void testPartitionReductionWhenReductionItemsHaveNoPartitions() {
        ReceiptItem remainItem = createReceiptItemWithPartitions(1, 1, new BigDecimal(30));
        ReceiptItem reductionItem = createReceiptItem(2, 2, new BigDecimal(40));
        List<ReceiptItem> paymentItems = List.of(remainItem, reductionItem);
        List<ReceiptItem> reductionItems = List.of(reductionItem);

        List<PaymentPartition> reducedPartitions = calculator.calculatePaymentPartitionAfterReduction(paymentItems,
                reductionItems);
        assertThat(reducedPartitions, hasSize(2));

        BigDecimal newTotal = reducedPartitions.stream()
                .map(PaymentPartition::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal spasiboTotal =
                reducedPartitions.stream().filter(p -> p.getPaymentAgent() == PaymentAgent.SBER_SPASIBO).findAny()
                        .get().getAmount();
        BigDecimal yandexTotal =
                reducedPartitions.stream().filter(p -> p.getPaymentAgent() == PaymentAgent.DEFAULT).findAny()
                        .get().getAmount();

        assertThat(yandexTotal, equalTo(new BigDecimal("10.00")));
        assertThat(spasiboTotal, equalTo(new BigDecimal("20.00")));
        assertThat(newTotal, equalTo(new BigDecimal("30.00")));
    }

    @Test
    public void testSubtractReceipts() {
        List<ReceiptItem> paymentItems = new ArrayList<>();
        paymentItems.add(addPartition(createReceiptItem(1, 1, new BigDecimal(20), 1)));
        paymentItems.add(addPartition(createReceiptItem(1, 2, new BigDecimal(30), 1)));
        paymentItems.add(addPartition(createReceiptItem(1, 3, new BigDecimal(40), 2)));
        paymentItems.add(addPartition(createReceiptItem(2, 4, new BigDecimal(50), 1)));
        paymentItems.add(addPartition(createReceiptItem(2, 5, new BigDecimal(60), 3)));

        List<ReceiptItem> reductionItems = new ArrayList<>();
        reductionItems.add((createReceiptItem(1, 1, new BigDecimal(20), 1)));
        reductionItems.add(addPartition(createReceiptItem(1, 3, new BigDecimal(40), 2)));
        reductionItems.add(addPartition(createReceiptItem(2, 5, new BigDecimal(40), 2)));
        Collection<ReceiptItem> result = calculator.subtractReceiptItems(paymentItems, reductionItems);

        assertThat(result, hasSize(5));
        ReceiptItem item5 = result.stream().filter(item -> item.getItemId() == 5).findAny().get();
        assertThat(item5.getCount(), equalTo(1));
        assertThat(item5.getPrice(), equalTo(new BigDecimal("20.00")));
        assertThat(item5.getAmount(), equalTo(new BigDecimal("20")));
        assertThat(item5.getPartitions(), hasSize(2));

        ReceiptItemPartition sberPartition = item5.getPartitions().stream()
                .filter(p -> p.getPaymentAgent() == PaymentAgent.SBER_SPASIBO)
                .findAny().get();
        assertThat(sberPartition.getAmount(), equalTo(new BigDecimal("13.33")));

        ReceiptItem item3 = result.stream().filter(item -> item.getItemId() == 3).findAny().get();
        assertThat(item3.getCount(), equalTo(0));
        assertThat(item3.getPrice(), equalTo(new BigDecimal("20.00")));
        assertThat(item3.getAmount(), equalTo(new BigDecimal("0")));
    }

    @Test
    public void testOdinaryPartitionTest() {
        Random random = new Random();
        int itemCount = 5 + random.nextInt(45);
        int maxItemAmountMinusTwo = 1 + random.nextInt(999);

        IntStream.range(0, 100).forEach(iter -> {
            List<ReceiptItem> receiptItems = createRandomReceiptItems(itemCount, maxItemAmountMinusTwo);
            BigDecimal total =
                    receiptItems.stream().map(ReceiptItem::getAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
            BigDecimal margin = PaymentPartitionCalculator.calculatePartitionMaxAmountForReceiptItems(receiptItems);
            BigDecimal odinary = margin.divide(new BigDecimal("2"))
                    .setScale(PaymentPartitionCalculator.SCALE, RoundingMode.HALF_EVEN);

            LOG.info("total = {}", total);
            LOG.info("count {} ", receiptItems.size());
            LOG.info("margin = {}", margin);
            LOG.info("odinary = {}", odinary);

            List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
                add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, odinary));
            }};

            calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);

            receiptItems.stream()
                    .map(i -> String.format("amount =%s, card=%s, spasibo=%s",
                            i.getAmount(),
                            i.amountByAgent(PaymentAgent.DEFAULT),
                            i.amountByAgent(PaymentAgent.SBER_SPASIBO))).forEach(LOG::info);

            validateParttitionTotal(receiptItems);
            PaymentPartitionCalculator.assertPartitions(receiptItems);
        });
    }

    @Test
    public void testMarginPartitionTest() {
        Random random = new Random();
        int itemCount = 5 + random.nextInt(45);
        int maxItemAmountMinusTwo = 1 + random.nextInt(999);

        IntStream.range(0, 100).forEach(iter -> {
            List<ReceiptItem> receiptItems = createRandomReceiptItems(itemCount, maxItemAmountMinusTwo);
            BigDecimal total =
                    receiptItems.stream().map(ReceiptItem::getAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
            BigDecimal margin = PaymentPartitionCalculator.calculatePartitionMaxAmountForReceiptItems(receiptItems);

            LOG.info("total = {}", total);
            LOG.info("count {} ", receiptItems.size());
            LOG.info("margin = {}", margin);

            List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
                add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, margin));
            }};

            calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);

            receiptItems.stream()
                    .map(i -> String.format("amount =%s, card=%s, spasibo=%s",
                            i.getAmount(),
                            i.amountByAgent(PaymentAgent.DEFAULT),
                            i.amountByAgent(PaymentAgent.SBER_SPASIBO))).forEach(LOG::info);

            validateParttitionTotal(receiptItems);
            PaymentPartitionCalculator.assertPartitions(receiptItems);
        });
    }

    @Test
    public void testMarginPartitionWithCustomDefaultAgentTest() {
        Random random = new Random();
        int itemCount = 5 + random.nextInt(45);
        int maxItemAmountMinusTwo = 1 + random.nextInt(999);

        IntStream.range(0, 100).forEach(iter -> {
            List<ReceiptItem> receiptItems = createRandomReceiptItems(itemCount, maxItemAmountMinusTwo);
            BigDecimal total =
                    receiptItems.stream().map(ReceiptItem::getAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
            BigDecimal margin = PaymentPartitionCalculator.calculatePartitionMaxAmountForReceiptItems(receiptItems);

            LOG.info("total = {}", total);
            LOG.info("count {} ", receiptItems.size());
            LOG.info("margin = {}", margin);

            List<PaymentPartition> paymentPartitions = new ArrayList<>() {{
                add(new PaymentPartition(PaymentAgent.YANDEX_CASHBACK, margin));
            }};

            calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true, PaymentAgent.WRAPPER_BNPL);

            receiptItems.stream()
                    .map(i -> String.format("amount =%s, card=%s, spasibo=%s",
                            i.getAmount(),
                            i.amountByAgent(PaymentAgent.WRAPPER_BNPL),
                            i.amountByAgent(PaymentAgent.YANDEX_CASHBACK))).forEach(LOG::info);

            validateParttitionTotal(receiptItems);
            PaymentPartitionCalculator.assertPartitions(receiptItems);
        });
    }


    @Test
    void testZeroAmountForDefaultClient() {
        var item = new ReceiptItem();
        item.setReceiptId(0);
        item.setItemId(1L);
        item.setCount(1);
        item.setPrice(BigDecimal.valueOf(183));
        item.setAmount(BigDecimal.valueOf(183));

        var listOfPartitions =
                List.of(new PaymentPartition(PaymentAgent.WRAPPER_BNPL, BigDecimal.valueOf(169)),
                        new PaymentPartition(PaymentAgent.YANDEX_CASHBACK, BigDecimal.valueOf(14)));


        assertDoesNotThrow(() -> PaymentPartitionCalculator.calculatePaymentPartitions(List.of(item),
                listOfPartitions,
                r -> true,
                PaymentAgent.DEFAULT));
    }

    //https://st.yandex-team.ru/MARKETCHECKOUT-11949 - подробное описание зачем - здесь
    @Test
    public void tesPartitionForItemsWithBigDifferenceInPrice() {
        List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(10)));
            add(createReceiptItem(2, 2, new BigDecimal(20)));
            add(createReceiptItem(3, 3, new BigDecimal(30000)));
        }};

        List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
            add(new PaymentPartition(PaymentAgent.SBER_SPASIBO, new BigDecimal("5")));
        }};

        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);

        BigDecimal totalSpasibo = receiptItems.stream()
                .map(i -> i.amountByAgent(PaymentAgent.SBER_SPASIBO))
                .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

        assertThat(totalSpasibo, equalTo(new BigDecimal(5)));
    }

    @Test
    public void testPartitionZeroAmount() {
        List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(30000)));
        }};

        List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
            add(new PaymentPartition(PaymentAgent.YANDEX_CASHBACK, new BigDecimal("0.00")));
        }};

        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);

        ReceiptItem item = receiptItems.stream().filter(i -> 1 == i.getOrderId()).findFirst().orElse(null);
        assertThat(item.amountByAgent(PaymentAgent.YANDEX_CASHBACK), nullValue());
        assertThat(item.amountByAgent(PaymentAgent.DEFAULT), equalTo(new BigDecimal(30000)));
    }

    @Test
    public void testEdaPartition() {
        List<Order> orders = new ArrayList<>() {{
            add(createOrderForMarkup(() -> {
                Order order = new Order();
                order.setId(1L);
                List<OrderItem> items = new ArrayList<>();

                LongStream.of(1L, 2L).forEach(id -> {
                    createOrderItem(items, id);
                });
                items.forEach(item -> item.setBalanceOrderId(order.getId() + "-allOrderItems"));

                order.setItems(items);
                return order;
            }));


            add(createOrderForMarkup(() -> {
                Order order = new Order();
                order.setId(2L);
                List<OrderItem> items = new ArrayList<>();

                LongStream.of(3L, 4L).forEach(id -> {
                    createOrderItem(items, id);
                });

                order.setItems(items);
                return order;
            }));
        }};

        List<ReceiptItem> receiptItems = new ArrayList<>() {{
            add(createReceiptItem(1, 1, new BigDecimal(10)));
            add(createReceiptItem(1, 2, new BigDecimal(20)));
            add(createReceiptItem(2, 3, new BigDecimal(30)));
            add(createReceiptItem(2, 4, new BigDecimal(40)));
        }};

        List<PaymentPartition> paymentPartitions = new ArrayList<>() {{
            add(new PaymentPartition(PaymentAgent.YANDEX_CASHBACK, new BigDecimal("10")));
        }};

        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, r -> true);
        BasketMarkup markup = createBasketMarkup(orders, receiptItems);

        Assertions.assertEquals(3, markup.getBasketLinesMarkup().values().size());

        Assertions.assertEquals(BigDecimal.valueOf(27),
                markup.getBasketLinesMarkup().get("1-allOrderItems").getLineMarkup().get("card"));
        Assertions.assertEquals(BigDecimal.valueOf(3),
                markup.getBasketLinesMarkup().get("1-allOrderItems").getLineMarkup().get("yandex_account"));

        Assertions.assertEquals(BigDecimal.valueOf(27),
                markup.getBasketLinesMarkup().get("3").getLineMarkup().get("card"));
        Assertions.assertEquals(BigDecimal.valueOf(3),
                markup.getBasketLinesMarkup().get("3").getLineMarkup().get("yandex_account"));

        Assertions.assertEquals(BigDecimal.valueOf(36),
                markup.getBasketLinesMarkup().get("4").getLineMarkup().get("card"));
        Assertions.assertEquals(BigDecimal.valueOf(4),
                markup.getBasketLinesMarkup().get("4").getLineMarkup().get("yandex_account"));
    }


    @Test
    public void testCreateBasketMarkupWithLimits() {
        List<Order> orders = new ArrayList<Order>() {{
            add(createOrderForMarkup(() -> {
                Order order = new Order();
                order.setId(1L);
                List<OrderItem> items = new ArrayList<>();

                LongStream.of(1L, 2L).forEach(id -> {
                    createOrderItem(items, id);
                });

                order.setItems(items);

                createOrderDelivery(order, 100L, BigDecimal.TEN);
                return order;
            }));


            add(createOrderForMarkup(() -> {
                Order order = new Order();
                order.setId(2L);
                List<OrderItem> items = new ArrayList<>();

                LongStream.of(3L, 4L).forEach(id -> {
                    createOrderItem(items, id);
                });

                order.setItems(items);

                createOrderDelivery(order, 200L, BigDecimal.TEN);
                return order;
            }));
        }};

        List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(10)));
            add(createReceiptItem(1, 2, new BigDecimal(10)));
            add(createReceiptItem(2, 3, new BigDecimal(30)));
            add(createReceiptItem(2, 4, new BigDecimal(40)));

            add(createReceiptItem(1, null, 100L, new BigDecimal(50)));
            add(createReceiptItem(2, null, 200L, new BigDecimal(50)));
        }};

        List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
            add(new PaymentPartition(PaymentAgent.YANDEX_CASHBACK, new BigDecimal("76.00")));
        }};

        PartitionLimits limits = new PartitionLimits();
        limits.addLimitForItem(receiptItems.get(0), PaymentAgent.YANDEX_CASHBACK, new BigDecimal("5"));
        limits.addLimitForItem(receiptItems.get(1), PaymentAgent.YANDEX_CASHBACK, new BigDecimal("7"));
        limits.addLimitForItem(receiptItems.get(2), PaymentAgent.YANDEX_CASHBACK, new BigDecimal("25"));
        limits.addLimitForItem(receiptItems.get(3), PaymentAgent.YANDEX_CASHBACK, new BigDecimal("39"));


        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, ReceiptItem::isOrderItem,
                PaymentAgent.DEFAULT, limits);
        BasketMarkup markup = createBasketMarkup(orders, receiptItems);

        Assertions.assertEquals(6, markup.getBasketLinesMarkup().values().size());
        Assertions.assertEquals(new BigDecimal("190.00"),
                markup.getBasketLinesMarkup()
                        .values().stream()
                        .flatMap(m -> m.getLineMarkup().values().stream())
                        .reduce(BigDecimal::add).orElse(BigDecimal.ONE));

        ReceiptItem item1 = receiptItems.stream().filter(receiptItem -> receiptItem.getItemId() == 1).findAny().get();
        ReceiptItem item2 = receiptItems.stream().filter(receiptItem -> receiptItem.getItemId() == 2).findAny().get();
        ReceiptItem item3 = receiptItems.stream().filter(receiptItem -> receiptItem.getItemId() == 3).findAny().get();
        ReceiptItem item4 = receiptItems.stream().filter(receiptItem -> receiptItem.getItemId() == 4).findAny().get();

        assertEquals(new BigDecimal("5"), item1.amountByAgent(PaymentAgent.YANDEX_CASHBACK));
        assertEquals(new BigDecimal("7"), item2.amountByAgent(PaymentAgent.YANDEX_CASHBACK));
        assertEquals(new BigDecimal("25"), item3.amountByAgent(PaymentAgent.YANDEX_CASHBACK));
        assertEquals(new BigDecimal("39.00"), item4.amountByAgent(PaymentAgent.YANDEX_CASHBACK));
    }


    @Test
    public void testCreateBasketMarkupWithLimits2() {
        List<Order> orders = new ArrayList<Order>() {{
            add(createOrderForMarkup(() -> {
                Order order = new Order();
                order.setId(1L);
                List<OrderItem> items = new ArrayList<>();

                LongStream.of(1L, 2L).forEach(id -> {
                    createOrderItem(items, id);
                });

                order.setItems(items);

                createOrderDelivery(order, 100L, BigDecimal.TEN);
                return order;
            }));


            add(createOrderForMarkup(() -> {
                Order order = new Order();
                order.setId(2L);
                List<OrderItem> items = new ArrayList<>();

                LongStream.of(3L, 4L).forEach(id -> {
                    createOrderItem(items, id);
                });

                order.setItems(items);

                createOrderDelivery(order, 200L, BigDecimal.TEN);
                return order;
            }));
        }};

        List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(10)));
            add(createReceiptItem(1, 2, new BigDecimal(10)));
            add(createReceiptItem(2, 3, new BigDecimal(30)));
            add(createReceiptItem(2, 4, new BigDecimal(40)));

            add(createReceiptItem(1, null, 100L, new BigDecimal(50)));
            add(createReceiptItem(2, null, 200L, new BigDecimal(50)));
        }};

        List<PaymentPartition> paymentPartitions = new ArrayList<PaymentPartition>() {{
            add(new PaymentPartition(PaymentAgent.YANDEX_CASHBACK, new BigDecimal("50.00")));
        }};

        PartitionLimits limits = new PartitionLimits();
        limits.addLimitForItem(receiptItems.get(0), PaymentAgent.YANDEX_CASHBACK, new BigDecimal("5"));
        limits.addLimitForItem(receiptItems.get(1), PaymentAgent.YANDEX_CASHBACK, new BigDecimal("7"));
        limits.addLimitForItem(receiptItems.get(2), PaymentAgent.YANDEX_CASHBACK, new BigDecimal("25"));
        limits.addLimitForItem(receiptItems.get(3), PaymentAgent.YANDEX_CASHBACK, new BigDecimal("39"));


        receiptItems = calculatePaymentPartitions(receiptItems, paymentPartitions, ReceiptItem::isOrderItem,
                PaymentAgent.DEFAULT, limits);
        BasketMarkup markup = createBasketMarkup(orders, receiptItems);

        Assertions.assertEquals(6, markup.getBasketLinesMarkup().values().size());
        Assertions.assertEquals(new BigDecimal("190.00"),
                markup.getBasketLinesMarkup()
                        .values().stream()
                        .flatMap(m -> m.getLineMarkup().values().stream())
                        .reduce(BigDecimal::add).orElse(BigDecimal.ONE));

        ReceiptItem item1 = receiptItems.stream().filter(receiptItem -> receiptItem.getItemId() == 1).findAny().get();
        ReceiptItem item2 = receiptItems.stream().filter(receiptItem -> receiptItem.getItemId() == 2).findAny().get();
        ReceiptItem item3 = receiptItems.stream().filter(receiptItem -> receiptItem.getItemId() == 3).findAny().get();
        ReceiptItem item4 = receiptItems.stream().filter(receiptItem -> receiptItem.getItemId() == 4).findAny().get();

        assertEquals(new BigDecimal("3"), item1.amountByAgent(PaymentAgent.YANDEX_CASHBACK));
        assertEquals(new BigDecimal("5"), item2.amountByAgent(PaymentAgent.YANDEX_CASHBACK));
        assertEquals(new BigDecimal("16"), item3.amountByAgent(PaymentAgent.YANDEX_CASHBACK));
        assertEquals(new BigDecimal("26.00"), item4.amountByAgent(PaymentAgent.YANDEX_CASHBACK));
    }

    @Test
    public void testCalculateSpendLimit() {
        PartitionLimits partitionLimits = new PartitionLimits();
        List<Order> orders = new ArrayList<Order>() {{
            add(createOrderForMarkup(() -> {
                Order order = new Order();
                order.setId(1L);
                List<OrderItem> items = new ArrayList<>();

                LongStream.of(1L, 2L).forEach(id -> {
                    createOrderItemWithCashback(items, id, BigDecimal.valueOf(id));
                });

                order.setItems(items);

                createOrderDelivery(order, 100L, BigDecimal.TEN);
                return order;
            }));


            add(createOrderForMarkup(() -> {
                Order order = new Order();
                order.setId(2L);
                List<OrderItem> items = new ArrayList<>();

                LongStream.of(3L, 4L).forEach(id -> {
                    createOrderItemWithCashback(items, id, BigDecimal.valueOf(id * 10));
                });

                order.setItems(items);

                createOrderDelivery(order, 200L, BigDecimal.TEN);
                return order;
            }));
        }};

        List<ReceiptItem> receiptItems = new ArrayList<ReceiptItem>() {{
            add(createReceiptItem(1, 1, new BigDecimal(10)));
            add(createReceiptItem(1, 2, new BigDecimal(10)));
            add(createReceiptItem(2, 3, new BigDecimal(30)));
            add(createReceiptItem(2, 4, new BigDecimal(40)));

            add(createReceiptItem(1, null, 100L, new BigDecimal(50)));
            add(createReceiptItem(2, null, 200L, new BigDecimal(50)));
        }};

        ReceiptItem item1 = receiptItems.stream().filter(receiptItem -> receiptItem.getItemId() == 1).findAny().get();
        ReceiptItem item2 = receiptItems.stream().filter(receiptItem -> receiptItem.getItemId() == 2).findAny().get();
        ReceiptItem item3 = receiptItems.stream().filter(receiptItem -> receiptItem.getItemId() == 3).findAny().get();
        ReceiptItem item4 = receiptItems.stream().filter(receiptItem -> receiptItem.getItemId() == 4).findAny().get();

        PaymentPartitionCalculator.calculateCashbackSpendLimit(orders, new BigDecimal("1000.00"), partitionLimits,
                receiptItems);

        assertEquals(partitionLimits.getLimit(item1, PaymentAgent.YANDEX_CASHBACK), new BigDecimal("1"));
        assertEquals(partitionLimits.getLimit(item2, PaymentAgent.YANDEX_CASHBACK), new BigDecimal("4"));
        assertEquals(partitionLimits.getLimit(item3, PaymentAgent.YANDEX_CASHBACK), new BigDecimal("90"));
        assertEquals(partitionLimits.getLimit(item4, PaymentAgent.YANDEX_CASHBACK), new BigDecimal("160"));
    }

    private List<ReceiptItem> createRandomReceiptItems(int itemsCount, int maxAmountMinusTwo) {
        Random random = new Random();
        List<ReceiptItem> receiptItems = new ArrayList<>();
        IntStream.range(0, itemsCount).forEach(i -> {
            receiptItems.add(createReceiptItem(i, i, new BigDecimal(2 + random.nextInt(maxAmountMinusTwo))));
        });

        return receiptItems;
    }


    private OrderItem createOrderItem(long id, String amount, int count) {
        return createOrderItem(id, new BigDecimal(amount), count);
    }

    private OrderItem createOrderItem(long id, BigDecimal amount, Integer count) {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(id);
        orderItem.setBuyerPrice(amount);
        orderItem.setCount(count);
        return orderItem;
    }

    private Delivery createOrderDelivery(Order order, Long id, BigDecimal price) {
        Delivery delivery = new Delivery();
        delivery.setBalanceOrderId("d" + id);
        delivery.setPrice(price);
        order.setDelivery(delivery);
        return delivery;
    }

    private OrderItem createOrderItem(List<OrderItem> items, long id) {
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setFeedOfferId(new FeedOfferId("" + id, 1L));
        item.setBalanceOrderId("" + id);
        items.add(item);
        return item;
    }

    private OrderItem createOrderItemWithCashback(List<OrderItem> items, long id, BigDecimal spendLimit) {
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setFeedOfferId(new FeedOfferId("" + id, 1L));
        item.setBalanceOrderId("" + id);
        item.setCount((int) id);
        ItemPromo cashbackPromo = new ItemPromo.ItemPromoBuilder().cashbackSpendLimit(spendLimit).build();
        item.getPromos().add(cashbackPromo);
        items.add(item);
        return item;
    }

    private Order createOrderForMarkup(Supplier<Order> orderSupplier) {
        return orderSupplier.get();
    }


    private void validateParttitionTotal(List<ReceiptItem> receiptItems) {
        BigDecimal total =
                receiptItems.stream()
                        .map(ReceiptItem::getAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

        BigDecimal totalByPartitions
                = receiptItems.stream()
                .flatMap(i -> i.getPartitions().stream()
                        .map(ReceiptItemPartition::getAmount))
                .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

        Assertions.assertEquals(total.setScale(totalByPartitions.scale(), RoundingMode.HALF_EVEN), totalByPartitions);
    }

    private ReceiptItem createReceiptItem(long orderid, long itemId, BigDecimal amount) {
        return createReceiptItem(orderid, itemId, null, amount);
    }

    private ReceiptItem createReceiptItem(long orderid, long itemId, BigDecimal amount, int count) {
        ReceiptItem receiptItem = createReceiptItem(orderid, itemId, null, amount);
        receiptItem.setCount(count);
        receiptItem.setPrice(receiptItem.getAmount().divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_EVEN));
        return receiptItem;
    }

    private ReceiptItem createReceiptItem(long orderid, Long itemId, Long deliveryId, BigDecimal amount) {
        ReceiptItem receiptItem = new ReceiptItem();
        receiptItem.setOrderId(orderid);
        receiptItem.setItemId(itemId);
        receiptItem.setAmount(amount);
        receiptItem.setDeliveryId(deliveryId);
        return receiptItem;
    }


    private ReceiptItem createReceiptItemWithPartitions(long orderId, long itemId, BigDecimal amount) {
        ReceiptItem item = createReceiptItem(orderId, itemId, null, amount);
        addPartition(item);
        return item;
    }

    private ReceiptItem addPartition(ReceiptItem item) {
        item.addPartition(new ReceiptItemPartition(PaymentAgent.DEFAULT,
                item.getAmount().divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_EVEN)));
        item.addPartition(new ReceiptItemPartition(PaymentAgent.SBER_SPASIBO,
                item.getAmount().multiply(BigDecimal.valueOf(2)).divide(BigDecimal.valueOf(3), 2,
                        RoundingMode.HALF_EVEN)));
        return item;
    }
}
