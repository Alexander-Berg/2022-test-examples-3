package ru.yandex.direct.web.entity.adgroup.converter;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.container.ComplexCpmAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmAudioAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmGeoproductAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmIndoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmOutdoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.adgroup.model.PageBlock;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.CpmIndoorBanner;
import ru.yandex.direct.core.entity.banner.model.CpmOutdoorBanner;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargeting;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeather;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeatherAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeatherLiteral;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.entity.bidmodifier.OperationType;
import ru.yandex.direct.core.entity.bidmodifier.OsType;
import ru.yandex.direct.core.entity.bidmodifier.WeatherType;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.dbschema.ppc.enums.PhrasesAdgroupType;
import ru.yandex.direct.web.entity.adgroup.model.PixelKind;
import ru.yandex.direct.web.entity.adgroup.model.WebAdGroupBidModifiers;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroup;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroupRetargeting;
import ru.yandex.direct.web.entity.adgroup.model.WebPageBlock;
import ru.yandex.direct.web.entity.banner.model.WebBannerCreative;
import ru.yandex.direct.web.entity.banner.model.WebBannerTurbolanding;
import ru.yandex.direct.web.entity.banner.model.WebCpmBanner;
import ru.yandex.direct.web.entity.banner.model.WebPixel;
import ru.yandex.direct.web.entity.bidmodifier.model.WebAdjustment;
import ru.yandex.direct.web.entity.bidmodifier.model.WebDemographicsAdjustment;
import ru.yandex.direct.web.entity.bidmodifier.model.WebDemographicsBidModifier;
import ru.yandex.direct.web.entity.bidmodifier.model.WebDesktopBidModifier;
import ru.yandex.direct.web.entity.bidmodifier.model.WebMobileBidModifier;
import ru.yandex.direct.web.entity.bidmodifier.model.WebRetargetingBidModifier;
import ru.yandex.direct.web.entity.bidmodifier.model.WebWeatherAdjustment;
import ru.yandex.direct.web.entity.bidmodifier.model.WebWeatherBidModifier;
import ru.yandex.direct.web.entity.bidmodifier.model.WebWeatherExpression;
import ru.yandex.direct.web.entity.keyword.model.WebKeyword;
import ru.yandex.direct.web.testing.data.TestBidModifiers;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.bidmodifier.AgeType._18_24;
import static ru.yandex.direct.core.entity.bidmodifier.GenderType.MALE;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl;
import static ru.yandex.direct.web.entity.adgroup.converter.CpmAdGroupConverter.webAdGroupToCoreComplexCpmAdGroup;

public class CpmAdGroupConverterTest {

    @Test
    public void convertEmptyCpmBannerAdGroup() {
        long adGroupId = 1L;
        String adGroupName = "123";
        WebCpmAdGroup webCpmAdGroup = getEmptyAdGroup(PhrasesAdgroupType.cpm_banner)
                .withId(adGroupId)
                .withName(adGroupName);
        ComplexCpmAdGroup complexCpmAdGroup = webAdGroupToCoreComplexCpmAdGroup(webCpmAdGroup, CampaignType.CPM_BANNER);

        ComplexCpmAdGroup expected = new ComplexCpmAdGroup()
                .withAdGroup(new CpmBannerAdGroup()
                        .withId(adGroupId)
                        .withName(adGroupName)
                        .withType(AdGroupType.CPM_BANNER)
                        .withCriterionType(CriterionType.KEYWORD)
                        .withMinusKeywords(emptyList())
                );

        assertThat(complexCpmAdGroup, beanDiffer(expected));
    }

    @Test
    public void convertCpmBannerAdGroupWithBanner() {
        long adGroupId = 1L;
        long bannerId = 3L;
        long creativeId = 7L;
        long turboId = 12L;
        String adGroupName = "123";
        String bannerTurbolandingHrefParams = "param1=value1&param2=value2";

        WebCpmBanner banner = new WebCpmBanner()
                .withId(bannerId)
                .withAdType(BannersBannerType.cpm_banner.getLiteral())
                .withPixels(singletonList(new WebPixel().withKind(PixelKind.AUDIENCE).withUrl(yaAudiencePixelUrl())))
                .withUrlProtocol("https")
                .withHref("bannerHref")
                .withCreative(new WebBannerCreative().withCreativeId(Long.toString(creativeId)))
                .withTurbolanding(new WebBannerTurbolanding().withId(turboId))
                .withTurbolandingHrefParams(bannerTurbolandingHrefParams);

        WebCpmAdGroup webCpmAdGroup = getEmptyAdGroup(PhrasesAdgroupType.cpm_banner)
                .withId(adGroupId)
                .withName(adGroupName)
                .withBanners(singletonList(banner));

        ComplexCpmAdGroup complexCpmAdGroup =
                webAdGroupToCoreComplexCpmAdGroup(webCpmAdGroup, CampaignType.CPM_BANNER);

        var expectedBanner = new CpmBanner()
                .withId(bannerId)
                .withPixels(singletonList(yaAudiencePixelUrl()))
                .withHref("https" + "bannerHref")
                .withCreativeId(creativeId)
                .withTurboLandingId(turboId)
                .withTurboLandingHrefParams(bannerTurbolandingHrefParams)
                .withMeasurers(emptyList());

        ComplexCpmAdGroup expected = new ComplexCpmAdGroup()
                .withAdGroup(new CpmBannerAdGroup()
                        .withId(adGroupId)
                        .withName(adGroupName)
                        .withType(AdGroupType.CPM_BANNER)
                        .withCriterionType(CriterionType.KEYWORD)
                        .withMinusKeywords(emptyList())
                )
                .withBanners(singletonList(expectedBanner));

        assertThat(complexCpmAdGroup, beanDiffer(expected));
    }

    @Test
    public void convertEmptyCpmAudioAdGroup() {
        WebCpmAdGroup webCpmAdGroup = getEmptyAdGroup(PhrasesAdgroupType.cpm_audio);
        ComplexCpmAdGroup complexCpmAdGroup = webAdGroupToCoreComplexCpmAdGroup(webCpmAdGroup, CampaignType.CPM_BANNER);

        ComplexCpmAdGroup expected = new ComplexCpmAdGroup()
                .withAdGroup(new CpmAudioAdGroup()
                        .withType(AdGroupType.CPM_AUDIO)
                        .withMinusKeywords(emptyList()));

        assertThat(complexCpmAdGroup, beanDiffer(expected));
    }

    @Test
    public void convertEmptyCpmVideoAdGroup() {
        WebCpmAdGroup webCpmAdGroup = getEmptyAdGroup(PhrasesAdgroupType.cpm_video);
        ComplexCpmAdGroup complexCpmAdGroup = webAdGroupToCoreComplexCpmAdGroup(webCpmAdGroup, CampaignType.CPM_BANNER);

        ComplexCpmAdGroup expected = new ComplexCpmAdGroup()
                .withAdGroup(new CpmVideoAdGroup()
                        .withType(AdGroupType.CPM_VIDEO)
                        .withMinusKeywords(emptyList())
                        .withCriterionType(CriterionType.USER_PROFILE));

        assertThat(complexCpmAdGroup, beanDiffer(expected));
    }

    @Test
    public void convertCpmVideoAdGroupWithBanner() {
        long adGroupId = 1L;
        long bannerId = 3L;
        long creativeId = 7L;
        long turboId = 12L;
        String adGroupName = "123";
        String bannerTurbolandingHrefParams = "param1=value1&param2=value2";

        WebCpmBanner banner = new WebCpmBanner()
                .withId(bannerId)
                .withAdType(BannersBannerType.cpm_banner.getLiteral())
                .withPixels(singletonList(new WebPixel().withKind(PixelKind.AUDIENCE).withUrl(yaAudiencePixelUrl())))
                .withUrlProtocol("https")
                .withHref("bannerHref")
                .withCreative(new WebBannerCreative().withCreativeId(Long.toString(creativeId)))
                .withTurbolanding(new WebBannerTurbolanding().withId(turboId))
                .withTurbolandingHrefParams(bannerTurbolandingHrefParams);

        WebCpmAdGroup webCpmAdGroup = new WebCpmAdGroup()
                .withId(adGroupId)
                .withCpmBannersType(PhrasesAdgroupType.cpm_video.getLiteral())
                .withName(adGroupName)
                .withBanners(singletonList(banner));

        ComplexCpmAdGroup complexCpmAdGroup = webAdGroupToCoreComplexCpmAdGroup(webCpmAdGroup, CampaignType.CPM_BANNER);

        var expectedBanner = new CpmBanner()
                .withId(bannerId)
                .withPixels(singletonList(yaAudiencePixelUrl()))
                .withHref("https" + "bannerHref")
                .withCreativeId(creativeId)
                .withTurboLandingId(turboId)
                .withTurboLandingHrefParams(bannerTurbolandingHrefParams)
                .withMeasurers(emptyList());

        ComplexCpmAdGroup expected = new ComplexCpmAdGroup()
                .withAdGroup(new CpmVideoAdGroup()
                        .withId(adGroupId)
                        .withName(adGroupName)
                        .withType(AdGroupType.CPM_VIDEO)
                        .withMinusKeywords(emptyList())
                        .withCriterionType(CriterionType.USER_PROFILE)
                )
                .withBanners(singletonList(expectedBanner));

        assertThat(complexCpmAdGroup, beanDiffer(expected));
    }

    @Test
    public void convertCpmOutdoorAdGroupWithBanner() {
        long adGroupId = 1L;
        long bannerId = 3L;
        long creativeId = 7L;
        String adGroupName = "123";

        WebCpmBanner banner = new WebCpmBanner()
                .withId(bannerId)
                .withAdType(BannersBannerType.cpm_banner.getLiteral())
                .withUrlProtocol("https")
                .withHref("bannerHref")
                .withCreative(new WebBannerCreative().withCreativeId(Long.toString(creativeId)));

        WebCpmAdGroup webCpmAdGroup = new WebCpmAdGroup()
                .withId(adGroupId)
                .withCpmBannersType(PhrasesAdgroupType.cpm_outdoor.getLiteral())
                .withName(adGroupName)
                .withPageBlocks(singletonList(new WebPageBlock()
                        .withPageId(2L)
                        .withImpId(3L)))
                .withBanners(singletonList(banner));

        ComplexCpmAdGroup complexCpmAdGroup = webAdGroupToCoreComplexCpmAdGroup(webCpmAdGroup, CampaignType.CPM_BANNER);

        var expectedBanner = new CpmOutdoorBanner()
                .withId(bannerId)
                .withHref("https" + "bannerHref")
                .withCreativeId(creativeId);

        ComplexCpmAdGroup expected = new ComplexCpmAdGroup()
                .withAdGroup(new CpmOutdoorAdGroup()
                        .withId(adGroupId)
                        .withType(AdGroupType.CPM_OUTDOOR)
                        .withName(adGroupName)
                        .withPageBlocks(singletonList(new PageBlock()
                                .withPageId(2L)
                                .withImpId(3L)))
                )
                .withBanners(singletonList(expectedBanner));

        assertThat(complexCpmAdGroup, beanDiffer(expected));
    }

    @Test
    public void convertEmptyCpmOutdoorAdGroup() {
        WebCpmAdGroup webCpmAdGroup = getEmptyAdGroup(PhrasesAdgroupType.cpm_outdoor)
                .withPageBlocks(singletonList(new WebPageBlock()
                        .withPageId(2L)
                        .withImpId(3L)));
        ComplexCpmAdGroup complexCpmAdGroup = webAdGroupToCoreComplexCpmAdGroup(webCpmAdGroup, CampaignType.CPM_BANNER);

        ComplexCpmAdGroup expected = new ComplexCpmAdGroup()
                .withAdGroup(new CpmOutdoorAdGroup()
                        .withType(AdGroupType.CPM_OUTDOOR)
                        .withPageBlocks(singletonList(new PageBlock()
                                .withPageId(2L)
                                .withImpId(3L))));

        assertThat(complexCpmAdGroup, beanDiffer(expected));
    }

    @Test
    public void convertCpmIndoorAdGroupWithBanner() {
        long adGroupId = 1L;
        long bannerId = 3L;
        long creativeId = 7L;
        String adGroupName = "123";

        WebCpmBanner banner = new WebCpmBanner()
                .withId(bannerId)
                .withAdType(BannersBannerType.cpm_banner.getLiteral())
                .withUrlProtocol("https")
                .withHref("bannerHref")
                .withCreative(new WebBannerCreative().withCreativeId(Long.toString(creativeId)));

        WebCpmAdGroup webCpmAdGroup = new WebCpmAdGroup()
                .withId(adGroupId)
                .withCpmBannersType(PhrasesAdgroupType.cpm_indoor.getLiteral())
                .withName(adGroupName)
                .withPageBlocks(singletonList(new WebPageBlock()
                        .withPageId(2L)
                        .withImpId(3L)))
                .withBanners(singletonList(banner));

        ComplexCpmAdGroup complexCpmAdGroup = webAdGroupToCoreComplexCpmAdGroup(webCpmAdGroup, CampaignType.CPM_BANNER);

        var expectedBanner = new CpmIndoorBanner()
                .withId(bannerId)
                .withHref("https" + "bannerHref")
                .withCreativeId(creativeId);

        ComplexCpmAdGroup expected = new ComplexCpmAdGroup()
                .withAdGroup(new CpmIndoorAdGroup()
                        .withId(adGroupId)
                        .withType(AdGroupType.CPM_INDOOR)
                        .withName(adGroupName)
                        .withPageBlocks(singletonList(new PageBlock()
                                .withPageId(2L)
                                .withImpId(3L)))
                )
                .withBanners(singletonList(expectedBanner));

        assertThat(complexCpmAdGroup, beanDiffer(expected));
    }

    @Test
    public void convertEmptyCpmIndoorAdGroup() {
        WebCpmAdGroup webCpmAdGroup = new WebCpmAdGroup()
                .withCpmBannersType(PhrasesAdgroupType.cpm_indoor.getLiteral())
                .withPageBlocks(singletonList(new WebPageBlock()
                        .withPageId(2L)
                        .withImpId(3L)));
        ComplexCpmAdGroup complexCpmAdGroup = webAdGroupToCoreComplexCpmAdGroup(webCpmAdGroup, CampaignType.CPM_BANNER);

        ComplexCpmAdGroup expected = new ComplexCpmAdGroup()
                .withAdGroup(new CpmIndoorAdGroup()
                        .withType(AdGroupType.CPM_INDOOR)
                        .withPageBlocks(singletonList(new PageBlock()
                                .withPageId(2L)
                                .withImpId(3L))));

        assertThat(complexCpmAdGroup, beanDiffer(expected));
    }

    @Test
    public void convertEmptyAdGroupFromCpmDealsCampaign() {
        WebCpmAdGroup webCpmAdGroup = new WebCpmAdGroup();
        ComplexCpmAdGroup complexCpmAdGroup = webAdGroupToCoreComplexCpmAdGroup(webCpmAdGroup, CampaignType.CPM_DEALS);

        ComplexCpmAdGroup expected = new ComplexCpmAdGroup()
                .withAdGroup(new CpmBannerAdGroup());
        assertThat(complexCpmAdGroup, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void convertAdGroupWithGeo() {
        String sourceGeo = "123,456";
        List<Long> destGeo = asList(123L, 456L);

        WebCpmAdGroup webCpmAdGroup = getEmptyAdGroup(PhrasesAdgroupType.cpm_banner)
                .withGeo(sourceGeo);
        ComplexCpmAdGroup complexCpmAdGroup = webAdGroupToCoreComplexCpmAdGroup(webCpmAdGroup, CampaignType.CPM_BANNER);
        assertThat(complexCpmAdGroup.getAdGroup().getGeo(), equalTo(destGeo));
    }

    @Test
    public void convertAdGroupWithTags() {
        Map<String, Integer> sourceTags = ImmutableMap.of("123", 1, "345", 0, "567", 1);

        WebCpmAdGroup webCpmAdGroup = getEmptyAdGroup(PhrasesAdgroupType.cpm_banner)
                .withTags(sourceTags);
        ComplexCpmAdGroup complexCpmAdGroup = webAdGroupToCoreComplexCpmAdGroup(webCpmAdGroup, CampaignType.CPM_BANNER);
        assertThat(complexCpmAdGroup.getAdGroup().getTags(), containsInAnyOrder(123L, 567L));
    }

    @Test
    public void convertAdGroupWithPageGroupTags() {
        List<String> pageGroupTags = asList("page_group_tag1", "page_group_tag2");
        WebCpmAdGroup webCpmAdGroup = getEmptyAdGroup(PhrasesAdgroupType.cpm_banner)
                .withPageGroupTags(pageGroupTags);
        ComplexCpmAdGroup complexCpmAdGroup = webAdGroupToCoreComplexCpmAdGroup(webCpmAdGroup, CampaignType.CPM_BANNER);
        assertThat(complexCpmAdGroup.getAdGroup().getPageGroupTags(), containsInAnyOrder(pageGroupTags.toArray()));
    }

    @Test
    public void convertAdGroupWithTargetTags() {
        List<String> targetTags = asList("target_tag1", "target_tag2");
        WebCpmAdGroup webCpmAdGroup = getEmptyAdGroup(PhrasesAdgroupType.cpm_banner)
                .withTargetTags(targetTags);
        ComplexCpmAdGroup complexCpmAdGroup = webAdGroupToCoreComplexCpmAdGroup(webCpmAdGroup, CampaignType.CPM_BANNER);
        assertThat(complexCpmAdGroup.getAdGroup().getTargetTags(), containsInAnyOrder(targetTags.toArray()));
    }

    @Test
    public void convertAdGroupWithMinusKeywords() {
        List<String> minusKeywords = asList("one", "two");
        WebCpmAdGroup webCpmAdGroup = getEmptyAdGroup(PhrasesAdgroupType.cpm_banner)
                .withMinusKeywords(minusKeywords);
        ComplexCpmAdGroup complexCpmAdGroup = webAdGroupToCoreComplexCpmAdGroup(webCpmAdGroup, CampaignType.CPM_BANNER);
        assertThat(complexCpmAdGroup.getAdGroup().getMinusKeywords(), sameInstance(minusKeywords));
    }

    @Test
    public void convertAdGroupWithKeywords() {
        WebCpmAdGroup webCpmAdGroup = getEmptyAdGroup(PhrasesAdgroupType.cpm_banner)
                .withKeywords(singletonList(new WebKeyword().withId(1L).withPhrase("456")));

        ComplexCpmAdGroup complexCpmAdGroup = webAdGroupToCoreComplexCpmAdGroup(webCpmAdGroup, CampaignType.CPM_BANNER);

        ComplexCpmAdGroup expected = new ComplexCpmAdGroup()
                .withKeywords(singletonList(new Keyword().withId(1L)
                        .withPhrase("456")));
        assertThat(complexCpmAdGroup,
                beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void convertAdGroupWithRetargetings() {
        WebCpmAdGroup webCpmAdGroup = getEmptyAdGroup(PhrasesAdgroupType.cpm_banner)
                .withRetargetings(singletonList(new WebCpmAdGroupRetargeting()
                        .withId(1L)
                        .withRetargetingConditionId(2L)
                        .withPriceContext(50.0)));

        ComplexCpmAdGroup complexCpmAdGroup = webAdGroupToCoreComplexCpmAdGroup(webCpmAdGroup, CampaignType.CPM_BANNER);
        ComplexCpmAdGroup expected = new ComplexCpmAdGroup()
                .withTargetInterests(singletonList(new TargetInterest()
                        .withId(1L)
                        .withRetargetingConditionId(2L)));

        assertThat(complexCpmAdGroup,
                beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void convertAdGroupWithBidModifiers() {
        WebWeatherBidModifier webWeatherBidModifier = new WebWeatherBidModifier()
                .withAdjustments(singletonList(new WebWeatherAdjustment()
                        .withExpression(singletonList(singletonList(
                                new WebWeatherExpression()
                                        .withParameter("temp")
                                        .withOperation("le")
                                        .withValue(20)
                        )))));
        BidModifierWeather expectedWeatherModifier = new BidModifierWeather()
                .withWeatherAdjustments(singletonList(new BidModifierWeatherAdjustment()
                        .withExpression(singletonList(singletonList(
                                new BidModifierWeatherLiteral()
                                        .withParameter(WeatherType.TEMP)
                                        .withOperation(OperationType.LE)
                                        .withValue(20)
                        )))));

        WebDesktopBidModifier webDesktopBidModifier = new WebDesktopBidModifier()
                .withPercent(2);
        BidModifierDesktop expectedDesktopModifier = new BidModifierDesktop()
                .withDesktopAdjustment(new BidModifierDesktopAdjustment().withPercent(2));

        WebMobileBidModifier webMobileBidModifier = new WebMobileBidModifier()
                .withOsType("ios")
                .withPercent(3);
        BidModifierMobile expectedMobileModifier = new BidModifierMobile()
                .withMobileAdjustment(
                        new BidModifierMobileAdjustment()
                                .withOsType(OsType.IOS)
                                .withPercent(3));

        WebRetargetingBidModifier webRetargetingBidModifier = new WebRetargetingBidModifier()
                .withAdjustments(singletonMap("5", new WebAdjustment().withPercent(4)));
        BidModifierRetargeting expectedRetargetingModifier = new BidModifierRetargeting()
                .withRetargetingAdjustments(singletonList(new BidModifierRetargetingAdjustment()
                        .withRetargetingConditionId(5L)
                        .withPercent(4)));

        WebDemographicsBidModifier webDemographicsBidModifier = new WebDemographicsBidModifier()
                .withAdjustments(singletonList(new WebDemographicsAdjustment()
                        .withAge("18-24")
                        .withGender("male")));
        BidModifierDemographics expectedDemographyModifier = new BidModifierDemographics()
                .withDemographicsAdjustments(singletonList(new BidModifierDemographicsAdjustment()
                        .withAge(_18_24)
                        .withGender(MALE)));

        WebCpmAdGroup webCpmAdGroup = getEmptyAdGroup(PhrasesAdgroupType.cpm_banner)
                .withBidModifiers(new WebAdGroupBidModifiers()
                        .withMobileBidModifier(webMobileBidModifier)
                        .withDesktopBidModifier(webDesktopBidModifier)
                        .withRetargetingBidModifier(webRetargetingBidModifier)
                        .withDemographicsBidModifier(webDemographicsBidModifier)
                        .withWeatherBidModifier(webWeatherBidModifier)
                        .withExpressionBidModifiers(List.of(TestBidModifiers.SAMPLE_WEB_EXPRESS_MODIFIER))
                );

        ComplexCpmAdGroup complexCpmAdGroup = webAdGroupToCoreComplexCpmAdGroup(webCpmAdGroup, CampaignType.CPM_BANNER);

        ComplexCpmAdGroup expected = new ComplexCpmAdGroup()
                .withComplexBidModifier(new ComplexBidModifier()
                        .withMobileModifier(expectedMobileModifier)
                        .withDesktopModifier(expectedDesktopModifier)
                        .withRetargetingModifier(expectedRetargetingModifier)
                        .withDemographyModifier(expectedDemographyModifier)
                        .withWeatherModifier(expectedWeatherModifier)
                        .withExpressionModifiers(List.of(TestBidModifiers.SAMPLE_EXPRESS_MODIFIER))
                );

        assertThat(complexCpmAdGroup,
                beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }


    @Test
    public void convertEmptyCpmGeoproductAdGroup() {
        long adGroupId = 1L;
        String adGroupName = "123";
        WebCpmAdGroup webCpmAdGroup = getEmptyAdGroup(PhrasesAdgroupType.cpm_geoproduct)
                .withId(adGroupId)
                .withName(adGroupName);
        ComplexCpmAdGroup complexCpmAdGroup = webAdGroupToCoreComplexCpmAdGroup(webCpmAdGroup, CampaignType.CPM_BANNER);

        ComplexCpmAdGroup expected = new ComplexCpmAdGroup()
                .withAdGroup(new CpmGeoproductAdGroup()
                        .withId(adGroupId)
                        .withName(adGroupName)
                        .withType(AdGroupType.CPM_GEOPRODUCT)
                );

        assertThat(complexCpmAdGroup, beanDiffer(expected));
    }

    @Test
    public void convertCpmGeoproductAdGroupWithBanner() {
        long adGroupId = 1L;
        long bannerId = 3L;
        long creativeId = 7L;
        long turboId = 12L;
        String adGroupName = "123";
        String bannerTurbolandingHrefParams = "param1=value1&param2=value2";

        WebCpmBanner banner = new WebCpmBanner()
                .withId(bannerId)
                .withAdType(BannersBannerType.cpm_banner.getLiteral())
                .withPixels(singletonList(new WebPixel().withKind(PixelKind.AUDIENCE).withUrl(yaAudiencePixelUrl())))
                .withUrlProtocol("https")
                .withHref("bannerHref")
                .withCreative(new WebBannerCreative().withCreativeId(Long.toString(creativeId)))
                .withTurbolanding(new WebBannerTurbolanding().withId(turboId))
                .withTurbolandingHrefParams(bannerTurbolandingHrefParams);

        WebCpmAdGroup webCpmAdGroup = getEmptyAdGroup(PhrasesAdgroupType.cpm_geoproduct)
                .withId(adGroupId)
                .withName(adGroupName)
                .withBanners(singletonList(banner));

        ComplexCpmAdGroup complexCpmAdGroup =
                webAdGroupToCoreComplexCpmAdGroup(webCpmAdGroup, CampaignType.CPM_BANNER);

        var expectedBanner = new CpmBanner()
                .withId(bannerId)
                .withPixels(singletonList(yaAudiencePixelUrl()))
                .withHref("https" + "bannerHref")
                .withCreativeId(creativeId)
                .withTurboLandingId(turboId)
                .withTurboLandingHrefParams(bannerTurbolandingHrefParams)
                .withMeasurers(emptyList());

        ComplexCpmAdGroup expected = new ComplexCpmAdGroup()
                .withAdGroup(new CpmGeoproductAdGroup()
                        .withId(adGroupId)
                        .withName(adGroupName)
                        .withType(AdGroupType.CPM_GEOPRODUCT)
                )
                .withBanners(singletonList(expectedBanner));

        assertThat(complexCpmAdGroup, beanDiffer(expected));
    }

    @Test
    public void convertCpmGeoproductAdGroupWithRetargetings() {
        WebCpmAdGroup webCpmAdGroup = getEmptyAdGroup(PhrasesAdgroupType.cpm_geoproduct)
                .withRetargetings(singletonList(new WebCpmAdGroupRetargeting()
                        .withId(1L)
                        .withRetargetingConditionId(2L)
                        .withPriceContext(50.0)));

        ComplexCpmAdGroup complexCpmAdGroup = webAdGroupToCoreComplexCpmAdGroup(webCpmAdGroup, CampaignType.CPM_BANNER);
        ComplexCpmAdGroup expected = new ComplexCpmAdGroup()
                .withAdGroup(new CpmGeoproductAdGroup())
                .withTargetInterests(singletonList(new TargetInterest()
                        .withId(1L)
                        .withRetargetingConditionId(2L)));

        assertThat(complexCpmAdGroup,
                beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    private WebCpmAdGroup getEmptyAdGroup(PhrasesAdgroupType adgroupType) {
        return new WebCpmAdGroup()
                .withCpmBannersType(adgroupType.getLiteral());
    }
}
