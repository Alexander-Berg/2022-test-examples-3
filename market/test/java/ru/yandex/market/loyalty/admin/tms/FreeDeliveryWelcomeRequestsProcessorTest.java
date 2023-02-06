package ru.yandex.market.loyalty.admin.tms;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.dao.welcome.FreeDeliveryRequestRegisterDao;
import ru.yandex.market.loyalty.core.dao.welcome.WelcomePromoRequestStatus;
import ru.yandex.market.loyalty.core.dao.ydb.AllUserOrdersDao;
import ru.yandex.market.loyalty.core.dao.ydb.model.UserOrder;
import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.model.delivery.FreeDeliveryRequestFromRegister;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.model.promo.SmartShoppingPromoBuilder;
import ru.yandex.market.loyalty.core.rule.MinOrderTotalCuttingRule;
import ru.yandex.market.loyalty.core.rule.RuleContainer;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.rule.RulesContainer;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.UserBlacklistService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.lightweight.DateUtils;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.Month;
import java.time.YearMonth;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.market.loyalty.core.dao.welcome.WelcomePromoRequestStatus.SUCCESS;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_COINS_LIMIT;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@TestFor(FreeDeliveryWelcomeRequestsProcessor.class)
public class FreeDeliveryWelcomeRequestsProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoUtils promoUtils;
    @Autowired
    private FreeDeliveryWelcomeRequestsProcessor freeDeliveryWelcomeRequestsProcessor;
    @Autowired
    private CoinService coinService;
    @Autowired
    UserBlacklistService userBlacklistService;
    @Autowired
    AllUserOrdersDao allUserOrdersDao;
    @Autowired
    ClockForTests clockForTests;
    @Autowired
    private FreeDeliveryRequestRegisterDao freeDeliveryRequestRegisterDao;


    private Promo activePromo;
    private static final java.util.Date START_DATE = DateUtils.toDate(
            YearMonth.of(2020, Month.FEBRUARY).atDay(1)
    );
    private static final java.util.Date END_DATE = DateUtils.toDate(YearMonth.of(2020, Month.FEBRUARY).atEndOfMonth());

    @Before
    public void init() {
        userBlacklistService.reloadBlacklist();
        allUserOrdersDao.upsert(
                new UserOrder(DEFAULT_UID, OrderStatus.PROCESSING.name(), Instant.now(), "binding_key",
                        Platform.DESKTOP));
        clockForTests.setDate(START_DATE);
        initPromos(START_DATE, END_DATE);
    }

    private void createFreeDeliveryRequestWithStatus(WelcomePromoRequestStatus status) {
        FreeDeliveryRequestFromRegister newFreeDeliveryRequestFromRegister =
                FreeDeliveryRequestFromRegister
                        .builder()
                        .withUid(DEFAULT_UID)
                        .withStatus(status)
                        .withMessage("")
                        .withCreationTime(new java.util.Date())
                        .withPreliminary(false)
                        .withReqId("")
                        .withProcessingTime(null)
                        .withTryCount(0)
                        .build();
        freeDeliveryRequestRegisterDao.createDeliveryPromoRequest(newFreeDeliveryRequestFromRegister);
    }

    private void initPromos(java.util.Date startDate, java.util.Date endDate) {
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

    private void configureActivePromos() {
        configurationService.configureFreeDeliveryPromo(
                activePromo
        );
        promoUtils.reloadFreeDeliveryPromosCache();
    }

    private static <T extends SmartShoppingPromoBuilder<T>> SmartShoppingPromoBuilder<T> testPromoBuilder(
            SmartShoppingPromoBuilder<T> smartShoppingPromoBuilder, java.util.Date startDate, java.util.Date endDate
    ) {
        return smartShoppingPromoBuilder
                .setStartDate(startDate)
                .setEndDate(endDate)
                .setStatus(PromoStatus.ACTIVE)
                .setBindOnlyOnce(true);
    }

    @Test
    public void shouldCreateRequests() {
        createFreeDeliveryRequestWithStatus(WelcomePromoRequestStatus.IN_QUEUE);
        configureActivePromos();

        freeDeliveryWelcomeRequestsProcessor.processFreeDeliveryWelcomeRequests(500, Duration.ofSeconds(0));

        assertThat(
                coinService.search.getCoinsByUid(DEFAULT_UID, DEFAULT_COINS_LIMIT),
                allOf(
                        hasSize(1)
                )
        );

        assertThat(
                "Если все монетки создались то заявка должна переводится в SUCCESS",
                freeDeliveryRequestRegisterDao.getRequestsWithStatus(SUCCESS, 500),
                hasSize(1)
        );
    }

    @Test
    public void shouldNotCreateCoinRequestIfFreeDeliveryRequestIsInCancelledState() {
        createFreeDeliveryRequestWithStatus(WelcomePromoRequestStatus.CANCELLED);
        configureActivePromos();

        freeDeliveryWelcomeRequestsProcessor.processFreeDeliveryWelcomeRequests(500, Duration.ofSeconds(0));

        assertThat(
                coinService.search.getCoinsByUid(DEFAULT_UID, DEFAULT_COINS_LIMIT),
                allOf(
                        hasSize(0)
                )
        );

        assertThat(
                freeDeliveryRequestRegisterDao.getRequestsWithStatus(WelcomePromoRequestStatus.CANCELLED, 500),
                hasSize(1)
        );
    }
}
