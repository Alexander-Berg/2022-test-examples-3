package ru.yandex.market.loyalty.core.dao;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.UserPromocodeEntry;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;

public class UserActivePromocodeEntryDaoTest extends MarketLoyaltyCoreMockedDbTestBase {

    private static final String PROMOCODE = "some_promocode";
    private static final long USER_ID = 123L;

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinService coinService;
    @Autowired
    private UserActivePromocodeEntryDao userActivePromocodeEntryDao;

    private Promo coinPromocode;

    @Before
    public void configure() {
        coinPromocode = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setCode(PROMOCODE));
    }

    @Test
    public void shouldAddCoinEntry() {
        CoinKey coinKey = coinService.create.createCoin(coinPromocode, defaultAuth(1).build());

        UserPromocodeEntry entry = userActivePromocodeEntryDao.create(UserPromocodeEntry.builder()
                .userId(USER_ID)
                .code(PROMOCODE)
                .coinKey(coinKey)
                .promoId(coinPromocode.getId()));

        assertThat(entry.getId(), greaterThan(0L));
    }

    @Test
    public void shouldGetCoinEntry() {
        CoinKey coinKey = coinService.create.createCoin(coinPromocode, defaultAuth(1).build());

        UserPromocodeEntry entry = userActivePromocodeEntryDao.create(UserPromocodeEntry.builder()
                .userId(USER_ID)
                .code(PROMOCODE)
                .coinKey(coinKey)
                .promoId(coinPromocode.getId()));

        assertThat(userActivePromocodeEntryDao.selectActive(UserActivePromocodeEntryDao.CODE.eqTo(PROMOCODE)),
                hasItem(allOf(
                hasProperty("id", comparesEqualTo(entry.getId()))
        )));
    }

    @Test(expected = DuplicateKeyException.class)
    public void shouldFailOnAddEntryTwice() {
        CoinKey coinKey = coinService.create.createCoin(coinPromocode, defaultAuth(1).build());

        for (int i = 0; i < 2; i++) {
            userActivePromocodeEntryDao.create(UserPromocodeEntry.builder()
                    .userId(USER_ID)
                    .code(PROMOCODE)
                    .coinKey(coinKey)
                    .promoId(coinPromocode.getId()));
        }
    }
}
