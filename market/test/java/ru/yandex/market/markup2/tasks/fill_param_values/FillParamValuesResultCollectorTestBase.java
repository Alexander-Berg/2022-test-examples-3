package ru.yandex.market.markup2.tasks.fill_param_values;

import org.junit.Assert;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import ru.yandex.market.markup2.utils.Markup2TestUtils;
import ru.yandex.market.markup2.utils.ModelTestUtils;
import ru.yandex.market.markup2.utils.email.EmailService;
import ru.yandex.market.markup2.utils.model.ModelStorageService;
import ru.yandex.market.markup2.utils.param.ParamUtils;
import ru.yandex.market.markup2.workflow.general.AbstractTaskDataItemPayload;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.resultMaker.ResultMakerContext;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 16.06.2017
 */
public abstract class FillParamValuesResultCollectorTestBase<I extends FillParamValuesModelIdentity,
    D extends AbstractTaskDataItemPayload<I>, R extends IFillParamValuesResponse> {

    private static final int CATEGORY_ID = 42;
    private static final int MODELS_COUNT = 15;
    private static final String NEW_PARAMETER_VALUE = "value";
    private static final Random RANDOM = new Random();

    private final List<MboParameters.Parameter> categoryParameters = ParametersData.generateParameters();
    protected final List<ModelStorage.Model> models = generateModels(categoryParameters);
    private final Map<Long, ModelStorage.Model> modelMap = models.stream()
        .collect(Collectors.toMap(ModelStorage.Model::getId, Function.identity()));

    @Mock
    private ModelStorageService modelStorageService;

    @Mock
    private ParamUtils paramUtils;

    @Mock
    protected EmailService emailService;

    @Captor
    private ArgumentCaptor<List<ModelStorage.Model>> modelsCaptor;

    private final AbstractParamValuesResultCollector<I, D, R> responseCollector =
        createResultCollector();

    protected abstract AbstractParamValuesResultCollector<I, D, R> createResultCollector();

    @Before
    public void setup() {
        ModelTestUtils.mockModelStorageSaveModelsWithForce(modelStorageService, modelMap.keySet());

        when(paramUtils.getUsedModelsMap(anyInt(), anySet())).thenReturn(modelMap);
        when(paramUtils.getAllParams(anyInt())).thenReturn(categoryParameters);

        responseCollector.setModelStorageService(modelStorageService);
        responseCollector.setParamUtils(paramUtils);
        responseCollector.setEmailService(emailService);
    }

    private ResultMakerContext<I, D, R>
        createResultMakerContext() {
        Collection<TaskDataItem<D, R>> items = generateItems();
        ResultMakerContext<I, D, R> resultMakerContext =
            Markup2TestUtils.createResultMakerContext(
                Markup2TestUtils.createBasicTaskInfo(CATEGORY_ID, MODELS_COUNT, Collections.emptyMap())
            );

        resultMakerContext.addDataItems(items);

        return resultMakerContext;
    }

    protected void modelsFromResponseSaved() {
        ResultMakerContext<I, D, R> resultMakerContext =
            createResultMakerContext();

        responseCollector.makeResults(resultMakerContext);

        Collection<TaskDataItem<D, R>> items =
            resultMakerContext.getTaskDataItems();

        Set<Long> modelIds = getExpectedSavedModelIds(
            items.iterator().next().getInputData().getDataIdentifier().getModelId(), items);

        verify(modelStorageService).saveModels(modelsCaptor.capture(), anyBoolean());
        List<ModelStorage.Model> savedModels = modelsCaptor.getValue();

        Set<Long> saveModelIds = savedModels.stream()
            .map(ModelStorage.Model::getId)
            .collect(Collectors.toSet());

        Assert.assertEquals(modelIds, saveModelIds);
        Assert.assertEquals(modelIds.size(), savedModels.size());
    }

    protected void replaceOnlyTolokerValues() {
        ResultMakerContext<I, D, R> resultMakerContext =
            createResultMakerContext();

        responseCollector.makeResults(resultMakerContext);

        verify(modelStorageService).saveModels(modelsCaptor.capture(), anyBoolean());
        List<ModelStorage.Model> savedModels = modelsCaptor.getValue();

        for (ModelStorage.Model modelAfter : savedModels) {
            ModelStorage.Model modelBefore = modelMap.get(modelAfter.getId());

            List<ModelStorage.ParameterValue> operatorValuesBefore = modelBefore.getParameterValuesList().stream()
                    .filter(pv -> pv.getValueSource() != ModelStorage.ModificationSource.TOLOKER)
                    .collect(Collectors.toList());

            Map<Long, ModelStorage.ParameterValue> valuesAfterMap = modelAfter.getParameterValuesList().stream()
                .collect(Collectors.toMap(ModelStorage.ParameterValue::getParamId, Function.identity()));

            for (ModelStorage.ParameterValue valueBefore : operatorValuesBefore) {
                ModelStorage.ParameterValue valueAfter = valuesAfterMap.get(valueBefore.getParamId());
                Assert.assertEquals(valueBefore, valueAfter);
            }

            Set<Long> operatorValuesBeforeIds = operatorValuesBefore.stream()
                .map(ModelStorage.ParameterValue::getParamId)
                .collect(Collectors.toSet());

            valuesAfterMap.keySet().removeAll(operatorValuesBeforeIds);
            for (ModelStorage.ParameterValue value : valuesAfterMap.values()) {
                Assert.assertEquals(getExpectedModificationSource(), value.getValueSource());
            }
        }
    }

    private Collection<TaskDataItem<D, R>> generateItems() {
        return generateItems(MODELS_COUNT);
    }

    protected abstract Collection<TaskDataItem<D, R>> generateItems(int count);

    protected abstract R createResponse(int number);

    protected Set<Long> getExpectedSavedModelIds(long firstModelId,
                                                 Collection<TaskDataItem<D, R>> items) {
        return items.stream()
            .map(TaskDataItem::getInputData)
            .map(payload -> payload.getDataIdentifier().getModelId())
            .collect(Collectors.toSet());
    }

    protected ModelStorage.ModificationSource getExpectedModificationSource() {
        return ModelStorage.ModificationSource.TOLOKER;
    }

    protected List<Map<String, Object>> generateCharacteristics() {
        Map<String, Object> characteristics = new HashMap<>();
        for (MboParameters.Parameter parameter : categoryParameters) {
            characteristics.put(parameter.getXslName(), NEW_PARAMETER_VALUE + RANDOM.nextInt());
        }

        return Collections.singletonList(characteristics);
    }

    private List<ModelStorage.Model> generateModels(List<MboParameters.Parameter> categoryParameters) {
        List<ModelStorage.Model> generatedModels =
                ModelsData.generateModels(CATEGORY_ID, categoryParameters, MODELS_COUNT);

        return generatedModels.stream()
            .map(ModelStorage.Model::toBuilder)
            .map(b -> {
                List<ModelStorage.ParameterValue.Builder> vbList = b.getParameterValuesBuilderList();
                for (ModelStorage.ParameterValue.Builder vb : vbList) {
                    vb.setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED);
                }
                if (vbList.size() > 1) {
                    vbList.get(0).setValueSource(getExpectedModificationSource());
                }
                return b;
            })
            .map(ModelStorage.Model.Builder::build)
            .collect(Collectors.toList());
    }
}
