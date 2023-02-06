package ru.yandex.market.loyalty.core.dao.coin;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Clock;
import java.time.temporal.ChronoUnit;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.core.model.OperationContext;
import ru.yandex.market.loyalty.core.model.RevertSource;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.applicability.PromoApplicabilityPolicy;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.coin.SourceKey;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.service.discount.SpendMode;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_FEED_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ORDER_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.DEFAULT_COIN_FIXED_NOMINAL;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.ANOTHER_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class CoinDaoBackwardCompatibilityTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CoinDao coinDao;
    @Autowired
    private CoinService coinService;
    @Autowired
    private Clock clock;
    @Autowired
    private DiscountService discountService;
    @Autowired
    private PromoService promoService;

    private Promo promo;
    private static final long UNAUTH_COIN_ID = 9000L;
    private static final long AUTH_COIN_ID = 9001L;

    @Before
    public void init() {
        promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        jdbcTemplate.update(
                "INSERT INTO coin(" +
                        "id, uid, status, start_date, end_date, activation_token, coin_props_id, " +
                        "certificate_token, source_key, creation_time, retry, reason," +
                        "reason_param, require_auth, platform, promo_subtype) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, ?, ?, ?, ?, ?)",
                UNAUTH_COIN_ID, null, "ACTIVE", Date.from(promo.getStartDate().toInstant()),
                Date.from(clock.instant().plus(30, ChronoUnit.DAYS)), null,
                promo.getCoinPropsId(), null, "sourceKey-1",
                Date.from(clock.instant()), "OTHER", null, true, "BLUE", "COIN"
        );
        jdbcTemplate.update(
                "INSERT INTO coin_history(" +
                        "coin_id, record_type, coin_status, operation_context_id) " +
                        "VALUES (?, ?, ?, ?)",
                UNAUTH_COIN_ID,
                "CREATION",
                "ACTIVE",
                null
        );
        jdbcTemplate.update(
                "INSERT INTO coin(" +
                        "id, uid, status, start_date, end_date, activation_token, coin_props_id, " +
                        "certificate_token, source_key, creation_time, retry, reason," +
                        "reason_param, require_auth, platform, promo_subtype) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, ?, ?, ?, ?, ?)",
                AUTH_COIN_ID, DEFAULT_UID, "ACTIVE", Date.from(promo.getStartDate().toInstant()),
                Date.from(clock.instant().plus(30, ChronoUnit.DAYS)), null,
                promo.getCoinPropsId(), null, "sourceKey-2",
                Date.from(clock.instant()), "OTHER", null, false, "BLUE", "COIN"
        );
        jdbcTemplate.update(
                "INSERT INTO coin_history(" +
                        "coin_id, record_type, coin_status, operation_context_id) " +
                        "VALUES (?, ?, ?, ?)",
                AUTH_COIN_ID,
                "CREATION",
                "ACTIVE",
                null
        );
    }

    @Test
    @Ignore
    public void shouldBindCoin() {
        coinDao.bindCoins(
                coinService.search
                        .getCoins(ImmutableSet.of(new CoinKey(UNAUTH_COIN_ID))),
                DEFAULT_UID
        );

        assertThat(
                coinService.search
                        .getCoins(ImmutableSet.of(new CoinKey(UNAUTH_COIN_ID))),
                contains(allOf(
                        hasProperty("uid", equalTo(DEFAULT_UID)),
                        hasProperty("requireAuth", equalTo(false))
                ))
        );
        assertThat(
                jdbcTemplate.queryForObject("SELECT count(*) FROM discount", Integer.class),
                equalTo(0)
        );
    }

    @Test
    @Ignore
    public void shouldRebindCoin() {
        coinDao.bindAlreadyAuthorizedCoins(
                coinService.search
                        .getCoins(ImmutableSet.of(new CoinKey(AUTH_COIN_ID))),
                DEFAULT_UID,
                ANOTHER_UID
        );

        assertThat(
                coinService.search
                        .getCoins(ImmutableSet.of(new CoinKey(AUTH_COIN_ID))),
                contains(allOf(
                        hasProperty("uid", equalTo(ANOTHER_UID)),
                        hasProperty("requireAuth", equalTo(false))
                ))
        );
        assertThat(
                jdbcTemplate.queryForObject("SELECT count(*) FROM discount", Integer.class),
                equalTo(0)
        );
    }

    @Test
    @Ignore
    public void shouldInsertCoin() {
        coinService.create.insertNoAuthCoins(
                promo.getId(),
                ImmutableSet.of(new SourceKey("sourceKey-3")),
                CoreCoinCreationReason.OTHER
        );

        assertThat(
                jdbcTemplate.queryForObject("SELECT count(*) FROM coin", Integer.class),
                equalTo(3)
        );
        assertThat(
                jdbcTemplate.queryForObject("SELECT count(*) FROM discount", Integer.class),
                equalTo(1)
        );
    }

    @Test
    @Ignore
    public void shouldSpendCoin() {
        final MultiCartWithBundlesDiscountResponse response = discountService.spendDiscounts(
                DiscountRequestWithBundlesBuilder.builder(
                                orderRequestWithBundlesBuilder()
                                        .withOrderId(DEFAULT_ORDER_ID)
                                        .withOrderItem(
                                                itemKey(DEFAULT_FEED_ID, DEFAULT_ORDER_ID),
                                                price(1000)
                                        ).build())
                        .withCoins(new CoinKey(AUTH_COIN_ID))
                        .build(),
                SpendMode.SPEND,
                DiscountUtils.getRulesPayload(SpendMode.SPEND, PromoApplicabilityPolicy.ANY),
                PromoApplicabilityPolicy.ANY,
                null
        );

        assertThat(
                promoService.getPromo(promo.getId()).getSpentBudget(),
                comparesEqualTo(DEFAULT_COIN_FIXED_NOMINAL)
        );

        discountService.revertDiscount(
                ImmutableSet.of(response.getOrders().get(0).getItems().get(0).getPromos().get(0).getDiscountToken()),
                new OperationContext(),
                java.util.Date.from(clock.instant()),
                RevertSource.HTTP
        );

        assertThat(
                promoService.getPromo(promo.getId()).getSpentBudget(),
                comparesEqualTo(BigDecimal.ZERO)
        );
    }
}
