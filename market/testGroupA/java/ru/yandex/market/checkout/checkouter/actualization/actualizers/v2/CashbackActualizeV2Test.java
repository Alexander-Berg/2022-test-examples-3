package ru.yandex.market.checkout.checkouter.actualization.actualizers.v2;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cashback.CashbackTestBase;
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
import static ru.yandex.market.checkout.util.loyalty.LoyaltyConfigurer.URI_CASHBACK_OPTIONS;

public class CashbackActualizeV2Test extends CashbackTestBase {

    @Test
    void shouldFillCashbackInfo() {
        MultiCart cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(singleItemWithCashbackParams);

        assertThat(cart, allOf(
                hasProperty("cashbackOptionsProfiles", not(empty())),
                hasProperty("cashbackBalance", not(is(BigDecimal.ZERO))),
                hasProperty("validationErrors", nullValue()),
                hasProperty("cashback", notNullValue())));
    }

    @Test
    void shouldFillCashbackInfoForBusinessClient() {
        singleItemWithCashbackParams.getOrder().getBuyer().setBusinessBalanceId(123L);
        MultiCart cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(singleItemWithCashbackParams);

        assertThat(cart, allOf(
                hasProperty("cashbackOptionsProfiles", nullValue()),
                hasProperty("cashbackBalance", notNullValue()),
                hasProperty("validationErrors", nullValue()),
                hasProperty("cashback", notNullValue())));
    }

    @Test
    void shouldNotBlockCartOnBalanceFail() {
        trustMockConfigurer.resetAll();
        singleItemWithCashbackParams.getTrustParameters()
                .setCustomTrustMockConfiguration(TrustMockConfigurer::mockRateLimitHit);

        MultiCart cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(singleItemWithCashbackParams);

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

        MultiCart cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(singleItemWithCashbackParams);

        assertThat(cart, allOf(
                hasProperty("cashbackOptionsProfiles", not(empty())),
                hasProperty("cashbackBalance", is(not(BigDecimal.ZERO))),
                hasProperty("validationErrors", hasItem(
                        hasProperty("code", is("OTHER_ERROR"))))));
    }

    @Test
    void shouldReturnSelectedCashbackOptionOnCart() {
        singleItemWithCashbackParams.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        MultiCart cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(singleItemWithCashbackParams);

        assertThat(cart, hasProperty("selectedCashbackOption", is(CashbackOption.EMIT)));
    }

    @Test
    void shouldNotFailIfCashbackOptionSelectedOnIncompleteCart() {
        singleItemWithCashbackParams.getLoyaltyParameters().setCalcsExpectedCashbackResponse(new CashbackResponse(
                CashbackOptions.restricted(CashbackRestrictionReason.INCOMPLETE_REQUEST),
                CashbackOptions.restricted(CashbackRestrictionReason.INCOMPLETE_REQUEST),
                CashbackType.EMIT)
        );
        MultiCart cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(singleItemWithCashbackParams);

        assertThat(cart, hasProperty("validationErrors", nullValue()));
    }

    @Test
    void shouldNotBlockCashbackOptionsIfCalcFailed() {
        loyaltyConfigurer.resetAll();
        singleItemWithCashbackParams.getLoyaltyParameters().setCustomLoyaltyMockConfiguration(configurer ->
                configurer.mockCalcError(null, 422));

        orderCreateHelper.multiCartActualizeWithMapToMultiCart(singleItemWithCashbackParams);

        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        assertThat(serveEvents.stream()
                .filter(event -> event.getRequest().getAbsoluteUrl().contains(URI_CASHBACK_OPTIONS))
                .collect(Collectors.toList()), not(emptyIterable()));
    }

}
