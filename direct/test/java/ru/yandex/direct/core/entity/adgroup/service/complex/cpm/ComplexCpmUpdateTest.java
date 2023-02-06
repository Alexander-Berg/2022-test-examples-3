package ru.yandex.direct.core.entity.adgroup.service.complex.cpm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.adgroup.container.ComplexCpmAdGroup;
import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdShowType;
import ru.yandex.direct.core.entity.adgroup.model.CpmAudioAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmGeoproductAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.adgroup.model.UsersSegment;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupTestCommons;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupUpdateOperationFactory;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.CpmAudioBanner;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.CpmIndoorBanner;
import ru.yandex.direct.core.entity.banner.model.CpmOutdoorBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmAudioBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.type.pixels.InventoryType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.PriceRetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingUtils;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionAutoPriceParams;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionFixedAutoPrices;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupBidModifierInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmAudioBannerInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.CpmIndoorBannerInfo;
import ru.yandex.direct.core.testing.info.CpmOutdoorBannerInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.randomCpmPriceRetargeting;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.eitherKeywordsOrRetargetingsAllowed;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.keywordsNotAllowed;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.notAllowedValue;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.noRightsToPixel;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.requiredCreativesWithHtml5TypeOnly;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierAdjustment.PERCENT;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile.MOBILE_ADJUSTMENT;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.onlyStopWords;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.retargetingConditionIsInvalidForPricePackage;
import static ru.yandex.direct.core.entity.userssegments.service.validation.UsersSegmentDefects.goalTypeNotSupportedInAdGroup;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adfoxPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adriverPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmAudioBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBannerWithTurbolanding;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmIndoorBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmOutdoorBanner;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientDemographicsAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientDemographicsModifier;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmAudioAddition;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmVideoAddition;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestKeywords.keywordForCpmBanner;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.fullCpmBanner;
import static ru.yandex.direct.core.testing.data.TestPricePackages.AGE_18_24;
import static ru.yandex.direct.core.testing.data.TestPricePackages.AGE_25_34;
import static ru.yandex.direct.core.testing.data.TestPricePackages.AGE_35_44;
import static ru.yandex.direct.core.testing.data.TestPricePackages.MALE_CRYPTA_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestPricePackages.MID_INCOME_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.emptyTargetingsCustom;
import static ru.yandex.direct.core.testing.data.TestRegions.CENTRAL_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultIndoorRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.interestsRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.core.testing.steps.TurboLandingSteps.defaultBannerTurboLanding;
import static ru.yandex.direct.core.validation.defects.MoneyDefects.invalidValueCpmNotLessThan;
import static ru.yandex.direct.dbschema.ppc.enums.TurbolandingsPreset.cpm_geoproduct_preset;
import static ru.yandex.direct.regions.Region.REGION_TYPE_COUNTRY;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.NumberDefects.lessThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexCpmUpdateTest {

    @Autowired
    private ComplexAdGroupUpdateOperationFactory operationFactory;
    @Autowired
    private ClientService clientService;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private BannerTypedRepository bannerTypedRepository;
    @Autowired
    private CreativeRepository creativeRepository;
    @Autowired
    private GeoTreeFactory geoTreeFactory;
    private GeoTree geoTree;

    @Autowired
    private Steps steps;
    @Autowired
    private ComplexAdGroupTestCommons commonChecks;
    @Autowired
    private TestCpmYndxFrontpageRepository testCpmYndxFrontpageRepository;
    @Autowired
    private MetrikaHelperStub metrikaHelperStub;

    private ClientInfo clientInfo;
    private Long retConditionId;
    private ClientId clientId;
    private int shard;

    //комплексная cpm banner группа с одним баннером и одним ретаргетингом
    private ComplexCpmAdGroup complexCpmBannerUserProfileAdGroup;
    private AdGroupInfo cpmBannerUserProfileAdGroup;

    //комплексная cpm banner группа с одним баннером, одной фразой и корректировкой
    private ComplexCpmAdGroup complexCpmBannerKeywordsAdGroup;
    private AdGroupInfo cpmBannerKeywordsAdGroup;

    //комплексная cpm geoproduct группа с одним баннером и одним ретаргетингом
    private ComplexCpmAdGroup complexCpmGeoproductAdGroup;
    private AdGroupInfo cpmGeoproductAdGroup;

    //комплексная cpm video группа с одним баннером и одним ретаргетингом
    private ComplexCpmAdGroup complexCpmVideoAdGroup;
    private AdGroupInfo cpmVideoAdGroup;

    //комплексная cpm audio группа с одним баннером и одним ретаргетингом
    private ComplexCpmAdGroup complexCpmAudioAdGroup;
    private AdGroupInfo cpmAudioAdGroup;

    //комплексная cpm outdoor группа с баннером и ретаргетингом
    private ComplexCpmAdGroup complexCpmOutdoorAdGroup;
    private AdGroupInfo cpmOutdoorAdGroup;

    //комплексная cpm indoor группа с баннером и ретаргетингом
    private ComplexCpmAdGroup complexCpmIndoorAdGroup;
    private AdGroupInfo cpmIndoorAdGroup;

    private ComplexCpmAdGroup complexCpmYndxFrontpageAdGroup;
    private AdGroupInfo cpmYndxFrontpageAdGroup;

    private ComplexCpmAdGroup complexCpmYndxFrontpageAdGroupForPriceSales;
    private AdGroup cpmYndxFrontpageAdGroupForPriceSales;
    private Goal behaviorGoalForPriceSales;
    private PricePackage pricePackage;

    @Before
    public void before() {
        geoTree = geoTreeFactory.getGlobalGeoTree();
        testCpmYndxFrontpageRepository.fillMinBidsTestValues();

        clientInfo = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.RUB));
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        retConditionId = steps.retConditionSteps()
                .createRetCondition(
                        (RetargetingCondition) defaultRetCondition(null).withType(ConditionType.interests),
                        clientInfo
                ).getRetConditionId();

        fillCpmBannerUserProfileAdGroupData();
        fillCpmBannerWithKeywordsAdGroupData();
        fillCpmGeoproductAdGroupData();
        fillCpmAudioAdGroupData();
        fillCpmVideoAdGroupData();
        fillCpmOutdoorAdGroupData();
        fillCpmIndoorAdGroupData();
        fillCpmYndxFrontpageAdGroupData();
        fillCpmYndxFrontpageForPriceSalesAdGroupData();
        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                cpmYndxFrontpageAdGroup.getShard(),
                cpmYndxFrontpageAdGroup.getCampaignId(),
                singletonList(FrontpageCampaignShowType.FRONTPAGE));
    }

    private void fillCpmBannerUserProfileAdGroupData() {
        cpmBannerUserProfileAdGroup = steps.adGroupSteps().createActiveCpmBannerAdGroup(clientInfo);
        CreativeInfo canvasCreative = steps.creativeSteps().addDefaultCanvasCreative(clientInfo);
        CpmBannerInfo cpmBanner = steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(cpmBannerUserProfileAdGroup.getCampaignId(), cpmBannerUserProfileAdGroup.getAdGroupId(),
                        canvasCreative.getCreativeId()), cpmBannerUserProfileAdGroup);
        complexCpmBannerUserProfileAdGroup =
                createCpmBannerAdGroupForUpdate(cpmBannerUserProfileAdGroup.getAdGroup(),
                        cpmBanner.getBanner(), CriterionType.USER_PROFILE);

        RetargetingInfo retargeting =
                steps.retargetingSteps().createDefaultRetargeting(cpmBannerUserProfileAdGroup);
        complexCpmBannerUserProfileAdGroup
                .withTargetInterests(RetargetingUtils
                        .convertRetargetingsToTargetInterests(singletonList(retargeting.getRetargeting()),
                                emptyList()));
    }

    private void fillCpmBannerWithKeywordsAdGroupData() {
        cpmBannerKeywordsAdGroup = steps.adGroupSteps().createActiveCpmBannerAdGroup(clientInfo, CriterionType.KEYWORD);
        CreativeInfo canvasCreative = steps.creativeSteps().addDefaultCanvasCreative(clientInfo);
        CpmBannerInfo cpmBanner = steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(cpmBannerKeywordsAdGroup.getCampaignId(), cpmBannerKeywordsAdGroup.getAdGroupId(),
                        canvasCreative.getCreativeId()), cpmBannerKeywordsAdGroup);
        complexCpmBannerKeywordsAdGroup =
                createCpmBannerAdGroupForUpdate(cpmBannerKeywordsAdGroup.getAdGroup(),
                        cpmBanner.getBanner(), CriterionType.KEYWORD);

        KeywordInfo keywordInfo =
                steps.keywordSteps().createKeyword(cpmBannerKeywordsAdGroup, keywordForCpmBanner());
        AdGroupBidModifierInfo bidModifierInfo =
                steps.bidModifierSteps().createDefaultAdGroupBidModifierMobile(cpmBannerKeywordsAdGroup);
        ComplexBidModifier complexBidModifier = new ComplexBidModifier()
                .withMobileModifier((BidModifierMobile) bidModifierInfo.getBidModifier());

        complexCpmBannerKeywordsAdGroup
                .withKeywords(singletonList(keywordInfo.getKeyword()))
                .withComplexBidModifier(complexBidModifier);
    }

    private void fillCpmGeoproductAdGroupData() {
        cpmGeoproductAdGroup = steps.adGroupSteps().createActiveCpmGeoproductAdGroup(clientInfo);
        CreativeInfo creative = steps.creativeSteps().addDefaultHtml5CreativeForGeoproduct(clientInfo);

        Long turbolandingId = steps.turboLandingSteps()
                .createTurboLanding(clientId, defaultBannerTurboLanding(clientId).withPreset(cpm_geoproduct_preset))
                .getId();
        OldCpmBanner banner = activeCpmBannerWithTurbolanding(cpmGeoproductAdGroup.getCampaignId(),
                cpmGeoproductAdGroup.getAdGroupId(),
                creative.getCreativeId(),
                turbolandingId);

        CpmBannerInfo cpmGeoproductBanner = steps.bannerSteps().createActiveCpmBanner(banner,
                cpmGeoproductAdGroup);
        complexCpmGeoproductAdGroup =
                createCpmGeoproductAdGroupForUpdate(cpmGeoproductAdGroup.getAdGroup(), cpmGeoproductBanner.getBanner());

        RetargetingInfo retargeting =
                steps.retargetingSteps().createDefaultRetargeting(cpmGeoproductAdGroup);
        complexCpmGeoproductAdGroup
                .withTargetInterests(RetargetingUtils
                        .convertRetargetingsToTargetInterests(singletonList(retargeting.getRetargeting()),
                                emptyList()));
    }

    private void fillCpmVideoAdGroupData() {
        cpmVideoAdGroup = steps.adGroupSteps()
                .createActiveCpmVideoAdGroup(clientId, clientInfo.getUid(), clientInfo);
        Long videoCreativeId = steps.creativeSteps().getNextCreativeId();
        Creative creative = defaultCpmVideoAddition(clientId, videoCreativeId)
                .withWidth(100L)
                .withHeight(100L);
        creativeRepository.add(shard, singletonList(creative));

        CpmBannerInfo cpmBannerForCpmVideo = steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(cpmVideoAdGroup.getCampaignId(), cpmVideoAdGroup.getAdGroupId(), videoCreativeId)
                        .withPixels(singletonList(adfoxPixelUrl())), cpmVideoAdGroup);
        complexCpmVideoAdGroup =
                createCpmVideoAdGroupForUpdate(cpmVideoAdGroup.getAdGroup(), cpmBannerForCpmVideo.getBanner());

        RetargetingInfo retargeting =
                steps.retargetingSteps().createDefaultRetargeting(cpmVideoAdGroup);
        complexCpmVideoAdGroup
                .withTargetInterests(RetargetingUtils
                        .convertRetargetingsToTargetInterests(singletonList(retargeting.getRetargeting()),
                                emptyList()));
    }

    private void fillCpmAudioAdGroupData() {
        cpmAudioAdGroup = steps.adGroupSteps()
                .createActiveCpmAudioAdGroup(clientInfo);
        Long audioCreativeId = steps.creativeSteps().getNextCreativeId();
        Creative creative = defaultCpmAudioAddition(clientId, audioCreativeId)
                .withWidth(900L)
                .withHeight(900L);
        creativeRepository.add(shard, singletonList(creative));

        CpmAudioBannerInfo cpmBannerForCpmAudio = steps.bannerSteps().createActiveCpmAudioBanner(
                activeCpmAudioBanner(cpmAudioAdGroup.getCampaignId(), cpmAudioAdGroup.getAdGroupId(), audioCreativeId)
                        .withPixels(singletonList(adfoxPixelUrl())), cpmAudioAdGroup);
        complexCpmAudioAdGroup =
                createCpmAudioAdGroupForUpdate(cpmAudioAdGroup.getAdGroup(), cpmBannerForCpmAudio.getBanner());

        RetargetingInfo retargeting =
                steps.retargetingSteps().createDefaultRetargeting(cpmAudioAdGroup);
        complexCpmAudioAdGroup
                .withTargetInterests(RetargetingUtils
                        .convertRetargetingsToTargetInterests(singletonList(retargeting.getRetargeting()),
                                emptyList()));
    }

    private void fillCpmOutdoorAdGroupData() {
        cpmOutdoorAdGroup = steps.adGroupSteps().createActiveCpmOutdoorAdGroup(clientInfo);

        Long videoCreativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpmOutdoorVideoCreative(clientInfo, videoCreativeId);
        CpmOutdoorBannerInfo cpmOutdoorBanner = steps.bannerSteps().createActiveCpmOutdoorBanner(
                activeCpmOutdoorBanner(cpmOutdoorAdGroup.getCampaignId(), cpmOutdoorAdGroup.getAdGroupId(),
                        videoCreativeId), cpmOutdoorAdGroup);

        RetargetingInfo retargeting =
                steps.retargetingSteps().createDefaultRetargeting(cpmOutdoorAdGroup);

        var existingClientBanner = new CpmOutdoorBanner()
                .withId(cpmOutdoorBanner.getBannerId())
                .withHref(cpmOutdoorBanner.getBanner().getHref())
                .withDomain(cpmOutdoorBanner.getBanner().getDomain())
                .withCreativeId(cpmOutdoorBanner.getBanner().getCreativeId());

        complexCpmOutdoorAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(cpmOutdoorAdGroup.getAdGroup())
                .withBanners(singletonList(existingClientBanner))
                .withTargetInterests(RetargetingUtils
                        .convertRetargetingsToTargetInterests(singletonList(retargeting.getRetargeting()),
                                emptyList()));
    }

    private void fillCpmIndoorAdGroupData() {
        CampaignInfo cpmBannerCampaign = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        cpmIndoorAdGroup = steps.adGroupSteps().createActiveCpmIndoorAdGroup(cpmBannerCampaign);

        Long videoCreativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpmIndoorVideoCreative(clientInfo, videoCreativeId);
        CpmIndoorBannerInfo cpmIndoorBanner = steps.bannerSteps().createActiveCpmIndoorBanner(
                activeCpmIndoorBanner(cpmIndoorAdGroup.getCampaignId(), cpmIndoorAdGroup.getAdGroupId(),
                        videoCreativeId), cpmIndoorAdGroup);

        RetargetingCondition retCondition =
                steps.retConditionSteps().createIndoorRetCondition(clientInfo).getRetCondition();

        var existingClientBanner = new CpmIndoorBanner()
                .withId(cpmIndoorBanner.getBannerId())
                .withHref(cpmIndoorBanner.getBanner().getHref())
                .withDomain(cpmIndoorBanner.getBanner().getDomain())
                .withCreativeId(cpmIndoorBanner.getBanner().getCreativeId());

        AdGroupBidModifierInfo bidModifierInfo =
                steps.bidModifierSteps().createDefaultAdGroupBidModifierDemographics(cpmIndoorAdGroup);
        ComplexBidModifier complexBidModifier = new ComplexBidModifier()
                .withDemographyModifier((BidModifierDemographics) bidModifierInfo.getBidModifier());

        complexCpmIndoorAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(cpmIndoorAdGroup.getAdGroup())
                .withBanners(singletonList(existingClientBanner))
                .withRetargetingConditions(singletonList(retCondition))
                .withTargetInterests(singletonList(randomCpmPriceRetargeting(retCondition.getId())))
                .withComplexBidModifier(complexBidModifier);
    }

    private void fillCpmYndxFrontpageAdGroupData() {
        cpmYndxFrontpageAdGroup = steps.adGroupSteps().createDefaultCpmYndxFrontpageAdGroup(clientInfo);
        CreativeInfo html5Creative = steps.creativeSteps().addDefaultHtml5CreativeForFrontpage(clientInfo,
                steps.creativeSteps().getNextCreativeId());

        CpmBannerInfo cpmBanner = steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(cpmYndxFrontpageAdGroup.getCampaignId(), cpmYndxFrontpageAdGroup.getAdGroupId(),
                        html5Creative.getCreativeId()), cpmYndxFrontpageAdGroup);

        AdGroup adGroupForUpdate = new CpmYndxFrontpageAdGroup()
                .withId(cpmYndxFrontpageAdGroup.getAdGroup().getId())
                .withType(cpmYndxFrontpageAdGroup.getAdGroup().getType())
                .withName(cpmYndxFrontpageAdGroup.getAdGroup().getName())
                .withGeo(cpmYndxFrontpageAdGroup.getAdGroup().getGeo())
                .withMinusKeywords(cpmYndxFrontpageAdGroup.getAdGroup().getMinusKeywords())
                .withTags(cpmYndxFrontpageAdGroup.getAdGroup().getTags());
        var bannerForUpdate = createBannerForUpdate(cpmBanner.getBanner());

        complexCpmYndxFrontpageAdGroup = new ComplexCpmAdGroup()
                .withAdGroup(adGroupForUpdate)
                .withBanners(singletonList(bannerForUpdate));
    }

    private void fillCpmYndxFrontpageForPriceSalesAdGroupData() {
        behaviorGoalForPriceSales = defaultGoalByType(GoalType.BEHAVIORS);
        pricePackage = approvedPricePackage()
                .withTargetingsCustom(emptyTargetingsCustom()
                        .withRetargetingCondition(new PriceRetargetingCondition().withCryptaSegments(
                            List.of(AGE_18_24, AGE_25_34, AGE_35_44, MALE_CRYPTA_GOAL_ID, MID_INCOME_GOAL_ID,
                                    behaviorGoalForPriceSales.getId())
                )))
                .withClients(List.of(allowedPricePackageClient(clientInfo)));
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA))
                .withGeoType(REGION_TYPE_COUNTRY);
        steps.pricePackageSteps().createPricePackage(pricePackage);

        var cpmPriceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
        cpmYndxFrontpageAdGroupForPriceSales = steps.adGroupSteps().createDefaultAdGroupForPriceSales(cpmPriceCampaign,
                clientInfo);
        CreativeInfo html5Creative = steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(clientInfo,
                cpmPriceCampaign);

        AdGroup adGroupForUpdate = new CpmYndxFrontpageAdGroup()
                .withId(cpmYndxFrontpageAdGroupForPriceSales.getId())
                .withType(cpmYndxFrontpageAdGroupForPriceSales.getType())
                .withName(cpmYndxFrontpageAdGroupForPriceSales.getName())
                .withGeo(cpmYndxFrontpageAdGroupForPriceSales.getGeo())
                .withMinusKeywords(cpmYndxFrontpageAdGroupForPriceSales.getMinusKeywords())
                .withTags(cpmYndxFrontpageAdGroupForPriceSales.getTags());

        Long campaignId = cpmYndxFrontpageAdGroupForPriceSales.getCampaignId();
        var cpmBanner = activeCpmBanner(campaignId,
                cpmYndxFrontpageAdGroupForPriceSales.getId(),
                html5Creative.getCreativeId());
        steps.bannerSteps().createActiveCpmBannerRaw(shard, cpmBanner, cpmYndxFrontpageAdGroupForPriceSales);
        var bannerForUpdate = createBannerForUpdate(cpmBanner);

        RetConditionInfo retConditionInfo = steps.retConditionSteps().createRetCondition(
                interestsRetCondition(clientInfo.getClientId(), List.of(behaviorGoalForPriceSales)), clientInfo);
        Retargeting retargeting =
                defaultRetargeting(campaignId, adGroupForUpdate.getId(), retConditionInfo.getRetConditionId())
                        .withAutobudgetPriority(null)
                        .withPriceContext(pricePackage.getPrice());
        steps.retargetingSteps().createRetargetingRaw(shard, retargeting, retConditionInfo);

        complexCpmYndxFrontpageAdGroupForPriceSales = new ComplexCpmAdGroup()
                .withAdGroup(adGroupForUpdate)
                .withBanners(singletonList(bannerForUpdate))
                .withTargetInterests(RetargetingUtils
                        .convertRetargetingsToTargetInterests(singletonList(retargeting),
                                emptyList()))
                .withRetargetingConditions(singletonList(retConditionInfo.getRetCondition()));
    }

    private ComplexCpmAdGroup createCpmBannerAdGroupForUpdate(AdGroup adGroup, OldCpmBanner cpmBanner,
                                                              CriterionType criterionType) {
        AdGroup adGroupForUpdate = new CpmBannerAdGroup()
                .withId(adGroup.getId())
                .withType(adGroup.getType())
                .withName(adGroup.getName())
                .withGeo(adGroup.getGeo())
                .withMinusKeywords(adGroup.getMinusKeywords())
                .withTags(adGroup.getTags())
                .withCriterionType(criterionType);
        var bannerForUpdate = createBannerForUpdate(cpmBanner);

        return new ComplexCpmAdGroup()
                .withAdGroup(adGroupForUpdate)
                .withBanners(singletonList(bannerForUpdate));
    }

    private ComplexCpmAdGroup createCpmGeoproductAdGroupForUpdate(AdGroup adGroup, OldCpmBanner cpmBanner) {
        AdGroup adGroupForUpdate = new CpmGeoproductAdGroup()
                .withId(adGroup.getId())
                .withType(adGroup.getType())
                .withName(adGroup.getName())
                .withGeo(adGroup.getGeo())
                .withMinusKeywords(adGroup.getMinusKeywords())
                .withTags(adGroup.getTags());
        var bannerForUpdate = createBannerForUpdate(cpmBanner)
                .withTurboLandingId(cpmBanner.getTurboLandingId());

        return new ComplexCpmAdGroup()
                .withAdGroup(adGroupForUpdate)
                .withBanners(singletonList(bannerForUpdate));
    }

    private ComplexCpmAdGroup createCpmVideoAdGroupForUpdate(AdGroup adGroup, OldCpmBanner cpmBanner) {
        AdGroup adGroupForUpdate = new CpmVideoAdGroup()
                .withId(adGroup.getId())
                .withType(adGroup.getType())
                .withName(adGroup.getName())
                .withGeo(adGroup.getGeo())
                .withMinusKeywords(adGroup.getMinusKeywords())
                .withTags(adGroup.getTags());
        var bannerForUpdate = createBannerForUpdate(cpmBanner);

        return new ComplexCpmAdGroup()
                .withAdGroup(adGroupForUpdate)
                .withBanners(singletonList(bannerForUpdate));
    }

    private ComplexCpmAdGroup createCpmAudioAdGroupForUpdate(AdGroup adGroup, OldCpmAudioBanner cpmBanner) {
        AdGroup adGroupForUpdate = new CpmAudioAdGroup()
                .withId(adGroup.getId())
                .withType(adGroup.getType())
                .withName(adGroup.getName())
                .withGeo(adGroup.getGeo())
                .withTags(adGroup.getTags());
        var bannerForUpdate = createBannerForUpdate(cpmBanner);

        return new ComplexCpmAdGroup()
                .withAdGroup(adGroupForUpdate)
                .withBanners(singletonList(bannerForUpdate));
    }

    private CpmBanner createBannerForUpdate(OldCpmBanner cpmBanner) {
        return new CpmBanner()
                .withId(cpmBanner.getId())
                .withCreativeId(cpmBanner.getCreativeId())
                .withHref(cpmBanner.getHref())
                .withPixels(cpmBanner.getPixels());
    }

    private CpmAudioBanner createBannerForUpdate(OldCpmAudioBanner cpmBanner) {
        return new CpmAudioBanner()
                .withId(cpmBanner.getId())
                .withCreativeId(cpmBanner.getCreativeId())
                .withHref(cpmBanner.getHref())
                .withPixels(cpmBanner.getPixels());
    }

    @Test
    public void updateCpmBannerAdGroupAndBanner() {
        complexCpmBannerUserProfileAdGroup.getAdGroup().withName("new name");
        ((BannerWithHref) complexCpmBannerUserProfileAdGroup.getBanners().get(0)).setHref("http://anoter.ru");

        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmBannerUserProfileAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexCpmBannerUserProfileAdGroup);
    }

    @Test
    public void addBannerToCpmBannerAdGroup() {
        Long creativeId = steps.creativeSteps().addDefaultCanvasCreative(clientInfo).getCreativeId();
        List<BannerWithSystemFields> banners = new ArrayList<>(complexCpmBannerUserProfileAdGroup.getBanners());
        banners.add(fullCpmBanner(null, null, creativeId));
        complexCpmBannerUserProfileAdGroup.setBanners(banners);

        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmBannerUserProfileAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexCpmBannerUserProfileAdGroup);
    }

    @Test
    public void updateKeywordAndBidModifier() {
        complexCpmBannerKeywordsAdGroup.getKeywords().get(0)
                .withPhrase("another phrase");
        complexCpmBannerKeywordsAdGroup.getComplexBidModifier().getMobileModifier().getMobileAdjustment()
                .withPercent(120);

        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmBannerKeywordsAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        commonChecks.checkKeywords(
                complexCpmBannerKeywordsAdGroup.getKeywords(),
                complexCpmBannerKeywordsAdGroup.getAdGroup().getId(), shard);
        commonChecks.checkBidModifiers(complexCpmBannerKeywordsAdGroup.getComplexBidModifier(),
                cpmBannerKeywordsAdGroup.getAdGroupId(), cpmBannerKeywordsAdGroup.getCampaignId(), shard);
    }

    @Test
    public void updateTwoAdGroupsWithKeywordsAndRetargetings() {
        complexCpmBannerKeywordsAdGroup.getKeywords().get(0)
                .withPhrase("another phrase");
        complexCpmBannerKeywordsAdGroup.getComplexBidModifier().getMobileModifier().getMobileAdjustment()
                .withPercent(120);
        complexCpmBannerUserProfileAdGroup.getTargetInterests().get(0)
                .withRetargetingConditionId(retConditionId);

        ComplexCpmAdGroupUpdateOperation operation =
                createOperation(asList(complexCpmBannerKeywordsAdGroup, complexCpmBannerUserProfileAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        commonChecks.checkKeywords(
                complexCpmBannerKeywordsAdGroup.getKeywords(),
                complexCpmBannerKeywordsAdGroup.getAdGroup().getId(), shard);
        commonChecks.checkBidModifiers(complexCpmBannerKeywordsAdGroup.getComplexBidModifier(),
                cpmBannerKeywordsAdGroup.getAdGroupId(), cpmBannerKeywordsAdGroup.getCampaignId(), shard);
        commonChecks.checkRetargetings(
                complexCpmBannerUserProfileAdGroup.getTargetInterests(),
                complexCpmBannerUserProfileAdGroup.getAdGroup().getId(), clientId, shard);
    }

    @Test
    public void updateTwoAdGroupsWithDifferentAdGroupTypes() {
        ComplexCpmAdGroupUpdateOperation operation =
                createOperation(asList(complexCpmBannerKeywordsAdGroup, complexCpmAudioAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void addKeywordAndBidModifiersToCpmBannerAdGroup() {
        List<Keyword> keywords = new ArrayList<>(complexCpmBannerKeywordsAdGroup.getKeywords());
        keywords.add(keywordForCpmBanner());
        complexCpmBannerKeywordsAdGroup.withKeywords(keywords);

        complexCpmBannerKeywordsAdGroup.getComplexBidModifier()
                .withDemographyModifier(createEmptyClientDemographicsModifier()
                        .withDemographicsAdjustments(createDefaultClientDemographicsAdjustments()));

        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmBannerKeywordsAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        commonChecks.checkKeywords(
                complexCpmBannerKeywordsAdGroup.getKeywords(),
                complexCpmBannerKeywordsAdGroup.getAdGroup().getId(), shard);
        commonChecks.checkBidModifiers(complexCpmBannerKeywordsAdGroup.getComplexBidModifier(),
                cpmBannerKeywordsAdGroup.getAdGroupId(), cpmBannerKeywordsAdGroup.getCampaignId(), shard);
    }

    @Test
    public void updateRetargetingInCpmBannerAdGroup() {
        complexCpmBannerUserProfileAdGroup.getTargetInterests().get(0)
                .withRetargetingConditionId(retConditionId);

        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmBannerUserProfileAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        commonChecks.checkRetargetings(
                complexCpmBannerUserProfileAdGroup.getTargetInterests(),
                complexCpmBannerUserProfileAdGroup.getAdGroup().getId(), clientId, shard);
    }

    @Test
    public void updateRetargetingInCpmGeoproductAdGroup_SuccessTest() {
        complexCpmGeoproductAdGroup.getTargetInterests().get(0)
                .withRetargetingConditionId(retConditionId);

        ComplexCpmAdGroupUpdateOperation operation =
                createOperation(singletonList(complexCpmGeoproductAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        commonChecks.checkRetargetings(
                complexCpmGeoproductAdGroup.getTargetInterests(),
                complexCpmGeoproductAdGroup.getAdGroup().getId(), clientId, shard);
    }

    @Test
    public void setHrefInCpmGeoproductAdGroup_ValidationError() {
        ((BannerWithHref) complexCpmGeoproductAdGroup.getBanners().get(0)).withHref("http://yandex.ru");

        ComplexCpmAdGroupUpdateOperation operation =
                createOperation(singletonList(complexCpmGeoproductAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        MatcherAssert.assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(
                path(index(0), field(AdGroup.BANNERS), index(0), field(CpmBanner.HREF)),
                isNull())));
    }

    @Test
    public void updateRetargetingInCpmVideoAdGroup() {
        complexCpmVideoAdGroup.getTargetInterests().get(0)
                .withRetargetingConditionId(retConditionId);

        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmVideoAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        commonChecks.checkRetargetings(complexCpmVideoAdGroup.getTargetInterests(),
                complexCpmVideoAdGroup.getAdGroup().getId(), clientId, shard);
    }

    @Test
    public void priceIsNotChangedWhenFixedPriceIsNotSet() {
        RetargetingInfo defaultRetargeting =
                steps.retargetingSteps().createDefaultRetargeting(cpmBannerUserProfileAdGroup);

        RetConditionInfo newRetCondition =
                steps.retConditionSteps().createDefaultRetCondition(cpmBannerUserProfileAdGroup.getClientInfo());

        complexCpmBannerUserProfileAdGroup.withTargetInterests(singletonList(new TargetInterest()
                .withId(defaultRetargeting.getRetargetingId())
                .withRetargetingConditionId(newRetCondition.getRetConditionId())
                .withPriceContext(defaultRetargeting.getRetargeting().getPriceContext().add(BigDecimal.ONE))));

        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmBannerUserProfileAdGroup));

        //old price expected
        complexCpmBannerUserProfileAdGroup.getTargetInterests().get(0)
                .withPriceContext(defaultRetargeting.getRetargeting().getPriceContext());

        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        commonChecks.checkRetargetings(complexCpmBannerUserProfileAdGroup.getTargetInterests(),
                complexCpmBannerUserProfileAdGroup.getAdGroup().getId(), clientInfo.getClientId(),
                clientInfo.getShard());
    }

    @Test
    public void updateCpmOutdoorAdGroup() {
        complexCpmOutdoorAdGroup.getAdGroup().withName("New name");

        Long videoCreativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpmOutdoorVideoCreative(clientInfo, videoCreativeId);
        BannerWithSystemFields newBanner = new CpmOutdoorBanner()
                .withHref("https://www.yandex.ru")
                .withCreativeId(videoCreativeId);

        var newBanners = new ArrayList<>(complexCpmOutdoorAdGroup.getBanners());
        newBanners.add(newBanner);
        complexCpmOutdoorAdGroup.withBanners(newBanners);
        complexCpmOutdoorAdGroup.getTargetInterests().get(0)
                .withRetargetingConditionId(retConditionId);

        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmOutdoorAdGroup));
        MassResult<Long> result = operation.prepareAndApply();

        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexCpmOutdoorAdGroup);
        commonChecks.checkRetargetings(complexCpmOutdoorAdGroup.getTargetInterests(),
                complexCpmOutdoorAdGroup.getAdGroup().getId(), clientId, shard);
    }

    @Test
    public void updateCpmOutdoorAdGroup_GeoAndMinusKeywordsNotUpdated() {
        List<Long> oldGeo = complexCpmOutdoorAdGroup.getAdGroup().getGeo();
        List<String> oldMinusKeywords = complexCpmOutdoorAdGroup.getAdGroup().getMinusKeywords();
        complexCpmOutdoorAdGroup.getAdGroup()
                .withGeo(asList(1L, 2L))
                .withMinusKeywords(asList("word 2", "word 2"));

        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmOutdoorAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());

        AdGroup adGroup =
                adGroupRepository.getAdGroups(shard, singletonList(cpmOutdoorAdGroup.getAdGroup().getId())).get(0);
        assertThat("geo не должно было измениться", adGroup.getGeo(), beanDiffer(oldGeo));
        assertThat("минус фразы не должны были измениться", adGroup.getMinusKeywords(), beanDiffer(oldMinusKeywords));
    }

    @Test
    public void updateCpmIndoorAdGroup() {
        complexCpmIndoorAdGroup.getAdGroup().withName("New name");

        Long videoCreativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpmIndoorVideoCreative(clientInfo, videoCreativeId);
        CpmIndoorBanner newBanner = new CpmIndoorBanner()
                .withHref("https://www.yandex.ru")
                .withCreativeId(videoCreativeId);

        var newBanners = new ArrayList<>(complexCpmIndoorAdGroup.getBanners());
        newBanners.add(newBanner);
        complexCpmIndoorAdGroup.withBanners(newBanners);
        Long retConditionId = steps.retConditionSteps()
                .createRetCondition(
                        defaultIndoorRetCondition(),
                        clientInfo
                ).getRetConditionId();
        complexCpmIndoorAdGroup.getTargetInterests().get(0).withRetargetingConditionId(retConditionId);
        complexCpmIndoorAdGroup.getComplexBidModifier().getDemographyModifier().getDemographicsAdjustments().get(0).withPercent(10);

        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmIndoorAdGroup));
        MassResult<Long> result = operation.prepareAndApply();

        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexCpmIndoorAdGroup);
        commonChecks.checkRetargetings(complexCpmIndoorAdGroup.getTargetInterests(),
                complexCpmIndoorAdGroup.getAdGroup().getId(), clientId, shard);
    }

    @Test
    public void updateCpmIndoorAdGroup_GeoAndMinusKeywordsNotUpdated() {
        List<Long> oldGeo = complexCpmIndoorAdGroup.getAdGroup().getGeo();
        List<String> oldMinusKeywords = complexCpmIndoorAdGroup.getAdGroup().getMinusKeywords();
        complexCpmIndoorAdGroup.getAdGroup()
                .withGeo(asList(1L, 2L))
                .withMinusKeywords(asList("word 2", "word 2"));

        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmIndoorAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());

        AdGroup adGroup =
                adGroupRepository.getAdGroups(shard, singletonList(cpmIndoorAdGroup.getAdGroup().getId())).get(0);
        assertThat("geo не должно было измениться", adGroup.getGeo(), beanDiffer(oldGeo));
        assertThat("минус фразы не должны были измениться", adGroup.getMinusKeywords(), beanDiffer(oldMinusKeywords));
    }

    @Test
    public void updateCpmIndoorAdGroup_WithWrongRetargeting_ValidationError() {

        RetConditionInfo retConditionInfo2 = steps.retConditionSteps().createCpmRetCondition(clientInfo);
        complexCpmIndoorAdGroup.getRetargetingConditions().get(0).setRules(retConditionInfo2.getRetCondition().getRules());

        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmIndoorAdGroup));
        MassResult<Long> result = operation.prepareAndApply();

        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), notAllowedValue())));
    }

    @Test
    public void addKeywordsToCpmBannerUserProfileAdGroup_ValidationError() {
        complexCpmBannerUserProfileAdGroup
                .withKeywords(singletonList(keywordForCpmBanner()));

        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmBannerUserProfileAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat("в группу c  ретаргетингами нельзя добавить ключевые фразы",
                result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), eitherKeywordsOrRetargetingsAllowed())));
    }

    @Test
    public void addKeywordsToCpmGeoproductAdGroup_ValidationError() {
        complexCpmGeoproductAdGroup
                .withKeywords(singletonList(keywordForCpmBanner()));

        ComplexCpmAdGroupUpdateOperation operation =
                createOperation(singletonList(complexCpmGeoproductAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat("в группу c  ретаргетингами нельзя добавить ключевые фразы",
                result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), keywordsNotAllowed())));
    }

    @Test
    public void addRetargetingsToCpmBannerKeywordsAdGroup_ValidationError() {
        RetargetingInfo retargeting =
                steps.retargetingSteps().createDefaultRetargeting(cpmBannerKeywordsAdGroup);
        complexCpmBannerKeywordsAdGroup
                .withTargetInterests(RetargetingUtils
                        .convertRetargetingsToTargetInterests(singletonList(retargeting.getRetargeting()),
                                emptyList()));

        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmBannerKeywordsAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat("в группу с ключевыми фразами нельзя добавить ретаргетинги",
                result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), eitherKeywordsOrRetargetingsAllowed())));
    }

    @Test
    public void adGroupWithBanner_ValidationError() {
        List<BannerWithSystemFields> banners = new ArrayList<>();
        banners.add(
                fullCpmBanner(cpmBannerUserProfileAdGroup.getCampaignId(), cpmBannerUserProfileAdGroup.getAdGroupId(),
                        null));
        banners.addAll(complexCpmBannerUserProfileAdGroup.getBanners());
        complexCpmBannerUserProfileAdGroup.setBanners(banners);

        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmBannerUserProfileAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        Path errPath = path(index(0), field(ComplexCpmAdGroup.BANNERS), index(0), field(CpmBanner.CREATIVE_ID));
        assertThat("баннер должен быть с ошибкой валидации", result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, notNull())));
    }

    @Test
    public void adGroupWithKeyword_ValidationError() {
        complexCpmBannerKeywordsAdGroup.getKeywords().get(0)
                .withPhrase("к с и");

        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmBannerKeywordsAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        Path errPath = path(index(0), field(ComplexCpmAdGroup.KEYWORDS), index(0), field(Keyword.PHRASE));
        assertThat("ключевая фраза должна быть с ошибкой валидации", result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, onlyStopWords())));
    }

    @Test
    public void adGroupWithRetargeting_ValidationError() {
        List<ComplexCpmAdGroup> adGroupsForUpdate = singletonList(complexCpmBannerUserProfileAdGroup);
        ShowConditionAutoPriceParams showConditionAutoPriceParams = new ShowConditionAutoPriceParams(
                ShowConditionFixedAutoPrices.ofGlobalFixedPrice(BigDecimal.ZERO),
                keywordRequests -> emptyMap());
        ComplexCpmAdGroupUpdateOperation operation =
                operationFactory.createCpmAdGroupUpdateOperation(adGroupsForUpdate, geoTree, true,
                        showConditionAutoPriceParams, clientInfo.getUid(), clientId, clientInfo.getUid(), true);

        MassResult<Long> result = operation.prepareAndApply();
        Path errPath =
                path(index(0), field(ComplexCpmAdGroup.TARGET_INTERESTS), index(0), field(Keyword.PRICE_CONTEXT));

        Currency currency = clientService.getWorkCurrency(clientId);
        assertThat("ретаргетинг должен быть с ошибкой валидации", result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath,
                        invalidValueCpmNotLessThan(Money.valueOf(currency.getMinCpmPrice(), currency.getCode())))));
    }

    @Test
    public void adGroupWithBidModifier_ValidationError() {
        complexCpmBannerKeywordsAdGroup.getComplexBidModifier().getMobileModifier()
                .getMobileAdjustment().withPercent(400_000);

        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmBannerKeywordsAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        Path errPath = path(index(0), field(ComplexTextAdGroup.COMPLEX_BID_MODIFIER.name()),
                field(ComplexBidModifier.MOBILE_MODIFIER), field(MOBILE_ADJUSTMENT), field(PERCENT));
        assertThat("корректировка должна быть с ошибкой валидации", result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, lessThanOrEqualTo(1300))));
    }

    @Test
    public void updateCpmOutdoorAddKeywords_ValidationError() {
        complexCpmOutdoorAdGroup.withKeywords(singletonList(keywordForCpmBanner()));
        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmOutdoorAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), keywordsNotAllowed())));
    }

    @Test
    public void updateCpmOutdoorAdGroupWithVideoGoalsWithWrongType_ValidationError() {
        complexCpmOutdoorAdGroup.withUsersSegments(singletonList(new UsersSegment().withType(AdShowType.THIRD_QUARTILE)));
        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmOutdoorAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        MatcherAssert.assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0), field("usersSegments"), index(0), field(UsersSegment.TYPE)),
                        goalTypeNotSupportedInAdGroup())));
    }

    @Test
    public void updateCpmIndoorAdGroupWithVideoGoalsWithWrongType_ValidationError() {
        complexCpmIndoorAdGroup.withUsersSegments(singletonList(new UsersSegment().withType(AdShowType.THIRD_QUARTILE)));
        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmIndoorAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        MatcherAssert.assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0), field("usersSegments"), index(0), field(UsersSegment.TYPE)),
                        goalTypeNotSupportedInAdGroup())));
    }

    @Test
    public void updateCpmYndxFrontpageAddMinusKeywords_NoError_NotChanged() {
        complexCpmYndxFrontpageAdGroup.withKeywords(null)
                .withAdGroup(complexCpmYndxFrontpageAdGroup.getAdGroup().withMinusKeywords(singletonList("abc")));
        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmYndxFrontpageAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        assertThat(adGroupRepository.getAdGroups(shard, singletonList(
                cpmYndxFrontpageAdGroup.getAdGroup().getId())).get(0).getMinusKeywords(), is(emptyList()));
    }

    @Test
    public void updateCpmYndxFrontpageAddKeywords_ValidationError() {
        complexCpmYndxFrontpageAdGroup.withKeywords(singletonList(keywordForCpmBanner()));
        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmYndxFrontpageAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), keywordsNotAllowed())));
    }

    @Test
    public void cpmYndxFrontpageAdGroup_AdriverPixel_ValidationError() {
        complexCpmYndxFrontpageAdGroup.withBanners(singletonList((
                (CpmBanner) complexCpmYndxFrontpageAdGroup.getBanners().get(0))
                .withPixels(singletonList(adriverPixelUrl()))));
        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmYndxFrontpageAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(index(0), field(ComplexCpmAdGroup.BANNERS), index(0), field(CpmBanner.PIXELS),
                                index(0)),
                        noRightsToPixel(adriverPixelUrl(), emptyList(), CampaignType.CPM_YNDX_FRONTPAGE,
                                InventoryType.NOT_DEAL))));
    }

    @Test
    public void cpmYndxFrontpageAdGroup_YaAudiencePixel_NoError() {
        complexCpmYndxFrontpageAdGroup.withBanners(singletonList((
                (CpmBanner) complexCpmYndxFrontpageAdGroup.getBanners().get(0))
                .withPixels(singletonList(yaAudiencePixelUrl()))));
        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmYndxFrontpageAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void cpmYndxFrontpageAdGroup_VideoCreative_Error() {
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(
                defaultCpmVideoAddition(null, null), clientInfo);

        complexCpmYndxFrontpageAdGroup.withBanners(singletonList((
                (CpmBanner) complexCpmYndxFrontpageAdGroup.getBanners().get(0))
                .withCreativeId(creativeInfo.getCreativeId())));
        ComplexCpmAdGroupUpdateOperation operation = createOperation(singletonList(complexCpmYndxFrontpageAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(
                path(index(0), field(ComplexCpmAdGroup.BANNERS), index(0), field(CpmBanner.CREATIVE_ID)),
                requiredCreativesWithHtml5TypeOnly())));
    }

    @Test
    public void cpmYndxFrontpageAdGroupForPriceSales_NoError() {
        List<Goal> goals = List.of(behaviorGoalForPriceSales);
        List<Rule> rules = List.of(new Rule()
                .withType(RuleType.OR)
                .withGoals(goals));
        metrikaHelperStub.addGoalsFromRules(clientInfo.getUid(), rules);

        complexCpmYndxFrontpageAdGroupForPriceSales.getRetargetingConditions().get(0).withRules(rules);

        ComplexCpmAdGroupUpdateOperation operation =
                createOperation(singletonList(complexCpmYndxFrontpageAdGroupForPriceSales));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void cpmYndxFrontpageAdGroupForPriceSales_WrongGeo_ValidationError() {
        complexCpmYndxFrontpageAdGroupForPriceSales.getAdGroup().withGeo(List.of(CENTRAL_DISTRICT));

        ComplexCpmAdGroupUpdateOperation operation =
                createOperation(singletonList(complexCpmYndxFrontpageAdGroupForPriceSales));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0), field(AdGroup.GEO)), invalidValue())));
    }

    @Test
    public void cpmYndxFrontpageAdGroupForPriceSales_IncorrectGoalType_Error() {
        List<Goal> validGoals = List.of(behaviorGoalForPriceSales);
        List<Goal> invalidGoals = List.of(defaultGoalByType(GoalType.FAMILY));
        List<Rule> rules = List.of(
                new Rule()
                        .withType(RuleType.OR)
                        .withGoals(validGoals),
                new Rule()
                        .withType(RuleType.OR)
                        .withGoals(invalidGoals));
        metrikaHelperStub.addGoalsFromRules(clientInfo.getUid(), rules);

        complexCpmYndxFrontpageAdGroupForPriceSales.getRetargetingConditions().get(0).withRules(rules);
        ComplexCpmAdGroupUpdateOperation operation =
                createOperation(singletonList(complexCpmYndxFrontpageAdGroupForPriceSales));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(
                path(index(0), field(ComplexCpmAdGroup.RETARGETING_CONDITIONS), index(0)),
                retargetingConditionIsInvalidForPricePackage())));
    }

    private void checkComplexAdGroup(ComplexCpmAdGroup complexAdGroup) {
        AdGroup expectedAdGroup = complexAdGroup.getAdGroup();
        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, singletonList(expectedAdGroup.getId()));
        assertThat("группа обновлена", adGroups,
                contains(beanDiffer(expectedAdGroup).useCompareStrategy(onlyFields(newPath("name")))));

        Long adGroupId = complexAdGroup.getAdGroup().getId();
        var actualBanners = bannerTypedRepository.getBannersByGroupIds(shard, singletonList(adGroupId));
        if (complexAdGroup.getBanners() == null) {
            assertThat("в группе не должно быть баннеров", actualBanners, empty());
        } else {

            Map<Long, Banner> actualBannersMap = listToMap(actualBanners, Banner::getId);
            var expectedBanners = complexAdGroup.getBanners();
            Map<Long, Banner> expectedBannersMap = listToMap(expectedBanners, Banner::getId);

            CompareStrategy bannerCompareStrategy = onlyExpectedFields()
                    .forFields(newPath("lastChange")).useMatcher(notNullValue())
                    .forFields(newPath("measurers/0/bannerId")).useMatcher(notNullValue())
                    .forFields(newPath("measurers/0/params")).useMatcher(notNullValue());

            assertThat(actualBannersMap.size(), equalTo(expectedBannersMap.size()));
            expectedBannersMap.forEach((bannerId, expectedBanner) -> {
                var actualBanner = actualBannersMap.get(bannerId);
                assertThat(actualBanner, beanDiffer(expectedBanner).useCompareStrategy(bannerCompareStrategy));
            });
        }
    }

    private ComplexCpmAdGroupUpdateOperation createOperation(List<ComplexCpmAdGroup> adGroups) {
        return operationFactory.createCpmAdGroupUpdateOperation(adGroups, geoTree, false, null,
                clientInfo.getUid(), clientId, clientInfo.getUid(), false);
    }
}
