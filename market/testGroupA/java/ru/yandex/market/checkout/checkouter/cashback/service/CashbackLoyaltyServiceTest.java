package ru.yandex.market.checkout.checkouter.cashback.service;


import java.util.Collections;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cashback.CashbackException;
import ru.yandex.market.checkout.checkouter.cashback.CashbackTestBase;
import ru.yandex.market.checkout.checkouter.service.business.LoyaltyContext;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.loyalty.api.model.CashbackOptionsResponse;
import ru.yandex.market.loyalty.api.model.CashbackPermision;
import ru.yandex.market.loyalty.api.model.CashbackProfileResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.cashback.CashbackRequestFactoryTest.CART_LABEL;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.REMOVE_RESTRICTED_CASHBACK_OPTION_PROFILES;
import static ru.yandex.market.checkout.providers.CashbackTestProvider.FEED;
import static ru.yandex.market.checkout.providers.CashbackTestProvider.OFFER;
import static ru.yandex.market.checkout.providers.CashbackTestProvider.singleItemCashbackResponse;
import static ru.yandex.market.checkout.test.providers.OrderProvider.orderBuilder;

public class CashbackLoyaltyServiceTest extends CashbackTestBase {

    @Test
    void shouldThrowExceptionOnLoyaltyFail() {
        loyaltyConfigurer.resetAll();
        loyaltyConfigurer.mockCashbackOptionsError(null, 400);
        MultiCart cart = MultiCartProvider.single(orderBuilder()
                .label(CART_LABEL)
                .item(OrderItemProvider.getOrderItem()));

        assertThrows(CashbackException.class, () ->
                cashbackProfilesService.fillCashbackOptions(cart,
                        LoyaltyContext.createContext(Collections.emptyList())));
    }

    @Test
    void shouldFillCartCashbackOptions() {
        checkouterFeatureWriter.writeValue(REMOVE_RESTRICTED_CASHBACK_OPTION_PROFILES, false);
        MultiCart cart = MultiCartProvider.single(orderBuilder()
                .itemBuilder(OrderItemProvider.orderItemBuilder()
                        .configure(OrderItemProvider::applyDefaults)
                        .configure(b -> b.offer(OFFER)
                                .feedId(FEED))));

        CashbackOptionsResponse cashbackOptionsResponse = singleItemCashbackResponse();
        assertTrue(cashbackOptionsResponse.getCashbackOptionsProfiles()
                .stream()
                .map(CashbackProfileResponse::getCashback)
                .flatMap(cashback -> Stream.of(cashback.getEmit(), cashback.getSpend()))
                .filter(Objects::nonNull)
                .anyMatch(option -> option.getType() == CashbackPermision.RESTRICTED));

        loyaltyConfigurer.mockCashbackOptions(cashbackOptionsResponse, HttpStatus.OK);

        cashbackProfilesService.fillCashbackOptions(cart, LoyaltyContext.createContext(Collections.emptyList()));

        assertThat(
                cart.getCashbackOptionsProfiles(),
                hasSize(cashbackOptionsResponse.getCashbackOptionsProfiles().size())
        );
        assertThat(cart.getValidationErrors(), nullValue());
    }

    @Test
    void shouldFillCartCashbackOptionsWithoutRestrictedProfiles() {
        checkouterFeatureWriter.writeValue(REMOVE_RESTRICTED_CASHBACK_OPTION_PROFILES, true);
        MultiCart cart = MultiCartProvider.single(orderBuilder()
                .itemBuilder(OrderItemProvider.orderItemBuilder()
                        .configure(OrderItemProvider::applyDefaults)
                        .configure(b -> b.offer(OFFER)
                                .feedId(FEED))));

        CashbackOptionsResponse cashbackOptionsResponse = singleItemCashbackResponse();
        int restrictedCount = (int) cashbackOptionsResponse.getCashbackOptionsProfiles()
                .stream()
                .map(CashbackProfileResponse::getCashback)
                .filter(cashback ->
                        (cashback.getEmit() == null
                                || cashback.getEmit().getType() == CashbackPermision.RESTRICTED)
                                && (cashback.getSpend() == null
                                || cashback.getSpend().getType() == CashbackPermision.RESTRICTED)
                )
                .count();

        assertThat(restrictedCount, greaterThan(0));

        loyaltyConfigurer.mockCashbackOptions(cashbackOptionsResponse, HttpStatus.OK);

        cashbackProfilesService.fillCashbackOptions(cart, LoyaltyContext.createContext(Collections.emptyList()));

        // Restricted profiles should be removed
        int profilesCount = cashbackOptionsResponse.getCashbackOptionsProfiles().size() - restrictedCount;
        assertThat(cart.getCashbackOptionsProfiles(), hasSize(profilesCount));
        assertThat(cart.getValidationErrors(), nullValue());
    }

}
