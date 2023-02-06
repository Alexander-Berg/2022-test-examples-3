package ru.yandex.market.checkout.checkouter.service.business;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOption;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.loyalty.LoyaltyParameters;
import ru.yandex.market.common.report.model.PayByYaPlus;
import ru.yandex.market.loyalty.api.model.CashbackOptions;
import ru.yandex.market.loyalty.api.model.CashbackResponse;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.loyalty.api.model.cart.CartFlag;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.checkout.checkouter.validation.ValidationResult.Severity.ERROR;
import static ru.yandex.market.checkout.checkouter.validation.ValidationResult.Severity.WARNING;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;

public class YandexPlusSaleTest extends AbstractWebTestBase {

    private static final ValidationResult YA_PLUS_SALE_MULTIPLE_OFFERS_IN_CART = new ValidationResult(
            "YA_PLUS_SALE_MULTIPLE_OFFERS_IN_CART", WARNING);

    private static final ValidationResult YA_PLUS_SALE_DISABLED = new ValidationResult(
            "YA_PLUS_SALE_DISABLED", WARNING);

    private static final ValidationResult YA_PLUS_SALE_USER_HAS_NOT_ENOUGH_PLUS = new ValidationResult(
            "YA_PLUS_SALE_USER_HAS_NOT_ENOUGH_PLUS", WARNING);

    private static final ValidationResult SELECTED_OPTION_IS_NOT_SELECTED =
            new ValidationResult("SELECTED_OPTION_IS_NOT_SELECTED", ERROR);

    @Test
    public void shouldNotAddValidationWarningWhenThereAreNoPayByYaPlusOffersInCart() {
        Parameters parameters = defaultBlueOrderParameters();
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        Order cart = getOnlyElement(multiCart.getCarts());
        assertThat(cart.getValidationWarnings(), is(nullValue()));
    }

    @Test
    public void shouldNotAddValidationWarningWhenEverythingIsOk() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        addPayByYaPlusOffer(parameters, 301);
        parameters.getLoyaltyParameters().setYandexPlusSale(CartFlag.YANDEX_PLUS_SALE_ENABLE);

        parameters.getLoyaltyParameters()
                .expectResponseItem(
                        itemResponseFor(parameters.getOrder().getItems().iterator().next())
                                .cashback(new CashbackResponse(null, CashbackOptions.allowed(BigDecimal.valueOf(300))))
                );
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        MultiOrder multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiCart.getValidationWarnings(), is(nullValue()));
        assertThat(multiOrder.getValidationWarnings(), is(nullValue()));
    }

    @Test
    public void shouldAddValidationWarningWhenYaPlusOfferIsAmongOtherOffersInCart() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.addOtherItem();
        addPayByYaPlusOffer(parameters, null);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(getOnlyElement(multiCart.getValidationWarnings()), is(YA_PLUS_SALE_MULTIPLE_OFFERS_IN_CART));
    }

    @Test
    public void shouldAddValidationWarningWhenThereIsNoYaPlusSaleAtTheMoment() {
        Parameters parameters = defaultBlueOrderParameters();
        addPayByYaPlusOffer(parameters, null);
        parameters.getLoyaltyParameters().setYandexPlusSale(CartFlag.YANDEX_PLUS_SALE_DISABLE);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(getOnlyElement(multiCart.getValidationWarnings()), is(YA_PLUS_SALE_DISABLED));
    }

    @Test
    public void shouldAddValidationWarningWhenUserDoesNotHaveEnoughYandexPlusPoints() {
        Parameters parameters = defaultBlueOrderParameters();
        addPayByYaPlusOffer(parameters, 300);
        LoyaltyParameters loyaltyParameters = parameters.getLoyaltyParameters();
        loyaltyParameters.setYandexPlusSale(CartFlag.YANDEX_PLUS_SALE_ENABLE);
        loyaltyParameters.expectResponseItem(
                itemResponseFor(parameters.getOrder().getItems().iterator().next())
                        .cashback(new CashbackResponse(CashbackOptions.allowed(BigDecimal.valueOf(298)), null))
        );
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(getOnlyElement(multiCart.getValidationWarnings()), is(YA_PLUS_SALE_USER_HAS_NOT_ENOUGH_PLUS));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void shouldAddValidationWarningWhenCashbackSpendAmountIsNull() {
        Parameters parameters = defaultBlueOrderParameters();
        addPayByYaPlusOffer(parameters, 300);
        LoyaltyParameters loyaltyParameters = parameters.getLoyaltyParameters();
        loyaltyParameters.setYandexPlusSale(CartFlag.YANDEX_PLUS_SALE_ENABLE);
        loyaltyParameters.expectResponseItem(
                itemResponseFor(parameters.getOrder().getItems().iterator().next())
                        .cashback(new CashbackResponse(
                                CashbackOptions.allowed(BigDecimal.valueOf(298)),
                                CashbackOptions.allowed(null))
                        )
        );
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(getOnlyElement(multiCart.getValidationWarnings()), is(YA_PLUS_SALE_USER_HAS_NOT_ENOUGH_PLUS));
    }

    @Test
    public void shouldExpectErrorIfSelectedCashbackOptionIsNotSpend() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.turnOffErrorChecks();
        parameters.setUseErrorMatcher(false);
        addPayByYaPlusOffer(parameters, 300);
        LoyaltyParameters loyaltyParameters = parameters.getLoyaltyParameters();
        loyaltyParameters.setYandexPlusSale(CartFlag.YANDEX_PLUS_SALE_ENABLE);
        loyaltyParameters.expectResponseItem(
                itemResponseFor(parameters.getOrder().getItems().iterator().next())
                        .cashback(new CashbackResponse(
                                CashbackOptions.allowed(BigDecimal.valueOf(299)),
                                CashbackOptions.allowed(null))
                        )
        );
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        multiCart.setSelectedCashbackOption(CashbackOption.UNKNOWN);
        MultiOrder checkout = orderCreateHelper.checkout(multiCart, parameters);
        List<ValidationResult> validationErrors = checkout.getValidationErrors();
        Assertions.assertFalse(checkout.isValid());
        assertThat(validationErrors, hasSize(1));
        assertThat(validationErrors.get(0), is(SELECTED_OPTION_IS_NOT_SELECTED));
    }

    private static void addPayByYaPlusOffer(Parameters parameters, Integer price) {
        OrderItem firstItem = parameters.getOrder().getItems().iterator().next();
        parameters.getReportParameters()
                .overrideItemInfo(firstItem.getFeedOfferId())
                .setPayByYaPlus(PayByYaPlus.of(price == null ? firstItem.getPrice().intValue() : price));
    }
}
