package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.promo.PromoConfigurer;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.PushExtraSubsidyRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.FREE_DELIVERY_THRESHOLD;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.ANAPLAN_ID;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SHOP_PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT;

/**
 * https://st.yandex-team.ru/MARKETCHECKOUT-20329
 */
public class PayExtraSubsidyTest extends AbstractPaymentTestBase {

    private static final String PUSH_EXTRA_SUBSIDY_ENDPOINT = "/update-subsidies";
    private static final String DD_PROMO = "direct discount";

    @Autowired
    private QueuedCallService queuedCallService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private TestSerializationService testSerializationService;

    @Autowired
    private ReceiptService receiptService;

    @Autowired
    private PromoConfigurer promoConfigurer;

    @BeforeEach
    void setUp() {
        checkouterProperties.setEnableUpdatePaymentMode(true);
        checkouterProperties.setEnablePushExtraSubsidy(true);
        checkouterProperties.setEnablePushExtraSubsidyController(true);
    }

    @Test
    public void testPushingExtraSubsidiesEndpointIsDisabled() throws Exception {
        checkouterProperties.setEnablePushExtraSubsidy(false);
        checkouterProperties.setEnablePushExtraSubsidyController(false);

        PushExtraSubsidyRequest request = buildPushExtraSubsidyRequest(1L, Map.entry(1L, 50L));
        callPushExtraSubsidy(request)
                .andExpect(status().isForbidden());
    }

    @Test
    public void testOrderNotFound() throws Exception {
        PushExtraSubsidyRequest request = buildPushExtraSubsidyRequest(9999L, Map.entry(1L, 50L));
        callPushExtraSubsidy(request)
                .andExpect(status().isNotFound());
    }

    @Test
    public void testOrderHasPeshedExtraSubsidyProperty() throws Exception {
        Parameters parameters = buildParameters();
        parameters.setCheckCartErrors(false);
        parameters.setCheckOrderCreateErrors(false);
        parameters.setUseErrorMatcher(false);
        parameters.getOrder().setProperty(OrderPropertyType.PUSHED_EXTRA_SUBSIDY, true);

        OrderItem firstItem = parameters.getOrder().getItems().iterator().next();

        firstItem.setBuyerPrice(BigDecimal.valueOf(500));
        firstItem.setPrice(BigDecimal.valueOf(500));
        firstItem.setSupplierType(SupplierType.THIRD_PARTY);

        BigDecimal buyerDiscount = BigDecimal.valueOf(150);

        applyDirectDiscount(firstItem, buyerDiscount, BigDecimal.valueOf(0));

        Order order = orderCreateHelper.createOrder(promoConfigurer.applyTo(parameters));
        long itemId = order.getItems().iterator().next().getId();

        PushExtraSubsidyRequest request = buildPushExtraSubsidyRequest(order.getId(), Map.entry(itemId, 150L));
        callPushExtraSubsidy(request, true)
                .andExpect(status().isOk());
    }

    @Test
    public void testRequestHasEmptyItemSize() throws Exception {
        Parameters whiteParameters = buildDefaultParametersWithPromo();
        Order order = createOrder(whiteParameters);

        PushExtraSubsidyRequest request = buildPushExtraSubsidyRequest(order.getId());
        callPushExtraSubsidy(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRequestHasNewItems() throws Exception {
        trustMockConfigurer.mockCreateBasket();
        Parameters parameters = buildParameters();
        parameters.setCheckCartErrors(false);
        parameters.setCheckOrderCreateErrors(false);
        parameters.setUseErrorMatcher(false);

        OrderItem firstItem = parameters.getOrder().getItems().iterator().next();

        firstItem.setBuyerPrice(BigDecimal.valueOf(500));
        firstItem.setPrice(BigDecimal.valueOf(500));
        firstItem.setSupplierType(SupplierType.THIRD_PARTY);

        BigDecimal buyerDiscount = BigDecimal.valueOf(150);

        applyDirectDiscount(firstItem, buyerDiscount, BigDecimal.valueOf(0));

        Order order = orderCreateHelper.createOrder(promoConfigurer.applyTo(parameters));

        PushExtraSubsidyRequest request = buildPushExtraSubsidyRequest(order.getId(), Map.entry(30L, 40L));
        callPushExtraSubsidy(request)
                .andExpect(status().isBadRequest());

        long itemId = order.getItems().iterator().next().getId();

        request = buildPushExtraSubsidyRequest(order.getId(), Map.entry(30L, 40L), Map.entry(itemId, 150L));
        callPushExtraSubsidy(request)
                .andExpect(status().isOk());

        //QC не создалась, заказ не в статусе DELIVERED
        assertFalse(queuedCallService.existsQueuedCall(ORDER_CREATE_SUBSIDY_PAYMENT, order.getId()));
        Order actualOrder = orderService.getOrder(order.getId());
        //У заказа проставилась проверти PUSHED_EXTRA_SUBSIDY и у айтема обновился buyerDiscount
        assertEquals(true, actualOrder.getProperty(OrderPropertyType.PUSHED_EXTRA_SUBSIDY));
        OrderItem actualItem = actualOrder.getItems().iterator().next();
        assertEquals(0, BigDecimal.valueOf(150L).compareTo(actualItem.getPrices().getSubsidy()));
        assertEquals(0, BigDecimal.valueOf(150L).compareTo(actualItem.getPrices().getBuyerSubsidy()));
    }

    @Test
    public void testPushExtraSubsidiesSuccessfullyWhitoutExistingPaymants() throws Exception {
        Parameters parameters = buildParameters();
        parameters.setCheckCartErrors(false);
        parameters.setCheckOrderCreateErrors(false);
        parameters.setUseErrorMatcher(false);

        OrderItem firstItem = parameters.getOrder().getItems().iterator().next();

        firstItem.setBuyerPrice(BigDecimal.valueOf(500));
        firstItem.setPrice(BigDecimal.valueOf(500));
        firstItem.setSupplierType(SupplierType.THIRD_PARTY);

        BigDecimal buyerDiscount = BigDecimal.valueOf(150);

        applyDirectDiscount(firstItem, buyerDiscount, BigDecimal.valueOf(0));

        Order order = orderCreateHelper.createOrder(promoConfigurer.applyTo(parameters));

        //check promos and subsidies
        Set<ItemPromo> actualPromos = order.getItems().iterator().next().getPromos();
        assertThat(actualPromos, hasSize(1));
        ItemPromo actualPromo = actualPromos.iterator().next();
        assertEquals(BigDecimal.valueOf(150), actualPromo.getBuyerDiscount());
        assertNull(actualPromo.getBuyerSubsidy());
        assertNull(actualPromo.getSubsidy());
        assertThat(order.getPromos(), hasSize(1));

        //change status to delivered
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_SUBSIDY_PAYMENT, order.getId()));
        queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());

        order = orderService.getOrder(order.getId());
        OrderItem orderItem = order.getItems().iterator().next();

        PushExtraSubsidyRequest request = buildPushExtraSubsidyRequest(order.getId(), Map.entry(orderItem.getId(),
                150L));
        //контроллер вернул 200
        callPushExtraSubsidy(request)
                .andExpect(status().isOk());
        //создалась QC
        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_SUBSIDY_PAYMENT, order.getId()));
        Order actualOrder = orderService.getOrder(order.getId());
        //У заказа проставилась проверти PUSHED_EXTRA_SUBSIDY и у айтема обновился buyerDiscount
        assertEquals(true, actualOrder.getProperty(OrderPropertyType.PUSHED_EXTRA_SUBSIDY));
        assertThat(actualOrder.getPromos(), hasSize(1));

        //в промо и в прайсах должено быть 150
        OrderItem actualItem = actualOrder.getItems().iterator().next();
        assertEquals(0, BigDecimal.valueOf(150L).compareTo(actualItem.getPrices().getSubsidy()));
        assertThat(actualItem.getPromos(), hasSize(1));
        actualPromo = actualItem.getPromos().iterator().next();
        assertEquals(0, BigDecimal.valueOf(150L).compareTo(actualPromo.getSubsidy()));
        //QC не выполнялась, платежи должны отсутствовать
        List<Payment> subsidyPayments = paymentService.getPayments(actualOrder.getId(),
                ClientInfo.SYSTEM, PaymentGoal.SUBSIDY);
        assertThat(subsidyPayments, hasSize(0));

        //создалась QC
        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_SUBSIDY_PAYMENT, order.getId()));
        queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());
        assertFalse(queuedCallService.existsQueuedCall(ORDER_CREATE_SUBSIDY_PAYMENT, order.getId()));

        actualOrder = orderService.getOrder(order.getId());
        //У заказа проставилась проверти PUSHED_EXTRA_SUBSIDY и у айтема обновился buyerDiscount
        assertEquals(true, actualOrder.getProperty(OrderPropertyType.PUSHED_EXTRA_SUBSIDY));

        actualItem = actualOrder.getItems().iterator().next();
        assertEquals(0, BigDecimal.valueOf(150L).compareTo(actualItem.getPrices().getSubsidy()));
        assertEquals(0, BigDecimal.valueOf(150L).compareTo(actualItem.getPrices().getBuyerSubsidy()));
        actualPromo = actualItem.getPromos().iterator().next();
        assertEquals(0, BigDecimal.valueOf(150L).compareTo(actualPromo.getSubsidy()));
        assertEquals(0, BigDecimal.valueOf(150L).compareTo(actualPromo.getBuyerSubsidy()));
        subsidyPayments = paymentService.getPayments(actualOrder.getId(),
                ClientInfo.SYSTEM, PaymentGoal.SUBSIDY);
        assertThat(subsidyPayments, hasSize(1));
        Payment actualPayment = subsidyPayments.get(0);
        assertEquals(0, BigDecimal.valueOf(150L).compareTo(actualPayment.getTotalAmount()));

        //проверка чеков (два чека: основной и пуш субсидий)
        List<Receipt> receipts = receiptService.findByPayment(actualPayment);
        assertThat(receipts, hasSize(1));
        Receipt actualReceipt = receipts.get(0);

        assertNotNull(actualReceipt);
        assertThat(actualReceipt.getItems(), hasSize(1));

        ReceiptItem actualReceiptItem = actualReceipt.getItems().get(0);
        assertEquals(actualItem.getId(), actualReceiptItem.getItemId());
        assertNull(actualReceiptItem.getItemServiceId());
        assertNull(actualReceiptItem.getDeliveryId());
        assertEquals(0, BigDecimal.valueOf(150L).compareTo(actualReceiptItem.getAmount()));
    }

    @Test
    @DisplayName("Обычный позитивный кейс создания заказа без дополнительного пуша субсидии")
    public void testWithoutExtraSubsidy() throws Exception {
        Order order = succeedOrderCreation();

        Collection<Payment> payments = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM,
                PaymentGoal.SUBSIDY);
        assertThat(payments, hasSize(1));
        Payment payment = payments.iterator().next();
        assertEquals(0, BigDecimal.valueOf(10L).compareTo(payment.getTotalAmount()));
    }

    private Order succeedOrderCreation() throws Exception {
        Parameters whiteParameters = buildDefaultParametersWithPromo();

        Order order = createOrder(whiteParameters);

        assertEquals(0, order.getDelivery().getPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, order.getDelivery().getPrices().getSubsidy().compareTo(BigDecimal.valueOf(10)));
        assertThat(order.getDelivery().getPromos(), contains(
                ItemPromo.createWithSubsidy(PromoDefinition.builder()
                        .type(PromoType.FREE_DELIVERY_THRESHOLD)
                        .build(), BigDecimal.valueOf(10))));


        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_SUBSIDY_PAYMENT, order.getId()));
        queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());

        RequestClientInfo requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, 0L);
        OrderRequest orderRequest = OrderRequest.builder(order.getId()).withArchived(false).build();
        order = client.getOrder(requestClientInfo, orderRequest);

        assertThat(order.getDelivery().getSubsidyBalanceOrderId(), notNullValue());
        assertEquals(0, order.getDelivery().getPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, order.getDelivery().getPrices().getSubsidy().compareTo(BigDecimal.valueOf(10)));
        assertThat(order.getDelivery().getPromos(), contains(
                ItemPromo.createWithSubsidy(PromoDefinition.builder()
                        .type(PromoType.FREE_DELIVERY_THRESHOLD)
                        .build(), BigDecimal.valueOf(10))));
        return order;
    }

    private Order createOrderWithoutSubsidies() throws Exception {
        Parameters whiteParameters = buildParameters();

        Order order = createOrder(whiteParameters);

        assertEquals(0, order.getDelivery().getPrice().compareTo(BigDecimal.valueOf(10)));
        assertNull(order.getDelivery().getPrices().getSubsidy());
        assertThat(order.getDelivery().getPromos(), not(contains(
                ItemPromo.createWithSubsidy(PromoDefinition.builder()
                        .type(PromoType.FREE_DELIVERY_THRESHOLD)
                        .build(), BigDecimal.valueOf(10)))));


        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_SUBSIDY_PAYMENT, order.getId()));
        queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());

        RequestClientInfo requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, 0L);
        OrderRequest orderRequest = OrderRequest.builder(order.getId()).withArchived(false).build();
        order = client.getOrder(requestClientInfo, orderRequest);

        assertThat(order.getDelivery().getSubsidyBalanceOrderId(), nullValue());
        assertEquals(0, order.getDelivery().getPrice().compareTo(BigDecimal.valueOf(10)));
        assertNull(order.getDelivery().getPrices().getSubsidy());
        assertThat(order.getDelivery().getPromos(), not(contains(
                ItemPromo.createWithSubsidy(PromoDefinition.builder()
                        .type(PromoType.FREE_DELIVERY_THRESHOLD)
                        .build(), BigDecimal.valueOf(10)))));
        return order;
    }

    private ResultActions callPushExtraSubsidy(PushExtraSubsidyRequest request) throws Exception {
        return callPushExtraSubsidy(request, null);
    }

    private ResultActions callPushExtraSubsidy(PushExtraSubsidyRequest request, Boolean force) throws Exception {
        String content = testSerializationService.serializeCheckouterObject(request);
        MockHttpServletRequestBuilder requestBuilder = post(PUSH_EXTRA_SUBSIDY_ENDPOINT)
                .content(content)
                .contentType(MediaType.APPLICATION_JSON_UTF8);
        if (force != null) {
            requestBuilder.param("force", force.toString());
        }
        return mockMvc.perform(requestBuilder);
    }

    @SafeVarargs
    private PushExtraSubsidyRequest buildPushExtraSubsidyRequest(Long orderId, Map.Entry<Long, Long>... params) {
        PushExtraSubsidyRequest request = new PushExtraSubsidyRequest();
        request.setOrderId(orderId);
        request.setItems(buildPushExtraSubsidyItems(params));
        return request;
    }

    @SafeVarargs
    private List<PushExtraSubsidyRequest.Item> buildPushExtraSubsidyItems(Map.Entry<Long, Long>... params) {
        if (params == null || params.length == 0) {
            return Collections.emptyList();
        }

        return Arrays.stream(params)
                .map(param -> buildPushExtraSubsidyItem(param.getKey(), param.getValue(), SHOP_PROMO_KEY))
                .collect(Collectors.toList());
    }

    private PushExtraSubsidyRequest.Item buildPushExtraSubsidyItem(Long id, Long subsidyAmount, String shopPromoId) {
        PushExtraSubsidyRequest.Item item = new PushExtraSubsidyRequest.Item();
        item.setId(id);
        item.setSubsidy(BigDecimal.valueOf(subsidyAmount));
        item.setShopPromoId(shopPromoId);
        return item;
    }

    private Parameters buildDefaultParametersWithPromo() {
        Parameters whiteParameters = buildParameters();
        whiteParameters.setFreeDelivery(true);
        whiteParameters.getLoyaltyParameters()
                .addDeliveryDiscount(LoyaltyDiscount.discountFor(10, FREE_DELIVERY_THRESHOLD));
        return whiteParameters;
    }

    private Parameters buildParameters() {
        Parameters whiteParameters = WhiteParametersProvider.defaultWhiteParameters();
        whiteParameters.setMockLoyalty(true);
        whiteParameters.setFreeDelivery(false);
        whiteParameters.setCheckCartErrors(false);
        return whiteParameters;
    }

    private Order createOrder(Parameters parameters) throws Exception {
        MultiCart cart = orderCreateHelper.cart(parameters);

        MultiOrder orders = orderCreateHelper.checkout(cart, parameters);
        return orders.getOrders().iterator().next();
    }

    @SafeVarargs
    @Nonnull
    private FoundOffer applyDirectDiscount(
            @Nonnull OrderItem item,
            @Nonnull BigDecimal discount,
            @Nullable BigDecimal subsidy,
            Consumer<FoundOfferBuilder>... customizers
    ) {
        return promoConfigurer.applyDirectDiscount(item, DD_PROMO, ANAPLAN_ID, SHOP_PROMO_KEY, discount, subsidy,
                true, false, customizers);
    }
}
