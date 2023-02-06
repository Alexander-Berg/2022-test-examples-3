package ru.yandex.direct.core.entity.banner.type.creative;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.model.BannerWithCreative;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.CpcVideoBanner;
import ru.yandex.direct.core.entity.banner.model.CpmAudioBanner;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.CpmGeoPinBanner;
import ru.yandex.direct.core.entity.banner.model.CpmOutdoorBanner;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.creative.constants.AdminRejectionReason;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.CreativeBusinessType;
import ru.yandex.direct.core.entity.creative.model.StatusModerate;
import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.cpcVideoNotFound;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.creativeNotFound;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentCreativeBusinessTypeToFeedBusinessType;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentCreativeFormat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentCreativeGeoToAdGroupGeo;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentCreativeTypeToBannerType;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.requiredCreativesWithCanvasOrHtml5Types;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.requiredCreativesWithVideoAdditionTypeOnly;
import static ru.yandex.direct.core.entity.creative.service.add.validation.CreativeDefects.creativeIsAdminRejected;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpcVideoForCpcVideoBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmAudioAddition;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmOutdoorVideoAddition;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmAudioAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMobileAppAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activePerformanceAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class BannerWithCreativeValidatorProviderCreativeValidatorTest {

    private static final Integer ZERO_INDEX = 0;
    private static final long DEFAULT_CREATIVE_ID = 5L;
    private static final long DEFAULT_CAMPAIGN_ID = 6L;
    private static final long DEFAULT_AD_GROUP_ID = 7L;

    private BannerWithCreativeValidatorProvider serviceUnderTest = new BannerWithCreativeValidatorProvider();


    @Parameterized.Parameter(0)
    public String testName;

    @Parameterized.Parameter(1)
    public AdGroup adGroup;

    @Parameterized.Parameter(2)
    public BannerWithCreative banner;

    @Parameterized.Parameter(3)
    public Creative creative;

    @Parameterized.Parameter(4)
    public boolean isCreativeIdRandom;

    @Parameterized.Parameter(5)
    public Defect defect;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "defaultCpmAudioAddition for NewCpmAudioBanner. Success",
                        activeCpmAudioAdGroup(null),
                        new CpmAudioBanner(),
                        defaultCpmAudioAddition(null, null),
                        false,
                        null
                },
                {
                        "defaultCpmOutdoorVideoAddition for NewCpmOutdoorBanner. Success",
                        activeCpmAudioAdGroup(null),
                        new CpmOutdoorBanner(),
                        defaultCpmOutdoorVideoAddition(null, null),
                        false,
                        null
                },
                {
                        "defaultCpcVideoForCpcVideoBanner for NewCpcVideoBanner. Success",
                        activeCpmAudioAdGroup(null),
                        new CpcVideoBanner(),
                        defaultCpcVideoForCpcVideoBanner(null, null),
                        false,
                        null
                },
                {
                        "defaultPerformanceCreative for NewPerformanceBanner. Success",
                        activePerformanceAdGroup(null),
                        new PerformanceBanner(),
                        defaultPerformanceCreative(null, null)
                                .withBusinessType(CreativeBusinessType.RETAIL),
                        false,
                        null
                },
                {
                        "defaultCanvas for NewCpmBanner. Success",
                        activeCpmBannerAdGroup(null),
                        new CpmBanner(),
                        defaultCanvas(null, null),
                        false,
                        null
                },
                {
                        "defaultCanvas for NewCpmGeoPinBanner. Success",
                        activeCpmBannerAdGroup(null),
                        new CpmGeoPinBanner(),
                        defaultCanvas(null, null),
                        false,
                        null
                },
                {
                        "requiredCreativeNewBannerValidator notNull",
                        activeCpmAudioAdGroup(null),
                        new CpmAudioBanner(),
                        null,
                        false,
                        notNull()
                },
                {
                        "requiredCreativeNewBannerValidator isCreativeTypeCorrespondTo",
                        activeCpmAudioAdGroup(null),
                        new CpmAudioBanner(),
                        defaultCanvas(null, null),
                        false,
                        inconsistentCreativeTypeToBannerType()
                },
                {
                        "requiredCreativeNewBannerValidator isClientHasCreative",
                        activeCpmAudioAdGroup(null),
                        new CpmAudioBanner(),
                        null,
                        true,
                        creativeNotFound()
                },
                {
                        "requiredCreativeNewBannerValidator override defect for isClientHasCreative",
                        activeTextAdGroup(null),
                        new CpcVideoBanner(),
                        null,
                        true,
                        cpcVideoNotFound()
                },
                {
                        "requiredCreativeNewBannerValidator override defect for isCreativeTypeCorrespondTo",
                        activeTextAdGroup(null),
                        new ImageBanner(),
                        defaultCpmAudioAddition(null, null),
                        false,
                        creativeNotFound()
                },
                {
                        "optionalCreativeNewBannerValidator. Text banner without creative",
                        activeTextAdGroup(null),
                        new TextBanner(),
                        null,
                        false,
                        null
                },
                {
                        "optionalCreativeNewBannerValidator override defect for isCreativeTypeCorrespondTo",
                        activeTextAdGroup(null),
                        new TextBanner(),
                        defaultCpmAudioAddition(null, null),
                        false,
                        requiredCreativesWithVideoAdditionTypeOnly()
                },
                {
                        "optionalCreativeNewBannerValidator. Mobile content banner without creative",
                        activeMobileAppAdGroup(null),
                        new MobileAppBanner(),
                        null,
                        false,
                        null
                },
                {
                        "optionalCreativeNewBannerValidator. Mobile content banner with CpmAudioAddition " +
                                "-> REQUIRED_VIDEO_ADDITION_TYPE_ONLY",
                        activeMobileAppAdGroup(null),
                        new MobileAppBanner(),
                        defaultCpmAudioAddition(null, null),
                        false,
                        requiredCreativesWithVideoAdditionTypeOnly()
                },


                {
                        "requiredCreativeNewBannerValidator isClientHasCreative",
                        activePerformanceAdGroup(null),
                        new PerformanceBanner(),
                        null,
                        true,
                        creativeNotFound()
                },
                {
                        "NewPerformanceBannerCreativeValidator duplicatedElement",
                        activePerformanceAdGroup(null),
                        new PerformanceBanner(),
                        defaultPerformanceCreative(null, null)
                                .withSumGeo(asList(Region.TURKEY_REGION_ID, -Region.ISTANBUL_REGION_ID)),
                        false,
                        inconsistentCreativeGeoToAdGroupGeo()
                },
                {
                        "NewPerformanceBannerCreativeValidator isCreativeBusinessTypeCorrespondTo",
                        activePerformanceAdGroup(null),
                        new PerformanceBanner(),
                        defaultPerformanceCreative(null, null)
                                .withBusinessType(CreativeBusinessType.FLIGHTS),
                        false,
                        inconsistentCreativeBusinessTypeToFeedBusinessType()
                },
                {
                        "NewCpmBanner requiredCreativeNewBannerValidator override defect for isClientHasCreative",
                        activeCpmBannerAdGroup(null),
                        new CpmBanner(),
                        null,
                        true,
                        creativeNotFound()
                },
                {
                        "NewCpmBannerCreativeValidator creativeIsNotAdminRejected",
                        activeCpmBannerAdGroup(null),
                        new CpmBanner(),
                        defaultCanvas(null, null)
                                .withStatusModerate(StatusModerate.ADMINREJECT),
                        false,
                        creativeIsAdminRejected(AdminRejectionReason.DEFAULT)
                },
                {
                        "NewCpmBannerCreativeValidator creativeTypeIsCanvasOrHtml5",
                        activeCpmBannerAdGroup(null),
                        new CpmBanner(),
                        defaultCpmAudioAddition(null, null),
                        false,
                        requiredCreativesWithCanvasOrHtml5Types()
                },
                {
                        "NewCpmBanner CreativeFormatValidator inconsistentCreativeFormat",
                        activeCpmBannerAdGroup(null),
                        new CpmBanner(),
                        defaultCanvas(null, null)
                                .withWidth(1L).withHeight(1L),
                        false,
                        inconsistentCreativeFormat()
                },
                {
                        "NewCpmGeoPinBanner requiredCreativeNewBannerValidator override defect for isClientHasCreative",
                        activeCpmBannerAdGroup(null),
                        new CpmGeoPinBanner(),
                        null,
                        true,
                        creativeNotFound()
                },
                {
                        "NewCpmGeoPinBanner NewCpmGeoPinBannerCreativeValidator",
                        activeCpmBannerAdGroup(null),
                        new CpmGeoPinBanner(),
                        defaultCanvas(null, null)
                                .withStatusModerate(StatusModerate.ADMINREJECT),
                        false,
                        creativeIsAdminRejected(AdminRejectionReason.DEFAULT)
                },

        });
    }

    @Test
    public void validate() {

        Long creativeId = prepareCreativeId(creative, isCreativeIdRandom);

        AdGroupInfo adGroupInfo = createAdGroupInfo(adGroup);

        fillBanner(banner, creativeId, adGroupInfo);
        var container = initContainer(adGroupInfo, creative);

        validateAndCheckDefect(banner, container, defect);
    }

    private void validateAndCheckDefect(BannerWithCreative banner,
                                        BannerWithCreativeValidationContainer container,
                                        Defect defect) {

        ValidationResult<BannerWithCreative, Defect> validationResult =
                serviceUnderTest.creativeValidator(container).apply(banner);

        if (defect != null) {
            assertThat(validationResult,
                    hasDefectDefinitionWith(validationError(
                            path(field(BannerWithCreative.CREATIVE_ID)), defect)));
        } else {
            assertThat(validationResult, hasNoDefectsDefinitions());
        }
    }

    private AdGroupInfo createAdGroupInfo(AdGroup adGroup) {

        adGroup.withId(DEFAULT_AD_GROUP_ID);

        CampaignInfo campaignInfo = new CampaignInfo()
                .withCampaign(newTextCampaign(null, null)
                        .withId(DEFAULT_CAMPAIGN_ID));

        ClientInfo clientInfo = new ClientInfo().withClient(defaultClient());

        return new AdGroupInfo()
                .withAdGroup(adGroup)
                .withCampaignInfo(campaignInfo)
                .withClientInfo(clientInfo);
    }

    private Long prepareCreativeId(Creative creative, boolean isCreativeIdRandom) {
        if (isCreativeIdRandom) {
            return RandomNumberUtils.nextPositiveLong();
        } else if (creative != null) {
            creative.setId(DEFAULT_CREATIVE_ID);
            return creative.getId();
        }
        return null;
    }

    private BannerWithCreativeValidationContainer initContainer(AdGroupInfo adGroupInfo, Creative creative) {
        Campaign campaign = adGroupInfo.getCampaignInfo().getCampaign();

        Map<Long, Creative> creativesByIds = emptyMap();
        if (creative != null) {
            creativesByIds = Map.of(creative.getId(), creative);
        }

        Map<Long, Set<Long>> performanceAdGroupCountriesByAdGroupId =
                Map.of(adGroupInfo.getAdGroupId(), Set.of(Region.RUSSIA_REGION_ID));
        Map<Long, BusinessType> performanceFeedBusinessTypeByAdGroupId =
                Map.of(adGroupInfo.getAdGroupId(), BusinessType.RETAIL);
        Map<Long, Set<Long>> performanceDuplicatedCreativeIdsByAdGroupId =
                Map.of(adGroupInfo.getAdGroupId(), emptySet());

        BannerWithCreativeValidationContainer container = mock(BannerWithCreativeValidationContainer.class);
        when(container.getPerformanceAdGroupCountriesByAdGroupId())
                .thenReturn(performanceAdGroupCountriesByAdGroupId);
        when(container.getPerformanceFeedBusinessTypeByAdGroupId())
                .thenReturn(performanceFeedBusinessTypeByAdGroupId);
        when(container.getPerformanceDuplicatedCreativeIdsByAdGroupId())
                .thenReturn(performanceDuplicatedCreativeIdsByAdGroupId);
        when(container.getCreativesByIds())
                .thenReturn(creativesByIds);
        when(container.getCampaignType(any()))
                .thenReturn(campaign.getType());
        when(container.getCampaignId(any()))
                .thenReturn(campaign.getId());
        when(container.getAdGroupType(any()))
                .thenReturn(adGroup.getType());
        when(container.getAdGroupId(any()))
                .thenReturn(adGroup.getId());
        return container;
    }

    private void fillBanner(BannerWithCreative banner, Long creativeId, AdGroupInfo adGroupInfo) {
        banner.setCreativeId(creativeId);

        ((BannerWithSystemFields) banner)
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId());
    }
}
