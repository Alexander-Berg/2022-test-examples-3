package ru.yandex.market.loyalty.admin.tms;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.loyalty.admin.service.bunch.BunchRequestService;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.utils.YtTestHelper;
import ru.yandex.market.loyalty.api.model.coin.creation.CoinCreationError;
import ru.yandex.market.loyalty.api.model.promogroup.PromoGroupType;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestDao;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName;
import ru.yandex.market.loyalty.core.dao.coin.CoinDao;
import ru.yandex.market.loyalty.core.dao.coin.GeneratorType;
import ru.yandex.market.loyalty.core.dao.custom.coin.BunchGenerationErrorStatDao;
import ru.yandex.market.loyalty.core.dao.query.Filter;
import ru.yandex.market.loyalty.core.model.coin.BunchGenerationRequest;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promogroup.PromoGroupImpl;
import ru.yandex.market.loyalty.core.model.promogroup.PromoGroupPromo;
import ru.yandex.market.loyalty.core.service.BunchGenerationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.mail.Attachment;
import ru.yandex.market.loyalty.core.service.mail.YabacksMailer;
import ru.yandex.market.loyalty.core.service.promogroup.PromoGroupService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.loyalty.admin.tms.CoinBunchRequestExecutor.MAX_DURATION;
import static ru.yandex.market.loyalty.admin.tms.CoinBunchRequestExecutor.WRITE_CHUNK_SIZE;
import static ru.yandex.market.loyalty.api.model.CoinGeneratorType.AUTH;
import static ru.yandex.market.loyalty.api.model.CoinGeneratorType.NO_AUTH;
import static ru.yandex.market.loyalty.api.model.TableFormat.CSV;
import static ru.yandex.market.loyalty.api.model.TableFormat.NONE;
import static ru.yandex.market.loyalty.api.model.TableFormat.YT;
import static ru.yandex.market.loyalty.api.model.coin.creation.CoinCreationError.COIN_ALREADY_CREATED;
import static ru.yandex.market.loyalty.api.model.coin.creation.CoinCreationError.EMISSION_BUDGET_EXCEEDED;
import static ru.yandex.market.loyalty.api.model.coin.creation.CoinCreationError.USER_IN_BLACKLIST;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.BUNCH_REQUEST_LAST_PROCESSED_UID;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.COIN_GENERATOR_TYPE;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.ERROR_OUTPUT_TABLE;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.IGNORE_BUDGET_EXHAUSTION;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.INPUT_TABLE;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.INPUT_TABLE_CLUSTER;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.OUTPUT_TABLE;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.OUTPUT_TABLE_CLUSTER;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestStatus.CANCELLED;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestStatus.IN_QUEUE;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestStatus.PREPARED;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestStatus.PROCESSED;
import static ru.yandex.market.loyalty.core.dao.coin.CoinDao.DISCOUNT_TABLE;
import static ru.yandex.market.loyalty.lightweight.DateUtils.fromDate;
import static ru.yandex.market.loyalty.lightweight.DateUtils.toDate;

@TestFor(BunchRequestProcessor.class)
public class CoinBunchGenerationRequestProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String MAIL = "krosh@example.com";
    private static final Map<Long, CoinCreationError> REQUEST_ERRORS =
            Map.of(1L, COIN_ALREADY_CREATED, 2L, USER_IN_BLACKLIST);
    @Autowired
    private BunchRequestService bunchRequestService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private YabacksMailer yabacksMailer;
    @Autowired
    private CoinDao coinDao;
    @Autowired
    private BunchGenerationRequestDao coinBunchRequestDao;
    @Autowired
    private BunchGenerationErrorStatDao coinBunchErrorStatDao;
    @Autowired
    private BunchRequestProcessor bunchRequestProcessor;
    @Autowired
    private BunchRequestService coinBunchRequestService;
    @Autowired
    private YtTestHelper ytTestHelper;
    @Autowired
    private PromoGroupService promoGroupService;
    @Autowired
    private BunchGenerationService coinBunchService;

    @Test
    public void shouldSendEmailOnce() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        promo.getPromoId().getId(),
                        "coin",
                        10,
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
        bunchRequestProcessor.processPreparedRequests(Duration.of(1, MINUTES));
        bunchRequestProcessor.coinBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);
        bunchRequestProcessor.processPreparedRequests(Duration.of(1, MINUTES));

        verify(yabacksMailer, times(1)).sendMail(eq(MAIL), anyString(), anyString(), any(Attachment.class));
    }

    @Test
    public void shouldSendErrorEmailAfterAllRetries() {
        final long unrealPromo = 100L;
        long requestId = bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        unrealPromo,
                        "coin",
                        10,
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
        assertRequestInQueue(requestId);
        verify(yabacksMailer, never()).sendMail(eq(MAIL), anyString(), anyString(), any(Attachment.class));

        bunchRequestProcessor.coinBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);
        assertRequestInQueue(requestId);
        verify(yabacksMailer, never()).sendMail(eq(MAIL), anyString(), anyString(), any(Attachment.class));

        bunchRequestProcessor.coinBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);
        assertRequestDismissed(requestId);

        verify(yabacksMailer, times(1)).sendMail(eq(MAIL), anyString(), anyString(), any(Attachment.class));
        // потушить монитор
        coinBunchRequestDao.markRequestCompleted(requestId);
    }

    @Test
    public void shouldSendGenerateCorrectAmount() {
        Promo promo = createCoinPromo(300_000);

        bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        promo.getPromoId().getId(),
                        "coin",
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
        bunchRequestProcessor.coinBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);

        int count = coinDao.getCoinsCount(DISCOUNT_TABLE.sourceKey.like("%coin%"));
        assertEquals(100, count);
    }

    @Test
    public void shouldGenerateCorrectAmount() {
        Promo promo = createCoinPromo(300_000);

        bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        promo.getPromoId().getId(),
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
        bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        promo.getPromoId().getId(),
                        "coin2",
                        155,
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

        int count = coinDao.getCoinsCount(DISCOUNT_TABLE.sourceKey.like("%coin%"));
        assertEquals(255, count);
    }

    @Test
    public void shouldIgnoreBudgetExhaustion_FixMarketdiscount5578() {
        Promo promo = createCoinPromo(10);
        final long requestId = generateCoins(promo, 100, 10, true);

        assertGeneratedCoinsCount(requestId, 10);
        assertRequestPrepared(requestId);
    }

    @Test
    public void shouldSendStartAndCompleteEmail() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        promo.getPromoId().getId(),
                        "coin",
                        11,
                        MAIL,
                        CSV,
                        null,
                        GeneratorType.COIN,
                        ImmutableMap.<BunchGenerationRequestParamName<?>, String>builder()
                                .put(COIN_GENERATOR_TYPE, NO_AUTH.getCode())
                                .build()
                )
        );
        verify(yabacksMailer, times(1)).sendMail(anyString(), anyString(), anyString());

        bunchRequestProcessor.coinBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);
        bunchRequestProcessor.processPreparedRequests(Duration.of(1, MINUTES));
        verify(yabacksMailer, times(1)).sendMail(anyString(), anyString(), anyString(), any(Attachment.class));
    }

    @Test
    public void shouldCompleteRequestWithNoneOutput() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        final long id = bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        promo.getPromoId().getId(),
                        "coin",
                        10,
                        null,
                        NONE,
                        null,
                        GeneratorType.COIN,
                        ImmutableMap.<BunchGenerationRequestParamName<?>, String>builder()
                                .put(COIN_GENERATOR_TYPE, NO_AUTH.getCode())
                                .build()
                )
        );

        bunchRequestProcessor.coinBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);
        bunchRequestProcessor.processPreparedRequests(Duration.of(1, MINUTES));

        int created = coinDao.getCoinsCount(DISCOUNT_TABLE.sourceKey.like("%coin%"));
        assertEquals(10, created);

        final BunchGenerationRequest request = coinBunchRequestDao.getRequest(id).build();

        assertEquals(request.getStatus(), PROCESSED);
    }

    @Test
    public void shouldUploadAndCompleteAuth() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());
        int count = 6;
        final long id = bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        promo.getPromoId().getId(),
                        "coin",
                        count,
                        null,
                        YT,
                        null,
                        GeneratorType.COIN,
                        ImmutableMap.<BunchGenerationRequestParamName<?>, String>builder()
                                .put(COIN_GENERATOR_TYPE, AUTH.getCode())
                                .put(INPUT_TABLE, "//input/input_table")
                                .put(INPUT_TABLE_CLUSTER, "hahn")
                                .put(OUTPUT_TABLE, "//output/output_table")
                                .put(OUTPUT_TABLE_CLUSTER, "hahn")
                                .put(ERROR_OUTPUT_TABLE, "//output/error_output_table")
                                .build()
                )
        );

        List<Long> uids = Arrays.asList(
                100000L, 100001L, 100002L, 100003L, 100004L, 100005L
        );
        ytTestHelper.mockYtInputTableReads(
                YPath.simple("//input/input_table"),
                uids
        );

        bunchRequestProcessor.coinBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);

        ytTestHelper.mockYtClientAttributes(
                a -> a.toString().startsWith("//tmp/market-promo-test/temp/"),
                ImmutableMap.<String, YTreeNode>builder()
                        .put("row_count", YTree.builder().value(6).build())
                        .build()
        );
        ytTestHelper.mockYtMergeOperation();

        BunchGenerationRequest preparedReq = coinBunchRequestService.getRequest(id);
        assertEquals(preparedReq.getStatus(), PREPARED);
        assertEquals(preparedReq.getParam(BUNCH_REQUEST_LAST_PROCESSED_UID).orElseThrow(), uids.get(uids.size() - 1));

        bunchRequestProcessor.processPreparedRequests(Duration.of(1, MINUTES));

        assertEquals(coinBunchRequestService.getRequest(id).getStatus(), PROCESSED);
    }

    @Test
    public void shouldRevokeOnCancelRequest() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        final long id = bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        promo.getPromoId().getId(),
                        "coin",
                        10,
                        null,
                        YT,
                        null,
                        GeneratorType.COIN,
                        ImmutableMap.<BunchGenerationRequestParamName<?>, String>builder()
                                .put(COIN_GENERATOR_TYPE, NO_AUTH.getCode())
                                .put(OUTPUT_TABLE, "//output/output_table")
                                .put(OUTPUT_TABLE_CLUSTER, "hahn")
                                .put(ERROR_OUTPUT_TABLE, "//output/error_output_table")
                                .build()
                )
        );

        bunchRequestProcessor.coinBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);

        assertEquals(coinBunchRequestService.getRequest(id).getStatus(), PREPARED);

        coinBunchService.cancelRequest("coin");

        bunchRequestProcessor.finalizeCancellingRequests(Duration.of(1, MINUTES), 1);

        assertEquals(coinBunchRequestService.getRequest(id).getStatus(), CANCELLED);

        assertEquals(10, coinDao.getCoinsCount(Filter.and(
                DISCOUNT_TABLE.sourceKey.like("%coin%"),
                DISCOUNT_TABLE.status.eqTo(CoreCoinStatus.REVOKED)
                )
        ));
    }

    @Test
    public void shouldUploadAndCompleteNoAuth() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        final long id = bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        promo.getPromoId().getId(),
                        "coin",
                        6,
                        null,
                        YT,
                        null,
                        GeneratorType.COIN,
                        ImmutableMap.<BunchGenerationRequestParamName<?>, String>builder()
                                .put(COIN_GENERATOR_TYPE, NO_AUTH.getCode())
                                .put(OUTPUT_TABLE, "//output/output_table")
                                .put(OUTPUT_TABLE_CLUSTER, "hahn")
                                .put(ERROR_OUTPUT_TABLE, "//output/error_output_table")
                                .build()
                )
        );

        bunchRequestProcessor.coinBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);

        ytTestHelper.mockYtClientAttributes(
                a -> a.toString().startsWith("//tmp/market-promo-test/temp/"),
                ImmutableMap.<String, YTreeNode>builder()
                        .put("row_count", YTree.builder().value(6).build())
                        .build()
        );
        ytTestHelper.mockYtMergeOperation();

        assertEquals(coinBunchRequestService.getRequest(id).getStatus(), PREPARED);

        bunchRequestProcessor.processPreparedRequests(Duration.of(1, MINUTES));

        assertEquals(coinBunchRequestService.getRequest(id).getStatus(), PROCESSED);
    }

    @Test
    public void shouldProcessAccordingMaxChunk() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        int requested = 10000000;
        bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        promo.getPromoId().getId(),
                        "coin",
                        requested,
                        MAIL,
                        CSV,
                        null,
                        GeneratorType.COIN,
                        ImmutableMap.<BunchGenerationRequestParamName<?>, String>builder()
                                .put(COIN_GENERATOR_TYPE, NO_AUTH.getCode())
                                .build()
                )
        );
        int chunkSize = 10;
        bunchRequestProcessor.coinBunchRequestProcess(chunkSize, Duration.ofMillis(200));
        int created = coinDao.getCoinsCount(DISCOUNT_TABLE.sourceKey.like("%coin%"));
        assertTrue(created < requested);
        assertEquals(0, created % chunkSize);
    }

    @Test
    public void shouldCreateErrorsForProblemUids() {
        final int coinsCount = 100;
        final int expectedErrorsCount = 1;

        Promo promo = createCoinPromo(coinsCount - expectedErrorsCount);

        bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        promo.getPromoId().getId(),
                        "coin",
                        coinsCount,
                        MAIL,
                        CSV,
                        null,
                        GeneratorType.COIN,
                        ImmutableMap.<BunchGenerationRequestParamName<?>, String>builder()
                                .put(COIN_GENERATOR_TYPE, AUTH.getCode())
                                .put(INPUT_TABLE, "//input/input")
                                .put(INPUT_TABLE_CLUSTER, "hahn")
                                .build()
                )
        );

        final YPath inputFile = YPath.simple("//input/input");
        final List<Long> uids = LongStream.range(0, coinsCount).boxed().collect(Collectors.toList());
        ytTestHelper.mockYtInputTable("//input", "//input/input", uids);
        ytTestHelper.mockYtInputTableReads(inputFile, uids);

        bunchRequestProcessor.coinBunchRequestProcess(100, Duration.ofMillis(200));

        assertEquals(
                coinsCount - expectedErrorsCount,
                coinDao.getCoinsCount(DISCOUNT_TABLE.sourceKey.like("%coin%"))
        );

        assertThat(
                coinBunchErrorStatDao.getRequestErrors("coin"),
                allOf(hasEntry(EMISSION_BUDGET_EXCEEDED, 1))
        );
    }

    @Test
    public void shouldSkipBudgetExhaustion() {
        final int coinsCount = 100;

        Promo promo = createCoinPromo(1);

        final long id = bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        promo.getPromoId().getId(),
                        "coin",
                        coinsCount,
                        MAIL,
                        CSV,
                        null,
                        GeneratorType.COIN,
                        ImmutableMap.<BunchGenerationRequestParamName<?>, String>builder()
                                .put(COIN_GENERATOR_TYPE, AUTH.getCode())
                                .put(INPUT_TABLE, "//input/input")
                                .put(INPUT_TABLE_CLUSTER, "hahn")
                                .put(IGNORE_BUDGET_EXHAUSTION, "true")
                                .build()
                )
        );

        final YPath inputFile = YPath.simple("//input/input");
        final List<Long> uids = LongStream.range(0, coinsCount).boxed().collect(Collectors.toList());
        ytTestHelper.mockYtInputTable("//input", "//input/input", uids);
        ytTestHelper.mockYtInputTableReads(inputFile, uids);

        bunchRequestProcessor.coinBunchRequestProcess(100, Duration.ofMillis(200));

        assertEquals(
                1,
                coinDao.getCoinsCount(DISCOUNT_TABLE.sourceKey.like("%coin%"))
        );

        assertThat(
                coinBunchErrorStatDao.getRequestErrors("coin"),
                anEmptyMap()
        );

        assertEquals(bunchRequestService.getRequest(id).getStatus(), PREPARED);
    }

    @Test
    public void shouldCorrectProcessRequestsWithFixMarketdiscount3510() {
        Promo yandexPlusPromo1 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );

        Promo yandexPlusPromo2 = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );

        createYandexPlusPromoGroup(
                PromoGroupType.YANDEX_PLUS,
                "Yandex Plus",
                "yandex_plus_token",
                yandexPlusPromo1,
                yandexPlusPromo2
        );

        final long id1 = scheduleRequest(
                yandexPlusPromo1,
                "coin1",
                10,
                "//input",
                "//input/input1",
                i -> (long) i
        );

        final long id2 = scheduleRequest(
                yandexPlusPromo2,
                "coin2",
                10,
                "//input",
                "//input/input2",
                i -> (long) i
        );

        bunchRequestProcessor.coinBunchRequestProcess(100, Duration.ofMillis(200));

        assertEquals(
                10,
                coinDao.getCoinsCount(DISCOUNT_TABLE.sourceKey.like("%coin1%"))
        );

        assertEquals(
                10,
                coinDao.getCoinsCount(DISCOUNT_TABLE.sourceKey.like("%coin2%"))
        );

        assertThat(
                coinBunchErrorStatDao.getRequestErrors("coin1"),
                anEmptyMap()
        );

        assertThat(
                coinBunchErrorStatDao.getRequestErrors("coin2"),
                anEmptyMap()
        );

        assertEquals(bunchRequestService.getRequest(id1).getStatus(), PREPARED);
        assertEquals(bunchRequestService.getRequest(id2).getStatus(), PREPARED);
    }

    @Test
    public void shouldCleanAdditionalData() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        long promoId = promo.getPromoId().getId();
        final long requestId = scheduleRequest(promoId);

        bunchRequestProcessor.coinBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);
        bunchRequestProcessor.processPreparedRequests(Duration.of(1, MINUTES));
        coinBunchRequestDao.saveErrorUidsToRetryLater(requestId, REQUEST_ERRORS);

        BunchGenerationRequest request = coinBunchRequestDao.getRequest(requestId).build();
        int limit = request.getCount();
        assertFalse(request.isAdditionalDataCleaned());
        assertEquals(limit, coinBunchRequestDao.getCoinBunchGenerationRequestIndexByThis(request, limit).size());
        assertEquals(REQUEST_ERRORS.size(), coinBunchRequestDao.getCoinBunchGenerationRequestErrorsByThis(request, limit).size());

        Date endDate = coinDao.getCoinPropsPrototypeByPromoId(promoId).get()
                .getExpirationPolicy().calculateEmissionEndDateForPromocode(promo.getEndDate().toInstant());
        clock.setDate(toDate(fromDate(endDate).plusDays(10)));

        bunchRequestProcessor.cleanAdditionalData(Duration.of(1, MINUTES));

        request = coinBunchRequestDao.getRequest(requestId).build();

        assertTrue(request.isAdditionalDataCleaned());
        assertTrue(coinBunchRequestDao.getCoinBunchGenerationRequestIndexByThis(request, limit).isEmpty());
        assertTrue(coinBunchRequestDao.getCoinBunchGenerationRequestErrorsByThis(request, limit).isEmpty());
    }

    @Test
    public void shouldNotCleanAdditionalDataIfStatusNotPROCESSED() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        long promoId = promo.getPromoId().getId();
        final long requestId = scheduleRequest(promoId);

        bunchRequestProcessor.coinBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);
        coinBunchRequestDao.saveErrorUidsToRetryLater(requestId, REQUEST_ERRORS);

        BunchGenerationRequest request = coinBunchRequestDao.getRequest(requestId).build();
        int limit = request.getCount();
        assertFalse(request.isAdditionalDataCleaned());
        assertEquals(limit, coinBunchRequestDao.getCoinBunchGenerationRequestIndexByThis(request, limit).size());
        assertEquals(REQUEST_ERRORS.size(), coinBunchRequestDao.getCoinBunchGenerationRequestErrorsByThis(request, limit).size());

        Date endDate = coinDao.getCoinPropsPrototypeByPromoId(promoId).get()
                .getExpirationPolicy().calculateEmissionEndDateForPromocode(promo.getEndDate().toInstant());
        clock.setDate(toDate(fromDate(endDate).plusDays(10)));

        bunchRequestProcessor.cleanAdditionalData(Duration.of(1, MINUTES));

        request = coinBunchRequestDao.getRequest(requestId).build();

        assertFalse(request.isAdditionalDataCleaned());
        assertFalse(coinBunchRequestDao.getCoinBunchGenerationRequestIndexByThis(request, limit).isEmpty());
        assertFalse(coinBunchRequestDao.getCoinBunchGenerationRequestErrorsByThis(request, limit).isEmpty());
    }

    @Test
    public void shouldNotCleanAdditionalDataNotFinishEndDate() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        long promoId = promo.getPromoId().getId();
        final long requestId = scheduleRequest(promoId);

        bunchRequestProcessor.coinBunchRequestProcess(WRITE_CHUNK_SIZE, MAX_DURATION);
        bunchRequestProcessor.processPreparedRequests(Duration.of(1, MINUTES));
        coinBunchRequestDao.saveErrorUidsToRetryLater(requestId, REQUEST_ERRORS);

        BunchGenerationRequest request = coinBunchRequestDao.getRequest(requestId).build();
        int limit = request.getCount();
        assertFalse(request.isAdditionalDataCleaned());
        assertEquals(limit, coinBunchRequestDao.getCoinBunchGenerationRequestIndexByThis(request, limit).size());
        assertEquals(REQUEST_ERRORS.size(), coinBunchRequestDao.getCoinBunchGenerationRequestErrorsByThis(request, limit).size());

        bunchRequestProcessor.cleanAdditionalData(Duration.of(1, MINUTES));

        request = coinBunchRequestDao.getRequest(requestId).build();

        assertFalse(request.isAdditionalDataCleaned());
        assertFalse(coinBunchRequestDao.getCoinBunchGenerationRequestIndexByThis(request, limit).isEmpty());
        assertFalse(coinBunchRequestDao.getCoinBunchGenerationRequestErrorsByThis(request, limit).isEmpty());
    }

    private Promo createCoinPromo(int initialEmissionBudget) {
        return promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed().setEmissionBudget(BigDecimal.valueOf(initialEmissionBudget))
        );
    }

    private Long scheduleRequest(long promoId) {
        return bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        promoId,
                        "coin",
                        10,
                        null,
                        NONE,
                        null,
                        GeneratorType.COIN,
                        ImmutableMap.<BunchGenerationRequestParamName<?>, String>builder()
                                .put(COIN_GENERATOR_TYPE, NO_AUTH.getCode())
                                .build()
                )
        );
    }

    @SuppressWarnings("SameParameterValue")
    private long generateCoins(Promo promo, int count, int batchSize, Boolean ignoreBudgetExhaustion) {
        final long requestId = bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        promo.getPromoId().getId(),
                        "coin1",
                        count,
                        MAIL,
                        CSV,
                        null,
                        GeneratorType.COIN,
                        ImmutableMap.<BunchGenerationRequestParamName<?>, String>builder()
                                .put(COIN_GENERATOR_TYPE, NO_AUTH.getCode())
                                .put(IGNORE_BUDGET_EXHAUSTION, ignoreBudgetExhaustion.toString())
                                .build()
                )
        );
        bunchRequestProcessor.coinBunchRequestProcess(batchSize, MAX_DURATION);
        return requestId;
    }

    private long scheduleRequest(
            Promo yandexPlusPromo1, String key, int coinsCount, String inputFileBasePath, String inputFilePath,
            Function<Integer, Long> uidProducer
    ) {
        final long id = bunchRequestService.scheduleRequest(
                BunchGenerationRequest.scheduled(
                        yandexPlusPromo1.getId(),
                        key,
                        coinsCount,
                        null,
                        null,
                        null,
                        GeneratorType.COIN,
                        ImmutableMap.<BunchGenerationRequestParamName<?>, String>builder()
                                .put(COIN_GENERATOR_TYPE, AUTH.getCode())
                                .put(INPUT_TABLE, inputFilePath)
                                .put(INPUT_TABLE_CLUSTER, "hahn")
                                .build()
                )
        );

        final YPath inputFile = YPath.simple(inputFilePath);
        final List<Long> uids = IntStream.range(0, coinsCount).boxed().map(uidProducer).collect(Collectors.toList());
        ytTestHelper.mockYtInputTable(inputFileBasePath, inputFilePath, uids);
        ytTestHelper.mockYtInputTableReads(inputFile, uids);
        return id;
    }

    private void createYandexPlusPromoGroup(
            PromoGroupType promoGroupType, String name, String token, Promo... promos
    ) {
        long promoGroupId = promoGroupService.insertOrUpdatePromoGroupReturningId(
                PromoGroupImpl.builder()
                        .setName(name)
                        .setToken(token)
                        .setPromoGroupType(promoGroupType)
                        .setStartDate(clock.dateTime())
                        .setEndDate(clock.dateTime().plus(1, ChronoUnit.MONTHS))
                        .build()
        );
        promoGroupService.replacePromoGroupPromos(
                promoGroupId,
                IntStream.range(0, promos.length)
                        .boxed()
                        .map(i ->
                                new PromoGroupPromo(
                                        null,
                                        promoGroupId,
                                        promos[i].getId(),
                                        i)
                        )
                        .collect(Collectors.toList())
        );
    }

    @SuppressWarnings("unused")
    private void assertRequestDismissed(long requestId) {
        List<BunchGenerationRequest> request = coinBunchRequestService.getScheduledRequests(GeneratorType.COIN, 10);
        assertThat(request, empty());
    }

    private void assertRequestInQueue(long requestId) {
        BunchGenerationRequest request = coinBunchRequestService.getRequest(requestId);
        assertThat(request, allOf(
                hasProperty("id", equalTo(requestId)),
                hasProperty("status", equalTo(IN_QUEUE))
        ));
    }

    private void assertRequestPrepared(long requestId) {
        BunchGenerationRequest request = coinBunchRequestService.getRequest(requestId);
        assertThat(request, allOf(
                hasProperty("id", equalTo(requestId)),
                hasProperty("status", equalTo(PREPARED))
        ));
    }

    @SuppressWarnings("SameParameterValue")
    private void assertGeneratedCoinsCount(long requestId, int expectedCount) {
        final BunchGenerationRequest request = coinBunchRequestDao.getRequest(requestId).build();
        int count = coinDao.getCoinsCount(DISCOUNT_TABLE.sourceKey.like("%" + request.getKey() + "%"));
        assertEquals(expectedCount, count);
        assertEquals((int) request.getProcessedCount(), 10);
    }
}
