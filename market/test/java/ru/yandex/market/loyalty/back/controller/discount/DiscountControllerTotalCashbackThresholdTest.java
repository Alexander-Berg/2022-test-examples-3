package ru.yandex.market.loyalty.back.controller.discount;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.temporal.ChronoUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.CashbackPermision;
import ru.yandex.market.loyalty.api.model.CashbackRestrictionReason;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.back.controller.DiscountController;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.back.util.AntifraudUtils;
import ru.yandex.market.loyalty.core.dao.ydb.CashbackOrdersDao;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(DiscountController.class)
public class DiscountControllerTotalCashbackThresholdTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    public CashbackOrdersDao cashbackOrdersDao;
    @Autowired
    private AntifraudUtils antifraudUtils;

    @Test
    public void shouldRestrictCashbackIfTotalCashbackThresholdDisabled() {
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ONE));

        configureCashback();
        antifraudUtils.mockTotalCashbackThresholdExhausted(DEFAULT_UID,
                Date.from(clock.instant().minus(5,
                        ChronoUnit.DAYS)),
                Pair.of(PerkType.YANDEX_PLUS, true));


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(discountResponse.getCashback().getEmit().getType(), equalTo(CashbackPermision.RESTRICTED));
        assertThat(discountResponse.getCashback().getEmit().getRestrictionReason(),
                equalTo(CashbackRestrictionReason.CASHBACK_TOTAL_EXHAUSTED));
    }


    @Test
    public void shouldAllowCashbackIfTotalCashbackThresholdEnabled() {
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.ONE));

        configureCashback();
        antifraudUtils.mockTotalCashbackThresholdAvailable(DEFAULT_UID,
                Date.from(clock.instant().minus(5,
                        ChronoUnit.DAYS)),
                Pair.of(PerkType.YANDEX_PLUS, true));


        MultiCartWithBundlesDiscountResponse discountResponse = marketLoyaltyClient.calculateDiscount(
                DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderItem(itemKey(DEFAULT_ITEM_KEY), price(100))
                                .withPaymentType(PaymentType.BANK_CARD)
                                .build())
                        .build()
        );

        assertThat(discountResponse.getCashback().getEmit().getType(), equalTo(CashbackPermision.ALLOWED));
    }

    private void configureCashback() {
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.set(ConfigurationService.YANDEX_EXTRA_CASHBACK_ENABLED, true);
        cashbackCacheService.reloadCashbackPromos();
    }
}
