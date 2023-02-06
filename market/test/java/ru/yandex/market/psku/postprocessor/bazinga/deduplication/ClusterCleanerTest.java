package ru.yandex.market.psku.postprocessor.bazinga.deduplication;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterContentDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterGenerationDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterMetaDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterContent;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterMeta;
import ru.yandex.market.psku.postprocessor.service.deduplication.ClusterCleaner;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.CLUSTER_GENERATION;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterStatus.CREATE_CARD_IN_PROCESS;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterStatus.CREATE_CARD_SUCCESS;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterStatus.MAPPING_MODERATION_FINISHED;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterStatus.MAPPING_MODERATION_IN_PROCESS;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterStatus.MAPPING_MODERATION_TOLOKA_FINISHED;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterStatus.MAPPING_MODERATION_TOLOKA_IN_PROCESS;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterStatus.PSKU_DELETE_FINISHED;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterStatus.PSKU_DELETE_IN_PROCESS;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterStatus.REMAPPING_FINISHED;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterType.DSBS;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterType.DSBS_WITH_BARCODE;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterType.MSKU_EXISTS;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterType.PSKU_EXISTS;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.ClusterContent.CLUSTER_CONTENT;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.ClusterMeta.CLUSTER_META;

public class ClusterCleanerTest extends BaseDBTest {

    @Autowired
    private ClusterGenerationDao clusterGenerationDao;
    @Autowired
    private ClusterMetaDao clusterMetaDao;
    @Autowired
    private ClusterContentDao clusterContentDao;

    private ClusterCleaner clusterCleaner;

    private List<ClusterStatus> statusesToClean;

    private List<ClusterStatus> statusesToStay;

    @Before
    public void init() {
        clusterCleaner = new ClusterCleaner(clusterGenerationDao, clusterMetaDao, clusterContentDao);
        statusesToClean = Arrays.asList(
                ClusterStatus.NEW,
                ClusterStatus.CREATE_CARD_NEW,
                ClusterStatus.CREATE_CARD_UNSUCCESS,
                ClusterStatus.MAPPING_MODERATION_NEW,
                ClusterStatus.MAPPING_MODERATION_TOLOKA_NEW,
                ClusterStatus.INVALID
        );
        statusesToStay = Arrays.asList(
                CREATE_CARD_IN_PROCESS,
                CREATE_CARD_SUCCESS,
                MAPPING_MODERATION_IN_PROCESS,
                MAPPING_MODERATION_TOLOKA_IN_PROCESS,
                MAPPING_MODERATION_FINISHED,
                MAPPING_MODERATION_TOLOKA_FINISHED,
                REMAPPING_FINISHED,
                PSKU_DELETE_IN_PROCESS,
                PSKU_DELETE_FINISHED
        );
    }

    @Test
    public void testCleanSuccess() {
        long oldGenerationId = createGeneration(false, Instant.now().minus(1L, ChronoUnit.DAYS));
        long currentGenerationId = createGeneration(true, Instant.now().minus(2L, ChronoUnit.HOURS));


        createClusters(oldGenerationId, statusesToClean);
        createClusters(oldGenerationId, statusesToStay);

        createClusters(currentGenerationId, statusesToClean);
        createClusters(currentGenerationId, statusesToStay);

        List<Long> contentIdsToClean = contentIds(oldGenerationId, statusesToClean);
        List<Long> contentIdsToStay = contentIds(oldGenerationId, statusesToStay);
        List<Long> clustersIdsToClean = clusterIds(oldGenerationId, statusesToClean);
        List<Long> clustersIdsToStay = clusterIds(oldGenerationId, statusesToStay);
        clusterCleaner.clean();

        List<Long> contentIds = contentIds();
        List<Long> clusterIds = clusterIds();
        assertThat(contentIds).doesNotContainAnyElementsOf(contentIdsToClean);
        assertThat(contentIds).containsAll(contentIdsToStay);
        assertThat(clusterIds).doesNotContainAnyElementsOf(clustersIdsToClean);
        assertThat(clusterIds).containsAll(clustersIdsToStay);
    }

    @Test
    public void testCurrentGenTooYangForClean() {
        long oldGenerationId = createGeneration(false, Instant.now().minus(1L, ChronoUnit.DAYS));
        long currentGenerationId = createGeneration(true, Instant.now().minus(30L, ChronoUnit.MINUTES));


        createClusters(oldGenerationId, statusesToClean);
        createClusters(oldGenerationId, statusesToStay);

        createClusters(currentGenerationId, statusesToClean);
        createClusters(currentGenerationId, statusesToStay);

        List<Long> contentIdsBeforeClean = contentIds();
        List<Long> clusterIdsBeforeClean = clusterIds();
        clusterCleaner.clean();
        List<Long> contentIds = contentIds();
        List<Long> clusterIds = clusterIds();

        assertThat(contentIds).containsAll(contentIdsBeforeClean);
        assertThat(clusterIds).containsAll(clusterIdsBeforeClean);

    }

    @Test
    public void testClustersWithContentsWithTaskNotToClean() {
        long oldGenerationId = createGeneration(false, Instant.now().minus(1L, ChronoUnit.DAYS));
        //  чтобы было текущее поколение
        createGeneration(true, Instant.now().minus(2L, ChronoUnit.HOURS));


        createClusters(oldGenerationId, statusesToClean);

        List<Long> clusterIdsBeforeClean = clusterIds();

        // в одном оффере из кластера проставляем taskId
        Long clusterIdWithTask = clusterIdsBeforeClean.get(0);
        List<ClusterContent> clusterContents = clusterContentDao.fetchByClusterMetaId(clusterIdWithTask);
        ClusterContent clusterContentWithTask = clusterContents.get(0);
        clusterContentWithTask.setTaskId(123L);
        clusterContentDao.update(clusterContentWithTask);
        List<Long> contentIdsInClusterWithTask = clusterContents.stream().map(ClusterContent::getId).collect(Collectors.toList());

        clusterCleaner.clean();
        List<Long> contentIds = contentIds();
        List<Long> clusterIds = clusterIds();

        // должен остаться кластер с задачей и все его офферы
        assertThat(contentIds).containsExactlyInAnyOrderElementsOf(contentIdsInClusterWithTask);
        assertThat(clusterIds).containsExactly(clusterIdWithTask);
    }

    private List<Long> contentIds(long generationId, List<ClusterStatus> statuses) {
        return dsl().select().from(CLUSTER_META).join(CLUSTER_CONTENT)
                .on(CLUSTER_META.ID.eq(CLUSTER_CONTENT.CLUSTER_META_ID))
                .where(CLUSTER_META.CLUSTER_GENERATION_ID.eq(generationId).and(CLUSTER_META.STATUS.in(statuses)))
                .fetch(CLUSTER_CONTENT.ID);
    }

    private List<Long> contentIds() {
        return dsl().select().from(CLUSTER_META).join(CLUSTER_CONTENT)
                .on(CLUSTER_META.ID.eq(CLUSTER_CONTENT.CLUSTER_META_ID))
                .fetch(CLUSTER_CONTENT.ID);
    }

    private List<Long> clusterIds(long generationId, List<ClusterStatus> statuses) {
        return dsl().select().from(CLUSTER_META)
                .where(CLUSTER_META.CLUSTER_GENERATION_ID.eq(generationId).and(CLUSTER_META.STATUS.in(statuses)))
                .fetch(CLUSTER_META.ID);
    }

    private List<Long> clusterIds() {
        return dsl().select().from(CLUSTER_META)
                .fetch(CLUSTER_META.ID);
    }

    private long createGeneration(boolean current, Instant createDate) {
        return dsl().insertInto(CLUSTER_GENERATION)
                .set(CLUSTER_GENERATION.YT_PATH, "/test2")
                .set(CLUSTER_GENERATION.IS_CURRENT, current)
                .set(CLUSTER_GENERATION.CREATE_DATE, Timestamp.from(createDate))
                .returning(CLUSTER_GENERATION.ID).fetchOne().getId();
    }

    private void createClusters(long clusterGenerationId, List<ClusterStatus> clusterStatuses) {

        for (ClusterType type : Arrays.asList(DSBS, DSBS_WITH_BARCODE)) {
            for (ClusterStatus status : clusterStatuses) {
                dsl().insertInto(CLUSTER_META)
                        .set(CLUSTER_META.CLUSTER_GENERATION_ID, clusterGenerationId)
                        .set(CLUSTER_META.STATUS, status)
                        .set(CLUSTER_META.TYPE, type)
                        .set(CLUSTER_META.WEIGHT, 0.0)
                        .set(CLUSTER_META.CREATE_DATE, Timestamp.from(Instant.now()))
                        .execute();
            }
        }
        for (ClusterType type : Arrays.asList(PSKU_EXISTS, MSKU_EXISTS)) {
            for (ClusterStatus status : clusterStatuses) {
                dsl().insertInto(CLUSTER_META)
                        .set(CLUSTER_META.CLUSTER_GENERATION_ID, clusterGenerationId)
                        .set(CLUSTER_META.STATUS, status)
                        .set(CLUSTER_META.TYPE, type)
                        .set(CLUSTER_META.WEIGHT, 0.0)
                        .set(CLUSTER_META.CREATE_DATE, Timestamp.from(Instant.now()))
                        .execute();
            }
        }

        List<ClusterMeta> clusters = dsl().selectFrom(CLUSTER_META).fetchInto(ClusterMeta.class);
        for (ClusterMeta cluster : clusters) {
            for (int i = 0; i < 5; i++) {
                dsl().insertInto(CLUSTER_CONTENT)
                        .set(CLUSTER_CONTENT.CLUSTER_META_ID, cluster.getId())
                        .set(CLUSTER_CONTENT.WEIGHT, 0.0)
                        .set(CLUSTER_CONTENT.TYPE, resolveClusterContentType(cluster.getType()))
                        .set(CLUSTER_CONTENT.STATUS, ClusterContentStatus.NEW).execute();
            }
        }
    }

    private ClusterContentType resolveClusterContentType(ClusterType clusterType) {
        if (clusterType == DSBS || clusterType == DSBS_WITH_BARCODE) {
            return ClusterContentType.DSBS;
        }
        if (clusterType == MSKU_EXISTS || clusterType == PSKU_EXISTS) {
            return ClusterContentType.PSKU;
        }
        return ClusterContentType.MSKU;
    }
}
