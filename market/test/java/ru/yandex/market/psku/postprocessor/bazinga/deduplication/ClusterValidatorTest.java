package ru.yandex.market.psku.postprocessor.bazinga.deduplication;

import java.io.File;
import java.sql.Timestamp;
import java.time.Instant;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterContentDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterGenerationDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterMetaDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.TaskPropertiesDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.ClusterGeneration;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterMeta;
import ru.yandex.market.psku.postprocessor.service.deduplication.ClusterValidator;
import ru.yandex.market.psku.postprocessor.service.deduplication.TaskPropertiesService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.ALLOWED_CATEGORIES_FOR_TOLOKA;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.ClusterContent.CLUSTER_CONTENT;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.ClusterMeta.CLUSTER_META;
import static ru.yandex.market.psku.postprocessor.service.deduplication.ClusterValidator.TOLOKA_CLUSTER_LIMIT_PROPERTY;
import static ru.yandex.market.psku.postprocessor.service.deduplication.ClusterValidator.VALIDATED_CLUSTERS_COUNT_LIMIT_PROPERTY;

public class ClusterValidatorTest extends BaseDBTest {

    public static final long GENERATION_ID = 101L;
    public static final long CATEGORY_ID = 500L;
    public static final int TOTAL_CLUSTERS = 20;
    public static final int TOLOKA_CLUSTER_LIMIT = 10;
    public static final int YANG_CLUSTER_LIMIT = 5;

    @Autowired
    ClusterMetaDao clusterMetaDao;
    @Autowired
    ClusterGenerationDao clusterGenerationDao;
    @Autowired
    ClusterContentDao clusterContentDao;
    @Autowired
    TaskPropertiesDao taskPropertiesDao;

    TaskPropertiesService taskPropertiesService;

    @Before
    public void setUp() throws Exception {
        System.setProperty("configs.path",
                getClass().getClassLoader().getResource("task_properties_service_test.properties").getFile());
        taskPropertiesService = new TaskPropertiesService(taskPropertiesDao);
    }

    public void updateProperties(int validatedClustersLimit, int tolokaClusterLimit) {
        taskPropertiesService.updateProperty(VALIDATED_CLUSTERS_COUNT_LIMIT_PROPERTY, validatedClustersLimit);
        taskPropertiesService.updateProperty(TOLOKA_CLUSTER_LIMIT_PROPERTY, tolokaClusterLimit);
    }

    @Test
    public void testIfClusterElementInProgressThenSkip() {
        // создадим 2 поколения
        long prevClusterGenerationId = 101L;
        initClusterGeneration(prevClusterGenerationId, "/test1", false);

        long currentClusterGenerationId = 102L;
        initClusterGeneration(currentClusterGenerationId, "/test2", true);

        long businessId = 1L;
        String duplicateOfferId = "duplicateOfferId";
        long pskuId = 2L;
        long newPskuId = 3L;
        String newOfferId = "newOfferId";

        long clusterMetaInProcess = insertMeta(ClusterStatus.CREATE_CARD_IN_PROCESS, prevClusterGenerationId);
        initClusterContent(clusterMetaInProcess, ClusterContentType.DSBS, null, businessId, duplicateOfferId);
        initClusterContent(clusterMetaInProcess, ClusterContentType.PSKU, pskuId, null, null);

        long clusterMetaNewId = insertMeta(ClusterStatus.NEW, currentClusterGenerationId);
        initClusterContent(clusterMetaNewId, ClusterContentType.DSBS, null, businessId, duplicateOfferId);
        initClusterContent(clusterMetaNewId, ClusterContentType.PSKU, pskuId, null, null);
        initClusterContent(clusterMetaNewId, ClusterContentType.PSKU, newPskuId, null, null);
        initClusterContent(clusterMetaNewId, ClusterContentType.DSBS, null, businessId, newOfferId);

        ClusterValidator validator = new ClusterValidator(clusterMetaDao, clusterGenerationDao, clusterContentDao,
                taskPropertiesService);
        validator.validate();

        ClusterMeta clusterMetaNew = clusterMetaDao.fetchById(clusterMetaNewId).stream().findAny().get();
        assertThat(clusterMetaNew.getStatus()).isEqualTo(ClusterStatus.INVALID);
    }

    @Test
    public void testIfClusterElementNotInProgressThenDontSkip() {
        // создадим 2 поколения
        long prevClusterGenerationId = 101L;
        initClusterGeneration(prevClusterGenerationId, "/test1", false);

        long currentClusterGenerationId = 102L;
        initClusterGeneration(currentClusterGenerationId, "/test2", true);

        long businessId = 1L;
        String duplicateOfferId = "duplicateOfferId";
        long pskuId = 2L;
        long newPskuId = 3L;
        String newOfferId = "newOfferId";

        long clusterMetaInProcess = insertMeta(ClusterStatus.CREATE_CARD_IN_PROCESS, prevClusterGenerationId);
        initClusterContent(clusterMetaInProcess, ClusterContentType.DSBS, null, businessId, duplicateOfferId);
        initClusterContent(clusterMetaInProcess, ClusterContentType.PSKU, pskuId, null, null);

        long clusterMetaNewId = insertMeta(ClusterStatus.NEW, currentClusterGenerationId);
        initClusterContent(clusterMetaNewId, ClusterContentType.PSKU, newPskuId, null, null);
        initClusterContent(clusterMetaNewId, ClusterContentType.DSBS, null, businessId, newOfferId);

        ClusterValidator validator = new ClusterValidator(clusterMetaDao, clusterGenerationDao, clusterContentDao,
                taskPropertiesService);
        updateProperties(100, 0);
        validator.validate();

        ClusterMeta clusterMetaNew = clusterMetaDao.fetchById(clusterMetaNewId).stream().findAny().get();
        assertThat(clusterMetaNew.getStatus()).isNotEqualTo(ClusterStatus.INVALID);
    }

    @Test
    public void markClusterInvalidIfClusterHasDuplicateSkuId() {
        initClusterGeneration(GENERATION_ID, "/test1", true);
        long pskuId = 2L;
        long clusterId = insertMeta(ClusterStatus.NEW, GENERATION_ID);
        initClusterContent(clusterId, ClusterContentType.PSKU, pskuId, null, null);
        initClusterContent(clusterId, ClusterContentType.PSKU, pskuId, null, null);

        ClusterValidator validator = new ClusterValidator(clusterMetaDao, clusterGenerationDao, clusterContentDao,
                taskPropertiesService);
        updateProperties(9, 9);
        validator.validate();
        ClusterMeta invalidCluster = clusterMetaDao.fetchOneById(clusterId);
        assertThat(invalidCluster).extracting(ClusterMeta::getStatus).isEqualTo(ClusterStatus.INVALID);
    }

    private void initClusterContent(long clusterMetaId, ClusterContentType type, Long skuId,
                                    Long businessId, String offerId) {
        dsl().insertInto(CLUSTER_CONTENT)
            .set(CLUSTER_CONTENT.CLUSTER_META_ID, clusterMetaId)
            .set(CLUSTER_CONTENT.TYPE, type)
            .set(CLUSTER_CONTENT.SKU_ID, skuId)
            .set(CLUSTER_CONTENT.BUSINESS_ID, businessId)
            .set(CLUSTER_CONTENT.OFFER_ID, offerId)
            .set(CLUSTER_CONTENT.STATUS, ClusterContentStatus.NEW)
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

    @Test
    public void test() {
        prepareState();
        Integer clustersInNewBefore = dsl().selectCount()
            .from(CLUSTER_META)
            .where(CLUSTER_META.STATUS.eq(ClusterStatus.NEW))
            .fetchOneInto(Integer.class);

        assertThat(clustersInNewBefore).isEqualTo(31);

        ClusterValidator validator = new ClusterValidator(clusterMetaDao, clusterGenerationDao, clusterContentDao,
                taskPropertiesService);
        updateProperties(120, 0);
        validator.validate();

        Integer clustersInNewAfter = dsl().selectCount()
            .from(CLUSTER_META)
            .where(CLUSTER_META.STATUS.eq(ClusterStatus.NEW))
            .fetchOneInto(Integer.class);
        assertThat(clustersInNewAfter).isEqualTo(1);

        dsl().update(CLUSTER_META).set(CLUSTER_META.STATUS, ClusterStatus.FINISHED)
            .where(CLUSTER_META.STATUS.eq(ClusterStatus.MAPPING_MODERATION_FINISHED)).execute();

        validator.validate();

        assertThat(countClustersWithStatus(ClusterStatus.NEW)).isEqualTo(0);
    }

    @Test
    public void testNewToInvalidStatus() {
        long clusterGenerationId = 102L;
        createClusterGenereation(clusterGenerationId);

        for (int i = 0; i < 100; i++) {
            dsl().insertInto(CLUSTER_META)
                .set(CLUSTER_META.CLUSTER_GENERATION_ID, clusterGenerationId)
                .set(CLUSTER_META.STATUS, ClusterStatus.NEW)
                .set(CLUSTER_META.TYPE, ClusterType.PSKU_EXISTS)
                .set(CLUSTER_META.WEIGHT, 0.0)
                .set(CLUSTER_META.CREATE_DATE, Timestamp.from(Instant.now()))
                .execute();
            dsl().insertInto(CLUSTER_META)
                .set(CLUSTER_META.CLUSTER_GENERATION_ID, clusterGenerationId)
                .set(CLUSTER_META.STATUS, ClusterStatus.NEW)
                .set(CLUSTER_META.TYPE, ClusterType.MULTIPLE_MSKU)
                .set(CLUSTER_META.WEIGHT, 0.0)
                .set(CLUSTER_META.CREATE_DATE, Timestamp.from(Instant.now()))
                .execute();
        }
        ClusterValidator validator = new ClusterValidator(clusterMetaDao, clusterGenerationDao,clusterContentDao,
                taskPropertiesService);
        updateProperties(100, 0);
        validator.validate();
        int inProgressCount = countClustersWithStatus(ClusterStatus.MAPPING_MODERATION_NEW);
        assertThat(inProgressCount).isEqualTo(100);
    }

    private int countClustersWithStatus(ClusterStatus status) {
        return dsl().selectCount()
                .from(CLUSTER_META)
                .where(CLUSTER_META.STATUS.eq(status))
                .fetchOneInto(Integer.class);
    }

    @Test
    public void whenNoClustersWithTolokaStatusThenAddLimitAmount() {
        createClusterGenereation(101);
        generateClustersWithCategory(TOTAL_CLUSTERS, CATEGORY_ID, ClusterStatus.NEW);
        addAllowedCategory(CATEGORY_ID);
        ClusterValidator validator = new ClusterValidator(clusterMetaDao,
                clusterGenerationDao,
                clusterContentDao,
                taskPropertiesService
        );
        validator.validate();

        assertThat(countClustersWithStatus(ClusterStatus.MAPPING_MODERATION_TOLOKA_NEW)).isEqualTo(TOLOKA_CLUSTER_LIMIT);
        assertThat(countClustersWithStatus(ClusterStatus.MAPPING_MODERATION_NEW)).isEqualTo(YANG_CLUSTER_LIMIT);
        assertThat(countClustersWithStatus(ClusterStatus.NEW)).isEqualTo(TOTAL_CLUSTERS - TOLOKA_CLUSTER_LIMIT - YANG_CLUSTER_LIMIT);
    }



    @Test
    public void whenNoClustersThenAddAllToToloka() {
        createClusterGenereation(101);
        int totalClusters = 2;
        generateClustersWithCategory(totalClusters, CATEGORY_ID, ClusterStatus.NEW);
        addAllowedCategory(CATEGORY_ID);
        ClusterValidator validator = new ClusterValidator(clusterMetaDao,
                clusterGenerationDao,
                clusterContentDao,
                taskPropertiesService
        );
        validator.validate();

        assertThat(countClustersWithStatus(ClusterStatus.MAPPING_MODERATION_TOLOKA_NEW)).isEqualTo(totalClusters);
        assertThat(countClustersWithStatus(ClusterStatus.MAPPING_MODERATION_NEW)).isEqualTo(0);
        assertThat(countClustersWithStatus(ClusterStatus.NEW)).isEqualTo(0);
    }

    @Test
    public void whenSomeClusterWithTolokaStatusThenAddMissingAmount() {
        int alreadyInProcess = 2;
        createClusterGenereation(GENERATION_ID);
        generateClustersWithCategory(TOTAL_CLUSTERS, CATEGORY_ID, ClusterStatus.NEW);
        generateClustersWithCategory(alreadyInProcess, CATEGORY_ID, ClusterStatus.MAPPING_MODERATION_TOLOKA_NEW);

        addAllowedCategory(CATEGORY_ID);
        ClusterValidator validator = new ClusterValidator(clusterMetaDao,
                clusterGenerationDao,
                clusterContentDao,
                taskPropertiesService
        );
        validator.validate();

        clusterMetaDao.findAll();
        assertThat(countClustersWithStatus(ClusterStatus.MAPPING_MODERATION_TOLOKA_NEW)).isEqualTo(TOLOKA_CLUSTER_LIMIT);
        assertThat(countClustersWithStatus(ClusterStatus.MAPPING_MODERATION_NEW)).isEqualTo(YANG_CLUSTER_LIMIT);
        assertThat(countClustersWithStatus(ClusterStatus.NEW)).isEqualTo(TOTAL_CLUSTERS - TOLOKA_CLUSTER_LIMIT - YANG_CLUSTER_LIMIT + alreadyInProcess);
    }

    @Test
    public void whenTolokaPoolIsNotFullThenStillAddEvenIfTaskQuotaIsFull() {
        int alreadyInProcess = 5;
        createClusterGenereation(GENERATION_ID);
        //кластера не подходящие для толоки будут пропущены
        generateClustersWithCategory(TOTAL_CLUSTERS, CATEGORY_ID + 1, ClusterStatus.NEW);
        generateClustersWithCategory(TOTAL_CLUSTERS, CATEGORY_ID, ClusterStatus.NEW);
        //заполняем taskQuota полностью
        generateClustersWithCategory(alreadyInProcess, CATEGORY_ID, ClusterStatus.MAPPING_MODERATION_NEW);

        addAllowedCategory(CATEGORY_ID);
        ClusterValidator validator = new ClusterValidator(clusterMetaDao,
                clusterGenerationDao,
                clusterContentDao,
                taskPropertiesService
        );
        validator.validate();

        clusterMetaDao.findAll();
        assertThat(countClustersWithStatus(ClusterStatus.MAPPING_MODERATION_TOLOKA_NEW)).isEqualTo(TOLOKA_CLUSTER_LIMIT);
        assertThat(countClustersWithStatus(ClusterStatus.MAPPING_MODERATION_NEW)).isEqualTo(YANG_CLUSTER_LIMIT);
        assertThat(countClustersWithStatus(ClusterStatus.NEW)).isEqualTo(2 * TOTAL_CLUSTERS - TOLOKA_CLUSTER_LIMIT - YANG_CLUSTER_LIMIT + alreadyInProcess);
    }

    @Test
    public void whenNoClustersInAllowedCategoriesThenDontAddToToloka() {
        createClusterGenereation(101);
        long anotherCategory = CATEGORY_ID + 1;
        generateClustersWithCategory(TOTAL_CLUSTERS, anotherCategory, ClusterStatus.NEW);
        addAllowedCategory(CATEGORY_ID);
        ClusterValidator validator = new ClusterValidator(clusterMetaDao,
                clusterGenerationDao,
                clusterContentDao,
                taskPropertiesService
        );
        validator.validate();

        assertThat(countClustersWithStatus(ClusterStatus.MAPPING_MODERATION_TOLOKA_NEW)).isEqualTo(0);
        assertThat(countClustersWithStatus(ClusterStatus.MAPPING_MODERATION_NEW)).isEqualTo(YANG_CLUSTER_LIMIT);
        assertThat(countClustersWithStatus(ClusterStatus.NEW)).isEqualTo(TOTAL_CLUSTERS - YANG_CLUSTER_LIMIT);
    }

    private void generateClustersWithCategory(int count, long categoryId, ClusterStatus status) {
        for (int i = 0; i < count; i++) {
            dsl().insertInto(CLUSTER_META)
                    .set(CLUSTER_META.CLUSTER_GENERATION_ID, GENERATION_ID)
                    .set(CLUSTER_META.CATEGORY_ID, categoryId)
                    .set(CLUSTER_META.STATUS, status)
                    .set(CLUSTER_META.TYPE, ClusterType.PSKU_EXISTS)
                    .set(CLUSTER_META.WEIGHT, 0.0)
                    .set(CLUSTER_META.CREATE_DATE, Timestamp.from(Instant.now()))
                    .execute();
        }
    }

    private void createClusterGenereation(long clusterGenerationId) {
        dsl().insertInto(ClusterGeneration.CLUSTER_GENERATION)
                .set(ClusterGeneration.CLUSTER_GENERATION.ID, clusterGenerationId)
                .set(ClusterGeneration.CLUSTER_GENERATION.YT_PATH, "/test2")
                .set(ClusterGeneration.CLUSTER_GENERATION.IS_CURRENT, true)
                .set(ClusterGeneration.CLUSTER_GENERATION.CREATE_DATE, Timestamp.from(Instant.now()))
                .execute();
    }

    private void addAllowedCategory(long categoryId) {
        dsl().insertInto(ALLOWED_CATEGORIES_FOR_TOLOKA)
                .set(ALLOWED_CATEGORIES_FOR_TOLOKA.ID, categoryId)
                .execute();
    }

    private void prepareState() {
        long clusterGenerationId = 101L;
        dsl().insertInto(ClusterGeneration.CLUSTER_GENERATION)
            .set(ClusterGeneration.CLUSTER_GENERATION.ID, clusterGenerationId)
            .set(ClusterGeneration.CLUSTER_GENERATION.YT_PATH, "/test")
            .set(ClusterGeneration.CLUSTER_GENERATION.IS_CURRENT, true)
            .set(ClusterGeneration.CLUSTER_GENERATION.CREATE_DATE, Timestamp.from(Instant.now()))
            .execute();

        for (int i = 0; i < 10; i++) {
            //finished
            insertMeta(ClusterStatus.INVALID, clusterGenerationId);
            insertMeta(ClusterStatus.CREATE_CARD_UNSUCCESS, clusterGenerationId);
            insertMeta(ClusterStatus.PSKU_DELETE_FINISHED, clusterGenerationId);
            insertMeta(ClusterStatus.FINISHED, clusterGenerationId);
            //in progress
            insertMeta(ClusterStatus.CREATE_CARD_NEW, clusterGenerationId);
            insertMeta(ClusterStatus.CREATE_CARD_SUCCESS, clusterGenerationId);
            insertMeta(ClusterStatus.CREATE_CARD_IN_PROCESS, clusterGenerationId);
            insertMeta(ClusterStatus.MAPPING_MODERATION_NEW, clusterGenerationId);
            insertMeta(ClusterStatus.MAPPING_MODERATION_IN_PROCESS, clusterGenerationId);
            insertMeta(ClusterStatus.MAPPING_MODERATION_FINISHED, clusterGenerationId);
            insertMeta(ClusterStatus.REMAPPING_IN_PROCESS, clusterGenerationId);
            insertMeta(ClusterStatus.REMAPPING_FINISHED, clusterGenerationId);
            insertMeta(ClusterStatus.PSKU_DELETE_IN_PROCESS, clusterGenerationId);
        }
        for (int i = 0; i < 31; i++) {
            insertMeta(ClusterStatus.NEW, clusterGenerationId);
        }
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
