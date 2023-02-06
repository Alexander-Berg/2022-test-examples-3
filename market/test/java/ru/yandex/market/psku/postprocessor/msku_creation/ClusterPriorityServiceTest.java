package ru.yandex.market.psku.postprocessor.msku_creation;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.mbo.export.CategorySizeMeasureService;
import ru.yandex.market.mbo.export.MboSizeMeasures;
import ru.yandex.market.psku.postprocessor.clusterization.SimplePskuCluster;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuInClusterInfoDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuKnowledgeDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuResultStorageDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.SimplePsku;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuInClusterState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuStorageState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuInClusterInfo;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuKnowledge;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuResultStorage;
import ru.yandex.market.psku.postprocessor.config.CommonTestConfig;
import ru.yandex.market.psku.postprocessor.config.MskuCreationConfig;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.CLUSTER;

@ContextConfiguration(classes = {
    CommonTestConfig.class,
    MskuCreationConfig.class
})
public class ClusterPriorityServiceTest extends BaseDBTest {
    private static final long OK_CATEGORY_ID = 100L;
    private static final long BAD_CATEGORY_ID = 110L;
    private static final long NO_KNOWLEDGE_CATEGORY_ID = 111L;
    private static final long APPROVED_CATEGORY_ID = 200L;
    private long HIGH_PRIORITY_CATEGORY_ID_1 = 1001L;
    private long HIGH_PRIORITY_CATEGORY_ID_2 = 1002L;

    private ClusterPriorityService clusterPriorityService;

    @Autowired
    ClusterDao clusterDao;
    @Autowired
    PskuResultStorageDao pskuResultStorageDao;
    @Autowired
    PskuInClusterInfoDao pskuInClusterInfoDao;
    @Autowired
    PskuKnowledgeDao pskuKnowledgeDao;
    @Autowired
    BrokenClusterProcessor brokenClusterProcessor;
    @Autowired
    PskuClusterDao ytPskuClusterDao;

    @Mock
    CategorySizeMeasureService categorySizeMeasureService;
    @Mock
    CategoryFormDownloader categoryFormDownloader;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        GenerationCategoryChecker generationCategoryChecker = initCategoryDataMocks();
        brokenClusterProcessor = new BrokenClusterProcessor(pskuKnowledgeDao, pskuResultStorageDao);
        CategoryPriorityComparator categoryPriorityComparator = new CategoryPriorityComparator(
            ImmutableSet.of(HIGH_PRIORITY_CATEGORY_ID_1, HIGH_PRIORITY_CATEGORY_ID_2));
        this.clusterPriorityService = new ClusterPriorityService(
            clusterDao,
            pskuResultStorageDao,
            pskuInClusterInfoDao,
            pskuKnowledgeDao,
            generationCategoryChecker,
            brokenClusterProcessor,
            ytPskuClusterDao,
            categoryPriorityComparator);
    }

    private GenerationCategoryChecker initCategoryDataMocks() {
        GenerationCategoryChecker generationCategoryChecker =
            new GenerationCategoryChecker(categorySizeMeasureService, categoryFormDownloader);
        Mockito.when(categorySizeMeasureService.getSizeMeasuresInfo(Mockito.eq(MboSizeMeasures.GetSizeMeasuresInfoRequest.newBuilder()
            .addCategoryIds(OK_CATEGORY_ID)
            .build()))).thenReturn(MboSizeMeasures.GetSizeMeasuresInfoResponse.newBuilder()
            .build());
        Mockito.when(categorySizeMeasureService.getSizeMeasuresInfo(Mockito.eq(MboSizeMeasures.GetSizeMeasuresInfoRequest.newBuilder()
            .addCategoryIds(HIGH_PRIORITY_CATEGORY_ID_1)
            .build()))).thenReturn(MboSizeMeasures.GetSizeMeasuresInfoResponse.newBuilder()
            .build());
        Mockito.when(categorySizeMeasureService.getSizeMeasuresInfo(Mockito.eq(MboSizeMeasures.GetSizeMeasuresInfoRequest.newBuilder()
            .addCategoryIds(HIGH_PRIORITY_CATEGORY_ID_2)
            .build()))).thenReturn(MboSizeMeasures.GetSizeMeasuresInfoResponse.newBuilder()
            .build());
        Mockito.when(categorySizeMeasureService.getSizeMeasuresInfo(Mockito.eq(MboSizeMeasures.GetSizeMeasuresInfoRequest.newBuilder()
            .addCategoryIds(NO_KNOWLEDGE_CATEGORY_ID)
            .build()))).thenReturn(MboSizeMeasures.GetSizeMeasuresInfoResponse.newBuilder()
            .build());
        Mockito.when(categorySizeMeasureService.getSizeMeasuresInfo(Mockito.eq(MboSizeMeasures.GetSizeMeasuresInfoRequest.newBuilder()
            .addCategoryIds(APPROVED_CATEGORY_ID)
            .build()))).thenReturn(MboSizeMeasures.GetSizeMeasuresInfoResponse.newBuilder()
            .build());
        Mockito.when(categorySizeMeasureService.getSizeMeasuresInfo(Mockito.eq(MboSizeMeasures.GetSizeMeasuresInfoRequest.newBuilder()
            .addCategoryIds(BAD_CATEGORY_ID)
            .build()))).thenReturn(MboSizeMeasures.GetSizeMeasuresInfoResponse.newBuilder()
            .addSizeMeasures(MboSizeMeasures.GetSizeMeasuresInfoResponse.CategoryResponse.newBuilder()
                .addSizeMeasureInfos(MboSizeMeasures.SizeMeasureInfo.newBuilder()
                    .setSizeMeasure(MboSizeMeasures.SizeMeasure.newBuilder()
                        .setId(1)
                        .setName("name")
                        .setValueParamId(1L)
                        .setUnitParamId(1L)
                        .setNumericParamId(1L)
                        .build())
                    .build())
                .setCategoryId(BAD_CATEGORY_ID)
                .build())
            .build());

        Mockito.when(categoryFormDownloader.downloadForm(anyLong())).thenReturn(new byte[10]);
        Mockito.when(categoryFormDownloader.downloadForm(NO_KNOWLEDGE_CATEGORY_ID)).thenReturn(new byte[0]);

        return generationCategoryChecker;
    }

    @Test
    public void getClusterPriorityListReturnsCorrectlyPrioritizedClusters() {
        Iterator<Long> seqIt = LongStream.iterate(1, i -> i + 1).iterator();
        long CLUSTER_1_ID = createClusterForPskuIds(true, seqIt, 2, OK_CATEGORY_ID);
        long CLUSTER_2_ID = createClusterForPskuIds(false, seqIt, 4, OK_CATEGORY_ID);
        long CLUSTER_3_ID = createClusterForPskuIds(false, seqIt, 2, OK_CATEGORY_ID);
        long CLUSTER_4_ID = createClusterForPskuIds(false, seqIt, 2, OK_CATEGORY_ID);
        long CLUSTER_5_ID = createClusterForPskuIds(false, seqIt, 3, HIGH_PRIORITY_CATEGORY_ID_1);
        long CLUSTER_6_ID = createClusterForPskuIds(false, seqIt, 2, HIGH_PRIORITY_CATEGORY_ID_2);
        long CLUSTER_7_ID = createClusterForPskuIds(true, seqIt, 3, HIGH_PRIORITY_CATEGORY_ID_2);

        List<PrioritizedCluster> prioritizedClusters = clusterPriorityService.getClusterPriorityList(100);

        assertThat(prioritizedClusters).hasSize(7);
        // Sorted by hid priorities and then, within them, by number of distinct suppliers, cluster size, cluster id
        assertThat(prioritizedClusters).extracting(PrioritizedCluster::getId)
            .containsExactly(CLUSTER_7_ID, CLUSTER_5_ID, CLUSTER_6_ID, CLUSTER_1_ID, CLUSTER_2_ID,
                CLUSTER_4_ID, CLUSTER_3_ID);
    }

    @Test
    public void shouldFilterClustersFromNotOkCategories() {
        Iterator<Long> seqIt = LongStream.iterate(1, i -> i + 1).iterator();
        long CLUSTER_1_ID = createClusterForPskuIds(true, seqIt, 2, OK_CATEGORY_ID);
        long CLUSTER_2_ID = createClusterForPskuIds(true, seqIt, 4, BAD_CATEGORY_ID);

        List<PrioritizedCluster> prioritizedClusters = clusterPriorityService.getClusterPriorityList(100);

        assertThat(prioritizedClusters).extracting(PrioritizedCluster::getId)
            .containsExactly(CLUSTER_1_ID);
    }

    @Test
    public void shouldFilterClustersFromCategoriesWithoutKnowledge() {
        Iterator<Long> seqIt = LongStream.iterate(1, i -> i + 1).iterator();
        long CLUSTER_1_ID = createClusterForPskuIds(true, seqIt, 2, OK_CATEGORY_ID);
        long CLUSTER_2_ID = createClusterForPskuIds(true, seqIt, 4, NO_KNOWLEDGE_CATEGORY_ID);

        List<PrioritizedCluster> prioritizedClusters = clusterPriorityService.getClusterPriorityList(100);

        assertThat(prioritizedClusters).hasSize(1);
        assertThat(prioritizedClusters).extracting(PrioritizedCluster::getId)
            .containsExactly(CLUSTER_1_ID);
    }

    @Test
    public void shouldNotSendClustersWithMoreThenOneApprovedCategory() {
        Iterator<Long> pskuIdIterator = LongStream.iterate(1, i -> i + 1).iterator();
        Iterator<Long> approvedCategoryIterator = new LongNullIterator(1L, true, 100);
        createClusterForPskuIds(true, pskuIdIterator, 3, OK_CATEGORY_ID, approvedCategoryIterator);
        List<PrioritizedCluster> prioritizedClusters = clusterPriorityService.getClusterPriorityList(100);
        assertThat(prioritizedClusters).hasSize(0);
        assertThat(pskuResultStorageDao.findAll())
            .extracting(PskuResultStorage::getState)
            .hasSize(0);
    }

    @Test
    public void ifOneApprovedCategoryShouldOverrideAndSend() {
        int pskuCount = 10;
        Iterator<Long> pskuIdIterator = LongStream.iterate(10, i -> i + 1).iterator();
        assertThat(APPROVED_CATEGORY_ID).isNotEqualTo(OK_CATEGORY_ID);
        Iterator<Long> approvedCategoryIterator = new LongNullIterator(APPROVED_CATEGORY_ID, false, 2);
        createClusterForPskuIds(true, pskuIdIterator, pskuCount, OK_CATEGORY_ID, approvedCategoryIterator);
        List<PrioritizedCluster> prioritizedClusters = clusterPriorityService.getClusterPriorityList(100);
        assertThat(prioritizedClusters)
            .extracting(PrioritizedCluster::getCategoryId)
            .containsOnly(APPROVED_CATEGORY_ID)
            .hasSize(1);
        assertThat(pskuResultStorageDao.findAll())
            .extracting(PskuResultStorage::getState)
            .hasSize(0);
    }

    @Test
    public void ifDifferentApprovedCategoriesShouldInvalidatePskusWithUnknownCategory() {
        int pskuCount = 10;
        int pskuCountWithApprovedCategory = 3;
        Iterator<Long> pskuIdIterator = LongStream.iterate(10, i -> i + 1).iterator();
        long approvedCategoryId = 10L;
        assertThat(approvedCategoryId).isNotEqualTo(OK_CATEGORY_ID);
        Iterator<Long> approvedCategoryIterator =
            new LongNullIterator(approvedCategoryId, true, pskuCountWithApprovedCategory);
        createClusterForPskuIds(true, pskuIdIterator, pskuCount, OK_CATEGORY_ID, approvedCategoryIterator);

        List<PrioritizedCluster> prioritizedClusters = clusterPriorityService.getClusterPriorityList(100);

        assertThat(prioritizedClusters)
            .hasSize(0);
        assertThat(pskuResultStorageDao.findAll())
            .extracting(PskuResultStorage::getState)
            .containsOnly(PskuStorageState.WRONG_CATEGORY)
            .hasSize(pskuCount - pskuCountWithApprovedCategory);
    }

    @Test
    public void shouldRefreshIfOldClusters() {
        ClusterPriorityService clusterPriorityServiceSpy = Mockito.spy(clusterPriorityService);
        clusterDao.createClusters(Collections.singletonList(
            new SimplePskuCluster(Arrays.asList(12L, 23L), 100L)
        ));
        Timestamp old = new Timestamp(System.currentTimeMillis() -
                        TimeUnit.HOURS.toMillis(ClusterPriorityService.HOURS_BEFORE_REFRESH + 1));
        dsl().update(CLUSTER)
            .set(CLUSTER.CREATE_TIME, old)
            .execute();
        clusterPriorityServiceSpy.refreshIfNeeded();
        Mockito.verify(clusterPriorityServiceSpy, Mockito.times(1)).refresh();
    }

    @Test
    public void shouldNotRefreshIfFresh() {
        ClusterPriorityService clusterPriorityServiceSpy = Mockito.spy(clusterPriorityService);
        clusterDao.createClusters(Collections.singletonList(
            new SimplePskuCluster(Arrays.asList(12L, 23L), 100L)
        ));

        clusterPriorityServiceSpy.refreshIfNeeded();
        Mockito.verify(clusterPriorityServiceSpy, Mockito.never()).refresh();
    }

    private long createClusterForPskuIds(boolean distinctSuppliers, Iterator<Long> pskuIdIterator,
                                         int pskuCount, long categoryId, Iterator<Long> approvedCategoryIterator) {
        List<Long> pskuIds = new ArrayList<>();
        for (int i = 0; i < pskuCount; i++) {
            pskuIds.add(pskuIdIterator.next());
        }
        long clusterId = clusterDao.createClusters(Arrays.asList(
            new SimplePskuCluster(pskuIds, 100L)
        )).get(0).getId();
        pskuIds.forEach(pskuId -> {
            long supplierId = distinctSuppliers ? pskuId * 10 : 10L;
            createPskuInCluster(pskuId, supplierId, clusterId, categoryId, approvedCategoryIterator.next());
        });
        return clusterId;

    }

    private static class LongNullIterator implements Iterator<Long> {

        private final Long start;
        private final boolean differentValues;
        private final int nonNullCount;
        private Long currentValue;
        private int currentNumber;

        private LongNullIterator(Long start, boolean differentValues, int nonNullCount) {
            this.start = start;
            this.differentValues = differentValues;
            this.nonNullCount = nonNullCount;
            this.currentValue = start;
            this.currentNumber = 1;
        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public Long next() {
            if (currentNumber > nonNullCount) {
                return null;
            }
            currentNumber++;
            if (differentValues) {
                return ++currentValue;
            } else {
                return start;
            }
        }
    }

    private long createClusterForPskuIds(boolean distinctSuppliers, Iterator<Long> pskuIdIterator,
                                         int pskuCount, long categoryId) {
        Iterator<Long> nullIterator = Stream.iterate((Long) null, i -> i).iterator();
        return createClusterForPskuIds(distinctSuppliers, pskuIdIterator, pskuCount, categoryId, nullIterator);

    }

    private void createPskuInCluster(long pskuId, long supplierId, long clusterId, long categoryId,
                                     Long approvedCategoryId) {
        PskuInClusterInfo p = createSimplePskuInClusterInfo(pskuId, clusterId, categoryId);
        pskuInClusterInfoDao.insert(p);
        pskuKnowledgeDao.upsert(Collections.singletonList(
            new PskuKnowledge(pskuId, "", supplierId, null, null, null, null, null, null, null)));
        if (approvedCategoryId != null) {
            pskuKnowledgeDao.setApprovedCategoryForPsku(new SimplePsku(pskuId), approvedCategoryId);
        }
    }

    private PskuInClusterInfo createSimplePskuInClusterInfo(long pskuId, long clusterId, long categoryId) {
        PskuInClusterInfo pskuInClusterInfo = new PskuInClusterInfo();
        pskuInClusterInfo.setPskuId(pskuId);
        pskuInClusterInfo.setState(PskuInClusterState.NEW);
        pskuInClusterInfo.setClusterId(clusterId);
        pskuInClusterInfo.setCategoryId(categoryId);
        return pskuInClusterInfo;
    }
}
