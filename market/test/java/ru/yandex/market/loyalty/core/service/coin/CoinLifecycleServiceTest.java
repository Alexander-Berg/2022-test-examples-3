package ru.yandex.market.loyalty.core.service.coin;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.coin.EmissionRestriction;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.antifraud.AsyncUserRestrictionResult;
import ru.yandex.market.loyalty.core.service.exclusions.ExcludedUsersService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyRegionSettingsCoreMockedDbTest;
import ru.yandex.market.loyalty.core.trigger.restrictions.actiononce.RefreshableBinding;
import ru.yandex.market.loyalty.core.trigger.restrictions.actiononce.RefreshableBindingService;
import ru.yandex.market.loyalty.core.utils.CoinRequestUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotEquals;
import static ru.yandex.market.loyalty.api.model.DiscountHistoryRecordType.EXPIRING;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.ACTIVE;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.EXPIRED;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.INACTIVE;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.TERMINATED;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;
import static ru.yandex.market.loyalty.core.utils.CoreCollectionUtils.enumCodeToSql;
import static ru.yandex.market.loyalty.core.utils.MatcherUtils.repeatMatcher;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_EXPIRATION_DAYS;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_MUID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

public class CoinLifecycleServiceTest extends MarketLoyaltyRegionSettingsCoreMockedDbTest {
    @Autowired
    private CoinLifecycleService coinLifecycleService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinCreationService coinCreationService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ExcludedUsersService excludedUsersService;
    @Autowired
    private CoinService coinService;
    @Autowired
    private RefreshableBindingService refreshableBindingService;
    @Autowired
    private PromoService promoService;

    @After
    public void clean() {
        excludedUsersService.reset();
    }

    @Test
    public void shouldExpireCoinsByOneBatch() throws InterruptedException {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()
                .setEmissionBudget(BigDecimal.valueOf(50))
        );

        coinCreationService.createCoinsBatch(
                promo,
                LongStream.range(0, 50)
                        .boxed()
                        .collect(Collectors.toList()),
                String::valueOf,
                AsyncUserRestrictionResult.empty()
        );

        Promo promoInactive = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setEmissionBudget(BigDecimal.valueOf(50))
                        .setCreateInactiveCoin()
        );

        coinCreationService.createCoinsBatch(
                promoInactive,
                LongStream.range(50, 100)
                        .boxed()
                        .collect(Collectors.toList()),
                String::valueOf,
                AsyncUserRestrictionResult.empty()
        );

        clock.spendTime(DEFAULT_EXPIRATION_DAYS + 1, ChronoUnit.DAYS);

        coinLifecycleService.expireCoins(100, 0, 1, false);

        assertCoinsInStatus(promo, 50, EXPIRED);
        assertCoinsInStatus(promoInactive, 50, INACTIVE);
        assertFoldingTransactionCount(promo, 1);

        clock.spendTime(120, ChronoUnit.DAYS);

        coinLifecycleService.expireCoins(100, 0, 1, false);

        assertCoinsInStatus(promoInactive, 50, EXPIRED);
        assertFoldingTransactionCount(promoInactive, 1);
    }

    @Test
    public void shouldLetCreateCoinsWithExistingRefreshableBindingForBindOnlyOncePromo() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setBindOnlyOnce(true)
                .setCode("test_code")
        );

        coinCreationService.createCoinsBatch(
                promo,
                LongStream.range(0, 10).boxed().collect(Collectors.toList()),
                uid -> Long.toString(uid),
                AsyncUserRestrictionResult.empty()
        );

        jdbcTemplate.update("TRUNCATE discount CASCADE");
        assertEquals(10, jdbcTemplate.queryForList("select * from refreshable_binding").size());

        coinCreationService.createCoinsBatch(
                promo,
                LongStream.range(5, 15).boxed().collect(Collectors.toList()),
                uid -> Long.toString(uid),
                AsyncUserRestrictionResult.empty()
        );

        List<Map<String, Object>> createdCoins = jdbcTemplate.queryForList("select * from discount");
        assertEquals(10, createdCoins.size());
    }

    @Test
    public void shouldLetCreateCoinsWithExistingRefreshableBindingTwiceWithUsersExcluded() {
        List<Long> uids = LongStream.range(0, 10).boxed().collect(Collectors.toList());

        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setBindOnlyOnce(true)
                .setCode("test_code")
        );

        coinCreationService.createCoinsBatch(
                promo,
                uids,
                uid -> Long.toString(uid),
                AsyncUserRestrictionResult.empty()
        );

        jdbcTemplate.update("TRUNCATE discount CASCADE");
        assertEquals(10, jdbcTemplate.queryForList("select * from refreshable_binding").size());

        uids.forEach(excludedUsersService::addUserToPromoBindOnlyOnceExcluded);

        coinCreationService.createCoinsBatch(
                promo,
                LongStream.range(5, 15).boxed().collect(Collectors.toList()),
                uid -> Long.toString(uid),
                AsyncUserRestrictionResult.empty()
        );

        List<Map<String, Object>> createdCoins = jdbcTemplate.queryForList("select * from discount");
        assertEquals(10, createdCoins.size());
    }

    @Test
    public void shouldLetCreateCoinsWithExistingRefreshableBindingForNotBindOnlyOncePromo() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setBindOnlyOnce(false)
                .setCode("test_code")
        );

        coinCreationService.createCoinsBatch(
                promo,
                LongStream.range(0, 10).boxed().collect(Collectors.toList()),
                uid -> Long.toString(uid),
                AsyncUserRestrictionResult.empty()
        );

        jdbcTemplate.update("TRUNCATE discount CASCADE");
        assertEquals(10, jdbcTemplate.queryForList("select * from refreshable_binding").size());

        coinCreationService.createCoinsBatch(
                promo,
                LongStream.range(5, 15).boxed().collect(Collectors.toList()),
                uid -> Long.toString(uid),
                AsyncUserRestrictionResult.empty()
        );

        List<Map<String, Object>> createdCoins = jdbcTemplate.queryForList("select * from discount");
        assertEquals(10, createdCoins.size());
    }

    @Test
    public void shouldLetCreateCoinsOnlyOnceForTwoBunchRequestsWithIntersectionInUserIds() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setBindOnlyOnce(true)
                .setCode("test_code")
        );

        coinCreationService.createCoinsBatch(
                promo,
                LongStream.range(0, 10).boxed().collect(Collectors.toList()),
                uid -> Long.toString(uid),
                AsyncUserRestrictionResult.empty()
        );

        coinCreationService.createCoinsBatch(
                promo,
                LongStream.range(5, 15).boxed().collect(Collectors.toList()),
                uid -> Long.toString(uid),
                AsyncUserRestrictionResult.empty()
        );

        List<Map<String, Object>> createdCoins = jdbcTemplate.queryForList("select * from discount");
        assertEquals(15, createdCoins.size());
    }

    @Test
    public void shouldExpireCoinsByEmissionFoldingPromoMixedWithRegularPromo() throws InterruptedException {
        Promo promo1 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()
                .setEmissionBudget(BigDecimal.valueOf(100))
        );
        Promo promo2 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()
                .setEmissionBudget(BigDecimal.valueOf(100))
                .setEmissionFolding(true)
        );

        coinCreationService.createCoinsBatch(
                promo1,
                LongStream.range(0, 100).boxed().collect(Collectors.toList()),
                uid -> Long.toString(uid),
                AsyncUserRestrictionResult.empty()
        );

        coinCreationService.createCoinsBatch(
                promo2,
                LongStream.range(100, 200).boxed().collect(Collectors.toList()),
                uid -> Long.toString(uid),
                AsyncUserRestrictionResult.empty()
        );

        clock.spendTime(DEFAULT_EXPIRATION_DAYS + 1, ChronoUnit.DAYS);

        coinLifecycleService.expireCoins(1000, 0, 1, false);

        assertCoinsInStatus(promo1, 100, EXPIRED);
        assertCoinsInStatus(promo2, 100, EXPIRED);
        assertFoldingTransactionCount(promo1, 1);
        assertFoldingTransactionCount(promo2, 0);
        assertCoinHistoryRecordCount(promo1, 100);
        assertCoinHistoryRecordCount(promo2, 0);
    }

    @Test
    public void shouldExpireCoinsByTwoBatches() throws InterruptedException {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()
                .setEmissionBudget(BigDecimal.valueOf(100))
        );

        coinCreationService.createCoinsBatch(
                promo,
                LongStream.range(0, 100).boxed().collect(Collectors.toList()),
                uid -> Long.toString(uid),
                AsyncUserRestrictionResult.empty()
        );

        clock.spendTime(DEFAULT_EXPIRATION_DAYS + 1, ChronoUnit.DAYS);

        coinLifecycleService.expireCoins(50, 0, 1, false);

        assertCoinsInStatus(promo, 100, EXPIRED);
        assertFoldingTransactionCount(promo, 2);
    }

    @Test
    public void shouldNotRefreshableRebindToArchivingPromo() {
        Promo promo1 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        Promo promo2 = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        CoinKey coinKey1 = coinService.create.createCoin(promo1, CoinRequestUtils.defaultNoAuth().build());
        CoinKey coinKey2 = coinService.create.createCoin(promo2, CoinRequestUtils.defaultNoAuth().build());

        coinService.lifecycle.bindCoinsToUser(DEFAULT_UID, DEFAULT_MUID, promo1.getPlatform());
        coinService.lifecycle.bindCoinsToUser(DEFAULT_UID, DEFAULT_MUID, promo2.getPlatform());

        Map<String, RefreshableBinding> bindings = refreshableBindingService.getBindings(DEFAULT_UID);
        assertThat(bindings.entrySet(), hasSize(2));

        // архивируем акцию с удалением биндингов
        promoService.updateStatus(promo1, PromoStatus.ARCHIVING);
        refreshableBindingService.deletePromoBindings(promo1, clock.instant().plus(Duration.ofMinutes(1)));
        bindings = refreshableBindingService.getBindings(DEFAULT_UID);
        assertThat(bindings.entrySet(), hasSize(1));

        // попытка заребиндить монету к архивной акции
        coinService.lifecycle.bindCoinsToUser(DEFAULT_UID, DEFAULT_MUID, promo1.getPlatform());
        bindings = refreshableBindingService.getBindings(DEFAULT_UID);
        assertThat(bindings.entrySet(), hasSize(1));
    }

    @Test
    public void shouldCreateNewCoinIfOldOneExpiredForOneCoinPromo() {
        Promo promo1 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setEmissionRestriction(EmissionRestriction.ONE_COIN));

        CoinKey coinKey1 = coinService.create.createCoin(promo1, defaultAuth().setStatus(ACTIVE).build());

        Optional<Coin> coin = coinService.search.getCoin(coinKey1);
        coinService.lifecycle.expireCoin(coin.get());

        long expiredCoinKey = coin.get().getCoinKey().getId();

        coinKey1 = coinService.create.createCoin(promo1, defaultAuth().setStatus(ACTIVE).build());

        coin = coinService.search.getCoin(coinKey1);
        long newCoinKey = coin.get().getCoinKey().getId();

        assertNotEquals(expiredCoinKey, newCoinKey);
    }

    @Test
    public void shouldNotCreateNewCoinIfUserHasTerminatedCoin() {
        Promo promo1 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
                        .setEmissionRestriction(EmissionRestriction.ONE_COIN));

        coinService.create.createCoin(promo1, defaultAuth().setStatus(TERMINATED).build());

        assertThrows(MarketLoyaltyException.class, () -> coinService.create.createCoin(promo1, defaultAuth().setStatus(ACTIVE).build()));
    }

    private void assertCoinHistoryRecordCount(Promo promo1, int count) {
        assertEquals(
                (Integer) count,
                jdbcTemplate.queryForObject(
                        "SELECT count(*)" +
                                "  FROM discount c," +
                                "       discount_history h," +
                                "       discount_history_x x" +
                                " WHERE x.discount_history_id = h.id" +
                                "   AND c.id = h.discount_id" +
                                "   AND h.record_type = " + enumCodeToSql(EXPIRING) + "" +
                                "   AND c.coin_props_id IN (SELECT id FROM coin_props WHERE promo_id = ?)",
                        Integer.class,
                        promo1.getPromoId().getId()
                )
        );
    }

    private void assertFoldingTransactionCount(Promo promo1, int count) {
        assertEquals(
                (Integer) count,
                jdbcTemplate.queryForObject(
                        "SELECT count(DISTINCT transaction_id)" +
                                "  FROM discount c," +
                                "       discount_history h," +
                                "       discount_history_x x" +
                                " WHERE x.discount_history_id = h.id" +
                                "   AND c.id = h.discount_id" +
                                "   AND h.record_type = " + enumCodeToSql(EXPIRING) + "" +
                                "   AND c.coin_props_id IN (SELECT id FROM coin_props WHERE promo_id = ?)",
                        Integer.class,
                        promo1.getPromoId().getId()
                )
        );
    }

    private void assertCoinsInStatus(Promo promo, int count, CoreCoinStatus coreCoinStatus) {
        assertThat(
                jdbcTemplate.query(
                        "SELECT status FROM discount WHERE coin_props_id IN (SELECT id FROM coin_props WHERE promo_id = ?)",
                        (rs, i) -> CoreCoinStatus.findByCode(rs.getString("status")),
                        promo.getPromoId().getId()
                ),
                containsInAnyOrder(
                        repeatMatcher(
                                count,
                                equalTo(coreCoinStatus)
                        )
                )
        );
    }
}
