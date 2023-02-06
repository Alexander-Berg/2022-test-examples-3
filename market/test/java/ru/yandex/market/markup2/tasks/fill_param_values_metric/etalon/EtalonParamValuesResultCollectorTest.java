//package ru.yandex.market.markup2.tasks.fill_param_values_metric.etalon;
//
//import com.google.common.collect.ImmutableMap;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Captor;
//import org.mockito.Mock;
//import org.mockito.junit.MockitoJUnitRunner;
//import ru.yandex.market.markup2.entries.group.ModelTypeValue;
//import ru.yandex.market.markup2.entries.group.PublishingValue;
//import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesIdentity;
//import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesResponse;
//import ru.yandex.market.markup2.utils.Markup2TestUtils;
//import ru.yandex.market.markup2.utils.ModelTestUtils;
//import ru.yandex.market.markup2.utils.email.EmailService;
//import ru.yandex.market.markup2.utils.param.ParamUtils;
//import ru.yandex.market.markup2.workflow.general.TaskDataItem;
//import ru.yandex.market.markup2.workflow.resultMaker.ResultMakerContext;
//import ru.yandex.market.mbo.export.MboParameters;
//import ru.yandex.market.mbo.http.ModelStorage;
//
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//import static org.junit.Assert.fail;
//import static org.mockito.ArgumentMatchers.anyBoolean;
//import static org.mockito.ArgumentMatchers.anyList;
//import static org.mockito.ArgumentMatchers.anySet;
//import static org.mockito.Matchers.anyInt;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//import static ru.yandex.market.markup2.utils.ParameterTestUtils.verifyModelParameter;
//import static ru.yandex.market.markup2.utils.ParameterTestUtils.verifyNoParameter;
//
///**
// * @author anmalysh
// */
//@SuppressWarnings("checkstyle:MagicNumber")
//@RunWith(MockitoJUnitRunner.class)
//public class EtalonParamValuesResultCollectorTest extends EtalonParamValuesTestBase {
//
//    private EtalonParamValuesResultCollector responseCollector;
//
//    private final Map<Long, ModelStorage.Model> modelMap = models.stream()
//        .collect(Collectors.toMap(ModelStorage.Model::getId, Function.identity()));
//
//    @Mock
//    EmailService emailService;
//
//    @Captor
//    ArgumentCaptor<List<ModelStorage.Model>> modelsCaptor;
//
//    @Captor
//    ArgumentCaptor<String> emailTitleCaptor;
//
//    @Captor
//    ArgumentCaptor<String> emailTextCaptor;
//
//    @Before
//    public void setup() {
//        super.setup(false);
//
//        ModelTestUtils.mockModelStorageSaveModelsWithForce(modelStorageService, modelMap.keySet());
//
//        when(paramUtils.getUsedModelsMap(anyInt(), anySet())).thenAnswer(i -> {
//            Set<Long> modelIds = i.getArgument(1);
//            Map<Long, ModelStorage.Model> result = new HashMap<>(modelMap);
//            result.keySet().retainAll(modelIds);
//            return result;
//        });
//
//        when(paramUtils.getAllParams(anyInt())).thenReturn(categoryParameters);
//
//        responseCollector = new EtalonParamValuesResultCollector();
//        responseCollector.setModelStorageService(modelStorageService);
//        responseCollector.setParamUtils(paramUtils);
//        responseCollector.setEmailService(emailService);
//    }
//
//    private ResultMakerContext<FillParamValuesIdentity, EtalonParamValuesDataItemPayload, FillParamValuesResponse>
//    processResponse(FillParamValuesResponse response) {
//        Collection<TaskDataItem<EtalonParamValuesDataItemPayload, FillParamValuesResponse>> items =
//            generateRequests(1, ModelTypeValue.GURU, PublishingValue.UNPUBLISHED);
//        ResultMakerContext<FillParamValuesIdentity, EtalonParamValuesDataItemPayload, FillParamValuesResponse>
//            resultMakerContext = Markup2TestUtils.createResultMakerContext(
//                Markup2TestUtils.createBasicTaskInfo(CATEGORY_ID, 10, Collections.emptyMap())
//            );
//
//        items.iterator().next().setResponseInfo(response);
//        resultMakerContext.addDataItems(items);
//
//        responseCollector.makeResults(resultMakerContext);
//
//        return resultMakerContext;
//    }
//
//    @Test
//    public void testSuccessfulSave() {
//        List<Map<String, Object>> characteristics = Arrays.asList(
//            ImmutableMap.of(customParam1.getXslName(), 10, customParam2.getXslName(), "Value1"),
//            ImmutableMap.of(customParam1.getXslName(), 10, customParam2.getXslName(), "Value1"),
//            ImmutableMap.of(customParam1.getXslName(), 10, customParam2.getXslName(), "Value2")
//        );
//        FillParamValuesResponse response = new FillParamValuesResponse(1, characteristics, Collections.emptyList());
//
//        ResultMakerContext<FillParamValuesIdentity, EtalonParamValuesDataItemPayload, FillParamValuesResponse> context =
//            processResponse(response);
//
//        verifyModelOne(context, ImmutableMap.of(customParam1.getId(), 10, customParam2.getId(), 10));
//    }
//
//    @Test
//    public void testConflicts() {
//        List<Map<String, Object>> characteristics = Arrays.asList(
//            ImmutableMap.of(customParam1.getXslName(), 10, customParam2.getXslName(), "Value1"),
//            ImmutableMap.of(customParam1.getXslName(), 20, customParam2.getXslName(), "Value2"),
//            ImmutableMap.of(customParam1.getXslName(), 15)
//        );
//        FillParamValuesResponse response = new FillParamValuesResponse(1, characteristics, Collections.emptyList());
//
//        ResultMakerContext<FillParamValuesIdentity, EtalonParamValuesDataItemPayload, FillParamValuesResponse> context =
//            processResponse(response);
//
//        verifyModelOne(context, ImmutableMap.of(
//            customParam1.getId(), ParamUtils.NO_VALUE, customParam2.getId(), ParamUtils.NO_VALUE));
//    }
//
//    @Test
//    public void testNoValue() {
//        List<Map<String, Object>> characteristics = Arrays.asList(
//            ImmutableMap.of(customParam1.getXslName(), 10, customParam2.getXslName(), ParamUtils.NO_VALUE),
//            ImmutableMap.of(customParam1.getXslName(), 10, customParam2.getXslName(), ParamUtils.NO_VALUE),
//            ImmutableMap.of(customParam1.getXslName(), ParamUtils.NO_VALUE, customParam2.getXslName(), "Value1")
//        );
//        FillParamValuesResponse response = new FillParamValuesResponse(1, characteristics, Collections.emptyList());
//
//        ResultMakerContext<FillParamValuesIdentity, EtalonParamValuesDataItemPayload, FillParamValuesResponse> context =
//            processResponse(response);
//
//        verifyModelOne(context, ImmutableMap.of(
//            customParam1.getId(), 1, customParam2.getId(), ParamUtils.NO_VALUE));
//    }
//
//    @Test
//    public void testEmptyCharacteristics() {
//        List<Map<String, Object>> characteristics = Collections.emptyList();
//        FillParamValuesResponse response = new FillParamValuesResponse(1, characteristics, Collections.emptyList());
//
//        ResultMakerContext<FillParamValuesIdentity, EtalonParamValuesDataItemPayload, FillParamValuesResponse> context =
//            processResponse(response);
//
//        verify(modelStorageService, never()).saveModels(anyList());
//    }
//
//    @Test
//    public void testNewValue() {
//        List<Map<String, Object>> characteristics = Arrays.asList(
//            ImmutableMap.of(customParam2.getXslName(), "Value3"),
//            ImmutableMap.of(customParam2.getXslName(), "Value3"),
//            ImmutableMap.of(customParam2.getXslName(), "Value3")
//        );
//        FillParamValuesResponse response = new FillParamValuesResponse(1, characteristics, Collections.emptyList());
//
//        ResultMakerContext<FillParamValuesIdentity, EtalonParamValuesDataItemPayload, FillParamValuesResponse> context =
//            processResponse(response);
//
//        verifyModelOne(context, ImmutableMap.of(customParam2.getId(), ParamUtils.NO_VALUE));
//
//        verify(emailService).sendSafe(emailTitleCaptor.capture(), emailTextCaptor.capture());
//
//        String title = emailTitleCaptor.getValue();
//        String text = emailTextCaptor.getValue();
//
//        assertTrue(title.contains(String.valueOf(CATEGORY_ID)));
//        assertTrue(text.contains(String.valueOf(customParam2.getId())));
//        assertTrue(text.contains("Value3"));
//    }
//
//    @Test
//    public void testExistingValue() {
//        List<Map<String, Object>> characteristics = Arrays.asList(
//            ImmutableMap.of(customParam1.getXslName(), 10),
//            ImmutableMap.of(customParam1.getXslName(), 10),
//            ImmutableMap.of(customParam1.getXslName(), 10)
//        );
//
//        models.remove(model1);
//        ModelStorage.Model model1Updated = model1.toBuilder()
//            .addParameterValues(ModelTestUtils.createNumericValue(customParam1, "100")
//                .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED).build()).build();
//        models.add(model1Updated);
//
//        FillParamValuesResponse response = new FillParamValuesResponse(1, characteristics, Collections.emptyList());
//
//        ResultMakerContext<FillParamValuesIdentity, EtalonParamValuesDataItemPayload, FillParamValuesResponse> context =
//            processResponse(response);
//
//        verifyModelOne(context, ImmutableMap.of(customParam1.getId(), 100));
//
//        models.remove(model1Updated);
//        models.add(model1);
//    }
//
//    private void verifyModelOne(
//        ResultMakerContext<FillParamValuesIdentity, EtalonParamValuesDataItemPayload, FillParamValuesResponse> context,
//        Map<Long, Object> expectedValues
//    ) {
//        verify(modelStorageService).saveModels(modelsCaptor.capture(), anyBoolean());
//        List<ModelStorage.Model> savedModels = modelsCaptor.getValue();
//
//        assertEquals(1, savedModels.size());
//
//        verifyModel(savedModels, 1L, expectedValues);
//
//        assertEquals(0, context.getRetryStatuses().size());
//        assertEquals(0, context.getFailureStatuses().size());
//    }
//
//    private void verifyModel(List<ModelStorage.Model> savedModels, Long id, Map<Long, Object> values) {
//        Map<Long, ModelStorage.Model> models = savedModels.stream()
//            .collect(Collectors.toMap(ModelStorage.Model::getId, Function.identity()));
//
//        ModelStorage.Model model = models.get(id);
//        if (model == null) {
//            fail("Model not saved: " + id);
//        }
//
//        Map<Long, MboParameters.Parameter> parameterMap = categoryParameters.stream()
//            .collect(Collectors.toMap(MboParameters.Parameter::getId, Function.identity()));
//
//        for (Map.Entry<Long, Object> expectedValue : values.entrySet()) {
//            MboParameters.Parameter param = parameterMap.get(expectedValue.getKey());
//            if (ParamUtils.NO_VALUE.equals(expectedValue.getValue())) {
//                verifyNoParameter(model, expectedValue.getKey());
//            } else {
//                verifyModelParameter(model, param, expectedValue.getValue());
//            }
//        }
//    }
//}
