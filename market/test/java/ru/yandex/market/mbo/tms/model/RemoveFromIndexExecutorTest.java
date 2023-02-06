package ru.yandex.market.mbo.tms.model;


import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.db.KeyValueMapService;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.user.AutoUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

@SuppressWarnings("checkstyle:magicnumber")
public class RemoveFromIndexExecutorTest {
    private static final int YQL_BATCH_SIZE = 10;
    private final int firstCategorySize =  YQL_BATCH_SIZE + 1;
    private final int secondCategorySize = YQL_BATCH_SIZE + 2;
    private static final long ONE_MONTH = 1000 * 60 * 60 * 24 * 30;
    private static final long  YEAR = 12 * ONE_MONTH;
    private RemoveFromIndexExecutor executor;
    private YqlModelJdbcTemplateMock yqlTemplateMock;
    private ModelStorageServiceStub modelStorageServiceStub;

    @Before
    public void setUp() {
        yqlTemplateMock = Mockito.spy(new YqlModelJdbcTemplateMock());
        yqlTemplateMock.setBatchSize(YQL_BATCH_SIZE);
        KeyValueMapService keyValueMapService = Mockito.mock(KeyValueMapService.class);
        modelStorageServiceStub = Mockito.spy(new ModelStorageServiceStub());
        // Stubs are organized really strange...
        // ModelStoreInterfaceStub modelStore = Mockito.spy(new ModelStoreInterfaceStub(modelStorageServiceStub));
        executor = new RemoveFromIndexExecutor(yqlTemplateMock,
            "",
            keyValueMapService, modelStorageServiceStub, new AutoUser(42), "");
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
            commonModel.setId(modelId);
            commonModel.setCategoryId(categoryId);
            map.put(modelId, commonModel);
        }
        return map;
    }

    private void putTwoCategoriesInStorage() {
        List<Map<String, Object>> objects1 = generateYqlAnswers(firstCategorySize, 1000, 4000);
        List<Map<String, Object>> objects2 = generateYqlAnswers(secondCategorySize, 1001, 5000);
        List<Map<String, Object>> objects = Stream.concat(objects1.stream(), objects2.stream())
            .collect(Collectors.toList());
        yqlTemplateMock.setObjects(objects);
        modelStorageServiceStub.setModelsMap(createCommonModelsMap(objects));
    }

    @Test
    public void testArchiving() throws Exception {
        putTwoCategoriesInStorage();
        modelStorageServiceStub.getAllModels().forEach(
            m -> {
                m.setDeleted(true);
                m.setDeletedDate(new Date(Long.MIN_VALUE));
            });
        executor.doRealJob(null);
        assertTrue(modelStorageServiceStub.getAllModels().stream().allMatch(CommonModel::isArchived));
        assertTrue(modelStorageServiceStub.getAllModels().stream().allMatch(m -> m.getArchivedDate() != null));
    }
}
