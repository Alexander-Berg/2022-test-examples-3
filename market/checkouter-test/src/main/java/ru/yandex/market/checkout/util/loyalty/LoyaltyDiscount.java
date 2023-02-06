package ru.yandex.market.checkout.util.loyalty;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.service.business.LoyaltyUtils;
import ru.yandex.market.loyalty.api.model.IdObject;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryDiscountWithPromoType;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryPromoResponse;

/**
 * Моделирует скидку лоялти.
 *
 * @author sergeykoles
 * Created on: 05.06.18
 */
public class LoyaltyDiscount {

    public static final BigDecimal TEST_ITEM_SUBSIDY_VALUE = BigDecimal.valueOf(99.99);
    public static final String PROMOCODE_PROMO_KEY = "some promo key";
    public static final String PROMOCODE = "PROMO_CODE";

    private BigDecimal discount;

    private PromoType promoType;
    /**
     * если null, то будет взят от OrderItem
     */
    private String promoKey;
    private String shopPromoId;
    private Long clientId;
    private String anaplanId;
    private Long coinId;
    private Boolean partnerPromo;
    private String promocode;
    private String sourceType;
    private BigDecimal extraCharge;

    private Map<PaymentType, DeliveryDiscountWithPromoType> deliveryDiscountByPaymentType;

    @Deprecated
    public LoyaltyDiscount() {
    }

    @Deprecated
    public LoyaltyDiscount(PromoType promoType) {
        this.discount = TEST_ITEM_SUBSIDY_VALUE;
        this.promoType = promoType;
    }

    @Deprecated
    public LoyaltyDiscount(BigDecimal discount, PromoType promoType) {
        this.discount = discount;
        this.promoType = promoType;
    }

    @Deprecated
    public LoyaltyDiscount(BigDecimal discount, PromoType promoType, String promoKey) {
        this.discount = discount;
        this.promoType = promoType;
        this.promoKey = promoKey;
    }

    @Deprecated
    public LoyaltyDiscount(Map<PaymentType, DeliveryDiscountWithPromoType> deliveryDiscountByPaymentType) {
        this.deliveryDiscountByPaymentType = deliveryDiscountByPaymentType;
    }

    public static LoyaltyDiscount createCoin(BigDecimal discount, String promoKey, long coinId) {
        LoyaltyDiscount loyaltyDiscount = new LoyaltyDiscount(discount, PromoType.MARKET_COIN);
        loyaltyDiscount.promoKey = promoKey;
        loyaltyDiscount.coinId = coinId;
        return loyaltyDiscount;
    }

    private static PromoType defaultPromoType(PromoType promoType) {
        return promoType != null ? promoType : PromoType.MARKET_COUPON;
    }

    public static LoyaltyDiscount discountFor(Number number, PromoType promoType) {
        return new LoyaltyDiscount(BigDecimal.valueOf(number.longValue()), promoType);
    }

    public static LoyaltyDiscountBuilder builder() {
        return new LoyaltyDiscountBuilder();
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public PromoType getPromoType() {
        return promoType;
    }

    public void setPromoType(PromoType promoType) {
        this.promoType = promoType;
    }

    public String getPromoKey() {
        return promoKey;
    }

    public void setPromoKey(String promoKey) {
        this.promoKey = promoKey;
    }

    public Long getCoinId() {
        return coinId;
    }

    public void setCoinId(Long coinId) {
        this.coinId = coinId;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Boolean getPartnerPromo() {
        return partnerPromo;
    }

    public LoyaltyDiscount setPartnerPromo(Boolean partnerPromo) {
        this.partnerPromo = partnerPromo;
        return this;
    }

    public String getPromocode() {
        return promocode;
    }

    public LoyaltyDiscount setPromocode(String promocode) {
        this.promocode = promocode;
        return this;
    }

    @SuppressWarnings("checkstyle:HiddenField")
    public ItemPromoResponse toItemPromoResponse() {
        PromoType promoType = defaultPromoType(getPromoType());
        return new ItemPromoResponse(
                defaultIfNullDiscount(),
                LoyaltyUtils.PromoTypeConverter.toLoyaltyPromoType(promoType),
                "loyaltyTokenForPromo" + getPromoKey() + promoType.getCode(),
                getPromoKey(),
                shopPromoId,
                clientId,
                anaplanId,
                Optional.ofNullable(getCoinId()).map(IdObject::new).orElse(null),
                partnerPromo,
                promocode,
                null,
                sourceType
        );
    }

    public DeliveryPromoResponse toDeliveryPromoResponse() {
        return toDeliveryPromoResponse(defaultIfNullDiscount());
    }

    @SuppressWarnings("checkstyle:HiddenField")
    public DeliveryPromoResponse toDeliveryPromoResponse(BigDecimal discount) {
        PromoType promoType = defaultPromoType(getPromoType());

        return DeliveryPromoResponse.Builder.builder()
                .setDiscount(discount)
                .setPromoType(LoyaltyUtils.PromoTypeConverter.toLoyaltyPromoType(promoType))
                .setDiscountToken("looyaltyTokenForPromo" + getPromoKey() + promoType.getCode())
                .setPromoKey(getPromoKey())
                .setUsedCoin(Optional.ofNullable(getCoinId()).map(IdObject::new).orElse(null))
                .setDiscountByPayment(deliveryDiscountByPaymentType)
                .setExtraCharge(extraCharge)
                .build();
    }

    private BigDecimal defaultIfNullDiscount() {
        return getDiscount() != null ? getDiscount() : TEST_ITEM_SUBSIDY_VALUE;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LoyaltyDiscount{");
        sb.append("discount=").append(discount);
        sb.append(", promoType=").append(promoType);
        sb.append(", promoKey='").append(promoKey).append('\'');
        sb.append(", coinId=").append(coinId);
        sb.append(", deliveryDiscountByPaymentType=").append(deliveryDiscountByPaymentType);
        sb.append(", extraCharge=").append(extraCharge);
        sb.append('}');
        return sb.toString();
    }

    public Map<PaymentType, DeliveryDiscountWithPromoType> getDeliveryDiscountByPaymentType() {
        return deliveryDiscountByPaymentType;
    }

    public void setDeliveryDiscountByPaymentType(
            Map<PaymentType, DeliveryDiscountWithPromoType> deliveryDiscountByPaymentType) {
        this.deliveryDiscountByPaymentType = deliveryDiscountByPaymentType;
    }

    public static final class LoyaltyDiscountBuilder {

        private BigDecimal discount;
        private PromoType promoType;
        private String promoKey;
        private String shopPromoId;
        private Long clientId;
        private String anaplanId;
        private Long coinId;
        private String promocode;
        private String sourceType;

        private Map<PaymentType, DeliveryDiscountWithPromoType> deliveryDiscountByPaymentType;
        private BigDecimal extraCharge;

        private LoyaltyDiscountBuilder() {
        }

        public LoyaltyDiscountBuilder discount(BigDecimal discount) {
            this.discount = discount;
            return this;
        }

        public LoyaltyDiscountBuilder promoType(PromoType promoType) {
            this.promoType = promoType;
            return this;
        }

        public LoyaltyDiscountBuilder promoKey(String promoKey) {
            this.promoKey = promoKey;
            return this;
        }

        public LoyaltyDiscountBuilder shopPromoId(String shopPromoId) {
            this.shopPromoId = shopPromoId;
            return this;
        }

        public LoyaltyDiscountBuilder clientId(Long clientId) {
            this.clientId = clientId;
            return this;
        }

        public LoyaltyDiscountBuilder anaplanId(String anaplanId) {
            this.anaplanId = anaplanId;
            return this;
        }

        public LoyaltyDiscountBuilder coinId(Long coinId) {
            this.coinId = coinId;
            return this;
        }

        public LoyaltyDiscountBuilder promocode(String promocode) {
            this.promocode = promocode;
            return this;
        }

        public LoyaltyDiscountBuilder sourceType(String sourceType) {
            this.sourceType = sourceType;
            return this;
        }

        public LoyaltyDiscountBuilder deliveryDiscountByPaymentType(
                Map<PaymentType, DeliveryDiscountWithPromoType> deliveryDiscountByPaymentType) {
            this.deliveryDiscountByPaymentType = deliveryDiscountByPaymentType;
            return this;
        }

        public LoyaltyDiscountBuilder extraCharge(BigDecimal extraCharge) {
            this.extraCharge = extraCharge;
            return this;
        }

        public LoyaltyDiscount build() {
            LoyaltyDiscount loyaltyDiscount = new LoyaltyDiscount();
            loyaltyDiscount.setDiscount(discount);
            loyaltyDiscount.setPromoType(promoType);
            loyaltyDiscount.setPromoKey(promoKey);
            loyaltyDiscount.setCoinId(coinId);
            loyaltyDiscount.setClientId(clientId);
            loyaltyDiscount.setDeliveryDiscountByPaymentType(deliveryDiscountByPaymentType);
            loyaltyDiscount.setPromocode(promocode);
            loyaltyDiscount.shopPromoId = this.shopPromoId;
            loyaltyDiscount.anaplanId = this.anaplanId;
            loyaltyDiscount.sourceType = this.sourceType;
            loyaltyDiscount.extraCharge = this.extraCharge;
            return loyaltyDiscount;
        }
    }
}
