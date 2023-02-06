package ru.yandex.market.antifraud.orders.storage.dao.loyalty;


import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.entity.loyalty.LoyaltyCoin;
import ru.yandex.market.antifraud.orders.entity.loyalty.LoyaltyPromo;
import ru.yandex.market.antifraud.orders.test.annotations.DaoLayerTest;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author dzvyagin
 */
@DaoLayerTest
@RunWith(SpringJUnit4ClassRunner.class)
public class LoyaltyDaoTest {

    @Autowired
    private NamedParameterJdbcOperations jdbcTemplate;

    @Test
    public void getPromo() {
        LoyaltyDao loyaltyDao = new LoyaltyDao(jdbcTemplate);
        LoyaltyPromo promo = getPromo(103L);
        loyaltyDao.savePromo(promo);
        LoyaltyPromo saved = loyaltyDao.findPromoByPromoId(103L).orElseThrow(RuntimeException::new);
        assertThat(promo.withId(saved.getId())).isEqualTo(saved);
    }


    @Test
    public void getPromos() {
        LoyaltyDao loyaltyDao = new LoyaltyDao(jdbcTemplate);
        LoyaltyPromo promo1 = getPromo(123L);
        LoyaltyPromo promo2 = getPromo(234L);
        LoyaltyPromo promo3 = getPromo(345L);

        promo1 = loyaltyDao.savePromo(promo1);
        promo2 = loyaltyDao.savePromo(promo2);
        promo3 = loyaltyDao.savePromo(promo3);

        List<LoyaltyPromo> saved = loyaltyDao.findPromoByPromoIds(Arrays.asList(123L, 345L));
        assertThat(saved).containsExactly(promo1, promo3);
    }

    @Test
    public void getBindedOnlyOncePromo() {
        LoyaltyDao loyaltyDao = new LoyaltyDao(jdbcTemplate);
        LoyaltyPromo promo1 = getPromo(103L);
        LoyaltyPromo promo2 = getPromo(103L, "");
        loyaltyDao.savePromo(promo1);
        loyaltyDao.savePromo(promo2);
        LoyaltyPromo saved = loyaltyDao.findPromoBindedOnlyOnceByPromoId(103L).orElseThrow(RuntimeException::new);
        assertThat(promo1.withId(saved.getId())).isEqualTo(saved);
    }


    @Test
    public void getBindedOnlyOncePromos() {
        LoyaltyDao loyaltyDao = new LoyaltyDao(jdbcTemplate);
        LoyaltyPromo promo11 = getPromo(456L);
        LoyaltyPromo promo12 = getPromo(456L, "");
        LoyaltyPromo promo21 = getPromo(567L);
        LoyaltyPromo promo22 = getPromo(567L, "");
        LoyaltyPromo promo31 = getPromo(678L);
        LoyaltyPromo promo32 = getPromo(678L, "");

        promo11 = loyaltyDao.savePromo(promo11);
        promo12 = loyaltyDao.savePromo(promo12);
        promo21 = loyaltyDao.savePromo(promo21);
        promo22 = loyaltyDao.savePromo(promo22);
        promo31 = loyaltyDao.savePromo(promo31);
        promo32 = loyaltyDao.savePromo(promo32);

        List<LoyaltyPromo> saved = loyaltyDao.findPromoBindedOnlyOnceByPromoIds(Arrays.asList(456L, 678L));
        assertThat(saved).containsExactly(promo11, promo31);
    }

    @Test
    public void getCoins() {
        LoyaltyDao loyaltyDao = new LoyaltyDao(jdbcTemplate);
        LoyaltyCoin c1 = getCoin(1L, 101L, 201L);
        LoyaltyCoin c2 = getCoin(2L, 102L, 201L);
        LoyaltyCoin c3 = getCoin(3L, 103L, 202L);
        LoyaltyCoin c4 = getCoin(4L, 104L, 203L);

        c1 = loyaltyDao.saveCoin(c1);
        c2 = loyaltyDao.saveCoin(c2);
        c3 = loyaltyDao.saveCoin(c3);
        c4 = loyaltyDao.saveCoin(c4);

        List<MarketUserId> users = Arrays.asList(
                new MarketUserId(null, "2", "crypta", 1L),
                new MarketUserId(null, "3", "uid", 1L),
                new MarketUserId(null, "4", "uid", 1L),
                new MarketUserId(null, "5", "uid", 1L)
        );

        List<LoyaltyCoin> coins = loyaltyDao.findCoinsUsedByUsers(users);
        assertThat(coins).containsExactly(c3, c4);
    }

    @Test
    public void invalidIds() {
        LoyaltyDao loyaltyDao = new LoyaltyDao(jdbcTemplate);

        List<MarketUserId> users = Arrays.asList(
                new MarketUserId(null, "2", "crypta", 1L),
                new MarketUserId(null, "3as", "uid", 1L),
                new MarketUserId(null, "00sdasd", "uid", 1L),
                new MarketUserId(null, "5,9", "uid", 1L)
        );

        List<LoyaltyCoin> coins = loyaltyDao.findCoinsUsedByUsers(users);
        assertThat(coins).isEmpty();
    }

    private LoyaltyCoin getCoin(long uid, long coinId, long promoId) {
        return LoyaltyCoin.builder()
                .coinId(coinId)
                .promoId(promoId)
                .uid(uid)
                .status("USED")
                .startDate(Instant.now())
                .endDate(Instant.now())
                .actionTime(Instant.now())
                .orderIds(Arrays.asList(99L, 100L, 101L))
                .build();
    }

    private LoyaltyPromo getPromo(Long promoId) {
        return getPromo(promoId, "CHECK_USER");
    }

    private LoyaltyPromo getPromo(Long promoId, String actionOnceRestrictionType) {
        return LoyaltyPromo.builder()
                .promoId(promoId)
                .promoGroupId("promo-group")
                .actionOnceRestrictionType(actionOnceRestrictionType)
                .promoName("name")
                .bindOnlyOnce(true)
                .build();
    }
}
