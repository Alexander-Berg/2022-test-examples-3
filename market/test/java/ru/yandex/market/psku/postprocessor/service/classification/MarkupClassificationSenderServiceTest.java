package ru.yandex.market.psku.postprocessor.service.classification;

import org.apache.commons.lang.mutable.MutableLong;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.ir.autogeneration.common.util.JooqUtils;
import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.ir.http.MarkupService;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuInClassificationDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuInClusterInfoDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuKnowledgeDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuResultStorageDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.SimplePsku;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ProcessingResult;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuStorageState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuInClassification;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuResultStorage;
import ru.yandex.market.psku.postprocessor.config.CommonTestConfig;
import ru.yandex.market.psku.postprocessor.config.MskuCreationConfig;
import ru.yandex.market.psku.postprocessor.config.TrackerTestConfig;
import ru.yandex.market.psku.postprocessor.service.markup.PskuClassificationTask;
import ru.yandex.market.psku.postprocessor.service.tracker.PskuTrackerService;
import ru.yandex.market.psku.postprocessor.service.tracker.mock.CategoryTrackerInfoProducerMock;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.PSKU_KNOWLEDGE;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.PskuResultStorage.PSKU_RESULT_STORAGE;

@ContextConfiguration(classes = {
    CommonTestConfig.class,
    MskuCreationConfig.class,
    TrackerTestConfig.class
})

public class MarkupClassificationSenderServiceTest extends BaseDBTest {
    private static final int CONFIG_ID = 123;
    private static final Long CATEGORY_ID = 12354L;
    private static final String TICKET_KEY = "TICKET_KEY";
    private static final String CATEGORY_NAME = "category name";
    private static final Long SUPPLIER_ID = 121L;
    private MutableLong currentConfigId = new MutableLong(CONFIG_ID);
    private MarkupClassificationSenderService senderService;

    @Autowired
    CategoryTrackerInfoProducerMock categoryTrackerInfoProducerMock;
    @Autowired
    PskuInClusterInfoDao pskuInClusterInfoDao;
    @Autowired
    PskuKnowledgeDao pskuKnowledgeDao;
    @Autowired
    PskuInClassificationDao pskuInClassificationDao;
    @Autowired
    PskuResultStorageDao pskuResultStorageDao;

    @Mock
    MarkupService markupServiceClient;
    @Mock
    PskuTrackerService pskuTrackerService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Mockito.doAnswer(invocation -> {
            currentConfigId.increment();
            return Markup.UpsertTaskConfigResponse.newBuilder()
                .setStatus(Markup.ResponseStatus.newBuilder()
                    .setStatus(Markup.ResponseStatus.Status.OK)
                    .build())
                .setTaskConfig(Markup.TaskConfig.newBuilder()
                    .setConfigId(currentConfigId.intValue())
                    .build())
                .build();
        }).when(markupServiceClient).upsertTaskConfig(Mockito.any());

        Mockito.when(pskuTrackerService.createClassificationTicket(Mockito.anyCollection()))
            .thenReturn(TICKET_KEY);
        categoryTrackerInfoProducerMock.addCategoryInfo(CATEGORY_ID, CATEGORY_NAME);
    }

    @Test
    public void whenHaveSpaceForTasksShouldSendAll() {
        initWithMaxTaskCount(3);
        int pskuCount = 25;
        int taskCount = (int) Math.ceil((double) pskuCount / MarkupClassificationSenderService.BATCH_SIZE);
        createPskuResultStorageRecordsForSend(pskuCount);
        senderService.sendTasks();
        Mockito.verify(markupServiceClient, Mockito.times(taskCount)).upsertTaskConfig(Mockito.any());
        Assertions.assertThat(pskuInClassificationDao.findAll())
            .hasSize(pskuCount)
            .extracting(PskuInClassification::getSendTs)
            .doesNotContainNull();
        Mockito.verify(pskuTrackerService, Mockito.times(taskCount))
            .createClassificationTicket(Mockito.anyCollection());
    }

    @Test
    public void whenZeroMaxTasksShouldSendNothing() {
        initWithMaxTaskCount(0);
        int pskuCount = 25;
        createPskuResultStorageRecordsForSend(pskuCount);

        senderService.sendTasks();

        Mockito.verify(markupServiceClient, Mockito.never()).upsertTaskConfig(Mockito.any());
        Assertions.assertThat(pskuInClassificationDao.findAll())
            .hasSize(0);
        Mockito.verify(pskuTrackerService, Mockito.never())
            .createClassificationTicket(Mockito.anyCollection());
    }

    @Test
    public void shouldNotSendIfPskuAlreadyHasApprovedCategory() {
        initWithMaxTaskCount(5);
        int pskuCount = 40;
        createPskuResultStorageRecordsForSend(pskuCount);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        Set<Long> idsWithAlreadyApprovedCategory = pskuResultStorageDao.findAll().stream()
            .limit(20)
            .map(PskuResultStorage::getPskuId)
            .collect(Collectors.toSet());

        idsWithAlreadyApprovedCategory.stream()
            .map(id -> {
                PskuInClassification psku = new PskuInClassification();
                psku.setPskuId(id);
                psku.setMarkupConfigId(123);
                psku.setReceiveTs(timestamp);
                psku.setSendTs(timestamp);
                psku.setApprovedCategoryId(1234L);
                psku.setTicketKey("ticket_key_" + id);
                return psku;
            }).forEach(pskuInClassificationDao::insert);

        senderService.sendTasks();

        Mockito.verify(markupServiceClient, Mockito.times(2)).upsertTaskConfig(Mockito.any());
        List<PskuInClassification> all = pskuInClassificationDao.findAll();
        Assertions.assertThat(all)
            .hasSize(40)
            .extracting(PskuInClassification::getSendTs)
            .doesNotContainNull();
        Assertions.assertThat(all)
            .filteredOn(psku -> psku.getApprovedCategoryId() == null)
            .extracting(PskuInClassification::getPskuId)
            .doesNotContainAnyElementsOf(idsWithAlreadyApprovedCategory);
        Mockito.verify(pskuTrackerService, Mockito.times(2))
            .createClassificationTicket(Mockito.anyCollection());
    }

    @Test
    public void whenHavePskusInProcessShouldSendOnlyDelta() {
        int tasksInYangQueueMaxSize = 4;
        initWithMaxTaskCount(tasksInYangQueueMaxSize);
        int pskuCount = 35;
        createPskuResultStorageRecordsForSend(pskuCount);
        int inProcessCount = 2;
        createInProcessClassificationTasks(inProcessCount);

        senderService.sendTasks();

        Assertions.assertThat(pskuInClassificationDao.findAll())
            .extracting(PskuInClassification::getSendTs)
            .doesNotContainNull();
        Assertions.assertThat(pskuInClassificationDao.getActiveConfigIds())
            .hasSize(tasksInYangQueueMaxSize);

        Mockito.verify(markupServiceClient, Mockito.times(tasksInYangQueueMaxSize - inProcessCount))
            .upsertTaskConfig(Mockito.any());
        Mockito.verify(pskuTrackerService, Mockito.times(tasksInYangQueueMaxSize - inProcessCount))
            .createClassificationTicket(Mockito.anyCollection());
    }

    private void createInProcessClassificationTasks(int count) {
        for (int i = 0; i < count; i++) {
            pskuInClassificationDao.markPskusSent(
                Collections.singletonList(SimplePsku.ofId(i + 100)),
                i + 1000, "ticket_key_" + i
            );
        }
    }

    private void initWithMaxTaskCount(int tasksInYangQueueMaxSize) {
        PskuClassificationTask pskuClassificationTask = new PskuClassificationTask(markupServiceClient, pskuTrackerService,
            categoryTrackerInfoProducerMock, pskuKnowledgeDao);
        senderService = new MarkupClassificationSenderService(pskuResultStorageDao, tasksInYangQueueMaxSize,
            pskuClassificationTask, pskuInClassificationDao);
    }

    private void createPskuResultStorageRecordsForSend(int count) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        dsl().batch(LongStream.range(10, count + 10)
            .mapToObj(id -> dsl().insertInto(PSKU_RESULT_STORAGE)
                .set(PSKU_RESULT_STORAGE.PSKU_ID, id)
                .set(PSKU_RESULT_STORAGE.CATEGORY_ID, CATEGORY_ID)
                .set(PSKU_RESULT_STORAGE.STATE, PskuStorageState.WRONG_CATEGORY)
                .set(PSKU_RESULT_STORAGE.CREATE_TIME, timestamp)
                .set(PSKU_RESULT_STORAGE.CLUSTERIZER_PROCESSING_RESULT, ProcessingResult.WRONG_CATEGORY)
            ).collect(Collectors.toList())
        ).execute();

        JooqUtils.batchInsert(
            dsl(),
            PSKU_KNOWLEDGE,
            LongStream.range(10, count + 10).boxed().collect(Collectors.toList()),
            (table, id) -> table
                .set(PSKU_KNOWLEDGE.ID, id)
                .set(PSKU_KNOWLEDGE.CREATION_TS, timestamp)
                .set(PSKU_KNOWLEDGE.LAST_UPDATE_TS, timestamp)
                .set(PSKU_KNOWLEDGE.SUPPLIER_ID, SUPPLIER_ID)
                .set(PSKU_KNOWLEDGE.PSKU_TITLE, "title" + id)
                .set(PSKU_KNOWLEDGE.SHOP_SKU, "shopSku" + id)
                .set(PSKU_KNOWLEDGE.MAIN_PICTURE_URL, "picture_" + id)
                .set(PSKU_KNOWLEDGE.VENDOR_NAME, "vendor_" + id)
        );
    }
}