package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.util.Arrays;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import jdk.jfr.Description;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.receiving.CpmYndxFrontpageModerationReceivingService;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.entity.pricepackage.service.PricePackageService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.adgroup.service.AdGroupCpmPriceUtils.PRIORITY_DEFAULT;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Yes;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmYndxFrontpageAdGroup;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.emptyTargetingsCustom;
import static ru.yandex.direct.core.testing.data.TestRegions.CENTRAL_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.FAR_EASTERN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.KIROV_PROVINCE;
import static ru.yandex.direct.core.testing.data.TestRegions.NORTHWESTERN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.NORTH_CAUCASIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.core.testing.data.TestRegions.SAINT_PETERSBURG_PROVINCE;
import static ru.yandex.direct.core.testing.data.TestRegions.SIBERIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.SOUTH_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.URAL_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.VOLGA_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;

@ParametersAreNonnullByDefault
@CoreTest
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveCpmPriceModerationResponseTest extends ReceiveModerationResponseBaseTest {

    protected static final List<ModerationReasonDetailed> DEFAULT_REASONS = Arrays.asList(
            new ModerationReasonDetailed().withId(2L),
            new ModerationReasonDetailed().withId(3L));

    @Autowired
    private CpmYndxFrontpageModerationReceivingService cpmYndxFrontpageModerationReceivingService;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private PricePackageService pricePackageService;

    private UserInfo userInfo;
    private ClientInfo clientInfo;
    private int shard;
    private PricePackage pricePackage;
    private CpmPriceCampaign campaign;
    private CpmYndxFrontpageAdGroup defaultAdGroup;

    public static final List<Long> DEFAULT_GEO = List.of(RUSSIA, -VOLGA_DISTRICT, -SIBERIAN_DISTRICT,
            -FAR_EASTERN_DISTRICT);
    public static final List<Long> DEFAULT_GEO_EXPANDED = List.of(NORTHWESTERN_DISTRICT, CENTRAL_DISTRICT,
            URAL_DISTRICT, SOUTH_DISTRICT, NORTH_CAUCASIAN_DISTRICT);
    public static final Integer DEFAULT_GEO_TYPE = REGION_TYPE_DISTRICT;

    private static final Long REGION_OVERLAPPING_CAMPAIGN = NORTHWESTERN_DISTRICT;
    private static final Long SUB_REGION_OVERLAPPING_CAMPAIGN = SAINT_PETERSBURG_PROVINCE;
    private static final Long REGION_NOT_OVERLAPPING_CAMPAIGN = VOLGA_DISTRICT;
    private static final Long SUB_REGION_NOT_OVERLAPPING_CAMPAIGN = KIROV_PROVINCE;

    @Before
    public void setUp() throws Exception {
        userInfo = steps.userSteps().createUser(generateNewUser());
        clientInfo = userInfo.getClientInfo();
        shard = clientInfo.getShard();

        pricePackage = approvedPricePackage()
                .withTargetingsFixed(new TargetingsFixed()
                        .withGeo(DEFAULT_GEO)
                        .withGeoType(DEFAULT_GEO_TYPE)
                        .withGeoExpanded(DEFAULT_GEO_EXPANDED)
                        .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB))
                        .withAllowExpandedDesktopCreative(true))

                .withTargetingsCustom(emptyTargetingsCustom())
                .withClients(List.of(allowedPricePackageClient(clientInfo)));

        steps.pricePackageSteps().createPricePackage(pricePackage);
        campaign = TestCampaigns.defaultCpmPriceCampaignWithSystemFields(clientInfo, pricePackage);
        steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, campaign);
        defaultAdGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, clientInfo);

        List<Long> calculatedGeoExpanded = pricePackageService.getGeoTreeConverter().expandGeo(DEFAULT_GEO,
                DEFAULT_GEO_TYPE);
        checkState(DEFAULT_GEO_EXPANDED.equals(calculatedGeoExpanded));
    }

    @Test
    public void regionNotOverlappingCampaign_StatusShowTrue_Persist() {
        var defaultBanner = createBanner(defaultAdGroup, true);
        testCpmPriceBannerModeration(List.of(REGION_NOT_OVERLAPPING_CAMPAIGN),
                defaultBanner, true);
    }

    @Test
    public void regionNotOverlappingCampaign_StatusShowFalse_Persist() {
        var defaultBanner = createBanner(defaultAdGroup, false);
        testCpmPriceBannerModeration(List.of(REGION_NOT_OVERLAPPING_CAMPAIGN),
                defaultBanner, false);
    }

    @Test
    public void subregionNotOverlappingCampaign_StatusShowTrue_Persist() {
        var defaultBanner = createBanner(defaultAdGroup, true);
        testCpmPriceBannerModeration(List.of(SUB_REGION_NOT_OVERLAPPING_CAMPAIGN),
                defaultBanner, true);
    }

    @Test
    public void subregionNotOverlappingCampaign_StatusShowFalse_Persist() {
        var defaultBanner = createBanner(defaultAdGroup, false);
        testCpmPriceBannerModeration(List.of(SUB_REGION_NOT_OVERLAPPING_CAMPAIGN),
                defaultBanner, false);
    }

    @Test
    public void regionOverlappingCampaign_StatusShowTrue_Reset() {
        var defaultBanner = createBanner(defaultAdGroup, true);
        testCpmPriceBannerModeration(List.of(REGION_OVERLAPPING_CAMPAIGN),
                defaultBanner, false);
    }

    @Test
    public void regionOverlappingCampaign_StatusShowFalse_Persist() {
        var defaultBanner = createBanner(defaultAdGroup, false);
        testCpmPriceBannerModeration(List.of(REGION_OVERLAPPING_CAMPAIGN),
                defaultBanner, false);
    }

    @Test
    public void subregionOverlappingCampaign_StatusShowTrue_Reset() {
        var defaultBanner = createBanner(defaultAdGroup, true);
        testCpmPriceBannerModeration(List.of(SUB_REGION_OVERLAPPING_CAMPAIGN),
                defaultBanner, false);
    }

    @Test
    public void subregionOverlappingCampaign_StatusShowFalse_Persist() {
        var defaultBanner = createBanner(defaultAdGroup, false);
        testCpmPriceBannerModeration(List.of(SUB_REGION_OVERLAPPING_CAMPAIGN),
                defaultBanner, false);
    }

    @Test
    public void emptyRegions_StatusShowTrue_Persist() {
        var defaultBanner = createBanner(defaultAdGroup, true);
        testCpmPriceBannerModeration(List.of(), defaultBanner, true);
    }

    @Test
    public void emptyRegions_StatusShowFalse_Persist() {
        var defaultBanner = createBanner(defaultAdGroup, false);
        testCpmPriceBannerModeration(List.of(), defaultBanner, false);
    }

    @Test
    @Description("Две группы одновременно. У дефолтной statusShow сбрасывается, а у специфической - нет.")
    public void twoBanners_StatusShowResetForDefaultAdGroup() {
        var specificAdGroup = steps.adGroupSteps().createSpecificAdGroupForPriceSales(campaign, clientInfo);
        var defaultBanner = createBanner(defaultAdGroup, true);
        var specificBanner = createBanner(specificAdGroup, true);
        testCpmPriceBannerModeration(List.of(REGION_OVERLAPPING_CAMPAIGN),
                defaultBanner, false, specificBanner, true);
    }

    @Test
    @Description("Проверяем, что YNDX_FRONTPAGE кампании (не прайсовые) не затронуты.")
    public void yndxFrontpageNotAffected() {
        CampaignInfo yndxFrontpageCampaign = steps.campaignSteps().createActiveCpmYndxFrontpageCampaign();
        CpmYndxFrontpageAdGroup yndxFrontpageAdgroup = activeCpmYndxFrontpageAdGroup(
                yndxFrontpageCampaign.getCampaignId())
                .withPriority(PRIORITY_DEFAULT);
        steps.adGroupSteps().createAdGroup(new AdGroupInfo().withAdGroup(yndxFrontpageAdgroup));

        var cpmPriceDefaultAdgroupBanner = createBanner(defaultAdGroup, true);
        var yndxFrontpageDefaultAdgroupBanner = createBanner(yndxFrontpageAdgroup, true);
        testCpmPriceBannerModeration(List.of(REGION_OVERLAPPING_CAMPAIGN),
                cpmPriceDefaultAdgroupBanner, false,
                yndxFrontpageDefaultAdgroupBanner, true);
    }

    private void testCpmPriceBannerModeration(List<Long> minusRegions,
                                              OldCpmBanner banner,
                                              boolean expectedStatusShow) {
        BannerModerationResponse response = createResponse(banner.getId(),
                ModerationObjectType.YNDX_FRONTPAGE_CREATIVE, Yes,
                null, 1L, emptyMap(), minusRegions, clientInfo, DEFAULT_REASONS);
        var unknownVerdictCountAndSuccess = cpmYndxFrontpageModerationReceivingService
                .processModerationResponses(shard, singletonList(response));
        int unknownVerdictCount = unknownVerdictCountAndSuccess.getLeft();
        List<BannerModerationResponse> successResponses = unknownVerdictCountAndSuccess.getRight();
        assertEquals(0, unknownVerdictCount);
        assertEquals(1, successResponses.size());
        OldBanner dbBanner = checkInDb(shard, banner.getId(), response, minusRegions, DEFAULT_REASONS);
        assertEquals(expectedStatusShow, dbBanner.getStatusShow());
    }

    private void testCpmPriceBannerModeration(List<Long> minusRegions,
                                              OldCpmBanner banner1,
                                              boolean expectedStatusShow1,
                                              OldCpmBanner banner2,
                                              boolean expectedStatusShow2) {
        BannerModerationResponse response1 = createResponse(banner1.getId(),
                ModerationObjectType.YNDX_FRONTPAGE_CREATIVE, Yes, null, 1L, emptyMap(), minusRegions, clientInfo,
                DEFAULT_REASONS);
        BannerModerationResponse response2 = createResponse(banner2.getId(),
                ModerationObjectType.YNDX_FRONTPAGE_CREATIVE, Yes, null, 1L, emptyMap(), minusRegions, clientInfo,
                DEFAULT_REASONS);
        var unknownVerdictCountAndSuccess = cpmYndxFrontpageModerationReceivingService
                .processModerationResponses(shard, List.of(response1, response2));
        int unknownVerdictCount = unknownVerdictCountAndSuccess.getLeft();
        List<BannerModerationResponse> successResponses = unknownVerdictCountAndSuccess.getRight();
        assertEquals(0, unknownVerdictCount);
        assertEquals(2, successResponses.size());
        OldBanner dbBanner1 = checkInDb(shard, banner1.getId(), response1, minusRegions, DEFAULT_REASONS);
        OldBanner dbBanner2 = checkInDb(shard, banner2.getId(), response1, minusRegions,
                DEFAULT_REASONS);
        assertEquals(expectedStatusShow1, dbBanner1.getStatusShow());
        assertEquals(expectedStatusShow2, dbBanner2.getStatusShow());
    }

    private OldCpmBanner createBanner(CpmYndxFrontpageAdGroup adGroup, Boolean statusShow) {
        CreativeInfo html5Creative = steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(clientInfo,
                campaign);
        OldCpmBanner banner = activeCpmBanner(campaign.getId(), adGroup.getId(),
                html5Creative.getCreativeId())
                .withStatusModerate(OldBannerStatusModerate.YES)
                .withLanguage(defaultLanguage())
                .withStatusShow(statusShow);
        steps.bannerSteps().createActiveCpmBannerRaw(shard, banner, adGroup);
        testModerationRepository.createBannerVersion(shard, banner.getId(), 1L);
        return banner;
    }
}
