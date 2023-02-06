package ru.yandex.market.checkout.util.loyalty.model;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.loyalty.api.model.CouponError;
import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.promocode.MarketLoyaltyPromocodeWarningCode;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeActivationResultCode;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeError;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeWarning;

public class PromocodeDiscountEntry {

    private final String promocode;
    private final String promoKey;
    private final String shopPromoId;
    private final String anaplanId;
    @Nullable
    private final Long clientId;
    private final PromocodeActivationResultCode activationResultCode;
    private final PromocodeError promocodeError;
    private final PromocodeWarning promocodeWarning;
    private final PromoType promoType;
    private final Map<OfferItemKey, BigDecimal> itemDiscounts;
    private final boolean unused;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public PromocodeDiscountEntry(String promocode, String promoKey, String shopPromoId,
                                  Long clientId, String anaplanId,
                                  PromocodeActivationResultCode activationResultCode,
                                  PromocodeError promocodeError, PromocodeWarning promocodeWarning,
                                  PromoType promoType, Map<OfferItemKey, BigDecimal> itemDiscounts,
                                  boolean unused) {
        this.promocode = promocode;
        this.promoKey = promoKey;
        this.shopPromoId = shopPromoId;
        this.anaplanId = anaplanId;
        this.clientId = clientId;
        this.activationResultCode = activationResultCode;
        this.promocodeError = promocodeError;
        this.promocodeWarning = promocodeWarning;
        this.promoType = promoType;
        this.unused = unused;
        this.itemDiscounts = itemDiscounts;
    }

    @Nonnull
    public static Builder promocode(@Nonnull String promocode, @Nonnull String promoKey) {
        return new Builder(promocode, promoKey);
    }

    @Nonnull
    public String getPromocode() {
        return promocode;
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
    public Long getClientId() {
        return clientId;
    }

    @Nonnull
    public PromocodeActivationResultCode getActivationResultCode() {
        return activationResultCode;
    }

    @Nullable
    public PromocodeError getPromocodeError() {
        return promocodeError;
    }

    @Nullable
    public PromocodeWarning getPromocodeWarning() {
        return promocodeWarning;
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

    public static final class Builder {

        private final String promocode;
        private final String promoKey;
        private String shopPromoId;
        private String anaplanId;
        private Long clientId;
        private PromocodeActivationResultCode activationResultCode = PromocodeActivationResultCode.SUCCESS;
        private PromocodeError promocodeError;
        private PromocodeWarning promocodeWarning;
        private final PromoType promoType = PromoType.MARKET_PROMOCODE;
        private Map<OfferItemKey, BigDecimal> itemDiscounts = new HashMap<>();
        private boolean unused;

        public Builder(String promocode, String promoKey) {
            this.promocode = promocode;
            this.promoKey = promoKey;
        }

        public Builder activationResultCode(PromocodeActivationResultCode activationResultCode) {
            this.activationResultCode = activationResultCode;
            return this;
        }

        public Builder promocodeError(CouponError promocodeError) {
            this.promocodeError = new PromocodeError(promocode, promocodeError);
            return this;
        }

        public Builder promocodeWarning(MarketLoyaltyPromocodeWarningCode warningCode,
                                        String message) {
            this.promocodeWarning = new PromocodeWarning(promocode, warningCode, message);
            return this;
        }

        public Builder shopPromoId(String shopPromoId) {
            this.shopPromoId = shopPromoId;
            return this;
        }

        public Builder clientId(Long clientId) {
            this.clientId = clientId;
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

        public Builder unused(boolean unused) {
            this.unused = unused;
            return this;
        }

        public PromocodeDiscountEntry build() {
            return new PromocodeDiscountEntry(promocode, promoKey, shopPromoId, clientId, anaplanId,
                    activationResultCode, promocodeError, promocodeWarning,
                    promoType, itemDiscounts, unused);
        }
    }
}
