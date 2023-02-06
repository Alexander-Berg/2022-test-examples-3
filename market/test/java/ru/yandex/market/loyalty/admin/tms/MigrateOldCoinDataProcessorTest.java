package ru.yandex.market.loyalty.admin.tms;

import java.time.Duration;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.dao.coin.CoinTestDataDao;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.coin.SourceKey;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(MigrateOldCoinDataProcessor.class)
public class MigrateOldCoinDataProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MigrateOldCoinDataProcessor migrateOldCoinDataProcessor;

    @Autowired
    private CoinService coinService;
    @Autowired
    private CoinTestDataDao coinTestDataDao;

    @Test
    public void shouldMigrateActiveCoins() throws InterruptedException {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        coinTestDataDao.createActiveCoin(promo, 9000L, "sourceKey-1", false, DEFAULT_UID);
        coinTestDataDao.createActiveCoin(promo, 9001L, "sourceKey-2", false, DEFAULT_UID);

        configurationService.set(CopyActiveCoinsProcessor.CUR_ID_PARAM_NAME, 0L);
        configurationService.set(CopyActiveCoinsProcessor.MAX_ID_PARAM_NAME, 9001L);

        coinService.create.insertNoAuthCoins(
                promo.getId(),
                ImmutableSet.of(new SourceKey("sourceKey-3")),
                CoreCoinCreationReason.OTHER
        );
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM discount", Integer.class), equalTo(1));

        migrateOldCoinDataProcessor.migrateOldData(Duration.ofSeconds(1), 1, 50);

        assertThat(configurationService.get(CopyActiveCoinsProcessor.CUR_ID_PARAM_NAME), equalTo("9001"));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM discount", Integer.class), equalTo(3));

    }

    @Test
    public void shouldMigrateTerminatedCoins() throws InterruptedException {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        coinTestDataDao.createTerminatedCoin(promo, 9000L, "sourceKey-1", false, DEFAULT_UID);
        coinTestDataDao.createTerminatedCoin(promo, 9001L, "sourceKey-2", false, DEFAULT_UID);

        configurationService.set(CopyActiveCoinsProcessor.CUR_ID_PARAM_NAME, 9001L);
        configurationService.set(CopyActiveCoinsProcessor.MAX_ID_PARAM_NAME, 9001L);
        configurationService.set(CopyRevokedAndTerminatedCoinsProcessor.CUR_ID_PARAM_NAME, 0L);
        configurationService.set(CopyRevokedAndTerminatedCoinsProcessor.MAX_ID_PARAM_NAME, 9001L);

        coinService.create.insertNoAuthCoins(
                promo.getId(),
                ImmutableSet.of(new SourceKey("sourceKey-3")),
                CoreCoinCreationReason.OTHER
        );
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM discount", Integer.class), equalTo(1));

        migrateOldCoinDataProcessor.migrateOldData(Duration.ofSeconds(1), 1, 50);

        assertThat(configurationService.get(CopyRevokedAndTerminatedCoinsProcessor.CUR_ID_PARAM_NAME), equalTo("9001"));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM discount", Integer.class), equalTo(3));
    }

    @Test
    public void shouldMigrateExpiredCoins() throws InterruptedException {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        coinTestDataDao.createExpiredCoin(promo, 9000L, "sourceKey-1", false, DEFAULT_UID);
        coinTestDataDao.createExpiredCoin(promo, 9001L, "sourceKey-2", false, DEFAULT_UID);

        configurationService.set(CopyActiveCoinsProcessor.CUR_ID_PARAM_NAME, 9001L);
        configurationService.set(CopyActiveCoinsProcessor.MAX_ID_PARAM_NAME, 9001L);
        configurationService.set(CopyRevokedAndTerminatedCoinsProcessor.CUR_ID_PARAM_NAME, 9001L);
        configurationService.set(CopyRevokedAndTerminatedCoinsProcessor.MAX_ID_PARAM_NAME, 9001L);
        configurationService.set(CopyExpiredCoinsProcessor.CUR_ID_PARAM_NAME, 0L);
        configurationService.set(CopyExpiredCoinsProcessor.MAX_ID_PARAM_NAME, 9001L);

        migrateOldCoinDataProcessor.migrateOldData(Duration.ofSeconds(1), 1, 50);

        assertThat(configurationService.get(CopyExpiredCoinsProcessor.CUR_ID_PARAM_NAME), equalTo("9001"));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM discount_archive", Integer.class), equalTo(2));
    }


    @Test
    public void shouldDeleteDuplicateCoins() throws InterruptedException {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        coinTestDataDao.createActiveCoin(promo, 9000L, "sourceKey-1", false, DEFAULT_UID);
        coinTestDataDao.createActiveCoin(promo, 9001L, "sourceKey-2", false, DEFAULT_UID);

        configurationService.set(CopyActiveCoinsProcessor.CUR_ID_PARAM_NAME, 0L);
        configurationService.set(CopyActiveCoinsProcessor.MAX_ID_PARAM_NAME, 9001L);
        configurationService.set(CopyRevokedAndTerminatedCoinsProcessor.CUR_ID_PARAM_NAME, 9001L);
        configurationService.set(CopyRevokedAndTerminatedCoinsProcessor.MAX_ID_PARAM_NAME, 9001L);
        configurationService.set(CopyExpiredCoinsProcessor.CUR_ID_PARAM_NAME, 9001L);
        configurationService.set(CopyExpiredCoinsProcessor.MAX_ID_PARAM_NAME, 9001L);

        migrateOldCoinDataProcessor.migrateOldData(Duration.ofSeconds(1), 1, 50);

        configurationService.set(DeleteDuplicateCoinsProcessor.CUR_ID_PARAM_NAME, 0L);
        configurationService.set(DeleteDuplicateCoinsProcessor.MAX_ID_PARAM_NAME, 9001L);

        migrateOldCoinDataProcessor.migrateOldData(Duration.ofSeconds(1), 1, 50);

        assertThat(configurationService.get(CopyActiveCoinsProcessor.CUR_ID_PARAM_NAME), equalTo("9001"));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM discount", Integer.class), equalTo(2));
        assertThat(configurationService.get(DeleteDuplicateCoinsProcessor.CUR_ID_PARAM_NAME), equalTo("9001"));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM coin", Integer.class), equalTo(0));

    }
}
