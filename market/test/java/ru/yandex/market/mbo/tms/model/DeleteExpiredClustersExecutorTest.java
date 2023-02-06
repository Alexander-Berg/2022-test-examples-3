package ru.yandex.market.mbo.tms.model;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.db.KeyValueMapService;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStoreInterfaceStub;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

/**
 * @author ivoralin
 */
public class DeleteExpiredClustersExecutorTest {
    private static final int YQL_BATCH_SIZE = 10;
    private final int firstCategorySize =  YQL_BATCH_SIZE + 1;
    private final int secondCategorySize = YQL_BATCH_SIZE + 2;
    private static final int DELETION_BATCH_SIZE = 4;
    private static final long ONE_MONTH = 1000 * 60 * 60 * 24 * 30;
    private static final long  SIX_MONTHS = 6 * ONE_MONTH;
    private DeleteExpiredClustersExecutor executor;
    private YqlModelJdbcTemplateMock yqlTemplateMock;
    private ModelStorageServiceStub modelStorageServiceStub;
    private ModelStoreInterfaceStub modelStoreStub;

    @Before
    public void setUp() {
        yqlTemplateMock = Mockito.spy(new YqlModelJdbcTemplateMock());
        yqlTemplateMock.setBatchSize(YQL_BATCH_SIZE);
        KeyValueMapService keyValueMapService = Mockito.mock(KeyValueMapService.class);
        modelStorageServiceStub = Mockito.spy(new ModelStorageServiceStub());
        modelStoreStub = Mockito.spy(new ModelStoreInterfaceStub(modelStorageServiceStub));
        executor = new DeleteExpiredClustersExecutor(modelStoreStub, yqlTemplateMock,
            "", keyValueMapService);
        executor.setYqlBatchSize(YQL_BATCH_SIZE);
        executor.setDeletionBatchSize(DELETION_BATCH_SIZE);
        executor = Mockito.spy(executor);
    }


    private List<Map<String, Object>> generateYqlAnswers(int size, long categoryId, long startModelId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int finalI = i;
            result.add(new HashMap<String, Object>() {{
                put("category_id", categoryId);
                put("model_id", startModelId + finalI); }});
        }
        return result;
    }

    private Map<Long, CommonModel> createCommonModelsMap(List<Map<String, Object>> ids) {
        Map<Long, CommonModel> map = new LinkedHashMap<>();
        for (Map<String, Object> expiredExecutor: ids) {
            long categoryId = (long) expiredExecutor.get("category_id");
            long modelId = (long) expiredExecutor.get("model_id");
            CommonModel commonModel = new CommonModel();
            commonModel.setCurrentType(Source.CLUSTER);
            commonModel.setId(modelId);
            commonModel.setCategoryId(categoryId);
            map.put(modelId, commonModel);
        }
        return map;
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void putTwoCategoriesInDB() {
        List<Map<String, Object>> objects1 = generateYqlAnswers(firstCategorySize, 1000, 4000);
        List<Map<String, Object>> objects2 = generateYqlAnswers(secondCategorySize, 1001, 5000);
        List<Map<String, Object>> objects = Stream.concat(objects1.stream(), objects2.stream())
            .collect(Collectors.toList());
        yqlTemplateMock.setObjects(objects);
        modelStorageServiceStub.setModelsMap(createCommonModelsMap(objects));
    }

    @Test
    public void checkIfYqlInBatches() throws Exception {
        putTwoCategoriesInDB();
        int numberOfObjects = firstCategorySize + secondCategorySize;
        executor.doRealJob(null);
        int numbOfBatches = (int) Math.ceil((double) numberOfObjects / YQL_BATCH_SIZE);
        // Checks if queryForList was called [numbOfBatches + 1] times
        // 1 time for each batch + 1 time to check if there is more batches
        Mockito.verify(yqlTemplateMock, Mockito.times(numbOfBatches + 1))
            .queryForList(Mockito.anyString());
    }

    @Test
    public void checkIfDeletionInBatches() throws Exception {
        putTwoCategoriesInDB();
        int numberOfObjects = firstCategorySize + secondCategorySize;
        executor.doRealJob(null);
        int numbOfFullYqlBatches = (int) Math.floor((double) numberOfObjects / YQL_BATCH_SIZE);
        int leftForLastBatch = numberOfObjects % YQL_BATCH_SIZE;
        int numbOfDeletionBatches = numbOfFullYqlBatches *
            (int) Math.ceil((double) YQL_BATCH_SIZE / DELETION_BATCH_SIZE)
            + (int) Math.ceil((double) leftForLastBatch / DELETION_BATCH_SIZE);
        Mockito.verify(modelStoreStub, Mockito.times(numbOfDeletionBatches))
            .saveClusters(Mockito.anyCollection());
    }

    @Test
    public void testDeletion() throws Exception {
        putTwoCategoriesInDB();
        executor.doRealJob(null);
        assertTrue(modelStorageServiceStub.getAllModels().stream().allMatch(CommonModel::isDeleted));
    }
}

