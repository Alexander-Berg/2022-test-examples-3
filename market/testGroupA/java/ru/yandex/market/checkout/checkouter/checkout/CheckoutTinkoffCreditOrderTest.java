package ru.yandex.market.checkout.checkouter.checkout;

import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.checkout.checkouter.order.MarketReportSearchService.REPORT_EXPERIMENTS_PARAM;

/**
 * @author : poluektov
 * date: 2020-11-05.
 */
public class CheckoutTinkoffCreditOrderTest extends AbstractWebTestBase {

    @Test
    public void testCheckoutCreditOrder() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);

        orderCreateHelper.createOrder(parameters);
        assertReportRequestHasShowCredit();
    }

    @Test
    public void testCreditOrderCreation() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);

        Order createdOrder = orderCreateHelper.createOrder(parameters);
        assertThat(createdOrder.getPaymentMethod(), equalTo(PaymentMethod.TINKOFF_CREDIT));
    }

    @Test
    public void failCheckoutForClickAndCollectCreditOrders() {
        Parameters parameters = BlueParametersProvider.clickAndCollectOrderParameters();
        parameters.setShowCredits(true);
        parameters.setPushApiDeliveryResponse(DeliveryProvider.shopSelfPickupDeliveryByOutletCode()
                .paymentOption(PaymentMethod.TINKOFF_CREDIT)
                .buildResponse(DeliveryResponse::new));
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDelivery());
        parameters.setPaymentMethod(PaymentMethod.TINKOFF_CREDIT);
        parameters.setErrorMatcher(jsonPath("$.checkedOut").value(false));
    }

    @Test
    public void creditNotAvailableForAll3p() {
        checkouterProperties.setDisableCreditFor3p(true);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setSupplierTypeForAllItems(SupplierType.THIRD_PARTY);
        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(cart.getPaymentOptions(), not(hasItem(PaymentMethod.TINKOFF_CREDIT)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void creditAvailableFor3pButNotForDropshipOrCrossdock(boolean disableCreditForDropshipAndCrossdock) {
        checkouterProperties.setDisableCreditFor3p(false);
        checkouterProperties.setDisableCreditForDropshipAndCrossdock(disableCreditForDropshipAndCrossdock);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.setSupplierTypeForAllItems(SupplierType.THIRD_PARTY);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        OrderItem orderItem = Iterables.getOnlyElement(parameters.getOrder().getItems());
        parameters.getReportParameters().overrideItemInfo(orderItem.getFeedOfferId()).setAtSupplierWarehouse(true);
        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(cart.getPaymentOptions(),
                disableCreditForDropshipAndCrossdock ? not(hasItem(PaymentMethod.TINKOFF_CREDIT)) :
                        hasItem(PaymentMethod.TINKOFF_CREDIT));
    }

    @Test
    public void testCheckoutCreditOrderWithMarketForceWhiteOnExperimentTest() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowCredits(true);
        parameters.addExperiment("market_force_white_on", "13,14,15,16,17,19,23,25,26");


        orderCreateHelper.createOrder(parameters);
        assertReportRequestHasNotContainsMarketForceWhiteOnExperiment();
    }

    private void assertReportRequestHasShowCredit() {
        assertThat(
                reportMock.getServeEvents().getServeEvents()
                        .stream()
                        .filter(se -> se.getRequest().queryParameter("place")
                                .containsValue(MarketReportPlace.CREDIT_INFO.getId()))
                        .filter(se -> se.getRequest().queryParameter("show-credits")
                                .containsValue("1"))
                        .collect(Collectors.toList()),
                hasSize(greaterThanOrEqualTo(1))
        );
    }

    private void assertReportRequestHasNotContainsMarketForceWhiteOnExperiment() {
        var containsMarketForceWhiteOnExperiment = reportMock.getAllServeEvents()
                .stream()
                .map(ServeEvent::getRequest)
                .filter(request -> request.queryParameter("place")
                        .containsValue(MarketReportPlace.CREDIT_INFO.getId()))
                .map(request -> request.queryParameter(REPORT_EXPERIMENTS_PARAM))
                .filter(QueryParameter::isPresent)
                .anyMatch(queryParameter -> queryParameter.values().stream()
                        .anyMatch(value -> value.contains("market_force_white_on")));

        assertFalse(containsMarketForceWhiteOnExperiment,
                "market_force_white_on experiment must be excluded from report request");
    }
}
