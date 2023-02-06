package ru.yandex.market.checkout.checkouter.receipt;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.Iterables;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentAgent;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.storage.payment.RefundWritingDao;
import ru.yandex.market.checkout.checkouter.storage.receipt.ReceiptDao;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.RefundProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.json.Names.History.RECEIPT;
import static ru.yandex.market.checkout.checkouter.receipt.ReceiptBuilder.receiptTemplate;

public class ReceiptDaoTest extends AbstractWebTestBase {

    private static final long REFUND_ID = 444;
    private static final long PAYMENT_ID = 333;

    private static final String PAYLOAD = "{\"field\": 45}";

    @Autowired
    private ReceiptDao receiptDao;
    @Autowired
    private DSLContext dsl;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private RefundWritingDao refundWritingDao;

    private Receipt receipt;
    private Payment createdPayment = null;

    public static Stream<Arguments> parameterizedTestData() {
        Order longEnglishTitleOrder = OrderProvider.getBlueOrder();
        OrderItem orderItem = OrderItemProvider.getOrderItem();
        orderItem.setId(123456L);
        orderItem.setOfferName(
                "qazwsxedcrfvtgbyhnujmikloQAZWSXEqazwsxedcrfvtgbyh" +
                        "nujmikloQAZWSXEDCRqazwsxedcrfvtgbyhnujmiklo" +
                        "QAZWSXEDCRFVTGBYHNUJMIKLOqazwsxedcrfvtgbyhnujmikloQAZW" +
                        "SXEDCRFVTGBYHNUJMIKLOFVTGBYHNUJMIKLO" +
                        "qazwsxedcrfvtgbyhnujmikloQAZWSXEDCRFVTGBYHNUJMIKLODCRFVTGBYHNUJ" +
                        "MIKLOqazwsxedcrfvtgbyhnujmikloQAZWSXEDCRFVTGBYHNUJMIKLO"
        );
        longEnglishTitleOrder.addItem(orderItem);
        Receipt longEnglishTitle = createReceipt(longEnglishTitleOrder);

        Order longUtfTitleOrder = OrderProvider.getBlueOrder();
        orderItem = OrderItemProvider.getOrderItem();
        orderItem.setId(456789L);
        orderItem.setOfferName(
                "œåΩ∑ß≈´œåΩ∑ß≈´®ƒ√∫©†¥˙˜∆¨œåΩ∑ß≈´œåΩ∑ß≈´®ƒ√∫©†¥˙˜∆¨ˆ≤≥¬œåœåΩ∑ß≈´œåΩ∑ß≈´®ƒ√∫©†¥˙˜∆¨œåΩ∑ß≈´œåΩ∑ß" +
                        "≈´®ƒ√∫©†¥˙˜∆¨ˆ≤≥¬œåΩ∑ß≈´œåΩ∑ß≈´®ƒ√∫©†¥˙˜∆¨œåΩ∑ß≈´œåΩ∑ß≈´®ƒ√∫©†¥˙˜∆¨ˆ≤≥¬øπ®ƒ√∫©†¥˙˜∆¨ˆ≤≥¬øπˆ" +
                        "≤≥¬øπ®ƒ√∫©†¥˙˜∆¨ˆ≤≥¬øπøπ®ƒ√∫©†¥˙˜∆¨ˆ≤≥¬øπˆ≤≥¬øπ®ƒ√∫©†¥˙˜∆¨ˆ≤≥¬øπΩ∑ß≈´œåΩ∑ß≈´®ƒ√∫©†¥˙˜∆¨œåΩ" +
                        "∑ß≈´œåΩ∑ß≈´®ƒ√∫©†¥˙˜∆¨ˆ≤≥¬øπ®ƒ√∫©†¥˙˜∆¨ˆ≤≥¬øπˆ≤≥¬øπ®ƒ√∫©†¥˙˜∆¨ˆ≤≥¬øπøπ®ƒ√∫©†¥˙˜∆¨ˆ≤≥¬øπˆ≤" +
                        "≥¬øπ®ƒ√∫©†¥˙˜∆¨ˆ≤≥¬øπ"
        );
        longUtfTitleOrder.addItem(orderItem);
        Receipt longUtfTitle = createReceipt(longUtfTitleOrder);
        Order standartOrder = OrderProvider.getBlueOrder();
        Iterables.getOnlyElement(standartOrder.getItems()).setId(789123L);
        Receipt standartReceipt = createReceipt(standartOrder);
        return Arrays.asList(new Object[][]{
                {"Standard", standartReceipt},
                {"Long english title 2 items", longEnglishTitle},
                {"Long UTF title 2 items", longUtfTitle}
        }).stream().map(Arguments::of);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldGetReceiptItemsForOrders(boolean useUnnestInReceiptGet) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_UNNEST_IN_RECEIPT_GET, useUnnestInReceiptGet);

        var orderToItems = IntStream.rangeClosed(1, 3).mapToObj(n -> {
            var order = orderCreateHelper.createOrder(BlueParametersProvider.prepaidBlueOrderParameters());
            var payment = orderPayHelper.payForOrder(order);

            assertThat(payment, is(notNullValue()));
            assertThat(payment.getFailReason(), is(nullValue()));
            assertThat(payment.requiresPrintableReceipt(), is(true));
            return order;
        }).collect(Collectors.toUnmodifiableMap(Order::getId, Order::getItems));

        var receiptItems = receiptDao.fetchReceiptsItemForOrderItems(orderToItems.keySet(),
                ReceiptType.INCOME,
                PaymentGoal.ORDER_PREPAY);
        assertThat(receiptItems, not(anEmptyMap()));
        assertThat(receiptItems.keySet(), hasItems(orderToItems.values().stream()
                .flatMap(Collection::stream)
                .map(OrderItem::getId)
                .toArray(Long[]::new)));
    }

    /**
     * Проверить операции чтения/записи.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void readWriteTest(String description, Receipt receipt1) {
        //setup
        this.receipt = receipt1;
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.prepaidBlueOrderParameters());
        receipt.getItems().forEach(i -> i.setOrderId(order.getId()));
        createdPayment = orderPayHelper.payForOrder(order);
        Refund refund = RefundProvider.createRefund();
        refund.setPaymentId(createdPayment.getId());
        refund.setId(REFUND_ID);
        refund.setOrderId(order.getId());
        transactionTemplate.execute(tc -> {
            refundWritingDao.insertRefund(refund, ClientInfo.SYSTEM);
            return null;
        });
        if (receipt.getPaymentId() != null) {
            receipt.setPaymentId(createdPayment.getId());
        }
        //do
        truncateReceipts();
        test(((jdbcTemplate, receipt) -> {
            final long receiptId = receiptDao.insert(receipt);

            final Receipt foundById = receiptDao.findById(receiptId);
            checkFound(receipt, foundById);

            final List<Receipt> foundMatching = receiptDao.findMatching(
                    receiptTemplate().withStatus(ReceiptStatus.NEW).buildBasicReceipt()
            );
            final List<Receipt> filteredOut = receiptDao.findMatching(
                    receiptTemplate().withStatus(ReceiptStatus.FAILED).buildBasicReceipt()
            );

            assertThat(foundMatching, notNullValue());
            assertThat(foundMatching, hasSize(greaterThanOrEqualTo(1)));
            assertThat(foundMatching, contains(equalTo(foundById)));

            assertThat(filteredOut, notNullValue());
            assertThat(filteredOut, not(contains(equalTo(foundById))));

            return null;
        }));
    }

    /**
     * Проверить смену статуса чека.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void updateStatusTest(String description, Receipt receipt1) {
        //setup
        this.receipt = receipt1;
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.prepaidBlueOrderParameters());
        receipt.getItems().forEach(i -> i.setOrderId(order.getId()));
        createdPayment = orderPayHelper.payForOrder(order);
        Refund refund = RefundProvider.createRefund();
        refund.setPaymentId(createdPayment.getId());
        refund.setId(REFUND_ID);
        refund.setOrderId(order.getId());
        transactionTemplate.execute(tc -> {
            refundWritingDao.insertRefund(refund, ClientInfo.SYSTEM);
            return null;
        });
        if (receipt.getPaymentId() != null) {
            receipt.setPaymentId(createdPayment.getId());
        }
        //do
        truncateReceipts();
        test(((jdbcTemplate, receipt) -> {
            final long receiptId = receiptDao.insert(receipt);

            Long paymentId = createdPayment.getId();
            receiptDao.updatePaymentStatus(paymentId, ReceiptStatus.FAILED, ReceiptType.INCOME, null);
            checkStatus(receiptDao.findById(receiptId), ReceiptStatus.FAILED, null);

            receiptDao.updatePaymentStatus(paymentId, ReceiptStatus.PRINTED, ReceiptType.INCOME, PAYLOAD);
            checkStatus(receiptDao.findById(receiptId), ReceiptStatus.PRINTED, PAYLOAD);

            receiptDao.updateRefundStatus(REFUND_ID, ReceiptStatus.FAILED, null);
            checkStatus(receiptDao.findById(receiptId), ReceiptStatus.FAILED, null);

            receiptDao.updateRefundStatus(REFUND_ID, ReceiptStatus.PRINTED, PAYLOAD);
            checkStatus(receiptDao.findById(receiptId), ReceiptStatus.PRINTED, PAYLOAD);

            return null;
        }));
    }

    /**
     * Проверить разбиение айтемов по средствам оплаты
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void updateItemPartitionTest(String description, Receipt receipt1) {
        //setup
        this.receipt = receipt1;
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.prepaidBlueOrderParameters());
        receipt.getItems().forEach(i -> i.setOrderId(order.getId()));
        createdPayment = orderPayHelper.payForOrder(order);
        Refund refund = RefundProvider.createRefund();
        refund.setPaymentId(createdPayment.getId());
        refund.setId(REFUND_ID);
        refund.setOrderId(order.getId());
        transactionTemplate.execute(tc -> {
            refundWritingDao.insertRefund(refund, ClientInfo.SYSTEM);
            return null;
        });
        if (receipt.getPaymentId() != null) {
            receipt.setPaymentId(createdPayment.getId());
        }
        //do

        truncateReceipts();
        test((jdbcTemplate, receipt) -> {
            final long receiptId = receiptDao.insert(receipt);

            receipt.getItems()
                    .forEach(i -> {
                        BigDecimal spasibo = BigDecimal.ONE;
                        i.addPartition(new ReceiptItemPartition(PaymentAgent.SBER_SPASIBO, spasibo));
                        i.addPartition(new ReceiptItemPartition(PaymentAgent.DEFAULT, i.getAmount().subtract(spasibo)));
                    });

            receiptDao.updateItems(receipt);
            Receipt receiptFromDB = receiptDao.findById(receiptId);
            receiptFromDB.getItems()
                    .forEach(i -> assertEquals(
                            BigDecimal.ONE,
                            i.amountByAgent(PaymentAgent.SBER_SPASIBO).setScale(0, BigDecimal.ROUND_HALF_EVEN)
                            )
                    );
            return null;
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void checkGetMissingReceipt(String description, Receipt receipt1) {
        //setup
        this.receipt = receipt1;
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.prepaidBlueOrderParameters());
        receipt.getItems().forEach(i -> i.setOrderId(order.getId()));
        createdPayment = orderPayHelper.payForOrder(order);
        Refund refund = RefundProvider.createRefund();
        refund.setPaymentId(createdPayment.getId());
        refund.setId(REFUND_ID);
        refund.setOrderId(order.getId());
        transactionTemplate.execute(tc -> {
            refundWritingDao.insertRefund(refund, ClientInfo.SYSTEM);
            return null;
        });
        if (receipt.getPaymentId() != null) {
            receipt.setPaymentId(createdPayment.getId());
        }
        //do

        Assertions.assertThrows(ReceiptNotFoundException.class, () -> {
            receiptDao.findById(-1);
        });
    }

    private void checkStatus(final Receipt receipt, final ReceiptStatus status, final String payload) {
        assertThat(receipt, notNullValue());
        assertThat(receipt.getStatus(), equalTo(status));
        assertThat(receipt.getTrustPayload(), equalTo(payload));
    }

    private void test(final BiFunction<JdbcTemplate, Receipt, Void> testCase) {
        final JdbcTemplate jdbcTemplate = getRandomWritableJdbcTemplate();
        transactionTemplate.execute(ts -> {
            testCase.apply(jdbcTemplate, receipt);
            return null;
        });
    }

    private void checkFound(final Receipt original, final Receipt found) {
        assertThat(found, notNullValue());
        assertThat(found.getPaymentId(), equalTo(original.getPaymentId()));
        assertThat(found.getRefundId(), equalTo(original.getRefundId()));
        assertThat(found.getStatus(), equalTo(original.getStatus()));
        assertThat(found.getType(), equalTo(original.getType()));
        assertThat(found.getCreatedAt(), notNullValue());
        assertThat(found.getUpdatedAt(), notNullValue());
        assertThat(found.getStatusUpdatedAt(), notNullValue());

        final List<ReceiptItem> originalItems = original.getItems();
        final List<ReceiptItem> foundItems = found.getItems();

        assertThat(foundItems, notNullValue());
        assertThat(foundItems, hasSize(originalItems.size()));
        originalItems.forEach(oi -> {
            ReceiptItem foundItem = foundItems.stream()
                    .filter(fi -> Objects.equals(fi.getItemTitle(), oi.getItemTitle()))
                    .findAny()
                    .orElse(null);
            checkFoundItem(oi, foundItem);
        });
    }

    private void checkFoundItem(final ReceiptItem orignal, final ReceiptItem found) {
        assertThat(found, notNullValue());
        assertThat(found.getCount(), equalTo(orignal.getCount()));
        assertThat(found.getPrice(), closeTo(orignal.getPrice(), BigDecimal.ZERO));
        assertThat(found.getAmount(), closeTo(orignal.getAmount(), BigDecimal.ZERO));
        assertThat(found.getItemTitle(), equalTo(orignal.getItemTitle()));
        assertThat(found.getItemId(), equalTo(orignal.getItemId()));
        assertThat(found.getItemServiceId(), equalTo(orignal.getItemServiceId()));
        assertThat(found.getDeliveryId(), equalTo(orignal.getDeliveryId()));
    }

    /**
     * Сгенерировать чек.
     */
    private static Receipt createReceipt(Order order) {
        return ReceiptBuilder.newPrintable()
                .withPaymentId(PAYMENT_ID)
                .withRefundId(REFUND_ID)
                .withType(ReceiptType.INCOME)
                .buildFromOrders(Collections.singletonList(order));
    }


    private void truncateReceipts() {
        transactionTemplate.execute(tc -> {
            dsl.truncate(RECEIPT).cascade().execute();
            return null;
        });
    }
}
