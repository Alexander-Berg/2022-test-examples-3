package ru.yandex.direct.jobs.takeout;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.takeout.model.TakeoutJobParams;
import ru.yandex.direct.core.entity.takeout.model.TakeoutJobResult;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbqueue.model.DbQueueJob;
import ru.yandex.direct.dbqueue.model.DbQueueJobStatus;
import ru.yandex.direct.dbqueue.repository.DbQueueRepository;
import ru.yandex.direct.dbqueue.service.DbQueueService;
import ru.yandex.direct.dbqueue.steps.DbQueueSteps;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.takeout.client.TakeoutClient;
import ru.yandex.direct.takeout.client.TakeoutResponse;

import static com.google.common.base.Preconditions.checkState;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.TAKEOUT_REQUEST;

@JobsTest
@ExtendWith(SpringExtension.class)
class TakeoutFailedDoneJobTest extends TakeoutUploadJobTestBase {
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

        takeoutUploadJob = new TakeoutUploadJob(SHARD, dbQueueService, takeoutJobService, 2);

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
        verify(takeoutClient, times(2)).done(anySet(), anyString());
        Assertions.assertThat(job.getStatus()).isEqualTo(DbQueueJobStatus.FAILED);
    }

    @Override
    protected TakeoutClient takeoutClient() {
        TakeoutClient c = mock(TakeoutClient.class);
        TakeoutResponse response = new TakeoutResponse();
        response.setStatus("missing");
        TakeoutResponse responseOk = new TakeoutResponse();
        responseOk.setStatus("ok");
        doReturn(responseOk).when(c).uploadFile(any(), anyString());
        doReturn(response).when(c).done(anySet(), anyString());
        return c;
    }

    private void executeJob() {
        Assertions.assertThatCode(() -> takeoutUploadJob.execute())
                .doesNotThrowAnyException();
    }
}
