package ru.yandex.direct.web.entity.adgroup.controller.cpm;

import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmGeoproductAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmIndoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmOutdoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerType;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.adgroup.converter.RetargetingConverter;
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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.adgroup.model.AdGroupType.CPM_GEOPRODUCT;
import static ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate.READY;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adfoxPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.dcmPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl;
import static ru.yandex.direct.core.testing.steps.TurboLandingSteps.defaultBannerTurboLanding;
import static ru.yandex.direct.dbschema.ppc.enums.TurbolandingsPreset.cpm_geoproduct_preset;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.web.entity.adgroup.converter.AdGroupConverterUtils.convertPageBlocks;
import static ru.yandex.direct.web.testing.data.TestAdGroups.defaultCpmAdGroupRetargeting;
import static ru.yandex.direct.web.testing.data.TestAdGroups.defaultCpmIndoorRetargeting;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmAudioAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmBannerAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmGeoproductAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmIndoorAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmOutdoorAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmVideoAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmYndxFrontpageAdGroup;
import static ru.yandex.direct.web.testing.data.TestBanners.webCpmBanner;
import static ru.yandex.direct.web.testing.data.TestBanners.webCpmIndoorBanner;
import static ru.yandex.direct.web.testing.data.TestBanners.webCpmOutdoorBanner;
import static ru.yandex.direct.web.testing.data.TestBidModifiers.cpmAdGroupDeviceBidModifiers;
import static ru.yandex.direct.web.testing.data.TestBidModifiers.cpmAdGroupWeatherBidModifiers;
import static ru.yandex.direct.web.testing.data.TestBidModifiers.fullCpmAdGroupBidModifiers;
import static ru.yandex.direct.web.testing.data.TestKeywords.randomPhraseKeyword;

@DirectWebTest
@RunWith(SpringRunner.class)
public class CpmAdGroupControllerAddTest extends CpmAdGroupControllerTestBase {

    private CampaignInfo campaignWithForeignPlacement;

    @Override
    public void before() {
        super.before();
        campaignWithForeignPlacement = createCpmDealCampaignWithInventory(singletonList(nonYandexOnlyPlacementDeal));
    }

    @Test
    public void cpmBannerAdGroupWithPublicRetargetingsAndDcmPixelSuccess() {
        WebCpmBanner webBanner = webCpmBanner(null, creativeId)
                .withPixels(singletonList(new WebPixel()
                        .withKind(PixelKind.AUDIT)
                        .withUrl(dcmPixelUrl())));

        WebRetargetingRule rule = new WebRetargetingRule()
                .withRuleType(RuleType.OR)
                .withGoals(singletonList(new WebRetargetingGoal()
                        .withId(publicGoal.getId())
                        .withGoalType(publicGoal.getType())
                        .withTime(publicGoal.getTime())));

        WebCpmAdGroupRetargeting retargeting = defaultCpmAdGroupRetargeting()
                .withGroups(singletonList(rule));

        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignWithForeignPlacement.getCampaignId())
                .withRetargetings(singletonList(retargeting))
                .withBanners(singletonList(webBanner));

        addAdGroups(singletonList(requestAdGroup), campaignWithForeignPlacement.getCampaignId());

        List<AdGroup> adGroups = findAdGroups(campaignWithForeignPlacement.getCampaignId());
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));

        checkBannerWithoutTurbo(adGroups.get(0).getId(), AdGroupType.CPM_BANNER, webBanner);
        checkRetargetings(requestAdGroup.getRetargetings(), adGroups.get(0).getId());
        checkRetargetingConditions(requestAdGroup.getRetargetings(), adGroups.get(0).getId());
    }

    @Test
    public void cpmBannerAdGroupWithPublicRetargetingsAndYaPixelError() {
        WebCpmBanner webBanner = webCpmBanner(null, creativeId)
                .withPixels(singletonList(new WebPixel()
                        .withKind(PixelKind.AUDIENCE)
                        .withUrl(yaAudiencePixelUrl())));

        WebRetargetingRule rule = new WebRetargetingRule()
                .withRuleType(RuleType.OR)
                .withGoals(singletonList(new WebRetargetingGoal()
                        .withId(publicGoal.getId())
                        .withGoalType(publicGoal.getType())
                        .withTime(publicGoal.getTime())));

        WebCpmAdGroupRetargeting retargeting = defaultCpmAdGroupRetargeting()
                .withGroups(singletonList(rule));

        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignWithForeignPlacement.getCampaignId())
                .withRetargetings(singletonList(retargeting))
                .withBanners(singletonList(webBanner));

        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(requestAdGroup), campaignWithForeignPlacement.getCampaignId(), true,
                        false, false, null);

        checkErrorResponse(webResponse,
                "[0]." + WebCpmAdGroup.Prop.BANNERS + "[0]." + WebCpmBanner.Prop.PIXELS + "[0]",
                BannerDefectIds.String.NO_RIGHTS_TO_AUDIENCE_PIXEL.getCode());
    }

    @Test
    public void cpmBannerAdGroupWithPrivateRetargetingsAndDcmPixelError() {
        WebCpmBanner webBanner = webCpmBanner(null, creativeId)
                .withPixels(singletonList(new WebPixel()
                        .withKind(PixelKind.AUDIT)
                        .withUrl(dcmPixelUrl())));

        WebRetargetingRule rule = new WebRetargetingRule()
                .withRuleType(RuleType.OR)
                .withGoals(singletonList(new WebRetargetingGoal()
                        .withId(privateGoal.getId())
                        .withGoalType(privateGoal.getType())
                        .withTime(privateGoal.getTime())));

        WebCpmAdGroupRetargeting retargeting = defaultCpmAdGroupRetargeting()
                .withGroups(singletonList(rule));

        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignWithForeignPlacement.getCampaignId())
                .withRetargetings(singletonList(retargeting))
                .withBanners(singletonList(webBanner));

        WebResponse webResponse = controller
                .saveCpmAdGroup(singletonList(requestAdGroup), campaignWithForeignPlacement.getCampaignId(), true,
                        false, false, null);

        checkErrorResponse(webResponse,
                "[0]." + WebCpmAdGroup.Prop.BANNERS + "[0]." + WebCpmBanner.Prop.PIXELS + "[0]",
                BannerDefectIds.PixelPermissions.NO_RIGHTS_TO_PIXEL.getCode());
    }

    @Test
    public void cpmBannerAdGroupWithPrivateRetargetingsAndAdfoxPixelSuccess() {
        WebCpmBanner webBanner = webCpmBanner(null, creativeId)
                .withPixels(singletonList(new WebPixel()
                        .withKind(PixelKind.AUDIT)
                        .withUrl(adfoxPixelUrl())));

        WebRetargetingRule rule = new WebRetargetingRule()
                .withRuleType(RuleType.OR)
                .withGoals(singletonList(new WebRetargetingGoal()
                        .withId(privateGoal.getId())
                        .withGoalType(privateGoal.getType())
                        .withTime(privateGoal.getTime())));

        WebCpmAdGroupRetargeting retargeting = defaultCpmAdGroupRetargeting()
                .withGroups(singletonList(rule));

        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignWithForeignPlacement.getCampaignId())
                .withRetargetings(singletonList(retargeting))
                .withBanners(singletonList(webBanner));

        addAdGroups(singletonList(requestAdGroup), campaignWithForeignPlacement.getCampaignId());

        List<AdGroup> adGroups = findAdGroups(campaignWithForeignPlacement.getCampaignId());
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));

        checkBannerWithoutTurbo(adGroups.get(0).getId(), AdGroupType.CPM_BANNER, webBanner);
        checkRetargetings(requestAdGroup.getRetargetings(), adGroups.get(0).getId());
        checkRetargetingConditions(requestAdGroup.getRetargetings(), adGroups.get(0).getId());
    }

    @Test
    public void emptyCpmBannerAdGroup() {
        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId());

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        assertThat("имя добавленной группы не совпадает с ожидаемым",
                adGroups.get(0).getName(), equalTo(requestAdGroup.getName()));
    }

    @Test
    public void cpmBannerAdGroupWithBanners() {
        WebCpmBanner webBanner = webCpmBanner(null, creativeId)
                .withTurbolanding(new WebBannerTurbolanding().withId(bannerTurboLandings.get(0).getId()));
        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withBanners(singletonList(webBanner));

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));

        checkBanner(adGroups.get(0).getId(), AdGroupType.CPM_BANNER, webBanner);
    }

    @Test
    public void cpmBannerAdGroupWithRetargetings() {
        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withRetargetings(singletonList(retargetingForAdd));

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));

        Long adGroupId = adGroups.get(0).getId();
        checkRetargetings(requestAdGroup.getRetargetings(), adGroupId);
        checkRetargetingConditions(requestAdGroup.getRetargetings(), adGroupId);
    }

    @Test
    public void cpmBannerAdGroupWithKeywords() {
        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withKeywords(singletonList(randomPhraseKeyword(null)));

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        checkKeywords(requestAdGroup.getKeywords());
    }

    @Test
    public void twoCpmBannerAdGroupsWithRetargetingsAndKeywords() {
        WebCpmAdGroup adGroupWithRetargeting = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withRetargetings(singletonList(retargetingForAdd));
        WebCpmAdGroup adGroupWithKeywords = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withKeywords(singletonList(randomPhraseKeyword(null)));

        addAdGroups(asList(adGroupWithRetargeting, adGroupWithKeywords));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлено две группы", adGroups, hasSize(2));
        checkKeywords(adGroupWithKeywords.getKeywords());
        checkRetargetings(adGroupWithRetargeting.getRetargetings(), adGroups.get(0).getId());
        checkRetargetingConditions(adGroupWithRetargeting.getRetargetings(), adGroups.get(0).getId());
    }

    @Test
    public void mobileBidModifierIsAddedInFullCpmBannerAdGroupWithKeywords() {
        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withKeywords(singletonList(randomPhraseKeyword(null)))
                .withBidModifiers(fullCpmAdGroupBidModifiers(retCondId));

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("должно быть добавлено 5 корректировок", bidModifiers, hasSize(5));

        checkMobileBidModifier(bidModifiers, adGroups.get(0).getId(),
                requestAdGroup.getBidModifiers().getMobileBidModifier());
    }

    @Test
    public void mobileIosBidModifierIsAddedInFullCpmBannerAdGroupWithKeywords() {
        WebAdGroupBidModifiers webCpmAdGroupBidModifiers = fullCpmAdGroupBidModifiers(retCondId);
        webCpmAdGroupBidModifiers.getMobileBidModifier().withOsType("ios");
        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withKeywords(singletonList(randomPhraseKeyword(null)))
                .withBidModifiers(webCpmAdGroupBidModifiers);

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("должно быть добавлено 5 корректировок", bidModifiers, hasSize(5));

        checkMobileBidModifier(bidModifiers, adGroups.get(0).getId(),
                requestAdGroup.getBidModifiers().getMobileBidModifier());
    }

    @Test
    public void desktopBidModifierIsAddedInFullCpmBannerAdGroupWithKeywords() {
        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withKeywords(singletonList(randomPhraseKeyword(null)))
                .withBidModifiers(fullCpmAdGroupBidModifiers(retCondId));

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("должно быть добавлено 5 корректировок", bidModifiers, hasSize(5));

        checkDesktopBidModifier(bidModifiers, adGroups.get(0).getId(),
                requestAdGroup.getBidModifiers().getDesktopBidModifier());
    }

    @Test
    public void mobileIosBidModifierIsAddedInFullCpmBannerAdGroupWithRetargetings() {
        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withRetargetings(singletonList(retargetingForAdd))
                .withBidModifiers(cpmAdGroupDeviceBidModifiers());

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("должно быть добавлено 2 корректировки", bidModifiers, hasSize(2));

        checkMobileBidModifier(bidModifiers, adGroups.get(0).getId(),
                requestAdGroup.getBidModifiers().getMobileBidModifier());
    }

    @Test
    public void weatherBidModifierIsAddedInFullCpmBannerAdGroupWithKeywords() {
        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withKeywords(singletonList(randomPhraseKeyword(null)))
                .withBidModifiers(fullCpmAdGroupBidModifiers(retCondId));

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("должно быть добавлено 5 корректировок", bidModifiers, hasSize(5));

        checkWeatherBidModifier(bidModifiers, adGroups.get(0).getId(),
                requestAdGroup.getBidModifiers().getWeatherBidModifier());
    }

    @Test
    public void weatherBidModifierIsAddedToCpmAudioAdGroup() {
        WebAdGroupBidModifiers webBidModifiers = cpmAdGroupWeatherBidModifiers();
        WebCpmAdGroup webCpmAdGroup = randomNameWebCpmAudioAdGroup(null, campaignInfo.getCampaignId())
                .withBidModifiers(webBidModifiers);

        addAdGroups(singletonList(webCpmAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("должна была добавиться 1 корректировка", bidModifiers, hasSize(1));

        checkWeatherBidModifier(bidModifiers, adGroups.get(0).getId(),
                webCpmAdGroup.getBidModifiers().getWeatherBidModifier());
    }

    @Test
    public void desktopBidModifierIsAddedInFullCpmBannerAdGroupWithRetargetings() {
        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withRetargetings(singletonList(retargetingForAdd))
                .withBidModifiers(cpmAdGroupDeviceBidModifiers());

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("должно быть добавлено 2 корректировки", bidModifiers, hasSize(2));

        checkDesktopBidModifier(bidModifiers, adGroups.get(0).getId(),
                requestAdGroup.getBidModifiers().getDesktopBidModifier());
    }

    @Test
    public void mobileIosBidModifierIsAddedInFullCpmVideoAdGroup() {
        WebCpmAdGroup requestAdGroup = randomNameWebCpmVideoAdGroup(null, campaignInfo.getCampaignId())
                .withRetargetings(singletonList(retargetingForAdd))
                .withBidModifiers(cpmAdGroupDeviceBidModifiers());

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("должно быть добавлено 2 корректировки", bidModifiers, hasSize(2));

        checkMobileBidModifier(bidModifiers, adGroups.get(0).getId(),
                requestAdGroup.getBidModifiers().getMobileBidModifier());
    }

    @Test
    public void desktopBidModifierIsAddedInFullCpmVideoAdGroup() {
        WebCpmAdGroup requestAdGroup = randomNameWebCpmVideoAdGroup(null, campaignInfo.getCampaignId())
                .withRetargetings(singletonList(retargetingForAdd))
                .withBidModifiers(cpmAdGroupDeviceBidModifiers());

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("должно быть добавлено 2 корректировки", bidModifiers, hasSize(2));

        checkDesktopBidModifier(bidModifiers, adGroups.get(0).getId(),
                requestAdGroup.getBidModifiers().getDesktopBidModifier());
    }

    @Test
    public void retargetingBidModifierIsAddedInFullCpmBannerAdGroupWithKeywords() {
        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withKeywords(singletonList(randomPhraseKeyword(null)))
                .withBidModifiers(fullCpmAdGroupBidModifiers(retCondId));

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("должно быть добавлено 5 корректировок", bidModifiers, hasSize(5));

        checkRetargetingBidModifier(bidModifiers, adGroups.get(0).getId(),
                requestAdGroup.getBidModifiers().getRetargetingBidModifier());
    }

    @Test
    public void demographyBidModifierIsAddedInFullCpmBannerAdGroupWithKeywords() {
        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withKeywords(singletonList(randomPhraseKeyword(null)))
                .withBidModifiers(fullCpmAdGroupBidModifiers(retCondId));

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("должно быть добавлено 5 корректировок", bidModifiers, hasSize(5));

        checkDemographyBidModifier(bidModifiers, adGroups.get(0).getId(),
                requestAdGroup.getBidModifiers().getDemographicsBidModifier());
    }

    @Test
    public void tagsAdded() {
        List<Long> campaignTags = steps.tagCampaignSteps()
                .createDefaultTags(campaignInfo.getShard(), campaignInfo.getClientId(), campaignInfo.getCampaignId(),
                        1);

        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withTags(singletonMap(campaignTags.get(0).toString(), 1));

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        List<Long> tags = findTags(adGroups.get(0).getId());
        assertThat(tags, containsInAnyOrder(campaignTags.toArray()));
    }

    @Test
    public void pageGroupTagsAdded() {
        List<String> pageGroupTags = asList("page_group_tag1", "page_group_tag2");

        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withPageGroupTags(pageGroupTags);

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        List<String> actualPageGroupTags = adGroups.get(0).getPageGroupTags();
        assertThat(actualPageGroupTags, containsInAnyOrder(pageGroupTags.toArray()));
    }

    @Test
    public void targetTagsAdded() {
        List<String> targetTags = asList("target_tag1", "target_tag2");

        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withTargetTags(targetTags);

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        List<String> actualTargetTags = adGroups.get(0).getTargetTags();
        assertThat(actualTargetTags, containsInAnyOrder(targetTags.toArray()));
    }

    //cpm_video
    @Test
    public void emptyCpmVideoAdGroup() {
        WebCpmAdGroup requestAdGroup = randomNameWebCpmVideoAdGroup(null, campaignInfo.getCampaignId());

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        assertThat("имя добавленной группы не совпадает с ожидаемым",
                adGroups.get(0).getName(), equalTo(requestAdGroup.getName()));
    }

    @Test
    public void fullCpmVideoAdGroup() {
        WebCpmAdGroup requestAdGroup = randomNameWebCpmVideoAdGroup(null, campaignInfo.getCampaignId())
                .withRetargetings(singletonList(retargetingForAdd));

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));

        checkRetargetings(requestAdGroup.getRetargetings(), adGroups.get(0).getId());
        checkRetargetingConditions(requestAdGroup.getRetargetings(), adGroups.get(0).getId());
    }

    //cpm_outdoor
    @Test
    public void fullCpmOutdoorAdGroup() {
        OutdoorPlacement placement = steps.placementSteps().addDefaultOutdoorPlacementWithOneBlock();
        WebPageBlock pageBlock = new WebPageBlock()
                .withPageId(placement.getId())
                .withImpId(placement.getBlocks().get(0).getBlockId());

        Long outdoorCreativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpmOutdoorVideoCreative(clientInfo, outdoorCreativeId);

        WebCpmAdGroup requestAdGroup = randomNameWebCpmOutdoorAdGroup(null, campaignInfo.getCampaignId(), pageBlock)
                .withBanners(singletonList(webCpmOutdoorBanner(null, outdoorCreativeId)))
                .withRetargetings(singletonList(retargetingForAdd));

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));

        AdGroup expected = new CpmOutdoorAdGroup()
                .withType(AdGroupType.CPM_OUTDOOR)
                .withName(requestAdGroup.getName())
                .withPageBlocks(convertPageBlocks(requestAdGroup.getPageBlocks()));
        assertThat(adGroups.get(0), beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        checkBanner(adGroups.get(0).getId(), AdGroupType.CPM_OUTDOOR, requestAdGroup.getBanners().get(0));
        checkRetargetings(requestAdGroup.getRetargetings(), adGroups.get(0).getId());
        checkRetargetingConditions(requestAdGroup.getRetargetings(), adGroups.get(0).getId());
    }


    //cpm_indoor
    @Test
    public void fullCpmIndoorAdGroup() {
        IndoorPlacement placement = steps.placementSteps().addDefaultIndoorPlacementWithOneBlock();
        WebPageBlock pageBlock = new WebPageBlock()
                .withPageId(placement.getId())
                .withImpId(placement.getBlocks().get(0).getBlockId());

        Long indoorCreativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpmIndoorVideoCreative(clientInfo, indoorCreativeId);

        WebCpmAdGroupRetargeting retargeting = defaultCpmIndoorRetargeting();
        List<WebRetargetingGoal> goals = StreamEx.of(retargeting.getGroups())
                .toFlatList(WebRetargetingRule::getGoals);
        List<Goal> coreGoals = mapList(goals, RetargetingConverter::webRetargetingGoalToCore);
        testCryptaSegmentRepository.addAll(coreGoals);
        WebCpmAdGroup requestAdGroup = randomNameWebCpmIndoorAdGroup(null, campaignInfo.getCampaignId(), pageBlock)
                .withBanners(singletonList(webCpmIndoorBanner(null, indoorCreativeId)))
                .withRetargetings(singletonList(retargeting));

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));

        AdGroup expected = new CpmIndoorAdGroup()
                .withType(AdGroupType.CPM_INDOOR)
                .withName(requestAdGroup.getName())
                .withPageBlocks(convertPageBlocks(requestAdGroup.getPageBlocks()));
        assertThat(adGroups.get(0), beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        checkBanner(adGroups.get(0).getId(), AdGroupType.CPM_INDOOR, requestAdGroup.getBanners().get(0));
        checkRetargetings(requestAdGroup.getRetargetings(), adGroups.get(0).getId());
        checkRetargetingConditions(requestAdGroup.getRetargetings(), adGroups.get(0).getId());
    }

    //cpm_yndx_frontpage
    @Test
    public void fullCpmYndxFrontpageAdGroup() {
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultHtml5CreativeForFrontpage(clientInfo);
        WebCpmAdGroupRetargeting retargetingForFrontpage = defaultCpmAdGroupRetargeting().withGroups(emptyList());
        WebCpmAdGroup requestAdGroup = randomNameWebCpmYndxFrontpageAdGroup(null, frontpageCampaignInfo.getCampaignId())
                .withBanners(singletonList(
                        webCpmBanner(null, creativeInfo.getCreativeId())
                                .withTurbolanding(
                                        new WebBannerTurbolanding().withId(bannerTurboLandings.get(0).getId()))))
                .withRetargetings(singletonList(retargetingForFrontpage));

        addFrontpageAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups(frontpageCampaignInfo.getCampaignId());
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));

        AdGroup expected = new CpmYndxFrontpageAdGroup()
                .withType(AdGroupType.CPM_YNDX_FRONTPAGE)
                .withName(requestAdGroup.getName())
                .withMinusKeywords(emptyList());
        assertThat(adGroups.get(0), beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        checkBanner(adGroups.get(0).getId(), AdGroupType.CPM_YNDX_FRONTPAGE, requestAdGroup.getBanners().get(0));
        checkRetargetings(requestAdGroup.getRetargetings(), adGroups.get(0).getId());
        checkRetargetingConditions(requestAdGroup.getRetargetings(), adGroups.get(0).getId());
    }

    //cpm_geoproduct
    @Test
    public void fullCpmGeoproductAdGroup() {
        Long campaignId = campaignInfo.getCampaignId();
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        long turbolandingId = steps.turboLandingSteps()
                .createTurboLanding(clientId, defaultBannerTurboLanding(clientId).withPreset(cpm_geoproduct_preset))
                .getId();
        steps.creativeSteps().addDefaultHtml5CreativeForGeoproduct(clientInfo, creativeId);
        WebCpmAdGroupRetargeting retargeting = defaultCpmAdGroupRetargeting().withGroups(emptyList());
        WebBannerTurbolanding turbolanding = new WebBannerTurbolanding().withId(turbolandingId);
        WebCpmBanner webCpmBanner = webCpmBanner(null, creativeId)
                .withHref(null)
                .withTurbolanding(turbolanding)
                .withTnsId("someTnsId");
        WebCpmAdGroup requestAdGroup = randomNameWebCpmGeoproductAdGroup(null, campaignId)
                .withBanners(singletonList(webCpmBanner))
                .withRetargetings(singletonList(retargeting));

        addAdGroups(singletonList(requestAdGroup), campaignId);

        List<AdGroup> adGroups = findAdGroups(campaignId);
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));

        AdGroup expected = new CpmGeoproductAdGroup()
                .withType(CPM_GEOPRODUCT)
                .withName(requestAdGroup.getName());
        assertThat(adGroups.get(0), beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));

        OldCpmBanner expectedBanner = new OldCpmBanner()
                .withBannerType(OldBannerType.CPM_BANNER)
                .withTnsId(webCpmBanner.getTnsId())
                .withPixels(mapList(webCpmBanner.getPixels(), WebPixel::getUrl))
                .withTurboLandingId(turbolandingId)
                .withTurboLandingStatusModerate(READY)
                .withCreativeId(Long.parseLong(webCpmBanner.getCreative().getCreativeId()))
                .withIsMobile(false)
                .withHref(webCpmBanner.getHref() == null
                        ? null
                        : webCpmBanner.getUrlProtocol() + webCpmBanner.getHref());

        assertThat("баннер обновился корректно", findOldBanners(adGroups.get(0).getId()).get(0),
                beanDiffer((OldBanner) expectedBanner).useCompareStrategy(onlyExpectedFields()));

        checkRetargetings(requestAdGroup.getRetargetings(), adGroups.get(0).getId());
        checkRetargetingConditions(requestAdGroup.getRetargetings(), adGroups.get(0).getId());
    }

    @Test
    public void addNoGeoproductIntoAdGroupsWithGeoproduct_Error() {
        steps.adGroupSteps().createActiveCpmGeoproductAdGroup(campaignInfo);
        WebCpmAdGroup requestAdGroup = randomNameWebCpmVideoAdGroup(null, campaignInfo.getCampaignId())
                .withRetargetings(singletonList(retargetingForAdd));
        addAndExpectError(requestAdGroup,
                "[0]." + AdGroup.TYPE.name(),
                AdGroupDefects.adGroupTypeNotSupported().defectId().getCode()
        );
    }

     @Test
    public void addNoGeoproductIntoAdGroupsWithoutGeoproduct_NoError() {
        steps.adGroupSteps().createActiveCpmBannerAdGroup(campaignInfo);
         Long campaignId = campaignInfo.getCampaignId();
         WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignId)
                .withRetargetings(singletonList(retargetingForAdd));
         addAdGroups(singletonList(requestAdGroup), campaignId);
         List<AdGroup> adGroups = findAdGroups(campaignId);
         assertThat(adGroups, hasSize(2));
    }

    @Test
    public void addGeoproductIntoAdGroupsWithGeoproduct_NoError() {
        steps.adGroupSteps().createActiveCpmGeoproductAdGroup(campaignInfo);
        Long campaignId = campaignInfo.getCampaignId();
        WebCpmAdGroup requestAdGroup = randomNameWebCpmGeoproductAdGroup(null, campaignId)
                .withRetargetings(singletonList(retargetingForAdd));
        addAdGroups(singletonList(requestAdGroup), campaignId);
        List<AdGroup> adGroups = findAdGroups(campaignId);
        assertThat(adGroups, hasSize(2));
    }

    private void addFrontpageAdGroups(List<WebCpmAdGroup> adGroups) {
        WebResponse webResponse =
                controller.saveCpmAdGroup(adGroups, frontpageCampaignInfo.getCampaignId(), true, false, false, null);
        checkResponse(webResponse);
    }

    private void addAdGroups(List<WebCpmAdGroup> adGroups) {
        addAdGroups(adGroups, campaignInfo.getCampaignId());
    }

    private void addAdGroups(List<WebCpmAdGroup> adGroups, Long campaignId) {
        WebResponse webResponse =
                controller.saveCpmAdGroup(adGroups, campaignId, true, false, false, null);
        checkResponse(webResponse);
    }
}
