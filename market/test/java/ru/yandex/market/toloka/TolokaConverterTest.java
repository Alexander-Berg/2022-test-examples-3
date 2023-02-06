package ru.yandex.market.toloka;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.googlecode.protobuf.format.JsonFormat;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.aliasmaker.AliasMaker;
import ru.yandex.market.ir.http.Matcher;
import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
import ru.yandex.market.markup2.entries.config.TaskConfigState;
import ru.yandex.market.markup2.entries.group.TaskConfigGroupInfo;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.entries.task.TaskState;
import ru.yandex.market.markup2.entries.type.TaskTypeInfo;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.SupplierOfferDataAttributes;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.SupplierOfferDataItemPayload;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.moderation_toloka.ModerationTolokaDataItemsProcessor;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.moderation_toloka.ModerationTolokaResponse;
import ru.yandex.market.markup2.utils.YtHoneypotsReader;
import ru.yandex.market.markup2.utils.tovarTree.TovarTreeProvider;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.pipe.Pipes;
import ru.yandex.market.markup2.workflow.taskDataUnique.TaskDataUniqueStrategy;
import ru.yandex.market.markup2.workflow.taskType.processor.TolokaDataItemsProcessor;
import ru.yandex.market.mbo.export.CategoryModelsService;
import ru.yandex.market.mbo.export.CategoryParametersService;
import ru.yandex.market.mbo.export.MboExport;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.model.forms.ModelFormService;
import ru.yandex.market.mbo.model.forms.ModelForms;
import ru.yandex.market.toloka.model.Task;

import static org.mockito.Matchers.any;

@ParametersAreNonnullByDefault
public class TolokaConverterTest {

    private final String modelsFile = "models.json";
    private final String parametersFile = "parameters.json";
    private final String formsFile = "forms.json";
    private CategoryModelsService categoryModelsService;
    private ModelFormService modelFormService;
    private CategoryParametersService categoryParametersService;

    TolokaModelConverter tolokaModelConverter;
    TolokaOfferConverter tolokaOfferConverter;
    ModerationTolokaDataItemsProcessor processor;

    @Before
    public void init() throws IOException {
        MboExport.GetCategoryModelsResponse.Builder modelsBuilder = MboExport.GetCategoryModelsResponse
                .newBuilder();
        JsonFormat.merge(
                new InputStreamReader(
                        getClass().getResourceAsStream("/" + modelsFile)
                ),
                modelsBuilder
        );
        categoryModelsService = Mockito.mock(CategoryModelsService.class);
        Mockito.when(categoryModelsService.getModels(any())).thenReturn(modelsBuilder.build());

        MboParameters.GetCategoryParametersResponse.Builder categoryBuilder =
                MboParameters.GetCategoryParametersResponse.newBuilder();
        JsonFormat.merge(
                new InputStreamReader(
                        getClass().getResourceAsStream("/" + parametersFile)
                ),
                categoryBuilder
        );
        categoryParametersService = Mockito.mock(CategoryParametersService.class);
        Mockito.when(categoryParametersService.getParameters(any())).thenReturn(categoryBuilder.build());

        ModelForms.GetModelFormsResponse.Builder formsBuilder = ModelForms.GetModelFormsResponse.newBuilder();
        JsonFormat.merge(
                new InputStreamReader(
                        getClass().getResourceAsStream("/" + formsFile)
                ),
                formsBuilder
        );
        modelFormService = Mockito.mock(ModelFormService.class);
        Mockito.when(modelFormService.getModelForms(any())).thenReturn(formsBuilder.build());

        tolokaModelConverter = new TolokaModelConverter(categoryModelsService, modelFormService,
                categoryParametersService);
        tolokaOfferConverter = new TolokaOfferConverter();
    }

    @Test
    public void tolokaModelConverterTest() {
        String json = tolokaModelConverter.convert(13480658, Arrays.asList(705620338L)).values()
                .iterator().next().toString();

        checkModel(json);
    }

    private void checkModel(String json) {
        Assert.assertTrue(json.contains("\"base_block\":"));
        Assert.assertTrue(json.contains("\"name\":\"barcode\""));
        Assert.assertTrue(json.contains("\"value\":\"6906244192249\""));
        Assert.assertTrue(json.contains("\"name\":\"vendor_code\""));
        Assert.assertTrue(json.contains("\"value\":\"IT100307\""));

        Assert.assertTrue(json.contains("\"parameter_blocks\":"));
        Assert.assertTrue(json.contains("\"name\":\"Общие характеристики\""));
        Assert.assertTrue(json.contains("\"param_values\":"));
        Assert.assertTrue(json.contains("{\"unit\":\"шт.\",\"name\":\"Количество предметов\",\"value\":20"));

        //убрали урл
        Assert.assertFalse(json.contains("\"url\":"));
        Assert.assertTrue(json.contains("\"vendor\":\"Girl's Club\""));
        Assert.assertTrue(json.contains("\"model_id\":705620338"));
        Assert.assertTrue(json.contains("\"title\":\"Набор продуктов Girl's Club IT100307\""));
    }

    @Test
    public void tolokaOfferConverterTest() {
        JSONObject root = tolokaOfferConverter.convert(createOffer().build());

        String json = root.toString();

        Assert.assertTrue(json.contains("{\"unit\":\"\",\"name\":\"Возраст от\",\"value\":\"3\"}"));
        Assert.assertTrue(json.contains("\"urls\":[\"tr.tr\",\"yy.yy\"]"));
        Assert.assertTrue(json.contains("\"shop_category_name\":\"shop_cat_nam\""));
        Assert.assertTrue(json.contains("\"offer_vendor\":\"vendor\""));
        Assert.assertTrue(json.contains("\"title\":\"title\""));
        Assert.assertTrue(json.contains("\"offer_model\":\"model\""));
        Assert.assertTrue(json.contains("\"description\":\"d3esc\""));
        Assert.assertFalse(json.contains("\"shop_name\""));
        Assert.assertTrue(json.contains("\"pictures\":[\"pics\",\"sdff\",\"sdf\",\"sdfs\"]"));

        TolokaOfferConverter.IGNORED_PARAMS.forEach(param -> {
            Assert.assertFalse(json.contains(param));
        });
    }

    @Test
    public void tolokaOfferConverterSpecificBarcodeAndVendorCodeTest() {
        JSONObject root = tolokaOfferConverter.convert(createOffer()
                .setOfferParams("<?xml version='1.0' encoding='UTF-8'?><offer_params><param name=\"bar_code\" " +
                        "unit=\"\">4620000637646,</param><param name=\"vendor_code\" " +
                        "unit=\"\">ПТ02</param></offer_params>")
                .build());

        String json = root.toString();

        Assert.assertFalse(json.contains("{\"unit\":\"\",\"name\":\"bar_code\""));
        Assert.assertFalse(json.contains("{\"unit\":\"\",\"name\":\"vendor_code\""));
        Assert.assertTrue(json.contains("\"urls\":[\"tr.tr\",\"yy.yy\"]"));
        Assert.assertTrue(json.contains("\"shop_category_name\":\"shop_cat_nam\""));
        Assert.assertTrue(json.contains("\"offer_vendor\":\"vendor\""));
        Assert.assertTrue(json.contains("\"title\":\"title\""));
        Assert.assertTrue(json.contains("\"offer_model\":\"model\""));
        Assert.assertTrue(json.contains("\"description\":\"d3esc\""));
        Assert.assertTrue(json.contains("\"barcode\":"));
        Assert.assertFalse(json.contains("\"shop_name\""));
        Assert.assertTrue(json.contains("\"pictures\":[\"pics\",\"sdff\",\"sdf\",\"sdfs\"]"));

        TolokaOfferConverter.IGNORED_PARAMS.forEach(param -> {
            Assert.assertFalse(json.contains(param));
        });
    }

    private AliasMaker.Offer.Builder createOffer() {
        return AliasMaker.Offer.newBuilder()
                .setOfferId("o_id")
                .setOfferModel("model")
                .setOfferParams("<?xml version='1.0' encoding='UTF-8'?><offer_params><param name=\"НДС\" " +
                        "unit=\"\">VAT_10</param><param name=\"Цвет\" unit=\"\">красный</param><param name=\"Цена\" " +
                        "unit=\"\">1149</param><param name=\"Материал\" unit=\"\">металл, дерево</param><param " +
                        "name=\"Возраст от\" unit=\"\">3</param><param name=\"Категория на Беру\" unit=\"\">Санки и " +
                        "аксессуары</param><param name=\"Возможный SKU на Яндексе\" " +
                        "unit=\"\">100285270760</param><param name=\"Название товара на Беру\" unit=\"\">Санки Nika " +
                        "Тимка 3 (Т3) красный</param><param name=\"Страница товара на Беру\" unit=\"\">https://beru" +
                        ".ru/product/100285270760</param><param name=\"Доступное количество товара\" unit=\"\">Санки " +
                        "Nika Тимка 3 (Т3) красный</param></offer_params>")
                .setOfferVendor("vendor")
                .setShopOfferId("shopid")
                .setBarcode("123402342342")
                .setClusterId(10L)
                .setDescription("d3esc")
                .setMatchType(Matcher.MatchType.CUT_OF_WORDS)
                .setModelId(133L)
                .setModelName("mdl")
                .setPictures("pics, sdff,sdf, sdfs")
                .setPrice(0.2)
                .setShopCategoryName("shop_cat_nam")
                .setShopName("shop")
                .setTitle("title")
                .addUrls("tr.tr")
                .addUrls("yy.yy")
                .setVendorId(1L)
                .setVendorName("vend");
    }

    @Test
    public void convertToTaskSuitesTest() {
        final int BATCH_SIZE = 10;
        final int HONEYPOTS_PER_BATCH = Math.round(ModerationTolokaDataItemsProcessor.HONEYPOTS_PERC * BATCH_SIZE);
        processor = new ModerationTolokaDataItemsProcessor();

        YtHoneypotsReader ytHoneypotsReader = Mockito.mock(YtHoneypotsReader.class);
        List<Task> honeyPots = Stream.generate(Task::new).limit(10).collect(Collectors.toList());
        Mockito.when(ytHoneypotsReader.loadHoneypots(Mockito.anyInt(),
                Mockito.anyCollection())).thenReturn(honeyPots);

        TovarTreeProvider tovarTreeProvider = Mockito.mock(TovarTreeProvider.class);
        Mockito.when(tovarTreeProvider.getCategoryName(1000)).thenReturn("categoryName");

        processor.setYtHoneypotsReader(ytHoneypotsReader);
        processor.setTovarTreeProvider(tovarTreeProvider);

        processor.setTolokaModelConverter(tolokaModelConverter);
        processor.setTolokaOfferConverter(tolokaOfferConverter);

        AtomicLong offerId = new AtomicLong(1);

        List<TaskDataItem<SupplierOfferDataItemPayload,
                ModerationTolokaResponse>> items = Stream.generate(() -> generateDataItem(offerId.incrementAndGet()))
                .limit(10).collect(Collectors.toList());

        List<TolokaDataItemsProcessor.TaskSuite> taskSuites = processor.convertToTaskSuites(createTaskInfo(), items);

        Assert.assertTrue(taskSuites.size() == 2);
        Assert.assertTrue(taskSuites.get(0).getHoneypots().size() == HONEYPOTS_PER_BATCH);
        Assert.assertTrue(taskSuites.get(0).getTasks().size() == BATCH_SIZE - HONEYPOTS_PER_BATCH);
        Gson gson = new Gson();
        Type gsonType = new TypeToken<HashMap>(){}.getType();

        taskSuites.get(0).getTasks().forEach((task, i) -> {
            Assert.assertTrue(((Task)task).getInputValues().get("offer_info").toString().contains("barcode"));

            checkModel(gson.toJson(((Task)task).getInputValues().get("msku_info"), gsonType).replace("\\u0027", "'"));
        });

    }

    private TaskDataItem<SupplierOfferDataItemPayload,
            ModerationTolokaResponse> generateDataItem(Long offerId) {
        SupplierOfferDataItemPayload payload = new SupplierOfferDataItemPayload(offerId.toString(),
                new SupplierOfferDataAttributes(1000, "namec", "",
                        "", "", createOffer(705620338L, offerId.toString())));
        return new TaskDataItem(1, payload);
    }

    private TaskInfo createTaskInfo() {
        int idSeq = 10;
        TaskTypeInfo taskTypeInfo = new TaskTypeInfo(idSeq++, "dummy", null, 0, 779449,
                Pipes.SEQUENTIALLY, TaskDataUniqueStrategy.TYPE_CATEGORY);

        TaskConfigGroupInfo groupConfigInfo = new TaskConfigGroupInfo.Builder()
                .setCategoryId(1000)
                .setTypeInfo(taskTypeInfo)
                .setId(idSeq++)
                .build();
        TaskConfigInfo taskConfigInfo = new TaskConfigInfo.Builder()
                .setCount(10)
                .setGroupInfo(groupConfigInfo)
                .setId(idSeq++)
                .setState(TaskConfigState.ACTIVE)
                .setMaxBatchSize(10)
                .build();

        return new TaskInfo.Builder()
                .setId(idSeq++)
                .setConfig(taskConfigInfo)
                .setState(TaskState.RUNNING)
                .build();
    }

    public static AliasMaker.Offer createOffer(Long modelId, String offerId) {
        return AliasMaker.Offer.newBuilder()
                .setOfferId(offerId)
                .setBarcode("345435")
                .setSuggestMappingInfo(AliasMaker.MappingInfo.newBuilder().setSkuId(modelId).build())
                .setVendorId(2L)
                .build();
    }


}
