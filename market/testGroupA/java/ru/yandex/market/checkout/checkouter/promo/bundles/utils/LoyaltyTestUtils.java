package ru.yandex.market.checkout.checkouter.promo.bundles.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.actualization.flow.ContextualFlowRuntimeSession;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.CartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.checkouter.service.business.LoyaltyContext;
import ru.yandex.market.checkout.helpers.FlowSessionHelper;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.FoundOffer;

public final class LoyaltyTestUtils {

    private LoyaltyTestUtils() {
    }

    public static LoyaltyContext createTestContextWithBuilders(
            MultiCart multiCart,
            Collection<FoundOfferBuilder> reportOffersBuilder) {
        return createTestContext(multiCart, reportOffersBuilder.stream()
                .map(FoundOfferBuilder::build)
                .collect(Collectors.toUnmodifiableList()));
    }

    public static LoyaltyContext createTestContext(MultiCart multiCart, Collection<FoundOffer> reportOffers) {
        MultiCartContext multiCartContext = MultiCartContext.createBy(ImmutableMultiCartParameters.builder().build(),
                Map.of());
        MultiCartFetchingContext fetchingContext = MultiCartFetchingContext.of(multiCartContext,
                multiCart);
        FlowSessionHelper.patchSession(
                fetchingContext,
                MultiCartFetchingContext::makeImmutableContext,
                (c, v) -> c.getMultiCartContext().setOffersStage(v),
                List.copyOf(reportOffers)
        );
        var contextBuilder = ActualizationContext.builder()
                .withImmutableMulticartContext(ImmutableMultiCartContext.from(multiCartContext, multiCart))
                .withCart(multiCart.getCarts().get(0))
                .withInitialCart(ImmutableOrder.from(multiCart.getCarts().get(0)))
                .withOriginalBuyerCurrency(Currency.RUR);
        return LoyaltyContext.createContext(
                Collections.singleton(contextBuilder
                        .withFlowRuntimeSession(
                                ContextualFlowRuntimeSession.empty(CartFetchingContext.of(
                                        fetchingContext,
                                        contextBuilder,
                                        multiCart.getCarts().iterator().next())))
                        .build()));
    }
}
