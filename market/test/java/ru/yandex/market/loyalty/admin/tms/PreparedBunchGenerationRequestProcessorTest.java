package ru.yandex.market.loyalty.admin.tms;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.loyalty.admin.service.bunch.BunchRequestService;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.utils.YtTestHelper;
import ru.yandex.market.loyalty.admin.yt.YtClient;
import ru.yandex.market.loyalty.api.model.CoinGeneratorType;
import ru.yandex.market.loyalty.api.model.TableFormat;
import ru.yandex.market.loyalty.core.config.YtHahn;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDao;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestDao;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestStatus;
import ru.yandex.market.loyalty.core.dao.coin.GeneratorType;
import ru.yandex.market.loyalty.core.model.coin.BunchGenerationRequest;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletNewTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionPriority;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.qe.yt.cypress.objects.YTMap;

import java.time.Duration;
import java.util.List;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.AllOf.allOf;
import static ru.yandex.market.loyalty.admin.utils.YtTestHelper.createWalletTopUps;
import static ru.yandex.market.loyalty.api.model.CoinGeneratorType.NO_AUTH;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.*;
import static ru.yandex.market.loyalty.core.dao.coin.GeneratorType.COIN;
import static ru.yandex.market.loyalty.core.dao.coin.GeneratorType.YANDEX_WALLET;


public class PreparedBunchGenerationRequestProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private BunchRequestService bunchRequestService;
    @Autowired
    private BunchGenerationRequestDao bunchGenerationRequestDao;
    @Autowired
    private BunchRequestProcessor bunchRequestProcessor;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    @YtHahn
    private JdbcTemplate jdbcTemplate;
    @Autowired
    @YtHahn
    private YtClient ytClient;
    @Autowired
    private YtTestHelper ytTestHelper;
    @Autowired
    private YandexWalletTransactionDao yandexWalletTransactionDao;
    @Autowired
    private PromoUtils promoUtils;

    private static final String TEST_CAMPAIGN = "test_campaign";
    private static final String YANDEX_WALLET_TEST_KEY = "testWalletKey";

    @Test
    public void shouldExportCreatedCoinsToYt() {
        final Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        final String requestKey = "coin";

        final long requestId = bunchRequestService.scheduleRequest(createTestScheduledRequest(promo.getId(),
                requestKey, COIN,
                NO_AUTH, 10
        ));

        bunchRequestProcessor.coinBunchRequestProcess(
                500,
                Duration.of(1, MINUTES)
        );

        ytTestHelper.mockYtCoinExportedTablesIds(jdbcTemplate, promo);
        YtTestHelper.mockYtObjectsExistence(ytClient, bunchRequestService.getRequest(requestId));


        ytTestHelper.mockYtClientAttributes(
                a -> a.toString().startsWith("//tmp/market-promo-test/temp/"),
                ImmutableMap.<String, YTreeNode>builder()
                        .put("row_count", YTree.builder().value(10).build())
                        .build()
        );
        ytTestHelper.mockYtMergeOperation();

        bunchRequestProcessor.processPreparedRequests(
                Duration.of(1, MINUTES)
        );

        assertThat(
                bunchRequestService.getRequest(requestId),
                allOf(
                        hasProperty(
                                "status",
                                equalTo(BunchGenerationRequestStatus.PROCESSED)
                        )
                )
        );
    }

    @Test
    public void shouldExportCreatedWalletAccrualsToYt() {
        final Promo promo = promoUtils.buildWalletAccrualPromo(PromoUtils.WalletAccrual.defaultAccrual());

        final long requestId = mockWalletAccrualPreparedRequest(promo, createWalletTopUps(10));

        ytTestHelper.mockYtObjectsExistence(ytClient, bunchRequestService.getRequest(requestId));


        ytTestHelper.mockYtClientAttributes(
                a -> a.toString().startsWith("//tmp/market-promo-test/temp/"),
                ImmutableMap.<String, YTreeNode>builder()
                        .put("row_count", YTree.builder().value(10).build())
                        .build()
        );
        ytTestHelper.mockYtMergeOperation();

        bunchRequestProcessor.processPreparedRequests(
                Duration.of(1, MINUTES)
        );

        assertThat(
                bunchRequestService.getRequest(requestId),
                allOf(
                        hasProperty(
                                "status",
                                equalTo(BunchGenerationRequestStatus.PROCESSED)
                        )
                )
        );
    }

    private long mockWalletAccrualPreparedRequest(Promo promo, List<YandexWalletNewTransaction> walletTopUps) {
        final long requestId = bunchRequestService.scheduleRequest(
                createTestScheduledRequest(
                        promo.getId(),
                        YANDEX_WALLET_TEST_KEY,
                        GeneratorType.YANDEX_WALLET,
                        null,
                        100
                )
        );
        yandexWalletTransactionDao.enqueueTransactions(requestId, TEST_CAMPAIGN,
                walletTopUps, YandexWalletTransactionPriority.LOW);
        for (YandexWalletTransaction t : yandexWalletTransactionDao.queryAll()) {
            yandexWalletTransactionDao.updateStatus(
                    t.getId(), t.getStatus(), YandexWalletTransactionStatus.CONFIRMED, null);
        }
        bunchGenerationRequestDao.updateRequestStatus(requestId, BunchGenerationRequestStatus.PREPARED);
        return requestId;
    }

    @NotNull
    public static BunchGenerationRequest createTestScheduledRequest(
            Long promoId, String requestKey, GeneratorType generatorType, CoinGeneratorType coinGeneratorType, int count
    ) {
        final ImmutableMap.Builder<BunchGenerationRequestParamName<?>, String> params =
                ImmutableMap.<BunchGenerationRequestParamName<?>, String>builder()
                .put(OUTPUT_TABLE, "//output/outputTable")
                .put(OUTPUT_TABLE_CLUSTER, "hahn")
                .put(ERROR_OUTPUT_TABLE, "//output/errorTable")
                .put(CAMPAIGN_NAME, "test_camp_name");

        if (generatorType == COIN) {
            params
                    .put(COIN_GENERATOR_TYPE, coinGeneratorType.getCode());
        }

        if (generatorType == YANDEX_WALLET || (generatorType == COIN && coinGeneratorType != NO_AUTH)) {
            params
                    .put(INPUT_TABLE, "//input/someInputTable")
                    .put(INPUT_TABLE_CLUSTER, "hahn")
                    .put(PRODUCT_ID, "test_product_id");
        }

        return BunchGenerationRequest.scheduled(
                promoId,
                requestKey,
                count,
                null,
                TableFormat.YT,
                null,
                generatorType,
                params.build()
        );
    }

    @Test
    @Ignore
    public void shouldExportCoinErrorsToYt() {
        // в задаче https://st.yandex-team.ru/MARKETDISCOUNT-2691
    }

    @Test
    @Ignore
    public void shouldRecoverExportCreatedCoinsToYt() {
        // в задаче https://st.yandex-team.ru/MARKETDISCOUNT-2691
    }
}
