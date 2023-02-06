package ru.yandex.market.checkout.checkouter.cashback;


import java.math.BigDecimal;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.cashback.model.CashbackProfile;
import ru.yandex.market.loyalty.api.model.CashbackOptionsPrecondition;
import ru.yandex.market.loyalty.api.model.CashbackOptionsResponse;
import ru.yandex.market.loyalty.api.model.CashbackPermision;
import ru.yandex.market.loyalty.api.model.CashbackPromoResponse;
import ru.yandex.market.loyalty.api.model.CashbackRestrictionReason;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.checkout.providers.CashbackTestProvider.CART_LABEL;
import static ru.yandex.market.checkout.providers.CashbackTestProvider.CASHBACK_THRESHOLD_AMOUNT;
import static ru.yandex.market.checkout.providers.CashbackTestProvider.FEED;
import static ru.yandex.market.checkout.providers.CashbackTestProvider.OFFER;
import static ru.yandex.market.checkout.providers.CashbackTestProvider.singleItemCashbackResponse;
import static ru.yandex.market.checkout.providers.CashbackTestProvider.singleItemCashbackResponseWithThresholds;
import static ru.yandex.market.checkout.providers.CashbackTestProvider.singleItemCashbackResponseWithUiParams;

public class CashbackResponseConverterTest extends CashbackTestBase {

    private static final String PROMO_KEY = "promo key";

    @Autowired
    private CashbackResponseConverter converter;

    @Test
    void shouldConvertLoyaltyResponseToDomainModel() {
        CashbackOptionsResponse cashbackOptionsResponse = singleItemCashbackResponse();

        List<CashbackProfile> cashbackProfiles = converter.convertFrom(cashbackOptionsResponse);

        Matcher<Object> cashbackAllowMatcher = allOf(
                hasProperty("emit", allOf(
                        hasProperty("amount", is(BigDecimal.valueOf(50L))),
                        hasProperty("type", is(CashbackPermision.ALLOWED)),
                        hasProperty("uiPromoFlags", nullValue()))),
                hasProperty("spend", allOf(
                        hasProperty("amount", is(BigDecimal.valueOf(300L))),
                        hasProperty("uiPromoFlags", nullValue()),
                        hasProperty("type", is(CashbackPermision.ALLOWED)))));
        Matcher<Object> cashbackRestrictMatcher = allOf(
                hasProperty("emit", nullValue()),
                hasProperty("spend", allOf(
                        hasProperty("type", is(CashbackPermision.RESTRICTED)),
                        hasProperty("uiPromoFlags", nullValue()),
                        hasProperty("restrictionReason",
                                is(CashbackRestrictionReason.NOT_SUITABLE_PAYMENT_TYPE)))));

        assertThat(cashbackProfiles, hasSize(2));
        assertThat(cashbackProfiles, hasItems(
                allOf(
                        hasProperty("cashbackTypes", containsInAnyOrder(CashbackType.EMIT, CashbackType.SPEND)),
                        hasProperty("cashbackOptionsPreconditions", contains(CashbackOptionsPrecondition.PAYMENT)),
                        hasProperty("payment", hasProperty("types",
                                containsInAnyOrder(PaymentType.APPLE_PAY, PaymentType.BANK_CARD, PaymentType.GOOGLE_PAY,
                                        PaymentType.YANDEX, PaymentType.YANDEX_MONEY))),
                        hasProperty("delivery", hasProperty("types",
                                contains(DeliveryType.COURIER, DeliveryType.PICKUP))),
                        hasProperty("cashback", cashbackAllowMatcher),
                        hasProperty("orders", hasItem(allOf(
                                hasProperty("cashback", cashbackAllowMatcher),
                                hasProperty("cartId", is(CART_LABEL)),
                                hasProperty("orderId", nullValue()),
                                hasProperty("items", hasItem(allOf(
                                        hasProperty("offerId", is(OFFER)),
                                        hasProperty("feedId", is(FEED)),
                                        hasProperty("cartId", is(CART_LABEL)),
                                        hasProperty("bundleId", nullValue()),
                                        hasProperty("cashback", cashbackAllowMatcher)
                                ))))))),
                allOf(
                        hasProperty("cashbackTypes", contains(CashbackType.SPEND)),
                        hasProperty("cashbackOptionsPreconditions", contains(CashbackOptionsPrecondition.PAYMENT)),
                        hasProperty("payment", hasProperty("types",
                                containsInAnyOrder(PaymentType.SHOP_PREPAID, PaymentType.CASH_ON_DELIVERY,
                                        PaymentType.CARD_ON_DELIVERY, PaymentType.EXTERNAL_CERTIFICATE,
                                        PaymentType.CREDIT, PaymentType.INSTALLMENT))),
                        hasProperty("delivery", nullValue()),
                        hasProperty("cashback", cashbackRestrictMatcher),
                        hasProperty("orders", hasItem(allOf(
                                hasProperty("cashback", cashbackRestrictMatcher),
                                hasProperty("cartId", is(CART_LABEL)),
                                hasProperty("orderId", nullValue()),
                                hasProperty("items", hasItem(allOf(
                                        hasProperty("offerId", is(OFFER)),
                                        hasProperty("feedId", is(FEED)),
                                        hasProperty("cartId", is(CART_LABEL)),
                                        hasProperty("bundleId", nullValue()),
                                        hasProperty("cashback", cashbackRestrictMatcher)
                                )))))))));
    }

    @Test
    void shouldConvertLoyaltyResponseToDomainModelWithUIParams() {
        CashbackOptionsResponse cashbackOptionsResponseWithUiParams = singleItemCashbackResponseWithUiParams();

        List<CashbackProfile> cashbackProfiles = converter.convertFrom(cashbackOptionsResponseWithUiParams);
        Matcher<Object> cashbackAllowMatcher = allOf(
                hasProperty("emit", allOf(
                        hasProperty("amount", is(BigDecimal.valueOf(50L))),
                        hasProperty("type", is(CashbackPermision.ALLOWED)),
                        hasProperty("uiPromoFlags", hasSize(1)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("uiPromoFlags", hasSize(1)),
                                        hasProperty("amount", is(new BigDecimal("50"))),
                                        hasProperty("promoKey", is("promo key"))
                                ))))),
                hasProperty("spend", allOf(
                        hasProperty("amount", is(BigDecimal.valueOf(300L))),
                        hasProperty("uiPromoFlags", hasSize(1)),
                        hasProperty("uiPromoFlags", hasSize(1)),
                        hasProperty("promos", hasSize(1)),
                        hasProperty("promos", hasItems(
                                allOf(
                                        hasProperty("uiPromoFlags", hasSize(1)),
                                        hasProperty("amount", is(new BigDecimal("300"))),
                                        hasProperty("promoKey", is("promo key"))
                                ))),
                        hasProperty("type", is(CashbackPermision.ALLOWED)))));

        assertThat(cashbackProfiles, hasSize(1));
        assertThat(cashbackProfiles, hasItems(
                allOf(
                        hasProperty("cashbackTypes", containsInAnyOrder(CashbackType.EMIT, CashbackType.SPEND)),
                        hasProperty("cashbackOptionsPreconditions", contains(CashbackOptionsPrecondition.PAYMENT)),
                        hasProperty("payment", hasProperty("types",
                                containsInAnyOrder(PaymentType.APPLE_PAY, PaymentType.BANK_CARD, PaymentType.GOOGLE_PAY,
                                        PaymentType.YANDEX, PaymentType.YANDEX_MONEY))),
                        hasProperty("delivery", hasProperty("types",
                                contains(DeliveryType.COURIER, DeliveryType.PICKUP))),
                        hasProperty("cashback", cashbackAllowMatcher),
                        hasProperty("orders", hasItem(allOf(
                                hasProperty("cashback", cashbackAllowMatcher),
                                hasProperty("cartId", is(CART_LABEL)),
                                hasProperty("orderId", nullValue()),
                                hasProperty("items", hasItem(allOf(
                                        hasProperty("offerId", is(OFFER)),
                                        hasProperty("feedId", is(FEED)),
                                        hasProperty("cartId", is(CART_LABEL)),
                                        hasProperty("bundleId", nullValue()),
                                        hasProperty("cashback", cashbackAllowMatcher)
                                )))))))
        ));
    }

    @Test
    void shouldReturnValidPercents() {
        checkouterProperties.setEnableValidationPercentsFromLoyalty(true);
        CashbackPromoResponse invalidCashbackPromo1 = CashbackPromoResponse.builder()
                .setMarketTariff(BigDecimal.valueOf(1050))
                .setPartnerTariff(BigDecimal.TEN)
                .build();
        CashbackPromoResponse invalidCashbackPromo2 = CashbackPromoResponse.builder()
                .setMarketTariff(BigDecimal.TEN)
                .setPartnerTariff(BigDecimal.valueOf(1000))
                .build();
        CashbackPromoResponse invalidCashbackPromo3 = CashbackPromoResponse.builder()
                .setPartnerTariff(BigDecimal.valueOf(50))
                .build();
        CashbackPromoResponse validCashbackPromo1 = CashbackPromoResponse.builder()
                .setMarketTariff(BigDecimal.TEN)
                .setPartnerTariff(BigDecimal.valueOf(150))
                .build();
        CashbackPromoResponse validCashbackPromo2 = CashbackPromoResponse.builder()
                .setMarketTariff(BigDecimal.TEN)
                .setPartnerTariff(BigDecimal.ZERO)
                .build();
        List<CashbackPromoResponse> cashbackPromos = List.of(
                invalidCashbackPromo1, invalidCashbackPromo2, invalidCashbackPromo3,
                validCashbackPromo1, validCashbackPromo2
        );
        List<CashbackPromoResponse> validatedCashbackPromos = converter.filterPromosByInvalidPercents(cashbackPromos);
        assertThat(validatedCashbackPromos, hasSize(2));
        assertThat(validatedCashbackPromos, hasItems(validCashbackPromo1, validCashbackPromo2));
        assertThat(validatedCashbackPromos, not(hasItems(invalidCashbackPromo1, invalidCashbackPromo2,
                invalidCashbackPromo3)));
    }

    @Test
    void shouldConvertLoyaltyResponseToDomainModelWithThresholds() {
        var cashbackOptionsResponse = singleItemCashbackResponseWithThresholds();

        var cashbackProfiles = converter.convertFrom(cashbackOptionsResponse);

        Matcher<Object> cashbackAllowMatcher = allOf(
                hasProperty("emit", allOf(
                        hasProperty("amount", is(BigDecimal.valueOf(50L))),
                        hasProperty("type", is(CashbackPermision.ALLOWED)),
                        hasProperty("uiPromoFlags", nullValue()),
                        hasProperty("thresholds", hasItem(allOf(
                                hasProperty("promoKey", is(PROMO_KEY)),
                                hasProperty("requiredPerks", nullValue()),
                                hasProperty("remainingMultiCartTotal", is(BigDecimal.ZERO)),
                                hasProperty("minMultiCartTotal", is(BigDecimal.TEN)),
                                hasProperty("amount", is(BigDecimal.TEN))
                        ))))),
                hasProperty("spend", allOf(
                        hasProperty("amount", is(BigDecimal.valueOf(300L))),
                        hasProperty("uiPromoFlags", nullValue()),
                        hasProperty("type", is(CashbackPermision.ALLOWED)),
                        hasProperty("thresholds", hasItem(allOf(
                                hasProperty("promoKey", is(PROMO_KEY)),
                                hasProperty("requiredPerks", nullValue()),
                                hasProperty("remainingMultiCartTotal", is(BigDecimal.ZERO)),
                                hasProperty("minMultiCartTotal", is(BigDecimal.TEN)),
                                hasProperty("amount", is(BigDecimal.TEN))
                        ))))));

        Matcher<Object> cashbackRestrictMatcher = allOf(
                hasProperty("emit", nullValue()),
                hasProperty("spend", allOf(
                        hasProperty("type", is(CashbackPermision.RESTRICTED)),
                        hasProperty("uiPromoFlags", nullValue()),
                        hasProperty("thresholds", hasItem(allOf(
                                hasProperty("promoKey", is(PROMO_KEY)),
                                hasProperty("requiredPerks", nullValue()),
                                hasProperty("remainingMultiCartTotal", is(BigDecimal.TEN)),
                                hasProperty("minMultiCartTotal", is(BigDecimal.TEN)),
                                hasProperty("amount", is(CASHBACK_THRESHOLD_AMOUNT))
                        ))))));

        assertThat(cashbackProfiles, hasSize(2));
        assertThat(cashbackProfiles, hasItems(
                allOf(
                        hasProperty("cashbackTypes", containsInAnyOrder(CashbackType.EMIT, CashbackType.SPEND)),
                        hasProperty("cashbackOptionsPreconditions", contains(CashbackOptionsPrecondition.PAYMENT)),
                        hasProperty("payment", hasProperty("types",
                                containsInAnyOrder(PaymentType.APPLE_PAY, PaymentType.BANK_CARD, PaymentType.GOOGLE_PAY,
                                        PaymentType.YANDEX, PaymentType.YANDEX_MONEY))),
                        hasProperty("delivery", hasProperty("types",
                                contains(DeliveryType.COURIER, DeliveryType.PICKUP))),
                        hasProperty("cashback", cashbackAllowMatcher),
                        hasProperty("orders", hasItem(allOf(
                                hasProperty("cashback", cashbackAllowMatcher),
                                hasProperty("cartId", is(CART_LABEL)),
                                hasProperty("orderId", nullValue()),
                                hasProperty("items", hasItem(allOf(
                                        hasProperty("offerId", is(OFFER)),
                                        hasProperty("feedId", is(FEED)),
                                        hasProperty("cartId", is(CART_LABEL)),
                                        hasProperty("bundleId", nullValue()),
                                        hasProperty("cashback", cashbackAllowMatcher)
                                ))))))),
                allOf(
                        hasProperty("cashbackTypes", contains(CashbackType.SPEND)),
                        hasProperty("cashbackOptionsPreconditions", contains(CashbackOptionsPrecondition.PAYMENT)),
                        hasProperty("payment", hasProperty("types",
                                containsInAnyOrder(PaymentType.SHOP_PREPAID, PaymentType.CASH_ON_DELIVERY,
                                        PaymentType.CARD_ON_DELIVERY, PaymentType.EXTERNAL_CERTIFICATE,
                                        PaymentType.CREDIT, PaymentType.INSTALLMENT))),
                        hasProperty("delivery", nullValue()),
                        hasProperty("cashback", cashbackRestrictMatcher),
                        hasProperty("orders", hasItem(allOf(
                                hasProperty("cashback", cashbackRestrictMatcher),
                                hasProperty("cartId", is(CART_LABEL)),
                                hasProperty("orderId", nullValue()),
                                hasProperty("items", hasItem(allOf(
                                        hasProperty("offerId", is(OFFER)),
                                        hasProperty("feedId", is(FEED)),
                                        hasProperty("cartId", is(CART_LABEL)),
                                        hasProperty("bundleId", nullValue()),
                                        hasProperty("cashback", cashbackRestrictMatcher)
                                )))))))));
    }
}
