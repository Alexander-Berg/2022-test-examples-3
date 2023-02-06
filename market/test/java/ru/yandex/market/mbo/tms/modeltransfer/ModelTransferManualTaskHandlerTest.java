package ru.yandex.market.mbo.tms.modeltransfer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import ru.yandex.market.mbo.db.transfer.step.ModelTransferQueueTask;
import ru.yandex.market.mbo.db.transfer.step.result.ModelTransferStepResultService;
import ru.yandex.market.mbo.db.transfer.step.result.TextResultService;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStep;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStepInfo;
import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;
import ru.yandex.market.mbo.gwt.models.transfer.step.TicketStepConfig;
import ru.yandex.market.mbo.tms.modeltransfer.processor.ModelTransferManualStepProcessor;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author dmserebr
 * @date 20.08.18
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ModelTransferManualTaskHandlerTest extends ModelTransferTaskHandlerTestBase {

    private static final long MANUAL_STEP_ID = 100L;

    private ModelTransferStepInfo manualStepInfo;

    private ModelTransferQueueTask modelTransferQueueTask = new ModelTransferQueueTask()
        .setModelTransferStepId(MANUAL_STEP_ID);

    @Before
    public void before() throws Exception {
        super.before();

        ModelTransferStepResultService manualStepResultService = Mockito.mock(TextResultService.class);
        Mockito.when(stepRepository.getExecutionService(Mockito.any(ModelTransferStep.Type.class)))
            .thenReturn(manualStepResultService);

        Mockito.doReturn(new ModelTransferManualStepProcessor()).when(modelTransferProcessingRepository)
            .getProcessor(ArgumentMatchers.any());

        Mockito.when(modelTransferStepInfoService.getStepInfo(Mockito.anyLong())).thenReturn(manualStepInfo);
    }

    @Test
    public void testManualJobExecutionSkipped() throws Exception {
        manualStepInfo.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.QUEUED)
                .resultType(ResultInfo.Type.EXECUTION).build());

        modelTransferTaskHandler.handle(modelTransferQueueTask, null);

        Assert.assertEquals(1, manualStepInfo.getExecutionResultInfos().size());
        Assert.assertEquals(ResultInfo.Status.QUEUED, manualStepInfo.getExecutionResultInfos().get(0).getStatus());
        Assert.assertEquals(0, manualStepInfo.getValidationResultInfos().size());
    }

    @Test
    public void testManualJobSuccessfulValidation() throws Exception {
        Mockito.doReturn(new TicketStepConfig(MANUAL_STEP_ID, "Manual step", "MBO-12345"))
            .when(modelTransferTaskHandler).getConfig(Mockito.any(), Mockito.anyMap());

        manualStepInfo.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
                .resultType(ResultInfo.Type.EXECUTION).completed(new Date()).build());

        manualStepInfo.getValidationResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.QUEUED).resultType(ResultInfo.Type.VALIDATION).build());

        modelTransferTaskHandler.handle(modelTransferQueueTask, null);

        Assert.assertEquals(1, manualStepInfo.getExecutionResultInfos().size());
        Assert.assertEquals(ResultInfo.Status.COMPLETED, manualStepInfo.getExecutionResultInfos().get(0).getStatus());
        Assert.assertEquals(1, manualStepInfo.getValidationResultInfos().size());
        Assert.assertEquals(ResultInfo.Status.COMPLETED, manualStepInfo.getValidationResultInfos().get(0).getStatus());
    }

    @Test
    public void testManualJobUnuccessfulValidationIfNoTicketNumber() throws Exception {
        manualStepInfo.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
                .resultType(ResultInfo.Type.EXECUTION).completed(new Date()).build());

        manualStepInfo.getValidationResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.QUEUED).resultType(ResultInfo.Type.VALIDATION).build());

        Mockito.doReturn(new TicketStepConfig(MANUAL_STEP_ID, "Manual step", null))
            .when(modelTransferTaskHandler).getConfig(Mockito.any(), Mockito.anyMap());

        modelTransferTaskHandler.handle(modelTransferQueueTask, null);

        Assert.assertEquals(1, manualStepInfo.getExecutionResultInfos().size());
        Assert.assertEquals(ResultInfo.Status.COMPLETED, manualStepInfo.getExecutionResultInfos().get(0).getStatus());
        Assert.assertEquals(1, manualStepInfo.getValidationResultInfos().size());
        Assert.assertEquals(ResultInfo.Status.FAILED, manualStepInfo.getValidationResultInfos().get(0).getStatus());
    }

    @Test
    public void testManualJobUnuccessfulValidationIfTicketNumberIncorrect() throws Exception {
        Mockito.doReturn(new TicketStepConfig(MANUAL_STEP_ID, "Manual step", "Kerkewek!"))
            .when(modelTransferTaskHandler).getConfig(Mockito.any(), Mockito.anyMap());

        manualStepInfo.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
                .resultType(ResultInfo.Type.EXECUTION).completed(new Date()).build());

        manualStepInfo.getValidationResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.QUEUED).resultType(ResultInfo.Type.VALIDATION).build());

        modelTransferTaskHandler.handle(modelTransferQueueTask, null);

        Assert.assertEquals(1, manualStepInfo.getValidationResultInfos().size());
        Assert.assertEquals(ResultInfo.Status.FAILED, manualStepInfo.getValidationResultInfos().get(0).getStatus());
        Assert.assertEquals("Ticket number Kerkewek! is incorrect!",
            manualStepInfo.getValidationResultInfos().get(0).getResultText());
    }

    @Test
    public void testManualJobNoValidationIfExecutionInProgress() throws Exception {
        manualStepInfo.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.IN_PROGRESS)
                .resultType(ResultInfo.Type.EXECUTION).build());

        modelTransferTaskHandler.handle(modelTransferQueueTask, null);

        Assert.assertEquals(0, manualStepInfo.getValidationResultInfos().size());
    }

    @Test
    public void testManualJobNoValidationIfExecutionFailed() throws Exception {
        manualStepInfo.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.FAILED)
                .resultType(ResultInfo.Type.EXECUTION).completed(new Date()).build());

        modelTransferTaskHandler.handle(modelTransferQueueTask, null);

        Assert.assertEquals(0, manualStepInfo.getValidationResultInfos().size());
    }

    @Test
    public void testManualJobNoValidationIfExecutionCompletedButNoValidationEnqueued() throws Exception {
        manualStepInfo.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
                .resultType(ResultInfo.Type.EXECUTION).completed(new Date()).build());

        modelTransferTaskHandler.handle(modelTransferQueueTask, null);

        Assert.assertEquals(0, manualStepInfo.getValidationResultInfos().size());
    }

    @Test
    public void testManualJobCanBeValidatedAgain() throws Exception {
        manualStepInfo.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
                .resultType(ResultInfo.Type.EXECUTION).started(new Date(1000)).completed(new Date(2000)).build());
        manualStepInfo.getValidationResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.FAILED)
                .resultType(ResultInfo.Type.VALIDATION).started(new Date(3000)).completed(new Date(4000)).build());
        manualStepInfo.getValidationResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.QUEUED)
                .resultType(ResultInfo.Type.VALIDATION).started(new Date(5000)).build());

        Mockito.doReturn(new TicketStepConfig(MANUAL_STEP_ID, "Manual step", "MBO-12345"))
            .when(modelTransferTaskHandler).getConfig(Mockito.any(), Mockito.anyMap());

        modelTransferTaskHandler.handle(modelTransferQueueTask, null);

        Assert.assertEquals(1, manualStepInfo.getExecutionResultInfos().size());
        Assert.assertEquals(ResultInfo.Status.COMPLETED, manualStepInfo.getExecutionResultInfos().get(0).getStatus());
        Assert.assertEquals(2, manualStepInfo.getValidationResultInfos().size());
        Assert.assertEquals(ResultInfo.Status.FAILED, manualStepInfo.getValidationResultInfos().get(0).getStatus());
        Assert.assertEquals(ResultInfo.Status.COMPLETED, manualStepInfo.getValidationResultInfos().get(1).getStatus());
    }

    @Test
    public void testManualJobOldStatusesIgnored() throws Exception {
        manualStepInfo.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
                .resultType(ResultInfo.Type.EXECUTION).started(new Date(1000)).completed(new Date(2000)).build());
        manualStepInfo.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
                .resultType(ResultInfo.Type.EXECUTION).started(new Date(5000)).completed(new Date(6000)).build());
        manualStepInfo.getValidationResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.QUEUED)
                .resultType(ResultInfo.Type.VALIDATION).started(new Date(3000)).build());

        modelTransferTaskHandler.handle(modelTransferQueueTask, null);

        Assert.assertEquals(2, manualStepInfo.getExecutionResultInfos().size());
        Assert.assertEquals(ResultInfo.Status.COMPLETED, manualStepInfo.getExecutionResultInfos().get(0).getStatus());
        Assert.assertEquals(ResultInfo.Status.COMPLETED, manualStepInfo.getExecutionResultInfos().get(1).getStatus());
        Assert.assertEquals(1, manualStepInfo.getValidationResultInfos().size());
        Assert.assertEquals(ResultInfo.Status.QUEUED, manualStepInfo.getValidationResultInfos().get(0).getStatus());
    }

    @Test(expected = RuntimeException.class)
    public void testResultInfoWithoutCompletedDateValidationFails() throws Exception {
        manualStepInfo.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
                .resultType(ResultInfo.Type.EXECUTION).build());

        modelTransferTaskHandler.handle(modelTransferQueueTask, null);
    }

    @Override
    protected List<ModelTransferStepInfo> getStepInfos() {
        manualStepInfo = getManualStepInfo();
        return Collections.singletonList(manualStepInfo);
    }

    private ModelTransferStepInfo getManualStepInfo() {
        ModelTransferStepInfo stepInfo = new ModelTransferStepInfo();
        stepInfo.setId(MANUAL_STEP_ID);
        stepInfo.setStepType(ModelTransferStep.Type.MANUAL_STEP_STUB);
        stepInfo.setExecutionResultInfos(executionResultInfos);
        stepInfo.setValidationResultInfos(validationResultInfos);

        return stepInfo;
    }
}
