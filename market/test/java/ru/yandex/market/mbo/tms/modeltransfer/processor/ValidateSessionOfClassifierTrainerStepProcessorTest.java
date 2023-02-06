package ru.yandex.market.mbo.tms.modeltransfer.processor;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.ir.http.ClassifierTrainerApi.CheckSessionsStatusResponse;
import ru.yandex.market.mbo.db.transfer.ModelTransferBuilder;
import ru.yandex.market.mbo.export.client.classifiertrainer.ClassifierTrainerServiceClient;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStep;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStep.Type;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStepInfo;
import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;
import ru.yandex.market.mbo.gwt.models.transfer.step.EmptyStepConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.TextResult;
import ru.yandex.market.mbo.tms.modeltransfer.ModelTransferJobContext;
import ru.yandex.market.mbo.tms.modeltransfer.ResultInfoBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.tms.modeltransfer.processor.ValidateSessionOfClassifierTrainerStepProcessor.FAILURE_MESSAGE;
import static ru.yandex.market.mbo.tms.modeltransfer.processor.ValidateSessionOfClassifierTrainerStepProcessor.getCategoryMessage;

/**
 * @author danfertev
 * @since 09.11.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ValidateSessionOfClassifierTrainerStepProcessorTest {
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private ValidateSessionOfClassifierTrainerStepProcessor processor;
    private ClassifierTrainerServiceClient client;
    private ModelTransferStepInfo stepInfo;
    private ModelTransferJobContext<EmptyStepConfig> context;
    private final List<Type> stepDependencies = new ArrayList<>();

    @Before
    public void setUp() {
        client = mock(ClassifierTrainerServiceClient.class);

        processor = new ValidateSessionOfClassifierTrainerStepProcessor(client, stepDependencies);

        stepInfo = stepInfo(Type.UNFREEZE_DUMP_FOR_CLASSIFIER,
            ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
                .completed(date("01.01.1970 00:00:00"))
                .build());

        stepDependencies.add(Type.UNFREEZE_DUMP_FOR_CLASSIFIER);
    }

    @Test
    public void noDependencies() {
        stepDependencies.clear();

        context = jobContext(ModelTransferBuilder.newBuilder()
            .sourceCategory(1L).destinationCategory(2L), stepInfo);

        TextResult result = processor.executeStep(new ResultInfo(), context);

        assertResult(result, ResultInfo.Status.FAILED, String.format(
            "%s Причина: невозможно получить дату завершения зависимых шагов.",
            FAILURE_MESSAGE));
    }

    @Test
    public void noResultsForDependencies() {
        stepDependencies.add(Type.SAVE_MODELS_AND_OFFERS_TO_FUTURE_VALIDATION);

        context = jobContext(ModelTransferBuilder.newBuilder()
            .sourceCategory(1L).destinationCategory(2L));

        TextResult result = processor.executeStep(new ResultInfo(), context);

        assertResult(result, ResultInfo.Status.FAILED, String.format(
            "%s Причина: невозможно получить дату завершения зависимых шагов.",
            FAILURE_MESSAGE));
    }

    @Test
    public void nullResponse() {
        context = jobContext(ModelTransferBuilder.newBuilder()
            .sourceCategory(1L).destinationCategory(2L), stepInfo);

        TextResult result = processor.executeStep(new ResultInfo(), context);

        assertResult(result, ResultInfo.Status.FAILED, String.format("%s %s Причина: ответ не содержит статус.",
            FAILURE_MESSAGE, getCategoryMessage(Arrays.asList(1L, 2L), Collections.emptyList())));
    }

    @Test
    public void noResponseStatus() {
        context = jobContext(ModelTransferBuilder.newBuilder()
            .sourceCategory(1L).destinationCategory(2L), stepInfo);

        when(client.checkSessionsStatus(anyLong(), anyList(), anyList()))
            .thenReturn(CheckSessionsStatusResponse.newBuilder().build());

        TextResult result = processor.executeStep(new ResultInfo(), context);

        assertResult(result, ResultInfo.Status.FAILED, String.format("%s %s Причина: ответ не содержит статус.",
            FAILURE_MESSAGE, getCategoryMessage(Arrays.asList(1L, 2L), Collections.emptyList())));
    }

    @Test
    public void exceptionDuringRequest() {
        context = jobContext(ModelTransferBuilder.newBuilder()
            .sourceCategory(1L).destinationCategory(2L), stepInfo);

        RuntimeException e = new RuntimeException("Error.");
        doThrow(e).when(client).checkSessionsStatus(anyLong(), anyList(), anyList());

        TextResult result = processor.executeStep(new ResultInfo(), context);

        assertResult(result, ResultInfo.Status.FAILED, String.format(
            "%s %s Причина: произошла ошибка во время запроса - %s.",
            FAILURE_MESSAGE, getCategoryMessage(Arrays.asList(1L, 2L), Collections.emptyList()),
            e.getLocalizedMessage()));
    }

    @Test
    public void getMaxResultTimeForTimestamp() {
        stepDependencies.add(Type.SAVE_MODELS_AND_OFFERS_TO_FUTURE_VALIDATION);

        ModelTransferStepInfo stepInfo1 = stepInfo(Type.UNFREEZE_DUMP_FOR_CLASSIFIER,
            ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
                .completed(date("01.01.1970 00:00:00"))
                .build());

        ModelTransferStepInfo stepInfo2 = stepInfo(Type.SAVE_MODELS_AND_OFFERS_TO_FUTURE_VALIDATION,
            ResultInfoBuilder.newBuilder(ResultInfo.Status.COMPLETED)
                .completed(date("01.01.1970 00:00:01"))
                .build());
        context = jobContext(ModelTransferBuilder.newBuilder()
                .sourceCategory(1L).destinationCategory(2L),
            stepInfo1, stepInfo2);

        processor.executeStep(new ResultInfo(), context);

        verify(client).checkSessionsStatus(eq(date("01.01.1970 00:00:01").getTime()), anyList(), anyList());
    }

    @Test
    public void excludeUnpublishOrNotLeaf() {
        context = jobContext(ModelTransferBuilder.newBuilder()
            .sourceCategory(1L, true, true)
            .sourceCategory(2L, false, false)
            .destinationCategory(3L), stepInfo);

        processor.executeStep(new ResultInfo(), context);

        verify(client).checkSessionsStatus(eq(date("01.01.1970 00:00:00").getTime()),
            eq(Collections.singletonList(3L)), eq(Arrays.asList(1L, 2L)));
    }

    @Test
    public void okResponseStatus() {
        context = jobContext(ModelTransferBuilder.newBuilder()
            .sourceCategory(1L, true).destinationCategory(2L), stepInfo);

        List<String> sessions = Arrays.asList("session1", "session2");
        when(client.checkSessionsStatus(anyLong(), anyList(), anyList()))
            .thenReturn(CheckSessionsStatusResponse.newBuilder()
                .setStatus(CheckSessionsStatusResponse.ResultStatus.OK)
                .addAllSessionId(sessions)
                .build());

        TextResult result = processor.executeStep(new ResultInfo(), context);

        assertResult(result, ResultInfo.Status.COMPLETED, String.format(
            "Сессия трейнера классификатора корректна. Успешные сессии: %s. %s",
            String.join(", ", sessions),
            getCategoryMessage(Collections.singletonList(2L), Collections.singletonList(1L))));
    }

    @Test
    public void noDataResponseStatus() {
        context = jobContext(ModelTransferBuilder.newBuilder()
            .sourceCategory(1L, true).destinationCategory(2L), stepInfo);

        when(client.checkSessionsStatus(anyLong(), anyList(), anyList()))
            .thenReturn(CheckSessionsStatusResponse.newBuilder()
                .setStatus(CheckSessionsStatusResponse.ResultStatus.NO_DATA)
                .setFailCause(CheckSessionsStatusResponse.FailCause.PROCESSING)
                .build());

        TextResult result = processor.executeStep(new ResultInfo(), context);

        assertResult(result, ResultInfo.Status.FAILED, String.format("%s %s Причина: %s.",
            FAILURE_MESSAGE,
            getCategoryMessage(Collections.singletonList(2L), Collections.singletonList(1L)),
            "сессия еще готовится (" + CheckSessionsStatusResponse.FailCause.PROCESSING.name() + ")"));
    }

    @Test
    public void unknownResponseStatus() {
        context = jobContext(ModelTransferBuilder.newBuilder()
            .sourceCategory(1L, true).destinationCategory(2L), stepInfo);

        when(client.checkSessionsStatus(anyLong(), anyList(), anyList()))
            .thenReturn(CheckSessionsStatusResponse.newBuilder()
                .setStatus(CheckSessionsStatusResponse.ResultStatus.UNKNOWN_STATUS)
                .build());

        TextResult result = processor.executeStep(new ResultInfo(), context);

        assertResult(result, ResultInfo.Status.FAILED, String.format("%s %s Причина: неизвестный статус.",
            FAILURE_MESSAGE,
            getCategoryMessage(Collections.singletonList(2L), Collections.singletonList(1L))));
    }

    private ModelTransferJobContext<EmptyStepConfig> jobContext(ModelTransferBuilder modelTransferBuilder,
                                                                ModelTransferStepInfo... stepInfos) {
        return new ModelTransferJobContext<>(modelTransferBuilder.build(), null,
            Arrays.asList(stepInfos), new EmptyStepConfig(), Collections.emptyList());
    }

    private void assertResult(TextResult result, ResultInfo.Status status, String resultText) {
        assertThat(result.getResultInfo().getStatus()).isEqualTo(status);
        assertThat(result.getText()).isEqualTo(resultText);
    }

    private ModelTransferStepInfo stepInfo(ModelTransferStep.Type stepType, ResultInfo... executionResults) {
        ModelTransferStepInfo si = new ModelTransferStepInfo();
        si.setStepType(stepType);
        for (ResultInfo ri : executionResults) {
            si.getExecutionResultInfos().add(ri);
        }
        return si;
    }

    private Date date(String textDate) {
        try {
            return SIMPLE_DATE_FORMAT.parse(textDate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
