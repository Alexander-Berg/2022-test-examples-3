package ru.yandex.market.gutgin.tms.pipeline.good.taskaction;

import io.qameta.allure.Issue;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.gutgin.tms.base.BaseDbGutGinTest;
import ru.yandex.market.gutgin.tms.engine.problem.ProblemInfo;
import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.gutgin.tms.mocks.FileParseServiceNGMock;
import ru.yandex.market.gutgin.tms.service.FileParseServiceNG;
import ru.yandex.market.partner.content.common.db.dao.FileDataProcessRequestDao;
import ru.yandex.market.partner.content.common.db.dao.FileProcessMessageService;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcRawSkuDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileProcessMessageType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcRawSku;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessFileData;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessFileOptionalResultData;
import ru.yandex.market.partner.content.common.entity.goodcontent.RawSku;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.partner.content.common.message.Messages;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ParseTaskActionTest extends BaseDbGutGinTest {
    private static final int SOURCE_ID = 100500;
    private static final int PARTNER_SHOP_ID = 100501;
    private static final ProblemInfo PROBLEM = ProblemInfo.builder()
        .setDescription("Test Problem")
        .setTs(new Timestamp(System.currentTimeMillis()))
        .build();
    private static final MessageInfo MESSAGE_INFO = Messages.get().apiEmptyModelName();
    private static final RawSku RAW_SKU = RawSku.newBuilder().setShopSku("shop_sku").build();

    @Autowired
    private FileDataProcessRequestDao fileDataProcessRequestDao;

    @Autowired
    private GcRawSkuDao gcRawSkuDao;

    @Autowired
    private FileProcessMessageService fileProcessMessageService;

    private final FileParseServiceNGMock fileParseServiceNG = new FileParseServiceNGMock();

    private ParseFileTaskAction parseFileTaskAction;
    private long fileDataProcessRequestId;

    @Before
    public void setUp() {
        createSource(SOURCE_ID, PARTNER_SHOP_ID);
        fileDataProcessRequestId = createFileDataProcessRequest(SOURCE_ID);

        parseFileTaskAction = new ParseFileTaskAction(
            fileParseServiceNG,
            fileDataProcessRequestDao,
            gcRawSkuDao,
            fileProcessMessageService);
    }

    @Test
    @Issue("MARKETIR-8540")
    public void whenParserReturnProblemGetProblemInResponse() {
        final long processId = createFileProcessId(fileDataProcessRequestId);
        fileParseServiceNG.putResponse(processId, FileParseServiceNG.Response.notSuccess(PROBLEM));
        ProcessFileData processFileData = createProcessSimpleExcelData(processId);
        final ProcessTaskResult<ProcessFileOptionalResultData> result = parseFileTaskAction.apply(processFileData);

        assertThat(result.getProblems()).hasSize(1);
        assertThat(result.getProblems()).contains(PROBLEM);
    }

    @Test
    @Issue("MARKETIR-8540")
    public void whenParserReturnInvalidGetInvalidResponse() {
        final long processId = createFileProcessId(fileDataProcessRequestId);
        fileParseServiceNG.putResponse(processId, FileParseServiceNG.Response.successNotValid(
            Collections.singletonList(MESSAGE_INFO)
        ));
        ProcessFileData processFileData = createProcessSimpleExcelData(processId);
        final ProcessTaskResult<ProcessFileOptionalResultData> result = parseFileTaskAction.apply(processFileData);
        final ProcessFileOptionalResultData processFileOptionalResultData = result.getResult();

        final List<MessageInfo> messages = fileProcessMessageService.getFileProcessMessages(
            processId, FileProcessMessageType.FILE_VALIDATION
        );

        assertThat(processFileOptionalResultData).isNotNull();
        assertThat(processFileOptionalResultData.isSuccess()).isFalse();
        assertThat(processFileOptionalResultData.getRequestId()).isEqualTo(processFileData.getRequestId());
        assertThat(processFileOptionalResultData.getProcessId()).isEqualTo(processFileData.getProcessId());
        assertThat(messages).containsExactly(MESSAGE_INFO);
    }

    @Test
    @Issue("MARKETIR-8540")
    public void whenParserReturnOkThenSaveRawSkus() {
        final long processId = createFileProcessId(fileDataProcessRequestId);
        fileParseServiceNG.putResponse(processId, FileParseServiceNG.Response.successValid(
            Collections.singletonList(RAW_SKU))
        );
        ProcessFileData processFileData = createProcessSimpleExcelData(processId);
        final ProcessTaskResult<ProcessFileOptionalResultData> result = parseFileTaskAction.apply(processFileData);
        final ProcessFileOptionalResultData processFileOptionalResultData = result.getResult();

        assertThat(processFileOptionalResultData).isNotNull();
        assertThat(processFileOptionalResultData.isSuccess()).isTrue();
        assertThat(processFileOptionalResultData.getRequestId()).isEqualTo(processFileData.getRequestId());
        assertThat(processFileOptionalResultData.getProcessId()).isEqualTo(processFileData.getProcessId());
        final List<RawSku> rawSkus = gcRawSkuDao.fetchByFileProcessId(processId).stream()
            .map(GcRawSku::getData)
            .collect(Collectors.toList());
        assertThat(rawSkus).containsExactly(RAW_SKU);
    }

    private ProcessFileData createProcessSimpleExcelData(long problemProcessId) {
        ProcessFileData processFileData = new ProcessFileData();
        processFileData.setProcessId(problemProcessId);
        processFileData.setRequestId(fileDataProcessRequestId);
        return processFileData;
    }
}