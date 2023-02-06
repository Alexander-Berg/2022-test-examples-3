package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.gson.JsonObject;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.CompatibleCancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.order.changerequest.EditPossibilityWrapper;
import ru.yandex.market.checkout.checkouter.order.changerequest.OrderEditPossibility;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.request.PaymentRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.balance.OneElementBackIterator;
import ru.yandex.market.checkout.util.balance.TrustMockConfigurer;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkout.util.balance.checkers.CreateBasketParams;
import ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker;
import ru.yandex.market.checkout.util.balance.checkers.TrustCallsParamsProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_PHONE_ID;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.UNPAID;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.PAYMENT_CALL_BALANCE_UPDATE_PAYMENT;
import static ru.yandex.market.checkout.util.balance.ShopSettingsHelper.metaWithEmptySupplierName;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_CREDIT;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_ORDERS_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.LOAD_PARTNER_STUB;

/**
 * @author : poluektov
 * date: 2020-11-18.
 */
public class TinkoffCreditPaymentTest extends AbstractWebTestBase {

    @Autowired
    protected WireMockServer shopInfoMock;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private CheckouterClient client;

    @Test
    public void testCreditOrderPayment() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);

        Order createdOrder = orderCreateHelper.createOrder(parameters);
        assertThat(createdOrder.getStatus(), equalTo(OrderStatus.UNPAID));
        assertThat(createdOrder.getPaymentMethod(), equalTo(PaymentMethod.TINKOFF_CREDIT));

        orderPayHelper.pay(createdOrder.getId());
        Order paidOrder = orderService.getOrder(createdOrder.getId());
        Payment payment = paidOrder.getPayment();
        assertThat(payment.getType(), equalTo(PaymentGoal.TINKOFF_CREDIT));
    }

    @Test
    public void testCreditOrderStatusChange() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);

        Order createdOrder = orderCreateHelper.createOrder(parameters);
        assertThat(createdOrder.getStatus(), equalTo(OrderStatus.UNPAID));
        assertThat(createdOrder.getPaymentMethod(), equalTo(PaymentMethod.TINKOFF_CREDIT));

        orderPayHelper.payForOrder(createdOrder);
        Order paidOrder = orderService.getOrder(createdOrder.getId());
        assertThat(paidOrder.getStatus(), equalTo(OrderStatus.PROCESSING));
        Payment payment = paidOrder.getPayment();
        assertThat(payment.getType(), equalTo(PaymentGoal.TINKOFF_CREDIT));
    }

    @Test
    public void testUpdatePaymentForCreditOrder() {
        checkouterProperties.setEnableUpdatePaymentMode(true);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);
        Order createdOrder = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(createdOrder);
        Order paidOrder = orderService.getOrder(createdOrder.getId());
        Payment payment = paidOrder.getPayment();

        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildPostAuth());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildPostAuth(), null);
        orderPayHelper.notifyPaymentClear(payment);
        payment = orderService.getOrder(paidOrder.getId()).getPayment();
        //При переходе в клир выставляется QC и он успешно проходит.
        assertThat(payment.getStatus(), equalTo(PaymentStatus.CLEARED));
        assertFalse(queuedCallService.existsQueuedCall(PAYMENT_CALL_BALANCE_UPDATE_PAYMENT, payment.getId()));
    }


    @Test
    public void testTrustCalls() {
        checkouterProperties.setEnableCessionPassParams(true);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);

        Order createdOrder = orderCreateHelper.createOrder(parameters);
        assertThat(createdOrder.getPaymentMethod(), equalTo(PaymentMethod.TINKOFF_CREDIT));

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();
        orderPayHelper.pay(createdOrder.getId());
        Order order = orderService.getOrder(createdOrder.getId());


        OneElementBackIterator<ServeEvent> iterator = trustMockConfigurer.eventsIterator();

        TrustCallsChecker.skipTrustCall(iterator, LOAD_PARTNER_STUB);
        TrustCallsChecker.checkOptionalCreateServiceProductCall(iterator, null);
        TrustCallsChecker.checkOptionalCreateServiceProductCall(iterator, null);
        TrustCallsChecker.skipTrustCall(iterator, CREATE_ORDERS_STUB);
        CreateBasketParams createBasket = TrustCallsParamsProvider.createBasketFulfilmentParams(order,
                order.getPaymentId());
        createBasket.withUserIp(null);
        createBasket.withPayMethodId("credit");
        createBasket.withReturnPath("http://localhost/!!ORDER_ID!!");
        createBasket.withPassParams(notNullValue(String.class));
        createBasket.withPassParams(containsString("\"credit\":{\"sellers\":["));
        createBasket.withPaymentTimeout(Matchers.equalTo("1800"));
        createBasket.withDeveloperPayload("{\"ProcessThroughYt\":1,\"call_preview_payment\":\"card_info\"}");
        TrustBasketKey key = TrustCallsChecker.checkCreateCreditCall(iterator, createBasket);
    }

    @Test
    public void testMetaDataNullSupplierName() {
        checkouterProperties.setEnabledSupplierNameUpdate(true);
        checkouterProperties.setEnableCessionPassParams(true);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);
        final Long shopId = parameters.getItems().iterator().next().getSupplierId();
        parameters.addShopMetaData(shopId, metaWithEmptySupplierName(shopId.intValue()));
        Order createdOrder = orderCreateHelper.createOrder(parameters);

        var oldMeta = shopService.getMeta(shopId);

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();
        final var suppplierName = "тестовый шоп";
        mockSupplierInfo(shopId, suppplierName);

        orderPayHelper.pay(createdOrder.getId());
        var updatedMeta = shopService.getMeta(shopId);

        assertNull(oldMeta.getSupplierName());
        assertNotNull(updatedMeta.getSupplierName());
        var createCreditCall = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_CREDIT))
                .findAny().get().getRequest().getBodyAsString();
        assertThat(createCreditCall, containsString("\"sellers\":[{\"inn\":\"1234567890\",\"companyName\":\"тестовый " +
                "шоп\"}]}"));
    }

    @Test
    public void testMetaDataNullSupplierNameRetry() {
        checkouterProperties.setEnabledSupplierNameUpdate(true);
        checkouterProperties.setEnableCessionPassParams(true);

        final Long shopId = 8883322214L;
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);
        parameters.addShopMetaData(shopId, metaWithEmptySupplierName(shopId.intValue()));
        parameters.setShopId(shopId);
        parameters.getOrder().setItems(List.of(customSuplierItem(shopId)));
        Order createdOrder = orderCreateHelper.createOrder(parameters);

        var oldMeta = shopService.getMeta(shopId);

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();
        final var suppplierName = "тестовый шоп2";
        mockNullSupplierInfo(shopId);
        mockShopInfo(shopId, suppplierName);

        orderPayHelper.pay(createdOrder.getId());
        var updatedMeta = shopService.getMeta(shopId);

        assertNull(oldMeta.getSupplierName());
        assertNotNull(updatedMeta.getSupplierName());
        var createCreditCall = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_CREDIT))
                .findAny().get().getRequest().getBodyAsString();
        assertThat(createCreditCall, containsString("\"sellers\":[{\"inn\":\"1234567890\",\"companyName\":\"тестовый " +
                "шоп2\"}]}"));
    }

    /**
     * Удалить в MARKETCHECKOUT-27094
     */
    @ParameterizedTest
    @CsvSource(
            value = {
                    "DISABLE:+77777777000",
                    "APPOINTMENT:+77777777001",
                    "REJECT:+77777777002",
                    "SES:+77777777003",
                    "DISABLE:+7 777 777-70-00",
                    "APPOINTMENT:+7 777 777-70-01",
                    "REJECT:+7 777 777-70-02",
                    "SES:+7 777 777-70-03",
                    "APPOINTMENT_REJECT:+7 777 777-70-04",
                    "PAPERLESS:+7 777 777-70-05"
            },
            delimiter = ':'
    )
    public void testCreditDemoFlowWithPhone(String creditDemoFlowStringValue, String phone) {
        CreditDemoFlow creditDemoFlow = CreditDemoFlow.valueOf(creditDemoFlowStringValue);
        Buyer buyer = BuyerProvider.getBuyer();
        buyer.setPhone(phone);
        buyer.setPersonalPhoneId(null);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(buyer);
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);

        Order createdOrder = orderCreateHelper.createOrder(parameters);
        assertThat(createdOrder.getPaymentMethod(), equalTo(PaymentMethod.TINKOFF_CREDIT));

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();
        orderPayHelper.pay(createdOrder.getId());

        JsonObject creditJsonRequest =
                TrustCallsChecker.getRequestBodyAsJson(trustMockConfigurer.servedEvents().stream()
                        .filter(e -> TrustMockConfigurer.CREATE_CREDIT.equals(e.getStubMapping().getName()))
                        .findFirst().orElseGet(() -> fail("creditJsonRequest not found")));
        if (creditDemoFlow == CreditDemoFlow.DISABLE) {
            assertNull(creditJsonRequest.getAsJsonObject("pass_params").get("demo_flow"));
        } else {
            assertEquals(creditDemoFlow.getValue(),
                    creditJsonRequest.getAsJsonObject("pass_params").get("demo_flow").getAsString());
        }
    }

    @ParameterizedTest
    @CsvSource(
            value = {
                    "APPOINTMENT:4ac20b57ca7e415b9b25ab56269777b3", // +77777777001
                    "REJECT:9fa8c0c2c3ea436e83ecdeb2d7315b67", //` +77777777002
                    "SES:9456d18783b64e07b1402644e7e2251b", // +77777777003
                    "APPOINTMENT_REJECT:0e050e3c258f4020bc2077d6a9a5d683", // +77777777004
                    "PAPERLESS:a04b29a45e1c46f6ac51596bbd82c394" // +77777777005
            },
            delimiter = ':'
    )
    public void testCreditDemoFlow(String creditDemoFlowStringValue, String personalPhoneId) {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_PHONE_ID, true);
        CreditDemoFlow creditDemoFlow = CreditDemoFlow.valueOf(creditDemoFlowStringValue);
        Buyer buyer = BuyerProvider.getBuyer();
        buyer.setPhone(null);
        buyer.setNormalizedPhone(null);
        buyer.setPersonalPhoneId(personalPhoneId);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(buyer);
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);

        Order createdOrder = orderCreateHelper.createOrder(parameters);
        assertThat(createdOrder.getPaymentMethod(), equalTo(PaymentMethod.TINKOFF_CREDIT));

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();
        orderPayHelper.pay(createdOrder.getId());

        JsonObject creditJsonRequest =
                TrustCallsChecker.getRequestBodyAsJson(trustMockConfigurer.servedEvents().stream()
                        .filter(e -> TrustMockConfigurer.CREATE_CREDIT.equals(e.getStubMapping().getName()))
                        .findFirst().orElseGet(() -> fail("creditJsonRequest not found")));
        if (creditDemoFlow == CreditDemoFlow.DISABLE) {
            assertNull(creditJsonRequest.getAsJsonObject("pass_params").get("demo_flow"));
        } else {
            assertEquals(creditDemoFlow.getValue(),
                    creditJsonRequest.getAsJsonObject("pass_params").get("demo_flow").getAsString());
        }
    }

    @Test
    public void changePaymentMethodPossibilityTest() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);
        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getStatus(), CoreMatchers.equalTo(UNPAID));

        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Collections.singleton(order.getId()), ClientRole.USER, BuyerProvider.UID,
                Collections.singletonList(Color.BLUE));
        assertThat(orderEditPossibilityList, hasSize(1));
        OrderEditPossibility orderEditPossibility = orderEditPossibilityList.get(0);
        EditPossibilityWrapper editPossibilityWrapper =
                EditPossibilityWrapper.build(orderEditPossibility.getEditPossibilities());
        assertTrue(editPossibilityWrapper.isPossible(ChangeRequestType.PAYMENT_METHOD));
    }

    @Test
    public void shouldFailOnCancelledOrder() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);
        Order order = orderCreateHelper.createOrder(parameters);
        Order cancelledOrder = orderStatusHelper.proceedOrderFromUnpaidToCancelled(order);
        assertThat(cancelledOrder.getStatus(), CoreMatchers.equalTo(CANCELLED));

        trustMockConfigurer.mockWholeTrust();
        ErrorCodeException errorCodeException = assertThrows(ErrorCodeException.class,
                () -> client.payments().payOrder(cancelledOrder.getId(), order.getBuyer().getUid(), "", null, false,
                        null));
        assertEquals(400, errorCodeException.getStatusCode());
        assertEquals("order_invalid", errorCodeException.getCode());
    }

    @Test
    public void shouldCancelTinkoffCreditOrderFromUnpaidWaitingBankDecision() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);
        Order order = orderCreateHelper.createOrder(parameters);

        Payment payment = orderPayHelper.payForOrderWithoutNotification(order);
        orderPayHelper.notifyWaitingBankDecision(payment);
        orderPayHelper.notifyPaymentCancel(payment);
        order = orderService.getOrder(order.getId());

        assertThat(order.getStatus(), equalTo(OrderStatus.CANCELLED));
        assertThat(order.getSubstatus(), equalTo(OrderSubstatus.USER_NOT_PAID));
    }

    @Test
    public void shouldCancelTinkoffCreditOrderFromUnpaidWaitingBankDecisionByUserAction() {
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);
        var order = orderCreateHelper.createOrder(parameters);

        var payment = orderPayHelper.payForOrderWithoutNotification(order);
        orderPayHelper.notifyWaitingBankDecision(payment);

        order = orderService.getOrder(order.getId());

        assertThat(order.getStatus(), equalTo(UNPAID));
        assertThat(order.getSubstatus(), equalTo(OrderSubstatus.WAITING_TINKOFF_DECISION));
        assertThat(order.getPayment().getStatus(), equalTo(PaymentStatus.WAITING_BANK_DECISION));

        assertFalse(trustMockConfigurer.trustMock().getAllServeEvents().stream()
                .map(ServeEvent::getRequest)
                .map(LoggedRequest::getUrl)
                .anyMatch(url -> url.contains(payment.getBasketKey().getPurchaseToken() + "/unhold"))
        );

        var cancellationRequest = new CompatibleCancellationRequest(
                OrderSubstatus.USER_CHANGED_MIND.name(),
                "notes"
        );
        var canceledOrder = client.createCancellationRequest(
                order.getId(),
                cancellationRequest,
                ClientRole.USER,
                BuyerProvider.UID,
                singletonList(order.getRgb()));

        assertThat(canceledOrder.getStatus(), equalTo(CANCELLED));
        assertThat(canceledOrder.getSubstatus(), equalTo(OrderSubstatus.USER_CHANGED_MIND));
        assertThat(canceledOrder.getPayment().getStatus(), equalTo(PaymentStatus.WAITING_BANK_DECISION));

        assertThat(queuedCallService.findQueuedCalls(CheckouterQCType.ORDER_REFUND, canceledOrder.getId()), hasSize(1));
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);
        assertThat(queuedCallService.findQueuedCalls(CheckouterQCType.ORDER_REFUND, canceledOrder.getId()), hasSize(1));
        assertThat(paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM).getStatus(),
                equalTo(PaymentStatus.WAITING_BANK_DECISION));
        setFixedTime(getClock().instant().plus(1, ChronoUnit.DAYS));
        orderPayHelper.notifyPaymentCancel(payment);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);
        assertThat(queuedCallService.findQueuedCalls(CheckouterQCType.ORDER_REFUND, canceledOrder.getId()), hasSize(0));

        order = orderService.getOrder(order.getId());

        assertThat(order.getStatus(), equalTo(CANCELLED));
        assertThat(order.getSubstatus(), equalTo(OrderSubstatus.USER_CHANGED_MIND));
        assertThat(order.getPayment().getStatus(), equalTo(PaymentStatus.CANCELLED));

        var creditUnholdCount = trustMockConfigurer.trustMock().getAllServeEvents().stream()
                .map(ServeEvent::getRequest)
                .map(LoggedRequest::getUrl)
                .filter(url -> url.contains(
                        "/credit/" + payment.getBasketKey().getPurchaseToken() + "/unhold"))
                .count();

        assertThat("Must be only 1 unhold call in trust", creditUnholdCount, equalTo(1L));
    }


    @Test
    public void testCessionOrderUpdate() {
        checkouterProperties.setEnableCessionPassParams(true);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);

        Order createdOrder = orderCreateHelper.createOrder(parameters);
        assertThat(createdOrder.getStatus(), equalTo(OrderStatus.UNPAID));
        assertThat(createdOrder.getPaymentMethod(), equalTo(PaymentMethod.TINKOFF_CREDIT));

        orderPayHelper.payForOrderWithoutNotification(createdOrder);
        Order unpaidOrder = orderService.getOrder(createdOrder.getId());
        Payment tinkoffPayment = unpaidOrder.getPayment();
        assertThat(tinkoffPayment.getType(), equalTo(PaymentGoal.TINKOFF_CREDIT));

        orderPayHelper.notifyTinkoffCessionClear(tinkoffPayment);
        Order processingOrder = orderService.getOrder(createdOrder.getId());

        PaymentRequest request = PaymentRequest.builder(processingOrder.getPayment().getId()).build();
        Payment cessionPayment = client.payments().getPayment(new RequestClientInfo(ClientRole.SYSTEM, 0L), request);

        assertThat(processingOrder.getStatus(), equalTo(OrderStatus.PROCESSING));
        assertThat(cessionPayment.getStatus(), equalTo(PaymentStatus.CLEARED));
        assertTrue(cessionPayment.getProperties().getCession());

        Receipt receipt = receiptService.findByPayment(cessionPayment, ReceiptType.INCOME).iterator().next();
        assertThat(receipt.getStatus(), equalTo(ReceiptStatus.GENERATED));
        assertFalse(receipt.isPrintable());
    }

    @Test
    public void testCheckoutCreditOrderWithMultiColorCart() {
        checkouterProperties.setSetOrderColorUsingReportOfferColor(true);
        Parameters whiteParameters = BlueParametersProvider.defaultBlueOrderParameters();
        var orderItem = whiteParameters.getOrder().getItems().iterator().next();
        orderItem.setCount(1);
        whiteParameters.getReportParameters().setOffers(List.of(
                FoundOfferBuilder.createFrom(orderItem)
                        .configure(WhiteParametersProvider.whiteOffer())
                        .build()
        ));
        whiteParameters.setShowCredits(true);
        var blueParameters = BlueParametersProvider.defaultBlueOrderParameters();
        whiteParameters.addOrder(blueParameters);
        whiteParameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);

        var createdOrder = orderCreateHelper.createMultiOrder(whiteParameters);
        assertThat(
                createdOrder.getOrders().stream().map(Order::getRgb).collect(Collectors.toSet()),
                equalTo(Set.of(Color.BLUE, Color.WHITE))
        );
        createdOrder.getOrders().forEach(order -> {
            assertThat(order.getPaymentMethod(), equalTo(PaymentMethod.TINKOFF_CREDIT));
        });

        orderPayHelper.payForOrders(createdOrder.getOrders());
        var orders = createdOrder.getOrders().stream()
                .map(order -> orderService.getOrder(order.getId()))
                .collect(Collectors.toList());
        assertThat(orders.stream().map(Order::getRgb).collect(Collectors.toSet()),
                equalTo(Set.of(Color.BLUE, Color.WHITE))
        );

        orders.forEach(order -> {
            assertThat(order.getPaymentMethod(), equalTo(PaymentMethod.TINKOFF_CREDIT));
            assertThat(order.getPayment().getType(), equalTo(PaymentGoal.TINKOFF_CREDIT));
        });
    }

    private void mockSupplierInfo(long shopId, String supplierName) {
        shopInfoMock.stubFor(WireMock.get(("/supplierNames?supplier-id=" + shopId))
                .willReturn(aResponse().withBody("[\n" +
                        "    {\n" +
                        "        \"id\": " + shopId + ",\n" +
                        "        \"name\": \"" + supplierName + "\"\n" +
                        "    }\n" +
                        "]")));
    }

    private void mockNullSupplierInfo(long shopId) {
        shopInfoMock.stubFor(WireMock.get(("/supplierNames?supplier-id=" + shopId))
                .willReturn(aResponse().withBody("[{\"id\":" + shopId + ",\"name\":null,\"slug\":null}]")));
    }

    private void mockShopInfo(long shopId, String supplierName) {
        shopInfoMock.stubFor(WireMock.get(("/shopNames?shop-id=" + shopId))
                .willReturn(aResponse().withBody("[\n" +
                        "    {\n" +
                        "        \"id\": " + shopId + ",\n" +
                        "        \"name\": \"" + supplierName + "\"\n" +
                        "    }\n" +
                        "]")));
    }


    private OrderItem customSuplierItem(long supplierId) {
        OrderItem newItem = OrderItemProvider.buildOrderItem("item-1", new BigDecimal("111.00"), 1);
        newItem.setMsku(332L);
        newItem.setShopSku("sku-1");
        newItem.setSku("332");
        newItem.setWareMd5(OrderItemProvider.OTHER_WARE_MD5);
        newItem.setShowInfo(OrderItemProvider.OTHER_SHOW_INFO);
        newItem.setSupplierId(supplierId);
        return newItem;
    }
}
