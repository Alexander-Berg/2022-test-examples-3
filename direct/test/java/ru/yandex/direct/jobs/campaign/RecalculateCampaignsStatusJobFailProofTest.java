package ru.yandex.direct.jobs.campaign;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.aggregatedstatuses.AggregatedStatusesService;
import ru.yandex.direct.core.aggregatedstatuses.repository.model.RecalculationDepthEnum;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.qatools.allure.annotations.Description;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.common.db.PpcPropertyNames.AGGREGATED_STATUS_PARALLEL;
import static ru.yandex.direct.common.db.PpcPropertyNames.RECALCULATE_CAMPAIGNS_STATUS_JOB_DEPTH;
import static ru.yandex.direct.common.db.PpcPropertyNames.RECALCULATE_CAMPAIGNS_STATUS_JOB_ITERATION_LIMIT;
import static ru.yandex.direct.common.db.PpcPropertyNames.RECALCULATE_CAMPAIGNS_STATUS_JOB_SLEEP_COEFFICIENT;
import static ru.yandex.direct.common.db.PpcPropertyNames.recalculateCampaignsStatusJobEnabled;

@JobsTest
@ExtendWith(SpringExtension.class)
class RecalculateCampaignsStatusJobFailProofTest {
    @Autowired
    private Steps steps;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private AggregatedStatusesService aggregatedStatusesService;

    @Mock
    private AdGroupRepository adGroupRepository;

    private int shard;
    private RecalculateCampaignsStatusJob job;

    private Long firstPaidCampaignId;
    private Long noMoneyCampaignId;
    private Long secondPaidCampaignId;

    @BeforeEach
    void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        aggregatedStatusesService = spy(aggregatedStatusesService);
        job = new RecalculateCampaignsStatusJob(shard, ppcPropertiesSupport, aggregatedStatusesService,
                campaignRepository);

        ppcPropertiesSupport.set(recalculateCampaignsStatusJobEnabled(shard).getName(), String.valueOf(true));
        ppcPropertiesSupport.set(RECALCULATE_CAMPAIGNS_STATUS_JOB_SLEEP_COEFFICIENT.getName(), String.valueOf(0.0));
        // Ставим лимит на число обрабатываемых кампаний за один цикл = 1
        ppcPropertiesSupport.set(RECALCULATE_CAMPAIGNS_STATUS_JOB_ITERATION_LIMIT.getName(), String.valueOf(1));
        ppcPropertiesSupport.set(RECALCULATE_CAMPAIGNS_STATUS_JOB_DEPTH.getName(), RecalculationDepthEnum.ALL.value());
        ppcPropertiesSupport.set(AGGREGATED_STATUS_PARALLEL.getName(), "true");

        // Создаем 3 кампании; с установленным лимитом таск должен отработать, как минимум, 3 цикла
        firstPaidCampaignId = steps.campaignSteps().createActiveTextCampaign(clientInfo).getCampaignId();
        var adGroupInfo = steps.adGroupSteps().createDefaultContentPromotionAdGroup(clientInfo,
                ContentPromotionAdgroupType.VIDEO);
        noMoneyCampaignId = adGroupInfo.getCampaignId();
        secondPaidCampaignId = steps.campaignSteps().createActiveTextCampaign(clientInfo).getCampaignId();
    }

    @Test
    @Description("Таск не зацикливается на списке id кампаний, бросающих ошибки при пересчете")
    void skipIterationWithBrokenCampaign() {
        // На цикле с адгруппой будет брошена ошибка, после которой таск должен все равно обновить id последней
        // обработанной кампании и запустить следующий цикл пересчета статусов со новым набором id кампаний
        doThrow(new RuntimeException("Imitating some exception inside adGroupRepository"))
                .when(adGroupRepository)
                .getAdGroupIdsByCampaignIds(shard, Set.of(noMoneyCampaignId));

        var updateBefore = LocalDateTime.now();
        // На следующем цикле имитируем отрабатывание пересчета статусов кампаний без ошибок
        doNothing()
                .when(aggregatedStatusesService)
                .fullyRecalculateStatuses(shard, updateBefore, Set.of(firstPaidCampaignId), RecalculationDepthEnum.ALL);

        job.execute(updateBefore);

        // Проверяем, что таск не остановился после ошибки на упавшем цикле и вызвал пересчет статусов для следующего
        // набора id кампаний. Также проверяем, что таск вызывал пересчет и для предыдущей кампании
        verify(aggregatedStatusesService, times(1)).fullyRecalculateStatuses(shard, updateBefore,
                Set.of(firstPaidCampaignId), RecalculationDepthEnum.ALL);
        verify(aggregatedStatusesService, times(1)).fullyRecalculateStatuses(shard, updateBefore,
                Set.of(noMoneyCampaignId), RecalculationDepthEnum.ALL);
        verify(aggregatedStatusesService, times(1)).fullyRecalculateStatuses(shard, updateBefore,
                Set.of(secondPaidCampaignId), RecalculationDepthEnum.ALL);
    }
}
