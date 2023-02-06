package ru.yandex.market.mbo.tms.model;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.convert.converter.Converter;

import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.TovarTreeDaoMock;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.HasModelIndexPayload;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.payload.YtModelIndexByGroupIdPayload;
import ru.yandex.market.mbo.db.rules.ModelRuleTaskService;
import ru.yandex.market.mbo.db.rules.ModelRuleTaskServiceImpl;
import ru.yandex.market.mbo.export.client.parameter.CategoryParametersServiceClient;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleSet;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleTask;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings({"checkstyle:magicnumber"})
public class ModelRuleTaskHandlerExecutorTest {
    private static final long GROUP_ID = 23L;
    private static final long CATEGORY_ID = 1L;
    private static final long CATEGORY_ID_2 = 2L;
    private static final long CATEGORY_ID_3 = 3L;

    private ModelRuleTaskHandlerExecutor taskHandlerExecutor;
    private final CategoryParametersServiceClient categoryParametersServiceClient =
        mock(CategoryParametersServiceClient.class);

    @Before
    public void setUp() {
        ParameterLoaderServiceStub parameterLoader = new ParameterLoaderServiceStub();
        ModelRuleTaskService ruleTaskService = Mockito.mock(ModelRuleTaskServiceImpl.class);
        ModelStorageServiceStub modelStorageService = new ModelStorageServiceStub() {
            @Override
            public void processQueryModels(MboIndexesFilter filter, Consumer<CommonModel> processor) {
                modelsMap.values().forEach(processor);
            }

            @Override
            public long count(MboIndexesFilter filter) {
                return modelsMap.size();
            }

            @Override
            public <T> Set<T> getFieldValues(MboIndexesFilter filter,
                                             Converter<HasModelIndexPayload, T> converterFromIndex) {
                return modelsMap.values().stream()
                    .map(ModelRuleTaskHandlerExecutorTest::buildPayload)
                    .map(p -> (T) p)
                    .collect(Collectors.toSet());
            }
        };

        TovarTreeDaoMock tovarTreeDao = new TovarTreeDaoMock();
        tovarTreeDao
            .addCategory(new TovarCategory("Root", KnownIds.GLOBAL_CATEGORY_ID, 0))
            .addCategory(new TovarCategory("GroupId", GROUP_ID, KnownIds.GLOBAL_CATEGORY_ID))
            .addCategory(CATEGORY_ID_3, GROUP_ID)
            .addCategory(CATEGORY_ID_2, CATEGORY_ID_3)
            .addCategory(CATEGORY_ID, CATEGORY_ID_2);
        tovarTreeDao.loadTovarTree().findByHid(KnownIds.GLOBAL_CATEGORY_ID)
            .foreach(x -> x.getData().setPublished(true));
        taskHandlerExecutor = new ModelRuleTaskHandlerExecutor(ruleTaskService, tovarTreeDao, modelStorageService,
            parameterLoader, categoryParametersServiceClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void execTaskCacheCleared() {
        ModelRuleTaskService ruleTaskService = mock(ModelRuleTaskServiceImpl.class);
        ModelRuleSet ruleSet = mock(ModelRuleSet.class);
        ModelRuleTask task = mock(ModelRuleTask.class);
        doReturn(ruleSet).when(task).getRuleSet();
        Set<CommonModel.Source> applicableModelTypes = new HashSet<>();
        applicableModelTypes.add(CommonModel.Source.SKU);
        applicableModelTypes.add(CommonModel.Source.GURU);
        doReturn(applicableModelTypes).when(ruleSet).getModelTypesAppliedTo();
        taskHandlerExecutor.execTask(task);
        verify(categoryParametersServiceClient, times(1)).invalidateCache();
    }

    private static YtModelIndexByGroupIdPayload buildPayload(CommonModel model) {
        return new YtModelIndexByGroupIdPayload(model.getId(), model.getCategoryId(), model.isDeleted(),
            model.getGroupId(), model.getParentModelId(), model.getCurrentType());
    }
}
