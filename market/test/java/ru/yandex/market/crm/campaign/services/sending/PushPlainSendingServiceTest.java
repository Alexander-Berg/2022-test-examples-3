package ru.yandex.market.crm.campaign.services.sending;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Provider;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.crm.campaign.domain.sending.PushPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.PushSendingFactInfo;
import ru.yandex.market.crm.campaign.domain.sending.SendingFactStatus;
import ru.yandex.market.crm.campaign.domain.sending.SendingFactTransportState;
import ru.yandex.market.crm.campaign.domain.sending.SendingFactType;
import ru.yandex.market.crm.campaign.domain.sending.SendingType;
import ru.yandex.market.crm.campaign.services.grouping.campaign.CampaignDAO;
import ru.yandex.market.crm.campaign.services.security.PromoPermissionsService;
import ru.yandex.market.crm.campaign.services.sending.descriptions.PushGenerationProcessDescription;
import ru.yandex.market.crm.campaign.services.sending.facts.PushSendingFactInfoDAO;
import ru.yandex.market.crm.campaign.services.sending.tasks.GeneratePushSendingTask;
import ru.yandex.market.crm.campaign.services.sending.validators.PushConfigValidator;
import ru.yandex.market.crm.campaign.services.tsum.timeline.PushPromoTimelineService;
import ru.yandex.market.crm.core.services.appmetrica.AppMetricaService;
import ru.yandex.market.crm.core.services.control.GlobalControlSaltProvider;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.PushTransfer;
import ru.yandex.market.crm.tasks.domain.TaskInstanceInfo;
import ru.yandex.market.crm.tasks.domain.TaskStatus;
import ru.yandex.market.crm.tasks.services.ClusterTasksService;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.mcrm.tx.TxService;

import static java.util.function.Predicate.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PushPlainSendingServiceTest {
    private PushPlainSendingService pushSendingService;

    @Mock
    private PushSendingDAO pushSendingDAO;
    @Mock
    private PushContextProvider pushContextProvider;
    @Mock
    private GeneratePushSendingTask pushSendingTask;
    @Mock
    private PluggableTablesInPushSendingDAO pluggableTablesInPushSendingDAO;
    @Mock
    private YtClient ytClient;
    @Mock
    private CampaignDAO campaignDAO;
    @Mock
    private ExecutorService executorService;
    @Mock
    private TransactionTemplate txTemplate;
    @Mock
    private PushSendTasksFactory pushSendTasksFactory;
    @Mock
    private SendingNfService nfService;
    @Mock
    private PushSendingFactInfoDAO pushSendingFactInfoDAO;
    @Mock
    private AppMetricaService appMetricaService;
    @Mock
    private PushPromoTimelineService pushTimelineService;
    @Mock
    private ClusterTasksService clusterTasksService;
    @Mock
    private Provider<PushSendingIdUniquenessChecker> idUniquenessChecker;
    @Mock
    private FrequencyToggleService frequencyToggleService;
    @Mock
    private PushConfigValidator configValidator;
    @Mock
    private TxService txService;
    @Mock
    private PromoPermissionsService permissionsService;

    @Before
    public void setUp() {
        PushSendingActionChecker pushSendingActionChecker = new PushSendingActionChecker(
                pushSendingFactInfoDAO,
                null,
                clusterTasksService
        );

        PushGenerationProcessDescription desc = new PushGenerationProcessDescription(
                pushSendingDAO,
                pushContextProvider,
                pushSendingTask,
                pluggableTablesInPushSendingDAO
        );
        pushSendingService = new PushPlainSendingService(
                desc,
                ytClient,
                campaignDAO,
                pushSendingActionChecker,
                executorService,
                txTemplate,
                pushSendTasksFactory,
                nfService,
                pushSendingFactInfoDAO,
                pushTimelineService,
                clusterTasksService,
                idUniquenessChecker,
                new PushDefaultConfigGenerator(),
                new GlobalControlSaltProvider(),
                frequencyToggleService,
                appMetricaService,
                configValidator,
                txService,
                permissionsService);
    }

    @Test
    public void testSuspendUploadAllowed() {
        assertUploadManagementAllowed(pushSendingService::suspendUpload,
                taskId -> verify(clusterTasksService, times(1)).pauseTask(taskId),
                TaskStatus.WAITING, TaskStatus.RUNNING);
    }

    @Test
    public void testSuspendUploadForbidden() {
        Set<TaskStatus> allowedTaskStatuses = Set.of(TaskStatus.WAITING, TaskStatus.RUNNING);

        assertUploadManagementNotAllowed(pushSendingService::suspendUpload,
                taskId -> verify(clusterTasksService, never()).pauseTask(taskId),
                allowedTaskStatuses);

        List<TaskStatus> forbiddenTaskStatuses = Arrays.stream(TaskStatus.values())
                .filter(not(allowedTaskStatuses::contains))
                .collect(Collectors.toUnmodifiableList());

        assertUploadManagementNotAllowed(pushSendingService::suspendUpload,
                taskId -> verify(clusterTasksService, never()).pauseTask(taskId),
                forbiddenTaskStatuses);
    }

    @Test
    public void testResumeUploadAllowed() {
        assertUploadManagementAllowed(pushSendingService::resumeUpload,
                taskId -> verify(clusterTasksService, times(1)).resumeTask(taskId),
                TaskStatus.PAUSING, TaskStatus.PAUSED, TaskStatus.FAILING, TaskStatus.FAILED);
    }

    @Test
    public void testResumeUploadForbidden() {
        Set<TaskStatus> allowedTaskStatuses = Set.of(
                TaskStatus.PAUSING, TaskStatus.PAUSED, TaskStatus.FAILING, TaskStatus.FAILED);

        assertUploadManagementNotAllowed(pushSendingService::resumeUpload,
                taskId -> verify(clusterTasksService, never()).resumeTask(taskId),
                allowedTaskStatuses);

        List<TaskStatus> forbiddenTaskStatuses = Arrays.stream(TaskStatus.values())
                .filter(not(allowedTaskStatuses::contains))
                .collect(Collectors.toUnmodifiableList());

        assertUploadManagementNotAllowed(pushSendingService::resumeUpload,
                taskId -> verify(clusterTasksService, never()).resumeTask(taskId),
                forbiddenTaskStatuses);
    }

    @Test
    public void testCancelSendingTaskAllowed() {
        assertUploadManagementAllowed(pushSendingService::stopSending,
                taskId -> verify(clusterTasksService, times(1)).cancelTask(taskId),
                TaskStatus.WAITING, TaskStatus.RUNNING, TaskStatus.PAUSING, TaskStatus.PAUSED);
    }

    @Test
    public void testCancelSendingTaskNotAllowed() {
        PushPlainSending sending = createSending();

        prepareUploadManagement(sending, false, new TaskInstanceInfo().setStatus(TaskStatus.COMPLETED));

        PushTransfer sent = createPushTransfer(PushTransfer.Status.IN_PROGRESS);
        PushSendingFactInfo fact = createFactInfo(
                SendingFactStatus.SENDING_IN_PROGRESS, Collections.singletonList(sent.getId()));
        fact.setType(SendingFactType.FINAL);

        when(pushSendingFactInfoDAO.getSendingFacts(sending.getId())).thenReturn(Collections.singletonList(fact));

        pushSendingService.stopSending(sending.getId());

        verify(appMetricaService, times(1)).archivePushSendGroup(sent.getGroupId());
    }

    @Test
    public void testCancelUploadForbidden() {
        Set<TaskStatus> allowedTaskStatuses = Set.of(
                TaskStatus.WAITING, TaskStatus.RUNNING, TaskStatus.PAUSING, TaskStatus.PAUSED);

        assertUploadManagementNotAllowed(pushSendingService::resumeUpload,
                taskId -> verify(clusterTasksService, never()).resumeTask(taskId),
                allowedTaskStatuses);

        List<TaskStatus> forbiddenTaskStatuses = Arrays.stream(TaskStatus.values())
                .filter(not(allowedTaskStatuses::contains))
                .collect(Collectors.toUnmodifiableList());

        assertUploadManagementNotAllowed(pushSendingService::resumeUpload,
                taskId -> verify(clusterTasksService, never()).resumeTask(taskId),
                forbiddenTaskStatuses);
    }

    @Test
    public void testUpdateSendingFactsStateNotReady() {
        PushTransfer failed = createErrorPushTransfer();
        PushTransfer sent = createPushTransfer(PushTransfer.Status.SENT);
        PushTransfer inProgress = createPushTransfer(PushTransfer.Status.IN_PROGRESS);
        PushTransfer pending = createPushTransfer(PushTransfer.Status.PENDING);

        createFactInfo(SendingFactStatus.SENDING_IN_PROGRESS,
                List.of(failed.getId(), sent.getId(), inProgress.getId(), pending.getId()));

        PushPlainSendingService pushSendingServiceSpy = spy(pushSendingService);
        pushSendingServiceSpy.updateSendingFactsState();

        verify(pushSendingServiceSpy, never()).tryUpdate(any(), any(), any());
        verify(pushSendingServiceSpy, never()).tryUpdateSendingState(any(), any());
        verify(pushSendingServiceSpy, never()).tryUpdateErrorMessage(any(), any());
        verify(pushSendingServiceSpy, never()).tryUpdateState(any(), any());
    }

    @Test
    public void testUpdateSendingFactsStateReadyNotFinalized() {
        PushTransfer failed = createErrorPushTransfer();
        PushTransfer sent = createPushTransfer(PushTransfer.Status.SENT);

        PushSendingFactInfo fact = createFactInfo(
                SendingFactStatus.SENDING_IN_PROGRESS, List.of(failed.getId(), sent.getId()));

        when(txTemplate.execute(any()))
                .then(inv -> inv.getArgument(0, TransactionCallback.class).doInTransaction(mock(TransactionStatus.class)))
                .thenReturn(null);
        when(pushSendingFactInfoDAO.getById(fact.getId())).thenReturn(fact);

        PushPlainSendingService pushSendingServiceSpy = spy(pushSendingService);
        pushSendingServiceSpy.updateSendingFactsState();

        String errorMessage = getErrorMessage(failed);

        verify(pushSendingServiceSpy, times(1)).tryUpdate(fact, SendingFactTransportState.ERROR, errorMessage);
        verify(pushSendingServiceSpy, times(1)).tryUpdateSendingState(fact, SendingFactTransportState.ERROR);
        verify(pushSendingServiceSpy, times(1)).tryUpdateErrorMessage(fact, errorMessage);
        verify(pushSendingServiceSpy, times(1)).tryUpdateState(fact, errorMessage);
    }

    @Test
    public void testUpdateSendingFactsStateReadyFinalized() {
        PushTransfer failed = createErrorPushTransfer();
        PushTransfer sent = createPushTransfer(PushTransfer.Status.SENT);

        PushSendingFactInfo fact = createFactInfo(SendingFactStatus.PAUSED, List.of(failed.getId(), sent.getId()));

        when(txTemplate.execute(any()))
                .then(inv -> inv.getArgument(0, TransactionCallback.class).doInTransaction(mock(TransactionStatus.class)))
                .thenReturn(null);
        when(pushSendingFactInfoDAO.getById(fact.getId())).thenReturn(fact);

        PushPlainSendingService pushSendingServiceSpy = spy(pushSendingService);
        pushSendingServiceSpy.updateSendingFactsState();

        String errorMessage = getErrorMessage(failed);

        verify(pushSendingServiceSpy, times(1)).tryUpdate(fact, SendingFactTransportState.ERROR, errorMessage);
        verify(pushSendingServiceSpy, times(1)).tryUpdateSendingState(fact, SendingFactTransportState.ERROR);
        verify(pushSendingServiceSpy, times(1)).tryUpdateErrorMessage(fact, errorMessage);
        verify(pushSendingServiceSpy, never()).tryUpdateState(any(), any());
    }

    @Test
    public void testUpdateSendingFactsStateReadyFinished() {
        PushTransfer sent = createPushTransfer(PushTransfer.Status.SENT);

        PushSendingFactInfo fact = createFactInfo(SendingFactStatus.PAUSED, List.of(sent.getId()));
        fact.setSendingState(SendingFactTransportState.FINISHED);
        when(pushSendingFactInfoDAO.getById(fact.getId())).thenReturn(fact);

        PushPlainSendingService pushSendingServiceSpy = spy(pushSendingService);
        pushSendingServiceSpy.updateSendingFactsState();

        verify(pushTimelineService, times(1)).onSendingCompleted(fact.getId());
        verify(nfService, times(1)).onSendingCompleted(fact.getSendingId());
        verify(pushTimelineService, never()).onSendingFailed(any());
        verify(nfService, never()).onSendingFailed(any(), any(), any());
    }

    @Test
    public void testUpdateSendingFactsStateReadyError() {
        PushTransfer sent = createPushTransfer(PushTransfer.Status.SENT);

        PushSendingFactInfo fact = createFactInfo(SendingFactStatus.PAUSED, List.of(sent.getId()));
        fact.setSendingState(SendingFactTransportState.ERROR);
        when(pushSendingFactInfoDAO.getById(fact.getId())).thenReturn(fact);

        PushPlainSendingService pushSendingServiceSpy = spy(pushSendingService);
        pushSendingServiceSpy.updateSendingFactsState();

        verify(pushTimelineService, times(1)).onSendingFailed(fact.getId());
        verify(nfService, times(1)).onSendingFailed(null, SendingType.PUSH, fact.getSendingId());
        verify(pushTimelineService, never()).onSendingCompleted(any());
        verify(nfService, never()).onSendingCompleted(any());
    }

    @Test
    public void testUpdateSendingFactsStateReadyInProgress() {
        PushTransfer sent = createPushTransfer(PushTransfer.Status.SENT);

        PushSendingFactInfo fact = createFactInfo(SendingFactStatus.PAUSED, List.of(sent.getId()));
        fact.setSendingState(SendingFactTransportState.IN_PROGRESS);
        when(pushSendingFactInfoDAO.getById(fact.getId())).thenReturn(fact);

        PushPlainSendingService pushSendingServiceSpy = spy(pushSendingService);
        pushSendingServiceSpy.updateSendingFactsState();

        verify(pushTimelineService, never()).onSendingFailed(any());
        verify(nfService, never()).onSendingFailed(any(), any(), any());
        verify(pushTimelineService, never()).onSendingCompleted(any());
        verify(nfService, never()).onSendingCompleted(any());
    }

    private PushPlainSending createSending() {
        PushPlainSending sending = new PushPlainSending();
        sending.setId(RandomStringUtils.random(10));
        sending.setTaskId(RandomUtils.nextLong());
        return sending;
    }

    private void assertUploadManagementAllowed(Consumer<String> action,
                                               Consumer<Long> assertion,
                                               TaskStatus... taskStatuses) {
        for (TaskStatus taskStatus : taskStatuses) {
            PushPlainSending sending = createSending();

            prepareUploadManagement(sending, true, new TaskInstanceInfo().setStatus(taskStatus));

            action.accept(sending.getId());

            assertion.accept(sending.getTaskId());
        }
    }

    private void assertUploadManagementNotAllowed(Consumer<String> action,
                                                  Consumer<Long> assertion,
                                                  Collection<TaskStatus> taskStatuses) {
        for (TaskStatus taskStatus : taskStatuses) {
            PushPlainSending sending = createSending();

            prepareUploadManagement(sending, false, new TaskInstanceInfo().setStatus(taskStatus));

            try {
                action.accept(sending.getId());
            } catch (IllegalStateException ignored) {
                continue;
            }

            assertion.accept(sending.getTaskId());
        }
    }

    private void prepareUploadManagement(PushPlainSending sending, boolean factManaged, TaskInstanceInfo task) {
        when(pushSendingDAO.getSending(sending.getId())).thenReturn(sending);
        when(pushSendingFactInfoDAO.hasSendingFactsWithTypesAndStatuses(
                sending.getId(),
                Set.of(SendingFactType.FINAL),
                Set.of(SendingFactStatus.UPLOADING_IN_PROGRESS, SendingFactStatus.PAUSED, SendingFactStatus.ERROR)
        )).thenReturn(factManaged);
        when(clusterTasksService.getTask(sending.getTaskId())).thenReturn(task);
    }

    private PushTransfer createPushTransfer(PushTransfer.Status status) {
        PushTransfer transfer = new PushTransfer();
        transfer.setId(RandomUtils.nextInt());
        transfer.setStatus(status);

        when(appMetricaService.getSendPushStatus(transfer.getId())).thenReturn(transfer);

        return transfer;
    }

    private PushTransfer createErrorPushTransfer() {
        PushTransfer failed = new PushTransfer();
        failed.setId(RandomUtils.nextInt());
        failed.setStatus(PushTransfer.Status.FAILED);
        failed.setErrors(List.of("error"));

        when(appMetricaService.getSendPushStatus(failed.getId())).thenReturn(failed);

        return failed;
    }

    private PushSendingFactInfo createFactInfo(SendingFactStatus status, List<Integer> transfers) {
        PushSendingFactInfo fact = new PushSendingFactInfo();
        fact.setId(RandomStringUtils.random(10));
        fact.setTransferIds(transfers);
        fact.setStatus(status);

        when(pushSendingFactInfoDAO.getSendingFactStateInProgress()).thenReturn(List.of(fact));

        return fact;
    }

    private String getErrorMessage(PushTransfer failed) {
        return failed.getId() + ": " + failed.getErrors() + '\n';
    }
}
