package ru.yandex.market.marketpromo.core.service.task;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.core.service.impl.TradesListCache;
import ru.yandex.market.marketpromo.core.test.ServiceTaskTestBase;
import ru.yandex.market.marketpromo.core.test.generator.Promos;
import ru.yandex.market.marketpromo.model.User;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static ru.yandex.market.marketpromo.core.test.generator.PromoMechanics.minimalDiscountPercentSize;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.ANOTHER_DD_PROMO_KEY;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.CAG_PROMO_KEY;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.DD_PROMO_KEY;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.cheapestAsGift;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.directDiscount;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.id;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promo;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.trade;

public class TradesListRefreshTaskTest extends ServiceTaskTestBase {

    public static final String USER_1 = "user1";
    public static final String USER_2 = "user2";
    public static final String USER_3 = "user3";

    @Autowired
    private PromoDao promoDao;
    @Autowired
    private TradesListRefreshTask task;
    @Autowired
    private TradesListCache tradesListCache;

    @BeforeEach
    void setUp() {
        promoDao.replace(promo(
                id(DD_PROMO_KEY.getId()),
                trade(User.builder().id(USER_1).build()),
                directDiscount(
                        minimalDiscountPercentSize(10)
                )
        ));
        promoDao.replace(
                promo(
                        id(CAG_PROMO_KEY.getId()),
                        trade(User.builder().id(USER_2).build()),
                        cheapestAsGift()
                )
        );
        tradesListCache.refreshCache();
    }

    @Test
    void shouldInitCacheOnStart() {
        assertFalse(tradesListCache.get().isEmpty());
        assertThat(tradesListCache.get(), hasSize(2));
        assertThat(tradesListCache.get(), hasItems(USER_1, USER_2));
    }

    @Test
    void shouldUpdateOnProcess() {
        promoDao.replace(promo(
                id(ANOTHER_DD_PROMO_KEY.getId()),
                trade(User.builder().id(USER_3).build()),
                directDiscount()
        ));

        task.process();

        assertThat(tradesListCache.get(), hasSize(3));
        assertThat(tradesListCache.get(), hasItems(USER_1, USER_2, USER_3));

        promoDao.replace(promo(
                id(ANOTHER_DD_PROMO_KEY.getId()),
                trade(User.builder().id(USER_1).build()),
                directDiscount()
        ));

        task.process();

        assertThat(tradesListCache.get(), hasSize(2));
        assertThat(tradesListCache.get(), hasItems(USER_1, USER_2));

    }
}
