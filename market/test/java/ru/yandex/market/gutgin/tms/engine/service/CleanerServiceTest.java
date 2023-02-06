package ru.yandex.market.gutgin.tms.engine.service;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.gutgin.tms.db.dao.LockInfoDao;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.dao.PipelineDao;
import ru.yandex.market.partner.content.common.db.dao.ProtocolMessageDao;
import ru.yandex.market.partner.content.common.db.dao.TaskProcessDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcValidationMessageDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuValidationType;
import ru.yandex.market.partner.content.common.db.jooq.enums.LockStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuValidation;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.LockInfo;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Pipeline;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.message.Messages;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CleanerServiceTest extends DBDcpStateGenerator {

    @Autowired
    TaskProcessDao taskProcessDao;
    @Autowired
    PipelineDao pipelineDao;
    LockInfoDao lockInfoDao;
    ProtocolMessageDao protocolMessageDao;
    GcValidationMessageDao gcValidationMessageDao;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        this.lockInfoDao = new LockInfoDao(configuration);
        this.protocolMessageDao = new ProtocolMessageDao(configuration);
        this.gcValidationMessageDao = new GcValidationMessageDao(configuration);
    }

    @Test
    public void clearOkValidations() {
        CleanerService cleanerService = new CleanerService(taskProcessDao);
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(1);
        Long dataBucketId = gcSkuTickets.get(0).getDataBucketId();

        Pipeline pipeline = createPipeline(dataBucketId);
        pipelineDao.insert(pipeline);

        Long ticketId = gcSkuTickets.get(0).getId();
        createValidation(ticketId, GcSkuValidationType.PICTURE_MBO_VALIDATION, true);
        createValidation(ticketId, GcSkuValidationType.TITLE_LENGTH, false);
        Long validationWithMessageId = createValidation(ticketId, GcSkuValidationType.INVALID_CHARS, true);
        List<Long> messageIds = protocolMessageDao.insertMessages(Collections.singleton(Messages.get().emptyShopSKU(1)));
        gcValidationMessageDao.newMessageLinks(validationWithMessageId, messageIds);
        createValidation(ticketId, GcSkuValidationType.CLEAN_WEB_IMAGE, true);

        cleanerService.clearOkValidations(pipeline.getId(), PipelineType.DATA_CAMP);

        List<GcSkuValidation> okValidation = gcSkuValidationDao.getGcSkuValidations(
            GcSkuValidationType.PICTURE_MBO_VALIDATION,
            ticketId
        );
        assertThat(okValidation).isEmpty();
        List<GcSkuValidation> failValidation = gcSkuValidationDao.getGcSkuValidations(
            GcSkuValidationType.TITLE_LENGTH,
            ticketId
        );
        assertThat(failValidation).hasSize(1);
        List<GcSkuValidation> okWithMessagesValidation = gcSkuValidationDao.getGcSkuValidations(
            GcSkuValidationType.INVALID_CHARS,
            ticketId
        );
        assertThat(okWithMessagesValidation).hasSize(1);
        List<GcSkuValidation> cwValidation = gcSkuValidationDao.getGcSkuValidations(
            GcSkuValidationType.CLEAN_WEB_IMAGE,
            ticketId
        );
        assertThat(cwValidation).hasSize(1);
    }

    private Long createValidation(Long ticketId, GcSkuValidationType type, boolean validationResult) {
        List<GcSkuValidation> validation = gcSkuValidationDao.createValidations(Collections.singleton(ticketId), type);
        HashMap<Long, Boolean> validationStatus = new HashMap<>();
        Long validationId = validation.get(0).getId();
        validationStatus.put(validationId, validationResult);
        gcSkuValidationDao.updateIsOkStatus(validationStatus);
        return validationId;
    }

    private Pipeline createPipeline(Long dataBucketId) {
        final LockInfo lockInfo = new LockInfo();
        lockInfo.setStatus(LockStatus.FREE);
        lockInfo.setCreateTime(Timestamp.valueOf(LocalDateTime.now()));
        lockInfoDao.insert(lockInfo);
        ProcessDataBucketData processDataBucketData = new ProcessDataBucketData();
        processDataBucketData.setDataBucketId(dataBucketId);
        final Pipeline pipeline = new Pipeline();
        pipeline.setInputData(processDataBucketData);
        pipeline.setType(PipelineType.DATA_CAMP);
        pipeline.setStartDate(Timestamp.valueOf(LocalDateTime.now()));
        pipeline.setUpdateDate(Timestamp.valueOf(LocalDateTime.now()));
        pipeline.setLockId(lockInfo.getId());
        pipeline.setDataBucketId(dataBucketId);
        return pipeline;
    }
}
