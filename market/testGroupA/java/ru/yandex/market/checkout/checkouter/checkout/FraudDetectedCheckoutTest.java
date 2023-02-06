package ru.yandex.market.checkout.checkouter.checkout;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.AntifraudCheckResult;
import ru.yandex.market.antifraud.orders.entity.OrderVerdict;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemResponseDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderResponseDto;
import ru.yandex.market.antifraud.orders.web.entity.OrderItemChange;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.actual.ActualItem;
import ru.yandex.market.checkout.checkouter.cart.CartChange;
import ru.yandex.market.checkout.checkouter.cart.ChangeReason;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentOperations;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.pay.TrustBasketKey;
import ru.yandex.market.checkout.checkouter.pay.balance.PaymentFormType;
import ru.yandex.market.checkout.checkouter.pay.cashier.CreatePaymentContext;
import ru.yandex.market.checkout.helpers.ActualizeHelper;
import ru.yandex.market.checkout.helpers.utils.ActualizeParameters;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.ActualItemProvider;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.balance.TrustMockConfigurer;
import ru.yandex.market.checkout.util.json.JsonTest;
import ru.yandex.market.checkout.util.mstat.MstatAntifraudConfigurer;
import ru.yandex.market.checkout.util.report.ItemInfo;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class FraudDetectedCheckoutTest extends AbstractWebTestBase {

    private static final String BASKET_ID = "58f4abba795be27b78188f0e";
    private static final String PURCHASE_TOKEN = "fbcdba795be27b7817def7654";
    private static final OrderVerdict FRAUD_VERDICT = OrderVerdict.builder()
            .checkResults(Collections.singleton(
                    new AntifraudCheckResult(AntifraudAction.CANCEL_ORDER, "", "")))
            .build();

    @Autowired
    private MstatAntifraudConfigurer mstatAntifraudConfigurer;
    @Autowired
    private ActualizeHelper actualizeHelper;
    @Autowired
    @Qualifier("routingPaymentOperations")
    private PaymentOperations paymentOperations;

    @BeforeEach
    public void init() {
        trustMockConfigurer.mockWholeTrust(new TrustBasketKey(BASKET_ID, PURCHASE_TOKEN));
    }

    @Test
    public void shouldFailCartIfFraudIsDetected() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setCheckCartErrors(false);
        mstatAntifraudConfigurer.mockVerdict(FRAUD_VERDICT);
        MultiCart multiCart = orderCreateHelper.cart(parameters);

        assertThat(multiCart.getCartFailures(), nullValue());

        assertThat(multiCart.getValidationErrorsCount(), CoreMatchers.is(1));
        assertThat(multiCart.getValidationErrors(), hasSize(1));
        assertThat(multiCart.getValidationErrors().get(0).getCode(), CoreMatchers.is("FRAUD_DETECTED"));
    }

    @Test
    public void shouldFailCheckoutIfFraudIsDetected() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setMultiCartAction(mc ->
                mstatAntifraudConfigurer.mockVerdict(FRAUD_VERDICT));

        parameters.setCheckOrderCreateErrors(false);
        MultiOrder order = orderCreateHelper.createMultiOrder(parameters);

        assertThat(order.isValid(), is(false));
        assertThat(order.getOrderFailures(), nullValue());

        assertThat(order.getValidationErrors(), hasSize(1));
        assertThat(order.getValidationErrors().get(0).getCode(),
                CoreMatchers.is("FRAUD_DETECTED"));
    }

    @Test
    public void shouldNotFailActualizeIfFraudIsDetected() throws Exception {
        mstatAntifraudConfigurer.mockVerdict(FRAUD_VERDICT);

        ActualItem actualItem = ActualItemProvider.buildActualItem();

        ActualizeParameters parameters = new ActualizeParameters(actualItem);
        parameters.getReportParameters().overrideItemInfo(actualItem.getFeedOfferId())
                .setFulfilment(new ItemInfo.Fulfilment(FulfilmentProvider.FF_SHOP_ID, FulfilmentProvider.TEST_SKU,
                        FulfilmentProvider.TEST_SHOP_SKU, FulfilmentProvider.TEST_WAREHOUSE_ID, true));
        parameters.getReportParameters().getOrder().getItems().forEach(oi -> oi.setWeight(10L));
        actualizeHelper.actualizeItemForActions(parameters)
                .andExpect(status().isOk());
    }

    @Test
    public void shouldFailCheckoutIfActionFraudIsDetected() {
        Parameters parameters = defaultBlueOrderParameters();

        Set<AntifraudCheckResult> checkResults = new HashSet<>(
                Collections.singletonList(new AntifraudCheckResult(AntifraudAction.CANCEL_ORDER, "", ""))
        );
        parameters.setMultiCartAction(mc ->
                mstatAntifraudConfigurer.mockVerdict(OrderVerdict.builder().checkResults(checkResults).build()));

        parameters.setCheckOrderCreateErrors(false);
        MultiOrder order = orderCreateHelper.createMultiOrder(parameters);

        assertThat(order.isValid(), Matchers.is(false));

        assertThat(order.getOrderFailures(), nullValue());

        assertThat(order.getOrders().get(0).getValidationErrors(), hasSize(1));
        assertThat(order.getOrders().get(0).getValidationErrors().get(0).getCode(),
                CoreMatchers.is("FRAUD_DETECTED"));
    }

    @Test
    public void shouldCheckoutIfActionRobocall() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);

        Set<AntifraudCheckResult> checkResults = new HashSet<>(
                Collections.singletonList(new AntifraudCheckResult(AntifraudAction.ROBOCALL, "", ""))
        );
        parameters.setMultiCartAction(mc ->
                mstatAntifraudConfigurer.mockVerdict(OrderVerdict.builder().checkResults(checkResults).build()));

        parameters.setCheckOrderCreateErrors(false);
        parameters.setErrorMatcher(jsonPath("$.checkedOut").value(true));
        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertEquals(OrderStatus.PENDING, order.getStatus());
        Assertions.assertEquals(OrderSubstatus.AWAIT_CONFIRMATION, order.getSubstatus());
        Boolean callNeeds = order.getProperty(OrderPropertyType.ASYNC_OUTLET_ANTIFRAUD);
        Assertions.assertNotNull(callNeeds);
        Assertions.assertTrue(callNeeds);
    }

    @Test
    public void shouldCheckoutIfActionPrepaidDelivery() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        Set<AntifraudCheckResult> checkResults = new HashSet<>(
                Collections.singletonList(new AntifraudCheckResult(AntifraudAction.PREPAID_ONLY, "", ""))
        );
        mstatAntifraudConfigurer.mockVerdict(OrderVerdict.builder().checkResults(checkResults).build());

        parameters.setCheckOrderCreateErrors(false);
        parameters.setErrorMatcher(jsonPath("$.checkedOut").value(true));
        MultiCart cart = orderCreateHelper.cart(parameters);

        Predicate<? super PaymentMethod> isPostpaid = option -> option.getPaymentType() == PaymentType.POSTPAID;
        Assertions.assertEquals(0,
                cart.getPaymentOptions().stream().filter(isPostpaid).count());

        Assertions.assertNull(cart.getPaymentMethod());
    }

    @Test
    public void shouldCheckoutIfActionDoesntForceThreeDs() {
        Parameters parameters = new Parameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        Set<AntifraudCheckResult> checkResults = new HashSet<>();
        parameters.setMultiCartAction(mc ->
                mstatAntifraudConfigurer.mockVerdict(OrderVerdict.builder().checkResults(checkResults).build()));

        parameters.setCheckOrderCreateErrors(false);
        parameters.setErrorMatcher(jsonPath("$.checkedOut").value(true));
        Order order = orderCreateHelper.createOrder(parameters);

        Boolean forceThreeDs = order.getProperty(OrderPropertyType.FORCE_THREE_DS);
        Assertions.assertNull(forceThreeDs);

        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, BuyerProvider.UID);

        Payment payment = paymentOperations.startPrepayPayment(
                order.getId(),
                clientInfo,
                "RETURN_PATH",
                null,
                PaymentFormType.DESKTOP,
                false,
                CreatePaymentContext.builder().build()
        );

        Assertions.assertNotNull(payment);
        List<LoggedRequest> requests = trustMockConfigurer.trustMock().findRequestsMatching(
                postRequestedFor(urlEqualTo(TrustMockConfigurer.TRUST_PAYMENTS_CREATE_BASKET_URL)).build()
        ).getRequests();
        requests.forEach(
                r -> {
                    String body = r.getBodyAsString();
                    JsonTest.checkJsonMatcher(r.getBodyAsString(), "$.pass_params.market_blue_3ds_policy",
                            containsString("UNKNOWN"));
                    JsonTest.checkJsonNotExist(body, "$.force_3ds");
                }
        );
    }

    @Test
    public void shouldFailCheckoutIfActionOrderItemChange() throws Exception {
        OrderItem item = OrderItemProvider.orderItemWithSortingCenter().build();
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(item);

        Set<AntifraudCheckResult> checkResults = Set.of(
                new AntifraudCheckResult(AntifraudAction.ROBOCALL, "", ""),
                new AntifraudCheckResult(AntifraudAction.ORDER_ITEM_CHANGE, "", "")
        );
        Set<OrderItemChange> changes = Set.of(
                OrderItemChange.MISSING,
                OrderItemChange.COUNT
        );

        OrderItemResponseDto orderItemResponse = OrderItemResponseDto.builder()
                .offerId(item.getOfferId())
                .feedId(item.getFeedId())
                .bundleId(item.getBundleId())
                .count(0)
                .changes(changes)
                .build();
        OrderResponseDto fixedOrderResponse = new OrderResponseDto(List.of(orderItemResponse));

        OrderVerdict orderVerdict = OrderVerdict.builder()
                .checkResults(checkResults)
                .fixedOrder(fixedOrderResponse).build();

        parameters.setMultiCartAction(mc ->
                mstatAntifraudConfigurer.mockVerdict(orderVerdict));

        parameters.turnOffErrorChecks();
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        assertFalse(multiOrder.isValid());

        List<OrderFailure> orderFailures = multiOrder.getOrderFailures();
        assertFalse(orderFailures.isEmpty());

        Order order = orderFailures.get(0).getOrder();
        assertThat(order.getChanges(), hasItem(CartChange.FRAUD_FIXED));
        assertThat(order.getChangesReasons(), hasEntry(CoreMatchers.is(CartChange.FRAUD_FIXED),
                CoreMatchers.everyItem(CoreMatchers.is(ChangeReason.FRAUD_FIXED))));

        OrderItem orderItem = order.getItems().iterator().next();
        assertThat(orderItem.getCount(), Matchers.equalTo(0));
        assertThat(orderItem.getChanges(), containsInAnyOrder(ItemChange.COUNT, ItemChange.MISSING));
    }
}
