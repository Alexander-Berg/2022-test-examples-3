package ru.yandex.market.loyalty.admin.tms;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.dao.CoinPropsResetQueueDao;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.coin.CoinCreationService;
import ru.yandex.market.loyalty.core.service.coin.CoinSearchService;
import ru.yandex.market.loyalty.core.trigger.actions.CoinInsertRequest;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(ResetCoinPropsProcessor.class)
public class ResetCoinPropsProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private ResetCoinPropsProcessor resetCoinPropsProcessor;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinCreationService coinCreationService;
    @Autowired
    private CoinSearchService coinSearchService;
    @Autowired
    private CoinPropsResetQueueDao coinPropsResetQueueDao;
    @Autowired
    private PromoService promoService;

    @Test
    public void shouldResetCoinPropsAndIncreaseEndDateOfActiveCoin() {
        Date initialEndDate = nowPlus(30, ChronoUnit.DAYS);
        Date updatedEndDate = nowPlus(60, ChronoUnit.DAYS);

        Date initialCoinEndDate = roundEndDateToEndOfTheDay(initialEndDate);
        Date updatedCoinEndDate = roundEndDateToEndOfTheDay(updatedEndDate);

        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed()
                        .setEndDate(initialEndDate)
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
        );


        CoinKey coin = coinCreationService.createCoin(
                promo,
                CoinInsertRequest.authMarketBonus(DEFAULT_UID)
                        .setSourceKey("coin_1")
                        .setReason(CoreCoinCreationReason.OTHER)
                        .setStatus(CoreCoinStatus.ACTIVE)
                        .build()
        );

        assertThat(
                coinSearchService.getCoin(coin).orElseThrow(),
                hasProperty("endDate", equalTo(initialCoinEndDate))
        );

        promoManager.updateCoinPromo(
                defaultFixed()
                        .setId(promo.getId())
                        .setEndDate(updatedEndDate)
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
        );

        coinPropsResetQueueDao.savePromoId(promo.getPromoId());
        resetCoinPropsProcessor.process(Duration.ofMinutes(1), 500);


        assertThat(
                coinSearchService.getCoin(coin).orElseThrow(),
                allOf(
                        hasProperty("endDate", equalTo(updatedCoinEndDate)),
                        hasProperty("coinPropsId", equalTo(promoService.getPromo(promo.getId()).getCoinPropsId()))
                )
        );
    }

    @Test
    public void shouldResetCoinPropsAndDecreaseEndDateOfActiveCoin() {
        Date initialEndDate = nowPlus(60, ChronoUnit.DAYS);
        Date updatedEndDate = nowPlus(30, ChronoUnit.DAYS);

        Date initialCoinEndDate = roundEndDateToEndOfTheDay(initialEndDate);
        Date updatedCoinEndDate = roundEndDateToEndOfTheDay(updatedEndDate);

        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed()
                        .setEndDate(initialEndDate)
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
        );


        CoinKey coin = coinCreationService.createCoin(
                promo,
                CoinInsertRequest.authMarketBonus(DEFAULT_UID)
                        .setSourceKey("coin_1")
                        .setReason(CoreCoinCreationReason.OTHER)
                        .setStatus(CoreCoinStatus.ACTIVE)
                        .build()
        );

        assertThat(
                coinSearchService.getCoin(coin).orElseThrow(),
                hasProperty("endDate", equalTo(initialCoinEndDate))
        );

        promoManager.updateCoinPromo(
                defaultFixed()
                        .setId(promo.getId())
                        .setEndDate(updatedEndDate)
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
        );

        coinPropsResetQueueDao.savePromoId(promo.getPromoId());
        resetCoinPropsProcessor.process(Duration.ofMinutes(1), 500);


        assertThat(
                coinSearchService.getCoin(coin).orElseThrow(),
                allOf(
                        hasProperty("endDate", equalTo(updatedCoinEndDate)),
                        hasProperty("coinPropsId", equalTo(promoService.getPromo(promo.getId()).getCoinPropsId()))
                )
        );
    }

    @Test
    public void shouldResetCoinPropsAndChangeEndDateOfUsedCoin() {
        Date initialEndDate = nowPlus(60, ChronoUnit.DAYS);
        Date updatedEndDate = nowPlus(30, ChronoUnit.DAYS);

        Date initialCoinEndDate = roundEndDateToEndOfTheDay(initialEndDate);
        Date updatedCoinEndDate = roundEndDateToEndOfTheDay(updatedEndDate);

        Promo initialPromo = promoManager.createSmartShoppingPromo(
                defaultFixed()
                        .setEndDate(initialEndDate)
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
        );


        CoinKey coin = coinCreationService.createCoin(
                initialPromo,
                CoinInsertRequest.authMarketBonus(DEFAULT_UID)
                        .setSourceKey("coin_1")
                        .setReason(CoreCoinCreationReason.OTHER)
                        .setStatus(CoreCoinStatus.USED)
                        .build()
        );

        assertThat(
                coinSearchService.getCoin(coin).orElseThrow(),
                hasProperty("endDate", equalTo(initialCoinEndDate))
        );

        promoManager.updateCoinPromo(
                defaultFixed()
                        .setId(initialPromo.getId())
                        .setEndDate(updatedEndDate)
                        .setExpiration(ExpirationPolicy.toEndOfPromo())
        );

        coinPropsResetQueueDao.savePromoId(initialPromo.getPromoId());
        resetCoinPropsProcessor.process(Duration.ofMinutes(1), 500);


        assertThat(
                coinSearchService.getCoin(coin).orElseThrow(),
                allOf(
                        hasProperty("endDate", equalTo(updatedCoinEndDate)),
                        hasProperty("coinPropsId", equalTo(initialPromo.getCoinPropsId()))
                )
        );
    }

    private Date nowPlus(int amountToAdd, ChronoUnit chronoUnit) {
        return Date.from(clock.instant().plus(amountToAdd, chronoUnit));
    }

    private Date roundEndDateToEndOfTheDay(Date promoEndDate) {
        return ExpirationPolicy.toEndOfPromo()
                .chooseEndDate(clock.instant(), promoEndDate);
    }

}
