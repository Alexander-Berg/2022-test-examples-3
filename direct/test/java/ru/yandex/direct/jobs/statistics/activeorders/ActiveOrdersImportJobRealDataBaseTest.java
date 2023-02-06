package ru.yandex.direct.jobs.statistics.activeorders;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.statistics.model.ActiveOrderChanges;
import ru.yandex.direct.core.entity.statistics.repository.OrderStatClusterChooseRepository;
import ru.yandex.direct.core.entity.statistics.repository.OrderStatRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.BalanceInfo;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsArchived;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.ytcomponents.config.DirectYtDynamicConfig;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.common.db.PpcPropertyNames.ACTIVE_ORDERS_BATCHES_COUNT;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.INTERNAL_DISTRIB;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.INTERNAL_FREE;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.TEXT;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeBalanceInfo;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeInternalDistribCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeInternalFreeCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;

@JobsTest
@ExtendWith(SpringExtension.class)
class ActiveOrdersImportJobRealDataBaseTest {
    private static final int BATCH_SIZE = 10;
    private static final List<YtCluster> CLUSTERS = List.of(YtCluster.ZENO);
    private static final LocalDateTime DEFAULT_LAST_SHOW_TIME = LocalDateTime.of(2010, 1, 1, 1, 1);

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private ActiveOrdersImportService activeOrdersImportService;

    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private ActiveOrdersImportParametersSource activeOrdersImportParametersSource;
    private OrderStatRepository orderStatRepository;
    private ActiveOrdersImportJob activeOrdersImportJob;

    @BeforeEach
    public void before() throws NoSuchFieldException {
        initMocks(this);

        // Mock stuff needed for job creation
        PpcProperty<Long> batchesCountProperty = mock(PpcProperty.class);
        doReturn(2L).when(batchesCountProperty).getOrDefault(any());

        var ppcPropertiesSupport = mock(PpcPropertiesSupport.class);
        doReturn(batchesCountProperty)
                .when(ppcPropertiesSupport).get(ACTIVE_ORDERS_BATCHES_COUNT);
        orderStatRepository = spy(new OrderStatRepository(mock(YtProvider.class), ppcPropertiesSupport,
                mock(DirectYtDynamicConfig.class)));
        doReturn(63L).when(orderStatRepository).getCampaignsYtHashMaxValue(any());
        var orderStatClusterChooseRepository = mock(OrderStatClusterChooseRepository.class);

        // Create job
        activeOrdersImportJob = new ActiveOrdersImportJob(activeOrdersImportService, ppcPropertiesSupport,
                orderStatRepository, orderStatClusterChooseRepository, shardHelper, activeOrdersImportParametersSource,
                BATCH_SIZE);

        Map<Integer, ActiveOrdersMetrics> metricsMap = mock(Map.class);
        doReturn(mock(ActiveOrdersMetrics.class)).when(metricsMap).get(anyInt());
        // Mock internal job state which is not needed for this test
        ReflectionTestUtils.setField(
                activeOrdersImportJob,
                "activeOrdersMetrics",
                metricsMap
        );
    }

    @Test
    void importActiveOrders_externalCampaignFinished() {
        var campaign = createCampaign(TEXT, activeBalanceInfo(CurrencyCode.RUB));
        var activeOrderChanges = new ActiveOrderChanges.Builder()
                .withOrderId(1L)
                .withCid(campaign.getCampaignId())
                .withShard(campaign.getShard())
                .withType(TEXT)
                .withArchived(CampaignsArchived.No.getLiteral())
                .withSum(100_000_000_000L)
                .withOldSumSpent(10_000_000_000L)
                .withNewSumSpent(100_001_000_000L) // No more money on campaign
                .build();
        doReturn(List.of(activeOrderChanges)).when(orderStatRepository).getChangedActiveOrders(any(), eq(CLUSTERS));
        activeOrdersImportJob.importActiveOrdersBatches(0, CLUSTERS);

        var campaigns = campaignRepository.getCampaigns(campaign.getShard(), List.of(campaign.getCampaignId()));
        assertThat(campaigns).hasSize(1);

        var updatedCampaign = campaigns.get(0);
        assertThat(updatedCampaign.getStatusActive()).isFalse();
    }

    @Test
    void importActiveOrders_internalFreeCampaignFinished() {
        var campaign = createCampaign(INTERNAL_FREE, activeBalanceInfo(CurrencyCode.RUB).withSumUnits(100L));
        var activeOrderChanges = new ActiveOrderChanges.Builder()
                .withOrderId(1L)
                .withCid(campaign.getCampaignId())
                .withShard(campaign.getShard())
                .withType(INTERNAL_FREE)
                .withArchived(CampaignsArchived.No.getLiteral())
                .withUnits(100L)
                .withOldSumSpentUnits(0L)
                .withNewSumSpentUnits(101L)
                .build();
        doReturn(List.of(activeOrderChanges)).when(orderStatRepository).getChangedActiveOrders(any(), eq(CLUSTERS));
        activeOrdersImportJob.importActiveOrdersBatches(0, CLUSTERS);

        var campaigns = campaignRepository.getCampaigns(campaign.getShard(), List.of(campaign.getCampaignId()));
        assertThat(campaigns).hasSize(1);

        var updatedCampaign = campaigns.get(0);
        assertThat(updatedCampaign.getStatusActive()).isFalse();
    }

    @Test
    void importActiveOrders_internalDistribCampaignNotFinishedAndHasNoNegativeBalance() {
        var campaign = createCampaign(INTERNAL_DISTRIB, activeBalanceInfo(CurrencyCode.RUB));
        var activeOrderChanges = new ActiveOrderChanges.Builder()
                .withOrderId(1L)
                .withCid(campaign.getCampaignId())
                .withShard(campaign.getShard())
                .withType(INTERNAL_DISTRIB)
                .withArchived(CampaignsArchived.No.getLiteral())
                .withSum(100_000_000_000L)
                .withOldSumSpent(10_000_000_000L)
                .withNewSumSpent(100_001_000_000L) // No more money on campaign
                .build();
        doReturn(List.of(activeOrderChanges)).when(orderStatRepository).getChangedActiveOrders(any(), eq(CLUSTERS));
        activeOrdersImportJob.importActiveOrdersBatches(0, CLUSTERS);

        var campaigns = campaignRepository.getCampaigns(campaign.getShard(), List.of(campaign.getCampaignId()));
        assertThat(campaigns).hasSize(1);

        var updatedCampaign = campaigns.get(0);
        assertThat(updatedCampaign.getStatusActive()).isTrue();
        assertThat(updatedCampaign.getSumSpent())
                .isEqualTo(BigDecimal.valueOf(100_000).setScale(6, RoundingMode.DOWN));
    }

    private CampaignInfo createCampaign(CampaignType type, BalanceInfo balanceInfo) {
        Campaign campaign;
        switch (type) {
            case INTERNAL_FREE:
                campaign = activeInternalFreeCampaign(null, null);
                break;
            case INTERNAL_DISTRIB:
                campaign = activeInternalDistribCampaign(null, null);
                break;
            default:
                campaign = activeTextCampaign(null, null);
                break;
        }
        return steps.campaignSteps().createCampaign(campaign
                .withLastShowTime(DEFAULT_LAST_SHOW_TIME)
                .withBalanceInfo(balanceInfo)
        );
    }
}
