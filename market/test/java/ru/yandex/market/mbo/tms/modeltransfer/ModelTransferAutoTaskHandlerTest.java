package ru.yandex.market.mbo.tms.modeltransfer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import ru.yandex.market.mbo.db.transfer.step.ModelTransferQueueTask;
import ru.yandex.market.mbo.db.transfer.step.result.ModelTransferStepResultService;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStep;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStepInfo;
import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author dmserebr
 * @date 20.08.18
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ModelTransferAutoTaskHandlerTest extends ModelTransferTaskHandlerTestBase {

    private static final long AUTO_STEP_ID = 110L;
    private static final long PREVIOUS_STEP_ID = 109L;

    private ModelTransferStepInfo autoStepInfo;

    private ModelTransferQueueTask modelTransferQueueTask = new ModelTransferQueueTask()
        .setModelTransferStepId(AUTO_STEP_ID);

    @Before
    public void before() throws Exception {
        super.before();

        Mockito.doReturn(new ModelTransferAutoStepProcessorStub()).when(modelTransferProcessingRepository)
            .getProcessor(ArgumentMatchers.any());

        ModelTransferStepResultService autoStepExecutionResultService =
            Mockito.mock(ModelTransferAutoStepResultServiceStub.class);

        Mockito.when(modelTransferStepInfoService.getStepInfo(Mockito.anyLong())).thenReturn(autoStepInfo);

        Mockito.when(stepRepository.getExecutionService(Mockito.any(ModelTransferStep.Type.class)))
            .thenReturn(autoStepExecutionResultService);
    }

    @Test
    public void testNextAutoStepExecutionIsBeingEnqueued() throws Exception {
        autoStepInfo.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.QUEUED)
                .resultType(ResultInfo.Type.EXECUTION).build());

        ModelTransferStepInfo nextStep =  getAutoStepInfo(12, ModelTransferStep.Type.AUTO_STEP_STUB,
            Collections.emptyList(), Collections.emptyList());

        stepInfos = Arrays.asList(
            getAutoStepInfo(AUTO_STEP_ID, ModelTransferStep.Type.MANUAL_STEP_STUB,
                executionResultInfos, validationResultInfos),
            nextStep
        );

        Mockito.when(modelTransferStepInfoService.getStepInfos(Mockito.anyLong())).thenReturn(stepInfos);

        Mockito.doAnswer(invocation -> {
            ModelTransferStepInfo stepInfo = invocation.getArgument(0);
            stepInfo.setReadyToExecute(true);
            return null;
        }).when(modelTransferStepDependencyService)
            .updateIsReadyToExecute(Mockito.any(ModelTransferStepInfo.class));

       Mockito.when(modelTransferStepDependencyService.getDependencies(ModelTransferStep.Type.AUTO_STEP_STUB))
           .thenReturn(Collections.singletonList(ModelTransferStep.Type.MANUAL_STEP_STUB));

        modelTransferTaskHandler.handle(modelTransferQueueTask, null);

        // Verify that next auto step is being enqueued
        Mockito.verify(modelTransferStepInfoService, Mockito.times(1))
            .doAction(Mockito.anyLong(), Mockito.any(ModelTransferStep.Action.class),
                Mockito.anyString(), Mockito.anyLong());

        Assert.assertEquals(1, autoStepInfo.getExecutionResultInfos().size());
        Assert.assertEquals(ResultInfo.Status.COMPLETED, autoStepInfo.getExecutionResultInfos().get(0).getStatus());
        Assert.assertEquals(1, autoStepInfo.getValidationResultInfos().size());
        Assert.assertEquals(ResultInfo.Status.COMPLETED, autoStepInfo.getValidationResultInfos().get(0).getStatus());
    }

    @Test
    public void testAutoStepExecutionAndValidation() throws Exception {
        autoStepInfo.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.QUEUED)
                .resultType(ResultInfo.Type.EXECUTION).build());

        modelTransferTaskHandler.handle(modelTransferQueueTask, null);

        Assert.assertEquals(1, autoStepInfo.getExecutionResultInfos().size());
        Assert.assertEquals(ResultInfo.Status.COMPLETED, autoStepInfo.getExecutionResultInfos().get(0).getStatus());
        Assert.assertEquals(1, autoStepInfo.getValidationResultInfos().size());
        Assert.assertEquals(ResultInfo.Status.COMPLETED, autoStepInfo.getValidationResultInfos().get(0).getStatus());
    }

    @Test
    public void testAutoStepNotExecutedIfNotEnqueued() throws Exception {
        modelTransferTaskHandler.handle(modelTransferQueueTask, null);

        Assert.assertEquals(0, autoStepInfo.getExecutionResultInfos().size());
        Assert.assertEquals(0, autoStepInfo.getValidationResultInfos().size());
    }

    @Test
    public void testAutoStepNotExecutedIfExecutionInProgress() throws Exception {
        autoStepInfo.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.IN_PROGRESS)
                .resultType(ResultInfo.Type.EXECUTION).build());

        modelTransferTaskHandler.handle(modelTransferQueueTask, null);

        Assert.assertEquals(1, autoStepInfo.getExecutionResultInfos().size());
        Assert.assertEquals(ResultInfo.Status.IN_PROGRESS, autoStepInfo.getExecutionResultInfos().get(0).getStatus());
        Assert.assertEquals(0, autoStepInfo.getValidationResultInfos().size());
    }

    @Test
    public void testAutoStepNotExecutedIfExecutionFailed() throws Exception {
        autoStepInfo.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.FAILED)
                .resultType(ResultInfo.Type.EXECUTION).completed(new Date()).build());

        modelTransferTaskHandler.handle(modelTransferQueueTask, null);

        Assert.assertEquals(1, autoStepInfo.getExecutionResultInfos().size());
        Assert.assertEquals(ResultInfo.Status.FAILED, autoStepInfo.getExecutionResultInfos().get(0).getStatus());
        Assert.assertEquals(0, autoStepInfo.getValidationResultInfos().size());
    }

    @Test
    public void testAutoStepNotExecutedIfExecutionBlocked() throws Exception {
        autoStepInfo.getBlockedByStepIds().add(PREVIOUS_STEP_ID);

        modelTransferTaskHandler.handle(modelTransferQueueTask, null);

        Assert.assertEquals(0, autoStepInfo.getExecutionResultInfos().size());
        Assert.assertEquals(0, autoStepInfo.getValidationResultInfos().size());
    }

    @Test
    public void testAutoJobCanBeExecutedAndValidatedAgain() throws Exception {
        autoStepInfo.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
                .resultType(ResultInfo.Type.EXECUTION).started(new Date(1000)).completed(new Date(2000)).build());
        autoStepInfo.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.QUEUED)
                .resultType(ResultInfo.Type.EXECUTION).started(new Date(5000)).build());
        autoStepInfo.getValidationResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.FAILED)
                .resultType(ResultInfo.Type.VALIDATION).started(new Date(3000)).completed(new Date(4000)).build());

        modelTransferTaskHandler.handle(modelTransferQueueTask, null);

        Assert.assertEquals(2, autoStepInfo.getExecutionResultInfos().size());
        Assert.assertEquals(ResultInfo.Status.COMPLETED, autoStepInfo.getExecutionResultInfos().get(0).getStatus());
        Assert.assertEquals(ResultInfo.Status.COMPLETED, autoStepInfo.getExecutionResultInfos().get(1).getStatus());
        Assert.assertEquals(2, autoStepInfo.getValidationResultInfos().size());
        Assert.assertEquals(ResultInfo.Status.FAILED, autoStepInfo.getValidationResultInfos().get(0).getStatus());
        Assert.assertEquals(ResultInfo.Status.COMPLETED, autoStepInfo.getValidationResultInfos().get(1).getStatus());
    }

    @Test
    public void testAutoJobCanBeValidatedAgain() throws Exception {
        autoStepInfo.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
                .resultType(ResultInfo.Type.EXECUTION).started(new Date(1000)).completed(new Date(2000)).build());
        autoStepInfo.getValidationResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.FAILED)
                .resultType(ResultInfo.Type.VALIDATION).started(new Date(3000)).completed(new Date(4000)).build());
        autoStepInfo.getValidationResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.QUEUED)
                .resultType(ResultInfo.Type.VALIDATION).started(new Date(5000)).build());

        modelTransferTaskHandler.handle(modelTransferQueueTask, null);

        Assert.assertEquals(1, autoStepInfo.getExecutionResultInfos().size());
        Assert.assertEquals(ResultInfo.Status.COMPLETED, autoStepInfo.getExecutionResultInfos().get(0).getStatus());
        Assert.assertEquals(2, autoStepInfo.getValidationResultInfos().size());
        Assert.assertEquals(ResultInfo.Status.FAILED, autoStepInfo.getValidationResultInfos().get(0).getStatus());
        Assert.assertEquals(ResultInfo.Status.COMPLETED, autoStepInfo.getValidationResultInfos().get(1).getStatus());
    }

    @Override
    protected List<ModelTransferStepInfo> getStepInfos() {
        autoStepInfo = getAutoStepInfo(AUTO_STEP_ID, ModelTransferStep.Type.AUTO_STEP_STUB,
            executionResultInfos, validationResultInfos);
        return Collections.singletonList(autoStepInfo);
    }

    private ModelTransferStepInfo getAutoStepInfo(long id, ModelTransferStep.Type type,
                                                  List<ResultInfo> executionResultInfos,
                                                  List<ResultInfo> validationResultInfos) {
        ModelTransferStepInfo stepInfo = new ModelTransferStepInfo();
        stepInfo.setId(id);
        stepInfo.setStepType(type);
        stepInfo.setStepExecutionType(ModelTransferStep.ExecutionType.AUTO);
        stepInfo.setExecutionResultInfos(executionResultInfos);
        stepInfo.setValidationResultInfos(validationResultInfos);

        return stepInfo;
    }
}
