package ru.yandex.market.checkout.checkouter.cashback;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.AntifraudCheckResult;
import ru.yandex.market.antifraud.orders.entity.OrderVerdict;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOption;
import ru.yandex.market.checkout.util.balance.TrustMockConfigurer;
import ru.yandex.market.loyalty.api.model.CashbackOptions;
import ru.yandex.market.loyalty.api.model.CashbackResponse;
import ru.yandex.market.loyalty.api.model.CashbackRestrictionReason;
import ru.yandex.market.loyalty.api.model.CashbackType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.util.loyalty.LoyaltyConfigurer.URI_CALC_V3;
import static ru.yandex.market.checkout.util.loyalty.LoyaltyConfigurer.URI_CASHBACK_OPTIONS;


public class CashbackActualizationTest extends CashbackTestBase {

    @Test
    void shouldFillCashbackInfo() {
        MultiCart cart = orderCreateHelper.cart(singleItemWithCashbackParams);

        assertThat(cart, allOf(
                hasProperty("cashbackOptionsProfiles", not(empty())),
                hasProperty("cashbackBalance", not(is(BigDecimal.ZERO))),
                hasProperty("validationErrors", nullValue()),
                hasProperty("cashback", notNullValue())));
    }

    @Test
    void shouldFillCashbackInfoForBusinessClient() {
        singleItemWithCashbackParams.getOrder().getBuyer().setBusinessBalanceId(123L);
        MultiCart cart = orderCreateHelper.cart(singleItemWithCashbackParams);

        assertThat(cart, allOf(
                hasProperty("cashbackOptionsProfiles", nullValue()),
                hasProperty("cashbackBalance", notNullValue()),
                hasProperty("validationErrors", nullValue()),
                hasProperty("cashback", notNullValue())));
    }

    @Test
    void shouldNotBlockCartOnBalanceFail() {
        trustMockConfigurer.resetAll();
        singleItemWithCashbackParams.getTrustParameters().setCustomTrustMockConfiguration(
                TrustMockConfigurer::mockRateLimitHit);

        MultiCart cart = orderCreateHelper.cart(singleItemWithCashbackParams);

        assertThat(cart, allOf(
                hasProperty("cashbackOptionsProfiles", not(empty())),
                hasProperty("cashbackBalance", is(BigDecimal.ZERO)),
                hasProperty("validationErrors", nullValue())));
    }

    @Test
    void shouldNotBlockCartOnLoyaltyFail() {
        loyaltyConfigurer.resetAll();
        singleItemWithCashbackParams.getLoyaltyParameters().setCustomLoyaltyMockConfiguration(configurer ->
                configurer.mockCashbackOptionsError(null, 400));

        MultiCart cart = orderCreateHelper.cart(singleItemWithCashbackParams);

        assertThat(cart, allOf(
                hasProperty("cashbackOptionsProfiles", not(empty())),
                hasProperty("cashbackBalance", is(not(BigDecimal.ZERO))),
                hasProperty("validationErrors", hasItem(
                        hasProperty("code", is("OTHER_ERROR"))))));
    }

    @Test
    void shouldReturnSelectedCashbackOptionOnCart() {
        singleItemWithCashbackParams.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        MultiCart cart = orderCreateHelper.cart(singleItemWithCashbackParams);

        assertThat(cart, hasProperty("selectedCashbackOption", is(CashbackOption.EMIT)));
    }

    @Test
    void shouldNotFailIfCashbackOptionSelectedOnIncompleteCart() {
        singleItemWithCashbackParams.getLoyaltyParameters().setCalcsExpectedCashbackResponse(new CashbackResponse(
                CashbackOptions.restricted(CashbackRestrictionReason.INCOMPLETE_REQUEST),
                CashbackOptions.restricted(CashbackRestrictionReason.INCOMPLETE_REQUEST),
                CashbackType.EMIT)
        );
        MultiCart cart = orderCreateHelper.cart(singleItemWithCashbackParams);

        assertThat(cart, hasProperty("validationErrors", nullValue()));
    }

    @Test
    void shouldNotBlockCashbackOptionsIfCalcFailed() {
        loyaltyConfigurer.resetAll();
        singleItemWithCashbackParams.getLoyaltyParameters().setCustomLoyaltyMockConfiguration(configurer ->
                configurer.mockCalcError(null, 422));

        orderCreateHelper.cart(singleItemWithCashbackParams);

        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        assertThat(serveEvents.stream()
                .filter(event -> event.getRequest().getAbsoluteUrl().contains(URI_CASHBACK_OPTIONS))
                .collect(Collectors.toList()), not(emptyIterable()));
    }

    @Test
    public void shouldNotCallCashbackOptionsIfThereIsCriticalValidationError() {
        var parameters = defaultBlueOrderParameters();
        parameters.setMultiCartAction(mc ->
                mstatAntifraudConfigurer.mockVerdict(
                        OrderVerdict.builder()
                                .checkResults(Collections.singleton(
                                        new AntifraudCheckResult(AntifraudAction.CANCEL_ORDER, "", "")
                                ))
                                .build()));
        parameters.setCheckOrderCreateErrors(false);
        var order = orderCreateHelper.createMultiOrder(parameters);

        assertThat(order.isValid(), is(false));
        assertThat(order.getOrderFailures(), nullValue());
        assertThat(order.getValidationErrors(), hasSize(1));
        assertThat(order.getValidationErrors().get(0).getCode(), CoreMatchers.is("FRAUD_DETECTED"));

        List<ServeEvent> servedEvents = loyaltyConfigurer.servedEvents();
        var calcEvents = servedEvents.stream()
                .filter(event -> event.getRequest().getAbsoluteUrl().contains(URI_CALC_V3))
                .collect(Collectors.toList());
        var cashbackEvents = servedEvents.stream()
                .filter(event -> event.getRequest().getAbsoluteUrl().contains(URI_CASHBACK_OPTIONS))
                .collect(Collectors.toList());

        // проверяем, что во время /cart, когда не было ошибки антифрода, был поход за кешбэком, а во время
        // /checkout уже была ошибка и второго похода за кешбэком не было
        assertThat(calcEvents, hasSize(2));
        assertThat(cashbackEvents, hasSize(1));
    }
}
