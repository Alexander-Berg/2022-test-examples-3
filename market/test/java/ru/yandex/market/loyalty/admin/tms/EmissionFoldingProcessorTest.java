package ru.yandex.market.loyalty.admin.tms;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.loyalty.admin.service.bunch.BunchRequestService;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.dao.budgeting.EmissionFoldingQueueDao;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName;
import ru.yandex.market.loyalty.core.dao.coin.CoinDao;
import ru.yandex.market.loyalty.core.dao.coin.GeneratorType;
import ru.yandex.market.loyalty.core.dao.query.Filter;
import ru.yandex.market.loyalty.core.dao.query.Limit;
import ru.yandex.market.loyalty.core.model.budgeting.EmissionFoldingRequestStatus;
import ru.yandex.market.loyalty.core.model.coin.BunchGenerationRequest;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.loyalty.admin.tms.CoinBunchRequestExecutor.MAX_DURATION;
import static ru.yandex.market.loyalty.admin.tms.CoinBunchRequestExecutor.WRITE_CHUNK_SIZE;
import static ru.yandex.market.loyalty.api.model.CoinGeneratorType.NO_AUTH;
import static ru.yandex.market.loyalty.api.model.TableFormat.CSV;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.COIN_GENERATOR_TYPE;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_EXPIRATION_DAYS;
import static ru.yandex.market.loyalty.spring.utils.DaoUtils.array;

@TestFor(EmissionFoldingProcessor.class)
public class EmissionFoldingProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String MAIL = "krosh@example.com";
    @Autowired
    private BunchRequestService bunchRequestService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CoinDao coinDao;
    @Autowired
    private BunchRequestProcessor bunchRequestProcessor;
    @Autowired
    private EmissionFoldingProcessor emissionFoldingProcessor;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CoinService coinService;
    @Autowired
    private EmissionFoldingQueueDao emissionFoldingQueueDao;

    @Test
    public void shouldGenerateCorrectAmount() throws InterruptedException {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed().setEmissionBudget(BigDecimal.valueOf(300_000)).setEmissionFolding(true)
        );

        bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        promo.getId(),
                        "coin1",
                        100,
                        MAIL,
                        CSV,
                        null,
                        GeneratorType.COIN,
                        ImmutableMap.<BunchGenerationRequestParamName<?>, String>builder()
                                .put(COIN_GENERATOR_TYPE, NO_AUTH.getCode())
                                .build()
                )
        );
        bunchRequestProcessor.coinBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);

        final long requestId = emissionFoldingQueueDao.submitRequest(promo.getId());
        emissionFoldingProcessor.process(1000, 10_000);

        List<Coin.Builder> createdCoins = coinDao.getCoinsByPromoId(promo.getId(), Filter.trueValue(), Limit.empty());
        assertThat(createdCoins, hasSize(100));

        assertThat(
                jdbcTemplate.queryForObject("" +
                                "SELECT COUNT(*)" +
                                "  FROM discount_history_x chx," +
                                "       discount_history ch " +
                                " WHERE ch.id = chx.discount_history_id" +
                                "   AND ch.discount_id = ANY(?::bigint[])",
                        Long.class,
                        array(createdCoins.stream()
                                .map(Coin.Builder::getCoinKey)
                                .mapToLong(CoinKey::getId)
                                .toArray()
                        )
                ),
                equalTo(0L)
        );

        assertThat(
                emissionFoldingQueueDao.getRequest(requestId),
                allOf(
                        hasProperty(
                                "status",
                                equalTo(EmissionFoldingRequestStatus.FOLDED)
                        )
                )
        );

        clock.spendTime(7, ChronoUnit.DAYS);
        clock.spendTime(1, ChronoUnit.MINUTES);

        emissionFoldingProcessor.recheck();

        assertThat(
                emissionFoldingQueueDao.getRequest(requestId),
                allOf(
                        hasProperty(
                                "status",
                                equalTo(EmissionFoldingRequestStatus.NOT_FOLDED)
                        )
                )
        );
    }

    @Test
    public void shouldExpireWorkOnFoldedCoins() throws InterruptedException {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed().setEmissionBudget(BigDecimal.valueOf(300_000)).setEmissionFolding(true)
        );

        bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        promo.getId(),
                        "coin1",
                        100,
                        MAIL,
                        CSV,
                        null,
                        GeneratorType.COIN,
                        ImmutableMap.<BunchGenerationRequestParamName<?>, String>builder()
                                .put(COIN_GENERATOR_TYPE, NO_AUTH.getCode())
                                .build()
                )
        );
        bunchRequestProcessor.coinBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);

        emissionFoldingQueueDao.submitRequest(promo.getId());
        emissionFoldingProcessor.process(0, 10_000);

        List<Coin.Builder> createdCoins = coinDao.getCoinsByPromoId(promo.getId(), Filter.trueValue(), Limit.empty());
        assertThat(createdCoins, hasSize(100));

        clock.spendTime(DEFAULT_EXPIRATION_DAYS + 1, ChronoUnit.DAYS);

        coinService.lifecycle.expireCoins(10_000, 0, 1000, true);

        assertThat(
                jdbcTemplate.queryForObject("" +
                                "SELECT COUNT(*)" +
                                "  FROM discount_history_x chx," +
                                "       discount_history ch " +
                                " WHERE ch.id = chx.discount_history_id" +
                                "   AND ch.discount_id = ANY(?::bigint[])",
                        Long.class,
                        array(createdCoins.stream()
                                .map(Coin.Builder::getCoinKey)
                                .mapToLong(CoinKey::getId)
                                .toArray()
                        )
                ),
                equalTo(0L)
        );

        assertThat(
                jdbcTemplate.queryForObject("" +
                                "SELECT COUNT(*) filter (where ch.discount_status = '" + CoreCoinStatus.EXPIRED.getCode() + "')" +
                                "  FROM discount_history ch " +
                                " WHERE ch.discount_id = ANY(?::bigint[])",
                        Long.class,
                        array(createdCoins.stream()
                                .map(Coin.Builder::getCoinKey)
                                .mapToLong(CoinKey::getId)
                                .toArray()
                        )
                ),
                equalTo(100L)
        );
    }

    @Test
    public void shouldWorkOnExpiredCoins() throws InterruptedException {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed().setEmissionBudget(BigDecimal.valueOf(300_000)).setEmissionFolding(true)
        );

        bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        promo.getId(),
                        "coin1",
                        100,
                        MAIL,
                        CSV,
                        null,
                        GeneratorType.COIN,
                        ImmutableMap.<BunchGenerationRequestParamName<?>, String>builder()
                                .put(COIN_GENERATOR_TYPE, NO_AUTH.getCode())
                                .build()
                )
        );
        bunchRequestProcessor.coinBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);

        clock.spendTime(DEFAULT_EXPIRATION_DAYS + 1, ChronoUnit.DAYS);

        coinService.lifecycle.expireCoins(10_000, 0, 0, true);

        emissionFoldingQueueDao.submitRequest(promo.getId());
        emissionFoldingProcessor.process(0, 10_000);

        List<Coin.Builder> createdCoins = coinDao.getCoinsByPromoId(promo.getId(), Filter.trueValue(), Limit.empty());
        assertThat(createdCoins, hasSize(100));

        assertThat(
                jdbcTemplate.queryForObject("" +
                                "SELECT COUNT(*)" +
                                "  FROM discount_history_x chx," +
                                "       discount_history ch " +
                                " WHERE ch.id = chx.discount_history_id" +
                                "   AND ch.discount_id = ANY(?::bigint[])",
                        Long.class,
                        array(createdCoins.stream()
                                .map(Coin.Builder::getCoinKey)
                                .mapToLong(CoinKey::getId)
                                .toArray()
                        )
                ),
                equalTo(0L)
        );
    }

    @Test
    public void shouldCorrectHandlePartial() throws InterruptedException {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed().setEmissionBudget(BigDecimal.valueOf(300_000)).setEmissionFolding(true)
        );

        bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        promo.getId(),
                        "coin1",
                        8,
                        MAIL,
                        CSV,
                        null,
                        GeneratorType.COIN,
                        ImmutableMap.<BunchGenerationRequestParamName<?>, String>builder()
                                .put(COIN_GENERATOR_TYPE, NO_AUTH.getCode())
                                .build()
                )
        );
        bunchRequestProcessor.coinBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);

        emissionFoldingQueueDao.submitRequest(promo.getId());
        emissionFoldingProcessor.process(0, 3);

        List<Coin.Builder> createdCoins = coinDao.getCoinsByPromoId(promo.getId(), Filter.trueValue(), Limit.empty());
        assertThat(createdCoins, hasSize(8));

        assertThat(
                jdbcTemplate.queryForObject("" +
                                "SELECT COUNT(*)" +
                                "  FROM discount_history_x chx," +
                                "       discount_history ch " +
                                " WHERE ch.id = chx.discount_history_id" +
                                "   AND ch.discount_id = ANY(?::bigint[])",
                        Long.class,
                        array(createdCoins.stream()
                                .map(Coin.Builder::getCoinKey)
                                .mapToLong(CoinKey::getId)
                                .toArray()
                        )
                ),
                greaterThan(0L)
        );
    }
}
