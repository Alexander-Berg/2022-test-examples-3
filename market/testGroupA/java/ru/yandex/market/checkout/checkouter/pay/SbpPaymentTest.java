package ru.yandex.market.checkout.checkouter.pay;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Iterables;
import com.google.gson.JsonObject;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptions;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.order.changerequest.EditPossibilityWrapper;
import ru.yandex.market.checkout.checkouter.order.changerequest.OrderEditPossibility;
import ru.yandex.market.checkout.checkouter.pay.balance.PaymentFormType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.PaymentParameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.UNPAID;
import static ru.yandex.market.checkout.checkouter.pay.balance.PaymentFormType.DESKTOP;
import static ru.yandex.market.checkout.checkouter.pay.balance.PaymentFormType.TOUCH;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.getRequestBodyAsJson;

public class SbpPaymentTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;

    @BeforeEach
    public void prepare() {
        checkouterProperties.setEnableSbpPayment(true);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MOBILE", "DESKTOP", "TOUCH"})
    public void testTrustCalls(String paymentFormType) {
        PaymentFormType formType = PaymentFormType.valueOf(paymentFormType);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.SBP);
        parameters.setShowSbp(true);

        Order createdOrder = orderCreateHelper.createOrder(parameters);
        assertThat(createdOrder.getPaymentMethod(), equalTo(PaymentMethod.SBP));

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();

        PaymentParameters paymentParameters = PaymentParameters.DEFAULT;
        paymentParameters.setPaymentFormType(formType);
        orderPayHelper.pay(createdOrder.getId(), paymentParameters);
        Order order = orderService.getOrder(createdOrder.getId());

        List<ServeEvent> createBasketEvents = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_BASKET_STUB))
                .collect(Collectors.toList());
        assertThat(createBasketEvents, hasSize(1));
        ServeEvent createBasketEvent = Iterables.getOnlyElement(createBasketEvents);
        JsonObject body = getRequestBodyAsJson(createBasketEvent);
        String expectedPaymethodId = (formType == DESKTOP || formType == TOUCH ? "trust_web_page" : "sbp_qr");
        assertThat(body.get("paymethod_id").getAsString(), is(expectedPaymethodId));
    }

    @ParameterizedTest(name = "showSbp = {0}")
    @ValueSource(booleans = {false, true})
    public void changePaymentMethodPossibilityTest(boolean showSbp) {
        Parameters parameters = BlueParametersProvider.prepaidBlueOrderParameters();
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
        OrderEditOptionsRequest request = new OrderEditOptionsRequest();
        request.setChangeRequestTypes(Set.of(ChangeRequestType.PAYMENT_METHOD));
        OrderEditOptions editOptions = client.getOrderEditOptions(order.getId(), ClientRole.USER,
                order.getBuyer().getUid(), singletonList(order.getRgb()), request, false, showSbp);
        assertThat(editOptions.getPaymentOptions().contains(PaymentMethod.SBP), is(showSbp));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MOBILE", "DESKTOP"})
    public void testShowSpbFalse(String paymentFormType) {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.SBP);
        parameters.setShowSbp(false);
        parameters.configuration()
                .checkout()
                .response()
                .setErrorMatcher(jsonPath("$.orderFailures[0].errorDetails")
                        .value("Actualization error: payment options mismatch."));
        parameters.configuration().checkout().response().setUseErrorMatcher(true);
        parameters.configuration().checkout().response().setCheckOrderCreateErrors(false);

        orderCreateHelper.createOrder(parameters);
    }

    @Test
    @DisplayName("Проверяем что при создании СБП платежа из тача в поле devPayload передается блок " +
            "is_sbp_active_touch со значением banks")
    public void createSbpPaymentFromTouch() {
        PaymentFormType formType = PaymentFormType.valueOf("TOUCH");
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.SBP);
        parameters.setShowSbp(true);

        Order createdOrder = orderCreateHelper.createOrder(parameters);
        assertThat(createdOrder.getPaymentMethod(), equalTo(PaymentMethod.SBP));

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();

        PaymentParameters paymentParameters = PaymentParameters.DEFAULT;
        paymentParameters.setPaymentFormType(formType);
        orderPayHelper.pay(createdOrder.getId(), paymentParameters);
        orderService.getOrder(createdOrder.getId());

        List<ServeEvent> createBasketEvents = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_BASKET_STUB))
                .collect(Collectors.toList());
        ServeEvent createBasketEvent = Iterables.getOnlyElement(createBasketEvents);
        JsonObject body = getRequestBodyAsJson(createBasketEvent);
        assertTrue(body.get("developer_payload").getAsString().contains("\"is_sbp_active_touch\":\"banks\"}"));
    }

    @Test
    @DisplayName("Проверяем что при создании НЕ СБП платежа из тача в поле devPayload НЕ передается блок " +
            "is_sbp_active_touch со значением banks")
    public void createNotSbpPaymentFromTouch() {
        PaymentFormType formType = PaymentFormType.valueOf("TOUCH");
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setShowSbp(true);

        Order createdOrder = orderCreateHelper.createOrder(parameters);
        assertThat(createdOrder.getPaymentMethod(), equalTo(PaymentMethod.YANDEX));

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();

        PaymentParameters paymentParameters = PaymentParameters.DEFAULT;
        paymentParameters.setPaymentFormType(formType);
        orderPayHelper.pay(createdOrder.getId(), paymentParameters);
        orderService.getOrder(createdOrder.getId());

        List<ServeEvent> createBasketEvents = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_BASKET_STUB))
                .collect(Collectors.toList());
        ServeEvent createBasketEvent = Iterables.getOnlyElement(createBasketEvents);
        JsonObject body = getRequestBodyAsJson(createBasketEvent);
        assertFalse(body.get("developer_payload").getAsString().contains("\"is_sbp_active_touch\":\"banks\"}"));
    }

    @Test
    @DisplayName("Проверяем что при создании СБП платежа НЕ из тача в поле devPayload НЕ передается блок " +
            "is_sbp_active_touch со значением banks")
    public void createSbpPaymentFromNotTouch() {
        PaymentFormType formType = PaymentFormType.valueOf("DESKTOP");
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.SBP);
        parameters.setShowSbp(true);

        Order createdOrder = orderCreateHelper.createOrder(parameters);
        assertThat(createdOrder.getPaymentMethod(), equalTo(PaymentMethod.SBP));

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();

        PaymentParameters paymentParameters = PaymentParameters.DEFAULT;
        paymentParameters.setPaymentFormType(formType);
        orderPayHelper.pay(createdOrder.getId(), paymentParameters);
        orderService.getOrder(createdOrder.getId());

        List<ServeEvent> createBasketEvents = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_BASKET_STUB))
                .collect(Collectors.toList());
        ServeEvent createBasketEvent = Iterables.getOnlyElement(createBasketEvents);
        JsonObject body = getRequestBodyAsJson(createBasketEvent);
        assertFalse(body.get("developer_payload").getAsString().contains("\"is_sbp_active_touch\":\"banks\"}"));
    }
}
