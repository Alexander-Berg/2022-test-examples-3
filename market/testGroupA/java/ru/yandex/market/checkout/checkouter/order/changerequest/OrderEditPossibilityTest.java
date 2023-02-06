package ru.yandex.market.checkout.checkouter.order.changerequest;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.Recipient;
import ru.yandex.market.checkout.checkouter.delivery.RecipientPerson;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.StatusAndSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.recipient.RecipientEditRequest;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.CancellationRequestHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.test.providers.RecipientProvider;
import ru.yandex.market.common.report.model.ActualDelivery;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.UNPAID;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.ANOTHER_MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_INTAKE_DELIVERY_SERVICE_ID;

/**
 * @author mmetlov
 */
public class OrderEditPossibilityTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private CancellationRequestHelper cancellationRequestHelper;
    @Autowired
    private CheckouterFeatureWriter checkouterFeatureWriter;
    @Autowired
    private OrderPayHelper orderPayHelper;

    private YandexMarketDeliveryHelper.MarDoOrderBuilder orderBuilder;

    @BeforeEach
    public void init() {
        orderBuilder = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID);
    }

    @Test
    public void shouldNotReturnPossibilitiesWhenOrderNotFound() {
        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Collections.singleton(Long.MAX_VALUE), ClientRole.SYSTEM, 0L, Collections.singletonList(Color.BLUE));
        assertTrue(orderEditPossibilityList.isEmpty());
    }

    @Test
    public void shouldReturnPossibleForAllChangesForUnpaidDeliveryOrder() {
        Order order = orderBuilder
                .withDeliveryType(DeliveryType.DELIVERY)
                .withDeliveryServiceId(ANOTHER_MOCK_DELIVERY_SERVICE_ID)
                .build();

        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Collections.singleton(order.getId()), ClientRole.CALL_CENTER_OPERATOR, 123L,
                Collections.singletonList(Color.BLUE));

        assertThat(orderEditPossibilityList, Matchers.hasSize(1));
        OrderEditPossibility orderEditPossibility = orderEditPossibilityList.get(0);
        assertThat(orderEditPossibility, hasProperty("orderId", is(order.getId())));
        EditPossibilityWrapper editPossibilityWrapper =
                EditPossibilityWrapper.build(orderEditPossibility.getEditPossibilities());
        assertTrue(editPossibilityWrapper.isPossible(ChangeRequestType.RECIPIENT));
        assertTrue(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_DATES));
        assertTrue(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_OPTION));
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_ADDRESS));
    }

    @Test
    public void shouldReturnPossibleForAllChangesForUnpaidPickupOrder() {
        Order order = orderBuilder
                .withDeliveryType(DeliveryType.PICKUP)
                .build();

        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Collections.singleton(order.getId()), ClientRole.CALL_CENTER_OPERATOR, 123L,
                Collections.singletonList(Color.BLUE));

        assertThat(orderEditPossibilityList, Matchers.hasSize(1));
        OrderEditPossibility orderEditPossibility = orderEditPossibilityList.get(0);
        assertThat(orderEditPossibility, hasProperty("orderId", is(order.getId())));
        EditPossibilityWrapper editPossibilityWrapper =
                EditPossibilityWrapper.build(orderEditPossibility.getEditPossibilities());
        assertTrue(editPossibilityWrapper.isPossible(ChangeRequestType.RECIPIENT));
        assertTrue(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_DATES));
        assertTrue(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_OPTION));
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_ADDRESS));
    }

    @Test
    public void shouldReturnImpossibleForCancelledOrder() {
        Order order = orderBuilder.build();
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);

        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Collections.singleton(order.getId()), ClientRole.SYSTEM, 0L, Collections.singletonList(Color.BLUE));

        assertThat(orderEditPossibilityList, Matchers.hasSize(1));
        OrderEditPossibility orderEditPossibility = orderEditPossibilityList.get(0);
        assertThat(orderEditPossibility, hasProperty("orderId", is(order.getId())));
        EditPossibilityWrapper editPossibilityWrapper =
                EditPossibilityWrapper.build(orderEditPossibility.getEditPossibilities());
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.RECIPIENT));
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_DATES));
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_ADDRESS));

        assertFalse(editPossibilityWrapper.isPossible(MethodOfChange.PARTNER_PHONE, ChangeRequestType.DELIVERY_DATES));
        assertFalse(editPossibilityWrapper.isPossible(MethodOfChange.PARTNER_SITE, ChangeRequestType.DELIVERY_DATES));
        assertFalse(editPossibilityWrapper.isPossible(MethodOfChange.PARTNER_PHONE, ChangeRequestType.RECIPIENT));
        assertFalse(editPossibilityWrapper.isPossible(MethodOfChange.PARTNER_SITE, ChangeRequestType.RECIPIENT));
    }

    @Test
    public void shouldReturnImpossibleForOrderWithCancellationRequest() throws Exception {
        Order order = orderBuilder.build();
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        CancellationRequest cancellationRequest = new CancellationRequest(USER_CHANGED_MIND, "");
        cancellationRequestHelper.createCancellationRequest(
                order.getId(), cancellationRequest, new ClientInfo(ClientRole.USER, BuyerProvider.UID));

        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Collections.singleton(order.getId()), ClientRole.SYSTEM, 0L, Collections.singletonList(Color.BLUE));

        assertThat(orderEditPossibilityList, Matchers.hasSize(1));
        OrderEditPossibility orderEditPossibility = orderEditPossibilityList.get(0);
        assertThat(orderEditPossibility, hasProperty("orderId", is(order.getId())));
        EditPossibilityWrapper editPossibilityWrapper =
                EditPossibilityWrapper.build(orderEditPossibility.getEditPossibilities());
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.RECIPIENT));
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_DATES));
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_ADDRESS));
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_OPTION));

        assertFalse(editPossibilityWrapper.isPossible(MethodOfChange.PARTNER_PHONE, ChangeRequestType.DELIVERY_DATES));
        assertFalse(editPossibilityWrapper.isPossible(MethodOfChange.PARTNER_SITE, ChangeRequestType.DELIVERY_DATES));
        assertFalse(editPossibilityWrapper.isPossible(MethodOfChange.PARTNER_PHONE, ChangeRequestType.RECIPIENT));
        assertFalse(editPossibilityWrapper.isPossible(MethodOfChange.PARTNER_SITE, ChangeRequestType.RECIPIENT));
    }

    @Test
    public void shouldReturnImpossibleForUnsupportedDeliveryService() {
        Order order = orderBuilder
                .withDeliveryServiceId(MOCK_INTAKE_DELIVERY_SERVICE_ID)
                .build();

        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Collections.singleton(order.getId()), ClientRole.USER, BuyerProvider.UID,
                Collections.singletonList(Color.BLUE));

        assertThat(orderEditPossibilityList, Matchers.hasSize(1));
        OrderEditPossibility orderEditPossibility = orderEditPossibilityList.get(0);
        assertThat(orderEditPossibility, hasProperty("orderId", is(order.getId())));
        EditPossibilityWrapper editPossibilityWrapper =
                EditPossibilityWrapper.build(orderEditPossibility.getEditPossibilities());
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.RECIPIENT));
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_DATES));
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_ADDRESS));
    }

    @Test
    public void shouldCannotCreateCRIfImpossible() {
        Order order = orderBuilder.build();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        RecipientEditRequest recipientEditRequest = new RecipientEditRequest();
        recipientEditRequest.setPerson(new RecipientPerson("Ivan", null, "Ivanov"));
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setRecipientEditRequest(recipientEditRequest);

        // создать заявку на изменение данных
        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), ClientRole.USER, BuyerProvider.UID,
                singletonList(BLUE), orderEditRequest);
        // проверим, что запрос создался
        assertThat(changeRequests, hasSize(1));

        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Collections.singleton(order.getId()), ClientRole.USER, BuyerProvider.UID,
                Collections.singletonList(Color.BLUE));

        assertThat(orderEditPossibilityList, Matchers.hasSize(1));
        OrderEditPossibility orderEditPossibility = orderEditPossibilityList.get(0);
        assertThat(orderEditPossibility, hasProperty("orderId", is(order.getId())));
        EditPossibilityWrapper editPossibilityWrapper =
                EditPossibilityWrapper.build(orderEditPossibility.getEditPossibilities());
        // изменение получателя должно быть недоступно
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.RECIPIENT));

        // все равно вызываем edit
        try {
            client.editOrder(order.getId(), ClientRole.CALL_CENTER_OPERATOR, 1121L, Collections.singletonList(BLUE),
                    orderEditRequest);
            Assertions.fail("Exception should be thrown");
        } catch (ErrorCodeException ignored) {
        }
    }

    @Test
    public void cannotCreateDuplicatedCRForRoleSystem() {
        Order order = orderBuilder.build();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        RecipientEditRequest recipientEditRequest = new RecipientEditRequest();
        recipientEditRequest.setPerson(RecipientProvider.getDefaultRecipient().getPerson());
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setRecipientEditRequest(recipientEditRequest);

        client.editOrder(order.getId(), ClientRole.USER, BuyerProvider.UID, singletonList(BLUE), orderEditRequest);

        // вызываем edit еще раз под ролью SYSTEM
        try {
            client.editOrder(order.getId(), ClientRole.SYSTEM, null, Collections.singletonList(BLUE), orderEditRequest);
            Assertions.fail("Exception should be thrown");
        } catch (ErrorCodeException ignored) {
        }
    }

    @Test
    public void shouldReturnImpossibleForUnsupportedDeliveryTypeForChangeDeliveryDates() {
        Order order = orderBuilder
                .withDeliveryType(DeliveryType.PICKUP)
                .build();

        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Collections.singleton(order.getId()), ClientRole.USER, BuyerProvider.UID,
                Collections.singletonList(Color.BLUE));

        assertThat(orderEditPossibilityList, Matchers.hasSize(1));
        OrderEditPossibility orderEditPossibility = orderEditPossibilityList.get(0);
        EditPossibilityWrapper editPossibilityWrapper =
                EditPossibilityWrapper.build(orderEditPossibility.getEditPossibilities());
        assertThat(orderEditPossibility, hasProperty("orderId", is(order.getId())));
        assertTrue(editPossibilityWrapper.isPossible(ChangeRequestType.RECIPIENT));
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_DATES));
    }

    @Test
    public void shouldReturnPossibleForUnsupportedDeliveryTypeForChangeDeliveryDatesForSystem() {
        Order order = orderBuilder
                .withDeliveryType(DeliveryType.PICKUP)
                .build();

        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Collections.singleton(order.getId()), ClientRole.SYSTEM, 0L,
                Collections.singletonList(Color.BLUE));

        assertThat(orderEditPossibilityList, Matchers.hasSize(1));
        OrderEditPossibility orderEditPossibility = orderEditPossibilityList.get(0);
        assertThat(orderEditPossibility, hasProperty("orderId", is(order.getId())));
        EditPossibilityWrapper editPossibilityWrapper =
                EditPossibilityWrapper.build(orderEditPossibility.getEditPossibilities());
        assertTrue(editPossibilityWrapper.isPossible(ChangeRequestType.RECIPIENT));
        assertTrue(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_DATES));
    }

    @Test
    public void shouldReturnImpossibleDeliveryDateForPreorderBeforeProcessing() {
        OrderItem item = OrderItemProvider.getOrderItem();
        item.setPreorder(true);
        item.setMsku(124L);

        Parameters parameters = new Parameters(OrderProvider.getBlueOrder(o -> {
            o.setItems(Collections.singletonList(item));
        }));
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        Order order = orderCreateHelper.createOrder(parameters);
        assertEquals(order.getStatus(), OrderStatus.UNPAID);
        assertEquals(order.getSubstatus(), OrderSubstatus.WAITING_USER_INPUT);
        assertTrue(order.isPreorder());
        assertTrue(canEditDeliveryDates(order, ClientRole.SYSTEM));
        assertTrue(canEditDeliveryDates(order, ClientRole.CALL_CENTER_OPERATOR));
        assertFalse(canEditDeliveryDates(order, ClientRole.USER));
        assertFalse(canEditDeliveryDates(order, ClientRole.SHOP));

        orderPayHelper.payForOrder(order);
        assertTrue(canEditDeliveryDates(order, ClientRole.SYSTEM));
        assertTrue(canEditDeliveryDates(order, ClientRole.CALL_CENTER_OPERATOR));
        assertFalse(canEditDeliveryDates(order, ClientRole.USER));
        assertFalse(canEditDeliveryDates(order, ClientRole.SHOP));

        order = orderService.getOrder(order.getId());
        assertEquals(order.getStatus(), OrderStatus.PENDING);
        assertEquals(order.getSubstatus(), OrderSubstatus.PREORDER);
        assertTrue(canEditDeliveryDates(order, ClientRole.SYSTEM));
        assertTrue(canEditDeliveryDates(order, ClientRole.CALL_CENTER_OPERATOR));
        assertFalse(canEditDeliveryDates(order, ClientRole.USER));
        assertFalse(canEditDeliveryDates(order, ClientRole.SHOP));

        orderUpdateService.updateOrderStatus(order.getId(), StatusAndSubstatus.of(OrderStatus.PROCESSING),
                ClientInfo.SYSTEM);
        order = orderService.getOrder(order.getId());
        assertEquals(order.getStatus(), OrderStatus.PROCESSING);
        assertTrue(canEditDeliveryDates(order, ClientRole.SYSTEM));
        assertFalse(canEditDeliveryDates(order, ClientRole.CALL_CENTER_OPERATOR));
        assertFalse(canEditDeliveryDates(order, ClientRole.USER));
        assertFalse(canEditDeliveryDates(order, ClientRole.SHOP));
    }

    private boolean canEditDeliveryDates(Order order, ClientRole clientRole) {
        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Collections.singleton(order.getId()), clientRole, 0L,
                Collections.singletonList(Color.BLUE));
        EditPossibilityWrapper editPossibilityWrapper =
                EditPossibilityWrapper.build(orderEditPossibilityList.get(0).getEditPossibilities());
        return editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_DATES);
    }

    @Test
    public void cannotCreateChangeRequestForUnsupportedDeliveryService() {
        Order order = orderBuilder
                .withDeliveryServiceId(ANOTHER_MOCK_DELIVERY_SERVICE_ID)
                .build();
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        Set<Long> orderIds = new HashSet<>(Collections.singleton(order.getId()));

        List<OrderEditPossibility> orderEditPossibilities = client.getOrderEditPossibilities(orderIds,
                ClientRole.USER, BuyerProvider.UID, singletonList(BLUE));

        assertThat(orderEditPossibilities, Matchers.hasSize(1));
        OrderEditPossibility orderEditPossibility = orderEditPossibilities.get(0);
        assertThat(orderEditPossibility, hasProperty("orderId", is(order.getId())));
        EditPossibilityWrapper editPossibilityWrapper =
                EditPossibilityWrapper.build(orderEditPossibility.getEditPossibilities());
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.RECIPIENT));
        assertTrue(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_DATES));

        // создать заявку на изменение данных
        Recipient defaultRecipient = RecipientProvider.getDefaultRecipient();

        RecipientEditRequest recipientEditRequest = new RecipientEditRequest();
        recipientEditRequest.setPerson(defaultRecipient.getPerson());
        recipientEditRequest.setPhone(defaultRecipient.getPhone());

        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setRecipientEditRequest(recipientEditRequest);

        try {
            client.editOrder(order.getId(), ClientRole.USER, BuyerProvider.UID, singletonList(BLUE), orderEditRequest);
            Assertions.fail("Exception should be thrown");
        } catch (ErrorCodeException ignored) {
        }
    }

    @Test
    public void changePaymentMethodPossibilityTest() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getStatus(), equalTo(UNPAID));

        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Collections.singleton(order.getId()), ClientRole.USER, BuyerProvider.UID,
                Collections.singletonList(Color.BLUE));
        assertThat(orderEditPossibilityList, Matchers.hasSize(1));
        OrderEditPossibility orderEditPossibility = orderEditPossibilityList.get(0);
        EditPossibilityWrapper editPossibilityWrapper =
                EditPossibilityWrapper.build(orderEditPossibility.getEditPossibilities());
        assertTrue(editPossibilityWrapper.isPossible(ChangeRequestType.PAYMENT_METHOD));
    }

    @ParameterizedTest
    @EnumSource(value = DeliveryType.class, names = {"DELIVERY", "PICKUP"})
    public void forDeliveryAndCourier_shouldReturnDeliveryLastMilePossibility(DeliveryType deliveryType) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_CHANGE_LAST_MILE, true);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_DELIVERY_LAST_MILE_POSSIBILITY_INVERSE, false);
        var exp = Experiments.of(Experiments.CHANGE_LAST_MILE_FROM_PICKUP,
                Experiments.CHANGE_LAST_MILE_FROM_PICKUP_VALUE);
        Order order = orderBuilder
                .withDeliveryType(deliveryType)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .build();

        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Collections.singleton(order.getId()), ClientRole.USER, BuyerProvider.UID,
                Collections.singletonList(Color.BLUE), exp.toExperimentString());

        assertThat(orderEditPossibilityList, Matchers.hasSize(1));
        OrderEditPossibility orderEditPossibility = orderEditPossibilityList.get(0);
        assertThat(orderEditPossibility, hasProperty("orderId", is(order.getId())));
        EditPossibilityWrapper editPossibilityWrapper =
                EditPossibilityWrapper.build(orderEditPossibility.getEditPossibilities());
        assertTrue(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_LAST_MILE));
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_LAST_MILE_COURIER));
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_LAST_MILE_PICKUP));
    }

    @ParameterizedTest
    @EnumSource(value = DeliveryType.class, names = {"DELIVERY", "PICKUP"})
    public void forDeliveryAndCourier_shouldReturnDeliveryLastMilePossibilities(DeliveryType deliveryType) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_CHANGE_LAST_MILE, true);
        var exp = Experiments.of(Experiments.CHANGE_LAST_MILE_FROM_PICKUP,
                Experiments.CHANGE_LAST_MILE_FROM_PICKUP_VALUE);
        Order order = orderBuilder
                .withDeliveryType(deliveryType)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .build();

        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Collections.singleton(order.getId()), ClientRole.USER, BuyerProvider.UID,
                Collections.singletonList(Color.BLUE), exp.toExperimentString());

        assertThat(orderEditPossibilityList, Matchers.hasSize(1));
        OrderEditPossibility orderEditPossibility = orderEditPossibilityList.get(0);
        assertThat(orderEditPossibility, hasProperty("orderId", is(order.getId())));
        EditPossibilityWrapper editPossibilityWrapper =
                EditPossibilityWrapper.build(orderEditPossibility.getEditPossibilities());
        assertTrue(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_LAST_MILE));
        assertTrue(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_LAST_MILE_COURIER));
        assertTrue(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_LAST_MILE_PICKUP));
    }

    @ParameterizedTest
    @MethodSource("falseConditions")
    public void beforeProcessing_shouldNotReturnDeliveryLastMilePossibility(
            boolean toggleEnabled,
            boolean reachedProcessing) {
        if (toggleEnabled) {
            checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_CHANGE_LAST_MILE, true);
        }
        Order order = orderBuilder
                .withDeliveryType(DeliveryType.DELIVERY)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .build();

        if (reachedProcessing) {
            order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        }

        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Collections.singleton(order.getId()), ClientRole.USER, BuyerProvider.UID,
                Collections.singletonList(Color.BLUE), null);

        assertThat(orderEditPossibilityList, Matchers.hasSize(1));
        OrderEditPossibility orderEditPossibility = orderEditPossibilityList.get(0);
        assertThat(orderEditPossibility, hasProperty("orderId", is(order.getId())));
        EditPossibilityWrapper editPossibilityWrapper =
                EditPossibilityWrapper.build(orderEditPossibility.getEditPossibilities());
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_LAST_MILE));
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_LAST_MILE_COURIER));
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_LAST_MILE_PICKUP));
    }

    @Test
    public void pickupOrderWithoutExp_shouldNotReturnDeliveryLastMilePossibility() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_CHANGE_LAST_MILE, true);
        Order order = orderBuilder
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .build();

        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Collections.singleton(order.getId()), ClientRole.USER, BuyerProvider.UID,
                Collections.singletonList(Color.BLUE), null);

        assertThat(orderEditPossibilityList, Matchers.hasSize(1));
        OrderEditPossibility orderEditPossibility = orderEditPossibilityList.get(0);
        assertThat(orderEditPossibility, hasProperty("orderId", is(order.getId())));
        EditPossibilityWrapper editPossibilityWrapper =
                EditPossibilityWrapper.build(orderEditPossibility.getEditPossibilities());
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_LAST_MILE));
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_LAST_MILE_COURIER));
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_LAST_MILE_PICKUP));
    }

    @Test
    public void estimatedDeliveryNotAllowdTest() {
        ActualDelivery actualDelivery = ActualDeliveryProvider.builder()
                .addDelivery(ANOTHER_MOCK_DELIVERY_SERVICE_ID, 1)
                .build();

        actualDelivery.getResults().get(0)
                .getDelivery()
                .forEach(p -> p.setEstimated(true));

        Order order = orderBuilder
                .withDeliveryType(DeliveryType.DELIVERY)
                .withDeliveryServiceId(ANOTHER_MOCK_DELIVERY_SERVICE_ID)
                .withActualDelivery(actualDelivery)
                .build();

        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Collections.singleton(order.getId()), ClientRole.USER, 123L,
                Collections.singletonList(Color.BLUE));

        assertThat(orderEditPossibilityList, Matchers.hasSize(1));
        OrderEditPossibility orderEditPossibility = orderEditPossibilityList.get(0);
        assertThat(orderEditPossibility, hasProperty("orderId", is(order.getId())));
        EditPossibilityWrapper editPossibilityWrapper =
                EditPossibilityWrapper.build(orderEditPossibility.getEditPossibilities());
        assertTrue(editPossibilityWrapper.isPossible(ChangeRequestType.RECIPIENT));
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_DATES));
        assertTrue(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_OPTION));
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_ADDRESS));
    }

    private static Stream<Arguments> falseConditions() {
        return Stream.of(
                new Object[]{true, false},
                new Object[]{false, true},
                new Object[]{false, false}
        ).map(Arguments::of);
    }
}
