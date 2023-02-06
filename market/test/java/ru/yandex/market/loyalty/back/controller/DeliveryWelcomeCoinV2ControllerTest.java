package ru.yandex.market.loyalty.back.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Month;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.antifraud.orders.web.dto.LoyaltyBuyerRestrictionsDto;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.api.model.welcome.DeliveryWelcomeCoinCheckResponse;
import ru.yandex.market.loyalty.api.model.welcome.DeliveryWelcomeCoinPromoStatus;
import ru.yandex.market.loyalty.api.model.welcome.DeliveryWelcomeCoinUserStatus;
import ru.yandex.market.loyalty.api.model.welcome.FreeDeliveryCoinCheckRequest;
import ru.yandex.market.loyalty.api.model.welcome.FreeDeliveryCoinRegistrationRequest;
import ru.yandex.market.loyalty.api.model.welcome.FreeDeliveryCoinRegistrationResponse;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.loyalty.core.dao.DeviceInfoDao;
import ru.yandex.market.loyalty.core.dao.UserBlackListDao;
import ru.yandex.market.loyalty.core.dao.welcome.FreeDeliveryRequestRegisterDao;
import ru.yandex.market.loyalty.core.dao.welcome.WelcomePromoRequestMessageCode;
import ru.yandex.market.loyalty.core.dao.welcome.WelcomePromoRequestStatus;
import ru.yandex.market.loyalty.core.dao.ydb.AllUserOrdersDao;
import ru.yandex.market.loyalty.core.dao.ydb.model.UserOrder;
import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.model.BlacklistRecord;
import ru.yandex.market.loyalty.core.model.RegionSettings;
import ru.yandex.market.loyalty.core.model.delivery.FreeDeliveryRequestFromRegister;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.model.promo.SmartShoppingPromoBuilder;
import ru.yandex.market.loyalty.core.rule.MinOrderTotalCuttingRule;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.rule.RulesContainer;
import ru.yandex.market.loyalty.core.service.DeviceInfoService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.RegionSettingsService;
import ru.yandex.market.loyalty.core.service.UserBlacklistService;
import ru.yandex.market.loyalty.core.service.welcome.FreeDeliveryPromoRegisterRequestsService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.lightweight.DateUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_PLUS;
import static ru.yandex.market.loyalty.api.model.welcome.DeliveryWelcomeCoinPromoStatus.PROMO_NOT_ACTIVE;
import static ru.yandex.market.loyalty.api.model.welcome.DeliveryWelcomeCoinPromoStatus.REGION_NOT_ALLOWED;
import static ru.yandex.market.loyalty.api.model.welcome.DeliveryWelcomeCoinPromoStatus.REGISTRATION_ALLOWED;
import static ru.yandex.market.loyalty.api.model.welcome.DeliveryWelcomeCoinPromoStatus.REGISTRATION_NOT_ALLOWED;
import static ru.yandex.market.loyalty.back.util.AntifraudUtils.setupDeclinedAntifraudResponseForUid;
import static ru.yandex.market.loyalty.core.dao.query.Field.bigint;
import static ru.yandex.market.loyalty.core.dao.query.Field.text;
import static ru.yandex.market.loyalty.core.dao.welcome.WelcomePromoRequestMessageCode.ANTIFRAUD;
import static ru.yandex.market.loyalty.core.dao.welcome.WelcomePromoRequestMessageCode.BLACKLIST;
import static ru.yandex.market.loyalty.core.dao.welcome.WelcomePromoRequestStatus.CANCELLED;
import static ru.yandex.market.loyalty.core.dao.welcome.WelcomePromoRequestStatus.IN_QUEUE;
import static ru.yandex.market.loyalty.core.dao.welcome.WelcomePromoRequestStatus.SUCCESS;
import static ru.yandex.market.loyalty.core.test.BlackboxUtils.mockBlackbox;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.ANOTHER_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_SBER_ID_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

/**
 * @author <a href="mailto:khamitov-rail@yandex-team.ru">Rail Khamitov</a>
 * @date 17.12.2020
 */
@TestFor(DeliveryWelcomeCoinV2Controller.class)
public class DeliveryWelcomeCoinV2ControllerTest extends MarketLoyaltyBackMockedDbTestBase {
    private static final int FREE_DELIVERY_BONUS_TEST_REGION = 213;
    private static final FreeDeliveryCoinCheckRequest TEST_CHECK_REQUEST = new FreeDeliveryCoinCheckRequest(
            FREE_DELIVERY_BONUS_TEST_REGION,
            DEFAULT_UID
    );

    private static final FreeDeliveryCoinRegistrationRequest TEST_REGISTRATION_REQUEST =
            new FreeDeliveryCoinRegistrationRequest(
                    FREE_DELIVERY_BONUS_TEST_REGION,
                    DEFAULT_UID
            );

    private static final Date START_DATE = DateUtils.toDate(
            YearMonth.of(2020, Month.FEBRUARY).atDay(1)
    );
    private static final Date END_DATE = DateUtils.toDate(YearMonth.of(2020, Month.FEBRUARY).atEndOfMonth());
    @Autowired
    RegionSettingsService regionSettingsService;
    @Autowired
    PromoUtils promoUtils;
    @Autowired
    PromoManager promoManager;
    @Autowired
    ClockForTests clockForTests;
    @Autowired
    UserBlackListDao userBlackListDao;
    @Autowired
    UserBlacklistService userBlacklistService;
    @Autowired
    AllUserOrdersDao allUserOrdersDao;
    @Autowired
    FreeDeliveryRequestRegisterDao freeDeliveryRequestRegisterDao;
    @Autowired
    RestTemplate antifraudRestTemplate;
    @Autowired
    DeviceInfoDao deviceInfoDao;
    @Autowired
    DeviceInfoService deviceInfoService;
    @Autowired
    FreeDeliveryPromoRegisterRequestsService freeDeliveryPromoRegisterRequestsService;
    @Autowired
    private MarketLoyaltyClient marketLoyaltyClient;

    private Promo activePromo;

    private static void assertBadRequestExceptionCause(MarketLoyaltyException marketLoyaltyException) {
        assertThat(marketLoyaltyException.getCause(), isA(HttpClientErrorException.BadRequest.class));
    }

    private static <T extends SmartShoppingPromoBuilder<T>> SmartShoppingPromoBuilder<T> testPromoBuilder(
            SmartShoppingPromoBuilder<T> smartShoppingPromoBuilder, Date startDate, Date endDate
    ) {
        return smartShoppingPromoBuilder
                .setStartDate(startDate)
                .setEndDate(endDate)
                .setStatus(PromoStatus.ACTIVE).setBindOnlyOnce(true);
    }

    private static void checkException(
            MarketLoyaltyException marketLoyaltyException,
            MarketLoyaltyErrorCode expectedErrorCode
    ) {
        assertEquals(
                marketLoyaltyException.getMessage(),
                expectedErrorCode.getDefaultDescription()
        );
    }

    private static void assertNotRegisteredWithPromoStatus(
            DeliveryWelcomeCoinCheckResponse welcomeCoinCheckResponse, DeliveryWelcomeCoinPromoStatus promoStatus
    ) {
        assertEquals(
                promoStatus,
                welcomeCoinCheckResponse.getPromoStatus()
        );
        assertEquals(
                DeliveryWelcomeCoinUserStatus.NOT_REGISTERED,
                welcomeCoinCheckResponse.getUserStatus()
        );
    }

    @Before
    public void setUp() {
        userBlacklistService.reloadBlacklist();
        allUserOrdersDao.upsert(
                new UserOrder(DEFAULT_UID, OrderStatus.PROCESSING.name(), Instant.now(), "binding_key",
                        Platform.DESKTOP));
        clockForTests.setDate(START_DATE);
        initPromos(START_DATE, END_DATE);
    }

    private void initPromos(Date startDate, Date endDate) {
        RulesContainer rulesContainer = new RulesContainer();
        RuleContainer<MinOrderTotalCuttingRule> minOrderTotalCuttingRule = RuleContainer
                .builder(RuleType.MIN_ORDER_TOTAL_CUTTING_RULE)
                .withParams(RuleParameterName.MIN_ORDER_TOTAL, Collections.singleton(BigDecimal.valueOf(699)))
                .build();
        rulesContainer.add(minOrderTotalCuttingRule);
        activePromo = promoManager.createSmartShoppingPromo(
                testPromoBuilder(PromoUtils.SmartShopping.defaultFreeDelivery(), startDate, endDate)
                        .setStatus(PromoStatus.ACTIVE)
                        .setRulesContainer(rulesContainer)
        );
        promoUtils.reloadFreeDeliveryPromosCache();
    }

    @Test
    @Ignore
    public void shouldFailedOnInvalidRegionIdValueInRegistrationRequest() {
        assertBadRequestExceptionCause(assertThrows(
                MarketLoyaltyException.class,
                () -> marketLoyaltyClient.registerFreeDeliveryCoinUser(
                        new FreeDeliveryCoinRegistrationRequest(0, 1)
                )
        ));
        ensureRegistrationFailed(1L);
    }

    @Test
    @Ignore
    public void shouldFailedRegisterDoubleCoinRequest() {
        setupActivePromosAndEnableDeliveryWelcomeBonusInTestRegion();
        marketLoyaltyClient.registerFreeDeliveryCoinUser(TEST_REGISTRATION_REQUEST);
        expectedCountInQueueRequestCreated(TEST_REGISTRATION_REQUEST.getUid(), 1);
        marketLoyaltyClient.registerFreeDeliveryCoinUser(TEST_REGISTRATION_REQUEST);
        expectedCountInQueueRequestCreated(TEST_REGISTRATION_REQUEST.getUid(), 1);
    }

    @Test
    @Ignore
    public void shouldFailedOnInvalidUidValueInRegistrationRequest() {
        assertBadRequestExceptionCause(assertThrows(
                MarketLoyaltyException.class,
                () -> marketLoyaltyClient.registerFreeDeliveryCoinUser(
                        new FreeDeliveryCoinRegistrationRequest(
                                FREE_DELIVERY_BONUS_TEST_REGION,
                                0
                        )
                )
        ));
        ensureRegistrationFailed(0L);
    }

    @Test
    @Ignore
    public void shouldThrowRegionNotAllowedException() {
        MarketLoyaltyException marketLoyaltyException = assertThrows(
                MarketLoyaltyException.class,
                () -> marketLoyaltyClient.registerFreeDeliveryCoinUser(TEST_REGISTRATION_REQUEST)
        );
        checkException(marketLoyaltyException, MarketLoyaltyErrorCode.REGION_NOT_ALLOWED);
        ensureRegistrationFailed(TEST_REGISTRATION_REQUEST.getUid());
    }

    @Test
    @Ignore
    public void shouldThrowUserNotAllowedForBlacklistedUser() {
        configurationService.configureFreeDeliveryPromo(activePromo);
        enableDeliveryWelcomeBonusInTestRegion();

        assertFalse(userBlacklistService.uidInBlacklist(DEFAULT_UID));
        addTestUserInBlacklist();

        checkException(assertThrows(
                MarketLoyaltyException.class,
                () -> marketLoyaltyClient.registerFreeDeliveryCoinUser(TEST_REGISTRATION_REQUEST)
        ), MarketLoyaltyErrorCode.USER_NOT_ALLOWED);

        checkRequestStatusAndRegistrationFail(BLACKLIST, DEFAULT_UID);
    }

    @Test
    @Ignore
    public void shouldCreateCoinRequestAndReturnCountWhenNeitherRestrictionViolated() {
        setupActivePromosAndEnableDeliveryWelcomeBonusInTestRegion();

        doChecksForSuccessfulRequestCoinsResponse(
                DEFAULT_UID,
                marketLoyaltyClient.registerFreeDeliveryCoinUser(
                        TEST_REGISTRATION_REQUEST
                )
        );
    }

    @Test
    @Ignore
    public void shouldReturnPromoNotActiveIfFreeDeliveryPromoNotConfigured() {
        assertNotRegisteredWithPromoStatus(marketLoyaltyClient.checkFreeDeliveryCoinUser(
                new FreeDeliveryCoinCheckRequest(
                        FREE_DELIVERY_BONUS_TEST_REGION,
                        DEFAULT_UID
                )
        ), PROMO_NOT_ACTIVE);
    }

    @Test
    @Ignore
    public void shouldAllowRegistrationIfUidIsNotNullButDeviceIdIsMissedOnCheck() {
        setupActivePromosAndEnableDeliveryWelcomeBonusInTestRegion();
        assertNotRegisteredWithPromoStatus(
                marketLoyaltyClient.checkFreeDeliveryCoinUser(
                        new FreeDeliveryCoinCheckRequest(
                                FREE_DELIVERY_BONUS_TEST_REGION,
                                DEFAULT_UID
                        )
                ), REGISTRATION_ALLOWED);
    }

    @Test
    @Ignore
    public void shouldFailRegistrationCheckIfFreeDeliveryBonusDisabledInRegion() {
        configureActivePromos();
        assertNotRegisteredWithPromoStatus(marketLoyaltyClient.checkFreeDeliveryCoinUser(
                TEST_CHECK_REQUEST
        ), REGION_NOT_ALLOWED);
    }

    @Test
    @Ignore
    public void shouldReturnRegistrationNotAllowedIfUserInBlacklist() {
        addTestUserInBlacklist();
        setupActivePromosAndEnableDeliveryWelcomeBonusInTestRegion();
        DeliveryWelcomeCoinCheckResponse welcomeCoinCheckResponse = marketLoyaltyClient.checkFreeDeliveryCoinUser(
                TEST_CHECK_REQUEST
        );
        assertNotRegisteredWithPromoStatus(
                welcomeCoinCheckResponse, REGISTRATION_NOT_ALLOWED);
    }

    @Test
    @Ignore
    public void shouldReturnNotRegisteredAndRegistrationAllowedOnCheckIfRequestValidAndUserNotRegistered() {
        setupActivePromosAndEnableDeliveryWelcomeBonusInTestRegion();
        DeliveryWelcomeCoinCheckResponse welcomeCoinCheckResponse = marketLoyaltyClient.checkFreeDeliveryCoinUser(
                TEST_CHECK_REQUEST
        );
        assertNotRegisteredWithPromoStatus(
                welcomeCoinCheckResponse, DeliveryWelcomeCoinPromoStatus.REGISTRATION_ALLOWED);
    }

    @Test
    @Ignore
    public void shouldFailRegistrationWithUserNotAllowedIfAntifraudDeclineCoinCreation() {
        setupActivePromosAndEnableDeliveryWelcomeBonusInTestRegion();
        setupDeclinedAntifraudResponseForUid(
                antifraudRestTemplate,
                DEFAULT_UID,
                LoyaltyBuyerRestrictionsDto.prohibited(DEFAULT_UID, null)
        );
        checkException(
                assertThrows(
                        MarketLoyaltyException.class,
                        () -> marketLoyaltyClient.registerFreeDeliveryCoinUser(TEST_REGISTRATION_REQUEST)
                ),
                MarketLoyaltyErrorCode.USER_NOT_ALLOWED
        );

        checkRequestStatusAndRegistrationFail(ANTIFRAUD, DEFAULT_UID);
    }

    //[TODO] Enable after fix ticket https://st.yandex-team.ru/MARKETDISCOUNT-4113
    @Test
    @Ignore
    public void shouldMoveRegistrationOnUserMerge() {
        setupActivePromosAndEnableDeliveryWelcomeBonusInTestRegion();
        doChecksForSuccessfulRequestCoinsResponse(
                DEFAULT_SBER_ID_UID,
                marketLoyaltyClient.registerFreeDeliveryCoinUser(
                        new FreeDeliveryCoinRegistrationRequest(
                                FREE_DELIVERY_BONUS_TEST_REGION,
                                DEFAULT_SBER_ID_UID
                        )
                )
        );
        assertEquals(
                DeliveryWelcomeCoinUserStatus.NOT_REGISTERED,
                marketLoyaltyClient.checkFreeDeliveryCoinUser(
                        new FreeDeliveryCoinCheckRequest(
                                FREE_DELIVERY_BONUS_TEST_REGION,
                                DEFAULT_UID
                        )
                ).getUserStatus()
        );

        marketLoyaltyClient.mergeUsers(DEFAULT_SBER_ID_UID, DEFAULT_UID);

        assertEquals(
                DeliveryWelcomeCoinUserStatus.REGISTERED,
                marketLoyaltyClient.checkFreeDeliveryCoinUser(
                        new FreeDeliveryCoinCheckRequest(
                                FREE_DELIVERY_BONUS_TEST_REGION,
                                DEFAULT_UID
                        )
                ).getUserStatus()
        );
    }

    @Test
    @Ignore
    public void shouldReturnNotRegisteredAndRegistrationAllowedOnCheckIfRequestValidAndUserNotRegisteredAndTableCheckNotEmpty() throws InterruptedException {
        setupActivePromosAndEnableDeliveryWelcomeBonusInTestRegion();
        marketLoyaltyClient.registerFreeDeliveryCoinUser(TEST_REGISTRATION_REQUEST);
        //wait create coins
        Thread.sleep(5000);
        DeliveryWelcomeCoinCheckResponse welcomeCoinCheckResponse = marketLoyaltyClient.checkFreeDeliveryCoinUser(
                new FreeDeliveryCoinCheckRequest(
                        FREE_DELIVERY_BONUS_TEST_REGION,
                        ANOTHER_UID
                )
        );
        assertNotRegisteredWithPromoStatus(
                welcomeCoinCheckResponse, DeliveryWelcomeCoinPromoStatus.REGISTRATION_ALLOWED);
    }

    @Test
    @Ignore
    public void shouldReturnNotRegisteredAndRegistrationNotAllowedForYaPlus() throws InterruptedException {
        setupActivePromosAndEnableDeliveryWelcomeBonusInTestRegion();
        marketLoyaltyClient.registerFreeDeliveryCoinUser(TEST_REGISTRATION_REQUEST);
        mockBlackbox(123L, YANDEX_PLUS, true, blackboxRestTemplate);
        //wait create coins
        Thread.sleep(5000);
        DeliveryWelcomeCoinCheckResponse welcomeCoinCheckResponse = marketLoyaltyClient.checkFreeDeliveryCoinUser(
                new FreeDeliveryCoinCheckRequest(
                        FREE_DELIVERY_BONUS_TEST_REGION,
                        123L
                )
        );
        assertNotRegisteredWithPromoStatus(
                welcomeCoinCheckResponse, DeliveryWelcomeCoinPromoStatus.REGISTRATION_NOT_ALLOWED);
    }

    @Test
    @Ignore
    public void shouldSaveRegionIdInRequest() {
        setupActivePromosAndEnableDeliveryWelcomeBonusInTestRegion();
        marketLoyaltyClient.registerFreeDeliveryCoinUser(TEST_REGISTRATION_REQUEST);
        List<FreeDeliveryRequestFromRegister> existingRequests = freeDeliveryPromoRegisterRequestsService.getByUid(
                DEFAULT_UID);
        assertNotNull(existingRequests);
        assertEquals(1, existingRequests.size());
        assertTrue(existingRequests.stream().findFirst().isPresent());
        assertEquals(Integer.valueOf(FREE_DELIVERY_BONUS_TEST_REGION),
                existingRequests.stream().findFirst().get().getRegionId());

    }

    private void doChecksForSuccessfulRequestCoinsResponse(Long uid, FreeDeliveryCoinRegistrationResponse response) {
        assertNotEquals(response.getMinOrderTotal(), BigDecimal.ZERO);
    }

    private void setupActivePromosAndEnableDeliveryWelcomeBonusInTestRegion() {
        configureActivePromos();
        enableDeliveryWelcomeBonusInTestRegion();
    }

    private void addTestUserInBlacklist() {
        userBlackListDao.addRecord(new BlacklistRecord.Uid(DEFAULT_UID));
        userBlacklistService.reloadBlacklist();
    }

    private void checkRequestStatusAndRegistrationFail(
            WelcomePromoRequestMessageCode expectedRequestStatus, Long uid
    ) {
        FreeDeliveryRequestFromRegister emissionRequest = getEmissionRequest(CANCELLED, uid);
        checkRequestMessage(expectedRequestStatus, emissionRequest);
        ensureCancelledRequestCreated(uid);
    }

    private void ensureRegistrationFailed(Long uid) {
        Optional<FreeDeliveryRequestFromRegister> optionalRequest = getOptionalEmissionRequest(SUCCESS, uid);
        assertFalse(optionalRequest.isPresent());
    }

    private void ensureCancelledRequestCreated(Long uid) {
        assertTrue(getOptionalEmissionRequest(CANCELLED, uid).isPresent());
    }

    private void ensureInQueueRequestCreated(Long uid) {
        assertTrue(getOptionalEmissionRequest(IN_QUEUE, uid).isPresent());
    }

    private void expectedCountInQueueRequestCreated(Long uid, int expectedCount) {
        assertEquals(getCountRequest(IN_QUEUE, uid), expectedCount);
    }

    private void checkRequestMessage(
            WelcomePromoRequestMessageCode expected, FreeDeliveryRequestFromRegister emissionRequest
    ) {
        Optional<WelcomePromoRequestMessageCode> optionalMessage = Optional.of(
                WelcomePromoRequestMessageCode.findByCode(emissionRequest.getMessage()));
        assertEquals(expected, optionalMessage.get());
    }

    private void configureActivePromos() {
        configurationService.configureFreeDeliveryPromo(activePromo);
        promoUtils.reloadFreeDeliveryPromosCache();
    }

    private FreeDeliveryRequestFromRegister getEmissionRequest(WelcomePromoRequestStatus status, Long uid) {
        Optional<FreeDeliveryRequestFromRegister> optionalEmissionRequest = getOptionalEmissionRequest(status, uid);
        assertTrue(optionalEmissionRequest.isPresent());
        return optionalEmissionRequest.get();
    }

    private Optional<FreeDeliveryRequestFromRegister> getOptionalEmissionRequest(
            WelcomePromoRequestStatus status, Long uid
    ) {
        List<FreeDeliveryRequestFromRegister> byStatus = freeDeliveryRequestRegisterDao.find(
                FreeDeliveryRequestRegisterDao
                        .getFreeDeliveryPromosWhere()
                        .where(bigint("uid").eqTo(uid))
                        .where(text("status").eqTo(status.getCode()))
        );
        if (byStatus == null || byStatus.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(byStatus.get(0));
    }

    private int getCountRequest(
            WelcomePromoRequestStatus status, Long uid
    ) {
        List<FreeDeliveryRequestFromRegister> byStatus = freeDeliveryRequestRegisterDao.find(
                FreeDeliveryRequestRegisterDao
                        .getFreeDeliveryPromosWhere()
                        .where(bigint("uid").eqTo(uid))
                        .where(text("status").eqTo(status.getCode()))
        );
        if (byStatus == null || byStatus.isEmpty()) {
            return 0;
        }
        return byStatus.size();
    }

    private void enableDeliveryWelcomeBonusInTestRegion() {
        regionSettingsService.saveOrUpdateRegionSettings(
                RegionSettings.builder()
                        .withRegionId(FREE_DELIVERY_BONUS_TEST_REGION)
                        .withWelcomeBonusEnabledValue(true).build()
        );
        regionSettingsService.reloadCache();
        promoUtils.reloadFreeDeliveryPromosCache();
    }
}
