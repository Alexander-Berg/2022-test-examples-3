package ru.yandex.direct.core.entity.campaign.utils;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.direct.core.entity.campaign.model.BroadMatch;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.ContentLanguage;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.PlacementType;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmsFlag;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.model.ModelChanges;

import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_METRIKA_TAG_TO_URL;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_OPENSTAT_TAG_TO_URL;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CONTEXT_LIMIT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ENABLE_COMPANY_INFO;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ENABLE_CPC_HOLD;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_EXCLUDE_PAUSED_COMPETING_ADS;
import static ru.yandex.direct.core.testing.data.TestCampaigns.DEFAULT_BANNER_HREF_PARAMS;

public class CampaignModifyTestUtils {
    private static final List<String> SSP_LIST = List.of("ImSSP");
    private static final List<String> DOMAIN_LIST = List.of("domain.com");
    private static final EnumSet<PlacementType> PLACEMENT_TYPES = EnumSet.of(PlacementType.ADV_GALLERY);
    private static final EnumSet<SmsFlag> SMS_FLAGS =
            EnumSet.of(SmsFlag.MODERATE_RESULT_SMS, SmsFlag.CAMP_FINISHED_SMS);
    private static final List<String> IPS_LIST = List.of("77.1.1.1", "77.1.1.3");
    private static final ContentLanguage CONTENT_LANGUAGE = ContentLanguage.KZ;
    private static final BroadMatch BROAD_MATCH = new BroadMatch()
            .withBroadMatchFlag(false)
            .withBroadMatchLimit(50)
            .withBroadMatchGoalId(123L);
    private static final CampaignAttributionModel CAMPAIGN_ATTRIBUTION_MODEL = CampaignAttributionModel.FIRST_CLICK;

    private static ModelChanges<? extends CommonCampaign> getCampaignModelChanges(CommonCampaign newCommonCampaign) {
        ModelChanges<CommonCampaign> commonCampaignModelChanges =
                ModelChanges.build(newCommonCampaign, CommonCampaign.NAME, newCommonCampaign.getName());

        return commonCampaignModelChanges
                .process(IPS_LIST, CommonCampaign.DISABLED_IPS)
                .process(RandomUtils.nextBoolean(), CommonCampaign.HAS_TITLE_SUBSTITUTION)
                .process(RandomUtils.nextBoolean(), CommonCampaign.HAS_EXTENDED_GEO_TARGETING)
                .process(SMS_FLAGS, CommonCampaign.SMS_FLAGS)
                .process(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL, CommonCampaign.SMS_TIME)
                .process(RandomStringUtils.randomAlphabetic(5) + "@yandex.ru", CommonCampaign.EMAIL)
                .process(CampaignConstants.MIN_CAMPAIGN_WARNING_BALANCE, CommonCampaign.WARNING_BALANCE)
                .process(RandomUtils.nextBoolean(), CommonCampaign.ENABLE_PAUSED_BY_DAY_BUDGET_EVENT)
                .process(RandomUtils.nextBoolean(), CommonCampaign.ENABLE_SEND_ACCOUNT_NEWS)
                .process(DEFAULT_ENABLE_COMPANY_INFO, CommonCampaign.ENABLE_COMPANY_INFO)
                .process(DEFAULT_ENABLE_CPC_HOLD, CommonCampaign.ENABLE_CPC_HOLD);
    }

    public static ModelChanges<TextCampaign> getTextCampaignModelChanges(List<Long> metrikaCounters,
                                                                         TextCampaign newTextCampaign,
                                                                         boolean newHasSiteMonitoring) {
        ModelChanges<TextCampaign> campaignModelChanges =
                (ModelChanges<TextCampaign>) getCampaignModelChanges(newTextCampaign);
        return campaignModelChanges
                .process(metrikaCounters, TextCampaign.METRIKA_COUNTERS)
                .process(SSP_LIST, TextCampaign.DISABLED_SSP)
                .process(DOMAIN_LIST, TextCampaign.DISABLED_DOMAINS)
                .process(PLACEMENT_TYPES, TextCampaign.PLACEMENT_TYPES)
                .process(BROAD_MATCH, TextCampaign.BROAD_MATCH)
                .process(DEFAULT_ADD_METRIKA_TAG_TO_URL, TextCampaign.HAS_ADD_METRIKA_TAG_TO_URL)
                .process(DEFAULT_ADD_OPENSTAT_TAG_TO_URL, TextCampaign.HAS_ADD_OPENSTAT_TAG_TO_URL)
                .process(true, TextCampaign.ENABLE_CHECK_POSITION_EVENT)
                .process(CampaignWarnPlaceInterval._30, TextCampaign.CHECK_POSITION_INTERVAL_EVENT)
                .process(RandomUtils.nextBoolean(), TextCampaign.ENABLE_OFFLINE_STAT_NOTICE)
                .process(CONTENT_LANGUAGE, TextCampaign.CONTENT_LANGUAGE)
                .process(CAMPAIGN_ATTRIBUTION_MODEL, TextCampaign.ATTRIBUTION_MODEL)
                .process(newHasSiteMonitoring, TextCampaign.HAS_SITE_MONITORING)
                .process(DEFAULT_CONTEXT_LIMIT, TextCampaign.CONTEXT_LIMIT)
                .process(DEFAULT_EXCLUDE_PAUSED_COMPETING_ADS, TextCampaign.EXCLUDE_PAUSED_COMPETING_ADS)
                .process(DEFAULT_BANNER_HREF_PARAMS, TextCampaign.BANNER_HREF_PARAMS);
    }

    public static ModelChanges<TextCampaign> getTextCampaignModelChanges(List<Long> metrikaCounters,
                                                                         TextCampaign newTextCampaign) {
        return getTextCampaignModelChanges(
                metrikaCounters,
                newTextCampaign,
                RandomUtils.nextBoolean());
    }

    public static ModelChanges<DynamicCampaign> getDynamicCampaignModelChanges(List<Long> metrikaCounters,
                                                                               DynamicCampaign newDynamicCampaign,
                                                                               boolean newHasSiteMonitoring) {
        ModelChanges<DynamicCampaign> campaignModelChanges =
                (ModelChanges<DynamicCampaign>) getCampaignModelChanges(newDynamicCampaign);
        return campaignModelChanges
                .process(metrikaCounters, DynamicCampaign.METRIKA_COUNTERS)
                .process(SSP_LIST, DynamicCampaign.DISABLED_SSP)
                .process(DOMAIN_LIST, DynamicCampaign.DISABLED_DOMAINS)
                .process(PLACEMENT_TYPES, DynamicCampaign.PLACEMENT_TYPES)
                .process(DEFAULT_ADD_METRIKA_TAG_TO_URL, DynamicCampaign.HAS_ADD_METRIKA_TAG_TO_URL)
                .process(DEFAULT_ADD_OPENSTAT_TAG_TO_URL, DynamicCampaign.HAS_ADD_OPENSTAT_TAG_TO_URL)
                .process(true, DynamicCampaign.ENABLE_CHECK_POSITION_EVENT)
                .process(CampaignWarnPlaceInterval._30, DynamicCampaign.CHECK_POSITION_INTERVAL_EVENT)
                .process(RandomUtils.nextBoolean(), DynamicCampaign.ENABLE_OFFLINE_STAT_NOTICE)
                .process(CONTENT_LANGUAGE, DynamicCampaign.CONTENT_LANGUAGE)
                .process(CAMPAIGN_ATTRIBUTION_MODEL, DynamicCampaign.ATTRIBUTION_MODEL)
                .process(newHasSiteMonitoring, DynamicCampaign.HAS_SITE_MONITORING)
                .process(DEFAULT_BANNER_HREF_PARAMS, DynamicCampaign.BANNER_HREF_PARAMS);
    }

    public static ModelChanges<DynamicCampaign> getDynamicCampaignModelChanges(List<Long> metrikaCounters,
                                                                               DynamicCampaign newDynamicCampaign) {
        return getDynamicCampaignModelChanges(
                metrikaCounters,
                newDynamicCampaign,
                RandomUtils.nextBoolean());
    }

    public static ModelChanges<SmartCampaign> getSmartCampaignModelChanges(List<Long> metrikaCounters,
                                                                           SmartCampaign newSmartCampaign,
                                                                           boolean newHasSiteMonitoring) {
        ModelChanges<SmartCampaign> campaignModelChanges =
                (ModelChanges<SmartCampaign>) getCampaignModelChanges(newSmartCampaign);
        return campaignModelChanges
                .process(metrikaCounters, SmartCampaign.METRIKA_COUNTERS)
                .process(SSP_LIST, SmartCampaign.DISABLED_SSP)
                .process(DOMAIN_LIST, SmartCampaign.DISABLED_DOMAINS)
                .process(DEFAULT_ADD_METRIKA_TAG_TO_URL, SmartCampaign.HAS_ADD_METRIKA_TAG_TO_URL)
                .process(RandomUtils.nextBoolean(), SmartCampaign.ENABLE_OFFLINE_STAT_NOTICE)
                .process(CONTENT_LANGUAGE, SmartCampaign.CONTENT_LANGUAGE)
                .process(CAMPAIGN_ATTRIBUTION_MODEL, SmartCampaign.ATTRIBUTION_MODEL)
                .process(DEFAULT_CONTEXT_LIMIT, SmartCampaign.CONTEXT_LIMIT)
                .process(DEFAULT_BANNER_HREF_PARAMS, SmartCampaign.BANNER_HREF_PARAMS);
    }

    public static ModelChanges<SmartCampaign> getSmartCampaignModelChanges(List<Long> metrikaCounters,
                                                                           SmartCampaign newSmartCampaign) {
        return getSmartCampaignModelChanges(
                metrikaCounters,
                newSmartCampaign,
                RandomUtils.nextBoolean());
    }

    public static ModelChanges<McBannerCampaign> getMcBannerCampaignModelChanges(List<Long> metrikaCounters,
                                                                                 McBannerCampaign newMcBannerCampaign,
                                                                                 boolean newHasSiteMonitoring) {
        ModelChanges<McBannerCampaign> campaignModelChanges =
                (ModelChanges<McBannerCampaign>) getCampaignModelChanges(newMcBannerCampaign);
        return campaignModelChanges
                .process(metrikaCounters, McBannerCampaign.METRIKA_COUNTERS)
                .process(SSP_LIST, McBannerCampaign.DISABLED_SSP)
                .process(DOMAIN_LIST, McBannerCampaign.DISABLED_DOMAINS)
                .process(DEFAULT_ADD_METRIKA_TAG_TO_URL, McBannerCampaign.HAS_ADD_METRIKA_TAG_TO_URL)
                .process(RandomUtils.nextBoolean(), McBannerCampaign.ENABLE_OFFLINE_STAT_NOTICE)
                .process(CONTENT_LANGUAGE, McBannerCampaign.CONTENT_LANGUAGE)
                .process(CAMPAIGN_ATTRIBUTION_MODEL, McBannerCampaign.ATTRIBUTION_MODEL)
                .process(DEFAULT_CONTEXT_LIMIT, McBannerCampaign.CONTEXT_LIMIT);
    }

    public static ModelChanges<McBannerCampaign> getMcBannerCampaignModelChanges(List<Long> metrikaCounters,
                                                                                 McBannerCampaign newMcBannerCampaign) {
        return getMcBannerCampaignModelChanges(
                metrikaCounters,
                newMcBannerCampaign,
                RandomUtils.nextBoolean());
    }

    private static CommonCampaign getExpectedCampaign(CampaignInfo campaignInfo,
                                                      CommonCampaign commonCampaign,
                                                      ModelChanges<? extends CommonCampaign> modelChanges) {
        return commonCampaign
                .withId(campaignInfo.getCampaignId())
                .withClientId(campaignInfo.getClientId().asLong())
                .withWalletId(campaignInfo.getCampaign().getWalletId())
                .withName(modelChanges.getPropIfChanged(CommonCampaign.NAME))
                .withDisabledIps(modelChanges.getPropIfChanged(CommonCampaign.DISABLED_IPS))
                .withHasTitleSubstitution(modelChanges.getPropIfChanged(CommonCampaign.HAS_TITLE_SUBSTITUTION))
                .withSmsTime(modelChanges.getPropIfChanged(CommonCampaign.SMS_TIME))
                .withSmsFlags(modelChanges.getPropIfChanged(CommonCampaign.SMS_FLAGS))
                .withEmail(modelChanges.getPropIfChanged(CommonCampaign.EMAIL))
                .withWarningBalance(modelChanges.getPropIfChanged(CommonCampaign.WARNING_BALANCE))
                .withEnableSendAccountNews(modelChanges.getPropIfChanged(CommonCampaign.ENABLE_SEND_ACCOUNT_NEWS))
                .withEnablePausedByDayBudgetEvent(modelChanges
                        .getPropIfChanged(CommonCampaign.ENABLE_PAUSED_BY_DAY_BUDGET_EVENT))
                .withEnableCpcHold(DEFAULT_ENABLE_CPC_HOLD)
                .withEnableCompanyInfo(DEFAULT_ENABLE_COMPANY_INFO);
    }

    public static CommonCampaign getExpectedCampaignByCampaignType(CampaignType campaignType,
                                                                   CampaignInfo campaignInfo,
                                                                   ModelChanges<? extends CommonCampaign> modelChanges) {
        if (campaignType == CampaignType.DYNAMIC) {
            return getExpectedDynamicCampaign(campaignInfo, (ModelChanges<DynamicCampaign>) modelChanges);
        } else if (campaignType == CampaignType.MCBANNER) {
            return getExpectedMcBannerCampaign(campaignInfo, (ModelChanges<McBannerCampaign>) modelChanges);
        }
        return getExpectedTextCampaign(campaignInfo, (ModelChanges<TextCampaign>) modelChanges);
    }

    public static TextCampaign getExpectedTextCampaign(CampaignInfo campaignInfo,
                                                       ModelChanges<TextCampaign> modelChanges) {
        TextCampaign textCampaign = (TextCampaign) getExpectedCampaign(campaignInfo, new TextCampaign(), modelChanges);
        return textCampaign
                .withType(CampaignType.TEXT)
                .withMetrikaCounters(modelChanges.getPropIfChanged(TextCampaign.METRIKA_COUNTERS))
                .withDisabledSsp(modelChanges.getPropIfChanged(TextCampaign.DISABLED_SSP))
                .withDisabledDomains(modelChanges.getPropIfChanged(TextCampaign.DISABLED_DOMAINS))
                .withPlacementTypes(modelChanges.getPropIfChanged(TextCampaign.PLACEMENT_TYPES))
                .withBroadMatch(modelChanges.getPropIfChanged(TextCampaign.BROAD_MATCH))
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
                .withEnableCheckPositionEvent(modelChanges.getPropIfChanged(TextCampaign.ENABLE_CHECK_POSITION_EVENT))
                .withCheckPositionIntervalEvent(modelChanges
                        .getPropIfChanged(TextCampaign.CHECK_POSITION_INTERVAL_EVENT))
                .withEnableOfflineStatNotice(modelChanges.getPropIfChanged(TextCampaign.ENABLE_OFFLINE_STAT_NOTICE))
                .withContentLanguage(modelChanges.getPropIfChanged(TextCampaign.CONTENT_LANGUAGE))
                .withHasSiteMonitoring(modelChanges.getPropIfChanged(TextCampaign.HAS_SITE_MONITORING))
                .withAttributionModel(modelChanges.getPropIfChanged((TextCampaign.ATTRIBUTION_MODEL)))
                .withExcludePausedCompetingAds(DEFAULT_EXCLUDE_PAUSED_COMPETING_ADS)
                .withContextLimit(DEFAULT_CONTEXT_LIMIT)
                .withBannerHrefParams(modelChanges.getPropIfChanged(TextCampaign.BANNER_HREF_PARAMS));
    }

    public static DynamicCampaign getExpectedDynamicCampaign(CampaignInfo campaignInfo,
                                                             ModelChanges<DynamicCampaign> modelChanges) {
        DynamicCampaign dynamicCampaign =
                (DynamicCampaign) getExpectedCampaign(campaignInfo, new DynamicCampaign(), modelChanges);
        return dynamicCampaign
                .withType(CampaignType.DYNAMIC)
                .withMetrikaCounters(modelChanges.getPropIfChanged(DynamicCampaign.METRIKA_COUNTERS))
                .withDisabledSsp(modelChanges.getPropIfChanged(DynamicCampaign.DISABLED_SSP))
                .withDisabledDomains(modelChanges.getPropIfChanged(DynamicCampaign.DISABLED_DOMAINS))
                .withPlacementTypes(modelChanges.getPropIfChanged(DynamicCampaign.PLACEMENT_TYPES))
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
                .withEnableCheckPositionEvent(modelChanges.getPropIfChanged(DynamicCampaign.ENABLE_CHECK_POSITION_EVENT))
                .withCheckPositionIntervalEvent(modelChanges
                        .getPropIfChanged(DynamicCampaign.CHECK_POSITION_INTERVAL_EVENT))
                .withEnableOfflineStatNotice(modelChanges.getPropIfChanged(DynamicCampaign.ENABLE_OFFLINE_STAT_NOTICE))
                .withContentLanguage(modelChanges.getPropIfChanged(DynamicCampaign.CONTENT_LANGUAGE))
                .withHasSiteMonitoring(modelChanges.getPropIfChanged(DynamicCampaign.HAS_SITE_MONITORING))
                .withAttributionModel(modelChanges.getPropIfChanged((DynamicCampaign.ATTRIBUTION_MODEL)))
                .withBannerHrefParams(modelChanges.getPropIfChanged(DynamicCampaign.BANNER_HREF_PARAMS));
    }

    public static SmartCampaign getExpectedSmartCampaign(CampaignInfo campaignInfo,
                                                         ModelChanges<SmartCampaign> modelChanges) {
        SmartCampaign smartCampaign =
                (SmartCampaign) getExpectedCampaign(campaignInfo, new SmartCampaign(), modelChanges);
        return smartCampaign
                .withType(CampaignType.PERFORMANCE)
                .withMetrikaCounters(modelChanges.getPropIfChanged(SmartCampaign.METRIKA_COUNTERS))
                .withDisabledSsp(modelChanges.getPropIfChanged(SmartCampaign.DISABLED_SSP))
                .withDisabledDomains(modelChanges.getPropIfChanged(SmartCampaign.DISABLED_DOMAINS))
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withEnableOfflineStatNotice(modelChanges.getPropIfChanged(SmartCampaign.ENABLE_OFFLINE_STAT_NOTICE))
                .withContentLanguage(modelChanges.getPropIfChanged(SmartCampaign.CONTENT_LANGUAGE))
                .withAttributionModel(modelChanges.getPropIfChanged((SmartCampaign.ATTRIBUTION_MODEL)))
                .withContextLimit(DEFAULT_CONTEXT_LIMIT)
                .withBannerHrefParams(modelChanges.getPropIfChanged(SmartCampaign.BANNER_HREF_PARAMS));
    }

    public static McBannerCampaign getExpectedMcBannerCampaign(CampaignInfo campaignInfo,
                                                               ModelChanges<McBannerCampaign> modelChanges) {
        McBannerCampaign mcBannerCampaign =
                (McBannerCampaign) getExpectedCampaign(campaignInfo, new McBannerCampaign(), modelChanges);
        return mcBannerCampaign
                .withType(CampaignType.MCBANNER)
                .withMetrikaCounters(modelChanges.getPropIfChanged(TextCampaign.METRIKA_COUNTERS))
                .withDisabledSsp(modelChanges.getPropIfChanged(TextCampaign.DISABLED_SSP))
                .withDisabledDomains(modelChanges.getPropIfChanged(TextCampaign.DISABLED_DOMAINS))
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
                .withEnableOfflineStatNotice(modelChanges.getPropIfChanged(TextCampaign.ENABLE_OFFLINE_STAT_NOTICE))
                .withContentLanguage(modelChanges.getPropIfChanged(TextCampaign.CONTENT_LANGUAGE))
                .withHasSiteMonitoring(modelChanges.getPropIfChanged(TextCampaign.HAS_SITE_MONITORING))
                .withAttributionModel(modelChanges.getPropIfChanged((TextCampaign.ATTRIBUTION_MODEL)))
                .withContextLimit(DEFAULT_CONTEXT_LIMIT);
    }

    public static RestrictedCampaignsAddOperationContainer createAddCampaignParametersContainer(
            UserInfo clientUser,
            UserInfo operatorUser) {
        return RestrictedCampaignsAddOperationContainer.create(
                clientUser.getShard(),
                operatorUser.getUid(),
                clientUser.getClientId(),
                clientUser.getUid(),
                clientUser.getUid());
    }

    public static BigDecimal getZero(int scale) {
        return BigDecimal.valueOf(0, scale);
    }
}
