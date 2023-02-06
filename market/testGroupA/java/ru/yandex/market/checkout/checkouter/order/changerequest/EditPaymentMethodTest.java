package ru.yandex.market.checkout.checkouter.order.changerequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Iterables;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.feature.type.common.IntegerFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.MapFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptions;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.UNPAID;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.CREDIT;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.GOOGLE_PAY;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.SBP;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.TINKOFF_CREDIT;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.YANDEX;
import static ru.yandex.market.checkout.checkouter.shop.MarketplaceFeature.SPASIBO_PAY;

/**
 * @author : poluektov
 * date: 2020-01-31.
 */
public class EditPaymentMethodTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private EventsGetHelper eventsGetHelper;

    @BeforeEach
    public void init() {
        checkouterProperties.setDisableCreditFor3p(true);
    }

    //Изменение пей метода положительный сценарий.
    @Test
    public void changePaymentMethodOptionsTest() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getStatus(), CoreMatchers.equalTo(UNPAID));


        OrderEditOptionsRequest request = new OrderEditOptionsRequest();
        request.setChangeRequestTypes(Set.of(ChangeRequestType.PAYMENT_METHOD));
        OrderEditOptions editOptions = client.getOrderEditOptions(order.getId(), ClientRole.USER,
                BuyerProvider.UID, singletonList(BLUE), request);
        assertThat(editOptions.getPaymentOptions(), containsInAnyOrder(YANDEX));
        assertThat(editOptions.getValidFeatures(), containsInAnyOrder(SPASIBO_PAY));
    }

    //Изменение пей метода положительный сценарий, СБП включен.
    @Test
    public void changePaymentMethodOptionsWithSbpTest() {
        checkouterProperties.setEnableSbpPayment(true);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowSbp(true);
        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getStatus(), CoreMatchers.equalTo(UNPAID));

        OrderEditOptionsRequest request = new OrderEditOptionsRequest();
        request.setChangeRequestTypes(Set.of(ChangeRequestType.PAYMENT_METHOD));
        OrderEditOptions editOptions = client.getOrderEditOptions(order.getId(), ClientRole.USER,
                BuyerProvider.UID, singletonList(BLUE), request, true, true);
        assertThat(editOptions.getPaymentOptions(), containsInAnyOrder(YANDEX, SBP));
        assertThat(editOptions.getValidFeatures(), containsInAnyOrder(SPASIBO_PAY));
    }

    //Любой пей метод который нельзя изменить отдает сам себя.
    @Test
    public void changePaymentMethodOptionsForCreditOrderTest() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(CREDIT);
        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getStatus(), CoreMatchers.equalTo(UNPAID));


        OrderEditOptionsRequest request = new OrderEditOptionsRequest();
        request.setChangeRequestTypes(Set.of(ChangeRequestType.PAYMENT_METHOD));
        OrderEditOptions editOptions = client.getOrderEditOptions(order.getId(), ClientRole.USER,
                BuyerProvider.UID, singletonList(BLUE), request);
        assertThat(editOptions.getPaymentOptions(), containsInAnyOrder(CREDIT));
        assertThat(editOptions.getValidFeatures(), nullValue());
    }

    //Если не указали тип чейндж реквеста, то в выдачу опции оплату не попадают.
    @Test
    public void changePaymentMethodOptionsWithoutType() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(CREDIT);
        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getStatus(), CoreMatchers.equalTo(UNPAID));


        OrderEditOptionsRequest request = new OrderEditOptionsRequest();
        OrderEditOptions editOptions = client.getOrderEditOptions(order.getId(), ClientRole.USER,
                BuyerProvider.UID, singletonList(BLUE), request);
        assertThat(editOptions.getPaymentOptions(), nullValue());
        assertThat(editOptions.getValidFeatures(), nullValue());
    }

    @Test
    public void changePaymentMethod() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getStatus(), CoreMatchers.equalTo(UNPAID));

        OrderEditRequest editRequest = new OrderEditRequest();
        PaymentEditRequest paymentEdit = new PaymentEditRequest();
        paymentEdit.setPaymentMethod(GOOGLE_PAY);
        editRequest.setPaymentEditRequest(paymentEdit);
        List<ChangeRequest> response = client.editOrder(order.getId(), ClientRole.USER, BuyerProvider.UID,
                singletonList(BLUE), editRequest);
        assertThat(response, hasSize(1));
        assertThat(response.get(0).getStatus(), equalTo(ChangeRequestStatus.APPLIED));
        assertThat(response.get(0).getType(), equalTo(ChangeRequestType.PAYMENT_METHOD));
    }

    @Test
    public void changePaymentMethodOrderEvent() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getStatus(), CoreMatchers.equalTo(UNPAID));

        OrderEditRequest editRequest = new OrderEditRequest();
        PaymentEditRequest paymentEdit = new PaymentEditRequest();
        paymentEdit.setPaymentMethod(GOOGLE_PAY);
        editRequest.setPaymentEditRequest(paymentEdit);
        client.editOrder(order.getId(), ClientRole.USER, BuyerProvider.UID, singletonList(BLUE), editRequest);
        PagedEvents orderHistoryEvents = eventsGetHelper.getOrderHistoryEvents(order.getId(), Integer.MAX_VALUE);
        assertTrue(orderHistoryEvents.getItems().stream()
                .anyMatch(e -> HistoryEventType.ORDER_PAYMENT_METHOD_UPDATED == e.getType()));
    }


    @Test
    public void changePaymentMethodNegativeCase() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
            Order order = orderCreateHelper.createOrder(parameters);
            assertThat(order.getStatus(), CoreMatchers.equalTo(UNPAID));

            OrderEditRequest editRequest = new OrderEditRequest();
            PaymentEditRequest paymentEdit = new PaymentEditRequest();
            paymentEdit.setPaymentMethod(CREDIT);
            editRequest.setPaymentEditRequest(paymentEdit);
            List<ChangeRequest> response = client.editOrder(order.getId(), ClientRole.USER, BuyerProvider.UID,
                    singletonList(BLUE), editRequest);
        });
    }

    @Test
    public void changePaymentMethodWithExistingPayment() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getStatus(), CoreMatchers.equalTo(UNPAID));
        orderPayHelper.pay(order.getId());
        order = orderService.getOrder(order.getId());

        OrderEditRequest editRequest = new OrderEditRequest();
        PaymentEditRequest paymentEdit = new PaymentEditRequest();
        paymentEdit.setPaymentMethod(GOOGLE_PAY);
        editRequest.setPaymentEditRequest(paymentEdit);
        List<ChangeRequest> response = client.editOrder(order.getId(), ClientRole.USER, BuyerProvider.UID,
                singletonList(BLUE), editRequest);
        assertThat(response, hasSize(1));
        assertThat(response.get(0).getStatus(), equalTo(ChangeRequestStatus.APPLIED));
    }

    @Test
    public void shouldAllowCreditPayment() {
        checkouterFeatureWriter.writeValue(MapFeatureType.CHANGEABLE_PAYMENT_METHOD, Map.of(
                YANDEX, Set.of(YANDEX, TINKOFF_CREDIT)));
        Parameters parameters = BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(3500));
        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getStatus(), CoreMatchers.equalTo(UNPAID));

        OrderEditOptionsRequest request = new OrderEditOptionsRequest();
        request.setChangeRequestTypes(Set.of(ChangeRequestType.PAYMENT_METHOD));
        OrderEditOptions editOptions = client.getOrderEditOptions(order.getId(), ClientRole.USER,
                BuyerProvider.UID, singletonList(BLUE), request, true);
        assertThat(editOptions.getPaymentOptions(), containsInAnyOrder(YANDEX, TINKOFF_CREDIT));
        assertThat(editOptions.getValidFeatures(), containsInAnyOrder(SPASIBO_PAY));
    }

    @Test
    public void shouldAllowCreditPaymentForBoundaryOrderPrices() {
        checkouterFeatureWriter.writeValue(MapFeatureType.CHANGEABLE_PAYMENT_METHOD, Map.of(
                YANDEX, Set.of(YANDEX, TINKOFF_CREDIT)));
        checkouterFeatureWriter.writeValue(IntegerFeatureType.TINKOFF_CREDIT_LIMIT_MIN, 3_000);
        checkouterFeatureWriter.writeValue(IntegerFeatureType.TINKOFF_CREDIT_LIMIT_MAX, 120_000);
        Order lowBoundOrder = orderCreateHelper.createOrder(BlueParametersProvider
                .bluePrepaidWithCustomPrice(BigDecimal.valueOf(3_000 - 100))); // subtract 100 for delivery
        Order highBoundOrder = orderCreateHelper.createOrder(BlueParametersProvider
                .bluePrepaidWithCustomPrice(BigDecimal.valueOf(120_000 - 100)));

        OrderEditOptionsRequest request = new OrderEditOptionsRequest();
        request.setChangeRequestTypes(Set.of(ChangeRequestType.PAYMENT_METHOD));
        OrderEditOptions lowBoundOrderEditOptions = client.getOrderEditOptions(lowBoundOrder.getId(), ClientRole.USER,
                BuyerProvider.UID, singletonList(BLUE), request, true);
        OrderEditOptions highOrderEditOptions = client.getOrderEditOptions(highBoundOrder.getId(), ClientRole.USER,
                BuyerProvider.UID, singletonList(BLUE), request, true);
        assertThat(lowBoundOrderEditOptions.getPaymentOptions(), hasItem(TINKOFF_CREDIT));
        assertThat(highOrderEditOptions.getPaymentOptions(), hasItem(TINKOFF_CREDIT));
    }

    @Test
    public void shouldNotAllowCreditPaymentForInappropriateOrderPrice() {
        checkouterFeatureWriter.writeValue(MapFeatureType.CHANGEABLE_PAYMENT_METHOD, Map.of(
                YANDEX, Set.of(YANDEX, TINKOFF_CREDIT)));
        checkouterFeatureWriter.writeValue(IntegerFeatureType.TINKOFF_CREDIT_LIMIT_MIN, 3_000);
        checkouterFeatureWriter.writeValue(IntegerFeatureType.TINKOFF_CREDIT_LIMIT_MAX, 120_000);
        Order cheapOrder = orderCreateHelper.createOrder(BlueParametersProvider
                .bluePrepaidWithCustomPrice(BigDecimal.valueOf(2_999 - 100))); // subtract 100 for delivery
        Order expensiveOrder = orderCreateHelper.createOrder(BlueParametersProvider
                .bluePrepaidWithCustomPrice(BigDecimal.valueOf(120_001 - 100)));

        OrderEditOptionsRequest request = new OrderEditOptionsRequest();
        request.setChangeRequestTypes(Set.of(ChangeRequestType.PAYMENT_METHOD));
        OrderEditOptions cheapOrderEditOptions = client.getOrderEditOptions(cheapOrder.getId(), ClientRole.USER,
                BuyerProvider.UID, singletonList(BLUE), request, true);
        OrderEditOptions expensiveOrderEditOptions = client.getOrderEditOptions(expensiveOrder.getId(), ClientRole.USER,
                BuyerProvider.UID, singletonList(BLUE), request, true);
        assertThat(cheapOrderEditOptions.getPaymentOptions(), not(hasItem(TINKOFF_CREDIT)));
        assertThat(expensiveOrderEditOptions.getPaymentOptions(), not(hasItem(TINKOFF_CREDIT)));
    }

    @Test
    public void creditNotAvailableForAll3p() {
        checkouterFeatureWriter.writeValue(MapFeatureType.CHANGEABLE_PAYMENT_METHOD, Map.of(
                YANDEX, Set.of(YANDEX, TINKOFF_CREDIT)));
        var parameters = BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(3500));
        parameters.setShowCredits(true);
        parameters.setSupplierTypeForAllItems(SupplierType.THIRD_PARTY);
        var order = orderCreateHelper.createOrder(parameters);

        var request = new OrderEditOptionsRequest();
        request.setChangeRequestTypes(Set.of(ChangeRequestType.PAYMENT_METHOD));
        var orderEditOptions = client.getOrderEditOptions(
                order.getId(),
                ClientRole.USER,
                BuyerProvider.UID,
                singletonList(BLUE),
                request,
                true
        );

        assertThat(orderEditOptions.getPaymentOptions(), not(hasItem(TINKOFF_CREDIT)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void creditAvailableFor3pButNotForDropshipOrCrossdock(boolean disableCreditForDropshipAndCrossdock) {
        checkouterFeatureWriter.writeValue(MapFeatureType.CHANGEABLE_PAYMENT_METHOD, Map.of(
                YANDEX, Set.of(YANDEX, TINKOFF_CREDIT)));
        checkouterProperties.setDisableCreditFor3p(false);
        checkouterProperties.setDisableCreditForDropshipAndCrossdock(disableCreditForDropshipAndCrossdock);
        var parameters = BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(3500));
        parameters.setShowCredits(true);
        parameters.setSupplierTypeForAllItems(SupplierType.THIRD_PARTY);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        var orderItem = Iterables.getOnlyElement(parameters.getOrder().getItems());
        parameters.getReportParameters().overrideItemInfo(orderItem.getFeedOfferId()).setAtSupplierWarehouse(true);
        var order = orderCreateHelper.createOrder(parameters);

        var request = new OrderEditOptionsRequest();
        request.setChangeRequestTypes(Set.of(ChangeRequestType.PAYMENT_METHOD));
        var orderEditOptions = client.getOrderEditOptions(
                order.getId(),
                ClientRole.USER,
                BuyerProvider.UID,
                singletonList(BLUE),
                request,
                true
        );
        assertThat(orderEditOptions.getPaymentOptions(),
                disableCreditForDropshipAndCrossdock ? not(hasItem(PaymentMethod.TINKOFF_CREDIT)) :
                        hasItem(PaymentMethod.TINKOFF_CREDIT));
    }

    @Test
    public void shouldNotAllowSpasiboDisabledViaToggle() {
        checkouterProperties.setDisableSpasibo(true);
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        assertThat(order.getStatus(), CoreMatchers.equalTo(UNPAID));

        OrderEditOptionsRequest request = new OrderEditOptionsRequest();
        request.setChangeRequestTypes(Set.of(ChangeRequestType.PAYMENT_METHOD));
        OrderEditOptions editOptions = client.getOrderEditOptions(order.getId(), ClientRole.USER,
                BuyerProvider.UID, singletonList(BLUE), request, true);
        assertThat(editOptions.getValidFeatures(), not(hasItem(SPASIBO_PAY)));
    }
}
