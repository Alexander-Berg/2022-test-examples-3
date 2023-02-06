package ru.yandex.market.partner.content.common.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.ir.autogeneration.common.util.JooqUtils;
import ru.yandex.market.ir.http.PartnerContent;
import ru.yandex.market.partner.content.common.DBStateGenerator;
import ru.yandex.market.partner.content.common.db.dao.FileDataProcessRequestDao;
import ru.yandex.market.partner.content.common.db.dao.FileProcessDao;
import ru.yandex.market.partner.content.common.db.dao.PartnerContentDao;
import ru.yandex.market.partner.content.common.db.dao.PipelineService;
import ru.yandex.market.partner.content.common.db.dao.SourceDao;
import ru.yandex.market.partner.content.common.db.dao.TemplateFeedUploadDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuTicketDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileProcessMessageType;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileProcessState;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileType;
import ru.yandex.market.partner.content.common.db.jooq.enums.MrgrienPipelineStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;
import ru.yandex.market.partner.content.common.db.jooq.enums.ProcessType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.FileDataProcessRequest;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Pipeline;
import ru.yandex.market.partner.content.common.engine.manager.PipelineManager;
import ru.yandex.market.partner.content.common.engine.parameter.Param;
import ru.yandex.market.partner.content.common.engine.parameter.RequestProcessFileData;
import ru.yandex.market.partner.content.common.entity.feed.DcpExcelFileDetails;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.partner.content.common.message.Messages;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.FILE_PROCESS_MESSAGE;
import static ru.yandex.market.partner.content.common.db.jooq.tables.ProtocolMessage.PROTOCOL_MESSAGE;

/**
 * @author s-ermakov
 */
public class PartnerContentFileServiceTest extends DBStateGenerator {

    private static final List<MessageInfo> FILE_PROCESS_MESSAGES = Arrays.asList(
        Messages.get().emptyMandatoryParamValue(12, "shopSku1", "paramName1", 1L),
        Messages.get().notNumberInNumericParam("shopSku2", 13, "value2", "paramName2", 2L),
        Messages.get().noParamIdInRawGoodContent("shopSku3", 15, "paramName1")
    );

    private static final List<MessageInfo> DATA_BUCKET_MESSAGES = Arrays.asList(
        Messages.get().gcCwTextNoSense("shopSku1", 123),
        Messages.get().gcCwImageViolence("shopSku2", 12, "pictureUrl2", 2L)
    );
    private static final int CREATED_COUNT = 5;
    private static final int EXISTING_COUNT = 7;
    private static final int BUSINESS_ID = 9865;
    private static final String DCP_EXCEL_FILENAME = "test-dcp-file.xls";
    private static final String FEED_URL = "http://some.url.ru/";

    @Autowired
    private PartnerContentDao partnerContentDao;

    @Autowired
    private PipelineManager pipelineManager;

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private FileDataProcessRequestDao fileDataProcessRequestDao;

    @Autowired
    private GcSkuTicketDao gcSkuTicketDao;

    @Autowired
    private TemplateFeedUploadDao templateFeedUploadDao;

    @Autowired
    private SourceDao sourceDao;

    @Mock
    private MdsFileStorageService mdsFileStorageService;

    private PartnerContentFileService partnerContentFileService;

    @Override
    @Before
    public void setUp() {
        super.setUp();

        MockitoAnnotations.initMocks(this);
        Mockito
            .when(mdsFileStorageService.upload(
                Mockito.<InputStream>any(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(new MdsFileStorageService.MdsFileInfo("bucket", "key", FEED_URL));

        sourceDao.mergeSource(SOURCE_ID, "source", "www.source.com", BUSINESS_ID);

        this.partnerContentFileService = new PartnerContentFileService(
            partnerContentDao, pipelineManager, gcSkuTicketDao, sourceDao, templateFeedUploadDao, mdsFileStorageService);
    }

    @Test
    public void testAddDcpXslFileSource() {
        long actualRequestId = prepareDcpRequest();
        assertThat(actualRequestId).isPositive();

        // assert request created
        FileDataProcessRequest processRequest = fileDataProcessRequestDao.fetchOneById(actualRequestId);
        assertThat(processRequest)
            .isEqualToIgnoringGivenFields(new FileDataProcessRequest(
                actualRequestId, SOURCE_ID, FileType.DCP_SINGLE_EXCEL, FEED_URL,
                null, false, null, null, false, false,
                BUSINESS_ID
            ), "id", "createTime");

        // assert pipeline is created
        Optional<Pipeline> lastPipeline = pipelineService.getLastPipeline(PipelineType.DCP_SINGLE_XLS);
        assertThat(lastPipeline).isPresent();
        Param inputData = lastPipeline.get().getInputData();

        // check that pipeline is new
        assertThat(lastPipeline.get().getStatus()).isEqualTo(MrgrienPipelineStatus.NEW);

        assertThat(inputData).isInstanceOf(RequestProcessFileData.class);
        RequestProcessFileData data = (RequestProcessFileData) inputData;

        assertThat(data.getRequestId()).isEqualTo(actualRequestId);

        // assert that feed upload has been tracked
        assertThat(templateFeedUploadDao.fetchOneByFileDataProcessRequestId(actualRequestId)).isNotNull();
    }

    private void prepareFileProcessMessages(Long fileProcessId) {
        List<Long> messageIds = JooqUtils.batchInsert(
            dsl(),
            PROTOCOL_MESSAGE,
            PROTOCOL_MESSAGE.ID,
            FILE_PROCESS_MESSAGES,
            (table, message) -> table
                .set(PROTOCOL_MESSAGE.CODE, message.getCode())
                .set(PROTOCOL_MESSAGE.PARAMS, message.getParams())
        );
        JooqUtils.batchInsert(
            dsl(),
            FILE_PROCESS_MESSAGE,
            messageIds,
            (table, id) -> table
                .set(FILE_PROCESS_MESSAGE.FILE_PROCESS_ID, fileProcessId)
                .set(FILE_PROCESS_MESSAGE.PROTOCOL_MESSAGE_ID, id)
                .set(FILE_PROCESS_MESSAGE.TYPE, FileProcessMessageType.FILE_VALIDATION)
        );
    }

    @Test
    public void getDcpExcelFileInfo_shouldReturnProcessingWhenNoFileProcess() {
        long requestId = prepareDcpRequest();

        DcpExcelFileDetails response = partnerContentFileService.getDcpExcelFileInfo(BUSINESS_ID, requestId);
        assertDcpResponse(requestId, response, PartnerContent.ProcessRequestStatus.PROCCESSING);
        assertThat(response.getProcessedAt()).isNull();
        assertThat(response.getMessages()).hasSize(0);
    }

    @Test
    public void getDcpExcelFileInfo_shouldReturnProcessingWhenProcessNotInTerminalState() {
        Set<FileProcessState> processingStates = EnumSet.allOf(FileProcessState.class);
        processingStates.removeAll(FileProcessDao.FINAL_STATUSES);

        for (FileProcessState state : processingStates) {
            long requestId = prepareDcpRequest();
            prepareFileProcess(requestId, ProcessType.DCP_FILE_PROCESS, state);

            DcpExcelFileDetails response = partnerContentFileService.getDcpExcelFileInfo(BUSINESS_ID, requestId);
            assertDcpResponse(requestId, response, PartnerContent.ProcessRequestStatus.PROCCESSING);
            assertThat(response.getProcessedAt()).isNull();
            assertThat(response.getMessages()).hasSize(0);
        }
    }

    @Test
    public void getDcpExcelFileInfo_shouldReturnProcessedAtAndFileProcessMessagesWhenInTerminalState() {
        for (FileProcessState state : FileProcessDao.FINAL_STATUSES) {
            long requestId = prepareDcpRequest();
            long fileProcessId = prepareFileProcess(requestId, ProcessType.DCP_FILE_PROCESS, state);
            prepareFileProcessMessages(fileProcessId);

            final PartnerContent.ProcessRequestStatus expectedStatus;
            switch (state) {
                case FINISHED:
                    expectedStatus = PartnerContent.ProcessRequestStatus.FINISHED;
                    break;
                case INVALID:
                    expectedStatus = PartnerContent.ProcessRequestStatus.INVALID;
                    break;
                case MIXED:
                    expectedStatus = PartnerContent.ProcessRequestStatus.MIXED;
                    break;
                default:
                    throw new RuntimeException(String.format("Unexpected terminal file process state - %s", state));
            }

            DcpExcelFileDetails response = partnerContentFileService.getDcpExcelFileInfo(BUSINESS_ID, requestId);
            assertDcpResponse(requestId, response, expectedStatus);
            assertThat(response.getProcessedAt()).isAfter(response.getUploadedAt());
            assertThat(response.getMessages())
                .extracting(DcpExcelFileDetails.Message::getCode)
                .containsExactlyElementsOf(FILE_PROCESS_MESSAGES.stream()
                    .map(MessageInfo::getCode)
                    .collect(Collectors.toList()));
        }
    }

    @Test(expected = FileProcessRequestNotFoundException.class)
    public void getDcpExcelFileInfo_shouldThrowWhenBusinessIdNotExist() {
        long requestId = prepareDcpRequest();
        partnerContentFileService.getDcpExcelFileInfo(BUSINESS_ID + 1, requestId);
    }

    @Test(expected = FileProcessRequestNotFoundException.class)
    public void getDcpExcelFileInfo_shouldThrowWhenRequestDoesNotBelongToBusinessId() {
        int businessId2 = BUSINESS_ID + 1;
        int sourceId2 = SOURCE_ID + 1;
        sourceDao.mergeSource(sourceId2, "source 2", "www.source2.com", businessId2);

        long requestId = prepareDcpRequest();
        partnerContentFileService.getDcpExcelFileInfo(businessId2, requestId);
    }

    @Test(expected = FileProcessRequestNotFoundException.class)
    public void getDcpExcelFileInfo_shouldThrowWhenRequestDoesNotExist() {
        partnerContentFileService.getDcpExcelFileInfo(BUSINESS_ID, 1234);
    }

    private long prepareDcpRequest() {
        return partnerContentFileService.addDcpExcelFile(
            SOURCE_ID,
            BUSINESS_ID,
            DCP_EXCEL_FILENAME,
            "application/octet-stream",
            100500L,
            new ByteArrayInputStream(new byte[0])
        );
    }

    private long prepareFileProcess(long processRequestId, ProcessType processType, FileProcessState state) {
        long fileProcessId = fileProcessDao.insertNewFileProcess(processRequestId, processType);
        fileProcessDao.updateFileProcess(fileProcessId, state);
        return fileProcessId;
    }

    private void assertDcpResponse(long requestId, DcpExcelFileDetails response, PartnerContent.ProcessRequestStatus status) {
        assertThat(response.getId()).isEqualTo(requestId);
        assertThat(response.getFilename()).isEqualTo(DCP_EXCEL_FILENAME);
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getUploadedAt()).isNotNull();
    }
}
