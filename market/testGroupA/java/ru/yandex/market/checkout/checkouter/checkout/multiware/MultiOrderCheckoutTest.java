package ru.yandex.market.checkout.checkouter.checkout.multiware;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.b2b.B2bCustomersMockConfigurer;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.marketoms.MarketOmsMockConfigurer;
import ru.yandex.market.checkout.util.report.ItemInfo;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.PROCAAS_EVENT;
import static ru.yandex.market.checkout.providers.FulfilmentProvider.TEST_SHOP_SKU;
import static ru.yandex.market.checkout.providers.FulfilmentProvider.TEST_SKU;

public class MultiOrderCheckoutTest extends AbstractWebTestBase {

    private static final String MARKET_REQUEST_ID = "OloloMultiOrder";
    private static final Long ANOTHER_SUPPLIER_ID = 999L;

    @Autowired
    private QueuedCallService queuedCallService;

    @Autowired
    private B2bCustomersMockConfigurer b2bCustomersMockConfigurer;
    @Autowired
    private MarketOmsMockConfigurer marketOmsMockConfigurer;
    @Autowired
    private WireMockServer marketOmsMock;

    @BeforeEach
    void init() {
        b2bCustomersMockConfigurer.mockIsClientCanOrder(BuyerProvider.UID,
                B2bCustomersTestProvider.BUSINESS_BALANCE_ID, true);

        marketOmsMock.resetRequests();
        marketOmsMockConfigurer.mockOrdersReserve();
    }

    @AfterEach
    void resetMocks() {
        b2bCustomersMockConfigurer.resetAll();
    }

    @Test
    public void checkoutMultiOrder() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addOrder(BlueParametersProvider.defaultBlueOrderParameters());
        parameters.setMarketRequestId(MARKET_REQUEST_ID);

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        assertThat(multiOrder, notNullValue());
        Order order1 = orderService.getOrder(multiOrder.getOrders().get(0).getId());
        Order order2 = orderService.getOrder(multiOrder.getOrders().get(1).getId());

        assertThat(order1.getProperty(OrderPropertyType.MULTI_ORDER_ID),
                equalTo(order2.getProperty(OrderPropertyType.MULTI_ORDER_ID)));
        assertThat(order1.getProperty(OrderPropertyType.MULTI_ORDER_SIZE), equalTo(2));
        assertThat(order2.getProperty(OrderPropertyType.MULTI_ORDER_SIZE), equalTo(2));
        assertThat(order1.getProperty(OrderPropertyType.MARKET_REQUEST_ID), equalTo(MARKET_REQUEST_ID));
        assertThat(order2.getProperty(OrderPropertyType.MARKET_REQUEST_ID), equalTo(MARKET_REQUEST_ID));
        assertThat(order1.getStatus(), equalTo(OrderStatus.PROCESSING));
        assertThat(order2.getStatus(), equalTo(OrderStatus.PROCESSING));
    }

    @Test
    public void checkoutMultiOrderWithAsyncCompletion() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SEND_PROCAAS_EVENT, true);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_CONCURRENT_MULTI_ORDER_COMPLETION, true);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.MARKET_OMS_RESERVATION, false);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addOrder(BlueParametersProvider.defaultBlueOrderParameters());
        parameters.setMarketRequestId(MARKET_REQUEST_ID);

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        assertThat(multiOrder, notNullValue());
        Order order1 = orderService.getOrder(multiOrder.getOrders().get(0).getId());
        Order order2 = orderService.getOrder(multiOrder.getOrders().get(1).getId());

        assertThat(order1.getProperty(OrderPropertyType.MULTI_ORDER_ID),
                equalTo(order2.getProperty(OrderPropertyType.MULTI_ORDER_ID)));
        assertThat(order1.getProperty(OrderPropertyType.MULTI_ORDER_SIZE), equalTo(2));
        assertThat(order2.getProperty(OrderPropertyType.MULTI_ORDER_SIZE), equalTo(2));
        assertThat(order1.getProperty(OrderPropertyType.MARKET_REQUEST_ID), equalTo(MARKET_REQUEST_ID));
        assertThat(order2.getProperty(OrderPropertyType.MARKET_REQUEST_ID), equalTo(MARKET_REQUEST_ID));
        assertThat(order1.getStatus(), equalTo(OrderStatus.PROCESSING));
        assertThat(order2.getStatus(), equalTo(OrderStatus.PROCESSING));

        Assertions.assertTrue(queuedCallService.existsQueuedCall(PROCAAS_EVENT, order1.getId()));
        Assertions.assertTrue(queuedCallService.existsQueuedCall(PROCAAS_EVENT, order2.getId()));

        checkouterFeatureWriter.writeValue(BooleanFeatureType.SEND_PROCAAS_EVENT, false);
    }

    @Test
    public void checkoutWhenPlaceMultiOrderAsyncShouldAllGoToProcessing() {
       Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addOrder(BlueParametersProvider.defaultBlueOrderParameters());
        parameters.setMarketRequestId(MARKET_REQUEST_ID);

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        assertThat(multiOrder, notNullValue());
        Order order1 = orderService.getOrder(multiOrder.getOrders().get(0).getId());
        Order order2 = orderService.getOrder(multiOrder.getOrders().get(1).getId());


        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(order1.getProperty(OrderPropertyType.MULTI_ORDER_ID))
                    .isEqualTo(order2.getProperty(OrderPropertyType.MULTI_ORDER_ID));
            softly.assertThat(order1.getProperty(OrderPropertyType.MARKET_REQUEST_ID))
                    .isEqualTo(MARKET_REQUEST_ID);
            softly.assertThat(order2.getProperty(OrderPropertyType.MARKET_REQUEST_ID))
                    .isEqualTo(MARKET_REQUEST_ID);
            softly.assertThat(order1.getStatus()).isEqualTo(OrderStatus.PROCESSING);
            softly.assertThat(order2.getStatus()).isEqualTo(OrderStatus.PROCESSING);
            softly.assertThat(order1.getId()).isNotEqualTo(order2.getId());
        });
    }

    @Test
    public void checkoutWhenEnabledNewShipmentCreationShouldCreateNewShipmentForEachOrder() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addOrder(BlueParametersProvider.defaultBlueOrderParameters());
        parameters.setMarketRequestId(MARKET_REQUEST_ID);

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        assertThat(multiOrder, notNullValue());
        Order order1 = orderService.getOrder(multiOrder.getOrders().get(0).getId());
        Order order2 = orderService.getOrder(multiOrder.getOrders().get(1).getId());


        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(order1.getProperty(OrderPropertyType.MULTI_ORDER_ID))
                    .isEqualTo(order2.getProperty(OrderPropertyType.MULTI_ORDER_ID));
            softly.assertThat(order1.getStatus()).isEqualTo(OrderStatus.PROCESSING);
            softly.assertThat(order2.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        });
    }

    @Test
    public void checkoutMultiOrderWithMultiplePaymentMethods() throws Exception {
        // 1st cart
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        // 2nd cart
        OrderItem anotherOrderItem = OrderItemProvider.getAnotherOrderItem();
        anotherOrderItem.setSupplierId(ANOTHER_SUPPLIER_ID);
        Parameters cashOnlyParameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(anotherOrderItem);
        cashOnlyParameters.getReportParameters().setDeliveryPartnerTypes(singletonList("SHOP"));
        cashOnlyParameters.getOrder().setItems(Collections.singleton(anotherOrderItem));
        cashOnlyParameters.setupFulfillment(new ItemInfo.Fulfilment(ANOTHER_SUPPLIER_ID, TEST_SKU, TEST_SHOP_SKU));
        cashOnlyParameters.getOrder().getItems().forEach(oi -> oi.setSupplierId(ANOTHER_SUPPLIER_ID));
        cashOnlyParameters.addShopMetaData(ANOTHER_SUPPLIER_ID, ShopSettingsHelper.getPostpayMeta());
        makeCashOnly(cashOnlyParameters);
        cashOnlyParameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        parameters.addOrder(cashOnlyParameters);

        MultiCart cart = orderCreateHelper.cart(parameters);
        MultiOrder multiOrder = checkoutWithPostpayPessimization(cart, parameters);
        assertThat(multiOrder, notNullValue());

        assertThat(multiOrder.getOrders(), hasItem(hasProperty("paymentType", equalTo(PaymentType.POSTPAID))));
        assertThat(multiOrder.getOrders(), hasItem(hasProperty("paymentType", equalTo(PaymentType.PREPAID))));
    }

    private void makeCashOnly(Parameters parameters) {
        // Мока пушапи формирует ответ с постоплатой на основе способов оплаты опций доставки
        parameters.getPushApiDeliveryResponses().forEach(
                d -> {
                    Set<PaymentMethod> paymentMethods = new HashSet<>();
                    paymentMethods.add(PaymentMethod.CASH_ON_DELIVERY);
                    paymentMethods.add(PaymentMethod.CARD_ON_DELIVERY);
                    d.setPaymentOptions(paymentMethods);
                }
        );
    }

    private MultiOrder checkoutWithPostpayPessimization(MultiCart cart, Parameters parameters) throws Exception {
        cart.setPaymentMethod(null);
        cart.setPaymentType(null);
        cart.getCarts().forEach(o -> {
            PaymentMethod paymentMethod = o.getPaymentOptions().stream()
                    // Фильтрую по методу, а не по типу, чтобы в тесте не было экзотики вроде APPLE_PAY
                    .filter(po -> po == PaymentMethod.YANDEX)
                    .findAny()
                    .orElse(
                            o.getPaymentOptions().stream()
                                    .findAny()
                                    .orElseThrow(() -> new RuntimeException("No payment options"))
                    );
            o.setPaymentMethod(paymentMethod);
            o.setPaymentType(paymentMethod.getPaymentType());
        });
        return orderCreateHelper.checkout(cart, parameters);
    }
}
