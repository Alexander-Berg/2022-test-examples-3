package ru.yandex.market.psku.postprocessor.common.db.dao;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterContent;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterMeta;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.CLUSTER_GENERATION;

public class ClusterMetaDaoTest extends BaseDBTest {

    @Autowired
    ClusterMetaDao clusterMetaDao;

    @Autowired
    ClusterContentDao clusterContentDao;

    @Test
    public void testBatchInsert() {
        List<ClusterMeta> clusterMetaList = new ArrayList<>();
        Set<String> clusterIds = new HashSet<>();

        long clusterGenerationId =
        dsl().insertInto(CLUSTER_GENERATION, CLUSTER_GENERATION.YT_PATH,
                CLUSTER_GENERATION.IS_CURRENT, CLUSTER_GENERATION.CREATE_DATE)
            .values("yt_path", true, Timestamp.from(Instant.now()))
            .returningResult(CLUSTER_GENERATION.ID)
            .fetchOne().get(CLUSTER_GENERATION.ID);

        for (int i = 0; i < 100; i++) {
            String clusterId = UUID.randomUUID().toString();
            ClusterMeta clusterMeta = new ClusterMeta();
            clusterMeta.setClusterGenerationId(clusterGenerationId);
            clusterMeta.setYtId(clusterId);
            clusterMeta.setWeight(null);
            clusterMeta.setCreateDate(Timestamp.from(Instant.now()));
            clusterMeta.setStatus(ClusterStatus.NEW);
            clusterMeta.setType(ClusterType.DSBS);

            clusterMetaList.add(clusterMeta);
            clusterIds.add(clusterId);
        }

        Map<String, Long> result = clusterMetaDao.insertClusters(clusterMetaList);
        assertThat(result.keySet()).containsAll(clusterIds);
    }

    @Test
    public void testFetchDSBSClusterMetasWithNewContentsForCreateCard() {
        clusterMetaDao.insert(
                IntStream.range(0, 100).mapToObj(i -> createSimpleMeta()).collect(Collectors.toList())
        );
        clusterMetaDao.findAll().stream().filter(clusterMeta -> clusterMeta.getId() % 2 == 0).forEach(
                clusterMeta -> {
                    Long id = clusterMeta.getId();
                    clusterContentDao.insert(createSimpleContentForClusterMeta(id));
                    if (id % 5 == 0) {
                        ClusterContent content = createSimpleContentForClusterMeta(id);
                        content.setWeight(Double.valueOf(id));
                        clusterContentDao.insert(content);
                    }
                }
        );

        Map<ClusterMeta, Optional<ClusterContent>> contents =
                clusterMetaDao.fetchDSBSClusterMetasWithNewContentsForCreateCard(
                        ClusterStatus.CREATE_CARD_NEW, ClusterContentStatus.NEW
                );
        assertThat(contents).hasSize(100);
        assertThat(contents.keySet().stream().map(ClusterMeta::getId).distinct().toArray()).hasSize(100);
        assertThat(contents.values().stream().filter(Optional::isPresent).toArray()).hasSize(50);
    }

    private ClusterMeta createSimpleMeta() {
        ClusterMeta clusterMeta = new ClusterMeta();
        clusterMeta.setStatus(ClusterStatus.CREATE_CARD_NEW);
        clusterMeta.setType(ClusterType.DSBS);
        clusterMeta.setWeight(0.0);
        return clusterMeta;
    }

    private ClusterContent createSimpleContentForClusterMeta(Long clusterMetaId) {
        ClusterContent clusterContent = new ClusterContent();
        clusterContent.setStatus(ClusterContentStatus.NEW);
        clusterContent.setType(ClusterContentType.DSBS);
        clusterContent.setClusterMetaId(clusterMetaId);
        clusterContent.setWeight(0.0);
        return clusterContent;
    }
}