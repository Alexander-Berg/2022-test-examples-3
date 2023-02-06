package ru.yandex.direct.intapi.entity.inventori.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.pricepackage.model.BbKeyword;
import ru.yandex.direct.core.entity.pricepackage.model.PriceMarkup;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.ProjectParamCondition;
import ru.yandex.direct.core.entity.pricepackage.model.ProjectParamConjunction;
import ru.yandex.direct.core.entity.pricepackage.model.StatusApprove;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.entity.projectparam.repository.ProjectParamConditionRepository;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.PricePackageInfo;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStrategyName;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.inventori.model.InventoriResponse;
import ru.yandex.direct.intapi.entity.inventori.model.InventoriResult;
import ru.yandex.direct.inventori.model.request.BlockSize;
import ru.yandex.direct.inventori.model.request.CampaignParameters;
import ru.yandex.direct.inventori.model.request.CampaignParametersRf;
import ru.yandex.direct.inventori.model.request.CampaignParametersSchedule;
import ru.yandex.direct.inventori.model.request.GroupType;
import ru.yandex.direct.inventori.model.request.InventoriCampaignType;
import ru.yandex.direct.inventori.model.request.ProjectParameter;
import ru.yandex.direct.inventori.model.request.StrategyType;
import ru.yandex.direct.inventori.model.request.Target;
import ru.yandex.direct.inventori.model.request.VideoCreative;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmVideoBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmVideoAddition;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmBannerAdGroupForPriceSales;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDefaultAdGroupForPriceSales;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDefaultVideoAdGroupForPriceSales;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;

@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class InventoriControllerPeriodFixBidStrategyTest extends BaseInventoriControllerTest {

    @Autowired
    ProjectParamConditionRepository projectParamConditionRepository;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    CreativeRepository creativeRepository;

    private static final List<String> DEFAULT_TAGS = List.of("portal-home-desktop", "portal-home-mobile");
    private static final List<String> INVENTORI_TARGET_TAGS =
            List.of("realty_c2_m", "realty_c1_m", "autoru_r1_d", "autoru_super_d");

    @Test
    public void getInventoriRequests_PeriodFixBidStrategy_AllFieldsAreCorrect() throws Exception {
        var pricePackage = steps.pricePackageSteps()
                .createPricePackage(approvedPricePackage()
                        .withCurrency(CurrencyCode.RUB)
                        .withStatusApprove(StatusApprove.YES)).getPricePackage();
        var cpmPriceCampaign = steps.campaignSteps()
                .createActiveCpmPriceCampaign(clientInfo, pricePackage);

        InventoriResult expectedResult = InventoriResult.success(cpmPriceCampaign.getId(), null,
                InventoriCampaignType.FIX_CPM,
                emptyList(),
                null, false, false, new CampaignParameters(
                        CampaignParametersSchedule.builder()
                                .withStrategyType(StrategyType.FIX_PRICE)
                                .withBudget(calcBudget(pricePackage, cpmPriceCampaign))
                                .withStartDate(pricePackage.getDateStart())
                                .withEndDate(pricePackage.getDateEnd())
                                .withCpm(calcCpm(pricePackage))
                                .withIsBooking(true)
                                .build(),
                        new CampaignParametersRf(0, 0))
        );

        check(singletonList(cpmPriceCampaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_NotActiveCpmPriceCampaignWithCpmVideoAdGroup_Success() throws Exception {
        var pricePackage = steps.pricePackageSteps()
                .createPricePackage(
                        approvedPricePackage()
                                .withCurrency(CurrencyCode.RUB)
                                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                                .withIsFrontpage(false))
                .getPricePackage();
        var cpmPriceCampaign = steps.campaignSteps()
                .createActiveCpmPriceCampaign(clientInfo, pricePackage);
        InventoriResult expectedResult = InventoriResult.success(cpmPriceCampaign.getId(), null,
                InventoriCampaignType.FIX_CPM,
                emptyList(),
                null, false, false, new CampaignParameters(
                        CampaignParametersSchedule.builder()
                                .withStrategyType(StrategyType.FIX_PRICE)
                                .withBudget(calcBudget(pricePackage, cpmPriceCampaign))
                                .withStartDate(pricePackage.getDateStart())
                                .withEndDate(pricePackage.getDateEnd())
                                .withCpm(calcCpm(pricePackage))
                                .withIsBooking(false)
                                .build(),
                        new CampaignParametersRf(0, 0))
        );
        check(singletonList(cpmPriceCampaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_NotActiveCpmPriceCampaignWithCpmFrontpageVideoAdGroup_Success() throws Exception {
        var pricePackage = steps.pricePackageSteps()
                .createPricePackage(
                        approvedPricePackage()
                                .withCurrency(CurrencyCode.RUB)
                                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                                .withIsFrontpage(true))
                .getPricePackage();
        var cpmPriceCampaign = steps.campaignSteps()
                .createActiveCpmPriceCampaign(clientInfo, pricePackage);
        InventoriResult expectedResult = InventoriResult.success(cpmPriceCampaign.getId(), null,
                InventoriCampaignType.FIX_CPM,
                emptyList(),
                null, false, false, new CampaignParameters(
                        CampaignParametersSchedule.builder()
                                .withStrategyType(StrategyType.FIX_PRICE)
                                .withBudget(calcBudget(pricePackage, cpmPriceCampaign))
                                .withStartDate(pricePackage.getDateStart())
                                .withEndDate(pricePackage.getDateEnd())
                                .withCpm(calcCpm(pricePackage))
                                .withIsBooking(false)
                                .build(),
                        new CampaignParametersRf(0, 0))
        );
        check(singletonList(cpmPriceCampaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_FullActiveCpmPriceCampaignWithCpmVideoAdGroup_Success() throws Exception {
        var pricePackage = steps.pricePackageSteps()
                .createPricePackage(
                        approvedPricePackage()
                                .withCurrency(CurrencyCode.RUB)
                                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO)))
                .getPricePackage();
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA));
        var cpmPriceCampaign = steps.campaignSteps()
                .createActiveCpmPriceCampaign(clientInfo, pricePackage);
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpmVideoAdditionCreative(clientInfo, creativeId);

        CpmVideoAdGroup defaultAdGroup = steps.adGroupSteps()
                .createDefaultVideoAdGroupForPriceSales(cpmPriceCampaign, clientInfo);
        OldCpmBanner activeBanner = activeCpmVideoBanner(defaultAdGroup.getCampaignId(), defaultAdGroup.getId(),
                creativeId);
        steps.bannerSteps().createActiveCpmBannerRaw(clientInfo.getShard(), activeBanner, defaultAdGroup);

        InventoriResult expectedResult = InventoriResult.success(
                cpmPriceCampaign.getId(),
                null,
                InventoriCampaignType.FIX_CPM,
                List.of(new Target()
                        .withAdGroupId(defaultAdGroup.getId())
                        .withGroupType(GroupType.VIDEO)
                        .withRegions(Set.of(225))
                        .withTargetTags(DEFAULT_TAGS)
                        .withOrderTags(DEFAULT_TAGS)
                        .withEnableNonSkippableVideo(false)
                        .withVideoCreatives(List.of(new VideoCreative(creativeId, 4000, null,
                                Set.of(new BlockSize(16, 9)))))
                        .withIsDefaultGroup(true)),
                null, false, false, new CampaignParameters(
                        CampaignParametersSchedule.builder()
                                .withStrategyType(StrategyType.FIX_PRICE)
                                .withBudget(calcBudget(pricePackage, cpmPriceCampaign))
                                .withStartDate(pricePackage.getDateStart())
                                .withEndDate(pricePackage.getDateEnd())
                                .withCpm(calcCpm(pricePackage))
                                .withIsBooking(false)
                                .build(),
                        new CampaignParametersRf(0, 0))
//                null,
        );

        check(singletonList(cpmPriceCampaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_CpmPriceCampaignWithCpmVideoAdGroupWithCreativeWithSize_Success()
            throws Exception {
        var pricePackage = steps.pricePackageSteps()
                .createPricePackage(
                        approvedPricePackage()
                                .withCurrency(CurrencyCode.RUB)
                                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO)))
                .getPricePackage();
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA));
        var cpmPriceCampaign = steps.campaignSteps()
                .createActiveCpmPriceCampaign(clientInfo, pricePackage);
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        Creative creative = defaultCpmVideoAddition(clientInfo.getClientId(), creativeId)
                .withWidth(1080L)
                .withHeight(540L);
        creativeRepository.add(clientInfo.getShard(), singletonList(creative));

        CpmVideoAdGroup defaultAdGroup = steps.adGroupSteps()
                .createDefaultVideoAdGroupForPriceSales(cpmPriceCampaign, clientInfo);
        OldCpmBanner activeBanner = activeCpmVideoBanner(defaultAdGroup.getCampaignId(), defaultAdGroup.getId(),
                creativeId);
        steps.bannerSteps().createActiveCpmBannerRaw(clientInfo.getShard(), activeBanner, defaultAdGroup);

        InventoriResult expectedResult = InventoriResult.success(
                cpmPriceCampaign.getId(),
                null,
                InventoriCampaignType.FIX_CPM,
                List.of(new Target()
                        .withAdGroupId(defaultAdGroup.getId())
                        .withGroupType(GroupType.VIDEO)
                        .withRegions(Set.of(225))
                        .withTargetTags(DEFAULT_TAGS)
                        .withOrderTags(DEFAULT_TAGS)
                        .withEnableNonSkippableVideo(false)
                        .withVideoCreatives(List.of(new VideoCreative(creativeId, 4000,
                                new BlockSize(1080, 540),
                                Set.of(new BlockSize(16, 9)))))
                        .withIsDefaultGroup(true)),
                null, false, false, new CampaignParameters(
                        CampaignParametersSchedule.builder()
                                .withStrategyType(StrategyType.FIX_PRICE)
                                .withBudget(calcBudget(pricePackage, cpmPriceCampaign))
                                .withStartDate(pricePackage.getDateStart())
                                .withEndDate(pricePackage.getDateEnd())
                                .withCpm(calcCpm(pricePackage))
                                .withIsBooking(false)
                                .build(),
                        new CampaignParametersRf(0, 0))
        );

        check(singletonList(cpmPriceCampaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_FullActiveCpmPriceCampaign_Success() throws Exception {
        var pricePackage = steps.pricePackageSteps()
                .createPricePackage(
                        approvedPricePackage()
                                .withCurrency(CurrencyCode.RUB)
                                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_YNDX_FRONTPAGE)))
                .getPricePackage();
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA));
        var cpmPriceCampaign = steps.campaignSteps()
                .createActiveCpmPriceCampaign(clientInfo, pricePackage);
        CreativeInfo creativeInfoForPriceSales =
                steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(clientInfo, cpmPriceCampaign);
        Long html5CreativeIdForPriceSales = creativeInfoForPriceSales.getCreativeId();

        CpmYndxFrontpageAdGroup defaultAdGroup = steps.adGroupSteps()
                .createDefaultAdGroupForPriceSales(cpmPriceCampaign, clientInfo);
        OldCpmBanner activeBanner = activeCpmBanner(defaultAdGroup.getCampaignId(), defaultAdGroup.getId(),
                html5CreativeIdForPriceSales);
        steps.bannerSteps().createActiveCpmBannerRaw(clientInfo.getShard(), activeBanner, defaultAdGroup);

        InventoriResult expectedResult = InventoriResult.success(
                cpmPriceCampaign.getId(),
                null,
                InventoriCampaignType.FIX_CPM,
                List.of(new Target()
                        .withGroupType(GroupType.MAIN_PAGE_AND_NTP)
                        .withRegions(Set.of(225))
                        .withTargetTags(DEFAULT_TAGS)
                        .withBlockSizes(List.of(new BlockSize(1836, 572)))
                        .withIsDefaultGroup(true)),
                null, false, false, new CampaignParameters(
                        CampaignParametersSchedule.builder()
                                .withStrategyType(StrategyType.FIX_PRICE)
                                .withBudget(calcBudget(pricePackage, cpmPriceCampaign))
                                .withStartDate(pricePackage.getDateStart())
                                .withEndDate(pricePackage.getDateEnd())
                                .withCpm(calcCpm(pricePackage))
                                .withIsBooking(true)
                                .build(),
                        new CampaignParametersRf(0, 0))
        );

        check(singletonList(cpmPriceCampaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_FullActiveCpmPriceCampaignWithTwoAdGroups_Success() throws Exception {
        var pricePackage = steps.pricePackageSteps()
                .createPricePackage(
                        approvedPricePackage()
                                .withCurrency(CurrencyCode.RUB)
                                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_YNDX_FRONTPAGE)))
                .getPricePackage();
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA));
        var cpmPriceCampaign = steps.campaignSteps()
                .createActiveCpmPriceCampaign(clientInfo, pricePackage);
        CreativeInfo creativeInfoForPriceSales =
                steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(clientInfo, cpmPriceCampaign);
        Long html5CreativeIdForPriceSales = creativeInfoForPriceSales.getCreativeId();

        CpmYndxFrontpageAdGroup defaultAdGroup = steps.adGroupSteps().
                createDefaultAdGroupForPriceSales(cpmPriceCampaign, clientInfo);
        CpmYndxFrontpageAdGroup specificAdGroup = steps.adGroupSteps()
                .createSpecificAdGroupForPriceSales(cpmPriceCampaign, clientInfo);
        OldCpmBanner defaultAdGroupActiveBanner = activeCpmBanner(defaultAdGroup.getCampaignId(),
                defaultAdGroup.getId(), html5CreativeIdForPriceSales);
        steps.bannerSteps().createActiveCpmBannerRaw(clientInfo.getShard(), defaultAdGroupActiveBanner, defaultAdGroup);

        OldCpmBanner specificAdGroupActiveBanner = activeCpmBanner(specificAdGroup.getCampaignId(),
                specificAdGroup.getId(), html5CreativeIdForPriceSales);

        steps.bannerSteps()
                .createActiveCpmBannerRaw(clientInfo.getShard(), specificAdGroupActiveBanner, specificAdGroup);

        InventoriResult expectedResult = InventoriResult.success(
                cpmPriceCampaign.getId(),
                null,
                InventoriCampaignType.FIX_CPM,
                List.of(new Target()
                                .withGroupType(GroupType.MAIN_PAGE_AND_NTP)
                                .withRegions(Set.of(225))
                                .withTargetTags(DEFAULT_TAGS)
                                .withBlockSizes(List.of(new BlockSize(1836, 572)))
                                .withIsDefaultGroup(true),
                        new Target()
                                .withGroupType(GroupType.MAIN_PAGE_AND_NTP)
                                .withRegions(Set.of(225))
                                .withTargetTags(DEFAULT_TAGS)
                                .withBlockSizes(List.of(new BlockSize(1836, 572)))
                                .withIsDefaultGroup(false)),
                null, false, false, new CampaignParameters(
                        CampaignParametersSchedule.builder()
                                .withStrategyType(StrategyType.FIX_PRICE)
                                .withBudget(calcBudget(pricePackage, cpmPriceCampaign))
                                .withStartDate(pricePackage.getDateStart())
                                .withEndDate(pricePackage.getDateEnd())
                                .withCpm(calcCpm(pricePackage))
                                .withIsBooking(true)
                                .build(),
                        new CampaignParametersRf(0, 0))
        );

        check(singletonList(cpmPriceCampaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_CpmPriceYndxFrontPage_NoBanners_GroupsReturned() throws Exception {
        PricePackage pricePackageToCreate = approvedPricePackage()
                .withCurrency(CurrencyCode.RUB)
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_YNDX_FRONTPAGE));
        pricePackageToCreate.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA));
        PricePackage pricePackage = steps.pricePackageSteps()
                .createPricePackage(pricePackageToCreate).getPricePackage();

        var cpmPriceCampaign = steps.campaignSteps()
                .createActiveCpmPriceCampaign(clientInfo, pricePackage);
        steps.adGroupSteps().createDefaultAdGroupForPriceSales(cpmPriceCampaign, clientInfo);

        InventoriResult expectedResult = InventoriResult.success(
                cpmPriceCampaign.getId(),
                null,
                InventoriCampaignType.FIX_CPM,
                List.of(new Target()
                        .withGroupType(GroupType.MAIN_PAGE_AND_NTP)
                        .withRegions(Set.of(225))
                        .withTargetTags(DEFAULT_TAGS)
                        .withIsDefaultGroup(true)),
                null, false, false, new CampaignParameters(
                        CampaignParametersSchedule.builder()
                                .withStrategyType(StrategyType.FIX_PRICE)
                                .withBudget(calcBudget(pricePackageToCreate, cpmPriceCampaign))
                                .withStartDate(pricePackageToCreate.getDateStart())
                                .withEndDate(pricePackageToCreate.getDateEnd())
                                .withCpm(calcCpm(pricePackageToCreate))
                                .withIsBooking(true)
                                .build(),
                        new CampaignParametersRf(0, 0))
        );

        check(singletonList(cpmPriceCampaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_CpmPriceVideo_NoBanners_GroupsNotReturned() throws Exception {
        PricePackage pricePackageToCreate = approvedPricePackage()
                .withCurrency(CurrencyCode.RUB)
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                .withIsFrontpage(false);
        pricePackageToCreate.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA));
        PricePackage pricePackage = steps.pricePackageSteps()
                .createPricePackage(pricePackageToCreate).getPricePackage();

        var cpmPriceCampaign = steps.campaignSteps()
                .createActiveCpmPriceCampaign(clientInfo, pricePackage);
        steps.adGroupSteps().createDefaultAdGroupForPriceSales(cpmPriceCampaign, clientInfo);

        InventoriResult expectedResult = InventoriResult.success(
                cpmPriceCampaign.getId(),
                null,
                InventoriCampaignType.FIX_CPM,
                emptyList(),
                null, false, false, new CampaignParameters(
                        CampaignParametersSchedule.builder()
                                .withStrategyType(StrategyType.FIX_PRICE)
                                .withBudget(calcBudget(pricePackageToCreate, cpmPriceCampaign))
                                .withStartDate(pricePackageToCreate.getDateStart())
                                .withEndDate(pricePackageToCreate.getDateEnd())
                                .withCpm(calcCpm(pricePackageToCreate))
                                .withIsBooking(false)
                                .build(),
                        new CampaignParametersRf(0, 0))
        );

        check(singletonList(cpmPriceCampaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_CpmPriceFrontPageVideo_NoBanners_GroupsReturned() throws Exception {
        PricePackage pricePackageToCreate = approvedPricePackage()
                .withCurrency(CurrencyCode.RUB)
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                .withIsFrontpage(true);
        pricePackageToCreate.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA));
        PricePackage pricePackage = steps.pricePackageSteps()
                .createPricePackage(pricePackageToCreate).getPricePackage();

        var cpmPriceCampaign = steps.campaignSteps()
                .createActiveCpmPriceCampaign(clientInfo, pricePackage);
        CpmYndxFrontpageAdGroup adGroup = activeDefaultAdGroupForPriceSales(cpmPriceCampaign)
                .withType(AdGroupType.CPM_VIDEO);
        steps.adGroupSteps().createAdGroupRaw(adGroup, clientInfo);

        InventoriResult expectedResult = InventoriResult.success(
                cpmPriceCampaign.getId(),
                null,
                InventoriCampaignType.FIX_CPM,
                List.of(new Target()
                        .withAdGroupId(adGroup.getId())
                        .withGroupType(GroupType.VIDEO)
                        .withEnableNonSkippableVideo(false)
                        .withRegions(Set.of(225))
                        .withTargetTags(DEFAULT_TAGS)
                        .withIsDefaultGroup(true)),
                null, false, false, new CampaignParameters(
                        CampaignParametersSchedule.builder()
                                .withStrategyType(StrategyType.FIX_PRICE)
                                .withBudget(calcBudget(pricePackageToCreate, cpmPriceCampaign))
                                .withStartDate(pricePackageToCreate.getDateStart())
                                .withEndDate(pricePackageToCreate.getDateEnd())
                                .withCpm(calcCpm(pricePackageToCreate))
                                .withIsBooking(false)
                                .build(),
                        new CampaignParametersRf(0, 0))
        );

        check(singletonList(cpmPriceCampaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_CpmPriceVideo_NoBanners_GroupsReturned() throws Exception {
        PricePackage pricePackageToCreate = approvedPricePackage()
                .withCurrency(CurrencyCode.RUB)
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                .withIsFrontpage(false);
        pricePackageToCreate.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA));
        PricePackage pricePackage = steps.pricePackageSteps()
                .createPricePackage(pricePackageToCreate).getPricePackage();

        var cpmPriceCampaign = steps.campaignSteps()
                .createActiveCpmPriceCampaign(clientInfo, pricePackage);
        CpmYndxFrontpageAdGroup adGroup = activeDefaultAdGroupForPriceSales(cpmPriceCampaign)
                .withType(AdGroupType.CPM_VIDEO);
        steps.adGroupSteps().createAdGroupRaw(adGroup, clientInfo);

        InventoriResult expectedResult = InventoriResult.success(
                cpmPriceCampaign.getId(),
                null,
                InventoriCampaignType.FIX_CPM,
                List.of(),
                null, false, false, new CampaignParameters(
                        CampaignParametersSchedule.builder()
                                .withStrategyType(StrategyType.FIX_PRICE)
                                .withBudget(calcBudget(pricePackageToCreate, cpmPriceCampaign))
                                .withStartDate(pricePackageToCreate.getDateStart())
                                .withEndDate(pricePackageToCreate.getDateEnd())
                                .withCpm(calcCpm(pricePackageToCreate))
                                .withIsBooking(false)
                                .build(),
                        new CampaignParametersRf(0, 0))
        );

        check(singletonList(cpmPriceCampaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_PeriodFixBidStrategy_SeasonPriceChanges() throws Exception {
        Integer seasonPercent = 115;
        BigDecimal packagePrice = new BigDecimal(300);
        Long orderVolume = 20_000L;
        BigDecimal priceWithSeasonPercent = new BigDecimal(345); // =300*1.15 = цена * процент наценки
        BigDecimal expectedBudget  = new BigDecimal(6900); // =20*345 = количество тыщ показов * цену с наценками

        var campaignStartDate = LocalDate.of(2022, 2, 10);
        var campaignEndDate = LocalDate.of(2022, 2, 24);
        List<PriceMarkup> priceMarkups = List.of(
                new PriceMarkup()
                        .withDateStart(LocalDate.of(2022, 2, 1))
                        .withDateEnd(LocalDate.of(2022, 2, 28))
                        .withPercent(seasonPercent));

        PricePackage defaultPricePackage = approvedPricePackage()
                .withCurrency(CurrencyCode.RUB)
                .withPrice(packagePrice)
                .withPriceMarkups(priceMarkups);

        PricePackage pricePackage = steps.pricePackageSteps().createPricePackage(
                new PricePackageInfo().withPricePackage(defaultPricePackage)).getPricePackage();

        var budget = Money.valueOf(priceWithSeasonPercent, pricePackage.getCurrency())
                .multiply(orderVolume)
                .divide(1000L)
                .bigDecimalValue();

        StrategyData strategyData = new StrategyData()
                .withName(CampaignsStrategyName.period_fix_bid.getLiteral())
                .withStart(campaignStartDate)
                .withFinish(campaignEndDate)
                .withAvgCpm(priceWithSeasonPercent)
                .withBudget(budget);
        DbStrategy strategy = (DbStrategy) new DbStrategy()
                .withStrategyData(strategyData)
                .withAutobudget(CampaignsAutobudget.YES)
                .withStrategyName(StrategyName.PERIOD_FIX_BID);

        var defaultCpmPriceCampaign = TestCampaigns.defaultCpmPriceCampaignWithSystemFields(clientInfo, pricePackage);
        defaultCpmPriceCampaign
                .withFlightOrderVolume(orderVolume)
                .withStrategy(strategy)
                .withStartDate(campaignStartDate)
                .withEndDate(campaignEndDate);
        var cpmPriceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, defaultCpmPriceCampaign);

        InventoriResult expectedResult = InventoriResult.success(cpmPriceCampaign.getId(), null,
                InventoriCampaignType.FIX_CPM,
                emptyList(),
                null, false, false, new CampaignParameters(
                        CampaignParametersSchedule.builder()
                                .withStrategyType(StrategyType.FIX_PRICE)
                                .withBudget(convertToMicroScale(expectedBudget))
                                .withStartDate(campaignStartDate)
                                .withEndDate(campaignEndDate)
                                .withCpm(convertToMicroScale(priceWithSeasonPercent))
                                .withIsBooking(true)
                                .build(),
                        new CampaignParametersRf(0, 0))
        );

        check(singletonList(cpmPriceCampaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_FullActiveVideoBannerWithProjectParameters_Success() throws Exception {
        addProjectParameters();
        var pricePackage = steps.pricePackageSteps()
                .createPricePackage(
                        approvedPricePackage()
                                .withCurrency(CurrencyCode.RUB)
                                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                                .withAllowedProjectParamConditions(List.of(6L, 11L, 16L, 31L, 46L))
                                .withIsFrontpage(false)
                )
                .getPricePackage();
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA));
        var cpmPriceCampaign = steps.campaignSteps()
                .createActiveCpmPriceCampaign(clientInfo, pricePackage);
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpmVideoAdditionCreative(clientInfo, creativeId);

        CpmVideoAdGroup defaultAdGroup = activeDefaultVideoAdGroupForPriceSales(cpmPriceCampaign)
                .withProjectParamConditions(List.of(6L, 11L, 16L, 51L, 52L));
        steps.adGroupSteps().createAdGroupRaw(defaultAdGroup, clientInfo);


        OldCpmBanner activeBanner = activeCpmVideoBanner(defaultAdGroup.getCampaignId(), defaultAdGroup.getId(),
                creativeId);
        steps.bannerSteps().createActiveCpmBannerRaw(clientInfo.getShard(), activeBanner, defaultAdGroup);

        InventoriResult expectedResult = InventoriResult.success(
                cpmPriceCampaign.getId(),
                null,
                InventoriCampaignType.FIX_CPM,
                List.of(new Target()
                        .withAdGroupId(defaultAdGroup.getId())
                        .withGroupType(GroupType.VIDEO)
                        .withRegions(Set.of(225))
                        .withTargetTags(DEFAULT_TAGS)
                        .withOrderTags(DEFAULT_TAGS)
                        .withEnableNonSkippableVideo(false)
                        .withProjectParameters(List.of(
                                new ProjectParameter(622L, List.of(3158L, 3214L, 3180L)),
                                new ProjectParameter(638L, List.of(213L, 10174L, 1L, 2L))))
                        .withVideoCreatives(List.of(new VideoCreative(creativeId, 4000, null,
                                Set.of(new BlockSize(16, 9)))))
                        .withIsDefaultGroup(true)),
                null, false, false, new CampaignParameters(
                        CampaignParametersSchedule.builder()
                                .withStrategyType(StrategyType.FIX_PRICE)
                                .withBudget(calcBudget(pricePackage, cpmPriceCampaign))
                                .withStartDate(pricePackage.getDateStart())
                                .withEndDate(pricePackage.getDateEnd())
                                .withCpm(calcCpm(pricePackage))
                                .withIsBooking(false)
                                .build(),
                        new CampaignParametersRf(0, 0))
        );

        check(singletonList(cpmPriceCampaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_FullActiveCpmPriceCampaign_Success1() throws Exception {
        var pricePackage = steps.pricePackageSteps()
                .createPricePackage(
                        approvedPricePackage()
                                .withCurrency(CurrencyCode.RUB)
                                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_BANNER)))
                .getPricePackage();
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA));
        var cpmPriceCampaign = steps.campaignSteps()
                .createActiveCpmPriceCampaign(clientInfo, pricePackage);
        CreativeInfo creativeInfoForPriceSales =
                steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(clientInfo, cpmPriceCampaign);
        Long html5CreativeIdForPriceSales = creativeInfoForPriceSales.getCreativeId();

        CpmYndxFrontpageAdGroup defaultAdGroup = steps.adGroupSteps()
                .createDefaultAdGroupForPriceSales(cpmPriceCampaign, clientInfo);
        OldCpmBanner activeBanner = activeCpmBanner(defaultAdGroup.getCampaignId(), defaultAdGroup.getId(),
                html5CreativeIdForPriceSales);
        steps.bannerSteps().createActiveCpmBannerRaw(clientInfo.getShard(), activeBanner, defaultAdGroup);

        InventoriResult expectedResult = InventoriResult.success(
                cpmPriceCampaign.getId(),
                null,
                InventoriCampaignType.FIX_CPM,
                List.of(new Target()
                        .withGroupType(GroupType.MAIN_PAGE_AND_NTP)
                        .withRegions(Set.of(225))
                        .withTargetTags(DEFAULT_TAGS)
                        .withBlockSizes(List.of(new BlockSize(1836, 572)))
                        .withIsDefaultGroup(true)),
                null, false, false, new CampaignParameters(
                        CampaignParametersSchedule.builder()
                                .withStrategyType(StrategyType.FIX_PRICE)
                                .withBudget(calcBudget(pricePackage, cpmPriceCampaign))
                                .withStartDate(pricePackage.getDateStart())
                                .withEndDate(pricePackage.getDateEnd())
                                .withCpm(calcCpm(pricePackage))
                                .withIsBooking(false)
                                .build(),
                        new CampaignParametersRf(0, 0))
        );

        check(singletonList(cpmPriceCampaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    @Test
    public void getInventoriRequests_FullActiveCpmPriceCampaign_Success2() throws Exception {
        var pricePackage = steps.pricePackageSteps()
                .createPricePackage(
                        approvedPricePackage()
                                .withCurrency(CurrencyCode.RUB)
                                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_BANNER))
                                .withIsFrontpage(false))
                .getPricePackage();
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA))
                .withViewTypes(List.of(ViewType.DESKTOP));
        var cpmPriceCampaign = TestCampaigns.defaultCpmPriceCampaignWithSystemFields(clientInfo, pricePackage);
        cpmPriceCampaign.getFlightTargetingsSnapshot().setViewTypes(List.of(ViewType.DESKTOP));
        cpmPriceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, cpmPriceCampaign);

        CreativeInfo creative = steps.creativeSteps()
                .addDefaultHtml5Creative(clientInfo, steps.creativeSteps().getNextCreativeId());

        CpmBannerAdGroup defaultAdGroup = activeCpmBannerAdGroupForPriceSales(cpmPriceCampaign)
                .withStatusModerate(StatusModerate.NO)
                .withStatusPostModerate(StatusPostModerate.NO);
        steps.adGroupSteps().createAdGroupRaw(defaultAdGroup, clientInfo);

        OldCpmBanner nonModeratedBanner = activeCpmBanner(
                defaultAdGroup.getCampaignId(), defaultAdGroup.getId(), creative.getCreativeId());
        nonModeratedBanner.withStatusModerate(OldBannerStatusModerate.NO);
        steps.bannerSteps().createActiveCpmBannerRaw(clientInfo.getShard(), nonModeratedBanner, defaultAdGroup);

        InventoriResult expectedResult = InventoriResult.success(
                cpmPriceCampaign.getId(),
                null,
                InventoriCampaignType.FIX_CPM,
                List.of(),
                null, false, false, new CampaignParameters(
                        CampaignParametersSchedule.builder()
                                .withStrategyType(StrategyType.FIX_PRICE)
                                .withBudget(calcBudget(pricePackage, cpmPriceCampaign))
                                .withStartDate(pricePackage.getDateStart())
                                .withEndDate(pricePackage.getDateEnd())
                                .withCpm(calcCpm(pricePackage))
                                .withIsBooking(false)
                                .build(),
                        new CampaignParametersRf(0, 0))
        );

        check(singletonList(cpmPriceCampaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }


    @Test
    public void getInventoriRequests_FullActiveCpmPriceCampaign_Success3() throws Exception {
        ppcPropertiesSupport.set("inventori_target_tags", String.join(", ", INVENTORI_TARGET_TAGS));
        List<String> inventoriTargetTags = List.of("autoru_r1_d", "autoru_super_d");
        var pricePackage = steps.pricePackageSteps()
                .createPricePackage(
                        approvedPricePackage()
                                .withCurrency(CurrencyCode.RUB)
                                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_BANNER))
                                .withIsFrontpage(false)
                                .withAllowedTargetTags(inventoriTargetTags))
                .getPricePackage();
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA))
                .withViewTypes(List.of(ViewType.DESKTOP));
        var cpmPriceCampaign = TestCampaigns.defaultCpmPriceCampaignWithSystemFields(clientInfo, pricePackage);
        cpmPriceCampaign.getFlightTargetingsSnapshot().setViewTypes(emptyList());
        cpmPriceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, cpmPriceCampaign);

        CreativeInfo creative = steps.creativeSteps()
                .addDefaultHtml5Creative(clientInfo, steps.creativeSteps().getNextCreativeId());

        CpmBannerAdGroup defaultAdGroup = activeCpmBannerAdGroupForPriceSales(cpmPriceCampaign)
                .withStatusModerate(StatusModerate.NO)
                .withTargetTags(inventoriTargetTags);
        steps.adGroupSteps().createAdGroupRaw(defaultAdGroup, clientInfo);

        OldCpmBanner nonModeratedBanner = activeCpmBanner(
                defaultAdGroup.getCampaignId(), defaultAdGroup.getId(), creative.getCreativeId());
        nonModeratedBanner.withStatusModerate(OldBannerStatusModerate.NO);
        steps.bannerSteps().createActiveCpmBannerRaw(clientInfo.getShard(), nonModeratedBanner, defaultAdGroup);

        InventoriResult expectedResult = InventoriResult.success(
                cpmPriceCampaign.getId(),
                null,
                InventoriCampaignType.FIX_CPM,
                List.of(new Target()
                        .withAdGroupId(defaultAdGroup.getId())
                        .withGroupType(GroupType.BANNER)
                        .withRegions(Set.of(225))
                        .withTargetTags(inventoriTargetTags)
                        .withBlockSizes(List.of(new BlockSize(480, 320)))),
                null, false, false, new CampaignParameters(
                        CampaignParametersSchedule.builder()
                                .withStrategyType(StrategyType.FIX_PRICE)
                                .withBudget(calcBudget(pricePackage, cpmPriceCampaign))
                                .withStartDate(pricePackage.getDateStart())
                                .withEndDate(pricePackage.getDateEnd())
                                .withCpm(calcCpm(pricePackage))
                                .withIsBooking(false)
                                .build(),
                        new CampaignParametersRf(0, 0))
        );

        check(singletonList(cpmPriceCampaign.getId()), new InventoriResponse(singletonList(expectedResult)));
    }

    protected void check(List<Long> campaignIds, InventoriResponse expected) throws Exception {
        InventoriResult actualResult = getResponse(campaignIds).getResults().get(0);
        InventoriResult expectedResults = expected.getResults().get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualResult.getCampaignId()).as("cid")
                    .isEqualTo(expectedResults.getCampaignId());
            soft.assertThat(actualResult.getDefaultTargetId()).as("default_target_id")
                    .isEqualTo(expectedResults.getDefaultTargetId());
            soft.assertThat(actualResult.getParameters()).as("campaign_parameters")
                    .isEqualTo(expectedResults.getParameters());
            soft.assertThat(actualResult.getCampaignType()).as("campaign_type")
                    .isEqualTo(expectedResults.getCampaignType());

            actualResult.getTargets().forEach(t -> {
                var list = t.getTargetTags();
                if (list != null) {
                    Collections.sort(list);
                }
                var pplist = t.getProjectParameters();
                if (pplist != null) {
                    Collections.sort(pplist);
                }
            });
            soft.assertThat(actualResult.getTargets()).as("groups")
                    .containsExactlyInAnyOrder(expectedResults.getTargets().toArray(new Target[0]));
        });
    }

    private long calcBudget(BigDecimal price, CurrencyCode getCurrency, Long orderVolume) {
        return Money.valueOf(price, getCurrency)
                .multiply(orderVolume)
                .divide(1000L)
                .roundToCentUp()
                .bigDecimalValue()
                .movePointRight(Money.MICRO_MULTIPLIER_SCALE)
                .longValue();
    }
    private long calcBudget(PricePackage pricePackage, CpmPriceCampaign campaign) {
        return calcBudget(pricePackage.getPrice(), pricePackage.getCurrency(), campaign.getFlightOrderVolume());
    }

    private long convertToMicroScale(BigDecimal price) {
        return price.movePointRight(Money.MICRO_MULTIPLIER_SCALE).longValue();
    }

    private long calcCpm(PricePackage pricePackage) {
        return convertToMicroScale(pricePackage.getPrice());
    }

    private void addProjectParameters() {
        addProjectParametersInternal(6L, "Auto.ru: Марка = INFINITI", new long[] {622L, 3158L});
        addProjectParametersInternal(11L, "Auto.ru: Марка = LIFAN", new long[] {622L, 3214L});
        addProjectParametersInternal(16L, "Auto.ru: Марка = PORSCHE", new long[] {622L, 3180L});
        addProjectParametersInternal(51L, "Недвижимость. Москва и МО", new long[] {638L, 213L, 638L, 1L});
        addProjectParametersInternal(52L, "Недвижимость. Санкт-Петербург и ЛО",
                new long[] {638L, 10174L, 638L, 2L});
    }

    private void addProjectParametersInternal(long id, String description, long[] bbkey) {
        var bbKeys = new ArrayList<BbKeyword>();
        for (int i = 0; i < bbkey.length - 1; i += 2) {
            bbKeys.add(new BbKeyword().withKeyword(bbkey[i]).withValue(bbkey[i + 1]));

        }
        projectParamConditionRepository.addProjectParamConditions(List.of(
           new ProjectParamCondition()
                   .withId(id)
                   .withDescription(description)
                   .withIsArchived(false)
                   .withLastUpdateTime(LocalDateTime.now())
                   .withConjunctions(List.of(new ProjectParamConjunction().withBbKeywords(bbKeys)))
        ));
    }
}
