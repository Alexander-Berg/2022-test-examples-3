package ru.yandex.direct.core.testing.steps;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ru.yandex.direct.core.testing.steps.adgroup.ContentPromotionAdGroupSteps;
import ru.yandex.direct.core.testing.steps.banner.ContentPromotionBannerSteps;
import ru.yandex.direct.core.testing.steps.campaign.CampAdditionalDataSteps;
import ru.yandex.direct.core.testing.steps.campaign.ContentPromotionCampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.CpmBannerCampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.CpmPriceCampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.CpmYndxFrontpageCampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.DynamicCampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.InternalAutobudgetCampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.InternalDistribCampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.InternalFreeCampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.McBannerCampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.MobileContentCampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.SmartCampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.TextCampaignSteps;
import ru.yandex.direct.core.testing.steps.strategy.AutobudgetAvgClickSteps;
import ru.yandex.direct.core.testing.steps.strategy.AutobudgetAvgCpaPerCampSteps;
import ru.yandex.direct.core.testing.steps.strategy.AutobudgetAvgCpaPerFilterSteps;
import ru.yandex.direct.core.testing.steps.strategy.AutobudgetAvgCpaSteps;
import ru.yandex.direct.core.testing.steps.strategy.AutobudgetAvgCpcPerCampSteps;
import ru.yandex.direct.core.testing.steps.strategy.AutobudgetAvgCpcPerFilterSteps;
import ru.yandex.direct.core.testing.steps.strategy.AutobudgetAvgCpiSteps;
import ru.yandex.direct.core.testing.steps.strategy.AutobudgetAvgCpvCustomPeriodSteps;
import ru.yandex.direct.core.testing.steps.strategy.AutobudgetAvgCpvSteps;
import ru.yandex.direct.core.testing.steps.strategy.AutobudgetCrrSteps;
import ru.yandex.direct.core.testing.steps.strategy.AutobudgetMaxImpressionsCustomPeriodSteps;
import ru.yandex.direct.core.testing.steps.strategy.AutobudgetMaxImpressionsSteps;
import ru.yandex.direct.core.testing.steps.strategy.AutobudgetMaxReachCustomPeriodSteps;
import ru.yandex.direct.core.testing.steps.strategy.AutobudgetMaxReachSteps;
import ru.yandex.direct.core.testing.steps.strategy.AutobudgetRoiSteps;
import ru.yandex.direct.core.testing.steps.strategy.AutobudgetWeekBundleSteps;
import ru.yandex.direct.core.testing.steps.strategy.AutobudgetWeekSumSteps;
import ru.yandex.direct.core.testing.steps.strategy.CpmDefaultSteps;
import ru.yandex.direct.core.testing.steps.strategy.DefaultManualStrategySteps;
import ru.yandex.direct.core.testing.steps.strategy.PeriodFixBidSteps;
import ru.yandex.direct.core.testing.steps.uac.UacAccountSteps;
import ru.yandex.direct.dbqueue.steps.DbQueueSteps;

public class Steps {

    @Autowired
    private ApplicationContext context;

    public DomainSteps domainSteps() {
        return getBean(DomainSteps.class);
    }

    public ClientSteps clientSteps() {
        return getBean(ClientSteps.class);
    }

    public AgencySteps agencySteps() {
        return getBean(AgencySteps.class);
    }

    public ClientOptionsSteps clientOptionsSteps() {
        return getBean(ClientOptionsSteps.class);
    }

    public FreelancerSteps freelancerSteps() {
        return getBean(FreelancerSteps.class);
    }

    public FeatureSteps featureSteps() {
        return getBean(FeatureSteps.class);
    }

    public CampaignSteps campaignSteps() {
        return getBean(CampaignSteps.class);
    }

    @Deprecated
    // use typed steps, for example TextCampaignSteps
    public TypedCampaignSteps typedCampaignSteps() {
        return getBean(TypedCampaignSteps.class);
    }

    public TextCampaignSteps textCampaignSteps() {
        return getBean(TextCampaignSteps.class);
    }

    public McBannerCampaignSteps mcBannerCampaignSteps() {
        return getBean(McBannerCampaignSteps.class);
    }

    public ContentPromotionCampaignSteps contentPromotionCampaignSteps() {
        return getBean(ContentPromotionCampaignSteps.class);
    }

    public CpmBannerCampaignSteps cpmBannerCampaignSteps() {
        return getBean(CpmBannerCampaignSteps.class);
    }

    public CpmPriceCampaignSteps cpmPriceCampaignSteps() {
        return getBean(CpmPriceCampaignSteps.class);
    }

    public CpmYndxFrontpageCampaignSteps cpmYndxFrontPageSteps() {
        return getBean(CpmYndxFrontpageCampaignSteps.class);
    }

    public DynamicCampaignSteps dynamicCampaignSteps() {
        return getBean(DynamicCampaignSteps.class);
    }

    public InternalAutobudgetCampaignSteps internalAutobudgetCampaignSteps() {
        return getBean(InternalAutobudgetCampaignSteps.class);
    }

    public InternalFreeCampaignSteps internalFreeCampaignSteps() {
        return getBean(InternalFreeCampaignSteps.class);
    }

    public InternalDistribCampaignSteps internalDistribCampaignSteps() {
        return getBean(InternalDistribCampaignSteps.class);
    }

    public MobileContentCampaignSteps mobileContentCampaignSteps() {
        return getBean(MobileContentCampaignSteps.class);
    }

    public SmartCampaignSteps smartCampaignSteps() {
        return getBean(SmartCampaignSteps.class);
    }

    public AdGroupSteps adGroupSteps() {
        return getBean(AdGroupSteps.class);
    }

    public ContentPromotionAdGroupSteps contentPromotionAdGroupSteps() {
        return getBean(ContentPromotionAdGroupSteps.class);
    }

    public BannerSteps bannerSteps() {
        return getBean(BannerSteps.class);
    }

    public TextBannerSteps textBannerSteps() {
        return getBean(TextBannerSteps.class);
    }

    public ContentPromotionBannerSteps contentPromotionBannerSteps() {
        return getBean(ContentPromotionBannerSteps.class);
    }

    public OldContentPromotionBannerSteps oldContentPromotionBannerSteps() {
        return getBean(OldContentPromotionBannerSteps.class);
    }

    public CpmOutdoorBannerSteps cpmOutdoorBannerSteps() {
        return getBean(CpmOutdoorBannerSteps.class);
    }

    public CpmIndoorBannerSteps cpmIndoorBannerSteps() {
        return getBean(CpmIndoorBannerSteps.class);
    }

    public CpmAudioBannerSteps cpmAudioBannerSteps() {
        return getBean(CpmAudioBannerSteps.class);
    }

    public CpmBannerSteps cpmBannerSteps() {
        return getBean(CpmBannerSteps.class);
    }

    public CpmGeoPinBannerSteps cpmGeoPinBannerSteps() {
        return getBean(CpmGeoPinBannerSteps.class);
    }

    public CpcVideoBannerSteps cpcVideoBannerSteps() {
        return getBean(CpcVideoBannerSteps.class);
    }

    public DynamicBannerSteps dynamicBannerSteps() {
        return getBean(DynamicBannerSteps.class);
    }

    public PerformanceBannerSteps performanceBannerSteps() {
        return getBean(PerformanceBannerSteps.class);
    }

    public PerformanceMainBannerSteps performanceMainBannerSteps() {
        return getBean(PerformanceMainBannerSteps.class);
    }

    public BannerCreativeSteps bannerCreativeSteps() {
        return getBean(BannerCreativeSteps.class);
    }

    public VcardSteps vcardSteps() {
        return getBean(VcardSteps.class);
    }

    public SitelinkSetSteps sitelinkSetSteps() {
        return getBean(SitelinkSetSteps.class);
    }

    public RetConditionSteps retConditionSteps() {
        return getBean(RetConditionSteps.class);
    }

    public RetargetingSteps retargetingSteps() {
        return getBean(RetargetingSteps.class);
    }

    public BidModifierSteps bidModifierSteps() {
        return getBean(BidModifierSteps.class);
    }

    public NewBidModifierSteps newBidModifierSteps() {
        return getBean(NewBidModifierSteps.class);
    }

    public BsFakeSteps bsFakeSteps() {
        return getBean(BsFakeSteps.class);
    }

    public RetargetingGoalsSteps retargetingGoalsSteps() {
        return getBean(RetargetingGoalsSteps.class);
    }

    public PerformanceFiltersSteps performanceFilterSteps() {
        return getBean(PerformanceFiltersSteps.class);
    }

    public DialogSteps dialogSteps() {
        return getBean(DialogSteps.class);
    }

    public DynamicsSteps dynamicConditionsSteps() {
        return getBean(DynamicsSteps.class);
    }

    public DynamicTextAdTargetSteps dynamicTextAdTargetsSteps() {
        return getBean(DynamicTextAdTargetSteps.class);
    }

    public DynamicsSteps dynamicConditionsFakeSteps() {
        return getBean(DynamicsSteps.class);
    }

    public UserSteps userSteps() {
        return getBean(UserSteps.class);
    }

    public KeywordSteps keywordSteps() {
        return getBean(KeywordSteps.class);
    }

    public NewKeywordSteps newKeywordSteps() {
        return getBean(NewKeywordSteps.class);
    }

    public PromoExtensionSteps promoExtensionSteps() {
        return getBean(PromoExtensionSteps.class);
    }

    public RelevanceMatchSteps relevanceMatchSteps() {
        return getBean(RelevanceMatchSteps.class);
    }

    public OfferRetargetingSteps offerRetargetingSteps() {
        return getBean(OfferRetargetingSteps.class);
    }

    public CreativeSteps creativeSteps() {
        return getBean(CreativeSteps.class);
    }

    public CalloutSteps calloutSteps() {
        return getBean(CalloutSteps.class);
    }

    public CurrencySteps currencySteps() {
        return getBean(CurrencySteps.class);
    }

    public TurboLandingSteps turboLandingSteps() {
        return getBean(TurboLandingSteps.class);
    }

    public DealSteps dealSteps() {
        return getBean(DealSteps.class);
    }

    public MobileAppSteps mobileAppSteps() {
        return getBean(MobileAppSteps.class);
    }

    public MobileContentSteps mobileContentSteps() {
        return getBean(MobileContentSteps.class);
    }

    public FeedSteps feedSteps() {
        return getBean(FeedSteps.class);
    }

    public ClientPhoneSteps clientPhoneSteps() {
        return getBean(ClientPhoneSteps.class);
    }

    public DbQueueSteps dbQueueSteps() {
        return getBean(DbQueueSteps.class);
    }

    public TagCampaignSteps tagCampaignSteps() {
        return getBean(TagCampaignSteps.class);
    }

    public TrustedRedirectSteps trustedRedirectSteps() {
        return getBean(TrustedRedirectSteps.class);
    }

    public MinusKeywordsPackSteps minusKeywordsPackSteps() {
        return getBean(MinusKeywordsPackSteps.class);
    }

    public BannerPriceSteps bannerPriceSteps() {
        return getBean(BannerPriceSteps.class);
    }

    public PlacementSteps placementSteps() {
        return getBean(PlacementSteps.class);
    }

    public ModerationReasonSteps moderationReasonSteps() {
        return getBean(ModerationReasonSteps.class);
    }

    public ModerationDiagSteps moderationDiagSteps() {
        return getBean(ModerationDiagSteps.class);
    }

    public ModerateBannerPageSteps moderateBannerPageSteps() {
        return getBean(ModerateBannerPageSteps.class);
    }

    public BannerModerationVersionSteps bannerModerationVersionSteps() {
        return getBean(BannerModerationVersionSteps.class);
    }

    public IdmGroupSteps idmGroupSteps() {
        return getBean(IdmGroupSteps.class);
    }

    public InternalAdProductSteps internalAdProductSteps() {
        return getBean(InternalAdProductSteps.class);
    }

    public InternalAdPlaceSteps internalAdPlaceSteps() {
        return getBean(InternalAdPlaceSteps.class);
    }

    public PricePackageSteps pricePackageSteps() {
        return getBean(PricePackageSteps.class);
    }

    public MdsFileSteps mdsFileSteps() {
        return getBean(MdsFileSteps.class);
    }

    public OrganizationsSteps organizationSteps() {
        return getBean(OrganizationsSteps.class);
    }

    public ContentPromotionSteps contentPromotionSteps() {
        return getBean(ContentPromotionSteps.class);
    }

    public TurboAppSteps turboAppSteps() {
        return getBean(TurboAppSteps.class);
    }

    public CryptaGoalsSteps cryptaGoalsSteps() {
        return getBean(CryptaGoalsSteps.class);
    }

    public ProductSteps productSteps() {
        return getBean(ProductSteps.class);
    }

    public RolesSteps rolesSteps() {
        return getBean(RolesSteps.class);
    }

    public ClientPixelProviderSteps clientPixelProviderSteps() {
        return getBean(ClientPixelProviderSteps.class);
    }

    public HyperGeoSteps hyperGeoSteps() {
        return getBean(HyperGeoSteps.class);
    }

    public AdGroupAdditionalTargetingSteps adGroupAdditionalTargetingSteps() {
        return getBean(AdGroupAdditionalTargetingSteps.class);
    }

    public ImageBannerSteps imageBannerSteps() {
        return getBean(ImageBannerSteps.class);
    }

    public McBannerSteps mcBannerSteps() {
        return getBean(McBannerSteps.class);
    }

    public MobileAppBannerSteps mobileAppBannerSteps() {
        return getBean(MobileAppBannerSteps.class);
    }

    public InternalBannerSteps internalBannerSteps() {
        return getBean(InternalBannerSteps.class);
    }

    public CampaignsPerformanceSteps campaignsPerformanceSteps() {
        return getBean(CampaignsPerformanceSteps.class);
    }

    public CalltrackingPhoneSteps calltrackingPhoneSteps() {
        return getBean(CalltrackingPhoneSteps.class);
    }

    public CalltrackingSettingsSteps calltrackingSettingsSteps() {
        return getBean(CalltrackingSettingsSteps.class);
    }

    public CampCalltrackingSettingsSteps campCalltrackingSettingsSteps() {
        return getBean(CampCalltrackingSettingsSteps.class);
    }

    public CampCalltrackingPhonesSteps campCalltrackingPhonesSteps() {
        return getBean(CampCalltrackingPhonesSteps.class);
    }

    public CampAdditionalDataSteps campAdditionalDataSteps() {
        return getBean(CampAdditionalDataSteps.class);
    }

    public UacAccountSteps uacAccountSteps() {
        return getBean(UacAccountSteps.class);
    }

    public EcomDomainsSteps ecomDomainsSteps() {
        return getBean(EcomDomainsSteps.class);
    }

    public SspPlatformsSteps sspPlatformsSteps() {
        return getBean(SspPlatformsSteps.class);
    }

    public ClientMccSteps clientMccSteps() {
        return getBean(ClientMccSteps.class);
    }

    public ApplicationContext applicationContext() {
        return context;
    }

    public ConversionSourceTypeSteps conversionSourceTypeSteps() {
        return getBean(ConversionSourceTypeSteps.class);
    }

    public AutobudgetAvgClickSteps autobudgetAvgClickSteps() {
        return getBean(AutobudgetAvgClickSteps.class);
    }

    public AutobudgetAvgCpaSteps autobudgetAvgCpaSteps() {
        return getBean(AutobudgetAvgCpaSteps.class);
    }

    public AutobudgetAvgCpaPerCampSteps autobudgetAvgCpaPerCampSteps() {
        return getBean(AutobudgetAvgCpaPerCampSteps.class);
    }

    public AutobudgetAvgCpaPerFilterSteps autobudgetAvgCpaPerFilterSteps() {
        return getBean(AutobudgetAvgCpaPerFilterSteps.class);
    }

    public AutobudgetAvgCpcPerCampSteps autobudgetAvgCpcPerCampSteps() {
        return getBean(AutobudgetAvgCpcPerCampSteps.class);
    }

    public AutobudgetAvgCpcPerFilterSteps autobudgetAvgCpcPerFilterSteps() {
        return getBean(AutobudgetAvgCpcPerFilterSteps.class);
    }

    public AutobudgetAvgCpiSteps autobudgetAvgCpiSteps() {
        return getBean(AutobudgetAvgCpiSteps.class);
    }

    public AutobudgetAvgCpvSteps autobudgetAvgCpvSteps() {
        return getBean(AutobudgetAvgCpvSteps.class);
    }

    public AutobudgetAvgCpvCustomPeriodSteps autobudgetAvgCpvCustomPeriodSteps() {
        return getBean(AutobudgetAvgCpvCustomPeriodSteps.class);
    }

    public AutobudgetCrrSteps autobudgetCrrSteps() {
        return getBean(AutobudgetCrrSteps.class);
    }

    public AutobudgetMaxImpressionsSteps autobudgetMaxImpressionsSteps() {
        return getBean(AutobudgetMaxImpressionsSteps.class);
    }

    public AutobudgetMaxImpressionsCustomPeriodSteps autobudgetMaxImpressionsCustomPeriodSteps() {
        return getBean(AutobudgetMaxImpressionsCustomPeriodSteps.class);
    }

    public AutobudgetMaxReachSteps autobudgetMaxReachSteps() {
        return getBean(AutobudgetMaxReachSteps.class);
    }

    public AutobudgetMaxReachCustomPeriodSteps autobudgetMaxReachCustomPeriodSteps() {
        return getBean(AutobudgetMaxReachCustomPeriodSteps.class);
    }

    public AutobudgetRoiSteps autobudgetRoiSteps() {
        return getBean(AutobudgetRoiSteps.class);
    }

    public AutobudgetWeekBundleSteps autobudgetWeekBundleSteps() {
        return getBean(AutobudgetWeekBundleSteps.class);
    }

    public AutobudgetWeekSumSteps autobudgetWeekSumSteps() {
        return getBean(AutobudgetWeekSumSteps.class);
    }

    public CpmDefaultSteps cpmDefaultSteps() {
        return getBean(CpmDefaultSteps.class);
    }

    public DefaultManualStrategySteps defaultManualStrategySteps() {
        return getBean(DefaultManualStrategySteps.class);
    }

    public PeriodFixBidSteps periodFixBidSteps() {
        return getBean(PeriodFixBidSteps.class);
    }

    /**
     * В Steps нужна ленивая инициализация, чтобы в тестах не тратилось лишнее время на инициализацию не нужных бинов.
     * <p>
     * Аннотация @Lazy не работает с kotlin классами, поэтому используем ApplicationContext.
     */
    private <T> T getBean(Class<T> tClass) {
        return context.getBean(tClass);
    }

    public CampaignsMobileContentSteps campaignsMobileContentSteps() {
        return getBean(CampaignsMobileContentSteps.class);
    }

    public MetrikaServiceSteps metrikaServiceSteps() {
        return getBean(MetrikaServiceSteps.class);
    }
}
