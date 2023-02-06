package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.Constants;
import ru.yandex.market.checkout.util.loyalty.LoyaltyConfigurer;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.checkout.util.loyalty.LoyaltyParameters;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.ActualDeliveryResult;
import ru.yandex.market.common.report.model.ExtraCharge;
import ru.yandex.market.common.report.model.ExtraChargeParameters;
import ru.yandex.market.loyalty.api.model.bundle.OrderExtraChargeDeliveryParams;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryRequest;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;

import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.NEW_LARGE_SIZE_CALCULATION;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

public class CartDeliveryExtraChargeTest extends AbstractWebTestBase {

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("dd-MM-yyyy HH:mm:ss")
            .setPrettyPrinting().create();

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @BeforeEach
    public void setUp() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_EXTRA_CHARGE, true);
    }

    @Test
    public void shouldReturnExtraChargeFromReport() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        ExtraCharge extraCharge = buildExtraCharge();
        ActualDeliveryResult actualDeliveryResult =
                parameters.getReportParameters().getActualDelivery().getResults().get(0);
        actualDeliveryResult.getDelivery().forEach(opt -> opt.setExtraCharge(extraCharge));
        actualDeliveryResult.getPickup().forEach(pickupOption -> pickupOption.setExtraCharge(extraCharge));
        MultiCart cart = orderCreateHelper.cart(parameters);
        long count = cart.getCarts().get(0).getDeliveryOptions()
                .stream()
                .filter(opt -> DeliveryType.DELIVERY.equals(opt.getType()) || DeliveryType.PICKUP.equals(opt.getType()))
                .map(Delivery::getExtraCharge)
                .peek(extraChargeActual -> {
                    assertNull(extraChargeActual.getValue());
                    assertEquals(extraCharge.getReasonCodes(), extraChargeActual.getReasonCodes());
                })
                .count();
        assertEquals(2, count);
    }

    @Test
    public void shouldAddExtraChargeParamsInRequestDiscountCalc() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        ActualDelivery actualDelivery = parameters.getReportParameters().getActualDelivery();
        ExtraChargeParameters paramsExpected = buildExtraChargeParameters();
        actualDelivery.setExtraChargeParameters(paramsExpected);
        orderCreateHelper.cart(parameters);

        List<ServeEvent> servedCalcEvents = loyaltyConfigurer.servedEvents().stream()
                .filter(event -> event.getRequest().getAbsoluteUrl().contains(LoyaltyConfigurer.URI_CALC_V3))
                .collect(Collectors.toList());
        servedCalcEvents.forEach(event -> {
            MultiCartWithBundlesDiscountRequest rq = GSON.fromJson(
                    event.getRequest().getBodyAsString(),
                    MultiCartWithBundlesDiscountRequest.class
            );
            OrderExtraChargeDeliveryParams paramsActual = rq.getOrders().get(0).getExtraChargeDeliveryParams();
            assertEquals(paramsExpected.getChargeQuant(), paramsActual.getChargeQuant());
            assertEquals(paramsExpected.getMaxCharge(), paramsActual.getMaxCharge());
            assertEquals(paramsExpected.getMinCharge(), paramsActual.getMinCharge());
            assertEquals(paramsExpected.getVatMultiplier(), paramsActual.getVatMultiplier());
            assertEquals(paramsExpected.getMinChargeOfGmv(), paramsActual.getMinChargeOfGmv());
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldAddExtraChargeParamsFromDeliveryRouteInRequestDiscountSpend(boolean enable) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_DELIVERY_ROUTE_STREAMING_JSON_PARSER, enable);
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                singleton(Constants.COMBINATOR_EXPERIMENT)));
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withCombinator(true)
                .buildParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setMinifyOutlets(true);
        ExtraChargeParameters paramsExpected = buildExtraChargeParameters();
        parameters.getReportParameters().getDeliveryRoute().setExtraChargeParameters(paramsExpected);
        orderCreateHelper.createOrder(parameters);

        List<ServeEvent> servedCalcEvents = loyaltyConfigurer.servedEvents().stream()
                .filter(event -> event.getRequest().getAbsoluteUrl().contains(LoyaltyConfigurer.URI_SPEND_V3))
                .collect(Collectors.toList());
        servedCalcEvents.forEach(event -> {
            MultiCartWithBundlesDiscountRequest rq = GSON.fromJson(
                    event.getRequest().getBodyAsString(),
                    MultiCartWithBundlesDiscountRequest.class
            );
            OrderExtraChargeDeliveryParams paramsActual = rq.getOrders().get(0).getExtraChargeDeliveryParams();
            assertEquals(paramsExpected.getChargeQuant(), paramsActual.getChargeQuant());
            assertEquals(paramsExpected.getMaxCharge(), paramsActual.getMaxCharge());
            assertEquals(paramsExpected.getMinCharge(), paramsActual.getMinCharge());
            assertEquals(paramsExpected.getVatMultiplier(), paramsActual.getVatMultiplier());
            assertEquals(paramsExpected.getMinChargeOfGmv(), paramsActual.getMinChargeOfGmv());
        });
    }

    @NotNull
    private ExtraChargeParameters buildExtraChargeParameters() {
        ExtraChargeParameters extraChargeParameters = new ExtraChargeParameters();
        extraChargeParameters.setMinCharge(BigDecimal.valueOf(1));
        extraChargeParameters.setMaxCharge(BigDecimal.valueOf(2));
        extraChargeParameters.setChargeQuant(BigDecimal.valueOf(3));
        extraChargeParameters.setVatMultiplier(BigDecimal.valueOf(4));
        extraChargeParameters.setMinChargeOfGmv(BigDecimal.valueOf(5));
        return extraChargeParameters;
    }

    @Test
    public void shouldReturnExtraChargeFromLoyalty() {
        checkouterFeatureWriter.writeValue(NEW_LARGE_SIZE_CALCULATION, true);

        BigDecimal extraChargeValueExpected = BigDecimal.valueOf(21);

        ExtraCharge extraCharge = buildExtraCharge();
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setExperiments(Experiments.DYNAMIC_TARIFFS_ENRICH_DELIVERY_PROMO);
        parameters.setCheckCartErrors(false);
        parameters.setSpreadAlgorithmV2(true);
        ActualDeliveryResult actualDeliveryResult =
                parameters.getReportParameters().getActualDelivery().getResults().get(0);
        actualDeliveryResult.getDelivery().forEach(opt -> opt.setExtraCharge(extraCharge));
        actualDeliveryResult.getPickup().forEach(pickupOption -> pickupOption.setExtraCharge(extraCharge));
        MultiCart cart = orderCreateHelper.cart(parameters);

        Delivery delivery = mockLoyalty(extraChargeValueExpected, parameters, cart);

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        checkLoyaltyRequest(extraCharge);
        List<Delivery> deliveriesActual = multiCart.getCarts().get(0).getDeliveryOptions()
                .stream()
                .filter(d -> d.getDeliveryOptionId().equals(delivery.getDeliveryOptionId()))
                .collect(Collectors.toList());
        assertEquals(1, deliveriesActual.size());
        var extraChargeActual = deliveriesActual.get(0).getExtraCharge();
        assertEquals(extraChargeValueExpected, extraChargeActual.getValue());
    }

    @NotNull
    private Delivery mockLoyalty(BigDecimal extraChargeValueExpected, Parameters parameters, MultiCart cart) {
        Delivery delivery = cart.getCarts().get(0).getDeliveryOptions()
                .stream().filter(d -> DeliveryType.DELIVERY.equals(d.getType())).findFirst().orElse(null);
        delivery.setRegionId(213L);
        parameters.getOrder().setDelivery(delivery);
        parameters.setMockLoyalty(true);
        LoyaltyParameters loyaltyParameters = parameters.getLoyaltyParameters();
        loyaltyParameters.setPromoOnlySelectedOption(true);
        loyaltyParameters.addDeliveryDiscount(LoyaltyDiscount.builder()
                .promoType(PromoType.MULTICART_DISCOUNT)
                .discount(BigDecimal.valueOf(4))
                .extraCharge(extraChargeValueExpected).build());
        return delivery;
    }

    private void checkLoyaltyRequest(ExtraCharge extraCharge) {
        List<ServeEvent> servedCalcEvents = loyaltyConfigurer.servedEvents().stream()
                .filter(event -> event.getRequest().getAbsoluteUrl().contains(LoyaltyConfigurer.URI_CALC_V3))
                .collect(Collectors.toList());
        servedCalcEvents.forEach(event -> {
            MultiCartWithBundlesDiscountRequest rq = GSON.fromJson(
                    event.getRequest().getBodyAsString(),
                    MultiCartWithBundlesDiscountRequest.class
            );
            rq.getOrders().get(0).getDeliveries()
                    .stream()
                    .filter(DeliveryRequest::isSelected)
                    .forEach(deliveryRequest ->
                            assertEquals(extraCharge.getUnitEconomyValue(), deliveryRequest.getUnitEconomyValue()));
        });
    }

    @NotNull
    private ExtraCharge buildExtraCharge() {
        ExtraCharge extraCharge = new ExtraCharge();
        extraCharge.setValue(new BigDecimal("123"));
        extraCharge.setUnitEconomyValue(new BigDecimal("222"));
        extraCharge.setReasonCodes(List.of("REASON"));
        return extraCharge;
    }
}
