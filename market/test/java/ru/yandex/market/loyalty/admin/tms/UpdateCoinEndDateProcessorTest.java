package ru.yandex.market.loyalty.admin.tms;

import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.tms.dao.TmsOperationalDataDao;
import ru.yandex.market.loyalty.core.dao.PromoDao;
import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.antifraud.AsyncUserRestrictionResult;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.lightweight.DateUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertFalse;
import static ru.yandex.market.loyalty.admin.tms.ActivateCoinsProcessor.ACTIVATE_COINS_TMS_ID;
import static ru.yandex.market.loyalty.admin.tms.ActivateCoinsProcessor.ACTIVATE_COINS_TMS_PROMO_ID_PARAM;
import static ru.yandex.market.loyalty.core.utils.MatcherUtils.repeatMatcher;

@TestFor(UpdateCoinEndDateProcessor.class)
public class UpdateCoinEndDateProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private ClockForTests clock;
    @Autowired
    private CoinService coinService;
    @Autowired
    private UpdateCoinEndDateProcessor updateCoinEndDateProcessor;
    @Autowired
    private PromoDao promoDao;
    @Autowired
    private TmsOperationalDataDao tmsOperationalDataDao;
    @Autowired
    private PromoService promoService;

    @Test
    public void shouldProcessCoins() throws Exception {
        final java.util.Date endDate = Date.from(clock.instant().plus(5, ChronoUnit.DAYS));
        final java.util.Date newEndDate = Date.from(clock.instant().plus(15, ChronoUnit.DAYS));

        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
                        .setEndDate(endDate)
        );

        final Set<CoinKey> coinKeys = createCoins(promo, 10);


        assertThat(
                coinService.search.getCoins(coinKeys),
                containsInAnyOrder(
                        repeatMatcher(
                                10,
                                hasProperty("endDate",
                                        equalTo(DateUtils.truncToDay(DateUtils.roundUpDate(endDate.toInstant(),
                                                ChronoUnit.DAYS))))
                        )
                )
        );

        promoManager.updateCoinPromo(PromoUtils.SmartShopping.defaultFixed()
                .setId(promo.getId())
                .setExpiration(ExpirationPolicy.toEndOfPromo())
                .setEndDate(newEndDate)
        );
        promo = promoService.getPromo(promo.getId());

        tmsOperationalDataDao.setValue(UpdateCoinEndDateProcessor.TMS_ID,
                UpdateCoinEndDateProcessor.PROMO_ID_PARAM,
                promo.getId().toString());

        updateCoinEndDateProcessor.processCoins(1000, 0);

        assertThat(
                coinService.search.getCoins(coinKeys),
                containsInAnyOrder(
                        repeatMatcher(
                                10,
                                hasProperty("endDate",
                                        equalTo(DateUtils.truncToDay(DateUtils.roundUpDate(newEndDate.toInstant(),
                                        ChronoUnit.DAYS))))
                        )
                )
        );

        assertFalse(
                tmsOperationalDataDao.getValues(UpdateCoinEndDateProcessor.TMS_ID,
                        UpdateCoinEndDateProcessor.PROMO_ID_PARAM)
                        .contains(Long.toString(promo.getId()))
        );
    }

    private void rewindTimeToPromoStartDate() {
        clock.spendTime(1, ChronoUnit.DAYS);
        clock.spendTime(1, ChronoUnit.SECONDS);
    }

    @NotNull
    private java.util.Date someDayInFuture() {
        return Date.from(clock.instant().plus(1, ChronoUnit.DAYS));
    }

    private Set<PromoParameterName<?>> getPromoParams(Promo promo) {
        return promoDao.getPromoParams(Collections.singletonList(promo.getId()))
                .get(promo.getId())
                .keys();
    }

    private List<String> getActivateCoinsTmsOperationalData() {
        return tmsOperationalDataDao.getValues(ACTIVATE_COINS_TMS_ID, ACTIVATE_COINS_TMS_PROMO_ID_PARAM);
    }

    @NotNull
    private Set<CoinKey> createCoins(Promo promo, int count) {
        return coinService.create.createCoinsBatch(
                promo,
                LongStream.range(0, count).boxed().collect(Collectors.toList()),
                uid -> Long.toString(uid),
                AsyncUserRestrictionResult.empty()
        ).getCoinIds().stream()
                .map(CoinKey::new)
                .collect(Collectors.toSet());
    }
}
