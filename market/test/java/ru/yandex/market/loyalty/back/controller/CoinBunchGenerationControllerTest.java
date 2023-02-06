package ru.yandex.market.loyalty.back.controller;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.BunchCheckResponseStatus;
import ru.yandex.market.loyalty.api.model.BunchSaveRequest;
import ru.yandex.market.loyalty.api.model.CoinGeneratorType;
import ru.yandex.market.loyalty.api.model.bunch.coin.CoinBunchCheckResponse;
import ru.yandex.market.loyalty.api.model.coin.CoinCreationReason;
import ru.yandex.market.loyalty.api.model.coin.creation.CoinCreationError;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.PromoDao;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestDao;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamsDao;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestStatus;
import ru.yandex.market.loyalty.core.dao.coin.GeneratorType;
import ru.yandex.market.loyalty.core.dao.custom.coin.BunchGenerationErrorStatDao;
import ru.yandex.market.loyalty.core.model.GenericParam;
import ru.yandex.market.loyalty.core.model.coin.BunchGenerationRequest;
import ru.yandex.market.loyalty.core.model.promo.Promo;
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
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.api.model.CoinGeneratorType.AUTH;
import static ru.yandex.market.loyalty.api.model.CoinGeneratorType.AUTH_DYNAMIC;
import static ru.yandex.market.loyalty.api.model.CoinGeneratorType.NO_AUTH;
import static ru.yandex.market.loyalty.api.model.TableFormat.CSV;
import static ru.yandex.market.loyalty.api.model.TableFormat.EXCEL;
import static ru.yandex.market.loyalty.api.model.TableFormat.NONE;
import static ru.yandex.market.loyalty.api.model.TableFormat.YT;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.COIN_CREATION_REASON;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.COIN_GENERATOR_TYPE;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.ERROR_OUTPUT_TABLE;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.IGNORE_BUDGET_EXHAUSTION;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.INPUT_TABLE;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.INPUT_TABLE_CLUSTER;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.OUTPUT_TABLE;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.OUTPUT_TABLE_CLUSTER;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestStatus.CANCELLED;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestStatus.CANCELLING;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestStatus.PROCESSED;
import static ru.yandex.market.loyalty.core.service.BunchRequestScheduler.ERRORS_TABLE_NAME_POSTFIX;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(CoinBunchGenerationController.class)
public class CoinBunchGenerationControllerTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    BunchGenerationService bunchGenerationService;
    @Autowired
    BunchGenerationRequestDao bunchGenerationRequestDao;
    @Autowired
    BunchGenerationErrorStatDao bunchGenerationErrorStatDao;
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

    private static final int DEFAULT_COUNT = 1111;
    private static final int COUNT_OF_COMMONS_ERRORS = 21;
    private static final String DEFAULT_UNIQUE_KEY = "unique_key";
    private static final String DEFAULT_PROMO_ALIAS = "somePromoAlias";
    private static final String ANOTHER_PROMO_ALIAS = "anotherPromoAlias";
    private static final String DEFAULT_REQUEST_ID = "someTestKey";
    private Promo testPromo;
    private Long testRequestId;

    @Before
    public void setUp() {
        testPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
                        .setActionCodeInternal(DEFAULT_PROMO_ALIAS)
        );
        testRequestId = bunchGenerationRequestDao.insertRequest(
                BunchGenerationRequest.builder()
                        .setKey(DEFAULT_REQUEST_ID)
                        .setPromoId(testPromo.getId())
                        .setFormat(YT)
                        .setCount(DEFAULT_COUNT)
                        .setPromoId(2222L)
                        .setStatus(BunchGenerationRequestStatus.PREPARED)
                        .setSource("Some source")
                        .setGeneratorType(GeneratorType.COIN)
                        .setEmail("email")
                        .build()
        );
        ParamsContainer<BunchGenerationRequestParamName<?>> paramsContainer = new ParamsContainer<>();
        ParamsContainer.addParam(
                paramsContainer,
                INPUT_TABLE,
                GenericParam.of("//some_input/some_input")
        );
        ParamsContainer.addParam(
                paramsContainer,
                OUTPUT_TABLE,
                GenericParam.of("//some_output/some_output")
        );
        bunchGenerationRequestParamsDao.insertParams(
                testRequestId,
                paramsContainer
        );
    }

    @Test
    public void shouldReturnSavedCoinRequestById() {
        BunchGenerationRequest request = bunchGenerationService.getBunchGenerationRequest(DEFAULT_REQUEST_ID);
        CoinBunchCheckResponse checkResponse = marketLoyaltyClient.checkCoinBunchRequestStatus(DEFAULT_REQUEST_ID);

        verifyBunchCheckResponse(request, checkResponse, BunchCheckResponseStatus.PROCESSING);
    }

    @Test
    public void shouldReturnErrorStatusIfRetryCountExceed() {
        BunchGenerationRequest request = bunchGenerationService.getBunchGenerationRequest(DEFAULT_REQUEST_ID);

        updateRetryCount(DEFAULT_REQUEST_ID, DEFAULT_COUNT);

        CoinBunchCheckResponse checkResponse = marketLoyaltyClient.checkCoinBunchRequestStatus(DEFAULT_REQUEST_ID);

        verifyBunchCheckResponse(request, checkResponse, BunchCheckResponseStatus.ERROR);
    }

    @Test
    public void shouldReturnCancelledStatusForCancelledRequest() {
        bunchGenerationRequestDao.updateRequestStatus(testRequestId, CANCELLING);

        BunchGenerationRequest request = bunchGenerationService.getBunchGenerationRequest(DEFAULT_REQUEST_ID);
        CoinBunchCheckResponse checkResponse = marketLoyaltyClient.checkCoinBunchRequestStatus(DEFAULT_REQUEST_ID);

        verifyBunchCheckResponse(request, checkResponse, BunchCheckResponseStatus.CANCELLING);
    }

    @Test
    public void shouldNotAllowToChangeCancelStatusAfterCancellingFinalization() {
        marketLoyaltyClient.cancelBunchRequest(DEFAULT_REQUEST_ID);

        assertEquals(CANCELLING, bunchGenerationService.getBunchGenerationRequest(DEFAULT_REQUEST_ID).getStatus());

        bunchGenerationRequestDao.updateRequestStatus(testRequestId, CANCELLED);

        marketLoyaltyClient.cancelBunchRequest(DEFAULT_REQUEST_ID);

        assertEquals(CANCELLED, bunchGenerationService.getBunchGenerationRequest(DEFAULT_REQUEST_ID).getStatus());
    }

    @Test
    public void shouldNotAllowToChangeCancelStatusAfterRequestProcessed() {
        bunchGenerationRequestDao.updateRequestStatus(testRequestId, PROCESSED);
        marketLoyaltyClient.cancelBunchRequest(DEFAULT_REQUEST_ID);
        assertEquals(PROCESSED, bunchGenerationService.getBunchGenerationRequest(DEFAULT_REQUEST_ID).getStatus());
    }

    @Test
    public void shouldSaveBunchRequest() {
        BunchSaveRequest request = createDefaultBunchRequest("some new alias", testPromo.getId());

        String batchId = marketLoyaltyClient.saveBunchRequest(request);
        BunchGenerationRequest savedRequest = checkSavedRequest(request, batchId, testPromo.getId());

        assertEquals(
                request.getInput(),
                savedRequest.getRequiredParamOrThrowException(INPUT_TABLE)
        );
        assertEquals(
                request.getOutput(),
                savedRequest.getRequiredParamOrThrowException(OUTPUT_TABLE)
        );
    }

    @Test
    public void shouldSaveBunchRequestResolvingPromoByPromoAlias() {
        Optional<Long> promoByAlias = promoDao.getPromoByInternalActionCode(DEFAULT_PROMO_ALIAS);
        assertTrue(promoByAlias.isPresent());

        BunchSaveRequest request = createDefaultBunchRequest(DEFAULT_PROMO_ALIAS, null);
        String batchId = marketLoyaltyClient.saveBunchRequest(request);

        checkSavedRequest(request, batchId, promoByAlias.get());
    }

    @Test
    public void shouldCancelBunchRequest() {
        marketLoyaltyClient.cancelBunchRequest(DEFAULT_REQUEST_ID);

        assertEquals(
                CANCELLING,
                bunchGenerationService.getBunchGenerationRequest(DEFAULT_REQUEST_ID).getStatus()
        );
    }

    @Test
    public void shouldReturnCorrectErrors() {
        bunchGenerationRequestDao.updateRequestStatus(testRequestId, PROCESSED);
        bunchGenerationErrorStatDao.saveErrorStat(
                testRequestId,
                ImmutableMap.of(DEFAULT_UID, CoinCreationError.COIN_ALREADY_CREATED)
        );
        BunchGenerationRequest request = bunchGenerationService.getBunchGenerationRequest(DEFAULT_REQUEST_ID);
        CoinBunchCheckResponse checkResponse = marketLoyaltyClient.checkCoinBunchRequestStatus(DEFAULT_REQUEST_ID);

        verifyBunchCheckResponse(
                request,
                checkResponse,
                BunchCheckResponseStatus.OK,
                ImmutableMap.of(CoinCreationError.COIN_ALREADY_CREATED, 1)
        );
    }

    @Test
    public void shouldReturnCorrectCountOfErrorsByRequestIdAndErrorType() {
        bunchGenerationRequestDao.updateRequestStatus(testRequestId, PROCESSED);
        Map<Long, CoinCreationError> map = IntStream.range(0, COUNT_OF_COMMONS_ERRORS).boxed().collect(
                Collectors.toMap(i -> DEFAULT_UID + i,
                        i -> CoinCreationError.COIN_ALREADY_CREATED,
                        (a, b) -> b)
        );
        IntStream.range(0, COUNT_OF_COMMONS_ERRORS).forEach(i -> map.put(DEFAULT_UID * 2 + i,
                CoinCreationError.PROMO_NOT_ACTIVE));
        bunchGenerationErrorStatDao.saveErrorStat(
                testRequestId,
                map
        );
        verifyBunchCheckResponse(
                bunchGenerationService.getBunchGenerationRequest(DEFAULT_REQUEST_ID),
                marketLoyaltyClient.checkCoinBunchRequestStatus(DEFAULT_REQUEST_ID),
                BunchCheckResponseStatus.OK,
                ImmutableMap.of(CoinCreationError.COIN_ALREADY_CREATED, COUNT_OF_COMMONS_ERRORS)
        );
        verifyBunchCheckResponse(
                bunchGenerationService.getBunchGenerationRequest(DEFAULT_REQUEST_ID),
                marketLoyaltyClient.checkCoinBunchRequestStatus(DEFAULT_REQUEST_ID),
                BunchCheckResponseStatus.OK,
                ImmutableMap.of(CoinCreationError.PROMO_NOT_ACTIVE, COUNT_OF_COMMONS_ERRORS)
        );
    }

    @Test
    public void shouldSaveBunchRequestWithNullOutputFormat() {
        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                testPromo.getId(),
                null,
                null,
                null,
                AUTH,
                "//input/input_1",
                null,
                NONE,
                null,
                null,
                null
        );

        String bunchId = marketLoyaltyClient.saveBunchRequest(request);

        checkSavedRequest(request, bunchId, testPromo.getId());
    }

    @Test
    public void shouldSaveBunchRequestWithReason() {
        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                testPromo.getId(),
                null,
                null,
                CoinCreationReason.OTHER,
                AUTH,
                "//input/input_1",
                null,
                NONE,
                null,
                null,
                null
        );

        String bunchId = marketLoyaltyClient.saveBunchRequest(request);

        checkSavedRequest(request, bunchId, testPromo.getId());
    }

    @Test
    public void shouldSaveBunchRequestWithNullInputForNoAuth() {
        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                testPromo.getId(),
                null,
                null,
                null,
                NO_AUTH,
                null,
                null,
                NONE,
                null,
                null,
                null
        );

        String bunchId = marketLoyaltyClient.saveBunchRequest(request);

        checkSavedRequest(request, bunchId, testPromo.getId());
    }

    @Test
    public void shouldSaveBunchRequestWithYtOutputAndNullErrorsOutput() {
        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                testPromo.getId(),
                null,
                null,
                null,
                NO_AUTH,
                null,
                null,
                YT,
                "//output/output_1",
                null,
                null
        );

        String bunchId = marketLoyaltyClient.saveBunchRequest(request);

        checkSavedRequest(request, bunchId, testPromo.getId());
    }

    @Test
    public void shouldSaveBunchRequestWithYtOutputAndErrorsOutput() {
        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                testPromo.getId(),
                null,
                null,
                null,
                NO_AUTH,
                null,
                null,
                YT,
                "//output/output_1",
                "//output/my_custom_errors_output",
                null
        );

        String bunchId = marketLoyaltyClient.saveBunchRequest(request);

        checkSavedRequest(request, bunchId, testPromo.getId());
    }

    @Test
    public void shouldSaveBunchRequestWithEmailCsvOutput() {
        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                testPromo.getId(),
                null,
                null,
                null,
                NO_AUTH,
                null,
                null,
                CSV,
                null,
                null,
                "email@email.email"
        );

        String bunchId = marketLoyaltyClient.saveBunchRequest(request);

        checkSavedRequest(request, bunchId, testPromo.getId());
    }

    @Test
    public void shouldSaveBunchRequestWithExcelEmailOutput() {
        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                testPromo.getId(),
                null,
                null,
                null,
                NO_AUTH,
                null,
                null,
                EXCEL,
                null,
                null,
                "email@email.email"
        );

        String bunchId = marketLoyaltyClient.saveBunchRequest(request);

        checkSavedRequest(request, bunchId, testPromo.getId());
    }

    @Test
    public void shouldSaveBunchRequestWithIgnoreBudgetExhaustion() {
        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                testPromo.getId(),
                null,
                true,
                null,
                NO_AUTH,
                null,
                null,
                EXCEL,
                null,
                null,
                "email@email.email"
        );

        String bunchId = marketLoyaltyClient.saveBunchRequest(request);

        checkSavedRequest(request, bunchId, testPromo.getId());
    }


    @Test
    public void shouldSaveBunchRequestWithPromoIdHavingHigherPrecedenceThanPromoAlias() {
        promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
                        .setActionCodeInternal(ANOTHER_PROMO_ALIAS)
        );

        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                testPromo.getId(),
                ANOTHER_PROMO_ALIAS,
                true,
                null,
                NO_AUTH,
                null,
                null,
                EXCEL,
                null,
                null,
                "email@email.email"
        );

        final String bunchId = marketLoyaltyClient.saveBunchRequest(request);

        checkSavedRequest(request, bunchId, testPromo.getId());
    }

    @Test
    public void shouldSaveBunchRequestWithRelativeInput() {
        promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
                        .setActionCodeInternal(ANOTHER_PROMO_ALIAS)
        );

        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                testPromo.getId(),
                ANOTHER_PROMO_ALIAS,
                true,
                null,
                AUTH,
                "input",
                null,
                EXCEL,
                null,
                null,
                "email@email.email"
        );

        final String bunchId = marketLoyaltyClient.saveBunchRequest(request);

        checkSavedRequest(request, bunchId, testPromo.getId());
    }

    @Test
    public void shouldSaveBunchRequestWithRelativeOutput() {
        promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
                        .setActionCodeInternal(ANOTHER_PROMO_ALIAS)
        );

        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                testPromo.getId(),
                ANOTHER_PROMO_ALIAS,
                true,
                null,
                NO_AUTH,
                null,
                null,
                YT,
                "output",
                null,
                null
        );

        final String bunchId = marketLoyaltyClient.saveBunchRequest(request);

        checkSavedRequest(request, bunchId, testPromo.getId());
    }

    @Test
    public void shouldSaveBunchRequestWithDefaultNoAuthType() {
        promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFreeDelivery()
                        .setActionCodeInternal(ANOTHER_PROMO_ALIAS)
        );

        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                testPromo.getId(),
                ANOTHER_PROMO_ALIAS,
                true,
                null,
                null,
                null,
                null,
                NONE,
                null,
                null,
                null
        );

        final String bunchId = marketLoyaltyClient.saveBunchRequest(request);

        checkSavedRequest(request, bunchId, testPromo.getId());
    }

    @Test(expected = MarketLoyaltyException.class)
    public void shouldFailWrongBunchRequestNorPromoIdNotPromoAliasSpecified() {
        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                null,
                null,
                true,
                null,
                NO_AUTH,
                null,
                null,
                EXCEL,
                null,
                null,
                "email@email.email"
        );

        marketLoyaltyClient.saveBunchRequest(request);
    }

    @Test(expected = MarketLoyaltyException.class)
    public void shouldFailWrongBunchRequestCountNotSpecified() {
        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                null,
                testPromo.getId(),
                null,
                true,
                null,
                NO_AUTH,
                null,
                null,
                CSV,
                null,
                null,
                "email@email.email"
        );

        marketLoyaltyClient.saveBunchRequest(request);
    }

    @Test(expected = MarketLoyaltyException.class)
    public void shouldFailWrongBunchRequestInputForAuthNotSpecified() {
        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                testPromo.getId(),
                null,
                true,
                null,
                AUTH,
                null,
                null,
                CSV,
                null,
                null,
                "email@email.email"
        );

        marketLoyaltyClient.saveBunchRequest(request);
    }

    @Test(expected = MarketLoyaltyException.class)
    public void shouldFailWrongBunchRequestOutputForYtNotSpecified() {
        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                testPromo.getId(),
                null,
                true,
                null,
                AUTH,
                null,
                null,
                YT,
                null,
                null,
                "email@email.email"
        );

        marketLoyaltyClient.saveBunchRequest(request);
    }

    @Test(expected = MarketLoyaltyException.class)
    public void shouldFailWrongBunchRequestEmailForCsvNotSpecified() {
        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                testPromo.getId(),
                null,
                true,
                null,
                NO_AUTH,
                null,
                null,
                CSV,
                null,
                null,
                null
        );

        marketLoyaltyClient.saveBunchRequest(request);
    }

    @Test(expected = MarketLoyaltyException.class)
    public void shouldFailWrongBunchRequestEmailForExcelNotSpecified() {
        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                testPromo.getId(),
                null,
                true,
                null,
                NO_AUTH,
                null,
                null,
                EXCEL,
                null,
                null,
                null
        );

        marketLoyaltyClient.saveBunchRequest(request);
    }


    @Test(expected = MarketLoyaltyException.class)
    public void shouldFailExcessiveBunchRequestInputSpecifiedForNoAuth() {
        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                null,
                null,
                true,
                null,
                NO_AUTH,
                "//input/input_1",
                null,
                CSV,
                null,
                null,
                "email@email.email"
        );

        marketLoyaltyClient.saveBunchRequest(request);
    }

    @Test(expected = MarketLoyaltyException.class)
    public void shouldFailExcessiveBunchRequestOutputSpecifiedForCsv() {
        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                null,
                null,
                true,
                null,
                NO_AUTH,
                "//input/input_1",
                null,
                CSV,
                null,
                null,
                "email@email.email"
        );

        marketLoyaltyClient.saveBunchRequest(request);
    }

    @Test(expected = MarketLoyaltyException.class)
    public void shouldFailExcessiveBunchRequestOutputSpecifiedForExcel() {
        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                null,
                null,
                true,
                null,
                NO_AUTH,
                null,
                null,
                YT,
                null,
                null,
                "email@email.email"
        );

        marketLoyaltyClient.saveBunchRequest(request);
    }

    @Test(expected = MarketLoyaltyException.class)
    public void shouldFailExcessiveBunchRequestEmailSpecifiedForYt() {
        BunchSaveRequest request = new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                null,
                null,
                true,
                null,
                NO_AUTH,
                "//input/input_1",
                null,
                YT,
                null,
                null,
                "email@email.email"
        );

        marketLoyaltyClient.saveBunchRequest(request);
    }


    @NotNull
    private BunchGenerationRequest checkSavedRequest(BunchSaveRequest request, String batchId, long promoId) {
        BunchGenerationRequest savedRequest = bunchGenerationService.getBunchGenerationRequest(batchId);
        assertThat(
                savedRequest,
                allOf(
                        hasProperty("key", equalTo(DEFAULT_UNIQUE_KEY)),
                        hasProperty("promoId", equalTo(promoId)),
                        hasProperty("count", equalTo(request.getCount()))
                )
        );

        final CoinGeneratorType savedType = savedRequest
                .getParam(COIN_GENERATOR_TYPE).orElse(null);
        final String savedInputTable = savedRequest
                .getParam(INPUT_TABLE).orElse(null);
        final String savedInputCluster = savedRequest
                .getParam(INPUT_TABLE_CLUSTER).orElse(null);
        final String savedOutputTable = savedRequest
                .getParam(OUTPUT_TABLE).orElse(null);
        final String savedOutputCluster = savedRequest
                .getParam(OUTPUT_TABLE_CLUSTER).orElse(null);
        final String savedErrorsOutputTable = savedRequest
                .getParam(ERROR_OUTPUT_TABLE).orElse(null);
        final CoinCreationReason savedReason = savedRequest
                .getParam(COIN_CREATION_REASON).orElse(null);
        final Boolean savedIgnoreBudgetExhaustion = savedRequest
                .getParam(IGNORE_BUDGET_EXHAUSTION).orElse(null);
        if (request.getType() == null) {
            assertThat(
                    savedType,
                    equalTo(NO_AUTH)
            );
        }
        if (savedType == AUTH || savedType == AUTH_DYNAMIC) {
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
        }
        if (request.getReason() == null) {
            assertThat(
                    savedReason,
                    is(nullValue())
            );
        } else {
            assertThat(
                    savedReason,
                    equalTo(request.getReason())
            );
        }
        if (request.getOutputFormat() == null || request.getOutputFormat() == NONE) {
            assertThat(
                    savedRequest,
                    hasProperty("format", equalTo(NONE))
            );
        } else {
            assertThat(
                    savedRequest,
                    hasProperty("format", equalTo(request.getOutputFormat()))
            );
            if (request.getOutputFormat() == YT) {
                assertThat(
                        savedOutputTable,
                        allOf(
                                is(not(nullValue())),
                                anyOf(
                                        equalTo(request.getOutput()),
                                        equalTo("//tmp/market-promo-test/bunch_request/output/" + request.getOutput())
                                )
                        )
                );
                assertThat(
                        savedOutputCluster,
                        equalTo("hahn")
                );
                if (request.getErrorsOutput() != null) {
                    assertThat(
                            savedErrorsOutputTable,
                            equalTo(request.getErrorsOutput())
                    );
                } else {
                    assertThat(
                            savedErrorsOutputTable,
                            anyOf(
                                    equalTo(request.getOutput() + ERRORS_TABLE_NAME_POSTFIX),
                                    equalTo("//tmp/market-promo-test/bunch_request/output/" + request.getOutput() + ERRORS_TABLE_NAME_POSTFIX)
                            )
                    );
                }
            }
        }
        assertThat(
                savedIgnoreBudgetExhaustion,
                equalTo(request.getIgnoreBudgetExhaustion())
        );
        return savedRequest;
    }

    @NotNull
    private static BunchSaveRequest createDefaultBunchRequest(String promoAlias, Long promoId) {
        return new BunchSaveRequest(
                DEFAULT_UNIQUE_KEY,
                10,
                promoId,
                promoAlias,
                null,
                CoinCreationReason.EMAIL_COMPANY,
                AUTH,
                "//input/some_new_input",
                null,
                YT,
                "//output/some_new_output",
                "//output/errorsOutput",
                null
        );
    }

    private static void verifyBunchCheckResponse(
            BunchGenerationRequest request, CoinBunchCheckResponse checkResponse, BunchCheckResponseStatus status
    ) {
        assertThat(
                checkResponse,
                allOf(
                        hasProperty(
                                "input",
                                equalTo(request.getRequiredParamOrThrowException(INPUT_TABLE))
                        ),
                        hasProperty(
                                "output",
                                equalTo(request.getRequiredParamOrThrowException(OUTPUT_TABLE))
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

    private static void verifyBunchCheckResponse(
            BunchGenerationRequest request, CoinBunchCheckResponse checkResponse, BunchCheckResponseStatus status,
            Map<CoinCreationError, Integer> errors
    ) {
        assertThat(
                checkResponse,
                allOf(
                        hasProperty(
                                "input",
                                equalTo(request.getRequiredParamOrThrowException(INPUT_TABLE))
                        ),
                        hasProperty(
                                "output",
                                equalTo(request.getRequiredParamOrThrowException(OUTPUT_TABLE))
                        ),
                        hasProperty(
                                "processedCount",
                                equalTo((long) request.getProcessedCount())
                        ),
                        hasProperty(
                                "totalCount",
                                equalTo(checkResponse.getTotalCount())
                        ),
                        hasProperty("status", equalTo(status)),
                        hasProperty(
                                "errors",
                                allOf(
                                        errors.entrySet().stream()
                                                .map(e -> hasEntry(e.getKey(), e.getValue()))
                                                .collect(Collectors.toList())
                                )
                        )
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
