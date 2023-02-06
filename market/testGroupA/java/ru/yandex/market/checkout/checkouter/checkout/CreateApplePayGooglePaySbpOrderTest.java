package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;
import java.util.Set;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.google.common.collect.Iterables;
import org.json.JSONObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentCollectionFeatureType;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreateApplePayGooglePaySbpOrderTest extends AbstractWebTestBase {

    // 149 - Беларусь
    // 159 - Казахстан
    //
    // 158 - Могилёв, Могилёвская область, Беларусь
    // 163 - Астана, Акмолинская область, Казахстан
    @ParameterizedTest
    @ValueSource(longs = {158, 163})
    public void cisCountriesPayment(Long regionId) throws Exception {
        mockReportCurrencyResponse();
        checkouterFeatureWriter.writeValue(PermanentCollectionFeatureType.CIS_COUNTRIES, Set.of(149L, 159L));
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getDelivery().setRegionId(regionId);
        parameters.getReportParameters().setRegionId(regionId);
        parameters.getBuiltMultiCart().setBuyerRegionId(regionId);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertTrue(Iterables.getOnlyElement(multiCart.getCarts()).getPaymentOptions().stream()
                .allMatch(it -> it.getPaymentType() == PaymentType.PREPAID));

        parameters.setPaymentMethod(PaymentMethod.GOOGLE_PAY);

        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        var orderRequest = OrderRequest.builder(Iterables.getOnlyElement(multiOrder.getOrders()).getId()).build();
        var requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, 0L);
        Order order = client.getOrder(requestClientInfo, orderRequest);
        assertEquals(PaymentMethod.GOOGLE_PAY, order.getPaymentMethod());
    }

    @ParameterizedTest
    @EnumSource(value = PaymentMethod.class, names = {"GOOGLE_PAY", "APPLE_PAY", "SBP"})
    public void shouldReturnGooglePayWhenEnabled(PaymentMethod payType) throws Exception {
        checkouterProperties.setEnableSbpPayment(true);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setShowSbp(true);
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(Iterables.getOnlyElement(multiCart.getCarts()).getPaymentOptions(), hasItem(payType));

        parameters.setPaymentMethod(payType);
        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        var orderRequest = OrderRequest.builder(Iterables.getOnlyElement(multiOrder.getOrders()).getId()).build();
        var requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, 0L);
        Order order = client.getOrder(requestClientInfo, orderRequest);
        assertThat(order.getPaymentMethod(), is(payType));
    }

    @ParameterizedTest
    @EnumSource(value = PaymentMethod.class, names = {"GOOGLE_PAY", "APPLE_PAY", "SBP"})
    public void shouldCreateOrderWithGooglePayWhenEnabled(PaymentMethod payType) {
        checkouterProperties.setEnableSbpPayment(true);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(payType);
        parameters.setShowSbp(true);
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        assertThat(Iterables.getOnlyElement(multiOrder.getCarts()).getPaymentMethod(), is(payType));

        var orderRequest = OrderRequest.builder(Iterables.getOnlyElement(multiOrder.getOrders()).getId()).build();
        var requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, 0L);
        Order order = client.getOrder(requestClientInfo, orderRequest);
        assertThat(order.getPaymentMethod(), is(payType));
    }

    private void mockReportCurrencyResponse() {
        MappingBuilder builderKz = get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo(MarketReportPlace.CURRENCY_CONVERT.getId()))
                .withQueryParam("currency-from", equalTo(Currency.RUR.name()))
                .withQueryParam("currency-to", equalTo(Currency.KZT.name()))
                .withQueryParam("currency-value", equalTo(BigDecimal.ONE.toString()));

        MappingBuilder builderBy = get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo(MarketReportPlace.CURRENCY_CONVERT.getId()))
                .withQueryParam("currency-from", equalTo(Currency.RUR.name()))
                .withQueryParam("currency-to", equalTo(Currency.BYN.name()))
                .withQueryParam("currency-value", equalTo(BigDecimal.ONE.toString()));

        JSONObject result = new JSONObject();
        result.put("currencyFrom", Currency.RUR);
        result.put("currencyTo", Currency.KZT);
        result.put("value", BigDecimal.valueOf(100));
        result.put("convertedValue", BigDecimal.valueOf(100));
        result.put("renderedValue", BigDecimal.valueOf(100));
        result.put("renderedConvertedValue", BigDecimal.valueOf(100));

        reportMockWhite.stubFor(builderKz.willReturn(aResponse().withBody(result.toString())));
        reportMockWhite.stubFor(builderBy.willReturn(aResponse().withBody(result.toString())));
    }
}
