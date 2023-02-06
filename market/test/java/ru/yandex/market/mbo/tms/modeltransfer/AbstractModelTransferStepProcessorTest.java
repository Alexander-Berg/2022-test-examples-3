package ru.yandex.market.mbo.tms.modeltransfer;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.db.transfer.step.ModelTransferQueueTask;
import ru.yandex.market.mbo.db.transfer.step.result.ModelTransferStepResultService;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStep;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStepInfo;
import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;
import ru.yandex.market.mbo.gwt.models.transfer.step.TextResult;
import ru.yandex.market.mbo.tms.modeltransfer.processor.ModelTransferAutoStepProcessorBase;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author s-ermakov
 */
public class AbstractModelTransferStepProcessorTest extends ModelTransferTaskHandlerTestBase {

    private static final long AUTO_STEP_ID = 110L;

    private ModelTransferStepInfo autoStepInfo;

    private ModelTransferQueueTask modelTransferQueueTask = new ModelTransferQueueTask()
        .setModelTransferStepId(AUTO_STEP_ID);

    @Before
    public void before() throws Exception {
        super.before();

        ModelTransferStepResultService autoStepExecutionResultService =
            Mockito.mock(ModelTransferAutoStepResultServiceStub.class);

        Mockito.when(stepRepository.getExecutionService(Mockito.any(ModelTransferStep.Type.class)))
            .thenReturn(autoStepExecutionResultService);

        Mockito.when(modelTransferStepInfoService.getStepInfo(Mockito.anyLong())).thenReturn(autoStepInfo);
    }

    @Test
    public void testUnhandledExceptionInExecuteStepIsSavedToDb() {
        // arrange
        Mockito.doReturn(new FailExecuteStepProcessor()).when(modelTransferProcessingRepository)
            .getProcessor(Mockito.any());
        autoStepInfo.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.QUEUED)
                .resultType(ResultInfo.Type.EXECUTION).build());

        // act
        Assertions.assertThatThrownBy(() -> modelTransferTaskHandler.handle(modelTransferQueueTask, null))
            .isInstanceOf(RuntimeException.class);

        // assert
        Assertions.assertThat(executionResultInfos).hasSize(1);
        Assertions.assertThat(executionResultInfos.get(0).getResultText())
            .contains("java.lang.RuntimeException", "Fail everything message");
    }

    @Test
    public void testUnhandledExceptionInValidateStepIsSavedToDb() {
        // arrange
        Mockito.doAnswer(invocation -> new FailValidateStepProcessor())
            .when(modelTransferProcessingRepository).getProcessor(Mockito.any());
        autoStepInfo.getExecutionResultInfos().add(
            ResultInfoBuilder.newBuilder(ResultInfo.Status.QUEUED)
                .resultType(ResultInfo.Type.EXECUTION).build());

        // act
        Assertions.assertThatThrownBy(() -> modelTransferTaskHandler.handle(modelTransferQueueTask, null))
            .isInstanceOf(RuntimeException.class);

        // assert
        Assertions.assertThat(validationResultInfos).hasSize(1);
        Assertions.assertThat(validationResultInfos.get(0).getResultText())
            .contains("java.lang.NullPointerException", "Failed validation");
    }

    @Override
    protected List<ModelTransferStepInfo> getStepInfos() {
        autoStepInfo = getAutoStepInfo();
        return Collections.singletonList(autoStepInfo);
    }

    private ModelTransferStepInfo getAutoStepInfo() {
        ModelTransferStepInfo stepInfo = new ModelTransferStepInfo();
        stepInfo.setId(AUTO_STEP_ID);
        stepInfo.setStepType(ModelTransferStep.Type.AUTO_STEP_STUB);
        stepInfo.setExecutionResultInfos(executionResultInfos);
        stepInfo.setValidationResultInfos(validationResultInfos);

        return stepInfo;
    }

    private class FailExecuteStepProcessor extends ModelTransferAutoStepProcessorBase<
        TextResult, TextResult, ModelTransferStepConfigStub> {

        @Override
        public TextResult executeStep(ResultInfo resultInfo,
                                      ModelTransferJobContext<ModelTransferStepConfigStub> context) {
            throw new RuntimeException("Fail everything message");
        }

        @Override
        public TextResult validateStep(ResultInfo resultInfo,
                                       ModelTransferJobContext<ModelTransferStepConfigStub> context) {
            resultInfo.setStatus(ResultInfo.Status.COMPLETED);
            resultInfo.setCompleted(new Date());
            return new TextResult(resultInfo);
        }

        @Override
        public TextResult createExecutionResult() {
            return new TextResult();
        }

        @Override
        public TextResult createValidationResult() {
            return new TextResult();
        }
    }

    private class FailValidateStepProcessor extends ModelTransferAutoStepProcessorBase<
        TextResult, TextResult, ModelTransferStepConfigStub> {

        @Override
        public TextResult executeStep(ResultInfo resultInfo,
                                      ModelTransferJobContext<ModelTransferStepConfigStub> context) {
            resultInfo.setStatus(ResultInfo.Status.COMPLETED);
            resultInfo.setCompleted(new Date());
            return new TextResult(resultInfo);
        }

        @Override
        public TextResult validateStep(ResultInfo resultInfo,
                                       ModelTransferJobContext<ModelTransferStepConfigStub> context) {
            throw new NullPointerException("Failed validation");
        }

        @Override
        public TextResult createExecutionResult() {
            return new TextResult();
        }

        @Override
        public TextResult createValidationResult() {
            return new TextResult();
        }
    }
}
