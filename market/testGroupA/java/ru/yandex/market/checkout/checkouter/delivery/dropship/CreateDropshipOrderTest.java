package ru.yandex.market.checkout.checkouter.delivery.dropship;

import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.collection.IsEmptyCollection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryResultProvider;
import ru.yandex.market.checkout.test.providers.DeliveryRouteProvider;
import ru.yandex.market.checkout.test.providers.LocalDeliveryOptionProvider;
import ru.yandex.market.checkout.util.Constants;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;


public class CreateDropshipOrderTest extends AbstractWebTestBase {

    private static final int ASNA_PHARMA_SHOP_ID = 595350;
    @Autowired
    protected PaymentService paymentService;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private QueuedCallService queuedCallService;
    private Parameters parameters;
    private Boolean oldLogicSelector;

    @BeforeEach
    public void setUp() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, 18);
        calendar.set(Calendar.MINUTE, 18);
        setFixedTime(calendar.toInstant());
        trustMockConfigurer.resetRequests();
    }


    @AfterEach
    public void tearDown() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.FORBID_FBS_POSTPAY_IN_SIBERIA, false);
    }

    @Test
    public void createPostpaidOrderAndPayTest() {
        parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        trustMockConfigurer.mockWholeTrust();

        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        Order order = orderCreateHelper.createOrder(parameters);
        orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.DELIVERY);
        orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.DELIVERED);
        List<Payment> payments = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM, PaymentGoal
                .ORDER_POSTPAY);
        Assertions.assertEquals(0, payments.size());

        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_CASH_PAYMENT);
        payments = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM, PaymentGoal
                .ORDER_POSTPAY);
        Assertions.assertEquals(1, payments.size());
        Assertions.assertEquals(PaymentStatus.IN_PROGRESS, payments.get(0).getStatus());
    }

    @Test
    public void checkDfoPostpaidFiltering() {
        parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        trustMockConfigurer.mockWholeTrust();
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        parameters.getOrder().getDelivery().setRegionId(121169L);
        parameters.getReportParameters().setRegionId(121169L);
        parameters.getBuiltMultiCart().setBuyerRegionId(121169L);

        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertEquals(121169L, order.getRegionId());
        Assertions.assertEquals(PaymentType.POSTPAID, order.getPaymentType());


        checkouterFeatureWriter.writeValue(BooleanFeatureType.FORBID_FBS_POSTPAY_IN_SIBERIA, true);
        parameters.setCheckCartErrors(false);

        MultiCart cart = orderCreateHelper.cart(parameters);
        Assertions.assertTrue(cart.getPaymentOptions().stream()
                .noneMatch(pm -> pm.getPaymentType() == PaymentType.POSTPAID), "Should not have postpaid");
    }

    @Test
    public void dontCreateASNAPharmaPostpaidOrderAndPayTest() {
        parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        trustMockConfigurer.mockWholeTrust();

        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        parameters.setShopId(ASNA_PHARMA_SHOP_ID);
        MultiCart cart = orderCreateHelper.cart(parameters);
        Assertions.assertTrue(cart.getCarts().get(0).getDeliveryOptions().stream()
                .flatMap(d -> d.getPaymentOptions().stream())
                .allMatch(p -> p.getPaymentType() == PaymentType.PREPAID), "Must have only prepaid payment options");
    }

    @Test
    public void shouldCreateDropshipOrder() {
        parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getRgb(), is(Color.BLUE));
        assertThat(order.getAcceptMethod(), is(OrderAcceptMethod.PUSH_API));
        assertThat(order.isFulfilment(), is(false));
        assertThat(order.getItems(), hasSize(1));
        assertThat(order.getItems().iterator().next().getWidth(), equalTo(DropshipDeliveryHelper.WIDTH));
        assertThat(order.getItems().iterator().next().getHeight(), equalTo(DropshipDeliveryHelper.HEIGHT));
        assertThat(order.getItems().iterator().next().getDepth(), equalTo(DropshipDeliveryHelper.DEPTH));
        assertThat(order.getDelivery().getDeliveryPartnerType(), is(DeliveryPartnerType.YANDEX_MARKET));
        assertThat(order.getDelivery().getDeliveryServiceId(),
                is(LocalDeliveryOptionProvider.DROPSHIP_DELIVERY_SERVICE_ID));
        assertThat(order.getDelivery().getParcels(), hasSize(1));
        assertThat(order.getDelivery().getParcels().get(0).getParcelItems(), hasSize(1));
    }

    @Test
    public void shouldCreateDropshipOrderWithCombinator() {
        parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();

        parameters.getReportParameters().setDeliveryRoute(DeliveryRouteProvider.fromActualDelivery(
                parameters.getReportParameters().getActualDelivery(), DeliveryType.DELIVERY
        ));
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setMinifyOutlets(true);
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        DeliveryRouteProvider.cleanActualDelivery(parameters.getReportParameters().getActualDelivery());

        Order order = orderCreateHelper.createOrder(parameters);

        JSONAssert.assertEquals(
                DeliveryResultProvider.ROUTE,
                order.getDelivery().getParcels().get(0).getRoute().toString(),
                JSONCompareMode.NON_EXTENSIBLE
        );
        assertThat(order.getRgb(), is(Color.BLUE));
        assertThat(order.getAcceptMethod(), is(OrderAcceptMethod.PUSH_API));
        assertThat(order.isFulfilment(), is(false));
        assertThat(order.getItems(), hasSize(1));
        assertThat(order.getItems().iterator().next().getWidth(), equalTo(DropshipDeliveryHelper.WIDTH));
        assertThat(order.getItems().iterator().next().getHeight(), equalTo(DropshipDeliveryHelper.HEIGHT));
        assertThat(order.getItems().iterator().next().getDepth(), equalTo(DropshipDeliveryHelper.DEPTH));
        assertThat(order.getDelivery().getDeliveryPartnerType(), is(DeliveryPartnerType.YANDEX_MARKET));
        assertThat(order.getDelivery().getDeliveryServiceId(),
                is(LocalDeliveryOptionProvider.DROPSHIP_DELIVERY_SERVICE_ID));
        assertThat(order.getDelivery().getParcels(), hasSize(1));
        assertThat(order.getDelivery().getParcels().get(0).getParcelItems(), hasSize(1));
    }

    @Test
    public void shouldMoveDropshipToProcessingAfterUnpaid() {
        parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), is(OrderStatus.PROCESSING));
    }

    @Test
    public void shouldCreateValidShipment() {
        parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        Order order = orderCreateHelper.createOrder(parameters);


        assertThat(order.getDelivery().getParcels().get(0).getShipmentDate(),
                is(DeliveryDates.daysOffsetToLocalDate(getClock(), 2)));
    }

    @Test
    public void filtersNonMardoDeliveryOptionsForDropship() {
        parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        parameters.getReportParameters().setActualDelivery(ActualDeliveryProvider.builder().build());
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        List<Delivery> nonMardoOptions = multiCart.getCarts()
                .stream()
                .flatMap(cart -> cart.getDeliveryOptions().stream())
                .filter(option -> option.getDeliveryPartnerType() != DeliveryPartnerType.YANDEX_MARKET)
                .collect(Collectors.toList());

        assertThat(nonMardoOptions, IsEmptyCollection.empty());
    }
}
