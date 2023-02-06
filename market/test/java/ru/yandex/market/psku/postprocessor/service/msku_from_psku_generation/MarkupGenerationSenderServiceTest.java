package ru.yandex.market.psku.postprocessor.service.msku_from_psku_generation;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.mutable.MutableLong;
import org.assertj.core.api.Assertions;
import org.jooq.Configuration;
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
import ru.yandex.market.mbo.export.CategorySizeMeasureService;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuInClusterInfoDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuKnowledgeDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuResultStorageDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.GenerationTaskType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuInClusterState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuInClusterInfo;
import ru.yandex.market.psku.postprocessor.config.CommonTestConfig;
import ru.yandex.market.psku.postprocessor.config.MskuCreationConfig;
import ru.yandex.market.psku.postprocessor.config.TrackerTestConfig;
import ru.yandex.market.psku.postprocessor.msku_creation.BrokenClusterProcessor;
import ru.yandex.market.psku.postprocessor.msku_creation.CategoryFormDownloader;
import ru.yandex.market.psku.postprocessor.msku_creation.CategoryPriorityComparator;
import ru.yandex.market.psku.postprocessor.msku_creation.ClusterPriorityService;
import ru.yandex.market.psku.postprocessor.msku_creation.GenerationCategoryChecker;
import ru.yandex.market.psku.postprocessor.msku_creation.PrioritizedCluster;
import ru.yandex.market.psku.postprocessor.msku_creation.PskuClusterDao;
import ru.yandex.market.psku.postprocessor.service.logging.HealthLogManager;
import ru.yandex.market.psku.postprocessor.service.markup.MskuFromPskuGenerationTask;
import ru.yandex.market.psku.postprocessor.service.tracker.PskuTrackerService;
import ru.yandex.market.psku.postprocessor.service.tracker.mock.CategoryTrackerInfoProducerMock;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.PSKU_KNOWLEDGE;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.Cluster.CLUSTER;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.PskuInClusterInfo.PSKU_IN_CLUSTER_INFO;

@ContextConfiguration(classes = {
    CommonTestConfig.class,
    MskuCreationConfig.class,
    TrackerTestConfig.class
})
public class MarkupGenerationSenderServiceTest extends BaseDBTest {

    private static final int CONFIG_ID = 123;
    private static final Long CATEGORY_ID = 12354L;
    private static final String TICKET_KEY = "TICKET_KEY";
    private static final String CATEGORY_NAME = "category name";
    private static final Long SUPPLIER_ID = 121L;
    private MutableLong currentConfigId = new MutableLong(CONFIG_ID);
    private MarkupGenerationSenderService senderService;
    private ClusterPriorityService clusterPriorityService;
    private GenerationCategoryChecker generationCategoryChecker;

    @Autowired
    CategoryTrackerInfoProducerMock categoryTrackerInfoProducerMock;
    @Autowired
    PskuInClusterInfoDao pskuInClusterInfoDao;
    @Autowired
    PskuKnowledgeDao pskuKnowledgeDao;
    @Autowired
    ClusterDao clusterDao;
    @Autowired
    HealthLogManager healthLogManager;
    @Autowired
    PskuResultStorageDao pskuResultStorageDao;
    @Autowired
    Configuration configuration;
    @Autowired
    BrokenClusterProcessor brokenClusterProcessor;
    @Autowired
    PskuClusterDao pskuClusterDao;
    @Autowired
    CategorySizeMeasureService categorySizeMeasureService;

    @Mock
    MarkupService markupServiceClient;
    @Mock
    PskuTrackerService pskuTrackerService;
    @Mock
    CategoryFormDownloader categoryFormDownloader;

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

        Mockito.when(categoryFormDownloader.downloadForm(Mockito.anyLong())).thenReturn(new byte[10]);

        Mockito.when(pskuTrackerService.createGenerationTicket(Mockito.any(), Mockito.anyCollection(), Mockito.any()))
            .thenReturn(TICKET_KEY);
        categoryTrackerInfoProducerMock.addCategoryInfo(CATEGORY_ID, CATEGORY_NAME);

        generationCategoryChecker = new GenerationCategoryChecker(categorySizeMeasureService, categoryFormDownloader);
        CategoryPriorityComparator categoryPriorityComparator = new CategoryPriorityComparator(ImmutableSet.of());
        clusterPriorityService = new ClusterPriorityService(clusterDao,
            pskuResultStorageDao,
            pskuInClusterInfoDao,
            pskuKnowledgeDao,
            generationCategoryChecker,
            brokenClusterProcessor,
            pskuClusterDao,
            categoryPriorityComparator);
    }

    @Test
    public void whenHaveSpaceForTasksShouldSendAll() {
        initWithMaxTaskCount(10);
        List<PrioritizedCluster> clusters = mockClusterPriorityListAnswer(5, PskuInClusterState.NEW);
        senderService.send();
        Mockito.verify(markupServiceClient, Mockito.times(clusters.size())).upsertTaskConfig(Mockito.any());
        Assertions.assertThat(pskuInClusterInfoDao.findAll())
            .extracting(PskuInClusterInfo::getState)
            .containsOnly(PskuInClusterState.IN_PROCESS);
        Mockito.verify(pskuTrackerService, Mockito.times(clusters.size()))
            .createGenerationTicket(Mockito.any(), Mockito.anyCollection(), Mockito.any());
    }

    @Test
    public void whenZeroMaxTasksShouldSendNothing() {
        initWithMaxTaskCount(0);
        mockClusterPriorityListAnswer(5, PskuInClusterState.NEW);
        senderService.send();
        Mockito.verify(markupServiceClient, Mockito.never()).upsertTaskConfig(Mockito.any());
        Assertions.assertThat(pskuInClusterInfoDao.findAll())
            .extracting(PskuInClusterInfo::getState)
            .containsOnly(PskuInClusterState.NEW);
        Mockito.verify(pskuTrackerService, Mockito.never())
            .createGenerationTicket(Mockito.any(), Mockito.anyCollection(), Mockito.any());
    }

    @Test
    public void whenHaveClustersInProcessShouldSendOnlyDelta() {
        int tasksInYangQueueMaxSize = 10;
        initWithMaxTaskCount(tasksInYangQueueMaxSize);
        mockClusterPriorityListAnswer(7, PskuInClusterState.NEW);
        List<PrioritizedCluster> inProcessClusters = mockClusterPriorityListAnswer(6, PskuInClusterState.IN_PROCESS);

        senderService.send();
        Mockito.verify(markupServiceClient, Mockito.times(tasksInYangQueueMaxSize - inProcessClusters.size()))
            .upsertTaskConfig(Mockito.any());

        Assertions.assertThat(pskuInClusterInfoDao.getActiveConfigIds())
            .hasSize(tasksInYangQueueMaxSize);
        Mockito.verify(pskuTrackerService, Mockito.times(tasksInYangQueueMaxSize - inProcessClusters.size()))
            .createGenerationTicket(Mockito.any(), Mockito.anyCollection(), Mockito.any());
    }

    private void initWithMaxTaskCount(int tasksInYangQueueMaxSize) {
        MskuFromPskuGenerationTask mskuFromPskuGenerationTask = new MskuFromPskuGenerationTask(markupServiceClient, pskuTrackerService,
            categoryTrackerInfoProducerMock, pskuKnowledgeDao);
        PskuGenerationProxyDao pskuGenerationProxyDao =
            new ClusterProxyDao(clusterPriorityService, pskuInClusterInfoDao, clusterDao);
        senderService = new MarkupGenerationSenderService(tasksInYangQueueMaxSize, mskuFromPskuGenerationTask,
            healthLogManager, pskuGenerationProxyDao, GenerationTaskType.CLUSTER);
    }

    private List<PrioritizedCluster> mockClusterPriorityListAnswer(int count, PskuInClusterState state) {
        return LongStream.range(0, count)
            .map(clusterId -> clusterId + state.ordinal() * 100)
            .mapToObj(clusterId -> new PrioritizedCluster(clusterId, createPskuInClusterInfoList(clusterId, state), 1))
            .collect(Collectors.toList());
    }

    private List<PskuInClusterInfo> createPskuInClusterInfoList(long clusterId, PskuInClusterState state) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        dsl()
            .insertInto(CLUSTER)
            .set(CLUSTER.ID, clusterId)
            .set(CLUSTER.CATEGORY_ID, CATEGORY_ID)
            .set(CLUSTER.CREATE_TIME, timestamp)
            .execute();
        List<Long> createdIds = JooqUtils.batchInsert(
            dsl(),
            PSKU_IN_CLUSTER_INFO,
            PSKU_IN_CLUSTER_INFO.ID,
            Arrays.asList(1L + clusterId * 100, 2L + clusterId * 100, 3L + clusterId * 100),
            (table, id) -> table
                .set(PSKU_IN_CLUSTER_INFO.PSKU_ID, id)
                .set(PSKU_IN_CLUSTER_INFO.STATE, state)
                .set(PSKU_IN_CLUSTER_INFO.CLUSTER_ID, clusterId)
                .set(PSKU_IN_CLUSTER_INFO.MARKUP_CONFIG_ID, state == PskuInClusterState.NEW ?
                    null : Math.toIntExact(clusterId))
                .set(PSKU_IN_CLUSTER_INFO.CATEGORY_ID, CATEGORY_ID)
        );
        JooqUtils.batchInsert(
            dsl(),
            PSKU_KNOWLEDGE,
            Arrays.asList(1L + clusterId * 100, 2L + clusterId * 100, 3L + clusterId * 100),
            (table, id) -> table
                .set(PSKU_KNOWLEDGE.ID, id)
                .set(PSKU_KNOWLEDGE.CREATION_TS, timestamp)
                .set(PSKU_KNOWLEDGE.LAST_UPDATE_TS, timestamp)
                .set(PSKU_KNOWLEDGE.SUPPLIER_ID, SUPPLIER_ID)
                .set(PSKU_KNOWLEDGE.PSKU_TITLE, "title" + id)
                .set(PSKU_KNOWLEDGE.SHOP_SKU, "shopSku" + id)
        );
        return pskuInClusterInfoDao.fetchById(createdIds.toArray(new Long[0]));
    }

}
