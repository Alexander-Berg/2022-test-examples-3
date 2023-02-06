package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.Sets;
import io.qameta.allure.junit4.Tag;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CheckoutParameters;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.BasicOrder;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.HitRateGroup;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.storage.receipt.ReceiptDao;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.common.tasks.ZooTask;
import ru.yandex.market.checkout.helpers.BlueCrossborderOrderHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.DeliveryResponseProvider;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.json.JsonTest;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.order.OrderServiceTestHelper.FF_SHOP_ID;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.TRUST_PAYMENTS_CREATE_BASKET_URL;


/**
 * @author : poluektov
 * date: 20.02.2019.
 */
public class SpasiboOrderPaymentTest extends AbstractPaymentTestBase {

    private static final Long EXCLUDED_VENDOR = 206928L;
    @Autowired
    protected WireMockServer trustMock;
    @Autowired
    private ReturnHelper returnHelper;
    @Autowired
    private ZooTask firstPartyOrderEventExportTask;
    @Autowired
    private ReceiptDao receiptDao;
    @Autowired
    private JdbcTemplate erpJdbcTemplate;
    @Autowired
    private BlueCrossborderOrderHelper blueCrossborderOrderHelper;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private RefundService refundService;

    @BeforeEach
    public void setUp() {
        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();
    }

    @Test
    public void createFFSpasiboOrderAndPay() throws Exception {
        createUnpaidBlueOrder();
        Long receiptId = paymentTestHelper.initPayment();
        paymentTestHelper.markupPayment(createDefaultPartitions());
        paymentTestHelper.notifyPaymentSucceeded(receiptId, false);
        paymentTestHelper.clearPayment();
    }

    @Test
    public void returnSpasiboOrder() throws Exception {
        createUnpaidBlueOrder();
        returnHelper.mockActualDelivery(order.get());
        Long receiptId = paymentTestHelper.initPayment();
        paymentTestHelper.markupPayment(createDefaultPartitions());
        paymentTestHelper.notifyPaymentSucceeded(receiptId, false);
        paymentTestHelper.clearPayment();
        orderStatusHelper.proceedOrderToStatus(order(), OrderStatus.DELIVERED);

        Return ret = returnHelper.createReturn(order().getId(), ReturnProvider.generateReturn(order.get()));
        trustMockConfigurer.resetRequests();
        returnHelper.processReturnPayments(order(), ret);
        refundTestHelper.checkRefunds(ret);
        returnTestHelper.checkReturnBallanceCalls(ret);
    }

    @Test
    public void testPartialReturnSpasiboOrder() throws Exception {
        createCustomOrderWith2Items();
        Long receiptId = paymentTestHelper.initPayment();
        paymentTestHelper.markupPayment(createCustomPaymentPartition(new BigDecimal(12017)));
        paymentTestHelper.notifyPaymentSucceeded(receiptId, false);
        paymentTestHelper.clearPayment();
        orderStatusHelper.proceedOrderToStatus(order(), OrderStatus.DELIVERED);

        OrderItem item =
                order().getItems().stream().filter(i -> i.getShopSku().equals("sku-1")).findAny().orElseThrow();
        Return ret = returnHelper.createReturn(order().getId(), ReturnProvider.generatePartialReturn(order.get(),
                item, 1));
        trustMockConfigurer.resetRequests();
        returnHelper.processReturnPayments(order(), ret);
        refundTestHelper.checkRefunds(ret);
        returnTestHelper.checkReturnBallanceCalls(ret);
    }

    @Test
    public void returnSpasiboOrderWithDelivery() throws Exception {
        createUnpaidBlueOrder();
        returnHelper.mockActualDelivery(order.get());
        Long receiptId = paymentTestHelper.initPayment();
        paymentTestHelper.markupPayment(createDefaultPartitions());
        paymentTestHelper.notifyPaymentSucceeded(receiptId, false);
        paymentTestHelper.clearPayment();
        orderStatusHelper.proceedOrderToStatus(order(), OrderStatus.DELIVERED);

        Return ret = returnHelper.createReturn(order().getId(), ReturnProvider.generateFullReturn(order.get()));
        trustMockConfigurer.resetRequests();
        returnHelper.processReturnPayments(order(), ret);
        refundTestHelper.checkRefunds(ret);
        returnTestHelper.checkReturnBallanceCalls(ret);
    }

    //TODO: этому тесту не место здесь! Но создать полноценный оплаченный спасибо заказ можно либо через
    // PaymentTestHelper, либо через боль.
    @Test
    public void spasiboOrderErpExport() throws Exception {
        createUnpaid1PBlueOrder();
        Long receiptId = paymentTestHelper.initPayment();
        paymentTestHelper.markupPayment(createDefaultPartitions());
        paymentTestHelper.notifyPaymentSucceeded(receiptId, false);
        paymentTestHelper.clearPayment();

        //Проверка чтения чеков из базы (https://st.yandex-team.ru/MARKETCHECKOUT-10253)
        Map<Long, ReceiptItem> map = receiptDao.fetchReceiptsItemForOrderItems(singletonList(order().getId()),
                ReceiptType.INCOME, PaymentGoal.ORDER_PREPAY);
        ReceiptItem receiptItemFromDB = map.values().iterator().next();
        assertThat(receiptItemFromDB.getPartitions(), notNullValue());

        firstPartyOrderEventExportTask.runOnce();
        verifyItemsExported(order(), 2);
        verifySpasiboExported(receiptItemFromDB.getItemId(),
                receiptItemFromDB.amountByAgent(PaymentAgent.SBER_SPASIBO).intValue());
    }

    @DisplayName("Разделение суммы платежа по paymentAgent мультизаказа")
    @Test
    public void shouldSeparateSpasiboPaymentPartitionsForMultiOrder() throws Exception {
        checkouterProperties.setEnableSeparateTotalAmountInPaymentByOrders(true);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Parameters anotherOrderParameters = BlueParametersProvider.defaultBlueOrderParameters();
        anotherOrderParameters.addOtherItem();
        parameters.addOrder(anotherOrderParameters);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        Payment payment = orderPayHelper.payForOrdersWithoutNotification(multiOrder.getOrders());

        multiOrder.getOrders().get(0).setPayment(payment);
        this.order.set(multiOrder.getOrders().get(0));

        BigDecimal spasiboTotal = new BigDecimal(300);
        paymentTestHelper.markupPayment(createCustomPaymentPartition(spasiboTotal));

        // вот так пересчёт пэймент партишенов запускается, а если делать два отдельных getOrder(), то этого не будет
        List<Order> orders = new ArrayList<>(orderService.getOrdersByPayment(payment.getId(), ClientInfo.SYSTEM));
        Order firstOrder = orders.get(0);
        Order secondOrder = orders.get(1);

        //Проверяем, что есть ненулевый дефолтные партишны и в сумме совпадают, доставка не учитывется в partitions
        BigDecimal deliveryTotalPrice = firstOrder.getDelivery().getBuyerPrice()
                .add(secondOrder.getDelivery().getBuyerPrice());

        checkSeparatePayment(payment, spasiboTotal, firstOrder.getPayment(), secondOrder.getPayment(),
                deliveryTotalPrice);
    }

    @DisplayName("Разделение суммы платежа по paymentAgent в рефандах мультизаказа")
    @Test
    public void shouldSeparateSpasiboPaymentPartitionsForMultiOrderRefunds() throws Exception {
        checkouterProperties.setEnableSeparateTotalAmountInPaymentByOrders(true);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addOrder(BlueParametersProvider.defaultBlueOrderParameters());
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        Payment payment = orderPayHelper.payForOrdersWithoutNotification(multiOrder.getOrders());

        multiOrder.getOrders().get(0).setPayment(payment);
        this.order.set(multiOrder.getOrders().get(0));

        BigDecimal spasiboTotal = new BigDecimal(300);
        paymentTestHelper.markupPayment(createCustomPaymentPartition(spasiboTotal));

        List<Long> orderIds = multiOrder.getOrders().stream()
                .map(BasicOrder::getId)
                .collect(Collectors.toList());

        notifyPayment(orderIds, payment);
//
        //клирим платеж путем перевода заказов в delivery
        paymentTestHelper.tryClearMultipayment(multiOrder.getOrders(), Collections.emptyList());

//        Отменяем один из заказов
        Order firstCanceledOrder = orderUpdateService.updateOrderStatus(multiOrder.getOrders().get(0).getId(),
                OrderStatus.CANCELLED,
                OrderSubstatus.CUSTOM);
        Order secondCanceledOrder = orderUpdateService.updateOrderStatus(multiOrder.getOrders().get(1).getId(),
                OrderStatus.CANCELLED,
                OrderSubstatus.CUSTOM);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND_SUBSIDY_PAYMENT);

//        //Проверяем что в базе есть рефанд и его сумма совпадает с общей стоимостью заказа
        Refund refund1 = refundService.getRefunds(firstCanceledOrder.getId()).iterator().next();
        assertEquals(refund1.getAmount(), firstCanceledOrder.getBuyerTotal());

        Refund refund2 = refundService.getRefunds(secondCanceledOrder.getId()).iterator().next();
        assertEquals(refund2.getAmount(), secondCanceledOrder.getBuyerTotal());

        //Проверяем, что есть ненулевый дефолтные партишны и в сумме совпадают, доставка не учитывется в partitions
        BigDecimal deliveryTotalPrice = multiOrder.getOrders().get(0).getDelivery().getBuyerPrice()
                .add(multiOrder.getOrders().get(1).getDelivery().getBuyerPrice());

        checkSeparatePayment(payment, spasiboTotal, refund1.getPayment(), refund2.getPayment(),
                deliveryTotalPrice);
    }

    private void checkSeparatePayment(Payment payment, BigDecimal spasiboTotal,
                                      Payment firstPayment, Payment secondPayment,
                                      BigDecimal deliveryTotalPrice) {
        PaymentPartition firstPaymentSpasiboPart = firstPayment.getPartitions().stream()
                .filter(part -> part.getPaymentAgent() == PaymentAgent.SBER_SPASIBO)
                .findAny()
                .orElseThrow();

        PaymentPartition secondPaymentSpasiboPart = secondPayment.getPartitions().stream()
                .filter(part -> part.getPaymentAgent() == PaymentAgent.SBER_SPASIBO)
                .findAny()
                .orElseThrow();

        PaymentPartition firstPaymentDefaultPart = firstPayment.getPartitions().stream()
                .filter(part -> part.getPaymentAgent() == PaymentAgent.DEFAULT)
                .findAny()
                .orElseThrow();

        PaymentPartition secondPaymentDefaultPart = secondPayment.getPartitions().stream()
                .filter(part -> part.getPaymentAgent() == PaymentAgent.DEFAULT)
                .findAny()
                .orElseThrow();

        //Проверяем, что есть ненулевый сбер партишны и в сумме совпадают
        assertThat(firstPaymentSpasiboPart.getAmount().add(secondPaymentSpasiboPart.getAmount()),
                Matchers.comparesEqualTo(spasiboTotal));

        assertThat(firstPaymentSpasiboPart.getAmount(), greaterThanOrEqualTo(BigDecimal.ONE));
        assertThat(secondPaymentSpasiboPart.getAmount(), greaterThanOrEqualTo(BigDecimal.ONE));

        assertThat(firstPaymentDefaultPart.getAmount().add(secondPaymentDefaultPart.getAmount()),
                Matchers.comparesEqualTo(payment.getTotalAmount()
                        .subtract(spasiboTotal))
        );

        assertThat(firstPaymentDefaultPart.getAmount(), greaterThanOrEqualTo(BigDecimal.ONE));
        assertThat(secondPaymentSpasiboPart.getAmount(), greaterThanOrEqualTo(BigDecimal.ONE));

        //Сумма всех партишнов совпадает с total
        assertThat(
                firstPaymentSpasiboPart.getAmount()
                        .add(firstPaymentDefaultPart.getAmount())
                        .add(secondPaymentSpasiboPart.getAmount())
                        .add(secondPaymentDefaultPart.getAmount()),
                Matchers.comparesEqualTo(payment.getTotalAmount())
        );
    }

    @Tag(Tags.CROSSBORDER)
    @Test
    public void checkSpasiboInMultiOrder() throws Exception {
        DeliveryResponse deliveryResponse = DeliveryResponseProvider.buildPostDeliveryResponse();
        deliveryResponse.setPaymentOptions(Sets.newHashSet(PaymentMethod.YANDEX));

        Parameters crossborderParameters = blueCrossborderOrderHelper.setupParametersForMultiOrder(deliveryResponse);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.addOrder(crossborderParameters);

        MultiCart cart = blueCrossborderOrderHelper.doCartBlueWithoutFulfilment(parameters);
        pushApiConfigurer.mockAccept(parameters.getOrders(), true);
        MultiOrder multiOrder = client.checkout(
                orderCreateHelper.mapCartToOrder(cart, parameters),
                CheckoutParameters.builder()
                        .withUid(cart.getBuyer().getUid())
                        .withSandbox(false)
                        .withRgb(Color.BLUE)
                        .withContext(Context.MARKET)
                        .withApiSettings(ApiSettings.PRODUCTION)
                        .withHitRateGroup(HitRateGroup.LIMIT)
                        .build()
        );

        orderPayHelper.payForOrders(multiOrder.getOrders());
        List<LoggedRequest> requests = trustMock.findRequestsMatching(
                postRequestedFor(urlEqualTo(TRUST_PAYMENTS_CREATE_BASKET_URL)).build()
        ).getRequests();
        requests.forEach(
                r -> {
                    JsonTest.checkJsonMatcher(r.getBodyAsString(), "$.developer_payload",
                            containsString("max_spasibo_amount"));
                    JsonTest.checkJsonMatcher(r.getBodyAsString(), "$.developer_payload",
                            containsString("min_spasibo_amount"));
                    JsonTest.checkJsonMatcher(r.getBodyAsString(), "$.pass_params.market_blue_3ds_policy",
                            containsString("UNKNOWN"));
                    //кросcбордер заказ не должен попадать в разметку
                    JsonTest.checkJsonMatcher(r.getBodyAsString(), "$.spasibo_order_map",
                            aMapWithSize(2));
                    JsonTest.checkJsonMatcher(r.getBodyAsString(), "$.spasibo_order_map",
                            hasValue("testShopSKU"));
                }
        );
    }

    @Test
    public void shouldCreateBasketWithoutSpasiboForExcludedVendor() throws Exception {
        checkouterProperties.setSpasiboExcludedVendors(Set.of(EXCLUDED_VENDOR));
        createCustomOrderWith2Items();
        paymentTestHelper.initPayment();

        List<LoggedRequest> requests = trustMock.findRequestsMatching(
                postRequestedFor(urlEqualTo(TRUST_PAYMENTS_CREATE_BASKET_URL)).build()
        ).getRequests();
        assertThat(requests, hasSize(1));
    }

    private PaymentPartitions createDefaultPartitions() {
        BigDecimal total = order.get().getBuyerItemsTotal();
        PaymentPartitions partitions = new PaymentPartitions();
        IncomingPaymentPartition spasiboPart = new IncomingPaymentPartition(PaymentAgent.SBER_SPASIBO,
                total.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_EVEN));
        partitions.setPartitions(singletonList(spasiboPart));
        return partitions;
    }

    private void createCustomOrderWith2Items() {
        Collection<OrderItem> items = new ArrayList<>();
        OrderItem item1 = OrderItemProvider.buildOrderItem("item-1", new BigDecimal("7622.00"), 2);
        item1.setMsku(322L);
        item1.setShopSku("sku-1");
        item1.setSku("322");
        item1.setVendorId(EXCLUDED_VENDOR);
        items.add(item1);
        OrderItem item2 = OrderItemProvider.buildOrderItem("item-2", new BigDecimal("8154.00"), 1);
        item2.setMsku(332L);
        item2.setShopSku("sku-2");
        item2.setSku("332");
        item2.setWareMd5(OrderItemProvider.ANOTHER_WARE_MD5);
        item2.setShowInfo(OrderItemProvider.ANOTHER_SHOW_INFO);
        items.add(item2);
        Parameters params = BlueParametersProvider.defaultBlueOrderParameters(true);
        params.getBuyer().setDontCall(true);
        params.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        params.getOrder().setItems(items);
        shopMetaData.set(shopService.getMeta(FF_SHOP_ID));
        shopService.updateMeta(item1.getSupplierId(),
                ShopSettingsHelper.createCustomNewPrepayMeta(item1.getSupplierId().intValue()));
        order.set(orderCreateHelper.createOrder(params));
    }

    private PaymentPartitions createCustomPaymentPartition(BigDecimal spasiboValue) {
        PaymentPartitions partitions = new PaymentPartitions();
        IncomingPaymentPartition spasiboPart = new IncomingPaymentPartition(PaymentAgent.SBER_SPASIBO,
                spasiboValue);
        partitions.setPartitions(singletonList(spasiboPart));
        return partitions;
    }

    private void verifyItemsExported(Order order, Integer count) {
        Integer res = erpJdbcTemplate.queryForObject(
                "SELECT count(DISTINCT ITEM_ID) FROM COOrderItem WHERE ORDER_ID=?",
                (rs, rowNum) -> rs.getInt(1),
                order.getId()
        );

        assertThat(res, Matchers.equalTo(count));
    }

    private void verifySpasiboExported(long itemId, long value) {
        Long res = erpJdbcTemplate.queryForObject(
                "SELECT SPASIBO FROM COOrderItem WHERE ITEM_ID=? LIMIT 1",
                (rs, rowNum) -> rs.getLong(1),
                itemId
        );

        assertThat(res, Matchers.equalTo(value));
    }
}
