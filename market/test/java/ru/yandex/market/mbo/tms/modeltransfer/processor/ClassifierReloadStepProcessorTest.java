package ru.yandex.market.mbo.tms.modeltransfer.processor;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.ir.http.Classifier;
import ru.yandex.market.ir.http.Classifier.HasReloadFinishedResponse.ReloadStatus;
import ru.yandex.market.ir.http.Classifier.NeedToReloadResponse;
import ru.yandex.market.ir.http.ClassifierService;
import ru.yandex.market.mbo.db.transfer.step.result.ClassifierReloadResultService;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransfer;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStep;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStepInfo;
import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;
import ru.yandex.market.mbo.gwt.models.transfer.step.ClassifierReloadRequestEntry;
import ru.yandex.market.mbo.gwt.models.transfer.step.ClassifierReloadResult;
import ru.yandex.market.mbo.gwt.models.transfer.step.EmptyStepConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.ResultEntry;
import ru.yandex.market.mbo.tms.modeltransfer.ModelTransferJobContext;
import ru.yandex.market.mbo.tms.modeltransfer.ResultInfoBuilder;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author danfertev
 * @since 21.01.2019
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ClassifierReloadStepProcessorTest {
    private static final long REQUEST_ID = 1L;
    private static final long ANOTHER_REQUEST_ID = 2L;

    private ClassifierReloadStepProcessor processor;
    private ClassifierReloadResultService executionResultService;
    private ClassifierReloadResultService validationResultService;
    private ClassifierService classifierService;
    private ModelTransferStepInfo stepInfo;
    private ModelTransferJobContext<EmptyStepConfig> context;

    @Before
    public void setUp() {
        executionResultService = mock(ClassifierReloadResultService.class);
        validationResultService = mock(ClassifierReloadResultService.class);
        classifierService = mock(ClassifierService.class);
        processor = new ClassifierReloadStepProcessor(executionResultService, validationResultService,
            classifierService);
        stepInfo = new ModelTransferStepInfo();
        stepInfo.setStepType(ModelTransferStep.Type.RELOAD_CLASSIFIER);
        context = new ModelTransferJobContext<>(new ModelTransfer(), stepInfo, Collections.singletonList(stepInfo),
            new EmptyStepConfig(), Collections.emptyList());
    }

    @Test
    public void needReloadRequestFailed() {
        doThrow(new RuntimeException("ERROR!")).when(classifierService).needToReloadAllClassifiers(any());

        ClassifierReloadResult result = processor.executeStep(new ResultInfo(), context);

        assertReloadFailure(result, "Ошибка при выполнении запроса: ERROR!",
            ClassifierReloadRequestEntry.RequestStatus.FAILED);
    }

    @Test
    public void needReloadRequestNoRequestId() {
        when(classifierService.needToReloadAllClassifiers(any()))
            .thenReturn(NeedToReloadResponse.newBuilder().build());

        ClassifierReloadResult result = processor.executeStep(new ResultInfo(), context);

        assertReloadFailure(result, "Ответ не содержит reload_request_id",
            ClassifierReloadRequestEntry.RequestStatus.OK);
    }

    @Test
    public void needReloadRequestNoStatus() {
        when(classifierService.needToReloadAllClassifiers(any()))
            .thenReturn(NeedToReloadResponse.newBuilder()
                .setReloadRequestId(REQUEST_ID)
                .build());

        ClassifierReloadResult result = processor.executeStep(new ResultInfo(), context);

        assertReloadFailure(result, "Ответ не содержит статус",
            ClassifierReloadRequestEntry.RequestStatus.OK, REQUEST_ID);
    }

    @Test
    public void needReloadRequestFailureStatus() {
        when(classifierService.needToReloadAllClassifiers(any()))
            .thenReturn(NeedToReloadResponse.newBuilder()
                .setReloadRequestId(REQUEST_ID)
                .setAcceptStatus(NeedToReloadResponse.ReloadRequestAcceptStatus.FAIL)
                .build());

        ClassifierReloadResult result = processor.executeStep(new ResultInfo(), context);

        assertReloadFailure(result, "Классификатор ответил - FAIL",
            ClassifierReloadRequestEntry.RequestStatus.OK, REQUEST_ID);
    }

    @Test
    public void needReloadRequestSuccessStatus() {
        when(classifierService.needToReloadAllClassifiers(any()))
            .thenReturn(NeedToReloadResponse.newBuilder()
                .setReloadRequestId(REQUEST_ID)
                .setAcceptStatus(NeedToReloadResponse.ReloadRequestAcceptStatus.OK)
                .build());

        ClassifierReloadResult result = processor.executeStep(new ResultInfo(), context);

        assertReloadSuccess(result, "Классификатор ответил - ОК",
            ClassifierReloadRequestEntry.RequestStatus.OK, REQUEST_ID);
    }

    @Test
    public void needReloadRequestUnableToLoadPreviousResults() {
        ResultInfo completedValidation = ResultInfoBuilder.newBuilder(ResultInfo.Status.FAILED)
            .resultType(ResultInfo.Type.VALIDATION)
            .started(new Date())
            .completed(new Date())
            .build();
        stepInfo.getValidationResultInfos().add(completedValidation);

        doThrow(new RuntimeException("ERROR!"))
            .when(validationResultService).getResult(eq(completedValidation.getId()));

        ClassifierReloadResult result = processor.executeStep(new ResultInfo(), context);

        verify(classifierService, never()).needToReloadAllClassifiers(any());
        assertReloadFailure(result, "Невозможно получить результаты предыдущих запусков: ERROR!",
            ClassifierReloadRequestEntry.RequestStatus.FAILED);
    }

    @Test
    public void needReloadRequestPreviousValidationCompleted() {
        ResultInfo completedValidation = ResultInfoBuilder.newBuilder(ResultInfo.Status.FAILED)
            .resultType(ResultInfo.Type.VALIDATION)
            .started(new Date())
            .completed(new Date())
            .build();
        stepInfo.getValidationResultInfos().add(completedValidation);
        ClassifierReloadRequestEntry requestEntry = new ClassifierReloadRequestEntry(ANOTHER_REQUEST_ID,
            ClassifierReloadRequestEntry.RequestStatus.OK, ClassifierReloadRequestEntry.ReloadStatus.CHECKING_ERROR);

        when(validationResultService.getResult(eq(completedValidation.getId())))
            .thenReturn(new ClassifierReloadResult(completedValidation, requestEntry));

        when(classifierService.needToReloadAllClassifiers(any()))
            .thenReturn(NeedToReloadResponse.newBuilder()
                .setReloadRequestId(REQUEST_ID)
                .setAcceptStatus(NeedToReloadResponse.ReloadRequestAcceptStatus.OK)
                .build());

        ClassifierReloadResult result = processor.executeStep(new ResultInfo(), context);

        assertReloadSuccess(result, "Классификатор ответил - ОК",
            ClassifierReloadRequestEntry.RequestStatus.OK, REQUEST_ID);
    }

    @Test
    public void needReloadRequestInProgress() {
        ResultInfo completedValidation = ResultInfoBuilder.newBuilder(ResultInfo.Status.FAILED)
            .resultType(ResultInfo.Type.VALIDATION)
            .started(new Date())
            .completed(new Date())
            .build();
        stepInfo.getValidationResultInfos().add(completedValidation);
        ClassifierReloadRequestEntry requestEntry = new ClassifierReloadRequestEntry(ANOTHER_REQUEST_ID,
            ClassifierReloadRequestEntry.RequestStatus.OK, ClassifierReloadRequestEntry.ReloadStatus.NOT_READY);

        when(validationResultService.getResult(eq(completedValidation.getId())))
            .thenReturn(new ClassifierReloadResult(completedValidation, requestEntry));

        ClassifierReloadResult result = processor.executeStep(new ResultInfo(), context);

        verify(classifierService, never()).needToReloadAllClassifiers(any());
        assertReloadSuccess(result, "Запрос на перезагрузку уже отправлен",
            ClassifierReloadRequestEntry.RequestStatus.OK, ANOTHER_REQUEST_ID);
    }

    @Test
    public void checkRequestNoReloadRequest() {
        ClassifierReloadResult result = processor.validateStep(new ResultInfo(), context);

        assertCheckFailure(result, "Нет выполненного запроса на перезагрузку классификатора",
            ClassifierReloadRequestEntry.RequestStatus.FAILED);
    }

    @Test
    public void checkRequestNoSuccessReloadRequest() {
        addExecutionResultInfo(ResultInfo.Status.QUEUED);

        ClassifierReloadResult result = processor.validateStep(new ResultInfo(), context);

        assertCheckFailure(result, "Нет успешно выполненного запроса на перезагрузку классификатора",
            ClassifierReloadRequestEntry.RequestStatus.FAILED);
    }

    @Test
    public void checkRequestUnableToLoadExecutionResult() {
        ResultInfo resultInfo = addExecutionResultInfo(ResultInfo.Status.COMPLETED);

        doThrow(new RuntimeException("ERROR!")).when(executionResultService).getResult(eq(resultInfo.getId()));

        ClassifierReloadResult result = processor.validateStep(new ResultInfo(), context);

        assertCheckFailure(result,
            "Ошибка во время чтения результата запроса на перезагрузку классификатора: ERROR!",
            ClassifierReloadRequestEntry.RequestStatus.FAILED);
    }

    @Test
    public void checkRequestNoStatus() {
        ResultInfo resultInfo = addExecutionResultInfo(ResultInfo.Status.COMPLETED);

        when(executionResultService.getResult(eq(resultInfo.getId())))
            .thenReturn(new ClassifierReloadResult(resultInfo, successEntry()));

        when(classifierService.hasReloadFinished(eq(checkRequest())))
            .thenReturn(Classifier.HasReloadFinishedResponse.newBuilder().build());

        ClassifierReloadResult result = processor.validateStep(new ResultInfo(), context);

        assertCheckFailure(result,
            "Ответ о статусе перезагрузки классификатора не содержит статус",
            ClassifierReloadRequestEntry.RequestStatus.OK, REQUEST_ID);
    }

    @Test
    public void checkRequestUnknownStatus() {
        ResultInfo resultInfo = addExecutionResultInfo(ResultInfo.Status.COMPLETED);

        when(executionResultService.getResult(eq(resultInfo.getId())))
            .thenReturn(new ClassifierReloadResult(resultInfo, successEntry()));

        when(classifierService.hasReloadFinished(eq(checkRequest())))
            .thenReturn(checkResponse(ReloadStatus.UNKNOWN_STATUS));

        ClassifierReloadResult result = processor.validateStep(new ResultInfo(), context);

        assertCheckFailure(result,
            "Неизвестный статус перезагрузки",
            ClassifierReloadRequestEntry.RequestStatus.OK, ClassifierReloadRequestEntry.ReloadStatus.UNKNOWN_STATUS,
            REQUEST_ID);
    }

    @Test
    public void checkRequestUnknownRequestId() {
        ResultInfo resultInfo = addExecutionResultInfo(ResultInfo.Status.COMPLETED);

        when(executionResultService.getResult(eq(resultInfo.getId())))
            .thenReturn(new ClassifierReloadResult(resultInfo, successEntry()));

        when(classifierService.hasReloadFinished(eq(checkRequest())))
            .thenReturn(checkResponse(ReloadStatus.UNKNOWN_REQ_ID));

        ClassifierReloadResult result = processor.validateStep(new ResultInfo(), context);

        assertCheckFailure(result,
            "Неизвестный идентификатор запроса",
            ClassifierReloadRequestEntry.RequestStatus.OK, ClassifierReloadRequestEntry.ReloadStatus.UNKNOWN_REQ_ID,
            REQUEST_ID);
    }

    @Test
    public void checkRequestCheckingError() {
        ResultInfo resultInfo = addExecutionResultInfo(ResultInfo.Status.COMPLETED);

        when(executionResultService.getResult(eq(resultInfo.getId())))
            .thenReturn(new ClassifierReloadResult(resultInfo, successEntry()));

        when(classifierService.hasReloadFinished(eq(checkRequest())))
            .thenReturn(checkResponse(ReloadStatus.CHECKING_ERROR));

        ClassifierReloadResult result = processor.validateStep(new ResultInfo(), context);

        assertCheckFailure(result,
            "Ошибка внутри классификатора при перезагрузке",
            ClassifierReloadRequestEntry.RequestStatus.OK, ClassifierReloadRequestEntry.ReloadStatus.CHECKING_ERROR,
            REQUEST_ID);
    }

    @Test
    public void checkRequestNotReady() {
        ResultInfo resultInfo = addExecutionResultInfo(ResultInfo.Status.COMPLETED);

        when(executionResultService.getResult(eq(resultInfo.getId())))
            .thenReturn(new ClassifierReloadResult(resultInfo, successEntry()));

        when(classifierService.hasReloadFinished(eq(checkRequest())))
            .thenReturn(checkResponse(ReloadStatus.NOT_READY));

        ClassifierReloadResult result = processor.validateStep(new ResultInfo(), context);

        assertCheckFailure(result,
            "Классификатор в процессе перезагрузки",
            ClassifierReloadRequestEntry.RequestStatus.OK, ClassifierReloadRequestEntry.ReloadStatus.NOT_READY,
            REQUEST_ID);
    }

    @Test
    public void checkRequestReady() {
        ResultInfo resultInfo = addExecutionResultInfo(ResultInfo.Status.COMPLETED);

        when(executionResultService.getResult(eq(resultInfo.getId())))
            .thenReturn(new ClassifierReloadResult(resultInfo, successEntry()));

        when(classifierService.hasReloadFinished(eq(checkRequest())))
            .thenReturn(checkResponse(ReloadStatus.READY));

        ClassifierReloadResult result = processor.validateStep(new ResultInfo(), context);

        assertCheckSuccess(result,
            "Классификатор перезагружен",
            ClassifierReloadRequestEntry.RequestStatus.OK, REQUEST_ID);
    }




    @SuppressWarnings("checkstyle:parameterNumber")
    private void assertResult(ClassifierReloadResult result, ResultInfo.Status status, String resultText,
                              ResultEntry.Status entryStatus, String statusMessage,
                              ClassifierReloadRequestEntry.RequestStatus requestStatus,
                              ClassifierReloadRequestEntry.ReloadStatus reloadStatus,
                              Long requestId) {
        ResultInfo resultInfo = result.getResultInfo();
        ClassifierReloadRequestEntry requestEntry = result.getRequestEntry();

        assertThat(resultInfo.getStatus()).isEqualTo(status);
        assertThat(resultInfo.getResultText()).isEqualTo(resultText);
        assertThat(requestEntry.getStatus()).isEqualTo(entryStatus);
        assertThat(requestEntry.getStatusMessage()).isEqualTo(statusMessage);
        assertThat(requestEntry.getRequestStatus()).isEqualTo(requestStatus);
        assertThat(requestEntry.getReloadStatus()).isEqualTo(reloadStatus);
        assertThat(requestEntry.getRequestId()).isEqualTo(requestId);
    }

    private void assertReloadFailure(ClassifierReloadResult result, String statusMessage,
                                     ClassifierReloadRequestEntry.RequestStatus requestStatus,
                                     Long requestId) {
        assertResult(result,
            ResultInfo.Status.FAILED, "Запрос на перезагрузку классификатора завершился с ошибкой",
            ResultEntry.Status.FAILURE, statusMessage, requestStatus, null, requestId);
    }

    private void assertReloadFailure(ClassifierReloadResult result, String statusMessage,
                                     ClassifierReloadRequestEntry.RequestStatus requestStatus) {
        assertReloadFailure(result, statusMessage, requestStatus, null);
    }

    private void assertReloadSuccess(ClassifierReloadResult result, String statusMessage,
                                     ClassifierReloadRequestEntry.RequestStatus requestStatus,
                                     Long requestId) {
        assertResult(result,
            ResultInfo.Status.COMPLETED, "Запрос на перезагрузку классификатора выполнен успешно",
            ResultEntry.Status.SUCCESS, statusMessage, requestStatus, null, requestId);
    }

    private void assertCheckFailure(ClassifierReloadResult result, String statusMessage,
                                    ClassifierReloadRequestEntry.RequestStatus requestStatus,
                                    ClassifierReloadRequestEntry.ReloadStatus reloadStatus,
                                    Long requestId) {
        assertResult(result,
            ResultInfo.Status.FAILED, "Классификатор еще не перезагружен",
            ResultEntry.Status.FAILURE, statusMessage, requestStatus, reloadStatus, requestId);
    }

    private void assertCheckFailure(ClassifierReloadResult result, String statusMessage,
                                    ClassifierReloadRequestEntry.RequestStatus requestStatus,
                                    Long requestId) {
        assertCheckFailure(result, statusMessage, requestStatus, null, requestId);
    }

    private void assertCheckFailure(ClassifierReloadResult result, String statusMessage,
                                    ClassifierReloadRequestEntry.RequestStatus requestStatus) {
        assertCheckFailure(result, statusMessage, requestStatus, null);
    }

    private void assertCheckSuccess(ClassifierReloadResult result, String statusMessage,
                                    ClassifierReloadRequestEntry.RequestStatus requestStatus,
                                    Long requestId) {
        assertResult(result,
            ResultInfo.Status.COMPLETED, "Классификатор успешно перезагружен",
            ResultEntry.Status.SUCCESS, statusMessage, requestStatus, ClassifierReloadRequestEntry.ReloadStatus.READY,
            requestId);
    }

    private ResultInfo addExecutionResultInfo(ResultInfo.Status status) {
        ResultInfo resultInfo = ResultInfoBuilder.newBuilder(status)
            .resultType(ResultInfo.Type.EXECUTION)
            .build();
        stepInfo.getExecutionResultInfos().add(resultInfo);

        return resultInfo;
    }

    private Classifier.HasReloadFinishedRequest checkRequest() {
        return Classifier.HasReloadFinishedRequest.newBuilder()
            .setReloadRequestId(REQUEST_ID)
            .build();
    }

    private Classifier.HasReloadFinishedResponse checkResponse(ReloadStatus status) {
        return Classifier.HasReloadFinishedResponse.newBuilder()
            .setStatus(status)
            .build();
    }

    private static ClassifierReloadRequestEntry successEntry() {
        return new ClassifierReloadRequestEntry(REQUEST_ID, ClassifierReloadRequestEntry.RequestStatus.OK,
            ClassifierReloadRequestEntry.ReloadStatus.READY);
    }
}
