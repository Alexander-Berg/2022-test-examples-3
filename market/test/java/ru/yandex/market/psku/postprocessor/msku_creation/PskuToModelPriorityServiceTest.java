package ru.yandex.market.psku.postprocessor.msku_creation;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.mbo.export.CategorySizeMeasureService;
import ru.yandex.market.mbo.export.MboSizeMeasures;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuKnowledgeDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuResultStorageDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuToModelBatchDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuToModelDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuToModelInBatchInfoDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.MatchTarget;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuKnowledge;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuToModel;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuToModelInBatchInfo;
import ru.yandex.market.psku.postprocessor.config.CommonTestConfig;
import ru.yandex.market.psku.postprocessor.config.MskuCreationConfig;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;

@ContextConfiguration(classes = {
    CommonTestConfig.class,
    MskuCreationConfig.class,
})
public class PskuToModelPriorityServiceTest extends BaseDBTest {
    private static final long OK_CATEGORY_ID = 100L;
    private static final long OTHER_CATEGORY_ID = 101L;
    private static final long BAD_CATEGORY_ID = 110L;
    private static final long NO_KNOWLEDGE_CATEGORY_ID = 111L;
    private static final long BLACKLIST_CATEGORY_ID = 90710L;
    private static final long GROUPED_CATEGORY_ID = 15685457L;
    private PskuToModelPriorityService pskuToModelPriorityService;

    @Autowired
    PskuToModelDao pskuToModelDao;
    @Autowired
    PskuToModelInBatchInfoDao pskuToModelInBatchInfoDao;
    @Autowired
    PskuToModelBatchDao pskuToModelBatchDao;
    @Autowired
    PskuResultStorageDao pskuResultStorageDao;
    @Autowired
    PskuKnowledgeDao pskuKnowledgeDao;

    @Mock
    CategorySizeMeasureService categorySizeMeasureService;
    @Mock
    CategoryFormDownloader categoryFormDownloader;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        GenerationCategoryChecker generationCategoryChecker = initCategoryDataMocks();
        this.pskuToModelPriorityService = new PskuToModelPriorityService(
            pskuToModelDao,
            pskuToModelBatchDao,
            pskuToModelInBatchInfoDao,
            pskuResultStorageDao,
            generationCategoryChecker,
            pskuKnowledgeDao,
            jooqConfiguration);
    }

    private GenerationCategoryChecker initCategoryDataMocks() {
        GenerationCategoryChecker generationCategoryChecker =
            new GenerationCategoryChecker(categorySizeMeasureService, categoryFormDownloader);
        Mockito.when(categorySizeMeasureService.getSizeMeasuresInfo(Mockito.eq(MboSizeMeasures.GetSizeMeasuresInfoRequest.newBuilder()
            .addCategoryIds(OK_CATEGORY_ID)
            .build()))).thenReturn(MboSizeMeasures.GetSizeMeasuresInfoResponse.newBuilder()
            .build());
        Mockito.when(categorySizeMeasureService.getSizeMeasuresInfo(Mockito.eq(MboSizeMeasures.GetSizeMeasuresInfoRequest.newBuilder()
            .addCategoryIds(NO_KNOWLEDGE_CATEGORY_ID)
            .build()))).thenReturn(MboSizeMeasures.GetSizeMeasuresInfoResponse.newBuilder()
            .build());
        Mockito.when(categorySizeMeasureService.getSizeMeasuresInfo(Mockito.eq(MboSizeMeasures.GetSizeMeasuresInfoRequest.newBuilder()
            .addCategoryIds(BLACKLIST_CATEGORY_ID)
            .build()))).thenReturn(MboSizeMeasures.GetSizeMeasuresInfoResponse.newBuilder()
            .build());
        Mockito.when(categorySizeMeasureService.getSizeMeasuresInfo(Mockito.eq(MboSizeMeasures.GetSizeMeasuresInfoRequest.newBuilder()
            .addCategoryIds(GROUPED_CATEGORY_ID)
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
    public void filtersChangedCategories() {
        long okPskuId = 1L;
        long changedPskuId = 2L;
        long changedPskuId2 = 3L;
        PskuToModel pskuNormal = createBasicPskuToModel(okPskuId, 1L, OK_CATEGORY_ID);
        PskuToModel pskuWithChangedCategory = createBasicPskuToModel(changedPskuId, 1L, OK_CATEGORY_ID);
        PskuToModel pskuWithChangedCategory2 = createBasicPskuToModel(changedPskuId2, 2L, OK_CATEGORY_ID);
        List<PskuToModel> pskus = Arrays.asList(
            pskuNormal,
            pskuWithChangedCategory,
            pskuWithChangedCategory2
        );
        pskuToModelDao.insert(pskus);
        pskuKnowledgeDao.insert(new PskuKnowledge(changedPskuId, "", 1L, Timestamp.from(Instant.now()),
            Timestamp.from(Instant.now()), "", OTHER_CATEGORY_ID, "", "", 1L));
        pskuKnowledgeDao.insert(new PskuKnowledge(changedPskuId2, "", 1L, Timestamp.from(Instant.now()),
            Timestamp.from(Instant.now()), "", OTHER_CATEGORY_ID, "", "", 1L));
        pskuToModelPriorityService.refreshIfNeeded();
        List<PrioritizedPskuToModelBatch> prioritizedPskus =
            pskuToModelPriorityService.getPrioritizedPskus(2);

        Assertions.assertThat(prioritizedPskus.size()).isEqualTo(1);

        List<PskuToModelInBatchInfo> batch1 = prioritizedPskus.get(0).getPskus();

        Assertions.assertThat(batch1).extracting(PskuToModelInBatchInfo::getPskuId).containsExactlyInAnyOrder(okPskuId);
    }


    @Test
    public void filtersBlackListCategories() {
        long okPskuId = 1L;
        long blacklistPskuId = 2L;
        PskuToModel pskuNormal = createBasicPskuToModel(okPskuId, 1L, OK_CATEGORY_ID);
        PskuToModel pskuInBlackListCategory = createBasicPskuToModel(blacklistPskuId, 2L, BLACKLIST_CATEGORY_ID);
        List<PskuToModel> pskus = Arrays.asList(
            pskuNormal,
            pskuInBlackListCategory
        );
        pskuToModelDao.insert(pskus);

        pskuToModelPriorityService.refreshIfNeeded();
        List<PrioritizedPskuToModelBatch> prioritizedPskus =
            pskuToModelPriorityService.getPrioritizedPskus(2);

        Assertions.assertThat(prioritizedPskus.size()).isEqualTo(1);

        List<PskuToModelInBatchInfo> batch1 = prioritizedPskus.get(0).getPskus();

        Assertions.assertThat(batch1).extracting(PskuToModelInBatchInfo::getPskuId).containsExactlyInAnyOrder(okPskuId);
    }

    @Test
    public void allowsOnlyModificationsInGroupedCategories() {
        long pskuToModelId = 1L;
        long pskuToModificationId = 2L;
        PskuToModel pskuInGroupedCategoryModel =
            createBasicPskuToModel(pskuToModelId, 1L, GROUPED_CATEGORY_ID, MatchTarget.MODEL);
        PskuToModel pskuInGroupedCategoryModification =
            createBasicPskuToModel(pskuToModificationId, 2L, GROUPED_CATEGORY_ID, MatchTarget.MODIFICATION);
        List<PskuToModel> pskus = Arrays.asList(
            pskuInGroupedCategoryModel,
            pskuInGroupedCategoryModification
        );
        pskuToModelDao.insert(pskus);

        pskuToModelPriorityService.refreshIfNeeded();
        List<PrioritizedPskuToModelBatch> prioritizedPskus =
            pskuToModelPriorityService.getPrioritizedPskus(2);

        Assertions.assertThat(prioritizedPskus.size()).isEqualTo(1);

        List<PskuToModelInBatchInfo> batch1 = prioritizedPskus.get(0).getPskus();

        Assertions.assertThat(batch1).extracting(PskuToModelInBatchInfo::getPskuId)
            .containsExactlyInAnyOrder(pskuToModificationId);
    }

    @Test
    public void getPrioritizedPskus() {
        PskuToModel pskuWithBadCategory1 = createBasicPskuToModel(9L, 5L, BAD_CATEGORY_ID);
        PskuToModel pskuWithBadCategory2 = createBasicPskuToModel(10L, 5L, BAD_CATEGORY_ID);
        PskuToModel pskuWithBadCategory3 = createBasicPskuToModel(11L, 5L, BAD_CATEGORY_ID);
        List<PskuToModel> pskus = Arrays.asList(
            createBasicPskuToModel(1L, 3L),
            createBasicPskuToModel(2L, 1L),
            createBasicPskuToModel(3L, 1L),
            createBasicPskuToModel(4L, 1L),
            createBasicPskuToModel(5L, 6L),
            createBasicPskuToModel(6L, 2L),
            createBasicPskuToModel(7L, 2L),
            createBasicPskuToModel(8L, 4L),
            pskuWithBadCategory1,
            pskuWithBadCategory2,
            pskuWithBadCategory3
        );
        pskuToModelDao.insert(pskus);

        pskuToModelPriorityService.refreshIfNeeded();
        List<PrioritizedPskuToModelBatch> prioritizedPskus = pskuToModelPriorityService.getPrioritizedPskus(2);
        List<PskuToModelInBatchInfo> batch1 = prioritizedPskus.get(0).getPskus();
        List<PskuToModelInBatchInfo> batch2 = prioritizedPskus.get(1).getPskus();

        Assertions.assertThat(batch1).extracting(PskuToModelInBatchInfo::getPskuId).containsExactlyInAnyOrder(2L, 3L, 4L);
        Assertions.assertThat(batch2).extracting(PskuToModelInBatchInfo::getPskuId).containsExactlyInAnyOrder(6L, 7L);
    }

    @Test
    public void filtersNoKnowledgeCategories() {
        long okPskuId = 1L;
        long blacklistPskuId = 2L;
        PskuToModel pskuNormal = createBasicPskuToModel(okPskuId, 1L, OK_CATEGORY_ID);
        PskuToModel pskuInNoKnowledgeCategory = createBasicPskuToModel(blacklistPskuId, 2L, NO_KNOWLEDGE_CATEGORY_ID);
        List<PskuToModel> pskus = Arrays.asList(
            pskuNormal,
            pskuInNoKnowledgeCategory
        );
        pskuToModelDao.insert(pskus);

        pskuToModelPriorityService.refreshIfNeeded();
        List<PrioritizedPskuToModelBatch> prioritizedPskus =
            pskuToModelPriorityService.getPrioritizedPskus(2);

        Assertions.assertThat(prioritizedPskus.size()).isEqualTo(1);

        List<PskuToModelInBatchInfo> batch1 = prioritizedPskus.get(0).getPskus();

        Assertions.assertThat(batch1).extracting(PskuToModelInBatchInfo::getPskuId).containsExactlyInAnyOrder(okPskuId);
    }

    private PskuToModel createBasicPskuToModel(long pskuId, long modelId) {
        return createBasicPskuToModel(pskuId, modelId, OK_CATEGORY_ID);
    }

    private PskuToModel createBasicPskuToModel(long pskuId, long modelId, long categoryId) {
        createBasicPskuToModel(pskuId, modelId, categoryId, MatchTarget.MODEL);
        PskuToModel pskuToModel = new PskuToModel();
        pskuToModel.setPskuId(pskuId);
        pskuToModel.setModelId(modelId);
        pskuToModel.setPskuCategoryId(categoryId);
        pskuToModel.setModelCategoryId(categoryId);
        pskuToModel.setSessionId(123L);
        return pskuToModel;
    }

    private PskuToModel createBasicPskuToModel(
        long pskuId, long modelId, long categoryId, MatchTarget matchTarget) {
        PskuToModel pskuToModel = new PskuToModel();
        pskuToModel.setPskuId(pskuId);
        pskuToModel.setModelId(modelId);
        pskuToModel.setPskuCategoryId(categoryId);
        pskuToModel.setModelCategoryId(categoryId);
        pskuToModel.setMatchTarget(matchTarget);
        pskuToModel.setSessionId(123L);
        return pskuToModel;
    }
}