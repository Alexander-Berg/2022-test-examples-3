package ru.yandex.market.gutgin.tms.pipeline.good.taskaction;

import io.qameta.allure.Issue;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.gutgin.tms.base.BaseDbGutGinTest;
import ru.yandex.market.gutgin.tms.engine.problem.ProblemInfo;
import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.gutgin.tms.service.FileDownloadService;
import ru.yandex.market.partner.content.common.service.MdsFileStorageService;
import ru.yandex.market.partner.content.common.db.dao.FileDataProcessRequestDao;
import ru.yandex.market.partner.content.common.db.dao.FileMdsCopyDao;
import ru.yandex.market.partner.content.common.db.dao.FileProcessDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileProcessState;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileType;
import ru.yandex.market.partner.content.common.db.jooq.enums.ProcessType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.FileDataProcessRequest;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.FileMdsCopy;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.FileProcess;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessFileData;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.ProblemInfoAssert.assertProblemInfoDescription;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.ProblemInfoAssert.assertProblemInfoThrowable;

/**
 * @author danfertev
 * @since 28.05.2019
 */
@Issue("MARKETIR-8539")
public class DownloadFileTaskActionTest extends BaseDbGutGinTest {
    private static final long REQUEST_ID = 1L;
    private static final int SOURCE_ID = 2;
    private static final String FILE_URL = "url";
    private static final int SUPPLIER_ID = 3;
    private static final String FILE_ERROR_MESSAGE = "error";
    private static final String FILE_TEMP_PATH = "tmp_path";
    private static final String MDS_BUCKET = "bucket";
    private static final String MDS_KEY = "key";
    private static final String MDS_URL = "mds_url";


    private DownloadFileTaskAction taskAction;

    @Resource
    private FileDataProcessRequestDao fileDataProcessRequestDao;
    @Resource
    private FileProcessDao fileProcessDao;
    @Resource
    private FileMdsCopyDao fileMdsCopyDao;

    private FileDownloadService fileDownloadServiceMock;
    private MdsFileStorageService mdsFileStorageServiceMock;
    private long processId;

    @Before
    public void setUp() {
        fileDownloadServiceMock = mock(FileDownloadService.class);
        mdsFileStorageServiceMock = mock(MdsFileStorageService.class);

        taskAction = new DownloadFileTaskAction();
        taskAction.setFileDataProcessRequestDao(fileDataProcessRequestDao);
        taskAction.setFileProcessDao(fileProcessDao);
        taskAction.setFileMdsCopyDao(fileMdsCopyDao);
        taskAction.setFileDownloadService(fileDownloadServiceMock);
        taskAction.setMdsFileStorageService(mdsFileStorageServiceMock);
        fileDataProcessRequestDao.insert(new FileDataProcessRequest(
            REQUEST_ID,
            SOURCE_ID,
            FileType.ONE_CATEGORY_SIMPLE_EXCEL,
            FILE_URL,
            Timestamp.from(Instant.now()),
            false,
            SUPPLIER_ID,
            false,
            false,
            false,
            null
        ));
        processId = fileProcessDao.insertNewFileProcess(REQUEST_ID, ProcessType.BETTER_FILE_PROCESS);
    }

    @Test
    public void testDownloadToTemFileFailed() {
        ProblemInfo expectedProblemInfo = createProblemInfo(FILE_ERROR_MESSAGE, new IOException(FILE_ERROR_MESSAGE));
        when(fileDownloadServiceMock.downloadToTempFile(eq(SOURCE_ID), eq(FILE_URL), any())).thenAnswer(args -> {
            Consumer<ProblemInfo> onError = args.getArgument(2);
            onError.accept(expectedProblemInfo);
            return null;
        });

        ProcessTaskResult<ProcessFileData> result = taskAction.apply(createProcessFileData());
        assertThat(result.getProblems()).hasOnlyOneElementSatisfying(pi -> {
                assertProblemInfoDescription(pi, expectedProblemInfo);
                assertProblemInfoThrowable(pi, expectedProblemInfo);
            }
        );
    }

    @Test(expected = RuntimeException.class)
    public void testUploadToMdsFailed() {
        Path tempPath = Paths.get(FILE_TEMP_PATH);
        when(fileDownloadServiceMock.downloadToTempFile(eq(SOURCE_ID), eq(FILE_URL), any()))
            .thenReturn(tempPath);
        when(mdsFileStorageServiceMock.upload(
            eq(tempPath),
            eq(SOURCE_ID),
            any(),
            eq(FILE_URL),
            eq(FileType.ONE_CATEGORY_SIMPLE_EXCEL))
        ).thenThrow(new RuntimeException());

        taskAction.apply(createProcessFileData());
    }

    @Test
    public void testUploadSuccess() {
        Path tempPath = Paths.get(FILE_TEMP_PATH);
        when(fileDownloadServiceMock.downloadToTempFile(eq(SOURCE_ID), eq(FILE_URL), any()))
            .thenReturn(tempPath);
        when(mdsFileStorageServiceMock.upload(
            eq(tempPath),
            eq(SOURCE_ID),
            any(),
            eq(FILE_URL),
            eq(FileType.ONE_CATEGORY_SIMPLE_EXCEL))
        ).thenReturn(new MdsFileStorageService.MdsFileInfo(MDS_BUCKET, MDS_KEY, MDS_URL));

        ProcessTaskResult<ProcessFileData> result = taskAction.doRun(createProcessFileData());

        assertThat(result.hasProblems()).isFalse();

        List<FileMdsCopy> fileMdsCopies = fileMdsCopyDao.fetchByUrl(MDS_URL);
        FileProcess fileProcess = fileProcessDao.findById(processId);
        assertThat(fileMdsCopies).hasOnlyOneElementSatisfying(file -> {
            assertThat(file.getFileProcessId()).isEqualTo(processId);
            assertThat(file.getBucket()).isEqualTo(MDS_BUCKET);
            assertThat(file.getKey()).isEqualTo(MDS_KEY);
            assertThat(file.getUrl()).isEqualTo(MDS_URL);
        });
        assertThat(fileProcess.getProcessState()).isEqualTo(FileProcessState.DOWNLOADED);
    }

    private ProcessFileData createProcessFileData() {
        return new ProcessFileData(REQUEST_ID, processId);
    }

    private static ProblemInfo createProblemInfo(String description, Throwable throwable) {
        return ProblemInfo.builder()
            .setDescription(description)
            .addThrowable(throwable)
            .build();
    }
}