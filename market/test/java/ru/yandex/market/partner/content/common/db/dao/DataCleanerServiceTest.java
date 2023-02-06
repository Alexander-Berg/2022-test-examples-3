package ru.yandex.market.partner.content.common.db.dao;

import Market.DataCamp.DataCampOffer;
import org.jooq.Param;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.autogeneration.common.util.JooqUtils;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.BaseDbCommonTest;
import ru.yandex.market.partner.content.common.db.jooq.enums.*;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.*;
import ru.yandex.market.partner.content.common.db.jooq.tables.records.LockInfoRecord;
import ru.yandex.market.partner.content.common.engine.parameter.EmptyData;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.engine.pipeline.PipelineData;
import ru.yandex.market.partner.content.common.entity.Model;
import ru.yandex.market.partner.content.common.entity.goodcontent.RawSku;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import static org.junit.Assert.*;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.*;
import static ru.yandex.market.partner.content.common.db.jooq.tables.ErrorProtoMessage.ERROR_PROTO_MESSAGE;
import static ru.yandex.market.partner.content.common.db.jooq.tables.Problem.PROBLEM;

public class DataCleanerServiceTest extends BaseDbCommonTest {

    private static final Timestamp OLDNESS_THRESHOLD = Timestamp.valueOf(LocalDateTime.now().minusMonths(2));

    @Autowired
    private DataCleanerService dataCleanerService;

    @Test
    public void shouldTakeOnlyOldProcessedPipelines() {
        Timestamp freshTimestamp = Timestamp.valueOf(LocalDateTime.now().minusMonths(1));
        Timestamp oldTimeStamp = Timestamp.valueOf(LocalDateTime.now().minusMonths(14));

        dsl().deleteFrom(PIPELINE).execute();

        dsl().insertInto(LOCK_INFO, LOCK_INFO.ID, LOCK_INFO.STATUS, LOCK_INFO.CREATE_TIME)
                .values(1L, LockStatus.FREE, oldTimeStamp)
                .values(2L, LockStatus.FREE, oldTimeStamp)
                .values(3L, LockStatus.FREE, oldTimeStamp)
                .values(4L, LockStatus.LOCKED, oldTimeStamp)
                .values(5L, LockStatus.FREE, oldTimeStamp)
                .execute();

        EmptyData data = new EmptyData();
        dsl().insertInto(PIPELINE, PIPELINE.ID, PIPELINE.STATUS, PIPELINE.TYPE, PIPELINE.INPUT_DATA,
                        PIPELINE.START_DATE, PIPELINE.UPDATE_DATE, PIPELINE.LOCK_ID, PIPELINE.DATA_BUCKET_ID)
                .values(100L, MrgrienPipelineStatus.CANCELLED, PipelineType.DATA_CAMP, data, freshTimestamp,
                        freshTimestamp, 1L, null)
                .values(101L, MrgrienPipelineStatus.CANCELLED, PipelineType.DATA_CAMP, data, oldTimeStamp,
                        oldTimeStamp, 2L, null)
                .values(102L, MrgrienPipelineStatus.FINISHED, PipelineType.DATA_CAMP, data, oldTimeStamp,
                        oldTimeStamp, 3L, null)
                .values(103L, MrgrienPipelineStatus.FINISHED, PipelineType.DATA_CAMP, data, oldTimeStamp,
                        oldTimeStamp, 4L, null)
                .values(104L, MrgrienPipelineStatus.RUNNING, PipelineType.DATA_CAMP, data, oldTimeStamp, oldTimeStamp
                        , 5L, null)
                .execute();

        int all = dsl().selectFrom(PIPELINE).execute();
        assertEquals(5, all);

        List<Long> pipelineIds = dataCleanerService.getProcessedPipelineIdBatchOldEnough(OLDNESS_THRESHOLD);

        // 100L has freshTimestamp
        // 101L fit requirements
        // 102L fit requirements
        // 103L fit requirements
        // 104L has RUNNING (not processed) status
        assertThat(pipelineIds).containsExactlyInAnyOrder(101L, 102L, 103L);
    }

    @Test
    public void shouldRemoveOldFinishedPipelineTasks() {
        Timestamp oldTimestamp = Timestamp.valueOf(LocalDateTime.now().minusDays(2));

        Long firstPipelineId = 1L;
        Long secondPipelineId = 2L;

        dsl().insertInto(LOCK_INFO, LOCK_INFO.ID, LOCK_INFO.STATUS, LOCK_INFO.CREATE_TIME)
                .values(1L, LockStatus.FREE, oldTimestamp)
                .values(2L, LockStatus.FREE, oldTimestamp)
                .execute();

        EmptyData data = new EmptyData();
        dsl().insertInto(PIPELINE, PIPELINE.ID, PIPELINE.STATUS, PIPELINE.TYPE, PIPELINE.INPUT_DATA,
                        PIPELINE.START_DATE, PIPELINE.UPDATE_DATE, PIPELINE.LOCK_ID, PIPELINE.STATE_DATA)
                .values(firstPipelineId, MrgrienPipelineStatus.CANCELLED, PipelineType.DATA_CAMP,
                        data, oldTimestamp, oldTimestamp, 1L, new PipelineData<>())
                .values(secondPipelineId, MrgrienPipelineStatus.FINISHED, PipelineType.DATA_CAMP,
                        data, oldTimestamp, oldTimestamp, 2L, new PipelineData<>())
                .execute();

        dsl().insertInto(TASK, TASK.ID, TASK.PIPELINE_ID, TASK.INPUT_DATA,
                        TASK.START_DATE, TASK.UPDATE_DATE, TASK.STATUS)
                .values(1L, firstPipelineId, data, oldTimestamp, oldTimestamp, TaskStatus.FINISHED)
                .values(2L, secondPipelineId, data, oldTimestamp, oldTimestamp, TaskStatus.FINISHED)
                .execute();

        dataCleanerService.cleanFinishedPipelineTasks();

        int numPipelines = dsl()
                .select(PIPELINE.ID)
                .from(PIPELINE)
                .where(PIPELINE.STATE_DATA.isNull())
                .and(PIPELINE.ID.in(firstPipelineId, secondPipelineId))
                .execute();

        assertEquals(2, numPipelines);
        assertEquals(0, dsl().selectFrom(TASK).execute());
    }

    @Test
    public void shouldNotRemoveNewAndUnfinishedPipelineTasks() {
        Timestamp newTimestamp = Timestamp.valueOf(LocalDateTime.now());
        Timestamp oldTimestamp = Timestamp.valueOf(LocalDateTime.now().minusDays(2));

        Long firstPipelineId = 1L;
        Long secondPipelineId = 2L;

        dsl().insertInto(LOCK_INFO, LOCK_INFO.ID, LOCK_INFO.STATUS, LOCK_INFO.CREATE_TIME)
                .values(1L, LockStatus.FREE, oldTimestamp)
                .values(2L, LockStatus.FREE, oldTimestamp)
                .execute();

        EmptyData data = new EmptyData();
        dsl().insertInto(PIPELINE, PIPELINE.ID, PIPELINE.STATUS, PIPELINE.TYPE, PIPELINE.INPUT_DATA,
                        PIPELINE.START_DATE, PIPELINE.UPDATE_DATE, PIPELINE.LOCK_ID, PIPELINE.STATE_DATA)
                .values(firstPipelineId, MrgrienPipelineStatus.RUNNING, PipelineType.DATA_CAMP,
                        data, oldTimestamp, oldTimestamp, 1L, new PipelineData<>())
                .values(secondPipelineId, MrgrienPipelineStatus.FINISHED, PipelineType.DATA_CAMP,
                        data, oldTimestamp, newTimestamp, 2L, new PipelineData<>())
                .execute();

        dsl().insertInto(TASK, TASK.ID, TASK.PIPELINE_ID, TASK.INPUT_DATA,
                        TASK.START_DATE, TASK.UPDATE_DATE, TASK.STATUS)
                .values(1L, firstPipelineId, data, oldTimestamp, oldTimestamp, TaskStatus.FINISHED)
                .values(2L, secondPipelineId, data, oldTimestamp, oldTimestamp, TaskStatus.FINISHED)
                .execute();

        dataCleanerService.cleanFinishedPipelineTasks();

        int numPipelines = dsl()
                .select(PIPELINE.ID)
                .from(PIPELINE)
                .where(PIPELINE.STATE_DATA.isNull())
                .and(PIPELINE.ID.in(firstPipelineId, secondPipelineId))
                .execute();

        assertEquals(0, numPipelines);
        assertEquals(2, dsl().selectFrom(TASK).execute());
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void shouldRemoveAnalyzedErrorProtoMessages() {
        dsl().insertInto(PROBLEM, PROBLEM.ID, PROBLEM.ANALYZED, PROBLEM.PROBLEM_TS)
            .values(100L, true, Timestamp.valueOf(LocalDateTime.now().minusMonths(1)))
            .values(101L, false, Timestamp.valueOf(LocalDateTime.now().minusWeeks(1)))
            .values(102L, false, Timestamp.valueOf(LocalDateTime.now().minusDays(1)))
            .values(103L, true, Timestamp.valueOf(LocalDateTime.now().minusHours(1)))
            .execute();

        dsl().insertInto(ERROR_PROTO_MESSAGE,
                ERROR_PROTO_MESSAGE.ID, ERROR_PROTO_MESSAGE.PROBLEM_ID)
            .values(200L, 100L)
            .values(201L, 101L)
            .values(202L, 102L)
            .values(203L, 103L)
            .values(204L, 100L)
            .execute();

        int all = dsl().selectFrom(ERROR_PROTO_MESSAGE).execute();
        assertEquals(5, all);

        dataCleanerService.cleanErrorProtoMessages();

        int afterClearing = dsl().selectFrom(ERROR_PROTO_MESSAGE).execute();
        assertEquals(2, afterClearing);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void shouldRemoveOldErrorProtoMessages() {
        dsl().insertInto(PROBLEM, PROBLEM.ID, PROBLEM.ANALYZED, PROBLEM.PROBLEM_TS)
            .values(100L, false, Timestamp.valueOf(LocalDateTime.now().minusMonths(3).minusHours(1)))
            .values(101L, false, Timestamp.valueOf(LocalDateTime.now().minusWeeks(3).minusMonths(3)))
            .values(102L, false, Timestamp.valueOf(LocalDateTime.now().minusDays(300)))
            .values(103L, false, Timestamp.valueOf(LocalDateTime.now().minusHours(1)))
            .execute();

        dsl().insertInto(ERROR_PROTO_MESSAGE,
            ERROR_PROTO_MESSAGE.ID, ERROR_PROTO_MESSAGE.PROBLEM_ID)
            .values(200L, 100L)
            .values(201L, 101L)
            .values(202L, 102L)
            .values(203L, 103L)
            .values(204L, 100L)
            .execute();

        int all = dsl().selectFrom(ERROR_PROTO_MESSAGE).execute();
        assertEquals(5, all);

        dataCleanerService.cleanErrorProtoMessages();

        int afterClearing = dsl().selectFrom(ERROR_PROTO_MESSAGE).execute();
        assertEquals(1, afterClearing);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void shouldRemoveOldAndAnalyzedErrorProtoMessages() {
        dsl().insertInto(PROBLEM, PROBLEM.ID, PROBLEM.ANALYZED, PROBLEM.PROBLEM_TS)
            .values(100L, false, Timestamp.valueOf(LocalDateTime.now().minusMonths(3).minusHours(1)))
            .values(101L, false, Timestamp.valueOf(LocalDateTime.now().minusWeeks(3).minusMonths(3)))
            .values(102L, true, Timestamp.valueOf(LocalDateTime.now().minusDays(300)))
            .values(103L, true, Timestamp.valueOf(LocalDateTime.now().minusHours(1)))
            .execute();

        dsl().insertInto(ERROR_PROTO_MESSAGE,
            ERROR_PROTO_MESSAGE.ID, ERROR_PROTO_MESSAGE.PROBLEM_ID)
            .values(200L, 100L)
            .values(201L, 101L)
            .values(202L, 102L)
            .values(203L, 103L)
            .values(204L, 100L)
            .execute();

        int all = dsl().selectFrom(ERROR_PROTO_MESSAGE).execute();
        assertEquals(5, all);

        dataCleanerService.cleanErrorProtoMessages();

        int afterClearing = dsl().selectFrom(ERROR_PROTO_MESSAGE).execute();
        assertEquals(0, afterClearing);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void shouldNotRemoveAnyErrorProtoMessages() {
        dsl().insertInto(PROBLEM, PROBLEM.ID, PROBLEM.ANALYZED, PROBLEM.PROBLEM_TS)
            .values(100L, false, Timestamp.valueOf(LocalDateTime.now().minusHours(1)))
            .values(101L, false, Timestamp.valueOf(LocalDateTime.now().minusWeeks(3)))
            .values(102L, false, Timestamp.valueOf(LocalDateTime.now().minusDays(27)))
            .values(103L, false, Timestamp.valueOf(LocalDateTime.now().minusHours(1)))
            .execute();

        dsl().insertInto(ERROR_PROTO_MESSAGE,
            ERROR_PROTO_MESSAGE.ID, ERROR_PROTO_MESSAGE.PROBLEM_ID)
            .values(200L, 100L)
            .values(201L, 101L)
            .values(202L, 102L)
            .values(203L, 103L)
            .values(204L, 100L)
            .execute();

        int all = dsl().selectFrom(ERROR_PROTO_MESSAGE).execute();
        assertEquals(5, all);

        dataCleanerService.cleanErrorProtoMessages();

        int afterClearing = dsl().selectFrom(ERROR_PROTO_MESSAGE).execute();
        assertEquals(5, afterClearing);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void shouldRemoveOnlyErrorProtoMessages() {
        dsl().insertInto(PROBLEM, PROBLEM.ID, PROBLEM.ANALYZED, PROBLEM.PROBLEM_TS)
            .values(100L, false, Timestamp.valueOf(LocalDateTime.now().minusMonths(3).minusHours(1)))
            .values(101L, false, Timestamp.valueOf(LocalDateTime.now().minusWeeks(14)))
            .values(102L, true, Timestamp.valueOf(LocalDateTime.now().minusDays(300)))
            .values(103L, true, Timestamp.valueOf(LocalDateTime.now().minusHours(1)))
            .execute();

        dsl().insertInto(ERROR_PROTO_MESSAGE,
            ERROR_PROTO_MESSAGE.ID, ERROR_PROTO_MESSAGE.PROBLEM_ID)
            .values(200L, 100L)
            .values(201L, 101L)
            .values(202L, 102L)
            .values(203L, 103L)
            .values(204L, 100L)
            .execute();

        int all = dsl().selectFrom(ERROR_PROTO_MESSAGE).execute();
        assertEquals(5, all);

        dataCleanerService.cleanErrorProtoMessages();

        int afterClearing = dsl().selectFrom(ERROR_PROTO_MESSAGE).execute();
        assertEquals(0, afterClearing);

        int problemCount = dsl().selectFrom(PROBLEM).execute();
        assertEquals(4, problemCount);
    }


    @Test
    public void shouldRemoveLocksWithoutReferences() {
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());

        Long lockedByPipelineId = 100L;
        Long lockedByTaskId = 103L;
        Long lockedByBusinessId = 104L;
        Long freeFirstId = 101L;
        Long freeSecondId = 102L;

        List<LockInfo> lockInfoList = new ArrayList<>();
        lockInfoList.add(genLockInfo(lockedByPipelineId, LockStatus.FREE,timestamp));
        lockInfoList.add(genLockInfo(lockedByTaskId, LockStatus.FREE,timestamp));
        lockInfoList.add(genLockInfo(lockedByBusinessId, LockStatus.FREE,timestamp));
        lockInfoList.add(genLockInfo(freeFirstId, LockStatus.FREE,timestamp));
        lockInfoList.add(genLockInfo(freeSecondId, LockStatus.FREE,timestamp));

        JooqUtils.batchInsert(
            dsl(),
            LOCK_INFO,
            lockInfoList,
            (table, lockInfo) -> {
                LockInfoRecord record = new LockInfoRecord();
                record.from(lockInfo);
                return table.set(record);
            }
        );

        dsl().insertInto(BUSINESS_TO_LOCK_INFO, BUSINESS_TO_LOCK_INFO.LOCK_INFO_ID, BUSINESS_TO_LOCK_INFO.BUSINESS_ID)
                .values(lockedByBusinessId, 999).execute();
        EmptyData data = new EmptyData();
        dsl().insertInto(PIPELINE,PIPELINE.ID,PIPELINE.LOCK_ID, PIPELINE.TYPE,PIPELINE.INPUT_DATA,
            PIPELINE.START_DATE, PIPELINE.UPDATE_DATE)
            .values(300L,lockedByPipelineId,PipelineType.DATA_CAMP,data,timestamp,timestamp)
            .execute();

        dsl().insertInto(TASK,TASK.ID,PIPELINE.LOCK_ID,TASK.PIPELINE_ID,TASK.INPUT_DATA,TASK.START_DATE,TASK.UPDATE_DATE)
            .values(200L,lockedByTaskId,300L,data,timestamp,timestamp)
            .execute();

        dataCleanerService.cleanFatTables(OLDNESS_THRESHOLD);

        List<Long> afterClearing = dsl().selectFrom(LOCK_INFO).where().fetch(LOCK_INFO.ID);
        // check that items deleted
        assertThat(freeFirstId).isNotIn(afterClearing);
        assertThat(freeSecondId).isNotIn(afterClearing);
        // check that locks is not deleted
        assertThat(lockedByPipelineId).isIn(afterClearing);
        assertThat(lockedByTaskId).isIn(afterClearing);
        assertThat(lockedByBusinessId).isIn(afterClearing);
    }

    @Test
    public void shouldRemoveLocksAfterUse()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(Timestamp.from(Instant.now()));
        calendar.add(Calendar.MONTH, -4);
        Timestamp beforeOldnessThreshold = new Timestamp(calendar.getTime().getTime());

        Long lockedByPipelineId = 100L;
        Long lockedByTaskId = 103L;
        Long freePipelineId = 101L;
        Long freeTaskId = 102L;

        List<LockInfo> lockInfoList = new ArrayList<>();
        lockInfoList.add(genLockInfo(lockedByPipelineId,LockStatus.LOCKED,beforeOldnessThreshold));
        lockInfoList.add(genLockInfo(freePipelineId,LockStatus.FREE,beforeOldnessThreshold));
        lockInfoList.add(genLockInfo(freeTaskId,LockStatus.FREE,beforeOldnessThreshold));
        lockInfoList.add(genLockInfo(lockedByTaskId,LockStatus.LOCKED,beforeOldnessThreshold));
        JooqUtils.batchInsert(
            dsl(),
            LOCK_INFO,
            lockInfoList,
            (table, lockInfo) -> {
                LockInfoRecord record = new LockInfoRecord();
                record.from(lockInfo);
                return table.set(record);
            }
        );

        EmptyData data = new EmptyData();
        dsl().insertInto(PIPELINE,PIPELINE.ID,PIPELINE.LOCK_ID, PIPELINE.TYPE,PIPELINE.INPUT_DATA,
            PIPELINE.START_DATE, PIPELINE.UPDATE_DATE,PIPELINE.STATUS)
            .values(300L,lockedByPipelineId,PipelineType.DATA_CAMP,data,beforeOldnessThreshold,beforeOldnessThreshold, MrgrienPipelineStatus.RUNNING)
            .values(301L,freePipelineId,PipelineType.DATA_CAMP,data,beforeOldnessThreshold,beforeOldnessThreshold, MrgrienPipelineStatus.FINISHED)
            .execute();

        dsl().insertInto(TASK,TASK.ID,PIPELINE.LOCK_ID,TASK.PIPELINE_ID,TASK.INPUT_DATA,TASK.START_DATE,TASK.UPDATE_DATE,TASK.STATUS)
            .values(200L,lockedByTaskId,300L,data,beforeOldnessThreshold,beforeOldnessThreshold, TaskStatus.RUNNING)
            .values(201L,freeTaskId,300L,data,beforeOldnessThreshold,beforeOldnessThreshold, TaskStatus.FINISHED)
            .execute();

        dataCleanerService.cleanFatTables(OLDNESS_THRESHOLD);

        List<Long> afterClearing = dsl().selectFrom(LOCK_INFO).where().fetch(LOCK_INFO.ID);
        // check that items deleted
        assertThat(freePipelineId).isNotIn(afterClearing);
        assertThat(freeTaskId).isNotIn(afterClearing);
        // check that locks is not deleted
        assertThat(lockedByPipelineId).isIn(afterClearing);
        assertThat(lockedByTaskId).isIn(afterClearing);

    }

    @Test
    public void testCWClear() {
        Timestamp now = Timestamp.from(Instant.now());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.DATE, -1);
        Timestamp yesterday = new Timestamp(calendar.getTime().getTime());

        long skuTicketId = 1001L;
        long validation1Id = 201L;
        long validation2Id = 202L;
        long cwTextValidationId = 301L;
        long cwImageValidationId = 302L;

        dsl().insertInto(GC_SKU_TICKET, GC_SKU_TICKET.ID, GC_SKU_TICKET.SOURCE_ID,
                GC_SKU_TICKET.CATEGORY_ID, GC_SKU_TICKET.STATUS,
                GC_SKU_TICKET.CREATE_DATE, GC_SKU_TICKET.UPDATE_DATE, GC_SKU_TICKET.VALID, GC_SKU_TICKET.TYPE)
                .values(skuTicketId, -1, 1L, GcSkuTicketStatus.SUCCESS, Timestamp.from(Instant.now()),
                        yesterday, true, GcSkuTicketType.DATA_CAMP).execute();

        dsl().insertInto(GC_SKU_VALIDATION, GC_SKU_VALIDATION.ID, GC_SKU_VALIDATION.SKU_TICKET_ID,
                GC_SKU_VALIDATION.VALIDATION_TYPE, GC_SKU_VALIDATION.CHECK_DATE)
                .values(validation1Id, skuTicketId, GcSkuValidationType.CLEAN_WEB_TEXT, yesterday).execute();
        dsl().insertInto(GC_SKU_VALIDATION, GC_SKU_VALIDATION.ID, GC_SKU_VALIDATION.SKU_TICKET_ID,
                GC_SKU_VALIDATION.VALIDATION_TYPE, GC_SKU_VALIDATION.CHECK_DATE)
                .values(validation2Id, skuTicketId, GcSkuValidationType.CLEAN_WEB_IMAGE, yesterday).execute();

        dsl().insertInto(GC_CLEAN_WEB_TEXT_VALIDATION, GC_CLEAN_WEB_TEXT_VALIDATION.ID,
                GC_CLEAN_WEB_TEXT_VALIDATION.VALIDATION_ID, GC_CLEAN_WEB_TEXT_VALIDATION.START_DATE,
                GC_CLEAN_WEB_TEXT_VALIDATION.REQUEST_MODE)
                .values(cwTextValidationId, validation1Id, yesterday, "").execute();

        dsl().insertInto(GC_CLEAN_WEB_IMAGE_VALIDATION, GC_CLEAN_WEB_IMAGE_VALIDATION.ID,
                GC_CLEAN_WEB_IMAGE_VALIDATION.VALIDATION_ID, GC_CLEAN_WEB_IMAGE_VALIDATION.START_DATE,
                GC_CLEAN_WEB_IMAGE_VALIDATION.REQUEST_MODE, GC_CLEAN_WEB_IMAGE_VALIDATION.IMAGE_ORDINAL)
                .values(cwImageValidationId, validation2Id, yesterday, "", 0).execute();

        dataCleanerService.cleanFatTables(now);

        assertThat(dsl().selectFrom(GC_SKU_TICKET).stream().anyMatch(t -> t.getId().equals(skuTicketId))).isFalse();
        assertThat(dsl().selectFrom(GC_SKU_VALIDATION).stream().findAny().isPresent()).isFalse();
        assertThat(dsl().selectFrom(GC_CLEAN_WEB_TEXT_VALIDATION).stream().anyMatch(v -> v.getId().equals(cwTextValidationId))).isTrue();
        assertThat(dsl().selectFrom(GC_CLEAN_WEB_IMAGE_VALIDATION).stream().anyMatch(v -> v.getId().equals(cwImageValidationId))).isTrue();

    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void shouldRemoveMessageReportFacts() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoDaysAgo = now.minusDays(2);
        long ticketId = 1001L;

        long dataBucketId = dsl().insertInto(DATA_BUCKET)
                .set(DATA_BUCKET.CATEGORY_ID, 1L)
                .set(DATA_BUCKET.SOURCE_ID, -1)
                .returning(DATA_BUCKET.ID)
                .fetchOne()
                .getValue(DATA_BUCKET.ID);

        dsl().insertInto(GC_SKU_TICKET, GC_SKU_TICKET.ID, GC_SKU_TICKET.SOURCE_ID,
                        GC_SKU_TICKET.CATEGORY_ID, GC_SKU_TICKET.STATUS,
                        GC_SKU_TICKET.CREATE_DATE, GC_SKU_TICKET.UPDATE_DATE, GC_SKU_TICKET.DATA_BUCKET_ID,
                        GC_SKU_TICKET.VALID, GC_SKU_TICKET.TYPE)
                .values(ticketId, -1, 1L, GcSkuTicketStatus.SUCCESS, Timestamp.valueOf(twoDaysAgo),
                        Timestamp.valueOf(twoDaysAgo), dataBucketId, true, GcSkuTicketType.DATA_CAMP).execute();

        dsl().insertInto(GC_MESSAGE_REPORT_FACTS)
                .set(GC_MESSAGE_REPORT_FACTS.DATA_BUCKET_ID, dataBucketId)
                .set(GC_MESSAGE_REPORT_FACTS.PROTOCOL_MESSAGE_ID, 1L)
                .execute();

        dataCleanerService.cleanFatTables(Timestamp.valueOf(now));

        List<Long> ticketIds = dsl().select(GC_SKU_TICKET.ID)
                .from(GC_SKU_TICKET)
                .where(GC_SKU_TICKET.ID.equal(ticketId))
                .fetch(GC_SKU_TICKET.ID);
        assertThat(ticketIds).hasSize(0);

        List<Long> messageReportFactIds = dsl().select(GC_MESSAGE_REPORT_FACTS.DATA_BUCKET_ID)
                .from(GC_MESSAGE_REPORT_FACTS)
                .where(GC_MESSAGE_REPORT_FACTS.DATA_BUCKET_ID.equal(dataBucketId))
                .fetch(GC_MESSAGE_REPORT_FACTS.DATA_BUCKET_ID);
        assertThat(messageReportFactIds).hasSize(0);
    }

    @Test
    public void shouldRemoveProtocolMessagesWithoutLinks() {
        dsl().insertInto(PROTOCOL_MESSAGE, PROTOCOL_MESSAGE.ID, PROTOCOL_MESSAGE.CODE, PROTOCOL_MESSAGE.PARAMS)
                .values(1L, "", Collections.emptyMap())
                .values(2L, "", Collections.emptyMap())
                .execute();

        dataCleanerService.cleanProtocolMessages();

        assertEquals(0, dsl().selectFrom(PROTOCOL_MESSAGE).stream().count());
    }

    @Test
    public void shouldNotRemoveProtocolMessagesWithLinks() {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        long fileProcessId = 1L;

        long dataBucketId = dsl().insertInto(DATA_BUCKET)
                .set(DATA_BUCKET.CATEGORY_ID, 1L)
                .set(DATA_BUCKET.SOURCE_ID, -1)
                .returning(DATA_BUCKET.ID)
                .fetchOne()
                .getValue(DATA_BUCKET.ID);

        dsl().insertInto(PROTOCOL_MESSAGE, PROTOCOL_MESSAGE.ID, PROTOCOL_MESSAGE.CODE, PROTOCOL_MESSAGE.PARAMS)
                .values(1L, "", Collections.emptyMap())
                .values(2L, "", Collections.emptyMap())
                .values(3L, "", Collections.emptyMap())
                .execute();

        dsl().insertInto(GC_SKU_VALIDATION, GC_SKU_VALIDATION.ID,
                        GC_SKU_VALIDATION.VALIDATION_TYPE, GC_SKU_VALIDATION.CHECK_DATE)
                .values(1L, GcSkuValidationType.CLEAN_WEB_TEXT, now)
                .execute();

        dsl().insertInto(GC_VALIDATION_MESSAGE, GC_VALIDATION_MESSAGE.PROTOCOL_MESSAGE_ID,
                        GC_VALIDATION_MESSAGE.VALIDATION_ID)
                .values(1L, 1L)
                .execute();

        dsl().insertInto(DATA_BUCKET_MESSAGE, DATA_BUCKET_MESSAGE.PROTOCOL_MESSAGE_ID,
                        DATA_BUCKET_MESSAGE.DATA_BUCKET_ID, DATA_BUCKET_MESSAGE.TYPE)
                .values(2L, dataBucketId, DataBucketMessageType.IS_SKU_VALIDATION)
                .execute();

        dsl().insertInto(FILE_DATA_PROCESS_REQUEST, FILE_DATA_PROCESS_REQUEST.ID,
                        FILE_DATA_PROCESS_REQUEST.SOURCE_ID, FILE_DATA_PROCESS_REQUEST.FILE_TYPE,
                        FILE_DATA_PROCESS_REQUEST.URL, FILE_DATA_PROCESS_REQUEST.CREATE_TIME)
                .values(1L, 1, FileType.ONE_CATEGORY_SIMPLE_EXCEL, "/", now)
                .execute();

        dsl().insertInto(ABSTRACT_PROCESS, ABSTRACT_PROCESS.ID, ABSTRACT_PROCESS.REQUEST_ID,
                        ABSTRACT_PROCESS.PROCESS_TYPE)
                .values(fileProcessId, 1L, ProcessType.BETTER_FILE_PROCESS)
                .execute();

        dsl().insertInto(FILE_PROCESS, FILE_PROCESS.ID, FILE_PROCESS.FILE_DATA_PROCESS_REQUEST_ID,
                        FILE_PROCESS.CREATE_TIME, FILE_PROCESS.UPDATE_TIME, FILE_PROCESS.PROCESS_STATE)
                .values(fileProcessId, 1L, now, now, FileProcessState.FINISHED)
                .execute();

        dsl().insertInto(FILE_PROCESS_MESSAGE, FILE_PROCESS_MESSAGE.PROTOCOL_MESSAGE_ID,
                        FILE_PROCESS_MESSAGE.FILE_PROCESS_ID, FILE_PROCESS_MESSAGE.TYPE)
                .values(3L, fileProcessId, FileProcessMessageType.MODEL_VALIDATION)
                .execute();

        dataCleanerService.cleanProtocolMessages();

        assertEquals(3, dsl().selectFrom(PROTOCOL_MESSAGE).stream().count());
    }

    @Ignore
    @Test
    public void shouldRemoveOldXlsDatacampOffersAndRelated() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoDaysAgo = now.minusDays(2);

        Long rawModelId = 1L;
        Long protocolMessageId = 1L;
        Long fdprId = 1000L;
        Long fileProcessId = 100L;
        Long gcRawSkuId = 10L;
        Long xlsDatacampOfferId = 1L;
        Long templateFeedUploadId = 1L;

        dsl().insertInto(FILE_DATA_PROCESS_REQUEST, FILE_DATA_PROCESS_REQUEST.ID,
                        FILE_DATA_PROCESS_REQUEST.SOURCE_ID, FILE_DATA_PROCESS_REQUEST.FILE_TYPE,
                        FILE_DATA_PROCESS_REQUEST.URL, FILE_DATA_PROCESS_REQUEST.CREATE_TIME)
                .values(fdprId, 1, FileType.ONE_CATEGORY_SIMPLE_EXCEL, "/", Timestamp.valueOf(twoDaysAgo)).execute();
        dsl().insertInto(ABSTRACT_PROCESS, ABSTRACT_PROCESS.ID, ABSTRACT_PROCESS.REQUEST_ID,
                        ABSTRACT_PROCESS.PROCESS_TYPE)
                .values(fileProcessId, 1L, ProcessType.BETTER_FILE_PROCESS).execute();
        dsl().insertInto(FILE_PROCESS, FILE_PROCESS.ID, FILE_PROCESS.FILE_DATA_PROCESS_REQUEST_ID,
                        FILE_PROCESS.CREATE_TIME, FILE_PROCESS.UPDATE_TIME, FILE_PROCESS.PROCESS_STATE)
                .values(fileProcessId, fdprId, Timestamp.valueOf(twoDaysAgo), Timestamp.valueOf(twoDaysAgo),
                        FileProcessState.FINISHED).execute();
        dsl().insertInto(FILE_MDS_COPY, FILE_MDS_COPY.FILE_PROCESS_ID, FILE_MDS_COPY.BUCKET,
                        FILE_MDS_COPY.KEY, FILE_MDS_COPY.URL)
                .values(fileProcessId, "", "", "").execute();
        dsl().insertInto(PROTOCOL_MESSAGE, PROTOCOL_MESSAGE.ID, PROTOCOL_MESSAGE.CODE, PROTOCOL_MESSAGE.PARAMS)
                        .values(protocolMessageId, "", Collections.emptyMap()).execute();
        dsl().insertInto(FILE_PROCESS_MESSAGE, FILE_PROCESS_MESSAGE.FILE_PROCESS_ID,
                        FILE_PROCESS_MESSAGE.PROTOCOL_MESSAGE_ID, FILE_PROCESS_MESSAGE.TYPE)
                .values(fileProcessId, protocolMessageId, FileProcessMessageType.MODEL_VALIDATION).execute();
        dsl().insertInto(RAW_MODEL, RAW_MODEL.ID, RAW_MODEL.CATEGORY_ID, RAW_MODEL.SOURCE_ID,
                        RAW_MODEL.NAME, RAW_MODEL.MODEL_DATA)
                .values(rawModelId, 1L, 1, "", new Model()).execute();
        dsl().insertInto(FILE_PROCESS_RAW_MODEL, FILE_PROCESS_RAW_MODEL.FILE_PROCESS_ID,
                        FILE_PROCESS_RAW_MODEL.RAW_MODEL_ID)
                .values(fileProcessId, rawModelId).execute();
        dsl().insertInto(GC_RAW_SKU, GC_RAW_SKU.ID, GC_RAW_SKU.FILE_PROCESS_ID,
                        GC_RAW_SKU.CREATE_DATE, GC_RAW_SKU.DATA)
                .values(gcRawSkuId, fileProcessId, Timestamp.valueOf(twoDaysAgo), new RawSku()).execute();
        dsl().insertInto(XLS_DATACAMP_OFFER, XLS_DATACAMP_OFFER.ID, XLS_DATACAMP_OFFER.GC_RAW_SKU_ID,
                        XLS_DATACAMP_OFFER.STATUS, XLS_DATACAMP_OFFER.CREATE_DATE,
                        XLS_DATACAMP_OFFER.UPDATE_DATE, XLS_DATACAMP_OFFER.DATACAMP_OFFER)
                .values(xlsDatacampOfferId, gcRawSkuId, XlsLogbrokerStatus.SUCCESS,
                        Timestamp.valueOf(twoDaysAgo), Timestamp.valueOf(twoDaysAgo),
                        DataCampOffer.Offer.newBuilder().build()).execute();
        dsl().insertInto(TEMPLATE_FEED_UPLOAD, TEMPLATE_FEED_UPLOAD.ID,
                        TEMPLATE_FEED_UPLOAD.FILE_DATA_PROCESS_REQUEST_ID,
                        TEMPLATE_FEED_UPLOAD.FEED_NAME, TEMPLATE_FEED_UPLOAD.UPLOAD_TS,
                        TEMPLATE_FEED_UPLOAD.CONTENT_TYPE, TEMPLATE_FEED_UPLOAD.BUSINESS_ID,
                        TEMPLATE_FEED_UPLOAD.BYTE_SIZE)
                .values(templateFeedUploadId, fdprId, "", Timestamp.valueOf(now), "", 1, 1L).execute();

        dataCleanerService.cleanXlsDatacampOffersAndRelated(Timestamp.valueOf(now));

        List<Long> fdprIds = dsl().select(FILE_DATA_PROCESS_REQUEST.ID)
                .from(FILE_DATA_PROCESS_REQUEST)
                .where(FILE_DATA_PROCESS_REQUEST.ID.equal(fdprId))
                .fetch(FILE_DATA_PROCESS_REQUEST.ID);
        assertThat(fdprIds).hasSize(0);

        List<Long> fileProcessIds = dsl().select(FILE_PROCESS.ID)
                .from(FILE_PROCESS)
                .where(FILE_PROCESS.ID.equal(fileProcessId))
                .fetch(FILE_PROCESS.ID);
        assertThat(fileProcessIds).hasSize(0);

        List<Long> abstractProcessIds = dsl().select(ABSTRACT_PROCESS.ID)
                .from(ABSTRACT_PROCESS)
                .where(ABSTRACT_PROCESS.ID.equal(fileProcessId))
                .fetch(ABSTRACT_PROCESS.ID);
        assertThat(abstractProcessIds).hasSize(0);

        List<Long> fileMdsCopyIds = dsl().select(FILE_MDS_COPY.FILE_PROCESS_ID)
                .from(FILE_MDS_COPY)
                .where(FILE_MDS_COPY.FILE_PROCESS_ID.equal(fileProcessId))
                .fetch(FILE_MDS_COPY.FILE_PROCESS_ID);
        assertThat(fileMdsCopyIds).hasSize(0);

        List<Long> fileProcessMessageIds = dsl().select(FILE_PROCESS_MESSAGE.FILE_PROCESS_ID)
                .from(FILE_PROCESS_MESSAGE)
                .where(FILE_PROCESS_MESSAGE.FILE_PROCESS_ID.equal(fileProcessId))
                .fetch(FILE_PROCESS_MESSAGE.FILE_PROCESS_ID);
        assertThat(fileProcessMessageIds).hasSize(0);

        List<Long> fileProcessRawModelIds = dsl().select(FILE_PROCESS_RAW_MODEL.FILE_PROCESS_ID)
                .from(FILE_PROCESS_RAW_MODEL)
                .where(FILE_PROCESS_RAW_MODEL.FILE_PROCESS_ID.equal(fileProcessId))
                .fetch(FILE_PROCESS_RAW_MODEL.FILE_PROCESS_ID);
        assertThat(fileProcessRawModelIds).hasSize(0);

        List<Long> gcuRawSkuIds = dsl().select(GC_RAW_SKU.ID)
                .from(GC_RAW_SKU)
                .where(GC_RAW_SKU.ID.equal(gcRawSkuId))
                .fetch(GC_RAW_SKU.ID);
        assertThat(gcuRawSkuIds).hasSize(0);

        List<Long> xlsDatacampOfferIds = dsl().select(XLS_DATACAMP_OFFER.ID)
                .from(XLS_DATACAMP_OFFER)
                .where(XLS_DATACAMP_OFFER.ID.equal(xlsDatacampOfferId))
                .fetch(XLS_DATACAMP_OFFER.ID);
        assertThat(xlsDatacampOfferIds).hasSize(0);

        List<Long> templateFeedUploadIds = dsl().select(TEMPLATE_FEED_UPLOAD.ID)
                .from(TEMPLATE_FEED_UPLOAD)
                .where(TEMPLATE_FEED_UPLOAD.ID.equal(templateFeedUploadId))
                .fetch(TEMPLATE_FEED_UPLOAD.ID);
        assertThat(templateFeedUploadIds).hasSize(0);
    }

    @Test
    public void shouldNotRemoveNewXlsDatacampOffersAndRelated() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoDaysAgo = now.minusDays(2);
        LocalDateTime threeDaysAgo = now.minusDays(3);

        Long fdprId = 1000L;
        Long fileProcessId = 100L;
        Long gcRawSkuId = 10L;
        Long firstXlsDatacampOfferId = 1L;
        Long secondXlsDatacampOfferId = 2L;

        dsl().insertInto(FILE_DATA_PROCESS_REQUEST, FILE_DATA_PROCESS_REQUEST.ID,
                        FILE_DATA_PROCESS_REQUEST.SOURCE_ID, FILE_DATA_PROCESS_REQUEST.FILE_TYPE,
                        FILE_DATA_PROCESS_REQUEST.URL, FILE_DATA_PROCESS_REQUEST.CREATE_TIME)
                .values(fdprId, 1, FileType.ONE_CATEGORY_SIMPLE_EXCEL, "/", Timestamp.valueOf(twoDaysAgo)).execute();
        dsl().insertInto(ABSTRACT_PROCESS, ABSTRACT_PROCESS.ID, ABSTRACT_PROCESS.REQUEST_ID,
                        ABSTRACT_PROCESS.PROCESS_TYPE)
                .values(fileProcessId, 1L, ProcessType.BETTER_FILE_PROCESS).execute();
        dsl().insertInto(FILE_PROCESS, FILE_PROCESS.ID, FILE_PROCESS.FILE_DATA_PROCESS_REQUEST_ID,
                        FILE_PROCESS.CREATE_TIME, FILE_PROCESS.UPDATE_TIME, FILE_PROCESS.PROCESS_STATE)
                .values(fileProcessId, fdprId, Timestamp.valueOf(twoDaysAgo), Timestamp.valueOf(twoDaysAgo),
                        FileProcessState.FINISHED).execute();
        dsl().insertInto(GC_RAW_SKU, GC_RAW_SKU.ID, GC_RAW_SKU.FILE_PROCESS_ID,
                        GC_RAW_SKU.CREATE_DATE, GC_RAW_SKU.DATA)
                .values(gcRawSkuId, fileProcessId, Timestamp.valueOf(now), new RawSku()).execute();
        dsl().insertInto(XLS_DATACAMP_OFFER, XLS_DATACAMP_OFFER.ID, XLS_DATACAMP_OFFER.GC_RAW_SKU_ID,
                        XLS_DATACAMP_OFFER.STATUS, XLS_DATACAMP_OFFER.CREATE_DATE,
                        XLS_DATACAMP_OFFER.UPDATE_DATE, XLS_DATACAMP_OFFER.DATACAMP_OFFER)
                .values(firstXlsDatacampOfferId, gcRawSkuId, XlsLogbrokerStatus.SUCCESS,
                        Timestamp.valueOf(now), Timestamp.valueOf(now),
                        DataCampOffer.Offer.newBuilder().build()).execute();
        dsl().insertInto(XLS_DATACAMP_OFFER, XLS_DATACAMP_OFFER.ID, XLS_DATACAMP_OFFER.GC_RAW_SKU_ID,
                        XLS_DATACAMP_OFFER.STATUS, XLS_DATACAMP_OFFER.CREATE_DATE,
                        XLS_DATACAMP_OFFER.UPDATE_DATE, XLS_DATACAMP_OFFER.DATACAMP_OFFER)
                .values(secondXlsDatacampOfferId, gcRawSkuId, XlsLogbrokerStatus.PROCESSING,
                        Timestamp.valueOf(threeDaysAgo), Timestamp.valueOf(threeDaysAgo),
                        DataCampOffer.Offer.newBuilder().build()).execute();

        dataCleanerService.cleanXlsDatacampOffersAndRelated(Timestamp.valueOf(twoDaysAgo));

        List<Long> gcuRawSkuIds = dsl().select(GC_RAW_SKU.ID)
                .from(GC_RAW_SKU)
                .where(GC_RAW_SKU.ID.equal(gcRawSkuId))
                .fetch(GC_RAW_SKU.ID);
        assertThat(gcuRawSkuIds).hasSize(1);

        List<Long> xlsDatacampOfferIds = dsl().select(XLS_DATACAMP_OFFER.ID)
                .from(XLS_DATACAMP_OFFER)
                .where(XLS_DATACAMP_OFFER.ID.in(firstXlsDatacampOfferId, secondXlsDatacampOfferId))
                .fetch(XLS_DATACAMP_OFFER.ID);
        assertThat(xlsDatacampOfferIds).hasSize(2);
    }

    @Test
    public void shouldRemoveOldTicketRequests() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoDaysAgo = now.minusDays(2);
        long ticketId = 1001L;
        long requestId = 2001L;

        dsl().insertInto(GC_SKU_TICKET, GC_SKU_TICKET.ID, GC_SKU_TICKET.SOURCE_ID,
                        GC_SKU_TICKET.CATEGORY_ID, GC_SKU_TICKET.STATUS,
                        GC_SKU_TICKET.CREATE_DATE, GC_SKU_TICKET.UPDATE_DATE, GC_SKU_TICKET.VALID, GC_SKU_TICKET.TYPE)
                .values(ticketId, -1, 1L, GcSkuTicketStatus.SUCCESS, Timestamp.valueOf(twoDaysAgo),
                        Timestamp.valueOf(twoDaysAgo), true, GcSkuTicketType.DATA_CAMP).execute();
        dsl().insertInto(GC_EXTERNAL_SERVICE_REQUEST, GC_EXTERNAL_SERVICE_REQUEST.ID, GC_EXTERNAL_SERVICE_REQUEST.REQUEST,
                GC_EXTERNAL_SERVICE_REQUEST.TYPE, GC_EXTERNAL_SERVICE_REQUEST.STATUS)
                        .values(requestId, ModelStorage.Model.newBuilder().buildPartial(), GcExternalRequestType.MBO_SAVE_PSKUS, GcExternalRequestStatus.FINISHED)
                                .execute();
        dsl().insertInto(GC_TICKET_REQUEST, GC_TICKET_REQUEST.TICKET_ID, GC_TICKET_REQUEST.REQUEST_ID)
                .values(ticketId, requestId).execute();

        dataCleanerService.cleanGcSkuTicketsRequests();

        List<Long> ticketIds = dsl().select(GC_TICKET_REQUEST.TICKET_ID)
                .from(GC_TICKET_REQUEST)
                .where(GC_TICKET_REQUEST.TICKET_ID.equal(ticketId))
                .fetch(GC_TICKET_REQUEST.TICKET_ID);
        assertThat(ticketIds).hasSize(0);

        List<Long> requestIds = dsl().select(GC_EXTERNAL_SERVICE_REQUEST.ID)
                .from(GC_EXTERNAL_SERVICE_REQUEST)
                .where(GC_EXTERNAL_SERVICE_REQUEST.ID.equal(requestId))
                .fetch(GC_EXTERNAL_SERVICE_REQUEST.ID);
        assertThat(requestIds).hasSize(0);

    }

    @Test
    public void shouldNotRemoveNotSoOldTicketRequests() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lessThanDayAgo = now.minusHours(2);
        long ticketId = 1001L;
        long requestId = 2001L;

        dsl().insertInto(GC_SKU_TICKET, GC_SKU_TICKET.ID, GC_SKU_TICKET.SOURCE_ID,
                        GC_SKU_TICKET.CATEGORY_ID, GC_SKU_TICKET.STATUS,
                        GC_SKU_TICKET.CREATE_DATE, GC_SKU_TICKET.UPDATE_DATE, GC_SKU_TICKET.VALID, GC_SKU_TICKET.TYPE)
                .values(ticketId, -1, 1L, GcSkuTicketStatus.SUCCESS, Timestamp.valueOf(lessThanDayAgo),
                        Timestamp.valueOf(lessThanDayAgo), true, GcSkuTicketType.DATA_CAMP).execute();
        dsl().insertInto(GC_EXTERNAL_SERVICE_REQUEST, GC_EXTERNAL_SERVICE_REQUEST.ID, GC_EXTERNAL_SERVICE_REQUEST.REQUEST,
                GC_EXTERNAL_SERVICE_REQUEST.TYPE, GC_EXTERNAL_SERVICE_REQUEST.STATUS)
                        .values(requestId, ModelStorage.Model.newBuilder().buildPartial(), GcExternalRequestType.MBO_SAVE_PSKUS, GcExternalRequestStatus.FINISHED)
                                .execute();
        dsl().insertInto(GC_TICKET_REQUEST, GC_TICKET_REQUEST.TICKET_ID, GC_TICKET_REQUEST.REQUEST_ID)
                .values(ticketId, requestId).execute();

        dataCleanerService.cleanGcSkuTicketsRequests();

        List<Long> ticketIds = dsl().select(GC_TICKET_REQUEST.TICKET_ID)
                .from(GC_TICKET_REQUEST)
                .where(GC_TICKET_REQUEST.TICKET_ID.equal(ticketId))
                .fetch(GC_TICKET_REQUEST.TICKET_ID);
        assertThat(ticketIds).hasSize(1);

        List<Long> requestIds = dsl().select(GC_EXTERNAL_SERVICE_REQUEST.ID)
                .from(GC_EXTERNAL_SERVICE_REQUEST)
                .where(GC_EXTERNAL_SERVICE_REQUEST.ID.equal(requestId))
                .fetch(GC_EXTERNAL_SERVICE_REQUEST.ID);
        assertThat(requestIds).hasSize(1);

    }

    @Test
    public void shouldNotRemoveUncompleteTicketRequests() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoDaysAgo = now.minusDays(2);
        long ticketId = 1001L;
        long requestId = 2001L;

        dsl().insertInto(GC_SKU_TICKET, GC_SKU_TICKET.ID, GC_SKU_TICKET.SOURCE_ID,
                        GC_SKU_TICKET.CATEGORY_ID, GC_SKU_TICKET.STATUS,
                        GC_SKU_TICKET.CREATE_DATE, GC_SKU_TICKET.UPDATE_DATE, GC_SKU_TICKET.VALID, GC_SKU_TICKET.TYPE)
                .values(ticketId, -1, 1L, GcSkuTicketStatus.WAITING, Timestamp.valueOf(twoDaysAgo),
                        Timestamp.valueOf(twoDaysAgo), true, GcSkuTicketType.DATA_CAMP).execute();
        dsl().insertInto(GC_EXTERNAL_SERVICE_REQUEST, GC_EXTERNAL_SERVICE_REQUEST.ID, GC_EXTERNAL_SERVICE_REQUEST.REQUEST,
                        GC_EXTERNAL_SERVICE_REQUEST.TYPE, GC_EXTERNAL_SERVICE_REQUEST.STATUS)
                .values(requestId, ModelStorage.Model.newBuilder().buildPartial(), GcExternalRequestType.MBO_SAVE_PSKUS, GcExternalRequestStatus.FINISHED)
                .execute();
        dsl().insertInto(GC_TICKET_REQUEST, GC_TICKET_REQUEST.TICKET_ID, GC_TICKET_REQUEST.REQUEST_ID)
                .values(ticketId, requestId).execute();

        dataCleanerService.cleanGcSkuTicketsRequests();

        List<Long> ticketIds = dsl().select(GC_TICKET_REQUEST.TICKET_ID)
                .from(GC_TICKET_REQUEST)
                .where(GC_TICKET_REQUEST.TICKET_ID.equal(ticketId))
                .fetch(GC_TICKET_REQUEST.TICKET_ID);
        assertThat(ticketIds).hasSize(1);

        List<Long> requestIds = dsl().select(GC_EXTERNAL_SERVICE_REQUEST.ID)
                .from(GC_EXTERNAL_SERVICE_REQUEST)
                .where(GC_EXTERNAL_SERVICE_REQUEST.ID.equal(requestId))
                .fetch(GC_EXTERNAL_SERVICE_REQUEST.ID);
        assertThat(requestIds).hasSize(1);

    }

    @Test
    public void shouldRemoveNullTicketGcSkuValidations() {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp twoDaysAgo = Timestamp.valueOf(LocalDateTime.now().minusDays(2));

        Long gcSkuValidationId = 1L;
        Long datacampOfferId = 1L;

        dsl().insertInto(DATACAMP_OFFER,
                        DATACAMP_OFFER.ID,
                        DATACAMP_OFFER.BUSINESS_ID,
                        DATACAMP_OFFER.OFFER_ID,
                        DATACAMP_OFFER.CREATE_TIME,
                        DATACAMP_OFFER.STATUS,
                        DATACAMP_OFFER.REQUEST_TS)
                .values(datacampOfferId, 100, "text", now, DatacampOfferStatus.NEW, now)
                .execute();

        dsl().insertInto(GC_SKU_TICKET,
                        GC_SKU_TICKET.ID,
                        GC_SKU_TICKET.SOURCE_ID,
                        GC_SKU_TICKET.CATEGORY_ID,
                        GC_SKU_TICKET.STATUS,
                        GC_SKU_TICKET.CREATE_DATE,
                        GC_SKU_TICKET.UPDATE_DATE,
                        GC_SKU_TICKET.DATACAMP_OFFER_ID)
                .values(100L, -1, 100L, GcSkuTicketStatus.SUCCESS, now, now, datacampOfferId)
                .execute();

        dsl().insertInto(GC_SKU_VALIDATION,
                        GC_SKU_VALIDATION.ID,
                        GC_SKU_VALIDATION.SKU_TICKET_ID,
                        GC_SKU_VALIDATION.VALIDATION_TYPE,
                        GC_SKU_VALIDATION.CHECK_DATE)
                .values(gcSkuValidationId, null, GcSkuValidationType.ASSORTMENT_EXISTENCE, now)
                .execute();

        dataCleanerService.cleanFatTables(twoDaysAgo);

        List<Long> gcSkuValidationIds = dsl()
                .select(GC_SKU_VALIDATION.ID)
                .from(GC_SKU_VALIDATION)
                .where(GC_SKU_VALIDATION.ID.equal(gcSkuValidationId))
                .fetch(GC_SKU_VALIDATION.ID);
        assertThat(gcSkuValidationIds).hasSize(0);
    }

    @Test
    public void shouldRemoveOldTicketGcSkuValidations() {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp twoDaysAgo = Timestamp.valueOf(LocalDateTime.now().minusDays(2));

        Long firstGcSkuValidationId = 1L;
        Long secondGcSkuValidationId = 2L;
        Long datacampOfferId = 1L;
        Long gcSkuTicketId = 1L;

        dsl().insertInto(DATACAMP_OFFER,
                        DATACAMP_OFFER.ID,
                        DATACAMP_OFFER.BUSINESS_ID,
                        DATACAMP_OFFER.OFFER_ID,
                        DATACAMP_OFFER.CREATE_TIME,
                        DATACAMP_OFFER.STATUS,
                        DATACAMP_OFFER.REQUEST_TS)
                .values(datacampOfferId, 100, "text", now, DatacampOfferStatus.NEW, now)
                .execute();

        dsl().insertInto(GC_SKU_TICKET,
                        GC_SKU_TICKET.ID,
                        GC_SKU_TICKET.SOURCE_ID,
                        GC_SKU_TICKET.CATEGORY_ID,
                        GC_SKU_TICKET.STATUS,
                        GC_SKU_TICKET.CREATE_DATE,
                        GC_SKU_TICKET.UPDATE_DATE,
                        GC_SKU_TICKET.DATACAMP_OFFER_ID)
                .values(gcSkuTicketId, -1, 100L, GcSkuTicketStatus.SUCCESS, twoDaysAgo, twoDaysAgo, datacampOfferId)
                .execute();

        dsl().insertInto(GC_SKU_VALIDATION,
                        GC_SKU_VALIDATION.ID,
                        GC_SKU_VALIDATION.SKU_TICKET_ID,
                        GC_SKU_VALIDATION.VALIDATION_TYPE,
                        GC_SKU_VALIDATION.CHECK_DATE)
                .values(firstGcSkuValidationId, gcSkuTicketId, GcSkuValidationType.ASSORTMENT_EXISTENCE, now)
                .values(secondGcSkuValidationId, gcSkuTicketId, GcSkuValidationType.CLEAN_WEB_IMAGE, now)
                .execute();

        dataCleanerService.cleanFatTables(now);

        List<Long> gcSkuValidationIds = dsl()
                .select(GC_SKU_VALIDATION.ID)
                .from(GC_SKU_VALIDATION)
                .where(GC_SKU_VALIDATION.ID.in(firstGcSkuValidationId, secondGcSkuValidationId))
                .fetch(GC_SKU_VALIDATION.ID);
        assertThat(gcSkuValidationIds).hasSize(0);
    }

    @Test
    public void shouldDRemoveOldActivatedAndSkippedOffers() {
        Timestamp oldTimestamp = Timestamp.valueOf(LocalDateTime.now().minusMonths(12));
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());

        dsl().insertInto(DATACAMP_OFFER,
                        DATACAMP_OFFER.ID,
                        DATACAMP_OFFER.BUSINESS_ID,
                        DATACAMP_OFFER.OFFER_ID,
                        DATACAMP_OFFER.CREATE_TIME,
                        DATACAMP_OFFER.STATUS,
                        DATACAMP_OFFER.REQUEST_TS)
                // Activated offer
                .values(100L, 100, "UAHSUDHUADHSD", oldTimestamp, DatacampOfferStatus.ACTIVATED,  timestamp)
                // Skipped offer
                .values(101L, 101, "OAKSOPDKAP", oldTimestamp, DatacampOfferStatus.SKIPPED, timestamp)
                .execute();


        dataCleanerService.cleanFatTables(OLDNESS_THRESHOLD);


        assertFalse(dsl().selectFrom(DATACAMP_OFFER).stream().findAny().isPresent());
    }

    @Test
    public void shouldNotRemoveNewAndExcepted() {
        Timestamp oldTimestamp = Timestamp.valueOf(LocalDateTime.now().minusMonths(12));
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());

        long newId = 100L;
        long exceptedId = 101L;

        dsl().insertInto(DATACAMP_OFFER,
                        DATACAMP_OFFER.ID,
                        DATACAMP_OFFER.BUSINESS_ID,
                        DATACAMP_OFFER.OFFER_ID,
                        DATACAMP_OFFER.CREATE_TIME,
                        DATACAMP_OFFER.STATUS,
                        DATACAMP_OFFER.REQUEST_TS)
                // New offer
                .values(newId, 100, "UAHSUDHUADHSD", oldTimestamp, DatacampOfferStatus.NEW,  timestamp)
                // Skipped offer
                .values(exceptedId, 101, "OAKSOPDKAP", oldTimestamp, DatacampOfferStatus.EXCEPTED, timestamp)
                .execute();


        dataCleanerService.cleanFatTables(OLDNESS_THRESHOLD);


        List<Long> ids = dsl().select(DATACAMP_OFFER.ID).from(DATACAMP_OFFER).fetch(DATACAMP_OFFER.ID);

        assertThat(ids).containsExactly(newId, exceptedId);
    }

    @Test
    public void shouldNotDeleteOfferWithRef() {
        Timestamp oldTimestamp = Timestamp.valueOf(LocalDateTime.now().minusMonths(12));
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());

        long offerId = 100L;

        dsl().insertInto(DATACAMP_OFFER,
                        DATACAMP_OFFER.ID,
                        DATACAMP_OFFER.BUSINESS_ID,
                        DATACAMP_OFFER.OFFER_ID,
                        DATACAMP_OFFER.CREATE_TIME,
                        DATACAMP_OFFER.STATUS,
                        DATACAMP_OFFER.REQUEST_TS)
                .values(offerId, 100, "UAHSUDHUADHSD", oldTimestamp, DatacampOfferStatus.NEW,  timestamp)
                .execute();

        dsl().insertInto(GC_SKU_TICKET,
                        GC_SKU_TICKET.ID,
                        GC_SKU_TICKET.SOURCE_ID,
                        GC_SKU_TICKET.CATEGORY_ID,
                        GC_SKU_TICKET.STATUS,
                        GC_SKU_TICKET.CREATE_DATE,
                        GC_SKU_TICKET.UPDATE_DATE,
                        GC_SKU_TICKET.DATACAMP_OFFER_ID)
                .values(100L, -1, 100L, GcSkuTicketStatus.SUCCESS, oldTimestamp, timestamp, offerId)
                .execute();


        dataCleanerService.cleanFatTables(OLDNESS_THRESHOLD);


        assertThat(dsl().selectFrom(GC_SKU_TICKET).fetch(GC_SKU_TICKET.ID)).contains(offerId);
    }

    @Test
    public void shouldRemoveOldWhenHasRefFromFake() {
        Timestamp oldTimestamp = Timestamp.valueOf(LocalDateTime.now().minusMonths(12));
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());

        long offerId = 100L;

        dsl().insertInto(DATACAMP_OFFER,
                        DATACAMP_OFFER.ID,
                        DATACAMP_OFFER.BUSINESS_ID,
                        DATACAMP_OFFER.OFFER_ID,
                        DATACAMP_OFFER.CREATE_TIME,
                        DATACAMP_OFFER.STATUS,
                        DATACAMP_OFFER.REQUEST_TS)
                .values(offerId, 100, "UAHSUDHUADHSD", oldTimestamp, DatacampOfferStatus.ACTIVATED,  timestamp)
                .execute();

        dsl().insertInto(FAKE_DATACAMP_OFFER, FAKE_DATACAMP_OFFER.DATACAMP_OFFER_ID)
                .values(offerId)
                .execute();

        dataCleanerService.cleanFatTables(OLDNESS_THRESHOLD);

        assertFalse(dsl().selectFrom(DATACAMP_OFFER).stream().findAny().isPresent());
    }

    @Test
    public void shouldCancelGsTicketsOnPipelineCancel() {
        Timestamp oldTimestamp = Timestamp.valueOf(LocalDateTime.now().minusMonths(12));

        dsl().insertInto(DATA_BUCKET)
                .columns(
                        DATA_BUCKET.ID,
                        DATA_BUCKET.SOURCE_ID,
                        DATA_BUCKET.CATEGORY_ID
                )
                .values(1L, -1, 100L)
                .values(2L, -1, 100L)
                .values(3L, -1, 100L)
                .execute();

        dsl().insertInto(PIPELINE, PIPELINE.ID, PIPELINE.TYPE, PIPELINE.DATA_BUCKET_ID,
                        PIPELINE.START_DATE, PIPELINE.UPDATE_DATE, PIPELINE.STATUS, PIPELINE.INPUT_DATA)
                .values(300L, PipelineType.DATA_CAMP, 1L, oldTimestamp,
                        oldTimestamp, MrgrienPipelineStatus.CANCELLED, new EmptyData())
                .values(301L, PipelineType.DATA_CAMP, 2L, Timestamp.from(Instant.now()),
                        Timestamp.from(Instant.now()), MrgrienPipelineStatus.FINISHED, new EmptyData())
                .values(302L, PipelineType.DATA_CAMP, 3L, Timestamp.from(Instant.now()),
                        Timestamp.from(Instant.now()), MrgrienPipelineStatus.CANCELLED, new EmptyData())
                .execute();

        dsl().insertInto(GC_SKU_TICKET,
                        GC_SKU_TICKET.ID,
                        GC_SKU_TICKET.DATA_BUCKET_ID,
                        GC_SKU_TICKET.SOURCE_ID,
                        GC_SKU_TICKET.CATEGORY_ID,
                        GC_SKU_TICKET.STATUS,
                        GC_SKU_TICKET.CREATE_DATE,
                        GC_SKU_TICKET.UPDATE_DATE)
                .values(100L, 1L, -1, 100L, GcSkuTicketStatus.SUCCESS,
                        Timestamp.from(Instant.now()), Timestamp.from(Instant.now()))
                .values(101L, 2L, -1, 100L, GcSkuTicketStatus.SUCCESS,
                        Timestamp.from(Instant.now()), Timestamp.from(Instant.now()))
                .values(102L, 3L, -1, 100L, GcSkuTicketStatus.SUCCESS,
                        Timestamp.from(Instant.now()), Timestamp.from(Instant.now()))
                .execute();

        dataCleanerService.cleanFatTables(OLDNESS_THRESHOLD);

        Record canceledRec = dsl().select()
                .from(GC_SKU_TICKET)
                .where(GC_SKU_TICKET.ID.eq(100L))
                .fetchOne();
        Record successRec = dsl().select()
                .from(GC_SKU_TICKET)
                .where(GC_SKU_TICKET.ID.eq(101L))
                .fetchOne();
        // If you think, that new ticket should be cancelled, you can remove this part
        Record newNotCancelled = dsl().select()
                .from(GC_SKU_TICKET)
                .where(GC_SKU_TICKET.ID.eq(102L))
                .fetchOne();

        assertEquals(canceledRec.get(GC_SKU_TICKET.STATUS), GcSkuTicketStatus.CANCELLED);
        assertEquals(successRec.get(GC_SKU_TICKET.STATUS), GcSkuTicketStatus.SUCCESS);
        assertEquals(newNotCancelled.get(GC_SKU_TICKET.STATUS), GcSkuTicketStatus.SUCCESS);
    }

    public void shouldRemoveOldOrphanGcSkuRaw() {
        long gcRawSkuId = 10L;
        long otherGcRawSkuId = 11L;
        long fileProcessId = 1L;
        long fileDataProcessRequestId = 1L;

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp oldTimestamp = Timestamp.valueOf(LocalDateTime.now().minusMonths(12));

        populateGcRawSkuRefs(fileDataProcessRequestId, fileProcessId);

        // Should be deleted
        dsl().insertInto(GC_RAW_SKU, GC_RAW_SKU.ID, GC_RAW_SKU.FILE_PROCESS_ID,
                        GC_RAW_SKU.CREATE_DATE, GC_RAW_SKU.DATA)
                .values(gcRawSkuId, fileProcessId, oldTimestamp, new RawSku()).execute();
        // Should not be deleted
        dsl().insertInto(GC_RAW_SKU, GC_RAW_SKU.ID, GC_RAW_SKU.FILE_PROCESS_ID,
                        GC_RAW_SKU.CREATE_DATE, GC_RAW_SKU.DATA)
                .values(otherGcRawSkuId, fileProcessId, now, new RawSku()).execute();

        dataCleanerService.cleanOldData();
        assertEquals(1, dsl().selectFrom(GC_RAW_SKU).stream().count());
    }

    @Test
    public void shouldNotRemoveGcRawSkuWithRef() {
        long gcRawSkuId = 10L;
        long fileProcessId = 1L;
        long fileDataProcessRequestId = 1L;
        long xlsDatacampOfferId = 100L;

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp oldTimestamp = Timestamp.valueOf(LocalDateTime.now().minusMonths(12));

        populateGcRawSkuRefs(fileDataProcessRequestId, fileProcessId);

        dsl().insertInto(GC_RAW_SKU, GC_RAW_SKU.ID, GC_RAW_SKU.FILE_PROCESS_ID,
                        GC_RAW_SKU.CREATE_DATE, GC_RAW_SKU.DATA)
                .values(gcRawSkuId, fileProcessId, oldTimestamp, new RawSku()).execute();

        dsl().insertInto(XLS_DATACAMP_OFFER, XLS_DATACAMP_OFFER.ID, XLS_DATACAMP_OFFER.GC_RAW_SKU_ID,
                        XLS_DATACAMP_OFFER.DATACAMP_OFFER, XLS_DATACAMP_OFFER.STATUS, XLS_DATACAMP_OFFER.UPDATE_DATE,
                        XLS_DATACAMP_OFFER.CREATE_DATE)
                .values(xlsDatacampOfferId, gcRawSkuId, DataCampOffer.Offer.newBuilder().build(),
                        XlsLogbrokerStatus.SUCCESS, now, now);

        assertEquals(1, dsl().selectFrom(GC_RAW_SKU).stream().count());
    }

    private void populateGcRawSkuRefs(long fileDataProcessRequestId, long fileProcessId) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        dsl().insertInto(FILE_DATA_PROCESS_REQUEST, FILE_DATA_PROCESS_REQUEST.ID,
                        FILE_DATA_PROCESS_REQUEST.SOURCE_ID, FILE_DATA_PROCESS_REQUEST.FILE_TYPE,
                        FILE_DATA_PROCESS_REQUEST.URL, FILE_DATA_PROCESS_REQUEST.CREATE_TIME)
                .values(fileDataProcessRequestId, 1, FileType.ONE_CATEGORY_SIMPLE_EXCEL, "/", now)
                .execute();

        dsl().insertInto(ABSTRACT_PROCESS, ABSTRACT_PROCESS.ID, ABSTRACT_PROCESS.REQUEST_ID,
                        ABSTRACT_PROCESS.PROCESS_TYPE)
                .values(fileProcessId, 1L, ProcessType.BETTER_FILE_PROCESS)
                .execute();

        dsl().insertInto(FILE_PROCESS, FILE_PROCESS.ID, FILE_PROCESS.FILE_DATA_PROCESS_REQUEST_ID,
                        FILE_PROCESS.CREATE_TIME, FILE_PROCESS.UPDATE_TIME, FILE_PROCESS.PROCESS_STATE)
                .values(fileProcessId, 1L, now, now, FileProcessState.FINISHED)
                .execute();
    }

    private LockInfo genLockInfo(Long id,LockStatus lockStatus,Timestamp timestamp)
    {
        LockInfo lockInfo = new LockInfo();
        lockInfo.setId(id);
        lockInfo.setStatus(lockStatus);
        lockInfo.setCreateTime(timestamp);
        lockInfo.setUpdateTime(timestamp);
        return lockInfo;
    }
}
