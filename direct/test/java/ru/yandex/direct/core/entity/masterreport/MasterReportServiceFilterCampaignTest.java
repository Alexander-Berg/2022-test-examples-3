package ru.yandex.direct.core.entity.masterreport;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.bannersystem.BannerSystemClient;
import ru.yandex.direct.core.aggregatedstatuses.AggregatedStatusesViewService;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData;
import ru.yandex.direct.core.entity.campaign.model.CampaignCalcType;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.StrategyType;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.masterreport.model.MasterReportFilters;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MasterReportServiceFilterCampaignTest {

    @Autowired
    BannerSystemClient bannerSystemClient;
    @Autowired
    ClientService clientService;
    @Autowired
    CampaignService campaignService;
    @Mock
    private AggregatedStatusesViewService aggregatedStatusesViewService;

    @Autowired
    protected Steps steps;

    MasterReportService masterReportService;
    UserInfo user;

    CampaignInfo actCamp;
    CampaignInfo actFavCamp;
    CampaignInfo actCpmCamp;
    CampaignInfo actFavCpmCamp;
    CampaignInfo actRoiCamp;
    CampaignInfo actFavRoiCamp;
    CampaignInfo archCamp;
    CampaignInfo archFavCamp;
    CampaignInfo archCpmCamp;
    CampaignInfo archFavCpmCamp;
    CampaignInfo archRoiCamp;
    CampaignInfo archFavRoiCamp;
    CampaignInfo stopCamp;
    CampaignInfo stopFavCamp;
    CampaignInfo stopCpmCamp;
    CampaignInfo stopFavCpmCamp;
    CampaignInfo stopRoiCamp;
    CampaignInfo stopFavRoiCamp;

    Map<Long, AggregatedStatusCampaignData> aggrStatusById = new HashMap<>();

    @Before
    public void before() {
        MockitoAnnotations.openMocks(this);
        masterReportService = new MasterReportService(
                bannerSystemClient,
                clientService,
                campaignService,
                aggregatedStatusesViewService
        );
        user = steps.userSteps().createDefaultUser();
        var client = user.getClientInfo();

        actCamp = createCamp(client, GdSelfStatusEnum.RUN_OK, null, false);
        actFavCamp = createCamp(client, GdSelfStatusEnum.RUN_OK, null, true);
        actCpmCamp = createCamp(client, GdSelfStatusEnum.RUN_OK, StrategyName.CPM_DEFAULT, false);
        actFavCpmCamp = createCamp(client, GdSelfStatusEnum.RUN_OK, StrategyName.CPM_DEFAULT, true);
        actRoiCamp = createCamp(client, GdSelfStatusEnum.RUN_OK, StrategyName.AUTOBUDGET_ROI, false);
        actFavRoiCamp = createCamp(client, GdSelfStatusEnum.RUN_OK, StrategyName.AUTOBUDGET_ROI, true);

        archCamp = createCamp(client, GdSelfStatusEnum.ARCHIVED, null, false);
        archFavCamp = createCamp(client, GdSelfStatusEnum.ARCHIVED, null, true);
        archCpmCamp = createCamp(client, GdSelfStatusEnum.ARCHIVED, StrategyName.CPM_DEFAULT, false);
        archFavCpmCamp = createCamp(client, GdSelfStatusEnum.ARCHIVED, StrategyName.CPM_DEFAULT, true);
        archRoiCamp = createCamp(client, GdSelfStatusEnum.ARCHIVED, StrategyName.AUTOBUDGET_ROI, false);
        archFavRoiCamp = createCamp(client, GdSelfStatusEnum.ARCHIVED, StrategyName.AUTOBUDGET_ROI, true);

        stopCamp = createCamp(client, GdSelfStatusEnum.STOP_OK, null, false);
        stopFavCamp = createCamp(client, GdSelfStatusEnum.STOP_OK, null, true);
        stopCpmCamp = createCamp(client, GdSelfStatusEnum.STOP_OK, StrategyName.CPM_DEFAULT, false);
        stopFavCpmCamp = createCamp(client, GdSelfStatusEnum.STOP_OK, StrategyName.CPM_DEFAULT, true);
        stopRoiCamp = createCamp(client, GdSelfStatusEnum.STOP_OK, StrategyName.AUTOBUDGET_ROI, false);
        stopFavRoiCamp = createCamp(client, GdSelfStatusEnum.STOP_OK, StrategyName.AUTOBUDGET_ROI, true);

        when(aggregatedStatusesViewService.getCampaignStatusesByIds(any(), anyCollection())).thenReturn(aggrStatusById);
    }

    @Test
    public void checkCampaignFilter_without() {
        var campaignIdByOrderId = getCampaignIdByOrderId(emptySet(), emptySet());

        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(campaignIdByOrderId).hasSize(18);
            sa.assertThat(campaignIdByOrderId).contains(
                    entry(actCamp),
                    entry(actFavCamp),
                    entry(actCpmCamp),
                    entry(actFavCpmCamp),
                    entry(actRoiCamp),
                    entry(actFavRoiCamp),
                    entry(archCamp),
                    entry(archFavCamp),
                    entry(archCpmCamp),
                    entry(archFavCpmCamp),
                    entry(archRoiCamp),
                    entry(archFavRoiCamp),
                    entry(stopCamp),
                    entry(stopFavCamp),
                    entry(stopCpmCamp),
                    entry(stopFavCpmCamp),
                    entry(stopRoiCamp),
                    entry(stopFavRoiCamp)
            );
        });
    }

    @Test
    public void checkCampaignFilter_fav() {
        var campaignIdByOrderId = getCampaignIdByOrderId(
                Set.of(MasterReportCampaignStatus.FAVORITE),
                emptySet()
        );

        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(campaignIdByOrderId).hasSize(9);
            sa.assertThat(campaignIdByOrderId).contains(
                    entry(actFavCamp),
                    entry(actFavCpmCamp),
                    entry(actFavRoiCamp),
                    entry(archFavCamp),
                    entry(archFavCpmCamp),
                    entry(archFavRoiCamp),
                    entry(stopFavCamp),
                    entry(stopFavCpmCamp),
                    entry(stopFavRoiCamp)
            );
        });
    }

    @Test
    public void checkCampaignFilter_active() {
        var campaignIdByOrderId = getCampaignIdByOrderId(
                Set.of(MasterReportCampaignStatus.ACTIVE),
                emptySet()
        );

        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(campaignIdByOrderId).hasSize(6);
            sa.assertThat(campaignIdByOrderId).contains(
                    entry(actCamp),
                    entry(actFavCamp),
                    entry(actCpmCamp),
                    entry(actFavCpmCamp),
                    entry(actRoiCamp),
                    entry(actFavRoiCamp)
            );
        });
    }

    @Test
    public void checkCampaignFilter_cpm() {
        var campaignIdByOrderId = getCampaignIdByOrderId(
                emptySet(),
                Set.of(StrategyType.CPM_DEFAULT)
        );

        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(campaignIdByOrderId).hasSize(6);
            sa.assertThat(campaignIdByOrderId).contains(
                    entry(actCpmCamp),
                    entry(actFavCpmCamp),
                    entry(archCpmCamp),
                    entry(archFavCpmCamp),
                    entry(stopCpmCamp),
                    entry(stopFavCpmCamp)
            );
        });
    }


    @Test
    public void checkCampaignFilter_archFav() {
        var campaignIdByOrderId = getCampaignIdByOrderId(
                Set.of(MasterReportCampaignStatus.ARCHIVED, MasterReportCampaignStatus.FAVORITE),
                emptySet()
        );

        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(campaignIdByOrderId).hasSize(12);
            sa.assertThat(campaignIdByOrderId).contains(
                    entry(actFavCamp),
                    entry(actFavCpmCamp),
                    entry(actFavRoiCamp),
                    entry(archCamp),
                    entry(archFavCamp),
                    entry(archCpmCamp),
                    entry(archFavCpmCamp),
                    entry(archRoiCamp),
                    entry(archFavRoiCamp),
                    entry(stopFavCamp),
                    entry(stopFavCpmCamp),
                    entry(stopFavRoiCamp)
            );
        });
    }

    @Test
    public void checkCampaignFilter_stoppedCpm() {
        var campaignIdByOrderId = getCampaignIdByOrderId(
                Set.of(MasterReportCampaignStatus.STOPPED),
                Set.of(StrategyType.CPM_DEFAULT)
        );

        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(campaignIdByOrderId).hasSize(2);
            sa.assertThat(campaignIdByOrderId).contains(
                    entry(stopCpmCamp),
                    entry(stopFavCpmCamp)
            );
        });
    }

    @Test
    public void checkCampaignFilter_favRoi() {
        var campaignIdByOrderId = getCampaignIdByOrderId(
                Set.of(MasterReportCampaignStatus.FAVORITE),
                Set.of(StrategyType.ROI)
        );

        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(campaignIdByOrderId).hasSize(3);
            sa.assertThat(campaignIdByOrderId).contains(
                    entry(actFavRoiCamp),
                    entry(archFavRoiCamp),
                    entry(stopFavRoiCamp)
            );
        });
    }


    @Test
    public void checkCampaignFilter_weekBundle() {
        Map<Long, Long> campaignIdByOrderId = getCampaignIdByOrderId(
                emptySet(),
                Set.of(StrategyType.WEEK_BUNDLE)
        );
        assertThat(campaignIdByOrderId).isEmpty();
    }


    private AbstractMap.SimpleEntry<Long, Long> entry(CampaignInfo actCamp) {
        return new AbstractMap.SimpleEntry<>(actCamp.getOrderId(), actCamp.getCampaignId());
    }

    private CampaignInfo createCamp(
            ClientInfo clientInfo,
            GdSelfStatusEnum status,
            StrategyName strategyName,
            boolean makeFavorite
    ) {
        var chiefUid = clientInfo.getChiefUserInfo().getChiefUid();
        var campaign = steps.campaignSteps().createActiveCampaign(clientInfo, RandomNumberUtils.nextPositiveLong());
        if (strategyName != null) {
            steps.campaignSteps().setStrategy(campaign, strategyName);
        }
        if (makeFavorite) {
            steps.campaignSteps().setFavorite(campaign, chiefUid);
        }
        aggrStatusById.put(campaign.getCampaignId(), new AggregatedStatusCampaignData(status));
        return campaign;
    }

    private Map<Long, Long> getCampaignIdByOrderId(Set<MasterReportCampaignStatus> archived,
                                                   Set<StrategyType> objects) {
        return masterReportService.getCampaignIdByOrderId(user.getClientId(), user.getChiefUid(),
                new MasterReportFilters(
                        emptySet(),
                        emptySet(),
                        archived,
                        emptySet(),
                        Set.of(RUSSIA_REGION_ID),
                        CampaignCalcType.CPC,
                        objects
                )
        );
    }
}
