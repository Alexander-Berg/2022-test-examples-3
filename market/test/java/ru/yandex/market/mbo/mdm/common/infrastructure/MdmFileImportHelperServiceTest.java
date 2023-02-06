package ru.yandex.market.mbo.mdm.common.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.param.ImportResult;
import ru.yandex.market.mboc.common.utils.SecurityUtil;

/**
 * @author dmserebr
 * @date 29/06/2020
 */
@SuppressWarnings("checkstyle:magicNumber")
public class MdmFileImportHelperServiceTest {
    private static final String FILENAME = "filename1";
    private static final byte[] FILE_BYTES = "content".getBytes();

    private MdmS3FileServiceMock mdmS3FileService;
    private MdmFileHistoryRepositoryMock mdmFileHistoryRepository;
    private MdmFileImportHelperService fileImportHelperService;

    @Before
    public void before() {
        mdmS3FileService = new MdmS3FileServiceMock();
        mdmFileHistoryRepository = new MdmFileHistoryRepositoryMock();
        fileImportHelperService = new MdmFileImportHelperService(mdmS3FileService, mdmFileHistoryRepository);
    }

    @Test
    public void testOk() {
        Assertions.assertThat(mdmFileHistoryRepository.findAll()).isEmpty();

        fileImportHelperService.uploadExcelToS3AndUpdateStatusFor(
            FILENAME,
            FILE_BYTES,
            MdmFileType.CATEGORY_PARAM,
            SecurityUtil.getCurrentUserLoginOrNull(),
            () -> new ImportResult(FileStatus.OK));

        MdmFileHistoryEntry entry = mdmFileHistoryRepository.findAll().get(0);
        Assertions.assertThat(entry.getS3Path()).isEqualTo("file-category-param-filename1");
        Assertions.assertThat(entry.getFileStatus()).isEqualTo(FileStatus.OK);
        Assertions.assertThat(entry.getUploadedAt()).isAfter(Instant.EPOCH);
        Assertions.assertThat(entry.getModifiedAt()).isAfter(Instant.EPOCH);
        Assertions.assertThat(entry.getErrors()).isEmpty();
        Assertions.assertThat(entry.getErrorCount()).isZero();
    }

    @Test
    public void testValidationErrors() {
        Assertions.assertThat(mdmFileHistoryRepository.findAll()).isEmpty();

        fileImportHelperService.uploadExcelToS3AndUpdateStatusFor(
            FILENAME,
            FILE_BYTES,
            MdmFileType.CATEGORY_PARAM,
            SecurityUtil.getCurrentUserLoginOrNull(),
            () -> new ImportResult(FileStatus.VALIDATION_ERRORS,
                List.of("error1", "error2")));

        MdmFileHistoryEntry entry = mdmFileHistoryRepository.findAll().get(0);
        Assertions.assertThat(entry.getS3Path()).isEqualTo("file-category-param-filename1");
        Assertions.assertThat(entry.getFileStatus()).isEqualTo(FileStatus.VALIDATION_ERRORS);
        Assertions.assertThat(entry.getUploadedAt()).isAfter(Instant.EPOCH);
        Assertions.assertThat(entry.getModifiedAt()).isAfter(Instant.EPOCH);
        Assertions.assertThat(entry.getErrors()).containsExactlyInAnyOrder("error1", "error2");
        Assertions.assertThat(entry.getErrorCount()).isEqualTo(2);
    }

    @Test
    public void testExceptionIsThrown() {
        Assertions.assertThat(mdmFileHistoryRepository.findAll()).isEmpty();

        fileImportHelperService.uploadExcelToS3AndUpdateStatusFor(
            FILENAME,
            FILE_BYTES,
            MdmFileType.CATEGORY_PARAM,
            SecurityUtil.getCurrentUserLoginOrNull(),
            () -> {
                throw new RuntimeException("msg");
            });

        MdmFileHistoryEntry entry = mdmFileHistoryRepository.findAll().get(0);
        Assertions.assertThat(entry.getS3Path()).isEqualTo("file-category-param-filename1");
        Assertions.assertThat(entry.getFileStatus()).isEqualTo(FileStatus.INTERNAL_ERROR);
        Assertions.assertThat(entry.getUploadedAt()).isAfter(Instant.EPOCH);
        Assertions.assertThat(entry.getModifiedAt()).isAfter(Instant.EPOCH);
        Assertions.assertThat(entry.getErrors()).containsExactly(
            "Exception caught: java.lang.RuntimeException: msg, see log for details");
        Assertions.assertThat(entry.getErrorCount()).isOne();
    }

    @Test
    public void testTooManyErrors() {
        Assertions.assertThat(mdmFileHistoryRepository.findAll()).isEmpty();

        fileImportHelperService.uploadExcelToS3AndUpdateStatusFor(
            FILENAME,
            FILE_BYTES,
            MdmFileType.CATEGORY_PARAM,
            SecurityUtil.getCurrentUserLoginOrNull(),
            () -> new ImportResult(FileStatus.VALIDATION_ERRORS,
                IntStream.range(0, 500).mapToObj(i -> "error" + i).collect(Collectors.toList())));

        MdmFileHistoryEntry entry = mdmFileHistoryRepository.findAll().get(0);
        Assertions.assertThat(entry.getS3Path()).isEqualTo("file-category-param-filename1");
        Assertions.assertThat(entry.getFileStatus()).isEqualTo(FileStatus.VALIDATION_ERRORS);
        Assertions.assertThat(entry.getUploadedAt()).isAfter(Instant.EPOCH);
        Assertions.assertThat(entry.getModifiedAt()).isAfter(Instant.EPOCH);
        Assertions.assertThat(entry.getErrors().size()).isEqualTo(MdmFileImportHelperService.MAX_ERRORS_COUNT);
        Assertions.assertThat(entry.getErrors()).containsExactlyElementsOf(
            IntStream.range(0, MdmFileImportHelperService.MAX_ERRORS_COUNT).mapToObj(i -> "error" + i)
                .collect(Collectors.toList()));
        Assertions.assertThat(entry.getErrorCount()).isEqualTo(500);
    }
}
