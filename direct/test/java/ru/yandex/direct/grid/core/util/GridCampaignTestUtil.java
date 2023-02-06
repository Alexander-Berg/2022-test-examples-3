package ru.yandex.direct.grid.core.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.RandomUtils;

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.grid.model.campaign.GdCampaignPlacementType;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaignAction;
import ru.yandex.direct.grid.model.campaign.GdiCampaignActionsHolder;
import ru.yandex.direct.grid.model.campaign.GdiCampaignSource;
import ru.yandex.direct.grid.model.campaign.GdiCampaignStatusBsSynced;
import ru.yandex.direct.grid.model.campaign.GdiCampaignStrategyName;
import ru.yandex.direct.grid.model.campaign.GdiDayBudgetShowMode;
import ru.yandex.direct.grid.model.campaign.GdiWalletAction;
import ru.yandex.direct.grid.model.campaign.GdiWalletActionsHolder;
import ru.yandex.direct.libs.timetarget.TimeTarget;
import ru.yandex.direct.utils.JsonUtils;

import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_METRIKA_TAG_TO_URL;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_OPENSTAT_TAG_TO_URL;

public class GridCampaignTestUtil {
    // Делаем 100 единиц денег на кампании по умолчанию
    public static final BigDecimal DEFAULT_CAMPAIGN_MONEY = BigDecimal.valueOf(1180);

    // Базовая дата для создания кампаний
    public static final LocalDate TEST_DATE = LocalDate.now();
    public static final LocalDateTime TEST_DATETIME = LocalDateTime.now();
    public static final int METRIKA_COUNTER = 1234543;
    public static final List<String> DISABLED_SSP = List.of("MobFox", "Smaato");
    public static final List<String> DISABLED_DOMAINS = List.of("rambler.ru", "vk.com");
    public static final List<String> DISABLED_IPS = List.of("77.1.1.1", "77.1.1.3");
    public static final Set<GdCampaignPlacementType> PLACEMENT_TYPES = EnumSet.of(GdCampaignPlacementType.ADV_GALLERY);
    public static final long BROAD_MATCH_GOAL_ID = 123L;

    private GridCampaignTestUtil() {
    }

    public static GdiCampaign defaultCampaign(CampaignAttributionModel defaultAttributionModel) {
        return defaultCampaign().withAttributionModel(defaultAttributionModel);
    }

    public static GdiCampaign defaultCampaign() {
        return defaultCampaign(RandomUtils.nextLong(1, Long.MAX_VALUE),
                CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE);
    }

    public static GdiCampaign defaultCampaignUnderWallet(long id, long walletId) {
        return defaultCampaign(id).withWalletId(walletId);
    }

    public static GdiCampaign defaultCampaign(long id, CampaignAttributionModel defaultAttributionModel) {
        return defaultCampaign(id).withAttributionModel(defaultAttributionModel);
    }

    public static GdiCampaign defaultCampaign(long id) {
        return new GdiCampaign()
                .withEmpty(false)
                .withId(id)
                .withOrderId(1234567L)
                .withWalletId(0L)
                .withUserId(1L)
                .withClientId(1L)
                .withManagerUserId(null)
                .withAgencyUserId(null)
                .withAgencyId(0L)
                .withName("Кампания")
                .withDescription("Описание")
                .withArchived(false)
                .withType(CampaignType.TEXT)
                .withGeo("1")
                .withTimeTarget(TimeTarget.parseRawString(""))
                .withTimezoneId(1L)
                .withStartDate(TEST_DATE.minusDays(10))
                .withFinishDate(null)
                .withCreateTime(TEST_DATETIME.minusDays(10))
                .withSum(DEFAULT_CAMPAIGN_MONEY)
                .withSumSpent(BigDecimal.ZERO)
                .withSumRest(DEFAULT_CAMPAIGN_MONEY)
                .withSumLast(DEFAULT_CAMPAIGN_MONEY)
                .withSumToPay(BigDecimal.ZERO)
                .withCurrencyCode(CurrencyCode.RUB)
                .withCurrencyConverted(false)
                .withHasBanners(true)
                .withHasActiveBanners(true)
                .withIsUniversal(false)
                .withHasNotArchiveBanners(true)
                .withHasSiteMonitoring(true)
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
                .withHasBidModifiers(false)
                .withHasBroadMatch(true)
                .withBroadMatchLimit(CampaignConstants.BROAD_MATCH_LIMIT_DEFAULT)
                .withBroadMatchGoalId(BROAD_MATCH_GOAL_ID)
                .withShows(0L)
                .withClicks(0L)
                .withShowing(true)
                .withActive(true)
                .withNoPay(false)
                .withUseCurrentRegion(false)
                .withUseRegularRegion(false)
                .withHasExtendedGeoTargeting(false)
                .withStatusBsSynced(GdiCampaignStatusBsSynced.YES)
                .withStatusModerate(CampaignStatusModerate.YES)
                .withStatusPostModerate(CampaignStatusPostmoderate.ACCEPTED)
                .withDayBudget(BigDecimal.ZERO)
                .withDayBudgetShowMode(GdiDayBudgetShowMode.DEFAULT_)
                .withDayBudgetStopTime(null)
                .withPlatform(CampaignsPlatform.BOTH)
                .withStrategyName(GdiCampaignStrategyName.DEFAULT_)
                .withStrategyData(null)
                .withDelayedOperation(null)
                .withMetrikaCounters(List.of(METRIKA_COUNTER))
                .withStopTime(null)
                .withMoneyBlocked(false)
                .withFavorite(false)
                .withEnableCheckPositionEvent(true)
                .withCheckPositionInterval(CampaignConstants.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                .withSmsTime(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL)
                .withSmsFlags(Collections.emptySet())
                .withDisabledDomains(String.join(",", DISABLED_DOMAINS))
                .withDisabledSsp(JsonUtils.toJson(DISABLED_SSP))
                .withDisabledIps(String.join(",", DISABLED_IPS))
                .withAttributionModel(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE)
                .withHasMediaplanBanners(false)
                .withHasNewMediaplan(false)
                .withMediaplanStatus(null)
                .withActions(defaultCampaignActionsHolder())
                .withSource(GdiCampaignSource.DIRECT)
                .withIsRecommendationsManagementEnabled(false)
                .withIsPriceRecommendationsManagementEnabled(false)
                .withIsBrandLiftHidden(false);
    }

    public static GdiCampaign defaultWallet() {
        return defaultCampaign(RandomUtils.nextLong(1, Long.MAX_VALUE));
    }

    public static GdiCampaign defaultWallet(long id) {
        return defaultCampaign(id)
                .withType(CampaignType.WALLET)
                .withMetrikaCounters(null)
                .withActions(null)
                .withWalletActions(defaultWalletActionsHolder())
                .withWalletCanPayBeforeModeration(false);
    }

    public static GdiCampaign walletWithCanPayBeforeModeration(long id) {
        return defaultWallet(id).withWalletCanPayBeforeModeration(true);
    }

    public static GdiCampaignActionsHolder campaignActionsHolder(GdiCampaignAction... availableActions) {
        return campaignActionsHolder(ImmutableSet.copyOf(availableActions));
    }

    public static GdiCampaignActionsHolder campaignActionsHolder(Set<GdiCampaignAction> availableActions) {
        return new GdiCampaignActionsHolder()
                .withActions(availableActions)
                .withCanEdit(true)
                .withHasManager(false)
                .withHasAgency(false);
    }

    public static GdiCampaignActionsHolder defaultCampaignActionsHolder() {
        return campaignActionsHolder(defaultCampaignActions());
    }

    public static Set<GdiCampaignAction> defaultCampaignActions() {
        return ImmutableSet.of(GdiCampaignAction.EDIT_CAMP, GdiCampaignAction.SHOW_CAMP_STAT,
                GdiCampaignAction.ARCHIVE_CAMP, GdiCampaignAction.UNARCHIVE_CAMP);
    }

    public static GdiWalletActionsHolder walletActionsHolder(GdiWalletAction... availableWalletActioins) {
        return walletActionsHolder(ImmutableSet.copyOf(availableWalletActioins));
    }

    public static GdiWalletActionsHolder walletActionsHolder(Set<GdiWalletAction> availableWalletActioins) {
        return new GdiWalletActionsHolder()
                .withSubclientCanEdit(true)
                .withSubclientCanEdit(false)
                .withActions(availableWalletActioins);
    }

    public static GdiWalletActionsHolder defaultWalletActionsHolder() {
        return walletActionsHolder(defaultWalletActions());
    }

    public static Set<GdiWalletAction> defaultWalletActions() {
        return ImmutableSet.of(GdiWalletAction.EDIT, GdiWalletAction.PAY);
    }
}
