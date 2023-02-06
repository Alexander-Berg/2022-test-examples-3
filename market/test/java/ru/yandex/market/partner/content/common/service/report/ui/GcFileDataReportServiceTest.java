package ru.yandex.market.partner.content.common.service.report.ui;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.ir.http.PartnerContentUi;
import ru.yandex.market.partner.content.common.DBStateGenerator;
import ru.yandex.market.partner.content.common.db.dao.FileDataProcessRequestDao;
import ru.yandex.market.partner.content.common.db.dao.FileProcessDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileType;
import ru.yandex.market.partner.content.common.db.jooq.enums.ProcessType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.FileDataProcessRequest;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class GcFileDataReportServiceTest extends DBStateGenerator {

    private static final int SOURCE_ID = 1;
    private static final String FILE_URL = "url";
    private static final int SUPPLIER_ID = 1;

    private GcFileDataReportService gcFileDataReportService;

    @Autowired
    private FileProcessDao fileProcessDao;
    @Autowired
    private FileDataProcessRequestDao fileDataProcessRequestDao;

    @Before
    public void setUp() {
        super.setUp();
        gcFileDataReportService = new GcFileDataReportService(configuration);
    }

    @Test
    public void getFilesOfDifferentTypes() {
        // запрос requestId и процесс для dcp content уже созданы базовым классом

        final long betterRequestId = 2;
        fileDataProcessRequestDao.insert(createRequest(betterRequestId, FileType.ONE_CATEGORY_SIMPLE_EXCEL));
        fileProcessDao.insertNewFileProcess(betterRequestId, ProcessType.BETTER_FILE_PROCESS);

        PartnerContentUi.ListGcFileDataRequest request = PartnerContentUi.ListGcFileDataRequest.newBuilder()
            .setPaging(PartnerContentUi.Paging.newBuilder()
                .setStartRow(0)
                .setPageSize(10)
                .build())
            .build();
        PartnerContentUi.ListGcFileDataResponse response = gcFileDataReportService.listData(request);
        assertThat(response.getDataList())
            .extracting(PartnerContentUi.ListGcFileDataResponse.Row::getRequestId)
            .containsExactlyInAnyOrder(requestId, betterRequestId);
        assertThat(response.getDataList())
            .extracting(PartnerContentUi.ListGcFileDataResponse.Row::getPipelineType)
            .containsExactlyInAnyOrder(
                PartnerContentUi.Pipeline.Type.SINGLE_XLS,
                PartnerContentUi.Pipeline.Type.DCP_SINGLE_XLS
            );
    }

    @Test
    public void filterByPipelineType() {
        final long betterRequestId = 2;
        fileDataProcessRequestDao.insert(createRequest(betterRequestId, FileType.ONE_CATEGORY_SIMPLE_EXCEL));
        fileProcessDao.insertNewFileProcess(betterRequestId, ProcessType.BETTER_FILE_PROCESS);

        PartnerContentUi.ListGcFileDataRequest request = PartnerContentUi.ListGcFileDataRequest.newBuilder()
            .setPaging(PartnerContentUi.Paging.newBuilder()
                .setStartRow(0)
                .setPageSize(10)
                .build())
            .addFilter(PartnerContentUi.ListGcFileDataRequest.Filter.newBuilder()
                .setColumn(PartnerContentUi.ListGcFileDataRequest.Filter.Column.TYPE)
                .setValue(PartnerContentUi.Pipeline.Type.DCP_SINGLE_XLS.name())
                .build())
            .build();
        PartnerContentUi.ListGcFileDataResponse response = gcFileDataReportService.listData(request);
        assertThat(response.getDataList())
            .extracting(PartnerContentUi.ListGcFileDataResponse.Row::getRequestId)
            .containsOnly(requestId); // base class make dcp by default in setup
        assertThat(response.getDataList())
            .extracting(PartnerContentUi.ListGcFileDataResponse.Row::getPipelineType)
            .containsOnly(PartnerContentUi.Pipeline.Type.DCP_SINGLE_XLS);
    }

    private FileDataProcessRequest createRequest(long id, FileType fileType) {
        FileDataProcessRequest request = new FileDataProcessRequest();
        request.setId(id);
        request.setSourceId(SOURCE_ID);
        request.setFileType(fileType);
        request.setUrl(FILE_URL);
        request.setCreateTime(Timestamp.from(Instant.now()));
        return request;
    }
}
