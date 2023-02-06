package ru.yandex.market.gutgin.tms.pipeline.good.taskaction;

import io.qameta.allure.Issue;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.gutgin.tms.base.BaseDbGutGinTest;
import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.partner.content.common.db.dao.FileDataProcessRequestDao;
import ru.yandex.market.partner.content.common.db.dao.FileProcessDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileProcessState;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.FileDataProcessRequest;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.FileProcess;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessFileData;
import ru.yandex.market.partner.content.common.engine.parameter.RequestProcessFileData;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.ProblemInfoAssert.assertProblemInfoDescription;

/**
 * @author danfertev
 * @since 28.05.2019
 */
@Issue("MARKETIR-8538")
public class CreateFileProcessTaskActionTest extends BaseDbGutGinTest {
    private static final long REQUEST_ID = 1L;
    private static final int SOURCE_ID = 2;
    private static final String FILE_URL = "url";
    private static final int SUPPLIER_ID = 3;

    private CreateFileProcessTaskAction taskAction;

    @Resource
    private FileDataProcessRequestDao fileDataProcessRequestDao;
    @Resource
    private FileProcessDao fileProcessDao;

    @Before
    public void setUp() {
        taskAction = new CreateFileProcessTaskAction();
        taskAction.setFileDataProcessRequestDao(fileDataProcessRequestDao);
        taskAction.setFileProcessDao(fileProcessDao);
    }

    @Test
    public void testUnknownRequestId() {
        RequestProcessFileData input = new RequestProcessFileData(REQUEST_ID);
        ProcessTaskResult<ProcessFileData> result = taskAction.apply(input);

        assertThat(result.getProblems()).hasOnlyOneElementSatisfying(pi ->
            assertProblemInfoDescription(
                pi,
                CreateFileProcessTaskAction.unknownRequestIdProblem(REQUEST_ID)
            )
        );
    }

    @Test
    public void testUnsupportedDynamicFile() {
        fileDataProcessRequestDao.insert(createRequest(true));

        RequestProcessFileData input = new RequestProcessFileData(REQUEST_ID);
        ProcessTaskResult<ProcessFileData> result = taskAction.apply(input);

        assertThat(result.getProblems()).hasOnlyOneElementSatisfying(pi ->
            assertProblemInfoDescription(
                pi,
                CreateFileProcessTaskAction.unsupportedDynamicFileProblem(REQUEST_ID)
            )
        );
    }

    @Test
    public void testUnsupportedFileTypeUsingMocks() {
        FileDataProcessRequestDao fileDataProcessRequestDaoMock = mock(FileDataProcessRequestDao.class);
        when(fileDataProcessRequestDaoMock.fetchOneById(eq(REQUEST_ID)))
            .thenReturn(createRequest(FileType.ONE_CATEGORY_SIMPLE_EXCEL, false));
        CreateFileProcessTaskAction taskActionWithMocks = new CreateFileProcessTaskAction();
        taskActionWithMocks.setFileDataProcessRequestDao(fileDataProcessRequestDaoMock);

        RequestProcessFileData input = new RequestProcessFileData(REQUEST_ID);
        ProcessTaskResult<ProcessFileData> result = taskActionWithMocks.apply(input);

        assertThat(result.getProblems()).hasOnlyOneElementSatisfying(pi ->
            assertProblemInfoDescription(
                pi,
                CreateFileProcessTaskAction.unsupportedFileTypeProblem(REQUEST_ID, FileType.ONE_CATEGORY_SIMPLE_EXCEL)
            )
        );
    }

    @Test
    public void testFileProcessed() {
        fileDataProcessRequestDao.insert(createRequest(false));

        RequestProcessFileData input = new RequestProcessFileData(REQUEST_ID);
        ProcessTaskResult<ProcessFileData> result = taskAction.apply(input);

        assertThat(result.hasProblems()).isFalse();
        assertThat(result.getResult().getRequestId()).isEqualTo(REQUEST_ID);

        long processId = result.getResult().getProcessId();
        FileProcess fileProcess = fileProcessDao.findById(processId);

        assertThat(fileProcess.getId()).isEqualTo(processId);
        assertThat(fileProcess.getFileDataProcessRequestId()).isEqualTo(REQUEST_ID);
        assertThat(fileProcess.getProcessState()).isEqualTo(FileProcessState.NOT_STARTED);
    }

    private static FileDataProcessRequest createRequest(FileType fileType, boolean dynamic) {
        return new FileDataProcessRequest(
            REQUEST_ID,
            SOURCE_ID,
            fileType,
            FILE_URL,
            Timestamp.from(Instant.now()),
            dynamic,
            SUPPLIER_ID,
            true,
            false,
            false,
            null
        );
    }

    private static FileDataProcessRequest createRequest(boolean dynamic) {
        return createRequest(FileType.DCP_SINGLE_EXCEL, dynamic);
    }
}
