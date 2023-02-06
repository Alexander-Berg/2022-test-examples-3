package ru.yandex.market.gutgin.tms.pipeline.dcp.xls;


import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.partner.content.common.BaseDcpExcelDBStateGenerator;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileProcessState;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessFileData;

import static org.assertj.core.api.Assertions.assertThat;

public class WaitInQueueTaskActionTest extends BaseDcpExcelDBStateGenerator {

    WaitInQueueTaskAction taskAction;

    @Before
    public void setUp() {
        taskAction = new WaitInQueueTaskAction(
            businessIdXlsExtractor,
            fileDataProcessRequestDao,
            waitUntilFinishedPipelineDao
        );
    }

    @Test
    public void whenProcessIsOldestForBusinessShouldNotWait() {
        long oldestRequest = createFileDataProcessRequest(SOURCE_ID);
        long oldestProcess = createFileProcessId(oldestRequest);
        long newestRequest = createFileDataProcessRequest(SOURCE_ID);
        long newestProcess = createFileProcessId(newestRequest);

        ProcessFileData oldestProcessFileData = new ProcessFileData(oldestRequest, oldestProcess);

        ProcessTaskResult<ProcessFileData> result = taskAction.doRun(oldestProcessFileData);
        assertThat(result.getProblems()).isEmpty();
        assertThat(result.hasResult()).isTrue();
    }

    @Test
    public void whenProcessIsNotOldestForBusinessShouldWaitAndAddProblem() {
        long oldestRequest = createFileDataProcessRequest(SOURCE_ID);
        long oldestProcess = createFileProcessId(oldestRequest);
        long newestRequest = createFileDataProcessRequest(SOURCE_ID);
        long newestProcess = createFileProcessId(newestRequest);

        ProcessFileData newestProcessFileData = new ProcessFileData(newestRequest, newestProcess);

        ProcessTaskResult<ProcessFileData> result = taskAction.doRun(newestProcessFileData);
        //making problems empty to speed up processing
        assertThat(result.getProblems()).isEmpty();
        assertThat(result.hasResult()).isFalse();
    }

    @Test
    public void whenDifferentBusinessShouldNotWaitEachOther() {
        long oneBusinessRequest = createFileDataProcessRequest(SOURCE_ID);
        long oneBusinessProcess = createFileProcessId(oneBusinessRequest);
        int anotherSourceId = SOURCE_ID + 1;
        int anotherBusinessId = BUSINESS_ID + 1;
        createSource(anotherSourceId, anotherBusinessId);
        long anotherBusinessRequest = createFileDataProcessRequest(anotherSourceId, anotherBusinessId);
        long anotherBusinessProcess = createFileProcessId(anotherBusinessRequest);

        ProcessFileData oneBusinessProcessFileData = new ProcessFileData(oneBusinessRequest, oneBusinessProcess);
        ProcessFileData anotherBusinessProcessFileData = new ProcessFileData(anotherBusinessRequest, anotherBusinessProcess);

        ProcessTaskResult<ProcessFileData> oneBusinessResult = taskAction.doRun(oneBusinessProcessFileData);
        ProcessTaskResult<ProcessFileData> anotherBusinessResult = taskAction.doRun(anotherBusinessProcessFileData);

        assertThat(oneBusinessResult.getProblems()).isEmpty();
        assertThat(oneBusinessResult.hasResult()).isTrue();
        assertThat(anotherBusinessResult.getProblems()).isEmpty();
        assertThat(anotherBusinessResult.hasResult()).isTrue();
    }

    @Test
    public void whenProcessIsOldestForBusinessExceptTerminalStatusesShouldNotWait() {
        long oldestRequest = createFileDataProcessRequest(SOURCE_ID);
        long oldestProcess = createFileProcessId(oldestRequest);
        fileProcessDao.updateFileProcess(oldestProcess, FileProcessState.FINISHED);
        long midRequest = createFileDataProcessRequest(SOURCE_ID);
        long midProcess = createFileProcessId(midRequest);
        fileProcessDao.updateFileProcess(midProcess, FileProcessState.INVALID);
        long newestRequest = createFileDataProcessRequest(SOURCE_ID);
        long newestProcess = createFileProcessId(newestRequest);

        ProcessFileData newestProcessFileData = new ProcessFileData(newestRequest, newestProcess);

        ProcessTaskResult<ProcessFileData> result = taskAction.doRun(newestProcessFileData);
        assertThat(result.getProblems()).isEmpty();
        assertThat(result.hasResult()).isTrue();
    }
}