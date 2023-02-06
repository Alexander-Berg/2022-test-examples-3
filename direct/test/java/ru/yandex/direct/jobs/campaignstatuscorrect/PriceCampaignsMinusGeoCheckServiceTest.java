package ru.yandex.direct.jobs.campaignstatuscorrect;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import jdk.jfr.Description;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.banner.model.BannerWithPricePackage;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.moderation.service.receiving.CpmYndxFrontpageModerationReceivingService;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.entity.pricepackage.service.PricePackageService;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersMinusGeoType;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.logicprocessor.processors.campaignstatuscorrect.PriceCampaignsMinusGeoCheckService;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
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
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.interestsRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;

@ParametersAreNonnullByDefault
@JobsTest
@ExtendWith(SpringExtension.class)
public class PriceCampaignsMinusGeoCheckServiceTest {

    @Autowired
    private CpmYndxFrontpageModerationReceivingService cpmYndxFrontpageModerationReceivingService;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private PricePackageService pricePackageService;

    @Autowired
    private TestBannerRepository testBannerRepository;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    @Autowired
    private PriceCampaignsMinusGeoCheckService priceCampaignsMinusGeoCheckService;

    private UserInfo userInfo;
    private ClientInfo clientInfo;
    private int shard;
    private Goal behaviorGoalForPriceSales;
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


    @BeforeEach
    public void setUp() throws Exception {
        userInfo = steps.userSteps().createUser(generateNewUser());
        clientInfo = userInfo.getClientInfo();
        shard = clientInfo.getShard();

        behaviorGoalForPriceSales = defaultGoalByType(GoalType.BEHAVIORS);
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

    private void createRetargeting(CpmYndxFrontpageAdGroup adGroup) {
        RetConditionInfo retConditionInfo = steps.retConditionSteps().createRetCondition(
                interestsRetCondition(clientInfo.getClientId(), List.of(behaviorGoalForPriceSales)), clientInfo);
        Retargeting retargeting =
                defaultRetargeting(campaign.getId(), adGroup.getId(), retConditionInfo.getRetConditionId())
                        .withAutobudgetPriority(null)
                        .withPriceContext(pricePackage.getPrice());
        steps.retargetingSteps().createRetargetingRaw(shard, retargeting, retConditionInfo);
    }

    private void addMinusGeo(int shard, OldCpmBanner banner, List<Long> minusGeo) {
        testBannerRepository.addMinusGeo(shard, banner.getId(),
                BannersMinusGeoType.current, minusGeo.stream().map(Object::toString).collect(joining(",")));
    }

    private OldCpmBanner createBanner(CpmYndxFrontpageAdGroup adGroup, List<Long> minusRegions) {
        CreativeInfo html5Creative = steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(clientInfo,
                campaign);
        OldCpmBanner banner = activeCpmBanner(campaign.getId(), adGroup.getId(),
                html5Creative.getCreativeId())
                .withStatusModerate(OldBannerStatusModerate.YES)
                .withLanguage(Language.RU_)
                .withStatusShow(true);
        steps.bannerSteps().createActiveCpmBannerRaw(shard, banner, adGroup);

        addMinusGeo(shard, banner, minusRegions);

        createRetargeting(adGroup);
        testModerationRepository.createBannerVersion(shard, banner.getId(), 1L);
        return banner;
    }

    private void testCpmPriceBanner(OldCpmBanner banner, boolean isValid) {
        var b = bannerTypedRepository.getBanners(shard, List.of(banner.getId()), null);
        BannerWithPricePackage bannerWithPricePackage = (BannerWithPricePackage) b.get(0);
        var result = priceCampaignsMinusGeoCheckService.filterBannersWithoutOverlappingMinusRegions(shard, List.of(bannerWithPricePackage));

        assertEquals(!result.isEmpty(), isValid);
    }

    @Test
    public void regionNotOverlappingCampaign_StatusShowTrue_Persist() {
        var defaultBanner = createBanner(defaultAdGroup, List.of(REGION_NOT_OVERLAPPING_CAMPAIGN));
        testCpmPriceBanner(defaultBanner, true);
    }

    @Test
    public void regionNotOverlappingCampaign_StatusShowFalse_Persist() {
        var defaultBanner = createBanner(defaultAdGroup, List.of(REGION_NOT_OVERLAPPING_CAMPAIGN));
        testCpmPriceBanner(defaultBanner, true);
    }

    @Test
    public void subregionNotOverlappingCampaign_StatusShowTrue_Persist() {
        var defaultBanner = createBanner(defaultAdGroup, List.of(SUB_REGION_NOT_OVERLAPPING_CAMPAIGN));
        testCpmPriceBanner(defaultBanner, true);
    }

    @Test
    public void subregionNotOverlappingCampaign_StatusShowFalse_Persist() {
        var defaultBanner = createBanner(defaultAdGroup, List.of(SUB_REGION_NOT_OVERLAPPING_CAMPAIGN));
        testCpmPriceBanner(defaultBanner, true);
    }

    @Test
    public void regionOverlappingCampaign_StatusShowTrue_Reset() {
        var defaultBanner = createBanner(defaultAdGroup, List.of(REGION_OVERLAPPING_CAMPAIGN));
        testCpmPriceBanner(defaultBanner, false);
    }

    @Test
    public void regionOverlappingCampaign_StatusShowFalse_Persist() {
        var defaultBanner = createBanner(defaultAdGroup, List.of(REGION_OVERLAPPING_CAMPAIGN));
        testCpmPriceBanner(defaultBanner, false);
    }

    @Test
    public void subregionOverlappingCampaign_StatusShowTrue_Reset() {
        var defaultBanner = createBanner(defaultAdGroup, List.of(SUB_REGION_OVERLAPPING_CAMPAIGN));
        testCpmPriceBanner(defaultBanner, false);
    }

    @Test
    public void subregionOverlappingCampaign_StatusShowFalse_Persist() {
        var defaultBanner = createBanner(defaultAdGroup, List.of(SUB_REGION_OVERLAPPING_CAMPAIGN));
        testCpmPriceBanner(defaultBanner, false);
    }

    @Test
    public void emptyRegions_StatusShowTrue_Persist() {
        var defaultBanner = createBanner(defaultAdGroup, List.of());
        testCpmPriceBanner(defaultBanner, true);
    }

    @Test
    @Description("Две группы одновременно. У дефолтной statusShow сбрасывается, а у специфической - нет.")
    public void twoBanners_StatusShowResetForDefaultAdGroup() {
        var specificAdGroup = steps.adGroupSteps().createSpecificAdGroupForPriceSales(campaign, clientInfo);
        var defaultBanner = createBanner(defaultAdGroup, List.of(REGION_OVERLAPPING_CAMPAIGN));
        var specificBanner = createBanner(specificAdGroup, List.of(REGION_OVERLAPPING_CAMPAIGN));
        testCpmPriceBanner(defaultBanner, false);
        testCpmPriceBanner(specificBanner, true);
    }

}
