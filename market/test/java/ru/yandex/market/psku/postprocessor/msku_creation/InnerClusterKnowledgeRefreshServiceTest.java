package ru.yandex.market.psku.postprocessor.msku_creation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.qameta.allure.Issue;
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
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ProcessingResult;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuInClusterState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuStorageState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.Cluster;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuInClusterInfo;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuResultStorage;
import ru.yandex.market.psku.postprocessor.config.CommonTestConfig;
import ru.yandex.market.psku.postprocessor.config.MskuCreationConfig;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.PskuInClusterInfo.PSKU_IN_CLUSTER_INFO;

@ContextConfiguration(classes = {
    CommonTestConfig.class,
    MskuCreationConfig.class,
})
public class InnerClusterKnowledgeRefreshServiceTest extends BaseDBTest {

    private static final int CLUSTERS_IN_YT_MOCK = 4;
    private ClusterPriorityService clusterPriorityService;

    private PskuClusterDao pskuClusterDao = new MockPskuClusterDao();

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

    @Mock
    CategorySizeMeasureService categorySizeMeasureService;
    @Mock
    CategoryFormDownloader categoryFormDownloader;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        Mockito.when(categorySizeMeasureService.getSizeMeasuresInfo(Mockito.any()))
            .thenReturn(MboSizeMeasures.GetSizeMeasuresInfoResponse.newBuilder().build());

        Mockito.when(categoryFormDownloader.downloadForm(anyLong())).thenReturn(new byte[10]);

        GenerationCategoryChecker generationCategoryChecker =
            new GenerationCategoryChecker(categorySizeMeasureService, categoryFormDownloader);

        CategoryPriorityComparator comparator = new CategoryPriorityComparator(ImmutableSet.of());
        clusterPriorityService = new ClusterPriorityService(clusterDao, pskuResultStorageDao, pskuInClusterInfoDao,
            pskuKnowledgeDao, generationCategoryChecker, brokenClusterProcessor, pskuClusterDao,
            comparator);
    }

    @Issue("MARKETIR-9507")
    @Test
    public void whenRefreshShouldDeleteOldClustersAndPskus() {
        int totalPskus = pskuClusterDao.getLastClusterizationSession().stream()
            .map(SimplePskuCluster::getPskuIds)
            .mapToInt(Collection::size)
            .sum();
        clusterPriorityService.refresh();
        List<Cluster> allClusters = clusterDao.findAll();
        List<PskuInClusterInfo> allPskus = pskuInClusterInfoDao.findAll();
        assertThat(allClusters).hasSize(CLUSTERS_IN_YT_MOCK);
        assertThat(allPskus).hasSize(totalPskus);

        clusterPriorityService.refresh();
        allClusters = clusterDao.findAll();
        assertThat(allClusters).hasSize(CLUSTERS_IN_YT_MOCK);
        assertThat(allPskus).hasSize(totalPskus);
    }

    @Issue("MARKETIR-9507")
    @Test
    public void whenPskuInProcessShouldNotDeleteAndClusterize() {
        List<Long> idsInProcess = pskuClusterDao.getLastClusterizationSession().stream()
            .flatMap(cluster -> cluster.getPskuIds().stream())
            .filter(id -> id % 2 == 0)
            .collect(Collectors.toList());
        int totalPskus = pskuClusterDao.getLastClusterizationSession().stream()
            .map(SimplePskuCluster::getPskuIds)
            .mapToInt(Collection::size)
            .sum();

        clusterPriorityService.refresh();
        List<Long> idsFromTable = pskuInClusterInfoDao.dsl().update(PSKU_IN_CLUSTER_INFO)
            .set(PSKU_IN_CLUSTER_INFO.STATE, PskuInClusterState.IN_PROCESS)
            .where(PSKU_IN_CLUSTER_INFO.PSKU_ID.in(idsInProcess))
            .returningResult(PSKU_IN_CLUSTER_INFO.ID)
            .fetch()
            .into(Long.class);

        clusterPriorityService.refresh();

        List<PskuInClusterInfo> allPskus = pskuInClusterInfoDao.findAll();
        assertThat(allPskus)
            .filteredOn(psku -> psku.getState() == PskuInClusterState.IN_PROCESS)
            .extracting(PskuInClusterInfo::getId)
            .containsExactlyElementsOf(idsFromTable);
        assertThat(allPskus)
            .filteredOn(psku -> psku.getState() != PskuInClusterState.IN_PROCESS)
            .extracting(PskuInClusterInfo::getId)
            .allMatch(id -> id > totalPskus);
    }

    @Issue("MARKETIR-9507")
    @Test
    public void whenAllPskuFromClusterAreInProcessShouldNotDuplicateThem() {
        List<SimplePskuCluster> lastClusterizationSession = pskuClusterDao.getLastClusterizationSession();
        int totalPskus = lastClusterizationSession.stream()
            .map(SimplePskuCluster::getPskuIds)
            .mapToInt(Collection::size)
            .sum();

        clusterPriorityService.refresh();
        pskuInClusterInfoDao.dsl().update(PSKU_IN_CLUSTER_INFO)
            .set(PSKU_IN_CLUSTER_INFO.STATE, PskuInClusterState.IN_PROCESS)
            .execute();

        clusterPriorityService.refresh();

        List<PskuInClusterInfo> allPskus = pskuInClusterInfoDao.findAll();
        assertThat(allPskus)
            .filteredOn(psku -> psku.getState() == PskuInClusterState.IN_PROCESS)
            .hasSize(totalPskus);
        assertThat(clusterDao.findAll())
            .hasSize(lastClusterizationSession.size());
    }

    @Issue("MARKETIR-9507")
    @Test
    public void whenClusterHavePskusWithAnswerInStorageShouldNotProcessThem() {
        List<SimplePskuCluster> lastClusterizationSession = pskuClusterDao.getLastClusterizationSession();
        int totalPskus = lastClusterizationSession.stream()
            .map(SimplePskuCluster::getPskuIds)
            .mapToInt(Collection::size)
            .sum();
        clusterPriorityService.refresh();

        Timestamp time = new Timestamp(System.currentTimeMillis());

        int maxIdWithAnswer = 10;
        pskuResultStorageDao.insert(lastClusterizationSession.stream()
            .map(SimplePskuCluster::getPskuIds)
            .flatMap(Set::stream)
            .filter(pSkuId -> pSkuId <= maxIdWithAnswer)
            .map(pSkuId -> {
                PskuResultStorage pskuResultStorage = new PskuResultStorage() ;
                pskuResultStorage.setPskuId(pSkuId);
                pskuResultStorage.setCategoryId(10L);
                pskuResultStorage.setClusterizerProcessingResult(ProcessingResult.NEED_INFO);
                pskuResultStorage.setCreateTime(time);
                pskuResultStorage.setState(PskuStorageState.NEED_INFO);
                return pskuResultStorage;
            })
            .collect(Collectors.toList())
        );

        clusterPriorityService.refresh();

        List<PskuInClusterInfo> allPskus = pskuInClusterInfoDao.findAll();
        List<Cluster> allClusters = clusterDao.findAll();
        assertThat(allPskus)
            .hasSize(totalPskus - maxIdWithAnswer);

        Long[] actualClusters = allPskus.stream()
            .map(PskuInClusterInfo::getClusterId).distinct().toArray(Long[]::new);

        assertThat(allClusters)
            .extracting(Cluster::getId)
            .containsExactlyInAnyOrder(actualClusters);
    }

    @Issue("MARKETIR-9507")
    @Test
    public void shouldNotTakeInPriorityListPskusWithAnswers() {
        List<SimplePskuCluster> lastClusterizationSession = pskuClusterDao.getLastClusterizationSession();

        Timestamp time = new Timestamp(System.currentTimeMillis());
        clusterPriorityService.refresh();
        int maxIdWithAnswer = 7;
        pskuResultStorageDao.insert(lastClusterizationSession.stream()
            .map(SimplePskuCluster::getPskuIds)
            .flatMap(Set::stream)
            .filter(pSkuId -> pSkuId <= maxIdWithAnswer)
            .map(pSkuId -> {
                PskuResultStorage pskuResultStorage = new PskuResultStorage();
                pskuResultStorage.setPskuId(pSkuId);
                pskuResultStorage.setCategoryId(10L);
                pskuResultStorage.setClusterizerProcessingResult(ProcessingResult.NEED_INFO);
                pskuResultStorage.setCreateTime(time);
                pskuResultStorage.setState(PskuStorageState.NEED_INFO);
                return pskuResultStorage;
                })
            .collect(Collectors.toList())
        );
        List<PrioritizedCluster> clusterPriorityList = clusterPriorityService.getClusterPriorityList(100);
        List<PskuInClusterInfo> allPskus = pskuInClusterInfoDao.findAll();
        assertThat(clusterPriorityList)
            .extracting(PrioritizedCluster::getSize)
            .allMatch(size -> size >= 2);
        assertThat(allPskus)
            .filteredOn(psku -> psku.getState() == PskuInClusterState.EXPIRED)
            .extracting(PskuInClusterInfo::getPskuId)
            .containsExactlyInAnyOrder(pskuResultStorageDao.getAlreadyProcessedPskuIds().toArray(new Long[0]));

        List<Long> pskuIdsInClusterPriorityList = clusterPriorityList.stream()
            .map(PrioritizedCluster::getPskus)
            .flatMap(List::stream)
            .map(PskuInClusterInfo::getPskuId)
            .collect(Collectors.toList());

        assertThat(pskuIdsInClusterPriorityList)
            .isSubsetOf(allPskus.stream()
                .filter(psku -> psku.getState() == PskuInClusterState.NEW)
                .map(PskuInClusterInfo::getPskuId)
                .collect(Collectors.toList())
            );
    }

    private static class MockPskuClusterDao implements PskuClusterDao {

        @Override
        public List<SimplePskuCluster> getLastClusterizationSession() {
            return ImmutableList.of(
                new SimplePskuCluster(ImmutableList.of(1L, 2L, 3L, 4L, 5L), 10L),
                new SimplePskuCluster(ImmutableList.of(6L, 7L, 8L, 9L, 10L, 11L, 12L), 10L),
                new SimplePskuCluster(ImmutableList.of(13L, 14L, 15L), 10L),
                new SimplePskuCluster(ImmutableList.of(16L), 10L)
            );
        }
    }

}
