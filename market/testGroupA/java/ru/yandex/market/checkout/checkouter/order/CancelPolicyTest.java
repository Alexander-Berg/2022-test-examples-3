package ru.yandex.market.checkout.checkouter.order;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.validation.cancelpolicy.CancelPolicyData;
import ru.yandex.market.checkout.checkouter.order.validation.cancelpolicy.CancelPolicyDataValidationService;
import ru.yandex.market.checkout.checkouter.storage.cancelpolicy.CancelPolicyDao;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.OrdersGetHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.matching.CheckoutErrorMatchers;
import ru.yandex.market.common.report.model.CancelType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CancelPolicyTest extends AbstractWebTestBase {

    @Autowired
    private CancelPolicyDao cancelPolicyDao;
    @Autowired
    private OrdersGetHelper ordersGetHelper;
    @Autowired
    private OrderGetHelper orderGetHelper;
    @Autowired
    private CheckouterClient checkouterAPI;

    @Autowired
    private CancelPolicyDataValidationService cancelPolicyDataValidationService;

    public static Stream<Arguments> parameterizedTestData() {
        return Arrays.stream(new Object[][]{
                {OrderStatus.PROCESSING},
                {OrderStatus.DELIVERY},
                {OrderStatus.DELIVERED}
        }).map(Arguments::of);
    }

    @Test
    public void insertCancelDataTest() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        CancelPolicyData orderCancelPolicy = new CancelPolicyData(CancelType.TIME_LIMIT);
        orderCancelPolicy.setDaysForCancel(3);
        orderCancelPolicy.setReason("abc abc");

        parameters.getOrder().getItems().iterator().next().setCancelPolicyData(orderCancelPolicy);

        Order order = orderCreateHelper.createOrder(parameters);
        OrderItem someItem = order.getItems().iterator().next();

        Optional<CancelPolicyData> cancelPolicyDataDb = cancelPolicyDao.find(someItem.getId());
        assertTrue(cancelPolicyDataDb.isPresent());

        CancelPolicyData policyData = cancelPolicyDataDb.get();
        assertEquals(CancelType.TIME_LIMIT, policyData.getType());
        assertEquals("abc abc", policyData.getReason());
        assertEquals(3, policyData.getDaysForCancel());

        CancelPolicyData cancelPolicyData = someItem.getCancelPolicyData();
        assertNotNull(cancelPolicyData);

        assertEquals(policyData.getType(), cancelPolicyData.getType());
        assertEquals(policyData.getReason(), cancelPolicyData.getReason());
        assertEquals(policyData.getDaysForCancel(), cancelPolicyData.getDaysForCancel());
    }

    @Test
    public void checkNegativeDaysErrorValidationTest() {
        CancelPolicyData cancelPolicyData = new CancelPolicyData(CancelType.TIME_LIMIT);
        cancelPolicyData.setDaysForCancel(-2);

        assertThrows(IllegalArgumentException.class,
                () -> cancelPolicyDataValidationService.validateFields(cancelPolicyData));
    }

    @Test
    public void checkSingletonCancelPolicyForTwoItemsExceptionTest() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        CancelPolicyData orderCancelPolicy = new CancelPolicyData(CancelType.TIME_LIMIT);
        orderCancelPolicy.setDaysForCancel(3);
        orderCancelPolicy.setReason("abc abc");

        parameters.getOrder().getItems().iterator().next().setCancelPolicyData(orderCancelPolicy);

        parameters.getOrder().addItem(OrderItemProvider.getOrderItem());

        parameters.setErrorMatcher(CheckoutErrorMatchers.missingCarts);

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        assertThat(multiCart.getCartFailures().get(0).getErrorDetails(),
                containsStringIgnoringCase("Order has singleton cancel policy, but orderItem amount is multiple"));
    }

    @Test
    public void checkMultipleCancelPolicyForTwoItemsExceptionTest() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        CancelPolicyData orderCancelPolicy1 = new CancelPolicyData(CancelType.TIME_LIMIT);
        orderCancelPolicy1.setDaysForCancel(3);
        orderCancelPolicy1.setReason("abc abc");

        CancelPolicyData orderCancelPolicy2 = new CancelPolicyData(CancelType.TIME_LIMIT);
        orderCancelPolicy2.setDaysForCancel(5);
        orderCancelPolicy2.setReason("abc abc 2");

        parameters.getOrder().getItems().iterator().next().setCancelPolicyData(orderCancelPolicy1);

        OrderItem secondItem = OrderItemProvider.getOrderItem();
        secondItem.setCancelPolicyData(orderCancelPolicy2);
        parameters.getOrder().addItem(secondItem);

        parameters.setErrorMatcher(CheckoutErrorMatchers.missingCarts);

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        assertThat(multiCart.getCartFailures().get(0).getErrorDetails(),
                containsStringIgnoringCase("Order has multiple cancel policy"));
    }

    @Test
    public void hasUniqueOrderAndCancelDateTest() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.getReportParameters().setUniqueOffer(true);

        CancelPolicyData orderCancelPolicy = new CancelPolicyData(CancelType.TIME_LIMIT);
        orderCancelPolicy.setDaysForCancel(3);

        parameters.getOrder().getItems().iterator().next().setCancelPolicyData(orderCancelPolicy);

        Order order = orderCreateHelper.createOrder(parameters);

        assertTrue(order.getProperty(OrderPropertyType.UNIQUE_ORDER));

        assertNotNull(order.getCancelExpiryDate());
        assertNotNull(order.getCancelExpiryDateTs());

        Order orderDb = orderService.getOrder(order.getId());

        assertEquals(order.getCancelExpiryDate(),
                Date.from(orderDb.getCancelExpiryDate().toInstant().truncatedTo(ChronoUnit.SECONDS)));

        LocalDateTime expireLocalDateTime =
                order.getCancelExpiryDate().toInstant().atZone(getClock().getZone()).toLocalDateTime();

        LocalDateTime createdLocalDateTime =
                order.getCreationDate().toInstant().atZone(getClock().getZone()).toLocalDateTime();

        Duration betweenCreated = Duration.between(createdLocalDateTime, expireLocalDateTime);

        assertEquals(4, betweenCreated.toDays());
    }

    @Test
    public void uniqueOrderWithTwoItemExceptionTest() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.getOrder().addItem(OrderItemProvider.getOrderItem());
        parameters.getReportParameters().setUniqueOffer(true);

        parameters.setErrorMatcher(CheckoutErrorMatchers.missingCarts);

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        assertThat(multiCart.getCartFailures().get(0).getErrorDetails(),
                containsStringIgnoringCase("Unique order must have single item"));
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void checkStatusChangeToCancelledExceptionTest(OrderStatus orderStatus) throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.getReportParameters().setUniqueOffer(true);

        CancelPolicyData orderCancelPolicy = new CancelPolicyData(CancelType.TIME_LIMIT);
        orderCancelPolicy.setDaysForCancel(3);

        parameters.getOrder().getItems().iterator().next().setCancelPolicyData(orderCancelPolicy);

        Order order = orderCreateHelper.createOrder(parameters);

        Order orderToStatus = orderStatusHelper.proceedOrderToStatus(order, orderStatus);

        assertEquals(orderStatus, orderToStatus.getStatus());
        setFixedTime(Instant.now().plus(4, ChronoUnit.DAYS));

        ResultActionsContainer resultActionsContainer = new ResultActionsContainer();
        resultActionsContainer.andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(
                        anyOf(containsString(OrderStatusNotAllowedException.CANCEL_POLICY_ORDER_CODE),
                                containsString(OrderStatusNotAllowedException.NOT_ALLOWED_CODE))));

        orderStatusHelper.updateOrderStatus(order.getId(),
                ClientInfo.builder(ClientRole.USER).withId(BuyerProvider.UID).build(),
                OrderStatus.CANCELLED,
                OrderSubstatus.USER_WANTS_TO_CHANGE_ADDRESS,
                resultActionsContainer,
                null);

        Order readOrder = orderGetHelper.getOrder(order.getId(), ClientInfo.SYSTEM);

        OrderCancelPolicy readOrderCancelPolicy = readOrder.getOrderCancelPolicy();
        assertNotNull(readOrderCancelPolicy);

        assertTrue(readOrderCancelPolicy.getNotAvailable());
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void checkStatusChangeToCancelledSuccessTest(OrderStatus orderStatus) {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.getReportParameters().setUniqueOffer(true);

        CancelPolicyData orderCancelPolicy = new CancelPolicyData(CancelType.TIME_LIMIT);
        orderCancelPolicy.setDaysForCancel(3);

        parameters.getOrder().getItems().iterator().next().setCancelPolicyData(orderCancelPolicy);

        Order order = orderCreateHelper.createOrder(parameters);

        Order orderToStatus = orderStatusHelper.proceedOrderToStatus(order, orderStatus);

        assertEquals(orderStatus, orderToStatus.getStatus());
        setFixedTime(Instant.now().plus(2, ChronoUnit.DAYS));

        ResultActionsContainer resultActionsContainer = new ResultActionsContainer();

        if (orderStatus == OrderStatus.DELIVERED) {
            resultActionsContainer.andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("$.code").value(OrderStatusNotAllowedException.NOT_ALLOWED_CODE));
        } else {
            resultActionsContainer.andExpect(status().is2xxSuccessful());
        }

        orderStatusHelper.updateOrderStatus(order.getId(),
                ClientInfo.builder(ClientRole.USER).withId(BuyerProvider.UID).build(),
                OrderStatus.CANCELLED,
                OrderSubstatus.USER_WANTS_TO_CHANGE_ADDRESS,
                resultActionsContainer,
                null);
    }

    private Parameters createParametersWithCancelPolicy() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.getReportParameters().setUniqueOffer(true);

        CancelPolicyData orderCancelPolicy = new CancelPolicyData(CancelType.TIME_LIMIT);
        orderCancelPolicy.setDaysForCancel(3);
        orderCancelPolicy.setReason("REASON");

        parameters.getOrder().getItems().iterator().next().setCancelPolicyData(orderCancelPolicy);

        return parameters;
    }

    @Test
    public void cartContractValidationTest() {
        Parameters parameters = createParametersWithCancelPolicy();
        MultiCart cart = orderCreateHelper.cart(parameters);

        baseOrderValidation(cart.getCarts().get(0));
    }

    @Test
    public void checkoutContractValidationTest() throws Exception {
        Parameters parameters = createParametersWithCancelPolicy();
        MultiCart cart = orderCreateHelper.cart(parameters);

        MultiOrder multiOrder = orderCreateHelper.checkout(cart, parameters);

        fullOrderValidation(multiOrder.getOrders().get(0));
    }

    @Test
    public void getOrdersContractValidationTest() throws Exception {
        Parameters parameters = createParametersWithCancelPolicy();
        parameters.getBuyer().setUid(53523534L);
        orderCreateHelper.createOrder(parameters);

        PagedOrders orders = ordersGetHelper.getOrders(
                ClientInfo.createFromJson(ClientRole.USER,
                        parameters.getBuyer().getUid(),
                        parameters.getBuyer().getUid(),
                         null, null));

        assertThat(orders.getItems(), hasSize(1));

        fullOrderValidation(orders.getItems().iterator().next());
    }

    @Test
    public void getOrderContractValidationTest() throws Exception {
        Parameters parameters = createParametersWithCancelPolicy();
        parameters.getBuyer().setUid(53523534L);
        Order order = orderCreateHelper.createOrder(parameters);

        fullOrderValidation(orderGetHelper.getOrder(order.getId(), ClientInfo.SYSTEM));
    }

    @Test
    public void getOrderByUidContractValidationTest() {
        Parameters parameters = createParametersWithCancelPolicy();
        parameters.getBuyer().setUid(53523534L);
        Order order = orderCreateHelper.createOrder(parameters);

        OrderSearchRequest searchRequest = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .withPageInfo(Pager.atPage(0, 50))
                .build();

        PagedOrders orders = checkouterAPI.getOrdersByUser(searchRequest, order.getBuyer().getUid());
        assertThat(orders.getItems(), hasSize(1));

        fullOrderValidation(orders.getItems().iterator().next());
    }

    @Test
    public void cancelledOrderHasNotAvailableCancelPolicyTest() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.getReportParameters().setUniqueOffer(true);

        CancelPolicyData orderCancelPolicy = new CancelPolicyData(CancelType.TIME_LIMIT);
        orderCancelPolicy.setDaysForCancel(3);

        parameters.getOrder().getItems().iterator().next().setCancelPolicyData(orderCancelPolicy);

        Order order = orderCreateHelper.createOrder(parameters);

        orderStatusHelper.updateOrderStatus(order.getId(),
                ClientInfo.builder(ClientRole.USER).withId(BuyerProvider.UID).build(),
                OrderStatus.CANCELLED,
                OrderSubstatus.USER_WANTS_TO_CHANGE_ADDRESS);


        Order readOrder = orderGetHelper.getOrder(order.getId(), ClientInfo.SYSTEM);

        assertEquals(OrderStatus.CANCELLED, readOrder.getStatus());

        OrderCancelPolicy readOrderCancelPolicy = readOrder.getOrderCancelPolicy();
        assertNotNull(readOrderCancelPolicy);

        assertTrue(readOrderCancelPolicy.getNotAvailable());
    }

    private void baseOrderValidation(Order order) {
        assertEquals(Boolean.TRUE, order.getProperty(OrderPropertyType.UNIQUE_ORDER));

        assertNotNull(order.getOrderCancelPolicy());

        OrderCancelPolicy orderCancelPolicy = order.getOrderCancelPolicy();

        assertEquals("REASON", orderCancelPolicy.getReason());
        assertEquals(3, orderCancelPolicy.getDaysForCancel());
        assertEquals(CancelType.TIME_LIMIT, orderCancelPolicy.getType());
    }

    private void fullOrderValidation(Order order) {
        baseOrderValidation(order);

        assertNotNull(order.getCancelExpiryDate());

        OrderCancelPolicy orderCancelPolicy = order.getOrderCancelPolicy();

        assertFalse(orderCancelPolicy.getNotAvailable());
        assertEquals(LocalDate.now(getClock().getZone()).plusDays(3), orderCancelPolicy.getTimeUntilExpiration());
    }
}
