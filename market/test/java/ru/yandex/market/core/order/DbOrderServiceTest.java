package ru.yandex.market.core.order;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import one.util.streamex.EntryStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.BuyerType;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentSubmethod;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.order.model.DeliveryBillingType;
import ru.yandex.market.core.order.model.DeliveryRoute;
import ru.yandex.market.core.order.model.MbiOrder;
import ru.yandex.market.core.order.model.MbiOrderBuilder;
import ru.yandex.market.core.order.model.MbiOrderItem;
import ru.yandex.market.core.order.model.MbiOrderItemPromo;
import ru.yandex.market.core.order.model.MbiOrderPromo;
import ru.yandex.market.core.order.model.MbiOrderStatus;
import ru.yandex.market.core.order.model.OrderBillingStatus;
import ru.yandex.market.core.order.model.OrderDelivery;
import ru.yandex.market.core.order.model.OrderDeliveryBuilder;
import ru.yandex.market.core.order.model.OrderDeliveryCosts;
import ru.yandex.market.core.order.model.OrderDeliveryDeclaredValue;
import ru.yandex.market.core.order.model.OrderDeliveryType;
import ru.yandex.market.core.order.model.OrderInfoStatus;
import ru.yandex.market.core.order.model.OrderItemExtendedStatus;
import ru.yandex.market.core.order.model.OrderItemForBilling;
import ru.yandex.market.core.order.model.Parcel;
import ru.yandex.market.core.order.model.WeightAndSize;
import ru.yandex.market.core.order.payment.OrderTransaction;
import ru.yandex.market.core.order.payment.OrderTransactionStatus;
import ru.yandex.market.core.order.payment.TransactionBankOrder;
import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.core.util.DateTimes;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Map.entry;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.core.order.TestOrderFactory.defaultOrderItem;
import static ru.yandex.market.core.order.TestOrderFactory.defaultOrderPromo;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@DbUnitDataSet(before = {"db/datasource.csv", "db/supplier.csv", "db/deliveryTypes.csv"})
class DbOrderServiceTest extends FunctionalTest {

    private static final int ORDER_ID = 12345;

    private static final Date DELIVERY_TRANTIME = java.sql.Date.valueOf(LocalDate.of(2018, 11, 2));

    private static final Parcel PARCEL = Parcel.builder()
            .withParcelId(123L)
            .withWeightAndSize(
                    WeightAndSize.builder()
                            .withWeight(1000L)
                            .withHeight(120L)
                            .withWidth(130L)
                            .withDepth(140L)
                            .build()
            )
            .build();

    private static final OrderDeliveryCosts COSTS = OrderDeliveryCosts.builder()
            .setDeliveryCost(BigDecimal.valueOf(1000))
            .setInsuranceCost(BigDecimal.valueOf(200))
            .setReturnCost(BigDecimal.valueOf(300))
            .setFinalCost(BigDecimal.valueOf(4000))
            .setChargeableWeight(5_000d)
            .build();

    private static final OrderDelivery ORDER_DELIVERY = new OrderDeliveryBuilder()
            .setRoute(new DeliveryRoute(1L, 2L))
            .setParcels(singletonList(PARCEL))
            .setDeclaredValue(new OrderDeliveryDeclaredValue(100, BigDecimal.TEN))
            .setBillingType(DeliveryBillingType.SPENT)
            .setCosts(COSTS)
            .setTrantime(DELIVERY_TRANTIME)
            .build();

    @Autowired
    private DbOrderService orderService;

    @Test
    @DbUnitDataSet(after = "saveOrderWithCoupon.after.csv")
    void saveOrderWithCoupon() {
        orderService.storeOrder(buildOrder());
    }

    @Test
    @DbUnitDataSet(after = "saveOrderWithoutCoupon.after.csv")
    void saveOrderWithoutCoupon() {
        orderService.storeOrder(buildOrderWithoutPromo());
    }

    @Test
    @DbUnitDataSet(after = "saveOrderFulfillment.after.csv")
    void saveOrderFulfillment() {
        orderService.storeOrder(buildFulfillmentOrder());
    }

    /**
     * Тест проверяет, что в поле cpa_order.allowed_payment_methods влезают все записи из {@link PaymentMethod}.
     * Если тест упал, значит нам нужно увеличивать возможную длину cpa_order.allowed_payment_methods, прежде чем
     * добавлять новый {@link PaymentMethod}.
     */
    @Test
    void saveMaxLength() {
        orderService.storeOrder(
                getOrderBuilder()
                        .setAllowedPaymentMethods(
                                Arrays.stream(PaymentMethod.values())
                                        .filter(it -> it != PaymentMethod.UNKNOWN)
                                        .collect(Collectors.toList())
                        )
                        .build()
        );
    }

    @Test
    @DbUnitDataSet(before = "DbOrderServiceTest.before.csv")
    void getOrderWithDeliveryBuyerPrice() {
        MbiOrder order = orderService.getOrder(2);
        assertThat(order.getDelivery(), equalTo(BigDecimal.ONE));
        assertThat(order.getDeliveryBuyerPrice(), equalTo(BigDecimal.TEN));
    }

    @Test
    @DbUnitDataSet(before = "DbOrderServiceTest.before.csv")
    void getOrderWithAdditionalInfo() {
        LocalDateTime expiryDate = LocalDateTime.of(2017, 1, 4, 16, 30, 0);
        MbiOrder order = orderService.getOrder(2);
        assertEquals(BuyerType.PERSON, order.getBuyerType());
        assertEquals(expiryDate, order.getStatusExpiryDate());
    }

    @Test
    @DisplayName("Проверяем получение cashOnly полей из БД.")
    @DbUnitDataSet(before = "DbOrderServiceTest.before.csv")
    void getCashOnDeliveryProperties() {
        MbiOrder order1 = orderService.getOrder(1L);
        MbiOrder order2 = orderService.getOrder(2L);
        MbiOrder order3 = orderService.getOrder(3L);
        assertThat(order1.isCashOnly(), equalTo(false));
        assertThat(order2.isCashOnly(), equalTo(true));
        assertThat(order3.isCashOnly(), equalTo(false));
        assertThat(order1.getAllowedPaymentMethods(), hasSize(0));
        assertThat(order2.getAllowedPaymentMethods(), contains(PaymentMethod.CASH_ON_DELIVERY));
        assertThat(order3.getAllowedPaymentMethods(), hasSize(14));
    }

    @Test
    @DisplayName("Получаем paymentSubmethod заказа для orderItemForBilling")
    @DbUnitDataSet(before = "DbOrderServiceInstallmentTest.before.csv")
    void whenPaymentSubmethodGivenThenFetchIt() {
        List<OrderItemForBilling> orderItemsForBilling = orderService.getOrderItemsForBilling(Set.of(1L, 3L, 4L));

        assertThat(orderItemsForBilling, hasSize(5));
        assertThat(orderItemsForBilling.stream()
                .map(OrderItemForBilling::getPaymentSubmethod)
                .collect(Collectors.toList()),
                containsInAnyOrder(
                        // проверяем что для одного заказа 3 айтема с нужным типом рассрочки
                        PaymentSubmethod.TINKOFF_INSTALLMENTS_6,
                        PaymentSubmethod.TINKOFF_INSTALLMENTS_6,
                        PaymentSubmethod.TINKOFF_INSTALLMENTS_6,
                        PaymentSubmethod.TINKOFF_INSTALLMENTS_12,
                        PaymentSubmethod.TINKOFF_INSTALLMENTS_24)
        );

        OrderItemForBilling itemWithBatchSize = orderItemsForBilling.stream()
                .filter(oifb -> oifb.getItemId() == 200L)
                .findAny()
                .orElseThrow();

        assertThat(itemWithBatchSize.getBatchSize(), equalTo(5));
    }

    @Test
    @DbUnitDataSet(before = "DbOrderServiceTest.before.csv")
    void getOrderWithNullDeliveryBuyerPrice() {
        MbiOrder order = orderService.getOrder(1);
        assertThat(order.getDelivery(), equalTo(BigDecimal.ONE));
        assertThat(order.getDeliveryBuyerPrice(), is(nullValue()));
    }

    @Test
    @DbUnitDataSet(before = "DbOrderServiceTest.before.csv")
    void testGetOrderItemWithFeedPriceValue() {
        Collection<MbiOrderItem> orderItems = orderService.getOrderItems(3);
        assertThat(orderItems, hasSize(1));
        assertThat(orderItems.iterator().next().getFeedPrice(), equalTo(new BigDecimal(102)));
    }

    @Test
    @DbUnitDataSet(before = "DbOrderServiceTest.before.csv")
    void testGetOrderItemWithNullFeedPriceValue() {
        Collection<MbiOrderItem> orderItems = orderService.getOrderItems(4);
        assertThat(orderItems, hasSize(1));
        assertThat(orderItems.iterator().next().getFeedPrice(), is(nullValue()));
    }

    @Test
    @DbUnitDataSet(before = "DbOrderServiceTest.before.csv")
    void testGetOrderItemWithWarehouseId() {
        Collection<MbiOrderItem> orderItems = orderService.getOrderItems(1);
        assertThat(orderItems, hasSize(3));
        assertThat(
                orderItems.stream().map(MbiOrderItem::getWarehouseId).collect(Collectors.toList()),
                contains(145, 147, 147)
        );
    }

    @Test
    @DbUnitDataSet(before = "DbOrderServiceTest.before.csv")
    void testGetFulfillmentOrderItems() {
        Collection<MbiOrderItem> orderItems =
                orderService.getFulfillmentOrderItems(new FulfillmentOrderFilter(1L,
                        new Date(1400000000000L), new Date(1600000000000L)));

        assertEquals(3, orderItems.size());
        Iterator<MbiOrderItem> iterator = orderItems.iterator();

        assertEquals(200L, (long) iterator.next().getId());
        assertEquals(202L, (long) iterator.next().getId());
        assertEquals(203L, (long) iterator.next().getId());
    }

    @Test
    @DbUnitDataSet(before = "DbOrderServiceTest.before.csv")
    void testGetAllFulfillmentOrderItems() {
        Collection<MbiOrderItem> orderItems =
                orderService.getFulfillmentOrderItems(new FulfillmentOrderFilter(null, new Date(1400000000000L),
                        new Date(1600000000000L)));

        assertEquals(4, orderItems.size());
        Iterator<MbiOrderItem> orderItemIterator = orderItems.iterator();
        assertEquals(200L, (long) orderItemIterator.next().getId());
        assertEquals(201L, (long) orderItemIterator.next().getId());
        assertEquals(202L, (long) orderItemIterator.next().getId());
        assertEquals(203L, (long) orderItemIterator.next().getId());
    }

    @Test
    @DbUnitDataSet(after = "storeOrderTransactionInsert.after.csv")
    void storeTransactionInsert() {
        orderService.storeOrderTransaction(createDummyTransaction(100L));
    }

    /**
     * Проверяем, что {@code storeOrderTransaction} корректно обновляет транзакции.
     * При обновлении изменяется статус, но не меняется сумма.
     */
    @Test
    @DbUnitDataSet(after = "storeOrderTransactionUpdate.after.csv")
    void storeTransactionUpdate() {
        OrderTransaction transaction = createDummyTransaction(123L);
        // создаём транзакцию
        orderService.storeOrderTransaction(transaction);
        transaction.setStatus(OrderTransactionStatus.DONE);
        transaction.setTrantime(Instant.now());
        transaction.setBalanceOrderId("balanceOrderId2");
        transaction.setPaymentId(123L);
        // обновляем статус
        orderService.storeOrderTransaction(transaction);
    }

    /**
     * При явном указании суммы, происходит обновление в хранилище.
     */
    @Test
    @DbUnitDataSet(after = "storeOrderTransactionUpdate_explicitSumSpecified.after.csv")
    void test_storeTransaction_when_sumSpecified_should_updateAtStorage() {
        OrderTransaction transaction = createDummyTransaction(null);
        transaction.setRefundId(321L);
        // создаём транзакцию
        orderService.storeOrderTransaction(transaction);
        transaction.setStatus(OrderTransactionStatus.DONE);
        transaction.setTrantime(Instant.now());
        transaction.setBalanceOrderId("balanceOrderId2");
        transaction.setSum(300L);
        transaction.setRefundId(321L);
        // обновляем статус
        orderService.storeOrderTransaction(transaction);
    }

    @Test
    @DbUnitDataSet(after = "storeOrderTransactionInsert_trustPaymentId.after.csv")
    void storeTrustIdInsert() {
        OrderTransaction paymentTransaction = createDummyTransaction(null);
        paymentTransaction.setPaymentId(100L);
        paymentTransaction.setType(PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        paymentTransaction.setTrustPaymentId(null);
        OrderTransaction refundTransaction = createDummyTransaction(null);
        refundTransaction.setRefundId(100L);
        refundTransaction.setType(PaymentGoal.ORDER_ACCOUNT_PAYMENT);
        refundTransaction.setTrustPaymentId(null);
        orderService.storeOrderTransaction(paymentTransaction);
        orderService.storeOrderTransaction(refundTransaction);
    }

    /**
     * Проверяем, что {@code storeOrderTransaction} обновляет только транзакцию нужного типа, не трогая вторую.
     * Например, субсидия и пользовательская оплата.
     * При обновлении изменяется статус, но не меняется сумма.
     */
    @Test
    @DbUnitDataSet(after = "storeOrderTransactionUpdateMulti.after.csv")
    void storeTransactionUpdateMulti() {
        OrderTransaction transaction = createDummyTransaction(123L);
        // создаём транзакцию
        orderService.storeOrderTransaction(transaction);
        // создаём субсидию на 1р
        transaction.setSum(1L);
        transaction.setType(PaymentGoal.SUBSIDY);
        transaction.setPaymentId(321L);
        transaction.setTrustPaymentId("subsidy");
        orderService.storeOrderTransaction(transaction);

        // обновляем пользоватукую оплату
        transaction.setType(PaymentGoal.ORDER_PREPAY);
        transaction.setPaymentId(123L);
        transaction.setSum(200L);
        transaction.setStatus(OrderTransactionStatus.DONE);
        transaction.setTrantime(Instant.now());
        transaction.setBalanceOrderId("balanceOrderId2");
        transaction.setTrustPaymentId("trust");
        // обновляем статус
        orderService.storeOrderTransaction(transaction);
    }

    OrderTransaction createDummyTransaction(Long paymentId) {
        return new OrderTransaction(
                paymentId,
                null,
                1L,
                "balanceOrderId",
                null,
                1L,
                1L,
                "trust",
                null,
                200L,
                Currency.RUR,
                OrderTransactionStatus.NEW,
                Instant.now(),
                Instant.now(),
                Instant.now(),
                "asdf",
                1L,
                ClientRole.USER,
                PaymentGoal.ORDER_PREPAY,
                null,
                null,
                false,
                null,
                10L,
                "100TTT"
        );
    }

    /**
     * Тест на логику {@link OrderService#updateOrderItems(OrderItemsUpdateModification)}.
     * <p>
     * Изменения в таблице MARKET_BILLING.CPA_ORDER проверяются в джава-коде, изменения в таблицах:
     * <ul>
     * <li>MARKET_BILLING.CPA_ORDER_ITEM</li>
     * <li>MARKET_BILLING.CPA_ORDER_ITEM_PROMO</li>
     * <li>MARKET_BILLING.CPA_ORDER_PROMO</li>
     * </ul>
     * через updateOrderItems.after.csv.
     */
    @Test
    @DbUnitDataSet(
            before = "updateOrderItems.before.csv",
            after = "updateOrderItems.after.csv"
    )
    void updateOrderItemsTest() {
        MbiOrderPromo orderPromo =
                new MbiOrderPromo.Builder()
                        .setId(1L)
                        .setOrderId(12L)
                        .setPromoType(PromoType.MARKET_DEAL)
                        .setSubsidy(BigDecimal.TEN)
                        .setMarketPromoId("market_promo_id_2")
                        .setAnaplanId("anaplan_id_2")
                        .setShopPromoId("shop_promo_id_2")
                        .build();

        MbiOrderPromo orderPromo1 =
                new MbiOrderPromo.Builder()
                        .setId(1L)
                        .setOrderId(12L)
                        .setPromoType(PromoType.MARKET_DEAL)
                        .setSubsidy(BigDecimal.valueOf(7))
                        .setMarketPromoId("market_promo_id_2")
                        .setAnaplanId("anaplan_id_2")
                        .setShopPromoId("shop_promo_id_2")
                        .build();
        MbiOrderItemPromo itemPromo1 = new MbiOrderItemPromo.Builder()
                .fromOrderPromo(orderPromo1)
                .build();
        MbiOrderPromo orderPromo2 =
                new MbiOrderPromo.Builder()
                        .setId(1L)
                        .setOrderId(12L)
                        .setPromoType(PromoType.MARKET_DEAL)
                        .setSubsidy(BigDecimal.valueOf(3))
                        .setMarketPromoId("market_promo_id_2")
                        .setAnaplanId("anaplan_id_2")
                        .setShopPromoId("shop_promo_id_2")
                        .build();
        MbiOrderItemPromo itemPromo2 = new MbiOrderItemPromo.Builder()
                .fromOrderPromo(orderPromo2)
                .build();

        MbiOrderItem orderItem =
                MbiOrderItem.builder().setId(1L).setOrderId(12L).setFeedId(12L).setOfferId("offer1").setOfferName(
                        "offerName1").setModelId(1)
                        .setWareMd5("qwerty").setCategoryId(1).setFeedCategoryId(BigInteger.ZERO).setShowUid("")
                        .setPrice(BigDecimal.ONE).setBuyerPrice(BigDecimal.TEN)
                        .setFeedPrice(new BigDecimal(11)).setCount(5).setNormFee(BigDecimal.TEN)
                        .setIntFee(15).setShopFee(BigDecimal.valueOf(20)).setNetFeeUE(BigDecimal.valueOf(25))
                        .setPromos(ImmutableList.of(itemPromo1, itemPromo2))
                        .build();

        MbiOrderItem item1 =
                MbiOrderItem.builder().setId(1L).setOrderId(12L).setFeedId(12L).setOfferId("offer1").setOfferName(
                        "offerName1").setModelId(1)
                        .setWareMd5("qwerty").setCategoryId(1).setFeedCategoryId(BigInteger.ZERO).setShowUid("")
                        .setPrice(BigDecimal.ONE).setBuyerPrice(BigDecimal.TEN)
                        .setFeedPrice(new BigDecimal(11)).setCount(5).setNormFee(BigDecimal.TEN)
                        .setIntFee(15).setShopFee(BigDecimal.valueOf(20)).setNetFeeUE(BigDecimal.valueOf(25))
                        .setPromos(emptyList())
                        .build();

        MbiOrderItem item2 =
                MbiOrderItem.builder().setId(2L).setOrderId(12L).setFeedId(12L).setOfferId("offer2").setOfferName(
                        "offerName2").setModelId(1)
                        .setWareMd5("qwerty").setCategoryId(1).setFeedCategoryId(BigInteger.ZERO).setShowUid("")
                        .setPrice(BigDecimal.ONE).setBuyerPrice(BigDecimal.TEN)
                        .setFeedPrice(new BigDecimal(12)).setCount(3).setNormFee(BigDecimal.valueOf(40))
                        .setIntFee(45).setShopFee(BigDecimal.valueOf(50)).setNetFeeUE(BigDecimal.valueOf(55))
                        .setPromos(emptyList())
                        .setCis("cis1 cis2")
                        .build();

        MbiOrderItem deletedItem = MbiOrderItem.builder().setId(4L).setOrderId(12L).setFeedId(12L).setOfferId(
                "offer4").setOfferName("offerName4").setModelId(1)
                .setWareMd5("qwerty").setCategoryId(1).setFeedCategoryId(BigInteger.ZERO).setShowUid("")
                .setPrice(BigDecimal.ONE).setBuyerPrice(BigDecimal.TEN)
                .setFeedPrice(new BigDecimal(11)).setCount(7).setNormFee(BigDecimal.valueOf(2))
                .setIntFee(3).setShopFee(BigDecimal.valueOf(4)).setNetFeeUE(BigDecimal.valueOf(5))
                .setPromos(emptyList()).setInitialCount(7).setCis("cis3")
                .build();

        OrderItemsUpdateModification orderItemsModification = OrderItemsUpdateModification.builder()
                .withOrderId(12L)
                .withTotal(BigDecimal.valueOf(1000))
                .withUeTotal(BigDecimal.valueOf(18))
                .withFeeSum(BigDecimal.valueOf(15))
                .withFeeCorrect(BigDecimal.valueOf(12))
                .withOrderItemsForUpdate(Arrays.asList(item1, item2))
                .withItemsForDeletion(singletonList(deletedItem))
                .withItemsForPromoUpdate(singletonList(orderItem))
                .withOrderPromos(singletonList(orderPromo))
                .withSubsidyTotal(BigDecimal.valueOf(19))
                .build();

        orderService.updateOrderItems(orderItemsModification);

        MbiOrder updatedOrder = orderService.getOrder(12);

        assertThat(updatedOrder.getTotal(), comparesEqualTo(BigDecimal.valueOf(1000)));
        assertThat(updatedOrder.getFeeSum(), comparesEqualTo(BigDecimal.valueOf(15)));
        assertThat(updatedOrder.getUeTotal(), comparesEqualTo(BigDecimal.valueOf(18)));
        assertThat(updatedOrder.getFeeCorrect(), comparesEqualTo(BigDecimal.valueOf(12)));
        assertThat(updatedOrder.getSubsidyTotal(), comparesEqualTo(BigDecimal.valueOf(19)));
    }

    @Test
    void testStoreOrderWithDeclaredValue() {
        orderService.storeOrder(buildOrder());
        MbiOrder mbiOrder = orderService.getOrder(1L);

        assertNotNull(mbiOrder.getOrderDelivery().declaredValue());
        assertEquals(new OrderDeliveryDeclaredValue(100, BigDecimal.TEN), mbiOrder.getOrderDelivery().declaredValue());
    }

    @Test
    @DbUnitDataSet(after = "saveOrderWithAgileCashback.after.csv")
    void saveOrderWithAgileCashback() {
        long shopId = 1L;

        var orderToPromos = Map.ofEntries(
                entry(11L, List.of(
                        defaultOrderPromo(PromoType.CASHBACK, "promo-2").build()
                )),
                entry(12L, List.of(
                        defaultOrderPromo(PromoType.CASHBACK, "promo-1").build(),
                        defaultOrderPromo(PromoType.CASHBACK, "promo-2").build(),
                        defaultOrderPromo(PromoType.MARKET_COIN, "promo-3").build()
                ))
        );

        var orderItemToPromos = Map.ofEntries(
                // айтем c одной промкой с заполненными partnerId, partnerCashbackPromo, marketCashbackPromo
                entry(Pair.of(11L, 111L), List.of(
                        new MbiOrderItemPromo.Builder()
                                .fromOrderPromo(orderToPromos.get(11L).get(0))
                                .setPartnerId(shopId)
                                .setMarketCashbackPercent(BigDecimal.valueOf(3))
                                .setPartnerCashbackPercent(BigDecimal.valueOf(10))
                                .build()
                )),
                // айтем c одной промкой с частично заполненными partnerId, partnerCashbackPromo, marketCashbackPromo
                entry(Pair.of(11L, 112L), List.of(
                        new MbiOrderItemPromo.Builder()
                                .fromOrderPromo(orderToPromos.get(11L).get(0))
                                .setPartnerId(shopId)
                                .build()
                )),
                // айтем c одной промкой с незаполненными partnerId, partnerCashbackPromo, marketCashbackPromo
                entry(Pair.of(11L, 113L), List.of(
                        new MbiOrderItemPromo.Builder()
                                .fromOrderPromo(orderToPromos.get(11L).get(0))
                                .build()
                )),
                // айтем c несколькими промками с разной заполненностью полей
                entry(Pair.of(12L, 124L), List.of(
                        new MbiOrderItemPromo.Builder()
                                .fromOrderPromo(orderToPromos.get(12L).get(0))
                                .setPartnerId(shopId)
                                .setMarketCashbackPercent(BigDecimal.valueOf(1.5))
                                .setPartnerCashbackPercent(BigDecimal.valueOf(3.5))
                                .build(),
                        new MbiOrderItemPromo.Builder()
                                .fromOrderPromo(orderToPromos.get(12L).get(1))
                                .setPartnerId(shopId)
                                .build(),
                        new MbiOrderItemPromo.Builder()
                                .fromOrderPromo(orderToPromos.get(12L).get(2))
                                .build()
                ))
        );

        orderToPromos.forEach((orderId, orderPromos) -> {
            var orderItems = EntryStream.of(orderItemToPromos)
                    .filterKeys(orderItem -> orderItem.getFirst().equals(orderId))
                    .mapKeyValue((orderItem, itemPromos) ->
                            defaultOrderItem(orderId, orderItem.getSecond(), itemPromos).build())
                    .toList();

            orderService.storeOrder(getOrderBuilder()
                    .setId(orderId)
                    .setShopId(shopId)
                    .setPromos(orderPromos)
                    .setItems(orderItems)
                    .build()
            );
        });
    }

    @Test
    @Description("Проверяет изменения новых свойств, добавленных для 'Гибкого кэшбэка', в cpa_order_item_promo")
    @DbUnitDataSet(
            before = "updateOrderItems.agileCashbackPromosTest.before.csv",
            after = "updateOrderItems.agileCashbackPromosTest.after.csv"
    )
    void updateOrderItemPromosTest_agileCashbackPropsModification() {
        var shopId = 1L;
        var oderId = 12L;
        var orderPromos = List.of(
                defaultOrderPromo(PromoType.CASHBACK, "promo-1").build(),
                defaultOrderPromo(PromoType.CASHBACK, "promo-2").build()
        );
        var itemPromos = List.of(
                new MbiOrderItemPromo.Builder()
                        .fromOrderPromo(orderPromos.get(0))
                        .setPartnerId(shopId)
                        .setMarketCashbackPercent(BigDecimal.valueOf(1.5))
                        .setPartnerCashbackPercent(BigDecimal.valueOf(3.5))
                        .build(),
                new MbiOrderItemPromo.Builder()
                        .fromOrderPromo(orderPromos.get(1))
                        .build()
        );

        OrderItemsUpdateModification orderItemsModification = OrderItemsUpdateModification.builder()
                .withOrderId(oderId)
                .withTotal(BigDecimal.valueOf(1000))
                .withUeTotal(BigDecimal.valueOf(18))
                .withFeeSum(BigDecimal.valueOf(15))
                .withFeeCorrect(BigDecimal.valueOf(12))
                .withSubsidyTotal(BigDecimal.valueOf(19))
                .withItemsForPromoUpdate(singletonList(
                        defaultOrderItem(oderId, 121L, itemPromos).build()
                ))
                .withItemsForDeletion(emptyList())
                .withOrderItemsForUpdate(emptyList())
                .build();

        orderService.updateOrderItems(orderItemsModification);
    }

    private static MbiOrder buildOrder() {
        return getOrderBuilder()
                .build();
    }

    private static MbiOrderBuilder getOrderBuilder() {
        return new MbiOrderBuilder()
                .setId(1)
                .setColor(BLUE)
                .setStatus(MbiOrderStatus.PROCESSING)
                .setCampaignId(1)
                .setShopId(1)
                .setTotal(BigDecimal.valueOf(3000))
                .setSubsidiesTotal(BigDecimal.valueOf(300))
                .setCurrency(Currency.RUR)
                .setCreationDate(new Date())
                .setTrantime(new Date())
                .setOrderDelivery(
                        new OrderDeliveryBuilder()
                                .setBillingType(DeliveryBillingType.SPENT)
                                .setRoute(new DeliveryRoute(1L, 2L))
                                .setDeclaredValue(new OrderDeliveryDeclaredValue(100, BigDecimal.TEN))
                                .build()
                )
                .setSubstatus(null)
                .setBillingStatus(OrderBillingStatus.BILLED)
                .setPaymentMethod(null)
                .setPaymentType(null)
                .setDelivery(BigDecimal.ZERO)
                .setFeeSum(BigDecimal.ZERO)
                .setFeeCorrect(BigDecimal.ZERO)
                .setBuyerType(BuyerType.PERSON)
                .setPromos(
                        singletonList(
                                new MbiOrderPromo.Builder()
                                        .setPromoType(PromoType.MARKET_COUPON)
                                        .setSubsidy(BigDecimal.valueOf(300))
                                        .setClientId(11223344L)
                                        .setPromoCode("best_deal_AF")
                                        .build()
                        )
                )
                .setItems(Arrays.asList(
                        MbiOrderItem.builder().setId(1L).setOrderId(1L).setFeedId(1L).setOfferId("1")
                                .setPrice(BigDecimal.valueOf(1000))
                                .setSubsidy(BigDecimal.valueOf(100))
                                .setCount(1)
                                .setOfferName("offerName").setWareMd5("waremd5").setCategoryId(1)
                                .setShowUid("nvmShowUid")
                                .setBuyerPrice(BigDecimal.TEN)
                                .setIntFee(1)
                                .setNormFee(BigDecimal.ONE)
                                .setNetFeeUE(BigDecimal.ONE)
                                .setShopFee(BigDecimal.ONE)
                                .setPromos(ImmutableList.of(
                                        new MbiOrderItemPromo.Builder()
                                                .setPromoType(PromoType.MARKET_COUPON)
                                                .setSubsidy(BigDecimal.valueOf(100))
                                                .setCashbackAccrualAmount(25000L)
                                                .build()
                                ))
                                .build(),
                        MbiOrderItem.builder().setId(2L).setOrderId(1L).setFeedId(1L).setOfferId("2")
                                .setPrice(BigDecimal.valueOf(2000))
                                .setSubsidy(BigDecimal.valueOf(200))
                                .setCount(1)
                                .setOfferName("offerName").setWareMd5("waremd5").setCategoryId(1)
                                .setShowUid("nvmShowUid")
                                .setBuyerPrice(BigDecimal.TEN)
                                .setIntFee(1)
                                .setNormFee(BigDecimal.ONE)
                                .setNetFeeUE(BigDecimal.ONE)
                                .setShopFee(BigDecimal.ONE)
                                .setPromos(ImmutableList.of(
                                        new MbiOrderItemPromo.Builder()
                                                .setPromoType(PromoType.MARKET_COUPON)
                                                .setSubsidy(BigDecimal.valueOf(200))
                                                .setBuyerDiscount(10000L)
                                                .build()
                                ))
                                .setPartnerPrice(BigDecimal.TEN)
                                .build()
                ));
    }

    private static MbiOrder buildFulfillmentOrder() {
        return new MbiOrderBuilder()
                .setId(1)
                .setColor(BLUE)
                .setStatus(MbiOrderStatus.PROCESSING)
                .setCampaignId(1)
                .setShopId(1)
                .setTotal(BigDecimal.valueOf(3000))
                .setCurrency(Currency.RUR)
                .setCreationDate(new Date())
                .setTrantime(new Date())
                .setOrderDelivery(
                        new OrderDeliveryBuilder()
                                .setBillingType(DeliveryBillingType.SPENT)
                                .setRoute(new DeliveryRoute(1L, 2L))
                                .build()
                )
                .setSubstatus(null)
                .setBillingStatus(OrderBillingStatus.BILLED)
                .setPaymentMethod(null)
                .setPaymentType(null)
                .setDelivery(BigDecimal.ZERO)
                .setFeeSum(BigDecimal.ZERO)
                .setFeeCorrect(BigDecimal.ZERO)
                .setFulfilment(true)
                .setItems(Arrays.asList(
                        //todo не все поля нужны
                        MbiOrderItem.builder().setId(101L).setOrderId(1L).setFeedId(1L).setOfferId("1").setOfferName(
                                "test").setModelId(1)
                                .setWareMd5("waremd5").setCategoryId(1).setFeedCategoryId(BigInteger.ONE).setShowUid(
                                "1")
                                .setPrice(BigDecimal.valueOf(1000)).setBuyerPrice(BigDecimal.valueOf(1000))
                                .setFeedPrice(BigDecimal.valueOf(1000)).setCount(1).setNormFee(BigDecimal.ZERO)
                                .setIntFee(0).setShopFee(BigDecimal.ZERO).setNetFeeUE(BigDecimal.ZERO)
                                .setSubsidy(BigDecimal.ZERO).setVatRate(VatRate.VAT_0)
                                .setFfSupplierId(555777L).setSku("xn763l").setShopSku("7j37sh")
                                .setBatchSize(10)
                                .build(),
                        MbiOrderItem.builder().setId(102L).setOrderId(1L).setFeedId(1L).setOfferId("2").setOfferName(
                                "test2").setModelId(1)
                                .setWareMd5("waremd5").setCategoryId(1).setFeedCategoryId(BigInteger.ONE).setShowUid(
                                "1")
                                .setPrice(BigDecimal.valueOf(2000)).setBuyerPrice(BigDecimal.valueOf(2000))
                                .setFeedPrice(BigDecimal.valueOf(2000)).setCount(1).setNormFee(BigDecimal.ZERO)
                                .setIntFee(0).setShopFee(BigDecimal.ZERO).setNetFeeUE(BigDecimal.ZERO)
                                .setSubsidy(BigDecimal.ZERO)
                                .setFfSupplierId(555777L).setSku("k39sj3l").setShopSku("l9j97n")
                                .build()
                ))
                .setAllowedPaymentMethods(List.of(PaymentMethod.CASH_ON_DELIVERY))
                .setBuyerType(BuyerType.PERSON)
                .build();
    }

    private static MbiOrder buildOrderWithoutPromo() {
        return new MbiOrderBuilder()
                .setId(1)
                .setColor(BLUE)
                .setStatus(MbiOrderStatus.PROCESSING)
                .setCampaignId(1)
                .setShopId(1)
                .setTotal(BigDecimal.valueOf(3000))
                .setCurrency(Currency.RUR)
                .setCreationDate(new Date())
                .setTrantime(new Date())
                .setOrderDelivery(
                        new OrderDeliveryBuilder()
                                .setBillingType(DeliveryBillingType.SPENT)
                                .setRoute(new DeliveryRoute(1L, 2L))
                                .build()
                )
                .setSubstatus(null)
                .setBillingStatus(OrderBillingStatus.BILLED)
                .setPaymentMethod(null)
                .setPaymentType(null)
                .setDelivery(BigDecimal.ZERO)
                .setFeeSum(BigDecimal.ZERO)
                .setFeeCorrect(BigDecimal.ZERO)
                .setBuyerType(BuyerType.PERSON)
                .setItems(Arrays.asList(
                        MbiOrderItem.builder().setId(1L).setOrderId(1L).setFeedId(1L).setOfferId("1").setOfferName(
                                "test").setModelId(1)
                                .setWareMd5("waremd5").setCategoryId(1).setFeedCategoryId(BigInteger.ONE).setShowUid(
                                "1")
                                .setPrice(BigDecimal.valueOf(1000)).setBuyerPrice(BigDecimal.valueOf(1001))
                                .setFeedPrice(BigDecimal.valueOf(1002)).setCount(1).setNormFee(BigDecimal.ZERO)
                                .setIntFee(0).setShopFee(BigDecimal.ZERO).setNetFeeUE(BigDecimal.ZERO)
                                .setSubsidy(BigDecimal.ZERO)
                                .build(),
                        MbiOrderItem.builder().setId(2L).setOrderId(1L).setFeedId(1L).setOfferId("2").setOfferName(
                                "test2").setModelId(1)
                                .setWareMd5("waremd52").setCategoryId(1).setFeedCategoryId(BigInteger.ONE).setShowUid(
                                "1")
                                .setPrice(BigDecimal.valueOf(2000)).setBuyerPrice(BigDecimal.valueOf(2000))
                                .setCount(1).setNormFee(BigDecimal.ZERO)
                                .setIntFee(0).setShopFee(BigDecimal.ZERO).setNetFeeUE(BigDecimal.ZERO)
                                .setSubsidy(BigDecimal.ZERO)
                                .build()
                ))
                .build();
    }

    @Test
    @DbUnitDataSet(before = "db/VirtualMarketDeliveryOrdersForBilling.before.csv")
    @DisplayName("Проверка, что виртуальные магазины не возвращаются в методе для обилливания доставки")
    void virtualMarketDeliveryOrdersForBilling() {
        List<MbiOrder> orders = orderService.findMarketDeliveryOrdersForBilling(new Date());
        assertThat(orders, hasSize(1));
        assertThat(orders.get(0).getShopId(), equalTo(774L));
    }

    @Test
    @DbUnitDataSet(before = "db/TypesMarketDeliveryOrdersForBilling.before.csv")
    @DisplayName("Проверка, что в методе для обилливания доставки возвращается только тип ПВЗ")
    void typesMarketDeliveryOrdersForBilling() {
        List<MbiOrder> orders = orderService.findMarketDeliveryOrdersForBilling(new Date());
        assertThat(orders, hasSize(1));
        assertThat(orders.stream()
                .map(MbiOrder::getId)
                .collect(Collectors.toList()), contains(222L));
    }

    @Test
    @DbUnitDataSet(before = "db/testGetMarketPaymentsStats.before.csv")
    void testGetMarketPaymentsStats() {
        OrderTransaction paymentTransaction = new OrderTransaction();
        paymentTransaction.setTrustPaymentId("payment");

        OrderTransaction refundTransaction = new OrderTransaction();
        refundTransaction.setTrustRefundId("refund");

        TransactionBankOrder expectedPayment = TransactionBankOrder.builder().setTrustPaymentId("payment")
                .setTrustRefundId(null)
                .setBankOrderId("12345")
                .setBankOrderTime(LocalDateTime.of(2017, Month.JANUARY, 2, 0, 0))
                .setHandlingTime(LocalDateTime.of(2017, Month.MAY, 6, 0, 0))
                .setDatasourceId(1L)
                .setPpAmount(100L)
                .build();
        TransactionBankOrder expectedRefund = TransactionBankOrder.builder().setTrustPaymentId(null)
                .setTrustRefundId("refund")
                .setBankOrderId("12347")
                .setBankOrderTime(LocalDateTime.of(2017, Month.FEBRUARY, 2, 0, 0))
                .setHandlingTime(LocalDateTime.of(2017, Month.MAY, 6, 0, 0))
                .setDatasourceId(1L)
                .setPpAmount(100L)
                .build();

        List<TransactionBankOrder> marketPaymentsStats = orderService.getMarketPaymentsStats(
                Arrays.asList(paymentTransaction, refundTransaction), 1L);
        assertThat(marketPaymentsStats, containsInAnyOrder(
                samePropertyValuesAs(expectedPayment), samePropertyValuesAs(expectedRefund)));
    }

    //TODO maybe better to test complete Order

    @Test
    @DbUnitDataSet(before = "DbOrderServiceTest.before.csv")
    void checkOrderFields() {
        MbiOrder order = orderService.getOrder(1L);
        assertThat(order.getDeliveryType(), equalTo(OrderDeliveryType.COURIER));
        assertThat(order.getOrderDelivery().costs().getReturnCost(), equalTo(BigDecimal.valueOf(6.5)));
    }

    @Test
    @DbUnitDataSet(before = "DbOrderServiceTest.before.csv")
    void getOrderFieldsNull() {
        MbiOrder order = orderService.getOrder(2L);
        assertThat(order.getDeliveryType(), nullValue());
        assertThat(order.getOrderDelivery().costs().getReturnCost(), nullValue());
    }

    @Test
    @DbUnitDataSet(after = "saveOrderWithOrderDeliveryType.after.csv")
    void saveOrderWithOrderDeliveryType() {
        MbiOrder order = getOrderBuilder()
                .setDeliveryType(OrderDeliveryType.PICKUP)
                .build();
        orderService.storeOrder(order);
    }

    @Test
    @DbUnitDataSet(after = "saveOrderWithOrderDeliveryTypeNull.after.csv")
    void saveOrderWithOrderDeliveryTypeNull() {
        MbiOrder order = getOrderBuilder()
                .setDeliveryType(null)
                .build();
        orderService.storeOrder(order);
    }

    @Test
    @DbUnitDataSet(after = "saveOrderWithAdditionalInfo.after.csv")
    void saveOrderWithAdditionalInfo() {
        LocalDateTime expiryDate = LocalDateTime.of(2020, 1, 1, 16, 30, 0);

        MbiOrder order = getOrderBuilder()
                .setBuyerType(BuyerType.PERSON)
                .setStatusExpiryDate(expiryDate)
                .build();
        orderService.storeOrder(order);
    }

    @Test
    @DbUnitDataSet(before = "DbOrderServiceTest.order.before.csv", after = "DbOrderServiceTest.update.costs.after.csv")
    void updateOrderDeliveryCosts() {
        orderService.updateOrderDeliveryCosts(ORDER_ID, ORDER_DELIVERY);
    }

    /**
     * в OrderSubstatus два статуса с id 26 (в одном из них опечатка), поэтому нельзя использовать HasIntId.
     * Вместо этого надр пользоваться ordinal и getEnumFromResultSet
     */
    @Test
    void testSubstatuses() {
        Date date = Date.from(
                LocalDateTime
                        .of(2020, 5, 19, 0, 0)
                        .atZone(ZoneId.systemDefault()).toInstant()
        );
        orderService.storeOrder(
                new MbiOrderBuilder()
                        .setId(1L)
                        .setCreationDate(date)
                        .setStatus(MbiOrderStatus.UNPAID)
                        .setSubstatus(OrderSubstatus.WAITING_USER_INPUT)
                        .setTrantime(date)
                        .setCreationDate(date)
                        .setBillingStatus(OrderBillingStatus.BILLED)
                        .setCurrency(Currency.RUR)
                        .setColor(BLUE)
                        .setTotal(BigDecimal.ONE)
                        .setDelivery(BigDecimal.ONE)
                        .setFeeSum(BigDecimal.ONE)
                        .setOrderDelivery(
                                new OrderDelivery(
                                        date,
                                        OrderDeliveryCosts.builder()
                                                .setChargeableWeight(0.5)
                                                .setDeliveryCost(BigDecimal.ONE)
                                                .setFinalCost(BigDecimal.ONE)
                                                .setInsuranceCost(BigDecimal.ONE)
                                                .setReturnCost(BigDecimal.ONE)
                                                .build(),
                                        DeliveryBillingType.UNKNOWN,
                                        new DeliveryRoute(1L, 1L),
                                        new OrderDeliveryDeclaredValue(1, BigDecimal.valueOf(1)),
                                        emptyList()
                                ))
                        .setBuyerType(BuyerType.PERSON)
                        .build()
        );
        assertEquals(OrderSubstatus.WAITING_USER_INPUT, orderService.getOrder(1L).getSubstatus());
    }

    @Test
    @DbUnitDataSet(before = "DbOrderServiceTest.testGetArchivedOrder.before.csv")
    void testGetArchivedOrder() {
        MbiOrder order = orderService.getOrder(1L);
        assertTrue(order.isArchived());
    }

    @Test
    @DbUnitDataSet(after = "saveOrderExtendedStatus.after.csv")
    void saveOrderExtendedStatus() {
        var orderTemplate = buildOrder();
        var itemsTemplate = orderTemplate.getItems();

        var items = List.of(
                Iterables.get(itemsTemplate, 0).toBuilder()
                        .setExtendedStatus(new OrderItemExtendedStatus(
                                OrderInfoStatus.DELIVERY, OrderInfoStatus.PICKUP))
                        .build(),
                Iterables.get(itemsTemplate, 1).toBuilder()
                        .setExtendedStatus(new OrderItemExtendedStatus(
                                OrderInfoStatus.PARTIALLY_RETURNED, OrderInfoStatus.UNREDEEMED))
                        .build()
        );
        var order = orderTemplate.toBuilder().setItems(items).build();

        orderService.storeOrder(order);
    }

    @Test
    void readUpdateOrderItemExtendedStatus() {
        var order = buildOrder();
        orderService.storeOrder(buildOrder());

        assertEquals(new OrderItemExtendedStatus(OrderInfoStatus.UNKNOWN, OrderInfoStatus.UNKNOWN),
                Iterables.get(orderService.getOrderItems(order.getId()), 0).getExtendedStatus());

        orderService.updateOrderItemsStatus(order.getId(), MbiOrderStatus.UNPAID,
                new OrderItemExtendedStatus(OrderInfoStatus.PICKUP, OrderInfoStatus.RESERVED));

        for (var item : orderService.getOrderItems(order.getId())) {
            assertEquals(MbiOrderStatus.UNPAID, item.getOrderStatus());
            assertEquals(OrderInfoStatus.PICKUP, item.getExtendedStatus().getSupplierOrderStatus());
            assertEquals(OrderInfoStatus.RESERVED, item.getExtendedStatus().getOrderItemStatus());
        }
    }

    @Test
    @DbUnitDataSet(before = "DbOrderServiceTest.before.csv")
    void readOrderExtendedStatusNotMigratedYet() {
        var orderItems = orderService.getOrderItems(1l);

        assertFalse(orderItems.isEmpty());
        for (var item : orderItems) {
            assertEquals(OrderInfoStatus.UNKNOWN, item.getExtendedStatus().getSupplierOrderStatus());
            assertEquals(OrderInfoStatus.UNKNOWN, item.getExtendedStatus().getOrderItemStatus());
        }
    }

    @Test
    @DbUnitDataSet(after = "mbiControlEnabledTest.after.csv")
    void mbiControlEnabledTest() {
        OrderTransaction transaction = createDummyTransaction(123L);
        transaction.setMbiControlEnabled(true);
        orderService.storeOrderTransaction(transaction);
    }

    @Test
    @DbUnitDataSet(before = "mbiControlEnabledFindTest.before.csv")
    void mbiControlEnabledFindTest() {
        List<OrderTransaction> transactions = orderService.findTransactionsByOrderIds(List.of(4L, 5L));
        assertEquals(2, transactions.size());
        assertTrue(transactions.stream().anyMatch(OrderTransaction::getMbiControlEnabled));
        assertTrue(transactions.stream().anyMatch(it -> !it.getMbiControlEnabled()));
    }

    @Test
    @DbUnitDataSet(after = "cessionTest.after.csv")
    void cessionTest() {
        OrderTransaction transaction = createDummyTransaction(123L);
        transaction.setCession(true);
        orderService.storeOrderTransaction(transaction);
        transaction = createDummyTransaction(124L);
        orderService.storeOrderTransaction(transaction);
    }

    @Test
    @DbUnitDataSet(before = "cessionTest.before.csv")
    void cessionFindTest() {
        List<OrderTransaction> transactions = orderService.findTransactionsByOrderIds(List.of(4L, 5L));
        assertEquals(2, transactions.size());
        assertTrue(transactions.stream().anyMatch(OrderTransaction::isCession));
        assertTrue(transactions.stream().anyMatch(it -> !it.isCession()));
    }

    @Test
    @DisplayName("Успешное обновление категорий товаров с категории 'все товары'")
    @DbUnitDataSet(before = "updateOrderItems.category.before.csv", after = "updateOrderItems.category.after.csv")
    void updateOrderItemsCategoryId() {
        orderService.updateOrderItemsCategoryId(List.of(1L, 3L), 100L);
    }

    @Test
    @DisplayName("Обновления категорий товаров с категории не 'все товары' запрещено")
    @DbUnitDataSet(before = "updateOrderItems.category.before.csv", after = "updateOrderItems.category.before.csv")
    void forbidNotEmptyCategoryId() {
        orderService.updateOrderItemsCategoryId(List.of(2L, 4L), 100L);
    }

    @Test
    @DisplayName("Обновления категорий товаров на несуществующую категорию запрещено")
    @DbUnitDataSet(before = "updateOrderItems.category.before.csv", after = "updateOrderItems.category.before.csv")
    void forbidNotExistingCategoryId() {
        orderService.updateOrderItemsCategoryId(List.of(1L, 2L, 3L, 4L), 200L);
    }
}
