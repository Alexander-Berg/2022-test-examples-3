package ru.yandex.direct.core.testing.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.annotation.Nullable;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignMulticurrencySums;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.ContentPromotionCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmYndxFrontpageCampaign;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign;
import ru.yandex.direct.core.entity.campaign.model.InternalDistribCampaign;
import ru.yandex.direct.core.entity.campaign.model.InternalFreeCampaign;
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy;
import ru.yandex.direct.core.testing.data.campaign.TestContentPromotionCampaigns;
import ru.yandex.direct.core.testing.data.campaign.TestCpmBannerCampaigns;
import ru.yandex.direct.core.testing.data.campaign.TestCpmPriceCampaigns;
import ru.yandex.direct.core.testing.data.campaign.TestCpmYndxFrontPageCampaigns;
import ru.yandex.direct.core.testing.data.campaign.TestDynamicCampaigns;
import ru.yandex.direct.core.testing.data.campaign.TestInternalAutobudgetCampaigns;
import ru.yandex.direct.core.testing.data.campaign.TestInternalDistribCampaigns;
import ru.yandex.direct.core.testing.data.campaign.TestInternalFreeCampaigns;
import ru.yandex.direct.core.testing.data.campaign.TestMcBannerCampaigns;
import ru.yandex.direct.core.testing.data.campaign.TestMobileContentCampaigns;
import ru.yandex.direct.core.testing.data.campaign.TestOldCampaigns;
import ru.yandex.direct.core.testing.data.campaign.TestSmartCampaigns;
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils;
import ru.yandex.direct.core.testing.steps.campaign.model0.BalanceInfo;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusPostModerate;
import ru.yandex.direct.core.testing.steps.campaign.model0.context.ContextSettings;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AutobudgetMaxImpressionsCustomPeriodStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AutobudgetMaxImpressionsStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AutobudgetMaxReachCustomPeriodStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AutobudgetMaxReachStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AverageBidStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AverageCpaPerCampStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AverageCpaStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.DayBudget;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.DayBudgetShowMode;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.ManualStrategy;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.ManualStrategyMode;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStrategyName;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.time.LocalDateTime.now;
import static ru.yandex.direct.utils.CommonUtils.nvl;

public final class TestCampaigns {
    public static final int DEFAULT_CONTEXT_PRICE_COEF = 100;
    public static final Long DEFAULT_PLACE_ID_FOR_INTERNAL_CAMPAIGNS = TemplatePlaceRepositoryMockUtils.PLACE_1;
    public static final Long MODERATED_PLACE_ID_FOR_INTERNAL_CAMPAIGNS =
            TemplatePlaceRepositoryMockUtils.MODERATED_PLACE_ID;
    public static final long EMPTY_WALLET_ID = 0L;
    public static final long EMPTY_ORDER_ID = 0L;
    public static final long DEFAULT_TIMEZONE_ID = 130L; // Moscow timezone
    public static final long TEXT_CAMPAIGN_PRODUCT_ID = 503162L;
    public static final String DEFAULT_BANNER_HREF_PARAMS = "utm_var=default_val";

    private TestCampaigns() {
    }

    public static CommonCampaign newCampaignByCampaignType(CampaignType campaignType) {
        CommonCampaign campaign;
        switch (campaignType) {
            case TEXT:
                campaign = new TextCampaign();
                break;
            case DYNAMIC:
                campaign = new DynamicCampaign();
                break;
            case PERFORMANCE:
                campaign = new SmartCampaign();
                break;
            case MOBILE_CONTENT:
                campaign = new MobileContentCampaign();
                break;
            case MCBANNER:
                campaign = new McBannerCampaign();
                break;
            case CPM_BANNER:
                campaign = new CpmBannerCampaign();
                break;
            case CPM_PRICE:
                campaign = new CpmPriceCampaign();
                break;
            case CPM_YNDX_FRONTPAGE:
                campaign = new CpmYndxFrontpageCampaign();
                break;
            case CONTENT_PROMOTION:
                campaign = new ContentPromotionCampaign();
                break;
            case INTERNAL_AUTOBUDGET:
                campaign = new InternalAutobudgetCampaign();
                break;
            case INTERNAL_DISTRIB:
                campaign = new InternalDistribCampaign();
                break;
            case INTERNAL_FREE:
                campaign = new InternalFreeCampaign();
                break;
            default:
                throw new IllegalArgumentException("Неизвестный тип кампании: " + campaignType);
        }
        return campaign.withType(campaignType);
    }

    public static Campaign newCampaignByCampaignTypeOld(CampaignType campaignType, ClientId clientId, Long uid) {
        Campaign campaign = manualCampaignWithoutType(clientId, uid).withType(campaignType);
        if (campaignType == CampaignType.BILLING_AGGREGATE) {
            campaign.withStatusModerate(StatusModerate.YES)
                    .withStatusPostModerate(StatusPostModerate.ACCEPTED);
        }
        return campaign;
    }

    public static Class<? extends BaseCampaign> getCampaignClassByCampaignType(CampaignType campaignType) {
        switch (campaignType) {
            case TEXT:
                return TextCampaign.class;
            case DYNAMIC:
                return DynamicCampaign.class;
            case PERFORMANCE:
                return SmartCampaign.class;
            case MOBILE_CONTENT:
                return MobileContentCampaign.class;
            case MCBANNER:
                return McBannerCampaign.class;
            case CPM_BANNER:
                return CpmBannerCampaign.class;
            case CPM_PRICE:
                return CpmPriceCampaign.class;
            case CPM_YNDX_FRONTPAGE:
                return CpmYndxFrontpageCampaign.class;
            case CONTENT_PROMOTION:
                return ContentPromotionCampaign.class;
            default:
                throw new IllegalArgumentException("Неизвестный тип кампании: " + campaignType);
        }
    }

    public static Campaign activeCampaignByCampaignType(CampaignType campaignType) {
        return activeCampaignByCampaignType(campaignType, null, null);
    }

    public static Campaign activeCampaignByCampaignType(CampaignType campaignType, ClientId clientId, Long uid) {
        switch (campaignType) {
            case TEXT:
                return activeTextCampaign(clientId, uid);
            case DYNAMIC:
                return activeDynamicCampaign(clientId, uid);
            case PERFORMANCE:
                return activePerformanceCampaign(clientId, uid);
            case WALLET:
                return activeWalletCampaign(clientId, uid);
            case MOBILE_CONTENT:
                return activeMobileAppCampaign(clientId, uid);
            case CPM_BANNER:
                return activeCpmBannerCampaign(clientId, uid);
            case CPM_DEALS:
                return activeCpmDealsCampaign(clientId, uid);
            case CPM_YNDX_FRONTPAGE:
                return activeCpmYndxFrontpageCampaign(clientId, uid);
            case CPM_PRICE:
                return activeCpmPriceCampaign(clientId, uid);
            case CONTENT_PROMOTION:
                return activeContentPromotionCampaign(clientId, uid);
            case INTERNAL_FREE:
                return activeInternalFreeCampaign(clientId, uid);
            case INTERNAL_DISTRIB:
                return activeInternalDistribCampaign(clientId, uid);
            case INTERNAL_AUTOBUDGET:
                return activeInternalAutobudgetCampaign(clientId, uid);
            case MCBANNER:
                return activeMcBannerCampaign(clientId, uid);
            default:
                throw new IllegalArgumentException("Неизвестный тип кампании");
        }
    }

    public static Campaign activeWalletCampaign(ClientId clientId, Long uid) {
        return activeCampaignWithoutType(clientId, uid).withType(CampaignType.WALLET);
    }

    public static Campaign activeTextCampaign(ClientId clientId, Long uid) {
        return activeCampaignWithoutType(clientId, uid).withType(CampaignType.TEXT);
    }

    public static Campaign activeMobileAppCampaign(ClientId clientId, Long uid) {
        return activeCampaignWithoutType(clientId, uid).withType(CampaignType.MOBILE_CONTENT);
    }

    public static Campaign activeUacMobileAppCampaign(ClientId clientId, Long uid) {
        return activeMobileAppCampaign(clientId, uid).withSource(CampaignSource.UAC);
    }

    public static Campaign activeDynamicCampaign(ClientId clientId, Long uid) {
        return activeCampaignWithoutType(clientId, uid).withType(CampaignType.DYNAMIC);
    }

    public static Campaign activePerformanceCampaign(ClientId clientId, Long uid) {
        return activeCampaignWithoutType(clientId, uid).withType(CampaignType.PERFORMANCE);
    }

    public static Campaign activePerformanceCampaignWithStrategy(ClientId clientId, Long uid) {
        return activeCampaignWithoutType(clientId, uid)
                .withType(CampaignType.PERFORMANCE)
                .withStrategy(averageCpaPerCampStrategy());
    }

    public static Campaign activeMobileContentCampaign(ClientId clientId, Long uid) {
        return activeCampaignWithoutType(clientId, uid).withType(CampaignType.MOBILE_CONTENT);
    }

    public static Campaign activeCpmBannerCampaign(ClientId clientId, Long uid) {
        return activeCampaignWithoutType(clientId, uid).withType(CampaignType.CPM_BANNER);
    }

    public static Campaign activeCpmDealsCampaign(ClientId clientId, Long uid) {
        return activeCampaignWithoutType(clientId, uid).withType(CampaignType.CPM_DEALS);
    }

    public static Campaign activeCpmYndxFrontpageCampaign(ClientId clientId, Long uid) {
        return activeCampaignWithoutType(clientId, uid).withType(CampaignType.CPM_YNDX_FRONTPAGE);
    }

    public static Campaign activeCpmPriceCampaign(ClientId clientId, Long uid) {
        return activeCampaignWithoutType(clientId, uid).withType(CampaignType.CPM_PRICE);
    }

    public static Campaign activeContentPromotionCampaign(ClientId clientId, Long uid) {
        return activeCampaignWithoutType(clientId, uid).withType(CampaignType.CONTENT_PROMOTION);
    }

    public static Campaign activeInternalFreeCampaign(ClientId clientId, Long uid) {
        return activeCampaignWithoutType(clientId, uid).withType(CampaignType.INTERNAL_FREE);
    }

    public static Campaign activeInternalDistribCampaign(ClientId clientId, Long uid) {
        return activeCampaignWithoutType(clientId, uid).withType(CampaignType.INTERNAL_DISTRIB);
    }

    public static Campaign activeInternalAutobudgetCampaign(ClientId clientId, Long uid) {
        return activeCampaignWithoutType(clientId, uid).withType(CampaignType.INTERNAL_AUTOBUDGET);
    }

    public static Campaign activeMcBannerCampaign(ClientId clientId, Long uid) {
        return activeCampaignWithoutType(clientId, uid).withType(CampaignType.MCBANNER);
    }

    public static Campaign newTextCampaign(ClientId clientId, Long uid) {
        return manualCampaignWithoutType(clientId, uid).withType(CampaignType.TEXT);
    }

    public static Campaign newDynamicCampaign(ClientId clientId, Long uid) {
        return manualCampaignWithoutType(clientId, uid).withType(CampaignType.DYNAMIC);
    }

    public static Campaign newSmartCampaign(ClientId clientId, Long uid) {
        return manualCampaignWithoutType(clientId, uid).withType(CampaignType.PERFORMANCE);
    }

    public static Campaign emptyCampaignByCampaignType(CampaignType campaignType, ClientId clientId, Long uid) {
        switch (campaignType) {
            case TEXT:
                return emptyTextCampaign(clientId, uid);
            case DYNAMIC:
                return emptyDynamicCampaign(clientId, uid);
            case PERFORMANCE:
                return emptySmartCampaign(clientId, uid);
            case MOBILE_CONTENT:
                return emptyMobileContentCampaign(clientId, uid);
            case MCBANNER:
                return emptyMcBannerCampaign(clientId, uid);
            default:
                throw new IllegalArgumentException("Неизвестный тип кампании: " + campaignType);
        }
    }

    public static Campaign emptyTextCampaign(ClientId clientId, Long uid) {
        return manualCampaignWithoutType(clientId, uid).withType(CampaignType.TEXT).withStatusEmpty(true);
    }

    public static Campaign emptyDynamicCampaign(ClientId clientId, Long uid) {
        return manualCampaignWithoutType(clientId, uid).withType(CampaignType.DYNAMIC).withStatusEmpty(true);
    }

    public static Campaign emptySmartCampaign(ClientId clientId, Long uid) {
        return manualCampaignWithoutType(clientId, uid).withType(CampaignType.PERFORMANCE).withStatusEmpty(true);
    }

    public static Campaign emptyMobileContentCampaign(ClientId clientId, Long uid) {
        return manualCampaignWithoutType(clientId, uid).withType(CampaignType.MOBILE_CONTENT).withStatusEmpty(true);
    }

    public static Campaign emptyMcBannerCampaign(ClientId clientId, Long uid) {
        return manualCampaignWithoutType(clientId, uid).withType(CampaignType.MCBANNER).withStatusEmpty(true);
    }

    public static Campaign newMcbCampaign(ClientId clientId, Long uid) {
        return manualCampaignWithoutType(clientId, uid).withType(CampaignType.MCB);
    }

    public static Campaign newMcbannerCampaign(ClientId clientId, Long uid) {
        return manualCampaignWithoutType(clientId, uid).withType(CampaignType.MCBANNER);
    }

    public static Campaign newCpmBannerCampaign(ClientId clientId, Long uid) {
        return manualCampaignWithoutType(clientId, uid).withType(CampaignType.CPM_BANNER);
    }

    public static Campaign newGeoCampaign(ClientId clientId, Long uid) {
        return manualCampaignWithoutType(clientId, uid).withType(CampaignType.GEO);
    }

    public static Campaign newInternalFreeCampaign(ClientId clientId, Long uid) {
        return manualCampaignWithoutType(clientId, uid).withType(CampaignType.INTERNAL_FREE);
    }

    public static Campaign newInternalAutobudgetCampaign(ClientId clientId, Long uid) {
        return manualCampaignWithoutType(clientId, uid).withType(CampaignType.INTERNAL_AUTOBUDGET);
    }

    public static Campaign newContentPromotionCampaign(ClientId clientId, Long uid) {
        return manualCampaignWithoutType(clientId, uid).withType(CampaignType.CONTENT_PROMOTION);
    }

    public static Campaign newBillingAggregate(ClientId clientId, Long uid) {
        return manualCampaignWithoutType(clientId, uid).withType(CampaignType.BILLING_AGGREGATE)
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.ACCEPTED);
    }

    public static Campaign activeCampaignWithoutType(@Nullable ClientId clientId, Long uid) {
        return activeCampaignWithoutType(clientId, uid, RandomNumberUtils.nextPositiveLong());
    }

    public static Campaign activeCampaignWithoutType(@Nullable ClientId clientId, Long uid, Long orderId) {
        return manualCampaignWithoutType(clientId, uid)
                .withStatusEmpty(false)
                .withArchived(false)
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.YES)
                .withStatusShow(true)
                .withStatusActive(true)
                .withOrderId(orderId)
                .withStatusBsSynced(StatusBsSynced.YES)
                .withBalanceInfo(activeBalanceInfo(CurrencyCode.RUB))
                .withAutobudgetForecastDate(now().minusDays(1))
                .withStatusMetricaControl(false)
                .withStrategy(manualStrategy());
    }

    public static Campaign manualCampaignWithoutType(@Nullable ClientId clientId, Long uid) {
        return TestOldCampaigns.manualCampaignWithoutType(clientId, uid);
    }


    public static BalanceInfo emptyBalanceInfo(CurrencyCode currencyCode) {
        return TestOldCampaigns.emptyBalanceInfo(currencyCode);
    }

    public static BalanceInfo activeBalanceInfo(CurrencyCode currencyCode) {
        return emptyBalanceInfo(currencyCode)
                .withSum(BigDecimal.valueOf(100_000L).setScale(6, RoundingMode.DOWN))
                .withSumSpent(BigDecimal.valueOf(10_000L).setScale(6, RoundingMode.DOWN))
                .withSumLast(BigDecimal.valueOf(50_000L).setScale(6, RoundingMode.DOWN));
    }

    public static final BigDecimal MONEY_ON_ACTIVE_BALANCE = new BigDecimal(90_000);


    public static ManualStrategy manualStrategyWithoutDayBudget() {
        return new ManualStrategy()
                .withManualStrategyMode(ManualStrategyMode.HIGHEST_POSITION_ALL)
                .withPlatform(CampaignsPlatform.BOTH)
                .withSeparateBids(true);
    }

    public static ManualStrategy manualStrategy() {
        return manualStrategyWithoutDayBudget()
                .withDayBudget(new DayBudget()
                        .withDayBudget(BigDecimal.valueOf(300).setScale(2, RoundingMode.DOWN))
                        .withShowMode(DayBudgetShowMode.STRETCHED)
                        .withDailyChangeCount(0L)
                        .withStopNotificationSent(false));
    }

    public static AverageCpaStrategy averageCpaStrategy() {
        return new AverageCpaStrategy()
                .withAverageCpa(new BigDecimal("44"))
                .withGoalId(1234L)
                .withMaxWeekSum(new BigDecimal("7000"))
                .withMaxBid(new BigDecimal("50"));
    }

    public static AverageCpaPerCampStrategy averageCpaPerCampStrategy() {
        return new AverageCpaPerCampStrategy()
                .withAverageCpa(44.0)
                .withGoalId(1234L)
                .withMaxWeekSum(7000.0)
                .withMaxBid(50.0);
    }

    public static AutobudgetMaxReachStrategy autobudgetMaxReachStrategy() {
        return new AutobudgetMaxReachStrategy()
                .withAvgCpm(new BigDecimal("45.6"))
                .withSum(new BigDecimal("34567.45"));
    }

    public static DbStrategy defaultAutobudgetMaxReachDbStrategy() {
        StrategyData strategyData = new StrategyData()
                .withName(CampaignsStrategyName.autobudget_max_reach.getLiteral())
                .withAvgCpm(new BigDecimal("45.6"))
                .withSum(new BigDecimal("34567.45"));
        return (DbStrategy) new DbStrategy()
                .withStrategyData(strategyData)
                .withAutobudget(CampaignsAutobudget.YES)
                .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH);
    }

    public static DbStrategy defaultAutobudgetMaxImpressionsDbStrategy() {
        StrategyData strategyData = new StrategyData()
                .withName(CampaignsStrategyName.autobudget_max_impressions.getLiteral())
                .withAvgCpm(new BigDecimal("45.6"))
                .withSum(new BigDecimal("34567.45"));
        return (DbStrategy) new DbStrategy()
                .withStrategyData(strategyData)
                .withAutobudget(CampaignsAutobudget.YES)
                .withStrategyName(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS);
    }

    public static DbStrategy defaultAutobudgetMaxReachCustomPeriodDbStrategy(LocalDateTime lastUpdateTime) {
        StrategyData strategyData = new StrategyData()
                .withName(CampaignsStrategyName.autobudget_max_reach_custom_period.getLiteral())
                .withAutoProlongation(1L)
                .withDailyChangeCount(1L)
                .withLastUpdateTime(lastUpdateTime)
                .withStart(lastUpdateTime.toLocalDate())
                .withFinish(lastUpdateTime.toLocalDate().plusWeeks(1))
                .withAvgCpm(new BigDecimal("100"))
                .withBudget(new BigDecimal("34567.45"));
        return (DbStrategy) new DbStrategy()
                .withStrategyData(strategyData)
                .withAutobudget(CampaignsAutobudget.YES)
                .withStrategyName(StrategyName.AUTOBUDGET_MAX_REACH_CUSTOM_PERIOD);
    }

    public static DbStrategy defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy(LocalDateTime lastUpdateTime) {
        StrategyData strategyData = new StrategyData()
                .withName(CampaignsStrategyName.autobudget_max_impressions_custom_period.getLiteral())
                .withAutoProlongation(1L)
                .withDailyChangeCount(1L)
                .withLastUpdateTime(lastUpdateTime)
                .withStart(lastUpdateTime.toLocalDate())
                .withFinish(lastUpdateTime.toLocalDate().plusWeeks(1))
                .withAvgCpm(new BigDecimal("100"))
                .withBudget(new BigDecimal("34567.45"));
        return (DbStrategy) new DbStrategy()
                .withStrategyData(strategyData)
                .withAutobudget(CampaignsAutobudget.YES)
                .withStrategyName(StrategyName.AUTOBUDGET_MAX_IMPRESSIONS_CUSTOM_PERIOD);
    }

    public static AutobudgetMaxReachCustomPeriodStrategy autobudgetMaxReachCustomPeriodStrategy() {
        return new AutobudgetMaxReachCustomPeriodStrategy()
                .withBudget(new BigDecimal("234563.5"))
                .withStartDate(LocalDate.now())
                .withFinishDate(LocalDate.now().plusDays(14))
                .withAvgCpm(new BigDecimal("543.2"))
                .withAutoProlongation(1L);
    }

    public static AutobudgetMaxImpressionsStrategy autobudgetMaxImpressionsStrategy() {
        return (AutobudgetMaxImpressionsStrategy) new AutobudgetMaxImpressionsStrategy()
                .withSum(new BigDecimal("154563.5"))
                .withAvgCpm(new BigDecimal("345.3"));
    }

    public static AutobudgetMaxImpressionsCustomPeriodStrategy autobudgetMaxImpressionsCustomPeriodStrategy() {
        return (AutobudgetMaxImpressionsCustomPeriodStrategy) new AutobudgetMaxImpressionsCustomPeriodStrategy()
                .withBudget(new BigDecimal("546563.5"))
                .withStartDate(LocalDate.now())
                .withFinishDate(LocalDate.now().plusDays(7))
                .withAvgCpm(new BigDecimal("765.2"))
                .withAutoProlongation(0L);
    }

    public static AverageBidStrategy averageBidStrategy() {
        return new AverageBidStrategy()
                .withDayBudget(new DayBudget()
                        .withDayBudget(BigDecimal.valueOf(100).setScale(2, RoundingMode.DOWN))
                        .withShowMode(DayBudgetShowMode.STRETCHED)
                        .withDailyChangeCount(0L)
                        .withStopNotificationSent(false))
                .withAverageBid(new BigDecimal("55"))
                .withMaxWeekSum(new BigDecimal("9000"));
    }

    public static DbStrategy averageClickStrategy(@Nullable BigDecimal avgBid, @Nullable BigDecimal sum) {
        var strategy = new DbStrategy();
        strategy.setAutobudget(CampaignsAutobudget.YES);
        strategy.setPlatform(CampaignsPlatform.BOTH);
        strategy.setStrategy(CampOptionsStrategy.DIFFERENT_PLACES);
        strategy.setStrategyName(StrategyName.AUTOBUDGET_AVG_CLICK);
        strategy.setStrategyData(new StrategyData()
                .withSum(sum)
                .withAvgBid(avgBid));
        return strategy;
    }

    public static DbStrategy defaultAverageClickStrategy() {
        var strategy = new DbStrategy();
        strategy.setAutobudget(CampaignsAutobudget.YES);
        strategy.setPlatform(CampaignsPlatform.BOTH);
        strategy.setStrategy(CampOptionsStrategy.DIFFERENT_PLACES);
        strategy.setStrategyName(StrategyName.AUTOBUDGET_AVG_CLICK);
        strategy.setStrategyData(new StrategyData()
                .withSum(new BigDecimal("1000"))
                .withAvgBid(new BigDecimal("123")));
        return strategy;
    }

    public static DbStrategy searchAverageClickStrategy() {
        var strategy = new DbStrategy();
        strategy.setAutobudget(CampaignsAutobudget.YES);
        strategy.setPlatform(CampaignsPlatform.SEARCH);
        strategy.setStrategy(CampOptionsStrategy.DIFFERENT_PLACES);
        strategy.setStrategyName(StrategyName.AUTOBUDGET_AVG_CLICK);
        strategy.setStrategyData(new StrategyData()
                .withSum(new BigDecimal("1000"))
                .withAvgBid(new BigDecimal("123")));
        return strategy;
    }

    public static DbStrategy contextAverageClickStrategy() {
        var strategy = new DbStrategy();
        strategy.setAutobudget(CampaignsAutobudget.YES);
        strategy.setPlatform(CampaignsPlatform.CONTEXT);
        strategy.setStrategy(CampOptionsStrategy.DIFFERENT_PLACES);
        strategy.setStrategyName(StrategyName.AUTOBUDGET_AVG_CLICK);
        strategy.setStrategyData(new StrategyData()
                .withSum(new BigDecimal("1000"))
                .withAvgBid(new BigDecimal("123")));
        return strategy;
    }

    public static DbStrategy defaultStrategy() {
        return TestCampaignsStrategy.INSTANCE.defaultStrategy();
    }

    public static DbStrategy defaultCpmStrategy() {
        var strategy = new DbStrategy();
        strategy.setStrategyName(StrategyName.CPM_DEFAULT);
        strategy.setAutobudget(CampaignsAutobudget.NO);
        strategy.setPlatform(CampaignsPlatform.CONTEXT);
        strategy.setStrategyData(new StrategyData()
                .withName(CampaignsStrategyName.cpm_default.getLiteral())
                .withVersion(1L));
        return strategy;
    }

    public static DbStrategy manualCpmBothDifferentStrategy() {
        var strategy = new DbStrategy();
        strategy.setStrategyName(StrategyName.CPM_DEFAULT);
        strategy.setAutobudget(CampaignsAutobudget.NO);
        strategy.setPlatform(CampaignsPlatform.BOTH);
        strategy.setStrategy(CampOptionsStrategy.DIFFERENT_PLACES);
        strategy.setStrategyData(new StrategyData()
                .withName(CampaignsStrategyName.cpm_default.getLiteral())
                .withVersion(1L));
        return strategy;
    }

    public static DbStrategy autoBudgetWeekBundle(Long limitClicks, @Nullable BigDecimal bid,
                                                  @Nullable BigDecimal avgBid) {
        return (DbStrategy) new DbStrategy()
                .withStrategyName(StrategyName.AUTOBUDGET_WEEK_BUNDLE)
                .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
                .withPlatform(CampaignsPlatform.BOTH)
                .withStrategyData(new StrategyData()
                        .withLimitClicks(limitClicks)
                        .withBid(bid)
                        .withAvgBid(avgBid));
    }

    public static DbStrategy defaultAverageCpcPerFilterStrategy(BigDecimal filterAvgBid,
                                                                BigDecimal bid,
                                                                BigDecimal sum) {
        return (DbStrategy) new DbStrategy()
                .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPC_PER_FILTER)
                .withStrategy(CampOptionsStrategy.AUTOBUDGET_AVG_CPC_PER_FILTER)
                .withPlatform(CampaignsPlatform.BOTH)
                .withAutobudget(CampaignsAutobudget.YES)
                .withStrategyData(new StrategyData()
                        .withName(CampaignsStrategyName.autobudget_avg_cpc_per_filter.getLiteral())
                        .withFilterAvgBid(filterAvgBid)
                        .withSum(sum)
                        .withBid(bid)
                );
    }

    public static DbStrategy defaultAverageCpcPerCamprStrategy(BigDecimal avgBid,
                                                               BigDecimal bid,
                                                               BigDecimal sum) {
        return (DbStrategy) new DbStrategy()
                .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP)
                .withStrategy(CampOptionsStrategy.AUTOBUDGET_AVG_CPC_PER_CAMP)
                .withPlatform(CampaignsPlatform.BOTH)
                .withAutobudget(CampaignsAutobudget.YES)
                .withStrategyData(new StrategyData()
                        .withName(CampaignsStrategyName.autobudget_avg_cpc_per_camp.getLiteral())
                        .withAvgBid(avgBid)
                        .withSum(sum)
                        .withBid(bid)
                        .withVersion(1L)
                );
    }

    public static DbStrategy defaultAverageCpaPerFilterrStrategy(Long goalId,
                                                                 BigDecimal filterAvgCpa,
                                                                 BigDecimal bid,
                                                                 BigDecimal sum) {
        return (DbStrategy) new DbStrategy()
                .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPA_PER_FILTER)
                .withStrategy(CampOptionsStrategy.AUTOBUDGET_AVG_CPA_PER_FILTER)
                .withPlatform(CampaignsPlatform.BOTH)
                .withAutobudget(CampaignsAutobudget.YES)
                .withStrategyData(new StrategyData()
                        .withName(CampaignsStrategyName.autobudget_avg_cpa_per_filter.getLiteral())
                        .withGoalId(goalId)
                        .withFilterAvgCpa(filterAvgCpa)
                        .withSum(sum)
                        .withBid(bid)
                );
    }

    public static DbStrategy defaultAverageCpaPerCamprStrategy(Long goalId,
                                                               BigDecimal avgCpa,
                                                               BigDecimal bid,
                                                               BigDecimal sum) {
        return (DbStrategy) new DbStrategy()
                .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP)
                .withStrategy(CampOptionsStrategy.AUTOBUDGET_AVG_CPA_PER_CAMP)
                .withPlatform(CampaignsPlatform.BOTH)
                .withAutobudget(CampaignsAutobudget.YES)
                .withStrategyData(new StrategyData()
                        .withName(CampaignsStrategyName.autobudget_avg_cpa_per_camp.getLiteral())
                        .withGoalId(goalId)
                        .withAvgCpa(avgCpa)
                        .withSum(sum)
                        .withBid(bid)
                );
    }

    public static DbStrategy defaultAverageCpaStrategy(Long goalId) {
        return (DbStrategy) new DbStrategy()
                .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPA)
                .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
                .withPlatform(CampaignsPlatform.BOTH)
                .withStrategyData(new StrategyData()
                        .withName(CampaignsStrategyName.autobudget_avg_cpa.getLiteral())
                        .withAvgCpa(new BigDecimal("2000"))
                        .withGoalId(goalId)
                        .withSum(null)
                        .withBid(null)
                );
    }

    public static DbStrategy defaultStrategyForSimpleView(Long goalId) {
        return (DbStrategy) new DbStrategy()
                .withAutobudget(CampaignsAutobudget.YES)
                .withStrategyName(StrategyName.AUTOBUDGET)
                .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
                .withPlatform(CampaignsPlatform.BOTH)
                .withStrategyData(new StrategyData()
                        .withName(CampaignsStrategyName.autobudget.getLiteral())
                        .withGoalId(goalId)
                        .withSum(new BigDecimal("2000"))
                );
    }

    public static DbStrategy defaultAutobudgetAverageCpaStrategy(Long goalId) {
        return (DbStrategy) new DbStrategy()
                .withAutobudget(CampaignsAutobudget.YES)
                .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPA)
                .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
                .withPlatform(CampaignsPlatform.BOTH)
                .withStrategyData(new StrategyData()
                        .withName(CampaignsStrategyName.autobudget_avg_cpa.getLiteral())
                        .withAvgCpa(new BigDecimal("2000"))
                        .withGoalId(goalId)
                        .withSum(null)
                        .withBid(null)
                );
    }

    public static DbStrategy averageCpiStrategy(Long goalId) {
        return averageCpiStrategy(goalId, new BigDecimal("2000"), null, null);
    }

    public static DbStrategy averageCpiStrategy(Long goalId, BigDecimal avgCpi, BigDecimal bid, BigDecimal sum) {
        return averageCpiStrategy(goalId, avgCpi, bid, sum, null);
    }

    public static DbStrategy averageCpiStrategy(Long goalId,
                                                BigDecimal avgCpi,
                                                BigDecimal bid,
                                                BigDecimal sum,
                                                Boolean payForConversion) {
        return (DbStrategy) new DbStrategy()
                .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPI)
                .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
                .withPlatform(CampaignsPlatform.BOTH)
                .withStrategyData(new StrategyData()
                        .withAvgCpi(avgCpi)
                        .withGoalId(goalId)
                        .withSum(sum)
                        .withBid(bid)
                        .withPayForConversion(payForConversion));
    }

    public static DbStrategy averageCpaStrategy(BigDecimal avgCpa, Long goalId, @Nullable BigDecimal sum,
                                                @Nullable BigDecimal bid) {
        return (DbStrategy) new DbStrategy()
                .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPA)
                .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
                .withPlatform(CampaignsPlatform.BOTH)
                .withStrategyData(new StrategyData()
                        .withAvgCpa(avgCpa)
                        .withGoalId(goalId)
                        .withSum(sum)
                        .withBid(bid)
                );
    }

    public static DbStrategy averageCpaPayForConversionStrategy(BigDecimal avgCpa, Long goalId,
                                                                @Nullable BigDecimal sum,
                                                                @Nullable BigDecimal bid) {
        return (DbStrategy) new DbStrategy()
                .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPA)
                .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
                .withAutobudget(CampaignsAutobudget.YES)
                .withPlatform(CampaignsPlatform.BOTH)
                .withStrategyData(new StrategyData()
                        .withName(StrategyName.toSource(StrategyName.AUTOBUDGET_AVG_CPA).getLiteral())
                        .withPayForConversion(true)
                        .withAvgCpa(avgCpa)
                        .withGoalId(goalId)
                        .withSum(sum)
                        .withBid(bid)
                );
    }

    public static DbStrategy dafaultAverageCpaPayForConversionStrategy() {
        return averageCpaPayForConversionStrategy(new BigDecimal("2000"), 12345L, null, null);
    }

    public static DbStrategy defaultAutobudgetRoiStrategy(long goalId) {
        return TestCampaignsStrategy.INSTANCE.defaultAutobudgetRoiStrategy(goalId);
    }

    public static DbStrategy defaultAutobudgetCrrStrategy(long goalId) {
        return autobudgetCrrStrategy(new BigDecimal("10000"), 1L, goalId);
    }

    public static DbStrategy defaultAutobudgetRoiStrategy(long goalId, boolean differentPlaces) {
        return TestCampaignsStrategy.INSTANCE.defaultAutobudgetRoiStrategy(goalId, differentPlaces);
    }

    public static DbStrategy autobudgetRoiStrategy(@Nullable BigDecimal sum, @Nullable BigDecimal bid,
                                                   BigDecimal roiCoef,
                                                   Long reserveReturn, @Nullable BigDecimal profitability,
                                                   long goalId) {
        return (DbStrategy) new DbStrategy()
                .withStrategyName(StrategyName.AUTOBUDGET_ROI)
                .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
                .withPlatform(CampaignsPlatform.BOTH)
                .withStrategyData(new StrategyData()
                        .withSum(sum)
                        .withBid(bid)
                        .withRoiCoef(roiCoef)
                        .withReserveReturn(reserveReturn)
                        .withProfitability(profitability)
                        .withGoalId(goalId)
                );
    }

    public static DbStrategy autobudgetCrrStrategy(@Nullable BigDecimal sum,
                                                   @Nullable Long crr,
                                                   long goalId) {
        return TestCampaignsStrategy.INSTANCE.autobudgetCrrStrategy(sum, crr, goalId);
    }

    public static DbStrategy defaultAutobudgetStrategy(Long goalId) {
        return TestCampaignsStrategy.INSTANCE.defaultAutobudgetStrategy(goalId);
    }

    public static DbStrategy defaultAutobudgetStrategy() {
        return TestCampaignsStrategy.INSTANCE.defaultAutobudgetStrategy();
    }

    public static DbStrategy autobudgetStrategy(BigDecimal sum, @Nullable BigDecimal bid, @Nullable Long goalId) {
        return (DbStrategy) new DbStrategy()
                .withStrategyName(StrategyName.AUTOBUDGET)
                .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
                .withPlatform(CampaignsPlatform.BOTH)
                .withStrategyData(new StrategyData()
                        .withSum(sum)
                        .withBid(bid)
                        .withGoalId(goalId)
                );
    }

    /**
     * Ручное управление ставками на поиске
     */
    public static DbStrategy manualSearchStrategy() {
        var strategy = new DbStrategy();
        strategy.setStrategyName(StrategyName.DEFAULT_);
        strategy.setAutobudget(CampaignsAutobudget.NO);
        strategy.setPlatform(CampaignsPlatform.SEARCH);
        strategy.setStrategy(null);
        strategy.setStrategyData(new StrategyData().withVersion(1L).withName("default"));
        return strategy;
    }

    public static DbStrategy manualBothDifferentStrategy() {
        var strategy = new DbStrategy();
        strategy.setStrategyName(StrategyName.DEFAULT_);
        strategy.setAutobudget(CampaignsAutobudget.NO);
        strategy.setPlatform(CampaignsPlatform.BOTH);
        strategy.setStrategy(CampOptionsStrategy.DIFFERENT_PLACES);
        strategy.setStrategyData(new StrategyData().withVersion(1L).withName("default"));
        return strategy;
    }

    public static DbStrategy manualBothStrategy() {
        var strategy = new DbStrategy();
        strategy.setStrategyName(StrategyName.DEFAULT_);
        strategy.setAutobudget(CampaignsAutobudget.NO);
        strategy.setPlatform(CampaignsPlatform.BOTH);
        strategy.setStrategy(null);
        strategy.setStrategyData(new StrategyData().withVersion(1L).withName("default"));
        return strategy;
    }

    public static DbStrategy manualBothStrategy(BigDecimal sum) {
        var strategy = new DbStrategy();
        strategy.setStrategyName(StrategyName.DEFAULT_);
        strategy.setAutobudget(CampaignsAutobudget.NO);
        strategy.setPlatform(CampaignsPlatform.BOTH);
        strategy.setStrategy(null);
        strategy.setStrategyData(new StrategyData()
                .withSum(sum)
                .withVersion(1L)
                .withName("default"));
        return strategy;
    }

    public static DbStrategy manualContextStrategy() {
        var strategy = new DbStrategy();
        strategy.setStrategyName(StrategyName.DEFAULT_);
        strategy.setAutobudget(CampaignsAutobudget.NO);
        strategy.setPlatform(CampaignsPlatform.CONTEXT);
        strategy.setStrategy(null);
        strategy.setStrategyData(new StrategyData().withVersion(1L).withName("default"));
        return strategy;
    }

    public static DbStrategy simpleStrategy() {
        var strategy = new DbStrategy();
        strategy.setStrategyName(StrategyName.AUTOBUDGET);
        strategy.setAutobudget(CampaignsAutobudget.YES);
        strategy.setPlatform(CampaignsPlatform.CONTEXT);
        strategy.setStrategy(CampOptionsStrategy.DIFFERENT_PLACES);
        strategy.setStrategyData(new StrategyData()
                .withName(CampaignsStrategyName.autobudget.getLiteral())
                .withSum(BigDecimal.valueOf(345))
                .withVersion(1L));
        return strategy;
    }

    public static DbStrategy defaultAvgCpvStrategy(LocalDateTime lastUpdateTime) {
        StrategyData strategyData = new StrategyData()
                .withName(CampaignsStrategyName.autobudget_avg_cpv.getLiteral())
                .withAutoProlongation(1L)
                .withDailyChangeCount(1L)
                .withLastUpdateTime(lastUpdateTime)
                .withStart(lastUpdateTime.toLocalDate())
                .withFinish(lastUpdateTime.toLocalDate().plusWeeks(1))
                .withAvgCpv(new BigDecimal("0.07"))
                .withBudget(new BigDecimal("155"));
        return (DbStrategy) new DbStrategy()
                .withStrategyData(strategyData)
                .withAutobudget(CampaignsAutobudget.YES)
                .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPV);
    }

    public static DbStrategy defaultAvgCpvCustomPeriodStrategy(LocalDateTime lastUpdateTime) {
        StrategyData strategyData = new StrategyData()
                .withName(CampaignsStrategyName.autobudget_avg_cpv_custom_period.getLiteral())
                .withAutoProlongation(1L)
                .withDailyChangeCount(1L)
                .withLastUpdateTime(lastUpdateTime)
                .withStart(lastUpdateTime.toLocalDate())
                .withFinish(lastUpdateTime.toLocalDate().plusWeeks(1))
                .withAvgCpv(new BigDecimal("0.07"))
                .withBudget(new BigDecimal("155"));
        return (DbStrategy) new DbStrategy()
                .withStrategyData(strategyData)
                .withAutobudget(CampaignsAutobudget.YES)
                .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPV_CUSTOM_PERIOD);
    }


    public static ContextSettings defaultSearchBasedContextSettings() {
        return TestOldCampaigns.defaultSearchBasedContextSettings();
    }

    public static CampaignMulticurrencySums defaultMulticurrencySums(long campaignId) {
        return new CampaignMulticurrencySums()
                .withId(campaignId)
                .withSum(BigDecimal.ZERO)
                .withChipsCost(BigDecimal.ZERO)
                .withChipsSpent(BigDecimal.ZERO)
                .withAvgDiscount(BigDecimal.ZERO)
                .withBalanceTid(0L);
    }

    public static CommonCampaign defaultCampaignWithSystemFieldsByCampaignType(CampaignType campaignType) {
        switch (campaignType) {
            case TEXT:
                return defaultTextCampaignWithSystemFields();
            case DYNAMIC:
                return defaultDynamicCampaignWithSystemFields();
            case PERFORMANCE:
                return defaultSmartCampaignWithSystemFields();
            case MOBILE_CONTENT:
                return defaultMobileContentCampaignWithSystemFields();
            case MCBANNER:
                return defaultMcBannerCampaignWithSystemFields();
            case CPM_BANNER:
                return defaultCpmBannerCampaignWithSystemFields();
            default:
                throw new IllegalArgumentException("Неизвестный тип кампании: " + campaignType);
        }
    }

    public static TextCampaign defaultTextCampaignWithSystemFields(ClientInfo clientInfo) {
        return defaultTextCampaignWithSystemFields()
                .withUid(clientInfo.getUid())
                .withClientId(clientInfo.getClientId().asLong());
    }

    public static TextCampaign defaultTextCampaignWithSystemFields() {
        return TestTextCampaigns.fullTextCampaign();
    }

    public static CommonCampaign defaultCampaignByCampaignType(CampaignType campaignType) {
        switch (campaignType) {
            case TEXT:
                return defaultTextCampaign();
            case DYNAMIC:
                return defaultDynamicCampaign();
            case PERFORMANCE:
                return defaultSmartCampaign();
            case MOBILE_CONTENT:
                return defaultMobileContentCampaign();
            case CPM_BANNER:
                return defaultCpmBannerCampaign();
            case INTERNAL_AUTOBUDGET:
                return defaultInternalAutobudgetCampaign();
            case INTERNAL_DISTRIB:
                return defaultInternalDistribCampaign();
            case INTERNAL_FREE:
                return defaultInternalFreeCampaign();
            case MCBANNER:
                return defaultMcBannerCampaign();
            case CONTENT_PROMOTION:
                return defaultContentPromotionCampaign();
            default:
                throw new IllegalArgumentException("Неизвестный тип кампании: " + campaignType);
        }
    }

    public static TextCampaign defaultTextCampaign() {
        return TestTextCampaigns.clientTextCampaign();
    }

    public static ContentPromotionCampaign defaultContentPromotionCampaign() {
        return TestContentPromotionCampaigns.clientContentPromotionCampaign();
    }

    public static ContentPromotionCampaign defaultContentPromotionCampaignWithSystemFields() {
        return TestContentPromotionCampaigns.fullContentPromotionCampaign();
    }

    public static CpmBannerCampaign defaultCpmBannerCampaignWithSystemFields() {
        return TestCpmBannerCampaigns.fullCpmBannerCampaign();
    }

    public static CpmBannerCampaign defaultCpmBannerCampaign() {
        return TestCpmBannerCampaigns.clientCpmBannerCampaign();
    }

    public static DynamicCampaign defaultDynamicCampaignWithSystemFields() {
        return TestDynamicCampaigns.fullDraftDynamicCampaign();
    }

    public static DynamicCampaign defaultDynamicCampaign() {
        return TestDynamicCampaigns.clientDynamicCampaign();
    }

    public static SmartCampaign defaultSmartCampaignWithSystemFields(ClientInfo clientInfo) {
        return defaultSmartCampaignWithSystemFields()
                .withUid(clientInfo.getUid())
                .withClientId(clientInfo.getClientId().asLong())
                .withFio(clientInfo.getChiefUserInfo().getUser().getFio());
    }

    public static SmartCampaign defaultSmartCampaignWithSystemFields() {
        return TestSmartCampaigns.fullSmartCampaign();
    }

    public static SmartCampaign defaultSmartCampaign() {
        return TestSmartCampaigns.clientSmartCampaign();
    }

    public static TextCampaign defaultTouchCampaign() {
        return defaultTextCampaignWithSystemFields().withIsTouch(true);
    }

    public static CpmPriceCampaign defaultCpmPriceCampaign(ClientInfo clientInfo, PricePackage pricePackage) {
        return TestCpmPriceCampaigns.clientCpmPriceCampaign(pricePackage)
                .withCurrency(clientInfo.getClient().getWorkCurrency())
                .withUid(clientInfo.getUid())
                .withClientId(clientInfo.getClientId().asLong());
    }

    public static CpmPriceCampaign defaultCpmPriceCampaignWithSystemFields(ClientInfo clientInfo,
                                                                           PricePackage pricePackage) {
        return TestCpmPriceCampaigns.fullCpmPriceCampaign(pricePackage)
                .withCurrency(clientInfo.getClient().getWorkCurrency())
                .withUid(clientInfo.getUid())
                .withClientId(clientInfo.getClientId().asLong())
                .withAutoProlongation(false);
    }

    public static CpmYndxFrontpageCampaign defaultCpmYndxFrontpageCampaign(ClientInfo clientInfo) {
        return TestCpmYndxFrontPageCampaigns.fullCpmYndxFrontpageCampaign()
                .withCurrency(clientInfo.getClient().getWorkCurrency())
                .withUid(clientInfo.getUid())
                .withClientId(clientInfo.getClientId().asLong())
                .withAutobudgetForecastDate(null);
    }

    public static InternalAutobudgetCampaign defaultInternalAutobudgetCampaign() {
        return TestInternalAutobudgetCampaigns.clientInternalAutobudgetCampaign();
    }

    public static InternalAutobudgetCampaign defaultInternalAutobudgetCampaignWithSystemFields(ClientInfo clientInfo) {
        InternalAutobudgetCampaign internalAutobudgetCampaign =
                TestInternalAutobudgetCampaigns.fullInternalAutobudgetCampaign();
        setInternalCampaignClientFields(internalAutobudgetCampaign, clientInfo);
        return internalAutobudgetCampaign;
    }

    public static InternalDistribCampaign defaultInternalDistribCampaign() {
        return TestInternalDistribCampaigns.clientInternalDistribCampaign();
    }

    public static InternalDistribCampaign defaultInternalDistribCampaignWithSystemFields(ClientInfo clientInfo) {
        InternalDistribCampaign internalDistribCampaign = TestInternalDistribCampaigns.fullInternalDistribCampaign();
        setInternalCampaignClientFields(internalDistribCampaign, clientInfo);
        return internalDistribCampaign;
    }

    public static InternalFreeCampaign defaultInternalFreeCampaign() {
        return TestInternalFreeCampaigns.clientInternalFreeCampaign();
    }

    public static InternalFreeCampaign defaultInternalFreeCampaignWithSystemFields(ClientInfo clientInfo) {
        InternalFreeCampaign internalFreeCampaign = TestInternalFreeCampaigns.fullInternalFreeCampaign();
        setInternalCampaignClientFields(internalFreeCampaign, clientInfo);
        return internalFreeCampaign;
    }

    private static void setInternalCampaignClientFields(CommonCampaign internalCampaign, ClientInfo clientInfo) {
        internalCampaign.withClientId(clientInfo.getClientId().asLong())
                .withUid(clientInfo.getUid())
                .withCurrency(clientInfo.getClient().getWorkCurrency())
                .withFio(nvl(clientInfo.getChiefUserInfo().getUser().getFio(), internalCampaign.getFio()));
    }

    public static MobileContentCampaign defaultMobileContentCampaign() {
        return TestMobileContentCampaigns.clientMobileContentCampaign(5L);
    }

    public static MobileContentCampaign defaultMobileContentCampaignWithSystemFields() {
        return TestMobileContentCampaigns.fullMobileContentCampaign(5L);
    }

    public static MobileContentCampaign defaultMobileContentCampaignWithSystemFields(ClientInfo clientInfo) {
        return defaultMobileContentCampaignWithSystemFields()
                .withClientId(clientInfo.getClientId().asLong())
                .withUid(clientInfo.getUid())
                .withCurrency(clientInfo.getClient().getWorkCurrency());
    }

    public static McBannerCampaign defaultMcBannerCampaign() {
        return TestMcBannerCampaigns.clientMcBannerCampaign();
    }

    public static McBannerCampaign defaultMcBannerCampaignWithSystemFields() {
        return TestMcBannerCampaigns.fullMcBannerCampaign();
    }

    public static McBannerCampaign defaultMcBannerCampaignWithSystemFields(ClientInfo clientInfo) {
        return defaultMcBannerCampaignWithSystemFields()
                .withClientId(clientInfo.getClientId().asLong())
                .withUid(clientInfo.getUid())
                .withCurrency(clientInfo.getClient().getWorkCurrency());
    }
}
