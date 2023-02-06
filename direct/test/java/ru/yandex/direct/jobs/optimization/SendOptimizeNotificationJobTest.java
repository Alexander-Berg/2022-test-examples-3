package ru.yandex.direct.jobs.optimization;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.campaign.model.OptimizingCampaignRequest;
import ru.yandex.direct.core.entity.campaign.model.OptimizingReqType;
import ru.yandex.direct.core.entity.campaign.model.OptimizingRequestStatus;
import ru.yandex.direct.core.entity.campaign.repository.OptimizingCampaignRequestRepository;
import ru.yandex.direct.core.entity.notification.NotificationService;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static java.time.LocalDateTime.now;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Тесты на джобу SendOptimizeNotificationJob с использованием базы из докера.
 *
 * @see SendOptimizeNotificationJob
 */
@JobsTest
@ExtendWith(SpringExtension.class)
class SendOptimizeNotificationJobTest {

    private SendOptimizeNotificationJob job;
    private long campaignId;
    private int shard;

    @Autowired
    private Steps steps;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private OptimizingCampaignRequestRepository optimizingCampaignRequestRepository;

    @Mock
    private NotificationService notificationService;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
        CampaignInfo defaultCampaign = steps.campaignSteps().createDefaultCampaign();
        campaignId = defaultCampaign.getCampaignId();
        shard = defaultCampaign.getShard();

        job = new SendOptimizeNotificationJob(shard, notificationService, optimizingCampaignRequestRepository);
    }

    private void executeJob() {
        assertThatCode(() -> job.execute())
                .doesNotThrowAnyException();
    }

    private void addOptimizingCampaignRequests(LocalDateTime readyTime) {
        OptimizingCampaignRequest optimizingCampaignRequest = new OptimizingCampaignRequest()
                .withRequestId(shardHelper.generateOptCampRequestIds(1).get(0))
                .withCampaignId(campaignId)
                .withReadyTime(readyTime.minusSeconds(1)) //Отнимаем секунду, чтобы было строго меньше now()
                .withBannersCount(1)
                .withReqType(OptimizingReqType.FIRSTAID)
                .withStatus(OptimizingRequestStatus.READY)
                .withIsAutomatic(false)
                .withIsSupport(false)
                .withCreateTime(now());

        optimizingCampaignRequestRepository.addRequests(shard, singletonList(optimizingCampaignRequest));
    }


    @Test
    void checkSendOptimizeNotification() {
        LocalDateTime readyTime = LocalDateTime.now().minusDays(3); //Если прошло 3 дня, то должны отправить уведомление
        addOptimizingCampaignRequests(readyTime);

        executeJob();

        verify(notificationService).addNotification(any());
    }
}
