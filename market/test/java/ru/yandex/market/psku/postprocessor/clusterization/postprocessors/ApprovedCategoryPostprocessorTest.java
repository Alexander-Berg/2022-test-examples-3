package ru.yandex.market.psku.postprocessor.clusterization.postprocessors;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.psku.postprocessor.clusterization.pojo.ClusterType;
import ru.yandex.market.psku.postprocessor.clusterization.pojo.ClusterizationData;
import ru.yandex.market.psku.postprocessor.clusterization.pojo.Psku;
import ru.yandex.market.psku.postprocessor.clusterization.pojo.PskuCluster;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuKnowledgeDao;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.PskuKnowledge.PSKU_KNOWLEDGE;


public class ApprovedCategoryPostprocessorTest extends BaseDBTest {

    private static final Long SUPPLIER_ID = 12L;
    private static final Long DEFAULT_CATEGORY = 1111L;
    ApprovedCategoryPostprocessor postprocessor;

    private static final Long PSKU_APPROVED_1 = 1001L;
    private static final Long PSKU_APPROVED_2 = 1002L;
    private static final Long PSKU_APPROVED_3 = 1003L;
    private static final Long PSKU_APPROVED_4 = 1004L;
    private static final Long APPROVED_CATEGORY_1 = 123L;
    private static final Long APPROVED_CATEGORY_2 = 321L;

    private static final Map<Long, Long> APPROVED_PSKUS = ImmutableMap.of(
        PSKU_APPROVED_1, APPROVED_CATEGORY_1,
        PSKU_APPROVED_2, APPROVED_CATEGORY_1,
        PSKU_APPROVED_3, APPROVED_CATEGORY_2,
        PSKU_APPROVED_4, APPROVED_CATEGORY_2
    );

    @Autowired
    PskuKnowledgeDao pskuKnowledgeDao;

    @Before
    public void setUp() throws Exception {
        postprocessor = new ApprovedCategoryPostprocessor(pskuKnowledgeDao);
    }

    private ClusterizationData createClusterizationData(Iterator<Long> pskuIdIterator, int count, int clusterSize) {
        return new ClusterizationData(
            EnumSet.of(ClusterType.BARCODE, ClusterType.VENDOR_CODE),
            createNotEligible(),
            createSingleByCategory(),
            createClusters(pskuIdIterator, count, clusterSize)
        );
    }

    private List<PskuCluster> createClusters(Iterator<Long> pskuIdIterator, int count, int clusterSize) {
        return IntStream.range(0, count).mapToObj(i -> createCluster(pskuIdIterator, clusterSize))
            .collect(Collectors.toList());
    }

    private PskuCluster createCluster(Iterator<Long> pskuIdIterator, int clusterSize) {
        Timestamp time = Timestamp.from(Instant.now());
        List<Psku> pskus = new ArrayList<>();
        for (int i = 0; i < clusterSize; i++) {
            Long pskuId = pskuIdIterator.next();
            Long approvedCategoryId = APPROVED_PSKUS.get(pskuId);
            pskuKnowledgeDao.dsl().insertInto(PSKU_KNOWLEDGE)
                .set(PSKU_KNOWLEDGE.ID, pskuId)
                .set(PSKU_KNOWLEDGE.PSKU_TITLE, "title_" + pskuId)
                .set(PSKU_KNOWLEDGE.SUPPLIER_ID, SUPPLIER_ID)
                .set(PSKU_KNOWLEDGE.CREATION_TS, time)
                .set(PSKU_KNOWLEDGE.SHOP_SKU, "shop_sku_" + pskuId)
                .set(PSKU_KNOWLEDGE.LAST_UPDATE_TS, time)
                .set(PSKU_KNOWLEDGE.APPROVED_CATEGORY_ID, approvedCategoryId)
                .execute();
            pskus.add(new Psku(Optional.ofNullable(approvedCategoryId).orElse(DEFAULT_CATEGORY),
                123, pskuId, "title_" + pskuId, Collections.emptyList(), Collections.emptyList(),
                "shop_sku_" + pskuId, "vendor")
            );
        }
        return new PskuCluster(pskus.get(0), pskus,
            EnumSet.of(ClusterType.BARCODE, ClusterType.VENDOR_CODE), "group_data", DEFAULT_CATEGORY);
    }

    private Map<Long, Set<Long>> createSingleByCategory() {
        return Collections.emptyMap();
    }

    private Map<Long, Set<Long>> createNotEligible() {
        return Collections.emptyMap();
    }

    @Test
    public void whenNoApprovedPskusClustersShouldNotChange() {
        ClusterizationData initialClusterizationData = createClusterizationData(
            new PskuIdsIterator(Collections.emptyList()), 4, 4);
        assertThat(initialClusterizationData.getClusters())
            .extracting(PskuCluster::getCategoryId)
            .containsOnly(DEFAULT_CATEGORY);
        ClusterizationData processedClusters = postprocessor.process(initialClusterizationData);
        assertThat(processedClusters.getClusters())
            .extracting(PskuCluster::getCategoryId)
            .containsOnly(DEFAULT_CATEGORY);
    }

    @Test
    public void whenOneApprovedCategoryPskuInClusterShouldOverrideCategory() {
        ClusterizationData initialClusterizationData = createClusterizationData(
            new PskuIdsIterator(Arrays.asList(PSKU_APPROVED_1, PSKU_APPROVED_2)), 1, 4);
        assertThat(initialClusterizationData.getClusters())
            .extracting(PskuCluster::getCategoryId)
            .containsOnly(DEFAULT_CATEGORY);

        ClusterizationData processedClusters = postprocessor.process(initialClusterizationData);

        assertThat(processedClusters.getClusters())
            .extracting(PskuCluster::getCategoryId)
            .containsOnly(APPROVED_CATEGORY_1);
    }

    @Test
    public void whenDifferentApprovedCategoriesInClusterAndSomeUnknownShouldNotChange() {
        ClusterizationData initialClusterizationData = createClusterizationData(
            new PskuIdsIterator(Arrays.asList(PSKU_APPROVED_1, PSKU_APPROVED_3)), 1, 4);
        assertThat(initialClusterizationData.getClusters())
            .extracting(PskuCluster::getCategoryId)
            .containsOnly(DEFAULT_CATEGORY);

        ClusterizationData processedClusters = postprocessor.process(initialClusterizationData);

        assertThat(processedClusters.getClusters())
            .extracting(PskuCluster::getCategoryId)
            .containsOnly(DEFAULT_CATEGORY);
    }

    @Test
    public void whenDifferentApprovedCategoriesInClusterAndAllKnownShouldSplit() {
        ClusterizationData initialClusterizationData = createClusterizationData(new PskuIdsIterator(
            Arrays.asList(PSKU_APPROVED_1, PSKU_APPROVED_2, PSKU_APPROVED_3, PSKU_APPROVED_4)), 1, 4);
        assertThat(initialClusterizationData.getClusters())
            .extracting(PskuCluster::getCategoryId)
            .containsOnly(DEFAULT_CATEGORY);

        ClusterizationData processedClusters = postprocessor.process(initialClusterizationData);

        assertThat(processedClusters.getClusters())
            .extracting(PskuCluster::getCategoryId)
            .containsExactlyInAnyOrder(APPROVED_CATEGORY_1, APPROVED_CATEGORY_2);
        assertThat(processedClusters.getClusters())
            .extracting(PskuCluster::getPskus)
            .allMatch(pskus -> pskus.size() == 2);
    }

    @Test
    public void whenSplitIntoClusterSizeOneShouldAddToSingleByCategory() {
        ClusterizationData initialClusterizationData = createClusterizationData(
            new PskuIdsIterator(Arrays.asList(PSKU_APPROVED_1, PSKU_APPROVED_3, PSKU_APPROVED_4)), 1, 3);
        assertThat(initialClusterizationData.getClusters())
            .extracting(PskuCluster::getCategoryId)
            .containsOnly(DEFAULT_CATEGORY);

        ClusterizationData processedClusters = postprocessor.process(initialClusterizationData);

        assertThat(processedClusters.getClusters())
            .extracting(PskuCluster::getCategoryId)
            .containsExactlyInAnyOrder(APPROVED_CATEGORY_2);
        assertThat(processedClusters.getSinglePskuByCategory())
            .hasEntrySatisfying(APPROVED_CATEGORY_1, pskus -> assertThat(pskus).containsExactly(PSKU_APPROVED_1));
    }


    private static class PskuIdsIterator implements Iterator<Long> {

        private final List<Long> approvedPskus;
        private int currentNumber = 0;
        private long currentId = 10L;

        private PskuIdsIterator(List<Long> approvedPskus) {
            this.approvedPskus = approvedPskus;
        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public Long next() {
            if (currentNumber < approvedPskus.size()) {
                return approvedPskus.get(currentNumber++);
            }
            return currentId++;
        }
    }

}