package ru.yandex.direct.teststeps.service;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@ParametersAreNonnullByDefault
public class TestStepsService {
    private final CampaignStepsService campaignStepsService;
    private final AdGroupStepsService adGroupStepsService;
    private final AdStepsService adStepsService;
    private final FeedStepsService feedStepsService;
    private final PricePackageStepsService pricePackageStepsService;
    private final RetargetingStepsService retargetingStepsService;
    private final RetargetingConditionStepsService retargetingConditionStepsService;
    private final KeywordStepsService keywordStepsService;
    private final MinusPhrasesPackService minusPhrasesPackService;
    private final CalloutStepsService calloutStepsService;
    private final TurboLandingStepsService turboLandingStepsService;
    private final SiteLinkStepsService siteLinkStepsService;
    private final PerformanceFiltersStepsService performanceFiltersStepsService;
    private final FeaturesStepsService featuresStepsService;

    private final EcomStepsService ecomStepsService;

    @Autowired

    public TestStepsService(CampaignStepsService campaignStepsService, AdGroupStepsService adGroupStepsService,
                            AdStepsService adStepsService, FeedStepsService feedStepsService,
                            PricePackageStepsService pricePackageStepsService,
                            RetargetingStepsService retargetingStepsService,
                            RetargetingConditionStepsService retargetingConditionStepsService,
                            KeywordStepsService keywordStepsService,
                            MinusPhrasesPackService minusPhrasesPackService,
                            CalloutStepsService calloutStepsService,
                            TurboLandingStepsService turboLandingStepsService,
                            PerformanceFiltersStepsService performanceFiltersStepsService,
                            SiteLinkStepsService siteLinkStepsService, FeaturesStepsService featuresStepsService,
                            EcomStepsService ecomStepsService) {
        this.campaignStepsService = campaignStepsService;
        this.adGroupStepsService = adGroupStepsService;
        this.adStepsService = adStepsService;
        this.feedStepsService = feedStepsService;
        this.pricePackageStepsService = pricePackageStepsService;
        this.retargetingStepsService = retargetingStepsService;
        this.retargetingConditionStepsService = retargetingConditionStepsService;
        this.keywordStepsService = keywordStepsService;
        this.minusPhrasesPackService = minusPhrasesPackService;
        this.calloutStepsService = calloutStepsService;
        this.turboLandingStepsService = turboLandingStepsService;
        this.performanceFiltersStepsService = performanceFiltersStepsService;
        this.siteLinkStepsService = siteLinkStepsService;
        this.featuresStepsService = featuresStepsService;
        this.ecomStepsService = ecomStepsService;
    }

    public CampaignStepsService campaignSteps() {
        return campaignStepsService;
    }

    public AdGroupStepsService adGroupSteps() {
        return adGroupStepsService;
    }

    public AdStepsService adSteps() {
        return adStepsService;
    }

    public FeedStepsService feedSteps() {
        return feedStepsService;
    }

    public PricePackageStepsService pricePackageSteps() {
        return pricePackageStepsService;
    }

    public RetargetingStepsService retargetingSteps() {
        return retargetingStepsService;
    }

    public RetargetingConditionStepsService retargetingConditionSteps() {
        return retargetingConditionStepsService;
    }

    public KeywordStepsService keywordSteps() {
        return keywordStepsService;
    }

    public MinusPhrasesPackService minusPhrasesPackService() {
        return minusPhrasesPackService;
    }

    public CalloutStepsService calloutSteps() {
        return calloutStepsService;
    }

    public TurboLandingStepsService turboLandingStepsService() {
        return turboLandingStepsService;
    }

    public PerformanceFiltersStepsService performanceFiltersSteps() {
        return performanceFiltersStepsService;
    }

    public SiteLinkStepsService siteLinkSteps() {
        return siteLinkStepsService;
    }

    public FeaturesStepsService featuresStepsService() {
        return featuresStepsService;
    }

    public EcomStepsService ecomStepsService() {
        return ecomStepsService;
    }
}
