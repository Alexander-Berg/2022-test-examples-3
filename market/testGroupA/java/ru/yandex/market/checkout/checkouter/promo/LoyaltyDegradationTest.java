package ru.yandex.market.checkout.checkouter.promo;

import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentBooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.validation.PromoCodeValidationResult;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.loyalty.LoyaltyConfigurer;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoyaltyDegradationTest extends AbstractWebTestBase {

    @BeforeEach
    public void setUp() throws Exception {
        checkouterFeatureWriter.writeValue(PermanentBooleanFeatureType.SKIP_DISCOUNT_CALCULATION_ENABLED, true);
    }

    @Test
    public void shouldThrowValidationErrorWhen5xxOnCalc() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setupPromo("PROMO");
        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode("PROMO_CODE"));
        parameters.getLoyaltyParameters().setCustomLoyaltyMockConfiguration(
                loyaltyConfigurer -> loyaltyConfigurer.mockCalcError(null, 500));

        PromoCodeValidationResult expectedErrors = new PromoCodeValidationResult("PROMO_CODE",
                MarketLoyaltyErrorCode.OTHER_ERROR.name(),
                ValidationResult.Severity.ERROR, null);

        parameters.setCheckCartErrors(false);
        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(cart.getValidationErrors(), containsInAnyOrder(expectedErrors));
    }

    @Test
    public void shouldThrowValidationErrorWhenRequestTimeoutOnCalc() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setupPromo("PROMO");
        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode("PROMO_CODE"));
        parameters.getLoyaltyParameters().setCustomLoyaltyMockConfiguration(LoyaltyConfigurer::mockCalcRequestTimeout);

        PromoCodeValidationResult expectedError = new PromoCodeValidationResult("PROMO_CODE",
                MarketLoyaltyErrorCode.OTHER_ERROR.name(),
                ValidationResult.Severity.ERROR, null);

        parameters.setCheckCartErrors(false);
        MultiCart cart = orderCreateHelper.cart(parameters);

        assertThat(cart.getValidationErrors(), containsInAnyOrder(expectedError));
    }

    @Test
    public void shouldThrowValidationErrorWhen5xxOnSpend() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setupPromo("PROMO");
        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode("PROMO_CODE"));
        parameters.getLoyaltyParameters().setCustomLoyaltyMockConfiguration(
                loyaltyConfigurer -> loyaltyConfigurer.mockSpendError(null, 500));

        PromoCodeValidationResult expectedError = new PromoCodeValidationResult("PROMO_CODE",
                MarketLoyaltyErrorCode.OTHER_ERROR.name(),
                ValidationResult.Severity.ERROR, null);

        parameters.setCheckCartErrors(false);
        parameters.setCheckOrderCreateErrors(false);

        MultiOrder order = orderCreateHelper.createMultiOrder(parameters);

        assertThat(order.getValidationErrors(), containsInAnyOrder(expectedError));
    }

    @Test
    public void shouldThrowValidationErrorWhenRequestTimeoutOnSpend() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setupPromo("PROMO");
        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode("PROMO_CODE"));
        parameters.getLoyaltyParameters().setCustomLoyaltyMockConfiguration(LoyaltyConfigurer::mockSpendRequestTimeout);

        PromoCodeValidationResult expectedError = new PromoCodeValidationResult("PROMO_CODE",
                MarketLoyaltyErrorCode.OTHER_ERROR.name(),
                ValidationResult.Severity.ERROR, null);

        parameters.setCheckCartErrors(false);
        parameters.setCheckOrderCreateErrors(false);
        MultiOrder order = orderCreateHelper.createMultiOrder(parameters);

        assertThat(order.getValidationErrors(), containsInAnyOrder(expectedError));
    }

    @Test
    public void shouldSkipDiscountCalculationOnCartWhenSkipDiscountCalulationIsTrue() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setSkipDiscountCalculation(true);

        parameters.setupPromo("PROMO");
        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode("PROMO_CODE"));
        parameters.getLoyaltyParameters().setCustomLoyaltyMockConfiguration(LoyaltyConfigurer::mockSpendRequestTimeout);

        orderCreateHelper.cart(parameters);

        assertTrue(loyaltyConfigurer.servedEvents().isEmpty());
    }


    @Test
    public void shouldSkipDiscountCalculationOnCheckoutWhenSkipDiscountCalulationIsTrue() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setSkipDiscountCalculation(true);

        parameters.setupPromo("PROMO");
        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode("PROMO_CODE"));
        parameters.getLoyaltyParameters().setCustomLoyaltyMockConfiguration(loyaltyConfigurer -> {
            loyaltyConfigurer.mockCalcRequestTimeout();
            loyaltyConfigurer.mockSpendRequestTimeout();
        });

        Order order = orderCreateHelper.createOrder(parameters);

        assertTrue(loyaltyConfigurer.servedEvents().isEmpty(), () -> loyaltyConfigurer.servedEvents().stream()
                .map(event -> event.getRequest().toString())
                .collect(Collectors.joining()));

        order = orderService.getOrder(order.getId());
        assertTrue(Objects.requireNonNull(order.getProperty(OrderPropertyType.CREATED_WITHOUT_DISCOUNT_CALCULATION)));
    }

    @Test
    public void shouldReturnErrorWhenLoyaltyDegradationMockIsSetOnCart() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setupPromo("PROMO");
        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode("PROMO_CODE"));
        parameters.configuration().cart().request().setMockLoyaltyDegradation(true);

        PromoCodeValidationResult expectedError = new PromoCodeValidationResult("PROMO_CODE",
                MarketLoyaltyErrorCode.OTHER_ERROR.name(),
                ValidationResult.Severity.ERROR, null);

        parameters.setCheckCartErrors(false);
        MultiCart cart = orderCreateHelper.cart(parameters);

        assertThat(cart.getValidationErrors(), containsInAnyOrder(expectedError));
    }

    @Test
    public void shouldReturnErrorWhenLoyaltyDegradationMockIsSetOnCheckout() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.configuration().cart().request().setMockLoyaltyDegradation(true);

        parameters.setupPromo("PROMO");
        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode("PROMO_CODE"));
        PromoCodeValidationResult expectedError = new PromoCodeValidationResult("PROMO_CODE",
                MarketLoyaltyErrorCode.OTHER_ERROR.name(),
                ValidationResult.Severity.ERROR, null);

        parameters.setCheckCartErrors(false);
        parameters.setCheckOrderCreateErrors(false);
        MultiOrder order = orderCreateHelper.createMultiOrder(parameters);

        assertThat(order.getValidationErrors(), containsInAnyOrder(expectedError));
    }

    @Test
    public void shouldThrowValidationErrorWhenPumpkinResponseOnCalc() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setupPromo("PROMO");
        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode("PROMO_CODE"));
        parameters.getLoyaltyParameters().setCustomLoyaltyMockConfiguration(LoyaltyConfigurer::mockCalcPumpkinResponse);

        PromoCodeValidationResult expectedError = new PromoCodeValidationResult("PROMO_CODE",
                MarketLoyaltyErrorCode.OTHER_ERROR.name(),
                ValidationResult.Severity.ERROR, null);

        parameters.setCheckCartErrors(false);
        MultiCart cart = orderCreateHelper.cart(parameters);

        assertThat(cart.getValidationErrors(), containsInAnyOrder(expectedError));
    }

    @Test
    public void shouldThrowValidationErrorWhenPumpkinResponseOnSpend() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setupPromo("PROMO");
        parameters.configureMultiCart(multiCart -> multiCart.setPromoCode("PROMO_CODE"));
        parameters.getLoyaltyParameters()
                .setCustomLoyaltyMockConfiguration(LoyaltyConfigurer::mockSpendPumpkinResponse);

        PromoCodeValidationResult expectedError = new PromoCodeValidationResult("PROMO_CODE",
                MarketLoyaltyErrorCode.OTHER_ERROR.name(),
                ValidationResult.Severity.ERROR, null);

        parameters.setCheckCartErrors(false);
        parameters.setCheckOrderCreateErrors(false);
        MultiOrder order = orderCreateHelper.createMultiOrder(parameters);

        assertThat(order.getValidationErrors(), containsInAnyOrder(expectedError));
    }
}
