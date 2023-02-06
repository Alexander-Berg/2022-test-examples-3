package ru.yandex.market.loyalty.admin.tms;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.coin.CoinType;
import ru.yandex.market.loyalty.core.dao.coin.CreateCoinsTaskDao;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinDescription;
import ru.yandex.market.loyalty.core.model.coin.CreateCoinTask;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.BudgetService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.admin.tms.CreateCoinsTaskProcessor.SPECIAL_PROMO_CODE;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.FIRST_CHILD_CATEGORY_ID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_COINS_LIMIT;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(CreateCoinsTaskProcessor.class)
public class CreateCoinsTaskProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private BudgetService budgetService;
    @Autowired
    private CreateCoinsTaskProcessor createCoinsTaskProcessor;
    @Autowired
    private CreateCoinsTaskDao createCoinsTaskDao;
    @Autowired
    private CoinService coinService;

    @Test
    public void shouldCreateCoins() {
        promoManager.createSmartShoppingPromo(SmartShopping.defaultDynamic().setActionCode(SPECIAL_PROMO_CODE));

        createCoinsTaskDao.addToQueue(Collections.singletonList(
                new CreateCoinTask(
                        null,
                        DEFAULT_UID,
                        FIRST_CHILD_CATEGORY_ID,
                        "порошки",
                        CoinType.PERCENT,
                        BigDecimal.valueOf(5),
                        null,
                        BigDecimal.valueOf(10_000)
                )
        ));

        createCoinsTaskProcessor.createCoins(Duration.ofSeconds(10), 500);

        List<Coin> coins = coinService.search.getCoinsByUid(DEFAULT_UID, DEFAULT_COINS_LIMIT);
        assertThat(coins, hasSize(1));

        CoinDescription coinDescription = getDescription(coins.get(0));
        assertEquals("на порошки", coinDescription.getRestrictionDescription());
        assertEquals(
                "Этот купон специально для вас. Мы просто положили его здесь, чтобы вы могли купить порошки до 10 000" +
                        " ₽ со скидкой. Используйте его отдельно или вместе с другими.",
                coinDescription.getDescription()
        );

        assertThat(createCoinsTaskDao.getNotProceeded(500), is(empty()));
    }

    @Test
    public void shouldCreateSeveralCoins() {
        Promo promo = promoManager.createSmartShoppingPromo(
                SmartShopping.defaultDynamic().setActionCode(SPECIAL_PROMO_CODE)
        );

        createCoinsTaskDao.addToQueue(
                IntStream.range(0, 10)
                        .mapToObj(i ->
                                new CreateCoinTask(
                                        null,
                                        ThreadLocalRandom.current().nextLong(),
                                        FIRST_CHILD_CATEGORY_ID,
                                        "порошки",
                                        CoinType.PERCENT,
                                        BigDecimal.valueOf(5),
                                        null,
                                        BigDecimal.valueOf(10_000)
                                )
                        )
                        .collect(Collectors.toList())
        );

        createCoinsTaskProcessor.createCoins(Duration.ofSeconds(10), 3);

        assertThat(
                budgetService.getBalance(promo.getSpendingEmissionAccountId()),
                comparesEqualTo(BigDecimal.TEN)
        );
    }

    private CoinDescription getDescription(Coin recommendedCoin) {
        return coinService.search.getCoinDescriptionsFromCacheOrLoadDynamicallyForCoins(
                Collections.singletonList(recommendedCoin)
        ).get(recommendedCoin.getCoinKey());
    }
}
