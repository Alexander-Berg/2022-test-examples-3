package ru.yandex.market.loyalty.admin.tms;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.core.config.DatasourceType;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.promo.MarketDepartment;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.BudgetService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.service.mail.YabacksMailer;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.OrderRequestUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.loyalty.admin.config.TestAuthorizationContext.DEFAULT_USER_NAME;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.QA_ALERT_EMAIL;
import static ru.yandex.market.loyalty.core.utils.CoinRequestUtils.defaultAuth;

@TestFor(AlertEmailQueueProcessor.class)
public class AlertEmailQueueProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    private static final String QA_EMAIL = "qa@yandex-team-for-test.ru";
    @Value("${market.loyalty.alert.mail.subscription}")
    private String alertEmail;
    @Autowired
    private AlertEmailQueueProcessor alertEmailQueueProcessor;
    @Autowired
    private CoinService coinService;
    @Autowired
    private DiscountService discountService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private YabacksMailer yabacksMailer;
    @Autowired
    private PromoEndExecutor checkPromoEndProcessor;
    @Autowired
    private BudgetService budgetService;
    @Autowired
    private DiscountUtils discountUtils;


    @Test
    public void shouldSendNotificationToDepartmentEmail() {
        configurationService.set(QA_ALERT_EMAIL, QA_EMAIL);

        final Promo smartShoppingPromo =
                promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed(BigDecimal.valueOf(300))
                .setStatus(PromoStatus.ACTIVE)
                .setMarketDepartment(MarketDepartment.QA)
                .setBudget(BigDecimal.valueOf(100))
                .setBudgetThreshold(BigDecimal.valueOf(10000))
        );

        CoinKey coinKey = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithBundlesRequest order = OrderRequestUtils.orderRequestWithBundlesBuilder().withOrderItem().build();

        DatasourceType.READ_WRITE.within(() ->
                discountService.spendDiscount(
                        DiscountRequestWithBundlesBuilder.builder(order).withCoins(coinKey).build(),
                        configurationService.currentPromoApplicabilityPolicy(),
                        null
                )
        );

        alertEmailQueueProcessor.processEmailQueue(100);

        verify(yabacksMailer, atLeast(1)).sendMail(eq(QA_EMAIL), eq(alertEmail), anyString(), anyString());
    }

    @Test
    public void shouldSendNotificationOnlyOnce() {
        final Promo smartShoppingPromo =
                promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed(BigDecimal.valueOf(300))
                .setStatus(PromoStatus.ACTIVE)
                .setBudget(BigDecimal.valueOf(299))
                .setBudgetThreshold(BigDecimal.valueOf(10000))
                .setCanBeRestoredFromReserveBudget(false)
        );

        // использование первой монеты
        CoinKey coinKey1 = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithBundlesRequest order1 =
                OrderRequestUtils.orderRequestWithBundlesBuilder().withOrderId("1").withOrderItem().build();

        DatasourceType.READ_WRITE.within(() ->
                discountService.spendDiscount(
                        DiscountRequestWithBundlesBuilder.builder(order1).withCoins(coinKey1).build(),
                        configurationService.currentPromoApplicabilityPolicy(),
                        null
                )
        );

        alertEmailQueueProcessor.processEmailQueue(100);
        verify(yabacksMailer, atLeast(1)).sendMail(eq(alertEmail), isNull(), anyString(), anyString());
        verify(yabacksMailer, atLeast(1)).sendMail(eq(DEFAULT_USER_NAME + "@yandex-team.ru"), anyString(), anyString());
        verifyNoMoreInteractions(yabacksMailer);

        // использование второй монеты
        CoinKey coinKey2 = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithBundlesRequest order2 =
                OrderRequestUtils.orderRequestWithBundlesBuilder().withOrderId("2").withOrderItem().build();

        DatasourceType.READ_WRITE.within(() ->
                discountService.spendDiscount(
                        DiscountRequestWithBundlesBuilder.builder(order2).withCoins(coinKey2).build(),
                        configurationService.currentPromoApplicabilityPolicy(),
                        null
                )
        );

        alertEmailQueueProcessor.processEmailQueue(100);
        verifyNoMoreInteractions(yabacksMailer);
    }

    @Test
    public void shouldSendBudgetExceeded() {
        configurationService.set(QA_ALERT_EMAIL, QA_EMAIL);

        final Promo smartShoppingPromo =
                promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed(BigDecimal.valueOf(300))
                .setStatus(PromoStatus.ACTIVE)
                .setBudget(BigDecimal.valueOf(299))
                .setMarketDepartment(MarketDepartment.QA)
                .setBudgetThreshold(BigDecimal.valueOf(100))
                .setCanBeRestoredFromReserveBudget(false)
        );

        // использование первой монеты
        CoinKey coinKey1 = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithBundlesRequest order1 =
                OrderRequestUtils.orderRequestWithBundlesBuilder().withOrderId("1").withOrderItem().build();

        DatasourceType.READ_WRITE.within(() ->
                discountService.spendDiscount(
                        DiscountRequestWithBundlesBuilder.builder(order1).withCoins(coinKey1).build(),
                        configurationService.currentPromoApplicabilityPolicy(),
                        null
                )
        );

        alertEmailQueueProcessor.processEmailQueue(100);
        verify(yabacksMailer).sendMail(eq(QA_EMAIL), eq(alertEmail),
                contains("Бюджет акции " + smartShoppingPromo.getName() + " закончился"), anyString());
    }

    @Test
    public void shouldSendSuccessWriteOff() {
        supplementaryDataLoader.createReserveIfNotExists(BigDecimal.valueOf(1_000_000));

        configurationService.set(QA_ALERT_EMAIL, QA_EMAIL);

        final Promo smartShoppingPromo =
                promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed(BigDecimal.valueOf(300))
                .setStatus(PromoStatus.ACTIVE)
                .setBudget(BigDecimal.valueOf(299))
                .setMarketDepartment(MarketDepartment.QA)
                .setBudgetThreshold(BigDecimal.valueOf(10000))
                .setCanBeRestoredFromReserveBudget(true)
        );

        // использование первой монеты
        CoinKey coinKey1 = coinService.create.createCoin(smartShoppingPromo, defaultAuth().build());

        OrderWithBundlesRequest order1 =
                OrderRequestUtils.orderRequestWithBundlesBuilder().withOrderId("1").withOrderItem().build();

        DatasourceType.READ_WRITE.within(() ->
                discountService.spendDiscount(
                        DiscountRequestWithBundlesBuilder.builder(order1).withCoins(coinKey1).build(),
                        configurationService.currentPromoApplicabilityPolicy(),
                        null
                )
        );

        budgetService.awaitExecutor();

        alertEmailQueueProcessor.processEmailQueue(100);
        verify(yabacksMailer).sendMail(eq(QA_EMAIL), eq(alertEmail), eq("Списание с резервного бюджета"), anyString());
    }

    @Test
    public void shouldSendNotificationToDefaultEmail() {
        configurationService.set(QA_ALERT_EMAIL, QA_EMAIL);

        promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed()
                .setStatus(PromoStatus.ACTIVE)
                .setMarketDepartment(MarketDepartment.QA)
                .setStartDate(new Date(clock.instant().minus(1, ChronoUnit.DAYS).toEpochMilli()))
                .setEndDate(new Date(clock.instant().plus(1, ChronoUnit.DAYS).toEpochMilli()))
        );

        checkPromoEndProcessor.checkPromoEnd();

        verify(yabacksMailer).sendMail(eq(alertEmail), anyString(), anyString());
    }

}
