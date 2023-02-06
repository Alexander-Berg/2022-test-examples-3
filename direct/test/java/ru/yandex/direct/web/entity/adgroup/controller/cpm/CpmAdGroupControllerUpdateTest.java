package ru.yandex.direct.web.entity.adgroup.controller.cpm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmGeoproductAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmIndoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmOutdoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerType;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingParams;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.testing.data.TestBidModifiers;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.adgroup.model.PixelKind;
import ru.yandex.direct.web.entity.adgroup.model.WebAdGroupBidModifiers;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroup;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroupRetargeting;
import ru.yandex.direct.web.entity.adgroup.model.WebPageBlock;
import ru.yandex.direct.web.entity.adgroup.model.WebRetargetingGoal;
import ru.yandex.direct.web.entity.adgroup.model.WebRetargetingRule;
import ru.yandex.direct.web.entity.banner.model.WebBannerTurbolanding;
import ru.yandex.direct.web.entity.banner.model.WebCpmBanner;
import ru.yandex.direct.web.entity.banner.model.WebPixel;
import ru.yandex.direct.web.entity.bidmodifier.model.WebMobileBidModifier;
import ru.yandex.direct.web.entity.keyword.model.WebKeyword;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate.READY;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.dcmPixelUrl;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.keywordForCpmBanner;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorPlacementWithBlock;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithBlock;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.core.testing.steps.TurboLandingSteps.defaultBannerTurboLanding;
import static ru.yandex.direct.dbschema.ppc.enums.TurbolandingsPreset.cpm_geoproduct_preset;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.web.entity.adgroup.converter.AdGroupConverterUtils.convertPageBlocks;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmAudioAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmBannerAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmGeoproductAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmIndoorAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmOutdoorAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmVideoAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmYndxFrontpageAdGroup;
import static ru.yandex.direct.web.testing.data.TestBanners.webCpmBanner;
import static ru.yandex.direct.web.testing.data.TestBidModifiers.cpmAdGroupDeviceBidModifiers;
import static ru.yandex.direct.web.testing.data.TestBidModifiers.cpmAdGroupWeatherBidModifiers;
import static ru.yandex.direct.web.testing.data.TestBidModifiers.fullCpmAdGroupBidModifiers;
import static ru.yandex.direct.web.testing.data.TestBidModifiers.randomPercentIosMobileBidModifier;
import static ru.yandex.direct.web.testing.data.TestKeywords.randomPhraseKeyword;

@DirectWebTest
@RunWith(SpringRunner.class)
public class CpmAdGroupControllerUpdateTest extends CpmAdGroupControllerTestBase {

    private AdGroupInfo cpmBannerUserProfileAdGroup;
    private Long userProfileAdGroupId;

    private AdGroupInfo cpmBannerUserKeywordsGroup;
    private Long keywordsAdGroupId;

    private AdGroupInfo cpmVideoAdGroup;
    private Long cpmVideoAdGroupId;

    private AdGroupInfo cpmOutdoorAdGroup;
    private Long cpmOutdoorAdGroupId;

    private AdGroupInfo cpmYndxFrontpageAdGroup;
    private Long cpmYndxFrontpageAdGroupId;

    private AdGroupInfo cpmGeoproductAdGroup;
    private Long cpmGeoproductAdGroupId;

    private AdGroupInfo cpmAudioAdGroup;
    private Long cpmAudioAdGroupId;

    private CampaignInfo campaignWithForeignPlacement;

    @Before
    public void before() {
        super.before();
        cpmBannerUserProfileAdGroup = steps.adGroupSteps().createActiveCpmBannerAdGroup(campaignInfo);
        userProfileAdGroupId = cpmBannerUserProfileAdGroup.getAdGroupId();

        cpmBannerUserKeywordsGroup =
                steps.adGroupSteps().createActiveCpmBannerAdGroupWithKeywordsCriterionType(campaignInfo);
        keywordsAdGroupId = cpmBannerUserKeywordsGroup.getAdGroupId();

        cpmVideoAdGroup = steps.adGroupSteps().createActiveCpmVideoAdGroup(campaignInfo);
        cpmVideoAdGroupId = cpmVideoAdGroup.getAdGroupId();

        cpmOutdoorAdGroup = steps.adGroupSteps().createActiveCpmOutdoorAdGroup(campaignInfo);
        cpmOutdoorAdGroupId = cpmOutdoorAdGroup.getAdGroupId();

        cpmYndxFrontpageAdGroup = steps.adGroupSteps().createDefaultCpmYndxFrontpageAdGroup(frontpageCampaignInfo);
        cpmYndxFrontpageAdGroupId = cpmYndxFrontpageAdGroup.getAdGroupId();

        cpmGeoproductAdGroup = steps.adGroupSteps().createDefaultCpmGeoproductAdGroup(campaignInfo);
        cpmGeoproductAdGroupId = cpmGeoproductAdGroup.getAdGroupId();

        cpmAudioAdGroup = steps.adGroupSteps().createActiveCpmAudioAdGroup(campaignInfo);
        cpmAudioAdGroupId = cpmAudioAdGroup.getAdGroupId();

        campaignWithForeignPlacement = createCpmDealCampaignWithInventory(singletonList(nonYandexOnlyPlacementDeal));
    }

    //cpm banner
    @Test
    public void primitiveFieldsUpdated() {
        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(userProfileAdGroupId, campaignInfo.getCampaignId());

        updateAdGroups(singletonList(webCpmAdGroup));

        AdGroup expected = new CpmBannerAdGroup()
                .withType(AdGroupType.CPM_BANNER)
                .withId(userProfileAdGroupId)
                .withName(webCpmAdGroup.getName());

        List<AdGroup> adGroups = findAdGroup(userProfileAdGroupId);
        assertThat("группа обновилась корректно", adGroups.get(0),
                beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void bannerAdded() {
        WebCpmBanner webBanner = webCpmBanner(null, creativeId)
                .withTurbolanding(new WebBannerTurbolanding().withId(bannerTurboLandings.get(0).getId()));
        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(userProfileAdGroupId, campaignInfo.getCampaignId())
                        .withBanners(singletonList(webBanner));

        updateAdGroups(singletonList(webCpmAdGroup));

        checkBanner(userProfileAdGroupId, AdGroupType.CPM_BANNER, webBanner);
    }

    @Test
    public void bannerDeleted() {
        steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(campaignInfo.getCampaignId(), userProfileAdGroupId, creativeId),
                cpmBannerUserProfileAdGroup);

        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(userProfileAdGroupId, campaignInfo.getCampaignId());

        updateAdGroups(singletonList(webCpmAdGroup));

        List<OldBanner> banners = findOldBanners(userProfileAdGroupId);
        assertThat("в группе не должно быть баннеров", banners, hasSize(1));
    }

    @Test
    public void bannerUpdated() {
        CpmBannerInfo cpmBanner = steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(campaignInfo.getCampaignId(), userProfileAdGroupId, creativeId)
                        .withTurboLandingId(bannerTurboLandings.get(1).getId())
                        .withTurboLandingStatusModerate(bannerTurboLandings.get(1).getStatusModerate()),
                cpmBannerUserProfileAdGroup);

        WebCpmBanner webBanner = webCpmBanner(cpmBanner.getBannerId(), creativeId)
                .withTurbolanding(new WebBannerTurbolanding().withId(bannerTurboLandings.get(0).getId()));
        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(userProfileAdGroupId, campaignInfo.getCampaignId())
                        .withBanners(singletonList(webBanner));

        updateAdGroups(singletonList(webCpmAdGroup));

        List<OldBanner> banners = findOldBanners(userProfileAdGroupId);
        assertThat("в группе должен быть один баннер", banners, hasSize(1));

        checkBanner(userProfileAdGroupId, AdGroupType.CPM_BANNER, webBanner);
    }

    @Test
    public void cpmBannerWithEmptyMeasurersBsSyncedNotUpdated() {
        var cpmBanner = activeCpmBanner(campaignInfo.getCampaignId(), userProfileAdGroupId, creativeId)
                .withMeasurers(null)
                .withTurboLandingParams(new OldBannerTurboLandingParams().withHrefParams("param1=value1&param2=value2"))
                .withStatusBsSynced(StatusBsSynced.YES)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES);
        var cpmBannerInfo = steps.bannerSteps().createActiveCpmBanner(
                cpmBanner,
                cpmBannerUserProfileAdGroup);

        var webBanner = webCpmBanner(cpmBannerInfo.getBannerId(), creativeId).withMeasurers(null);

        var webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(userProfileAdGroupId, campaignInfo.getCampaignId())
                        .withBanners(singletonList(webBanner));

        updateAdGroups(singletonList(webCpmAdGroup));

        List<OldBanner> banners = findOldBanners(userProfileAdGroupId);
        assertThat("в группе должен быть один баннер", banners, hasSize(1));

        assertEquals(StatusBsSynced.YES, banners.get(0).getStatusBsSynced());
        assertEquals(OldBannerCreativeStatusModerate.YES, ((OldCpmBanner) banners.get(0)).getCreativeStatusModerate());
    }

    @Test
    public void cpmBannerWithNotEmptyMeasurersBsSyncedUpdated() {
        var cpmBanner = activeCpmBanner(campaignInfo.getCampaignId(), userProfileAdGroupId, creativeId)
                .withTurboLandingParams(new OldBannerTurboLandingParams().withHrefParams("param1=value1&param2=value2"))
                .withStatusBsSynced(StatusBsSynced.YES)
                .withCreativeStatusModerate(OldBannerCreativeStatusModerate.YES);
        var cpmBannerInfo = steps.bannerSteps().createActiveCpmBanner(
                cpmBanner,
                cpmBannerUserProfileAdGroup);

        var webBanner = webCpmBanner(cpmBannerInfo.getBannerId(), creativeId).withMeasurers(null);

        var webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(userProfileAdGroupId, campaignInfo.getCampaignId())
                        .withBanners(singletonList(webBanner));

        updateAdGroups(singletonList(webCpmAdGroup));

        List<OldBanner> banners = findOldBanners(userProfileAdGroupId);
        assertThat("в группе должен быть один баннер", banners, hasSize(1));

        assertEquals(StatusBsSynced.NO, banners.get(0).getStatusBsSynced());
        assertEquals(OldBannerCreativeStatusModerate.YES, ((OldCpmBanner) banners.get(0)).getCreativeStatusModerate());
    }

    @Test
    public void retargetingUpdated() {
        WebCpmAdGroupRetargeting retargetingForUpdate = createRetargeting(cpmBannerUserProfileAdGroup);

        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(userProfileAdGroupId, campaignInfo.getCampaignId())
                        .withRetargetings(singletonList(retargetingForUpdate
                                .withPriceContext(333.0)
                                .withName("another condition name")
                                .withDescription("another condition description")));

        updateAdGroups(singletonList(webCpmAdGroup));

        checkRetargetings(webCpmAdGroup.getRetargetings(), webCpmAdGroup.getId());
        checkRetargetingConditions(webCpmAdGroup.getRetargetings(), webCpmAdGroup.getId());
    }

    @Test
    public void updateOnlyRetargetingPrice() {
        WebCpmAdGroupRetargeting retargetingForUpdate = createRetargeting(cpmBannerUserProfileAdGroup);

        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(userProfileAdGroupId, campaignInfo.getCampaignId())
                        .withRetargetings(singletonList(retargetingForUpdate));

        updateAdGroups(singletonList(webCpmAdGroup));

        List<Retargeting> retargetings = findRetargetings(userProfileAdGroupId);
        assertThat("в группе должен быть один ретаргетинг", retargetings, hasSize(1));

        Retargeting expectedRetargeting = new Retargeting()
                .withId(retargetingForUpdate.getId())
                .withRetargetingConditionId(retargetingForUpdate.getRetargetingConditionId())
                .withPriceContext(
                        BigDecimal.valueOf(retargetingForUpdate.getPriceContext()).setScale(2, RoundingMode.DOWN));
        assertThat("обновилась ставка у ретаргетинга", retargetings.get(0),
                beanDiffer(expectedRetargeting).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void keywordAdded() {
        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(keywordsAdGroupId, campaignInfo.getCampaignId())
                        .withGeneralPrice(120.0)
                        .withKeywords(singletonList(randomPhraseKeyword(null)));

        updateAdGroups(singletonList(webCpmAdGroup));

        List<Keyword> keywords = findKeywords();
        assertThat("в группе должна быть одна фраза", keywords, hasSize(1));

        Double generalPrice = webCpmAdGroup.getGeneralPrice();
        Keyword expectedKeyword = new Keyword()
                .withPrice(null)
                .withPriceContext(BigDecimal.valueOf(generalPrice).setScale(2, RoundingMode.DOWN))
                .withPhrase(webCpmAdGroup.getKeywords().get(0).getPhrase());
        assertThat("добавилась корректная фраза", keywords.get(0),
                beanDiffer(expectedKeyword).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void keywordDeleted() {
        steps.keywordSteps().createKeyword(cpmBannerUserKeywordsGroup, keywordForCpmBanner());

        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(keywordsAdGroupId, campaignInfo.getCampaignId());

        updateAdGroups(singletonList(webCpmAdGroup));

        List<Keyword> keywords = findKeywords();
        assertThat("в группе не должно быть фраз", keywords, hasSize(0));
    }

    @Test
    public void keywordUpdated() {
        KeywordInfo keyword = steps.keywordSteps().createKeyword(cpmBannerUserKeywordsGroup, keywordForCpmBanner());

        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(keywordsAdGroupId, campaignInfo.getCampaignId())
                        .withGeneralPrice(130.0)
                        .withKeywords(singletonList(new WebKeyword()
                                .withId(keyword.getId())
                                .withPhrase("new phrase")));

        updateAdGroups(singletonList(webCpmAdGroup));

        List<Keyword> keywords = findKeywords();
        assertThat("в группе должна быть одна фраза", keywords, hasSize(1));

        Double generalPrice = webCpmAdGroup.getGeneralPrice();
        Keyword expectedKeyword = new Keyword()
                .withId(keyword.getId())
                .withPriceContext(BigDecimal.valueOf(generalPrice).setScale(2, RoundingMode.DOWN))
                .withPhrase(webCpmAdGroup.getKeywords().get(0).getPhrase());
        assertThat("фраза обновилась корректно", keywords.get(0),
                beanDiffer(expectedKeyword).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void twoAdGroupsUpdated() {
        WebCpmAdGroupRetargeting retargetingForUpdate = createRetargeting(cpmBannerUserProfileAdGroup);

        WebCpmAdGroup adGroupWithRetargetings =
                randomNameWebCpmBannerAdGroup(userProfileAdGroupId, campaignInfo.getCampaignId())
                        .withRetargetings(singletonList(retargetingForUpdate
                                .withPriceContext(333.0)
                                .withName("another condition name")
                                .withDescription("another condition description")));

        KeywordInfo keyword = steps.keywordSteps().createKeyword(cpmBannerUserKeywordsGroup, keywordForCpmBanner());

        WebCpmAdGroup adGroupWithKeywords =
                randomNameWebCpmBannerAdGroup(keywordsAdGroupId, campaignInfo.getCampaignId())
                        .withGeneralPrice(130.0)
                        .withKeywords(singletonList(new WebKeyword()
                                .withId(keyword.getId())
                                .withPhrase("new phrase")));

        updateAdGroups(asList(adGroupWithRetargetings, adGroupWithKeywords));
        checkRetargetings(adGroupWithRetargetings.getRetargetings(), userProfileAdGroupId);
        checkRetargetingConditions(adGroupWithRetargetings.getRetargetings(), userProfileAdGroupId);

        checkKeywords(adGroupWithKeywords.getKeywords());
    }

    @Test
    public void bidModifiersAdded() {
        WebAdGroupBidModifiers webBidModifiers = fullCpmAdGroupBidModifiers(retCondId);
        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(keywordsAdGroupId, campaignInfo.getCampaignId())
                        .withKeywords(singletonList(randomPhraseKeyword(null)))
                        .withBidModifiers(webBidModifiers);

        updateAdGroups(singletonList(webCpmAdGroup));

        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("должно было добавиться 5 корректировок", bidModifiers, hasSize(5));
        checkMobileBidModifier(bidModifiers, keywordsAdGroupId, webBidModifiers.getMobileBidModifier());
        checkDesktopBidModifier(bidModifiers, keywordsAdGroupId, webBidModifiers.getDesktopBidModifier());
        checkDemographyBidModifier(bidModifiers, keywordsAdGroupId, webBidModifiers.getDemographicsBidModifier());
        checkWeatherBidModifier(bidModifiers, keywordsAdGroupId, webBidModifiers.getWeatherBidModifier());
        checkRetargetingBidModifier(bidModifiers, keywordsAdGroupId, webBidModifiers.getRetargetingBidModifier());
    }

    @Test
    public void bidModifiersDeleted() {
        steps.bidModifierSteps().createDefaultAdGroupBidModifierMobile(cpmBannerUserKeywordsGroup);

        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(keywordsAdGroupId, campaignInfo.getCampaignId())
                        .withKeywords(singletonList(randomPhraseKeyword(null)));

        updateAdGroups(singletonList(webCpmAdGroup));

        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("корректировки должны были удалиться", bidModifiers, hasSize(0));
    }

    @Test
    public void bidModifiersMobileIosUpdated() {
        steps.bidModifierSteps().createDefaultAdGroupIosBidModifierMobile(cpmBannerUserKeywordsGroup);

        WebMobileBidModifier webMobileBidModifier = randomPercentIosMobileBidModifier()
                .withPercent(TestBidModifiers.PERCENT_MAX);
        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(keywordsAdGroupId, campaignInfo.getCampaignId())
                        .withKeywords(singletonList(randomPhraseKeyword(null)))
                        .withBidModifiers(new WebAdGroupBidModifiers().withMobileBidModifier(webMobileBidModifier));

        updateAdGroups(singletonList(webCpmAdGroup));

        List<BidModifier> bidModifiers = findBidModifiers();
        checkMobileBidModifier(bidModifiers, keywordsAdGroupId, webMobileBidModifier);
    }

    @Test
    public void deviceBidModifiersAddedToCpmBannerAdGroupWithRetargetings() {
        WebAdGroupBidModifiers webBidModifiers = cpmAdGroupDeviceBidModifiers();
        WebCpmAdGroupRetargeting retargetingForUpdate = createRetargeting(cpmBannerUserProfileAdGroup);
        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(userProfileAdGroupId, campaignInfo.getCampaignId())
                        .withRetargetings(singletonList(retargetingForUpdate))
                        .withBidModifiers(webBidModifiers);

        updateAdGroups(singletonList(webCpmAdGroup));

        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("должно было добавиться 2 корректировки", bidModifiers, hasSize(2));
        checkMobileBidModifier(bidModifiers, userProfileAdGroupId, webBidModifiers.getMobileBidModifier());
        checkDesktopBidModifier(bidModifiers, userProfileAdGroupId, webBidModifiers.getDesktopBidModifier());
    }

    @Test
    public void deviceBidModifiersDeletedInCpmBannerAdGroupWithRetargetings() {
        steps.bidModifierSteps().createDefaultAdGroupIosBidModifierMobile(cpmBannerUserProfileAdGroup);
        steps.bidModifierSteps().createDefaultAdGroupBidModifierDesktop(cpmBannerUserProfileAdGroup);

        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(userProfileAdGroupId, campaignInfo.getCampaignId())
                        .withRetargetings(singletonList(createRetargeting(cpmBannerUserProfileAdGroup)));

        updateAdGroups(singletonList(webCpmAdGroup));

        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("корректировки должны были удалиться", bidModifiers, hasSize(0));
    }

    @Test
    public void weatherBidModifierAddedToCpmAudioAdGroup() {
        WebAdGroupBidModifiers webBidModifiers = cpmAdGroupWeatherBidModifiers();
        WebCpmAdGroup webCpmAdGroup = randomNameWebCpmAudioAdGroup(cpmAudioAdGroupId, campaignInfo.getCampaignId())
                .withBidModifiers(webBidModifiers);

        updateAdGroups(singletonList(webCpmAdGroup));

        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("должна была добавиться 1 корректировка", bidModifiers, hasSize(1));

        checkWeatherBidModifier(bidModifiers, cpmAudioAdGroupId,
                webBidModifiers.getWeatherBidModifier());
    }

    @Test
    public void deviceBidModifiersAddedToCpmVideoAdGroup() {
        WebAdGroupBidModifiers webBidModifiers = cpmAdGroupDeviceBidModifiers();
        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmVideoAdGroup(cpmVideoAdGroupId, campaignInfo.getCampaignId())
                        .withBidModifiers(webBidModifiers);

        updateAdGroups(singletonList(webCpmAdGroup));

        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("должно было добавиться 2 корректировки", bidModifiers, hasSize(2));
        checkMobileBidModifier(bidModifiers, cpmVideoAdGroupId, webBidModifiers.getMobileBidModifier());
        checkDesktopBidModifier(bidModifiers, cpmVideoAdGroupId, webBidModifiers.getDesktopBidModifier());
    }

    @Test
    public void deviceBidModifiersDeletedInCpmVideoAdGroup() {
        steps.bidModifierSteps().createDefaultAdGroupIosBidModifierMobile(cpmVideoAdGroup);
        steps.bidModifierSteps().createDefaultAdGroupBidModifierDesktop(cpmVideoAdGroup);

        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmVideoAdGroup(cpmVideoAdGroupId, campaignInfo.getCampaignId());

        updateAdGroups(singletonList(webCpmAdGroup));

        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("корректировки должны были удалиться", bidModifiers, hasSize(0));
    }

    @Test
    public void tagsUpdated() {
        List<Long> campaignTags = steps.tagCampaignSteps()
                .createDefaultTags(campaignInfo.getShard(), campaignInfo.getClientId(), campaignInfo.getCampaignId(),
                        3);
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(activeCpmBannerAdGroup(campaignInfo.getCampaignId())
                .withTags(campaignTags), campaignInfo);

        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(adGroup.getAdGroupId(), campaignInfo.getCampaignId())
                        .withTags(singletonMap(campaignTags.get(0).toString(), 1));

        updateAdGroups(singletonList(webCpmAdGroup));

        List<Long> tags = findTags(adGroup.getAdGroupId());
        assertThat("теги обновились", tags, containsInAnyOrder(campaignTags.get(0)));
    }

    @Test
    public void pageGroupTagsUpdated() {
        List<String> pageGroupTags = asList("page_group_tag1", "page_group_tag2");
        AdGroupInfo adGroupInfo =
                steps.adGroupSteps().createAdGroup(activeCpmBannerAdGroup(campaignInfo.getCampaignId()), campaignInfo);

        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(adGroupInfo.getAdGroupId(), campaignInfo.getCampaignId())
                        .withPageGroupTags(pageGroupTags);

        updateAdGroups(singletonList(webCpmAdGroup));

        AdGroup actualAdGroup = findAdGroup(adGroupInfo.getAdGroupId()).get(0);
        assertThat("statusBsSynced сбросился", actualAdGroup.getStatusBsSynced(), equalTo(StatusBsSynced.NO));
        assertThat("теги для таргетинга обновились", actualAdGroup.getPageGroupTags(),
                containsInAnyOrder(pageGroupTags.toArray()));
    }

    @Test
    public void targetTagsUpdated() {
        List<String> targetTags = asList("target_tag1", "target_tag2");
        AdGroupInfo adGroupInfo =
                steps.adGroupSteps().createAdGroup(activeCpmBannerAdGroup(campaignInfo.getCampaignId()), campaignInfo);

        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(adGroupInfo.getAdGroupId(), campaignInfo.getCampaignId())
                        .withTargetTags(targetTags);

        updateAdGroups(singletonList(webCpmAdGroup));

        AdGroup actualAdGroup = findAdGroup(adGroupInfo.getAdGroupId()).get(0);
        assertThat("statusBsSynced сбросился", actualAdGroup.getStatusBsSynced(), equalTo(StatusBsSynced.NO));
        assertThat("теги для таргетинга обновились", actualAdGroup.getTargetTags(),
                containsInAnyOrder(targetTags.toArray()));
    }

    //cpm_video
    @Test
    public void cpmVideoAdGroupUpdated() {
        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmVideoAdGroup(cpmVideoAdGroupId, campaignInfo.getCampaignId());

        updateAdGroups(singletonList(webCpmAdGroup));

        AdGroup expected = new CpmVideoAdGroup()
                .withType(AdGroupType.CPM_VIDEO)
                .withId(cpmVideoAdGroupId)
                .withName(webCpmAdGroup.getName());

        List<AdGroup> adGroups = findAdGroup(cpmVideoAdGroupId);
        assertThat("группа обновилась корректно", adGroups.get(0),
                beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    //cpm_outdoor
    @Test
    public void cpmOutdoorAdGroupUpdated() {
        steps.placementSteps().clearPlacements();
        long pageId = 29342L;
        long blockId = 123L;
        OutdoorPlacement placement = outdoorPlacementWithBlock(pageId, outdoorBlockWithOneSize(pageId, blockId));
        steps.placementSteps().addPlacement(placement);
        WebPageBlock newPageBlock = new WebPageBlock()
                .withPageId(placement.getId())
                .withImpId(placement.getBlocks().get(0).getBlockId());

        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmOutdoorAdGroup(cpmOutdoorAdGroupId, campaignInfo.getCampaignId(), newPageBlock);

        updateAdGroups(singletonList(webCpmAdGroup));


        AdGroup expected = new CpmOutdoorAdGroup()
                .withType(AdGroupType.CPM_OUTDOOR)
                .withId(cpmOutdoorAdGroupId)
                .withName(webCpmAdGroup.getName())
                .withPageBlocks(convertPageBlocks(webCpmAdGroup.getPageBlocks()));

        List<AdGroup> adGroups = findAdGroup(cpmOutdoorAdGroupId);
        assertThat("группа обновилась корректно", adGroups.get(0),
                beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    //cpm_indoor
    @Test
    public void cpmIndoorAdGroupUpdated() {
        steps.placementSteps().clearPlacements();
        long pageId = 29342L;
        long blockId = 123L;
        IndoorPlacement placement = indoorPlacementWithBlock(pageId, indoorBlockWithOneSize(pageId, blockId));
        steps.placementSteps().addPlacement(placement);
        WebPageBlock newPageBlock = new WebPageBlock()
                .withPageId(placement.getId())
                .withImpId(placement.getBlocks().get(0).getBlockId());

        AdGroupInfo cpmIndoorAdGroup = steps.adGroupSteps().createActiveCpmIndoorAdGroup(campaignInfo);
        Long cpmIndoorAdGroupId = cpmIndoorAdGroup.getAdGroupId();

        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmIndoorAdGroup(cpmIndoorAdGroupId, campaignInfo.getCampaignId(), newPageBlock);

        updateAdGroups(singletonList(webCpmAdGroup));

        AdGroup expected = new CpmIndoorAdGroup()
                .withType(AdGroupType.CPM_INDOOR)
                .withId(cpmIndoorAdGroupId)
                .withName(webCpmAdGroup.getName())
                .withPageBlocks(convertPageBlocks(webCpmAdGroup.getPageBlocks()));

        List<AdGroup> adGroups = findAdGroup(cpmIndoorAdGroupId);
        assertThat("группа обновилась корректно", adGroups.get(0),
                beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void cpmIndoorAdGroup_WrongRetargetings() {
        steps.placementSteps().clearPlacements();
        long pageId = 29342L;
        long blockId = 123L;
        IndoorPlacement placement = indoorPlacementWithBlock(pageId, indoorBlockWithOneSize(pageId, blockId));
        steps.placementSteps().addPlacement(placement);
        WebPageBlock newPageBlock = new WebPageBlock()
                .withPageId(placement.getId())
                .withImpId(placement.getBlocks().get(0).getBlockId());

        AdGroupInfo adGroup = steps.adGroupSteps().createActiveCpmIndoorAdGroup(campaignInfo);
        WebCpmAdGroupRetargeting retargeting = createRetargeting(adGroup);

        WebCpmAdGroup complexCpmAdGroup = randomNameWebCpmIndoorAdGroup(adGroup.getAdGroupId(),
                campaignInfo.getCampaignId(), newPageBlock)
                .withRetargetings(singletonList(retargeting));

        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(complexCpmAdGroup), campaignInfo.getCampaignId(),
                        false, false, false, null);
        assertFalse(webResponse.isSuccessful());
    }

    //cpm_yndx_frontpage
    @Test
    public void cpmYndxFrontpageAdGroupWithBannersUpdated() {
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultHtml5CreativeForFrontpage(clientInfo);
        CpmBannerInfo cpmBanner = steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(cpmYndxFrontpageAdGroup.getCampaignId(), cpmYndxFrontpageAdGroupId,
                        creativeInfo.getCreativeId())
                        .withTurboLandingId(bannerTurboLandings.get(1).getId())
                        .withTurboLandingStatusModerate(bannerTurboLandings.get(1).getStatusModerate()),
                cpmYndxFrontpageAdGroup);
        WebCpmBanner webBanner = webCpmBanner(cpmBanner.getBannerId(), creativeInfo.getCreativeId())
                .withTurbolanding(new WebBannerTurbolanding().withId(bannerTurboLandings.get(0).getId()));

        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmYndxFrontpageAdGroup(cpmYndxFrontpageAdGroupId, frontpageCampaignInfo.getCampaignId())
                        .withBanners(singletonList(webBanner));

        updateFrontpageAdGroups(singletonList(webCpmAdGroup));

        AdGroup expected = new CpmYndxFrontpageAdGroup()
                .withType(AdGroupType.CPM_YNDX_FRONTPAGE)
                .withId(cpmYndxFrontpageAdGroupId)
                .withName(webCpmAdGroup.getName());

        List<AdGroup> adGroups = findAdGroup(cpmYndxFrontpageAdGroupId);
        assertThat("группа обновилась корректно", adGroups.get(0),
                beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        List<OldBanner> banners = findOldBanners(cpmYndxFrontpageAdGroupId);
        assertThat("в группе должен быть один баннер", banners, hasSize(1));

        checkBanner(cpmYndxFrontpageAdGroupId, AdGroupType.CPM_YNDX_FRONTPAGE, webBanner);
    }

    //cpm_geoproduct
    @Test
    public void cpmGeoproductAdGroupWithBannersUpdated() {
        Long campaignId = campaignInfo.getCampaignId();
        long oldTurbolanding = steps.turboLandingSteps()
                .createTurboLanding(clientId, defaultBannerTurboLanding(clientId).withPreset(cpm_geoproduct_preset))
                .getId();
        long newTurbolanding = steps.turboLandingSteps()
                .createTurboLanding(clientId, defaultBannerTurboLanding(clientId).withPreset(cpm_geoproduct_preset))
                .getId();

        Long creativeId = steps.creativeSteps().getNextCreativeId();
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultHtml5CreativeForGeoproduct(clientInfo, creativeId);
        OldCpmBanner oldBanner = activeCpmBanner(campaignId, cpmGeoproductAdGroupId, creativeInfo.getCreativeId())
                .withTurboLandingId(oldTurbolanding)
                .withTurboLandingStatusModerate(READY);

        CpmBannerInfo cpmBanner = steps.bannerSteps().createActiveCpmBanner(oldBanner, cpmGeoproductAdGroup);

        WebCpmBanner webBanner = webCpmBanner(cpmBanner.getBannerId(), creativeInfo.getCreativeId())
                .withHref(null)
                .withTurbolanding(new WebBannerTurbolanding().withId(newTurbolanding))
                .withTnsId("12543");
        WebCpmAdGroup webCpmAdGroup = randomNameWebCpmGeoproductAdGroup(cpmGeoproductAdGroupId, campaignId)
                .withBanners(singletonList(webBanner));
        updateAdGroups(singletonList(webCpmAdGroup));

        AdGroup expected = new CpmGeoproductAdGroup()
                .withType(AdGroupType.CPM_GEOPRODUCT)
                .withId(cpmGeoproductAdGroupId)
                .withName(webCpmAdGroup.getName());

        List<AdGroup> adGroups = findAdGroup(cpmGeoproductAdGroupId);
        assertThat("группа обновилась корректно", adGroups.get(0),
                beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        List<OldBanner> banners = findOldBanners(cpmGeoproductAdGroupId);
        assertThat("в группе должен быть один баннер", banners, hasSize(1));

        OldCpmBanner expectedBanner = new OldCpmBanner()
                .withBannerType(OldBannerType.CPM_BANNER)
                .withPixels(mapList(webBanner.getPixels(), WebPixel::getUrl))
                .withTnsId(webBanner.getTnsId())
                .withTurboLandingId(newTurbolanding)
                .withTurboLandingStatusModerate(READY)
                .withCreativeId(Long.parseLong(webBanner.getCreative().getCreativeId()))
                .withIsMobile(false)
                .withHref(webBanner.getHref() == null ? null : webBanner.getUrlProtocol() + webBanner.getHref());

        assertThat("баннер обновился корректно", (OldCpmBanner) findOldBanners(cpmGeoproductAdGroupId).get(0),
                beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields()));
    }

    //проверка на то, что не обязательно присылать ставку, если фразы не изменились
    @Test
    public void updateOnlyAdGroupFieldsInAdGroupWithKeywords() {
        KeywordInfo keyword = steps.keywordSteps().createKeyword(cpmBannerUserKeywordsGroup, keywordForCpmBanner());

        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(keywordsAdGroupId, campaignInfo.getCampaignId())
                        .withKeywords(singletonList(new WebKeyword()
                                .withId(keyword.getId())
                                .withPhrase(keyword.getKeyword().getPhrase())));

        updateAdGroups(singletonList(webCpmAdGroup));
    }

    @Test
    public void updateRetargetingToPrivatePixelPermissionError() {
        // CREATE

        // create AdGroup
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(campaignWithForeignPlacement);

        // create Retargeting Condition
        RetConditionInfo retConditionInfo = steps.retConditionSteps()
                .createDefaultRetCondition(singletonList(publicGoal), campaignWithForeignPlacement.getClientInfo(),
                        ConditionType.interests,
                        RuleType.OR);

        // create Retargeting
        RetargetingInfo retargetingInfo =
                steps.retargetingSteps().createRetargeting(defaultRetargeting(), adGroupInfo, retConditionInfo);

        // create Banner
        CpmBannerInfo cpmBannerInfo = steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), creativeId)
                        .withPixels(singletonList(dcmPixelUrl())),
                adGroupInfo);


        // UPDATE

        WebRetargetingRule rule = new WebRetargetingRule()
                .withRuleType(RuleType.OR)
                .withGoals(singletonList(new WebRetargetingGoal()
                        .withId(privateGoal.getId())
                        .withGoalType(privateGoal.getType())
                        .withTime(privateGoal.getTime())));

        WebCpmAdGroupRetargeting retargetingForUpdate = new WebCpmAdGroupRetargeting()
                .withId(retargetingInfo.getRetargetingId())
                .withRetargetingConditionId(retConditionInfo.getRetConditionId())
                .withPriceContext(retargetingInfo.getRetargeting().getPriceContext().doubleValue())
                .withName(retConditionInfo.getRetCondition().getName())
                .withDescription(retConditionInfo.getRetCondition().getDescription())
                .withConditionType(retConditionInfo.getRetCondition().getType())
                .withGroups(singletonList(rule));

        WebCpmBanner bannerForUpdate = webCpmBanner(cpmBannerInfo.getBannerId(), creativeId)
                .withPixels(singletonList(new WebPixel()
                        .withKind(PixelKind.AUDIT)
                        .withUrl(dcmPixelUrl())));

        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(adGroupInfo.getAdGroupId(), campaignWithForeignPlacement.getCampaignId())
                        .withBanners(singletonList(bannerForUpdate))
                        .withRetargetings(singletonList(retargetingForUpdate));

        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(webCpmAdGroup), campaignWithForeignPlacement.getCampaignId(), false,
                        false, false, null);

        checkErrorResponse(webResponse,
                "[0]." + WebCpmAdGroup.Prop.BANNERS + "[0]." + WebCpmBanner.Prop.PIXELS + "[0]",
                BannerDefectIds.PixelPermissions.NO_RIGHTS_TO_PIXEL.getCode());
    }

    @Test
    public void updateRetargetingToPrivateWithPixelDeletionSuccess() {
        // CREATE

        // create AdGroup
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(campaignWithForeignPlacement);

        // create Retargeting Condition
        RetConditionInfo retConditionInfo = steps.retConditionSteps()
                .createDefaultRetCondition(singletonList(publicGoal), campaignWithForeignPlacement.getClientInfo(),
                        ConditionType.interests,
                        RuleType.OR);

        // create Retargeting
        RetargetingInfo retargetingInfo =
                steps.retargetingSteps().createRetargeting(defaultRetargeting(), adGroupInfo, retConditionInfo);

        // create Banner
        CpmBannerInfo cpmBannerInfo = steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), creativeId)
                        .withPixels(singletonList(dcmPixelUrl())),
                adGroupInfo);

        // UPDATE

        WebRetargetingRule rule = new WebRetargetingRule()
                .withRuleType(RuleType.OR)
                .withGoals(singletonList(new WebRetargetingGoal()
                        .withId(privateGoal.getId())
                        .withGoalType(privateGoal.getType())
                        .withTime(privateGoal.getTime())));

        WebCpmAdGroupRetargeting retargetingForUpdate = new WebCpmAdGroupRetargeting()
                .withId(retargetingInfo.getRetargetingId())
                .withRetargetingConditionId(retConditionInfo.getRetConditionId())
                .withPriceContext(retargetingInfo.getRetargeting().getPriceContext().doubleValue())
                .withName(retConditionInfo.getRetCondition().getName())
                .withDescription(retConditionInfo.getRetCondition().getDescription())
                .withConditionType(retConditionInfo.getRetCondition().getType())
                .withGroups(singletonList(rule));

        WebCpmBanner bannerForUpdate = webCpmBanner(cpmBannerInfo.getBannerId(), creativeId)
                .withPixels(null);

        WebCpmAdGroup webCpmAdGroup =
                randomNameWebCpmBannerAdGroup(adGroupInfo.getAdGroupId(), campaignWithForeignPlacement.getCampaignId())
                        .withBanners(singletonList(bannerForUpdate))
                        .withRetargetings(singletonList(retargetingForUpdate));

        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(webCpmAdGroup), campaignWithForeignPlacement.getCampaignId(), false,
                        false, false, null);
        checkResponse(webResponse);

        List<AdGroup> adGroups = findAdGroups(campaignWithForeignPlacement.getCampaignId());
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));

        checkBannerWithoutTurbo(adGroups.get(0).getId(), AdGroupType.CPM_BANNER, bannerForUpdate);
        checkRetargetings(webCpmAdGroup.getRetargetings(), adGroups.get(0).getId());
        checkRetargetingConditions(webCpmAdGroup.getRetargetings(), adGroups.get(0).getId());
    }

    private void updateFrontpageAdGroups(List<WebCpmAdGroup> webCpmAdGroups) {
        WebResponse webResponse = controller
                .saveCpmAdGroup(webCpmAdGroups, frontpageCampaignInfo.getCampaignId(), false, false, false, null);
        checkResponse(webResponse);
    }

    private void updateAdGroups(List<WebCpmAdGroup> webCpmAdGroups) {
        WebResponse webResponse = controller
                .saveCpmAdGroup(webCpmAdGroups, campaignInfo.getCampaignId(), false, false, false, null);
        checkResponse(webResponse);
    }

}
