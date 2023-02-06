package ru.yandex.market.psku.postprocessor.service.msku_from_psku_generation;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.util.JooqUtils;
import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.psku.postprocessor.clusterization.SimplePskuCluster;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuInClusterInfoDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuKnowledgeDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuResultStorageDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.Tables;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.GenerationTaskType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.MskuFromPskuGenResultStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuInClusterState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuStorageState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuInClusterInfo;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuResultStorage;
import ru.yandex.market.psku.postprocessor.config.CommonTestConfig;
import ru.yandex.market.psku.postprocessor.config.MskuCreationConfig;
import ru.yandex.market.psku.postprocessor.msku_creation.ClusterPriorityService;
import ru.yandex.market.psku.postprocessor.service.enrich.SkuEnricher;
import ru.yandex.market.psku.postprocessor.service.logging.HealthLogManager;
import ru.yandex.market.psku.postprocessor.service.markup.MskuFromPskuGenerationTask;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.Cluster.CLUSTER;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.PskuInClusterInfo.PSKU_IN_CLUSTER_INFO;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.tables.PskuKnowledge.PSKU_KNOWLEDGE;


@ContextConfiguration(classes = {
    CommonTestConfig.class,
    MskuCreationConfig.class
})
public class MarkupGenerationResultReceiverServiceTest extends BaseDBTest {

    private static final String TICKET_KEY = "ticket_key";
    private static final String CATEGORY_NAME = "category_name";
    private static final long MODEL_ID = 123456L;
    private static final Long SUPPLIER_ID = 123L;
    private MarkupGenerationResultReceiverService service;
    private SkuEnricher skuEnricher;

    @Autowired
    PskuInClusterInfoDao pskuInClusterInfoDao;
    @Autowired
    ClusterPriorityService clusterPriorityService;
    @Autowired
    PskuResultStorageDao pskuResultStorageDao;
    @Autowired
    ClusterDao clusterDao;
    @Autowired
    PskuKnowledgeDao pskuKnowledgeDao;
    @Autowired
    HealthLogManager healthLogManager;

    @Mock
    MskuFromPskuGenerationTask mskuFromPskuGenerationTask;
    @Mock
    CategoryDataKnowledge categoryDataKnowledge;
    @Mock
    CategoryData categoryData;
    @Mock
    ModelStorageHelper modelStorageHelper;

    private static final String TEST_USER_LOGIN = "test_user";

    private static final int CONFIG_ID_WITH_RESULT = 10;
    private static final int CONFIG_ID_WITH_NO_RESULT = 100;
    private static final int CONFIG_ID_WITH_EMPTY_RESULT = 1000;
    private static final int CONFIG_ID_WITH_WAIT_CONTENT = 10000;
    private static final int CONFIG_ID_WITH_USER_LOGIN = 100000;

    private static final long PSKU_WITH_RESULT_1 = 1L;
    private static final long PSKU_WITH_RESULT_2 = 2L;
    private static final long PSKU_WITH_RESULT_3 = 3L;
    private static final long PSKU_WITH_RESULT_4 = 4L;
    private static final long PSKU_WITH_NO_RESULT_1 = 10L;
    private static final long PSKU_WITH_NO_RESULT_2 = 20L;
    private static final long PSKU_WITH_NO_RESULT_3 = 30L;
    private static final long PSKU_WITH_EMPTY_RESULT_1 = 1001L;
    private static final long PSKU_WITH_EMPTY_RESULT_2 = 1002L;
    private static final long PSKU_WITH_EMPTY_RESULT_3 = 1003L;
    private static final long PSKU_WAIT_CONTENT = 10001L;
    private static final long REMAPPED_PSKU = 100001L;
    private static final long CATEGORY_ID = 1233L;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        skuEnricher = new SkuEnricher(pskuKnowledgeDao, modelStorageHelper);
        PskuGenerationProxyDao pskuGenerationProxyDao = new ClusterProxyDao(clusterPriorityService, pskuInClusterInfoDao, clusterDao);
        service = new MarkupGenerationResultReceiverService(pskuGenerationProxyDao, pskuResultStorageDao,
            mskuFromPskuGenerationTask, skuEnricher, healthLogManager, jooqConfiguration, GenerationTaskType.CLUSTER);
        Mockito.when(mskuFromPskuGenerationTask.getMskuFromPskuGenerationTaskResult(CONFIG_ID_WITH_RESULT))
            .thenReturn(getMarkupMockResponse());
        Mockito.when(mskuFromPskuGenerationTask.getMskuFromPskuGenerationTaskResult(CONFIG_ID_WITH_EMPTY_RESULT))
                .thenReturn(getMarkupMockEmptyResponse());
        Mockito.when(mskuFromPskuGenerationTask.getMskuFromPskuGenerationTaskResult(CONFIG_ID_WITH_WAIT_CONTENT))
            .thenReturn(getMarkupMockWaitContentResponse());
        Mockito.when(mskuFromPskuGenerationTask.getMskuFromPskuGenerationTaskResult(CONFIG_ID_WITH_USER_LOGIN))
            .thenReturn(getMarkupMockResponseWithUserLogin());
        Mockito.when(mskuFromPskuGenerationTask.isTaskFinished(CONFIG_ID_WITH_RESULT))
            .thenReturn(Boolean.TRUE);
        Mockito.when(mskuFromPskuGenerationTask.isTaskFinished(CONFIG_ID_WITH_NO_RESULT))
            .thenReturn(Boolean.FALSE);
        Mockito.when(mskuFromPskuGenerationTask.isTaskFinished(CONFIG_ID_WITH_EMPTY_RESULT))
            .thenReturn(Boolean.TRUE);
        Mockito.when(mskuFromPskuGenerationTask.isTaskFinished(CONFIG_ID_WITH_WAIT_CONTENT))
            .thenReturn(Boolean.TRUE);
        Mockito.when(mskuFromPskuGenerationTask.isTaskFinished(CONFIG_ID_WITH_USER_LOGIN))
            .thenReturn(Boolean.TRUE);
        Mockito.when(categoryData.getUniqueName()).thenReturn(CATEGORY_NAME);
        Mockito.when(categoryDataKnowledge.getCategoryData(Mockito.anyLong()))
            .thenReturn(categoryData);
        Mockito.when(modelStorageHelper.getModelsOrFail(Mockito.anyLong(), Mockito.any()))
            .thenReturn(Collections.singletonList(
                ModelStorage.Model.newBuilder()
                    .setId(MODEL_ID)
                    .addAllTitles(Collections.singletonList(ModelStorage.LocalizedString.newBuilder()
                        .setValue("TITLE")
                        .build()))
                    .build()
            ));
    }

    @Test
    public void correctlyPassesUserLogin() {
        clusterPriorityService.refresh(Collections.singletonList(generateSimpleCluster(
            REMAPPED_PSKU
        )));
        pskuInClusterInfoDao.dsl()
            .update(PSKU_IN_CLUSTER_INFO)
            .set(PSKU_IN_CLUSTER_INFO.MARKUP_CONFIG_ID, CONFIG_ID_WITH_USER_LOGIN)
            .set(PSKU_IN_CLUSTER_INFO.STATE, PskuInClusterState.IN_PROCESS)
            .execute();
        clusterDao.dsl()
            .update(CLUSTER)
            .set(CLUSTER.CONFIG_ID, CONFIG_ID_WITH_USER_LOGIN)
            .set(CLUSTER.TICKET_KEY, TICKET_KEY)
            .execute();

        service.getAndStoreResultsFromMarkup();

        List<PskuInClusterInfo> all = pskuInClusterInfoDao.findAll();
        assertThat(all)
            .extracting(PskuInClusterInfo::getState)
            .containsOnly(PskuInClusterState.PROCESSED);
        assertThat(all)
            .extracting(PskuInClusterInfo::getMarkupRawResult)
            .extracting(Markup.MskuFromPskuGenerationTaskResult::hasMappingStatus)
            .containsOnly(Boolean.TRUE);

        Map<Long, PskuInClusterInfo> map = all.stream().collect(Collectors.toMap(PskuInClusterInfo::getPskuId, Function.identity()));
        assertThat(map.get(REMAPPED_PSKU).getResultStatus()).isEqualTo(MskuFromPskuGenResultStatus.MAPPED);
        Mockito.verify(mskuFromPskuGenerationTask, Mockito.times(1))
            .closeTicket(Mockito.any(), Mockito.any(), Mockito.eq(GenerationTaskType.CLUSTER));

        List<PskuResultStorage> resultStorages = pskuResultStorageDao.findAll();
        assertThat(resultStorages).extracting(PskuResultStorage::getUserLogin)
            .allMatch(Predicate.isEqual(TEST_USER_LOGIN));
    }

    @Test
    public void whenNoResultsShouldSaveNothing() {
        clusterPriorityService.refresh(Collections.singletonList(generateSimpleCluster(
            PSKU_WITH_NO_RESULT_1, PSKU_WITH_NO_RESULT_2, PSKU_WITH_NO_RESULT_3
        )));
        pskuInClusterInfoDao.dsl()
            .update(PSKU_IN_CLUSTER_INFO)
            .set(PSKU_IN_CLUSTER_INFO.MARKUP_CONFIG_ID, CONFIG_ID_WITH_NO_RESULT)
            .set(PSKU_IN_CLUSTER_INFO.STATE, PskuInClusterState.IN_PROCESS)
            .execute();
        service.getAndStoreResultsFromMarkup();
        assertThat(pskuInClusterInfoDao.findAll())
            .extracting(PskuInClusterInfo::getState)
            .containsOnly(PskuInClusterState.IN_PROCESS);
        Mockito.verify(mskuFromPskuGenerationTask, Mockito.never())
            .closeTicket(Mockito.any(), Mockito.any(), Mockito.eq(GenerationTaskType.CLUSTER));
    }

    @Test
    public void whenEmptyResultsShouldMarkCanceled() {
        clusterPriorityService.refresh(Collections.singletonList(generateSimpleCluster(
                PSKU_WITH_EMPTY_RESULT_1, PSKU_WITH_EMPTY_RESULT_2, PSKU_WITH_EMPTY_RESULT_3
        )));
        pskuInClusterInfoDao.dsl()
                .update(PSKU_IN_CLUSTER_INFO)
                .set(PSKU_IN_CLUSTER_INFO.MARKUP_CONFIG_ID, CONFIG_ID_WITH_EMPTY_RESULT)
                .set(PSKU_IN_CLUSTER_INFO.STATE, PskuInClusterState.IN_PROCESS)
                .execute();
        clusterDao.dsl()
                .update(CLUSTER)
                .set(CLUSTER.CONFIG_ID, CONFIG_ID_WITH_EMPTY_RESULT)
                .execute();

        service.getAndStoreResultsFromMarkup();
        assertThat(pskuInClusterInfoDao.findAll())
                .extracting(PskuInClusterInfo::getState)
                .containsOnly(PskuInClusterState.EXPIRED);
    }

    @Test
    public void whenHaveResultsShouldSaveCorrectly() {
        clusterPriorityService.refresh(Collections.singletonList(generateSimpleCluster(
            PSKU_WITH_RESULT_1, PSKU_WITH_RESULT_2, PSKU_WITH_RESULT_3, PSKU_WITH_RESULT_4
        )));
        pskuInClusterInfoDao.dsl()
            .update(PSKU_IN_CLUSTER_INFO)
            .set(PSKU_IN_CLUSTER_INFO.MARKUP_CONFIG_ID, CONFIG_ID_WITH_RESULT)
            .set(PSKU_IN_CLUSTER_INFO.STATE, PskuInClusterState.IN_PROCESS)
            .execute();
        clusterDao.dsl()
            .update(CLUSTER)
            .set(CLUSTER.CONFIG_ID, CONFIG_ID_WITH_RESULT)
            .set(CLUSTER.TICKET_KEY, TICKET_KEY)
            .execute();

        service.getAndStoreResultsFromMarkup();

        List<PskuInClusterInfo> all = pskuInClusterInfoDao.findAll();
        assertThat(all)
            .extracting(PskuInClusterInfo::getState)
            .containsOnly(PskuInClusterState.PROCESSED);
        assertThat(all)
            .extracting(PskuInClusterInfo::getMarkupRawResult)
            .extracting(Markup.MskuFromPskuGenerationTaskResult::hasMappingStatus)
            .containsOnly(Boolean.TRUE);

        Map<Long, PskuInClusterInfo> map = all.stream().collect(Collectors.toMap(PskuInClusterInfo::getPskuId, Function.identity()));
        assertThat(map.get(PSKU_WITH_RESULT_4).getResultStatus()).isEqualTo(MskuFromPskuGenResultStatus.ALREADY_DELETED);
        Mockito.verify(mskuFromPskuGenerationTask, Mockito.times(1))
            .closeTicket(Mockito.any(), Mockito.any(), Mockito.eq(GenerationTaskType.CLUSTER));
    }

    @Test
    public void waitContentIsSavedCorrectly() {
        clusterPriorityService.refresh(Collections.singletonList(generateSimpleCluster(
            PSKU_WAIT_CONTENT
        )));
        pskuInClusterInfoDao.dsl()
            .update(PSKU_IN_CLUSTER_INFO)
            .set(PSKU_IN_CLUSTER_INFO.MARKUP_CONFIG_ID, CONFIG_ID_WITH_WAIT_CONTENT)
            .set(PSKU_IN_CLUSTER_INFO.STATE, PskuInClusterState.IN_PROCESS)
            .execute();
        clusterDao.dsl()
            .update(CLUSTER)
            .set(CLUSTER.CONFIG_ID, CONFIG_ID_WITH_WAIT_CONTENT)
            .set(CLUSTER.TICKET_KEY, TICKET_KEY)
            .execute();
        service.getAndStoreResultsFromMarkup();

        List<PskuInClusterInfo> all = pskuInClusterInfoDao.findAll();
        assertThat(all)
            .extracting(PskuInClusterInfo::getState)
            .containsOnly(PskuInClusterState.PROCESSED);
        assertThat(all)
            .extracting(PskuInClusterInfo::getMarkupRawResult)
            .extracting(Markup.MskuFromPskuGenerationTaskResult::hasMappingStatus)
            .containsOnly(Boolean.TRUE);
        List<PskuResultStorage> pskuResultStorageList = pskuResultStorageDao.fetchByPskuId(PSKU_WAIT_CONTENT);
        assertThat(pskuResultStorageList)
            .extracting(PskuResultStorage::getState).containsExactly(PskuStorageState.WAIT_CONTENT);

        Map<Long, PskuInClusterInfo> pskuInClusterInfoById = all.stream()
            .collect(Collectors.toMap(PskuInClusterInfo::getPskuId, Function.identity()));
        assertThat(pskuInClusterInfoById.get(PSKU_WAIT_CONTENT).getResultStatus())
            .isEqualTo(MskuFromPskuGenResultStatus.WAIT_CONTENT);
        Mockito.verify(mskuFromPskuGenerationTask, Mockito.times(1))
            .closeTicket(Mockito.any(), Mockito.any(), Mockito.eq(GenerationTaskType.CLUSTER));
    }

    private Markup.MskuFromPskuGenerationTaskResponse getMarkupMockResponse() {
        return Markup.MskuFromPskuGenerationTaskResponse.newBuilder()
            .setStatus(Markup.ResponseStatus.newBuilder()
                .setStatus(Markup.ResponseStatus.Status.OK)
                .build())
            .setTaskConfigId(CONFIG_ID_WITH_RESULT)
            .setUserLogin(TEST_USER_LOGIN)
            .addTaskResult(Markup.MskuFromPskuGenerationTaskResult.newBuilder()
                .setMappingStatus(Markup.MskuFromPskuGenerationTaskResult.MappingStatus.MAPPED)
                .setMskuId("11")
                .setPskuId(String.valueOf(PSKU_WITH_RESULT_1))
                .build())
            .addTaskResult(
                Markup.MskuFromPskuGenerationTaskResult.newBuilder()
                    .setMappingStatus(Markup.MskuFromPskuGenerationTaskResult.MappingStatus.MAPPED)
                    .setMskuId("22")
                    .setPskuId(String.valueOf(PSKU_WITH_RESULT_2))
                    .build())
            .addTaskResult(
                Markup.MskuFromPskuGenerationTaskResult.newBuilder()
                    .setMappingStatus(Markup.MskuFromPskuGenerationTaskResult.MappingStatus.MAPPED)
                    .setMskuId("33")
                    .setPskuId(String.valueOf(PSKU_WITH_RESULT_3))
                    .build())
            .addTaskResult(
                Markup.MskuFromPskuGenerationTaskResult.newBuilder()
                    .setMappingStatus(Markup.MskuFromPskuGenerationTaskResult.MappingStatus.UNDEFINED)
                    .setDeleted(true)
                    .setPskuId(String.valueOf(PSKU_WITH_RESULT_4))
                    .build())
            .build();
    }

    private Markup.MskuFromPskuGenerationTaskResponse getMarkupMockWaitContentResponse() {
        return Markup.MskuFromPskuGenerationTaskResponse.newBuilder()
            .setStatus(Markup.ResponseStatus.newBuilder()
                .setStatus(Markup.ResponseStatus.Status.OK)
                .build())
            .setTaskConfigId(CONFIG_ID_WITH_WAIT_CONTENT)
            .addTaskResult(Markup.MskuFromPskuGenerationTaskResult.newBuilder()
                .setMappingStatus(Markup.MskuFromPskuGenerationTaskResult.MappingStatus.TRASH)
                .addComments(Markup.MskuFromPskuGenerationTaskResult.Comment.newBuilder()
                .setType("FOR_REVISION"))
                .setPskuId(String.valueOf(PSKU_WAIT_CONTENT))
                .build())
            .build();
    }

    private Markup.MskuFromPskuGenerationTaskResponse getMarkupMockEmptyResponse() {
        return Markup.MskuFromPskuGenerationTaskResponse.newBuilder()
                .setStatus(Markup.ResponseStatus.newBuilder()
                        .setStatus(Markup.ResponseStatus.Status.OK)
                        .build())
                .setTaskConfigId(CONFIG_ID_WITH_EMPTY_RESULT)
                .build();
    }

    private Markup.MskuFromPskuGenerationTaskResponse getMarkupMockResponseWithUserLogin() {
        return Markup.MskuFromPskuGenerationTaskResponse.newBuilder()
            .setStatus(Markup.ResponseStatus.newBuilder()
                .setStatus(Markup.ResponseStatus.Status.OK)
                .build())
            .setTaskConfigId(CONFIG_ID_WITH_USER_LOGIN)
            .setUserLogin(TEST_USER_LOGIN)
            .addTaskResult(Markup.MskuFromPskuGenerationTaskResult.newBuilder()
                .setMappingStatus(Markup.MskuFromPskuGenerationTaskResult.MappingStatus.MAPPED)
                .setMskuId("11")
                .setPskuId(String.valueOf(REMAPPED_PSKU))
                .build())
            .build();
    }

    SimplePskuCluster generateSimpleCluster(Long... ids) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        JooqUtils.batchInsert(
            dsl(),
            Tables.PSKU_KNOWLEDGE,
            Arrays.asList(ids),
            (table, id) -> table
                .set(PSKU_KNOWLEDGE.ID, id)
                .set(PSKU_KNOWLEDGE.CREATION_TS, timestamp)
                .set(PSKU_KNOWLEDGE.LAST_UPDATE_TS, timestamp)
                .set(PSKU_KNOWLEDGE.SUPPLIER_ID, SUPPLIER_ID)
                .set(PSKU_KNOWLEDGE.PSKU_TITLE, "title" + id)
                .set(PSKU_KNOWLEDGE.SHOP_SKU, "shopSku" + id)
        );
        return new SimplePskuCluster(Arrays.asList(ids), CATEGORY_ID);
    }
}
