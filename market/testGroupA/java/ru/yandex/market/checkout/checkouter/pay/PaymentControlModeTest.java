package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.request.PagedRefundsRequest;
import ru.yandex.market.checkout.checkouter.request.PaymentRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.ControllerUtils.buildUnlimitedPager;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_CASH_PAYMENT;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_REFUND;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.PAYMENT_CALL_BALANCE_UPDATE_PAYMENT;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.postpaidBlueOrderParameters;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.prepaidBlueOrderParameters;
import static ru.yandex.market.checkout.util.balance.ShopSettingsHelper.createCustomNewPrepayMeta;
import static ru.yandex.market.checkout.util.balance.ShopSettingsHelper.metaWithPaymentControlFlag;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.getRequestBodyAsJson;

/**
 * @author : poluektov
 * date: 2021-08-10.
 */
public class PaymentControlModeTest extends AbstractWebTestBase {

    private static final Long SHOP_ID = 3366994L;
    private static final Long ANOTHER_SHOP_ID = 3366888L;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private CheckouterClient checkouterClient;
    @Autowired
    private CheckouterFeatureWriter checkouterFeatureWriter;
    @Autowired
    private ReturnHelper returnHelper;
    @Autowired
    private RefundHelper refundHelper;
    private Order order;


    @Test
    public void testSaveMbiFlag() {
        createOrder();
        order = orderService.getOrder(order.getId());
        assertTrue(order.getPayment().getMbiControlEnabled());

        //check client
        PaymentRequest request = PaymentRequest.builder(order.getPayment().getId())
                .build();
        Payment response = checkouterClient.payments().getPayment(new RequestClientInfo(ClientRole.SYSTEM, 0L),
                request);
        assertTrue(response.getMbiControlEnabled());
    }

    @Test
    public void testTrustPayload() {
        createOrder();
        ServeEvent createBasketEvent = getBasketEvent();
        JsonObject body = getRequestBodyAsJson(createBasketEvent);
        String devPayload = body.get("developer_payload").getAsString();
        assertTrue(devPayload.contains("\"ProcessThroughYt\":1"));
        assertTrue(devPayload.contains("\"call_preview_payment\":\"card_info\""));
    }

    @Test
    public void testTrustPayloadForSubsidy() {
        createOrder();
        trustMockConfigurer.resetRequests();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT, order.getId()));

        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());
        ServeEvent createBasketEvent = getBasketEvent();
        JsonObject body = getRequestBodyAsJson(createBasketEvent);
        String devPayload = body.get("developer_payload").getAsString();
        assertTrue(devPayload.contains("\"ProcessThroughYt\":1"));
    }

    @Test
    public void testUpdatePaymentQC() {
        createOrder();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertFalse(queuedCallService.existsQueuedCall(PAYMENT_CALL_BALANCE_UPDATE_PAYMENT,
                order.getPayment().getId()));
    }

    @Test
    public void testFlagForGetRefunds() {
        createOrder();
        orderPayHelper.notifyPaymentClear(order.getPayment());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);
        assertTrue(queuedCallService.existsQueuedCall(ORDER_REFUND, order.getId()));
        queuedCallService.executeQueuedCallSynchronously(ORDER_REFUND, order.getId());
        var req = new RefundSearchRequest(order.getId(), null, null, PaymentGoal.ORDER_PREPAY);
        PagedRefunds response = refundService.getRefunds(req, buildUnlimitedPager(1, 100), true);
        Refund refund = response.getItems().iterator().next();
        assertTrue(refund.getMbiControlEnabled());

        //check client
        PagedRefundsRequest request = PagedRefundsRequest.builder(order.getId())
                .withShowPaymentControl(true)
                .build();
        response = checkouterClient.refunds().getRefunds(new RequestClientInfo(ClientRole.SYSTEM, 0L),
                request);
        refund = response.getItems().iterator().next();
        assertTrue(refund.getMbiControlEnabled());
        assertFalse(refund.getUsingCashRefundService());
    }

    @Test
    public void testSaveMbiFlagForDifferentShops() {
        checkouterProperties.setEnablePaymentControlByDefault(false);
        createOrderWithDifferentShops();
        order = orderService.getOrder(order.getId());
        assertFalse(order.getPayment().getMbiControlEnabled());
    }

    @Test
    public void testServiceFeeMapForCashRefunds() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_REFUND_SERVICE_FEE_PARTITIONS, true);
        createPostpaidOrder();
        Return ret = returnHelper.createReturn(order.getId(), ReturnProvider
                .generatePartialReturnWithDelivery(order, order.getDelivery().getDeliveryServiceId(), 1));
        tmsTaskHelper.runProcessReturnPaymentsPartitionTaskV2();

        refundHelper.proceedAsyncRefunds(order.getId());

        Refund refund = refundService.getReturnRefunds(ret).iterator().next();
        assertNotNull(refund.getProperties());
        assertEquals(1, refund.getProperties().getServiceFeePartitions().size());
        Map<Integer, BigDecimal> serviceFeeMap =
                refund.getProperties().getServiceFeePartitions().get(0).getServiceFeeValues();
        //В мапе лежат айтем с serviceFee=11 и его сумма совпадает с тоталу по айтемам заказа.
        assertThat(order.getItemsTotal(), comparesEqualTo(serviceFeeMap.get(11)));
        assertTrue(refund.getUsingCashRefundService());
    }

    @Test
    public void testServiceFeeMapForFullReturn() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_REFUND_SERVICE_FEE_PARTITIONS, true);
        createPostpaidOrder();
        Return ret = returnHelper.createReturn(order.getId(), ReturnProvider.generateFullReturn(order));
        tmsTaskHelper.runProcessReturnPaymentsPartitionTaskV2();

        refundHelper.proceedAsyncRefunds(order.getId());

        Refund refund = refundService.getReturnRefunds(ret).iterator().next();
        assertNotNull(refund.getProperties());
        assertEquals(2, refund.getProperties().getServiceFeePartitions().size());
        Map<Integer, BigDecimal> itemServiceFeeMap =
                refund.getProperties().getServiceFeePartitions().stream()
                        .filter(partition -> partition.getType() == EntityType.ITEM)
                        .findAny().orElseThrow().getServiceFeeValues();
        //В мапе лежит айтем с serviceFee=11 и его сумма совпадает с тоталу по айтемам заказа.
        assertThat(order.getItemsTotal(), comparesEqualTo(itemServiceFeeMap.get(11)));

        Map<Integer, BigDecimal> deliveryServiceFeeMap =
                refund.getProperties().getServiceFeePartitions().stream()
                        .filter(partition -> partition.getType() == EntityType.DELIVERY)
                        .findAny().orElseThrow().getServiceFeeValues();
        //В мапе лежит доставка с serviceFee=11 и его сумма совпадает с ценой доставки.
        assertEquals(order.getDelivery().getBuyerPrice(), deliveryServiceFeeMap.get(11));
        assertTrue(refund.getUsingCashRefundService());
    }

    @Test
    public void testServiceFeeMapForFullOfflineReturn() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_REFUND_SERVICE_FEE_PARTITIONS, true);
        createPostpaidOrder();
        Return retRequest = ReturnProvider.generateFullReturn(order);
        retRequest.setPayOffline(true);
        Return ret = returnHelper.createReturn(order.getId(), retRequest);
        tmsTaskHelper.runProcessReturnPaymentsPartitionTaskV2();

        refundHelper.proceedAsyncRefunds(order.getId());

        Refund refund = refundService.getReturnRefunds(ret).iterator().next();
        assertNotNull(refund.getProperties());
        assertEquals(2, refund.getProperties().getServiceFeePartitions().size());
        Map<Integer, BigDecimal> itemServiceFeeMap =
                refund.getProperties().getServiceFeePartitions().stream()
                        .filter(partition -> partition.getType() == EntityType.ITEM)
                        .findAny().orElseThrow().getServiceFeeValues();
        //В мапе лежит айтем с serviceFee=14 и его сумма совпадает с тоталу по айтемам заказа.
        assertThat(order.getItemsTotal(), comparesEqualTo(itemServiceFeeMap.get(14)));

        Map<Integer, BigDecimal> deliveryServiceFeeMap =
                refund.getProperties().getServiceFeePartitions().stream()
                        .filter(partition -> partition.getType() == EntityType.DELIVERY)
                        .findAny().orElseThrow().getServiceFeeValues();
        //В мапе лежит доставка с serviceFee=4 и его сумма совпадает с ценой доставки.
        assertEquals(order.getDelivery().getBuyerPrice(), deliveryServiceFeeMap.get(14));
        assertTrue(refund.getUsingCashRefundService());
    }

    private ServeEvent getBasketEvent() {
        return trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_BASKET_STUB))
                .findAny().get();
    }

    private void createOrder() {
        Parameters parameters = prepaidBlueOrderParameters();
        OrderItem item1 = OrderItemProvider.buildOrderItem("item-1", new BigDecimal("111.00"), 1);
        item1.setMsku(332L);
        item1.setShopSku("sku-1");
        item1.setSku("332");
        item1.setSupplierId(SHOP_ID);
        item1.setWareMd5(OrderItemProvider.OTHER_WARE_MD5);
        item1.setShowInfo(OrderItemProvider.OTHER_SHOW_INFO);

        parameters.setMockLoyalty(true);
        parameters.getLoyaltyParameters()
                .addLoyaltyDiscount(item1, LoyaltyDiscount.discountFor(10, PromoType.MARKET_PROMOCODE));

        parameters.getOrder().setItems(List.of(item1));
        parameters.addShopMetaData(SHOP_ID, metaWithPaymentControlFlag(
                SHOP_ID.intValue()));
        order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
    }

    private void createOrderWithDifferentShops() {
        //payment_control=enabled
        Parameters parameters = prepaidBlueOrderParameters();
        OrderItem item1 = OrderItemProvider.buildOrderItem("item-1", new BigDecimal("111.00"), 1);
        item1.setMsku(332L);
        item1.setShopSku("sku-1");
        item1.setSku("332");
        item1.setSupplierId(SHOP_ID);
        item1.setWareMd5(OrderItemProvider.OTHER_WARE_MD5);
        item1.setShowInfo(OrderItemProvider.OTHER_SHOW_INFO);
        parameters.addShopMetaData(SHOP_ID, metaWithPaymentControlFlag(
                SHOP_ID.intValue()));

        //payment_control=disabled
        OrderItem item2 = OrderItemProvider.buildOrderItem("item-2", new BigDecimal("777.00"), 1);
        item2.setMsku(334L);
        item2.setShopSku("sku-2");
        item2.setSku("334");
        item2.setSupplierId(ANOTHER_SHOP_ID);
        item2.setWareMd5(OrderItemProvider.ANOTHER_WARE_MD5);
        item2.setShowInfo(OrderItemProvider.ANOTHER_SHOW_INFO);
        parameters.addShopMetaData(ANOTHER_SHOP_ID, createCustomNewPrepayMeta(ANOTHER_SHOP_ID.intValue()));

        parameters.getOrder().setItems(List.of(item1, item2));
        order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
    }

    private void createPostpaidOrder() {
        //payment_control=enabled
        Parameters parameters = postpaidBlueOrderParameters();
        OrderItem item1 = OrderItemProvider.buildOrderItem("item-1", new BigDecimal("111.00"), 1);
        item1.setMsku(332L);
        item1.setShopSku("sku-1");
        item1.setSku("332");
        item1.setSupplierId(SHOP_ID);
        item1.setWareMd5(OrderItemProvider.OTHER_WARE_MD5);
        item1.setShowInfo(OrderItemProvider.OTHER_SHOW_INFO);
        parameters.addShopMetaData(SHOP_ID, metaWithPaymentControlFlag(
                SHOP_ID.intValue()));
        long delieveryShop = colorConfig.getFor(Color.BLUE).getMarketplaceShopId();
        parameters.addShopMetaData(delieveryShop, metaWithPaymentControlFlag((int) delieveryShop));
        parameters.getOrder().setItems(List.of(item1));
        order = orderCreateHelper.createOrder(parameters);
        trustMockConfigurer.mockWholeTrust();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_CASH_PAYMENT, order.getId());
        order = orderService.getOrder(order.getId());
    }
}
