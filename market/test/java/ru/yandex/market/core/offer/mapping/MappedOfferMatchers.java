package ru.yandex.market.core.offer.mapping;

import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.mbi.util.MbiMatchers;

@ParametersAreNonnullByDefault
final class MappedOfferMatchers {
    private MappedOfferMatchers() {
        throw new UnsupportedOperationException("Shouldn't be instantiated");
    }

    static <T> Matcher<ModeratedLink<T>> isModeratedLink(
            ModerationStatus status,
            Matcher<T> matcher
    ) {
        return Matchers.allOf(
                MbiMatchers.transformedBy(ModeratedLink::status, Matchers.equalTo(status)),
                MbiMatchers.transformedBy(ModeratedLink::target, matcher)
        );
    }

    static Matcher<MarketEntityInfo> isMarketSku(long value) {
        return MbiMatchers.transformedBy(
                MarketEntityInfo::marketSku,
                MbiMatchers.isPresent(MbiMatchers.transformedBy(MarketSkuInfo::marketSku, Matchers.is(value))));
    }

    static Matcher<ShopOffer> isShopOffer(long shopId, String shopSku, String title) {
        return Matchers.allOf(
                MbiMatchers.transformedBy(ShopOffer::supplierId, Matchers.equalTo(shopId)),
                MbiMatchers.transformedBy(ShopOffer::shopSku, Matchers.equalTo(shopSku)),
                MbiMatchers.transformedBy(ShopOffer::title, Matchers.equalTo(title))
        );
    }

    static Matcher<MappedOffer> isMappedOffer(
            Matcher<ShopOffer> shopOfferMatcher,
            Matcher<Optional<MarketEntityInfo>> activeLinkMatcher,
            Matcher<Optional<ModeratedLink<MarketEntityInfo>>> supplierLinkMatcher
    ) {
        return Matchers.allOf(
                MbiMatchers.transformedBy(MappedOffer::shopOffer, shopOfferMatcher),
                MbiMatchers.transformedBy(MappedOffer::activeLink, activeLinkMatcher),
                MbiMatchers.transformedBy(MappedOffer::partnerLink, supplierLinkMatcher)
        );
    }
}
