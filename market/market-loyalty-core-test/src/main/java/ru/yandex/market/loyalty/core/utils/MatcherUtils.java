package ru.yandex.market.loyalty.core.utils;

import org.hamcrest.Matcher;

import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.coin.CoinStatus;
import ru.yandex.market.loyalty.api.model.coin.UserCoinResponse;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.order.ItemKey;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public final class MatcherUtils {
    private MatcherUtils() {
    }

    public static <T> Matcher<T> hasItemKey(ItemKey itemKey) {
        return allOf(
                hasProperty("offerId", equalTo(itemKey.getOfferId())),
                hasProperty("feedId", comparesEqualTo(itemKey.getFeedId()))
        );
    }

    public static <T> Matcher<T> hasPromo(PromoType promoType, String promoKey, CoinKey coinKey) {
        return allOf(
                hasProperty("promoType", equalTo(promoType)),
                hasProperty("promoKey", equalTo(promoKey)),
                hasProperty("usedCoin", coinHasKey(coinKey))
        );
    }

    public static <T> Matcher<T> hasSmartShopingPromo(String promoKey, CoinKey coinKey) {
        return hasPromo(PromoType.SMART_SHOPPING, promoKey, coinKey);
    }

    public static Matcher<UserCoinResponse> coinHasKey(CoinKey coinKey) {
        return hasProperty("id", equalTo(coinKey.getId()));
    }

    public static Matcher<UserCoinResponse> coinStatus(CoinStatus coinStatus, boolean requireAuth) {
        return allOf(
                hasProperty("status", equalTo(coinStatus)),
                hasProperty("requireAuth", equalTo(requireAuth)),
                hasProperty("activationToken", requireAuth ? is(not(emptyOrNullString())) : is(emptyOrNullString()))
        );
    }

    public static <T> List<Matcher<? super T>> repeatMatcher(int count, Matcher<T> matcher) {
        return IntStream.range(0, count)
                .mapToObj(i -> matcher)
                .collect(Collectors.toList());
    }
}
