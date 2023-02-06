package ru.yandex.market.loyalty.core.dao.coin;

import java.sql.Date;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoinProps;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinType;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.MSKU_ID;
import static ru.yandex.market.loyalty.core.rule.RuleType.MSKU_FILTER_RULE;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@Log4j2
public class CoinDaoTest extends MarketLoyaltyCoreMockedDbTestBase {

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CoinDao coinDao;
    @Autowired
    private Clock clock;

    private Promo promo;

    @Before
    public void setUp() throws Exception {
        promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        for (int i = 0; i < 100; i++) {
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
    public void testUnlinkCoinsMethod() {
        List<Long> range = LongStream.range(2000, 2099).boxed().collect(Collectors.toList());
        coinDao.unlinkActiveCoins(range);
        List<Coin.Builder> coins = coinDao.getCoins(range.stream().map(CoinKey::new).collect(Collectors.toSet()));
        assertThat(coins, everyItem(
                hasProperty("uid", lessThan(0L))
        ));
    }

    @Test
    public void testInsertCoinRuleParams() {
        final int chunkSize = 10_000;

        var values = LongStream.range(100_000_000_000L, 100_000_000_000L + chunkSize).boxed()
                .map(String::valueOf)
                .collect(Collectors.toSet());
        var prop = CoinProps.builder()
                .setPromoData(promo)
                .setType(CoreCoinType.PERCENT)
                .setExpirationPolicy(ExpirationPolicy.toEndOfPromo())
                .addRule(RuleContainer.builder(MSKU_FILTER_RULE)
                        .withParams(MSKU_ID, values)
                        .build())
                .build();
        long time = System.currentTimeMillis();
        long propsId = coinDao.saveCoinProps(prop);
        time = System.currentTimeMillis() - time;
        log.debug("Time to save chunk of {} entries: {} sec.", chunkSize, (time / 1_000F));

        Optional<CoinProps> props = coinDao.getCoinPropsByIds(Set.of(propsId)).get(propsId);
        assertTrue(props.isPresent());
        var actual = props.get().getRulesContainer().getAllRules().stream().findFirst().orElseThrow()
                .getParams(MSKU_ID);
        assertThat(actual, hasSize(chunkSize));
    }

}
