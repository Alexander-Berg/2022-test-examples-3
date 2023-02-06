package ru.yandex.direct.core.entity.banner.type.href;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupForBannerOperation;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainer;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppTracker;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppTrackerTrackingSystem;
import ru.yandex.direct.core.entity.trustedredirects.service.TrustedRedirectsService;
import ru.yandex.direct.core.entity.uac.service.trackingurl.TrackingUrlParseService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.core.entity.campaign.model.StrategyName.AUTOBUDGET_AVG_CPI;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CPI_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestBannerValidationContainers.newBannerValidationContainer;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithHrefValidatorMobileTrackingSystemTest {

    private static final Path PATH = path(index(0), field(BannerWithHref.HREF));
    private static final String INVALID_TRACKER_HREF = "https://apps.apple.com/ru/id123";
    private static final String VALID_TRACKER_HREF = "https://app.appsflyer.com/ru.test?clickid={logid}";
    private static final Long CAMPAIGN_ID = 12345L;
    private static final Long MOBILE_APP_ID = 54321L;

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public MobileContentCampaign campaignWithMobileContent;

    @Parameterized.Parameter(2)
    public MobileAppTracker mobileAppTracker;

    @Parameterized.Parameter(3)
    public Set<FeatureName> features;

    @Parameterized.Parameter(4)
    public BannerWithHref banner;

    @Parameterized.Parameter(5)
    public Defect expectedDefect;

    @Mock
    private TrustedRedirectsService trustedRedirectsService;

    @Autowired
    private TrackingUrlParseService trackingUrlParseService;

    private BannerWithHrefValidatorProvider provider;
    private BannersAddOperationContainer validationContainer;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // MobileAppBanner - один тип баннера тестируем подробно
                {
                        "MobileAppBanner: не uac-кампания, с трекером, с фичой, href не трекинговый",
                        mobileAppCampaign(false),
                        mobileAppTracker(true),
                        featureIsOn(),
                        new MobileAppBanner().withHref(INVALID_TRACKER_HREF),
                        BannerDefects.trackingSystemDomainNotSupported()
                },
                {
                        "MobileAppBanner: не uac-кампания, с трекером, без фичи, href не трекинговый",
                        mobileAppCampaign(false),
                        mobileAppTracker(true),
                        featureIsOff(),
                        new MobileAppBanner().withHref(INVALID_TRACKER_HREF),
                        null
                },
                {
                        "MobileAppBanner: не uac-кампания, с трекером, c фичой, href - трекинговый",
                        mobileAppCampaign(false),
                        mobileAppTracker(true),
                        featureIsOn(),
                        new MobileAppBanner().withHref(VALID_TRACKER_HREF),
                        null
                },
                {
                        "MobileAppBanner: не uac-кампания, без трекера, c фичой, href - не трекинговый",
                        mobileAppCampaign(false),
                        mobileAppTracker(false),
                        featureIsOn(),
                        new MobileAppBanner().withHref(INVALID_TRACKER_HREF),
                        null
                },
                {
                        "MobileAppBanner:  uac-кампания, с трекером, c фичой, href - не трекинговый",
                        mobileAppCampaign(true),
                        mobileAppTracker(true),
                        featureIsOn(),
                        new MobileAppBanner().withHref(INVALID_TRACKER_HREF),
                        null
                },
                // ImageBanner - один тип баннера тестируем подробно
                {
                        "MobileAppBanner: не uac-кампания, с трекером, с фичой, href не трекинговый",
                        mobileAppCampaign(false),
                        mobileAppTracker(true),
                        featureIsOn(),
                        new ImageBanner().withHref(INVALID_TRACKER_HREF),
                        BannerDefects.trackingSystemDomainNotSupported()
                },
                {
                        "MobileAppBanner: не uac-кампания, с трекером, c фичой, href - трекинговый",
                        mobileAppCampaign(false),
                        mobileAppTracker(true),
                        featureIsOn(),
                        new MobileAppBanner().withHref(VALID_TRACKER_HREF),
                        null
                },
                {
                        "MobileAppBanner: не uac-кампания, без трекера, c фичой, href - не трекинговый",
                        mobileAppCampaign(false),
                        mobileAppTracker(false),
                        featureIsOn(),
                        new MobileAppBanner().withHref(INVALID_TRACKER_HREF),
                        null
                },
                {
                        "MobileAppBanner:  uac-кампания, с трекером, c фичой, href - не трекинговый",
                        mobileAppCampaign(true),
                        mobileAppTracker(true),
                        featureIsOn(),
                        new MobileAppBanner().withHref(INVALID_TRACKER_HREF),
                        null
                },
        });
    }

    private static MobileContentCampaign mobileAppCampaign(boolean uac) {
        StrategyData strategyData = new StrategyData().withName(AUTOBUDGET_AVG_CPI.toString()).withGoalId(DEFAULT_CPI_GOAL_ID);
        DbStrategy strategy = new DbStrategy();
        strategy.withStrategyData(strategyData);
        return new MobileContentCampaign()
                .withStrategy(strategy)
                .withId(CAMPAIGN_ID)
                .withMobileAppId(MOBILE_APP_ID)
                .withSource(uac ? CampaignSource.UAC : CampaignSource.DIRECT);
    }

    private static MobileAppTracker mobileAppTracker(boolean withTrackingSystem) {
        return new MobileAppTracker()
                .withMobileAppId(MOBILE_APP_ID)
                .withUrl(withTrackingSystem ? VALID_TRACKER_HREF : INVALID_TRACKER_HREF)
                .withTrackingSystem(withTrackingSystem ? MobileAppTrackerTrackingSystem.APPSFLYER : MobileAppTrackerTrackingSystem.OTHER);
    }

    private static Set<FeatureName> featureIsOff() {
        return Set.of();
    }

    private static Set<FeatureName> featureIsOn() {
        return Set.of(FeatureName.RMP_CPI_UNDER_KNOWN_TRACKING_SYSTEM_ONLY);
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        initMocks(this);
        when(trustedRedirectsService.checkTrackingHref(any(String.class)))
                .thenReturn(TrustedRedirectsService.Result.TRUSTED);

        provider = new BannerWithHrefValidatorProvider(trustedRedirectsService, trackingUrlParseService);

        AdGroupForBannerOperation adGroupForBannerOperation =
                new MobileContentAdGroup()
                        .withCampaignId(CAMPAIGN_ID)
                        .withType(AdGroupType.MOBILE_CONTENT);

        validationContainer = newBannerValidationContainer()
                .withClientEnabledFeatures(listToSet(features, FeatureName::getName))
                .withIndexToAdGroupForOperationMap(Map.of(0, adGroupForBannerOperation))
                .withCampaignIdToCampaignWithMobileContentMap(Map.of(CAMPAIGN_ID, campaignWithMobileContent))
                .withIndexToCampaignMap(Map.of(0, campaignWithMobileContent))
                .withMobileAppIdToTrackersMap(Map.of(MOBILE_APP_ID, List.of(mobileAppTracker)))
                .withBannerToIndexMap(Map.of(banner, 0))
                .build();
    }

    @Test
    public void testValidationProvider() {
        ValidationResult<List<BannerWithHref>, Defect> vr = validate(banner);
        if (expectedDefect != null) {
            assertThat(vr, hasDefectDefinitionWith(validationError(PATH, expectedDefect)));
        } else {
            assertThat(vr, hasNoDefectsDefinitions());
        }
    }

    private ValidationResult<List<BannerWithHref>, Defect> validate(BannerWithHref banner) {
        return ListValidationBuilder.<BannerWithHref, Defect>of(singletonList(banner))
                .checkEachBy(provider.bannerWithHrefValidator(validationContainer))
                .getResult();
    }
}
