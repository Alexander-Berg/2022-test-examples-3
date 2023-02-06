package ru.yandex.market.markup2.tasks.fill_param_values;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.markup2.entries.group.ModelTypeValue;
import ru.yandex.market.markup2.entries.group.PublishingValue;
import ru.yandex.market.markup2.utils.Markup2TestUtils;
import ru.yandex.market.markup2.utils.offer.Offer;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.generation.RequestGeneratorContext;
import ru.yandex.market.markup2.workflow.taskDataUnique.FullTaskDataUniqueContext;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 15.06.2017
 */
@RunWith(MockitoJUnitRunner.class)
public class FillParamValuesRequestGeneratorTest extends FillParamValuesRequestGeneratorTestBase {

    private static final int MODELS_COUNT = 20;
    private static final int TOTAL_MODELS_COUNT = MODELS_COUNT * 3;

    private final List<Offer> offers = generateOffers(TOTAL_MODELS_COUNT);
    private final Map<String, Offer> offersById = offers.stream()
        .collect(Collectors.toMap(Offer::getOfferId, Function.identity()));

    private final List<MboParameters.Parameter> categoryParameters = ParametersData.generateParametersWithString();

    private final List<ModelStorage.Model> filledModels =
        ModelsData.generateModels(CATEGORY_ID, categoryParameters, MODELS_COUNT,
                offers.subList(0, MODELS_COUNT), false);
    private final List<ModelStorage.Model> withEmptyValuesModels =
        ModelsData.generateModels(CATEGORY_ID, categoryParameters, MODELS_COUNT,
                offers.subList(MODELS_COUNT, offers.size()), true);

    private final Map<Long, Offer> offersByModelId = allModels(filledModels, withEmptyValuesModels).stream()
        .collect(Collectors.toMap(ModelStorage.Model::getId, m -> offersById.get(m.getClusterizerOfferIds(0))));

    private final Long2ObjectMap<String> shopModelLinks = generateShopModelLinks(offersByModelId);

    private FillParamValuesRequestGenerator generator;

    @Before
    public void setup() {
        super.setup();

        when(paramUtils.getParams(anyInt(), anyBoolean())).thenReturn(categoryParameters);

        when(modelStorageService.getShuffledModelsInCategory(anyInt(), any(ModelTypeValue.class),
            any(PublishingValue.class), Matchers.any(BooleanSupplier.class))).thenReturn(
            allModels(filledModels, withEmptyValuesModels));

        when(yqlDao.selectShopModelLinks(anyInt())).thenReturn(shopModelLinks);

        generator = new FillParamValuesRequestGenerator();

        generator.setParamUtils(paramUtils);
        generator.setYqlDao(yqlDao);
        generator.setModelStorageService(modelStorageService);
        generator.setFormalizedValuesService(formalizedValuesService);
        generator.setTovarTreeProvider(tovarTreeProvider);
        generator.setStringParametersAllowed(true);
    }

    private Collection<TaskDataItem<FillParamValuesDataItemPayload, FillParamValuesResponse>> generateRequests(
        FullTaskDataUniqueContext<FillParamValuesIdentity> uniqueContext) {

        RequestGeneratorContext<FillParamValuesIdentity, FillParamValuesDataItemPayload,
                FillParamValuesResponse> context =
            Markup2TestUtils.createGenerationContext(
                Markup2TestUtils.createBasicTaskInfo(CATEGORY_ID, MODELS_COUNT, Collections.emptyMap()),
                uniqueContext,
                idGenerator
            );

        generator.generateRequests(context);
        return context.getTaskDataItems();
    }

    @Test
    public void generateForNotFilledModels() throws InterruptedException {
        FullTaskDataUniqueContext<FillParamValuesIdentity> uniqueContext = Markup2TestUtils.createBasicUniqueContext();
        Collection<TaskDataItem<FillParamValuesDataItemPayload, FillParamValuesResponse>> requests =
            generateRequests(uniqueContext);

        List<ModelStorage.Model> modelsForGeneration = getModelsForGeneration();

        Assert.assertEquals(modelsForGeneration.size(), requests.size());

        // проверяем, что сгенерировались запросы для всех незаполненных моделей и параметров
        Map<Long, Set<Long>> generatedModelIdToParamIds = new HashMap<>();

        for (TaskDataItem<FillParamValuesDataItemPayload, FillParamValuesResponse> task : requests) {
            FillParamValuesIdentity identity = task.getInputData().getDataIdentifier();
            generatedModelIdToParamIds.put(identity.getModelId(), identity.getParamIds());
        }

        Set<Long> parameterIds = getParameterIds(categoryParameters);
        Map<Long, Set<Long>> shouldBeenGenerated = new HashMap<>();

        for (ModelStorage.Model model : modelsForGeneration) {
            shouldBeenGenerated.put(model.getId(), Sets.difference(parameterIds, getFilledParameterIds(model)));
        }

        Assert.assertEquals(shouldBeenGenerated, generatedModelIdToParamIds);
    }

    @Test
    public void skipModelsWithoutUrl() {
        FullTaskDataUniqueContext<FillParamValuesIdentity> uniqueContext = Markup2TestUtils.createBasicUniqueContext();
        Collection<TaskDataItem<FillParamValuesDataItemPayload, FillParamValuesResponse>> requests =
            generateRequests(uniqueContext);

        List<ModelStorage.Model> modelsForGeneration = getModelsForGeneration();
        Set<Long> modelIdsForGeneration = modelsForGeneration.stream()
            .map(ModelStorage.Model::getId)
            .collect(Collectors.toSet());

        Assert.assertEquals(modelsForGeneration.size(), requests.size());

        // проверяем, что все моделеи без ссылки на магазин не были взяты в задание

        Set<Long> generatedModelIds = requests.stream()
            .map(item -> item.getInputData().getDataIdentifier().getModelId())
            .collect(Collectors.toSet());

        Assert.assertEquals(modelIdsForGeneration, generatedModelIds);
    }

    @Test
    public void generateOnlyForNotProcessed() {
        FullTaskDataUniqueContext<FillParamValuesIdentity> uniqueContext = Markup2TestUtils.createBasicUniqueContext();
        List<ModelStorage.Model> processedModels = withEmptyValuesModels.subList(0, withEmptyValuesModels.size() / 2);
        List<ModelStorage.Model> modelsForFill =
            withEmptyValuesModels.subList(processedModels.size(), withEmptyValuesModels.size());
        Set<Long> modelIdsForFill = modelsForFill.stream()
                .map(ModelStorage.Model::getId).collect(Collectors.toSet());

        Set<Long> parameterIds = getParameterIds(categoryParameters);

        for (ModelStorage.Model model : processedModels) {
            Set<Long> filledParameterIds = getFilledParameterIds(model);
            Set<Long> parameterIdsForFill = Sets.difference(parameterIds, filledParameterIds);
            Assert.assertTrue(
                uniqueContext.addIfAbsent(new FillParamValuesIdentity(model.getId(), parameterIdsForFill))
            );
        }

        Collection<TaskDataItem<FillParamValuesDataItemPayload, FillParamValuesResponse>> requests =
            generateRequests(uniqueContext);

        List<ModelStorage.Model> modelsForGeneration = getModelsForGeneration();
        modelsForGeneration = modelsForGeneration.stream()
            .filter(m -> modelIdsForFill.contains(m.getId()))
            .collect(Collectors.toList());

        Assert.assertEquals(modelsForGeneration.size(), requests.size());

        // проверяем, что запросы сгенерились только для невыданных в текущий момент моделей
        Set<Long> modelIdsShouldBeenGenerated = modelsForGeneration.stream()
            .map(ModelStorage.Model::getId)
            .collect(Collectors.toSet());

        Set<Long> generatedModelIds = requests.stream()
            .map(TaskDataItem::getInputData)
            .map(FillParamValuesDataItemPayload::getDataIdentifier)
            .map(FillParamValuesIdentity::getModelId)
            .collect(Collectors.toSet());

        Assert.assertEquals(modelIdsShouldBeenGenerated, generatedModelIds);
    }

    private List<ModelStorage.Model> getModelsForGeneration() {
        return withEmptyValuesModels.stream()
            .filter(m -> shopModelLinks.containsKey(m.getId()))
            .collect(Collectors.toList());
    }

    private Long2ObjectMap<String> generateShopModelLinks(Map<Long, Offer> offersByModelId) {
        Long2ObjectMap<String> result = new Long2ObjectOpenHashMap<>();
        for (int i = 0; i < MODELS_COUNT; ++i) {
            // добавляем ссылки и заполненным моделям и моделям с часть незаполненных параметров
            ModelStorage.Model model;
            if (i % 2 == 0) {
                model = filledModels.get(i);
            } else {
                model = withEmptyValuesModels.get(i);
            }

            Offer offer = offersByModelId.get(model.getId());
            result.put(model.getId(), offer.getUrl());
        }
        return result;
    }
}
