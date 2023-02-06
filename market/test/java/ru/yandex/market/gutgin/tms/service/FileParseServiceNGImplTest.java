package ru.yandex.market.gutgin.tms.service;

import io.qameta.allure.Issue;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.gutgin.tms.engine.problem.ProblemInfo;
import ru.yandex.market.partner.content.common.db.dao.FileMdsCopyDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileType;
import ru.yandex.market.partner.content.common.db.jooq.tables.RawSku;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.FileDataProcessRequest;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.partner.content.common.message.Messages;
import ru.yandex.market.partner.content.common.service.MdsFileStorageService;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class FileParseServiceNGImplTest {
    private static final long PROCESS_ID = 1;
    private static final long REQUEST_ID = 1;
    private static final int SOURCE_ID = 100500;

    private static final String BUCKET = "MDS_BUCKET";
    private static final String KEY = "MDS_KEY";
    private static final String URL = "https://some.url/file_name";

    private static final MessageInfo ERROR_XLS_STRUCTURE = Messages.get()
        .excelWrongFileFormat("File has wrong excel structure");


    private FileParseServiceNGImpl fileParseServiceNG;

    private FileMdsCopyDao fileMdsCopyDao;
    private MdsFileStorageService mdsFileStorageService;


    @Before
    public void setUp() {
        fileMdsCopyDao = mock(FileMdsCopyDao.class);
        mdsFileStorageService = mock(MdsFileStorageService.class);
        fileParseServiceNG = new FileParseServiceNGImpl(fileMdsCopyDao, mdsFileStorageService);
    }

    @Test
    @Issue("MARKETIR-8540")
    public void whenParserNotSetThenProblemFound() {
        FileDataProcessRequest fileDataProcessRequest = createFileDataProcessRequest();

        final FileParseServiceNG.Response<RawSku> rawSkuResponse = fileParseServiceNG.parseFile(
            fileDataProcessRequest, PROCESS_ID
        );

        assertThat(rawSkuResponse.hasProblems()).isTrue();
        assertThat(rawSkuResponse.getProblems()).extracting(ProblemInfo::getDescription)
            .containsExactly("Has not fileParser for fileType=" + fileDataProcessRequest.getFileType());
    }

    private FileDataProcessRequest createFileDataProcessRequest() {
        return createFileDataProcessRequest(URL);
    }

    private FileDataProcessRequest createFileDataProcessRequest(String url) {
        FileDataProcessRequest fileDataProcessRequest = new FileDataProcessRequest();
        fileDataProcessRequest.setCreateTime(Timestamp.from(Instant.now()));
        fileDataProcessRequest.setFileType(FileType.DCP_SINGLE_EXCEL);
        fileDataProcessRequest.setDynamic(false);
        fileDataProcessRequest.setId(REQUEST_ID);
        fileDataProcessRequest.setSourceId(SOURCE_ID);
        fileDataProcessRequest.setUrl(url);
        return fileDataProcessRequest;
    }
}
