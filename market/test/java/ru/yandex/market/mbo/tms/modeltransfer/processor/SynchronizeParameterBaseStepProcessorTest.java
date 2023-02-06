package ru.yandex.market.mbo.tms.modeltransfer.processor;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransfer;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStepInfo;
import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;
import ru.yandex.market.mbo.gwt.models.transfer.step.ListOfModelParameterLandingConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.ParameterResultEntry;
import ru.yandex.market.mbo.gwt.models.transfer.step.ParametersResult;
import ru.yandex.market.mbo.gwt.models.transfer.step.ResultEntry;
import ru.yandex.market.mbo.tms.modeltransfer.ListOfModelsConfigBuilder;
import ru.yandex.market.mbo.tms.modeltransfer.ModelTransferJobContext;
import ru.yandex.market.mbo.tms.modeltransfer.ResultInfoBuilder;
import ru.yandex.market.mbo.tms.modeltransfer.worker.ResultEntryWorker;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author danfertev
 * @since 05.10.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SynchronizeParameterBaseStepProcessorTest {
    private SynchronizeParameterBaseStepProcessor processor;
    private ResultEntryWorker<ParameterResultEntry, ListOfModelParameterLandingConfig, Void> worker;

    @Before
    public void setUp() {
        worker = mock(SynchronizeParameterWorkerStub.class);
        processor = new SynchronizeParameterBaseStepProcessorStub(worker);
    }

    @Test
    public void testEmptyResult() {
        when(worker.doWork(any())).thenReturn(Collections.emptyList());

        ParametersResult result = processor.executeStep(resultInfo(), context());

        assertThat(result.getResultInfo().getStatus()).isEqualTo(ResultInfo.Status.COMPLETED);
        assertThat(result.getResultInfo().getResultText())
            .isEqualTo(SynchronizeParameterBaseStepProcessorStub.NO_SYNC_MESSAGE);
        assertThat(result.getResultEntries()).isEmpty();
    }

    @Test
    public void testAllFailureResult() {
        when(worker.doWork(any())).thenReturn(Arrays.asList(
            ParameterResultEntryBuilder.newBuilder().status(ResultEntry.Status.FAILURE).build(),
            ParameterResultEntryBuilder.newBuilder().status(ResultEntry.Status.FAILURE).build()
        ));

        ParametersResult result = processor.executeStep(resultInfo(), context());

        assertThat(result.getResultInfo().getStatus()).isEqualTo(ResultInfo.Status.FAILED);
        assertThat(result.getResultInfo().getResultText())
            .isEqualTo(SynchronizeParameterBaseStepProcessorStub.FAILURE_MESSAGE);
        assertThat(result.getResultEntries()).hasSize(2);
    }

    @Test
    public void testOneFailureResult() {
        when(worker.doWork(any())).thenReturn(Arrays.asList(
            ParameterResultEntryBuilder.newBuilder().status(ResultEntry.Status.FAILURE).build(),
            ParameterResultEntryBuilder.newBuilder().status(ResultEntry.Status.SUCCESS).build()
        ));

        ParametersResult result = processor.executeStep(resultInfo(), context());

        assertThat(result.getResultInfo().getStatus()).isEqualTo(ResultInfo.Status.FAILED);
        assertThat(result.getResultInfo().getResultText())
            .isEqualTo(SynchronizeParameterBaseStepProcessorStub.FAILURE_MESSAGE);
        assertThat(result.getResultEntries()).hasSize(2);
    }

    @Test
    public void testSuccessResult() {
        when(worker.doWork(any())).thenReturn(Arrays.asList(
            ParameterResultEntryBuilder.newBuilder().status(ResultEntry.Status.SUCCESS).build(),
            ParameterResultEntryBuilder.newBuilder().status(ResultEntry.Status.SUCCESS).build()
        ));

        ParametersResult result = processor.executeStep(resultInfo(), context());

        assertThat(result.getResultInfo().getStatus()).isEqualTo(ResultInfo.Status.COMPLETED);
        assertThat(result.getResultInfo().getResultText())
            .startsWith(SynchronizeParameterBaseStepProcessorStub.SUCCESS_MESSAGE);
        assertThat(result.getResultEntries()).hasSize(2);
    }

    private ResultInfo resultInfo() {
        return ResultInfoBuilder.newBuilder(ResultInfo.Status.QUEUED).resultType(ResultInfo.Type.EXECUTION).build();
    }

    private ModelTransferJobContext<ListOfModelParameterLandingConfig> context() {
        ModelTransfer modelTransfer = new ModelTransfer();
        ModelTransferStepInfo stepInfo = new ModelTransferStepInfo();
        return new ModelTransferJobContext<>(modelTransfer, stepInfo, Collections.singletonList(stepInfo),
            new ListOfModelParameterLandingConfig(ListOfModelsConfigBuilder.newBuilder()
                .models(1L, 2L, 11L, 22L, 33L)
                .build()),
            Collections.emptyList());
    }
}
