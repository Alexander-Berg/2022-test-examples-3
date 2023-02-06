package ru.yandex.market.checkout.util.loyalty.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.codec.digest.DigestUtils;

import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.loyalty.api.model.IdObject;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyError;
import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.coin.CoinCreationReason;
import ru.yandex.market.loyalty.api.model.coin.CoinError;
import ru.yandex.market.loyalty.api.model.coin.CoinStatus;
import ru.yandex.market.loyalty.api.model.coin.CoinType;
import ru.yandex.market.loyalty.api.model.coin.ReasonParamExt;
import ru.yandex.market.loyalty.api.model.coin.UserCoinResponse;

public class CoinDiscountEntry {

    private final long coinId;
    private final String promoKey;
    private final String shopPromoId;
    private final String anaplanId;
    private final MarketLoyaltyError coinError;
    private final PromoType promoType;
    private final Map<OfferItemKey, BigDecimal> itemDiscounts;
    private final boolean unused;
    private final Date created;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public CoinDiscountEntry(long coinId, String promoKey, String shopPromoId, String anaplanId,
                             MarketLoyaltyError coinError, PromoType promoType, Map<OfferItemKey,
            BigDecimal> itemDiscounts, boolean unused) {
        this.coinId = coinId;
        this.promoKey = promoKey;
        this.shopPromoId = shopPromoId;
        this.anaplanId = anaplanId;
        this.coinError = coinError;
        this.promoType = promoType;
        this.itemDiscounts = itemDiscounts;
        this.unused = unused;
        this.created = new Date();
    }

    @Nonnull
    public static Builder coin(long coinId, @Nonnull String promoKey) {
        return new Builder(coinId, promoKey);
    }

    @Nonnull
    public Long getCoinId() {
        return coinId;
    }

    @Nonnull
    public String getPromoKey() {
        return promoKey;
    }

    @Nullable
    public String getAnaplanId() {
        return anaplanId;
    }

    @Nullable
    public String getShopPromoId() {
        return shopPromoId;
    }

    @Nullable
    public MarketLoyaltyError getCoinError() {
        return coinError;
    }

    @Nullable
    public PromoType getPromoType() {
        return promoType;
    }

    @Nonnull
    public Map<OfferItemKey, BigDecimal> getItemDiscounts() {
        return itemDiscounts;
    }

    public boolean isUnused() {
        return unused;
    }

    public boolean hasError() {
        return coinError != null;
    }

    @Nonnull
    public UserCoinResponse toCoinResponse() {
        var someString = DigestUtils.md5Hex(coinId + "");
        return new UserCoinResponse(
                coinId,
                someString,
                someString,
                CoinType.FIXED,
                //TODO: не учитывается количество
                itemDiscounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add),
                someString,
                someString,
                created,
                Date.from(LocalDate.now()
                        .plusDays(1)
                        .atStartOfDay().toInstant(ZoneOffset.UTC)),
                someString,
                Map.of(someString, someString),
                someString,
                CoinStatus.ACTIVE,
                true,
                someString,
                List.of(),
                CoinCreationReason.FOR_USER_ACTION,
                null,
                false,
                List.of(),
                null,
                true,
                promoKey,
                new ReasonParamExt(List.of(), null)
        );
    }

    @Nonnull
    public CoinError toCoinError() {
        return new CoinError(
                new IdObject(coinId),
                coinError
        );
    }

    public static final class Builder {

        private final long coinId;
        private final String promoKey;
        private String shopPromoId;
        private String anaplanId;
        private MarketLoyaltyError coinError;
        private final PromoType promoType = PromoType.SMART_SHOPPING;
        private Map<OfferItemKey, BigDecimal> itemDiscounts = new HashMap<>();
        private boolean unused;

        public Builder(long coinId, String promoKey) {
            this.coinId = coinId;
            this.promoKey = promoKey;
        }

        public Builder coinError(MarketLoyaltyError coinError) {
            this.coinError = coinError;
            return this;
        }

        public Builder shopPromoId(String shopPromoId) {
            this.shopPromoId = shopPromoId;
            return this;
        }

        public Builder anaplanId(String anaplanId) {
            this.anaplanId = anaplanId;
            return this;
        }

        public Builder discount(Map<OfferItemKey, BigDecimal> itemDiscounts) {
            this.itemDiscounts = itemDiscounts;
            return this;
        }

        public Builder discount(OfferItemKey item, BigDecimal discount) {
            this.itemDiscounts.put(item, discount);
            return this;
        }

        public Builder unused(boolean unused) {
            this.unused = unused;
            return this;
        }

        public CoinDiscountEntry build() {
            return new CoinDiscountEntry(coinId, promoKey, shopPromoId, anaplanId,
                    coinError,
                    promoType, itemDiscounts, unused);
        }
    }
}
