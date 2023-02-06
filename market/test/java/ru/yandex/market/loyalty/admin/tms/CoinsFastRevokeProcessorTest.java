package ru.yandex.market.loyalty.admin.tms;

import java.sql.Date;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.tms.coin_fast_revoke.CoinFastRevokeProcessor;
import ru.yandex.market.loyalty.core.config.YtHahn;
import ru.yandex.market.loyalty.core.dao.coin.CoinDao;
import ru.yandex.market.loyalty.core.dao.query.Filter;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

import ru.yandex.market.loyalty.test.TestFor;

@TestFor(CoinFastRevokeProcessor.class)
public class CoinsFastRevokeProcessorTest extends MarketLoyaltyAdminMockedDbTest {

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CoinDao coinDao;
    @Autowired
    private Clock clock;
    @Autowired
    private CoinFastRevokeProcessor coinFastRevokeProcessor;
    @MockBean
    @YtHahn
    private JdbcTemplate ytJdbcTemplate;

    private Promo promo;

    public CoinsFastRevokeProcessorTest() {
    }

    private void prepareCoins(int startId, int coinsCount) {
        promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        for (int i = startId; i < startId + coinsCount; i++) {
            jdbcTemplate.update(
                    "INSERT INTO discount(" +
                            "id, uid, status, start_date, end_date, activation_token, coin_props_id, " +
                            "certificate_token, source_key, creation_time, retry, reason," +
                            "reason_param, require_auth, platform, promo_subtype) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, ?, ?, ?, ?, ?)",
                    i + 2000, DEFAULT_UID, "ACTIVE", Date.from(promo.getStartDate().toInstant()),
                    Date.from(clock.instant().plus(30, ChronoUnit.DAYS)), null,
                    promo.getCoinPropsId(), null, "sourceKey-" + i,
                    Date.from(clock.instant()), "OTHER", null, false, "BLUE", "COIN"
            );
            jdbcTemplate.update(
                    "INSERT INTO discount_history(" +
                            "discount_id, record_type, discount_status, operation_context_id) " +
                            "VALUES (?, ?, ?, ?)",
                    i + 2000,
                    "CREATION",
                    "ACTIVE",
                    null
            );
        }
    }

    @Test
    public void testCoinFastProcessor() {
        int coinsCount = 10_000;
        int batchSize = 3000;
        prepareCoins(0, coinsCount);
        configurationService.set(ConfigurationService.COINS_TO_FAST_REVOKE_YT_TABLE, "table_path");
        configurationService.set(ConfigurationService.COINS_TO_FAST_REVOKE_BATCH_SIZE, batchSize);
        List<Coin.Builder> coins = getAllPreparedCoins();

        Mockito.when(ytJdbcTemplate.query(any(String.class),
                any(Object[].class), any(RowMapper.class)
        )).then(invocation -> {
            Object[] params = invocation.getArgument(1, Object[].class);
            Long lastRevokedId = (Long) params[0];
            return coins.stream()
                    .map(c -> c.getCoinKey().getId())
                    .filter(coinId -> coinId > lastRevokedId)
                    .collect(Collectors.toList());
        });
        for (int i = 0; i < coinsCount / batchSize + 1; i++) {
            coinFastRevokeProcessor.revokeBatchCoins();
        }
        List<Coin.Builder> coinsAfterJob = coinDao.getCoins(Filter.trueValue(), false);
        assertThat(coinsAfterJob, everyItem(
                hasProperty("uid", lessThan(0L))
        ));
        assertThat(configurationService.getCoinsToFastRevokeTablePath(), equalTo(Optional.empty()));
    }

    private List<Coin.Builder> getAllPreparedCoins() {
        return coinDao.getCoinsForUids(promo.getId(), Set.of(DEFAULT_UID));
    }
}
