package ru.yandex.market.loyalty.back.util;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ru.yandex.market.loyalty.api.model.CashbackPermision;
import ru.yandex.market.loyalty.api.model.CashbackRestrictionReason;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.loyalty.api.model.CashbackRestrictionReason.NOT_SUITABLE_CATEGORY;
import static ru.yandex.market.loyalty.api.model.CashbackRestrictionReason.NOT_SUITABLE_DELIVERY_TYPE;
import static ru.yandex.market.loyalty.api.model.CashbackRestrictionReason.NOT_YA_PLUS_SUBSCRIBER;
import static ru.yandex.market.loyalty.api.model.CashbackRestrictionReason.NO_SUITABLE_PROMO;
import static ru.yandex.market.loyalty.api.model.PaymentType.APPLE_PAY;
import static ru.yandex.market.loyalty.api.model.PaymentType.BANK_CARD;
import static ru.yandex.market.loyalty.api.model.PaymentType.CARD_ON_DELIVERY;
import static ru.yandex.market.loyalty.api.model.PaymentType.CASH_ON_DELIVERY;
import static ru.yandex.market.loyalty.api.model.PaymentType.CREDIT;
import static ru.yandex.market.loyalty.api.model.PaymentType.EXTERNAL_CERTIFICATE;
import static ru.yandex.market.loyalty.api.model.PaymentType.GOOGLE_PAY;
import static ru.yandex.market.loyalty.api.model.PaymentType.INSTALLMENT;
import static ru.yandex.market.loyalty.api.model.PaymentType.SBP;
import static ru.yandex.market.loyalty.api.model.PaymentType.SHOP_PREPAID;
import static ru.yandex.market.loyalty.api.model.PaymentType.YANDEX;
import static ru.yandex.market.loyalty.api.model.PaymentType.YANDEX_MONEY;

public class CashbackMatchers {
    @NotNull
    public static Matcher<MultiCartWithBundlesDiscountResponse> allowedCashback(
            BigDecimal totalEmit, BigDecimal totalSpend
    ) {
        ImmutableList.Builder<Matcher<? super Object>> builder = ImmutableList.builder();

        if (totalEmit == null && totalSpend == null) {
            throw new IllegalArgumentException();
        }

        if (totalEmit != null) {
            builder.add(hasProperty(
                    "emit", allOf(
                            hasProperty(
                                    "type",
                                    equalTo(CashbackPermision.ALLOWED)
                            ),
                            hasProperty("amount", comparesEqualTo(totalEmit))
                    )));
        }
        if (totalSpend != null) {
            builder.add(hasProperty(
                    "spend", allOf(
                            hasProperty(
                                    "type",
                                    equalTo(CashbackPermision.ALLOWED)
                            ),
                            hasProperty("amount", comparesEqualTo(totalSpend))
                    )));
        }
        return hasProperty(
                "cashback",
                allOf(
                        builder.build()
                )
        );
    }

    @NotNull
    public static Matcher<Object> cashback(@Nullable Matcher<Object> emitCashback,
                                           @Nullable Matcher<Object> spendCashback) {
        ImmutableList.Builder<Matcher<? super Object>> builder = ImmutableList.builder();

        if (emitCashback == null && spendCashback == null) {
            throw new IllegalArgumentException();
        }

        if (emitCashback != null) {
            builder.add(emitCashback);
        }
        if (spendCashback != null) {
            builder.add(spendCashback);
        }

        return hasProperty(
                "cashback",
                allOf(builder.build())
        );
    }

    @NotNull
    public static Matcher<Object> noPromoEmitCashback() {
        return restrictedEmitCashback(NO_SUITABLE_PROMO);
    }

    @NotNull
    public static Matcher<Object> notSuitableDeliveryTypeEmitCashback() {
        return restrictedEmitCashback(NOT_SUITABLE_DELIVERY_TYPE);
    }

    @NotNull
    public static Matcher<Object> notSuitableDeliveryTypeSpendCashback() {
        return restrictedSpendCashback(NOT_SUITABLE_DELIVERY_TYPE);
    }

    @NotNull
    public static Matcher<Object> notSuitableCategoryTypeEmitCashback() {
        return restrictedEmitCashback(NOT_SUITABLE_CATEGORY);
    }

    @NotNull
    public static Matcher<Object> notSuitableCategoryTypeSpendCashback() {
        return restrictedSpendCashback(NOT_SUITABLE_CATEGORY);
    }

    @NotNull
    public static Matcher<Object> restrictedEmitCashback(CashbackRestrictionReason restriction) {
        return hasProperty(
                "emit", allOf(
                        hasProperty(
                                "type",
                                equalTo(CashbackPermision.RESTRICTED)
                        ),
                        hasProperty(
                                "restrictionReason",
                                equalTo(restriction)
                        )
                ));
    }

    @NotNull
    public static Matcher<Object> restrictedSpendCashback(CashbackRestrictionReason restriction) {
        return hasProperty(
                "spend", allOf(
                        hasProperty(
                                "type",
                                equalTo(CashbackPermision.RESTRICTED)
                        ),
                        hasProperty(
                                "restrictionReason",
                                equalTo(restriction)
                        )
                ));
    }

    @NotNull
    public static Matcher<Object> allowedEmitCashback(BigDecimal amount) {
        return hasProperty(
                "emit", allOf(
                        hasProperty(
                                "type",
                                equalTo(CashbackPermision.ALLOWED)
                        ),
                        hasProperty(
                                "amount",
                                comparesEqualTo(amount)
                        )
                ));
    }

    @NotNull
    public static Matcher<Object> allowedEmitCashback(BigDecimal amount, Matcher<Iterable<?>> promosMatcher) {
        return hasProperty(
                "emit", allOf(
                        hasProperty(
                                "type",
                                equalTo(CashbackPermision.ALLOWED)
                        ),
                        hasProperty(
                                "amount",
                                comparesEqualTo(amount)
                        ),
                        hasProperty(
                                "promos",
                                promosMatcher
                        )
                ));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @NotNull
    public static Matcher<Object> allowedEmitCashback(BigDecimal amount, Matcher<Iterable<?>> promosMatcher,
                                                      Matcher<Iterable<?>> thresholdMatcher) {
        ImmutableList.Builder matchers = ImmutableList.builder();
        matchers.add(hasProperty(
                "type",
                equalTo(CashbackPermision.ALLOWED)
        ));
        matchers.add(hasProperty(
                "amount",
                comparesEqualTo(amount)
        ));
        if (promosMatcher != null) {
            matchers.add(hasProperty(
                    "promos",
                    promosMatcher
            ));
        }
        matchers.add(hasProperty(
                "thresholds",
                thresholdMatcher
        ));

        return hasProperty(
                "emit", allOf(matchers.build())
        );
    }

    @NotNull
    public static Matcher<Object> allowedWithUiFlags(String... flags) {
        return hasProperty(
                "emit", allOf(
                        hasProperty(
                                "uiPromoFlags",
                                containsInAnyOrder(flags)
                        )
                ));
    }

    @NotNull
    public static Matcher<Object> promoKeyIsNotNull() {
        return hasProperty("promoKey");
    }

    @NotNull
    public static Matcher<Object> allowedSpendCashback(BigDecimal amount) {
        return hasProperty(
                "spend", allOf(
                        hasProperty(
                                "type",
                                equalTo(CashbackPermision.ALLOWED)
                        ),
                        hasProperty(
                                "amount",
                                comparesEqualTo(amount)
                        )
                ));
    }

    @NotNull
    public static Matcher<MultiCartWithBundlesDiscountResponse> notYandexPlusSubscriberCashback() {
        return restrictedCashback(NOT_YA_PLUS_SUBSCRIBER);
    }

    @NotNull
    public static Matcher<MultiCartWithBundlesDiscountResponse> restrictedCashback(CashbackRestrictionReason restriction) {
        return hasProperty(
                "cashback",
                allOf(
                        restrictedEmitCashback(restriction),
                        restrictedSpendCashback(restriction)
                )
        );
    }

    public static Matcher<Iterable<? extends PaymentType>> cashbackPaymentTypes() {
        return containsInAnyOrder(
                BANK_CARD,
                YANDEX,
                YANDEX_MONEY,
                APPLE_PAY,
                GOOGLE_PAY,
                SBP
        );
    }

    public static Matcher<Iterable<? extends PaymentType>> nonCashbackPaymentTypes() {
        return containsInAnyOrder(
                SHOP_PREPAID,
                CASH_ON_DELIVERY,
                CARD_ON_DELIVERY,
                EXTERNAL_CERTIFICATE,
                CREDIT,
                INSTALLMENT
        );
    }

    public static Matcher<Iterable<? extends PaymentType>> allPaymentTypes() {
        return containsInAnyOrder(
                BANK_CARD,
                YANDEX,
                YANDEX_MONEY,
                APPLE_PAY,
                GOOGLE_PAY,
                SHOP_PREPAID,
                CASH_ON_DELIVERY,
                CARD_ON_DELIVERY,
                EXTERNAL_CERTIFICATE,
                CREDIT,
                INSTALLMENT,
                SBP
        );
    }

    public static Matcher<Iterable<?>> successCashbackPromo(BigDecimal amount, String promoKey) {
        return containsInAnyOrder(allOf(
                hasProperty("amount", comparesEqualTo(amount)),
                hasProperty("promoKey", equalTo(promoKey)),
                hasProperty("error", is(nullValue())),
                hasProperty("revertToken", is(nullValue()))
        ));
    }

    public static Matcher<Iterable<?>> failCashbackPromo(BigDecimal amount, String promoKey) {
        return containsInAnyOrder(allOf(
                hasProperty("amount", comparesEqualTo(amount)),
                hasProperty("promoKey", equalTo(promoKey)),
                hasProperty("error", is(nullValue())),
                hasProperty("revertToken", is(nullValue()))
        ));
    }
}
