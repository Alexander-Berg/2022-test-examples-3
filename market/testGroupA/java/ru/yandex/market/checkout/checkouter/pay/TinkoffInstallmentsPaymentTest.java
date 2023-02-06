package ru.yandex.market.checkout.checkouter.pay;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.installments.InstallmentsInfo;
import ru.yandex.market.checkout.checkouter.installments.InstallmentsOption;
import ru.yandex.market.checkout.checkouter.installments.MonthlyPayment;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.PaymentEditRequest;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.balance.checkers.CreateBasketParams;
import ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker;
import ru.yandex.market.checkout.util.balance.checkers.TrustCallsParamsProvider;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.YANDEX;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_CREDIT;

public class TinkoffInstallmentsPaymentTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;

    @BeforeEach
    void init() {
        checkouterProperties.setEnableInstallments(true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void actualizeFlagTest(boolean showInstallments) {
        var parameters = BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(5000));
        parameters.setShowInstallments(showInstallments);
        MultiCart cart = orderCreateHelper.cart(parameters);

        if (showInstallments) {
            assertTrue(cart.getPaymentOptions().contains(PaymentMethod.TINKOFF_INSTALLMENTS));
            validateInstallmentsInfo(cart.getInstallmentsInfo(), cart.getTotals().getBuyerTotal());
        } else {
            assertFalse(cart.getPaymentOptions().contains(PaymentMethod.TINKOFF_INSTALLMENTS));
        }
    }

    @Test
    void shouldPayViaCreditStrategy() {
        var parameters = getInstallmentsParameters();

        var order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);

        order = orderService.getOrder(order.getId());
        var payment = order.getPayment();
        assertEquals(PaymentMethod.TINKOFF_INSTALLMENTS, order.getPaymentMethod());
        assertEquals(PaymentGoal.TINKOFF_CREDIT, payment.getType(), "Оплата рассрочки идет по флоу кредитов");
        assertReportRequestHasInstallmentsParameters();
    }

    @Test
    void checkoutWithInstallmentsAndReportMock() throws Exception {
        var parameters = getInstallmentsParameters();
        var multiCart = orderCreateHelper.cart(parameters);

        assertReportRequestHasInstallmentsParameters();

        validateInstallmentsInfo(multiCart.getInstallmentsInfo(), multiCart.getTotals().getBuyerTotal());

        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        var order = multiOrder.getOrders().get(0);
        orderPayHelper.payForOrder(order);

        order = orderService.getOrder(order.getId());
        var payment = order.getPayment();
        assertEquals(PaymentMethod.TINKOFF_INSTALLMENTS, order.getPaymentMethod());
        assertEquals(PaymentGoal.TINKOFF_CREDIT, payment.getType(), "Оплата рассрочки идет по флоу кредитов");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(value = {"3", "6", "12"})
    void checkoutWithInstallmentsAndReportAndItemCountMock(String term) throws Exception {
        Parameters parameters = getInstallmentsParameters();
        parameters.getOrders()
                .stream()
                .map(Order::getItems)
                .flatMap(Collection::stream)
                .forEach(item -> item.setCount(3));

        var multiCart = orderCreateHelper.cart(parameters);

        assertReportRequestHasInstallmentsParameters();

        validateInstallmentsInfo(multiCart.getInstallmentsInfo(), multiCart.getTotals().getBuyerTotal());

        var installmentInfo = multiCart.getInstallmentsInfo();
        parameters.getBuiltMultiCart().setInstallmentsInfo(
                new InstallmentsInfo(
                        installmentInfo.getOptions(),
                        installmentInfo.getOptions().stream()
                                .filter(option -> term.equals(option.getTerm()))
                                .findFirst()
                                .orElseThrow()
                )
        );
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        var order = multiOrder.getOrders().get(0);
        assertThat(order.getPaymentSubmethod().name()).isEqualTo("TINKOFF_INSTALLMENTS_" + term);
        orderPayHelper.payForOrder(order);

        order = orderService.getOrder(order.getId());
        var payment = order.getPayment();
        assertEquals(PaymentMethod.TINKOFF_INSTALLMENTS, order.getPaymentMethod());
        assertEquals(PaymentGoal.TINKOFF_CREDIT, payment.getType(), "Оплата рассрочки идет по флоу кредитов");
        assertThat(order.getPaymentSubmethod().name()).isEqualTo("TINKOFF_INSTALLMENTS_" + term);
    }

    private Parameters getInstallmentsParameters() {
        var parameters = BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(5000));
        parameters.setShowInstallments(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_INSTALLMENTS);
        return parameters;
    }

    @Test
    void shouldSavePaymentSubtype() {
        var parameters = getInstallmentsParameters();
        Order order = orderCreateHelper.createOrder(parameters);

        order = orderService.getOrder(order.getId());
        assertThat(order.getPaymentMethod()).isEqualTo(PaymentMethod.TINKOFF_INSTALLMENTS);
        assertThat(order.getPaymentSubmethod()).withFailMessage("Should preselect max available term")
                .isEqualTo(PaymentSubmethod.TINKOFF_INSTALLMENTS_12);
    }

    @Test
    void shouldSaveDefaultPaymentSubtype() {
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        order = orderService.getOrder(order.getId());
        assertThat(order.getPaymentSubmethod()).isEqualTo(PaymentSubmethod.DEFAULT);
    }

    @Test
    void totalUpperBoundViolationTest() {
        var parameters = BlueParametersProvider.bluePrepaidWithCustomPrice(BigDecimal.valueOf(200_001));
        parameters.setShowInstallments(true);
        MultiCart cart = orderCreateHelper.cart(parameters);
        assertFalse(cart.getPaymentOptions().contains(PaymentMethod.TINKOFF_INSTALLMENTS));
    }

    @Test
    void promoCodeInTrustPassParams() {
        var parameters = getInstallmentsParameters();
        var order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getPaymentSubmethod()).isEqualTo(PaymentSubmethod.TINKOFF_INSTALLMENTS_12);

        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());

        var createCreditEvent = trustMockConfigurer.servedEvents().stream()
                .filter(event -> CREATE_CREDIT.equals(event.getStubMapping().getName())).findFirst().get();

        CreateBasketParams createBasket = TrustCallsParamsProvider.createBasketFulfilmentParams(order,
                order.getPaymentId());
        createBasket.withUserIp(null);
        createBasket.withPayMethodId("credit");
        createBasket.withReturnPath("http://localhost/!!ORDER_ID!!");
        createBasket.withPassParams(notNullValue(String.class));
        createBasket.withPassParams(containsString("\"credit\":{\"creditPromoCode\":\"default.0,001.fix.12\""));
        createBasket.withPaymentTimeout(Matchers.equalTo("1800"));
        createBasket.withDeveloperPayload("{\"ProcessThroughYt\":1,\"call_preview_payment\":\"card_info\"}");
        TrustCallsChecker.checkCreateCreditCall(createCreditEvent, createBasket);
    }

    @Test
    void shouldChangePaymentSubmethod() {
        var parameters = getInstallmentsParameters();
        var order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getPaymentSubmethod()).isEqualTo(PaymentSubmethod.TINKOFF_INSTALLMENTS_12);

        OrderEditRequest editRequest = new OrderEditRequest();
        var paymentEdit = new PaymentEditRequest();
        paymentEdit.setPaymentMethod(YANDEX);
        editRequest.setPaymentEditRequest(paymentEdit);
        client.editOrder(order.getId(), ClientRole.USER, BuyerProvider.UID, singletonList(BLUE), editRequest);

        order = orderService.getOrder(order.getId());
        assertEquals(order.getPaymentSubmethod(), PaymentSubmethod.DEFAULT);
    }

    private InstallmentsInfo expectedInstallmentInfo(BigDecimal buyerTotal) {
        InstallmentsOption sixMonthsInstallment = new InstallmentsOption(
                "6",
                new MonthlyPayment(Currency.RUR, buyerTotal.divide(BigDecimal.valueOf(6), RoundingMode.UP)
                        .toString())
        );

        InstallmentsOption twelveMonthsInstallment = new InstallmentsOption(
                "12",
                new MonthlyPayment(Currency.RUR, buyerTotal.divide(BigDecimal.valueOf(12), RoundingMode.UP)
                        .toString())
        );

        InstallmentsOption threeMonthsInstallment = new InstallmentsOption(
                "3",
                new MonthlyPayment(Currency.RUR, buyerTotal.divide(BigDecimal.valueOf(3), RoundingMode.UP)
                        .toString())
        );
        return new InstallmentsInfo(
                List.of(threeMonthsInstallment, sixMonthsInstallment, twelveMonthsInstallment),
                twelveMonthsInstallment
        );
    }

    private void validateInstallmentsInfo(InstallmentsInfo installmentsInfo, BigDecimal buyerTotal) {
        InstallmentsInfo expectedInstallmentInfo = expectedInstallmentInfo(buyerTotal);

        assertThat(installmentsInfo).isEqualTo(expectedInstallmentInfo);
    }

    private void assertReportRequestHasInstallmentsParameters() {
        var queryParameters = reportMock.getAllServeEvents()
                .stream()
                .map(ServeEvent::getRequest)
                .filter(request -> request.queryParameter("place").containsValue(MarketReportPlace.OFFER_INFO.getId()))
                .findFirst()
                .map(LoggedRequest::getQueryParams)
                .orElseGet(Collections::emptyMap);

        assertThat(queryParameters.get("show-installments").values()).contains("1");
        assertThat(queryParameters.get("rearr-factors").firstValue()).contains("enable_installments=1");
    }
}
