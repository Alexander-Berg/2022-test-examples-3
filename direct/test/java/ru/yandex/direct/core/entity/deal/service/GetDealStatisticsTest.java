package ru.yandex.direct.core.entity.deal.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.deal.container.DealStat;
import ru.yandex.direct.core.entity.deal.repository.DealRepository;
import ru.yandex.direct.core.entity.deal.service.validation.DealValidationService;
import ru.yandex.direct.core.entity.placements.repository.PlacementsRepository;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.DealInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.DealSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.PpcRbac;
import ru.yandex.direct.ytcomponents.model.DealStatsResponse;
import ru.yandex.direct.ytcomponents.statistics.service.CampaignStatService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetDealStatisticsTest {
    private static final long CLICKS_FOR_ONE_DEAL_ONE_CAMPAIGN = 1L;
    private static final long SHOWS_FOR_ONE_DEAL_ONE_CAMPAIGN = 100L;
    private static final BigDecimal SUM_SPENT_FOR_ONE_DEAL_ONE_CAMPAIGN = BigDecimal.valueOf(1000L);

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Autowired
    private DealSteps dealSteps;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private DealService dealService;

    @Autowired
    private CampaignStatService campaignStatService;

    @Autowired
    private PlacementsRepository placementsRepository;

    @Autowired
    private DealRepository dealRepository;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private PpcRbac ppcRbac;

    private UserInfo defaultUser;
    private List<DealInfo> dealInfos;
    private List<Long> dealIds;


    @Before
    public void before() {
        defaultUser = userSteps.createDefaultUser();
        CampaignInfo campaign = campaignSteps.createActiveCampaign(defaultUser.getClientInfo());

        dealInfos = dealSteps.addRandomDeals(defaultUser.getClientInfo(), 1);
        dealIds = mapList(dealInfos, DealInfo::getDealId);
        dealIds.forEach(dealId -> dealSteps.linkDealWithCampaign(dealId, campaign.getCampaignId()));

        campaignStatService = mock(CampaignStatService.class);
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        DealValidationService dealValidationService = mock(DealValidationService.class);
        DealNotificationService dealNotificationService = mock(DealNotificationService.class);
        CampaignService campaignService = mock(CampaignService.class);
        UserService userService = mock(UserService.class);
        DslContextProvider dslContextProvider = mock(DslContextProvider.class);
        dealService = new DealService(dealRepository, shardHelper, campaignRepository,
                dealValidationService, dealNotificationService,
                dslContextProvider, campaignStatService, campaignService, userService, placementsRepository,
                ppcRbac, false);
    }

    @Test
    public void getDealStatisticsForOneDeal() {
        Map<Long, DealStatsResponse> statFromRepository = new HashMap<>();
        statFromRepository.put(dealIds.get(0), new DealStatsResponse()
                .withDealId(dealIds.get(0))
                .withClicks(CLICKS_FOR_ONE_DEAL_ONE_CAMPAIGN)
                .withShows(SHOWS_FOR_ONE_DEAL_ONE_CAMPAIGN)
                .withSpent(SUM_SPENT_FOR_ONE_DEAL_ONE_CAMPAIGN)
        );
        when(campaignStatService.getDealsStatistics(any())).thenReturn(statFromRepository);

        Collection<DealStat> dealStatistics = dealService.getDealStatistics(dealIds);
        BigDecimal expectedCpc = BigDecimal.valueOf(1000);

        BigDecimal expectedCtr = BigDecimal.ONE;

        BigDecimal expectedCpm = BigDecimal.TEN.multiply(BigDecimal.valueOf(1000));

        softly.assertThat(dealStatistics).isNotEmpty();
        softly.assertThat(dealStatistics).allMatch(x -> x.getClicks().equals(CLICKS_FOR_ONE_DEAL_ONE_CAMPAIGN))
                .allMatch(stat -> stat.getShows().equals(SHOWS_FOR_ONE_DEAL_ONE_CAMPAIGN))
                .allMatch(stat -> stat.getClicks().equals(CLICKS_FOR_ONE_DEAL_ONE_CAMPAIGN))
                .allMatch(
                        stat -> stat.getSpent().compareTo(SUM_SPENT_FOR_ONE_DEAL_ONE_CAMPAIGN) == 0)
                .allMatch(stat -> stat.getCpc().compareTo(expectedCpc) == 0)
                .allMatch(stat -> stat.getCtr().asPercent().compareTo(expectedCtr) == 0)
                .allMatch(stat -> stat.getCpm().compareTo(expectedCpm) == 0);

    }
}
