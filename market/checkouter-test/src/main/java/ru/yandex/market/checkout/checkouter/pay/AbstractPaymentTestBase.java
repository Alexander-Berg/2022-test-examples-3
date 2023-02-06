package ru.yandex.market.checkout.checkouter.pay;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.backbone.fintech.AccountPaymentFeatureToggle;
import ru.yandex.market.checkout.checkouter.balance.BasketStatus;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderServiceTestHelper;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.returns.ReturnTestHelper;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryRouteProvider;
import ru.yandex.market.checkout.util.Holder;
import ru.yandex.market.checkout.util.b2b.B2bCustomersMockConfigurer;
import ru.yandex.market.checkout.util.balance.TrustMockConfigurer;
import ru.yandex.market.checkout.util.sberbank.SberMockConfigurer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author mkasumov
 */
public abstract class AbstractPaymentTestBase extends AbstractWebTestBase {

    @Autowired
    protected OrderPayHelper paymentHelper;
    @Autowired
    protected SberMockConfigurer sberMockConfigurer;
    @Autowired
    private B2bCustomersMockConfigurer b2bCustomersMockConfigurer;

    protected OrderServiceTestHelper orderServiceTestHelper;
    protected PaymentTestHelper paymentTestHelper;
    protected ReceiptTestHelper receiptTestHelper;
    protected RefundTestHelper refundTestHelper;
    protected ReturnTestHelper returnTestHelper;

    protected final Holder<ShopMetaData> shopMetaData = Holder.empty();
    protected final Holder<Order> order = Holder.empty();

    protected Order order() {
        return order.get();
    }

    @BeforeEach
    public void init() throws Exception {
        checkouterProperties.setEnableSeparateTotalAmountInPaymentByOrders(false);
        trustMockConfigurer.mockWholeTrust();
        orderServiceTestHelper = new OrderServiceTestHelper(this, order, shopMetaData);
        receiptTestHelper = new ReceiptTestHelper(this, order, shopMetaData);
        refundTestHelper = new RefundTestHelper(this, receiptTestHelper, order, shopMetaData);
        paymentTestHelper = new PaymentTestHelper(this, receiptTestHelper, refundTestHelper, order, shopMetaData);
        returnTestHelper = new ReturnTestHelper(this, receiptTestHelper, paymentTestHelper, order, shopMetaData,
                colorConfig);
    }

    @SuppressWarnings("checkstyle:HiddenField")
    public void createUnpaidB2bOrder() {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE, AccountPaymentFeatureToggle.ON);

        b2bCustomersMockConfigurer.mockGeneratePaymentInvoice();
        b2bCustomersMockConfigurer.mockIsClientCanOrder(BuyerProvider.UID,
                B2bCustomersTestProvider.BUSINESS_BALANCE_ID, true);

        var parameters = B2bCustomersTestProvider.defaultB2bParameters();
        parameters.getReportParameters().setDeliveryRoute(DeliveryRouteProvider.fromActualDelivery(
                parameters.getReportParameters().getActualDelivery(), DeliveryType.DELIVERY
        ));
        var order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getStatus(), Matchers.is(OrderStatus.UNPAID));

        this.order.set(order);
        receiptTestHelper.resetExpectedReceiptsCount();
    }

    @SuppressWarnings("checkstyle:HiddenField")
    public void createUnpaidOrder() {
        var order = orderServiceTestHelper.createOrderUnpaid(true);
        assertThat(order.getStatus(), Matchers.is(OrderStatus.UNPAID));
        receiptTestHelper.resetExpectedReceiptsCount();
    }

    public void createUnpaidBlueOrder() {
        orderServiceTestHelper.createUnpaidBlueOrder(null);
    }

    @SuppressWarnings("checkstyle:HiddenField")
    public void createUnpaidBlueOrderWithShopDelivery() {
        var order = orderServiceTestHelper.createUnpaidBlueOrderWithShopDelivery(null);
        assertThat(order, notNullValue());
        assertThat(order.getStatus(), Matchers.is(OrderStatus.UNPAID));
    }

    public void createUnpaid1PBlueOrder() {
        orderServiceTestHelper.createUnpaidBlue1POrder();
    }

    public void createUnpaidFFOrderWithDiffShop() {
        orderServiceTestHelper.createUnpaidFFOrderWithDiffShops();
    }

    public void createUnpaidFFOrder() {
        orderServiceTestHelper.createUnpaidFFBlueOrder();
    }

    protected ResultActions ordersPay(List<Long> orderIds, String returnPath) throws Exception {
        MockHttpServletRequestBuilder builder = post("/orders/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content((new JSONArray(orderIds)).toString())
                .param("uid", order().getBuyer().getUid().toString());
        if (returnPath != null) {
            builder.param("returnPath", returnPath);
        }

        return mockMvc.perform(builder);
    }

    protected void notifyPayment(List<Long> orderIds, Payment createdPayment) throws Exception {
        mockMvc.perform(
                        post("/payments/" + createdPayment.getId() + "/notify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("status", BasketStatus.success.name())
                                .content((new JSONArray(orderIds)).toString())
                                .param("uid", order().getBuyer().getUid().toString()))
                .andExpect(status().isOk());
    }

    protected void reloadOrder() {
        order.set(orderService.getOrder(order().getId()));
    }

    void validateThatPayBasketEventContainsXUid() {
        trustMockConfigurer.servedEvents().stream()
                .filter(e -> TrustMockConfigurer.PAY_BASKET_STUB.equals(e.getStubMapping().getName()))
                .forEach(e -> Assertions.assertNotNull(e.getRequest().getHeader("X-Uid")));
    }

    protected void createPaidCreditOrder() throws IOException {
        sberMockConfigurer.mockRegisterDo();
        sberMockConfigurer.mockGetOrderStatusCompleted();

        // 20 минут, чтобы при последующем срабатывании таски инспектора, платеж подхватился и отметился как прошедший
        setFixedTime(getClock().instant().minus(20, ChronoUnit.MINUTES));
        orderServiceTestHelper.createUnpaidBlueOrder(order -> order.setPaymentMethod(PaymentMethod.CREDIT));

        Payment payment = paymentHelper.pay(order().getId());
        assertNotNull(payment.getPaymentUrl());
        assertEquals(PaymentURLActionType.EXTERNAL_SITE, payment.getPaymentURLActionType());
        assertThat(order().getBuyerTotal().compareTo(payment.getTotalAmount()), equalTo(0));

        //перезагружаем платеж
        payment = orderService.getOrder(order().getId()).getPayment();
        assertEquals(PaymentGoal.CREDIT, payment.getType());

        clearFixed();
        runPaymentInspectorTask();

        //перегружаем заказ
        order.set(orderService.getOrder(order().getId()));
        assertEquals(OrderStatus.PROCESSING, order.get().getStatus());
    }

    protected void runPaymentInspectorTask() {
        tmsTaskHelper.runInspectExpiredPaymentTaskV2();
    }

    private Order createOrderWithCargotype(Parameters parameters, Integer count, Integer cargotype) {
        parameters.getOrders().get(0).getItems().forEach(item -> {
            if (count != null) {
                item.setCount(count);
                item.setValidIntQuantity(count);
            }
            if (cargotype != null) {
                item.setCargoTypes(Collections.singleton(cargotype));
            }
        });
        return orderCreateHelper.createOrder(parameters);
    }

    protected Order createDropshipOrderWithCargotype(Integer count, Integer cargotype) {
        Parameters parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        return createOrderWithCargotype(parameters, count, cargotype);
    }

    protected Order createDropshipPostpaidOrderWithCargotype(Integer count, Integer cargotype) {
        Parameters parameters = DropshipDeliveryHelper.getDropshipPostpaidParameters();
        return createOrderWithCargotype(parameters, count, cargotype);
    }

    @SuppressWarnings("checkstyle:HiddenField")
    protected Order createPaidDropshipOrderWithCargotype(Integer count, Integer cargotype) {
        Order order = createDropshipOrderWithCargotype(count, cargotype);
        paymentHelper.payForOrder(order);
        return orderService.getOrder(order.getId());
    }
}
