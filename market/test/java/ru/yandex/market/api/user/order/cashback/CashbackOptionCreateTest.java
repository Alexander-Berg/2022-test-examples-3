package ru.yandex.market.api.user.order.cashback;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.market.api.matchers.CashbackPromoMatcher;
import ru.yandex.market.api.matchers.CashbackThresholdMatcher;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOptions;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackPromoResponse;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackThreshold;
import ru.yandex.market.loyalty.api.model.CashbackPermision;
import ru.yandex.market.loyalty.api.model.CashbackRestrictionReason;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.perk.PerkType;

import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.matchers.CashbackOptionMatcher.amount;
import static ru.yandex.market.api.matchers.CashbackOptionMatcher.amountByPromoKey;
import static ru.yandex.market.api.matchers.CashbackOptionMatcher.cashbackOption;
import static ru.yandex.market.api.matchers.CashbackOptionMatcher.promoKey;
import static ru.yandex.market.api.matchers.CashbackOptionMatcher.promos;
import static ru.yandex.market.api.matchers.CashbackOptionMatcher.restrictionReason;
import static ru.yandex.market.api.matchers.CashbackOptionMatcher.thresholds;
import static ru.yandex.market.api.matchers.CashbackOptionMatcher.type;
import static ru.yandex.market.api.matchers.CashbackOptionMatcher.uiPromoFlags;
import static ru.yandex.market.api.matchers.CashbackOptionMatcher.version;

public class CashbackOptionCreateTest {

    @Test
    public void testCreateFromCheckouter() {
        CashbackOptions checkouterOptions = new CashbackOptions(
                "optionPromo",
                12,
                new BigDecimal(456),
                new HashMap<String, BigDecimal>(2) {
                    {
                        put("promo1", new BigDecimal(20));
                        put("promo2", new BigDecimal(30));
                    }
                },
                new ArrayList<CashbackPromoResponse>(2) {
                    {
                        add(new CashbackPromoResponse(new BigDecimal(100), "promoKey1", 1L, BigDecimal.TEN,
                                BigDecimal.ZERO, BigDecimal.ONE, MarketLoyaltyErrorCode.OTHER_ERROR,
                                "revertToken1", Collections.singletonList("promo flag1"), null, null,
                                null, null, null, 1, null,
                                null, null, null));
                        add(new CashbackPromoResponse(new BigDecimal(200), "promoKey2", 2L, BigDecimal.ONE,
                                BigDecimal.TEN, BigDecimal.ZERO, MarketLoyaltyErrorCode.REGION_MISMATCH_ERROR,
                                "revertToken2", Collections.singletonList("promo flag2"), null, null,
                                null, null, null, 1, null, null,
                                null, null));
                    }
                },
                CashbackPermision.ALLOWED,
                CashbackRestrictionReason.CASHBACK_DISABLED,
                new ArrayList<String>(2) {
                    {
                        add("uiPromoFlag1");
                        add("uiPromoFlag2");
                    }
                },
                new ArrayList<CashbackThreshold>(2) {
                    {
                        add(new CashbackThreshold(
                                "promoKey1",
                                Collections.singleton(PerkType.WELCOME_CASHBACK),
                                new BigDecimal(100),
                                new BigDecimal(200),
                                new BigDecimal(300),
                                1)
                        );
                        add(new CashbackThreshold(
                                "promoKey2",
                                Collections.singleton(PerkType.YANDEX_CASHBACK),
                                new BigDecimal(400),
                                new BigDecimal(500),
                                new BigDecimal(600),
                                2)
                        );
                    }
                },
                null
        );
        CashbackOption cashbackOption = CashbackOption.fromCheckouter(checkouterOptions);

        assertThat(cashbackOption, cashbackOption(
                promoKey("optionPromo"),
                version(12),
                amount(new BigDecimal(456)),
                amountByPromoKey(
                        cast(Matchers.hasEntry("promo1", new BigDecimal(20))),
                        cast(Matchers.hasEntry("promo2", new BigDecimal(30)))
                ),
                promos(cast(Matchers.containsInAnyOrder(
                        CashbackPromoMatcher.promos(
                                CashbackPromoMatcher.amount(new BigDecimal(100)),
                                CashbackPromoMatcher.promoKey("promoKey1"),
                                CashbackPromoMatcher.partnerId(1L),
                                CashbackPromoMatcher.nominal(BigDecimal.TEN),
                                CashbackPromoMatcher.marketTariff(BigDecimal.ZERO),
                                CashbackPromoMatcher.partnerTariff(BigDecimal.ONE),
                                CashbackPromoMatcher.error(MarketLoyaltyErrorCode.OTHER_ERROR),
                                CashbackPromoMatcher.revertToken("revertToken1"),
                                CashbackPromoMatcher.uiPromoFlags(cast(
                                        Matchers.contains("promo flag1")
                                ))
                        ),
                        CashbackPromoMatcher.promos(
                                CashbackPromoMatcher.amount(new BigDecimal(200)),
                                CashbackPromoMatcher.promoKey("promoKey2"),
                                CashbackPromoMatcher.partnerId(2L),
                                CashbackPromoMatcher.nominal(BigDecimal.ONE),
                                CashbackPromoMatcher.marketTariff(BigDecimal.TEN),
                                CashbackPromoMatcher.partnerTariff(BigDecimal.ZERO),
                                CashbackPromoMatcher.error(MarketLoyaltyErrorCode.REGION_MISMATCH_ERROR),
                                CashbackPromoMatcher.revertToken("revertToken2"),
                                CashbackPromoMatcher.uiPromoFlags(cast(
                                        Matchers.contains("promo flag2")
                                ))
                        )
                ))),
                type(CashbackPermission.ALLOWED),
                restrictionReason(ru.yandex.market.api.user.order.cashback.CashbackRestrictionReason.CASHBACK_DISABLED),
                uiPromoFlags(cast(Matchers.containsInAnyOrder(
                        "uiPromoFlag1",
                        "uiPromoFlag2"
                ))),
                thresholds(cast(Matchers.containsInAnyOrder(
                        CashbackThresholdMatcher.thresholds(
                                CashbackThresholdMatcher.promoKey("promoKey1"),
                                CashbackThresholdMatcher.requiredPerks(cast(Matchers.containsInAnyOrder(PerkType.WELCOME_CASHBACK))),
                                CashbackThresholdMatcher.remainingMultiCartTotal(new BigDecimal(100)),
                                CashbackThresholdMatcher.minMultiCartTotal(new BigDecimal(200)),
                                CashbackThresholdMatcher.amount(new BigDecimal(300)),
                                CashbackThresholdMatcher.agitationPriority(1)
                        ),
                        CashbackThresholdMatcher.thresholds(
                                CashbackThresholdMatcher.promoKey("promoKey2"),
                                CashbackThresholdMatcher.requiredPerks(cast(Matchers.containsInAnyOrder(PerkType.YANDEX_CASHBACK))),
                                CashbackThresholdMatcher.remainingMultiCartTotal(new BigDecimal(400)),
                                CashbackThresholdMatcher.minMultiCartTotal(new BigDecimal(500)),
                                CashbackThresholdMatcher.amount(new BigDecimal(600)),
                                CashbackThresholdMatcher.agitationPriority(2)
                        ))))
        ));
    }

    private static <In, Out> Matcher<Out> cast(Matcher<? extends In> matcher) {
        return (Matcher<Out>) matcher;
    }
}
