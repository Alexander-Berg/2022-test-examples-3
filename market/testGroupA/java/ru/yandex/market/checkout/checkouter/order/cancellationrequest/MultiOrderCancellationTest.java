package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.order.BasicOrder;
import ru.yandex.market.checkout.checkouter.order.BulkOrderCancellationResponse;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.CancellationRequestNotAllowed;
import ru.yandex.market.checkout.checkouter.order.CancelledOrderInfo;
import ru.yandex.market.checkout.checkouter.order.CompatibleCancellationRequest;
import ru.yandex.market.checkout.checkouter.order.DeliveryEditRequest;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderCancellationListResult;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.checkouter.validation.PaymentMethodNotApplicableError;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.client.CheckoutCommonParams.X_EXPERIMENTS;
import static ru.yandex.market.checkout.checkouter.feature.type.common.MapFeatureType.MULTI_CART_MIN_COSTS_BY_REGION;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.AS_PART_OF_MULTI_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_MULTIORDER_CANCEL;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_MULTIORDER_CANCEL_VALUE;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

/**
 * @author kl1san
 */
@DisplayName("Тесты сценариев эксперимента по частичной отмене мультизаказа")
public class MultiOrderCancellationTest extends AbstractWebTestBase {

    @Autowired
    private OrderStatusHelper orderStatusHelper;

    @Test
    @DisplayName("Возвращать список заказов на отмену")
    public void shouldReturnCancellationList() {
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(new Parameters(3));
        Long requestedToCancelOrderId = multiOrder.getOrders().get(0).getId();
        setDefaultRegionMinCost(BigDecimal.valueOf(99999));

        OrderCancellationListResult cancellationListResult = client.getOrderCancellationList(requestedToCancelOrderId);
        assertEquals(requestedToCancelOrderId, cancellationListResult.getWithOrderId());
        assertEquals(3, cancellationListResult.getOrdersToCancel().size());
        assertThat(cancellationListResult.getOrdersToCancel(), everyItem(
                allOf(hasProperty("id", notNullValue()),
                        hasProperty("buyerTotal", notNullValue()),
                        hasProperty("buyerCurrency", notNullValue()),
                        hasProperty("itemsCount", notNullValue()))
        ));
    }

    @Test
    @DisplayName("Не разрешать отменять заказ отдельно, если остаток перестает удовлетворять " +
            "условиям по минимальной сумме корзины и передан флаг эксперимента")
    public void shouldNotAllowSeparateCancellationOfMultiOrderPart() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.addOrder(defaultBlueOrderParameters());

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        CompatibleCancellationRequest cancellationRequest =
                new CompatibleCancellationRequest(USER_CHANGED_MIND.name(), "note");
        Experiments multiCancelExp = Experiments.empty();
        multiCancelExp.addExperiment(MARKET_MULTIORDER_CANCEL, MARKET_MULTIORDER_CANCEL_VALUE);
        Long orderId = multiOrder.getOrders().get(0).getId();
        CancellationRequestNotAllowed expected = CancellationRequestNotAllowed.prohibitedToCancelSeparately(orderId);
        setDefaultRegionMinCost(BigDecimal.valueOf(999999));

        ErrorCodeException actualException = assertThrows(ErrorCodeException.class,
                () -> client.createCancellationRequest(orderId, cancellationRequest,
                        ClientRole.USER, BuyerProvider.UID, null, multiCancelExp.toExperimentString()));
        assertEquals(expected.getStatusCode(), actualException.getStatusCode());
        assertEquals(expected.getCode(), actualException.getCode());
        assertEquals(expected.getMessage(), actualException.getMessage());
    }

    private HttpHeaders buildExperimentHeader() {
        Experiments experiments = Experiments.empty();
        experiments.addExperiment(MARKET_MULTIORDER_CANCEL, MARKET_MULTIORDER_CANCEL_VALUE);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(X_EXPERIMENTS, experiments.toExperimentString());
        return httpHeaders;
    }

    @Test
    @DisplayName("Не разрешать частичную отмену, если оставшиеся заказы перестают удовлетворять минимальному порогу")
    public void whenRemainingOrdersDoNotHitRegionLimit() {
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(new Parameters(3));
        Long someOrderId = multiOrder.getOrders().get(0).getId();

        setDefaultRegionMinCost(BigDecimal.valueOf(999999));
        Collection<Long> alsoCancelOrderIds = orderService.shouldAlsoBeCancelledOrders(someOrderId).stream()
                .map(BasicOrder::getId)
                .collect(Collectors.toSet());

        Set<Long> initialOrderIdsExceptSelected = multiOrder.getOrders().stream()
                .map(BasicOrder::getId)
                .filter(id -> !someOrderId.equals(id))
                .collect(Collectors.toSet());

        assertAll(
                () -> assertEquals(2, alsoCancelOrderIds.size()),
                () -> assertThat(alsoCancelOrderIds, not(contains(
                        hasProperty("id", is(someOrderId)))))
        );
        assertEquals(initialOrderIdsExceptSelected, alsoCancelOrderIds);
    }

    @Test
    @DisplayName("Разрешать частичную отмену, если оставшиеся заказы все еще удовлетворяют минимальному порогу")
    public void whenRemainingOrdersHitRegionLimit() {
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(new Parameters(3));
        Long someOrderId = multiOrder.getOrders().get(0).getId();

        setDefaultRegionMinCost(BigDecimal.valueOf(10));
        Collection<Order> alsoCancelOrders = orderService.shouldAlsoBeCancelledOrders(someOrderId);
        assertTrue(alsoCancelOrders.isEmpty());
    }

    @Test
    @DisplayName("В условии мульти отмены не учитываем еще не оплаченные заказы")
    public void whenPaidPartially() {
        Parameters parameters = BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(500));
        parameters.addOrder(BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(500)));
        parameters.addOrder(BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(500)));
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        Order unpaidOrder1 = multiOrder.getOrders().get(0);
        Order unpaidOrder2 = multiOrder.getOrders().get(1);
        Order processingOrder = multiOrder.getOrders().get(2);
        orderStatusHelper.proceedOrderToStatus(processingOrder, OrderStatus.PROCESSING);

        setDefaultRegionMinCost(BigDecimal.valueOf(900));
        Collection<Order> alsoCancelWithProcessing = orderService.shouldAlsoBeCancelledOrders(processingOrder.getId());
        Collection<Order> alsoCancelWithUnpaid1 = orderService.shouldAlsoBeCancelledOrders(unpaidOrder1.getId());
        assertEquals(2, alsoCancelWithUnpaid1.size(),
                "Заказ должен отмениться полностью, т.к оплаченная часть меньше суммы минимальной корзины");
        assertEquals(2, alsoCancelWithProcessing.size(),
                "Заказ должен отмениться полностью, т.к оплаченная часть меньше суммы минимальной корзины");
        assertThat(alsoCancelWithUnpaid1, containsInAnyOrder(
                hasProperty("id", is(unpaidOrder2.getId())),
                hasProperty("id", is(processingOrder.getId()))
        ));
        assertThat(alsoCancelWithProcessing, containsInAnyOrder(
                hasProperty("id", is(unpaidOrder1.getId())),
                hasProperty("id", is(unpaidOrder2.getId()))
        ));
    }

    @Test
    @DisplayName("Позволяем отменить заказ с нашим факапом в любом случае")
    public void whenServiceFailedDelivery() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.addOrder(defaultBlueOrderParameters());
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        Order order = multiOrder.getOrders().get(0);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .fromDate(LocalDate.now(getClock()).plusDays(5))
                .toDate(LocalDate.now(getClock()).plusDays(5))
                .reason(HistoryEventReason.DELIVERY_SERVICE_DELAYED)
                .build());
        client.editOrder(order.getId(), ClientRole.SYSTEM, null,
                singletonList(BLUE), orderEditRequest);

        setDefaultRegionMinCost(BigDecimal.valueOf(99999));
        List<Order> ordersToCancel = orderService.shouldAlsoBeCancelledOrders(order.getId());
        assertTrue(ordersToCancel.isEmpty(), "Заказ, с нашим факапом можно отменять отдельно");
    }

    @Test
    @DisplayName("В условии мульти отмены не учитываем отмененные заказы")
    public void whenContainsCancelled() {
        Parameters parameters = BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(500));
        parameters.addOrder(BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(500)));
        parameters.addOrder(BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(500)));
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        orderStatusHelper.proceedOrderFromUnpaidToCancelled(multiOrder.getOrders().get(0));
        Order processing1 = orderStatusHelper.proceedOrderToStatus(multiOrder.getOrders().get(1),
                OrderStatus.PROCESSING);
        Order processing2 = orderStatusHelper.proceedOrderToStatus(multiOrder.getOrders().get(2),
                OrderStatus.PROCESSING);

        setDefaultRegionMinCost(BigDecimal.valueOf(1100));
        List<Order> alsoCancelOrders = orderService.shouldAlsoBeCancelledOrders(processing1.getId());

        assertAll("Должны отменить второй активный заказ, т.к сумма корзины " +
                        "исключая уже отмененный и запрашиваемый меньше порога",
                () -> assertEquals(1, alsoCancelOrders.size()),
                () -> assertThat(alsoCancelOrders, containsInAnyOrder(
                        hasProperty("id", is(processing2.getId()))))
        );
    }

    @Test
    @DisplayName("Не запрашиваем отмену уже доставленных заказов")
    public void whenContainsDelivered() {
        Parameters parameters = BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(500));
        parameters.addOrder(BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(500)));
        parameters.addOrder(BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(500)));
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);

        Order delivered = orderStatusHelper.proceedOrderToStatus(multiOrder.getOrders().get(0), OrderStatus.DELIVERED);
        Order processing1 = orderStatusHelper.proceedOrderToStatus(multiOrder.getOrders().get(1),
                OrderStatus.PROCESSING);
        orderStatusHelper.proceedOrderToStatus(multiOrder.getOrders().get(2), OrderStatus.PROCESSING);

        setDefaultRegionMinCost(BigDecimal.valueOf(900));
        List<Order> alsoCancelOrders = orderService.shouldAlsoBeCancelledOrders(processing1.getId());
        assertTrue(alsoCancelOrders.isEmpty(),
                "Доставленные заказы должны учитываться при расчете остатка в корзине");

        setDefaultRegionMinCost(BigDecimal.valueOf(1100));
        List<Order> alsoCancelWithoutDelivered = orderService.shouldAlsoBeCancelledOrders(processing1.getId());
        assertAll("Не запрашивать отмену уже доставленных заказов",
                () -> assertEquals(1, alsoCancelWithoutDelivered.size()),
                () -> assertNotEquals(delivered.getId(), alsoCancelWithoutDelivered.get(0).getId())
        );
    }

    @Test
    @DisplayName("Берем специфичные сабстатусы отмены в приоритете при балковой отмене")
    public void shouldCreateCancellationRequestsWithSpecificStatus() {
        var parameters = defaultBlueOrderParameters();
        parameters.addOrder(defaultBlueOrderParameters());
        var multiOrder = orderCreateHelper.createMultiOrder(parameters);
        Order order1 = multiOrder.getOrders().get(0);
        Order order2 = multiOrder.getOrders().get(1);
        var changedMind = new CompatibleCancellationRequest(USER_CHANGED_MIND.name(), "notes");
        var specificRequestMap = Map.of(order2.getId(),
                new CompatibleCancellationRequest(AS_PART_OF_MULTI_ORDER.name(), "notes"));

        orderStatusHelper.proceedOrderToStatus(order1, OrderStatus.DELIVERY);
        orderStatusHelper.proceedOrderToStatus(order2, OrderStatus.DELIVERY);
        setDefaultRegionMinCost(BigDecimal.valueOf(500));
        BulkOrderCancellationResponse response = client.bulkCreateCancellationRequestsWithResponseBody(
                ClientRole.USER,
                BuyerProvider.UID,
                changedMind,
                List.of(order1.getId()),
                specificRequestMap);

        CancellationRequest cr1 = orderService.getOrder(order1.getId()).getCancellationRequest();
        CancellationRequest cr2 = orderService.getOrder(order2.getId()).getCancellationRequest();

        assertAll(
                () -> assertThat(response.getCancelledOrders(), containsInAnyOrder(order2.getId(), order1.getId())),
                () -> assertThat(response.getCancelledOrdersInfo().stream()
                                .map(CancelledOrderInfo::getOrderId).collect(Collectors.toList()),
                        containsInAnyOrder(order2.getId(), order1.getId())),
                () -> assertThat(response.getCancelledOrdersInfo().stream()
                        .flatMap(it -> it.getChangeRequestInfo().stream())
                        .collect(Collectors.toList()), hasSize(4)),
                () -> assertNotNull(cr1), () -> assertNotNull(cr2),
                () -> assertEquals(USER_CHANGED_MIND, cr1.getSubstatus()),
                () -> assertEquals(AS_PART_OF_MULTI_ORDER, cr2.getSubstatus()));
    }

    @Test
    @DisplayName("При балковой отмене частей мульти заказа еще раз валидировать состав отменяемых заказов")
    public void shouldFailToCancelNotCompleteSetOfMultiOrder() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.addOrder(defaultBlueOrderParameters());
        parameters.addOrder(defaultBlueOrderParameters());
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        Long order1 = multiOrder.getOrders().get(0).getId();
        Long order2 = multiOrder.getOrders().get(1).getId();
        Long order3 = multiOrder.getOrders().get(2).getId();
        Map<Long, CompatibleCancellationRequest> validMap = Map.of(
                order1, new CompatibleCancellationRequest(USER_CHANGED_MIND.name(), "notes"),
                order2, new CompatibleCancellationRequest(AS_PART_OF_MULTI_ORDER.name(), "notes"),
                order3, new CompatibleCancellationRequest(AS_PART_OF_MULTI_ORDER.name(), "notes")
        );
        Map<Long, CompatibleCancellationRequest> mapWithoutUserRequested = new HashMap<>(validMap);
        Map<Long, CompatibleCancellationRequest> mapWithoutOnePart = new HashMap<>(validMap);
        mapWithoutUserRequested.remove(order1);
        mapWithoutOnePart.remove(order2);

        assertThrows(ErrorCodeException.class,
                () -> client.bulkCreateCancellationRequestsWithResponseBody(ClientRole.USER, BuyerProvider.UID,
                        null, null, mapWithoutUserRequested)
        );
        assertThrows(ErrorCodeException.class,
                () -> client.bulkCreateCancellationRequestsWithResponseBody(ClientRole.USER, BuyerProvider.UID,
                        null, null, mapWithoutOnePart)
        );
    }

    @Test
    @DisplayName("Должны отключить POSTPAID, если есть дешевое отправление")
    public void shouldDisablePostpaid() {
        setDefaultRegionMinCost(BigDecimal.valueOf(900));
        Parameters parameters = defaultBlueOrderParameters();
        //способ оплаты перед /cart еще не выбран
        parameters.setPaymentMethod(null);
        parameters.getOrders().forEach(order -> {
            order.setPaymentMethod(null);
        });
        MultiCart multiOrder = orderCreateHelper.cart(parameters, buildExperimentHeader());
        Set<PaymentMethod> paymentOptions = multiOrder.getPaymentOptions();

        assertAll("Should not return any POSTPAID options",
                () -> assertFalse(paymentOptions.isEmpty()),
                () -> assertThat(paymentOptions, everyItem(hasProperty("paymentType",
                        not(PaymentType.POSTPAID))))
        );
    }

    @Test
    @DisplayName("Не даем оформить POSTPAID в обход /cart")
    public void shouldNotCheckoutPostpaid() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.addOrder(defaultBlueOrderParameters());
        parameters.configuration().checkout().orderOptions().values()
                .forEach(p -> p.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET));
        parameters.getBuiltMultiCart().setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        parameters.getBuiltMultiCart().setPaymentType(PaymentType.POSTPAID);
        parameters.turnOffErrorChecks();
        Experiments experiments = Experiments.empty();
        experiments.addExperiment(MARKET_MULTIORDER_CANCEL, MARKET_MULTIORDER_CANCEL_VALUE);
        parameters.setExperiments(experiments.toExperimentString());
        parameters.setMultiCartAction(multiCart -> {
            multiCart.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
            multiCart.setPaymentType(PaymentType.POSTPAID);
        });
        setDefaultRegionMinCost(BigDecimal.valueOf(900));
        MultiOrder order = orderCreateHelper.createMultiOrder(parameters);

        PaymentMethodNotApplicableError expected = new PaymentMethodNotApplicableError();
        assertAll("Checkout should fail with PaymentMethodNotApplicableError error",
                () -> assertFalse(order.isValid()),
                () -> assertThat(order.getValidationErrors(), hasItem(allOf(
                        hasProperty("code", equalTo(expected.getCode())),
                        hasProperty("severity", equalTo(expected.getSeverity()))
                )))
        );
    }

    private void setDefaultRegionMinCost(BigDecimal cost) {
        checkouterFeatureWriter.writeValue(MULTI_CART_MIN_COSTS_BY_REGION, Map.of(213L, cost));
    }

}
