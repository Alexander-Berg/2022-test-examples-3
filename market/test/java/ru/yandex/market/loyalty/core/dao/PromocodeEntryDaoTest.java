package ru.yandex.market.loyalty.core.dao;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.core.dao.promocode.PromocodeEntryDao;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromocodeEntry;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.coupon.CouponService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.lightweight.DateUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.lightweight.DateUtils.FAR_FUTURE_INSTANT;

public class PromocodeEntryDaoTest extends MarketLoyaltyCoreMockedDbTestBase {

    private static final String PROMOCODE = "some_promocode";

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;
    @Autowired
    private CouponService couponService;
    @Autowired
    private PromocodeEntryDao promocodeEntryDao;

    private Promo coinPromocode;
    private Promo couponPromocode;

    @Before
    public void configure() {
        coinPromocode = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setCode(PROMOCODE));
        couponPromocode = promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse()
                .setCouponCode(PROMOCODE));
    }

    @Test
    public void shouldAddCoinEntry() {
        CoinKey coinKey = coinService.create.createCoin(coinPromocode, defaultAuth(1).build());

        PromocodeEntry entry = promocodeEntryDao.create(PromocodeEntry.builder()
                .code(PROMOCODE)
                .coinKey(coinKey)
                .promoId(coinPromocode.getId())
                .startsAt(LocalDateTime.ofInstant(coinPromocode.getStartDate().toInstant(), clock.getZone()))
                .endsAt(LocalDateTime.ofInstant(coinPromocode.getEndDate().toInstant(), clock.getZone())));

        assertThat(entry.getId(), greaterThan(0L));
    }

    @Test
    public void shouldGetCoinEntry() {
        CoinKey coinKey = coinService.create.createCoin(coinPromocode, defaultAuth(1).build());

        PromocodeEntry entry = promocodeEntryDao.create(PromocodeEntry.builder()
                .code(PROMOCODE)
                .coinKey(coinKey)
                .promoId(coinPromocode.getId())
                .startsAt(LocalDateTime.ofInstant(coinPromocode.getStartDate().toInstant(), clock.getZone()))
                .endsAt(LocalDateTime.ofInstant(coinPromocode.getEndDate().toInstant(), clock.getZone())));

        assertThat(promocodeEntryDao.selectActiveCurrent(PromocodeEntryDao.CODE.eqTo(PROMOCODE)), hasItem(allOf(
                hasProperty("id", comparesEqualTo(entry.getId()))
        )));
    }

    @Test
    public void shouldAddCouponEntry() {
        PromocodeEntry entry = promocodeEntryDao.create(PromocodeEntry.builder()
                .code(PROMOCODE)
                .couponId(couponService.getCouponByPromo(couponPromocode).getId())
                .startsAt(DateUtils.fromDate(couponPromocode.getStartDate()))
                .endsAt(DateUtils.fromDate(couponPromocode.getEndDate()))
                .promoId(couponPromocode.getId()));

        assertThat(entry.getId(), greaterThan(0L));
    }

    @Test(expected = MarketLoyaltyException.class)
    public void shouldNotDuplicateOnAddEntryTwice() {
        CoinKey coinKey = coinService.create.createCoin(coinPromocode, defaultAuth(1).build());

        for (int i = 0; i < 2; i++) {
            promocodeEntryDao.create(PromocodeEntry.builder()
                    .code(PROMOCODE.toUpperCase())
                    .coinKey(coinKey)
                    .promoId(coinPromocode.getId())
                    .startsAt(LocalDateTime.ofInstant(coinPromocode.getStartDate().toInstant(), clock.getZone()))
                    .endsAt(LocalDateTime.ofInstant(coinPromocode.getEndDate().toInstant(), clock.getZone())));
        }

        assertThat(promocodeEntryDao.countByCode(PROMOCODE), is(1));
        assertThat(promocodeEntryDao.selectActiveCurrent(PromocodeEntryDao.CODE.eqTo(PROMOCODE)), hasItem(
                allOf(
                        hasProperty("coinKey", comparesEqualTo(coinKey)),
                        hasProperty("promoId", comparesEqualTo(coinPromocode.getId()))
                )));
    }


    @Test(expected = MarketLoyaltyException.class)
    public void testReserveAndCheckPromocode() {
        String code = "ABABAB";
        promocodeEntryDao.create(
                PromocodeEntry.builder()
                        .code(code.toUpperCase())
                        .startsAt(LocalDateTime.ofInstant(clock.instant(), clock.getZone()))
                        .endsAt(LocalDateTime.ofInstant(FAR_FUTURE_INSTANT, clock.getZone())));
        promocodeEntryDao.checkBindedOverlappingPromocodes(
                code,
                LocalDateTime.ofInstant(clock.instant().plus(1, ChronoUnit.DAYS), clock.getZone()),
                LocalDateTime.ofInstant(clock.instant().plus(3, ChronoUnit.DAYS), clock.getZone()),
                null, false);

    }
}
