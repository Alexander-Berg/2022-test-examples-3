package ru.yandex.market.psku.postprocessor.bazinga.deduplication;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterContentDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterMetaDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.ClusterGeneration;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterContent;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterMeta;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.ClusterContent.CLUSTER_CONTENT;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.ClusterMeta.CLUSTER_META;

public class MappingModerationToRemappingTaskTest extends BaseDBTest {

    @Autowired
    ClusterContentDao clusterContentDao;

    @Autowired
    ClusterMetaDao clusterMetaDao;

    @Test
    public void testIfAllElementsMinusOneIsReceiveResult() {
        MappingModerationToRemappingTask mappingModerationToRemappingTask =
            new MappingModerationToRemappingTask(null,null, null);
        long clusterGenerationId = 1L;
        initClusterGeneration(clusterGenerationId, "/test", true);
        long clusterMetaId = insertMeta(ClusterStatus.MAPPING_MODERATION_IN_PROCESS, clusterGenerationId);
        ClusterMeta clusterMeta = clusterMetaDao.findById(clusterMetaId);
        initClusterContent(clusterMetaId, ClusterContentType.MSKU, 100630952580L, null, null, null, null);
        initClusterContent(clusterMetaId, ClusterContentType.DSBS, null, 683844L, "118432", 1L, -1L);

        List<ClusterContent> clusterContentList = clusterContentDao.fetchByClusterMetaId(clusterMetaId);
        assertThat(mappingModerationToRemappingTask
            .isClusterProcessingFinished(clusterContentList, clusterGenerationId, clusterMeta)).isTrue();
    }

    @Test
    public void testIfAllElementsMinusTwoIsReceiveResult() {
        MappingModerationToRemappingTask mappingModerationToRemappingTask =
            new MappingModerationToRemappingTask(null,null, null);
        long clusterGenerationId = 1L;
        initClusterGeneration(clusterGenerationId, "/test", true);
        long clusterMetaId = insertMeta(ClusterStatus.MAPPING_MODERATION_IN_PROCESS, clusterGenerationId);
        ClusterMeta clusterMeta = clusterMetaDao.findById(clusterMetaId);
        initClusterContent(clusterMetaId, ClusterContentType.MSKU, 100630952580L, null, null, null, null);
        initClusterContent(clusterMetaId, ClusterContentType.DSBS, null, 683844L, "118432", 1L, -1L);
        initClusterContent(clusterMetaId, ClusterContentType.DSBS, null, 683845L, "118432", null, null);

        List<ClusterContent> clusterContentList = clusterContentDao.fetchByClusterMetaId(clusterMetaId);
        assertThat(mappingModerationToRemappingTask
            .isClusterProcessingFinished(clusterContentList, clusterGenerationId, clusterMeta)).isFalse();
    }

    @Test
    public void testIfExistsOneResultInPrevGeneration() {
        MappingModerationToRemappingTask mappingModerationToRemappingTask =
            new MappingModerationToRemappingTask(null,null, null);
        long clusterGenerationId = 1L;
        long newClusterGenerationId = 2L;
        initClusterGeneration(clusterGenerationId, "/test", false);
        long clusterMetaId = insertMeta(ClusterStatus.MAPPING_MODERATION_IN_PROCESS, clusterGenerationId);
        ClusterMeta clusterMeta = clusterMetaDao.findById(clusterMetaId);
        initClusterContent(clusterMetaId, ClusterContentType.MSKU, 100630952580L, null, null, null, null);
        initClusterContent(clusterMetaId, ClusterContentType.DSBS, null, 683844L, "118432", 1L, -1L);
        initClusterContent(clusterMetaId, ClusterContentType.DSBS, null, 683845L, "118432", null, null);

        List<ClusterContent> clusterContentList = clusterContentDao.fetchByClusterMetaId(clusterMetaId);
        assertThat(mappingModerationToRemappingTask
            .isClusterProcessingFinished(clusterContentList, newClusterGenerationId, clusterMeta)).isTrue();
    }

    private void initClusterContent(long clusterMetaId, ClusterContentType type, Long skuId,
                                    Long businessId, String offerId, Long taskId, Long targetSkuId) {
        dsl().insertInto(CLUSTER_CONTENT)
            .set(CLUSTER_CONTENT.CLUSTER_META_ID, clusterMetaId)
            .set(CLUSTER_CONTENT.TYPE, type)
            .set(CLUSTER_CONTENT.SKU_ID, skuId)
            .set(CLUSTER_CONTENT.BUSINESS_ID, businessId)
            .set(CLUSTER_CONTENT.OFFER_ID, offerId)
            .set(CLUSTER_CONTENT.STATUS, ClusterContentStatus.NEW)
            .set(CLUSTER_CONTENT.TASK_ID, taskId)
            .set(CLUSTER_CONTENT.TARGET_SKU_ID, targetSkuId)
            .execute();
    }

    private void initClusterGeneration(long prevClusterGenerationId, String s, boolean b) {
        dsl().insertInto(ClusterGeneration.CLUSTER_GENERATION)
            .set(ClusterGeneration.CLUSTER_GENERATION.ID, prevClusterGenerationId)
            .set(ClusterGeneration.CLUSTER_GENERATION.YT_PATH, s)
            .set(ClusterGeneration.CLUSTER_GENERATION.IS_CURRENT, b)
            .set(ClusterGeneration.CLUSTER_GENERATION.CREATE_DATE, Timestamp.from(Instant.now()))
            .execute();
    }

    private long insertMeta(ClusterStatus clusterStatus, Long clusterGenerationId) {
        return dsl().insertInto(CLUSTER_META)
            .set(CLUSTER_META.CLUSTER_GENERATION_ID, clusterGenerationId)
            .set(CLUSTER_META.STATUS, clusterStatus)
            .set(CLUSTER_META.TYPE, ClusterType.PSKU_EXISTS)
            .set(CLUSTER_META.WEIGHT, 0.0)
            .set(CLUSTER_META.CREATE_DATE, Timestamp.from(Instant.now()))
            .returning(CLUSTER_META.ID)
            .fetchOne().component1();
    }
}
