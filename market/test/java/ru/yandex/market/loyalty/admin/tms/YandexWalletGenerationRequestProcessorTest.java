package ru.yandex.market.loyalty.admin.tms;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.loyalty.admin.service.bunch.BunchRequestService;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.utils.YtTestHelper;
import ru.yandex.market.loyalty.core.config.TrustPayments;
import ru.yandex.market.loyalty.core.config.YtHahn;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDao;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestStatus;
import ru.yandex.market.loyalty.core.dao.coin.GeneratorType;
import ru.yandex.market.loyalty.core.model.coin.BunchGenerationRequest;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletRefundTransactionStatus;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.service.BunchGenerationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.trust.CreateProductResponse;
import ru.yandex.market.loyalty.core.trust.CreateRefundResponse;
import ru.yandex.market.loyalty.core.trust.CreateTopUpResponse;
import ru.yandex.market.loyalty.core.trust.CreateWalletResponse;
import ru.yandex.market.loyalty.core.trust.PaymentResponse;
import ru.yandex.market.loyalty.core.trust.RefundStatusResponse;
import ru.yandex.market.loyalty.core.trust.StartRefundResponse;
import ru.yandex.market.loyalty.core.trust.StartTopUpResponse;
import ru.yandex.market.loyalty.core.trust.TopUpStatusResponse;
import ru.yandex.market.loyalty.core.trust.TrustTopUpService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static ru.yandex.market.loyalty.admin.tms.PreparedBunchGenerationRequestProcessorTest.createTestScheduledRequest;
import static ru.yandex.market.loyalty.admin.utils.YtTestHelper.createWalletTopUps;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionPriority.HIGH;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionPriority.LOW;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.CONFIRMED;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.IN_QUEUE;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.PROCESSED;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.PROCESSING_ERROR;

@Log4j2
@TestFor({BunchRequestProcessor.class, YandexWalletTopUpProcessor.class})
public class YandexWalletGenerationRequestProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String TEST_KEY = "test_key";
    private static final String TEST_CAMPAIGN = "test_campaign";

    @Autowired
    private YandexWalletTransactionDao yandexWalletTransactionDao;
    @Autowired
    private BunchRequestProcessor bunchRequestProcessor;
    @Autowired
    private BunchRequestService bunchRequestService;
    @Autowired
    private BunchGenerationService bunchGenerationService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    @YtHahn
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private YandexWalletTopUpProcessor yandexWalletTopUpProcessor;

    @Autowired
    private TrustTopUpService topUpService;
    @Autowired
    @TrustPayments
    private RestTemplate trustPaymentsRestTemplate;
    @Autowired
    private YtTestHelper ytTestHelper;


    @Before
    public void init() {
        configurationService.set("market.loyalty.config.trust.yandex.wallet.topup.threads.count", 2);
        configurationService.set("market.loyalty.config.trust.yandex.wallet.check.threads.count", 2);
        configurationService.set("market.loyalty.config.trust.yandex.wallet.topup.rate", Integer.MAX_VALUE);
        topUpService.changeTrustRateLimit();
    }

    @Test
    public void shouldCreateTransactionFromYtFile() {
        mockCreateWalletRequest(trustPaymentsRestTemplate);
        mockCreateTopUp(trustPaymentsRestTemplate);
        mockCreateProduct(trustPaymentsRestTemplate);
        mockStartTopUp(trustPaymentsRestTemplate);
        mockTopUpPaymentCreatedStatus(trustPaymentsRestTemplate);

        YtTestUtils.mockAnyYtRequestWithArgsResult(createWalletTopUps(100), jdbcTemplate);
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        bunchRequestService.scheduleRequest(
                createTestScheduledRequest(
                        promo.getPromoId().getId(),
                        TEST_KEY,
                        GeneratorType.YANDEX_WALLET,
                        null,
                        100
                )
        );

        bunchRequestProcessor.yandexWalletBunchRequestProcess(
                500,
                Duration.of(1, MINUTES)
        );


        final List<YandexWalletTransaction> transactions = yandexWalletTransactionDao.query(IN_QUEUE, 500);
        assertThat(transactions, hasSize(100));
        assertThat(transactions, everyItem(hasProperty("payload", containsString("product_id"))));
    }

    @Test
    public void shouldStopGenerationIfCanNotReadYtFile() {
        mockCreateWalletRequest(trustPaymentsRestTemplate);
        mockCreateTopUp(trustPaymentsRestTemplate);
        mockCreateProduct(trustPaymentsRestTemplate);
        mockStartTopUp(trustPaymentsRestTemplate);
        mockTopUpPaymentCreatedStatus(trustPaymentsRestTemplate);

        YtTestUtils.mockAnyYtRequestWithError(jdbcTemplate);
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        final long id = bunchRequestService.scheduleRequest(
                createTestScheduledRequest(
                        promo.getPromoId().getId(),
                        TEST_KEY,
                        GeneratorType.YANDEX_WALLET,
                        null, 10
                )
        );

        bunchRequestProcessor.yandexWalletBunchRequestProcess(
                500,
                Duration.of(1, MINUTES)
        );


        final BunchGenerationRequest request = bunchRequestService.getRequest(id);
        assertThat(
                request,
                allOf(
                        hasProperty("status", equalTo(BunchGenerationRequestStatus.IN_QUEUE)),
                        hasProperty("subStatus", equalTo("TRANSACTION_ENQUEUE")),
                        hasProperty("message", not(empty()))
                )
        );
    }

    @Test
    public void shouldProcessAndCheckSuccessTransactions() throws InterruptedException {
        mockCreateWalletRequest(trustPaymentsRestTemplate);
        mockCreateTopUp(trustPaymentsRestTemplate);
        mockCreateProduct(trustPaymentsRestTemplate);
        mockStartTopUp(trustPaymentsRestTemplate);
        mockTopUpPaymentCreatedStatus(trustPaymentsRestTemplate);
        YtTestUtils.mockAnyYtRequestWithArgsResult(createWalletTopUps(100), jdbcTemplate);

        Promo promo = promoManager.createAccrualPromo(
                PromoUtils.WalletAccrual.defaultModelAccrual()
        );
        final long requestId = bunchRequestService.scheduleRequest(
                createTestScheduledRequest(
                        promo.getPromoId().getId(),
                        TEST_KEY,
                        GeneratorType.YANDEX_WALLET,
                        null,
                        100
                )
        );
        bunchRequestProcessor.yandexWalletBunchRequestProcess(
                500,
                Duration.of(1, MINUTES)
        );
        assertThat(bunchRequestService.getRequest(requestId), allOf(
                hasProperty("status", equalTo(BunchGenerationRequestStatus.IN_QUEUE)),
                hasProperty("subStatus", equalTo("TRANSACTION_PROCESS"))
        ));
        yandexWalletTopUpProcessor.yandexWalletTransactionsProcess(Duration.ofMinutes(1), 500);
        bunchRequestProcessor.yandexWalletBunchRequestProcess(
                500,
                Duration.of(1, MINUTES)
        );
        assertThat(bunchRequestService.getRequest(requestId), allOf(
                hasProperty("status", equalTo(BunchGenerationRequestStatus.IN_QUEUE)),
                hasProperty("subStatus", equalTo("TRANSACTION_CHECK"))
        ));
        clock.spendTime(1, ChronoUnit.HOURS);
        yandexWalletTopUpProcessor.yandexWalletTransactionsCheck(Duration.ofMinutes(1), 500);
        bunchRequestProcessor.yandexWalletBunchRequestProcess(
                500,
                Duration.of(1, MINUTES)
        );

        assertThat(bunchRequestService.getRequest(requestId), allOf(
                hasProperty("status", equalTo(BunchGenerationRequestStatus.PREPARED))
        ));
        ytTestHelper.mockYtClientAttributes(
                a -> a.toString().startsWith("//tmp/market-promo-test/temp/"),
                ImmutableMap.<String, YTreeNode>builder()
                        .put("row_count", YTree.builder().value(100).build())
                        .build()
        );
        ytTestHelper.mockYtMergeOperation();
        bunchRequestProcessor.processPreparedRequests(Duration.ofMinutes(1));
        assertThat(bunchRequestService.getRequest(requestId), allOf(
                hasProperty("status", equalTo(BunchGenerationRequestStatus.PROCESSED))
        ));
        final List<YandexWalletTransaction> transactions = yandexWalletTransactionDao.query(CONFIRMED, 500);
        assertThat(transactions, hasSize(100));
    }

    @Test
    public void shouldProcessAndCheckSuccessAndRefundTransactions() throws InterruptedException {
        mockCreateWalletRequest(trustPaymentsRestTemplate);
        mockCreateTopUp(trustPaymentsRestTemplate);
        mockCreateProduct(trustPaymentsRestTemplate);
        mockStartTopUp(trustPaymentsRestTemplate);
        mockTopUpPaymentCreatedStatus(trustPaymentsRestTemplate);
        mockRefundStatus(trustPaymentsRestTemplate);
        mockCreateRefund(trustPaymentsRestTemplate);
        mockStartRefund(trustPaymentsRestTemplate);
        mockPaymentStatus(trustPaymentsRestTemplate, "100");
        YtTestUtils.mockAnyYtRequestWithArgsResult(createWalletTopUps(100), jdbcTemplate);

        Promo promo = promoManager.createAccrualPromo(
                PromoUtils.WalletAccrual.defaultModelAccrual()
        );
        final long requestId = bunchRequestService.scheduleRequest(
                createTestScheduledRequest(
                        promo.getPromoId().getId(),
                        TEST_KEY,
                        GeneratorType.YANDEX_WALLET,
                        null,
                        100
                )
        );
        bunchRequestProcessor.yandexWalletBunchRequestProcess(
                500,
                Duration.of(1, MINUTES)
        );
        assertThat(bunchRequestService.getRequest(requestId), allOf(
                hasProperty("status", equalTo(BunchGenerationRequestStatus.IN_QUEUE)),
                hasProperty("subStatus", equalTo("TRANSACTION_PROCESS"))
        ));
        clock.spendTime(1, ChronoUnit.HOURS);
        yandexWalletTopUpProcessor.yandexWalletTransactionsProcess(Duration.ofMinutes(1), 500);
        clock.spendTime(1, ChronoUnit.HOURS);
        bunchRequestProcessor.yandexWalletBunchRequestProcess(
                500,
                Duration.of(1, MINUTES)
        );
        assertThat(bunchRequestService.getRequest(requestId), allOf(
                hasProperty("status", equalTo(BunchGenerationRequestStatus.IN_QUEUE)),
                hasProperty("subStatus", equalTo("TRANSACTION_CHECK"))
        ));
        clock.spendTime(1, ChronoUnit.HOURS);
        yandexWalletTopUpProcessor.yandexWalletTransactionsCheck(Duration.ofMinutes(1), 500);
        bunchRequestProcessor.yandexWalletBunchRequestProcess(
                500,
                Duration.of(1, MINUTES)
        );

        assertThat(bunchRequestService.getRequest(requestId), allOf(
                hasProperty("status", equalTo(BunchGenerationRequestStatus.PREPARED))
        ));
        ytTestHelper.mockYtClientAttributes(
                a -> a.toString().startsWith("//tmp/market-promo-test/temp/"),
                ImmutableMap.<String, YTreeNode>builder()
                        .put("row_count", YTree.builder().value(100).build())
                        .build()
        );
        ytTestHelper.mockYtMergeOperation();
        bunchRequestProcessor.processPreparedRequests(Duration.ofMinutes(1));
        assertThat(bunchRequestService.getRequest(requestId), allOf(
                hasProperty("status", equalTo(BunchGenerationRequestStatus.PROCESSED))
        ));
        List<YandexWalletTransaction> transactions = yandexWalletTransactionDao.query(CONFIRMED, 500);
        assertThat(transactions, hasSize(100));
        bunchGenerationService.revertRequest(requestId);
        clock.spendTime(1, ChronoUnit.HOURS);
        bunchRequestProcessor.yandexWalletBunchRequestProcess(
                500,
                Duration.of(1, MINUTES)
        );
        bunchRequestProcessor.finalizeCancellingRequests(Duration.ofMinutes(1), 500);
        assertThat(bunchRequestService.getRequest(requestId),
                hasProperty("status", equalTo(BunchGenerationRequestStatus.CANCELLING))
        );
        log.info("4656 {}", yandexWalletTransactionDao.countRefund(requestId));
//        assertThat(yandexWalletTransactionDao.query(YandexWalletRefundTransactionStatus.IN_QUEUE, 500), hasSize(100));
        yandexWalletTopUpProcessor.yandexWalletRefundTransactionsProcess(Duration.ofMinutes(1), 500);
        log.info("4656 {}", yandexWalletTransactionDao.countRefund(requestId));
//        assertThat(yandexWalletTransactionDao.query(YandexWalletRefundTransactionStatus.PROCESSED, 500), hasSize
//        (100));
        bunchRequestProcessor.yandexWalletBunchRequestProcess(
                500,
                Duration.of(1, MINUTES)
        );
        assertThat(bunchRequestService.getRequest(requestId),
                hasProperty("status", equalTo(BunchGenerationRequestStatus.CANCELLING))
        );
        clock.spendTime(1, ChronoUnit.HOURS);
        yandexWalletTopUpProcessor.yandexWalletRefundTransactionsCheck(Duration.ofMinutes(1), 500);
        log.info("4656 {}", yandexWalletTransactionDao.countRefund(requestId));
//        assertThat(yandexWalletTransactionDao.query(YandexWalletRefundTransactionStatus.CONFIRMED, 500), hasSize
//        (100));
        bunchRequestProcessor.finalizeCancellingRequests(Duration.ofMinutes(1), 500);

        assertThat(bunchRequestService.getRequest(requestId),
                hasProperty("status", equalTo(BunchGenerationRequestStatus.CANCELLED))
        );
        transactions = yandexWalletTransactionDao.query(YandexWalletTransactionStatus.NON_FAKE_STATUSES,
                YandexWalletRefundTransactionStatus.CONFIRMED, 500);
        assertThat(transactions, hasSize(100));
        assertThat(transactions, everyItem(hasProperty("refundEmissionTransactionId", notNullValue())));
    }

    @Test
    public void shouldProcessAndCheckAwaitingTransactions() throws InterruptedException {
        mockCreateWalletRequest(trustPaymentsRestTemplate);
        mockCreateTopUp(trustPaymentsRestTemplate);
        mockCreateProduct(trustPaymentsRestTemplate);
        mockStartTopUp(trustPaymentsRestTemplate);
        mockTopUpPaymentAwaitingStatus(trustPaymentsRestTemplate);

        Promo promo = promoManager.createAccrualPromo(
                PromoUtils.WalletAccrual.defaultModelAccrual()
        );
        final long requestId = bunchRequestService.scheduleRequest(
                createTestScheduledRequest(
                        promo.getPromoId().getId(),
                        TEST_KEY,
                        GeneratorType.YANDEX_WALLET,
                        null,
                        100
                )
        );
        yandexWalletTransactionDao.enqueueTransactions(requestId, TEST_CAMPAIGN, createWalletTopUps(1),
                LOW);

        yandexWalletTopUpProcessor.yandexWalletTransactionsProcess(Duration.ofMinutes(1), 10);

        clock.spendTime(10, MINUTES);
        yandexWalletTopUpProcessor.yandexWalletTransactionsCheck(Duration.ofMinutes(1), 10);
        clock.spendTime(10, MINUTES);
        assertThat(yandexWalletTransactionDao.query(PROCESSED, 500), hasSize(1));

        mockTopUpPaymentCreatedStatus(trustPaymentsRestTemplate);

        clock.spendTime(6, MINUTES);
        yandexWalletTopUpProcessor.yandexWalletTransactionsCheck(Duration.ofMinutes(1), 10);
        assertThat(yandexWalletTransactionDao.query(CONFIRMED, 500), hasSize(1));
    }

    @Test
    public void shouldCompleteFailureTransactions() throws InterruptedException {
        mockCreateWalletRequest(trustPaymentsRestTemplate);
        mockCreateTopUp(trustPaymentsRestTemplate);
        mockCreateProduct(trustPaymentsRestTemplate);
        mockStartTopUpFailure(trustPaymentsRestTemplate);
        mockTopUpPaymentAwaitingStatus(trustPaymentsRestTemplate);

        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        final long requestId = bunchRequestService.scheduleRequest(
                createTestScheduledRequest(
                        promo.getPromoId().getId(),
                        TEST_KEY,
                        GeneratorType.YANDEX_WALLET,
                        null,
                        100
                )
        );
        yandexWalletTransactionDao.enqueueTransactions(requestId, TEST_CAMPAIGN, createWalletTopUps(1),
                LOW);

        yandexWalletTopUpProcessor.yandexWalletTransactionsProcess(Duration.ofMinutes(1), 10);
        clock.spendTime(10, MINUTES);
        yandexWalletTopUpProcessor.yandexWalletTransactionsProcess(Duration.ofMinutes(1), 10);
        clock.spendTime(10, MINUTES);
        yandexWalletTopUpProcessor.yandexWalletTransactionsProcess(Duration.ofMinutes(1), 10);
        clock.spendTime(10, MINUTES);
        yandexWalletTopUpProcessor.yandexWalletTransactionsProcess(Duration.ofMinutes(1), 10);

        final List<YandexWalletTransaction> transactions = yandexWalletTransactionDao.queryAll(PROCESSING_ERROR);
        assertThat(transactions, hasSize(1));
        assertThat(transactions, contains(allOf(
                hasProperty("tryCount", equalTo(3)),
                hasProperty("message", is(not(empty())))
        )));
    }

    @Test
    public void shouldQueryOrdered() {
        mockCreateWalletRequest(trustPaymentsRestTemplate);
        mockCreateTopUp(trustPaymentsRestTemplate);
        mockCreateProduct(trustPaymentsRestTemplate);
        mockStartTopUpFailure(trustPaymentsRestTemplate);
        mockTopUpPaymentAwaitingStatus(trustPaymentsRestTemplate);

        Promo promo = promoManager.createAccrualPromo(
                PromoUtils.WalletAccrual.defaultModelAccrual()
        );
        @SuppressWarnings("StringConcatenationMissingWhitespace") final long requestId1 =
                bunchRequestService.scheduleRequest(
                createTestScheduledRequest(
                        promo.getPromoId().getId(),
                        TEST_KEY + '1',
                        GeneratorType.YANDEX_WALLET,
                        null,
                        100
                )
        );
        @SuppressWarnings("StringConcatenationMissingWhitespace") final long requestId2 =
                bunchRequestService.scheduleRequest(
                createTestScheduledRequest(
                        promo.getPromoId().getId(),
                        TEST_KEY + '2',
                        GeneratorType.YANDEX_WALLET,
                        null,
                        100
                )
        );


        yandexWalletTransactionDao.enqueueTransactions(requestId1, TEST_CAMPAIGN,
                createWalletTopUps(2), LOW);

        yandexWalletTransactionDao.enqueueTransactions(requestId2, TEST_CAMPAIGN,
                createWalletTopUps(4).subList(2, 4), HIGH);

        final List<YandexWalletTransaction> yandexWalletTransactions = yandexWalletTransactionDao.queryAll();
        assertThat(yandexWalletTransactions, hasSize(4));
        assertThat(yandexWalletTransactions.get(0), hasProperty("priority", equalTo(HIGH)));
        assertThat(yandexWalletTransactions.get(1), hasProperty("priority", equalTo(HIGH)));
        assertThat(yandexWalletTransactions.get(2), hasProperty("priority", equalTo(LOW)));
        assertThat(yandexWalletTransactions.get(3), hasProperty("priority", equalTo(LOW)));
    }

    @SuppressWarnings("unchecked")
    private static void mockCreateWalletRequest(RestTemplate restTemplate) {
        Mockito.when(
                restTemplate.exchange(
                        eq("/account"),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        any(Class.class)
                ))
                .then(i -> new ResponseEntity<>(
                                new CreateWalletResponse(getUid(i), "RUB"),
                                HttpStatus.OK
                        )
                );
    }

    @SuppressWarnings("unchecked")
    private static void mockCreateTopUp(RestTemplate restTemplate) {
        Mockito.when(
                restTemplate.exchange(
                        eq("/topup"),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        any(Class.class)
                ))
                .then(i -> new ResponseEntity<>(
                                new CreateTopUpResponse(
                                        "success",
                                        "",
                                        "token-" + getUid(i)
                                ),
                                HttpStatus.OK
                        )
                );
    }

    @SuppressWarnings("unchecked")
    private static void mockCreateRefund(RestTemplate restTemplate) {
        Mockito.when(
                restTemplate.exchange(
                        eq("/refunds"),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        any(Class.class)
                ))
                .then(i -> new ResponseEntity<>(
                                new CreateRefundResponse(
                                        "success",
                                        "token-" + getUid(i)
                                ),
                                HttpStatus.OK
                        )
                );
    }

    @SuppressWarnings("unchecked")
    private static void mockCreateProduct(RestTemplate restTemplate) {
        Mockito.when(
                restTemplate.exchange(
                        eq("/products"),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        any(Class.class)
                ))
                .then(i -> new ResponseEntity<>(
                                new CreateProductResponse("success"),
                                HttpStatus.OK
                        )
                );
    }

    @SuppressWarnings("unchecked")
    private static void mockStartTopUp(RestTemplate restTemplate) {
        Mockito.when(
                restTemplate.exchange(
                        matches("/topup/token-[0-9]+/start"),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        any(Class.class)
                ))
                .then(i -> new ResponseEntity<>(
                                new StartTopUpResponse(
                                        "success",
                                        "",
                                        i.getArgument(0).toString().replaceFirst("/topup/(token-[0-9]+)/start", "$1")
                                ),
                                HttpStatus.OK
                        )
                );
    }

    @SuppressWarnings("unchecked")
    private static void mockStartRefund(RestTemplate restTemplate) {
        Mockito.when(
                restTemplate.exchange(
                        matches("/refunds/token-[0-9]+/start"),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        any(Class.class)
                ))
                .then(i -> new ResponseEntity<>(
                                new StartRefundResponse(
                                        "success",
                                        "started"
                                ),
                                HttpStatus.OK
                        )
                );
    }

    @SuppressWarnings("unchecked")
    private static void mockStartTopUpFailure(RestTemplate restTemplate) {
        Mockito.when(
                restTemplate.exchange(
                        matches("/topup/token-[0-9]+/start"),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        any(Class.class)
                ))
                .thenThrow(RestClientException.class);
    }

    private static void mockTopUpPaymentCreatedStatus(RestTemplate restTemplate) {
        mockTopUpStatus(restTemplate, "cleared");
    }

    private static void mockTopUpPaymentAwaitingStatus(RestTemplate restTemplate) {
        mockTopUpStatus(restTemplate, "");
    }

    @SuppressWarnings("unchecked")
    private static void mockTopUpStatus(RestTemplate restTemplate, String status) {
        Mockito.when(
                restTemplate.exchange(
                        matches("/topup/token-[0-9]+"),
                        eq(HttpMethod.GET),
                        any(HttpEntity.class),
                        any(Class.class)
                ))
                .then(i -> new ResponseEntity<>(
                                new TopUpStatusResponse(
                                        "success",
                                        status,
                                        i.getArgument(0).toString().replaceFirst("/topup/(token-[0-9]+)", "$1")
                                ),
                                HttpStatus.OK
                        )
                );
    }

    @SuppressWarnings("unchecked")
    private static void mockRefundStatus(RestTemplate restTemplate) {
//        ResponseEntity<RefundStatusResponse> response = trustRestTemplate.exchange(
//            "/refunds/" + trustRefundId, HttpMethod.GET, request, RefundStatusResponse.class
//        );
        Mockito.when(
                restTemplate.exchange(
                        matches("/refunds/token-[0-9]+"),
                        eq(HttpMethod.GET),
                        any(HttpEntity.class),
                        any(Class.class)
                ))
                .then(i -> new ResponseEntity<>(
                                new RefundStatusResponse(
                                        "success",
                                        ""
                                ),
                                HttpStatus.OK
                        )
                );
    }

    @SuppressWarnings("SameParameterValue, unchecked")
    private static void mockPaymentStatus(RestTemplate restTemplate, String amount) {
        Mockito.when(
                restTemplate.exchange(
                        matches("/payments/token-[0-9]+"),
                        eq(HttpMethod.GET),
                        any(HttpEntity.class),
                        any(Class.class)
                ))
                .then(i -> new ResponseEntity<>(
                                new PaymentResponse(
                                        List.of(new PaymentResponse.Order("1", amount))
                                ),
                                HttpStatus.OK
                        )
                );
    }

    @SuppressWarnings("rawtypes")
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private static String getUid(InvocationOnMock i) {
        return Objects.requireNonNull(((HttpEntity) i.getArguments()[2]).getHeaders().get("X-Uid")).get(0);
    }
}
