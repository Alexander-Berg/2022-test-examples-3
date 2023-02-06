package ru.yandex.direct.jobs.takeout;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.balance.client.BalanceClient;
import ru.yandex.direct.balance.client.model.response.FindClientResponseItem;
import ru.yandex.direct.core.entity.takeout.model.TakeoutJobParams;
import ru.yandex.direct.core.entity.takeout.model.TakeoutJobResult;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbqueue.model.DbQueueJob;
import ru.yandex.direct.dbqueue.repository.DbQueueRepository;
import ru.yandex.direct.dbqueue.service.DbQueueService;
import ru.yandex.direct.dbqueue.steps.DbQueueSteps;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.regions.Region;

import static com.google.common.base.Preconditions.checkState;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.TAKEOUT_REQUEST;

@JobsTest
@ExtendWith(SpringExtension.class)
class TakeoutUploadJobTest extends TakeoutUploadJobTestBase {
    private static final int SHARD = 1;
    @Autowired
    private Steps steps;
    @Autowired
    private DbQueueSteps dbQueueSteps;
    @Autowired
    private DbQueueService dbQueueService;
    @Autowired
    private DbQueueRepository dbQueueRepository;

    private TakeoutUploadJob takeoutUploadJob;

    private TextBannerInfo bannerInfo;

    @BeforeEach
    void before() {
        //создаём клиента, чтобы занять clientId = 1 и баннер создался для второго клиента. Да, это жирный костыль^^
        steps.clientSteps().createDefaultClient();
        bannerInfo = steps.bannerSteps().createActiveTextBanner();
        TakeoutJobService takeoutJobService = initJobService();

        takeoutUploadJob = new TakeoutUploadJob(SHARD, dbQueueService, takeoutJobService, 1);

        dbQueueSteps.registerJobType(TAKEOUT_REQUEST);
        dbQueueSteps.clearQueue(TAKEOUT_REQUEST);
    }

    @Test
    void uploadDataSmoke() {
        TakeoutJobParams params = new TakeoutJobParams()
                .withUid(bannerInfo.getUid())
                .withJobId("job_id");

        Long jobId = dbQueueRepository
                .insertJob(SHARD, TAKEOUT_REQUEST, bannerInfo.getClientId(), bannerInfo.getUid(), params).getId();

        executeJob();

        DbQueueJob<TakeoutJobParams, TakeoutJobResult> job =
                dbQueueRepository.findJobById(SHARD, TAKEOUT_REQUEST, jobId);
        checkState(job != null);
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<Set<String>> fileNamesCaptor = ArgumentCaptor.forClass(Set.class);

        verify(takeoutClient, times(3))
                .uploadFile(fileCaptor.capture(), anyString());
        verify(takeoutClient, times(1))
                .done(fileNamesCaptor.capture(), anyString());

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(fileCaptor.getAllValues()).hasSize(3);
        sa.assertThat(fileCaptor.getAllValues().stream()
                .filter(f -> f.getName().equals("campaign_" + bannerInfo.getCampaignId() + ".json"))
                .count()).isEqualTo(1);
        sa.assertThat(fileCaptor.getAllValues().stream()
                .filter(f -> f.getName().equals("user_" + bannerInfo.getUid() + ".json"))
                .count()).isEqualTo(1);
        sa.assertThat(fileCaptor.getAllValues().stream()
                .filter(f -> f.getName().equals("user_action_logs.json"))
                .count()).isEqualTo(1);
        sa.assertThat(fileNamesCaptor.getValue()).hasSize(3);
        sa.assertAll();
    }

    @Override
    protected BalanceClient balanceClient() {
        BalanceClient balanceClient = super.balanceClient();
        FindClientResponseItem responseItem = new FindClientResponseItem()
                .withClientId(bannerInfo.getClientId().asLong()).withRegionId(Region.TURKEY_REGION_ID);
        when(balanceClient.findClient(any())).thenReturn(Collections.singletonList(responseItem));
        return balanceClient;
    }

    private void executeJob() {
        Assertions.assertThatCode(() -> takeoutUploadJob.execute())
                .doesNotThrowAnyException();
    }
}
