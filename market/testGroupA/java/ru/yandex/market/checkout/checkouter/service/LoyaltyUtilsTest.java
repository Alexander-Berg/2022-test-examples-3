package ru.yandex.market.checkout.checkouter.service;


import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.cashback.model.CashbackMergeOption;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.service.business.LoyaltyUtils;
import ru.yandex.market.loyalty.api.model.PaymentType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class LoyaltyUtilsTest {

    @Test
    public void shouldConvertAllLoyaltyPaymentTypes() {
        for (PaymentType paymentType : PaymentType.values()) {
            if (paymentType.equals(PaymentType.UNKNOWN)) {
                continue;
            }
            PaymentMethod paymentMethod = LoyaltyUtils.PaymentTypeConverter.fromLoyaltyPaymentType(paymentType);
            Assertions.assertNotEquals(paymentMethod, PaymentMethod.UNKNOWN);
        }
    }

    @Test
    public void shouldConvertAllPaymentMethodsToLoyalty() {
        for (PaymentMethod paymentMethod : PaymentMethod.values()) {
            if (paymentMethod.equals(PaymentMethod.UNKNOWN)) {
                continue;
            }
            PaymentType paymentType = LoyaltyUtils.PaymentTypeConverter.toLoyaltyPaymentType(paymentMethod);
            Assertions.assertNotEquals(paymentType, PaymentType.UNKNOWN);
        }
    }

    @Test
    public void shouldConvertPromoTypesToLoyaltyPromoType() {
        final var exclusions = Set.of(
                PromoType.UNKNOWN,
                PromoType.DIRECT_DISCOUNT,
                PromoType.MARKET_DEAL,
                PromoType.MARKET_PRIME,
                PromoType.MARKET_BLUE,
                PromoType.PRICE_DROP_AS_YOU_SHOP,
                PromoType.CASHBACK,
                PromoType.SUPPLIER_MULTICART_DISCOUNT,
                PromoType.SECRET_SALE);
        for (var promoType : PromoType.values()) {
            if (exclusions.contains(promoType)) {
                continue;
            }
            final ru.yandex.market.loyalty.api.model.PromoType loyaltyPromoType =
                    LoyaltyUtils.PromoTypeConverter.toLoyaltyPromoType(promoType);
            assertNotNull(loyaltyPromoType, promoType.toString());
            assertThat(loyaltyPromoType.getCode(), not(blankOrNullString()));
        }
    }

    @Test
    public void shouldConvertMergeOptionToLoyaltyMergeOption() {
        Map<CashbackMergeOption, ru.yandex.market.loyalty.api.model.cashback.details.CashbackMergeOption>
                mergeOptionMap = Map.of(
                CashbackMergeOption.FULL,
                ru.yandex.market.loyalty.api.model.cashback.details.CashbackMergeOption.FULL,
                CashbackMergeOption.MERGED,
                ru.yandex.market.loyalty.api.model.cashback.details.CashbackMergeOption.MERGED,
                CashbackMergeOption.SEPARATE,
                ru.yandex.market.loyalty.api.model.cashback.details.CashbackMergeOption.SEPARATE
        );
        for (CashbackMergeOption mergeOption : mergeOptionMap.keySet()) {
            assertSame(LoyaltyUtils.toLoyaltyCashbackMergeOption(mergeOption), mergeOptionMap.get(mergeOption));
        }
    }
}
