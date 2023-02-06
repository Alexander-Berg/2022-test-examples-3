package ru.yandex.direct.core.testing.steps;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import junitparams.converters.Nullable;
import org.jooq.Configuration;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.adgroup.model.DynamicFeedAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.entity.feed.model.FeedType;
import ru.yandex.direct.core.entity.feed.model.Source;
import ru.yandex.direct.core.entity.feedfilter.model.FeedFilter;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.testing.data.campaign.TestCpmBannerCampaigns;
import ru.yandex.direct.core.testing.data.campaign.TestCpmPriceCampaigns;
import ru.yandex.direct.core.testing.data.campaign.TestCpmYndxFrontPageCampaigns;
import ru.yandex.direct.core.testing.data.campaign.TestDynamicCampaigns;
import ru.yandex.direct.core.testing.data.campaign.TestSmartCampaigns;
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.info.campaign.ContentPromotionCampaignInfo;
import ru.yandex.direct.core.testing.info.campaign.CpmBannerCampaignInfo;
import ru.yandex.direct.core.testing.info.campaign.CpmPriceCampaignInfo;
import ru.yandex.direct.core.testing.info.campaign.CpmYndxFrontpageCampaignInfo;
import ru.yandex.direct.core.testing.info.campaign.DynamicCampaignInfo;
import ru.yandex.direct.core.testing.info.campaign.InternalAutobudgetCampaignInfo;
import ru.yandex.direct.core.testing.info.campaign.InternalDistribCampaignInfo;
import ru.yandex.direct.core.testing.info.campaign.InternalFreeCampaignInfo;
import ru.yandex.direct.core.testing.info.campaign.MobileContentCampaignInfo;
import ru.yandex.direct.core.testing.info.campaign.SmartCampaignInfo;
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo;
import ru.yandex.direct.core.testing.steps.campaign.ContentPromotionCampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.CpmBannerCampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.CpmPriceCampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.CpmYndxFrontpageCampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.DynamicCampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.InternalAutobudgetCampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.InternalDistribCampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.InternalFreeCampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.MobileContentCampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.SmartCampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.TextCampaignSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.common.util.RepositoryUtils.zeroableDateTimeToDb;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeContentPromotionCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmPriceCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmYndxFrontpageCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeDynamicCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeMcBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeMobileContentCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activePerformanceCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualCpmBothDifferentStrategy;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestGroups.activeContentPromotionAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmAudioAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmGeoPinAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmGeoproductAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmIndoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmOutdoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmVideoAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmYndxFrontpageAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmYndxFrontpageAdGroupWithPriority;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDefaultAdGroupForPriceSales;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDefaultVideoAdGroupForPriceSales;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicFeedAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeInternalAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMcBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMobileAppAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeNonSkippableCpmVideoAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activePerformanceAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeSpecificAdGroupForPriceSales;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.createMobileAppAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultPerformanceAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.draftContentPromotionAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.draftCpmIndoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.draftDynamicFeedAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.draftDynamicTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.draftPerformanceAdGroup;
import static ru.yandex.direct.core.testing.data.TestUserSegments.createAllTypesSegments;
import static ru.yandex.direct.core.testing.data.adgroup.TestContentPromotionAdGroups.fullContentPromotionAdGroup;
import static ru.yandex.direct.core.testing.data.campaign.TestMobileContentCampaigns.fullMobileContentCampaign;
import static ru.yandex.direct.dbschema.ppc.tables.Phrases.PHRASES;

@SuppressWarnings("WeakerAccess")
public class AdGroupSteps {

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private TextCampaignSteps textCampaignSteps;
    @Autowired
    private CpmBannerCampaignSteps cpmBannerCampaignSteps;
    @Autowired
    private CpmYndxFrontpageCampaignSteps cpmYndxFrontPageCampaignSteps;
    @Autowired
    private CpmPriceCampaignSteps cpmPriceCampaignSteps;
    @Autowired
    private MobileContentCampaignSteps mobileContentCampaignSteps;
    @Autowired
    private DynamicCampaignSteps dynamicCampaignSteps;
    @Autowired
    private SmartCampaignSteps smartCampaignSteps;
    @Autowired
    private ContentPromotionCampaignSteps contentPromotionCampaignSteps;
    @Autowired
    private InternalAutobudgetCampaignSteps internalAutobudgetCampaignSteps;
    @Autowired
    private InternalDistribCampaignSteps internalDistribCampaignSteps;
    @Autowired
    private InternalFreeCampaignSteps internalFreeCampaignSteps;
    @Autowired
    private CampaignSteps campaignSteps;
    @Autowired
    private PlacementSteps placementSteps;
    @Autowired
    private ClientSteps clientSteps;
    @Autowired
    private FeedSteps feedSteps;

    public AdGroupInfo createActiveAdGroupByType(AdGroupType adGroupType, ClientInfo clientInfo) {
        switch (adGroupType) {
            case BASE:
                return createActiveTextAdGroup(clientInfo);
            case MOBILE_CONTENT:
                return createActiveMobileContentAdGroup(clientInfo);
            default:
                throw new IllegalArgumentException("Unknown adGroup type " + adGroupType);
        }
    }

    public AdGroupInfo createActiveAdGroupByType(AdGroupType adGroupType, CampaignInfo campaignInfo) {
        return createActiveAdGroupByType(adGroupType, campaignInfo, null);
    }

    public AdGroupInfo createActiveAdGroupByType(AdGroupType adGroupType, CampaignInfo campaignInfo,
                                                 @Nullable Long feedId) {
        switch (adGroupType) {
            case BASE:
                return createActiveTextAdGroup(campaignInfo);
            case DYNAMIC:
                return createActiveDynamicTextAdGroup(campaignInfo);
            case PERFORMANCE:
                return createActivePerformanceAdGroup(campaignInfo, feedId);
            case MOBILE_CONTENT:
                return createActiveMobileContentAdGroup(campaignInfo);
            case MCBANNER:
                return createActiveMcBannerAdGroup(campaignInfo);
            default:
                throw new IllegalArgumentException("Неизвестный тип группы: " + adGroupType);
        }
    }

    public AdGroupInfo createActiveTextAdGroup() {
        return createActiveTextAdGroup(new ClientInfo().withClient(defaultClient()));
    }

    public AdGroupInfo createActiveTextAdGroup(ClientInfo clientInfo) {
        TextCampaignInfo campaignInfo = new TextCampaignInfo();
        campaignInfo.withTypedCampaign(TestTextCampaigns.INSTANCE.fullTextCampaign());
        campaignInfo.withCampaign(activeTextCampaign(null, null))
                .withClientInfo(clientInfo);
        return createActiveTextAdGroup(campaignInfo);
    }

    public AdGroupInfo createActiveTextAdGroup(ClientInfo clientInfo, @Nullable Long feedId,
                                               @Nullable FeedFilter feedFilter) {
        CampaignInfo campaignInfo = new TextCampaignInfo()
                .withTypedCampaign(TestTextCampaigns.fullTextCampaign())
                .withCampaign(activeTextCampaign(null, null))
                .withClientInfo(clientInfo);
        return createActiveTextAdGroup(campaignInfo, feedId, feedFilter);
    }

    public AdGroupInfo createActiveInternalAdGroup() {
        CampaignInfo campaignInfo = campaignSteps.createActiveInternalDistribCampaign();
        return createActiveInternalAdGroup(campaignInfo);
    }

    public AdGroupInfo createActiveInternalAdGroup(ClientInfo clientInfo) {
        CampaignInfo campaignInfo = campaignSteps.createActiveInternalDistribCampaign(clientInfo);
        return createActiveInternalAdGroup(campaignInfo);
    }

    public AdGroupInfo createActiveInternalAdGroup(CampaignInfo campaignInfo) {
        return createAdGroup(activeInternalAdGroup(campaignInfo.getCampaignId(), 0L), campaignInfo);
    }

    public AdGroupInfo createActiveInternalAdGroup(CampaignInfo campaignInfo, Long level, Integer rf, Integer rfReset) {
        return createAdGroup(activeInternalAdGroup(campaignInfo.getCampaignId(), level, rf, rfReset), campaignInfo);
    }

    public AdGroupInfo createActiveInternalAdGroup(CampaignInfo campaignInfo, Long level, Integer rf, Integer rfReset,
                                                   Integer maxClicksCount, Integer maxClicksPeriod,
                                                   Integer maxStopsCount, Integer maxStopsPeriod) {
        return createAdGroup(activeInternalAdGroup(campaignInfo.getCampaignId(), level, rf, rfReset, null, null,
                maxClicksCount, maxClicksPeriod, maxStopsCount, maxStopsPeriod), campaignInfo);
    }

    public AdGroupInfo createActiveTextAdGroup(CampaignInfo campaignInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeTextAdGroup(null))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createActiveTextAdGroup(CampaignInfo campaignInfo, @Nullable Long feedId,
                                               @Nullable FeedFilter feedFilter) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeTextAdGroup(null, feedId, feedFilter))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createActiveMobileContentAdGroup() {
        return createActiveMobileContentAdGroup(new ClientInfo().withClient(defaultClient()));
    }

    public AdGroupInfo createActiveMobileContentAdGroup(ClientInfo clientInfo) {
        CampaignInfo campaignInfo = new MobileContentCampaignInfo()
                .withTypedCampaign(fullMobileContentCampaign(null))
                .withCampaign(activeMobileContentCampaign(null, null))
                .withClientInfo(clientInfo);
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeMobileAppAdGroup(null))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createActiveMobileContentAdGroup(CampaignInfo campaignInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeMobileAppAdGroup(null))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createActiveMobileContentAdGroup(MobileContentInfo mobileContentInfo) {
        CampaignInfo campaignInfo = new MobileContentCampaignInfo()
                .withTypedCampaign(fullMobileContentCampaign(null))
                .withCampaign(activeMobileContentCampaign(null, null))
                .withClientInfo(mobileContentInfo.getClientInfo());
        return createActiveMobileContentAdGroup(campaignInfo, mobileContentInfo);
    }

    public AdGroupInfo createActiveMobileContentAdGroup(CampaignInfo campaignInfo,
                                                        MobileContentInfo mobileContentInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(createMobileAppAdGroup(null, mobileContentInfo.getMobileContent()))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createActiveDynamicTextAdGroup() {
        return createActiveDynamicTextAdGroup(new ClientInfo().withClient(defaultClient()));
    }

    public AdGroupInfo createActiveDynamicTextAdGroup(ClientInfo clientInfo) {
        return createDynamicTextAdGroup(clientInfo, activeDynamicTextAdGroup(null));
    }

    public AdGroupInfo createDraftDynamicTextAdGroup(ClientInfo clientInfo) {
        return createDynamicTextAdGroup(clientInfo, draftDynamicTextAdGroup(null));
    }

    public AdGroupInfo createDynamicTextAdGroup(ClientInfo clientInfo, DynamicTextAdGroup adGroup) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(adGroup)
                .withCampaignInfo(new DynamicCampaignInfo()
                        .withCampaign(activeDynamicCampaign(null, null))
                        .withClientInfo(clientInfo)));
    }

    public AdGroupInfo createDynamicTextAdGroup(ClientInfo clientInfo, CampaignInfo campaignInfo,
                                                DynamicTextAdGroup adGroup) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(adGroup)
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createDynamicTextAdGroup(CampaignInfo campaignInfo, DynamicTextAdGroup adGroup) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(adGroup.withCampaignId(campaignInfo.getCampaignId()))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createActiveDynamicTextAdGroup(CampaignInfo campaignInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeDynamicTextAdGroup(null))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createActivePerformanceAdGroup(Long feedId) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activePerformanceAdGroup(null, feedId))
                .withCampaignInfo(new SmartCampaignInfo()
                        .withTypedCampaign(TestSmartCampaigns.INSTANCE.fullSmartCampaign())
                        .withCampaign(activePerformanceCampaign(null, null))));
    }

    public AdGroupInfo createActivePerformanceAdGroup(ClientInfo clientInfo, Long feedId) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activePerformanceAdGroup(null, feedId))
                .withCampaignInfo(new SmartCampaignInfo()
                        .withTypedCampaign(TestSmartCampaigns.INSTANCE.fullSmartCampaign())
                        .withCampaign(activePerformanceCampaign(null, null))
                        .withClientInfo(clientInfo)));
    }

    public AdGroupInfo createActivePerformanceAdGroup(CampaignInfo campaignInfo, Long feedId) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activePerformanceAdGroup(campaignInfo.getCampaignId(), feedId))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createActiveCpmBannerAdGroup() {
        return createActiveCpmBannerAdGroup(new ClientInfo().withClient(defaultClient()));
    }

    public AdGroupInfo createActiveCpmGeoproductAdGroup() {
        return createActiveCpmGeoproductAdGroup(new ClientInfo().withClient(defaultClient()));
    }

    public AdGroupInfo createActiveCpmOutdoorAdGroup() {
        return createActiveCpmOutdoorAdGroup(new ClientInfo().withClient(defaultClient()));
    }

    public AdGroupInfo createActiveCpmBannerAdGroup(CampaignInfo campaignInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmBannerAdGroup(campaignInfo.getCampaignId()))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createActiveCpmBannerAdGroupWithKeywordsCriterionType(CampaignInfo campaignInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmBannerAdGroup(campaignInfo.getCampaignId())
                        .withCriterionType(CriterionType.KEYWORD))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createActiveCpmBannerAdGroup(CampaignInfo campaignInfo, CriterionType criterionType) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmBannerAdGroup(campaignInfo.getCampaignId()).withCriterionType(criterionType))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createActiveCpmBannerAdGroup(ClientInfo clientInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmBannerAdGroup(null))
                .withCampaignInfo(new CpmBannerCampaignInfo()
                        .withTypedCampaign(TestCpmBannerCampaigns.INSTANCE.fullCpmBannerCampaign()
                                .withStrategy(defaultCpmStrategy()))
                        .withCampaign(activeCpmBannerCampaign(null, null))
                        .withClientInfo(clientInfo)));
    }

    public AdGroupInfo createActiveCpmBannerAdGroupWithAllVideoShowTypes(ClientInfo clientInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmBannerAdGroup(null)
                        .withUsersSegments(createAllTypesSegments()))
                .withCampaignInfo(new CpmBannerCampaignInfo()
                        .withTypedCampaign(TestCpmBannerCampaigns.INSTANCE.fullCpmBannerCampaign()
                                .withStrategy(defaultCpmStrategy()))
                        .withCampaign(activeCpmBannerCampaign(null, null))
                        .withClientInfo(clientInfo)));
    }

    public AdGroupInfo createActiveCpmBannerAdGroup(ClientInfo clientInfo, CriterionType criterionType) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmBannerAdGroup(null).withCriterionType(criterionType))
                .withCampaignInfo(new CpmBannerCampaignInfo()
                        .withTypedCampaign(TestCpmBannerCampaigns.INSTANCE.fullCpmBannerCampaign()
                                .withStrategy(defaultCpmStrategy()))
                        .withCampaign(activeCpmBannerCampaign(null, null))
                        .withClientInfo(clientInfo)));
    }

    public AdGroupInfo createActiveCpmBannerAdGroupWithManualStrategy(
            ClientInfo clientInfo, CriterionType criterionType) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmBannerAdGroup(null).withCriterionType(criterionType))
                .withCampaignInfo(new CpmBannerCampaignInfo()
                        .withTypedCampaign(TestCpmBannerCampaigns.INSTANCE.fullCpmBannerCampaign()
                                .withStrategy(manualCpmBothDifferentStrategy()))
                        .withCampaign(activeCpmBannerCampaign(null, null))
                        .withClientInfo(clientInfo)));
    }

    public AdGroupInfo createActiveCpmGeoproductAdGroup(ClientInfo clientInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmGeoproductAdGroup(null))
                .withCampaignInfo(new CpmBannerCampaignInfo()
                        .withTypedCampaign(TestCpmBannerCampaigns.INSTANCE.fullCpmBannerCampaign()
                                .withStrategy(defaultCpmStrategy()))
                        .withCampaign(activeCpmBannerCampaign(null, null))
                        .withClientInfo(clientInfo)));
    }

    public AdGroupInfo createActiveCpmGeoproductAdGroup(CampaignInfo campaignInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmGeoproductAdGroup(campaignInfo.getCampaignId()))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createActiveCpmGeoPinAdGroup(ClientInfo clientInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmGeoPinAdGroup(null))
                .withCampaignInfo(new CpmBannerCampaignInfo()
                        .withTypedCampaign(TestCpmBannerCampaigns.INSTANCE.fullCpmBannerCampaign()
                                .withStrategy(defaultCpmStrategy()))
                        .withCampaign(activeCpmBannerCampaign(null, null))
                        .withClientInfo(clientInfo)));
    }

    public AdGroupInfo createActiveCpmGeoPinAdGroup() {
        return createActiveCpmGeoPinAdGroup(new ClientInfo().withClient(defaultClient()));
    }

    public AdGroupInfo createActiveCpmGeoPinAdGroup(CampaignInfo campaignInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmGeoPinAdGroup(campaignInfo.getCampaignId()))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createActiveCpmVideoAdGroup(ClientId clientId, Long uid, ClientInfo clientInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmVideoAdGroup(null))
                .withCampaignInfo(new CpmBannerCampaignInfo()
                        .withTypedCampaign(TestCpmBannerCampaigns.INSTANCE.fullCpmBannerCampaign()
                                .withStrategy(defaultCpmStrategy()))
                        .withCampaign(activeCpmBannerCampaign(clientId, uid))
                        .withClientInfo(clientInfo)));
    }

    public AdGroupInfo createActiveCpmVideoAdGroupWithAllVideoGoalTypes(ClientInfo clientInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmVideoAdGroup(null)
                        .withUsersSegments(createAllTypesSegments()))
                .withCampaignInfo(new CpmBannerCampaignInfo()
                        .withTypedCampaign(TestCpmBannerCampaigns.INSTANCE.fullCpmBannerCampaign()
                                .withStrategy(defaultCpmStrategy()))
                        .withCampaign(activeCpmBannerCampaign(null, null))
                        .withClientInfo(clientInfo)));
    }

    public AdGroupInfo createActiveCpmVideoAdGroup(CampaignInfo campaignInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmVideoAdGroup(campaignInfo.getCampaignId()))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createActiveCpmAudioAdGroup(CampaignInfo campaignInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmAudioAdGroup(campaignInfo.getCampaignId()))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createActiveCpmAudioAdGroup() {
        return createActiveCpmAudioAdGroup(new ClientInfo().withClient(defaultClient()));
    }

    public AdGroupInfo createActiveCpmAudioAdGroup(ClientInfo clientInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmAudioAdGroup(null))
                .withClientInfo(clientInfo));
    }

    public AdGroupInfo createActiveCpmVideoAdGroupWithGeo(List<Long> geo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmVideoAdGroup(null).withGeo(geo))
                .withClientInfo(new ClientInfo().withClient(defaultClient())));
    }

    public AdGroupInfo createActiveCpmVideoAdGroup() {
        return createActiveCpmVideoAdGroup(new ClientInfo().withClient(defaultClient()));
    }

    public AdGroupInfo createActiveCpmVideoAdGroup(ClientInfo clientInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmVideoAdGroup(null))
                .withClientInfo(clientInfo));
    }

    public AdGroupInfo createActiveNonSkippableCpmVideoAdGroup(CampaignInfo campaignInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeNonSkippableCpmVideoAdGroup(campaignInfo.getCampaignId()))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createActiveCpmOutdoorAdGroup(ClientInfo clientInfo) {
        OutdoorPlacement placement = placementSteps.addDefaultOutdoorPlacementWithOneBlock();
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmOutdoorAdGroup(null, placement))
                .withCampaignInfo(new CpmBannerCampaignInfo()
                        .withTypedCampaign(TestCpmBannerCampaigns.INSTANCE.fullCpmBannerCampaign()
                                .withStrategy(defaultCpmStrategy()))
                        .withCampaign(activeCpmBannerCampaign(null, null))
                        .withClientInfo(clientInfo)));
    }

    public AdGroupInfo createActiveCpmOutdoorAdGroup(CampaignInfo campaignInfo) {
        OutdoorPlacement placement = placementSteps.addDefaultOutdoorPlacementWithOneBlock();
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmOutdoorAdGroup(null, placement))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createActiveCpmIndoorAdGroup() {
        return createActiveCpmIndoorAdGroup(new ClientInfo().withClient(defaultClient()));
    }

    public AdGroupInfo createActiveCpmIndoorAdGroup(ClientInfo clientInfo) {
        IndoorPlacement placement = placementSteps.addDefaultIndoorPlacementWithOneBlock();
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmIndoorAdGroup(null, placement))
                .withCampaignInfo(new CpmBannerCampaignInfo()
                        .withTypedCampaign(TestCpmBannerCampaigns.INSTANCE.fullCpmBannerCampaign()
                                .withStrategy(defaultCpmStrategy()))
                        .withCampaign(activeCpmBannerCampaign(null, null))
                        .withClientInfo(clientInfo)));
    }

    public AdGroupInfo createActiveCpmIndoorAdGroup(CampaignInfo campaignInfo) {
        IndoorPlacement placement = placementSteps.addDefaultIndoorPlacementWithOneBlock();
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmIndoorAdGroup(null, placement))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createActiveDynamicFeedAdGroup() {
        ClientInfo clientInfo = clientSteps.createDefaultClient();
        return createActiveDynamicFeedAdGroup(clientInfo);
    }

    public AdGroupInfo createActiveDynamicFeedAdGroup(ClientInfo clientInfo) {
        FeedInfo feedInfo = feedSteps.createDefaultFeed(clientInfo);
        return createActiveDynamicFeedAdGroup(feedInfo);
    }

    public AdGroupInfo createActiveDynamicFeedAdGroup(FeedInfo feedInfo) {
        DynamicFeedAdGroup adGroup = activeDynamicFeedAdGroup(null, feedInfo.getFeedId());
        return createDynamicFeedAdGroup(feedInfo.getClientInfo(), adGroup);
    }

    public AdGroupInfo createActiveDynamicFeedAdGroup(CampaignInfo campaignInfo) {
        FeedInfo feedInfo = feedSteps.createDefaultFeed(campaignInfo.getClientInfo());
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeDynamicFeedAdGroup(null, feedInfo.getFeedId()))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createActiveDynamicFeedAdGroup(CampaignInfo campaignInfo, FeedInfo feedInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeDynamicFeedAdGroup(null, feedInfo.getFeedId()))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createDraftDynamicFeedAdGroup(ClientInfo clientInfo) {
        FeedInfo feedInfo = feedSteps.createDefaultFeed(clientInfo);
        return createDynamicFeedAdGroup(clientInfo, draftDynamicFeedAdGroup(null, feedInfo.getFeedId()));
    }

    public AdGroupInfo createDynamicFeedAdGroup(ClientInfo clientInfo, AdGroup adGroup) {
        CampaignInfo campaignInfo = new DynamicCampaignInfo()
                .withTypedCampaign(TestDynamicCampaigns.fullDynamicCampaign())
                .withCampaign(activeDynamicCampaign(null, null))
                .withClientInfo(clientInfo);
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(adGroup)
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createDynamicFeedAdGroup(ClientInfo clientInfo, DynamicFeedAdGroup adGroup) {
        CampaignInfo campaignInfo = new CampaignInfo()
                .withCampaign(activeDynamicCampaign(null, null))
                .withClientInfo(clientInfo);
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(adGroup)
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createActiveMcBannerAdGroup() {
        return createActiveMcBannerAdGroup(new ClientInfo().withClient(defaultClient()));
    }

    public AdGroupInfo createActiveMcBannerAdGroup(CampaignInfo campaignInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeMcBannerAdGroup(null))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createActiveMcBannerAdGroup(ClientInfo clientInfo) {
        CampaignInfo campaignInfo = new CampaignInfo()
                .withCampaign(activeMcBannerCampaign(null, null))
                .withClientInfo(clientInfo);
        return createActiveMcBannerAdGroup(campaignInfo);
    }

    /**
     * Создает группу CPM_YNDX_FRONTPAGE (плюс кампанию для нее), с полями, заполненными, как будто она активная
     *
     * @param clientInfo клиент, для которого создается группа
     */
    public AdGroupInfo createActiveCpmYndxFrontpageAdGroup(ClientInfo clientInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmYndxFrontpageAdGroup(null))
                .withCampaignInfo(new CpmYndxFrontpageCampaignInfo()
                        .withTypedCampaign(TestCpmYndxFrontPageCampaigns.INSTANCE.fullCpmYndxFrontpageCampaign())
                        .withCampaign(activeCpmYndxFrontpageCampaign(null, null))
                        .withClientInfo(clientInfo)));
    }

    public AdGroupInfo createActiveCpmYndxFrontpageAdGroup(CampaignInfo campaignInfo) {
        return createActiveCpmYndxFrontpageAdGroup(campaignInfo, null);
    }

    public AdGroupInfo createActiveCpmYndxFrontpageAdGroup(CampaignInfo campaignInfo, Long priority) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmYndxFrontpageAdGroupWithPriority(null, priority))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createDefaultAdGroup() {
        return createAdGroup(defaultTextAdGroup(null));
    }

    public AdGroupInfo createDefaultAdGroup(ClientInfo clientInfo) {
        return createAdGroup(defaultTextAdGroup(null), clientInfo);
    }

    public AdGroupInfo createDefaultAdGroup(CampaignInfo campaignInfo) {
        return createAdGroup(null, campaignInfo);
    }

    public AdGroupInfo createDefaultCpmBannerAdGroup() {
        return createAdGroup(activeCpmBannerAdGroup(null));
    }

    public AdGroupInfo createActiveCpmBannerAdGroupWithKeywordsCriterionType() {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmBannerAdGroup(null)
                        .withCriterionType(CriterionType.KEYWORD)));
    }

    public AdGroupInfo createDefaultCpmOutdoorAdGroup() {
        OutdoorPlacement placement = placementSteps.addDefaultOutdoorPlacementWithOneBlock();
        return createAdGroup(activeCpmOutdoorAdGroup(null, placement));
    }

    public AdGroupInfo createDefaultCpmIndoorAdGroup() {
        IndoorPlacement placement = placementSteps.addDefaultIndoorPlacementWithOneBlock();
        return createAdGroup(activeCpmIndoorAdGroup(null, placement));
    }

    public AdGroupInfo createDefaultFixCpmYndxFrontpageAdGroup() {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmYndxFrontpageAdGroup(null))
                .withCampaignInfo(new CpmPriceCampaignInfo()
                        .withTypedCampaign(TestCpmPriceCampaigns.INSTANCE.fullCpmPriceCampaign(null))
                        .withCampaign(activeCpmPriceCampaign(null, null))));
    }

    public AdGroupInfo createDefaultCpmYndxFrontpageAdGroup() {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmYndxFrontpageAdGroup(null))
                .withCampaignInfo(new CpmYndxFrontpageCampaignInfo()
                        .withTypedCampaign(TestCpmYndxFrontPageCampaigns.INSTANCE.fullCpmYndxFrontpageCampaign())
                        .withCampaign(activeCpmYndxFrontpageCampaign(null, null))));
    }

    public AdGroupInfo createDefaultCpmYndxFrontpageAdGroup(CampaignInfo campaignInfo) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmYndxFrontpageAdGroup(campaignInfo.getCampaignId()))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createDefaultCpmVideoAdGroup(ClientInfo clientInfo) {
        return createAdGroup(activeCpmVideoAdGroup(null), clientInfo);
    }

    public AdGroupInfo createDefaultCpmYndxFrontpageAdGroup(ClientInfo clientInfo) {
        return createActiveCpmYndxFrontpageAdGroup(clientInfo);
    }

    /**
     * временное решение, будем думать что делать с этим в DIRECT-108812
     */
    public CpmYndxFrontpageAdGroup createDefaultAdGroupForPriceSales(CpmPriceCampaign priceCampaign,
                                                                     ClientInfo clientInfo) {
        CpmYndxFrontpageAdGroup cpmYndxFrontpageAdGroup = activeDefaultAdGroupForPriceSales(priceCampaign);
        createAdGroupRaw(cpmYndxFrontpageAdGroup, clientInfo);
        return cpmYndxFrontpageAdGroup;
    }

    public CpmVideoAdGroup createDefaultVideoAdGroupForPriceSales(CpmPriceCampaign priceCampaign,
                                                                  ClientInfo clientInfo) {
        CpmVideoAdGroup cpmVideoAdGroup = activeDefaultVideoAdGroupForPriceSales(priceCampaign);
        createAdGroupRaw(cpmVideoAdGroup, clientInfo);
        return cpmVideoAdGroup;
    }

    /**
     * временное решение, будем думать что делать с этим в DIRECT-108812
     */
    public CpmYndxFrontpageAdGroup createSpecificAdGroupForPriceSales(CpmPriceCampaign priceCampaign,
                                                                      ClientInfo clientInfo) {
        CpmYndxFrontpageAdGroup cpmYndxFrontpageAdGroup = activeSpecificAdGroupForPriceSales(priceCampaign);
        createAdGroupRaw(cpmYndxFrontpageAdGroup, clientInfo);
        return cpmYndxFrontpageAdGroup;
    }

    public AdGroupInfo createDefaultCpmGeoproductAdGroup(ClientInfo clientInfo) {
        return createAdGroup(activeCpmGeoproductAdGroup(null), clientInfo);
    }

    public AdGroupInfo createDefaultCpmGeoproductAdGroup(CampaignInfo campaignInfo) {
        return createAdGroup(activeCpmGeoproductAdGroup(campaignInfo.getCampaignId()), campaignInfo.getClientInfo());
    }

    public AdGroupInfo createDefaultInternalAdGroup() {
        return createDefaultInternalAdGroup(0L);
    }

    public AdGroupInfo createDefaultInternalAdGroup(Long level) {
        return createDefaultInternalAdGroup(level, 0, 0);
    }

    public AdGroupInfo createDefaultInternalAdGroup(Long level, Integer rf, Integer rfReset) {
        return createAdGroup(activeInternalAdGroup(null, level, rf, rfReset), new InternalFreeCampaignInfo());
    }

    public AdGroupInfo createDefaultInternalAdGroup(Long level, Integer rf, Integer rfReset,
                                                    LocalDateTime startTime,
                                                    LocalDateTime finishTime,
                                                    CampaignInfo campaignInfo) {
        return createAdGroup(activeInternalAdGroup(null, level, rf, rfReset, startTime, finishTime), campaignInfo);
    }

    public AdGroupInfo createDefaultInternalAdGroup(Long level,
                                                    Integer rf, Integer rfReset,
                                                    LocalDateTime startTime, LocalDateTime finishTime,
                                                    Integer maxClicksCount, Integer maxClicksPeriod,
                                                    Integer maxStopsCount, Integer maxStopsPeriod,
                                                    CampaignInfo campaignInfo) {
        return createAdGroup(activeInternalAdGroup(null, level, rf, rfReset, startTime, finishTime, maxClicksCount,
                maxClicksPeriod, maxStopsCount, maxStopsPeriod), campaignInfo);
    }

    /**
     * Создание CONTENT_PROMOTION группы
     */
    @Deprecated // use steps.contentPromotionAdGroupSteps()
    public AdGroupInfo createDefaultContentPromotionAdGroup(ContentPromotionAdgroupType contentPromotionAdgroupType) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(fullContentPromotionAdGroup(contentPromotionAdgroupType))
                .withCampaignInfo(new ContentPromotionCampaignInfo()
                        .withCampaign(activeContentPromotionCampaign(null, null))));
    }

    @Deprecated // use steps.contentPromotionAdGroupSteps()
    public AdGroupInfo createDefaultContentPromotionAdGroup(ClientInfo clientInfo,
                                                            ContentPromotionAdgroupType contentPromotionAdgroupType) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(fullContentPromotionAdGroup(contentPromotionAdgroupType))
                .withClientInfo(clientInfo)
                .withCampaignInfo(new ContentPromotionCampaignInfo()
                        .withCampaign(activeContentPromotionCampaign(null, null))
                        .withClientInfo(clientInfo)));
    }

    @Deprecated // use steps.contentPromotionAdGroupSteps()
    public AdGroupInfo createDefaultContentPromotionAdGroup(CampaignInfo campaignInfo,
                                                            ContentPromotionAdgroupType contentPromotionAdgroupType) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(activeContentPromotionAdGroup(campaignInfo.getCampaignId(), contentPromotionAdgroupType))
                .withCampaignInfo(campaignInfo));
    }

    @Deprecated // use steps.contentPromotionAdGroupSteps()
    public AdGroupInfo createDraftContentPromotionAdGroup(
            CampaignInfo campaignInfo,
            ContentPromotionAdgroupType contentPromotionAdgroupType) {
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(draftContentPromotionAdGroup(campaignInfo.getCampaignId(), contentPromotionAdgroupType))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createDraftCpmIndoorAdGroup(CampaignInfo campaignInfo) {
        IndoorPlacement placement = placementSteps.addDefaultIndoorPlacementWithOneBlock();
        return createAdGroup(new AdGroupInfo()
                .withAdGroup(draftCpmIndoorAdGroup(null, placement))
                .withCampaignInfo(campaignInfo));
    }

    public AdGroupInfo createAdGroup(AdGroup adGroup) {
        return createAdGroup(new AdGroupInfo().withAdGroup(adGroup));
    }

    /**
     * временное решение, будем думать что делать с этим в DIRECT-108812
     */
    public void createAdGroupRaw(AdGroup adGroup, ClientInfo clientInfo) {
        adGroupRepository.addAdGroups(dslContextProvider.ppc(clientInfo.getShard()).configuration(),
                clientInfo.getClientId(), singletonList(adGroup));
    }

    public AdGroupInfo createAdGroup(AdGroup adGroup, ClientInfo clientInfo) {
        return createAdGroup(adGroup, new CampaignInfo().withClientInfo(clientInfo));
    }

    public AdGroupInfo createAdGroup(AdGroup adGroup, CampaignInfo campaignInfo) {
        return createAdGroup(new AdGroupInfo()
                .withCampaignInfo(campaignInfo)
                .withAdGroup(adGroup));
    }

    public AdGroupInfo createAdGroup(AdGroupInfo adGroupInfo) {
        if (adGroupInfo.getAdGroup() == null) {
            adGroupInfo.withAdGroup(defaultTextAdGroup(null));
        }
        if (adGroupInfo.getCampaignId() == null) {
            resolveCampaignSteps(adGroupInfo.getCampaignInfo());
            adGroupInfo.getAdGroup().withCampaignId(adGroupInfo.getCampaignId());
        }
        if (adGroupInfo.getAdGroupId() == null) {
            adGroupInfo.getAdGroup().withCampaignId(adGroupInfo.getCampaignId());
            saveAdGroup(adGroupInfo.getAdGroup(), adGroupInfo.getClientInfo());
        }
        return adGroupInfo;
    }

    private void resolveCampaignSteps(CampaignInfo campaignInfo) {
        if (campaignInfo instanceof TextCampaignInfo) {
            textCampaignSteps.createCampaign((TextCampaignInfo) campaignInfo);
        } else if (campaignInfo instanceof CpmBannerCampaignInfo) {
            cpmBannerCampaignSteps.createCampaign((CpmBannerCampaignInfo) campaignInfo);
        } else if (campaignInfo instanceof CpmPriceCampaignInfo) {
            cpmPriceCampaignSteps.createCampaign((CpmPriceCampaignInfo) campaignInfo);
        } else if (campaignInfo instanceof CpmYndxFrontpageCampaignInfo) {
            cpmYndxFrontPageCampaignSteps.createCampaign((CpmYndxFrontpageCampaignInfo) campaignInfo);
        } else if (campaignInfo instanceof MobileContentCampaignInfo) {
            mobileContentCampaignSteps.createCampaign((MobileContentCampaignInfo) campaignInfo);
        } else if (campaignInfo instanceof DynamicCampaignInfo) {
            dynamicCampaignSteps.createCampaign((DynamicCampaignInfo) campaignInfo);
        } else if (campaignInfo instanceof SmartCampaignInfo) {
            smartCampaignSteps.createCampaign((SmartCampaignInfo) campaignInfo);
        } else if (campaignInfo instanceof ContentPromotionCampaignInfo) {
            contentPromotionCampaignSteps.createCampaign((ContentPromotionCampaignInfo) campaignInfo);
        } else if (campaignInfo instanceof InternalAutobudgetCampaignInfo) {
            internalAutobudgetCampaignSteps.createCampaign((InternalAutobudgetCampaignInfo) campaignInfo);
        } else if (campaignInfo instanceof InternalDistribCampaignInfo) {
            internalDistribCampaignSteps.createCampaign((InternalDistribCampaignInfo) campaignInfo);
        } else if (campaignInfo instanceof InternalFreeCampaignInfo) {
            internalFreeCampaignSteps.createCampaign((InternalFreeCampaignInfo) campaignInfo);
        } else {
            campaignSteps.createCampaign(campaignInfo);
        }
    }

    public AdGroup saveAdGroup(AdGroup adGroup, ClientInfo clientInfo) {
        adGroupRepository.addAdGroups(dslContextProvider.ppc(clientInfo.getShard()).configuration(),
                clientInfo.getClientId(), singletonList(adGroup));
        return adGroup;
    }

    /**
     * Добавляет несколько групп одним запросом в БД.
     * В качестве клиента и шарда используются клиент и шард, определённые в первой по списку группе.
     */
    public void createAdGroups(List<AdGroupInfo> adGroupInfos) {
        if (adGroupInfos.isEmpty()) {
            return;
        }

        List<AdGroup> adGroups = new ArrayList<>();
        for (AdGroupInfo adGroupInfo : adGroupInfos) {
            if (adGroupInfo.getAdGroup() == null) {
                adGroupInfo.withAdGroup(defaultTextAdGroup(adGroupInfo.getCampaignId()));
            }
            if (adGroupInfo.getAdGroupId() == null) {
                adGroups.add(adGroupInfo.getAdGroup()
                        .withCampaignId(adGroupInfo.getCampaignId()));
            }
        }
        ClientInfo clientInfo = adGroupInfos.get(0).getClientInfo();
        adGroupRepository.addAdGroups(dslContextProvider.ppc(clientInfo.getShard()).configuration(),
                clientInfo.getClientId(), adGroups);
    }

    public <V> void setAdGroupProperty(AdGroupInfo adGroupInfo, ModelProperty<? super AdGroup, V> property, V value) {
        AdGroup adGroup = adGroupInfo.getAdGroup();
        if (!AdGroup.allModelProperties().contains(property)) {
            throw new IllegalArgumentException(
                    "Model " + adGroup.getName() + " doesn't contain property " + property.name());
        }

        AppliedChanges<AdGroup> appliedChanges = new ModelChanges<>(adGroup.getId(), AdGroup.class)
                .process(value, property)
                .applyTo(adGroup);

        adGroupRepository.updateAdGroups(adGroupInfo.getShard(), adGroupInfo.getClientId(),
                singletonList(appliedChanges));
    }

    public <V> void setPerformanceAdGroupProperty(PerformanceAdGroupInfo adGroupInfo,
                                                  ModelProperty<PerformanceAdGroup, V> property, V value) {
        PerformanceAdGroup adGroup = (PerformanceAdGroup) adGroupInfo.getAdGroup();
        if (!PerformanceAdGroup.allModelProperties().contains(property)) {
            throw new IllegalArgumentException(
                    "Model " + adGroup.getName() + " doesn't contain property " + property.name());
        }

        AppliedChanges<AdGroup> appliedChanges = new ModelChanges<>(adGroup.getId(), PerformanceAdGroup.class)
                .process(value, property)
                .applyTo(adGroup)
                .castModelUp(AdGroup.class);

        adGroupRepository.updateAdGroups(adGroupInfo.getShard(), adGroupInfo.getClientId(),
                singletonList(appliedChanges));
    }

    public void setBsRarelyLoaded(int shard, Long adGroupId, boolean bsRarelyLoaded) {
        DSL.using(dslContextProvider.ppc(shard).configuration())
                .update(PHRASES)
                .set(PHRASES.IS_BS_RARELY_LOADED, bsRarelyLoaded ? 1L : 0L)
                .set(PHRASES.LAST_CHANGE, DSL.currentLocalDateTime())
                .where(PHRASES.PID.eq(adGroupId))
                .execute();
    }

    public void setLastChange(int shard, Long adGroupId, LocalDateTime lastChange) {
        dslContextProvider.ppc(shard)
                .update(PHRASES)
                .set(PHRASES.LAST_CHANGE, zeroableDateTimeToDb(lastChange))
                .where(PHRASES.PID.eq(adGroupId))
                .execute();
    }

    public PerformanceAdGroupInfo createDefaultPerformanceAdGroup() {
        return addPerformanceAdGroup(new PerformanceAdGroupInfo());
    }

    public PerformanceAdGroupInfo createDefaultPerformanceAdGroup(ClientInfo clientInfo) {
        return addPerformanceAdGroup(new PerformanceAdGroupInfo().withClientInfo(clientInfo));
    }

    public PerformanceAdGroupInfo createDefaultPerformanceAdGroup(CampaignInfo campaignInfo) {
        return addPerformanceAdGroup(new PerformanceAdGroupInfo()
                .withClientInfo(campaignInfo.getClientInfo())
                .withCampaignInfo(campaignInfo));
    }

    public PerformanceAdGroupInfo createDefaultPerformanceAdGroup(FeedInfo feedInfo) {
        PerformanceAdGroupInfo adGroupInfo = new PerformanceAdGroupInfo()
                .withClientInfo(feedInfo.getClientInfo())
                .withFeedInfo(feedInfo);
        return addPerformanceAdGroup(adGroupInfo);
    }

    public PerformanceAdGroupInfo createPerformanceAdGroupWithSiteFeed(ClientInfo clientInfo) {
        FeedInfo feedInfo = feedSteps.createFeed(clientInfo, FeedType.YANDEX_MARKET, BusinessType.RETAIL, Source.SITE);
        PerformanceAdGroupInfo adGroupInfo = new PerformanceAdGroupInfo()
                .withClientInfo(clientInfo)
                .withFeedInfo(feedInfo);
        return addPerformanceAdGroup(adGroupInfo);
    }

    public PerformanceAdGroupInfo addPerformanceAdGroup(PerformanceAdGroupInfo performanceAdGroupInfo) {
        PerformanceAdGroup performanceAdGroup = performanceAdGroupInfo.getPerformanceAdGroup();
        if (performanceAdGroup == null) {
            ClientInfo clientInfo = performanceAdGroupInfo.getClientInfo();
            if (clientInfo == null || clientInfo.getClient() == null || clientInfo.getClient().getClientId() == null) {
                clientInfo = clientSteps.createDefaultClient();
                performanceAdGroupInfo.withClientInfo(clientInfo);
            }
            CampaignInfo campaignInfo = performanceAdGroupInfo.getCampaignInfo();
            if (campaignInfo == null || campaignInfo.getCampaign() == null) {
                // Если campaignInfo не задан или задан без кампании, создадим дефолтную
                campaignInfo = campaignSteps.createActivePerformanceCampaignWithStrategy(clientInfo);
                performanceAdGroupInfo.withCampaignInfo(campaignInfo);
            } else if (campaignInfo.getCampaignId() == null) {
                // Если campaignInfo есть, но у связанной кампании нет ID, этот campaignInfo надо материализовать
                campaignInfo = campaignSteps.createCampaign(campaignInfo);
                performanceAdGroupInfo.withCampaignInfo(campaignInfo);
            }
            FeedInfo feedInfo = performanceAdGroupInfo.getFeedInfo();
            if (feedInfo == null || performanceAdGroupInfo.getFeedInfo().getFeed() == null) {
                feedInfo = feedSteps.createDefaultFeed(clientInfo);
                performanceAdGroupInfo.withFeedInfo(feedInfo);
            }
            performanceAdGroup = defaultPerformanceAdGroup(campaignInfo.getCampaignId(), feedInfo.getFeedId());
            performanceAdGroupInfo.withAdGroup(performanceAdGroup);
        }
        int shard = performanceAdGroupInfo.getShard();
        ClientId clientId = performanceAdGroupInfo.getClientId();
        Configuration configuration = dslContextProvider.ppc(shard).configuration();
        adGroupRepository.addAdGroups(configuration, clientId, singletonList(performanceAdGroup));
        return performanceAdGroupInfo;
    }

    public PerformanceAdGroupInfo createDraftPerformanceAdGroup(CampaignInfo campaignInfo) {
        var feedInfo = feedSteps.createDefaultFeed(campaignInfo.getClientInfo());
        PerformanceAdGroup adGroup = draftPerformanceAdGroup(campaignInfo.getCampaignId(), feedInfo.getFeedId());
        PerformanceAdGroupInfo adGroupInfo = new PerformanceAdGroupInfo()
                .withClientInfo(campaignInfo.getClientInfo())
                .withFeedInfo(feedInfo)
                .withAdGroup(adGroup)
                .withCampaignInfo(campaignInfo);
        return addPerformanceAdGroup(adGroupInfo);
    }

    public void deleteAdGroups(int shard, ClientId clientId, long operatorUid, List<Long> adGroupIds) {
        adGroupRepository.delete(shard, clientId, operatorUid, adGroupIds);
    }

}
