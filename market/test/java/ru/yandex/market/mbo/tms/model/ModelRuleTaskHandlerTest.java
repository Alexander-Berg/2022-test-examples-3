package ru.yandex.market.mbo.tms.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.core.convert.converter.Converter;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.TovarTreeDaoMock;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.HasModelIndexPayload;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.payload.YtModelIndexByGroupIdPayload;
import ru.yandex.market.mbo.db.rules.ModelRuleQueueTask;
import ru.yandex.market.mbo.db.rules.ModelRuleTaskService;
import ru.yandex.market.mbo.db.rules.ModelRuleTaskServiceImpl;
import ru.yandex.market.mbo.db.utils.ParameterGenerator;
import ru.yandex.market.mbo.export.client.parameter.CategoryParametersServiceClient;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.rules.ModelProcessDetails;
import ru.yandex.market.mbo.gwt.models.rules.ModelProcessStatus;
import ru.yandex.market.mbo.gwt.models.rules.ModelRule;
import ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleResult;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleResultItem;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleSet;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleTask;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleTaskStatus;
import ru.yandex.market.mbo.gwt.models.rules.ValueHolder;
import ru.yandex.market.mbo.gwt.models.rules.ValueSource;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;

import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.gwt.models.rules.ModelProcessStatus.FULLY_ROLLED_BACK;
import static ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate.PredicateOperation.ADD_VALUE;
import static ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate.PredicateOperation.MATCHES;
import static ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate.PredicateType.IF;
import static ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate.PredicateType.THEN;

/**
 * @author yuramalinov
 * @created 09.01.19
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "checkstyle:magicnumber"})
public class ModelRuleTaskHandlerTest {
    private static final Logger log = LogManager.getLogger();
    private static final int TASK_RESUME_MAX_COUNT = 5;
    private static final long GROUP_ID = 23L;
    private static final long CATEGORY_ID = 1L;
    private static final long CATEGORY_ID_2 = 2L;
    private static final long CATEGORY_ID_3 = 3L;
    private static final long MODEL_ID = 301L;
    private static final long SKU_1_ID = MODEL_ID + 1;
    private static final long SKU_2_ID = MODEL_ID + 2;
    private static final Date START_DATE = new Date(
        ModelStorageServiceStub.LAST_MODIFIED_START.toInstant(ZoneOffset.UTC).toEpochMilli());

    @Rule
    public ParameterGenerator params = new ParameterGenerator();

    @Rule
    public final MockitoRule rule = MockitoJUnit.rule();

    private CategoryParametersServiceClient categoryParametersServiceClient =
        mock(CategoryParametersServiceClient.class);
    private ModelRuleTaskHandler taskHandler;
    private ModelRuleTaskService ruleTaskService;
    private ModelStorageServiceStub modelStorageService;
    private TovarTreeDaoMock tovarTreeDao;
    private Parameter listParam;
    private Parameter listParamSku;
    private Parameter paramA;
    private Parameter paramB;
    private long optionAId;
    private long optionAId1;

    @Before
    public void setUp() {
        ParameterLoaderServiceStub parameterLoader = new ParameterLoaderServiceStub();
        ruleTaskService = Mockito.mock(ModelRuleTaskServiceImpl.class);
        modelStorageService = new ModelStorageServiceStub() {
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
                    .map(ModelRuleTaskHandlerTest::buildPayload)
                    .map(p -> (T) p)
                    .collect(Collectors.toSet());
            }
        };

        tovarTreeDao = new TovarTreeDaoMock();
        tovarTreeDao
            .addCategory(new TovarCategory("Root", KnownIds.GLOBAL_CATEGORY_ID, 0))
            .addCategory(new TovarCategory("GroupId", GROUP_ID, KnownIds.GLOBAL_CATEGORY_ID))
            .addCategory(CATEGORY_ID_3, GROUP_ID)
            .addCategory(CATEGORY_ID_2, CATEGORY_ID_3)
            .addCategory(CATEGORY_ID, CATEGORY_ID_2);
        tovarTreeDao.loadTovarTree().findByHid(KnownIds.GLOBAL_CATEGORY_ID)
            .foreach(x -> x.getData().setPublished(true));

        taskHandler = new ModelRuleTaskHandler(tovarTreeDao, modelStorageService, ruleTaskService, parameterLoader,
            TASK_RESUME_MAX_COUNT, categoryParametersServiceClient);

        listParam = params.param("listParam", Param.Type.ENUM)
            .multifield(true)
            .option("A")
            .option("B")
            .get();
        listParamSku = params.param("listParamSku", Param.Type.ENUM)
            .multifield(true)
            .option("A1")
            .option("B1")
            .get();
        listParamSku.setSkuParameterMode(SkuParameterMode.SKU_INFORMATIONAL);

        optionAId = params.getOptionId("A");
        optionAId1 = params.getOptionId("A1");
        paramA = params.boolParam("paramA").get();
        paramB = params.boolParam("paramB").get();
        parameterLoader.addCategoryEntities(new CategoryEntities(CATEGORY_ID, emptyList())
            .addParameter(listParam)
            .addParameter(listParamSku)
            .addParameter(paramA)
            .addParameter(paramB));
    }

    /**
     * Test isn't that good, only tests that parent model isn't affected.
     * Full test with generalization is currently impossible as mock storage doesn't support generalization.
     */
    @Test
    public void testSkuIsNotTouched() throws Exception {
        CommonModel parent = createModel(MODEL_ID);
        CommonModel sku = createSku(SKU_1_ID, parent);

        modelStorageService.initializeWithModels(parent, sku);

        ModelRuleTask task = new ModelRuleTask();
        task.setStatus(ModelRuleTaskStatus.EXEC_ENQUEUED);

        configureTaskService(task, new ModelRuleSet()
            .setCategoryId(CATEGORY_ID)
            .setRules(singletonList(new ModelRule()
                .setName("name")
                .setIfs(new ModelRulePredicate(paramA.getId(), IF, MATCHES)
                    .setValueIds(singleton(params.getTrueOptionId(paramA))))
                .setThens(new ModelRulePredicate(listParam.getId(), THEN, ADD_VALUE)
                    .setValueIds(singleton(optionAId)))
            )), null);

        ModelRuleQueueTask queueTask = new ModelRuleQueueTask();
        queueTask.setModelRuleTaskId(task.getId());
        taskHandler.handle(queueTask, null);

        // Some checks, just that rule works fine
        assertThat(modelStorageService.getModel(CATEGORY_ID, MODEL_ID).get()
            .getParameterValues(listParam.getId()))
            .extracting(ParameterValue::getOptionId)
            .containsExactly(optionAId);

        // Check model
        assertThat(modelStorageService.getModel(CATEGORY_ID, MODEL_ID).get().getModificationDate())
            .isAfter(sku.getModificationDate());

        // Sku shouldn't be affected
        assertThat(modelStorageService.getModel(CATEGORY_ID, SKU_1_ID).get().getModificationDate())
            .isEqualTo(parent.getModificationDate());
    }

    @Test
    public void testTaskResumeExceedsResumesMaxCount() throws Exception {
        ModelRuleTask task = new ModelRuleTask();
        task.setStatus(ModelRuleTaskStatus.EXEC_INPROGRESS);
        task.setResumesCount(TASK_RESUME_MAX_COUNT);

        configureTaskService(task, null, null);

        ModelRuleQueueTask queueTask = new ModelRuleQueueTask();
        queueTask.setModelRuleTaskId(task.getId());
        taskHandler.handle(queueTask, null);

        verify(ruleTaskService).failTaskInProgress(eq(task.getId()), any());
    }

    @Test
    public void testExecResumeOk() throws Exception {
        execResumeOk(CATEGORY_ID);
    }

    @Test
    public void testExecResumeOkGroupId() throws Exception {
        execResumeOk(GROUP_ID);
    }

    private void execResumeOk(Long groupId) throws Exception {
        CommonModel parent = createModel(MODEL_ID);
        CommonModel sku1 = createSku(SKU_1_ID, parent);
        CommonModel sku2 = createSku(SKU_2_ID, parent);

        modelStorageService.initializeWithModels(parent, sku1, sku2);

        // create interrupted task
        ModelRuleTask task = new ModelRuleTask();
        task.setStatus(ModelRuleTaskStatus.EXEC_INPROGRESS);

        configureTaskService(task, new ModelRuleSet()
            .setCategoryId(groupId)
            .setRules(singletonList(new ModelRule()
                .setApplyToSKU(true)
                .setApplyToGuru(false)
                .setName("name")
                .setIfs(new ModelRulePredicate(paramA.getId(), IF, MATCHES)
                    .setValueIds(singleton(params.getTrueOptionId(paramA))))
                .setThens(new ModelRulePredicate(listParamSku.getId(), THEN, ADD_VALUE)
                    .setValueIds(singleton(optionAId1)))
            )), null);

        // first sku has been processed during previous interrupted session
        Map<Long, ModelProcessDetails> result = new HashMap<>();
        ModelProcessDetails modelProcessDetails = new ModelProcessDetails();
        modelProcessDetails.setModelId(sku1.getId());
        result.put(sku1.getId(), modelProcessDetails);
        when(ruleTaskService.processTaskModels(anyLong(), anyList())).thenReturn(result);

        ModelRuleQueueTask queueTask = new ModelRuleQueueTask();
        queueTask.setModelRuleTaskId(task.getId());
        taskHandler.handle(queueTask, null);

        // check task stats
        verify(ruleTaskService).registerTaskResume(any());
        verify(ruleTaskService, times(2))
            .registerModelProcessResult(anyLong(), any(), any(), any(), any(), any());

        // first sku should be left untouched as already processed
        assertThat(
            modelStorageService.getModel(CATEGORY_ID, SKU_1_ID).get().getParameterValues(listParamSku.getId()))
            .isNullOrEmpty();
        assertThat(modelStorageService.getModel(CATEGORY_ID, SKU_1_ID).get().getModificationDate())
            .isEqualTo(sku1.getModificationDate());

        // second sku should be modified
        assertThat(
            modelStorageService.getModel(CATEGORY_ID, SKU_2_ID).get().getParameterValues(listParamSku.getId()))
            .extracting(ParameterValue::getOptionId)
            .containsExactly(optionAId1);
        assertThat(modelStorageService.getModel(CATEGORY_ID, SKU_2_ID).get().getModificationDate())
            .isAfter(sku2.getModificationDate());

        // Parent shouldn't be affected
        assertThat(modelStorageService.getModel(CATEGORY_ID, MODEL_ID).get().getModificationDate())
            .isEqualTo(parent.getModificationDate());
    }


    @Test
    public void testRollbackResumeOk() throws Exception {
        rollbackResumeOk(CATEGORY_ID);
    }

    @Test
    public void testRollbackResumeOkGroupId() throws Exception {
        rollbackResumeOk(GROUP_ID);
    }

    private void rollbackResumeOk(Long categoryHid) throws Exception {
        CommonModel parent = createModel(MODEL_ID);
        CommonModel mod1 = createSku(SKU_1_ID, parent);
        CommonModel mod2 = createSku(SKU_2_ID, parent);
        mod1.addParameterValue(new ParameterValue(listParam, optionAId));
        mod2.addParameterValue(new ParameterValue(listParam, optionAId));

        modelStorageService.initializeWithModels(parent, mod1, mod2);

        // create interrupted rollback task
        ModelRuleTask task = new ModelRuleTask();
        task.setStatus(ModelRuleTaskStatus.ROLLBACK_INPROGRESS);

        configureTaskService(task,
            new ModelRuleSet()
                .setCategoryId(categoryHid)
                .setRules(singletonList(
                    new ModelRule()
                        .setName("name")
                        .setIfs(new ModelRulePredicate()
                            .setOperation(MATCHES)
                            .setValueIds(singleton(params.getTrueOptionId(paramA)))
                            .setValueHolder(new ValueHolder(ValueSource.MODEL_PARAMETER, paramA.getId())))
                        .setThens(new ModelRulePredicate()
                            .setOperation(ADD_VALUE)
                            .setValueHolder(new ValueHolder(ValueSource.MODEL_PARAMETER, listParam.getId()))
                            .setValueIds(singleton(optionAId)))
                )),
            new ModelRuleResult()
                .addItem(new ModelRuleResultItem(
                    ValueSource.MODEL_PARAMETER, listParam.getId(), listParam.getType(),
                    ParameterValues.of(listParam), ParameterValues.of(new ParameterValue(listParam, optionAId)))
                ));

        // first modification has been processed during previous interrupted rollback
        // return second modification
        Map<Long, ModelProcessDetails> result = new HashMap<>();
        ModelProcessDetails modelProcessDetails = new ModelProcessDetails();
        modelProcessDetails.setModelId(mod2.getId());
        result.put(mod2.getId(), modelProcessDetails);
        when(ruleTaskService.processTaskModels(anyLong(), anyList())).thenReturn(result);

        ModelRuleQueueTask queueTask = new ModelRuleQueueTask();
        queueTask.setModelRuleTaskId(task.getId());
        taskHandler.handle(queueTask, null);

        // check task stats
        verify(ruleTaskService).registerTaskResume(any());
        verify(ruleTaskService).registerModelRollbackResult(any(), eq(FULLY_ROLLED_BACK), any(), anyList(), anyLong());

        // first modification should be left untouched as already processed
        assertThat(
            modelStorageService.getModel(CATEGORY_ID, SKU_1_ID).get()
                .getParameterValues(listParam.getId()))
            .extracting(ParameterValue::getOptionId)
            .containsExactly(optionAId);
        assertThat(modelStorageService.getModel(CATEGORY_ID, SKU_1_ID).get().getModificationDate())
            .isEqualTo(mod1.getModificationDate());

        // second modification should be rolled back
        assertThat(
            modelStorageService.getModel(CATEGORY_ID, SKU_2_ID).get()
                .getParameterValues(listParam.getId()))
            .isNullOrEmpty();
        assertThat(modelStorageService.getModel(CATEGORY_ID, SKU_2_ID).get().getModificationDate())
            .isAfter(mod2.getModificationDate());

        // Parent shouldn't be affected
        assertThat(modelStorageService.getModel(CATEGORY_ID, MODEL_ID).get().getModificationDate())
            .isEqualTo(parent.getModificationDate());
    }

    private CommonModel createModel(long modelId) {
        CommonModel model = new CommonModel();
        model.setId(modelId);
        model.setGroupId(modelId);
        model.setCategoryId(CATEGORY_ID);
        model.setModificationDate(START_DATE);
        model.addParameterValue(new ParameterValue(paramA, true, params.getTrueOptionId(paramA)));
        model.setCurrentType(CommonModel.Source.GURU);
        return model;
    }

    private CommonModel createSku(long id, CommonModel parent) {
        CommonModel sku = createModel(id);
        sku.setGroupId(parent.getId());
        sku.setCurrentType(CommonModel.Source.SKU);
        sku.setCategoryId(parent.getCategoryId());
        sku.addParameterValue(new ParameterValue(paramB, true, params.getTrueOptionId(paramB)));
        parent.addRelation(new ModelRelation(id, parent.getCategoryId(), ModelRelation.RelationType.SKU_MODEL));
        sku.addRelation(
            new ModelRelation(parent.getId(), parent.getCategoryId(), ModelRelation.RelationType.SKU_PARENT_MODEL));

        return sku;
    }

    private void configureTaskService(ModelRuleTask task, ModelRuleSet ruleSet, ModelRuleResult ruleResult) {
        task.setRuleSet(ruleSet);
        when(ruleTaskService.registerModelProcessResult(anyLong(), any(), any(), any(), any(), any()))
            .then(call -> {
                log.info("registerModelProcessResult({})", Arrays.toString(call.getArguments()));
                if (call.getArgument(3) == null) {
                    log.info("Processed model {} no modification", ((CommonModel) call.getArgument(1)).getId());
                } else {
                    assertThat((ModelProcessStatus) call.getArgument(2)).isEqualTo(ModelProcessStatus.APPLIED);
                }
                return ModelRuleTaskStatus.EXEC_INPROGRESS;
            });

        when(ruleTaskService.registerModelRollbackResult(any(), any(), any(), anyList(), anyLong()))
            .then(call -> {
                log.info("registerModelRollbackResult({})", Arrays.toString(call.getArguments()));
                assertThat((ModelProcessStatus) call.getArgument(1))
                    .isEqualTo(FULLY_ROLLED_BACK);
                return ModelRuleTaskStatus.ROLLBACK_INPROGRESS;
            });

        when(ruleTaskService.getModelChangedParameters(anyLong(), anyLong()))
            .thenReturn(ruleResult);

        Mockito.when(ruleTaskService.getTask(anyLong(), anyBoolean()))
            .thenReturn(task);
    }

    private static YtModelIndexByGroupIdPayload buildPayload(CommonModel model) {
        return new YtModelIndexByGroupIdPayload(model.getId(), model.getCategoryId(), model.isDeleted(),
            model.getGroupId(), model.getParentModelId(), model.getCurrentType());
    }

    @Test
    public void mergeToGroupByBachSize() {
        Map<Long, Set<Long>> modelIdsByGroupId = new HashMap<>();
        modelIdsByGroupId.put(1L, generateSet(10));
        modelIdsByGroupId.put(2L, generateSet(10));
        modelIdsByGroupId.put(3L, generateSet(10));
        modelIdsByGroupId.put(4L, generateSet(500));
        modelIdsByGroupId.put(5L, generateSet(500));
        modelIdsByGroupId.put(6L, generateSet(1020));
        modelIdsByGroupId.put(7L, generateSet(500));
        modelIdsByGroupId.put(8L, generateSet(500));
        modelIdsByGroupId.put(9L, generateSet(10));

        List<Set<Long>> result = ModelRuleTaskHandlerExecutor.mergeToGroupByBachSize(modelIdsByGroupId);

        assertThat(result.size()).isEqualTo(5);
        assertThat(result.get(0).size()).isEqualTo(530);
        assertThat(result.get(1).size()).isEqualTo(500);
        assertThat(result.get(2).size()).isEqualTo(1020);
        assertThat(result.get(3).size()).isEqualTo(1000);
        assertThat(result.get(4).size()).isEqualTo(10);
    }

    private Set<Long> generateSet(int size) {
        return new Random().longs(size).boxed().collect(Collectors.toSet());
    }
}

