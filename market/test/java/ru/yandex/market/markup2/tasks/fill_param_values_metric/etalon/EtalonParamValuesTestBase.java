package ru.yandex.market.markup2.tasks.fill_param_values_metric.etalon;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import ru.yandex.market.markup2.entries.group.ModelIds;
import ru.yandex.market.markup2.entries.group.ModelTypeValue;
import ru.yandex.market.markup2.entries.group.ParameterType;
import ru.yandex.market.markup2.entries.group.PublishingValue;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesDataAttributes;
import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesDataItemPayload;
import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesIdentity;
import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesRequestGeneratorTestBase;
import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesResponse;
import ru.yandex.market.markup2.utils.Markup2TestUtils;
import ru.yandex.market.markup2.utils.ModelTestUtils;
import ru.yandex.market.markup2.utils.param.ParamUtils;
import ru.yandex.market.markup2.utils.vendor.Vendor;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.generation.RequestGeneratorContext;
import ru.yandex.market.markup2.workflow.taskDataUnique.FullTaskDataUniqueContext;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.markup2.tasks.fill_param_values.ModelsData.VENDOR_OPTION_ID;
import static ru.yandex.market.markup2.utils.ModelTestUtils.createModel;
import static ru.yandex.market.markup2.utils.ModelTestUtils.createModelBuilder;
import static ru.yandex.market.markup2.utils.ModelTestUtils.mockModelStorageConditionalProcessModels;
import static ru.yandex.market.markup2.utils.ParameterTestUtils.createOption;
import static ru.yandex.market.markup2.utils.ParameterTestUtils.createParameter;
import static ru.yandex.market.markup2.utils.ParameterTestUtils.createParameterBuilder;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 15.06.2017
 */
public class EtalonParamValuesTestBase extends FillParamValuesRequestGeneratorTestBase {

    static final String SHOP_SITE = "http://site.ru";

    protected MboParameters.Parameter customParam1 = createParameter(1, MboParameters.ValueType.NUMERIC);
    protected MboParameters.Parameter customParam2 =
        createParameterBuilder(2, MboParameters.ValueType.ENUM, "xsl_name2")
            .addOption(createOption(10, "Value1"))
            .addOption(createOption(20, "Value2"))
            .setImportant(true)
            .build();

    protected final List<MboParameters.Parameter> categoryParameters = Arrays.asList(
        customParam1, customParam2
    );

    protected ModelStorage.Model model1 = createModel(CATEGORY_ID, VENDOR_OPTION_ID, 1L, "GURU", false);
    protected ModelStorage.Model model2 = createModel(CATEGORY_ID, VENDOR_OPTION_ID, 2L, "GURU", true);
    protected ModelStorage.Model model3 = createModel(CATEGORY_ID, null, 3L, "GURU",
        "Model3", "http://url3", true);
    protected ModelStorage.Model model4 = createModel(CATEGORY_ID, VENDOR_OPTION_ID, 4L, "GURU",
        null, "http://url4", true);
    protected ModelStorage.Model model5 = createModel(CATEGORY_ID, VENDOR_OPTION_ID, 5L, "GURU",
        "Model5", null, true);
    protected ModelStorage.Model model6 = createModel(CATEGORY_ID, Vendor.FAKE_VENDOR, 6L, "GURU",
        "Model5", "http://url6", true);
    protected ModelStorage.Model model7 = createModel(CATEGORY_ID, VENDOR_OPTION_ID, 7L, "GURU",
        ParamUtils.NO_TITLE, "http://url7", true);
    protected ModelStorage.Model cluster1 = createModelBuilder(CATEGORY_ID, VENDOR_OPTION_ID, 10L, "CLUSTER", false)
        .addClusterizerOfferIds("10").build();
    protected ModelStorage.Model cluster2 = createModelBuilder(CATEGORY_ID, VENDOR_OPTION_ID, 11L, "CLUSTER", true)
        .addClusterizerOfferIds("11").build();
    protected ModelStorage.Model cluster3 = createModelBuilder(CATEGORY_ID, VENDOR_OPTION_ID, 12L, "CLUSTER", true)
        .addClusterizerOfferIds("12").build();
    protected List<ModelStorage.Model> models = new ArrayList<>(Arrays.asList(
        model1, model2, model3, model4, model5, model6, model7, cluster1, cluster2, cluster3));

    Long2ObjectMap<String> shopModelLinks = new Long2ObjectOpenHashMap<>();
    {
        shopModelLinks.put(model1.getId(), SHOP_SITE);
        shopModelLinks.put(model2.getId(), SHOP_SITE);
        shopModelLinks.put(model3.getId(), SHOP_SITE);
        shopModelLinks.put(model4.getId(), SHOP_SITE);
        shopModelLinks.put(model5.getId(), SHOP_SITE);
        shopModelLinks.put(model6.getId(), SHOP_SITE);
        shopModelLinks.put(model7.getId(), SHOP_SITE);
    }

    Map<Long, String> shopUrsForClusters = new Long2ObjectOpenHashMap<>();
    {
        shopUrsForClusters.put(cluster1.getId(), SHOP_SITE);
        shopUrsForClusters.put(cluster2.getId(), SHOP_SITE);
    }

    private EtalonParamValuesRequestGenerator generator;

    public void setup(boolean forGeneration) {
        super.setup();

        when(paramUtils.getAllParams(anyInt())).thenReturn(categoryParameters);
        when(paramUtils.getGuruTemplateGroups(anyInt(), any())).thenAnswer(i -> {
            List<MboParameters.Parameter> parameters = i.getArgument(1);
            Set<String> paramXslNames = parameters.stream()
                .map(MboParameters.Parameter::getXslName)
                .collect(Collectors.toSet());
            Map<String, List<String>> result = new LinkedHashMap<>();
            result.put("tabbba", new ArrayList<>(paramXslNames));
            return result;
        });

        when(paramUtils.generateDefaultParamPredicate(anyInt(), anyBoolean())).thenReturn(any -> true);

        if (forGeneration) {
            when(paramUtils.findShopUrlsForClusters(anyMap())).thenAnswer(i -> {
                Map<String, Long> offerToModel = i.getArgument(0);
                Map<Long, String> result = new HashMap<>(shopUrsForClusters);
                result.keySet().retainAll(offerToModel.values());
                return result;
            });
        }

        when(modelStorageService.getShuffledModelsInCategory(anyInt(), any(ModelTypeValue.class),
            any(PublishingValue.class), any(BooleanSupplier.class)))
            .thenCallRealMethod();
        mockModelStorageConditionalProcessModels(modelStorageService, models);

        when(yqlDao.selectShopModelLinks(anyInt())).thenReturn(shopModelLinks);

        generator = new EtalonParamValuesRequestGenerator();

        generator.setParamUtils(paramUtils);
        generator.setYqlDao(yqlDao);
        generator.setModelStorageService(modelStorageService);
        generator.setTovarTreeProvider(tovarTreeProvider);
        generator.setFormalizedValuesService(formalizedValuesService);
    }

    protected Collection<TaskDataItem<EtalonParamValuesDataItemPayload, FillParamValuesResponse>> generateRequests(
        int count, ModelTypeValue modelType, PublishingValue publishing) {
        FullTaskDataUniqueContext<FillParamValuesIdentity> uniqueContext = Markup2TestUtils.createBasicUniqueContext();

        Map<ParameterType, Object> parameters = new HashMap<>();
        parameters.put(ParameterType.MODEL_TYPE, modelType);
        parameters.put(ParameterType.PUBLISHING, publishing);
        RequestGeneratorContext<FillParamValuesIdentity, EtalonParamValuesDataItemPayload, FillParamValuesResponse>
            context = Markup2TestUtils.createGenerationContext(
                Markup2TestUtils.createBasicTaskInfo(CATEGORY_ID, count, parameters),
                uniqueContext,
                idGenerator
            );

        generator.generateRequests(context);
        return context.getTaskDataItems();
    }

    protected RequestGeneratorContext<
        FillParamValuesIdentity, EtalonParamValuesDataItemPayload, FillParamValuesResponse> runGenerateRequests(
        int count, ModelTypeValue modelType, PublishingValue publishing, ModelIds modelIds) {
        FullTaskDataUniqueContext<FillParamValuesIdentity> uniqueContext = Markup2TestUtils.createBasicUniqueContext();

        Map<ParameterType, Object> params = createParams(modelType, publishing, modelIds);
        TaskInfo taskInfo = Markup2TestUtils.createBasicTaskInfo(CATEGORY_ID, count, params);
        RequestGeneratorContext<FillParamValuesIdentity, EtalonParamValuesDataItemPayload, FillParamValuesResponse>
            context = Markup2TestUtils.createGenerationContext(taskInfo, uniqueContext, idGenerator);

        generator.generateRequests(context);

        return context;
    }

    protected Map<ParameterType, Object> createParams(ModelTypeValue modelType,
                                                      PublishingValue publishing, ModelIds modelIds) {
        Map<ParameterType, Object> parameters = new HashMap<>();
        parameters.put(ParameterType.MODEL_TYPE, modelType);
        parameters.put(ParameterType.PUBLISHING, publishing);
        if (modelIds != null) {
            parameters.put(ParameterType.MODEL_IDS, modelIds);
        }
        return parameters;
    }

    protected RequestGeneratorContext<
        FillParamValuesIdentity, EtalonParamValuesDataItemPayload, FillParamValuesResponse> runGenerateRequests(
        TaskInfo taskInfo) {
        FullTaskDataUniqueContext<FillParamValuesIdentity> uniqueContext = Markup2TestUtils.createBasicUniqueContext();

        RequestGeneratorContext<FillParamValuesIdentity, EtalonParamValuesDataItemPayload, FillParamValuesResponse>
            context = Markup2TestUtils.createGenerationContext(taskInfo, uniqueContext, idGenerator);

        generator.generateRequests(context);

        return context;
    }


    protected void assertTask(TaskDataItem<? extends FillParamValuesDataItemPayload, FillParamValuesResponse> request,
                            ModelStorage.Model model) {
        FillParamValuesDataItemPayload payload = request.getInputData();
        Set<Long> paramIds = payload.getDataIdentifier().getParamIds();
        assertEquals(categoryParameters.size(), paramIds.size());
        for (MboParameters.Parameter param : categoryParameters) {
            assertTrue(paramIds.contains(param.getId()));
        }

        FillParamValuesDataAttributes attributes = payload.getAttributes();
        assertEquals(categoryParameters.size(), attributes.getTemplate().size());

        String name = model.getTitles(0).getValue();
        assertEquals(name, attributes.getName());
        assertEquals(CATEGORY_ID, attributes.getCategoryId());

        String vendorUrl = ModelTestUtils.getStringValue(model, ParamUtils.URL_ID);
        if (vendorUrl != null) {
            assertEquals(vendorUrl, attributes.getVendorLink());
        }
        assertEquals("vendor1_name", attributes.getVendor());
        String site = shopModelLinks.get(model.getId());
        if (site == null) {
            site = shopUrsForClusters.get(model.getId());
        }
        assertEquals(site, attributes.getShoplink());
    }

    protected ModelStorage.Model getModel(long id) {
        return models.stream().filter(m -> m.getId() == id).findFirst().orElse(null);
    }
}
