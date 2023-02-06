package ru.yandex.market.psku.postprocessor.service.classification;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.ir.autogeneration.common.util.JooqUtils;
import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuInClassificationDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuKnowledgeDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuResultStorageDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuResultStorageHistoryDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.SimplePsku;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuStorageState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuInClassification;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuKnowledge;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuResultStorage;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuResultStorageHistory;
import ru.yandex.market.psku.postprocessor.config.CommonTestConfig;
import ru.yandex.market.psku.postprocessor.config.MskuCreationConfig;
import ru.yandex.market.psku.postprocessor.config.TrackerTestConfig;
import ru.yandex.market.psku.postprocessor.service.enrich.SkuEnricher;
import ru.yandex.market.psku.postprocessor.service.markup.PskuClassificationTask;
import ru.yandex.market.psku.postprocessor.service.tracker.PskuTrackerService;
import ru.yandex.market.psku.postprocessor.service.tracker.models.CategoryTrackerInfoProducer;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.PSKU_RESULT_STORAGE;


@ContextConfiguration(classes = {
    CommonTestConfig.class,
    MskuCreationConfig.class,
    TrackerTestConfig.class
})
public class MarkupClassificationReceiverServiceTest extends BaseDBTest {

    private static final long PSKU_ID_NO_RESULT_1 = 101L;
    private static final long PSKU_ID_NO_RESULT_2 = 102L;
    private static final long PSKU_ID_APPROVED_CATEGORY_1 = 301L;
    private static final long PSKU_ID_APPROVED_CATEGORY_2 = 302L;
    private static final long PSKU_ID_BAD_RESULT = 666L;
    private static final long PSKU_ID_EMPTY_RESULT = 777L;

    private static final int CONFIG_ID_WITH_NO_RESULT = 200;
    private static final int CONFIG_ID_WITH_RESULT = 201;
    private static final int CONFIG_ID_WITH_BAD_RESULT = 202;
    private static final int CONFIG_ID_WITH_EMPTY_RESULT = 203;

    private static final long CATEGORY_ID = 12345L;
    private static final int APPROVED_CATEGORY_1 = 10001;
    private static final int APPROVED_CATEGORY_2 = 10002;
    private static final Map<Long, Integer> APPROVED_CATEGORY_BY_PSKU = ImmutableMap.of(
        PSKU_ID_APPROVED_CATEGORY_1, APPROVED_CATEGORY_1,
        PSKU_ID_APPROVED_CATEGORY_2, APPROVED_CATEGORY_2
    );

    MarkupClassificationReceiverService service;
    SkuEnricher skuEnricher;

    @Autowired
    PskuResultStorageDao pskuResultStorageDao;
    @Autowired
    PskuResultStorageHistoryDao pskuResultStorageHistoryDao;
    @Autowired
    PskuInClassificationDao pskuInClassificationDao;
    @Autowired
    PskuKnowledgeDao pskuKnowledgeDao;
    @Autowired
    CategoryTrackerInfoProducer categoryTrackerInfoProducer;

    @Mock
    PskuClassificationTask pskuClassificationTask;
    @Mock
    PskuTrackerService pskuTrackerService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Mockito.when(pskuClassificationTask.isTaskFinished(CONFIG_ID_WITH_NO_RESULT))
            .thenReturn(false);
        Mockito.when(pskuClassificationTask.isTaskFinished(CONFIG_ID_WITH_RESULT))
            .thenReturn(true);
        Mockito.when(pskuClassificationTask.isTaskFinished(CONFIG_ID_WITH_BAD_RESULT))
            .thenReturn(true);
        Mockito.when(pskuClassificationTask.isTaskFinished(CONFIG_ID_WITH_EMPTY_RESULT))
            .thenReturn(true);
        Mockito.when(pskuClassificationTask.getPskuClassificationTaskResult(CONFIG_ID_WITH_RESULT))
            .thenReturn(mockClassificationTaskResult());
        Mockito.when(pskuClassificationTask.getPskuClassificationTaskResult(CONFIG_ID_WITH_BAD_RESULT))
            .thenReturn(mockClassificationBadTaskResult());
        Mockito.when(pskuClassificationTask.getPskuClassificationTaskResult(CONFIG_ID_WITH_EMPTY_RESULT))
            .thenReturn(mockClassificationEmptyTaskResult());

        skuEnricher = new SkuEnricher(pskuKnowledgeDao, null);
        service = new MarkupClassificationReceiverService(
            pskuResultStorageHistoryDao, pskuClassificationTask, pskuInClassificationDao,
            pskuTrackerService, skuEnricher, categoryTrackerInfoProducer);
    }

    @Test
    public void whenNoResultsShouldSaveNothing() {
        initState(CONFIG_ID_WITH_NO_RESULT, PSKU_ID_NO_RESULT_1, PSKU_ID_NO_RESULT_2);

        service.getAndStoreResultsFromMarkup();

        assertThat(pskuInClassificationDao.findAll())
            .flatExtracting(PskuInClassification::getReceiveTs, PskuInClassification::getApprovedCategoryId)
            .containsOnlyNulls();
        assertThat(pskuResultStorageDao.findAll())
            .extracting(PskuResultStorage::getState)
            .containsOnly(PskuStorageState.WRONG_CATEGORY);
        assertThat(pskuKnowledgeDao.findAll())
            .extracting(PskuKnowledge::getApprovedCategoryId)
            .containsOnlyNulls();
        Mockito.verify(pskuTrackerService, Mockito.never())
            .closeClassificationTicket(Mockito.any(), Mockito.any());
    }

    @Test
    public void whenHaveResultsWithApprovedCategoryShouldSaveCorrectly() {
        initState(CONFIG_ID_WITH_RESULT, PSKU_ID_APPROVED_CATEGORY_1, PSKU_ID_APPROVED_CATEGORY_2);

        service.getAndStoreResultsFromMarkup();

        List<PskuInClassification> all = pskuInClassificationDao.findAll();
        assertThat(all)
            .flatExtracting(PskuInClassification::getReceiveTs)
            .doesNotContainNull();
        assertThat(all.stream()
            .collect(Collectors.toMap(
                PskuInClassification::getPskuId,
                pskuInClassification -> Math.toIntExact(pskuInClassification.getApprovedCategoryId()
                )))).containsAllEntriesOf(APPROVED_CATEGORY_BY_PSKU);
        assertThat(pskuResultStorageDao.findAll())
            .hasSize(0);
        assertThat(pskuResultStorageHistoryDao.findAll())
            .extracting(PskuResultStorageHistory::getPskuId)
            .containsExactlyInAnyOrder(PSKU_ID_APPROVED_CATEGORY_1, PSKU_ID_APPROVED_CATEGORY_2);
        assertThat(pskuKnowledgeDao.findAll())
            .extracting(PskuKnowledge::getId, PskuKnowledge::getApprovedCategoryId)
            .containsExactlyInAnyOrder(
                Tuple.tuple(PSKU_ID_APPROVED_CATEGORY_1, (long) APPROVED_CATEGORY_1),
                Tuple.tuple(PSKU_ID_APPROVED_CATEGORY_2, (long) APPROVED_CATEGORY_2));
    }

    @Test
    public void whenHaveBadResultShouldSaveInClassificationAndDoNotRotateStorage() {
        initState(CONFIG_ID_WITH_BAD_RESULT, PSKU_ID_APPROVED_CATEGORY_1, PSKU_ID_BAD_RESULT);
        PskuResultStorage badInitial = pskuResultStorageDao.fetchOneByPskuId(PSKU_ID_BAD_RESULT);

        service.getAndStoreResultsFromMarkup();

        PskuResultStorage good = pskuResultStorageDao.fetchOneByPskuId(PSKU_ID_APPROVED_CATEGORY_1);
        PskuResultStorage bad = pskuResultStorageDao.fetchOneByPskuId(PSKU_ID_BAD_RESULT);

        assertThat(good).isNull();
        assertThat(bad).isEqualToComparingFieldByField(badInitial);
        PskuInClassification classificationBad = pskuInClassificationDao.fetchOneByPskuId(PSKU_ID_BAD_RESULT);
        assertThat(classificationBad)
            .extracting(PskuInClassification::getReceiveTs, PskuInClassification::getMarkupRawResult)
            .doesNotContainNull();
        assertThat(classificationBad.getApprovedCategoryId()).isNull();
    }

    @Test
    public void whenHaveEmptyResultShouldDeleteToResend() {
        initState(CONFIG_ID_WITH_RESULT, PSKU_ID_APPROVED_CATEGORY_1, PSKU_ID_APPROVED_CATEGORY_2);
        initState(CONFIG_ID_WITH_EMPTY_RESULT, PSKU_ID_BAD_RESULT, PSKU_ID_EMPTY_RESULT);

        service.getAndStoreResultsFromMarkup();

        PskuInClassification classificationGood1 = pskuInClassificationDao.fetchOneByPskuId(PSKU_ID_APPROVED_CATEGORY_1);
        PskuInClassification classificationGood2 = pskuInClassificationDao.fetchOneByPskuId(PSKU_ID_APPROVED_CATEGORY_1);
        PskuInClassification classificationEmpty1 = pskuInClassificationDao.fetchOneByPskuId(PSKU_ID_EMPTY_RESULT);
        PskuInClassification classificationEmpty2 = pskuInClassificationDao.fetchOneByPskuId(PSKU_ID_BAD_RESULT);

        // should not be deleted:
        assertThat(classificationGood1).isNotNull();
        assertThat(classificationGood2).isNotNull();
        // should be deleted, to resend:
        assertThat(classificationEmpty1).isNull();
        assertThat(classificationEmpty2).isNull();
    }

    private Markup.PskuClassificationTaskResponse mockClassificationEmptyTaskResult() {
        return Markup.PskuClassificationTaskResponse.newBuilder()
            .setTaskConfigId(CONFIG_ID_WITH_EMPTY_RESULT)
            .build();
    }

    private Markup.PskuClassificationTaskResponse mockClassificationBadTaskResult() {
        return Markup.PskuClassificationTaskResponse.newBuilder()
            .addTaskResult(Markup.PskuClassificationTaskResult.newBuilder()
                .setPskuId(String.valueOf(PSKU_ID_APPROVED_CATEGORY_1))
                .setFixedCategoryId(APPROVED_CATEGORY_BY_PSKU.get(PSKU_ID_APPROVED_CATEGORY_1))
                .build())
            .addTaskResult(Markup.PskuClassificationTaskResult.newBuilder()
                .setPskuId(String.valueOf(PSKU_ID_BAD_RESULT))
                .addComments(Markup.PskuClassificationTaskResult.Comment.newBuilder()
                    .addItems("плохая psku, плохая")
                    .build())
                .build())
            .setTaskConfigId(CONFIG_ID_WITH_BAD_RESULT)
            .build();
    }

    private Markup.PskuClassificationTaskResponse mockClassificationTaskResult() {
        return Markup.PskuClassificationTaskResponse.newBuilder()
            .addTaskResult(Markup.PskuClassificationTaskResult.newBuilder()
                .setPskuId(String.valueOf(PSKU_ID_APPROVED_CATEGORY_1))
                .setFixedCategoryId(APPROVED_CATEGORY_BY_PSKU.get(PSKU_ID_APPROVED_CATEGORY_1))
                .build())
            .addTaskResult(Markup.PskuClassificationTaskResult.newBuilder()
                .setPskuId(String.valueOf(PSKU_ID_APPROVED_CATEGORY_2))
                .setFixedCategoryId(APPROVED_CATEGORY_BY_PSKU.get(PSKU_ID_APPROVED_CATEGORY_2))
                .build())
            .setTaskConfigId(CONFIG_ID_WITH_RESULT)
            .build();
    }

    private void initState(int configIdWithNoResult, Long... pskuIds) {
        pskuInClassificationDao.markPskusSent(
            Arrays.stream(pskuIds)
                .map(SimplePsku::ofId)
                .collect(Collectors.toList()),
            configIdWithNoResult, "ticket_key"
        );

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        JooqUtils.batchInsert(
            dsl(),
            PSKU_RESULT_STORAGE,
            Arrays.asList(pskuIds),
            (table, id) -> table
                .set(PSKU_RESULT_STORAGE.PSKU_ID, id)
                .set(PSKU_RESULT_STORAGE.CATEGORY_ID, CATEGORY_ID)
                .set(PSKU_RESULT_STORAGE.STATE, PskuStorageState.WRONG_CATEGORY)
                .set(PSKU_RESULT_STORAGE.CREATE_TIME, timestamp)
        );

        pskuKnowledgeDao.upsert(Arrays.stream(pskuIds)
            .map(id -> {
                PskuKnowledge pskuKnowledge = new PskuKnowledge();
                pskuKnowledge.setId(id);
                pskuKnowledge.setPskuTitle("title_" + id);
                pskuKnowledge.setShopSku("shop_sku_" + id);
                pskuKnowledge.setSupplierId(123L);
                return pskuKnowledge;
            }).collect(Collectors.toList())
        );
    }
}
