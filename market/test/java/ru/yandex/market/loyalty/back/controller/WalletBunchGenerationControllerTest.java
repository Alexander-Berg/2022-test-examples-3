package ru.yandex.market.loyalty.back.controller;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.loyalty.api.model.BunchCheckResponseStatus;
import ru.yandex.market.loyalty.api.model.bunch.wallet.WalletBunchCheckResponse;
import ru.yandex.market.loyalty.api.model.bunch.wallet.WalletBunchSaveRequest;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.PromoDao;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestDao;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamsDao;
import ru.yandex.market.loyalty.core.dao.coin.GeneratorType;
import ru.yandex.market.loyalty.core.model.GenericParam;
import ru.yandex.market.loyalty.core.model.coin.BunchGenerationRequest;
import ru.yandex.market.loyalty.core.rule.ParamsContainer;
import ru.yandex.market.loyalty.core.service.BunchGenerationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.IGNORE_BUDGET_EXHAUSTION;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.INPUT_TABLE;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.INPUT_TABLE_CLUSTER;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestStatus.CANCELLED;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestStatus.CANCELLING;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestStatus.PROCESSED;

@TestFor(WalletBunchGenerationController.class)
public class WalletBunchGenerationControllerTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    BunchGenerationService bunchGenerationService;
    @Autowired
    BunchGenerationRequestDao bunchGenerationRequestDao;
    @Autowired
    BunchGenerationRequestParamsDao bunchGenerationRequestParamsDao;
    @Autowired
    PromoManager promoManager;
    @Autowired
    CoinService coinService;
    @Autowired
    PromoDao promoDao;
    @Autowired
    JdbcTemplate jdbcTemplate;

    private static final String DEFAULT_UNIQUE_KEY = "unique_key";
    private static final String DEFAULT_REQUEST_ID = "someTestKey";
    private String testRequestId;
    private Long testPromoId;

    @Before
    public void setUp() {
        testPromoId = promoManager.createAccrualPromo(
                PromoUtils.WalletAccrual.defaultModelAccrual()
        ).getId();
        var paramsContainer = new ParamsContainer<BunchGenerationRequestParamName<?>>();

        ParamsContainer.addParam(
                paramsContainer,
                BunchGenerationRequestParamName.INPUT_TABLE,
                GenericParam.of("//tmp/input_table")
        );

        testRequestId = bunchGenerationService.saveRequestAsScheduledReturningBunchId(
                BunchGenerationRequest.builder()
                        .setKey(DEFAULT_REQUEST_ID)
                        .setPromoId(testPromoId)
                        .setSource("Some source")
                        .setCount(100)
                        .setGeneratorType(GeneratorType.YANDEX_WALLET)
                        .setEmail("email")
                        .setParamsContainer(paramsContainer)
                        .build()
        );
    }

    @Test
    public void shouldReturnSavedCoinRequestById() {
        BunchGenerationRequest request = bunchGenerationService.getBunchGenerationRequest(DEFAULT_REQUEST_ID);
        WalletBunchCheckResponse checkResponse = marketLoyaltyClient.checkWalletBunchRequestStatus(DEFAULT_REQUEST_ID);

        verifyBunchCheckResponse(request, checkResponse, BunchCheckResponseStatus.PROCESSING);
    }

    @Test
    public void shouldReturnErrorStatusIfRetryCountExceed() {
        BunchGenerationRequest request = bunchGenerationService.getBunchGenerationRequest(DEFAULT_REQUEST_ID);

        updateRetryCount(DEFAULT_REQUEST_ID, 500);

        WalletBunchCheckResponse checkResponse = marketLoyaltyClient.checkWalletBunchRequestStatus(DEFAULT_REQUEST_ID);

        verifyBunchCheckResponse(request, checkResponse, BunchCheckResponseStatus.ERROR);
    }

    @Test
    public void shouldReturnCancelledStatusForCancelledRequest() {
        bunchGenerationRequestDao.updateRequestStatus(testRequestId, CANCELLING);

        BunchGenerationRequest request = bunchGenerationService.getBunchGenerationRequest(DEFAULT_REQUEST_ID);
        WalletBunchCheckResponse checkResponse = marketLoyaltyClient.checkWalletBunchRequestStatus(DEFAULT_REQUEST_ID);

        verifyBunchCheckResponse(request, checkResponse, BunchCheckResponseStatus.CANCELLING);
    }

    @Test
    public void shouldNotAllowToChangeCancelStatusAfterCancellingFinalization() {
        marketLoyaltyClient.cancelWalletBunchRequest(DEFAULT_REQUEST_ID);

        assertEquals(CANCELLING, bunchGenerationService.getBunchGenerationRequest(DEFAULT_REQUEST_ID).getStatus());

        bunchGenerationRequestDao.updateRequestStatus(testRequestId, CANCELLED);

        marketLoyaltyClient.cancelBunchRequest(DEFAULT_REQUEST_ID);

        assertEquals(CANCELLED, bunchGenerationService.getBunchGenerationRequest(DEFAULT_REQUEST_ID).getStatus());
    }

    @Test
    public void shouldNotAllowToChangeCancelStatusAfterRequestProcessed() {
        bunchGenerationRequestDao.updateRequestStatus(testRequestId, PROCESSED);
        marketLoyaltyClient.cancelWalletBunchRequest(DEFAULT_REQUEST_ID);
        assertEquals(PROCESSED, bunchGenerationService.getBunchGenerationRequest(DEFAULT_REQUEST_ID).getStatus());
    }

    @Test
    public void shouldSaveBunchRequest() {
        WalletBunchSaveRequest request = createDefaultBunchRequest("some new alias", testPromoId);

        String batchId = marketLoyaltyClient.saveWalletBunchRequest(request);
        checkSavedRequest(request, batchId, testPromoId);
    }

    @Test
    public void shouldCancelBunchRequest() {
        marketLoyaltyClient.cancelWalletBunchRequest(DEFAULT_REQUEST_ID);

        assertEquals(
                CANCELLING,
                bunchGenerationService.getBunchGenerationRequest(DEFAULT_REQUEST_ID).getStatus()
        );
    }

    private void checkSavedRequest(WalletBunchSaveRequest request, String batchId, long promoId) {
        BunchGenerationRequest savedRequest = bunchGenerationService.getBunchGenerationRequest(batchId);
        assertThat(
                savedRequest,
                allOf(
                        hasProperty("key", equalTo(DEFAULT_UNIQUE_KEY)),
                        hasProperty("promoId", equalTo(promoId)),
                        hasProperty("count", equalTo(request.getCount()))
                )
        );
        assertEquals(
                request.getInput(),
                savedRequest.getRequiredParamOrThrowException(INPUT_TABLE)
        );

        final String savedInputTable = savedRequest
                .getParam(INPUT_TABLE).orElse(null);
        final String savedInputCluster = savedRequest
                .getParam(INPUT_TABLE_CLUSTER).orElse(null);
        final Boolean savedIgnoreBudgetExhaustion = savedRequest
                .getParam(IGNORE_BUDGET_EXHAUSTION).orElse(null);
        assertThat(
                savedInputTable,
                allOf(
                        is(not(nullValue())),
                        anyOf(
                                equalTo(request.getInput()),
                                equalTo("//tmp/market-promo-test/bunch_request/input/" + request.getInput())
                        )
                )
        );
        assertThat(
                savedInputCluster,
                equalTo("hahn")
        );
        assertThat(
                savedIgnoreBudgetExhaustion,
                equalTo(request.getIgnoreBudgetExhaustion())
        );
    }

    @NotNull
    private static WalletBunchSaveRequest createDefaultBunchRequest(String promoAlias, Long promoId) {
        return new WalletBunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                promoId,
                promoAlias,
                null,
                100,
                "//input/some_new_input",
                "//input/some_output",
                "//input/some_error_output",
                "email",
                "test stock",
                "test_product_id"
        );
    }

    private static void verifyBunchCheckResponse(
            BunchGenerationRequest request, WalletBunchCheckResponse checkResponse, BunchCheckResponseStatus status
    ) {
        assertThat(
                checkResponse,
                allOf(
                        hasProperty(
                                "input",
                                equalTo(request.getRequiredParamOrThrowException(INPUT_TABLE))
                        ),
                        hasProperty(
                                "processedCount",
                                equalTo((long) request.getProcessedCount())
                        ),
                        hasProperty(
                                "totalCount",
                                equalTo(checkResponse.getTotalCount())
                        ),
                        hasProperty("status", equalTo(status))
                )
        );
    }


    @Override
    protected boolean shouldCheckConsistence() {
        return false;
    }

    private void updateRetryCount(String id, int retryCount) {
        jdbcTemplate.update("UPDATE bunch_generation_request SET retry_count=? WHERE key=?", retryCount, id);
    }
}
