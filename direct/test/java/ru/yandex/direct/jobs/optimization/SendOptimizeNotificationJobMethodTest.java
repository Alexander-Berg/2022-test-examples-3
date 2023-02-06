package ru.yandex.direct.jobs.optimization;

import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.campaign.model.OptimizingCampaignRequest;
import ru.yandex.direct.core.entity.campaign.model.OptimizingCampaignRequestNotificationData;
import ru.yandex.direct.core.entity.campaign.model.OptimizingReqType;
import ru.yandex.direct.core.entity.campaign.repository.OptimizingCampaignRequestRepository;
import ru.yandex.direct.core.entity.notification.NotificationService;
import ru.yandex.direct.core.entity.notification.container.OptimizationResultNotification;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.jobs.optimization.SendOptimizeNotificationJob.OPTIMIZE_NOTIFICATION_INTERVALS;
import static ru.yandex.direct.jobs.optimization.SendOptimizeNotificationJob.getOptimizationResultNotification;


/**
 * Тесты на внутренние методы джобы SendOptimizeNotificationJob.
 *
 * @see SendOptimizeNotificationJob
 */
class SendOptimizeNotificationJobMethodTest {

    private static final int SHARD = 2;

    private SendOptimizeNotificationJob job;

    @Mock
    private OptimizingCampaignRequestRepository optimizingCampaignRequestRepository;

    @Mock
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<OptimizationResultNotification> argumentCaptor;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
        job = spy(new SendOptimizeNotificationJob(SHARD, notificationService, optimizingCampaignRequestRepository));
    }

    private void executeJob() {
        assertThatCode(() -> job.execute())
                .doesNotThrowAnyException();
    }

    private static OptimizingCampaignRequestNotificationData generateNotificationData() {
        return new OptimizingCampaignRequest()
                .withCampaignId(RandomUtils.nextLong(1, Long.MAX_VALUE))
                .withUid(RandomUtils.nextLong(1, Long.MAX_VALUE))
                .withReqType(OptimizingReqType.SECONDAID)
                .withDaysToGo(14)
                .withFio("Vasya Pupkin")
                .withEmail("pupkin14@yandex.ru");
    }


    @Test
    void checkNotSendOptimizeNotification_whenGetEmptyNotificationsData() {
        doReturn(Collections.emptyList()).when(optimizingCampaignRequestRepository)
                .getNotificationsData(SHARD, OPTIMIZE_NOTIFICATION_INTERVALS);

        executeJob();

        verify(job, never()).sendOptimizeNotification(any());
    }

    @Test
    void checkSendOptimizeNotificationParams() {
        OptimizingCampaignRequestNotificationData notificationData = generateNotificationData();
        doReturn(singletonList(notificationData)).when(optimizingCampaignRequestRepository)
                .getNotificationsData(SHARD, OPTIMIZE_NOTIFICATION_INTERVALS);
        OptimizingCampaignRequestNotificationData expectedNotificationData = new OptimizingCampaignRequest()
                .withCampaignId(notificationData.getCampaignId())
                .withUid(notificationData.getUid())
                .withReqType(notificationData.getReqType())
                .withDaysToGo(notificationData.getDaysToGo())
                .withFio(notificationData.getFio())
                .withEmail(notificationData.getEmail());

        executeJob();

        verify(job).sendOptimizeNotification(expectedNotificationData);
    }

    @Test
    void checkCallSendOptimizeNotification_forTwoCampaigns() {
        doReturn(Arrays.asList(generateNotificationData(), generateNotificationData()))
                .when(optimizingCampaignRequestRepository).getNotificationsData(SHARD, OPTIMIZE_NOTIFICATION_INTERVALS);

        executeJob();

        verify(job, times(2)).sendOptimizeNotification(any());
    }

    @Test
    void checkAddNotificationParams() {
        OptimizingCampaignRequestNotificationData notificationData = generateNotificationData();
        doReturn(singletonList(notificationData)).when(optimizingCampaignRequestRepository)
                .getNotificationsData(SHARD, OPTIMIZE_NOTIFICATION_INTERVALS);

        executeJob();

        verify(notificationService).addNotification(argumentCaptor.capture());
        assertThat("отправляемые параметры уведомления соответствуют ожиданиям",
                argumentCaptor.getValue(), beanDiffer(getOptimizationResultNotification(notificationData)));
    }

    @Test
    void checkGetOptimizationResultNotification() {
        OptimizingCampaignRequestNotificationData notificationData = generateNotificationData();

        OptimizationResultNotification expectedOptimizationResultNotification = new OptimizationResultNotification()
                .withCampaignId(notificationData.getCampaignId())
                .withUid(notificationData.getUid())
                .withSecondAid(OptimizingReqType.SECONDAID.equals(notificationData.getReqType()))
                .withDaysToGo(notificationData.getDaysToGo())
                .withFio(notificationData.getFio())
                .withEmail(notificationData.getEmail());

        assertThat("параметры уведомления соответствуют ожиданиям",
                getOptimizationResultNotification(notificationData),
                beanDiffer(expectedOptimizationResultNotification));
    }
}
