package ru.yandex.market.partner.content.common.db.dao;

import io.qameta.allure.Issue;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import ru.yandex.market.ir.http.PartnerContent;
import ru.yandex.market.partner.content.common.BaseDbCommonTest;
import ru.yandex.market.partner.content.common.db.dao.dcp.DatacampOfferDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.DataBucketState;
import ru.yandex.market.partner.content.common.db.jooq.enums.DatacampOfferStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileProcessState;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.LockStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.MrgrienPipelineStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineActionType;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;
import ru.yandex.market.partner.content.common.db.jooq.enums.TaskStatus;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Pipeline;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.PipelineActionRequest;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Task;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.WaitUntilFinishedPipeline;
import ru.yandex.market.partner.content.common.engine.parameter.EmptyData;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.engine.parameter.RequestProcessFileData;
import ru.yandex.market.partner.content.common.engine.pipeline.PipelineData;
import ru.yandex.market.partner.content.common.service.UnauthorizedUserException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.yandex.market.partner.content.common.db.jooq.Tables.ACTIVE_DATA_BUCKET;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.DATACAMP_OFFER;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.GC_SKU_TICKET;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.LOCK_INFO;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.PIPELINE_ACTION_REQUEST;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.TASK;

@Issue("MARKETIR-9687")
@Issue("MARKETIR-9852")
public class StopPipelineDaoTest extends BaseDbCommonTest {
    private static final int SOURCE_ID = 4323927;
    private static final int PARTNER_SHOP_ID = 111;
    private static final long CATEGORY_ID = 456789;
    private static final long USER_ID = 437677L;
    private static final String USER_NAME = "User 12345";

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private ActiveDataBucketDao activeDataBucketDao;

    @Autowired
    private StopPipelineDao stopPipelineDao;

    @Autowired
    private DatacampOfferDao datacampOfferDao;

    @Autowired
    private WaitUntilFinishedPipelineDao waitUntilFinishedPipelineDao;

    @Test(expected = UnauthorizedUserException.class)
    public void stopPipelineForFileProcessFailsWithoutUser() throws UnauthorizedUserException {
        stopPipelineDao.stopPipelineForFileProcess(0L, null, null);
    }

    @Test(expected = UnauthorizedUserException.class)
    public void invalidateProcessFailsWithoutUser() throws UnauthorizedUserException {
        stopPipelineDao.invalidateProcess(0L, null, null);
    }

    @Test(expected = UnauthorizedUserException.class)
    public void resetPipelineFailsWithoutUser() throws UnauthorizedUserException {
        stopPipelineDao.resetPipeline(0L, 0L, null, null);
    }

    @Test
    public void stopPipelineForFileProcessWithSingleProcess() throws UnauthorizedUserException {
        createSource(SOURCE_ID, PARTNER_SHOP_ID);
        Long fileDataProcessRequest = createFileDataProcessRequest(SOURCE_ID);

        Long fileProcessId = createFileProcessId(fileDataProcessRequest);
        fileProcessDao.updateFileProcess(fileProcessId, FileProcessState.BUCKETS_PROCESSING);

        Long pipelineId = pipelineService.createPipeline(
            new RequestProcessFileData(fileDataProcessRequest),
            PipelineType.GOOD_CONTENT_SINGLE_XLS,
            1
        );
        Long taskId = createTask(pipelineId, TaskStatus.RUNNING);

        // ------

        stopPipelineDao.stopPipelineForFileProcess(fileProcessId, USER_ID, USER_NAME);

        // ------

        Assert.assertEquals(MrgrienPipelineStatus.PAUSED, pipelineService.getPipeline(pipelineId).getStatus());
        Assert.assertEquals(TaskStatus.CANCELLED, getTask(taskId).getStatus());

        Assert.assertNull(getActiveDataBucket());

        Assert.assertEquals(
            FileProcessState.BUCKETS_PROCESSING, fileProcessDao.fetchOneById(fileProcessId).getProcessState()
        );

        PipelineActionRequest actionRequest = getActionRequest(pipelineId);
        Assert.assertEquals(actionRequest.getActionType(), PipelineActionType.STOP_PIPELINE);
        Assert.assertEquals(actionRequest.getUserId().longValue(), USER_ID);
        Assert.assertEquals(actionRequest.getUserName(), USER_NAME);
        Assert.assertNotNull(actionRequest.getScheduleDate());
        Assert.assertNotNull(actionRequest.getExecutedDate());

        WaitUntilFinishedPipeline waitEntityForCurrentPipeline =
            waitUntilFinishedPipelineDao.fetchOneByPipelineId(pipelineId);
        Assert.assertNotNull(waitEntityForCurrentPipeline);
    }

    @Test
    public void stopPipelineForFileProcessWithOldProcess() throws UnauthorizedUserException {
        createSource(SOURCE_ID, PARTNER_SHOP_ID);
        Long fileDataProcessRequest = createFileDataProcessRequest(SOURCE_ID);

        Long finishedFileProcessId = createFileProcessId(fileDataProcessRequest);
        fileProcessDao.updateFileProcess(finishedFileProcessId, FileProcessState.INVALID);

        Long activeFileProcessId = createFileProcessId(fileDataProcessRequest);
        fileProcessDao.updateFileProcess(activeFileProcessId, FileProcessState.BUCKETS_PROCESSING);

        Long dataBucketId = dataBucketDao.getOrCreateDataBucket(CATEGORY_ID, activeFileProcessId, SOURCE_ID,
                Timestamp.from(Instant.now()));
        dataBucketDao.updateDataBucketState(dataBucketId, DataBucketState.INTERNAL_MODEL_CREATED);
        activeDataBucketDao.markDataBucketAsActive(dataBucketId, CATEGORY_ID, SOURCE_ID);

        Long pipelineId = pipelineService.createPipeline(
            new RequestProcessFileData(fileDataProcessRequest),
            PipelineType.GOOD_CONTENT_SINGLE_XLS,
            1
        );
        Long taskId = createTask(pipelineId, TaskStatus.RUNNING);

        // ------

        stopPipelineDao.stopPipelineForFileProcess(activeFileProcessId, USER_ID, USER_NAME);

        // ------

        Assert.assertEquals(MrgrienPipelineStatus.PAUSED, pipelineService.getPipeline(pipelineId).getStatus());
        Assert.assertEquals(TaskStatus.CANCELLED, getTask(taskId).getStatus());

        Assert.assertEquals(
            FileProcessState.INVALID, fileProcessDao.fetchOneById(finishedFileProcessId).getProcessState()
        );
        Assert.assertEquals(
            FileProcessState.BUCKETS_PROCESSING, fileProcessDao.fetchOneById(activeFileProcessId).getProcessState()
        );

        PipelineActionRequest actionRequest = getActionRequest(pipelineId);
        Assert.assertEquals(actionRequest.getActionType(), PipelineActionType.STOP_PIPELINE);
        Assert.assertEquals(actionRequest.getUserId().longValue(), USER_ID);
        Assert.assertEquals(actionRequest.getUserName(), USER_NAME);
        Assert.assertNotNull(actionRequest.getScheduleDate());
        Assert.assertNotNull(actionRequest.getExecutedDate());

        WaitUntilFinishedPipeline waitEntityForCurrentPipeline =
            waitUntilFinishedPipelineDao.fetchOneByPipelineId(pipelineId);
        Assert.assertNotNull(waitEntityForCurrentPipeline);
    }

    @Test
    public void stopPipelineForFileProcessWithSeveralTasks() throws UnauthorizedUserException {
        createSource(SOURCE_ID, PARTNER_SHOP_ID);
        Long fileDataProcessRequest = createFileDataProcessRequest(SOURCE_ID);

        Long fileProcessId = createFileProcessId(fileDataProcessRequest);
        fileProcessDao.updateFileProcess(fileProcessId, FileProcessState.BUCKETS_PROCESSING);

        Long dataBucketId = dataBucketDao.getOrCreateDataBucket(CATEGORY_ID, fileProcessId, SOURCE_ID, Timestamp.from(Instant.now()));
        dataBucketDao.updateDataBucketState(dataBucketId, DataBucketState.INTERNAL_MODEL_CREATED);
        activeDataBucketDao.markDataBucketAsActive(dataBucketId, CATEGORY_ID, SOURCE_ID);

        Long pipelineId = pipelineService.createPipeline(
            new RequestProcessFileData(fileDataProcessRequest),
            PipelineType.GOOD_CONTENT_SINGLE_XLS,
            1
        );
        Long taskIdFinished = createTask(pipelineId, TaskStatus.FINISHED);
        Long taskIdActive1 = createTask(pipelineId, TaskStatus.RUNNING);
        Long taskIdActive2 = createTask(pipelineId, TaskStatus.RUNNING);

        // ------

        stopPipelineDao.stopPipelineForFileProcess(fileProcessId, USER_ID, USER_NAME);

        // ------

        Assert.assertEquals(MrgrienPipelineStatus.PAUSED, pipelineService.getPipeline(pipelineId).getStatus());
        Assert.assertEquals(TaskStatus.FINISHED, getTask(taskIdFinished).getStatus());
        Assert.assertEquals(TaskStatus.CANCELLED, getTask(taskIdActive1).getStatus());
        Assert.assertEquals(TaskStatus.CANCELLED, getTask(taskIdActive2).getStatus());

        Assert.assertEquals(
            FileProcessState.BUCKETS_PROCESSING, fileProcessDao.fetchOneById(fileProcessId).getProcessState()
        );

        PipelineActionRequest actionRequest = getActionRequest(pipelineId);
        Assert.assertEquals(actionRequest.getActionType(), PipelineActionType.STOP_PIPELINE);
        Assert.assertEquals(actionRequest.getUserId().longValue(), USER_ID);
        Assert.assertEquals(actionRequest.getUserName(), USER_NAME);
        Assert.assertNotNull(actionRequest.getScheduleDate());
        Assert.assertNotNull(actionRequest.getExecutedDate());

        WaitUntilFinishedPipeline waitEntityForCurrentPipeline =
            waitUntilFinishedPipelineDao.fetchOneByPipelineId(pipelineId);
        Assert.assertNotNull(waitEntityForCurrentPipeline);
    }

    @Test
    public void resetPipeline() throws UnauthorizedUserException {

        createSource(SOURCE_ID, PARTNER_SHOP_ID);
        Long fileDataProcessRequest = createFileDataProcessRequest(SOURCE_ID);

        Long fileProcessId = createFileProcessId(fileDataProcessRequest);
        fileProcessDao.updateFileProcess(fileProcessId, FileProcessState.BUCKETS_PROCESSING);

        Long pipelineId = pipelineService.createPipeline(
            new RequestProcessFileData(fileDataProcessRequest),
            PipelineType.GOOD_CONTENT_SINGLE_XLS,
            1
        );

        pipelineService.updatePipelineIfModifiable(pipelineId, MrgrienPipelineStatus.PAUSED, new PipelineData());

        // ------

        stopPipelineDao.resetPipeline(pipelineId, fileProcessId, USER_ID, USER_NAME);

        // ------

        Pipeline pipeline = pipelineService.getPipeline(pipelineId);
        Assert.assertEquals(MrgrienPipelineStatus.NEW, pipeline.getStatus());
        Assert.assertNull(pipeline.getStateData());

        PipelineActionRequest actionRequest = getActionRequest(pipelineId);
        Assert.assertEquals(actionRequest.getActionType(), PipelineActionType.RESET_PIPELINE);
        Assert.assertEquals(actionRequest.getUserId().longValue(), USER_ID);
        Assert.assertEquals(actionRequest.getUserName(), USER_NAME);
        Assert.assertNotNull(actionRequest.getScheduleDate());
        Assert.assertNotNull(actionRequest.getExecutedDate());
    }

    @Test
    public void stopDcpPipeline() throws UnauthorizedUserException {
        createSource(SOURCE_ID, PARTNER_SHOP_ID);

        long dataBucketId = createDataBucketId(CATEGORY_ID, SOURCE_ID);
        long pipelineId = pipelineService.createPipeline(
                new ProcessDataBucketData(dataBucketId),
                PipelineType.DATA_CAMP,
                2, false
        );
        Long taskIdFinished = createTask(pipelineId, TaskStatus.FINISHED);
        Long taskIdActive1 = createTask(pipelineId, TaskStatus.RUNNING);
        Long taskIdActive2 = createTask(pipelineId, TaskStatus.RUNNING);

        List<Long> dataCampOfferIds = createDataCampOffersInDB(
                buildDataCampOffers(Arrays.asList("t1", "t2", "t3"), true),
                Timestamp.valueOf("2021-04-15 12:00:00")
        );
        createGcSkuTicketsInDB(dataBucketId, dataCampOfferIds);
        createDataCampOffersInDB(
                buildDataCampOffers(Arrays.asList("t1"), true),
                Timestamp.valueOf("2021-04-15 12:01:00")
        );
        createDataCampOffersInDB(
                buildDataCampOffers(Arrays.asList("t2"), false),
                Timestamp.valueOf("2021-04-15 12:01:00")
        );

        PartnerContent.StopDcpPipelineResponse.Builder responseBuilder =
                PartnerContent.StopDcpPipelineResponse.newBuilder();

        // Останавливаем работающий пайп. Не-завершенные таски должны стать CANCELLED.
        // В ответе проверяем
        // - общее кол-во оферов (3) и
        // - кол-во оферов, которые надо бы перекопировать для рестарта пайпа (2)
        // (для t1 есть dc-офер в статусе ACTIVATED, для t2 есть dc-офер в статусе NEW и t3 - надо копировать)
        stopPipelineDao.stopDcpPipeline(USER_ID, USER_NAME, pipelineId, false, responseBuilder);

        Assert.assertEquals(MrgrienPipelineStatus.CANCELLED, pipelineService.getPipeline(pipelineId).getStatus());
        Assert.assertEquals(TaskStatus.FINISHED, getTask(taskIdFinished).getStatus());
        Assert.assertEquals(TaskStatus.CANCELLED, getTask(taskIdActive1).getStatus());
        Assert.assertEquals(TaskStatus.CANCELLED, getTask(taskIdActive2).getStatus());

        Assert.assertEquals(responseBuilder.getStatus(), PartnerContent.Status.OK);
        Assert.assertEquals(responseBuilder.getOffersInPipeline(), 3);
        Assert.assertEquals(responseBuilder.getOffersHaveNoFreshVersion(), 1);

        responseBuilder.clear();

        // Проверяем, что можно остановить уже остановленный пайп.
        stopPipelineDao.stopDcpPipeline(USER_ID, USER_NAME, pipelineId, false, responseBuilder);
        Assert.assertEquals(MrgrienPipelineStatus.CANCELLED, pipelineService.getPipeline(pipelineId).getStatus());
        Assert.assertEquals(TaskStatus.FINISHED, getTask(taskIdFinished).getStatus());
        Assert.assertEquals(TaskStatus.CANCELLED, getTask(taskIdActive1).getStatus());
        Assert.assertEquals(TaskStatus.CANCELLED, getTask(taskIdActive2).getStatus());

        Assert.assertEquals(responseBuilder.getStatus(), PartnerContent.Status.OK);
        Assert.assertEquals(responseBuilder.getOffersInPipeline(), 3);
        Assert.assertEquals(responseBuilder.getOffersHaveNoFreshVersion(), 1);

        responseBuilder.clear();

        // Проверяем, что можно остановить уже остановленный пайп и перекопировать оферы,
        // у которых нет более свежей версии, для рестарта исходного пайпа
        stopPipelineDao.stopDcpPipeline(USER_ID, USER_NAME, pipelineId, true, responseBuilder);

        Assert.assertEquals(responseBuilder.getStatus(), PartnerContent.Status.OK);
        Assert.assertEquals(responseBuilder.getOffersInPipeline(), 3);
        Assert.assertEquals(responseBuilder.getOffersHaveNoFreshVersion(), 1);

        responseBuilder.clear();

        // Проверяем, что можно остановить уже остановленный пайп и
        // т.к. в предыдущем запуске мы уже скопировали необходимые оферы оферы -
        // сейчас уже у всех оферов есть более свежая версия
        stopPipelineDao.stopDcpPipeline(USER_ID, USER_NAME, pipelineId, false, responseBuilder);

        Assert.assertEquals(responseBuilder.getStatus(), PartnerContent.Status.OK);
        Assert.assertEquals(responseBuilder.getOffersInPipeline(), 3);
        Assert.assertEquals(responseBuilder.getOffersHaveNoFreshVersion(), 0);
    }

    @Test
    public void invalidateProcessForFinish() throws UnauthorizedUserException {
        createSource(SOURCE_ID, PARTNER_SHOP_ID);
        Long fileDataProcessRequest = createFileDataProcessRequest(SOURCE_ID);

        Long finishedFileProcessId = createFileProcessId(fileDataProcessRequest);
        fileProcessDao.updateFileProcess(finishedFileProcessId, FileProcessState.INVALID);

        Long activeFileProcessId = createFileProcessId(fileDataProcessRequest);
        fileProcessDao.updateFileProcess(activeFileProcessId, FileProcessState.BUCKETS_PROCESSING);

        Long pipelineId = pipelineService.createPipeline(
            new RequestProcessFileData(fileDataProcessRequest),
            PipelineType.GOOD_CONTENT_SINGLE_XLS,
            1
        );
        pipelineService.updatePipelineIfModifiable(pipelineId, MrgrienPipelineStatus.PAUSED);
        waitUntilFinishedPipelineDao.addPipeline(pipelineId, fileDataProcessRequest);

        // ------

        stopPipelineDao.invalidateProcess(activeFileProcessId, USER_ID, USER_NAME);

        // ------

        Assert.assertEquals(
            FileProcessState.INVALID, fileProcessDao.fetchOneById(finishedFileProcessId).getProcessState()
        );
        Assert.assertEquals(
            FileProcessState.INVALID, fileProcessDao.fetchOneById(activeFileProcessId).getProcessState()
        );

        Assert.assertEquals(MrgrienPipelineStatus.CANCELLED, pipelineService.getPipeline(pipelineId).getStatus());

        List<WaitUntilFinishedPipeline> waitEntities = waitUntilFinishedPipelineDao.fetchByPipelineId(pipelineId);
        Assert.assertTrue(waitEntities.isEmpty());

        PipelineActionRequest actionRequest = getActionRequest(pipelineId);
        Assert.assertEquals(actionRequest.getActionType(), PipelineActionType.INVALIDATE_FILE_PROCESS);
        Assert.assertEquals(actionRequest.getUserId().longValue(), USER_ID);
        Assert.assertEquals(actionRequest.getUserName(), USER_NAME);
        Assert.assertNotNull(actionRequest.getScheduleDate());
        Assert.assertNotNull(actionRequest.getExecutedDate());
    }

    @Test
    public void invalidateProcessAfterRestart() throws UnauthorizedUserException {
        createSource(SOURCE_ID, PARTNER_SHOP_ID);
        Long fileDataProcessRequest = createFileDataProcessRequest(SOURCE_ID);

        Long currentFileProcessId = createFileProcessId(fileDataProcessRequest);
        fileProcessDao.updateFileProcess(currentFileProcessId, FileProcessState.BUCKETS_PROCESSING);

        Long newFileProcessId = createFileProcessId(fileDataProcessRequest);
        fileProcessDao.updateFileProcess(newFileProcessId, FileProcessState.BUCKETS_PROCESSING);

        Long pipelineId = pipelineService.createPipeline(
            new RequestProcessFileData(fileDataProcessRequest),
            PipelineType.GOOD_CONTENT_SINGLE_XLS,
            1
        );
        pipelineService.updatePipelineIfModifiable(pipelineId, MrgrienPipelineStatus.RUNNING);

        // ------

        stopPipelineDao.invalidateProcess(currentFileProcessId, USER_ID, USER_NAME);

        // ------

        Assert.assertEquals(
            FileProcessState.INVALID, fileProcessDao.fetchOneById(currentFileProcessId).getProcessState()
        );
        Assert.assertEquals(
            FileProcessState.BUCKETS_PROCESSING, fileProcessDao.fetchOneById(newFileProcessId).getProcessState()
        );

        Assert.assertEquals(MrgrienPipelineStatus.RUNNING, pipelineService.getPipeline(pipelineId).getStatus());

        PipelineActionRequest actionRequest = getActionRequest(pipelineId);
        Assert.assertEquals(actionRequest.getActionType(), PipelineActionType.INVALIDATE_FILE_PROCESS);
        Assert.assertEquals(actionRequest.getUserId().longValue(), USER_ID);
        Assert.assertEquals(actionRequest.getUserName(), USER_NAME);
        Assert.assertNotNull(actionRequest.getScheduleDate());
        Assert.assertNotNull(actionRequest.getExecutedDate());
    }

    @Test
    public void invalidateProcessProcessWithActiveDataBucket() throws UnauthorizedUserException {
        createSource(SOURCE_ID, PARTNER_SHOP_ID);
        Long fileDataProcessRequest = createFileDataProcessRequest(SOURCE_ID);

        Long fileProcessId = createFileProcessId(fileDataProcessRequest);
        fileProcessDao.updateFileProcess(fileProcessId, FileProcessState.BUCKETS_PROCESSING);

        Long dataBucketId = dataBucketDao.getOrCreateDataBucket(CATEGORY_ID, fileProcessId, SOURCE_ID, Timestamp.from(Instant.now()));
        dataBucketDao.updateDataBucketState(dataBucketId, DataBucketState.INTERNAL_MODEL_CREATED);
        activeDataBucketDao.markDataBucketAsActive(dataBucketId, CATEGORY_ID, SOURCE_ID);

        Long pipelineId = pipelineService.createPipeline(
            new RequestProcessFileData(fileDataProcessRequest),
            PipelineType.GOOD_CONTENT_SINGLE_XLS,
            1
        );
        pipelineService.updatePipelineIfModifiable(pipelineId, MrgrienPipelineStatus.PAUSED);

        // ------

        stopPipelineDao.invalidateProcess(fileProcessId, USER_ID, USER_NAME);

        // ------

        Assert.assertEquals(
            FileProcessState.INVALID, fileProcessDao.fetchOneById(fileProcessId).getProcessState()
        );

        Assert.assertEquals(
            DataBucketState.INVALID, dataBucketDao.fetchOneById(dataBucketId).getState()
        );
        Assert.assertNull(getActiveDataBucket());

        Assert.assertEquals(MrgrienPipelineStatus.CANCELLED, pipelineService.getPipeline(pipelineId).getStatus());

        PipelineActionRequest actionRequest = getActionRequest(pipelineId);
        Assert.assertEquals(actionRequest.getActionType(), PipelineActionType.INVALIDATE_FILE_PROCESS);
        Assert.assertEquals(actionRequest.getUserId().longValue(), USER_ID);
        Assert.assertEquals(actionRequest.getUserName(), USER_NAME);
        Assert.assertNotNull(actionRequest.getScheduleDate());
        Assert.assertNotNull(actionRequest.getExecutedDate());
    }

    @Test
    public void invalidateProcessWithWaitingDataBucket() throws UnauthorizedUserException {
        createSource(SOURCE_ID, PARTNER_SHOP_ID);
        Long fileDataProcessRequest = createFileDataProcessRequest(SOURCE_ID);

        Long fileProcessId = createFileProcessId(fileDataProcessRequest);
        fileProcessDao.updateFileProcess(fileProcessId, FileProcessState.BUCKETS_PROCESSING);

        Long dataBucketId = dataBucketDao.getOrCreateDataBucket(CATEGORY_ID, fileProcessId, SOURCE_ID,
                Timestamp.from(Instant.now()));
        dataBucketDao.updateDataBucketState(dataBucketId, DataBucketState.WAITING);

        Long pipelineId = pipelineService.createPipeline(
            new RequestProcessFileData(fileDataProcessRequest),
            PipelineType.GOOD_CONTENT_SINGLE_XLS,
            1
        );
        pipelineService.updatePipelineIfModifiable(pipelineId, MrgrienPipelineStatus.PAUSED);

        // ------

        stopPipelineDao.invalidateProcess(fileProcessId, USER_ID, USER_NAME);

        // ------

        Assert.assertEquals(
            FileProcessState.INVALID, fileProcessDao.fetchOneById(fileProcessId).getProcessState()
        );

        Assert.assertEquals(
            DataBucketState.INVALID, dataBucketDao.fetchOneById(dataBucketId).getState()
        );
        Assert.assertNull(getActiveDataBucket());

        Assert.assertEquals(MrgrienPipelineStatus.CANCELLED, pipelineService.getPipeline(pipelineId).getStatus());

        PipelineActionRequest actionRequest = getActionRequest(pipelineId);
        Assert.assertEquals(actionRequest.getActionType(), PipelineActionType.INVALIDATE_FILE_PROCESS);
        Assert.assertEquals(actionRequest.getUserId().longValue(), USER_ID);
        Assert.assertEquals(actionRequest.getUserName(), USER_NAME);
        Assert.assertNotNull(actionRequest.getScheduleDate());
        Assert.assertNotNull(actionRequest.getExecutedDate());
    }


    @Test
    public void splitDcpPipeline() throws UnauthorizedUserException {
        createSource(SOURCE_ID, PARTNER_SHOP_ID);

        long dataBucketId = createDataBucketId(CATEGORY_ID, SOURCE_ID);
        long pipelineId = pipelineService.createPipeline(
            new ProcessDataBucketData(dataBucketId),
            PipelineType.DATA_CAMP,
            2, false
        );
        Long taskIdFinished = createTask(pipelineId, TaskStatus.FINISHED);
        Long taskIdActive1 = createTask(pipelineId, TaskStatus.RUNNING);
        Long taskIdActive2 = createTask(pipelineId, TaskStatus.RUNNING);

        String leftOfferWithoutFreshId = "t1";
        String leftOfferWithFreshId = "t2";
        String leftOfferWithGroupId = "wg1";
        String shoudleftOfferWithGroupId = "wg2";
        String dontleftOfferWithoutFresh = "t3";
        String dontleftOfferWithFresh = "t4";


        List<Long> dataCampOfferIds = createDataCampOffersInDB(
            buildDataCampOffers(Arrays.asList(leftOfferWithoutFreshId, leftOfferWithFreshId,
                dontleftOfferWithoutFresh, dontleftOfferWithFresh), true),
            Timestamp.valueOf("2021-04-15 12:00:00")
        );
        createGcSkuTicketsInDB(dataBucketId, dataCampOfferIds);

        List<Long> dataCampOfferWithGroupIds = createDataCampOffersInDB(
            buildDataCampOffers(Arrays.asList(leftOfferWithGroupId, shoudleftOfferWithGroupId), true),
            Timestamp.valueOf("2021-04-15 12:00:00")
        );
        createGcSkuTicketsInDBWithGroup(dataBucketId, dataCampOfferWithGroupIds, 1);

        createDataCampOffersInDB(
            buildDataCampOffers(Arrays.asList(dontleftOfferWithFresh), false),
            Timestamp.valueOf("2021-04-15 12:01:00")
        );

        List<GcSkuTicket> ticketsBeforeSplit = gcSkuTicketDao.findAll();
        Map<Long, String> datacampOffersBeforeSplit = datacampOfferDao.fetchById(
            ticketsBeforeSplit.stream().map(GcSkuTicket::getDatacampOfferId).toArray(Long[]::new)
        ).stream().collect(Collectors.toMap(DatacampOffer::getId, DatacampOffer::getOfferId));
        Map<String, GcSkuTicket> offerIdToTicketBeforeSplit = ticketsBeforeSplit
            .stream().collect(Collectors
                .toMap(gcSkuTicket -> datacampOffersBeforeSplit.get(gcSkuTicket.getDatacampOfferId()), Function.identity()));


        PartnerContent.SplitDcpPipelineResponse.Builder responseBuilder =
            PartnerContent.SplitDcpPipelineResponse.newBuilder();

        // Разделяем работающий пайплайн
        // В проверяем
        // - что групповой офер остался целиком группой
        // - проверяем что обычные оферы остались
        // - проверяем для уезжающих копируется если нет свежей
        // - проверяем для уезжающих что не копируется если есть свежее

        stopPipelineDao.splitDcpPipeline(USER_ID, USER_NAME, pipelineId,
            Stream.of(leftOfferWithoutFreshId, leftOfferWithFreshId, leftOfferWithGroupId)
                .map(offerIdToTicketBeforeSplit::get)
                .map(GcSkuTicket::getId)
                .collect(Collectors.toSet()), responseBuilder);

        List<GcSkuTicket> ticketsAfterSplit = gcSkuTicketDao.findAll();
        Map<String, GcSkuTicket> offerIdToTicketAfterSplit = ticketsAfterSplit
            .stream().collect(Collectors
                .toMap(gcSkuTicket -> datacampOffersBeforeSplit.get(gcSkuTicket.getDatacampOfferId()), Function.identity()));

        // проверяем ответ
        Assert.assertEquals(responseBuilder.getStatus(), PartnerContent.Status.OK);
        Assert.assertEquals(responseBuilder.getOffersLeftInPipeline(), 4);
        Assert.assertEquals(responseBuilder.getOffersSendToRestart(), 2);
        Assert.assertEquals(responseBuilder.getOffersHaveFreshVersion(), 1);
        Assert.assertEquals(responseBuilder.getOffersHaveNoFreshVersion(), 1);

        // проверяем, что оферы остались
        Assert.assertEquals(offerIdToTicketAfterSplit.get(leftOfferWithoutFreshId).getDataBucketId().longValue(), dataBucketId);
        Assert.assertEquals(offerIdToTicketAfterSplit.get(leftOfferWithFreshId).getDataBucketId().longValue(), dataBucketId);
        Assert.assertEquals(offerIdToTicketAfterSplit.get(leftOfferWithGroupId).getDataBucketId().longValue(), dataBucketId);
        Assert.assertEquals(offerIdToTicketAfterSplit.get(shoudleftOfferWithGroupId).getDataBucketId().longValue(), dataBucketId);

        // проверяем, что оферы отменились и ушли в рестарт
        Assert.assertNotEquals(offerIdToTicketAfterSplit.get(dontleftOfferWithoutFresh).getDataBucketId().longValue(), dataBucketId);
        Assert.assertNotEquals(offerIdToTicketAfterSplit.get(dontleftOfferWithFresh).getDataBucketId().longValue(), dataBucketId);

        // проверяем, что в пайпе стало 4 офера
        Assert.assertEquals(pipelineService.getPipeline(pipelineId).getTicketsCount().intValue(), 4);
    }

    private void createGcSkuTicketsInDB(long dataBucketId, List<Long> dataCampOfferIds) {
        dataCampOfferIds.forEach(offerId ->
                dsl().insertInto(GC_SKU_TICKET,
                        GC_SKU_TICKET.DATA_BUCKET_ID,
                        GC_SKU_TICKET.DATACAMP_OFFER_ID,
                        GC_SKU_TICKET.STATUS,
                        GC_SKU_TICKET.SOURCE_ID,
                        GC_SKU_TICKET.CATEGORY_ID,
                        GC_SKU_TICKET.CREATE_DATE,
                        GC_SKU_TICKET.UPDATE_DATE)
                        .values(dataBucketId, offerId, GcSkuTicketStatus.RESULT_UPLOAD_STARTED, SOURCE_ID,
                                CATEGORY_ID, Timestamp.from(Instant.now()), Timestamp.from(Instant.now()))
                .execute()
        );

    }

    private void createGcSkuTicketsInDBWithGroup(long dataBucketId, List<Long> dataCampOfferIds, Integer groupId) {
        dataCampOfferIds.forEach(offerId ->
            dsl().insertInto(GC_SKU_TICKET,
                    GC_SKU_TICKET.DATA_BUCKET_ID,
                    GC_SKU_TICKET.DATACAMP_OFFER_ID,
                    GC_SKU_TICKET.STATUS,
                    GC_SKU_TICKET.SOURCE_ID,
                    GC_SKU_TICKET.CATEGORY_ID,
                    GC_SKU_TICKET.CREATE_DATE,
                    GC_SKU_TICKET.UPDATE_DATE,
                    GC_SKU_TICKET.DCP_GROUP_ID)
                .values(dataBucketId, offerId, GcSkuTicketStatus.RESULT_UPLOAD_STARTED, SOURCE_ID,
                    CATEGORY_ID, Timestamp.from(Instant.now()), Timestamp.from(Instant.now()), groupId)
                .execute()
        );

    }

    public List<DatacampOffer> buildDataCampOffers(List<String> shopSkus, boolean activated) {
        List<DatacampOffer> result = new ArrayList<>();
        for (String s : shopSkus) {
            DatacampOffer pojo = new DatacampOffer();
            pojo.setOfferId(s);
            pojo.setBusinessId(PARTNER_SHOP_ID);
            pojo.setStatus(activated ? DatacampOfferStatus.ACTIVATED : DatacampOfferStatus.NEW);

            result.add(pojo);
        }
        return result;
    }

    private List<Long> createDataCampOffersInDB(List<DatacampOffer> offers, Timestamp ts) {
        List<Long> offerIds = new ArrayList<>();
        offers.forEach(offer -> {
            List<Long> ids = dsl().insertInto(DATACAMP_OFFER)
                    .set(DATACAMP_OFFER.OFFER_ID, offer.getOfferId())
                    .set(DATACAMP_OFFER.BUSINESS_ID, offer.getBusinessId())
                    .set(DATACAMP_OFFER.SOURCE_ID, SOURCE_ID)
                    .set(DATACAMP_OFFER.CREATE_TIME, ts)
                    .set(DATACAMP_OFFER.REQUEST_TS, ts)
                    .set(DATACAMP_OFFER.STATUS, offer.getStatus())
                    .returning(DATACAMP_OFFER.ID)
                    .fetch().getValues(DATACAMP_OFFER.ID);
            offerIds.addAll(ids);
        });
        ;
        return offerIds;
    }

    public Long createTask(Long pipelineId, TaskStatus status) {
        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        Long lockId = createLock();

        return dsl()
            .insertInto(TASK)
            .set(TASK.PIPELINE_ID, pipelineId)
            .set(TASK.STATUS, status)
            .set(TASK.START_DATE, currentTimestamp)
            .set(TASK.UPDATE_DATE, currentTimestamp)
            .set(TASK.LOCK_ID, lockId)
            .set(TASK.INPUT_DATA, new EmptyData())
            .returning(TASK.ID)
            .fetchOne()
            .getId();
    }

    private Long createLock() {
        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        return dsl()
            .insertInto(LOCK_INFO)
            .set(LOCK_INFO.CREATE_TIME, currentTimestamp)
            .set(LOCK_INFO.UPDATE_TIME, currentTimestamp)
            .set(LOCK_INFO.STATUS, LockStatus.FREE)
            .returning(LOCK_INFO.ID)
            .fetchOne()
            .getId();
    }

    private Task getTask(Long taskId) {
        return dsl().select()
            .from(TASK)
            .where(TASK.ID.eq(taskId))
            .fetchOneInto(Task.class);
    }

    private PipelineActionRequest getActionRequest(Long pipelineId) {
        return dsl().select()
            .from(PIPELINE_ACTION_REQUEST)
            .where(PIPELINE_ACTION_REQUEST.PIPELINE_ID.eq(pipelineId))
            .fetchOneInto(PipelineActionRequest.class);
    }

    private Long getActiveDataBucket() {
        List<Long> bucketIds = dsl().select(ACTIVE_DATA_BUCKET.DATA_BUCKET_ID)
            .from(ACTIVE_DATA_BUCKET)
            .where(ACTIVE_DATA_BUCKET.CATEGORY_ID.eq(CATEGORY_ID))
            .and(ACTIVE_DATA_BUCKET.SOURCE_ID.eq(SOURCE_ID))
            .fetchInto(Long.class);

        if (CollectionUtils.isEmpty(bucketIds)) {
            return null;
        }
        return bucketIds.get(0);
    }
}