package ru.yandex.direct.core.entity.campaign.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.brandlift.model.BrandLiftRecalcJobParams;
import ru.yandex.direct.core.entity.brandlift.model.BrandLiftRecalcJobResult;
import ru.yandex.direct.core.entity.campaign.model.BrandSurveyStatus;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.SurveyStatus;
import ru.yandex.direct.core.entity.campaign.repository.CampaignBrandSurveyYtRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignBudgetReachDailyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.pricepackage.service.PricePackageService;
import ru.yandex.direct.dbqueue.model.DbQueueJob;
import ru.yandex.direct.dbqueue.repository.DbQueueRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;

@ParametersAreNonnullByDefault
@RunWith(MockitoJUnitRunner.class)
public class CampaignBudgetReachServiceTest {

    @Mock
    private CampaignBrandSurveyYtRepository campaignBrandSurveyYtRepository;
    @Mock
    private DbQueueRepository dbQueueRepository;
    @Mock
    private CampaignBudgetReachDailyRepository campaignBudgetReachDailyRepository;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private PricePackageService pricePackageService;
    @Mock
    private BrandSurveyConditionsService brandSurveyConditionsService;
    @InjectMocks
    private CampaignBudgetReachService campaignBudgetReachService;

    private long campaignId;
    private long campaignId2;
    private ClientId clientId;

    @Before
    public void before() {
        campaignId = RandomNumberUtils.nextPositiveLong();
        campaignId2 = RandomNumberUtils.nextPositiveLong();
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveInteger());
        var campaign = new Campaign().withId(campaignId);
        var campaign2 = new Campaign().withId(campaignId2);
        doReturn(emptyMap()).when(campaignBrandSurveyYtRepository).getBrandSurveyStatusForCampaigns(any());
        doReturn(List.of(campaign, campaign2)).when(campaignRepository).getCampaigns(anyInt(), anyList());
        var queueJob = new DbQueueJob<BrandLiftRecalcJobParams, BrandLiftRecalcJobResult>().withArgs(new BrandLiftRecalcJobParams().withCampaignId(campaignId));
        doReturn(List.of(queueJob)).when(dbQueueRepository).getJobsByJobTypeAndClientIds(anyInt(),  any(), any(), any());

    }

    @Test
    public void getBrandStatusForCampaigns_calculationSurveyStatus_statusesAreNotSame() {
        Map<Long, BrandSurveyStatus> bsId = campaignBudgetReachService.getBrandStatusForCampaigns(1, clientId,
                Map.of(campaignId, "bsId", campaignId2, "anotherBsId"));
        assertThat(bsId).isNotNull();
        assertThat(bsId.values().stream().map(BrandSurveyStatus::getSurveyStatusDaily).collect(Collectors.toList()))
                .containsAll(List.of(SurveyStatus.DRAFT, SurveyStatus.CALCULATION));
    }

    @Test
    public void getBrandStatusForCampaigns_calculationSurveyStatus_statusIsEqualForAllCampaignsWithSameBL() {
        Map<Long, BrandSurveyStatus> bsId = campaignBudgetReachService.getBrandStatusForCampaigns(1, clientId,
                Map.of(campaignId, "bsId", campaignId2, "bsId"));
        assertThat(bsId).isNotNull();
        assertThat(bsId.values().stream().map(BrandSurveyStatus::getSurveyStatusDaily).collect(Collectors.toList()))
                .containsOnly(SurveyStatus.CALCULATION);
    }

}
