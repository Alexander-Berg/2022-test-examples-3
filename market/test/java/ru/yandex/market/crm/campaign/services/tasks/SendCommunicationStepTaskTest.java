package ru.yandex.market.crm.campaign.services.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.campaign.services.actions.SendEmailStepContextFactory;
import ru.yandex.market.crm.campaign.services.actions.SendPushesStepContextFactory;
import ru.yandex.market.crm.campaign.services.actions.contexts.SendEmailsStepContext;
import ru.yandex.market.crm.campaign.services.actions.contexts.SendPushesStepContext;
import ru.yandex.market.crm.campaign.services.actions.steps.ConcatResolvedEmailsWithInitialStep;
import ru.yandex.market.crm.campaign.services.actions.steps.ConcatResolvedUuidsWithInitialStep;
import ru.yandex.market.crm.campaign.services.actions.steps.DedublicateByOriginalIdsStep;
import ru.yandex.market.crm.campaign.services.actions.steps.LogUuidGlobalControlStep;
import ru.yandex.market.crm.campaign.services.actions.steps.MergeActionVarsWithSendingVarsStep;
import ru.yandex.market.crm.campaign.services.actions.steps.PrepareOutputTableStep;
import ru.yandex.market.crm.campaign.services.actions.steps.PrepareVariantsTableStep;
import ru.yandex.market.crm.campaign.services.actions.steps.ResolveEmailsStep;
import ru.yandex.market.crm.campaign.services.actions.steps.ResolveUuidsStep;
import ru.yandex.market.crm.campaign.services.actions.steps.UploadPushSendingStep;
import ru.yandex.market.crm.campaign.services.actions.tasks.ActionExecutionContext;
import ru.yandex.market.crm.campaign.services.actions.tasks.SendCommunicationStepTask;
import ru.yandex.market.crm.campaign.services.actions.tasks.SendEmailsStepTask;
import ru.yandex.market.crm.campaign.services.actions.tasks.SendPushesStepTask;
import ru.yandex.market.crm.campaign.services.actions.tasks.UploadEmailSendingStep;
import ru.yandex.market.crm.campaign.services.gen.CreateTmpDirectoryStep;
import ru.yandex.market.crm.campaign.services.gen.FilterSubscribedEmailsStep;
import ru.yandex.market.crm.campaign.services.gen.ResolveCryptaIdsStep;
import ru.yandex.market.crm.campaign.services.gen.SubstractGlobalControlStep;
import ru.yandex.market.crm.campaign.services.sending.steps.AppendVarsStep;
import ru.yandex.market.crm.campaign.services.sending.steps.AssembleSendingStep;
import ru.yandex.market.crm.campaign.services.sending.steps.BuildPushSendingDataStep;
import ru.yandex.market.crm.campaign.services.sending.steps.BuildSendingDataStep;
import ru.yandex.market.crm.campaign.services.sending.steps.BuildSentEarlierModelsStep;
import ru.yandex.market.crm.campaign.services.sending.steps.CollectVariablesStep;
import ru.yandex.market.crm.campaign.services.sending.steps.PrepareEmailsStep;
import ru.yandex.market.crm.campaign.services.sending.steps.ProcessEmailSendingStep;
import ru.yandex.market.crm.campaign.services.sending.steps.ResolveDeviceIdsStep;
import ru.yandex.market.crm.campaign.services.sending.steps.ResolvePushVarsStep;
import ru.yandex.market.crm.campaign.services.sending.steps.YaSenderStep;
import ru.yandex.market.crm.campaign.services.tasks.templates.MeasurableSequenceTaskData;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.tasks.domain.Control;
import ru.yandex.market.crm.tasks.domain.ExecutionResult;
import ru.yandex.market.crm.tasks.domain.Task;
import ru.yandex.market.crm.tasks.domain.TaskStatus;
import ru.yandex.market.crm.yt.client.YtClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SendCommunicationStepTaskTest {

    @Mock
    private YtClient ytClient;

    @Before
    public void setUp() {
        when(ytClient.getRowCount(any())).thenReturn(0L);
    }

    private <T extends Task<?, ?>> T mockStep(Class<T> clazz) {
        var step = Mockito.mock(clazz);
        when(step.getId()).thenReturn(clazz.getName());
        return step;
    }

    private SendEmailsStepTask createEmptySendEmailsStepTask(YtClient ytClient) throws Exception {
        var innerContext = Mockito.mock(SendEmailsStepContext.class);

        var jsonDeserializer = Mockito.mock(JsonDeserializer.class);
        var jsonSerializer = Mockito.mock(JsonSerializer.class);
        var contextFactory = Mockito.mock(SendEmailStepContextFactory.class);

        var createTmpDirectoryStep = mockStep(CreateTmpDirectoryStep.class);
        var resolveEmailsStep = mockStep(ResolveEmailsStep.class);
        var concatResolvedEmailsWithInitial = mockStep(ConcatResolvedEmailsWithInitialStep.class);
        var filterSubscribedEmails = mockStep(FilterSubscribedEmailsStep.class);
        var resolveCryptaIdsStep = mockStep(ResolveCryptaIdsStep.class);
        var substractGlobalControlStep = mockStep(SubstractGlobalControlStep.class);
        var prepareEmailsStep = mockStep(PrepareEmailsStep.class);
        var buildSentEarlierModelsStep = mockStep(BuildSentEarlierModelsStep.class);
        var buildSendingDataStep = mockStep(BuildSendingDataStep.class);
        var collectVariablesStep = mockStep(CollectVariablesStep.class);
        var mergeActionVarsWithSendingVarsStep = mockStep(MergeActionVarsWithSendingVarsStep.class);
        var assembleSendingStep = mockStep(AssembleSendingStep.class);
        var processEmailSendingStep = mockStep(ProcessEmailSendingStep.class);
        var yaSenderStep = mockStep(YaSenderStep.class);
        var prepareOutputTableStep = mockStep(PrepareOutputTableStep.class);
        var uploadEmailSendingStep = mockStep(UploadEmailSendingStep.class);

        when(contextFactory.create(any())).thenReturn(innerContext);

        Mockito.verify(createTmpDirectoryStep, never()).run(any(), any(), any());

        return new SendEmailsStepTask(jsonDeserializer,
                jsonSerializer,
                ytClient,
                contextFactory,
                createTmpDirectoryStep,
                resolveEmailsStep,
                concatResolvedEmailsWithInitial,
                filterSubscribedEmails,
                resolveCryptaIdsStep,
                substractGlobalControlStep,
                prepareEmailsStep,
                buildSentEarlierModelsStep,
                buildSendingDataStep,
                collectVariablesStep,
                mergeActionVarsWithSendingVarsStep,
                assembleSendingStep,
                processEmailSendingStep,
                yaSenderStep,
                prepareOutputTableStep,
                uploadEmailSendingStep
        );
    }

    private SendPushesStepTask createEmptySendPushesStepTask(YtClient ytClient) throws Exception {
        var innerContext = Mockito.mock(SendPushesStepContext.class);

        var jsonDeserializer = Mockito.mock(JsonDeserializer.class);
        var jsonSerializer = Mockito.mock(JsonSerializer.class);
        var contextFactory = Mockito.mock(SendPushesStepContextFactory.class);

        var createTmpDirectoryStep = mockStep(CreateTmpDirectoryStep.class);
        var resolveUuidsStep = mockStep(ResolveUuidsStep.class);
        var concatResolvedUuidsWithInitialStep = mockStep(ConcatResolvedUuidsWithInitialStep.class);
        var resolveCryptaIdsStep = mockStep(ResolveCryptaIdsStep.class);
        var substractGlobalControlStep = mockStep(SubstractGlobalControlStep.class);
        var resolveDeviceIdsStep = mockStep(ResolveDeviceIdsStep.class);
        var dedublicateByOriginalIdsStep = mockStep(DedublicateByOriginalIdsStep.class);
        var resolvePushVarsStep = mockStep(ResolvePushVarsStep.class);
        var collectVariablesStep = mockStep(CollectVariablesStep.class);
        var prepareVariantsTableStep = mockStep(PrepareVariantsTableStep.class);
        var mergeActionVarsWithSendingVarsStep = mockStep(MergeActionVarsWithSendingVarsStep.class);
        var appendVarsStep = mockStep(AppendVarsStep.class);
        var buildPushSendingDataStep = mockStep(BuildPushSendingDataStep.class);
        var uploadPushSendingStep = mockStep(UploadPushSendingStep.class);
        var logUuidGlobalControlStep = mockStep(LogUuidGlobalControlStep.class);
        var prepareOutputTableStep = mockStep(PrepareOutputTableStep.class);

        when(contextFactory.create(any())).thenReturn(innerContext);
        Mockito.verify(createTmpDirectoryStep, never()).run(any(), any(), any());

        return new SendPushesStepTask(jsonDeserializer,
                jsonSerializer,
                ytClient,
                contextFactory,
                createTmpDirectoryStep,
                resolveUuidsStep,
                concatResolvedUuidsWithInitialStep,
                resolveCryptaIdsStep,
                substractGlobalControlStep,
                resolveDeviceIdsStep,
                dedublicateByOriginalIdsStep,
                resolvePushVarsStep,
                collectVariablesStep,
                prepareVariantsTableStep,
                mergeActionVarsWithSendingVarsStep,
                appendVarsStep,
                buildPushSendingDataStep,
                uploadPushSendingStep,
                logUuidGlobalControlStep,
                prepareOutputTableStep);
    }

    @Test
    public void testSendEmailsEndEmptyInput() throws Exception {
        var sendEmailsStepTask = createEmptySendEmailsStepTask(ytClient);
        checkNotGoingDeeper(sendEmailsStepTask);
    }

    @Test
    public void testSendPushesEndEmptyInput() throws Exception {
        var sendEmailsStepTask = createEmptySendPushesStepTask(ytClient);
        checkNotGoingDeeper(sendEmailsStepTask);
    }

    private void checkNotGoingDeeper(SendCommunicationStepTask<?, ?, ?> sendCommunicationStepTask) throws Exception {
        var context = Mockito.mock(ActionExecutionContext.class);
        var status = Mockito.mock(MeasurableSequenceTaskData.class);
        var control = Mockito.mock(Control.class);

        ExecutionResult result = sendCommunicationStepTask.run(context, status, control);
        Assertions.assertEquals(TaskStatus.COMPLETING, result.getNextStatus());
    }
}
