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
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.antifraud.AsyncUserRestrictionResult;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.admin.tms.ActivateCoinsProcessor.ACTIVATE_COINS_TMS_ID;
import static ru.yandex.market.loyalty.admin.tms.ActivateCoinsProcessor.ACTIVATE_COINS_TMS_PROMO_ID_PARAM;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.ACTIVE;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.INACTIVE;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.CREATE_INACTIVE_COIN;
import static ru.yandex.market.loyalty.core.utils.MatcherUtils.repeatMatcher;

@TestFor(ActivateCoinsProcessor.class)
public class ActivateCoinsProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private ClockForTests clock;
    @Autowired
    private CoinService coinService;
    @Autowired
    private ActivateCoinsProcessor activateCoinsProcessor;
    @Autowired
    private PromoDao promoDao;
    @Autowired
    private TmsOperationalDataDao tmsOperationalDataDao;

    @Test
    public void shouldActivateCoins() throws Exception {
        final Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setCreateInactiveCoin()
                        .setStartDate(someDayInFuture())
        );

        final Set<CoinKey> coinKeys = createCoins(promo, 10);

        assertThat(
                coinService.search.getCoins(coinKeys),
                containsInAnyOrder(
                        repeatMatcher(10, hasProperty("status", equalTo(INACTIVE)))
                )
        );

        rewindTimeToPromoStartDate();

        activateCoinsProcessor.checkPromoCreateInactiveCoinTag();

        assertFalse(
                getPromoParams(promo)
                        .contains(CREATE_INACTIVE_COIN)
        );

        assertTrue(
                getActivateCoinsTmsOperationalData()
                        .contains(Long.toString(promo.getId()))
        );

        activateCoinsProcessor.activateInactiveCoins(1000, 0);

        assertThat(
                coinService.search.getCoins(coinKeys),
                containsInAnyOrder(
                        repeatMatcher(
                                10,
                                hasProperty("status", equalTo(ACTIVE))
                        )
                )
        );

        assertFalse(
                getPromoParams(promo)
                        .contains(CREATE_INACTIVE_COIN)
        );

        assertFalse(
                getActivateCoinsTmsOperationalData().contains(
                        Long.toString(promo.getId())
                )
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
