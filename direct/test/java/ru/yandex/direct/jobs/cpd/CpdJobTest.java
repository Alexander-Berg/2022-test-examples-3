package ru.yandex.direct.jobs.cpd;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusApprove;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusCorrect;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.testing.data.TestPricePackages;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static ru.yandex.direct.common.db.PpcPropertyNames.CPD_JOB_ENABLED;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmPriceCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultHtml5;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.emptyTargetingsCustom;

@JobsTest
@ExtendWith(SpringExtension.class)
public class CpdJobTest {

    @Autowired
    CpdJob cpdJob;

    @Autowired
    PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    Steps steps;

    @Autowired
    ShardHelper shardHelper;

    @Autowired
    CampaignRepository campaignRepository;

    private static final LocalTime TIME_TO_START_CAMPAIGNS = LocalTime.of(20, 0);


    @Autowired
    protected CreativeRepository creativeRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    private Long cpdCampaignId;

    private int shardOfCpdCampaign;


    @BeforeEach
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        PricePackage pricePackage = createPricePackage(true, List.of(ViewType.DESKTOP), clientInfo);

        var pairIdAndGroup = createCampaignAndGroup(List.of(225L, -2L), pricePackage, clientInfo);

        cpdCampaignId = pairIdAndGroup.first;

        shardOfCpdCampaign = clientInfo.getShard();
    }

    @AfterEach
    public void after() {
        steps.campaignSteps().archiveCampaign(shardOfCpdCampaign, cpdCampaignId);
//        steps.pricePackageSteps().deletePricePackage(pricePackageId);
    }


    protected CpmPriceCampaign activeCpmPriceCampaign(ClientInfo clientInfo, PricePackage pricePackage) {
        return defaultCpmPriceCampaignWithSystemFields(clientInfo, pricePackage)
                .withStartDate(LocalDate.now().minusDays(1))
                .withEndDate(LocalDate.now().plusDays(1))
                .withStatusShow(true)
                .withFlightStatusCorrect(PriceFlightStatusCorrect.YES)
                .withFlightStatusApprove(PriceFlightStatusApprove.YES);
    }

    protected OldCpmBanner createBannerWithCreativeFormat(AdGroup adGroup, ClientInfo clientInfo) {
        Creative creative = createCreativeWithFormat(1000L, 1000L, clientInfo);
        OldCpmBanner banner = activeCpmBanner(adGroup.getCampaignId(), adGroup.getId(), creative.getId())
                .withHref("http://old.url")
                .withMeasurers(emptyList());
        steps.bannerSteps().createActiveCpmBannerRaw(clientInfo.getShard(), banner, adGroup);
        return banner;
    }

    protected Creative createCreativeWithFormat(Long width, Long height, ClientInfo clientInfo) {
        Creative creative = defaultHtml5(clientInfo.getClientId(), steps.creativeSteps().getNextCreativeId())
                .withWidth(width)
                .withHeight(height)
                .withExpandedPreviewUrl(null);
        creativeRepository.add(clientInfo.getShard(), singletonList(creative));
        return creative;
    }

    private PricePackage createPricePackage(boolean isCpd, List<ViewType> viewTypes, ClientInfo clientInfo) {
        PricePackage pricePackage = TestPricePackages.approvedPricePackage()
                .withClients(List.of(allowedPricePackageClient(clientInfo)))
                .withDateStart(LocalDate.now().minusYears(1))
                .withDateEnd(LocalDate.now().plusYears(1))
                .withTargetingsCustom(emptyTargetingsCustom())
                .withIsCpd(isCpd);

        pricePackage.getTargetingsFixed()
                .withViewTypes(viewTypes)
                .withAllowExpandedDesktopCreative(false);
        steps.pricePackageSteps().createPricePackage(pricePackage);
        return pricePackage;
    }

    private Pair<Long, CpmYndxFrontpageAdGroup> createCampaignAndGroup(List<Long> geo, PricePackage pricePackage,
                                                                       ClientInfo clientInfo) {
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo,
                activeCpmPriceCampaign(clientInfo, pricePackage));
        CpmYndxFrontpageAdGroup specificAdGroup = steps.adGroupSteps().createSpecificAdGroupForPriceSales(campaign,
                clientInfo);
        AppliedChanges<AdGroup> appliedChanges = new ModelChanges<>(specificAdGroup.getId(), AdGroup.class)
                .process(geo, AdGroup.GEO)
                .applyTo(specificAdGroup);
        adGroupRepository.updateAdGroups(clientInfo.getShard(), clientInfo.getClientId(), List.of(appliedChanges));
        createBannerWithCreativeFormat(specificAdGroup, clientInfo);
        return new Pair<>(campaign.getId(), specificAdGroup);
    }

    @Test
    public void testCpdJobFullWorkStopCampaign() {
        Boolean valueOfPropertyBeforeTest = ppcPropertiesSupport.get(CPD_JOB_ENABLED).getOrDefault(false);
        ppcPropertiesSupport.set(CPD_JOB_ENABLED, "true");
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();


        PricePackage pricePackage = createPricePackage(false, List.of(ViewType.DESKTOP), clientInfo);

        var pairIdAndGroup = createCampaignAndGroup(List.of(225L, -2L), pricePackage, clientInfo);


        cpdJob.execute();

        check(pairIdAndGroup.first, true, clientInfo);
        ppcPropertiesSupport.set(CPD_JOB_ENABLED, valueOfPropertyBeforeTest.toString());
    }


    @Test
    public void testCpdJobFullWorkIncorrectViewType() {
        Boolean valueOfPropertyBeforeTest = ppcPropertiesSupport.get(CPD_JOB_ENABLED).getOrDefault(false);
        ppcPropertiesSupport.set(CPD_JOB_ENABLED, "true");
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();


        PricePackage pricePackage = createPricePackage(false, List.of(ViewType.DESKTOP, ViewType.MOBILE), clientInfo);

        var pairIdAndGroup = createCampaignAndGroup(List.of(225L, -2L), pricePackage, clientInfo);


        cpdJob.execute();

        check(pairIdAndGroup.first, false, clientInfo);
        ppcPropertiesSupport.set(CPD_JOB_ENABLED, valueOfPropertyBeforeTest.toString());
    }

    @Test
    public void testCpdJobFullWorkIncorrectGeoPlusRegion() {
        Boolean valueOfPropertyBeforeTest = ppcPropertiesSupport.get(CPD_JOB_ENABLED).getOrDefault(false);
        ppcPropertiesSupport.set(CPD_JOB_ENABLED, "true");
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();


        PricePackage pricePackage = createPricePackage(false, List.of(ViewType.DESKTOP), clientInfo);

        var pairIdAndGroup = createCampaignAndGroup(List.of(225L, 200L, -2L), pricePackage, clientInfo);


        cpdJob.execute();

        check(pairIdAndGroup.first, false, clientInfo);
        ppcPropertiesSupport.set(CPD_JOB_ENABLED, valueOfPropertyBeforeTest.toString());
    }

    @Test
    public void testCpdJobFullWorkIncorrectGeoWithoutMinusRegion() {
        Boolean valueOfPropertyBeforeTest = ppcPropertiesSupport.get(CPD_JOB_ENABLED).getOrDefault(false);
        ppcPropertiesSupport.set(CPD_JOB_ENABLED, "true");
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();


        PricePackage pricePackage = createPricePackage(false, List.of(ViewType.DESKTOP), clientInfo);

        var pairIdAndGroup = createCampaignAndGroup(List.of(225L), pricePackage, clientInfo);


        cpdJob.execute();

        check(pairIdAndGroup.first, false, clientInfo);
        ppcPropertiesSupport.set(CPD_JOB_ENABLED, valueOfPropertyBeforeTest.toString());
    }

    @Test
    public void testToStopCampaignAndRestartBecauseOfItChange() {
        Boolean valueOfPropertyBeforeTest = ppcPropertiesSupport.get(CPD_JOB_ENABLED).getOrDefault(false);
        ppcPropertiesSupport.set(CPD_JOB_ENABLED, "true");
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();


        PricePackage pricePackage = createPricePackage(false, List.of(ViewType.DESKTOP), clientInfo);

        var pairIdAndGroup = createCampaignAndGroup(List.of(225L, -2L), pricePackage, clientInfo);


        cpdJob.execute();

        check(pairIdAndGroup.first, true, clientInfo);

        AppliedChanges<AdGroup> appliedChanges = new ModelChanges<>(pairIdAndGroup.second.getId(), AdGroup.class)
                .process(List.of(225L, 200L), AdGroup.GEO)
                .applyTo(pairIdAndGroup.second);
        adGroupRepository.updateAdGroups(clientInfo.getShard(), clientInfo.getClientId(), List.of(appliedChanges));

        cpdJob.execute();

        check(pairIdAndGroup.first, false, clientInfo);
        ppcPropertiesSupport.set(CPD_JOB_ENABLED, valueOfPropertyBeforeTest.toString());
    }

    private void check(Long campaignId, boolean isMustContains, ClientInfo clientInfo) {
        if (LocalTime.now().isAfter(TIME_TO_START_CAMPAIGNS)) {
            for (int shard : shardHelper.dbShards()) {
                var pausedCampaigns = campaignRepository
                        .getCampaignsWithIsCpdPaused(shard);
                assertEquals(0, pausedCampaigns.size());
            }
        } else {
            var campaignIdsToRestart = campaignRepository
                    .getCampaignsWithIsCpdPaused(clientInfo.getShard())
                    .stream()
                    .map(Campaign::getId)
                    .collect(Collectors.toList());
            assertEquals(isMustContains, campaignIdsToRestart.contains(campaignId));
            assertFalse(campaignIdsToRestart.contains(cpdCampaignId));
        }
    }

}
