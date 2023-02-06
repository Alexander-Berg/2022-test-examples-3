package ru.yandex.market.loyalty.core.utils;

import java.util.concurrent.atomic.AtomicLong;

import ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.promo.PromoSubType;
import ru.yandex.market.loyalty.core.trigger.actions.CoinInsertRequest;

import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.defaultNoAuthInfo;
import static ru.yandex.market.sdk.userinfo.service.UidConstants.NO_SIDE_EFFECT_UID;

public class CoinRequestUtils {
    public static final String DEFAULT_ACTIVATION_TOKEN = "someActivationToken";
    private static final AtomicLong COUNTER = new AtomicLong();

    private CoinRequestUtils() {
    }

    public static CoinInsertRequest.Builder defaultNoAuth() {
        return defaultNoAuth(DEFAULT_ACTIVATION_TOKEN);
    }

    public static CoinInsertRequest.Builder defaultNoAuth(String activationToken) {
        return CoinInsertRequest.noAuthMarketBonus(defaultNoAuthInfo().build(), activationToken)
                .setSourceKey(String.valueOf(COUNTER.incrementAndGet()))
                .setReason(CoreCoinCreationReason.OTHER)
                .setStatus(CoreCoinStatus.ACTIVE);
    }

    public static CoinInsertRequest.Builder defaultAuth() {
        return defaultAuth(DEFAULT_UID);
    }

    public static CoinInsertRequest.Builder fakeUserAuth() {
        return defaultAuth(NO_SIDE_EFFECT_UID);
    }

    public static CoinInsertRequest.Builder defaultAuth(long uid) {
        return CoinInsertRequest.authMarketBonus(uid)
                .setSourceKey(String.valueOf(COUNTER.incrementAndGet()))
                .setReason(CoreCoinCreationReason.OTHER)
                .setStatus(CoreCoinStatus.ACTIVE);
    }

    public static CoinInsertRequest.Builder defaultAuthForPromocode(long uid) {
        return CoinInsertRequest.authMarketBonus(uid, PromoSubType.PROMOCODE)
                .setSourceKey(String.valueOf(COUNTER.incrementAndGet()))
                .setReason(CoreCoinCreationReason.OTHER);
    }
}
