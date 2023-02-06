package ru.yandex.market.markup3.mapping_moderation.toloka;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.protobuf.format.JsonFormat;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.aliasmaker.AliasMaker;
import ru.yandex.market.ir.http.Matcher;
import ru.yandex.market.markup3.tasks.mapping_moderation.toloka.dto.TolokaMappingModerationOfferInfo;
import ru.yandex.market.markup3.tasks.mapping_moderation.toloka.dto.TolokaMappingModerationSkuInfo;
import ru.yandex.market.mbo.export.MboExport;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.model.forms.ModelForms;

import static ru.yandex.market.markup3.tasks.mapping_moderation.toloka.ConstsKt.IGNORED_PARAMS;
import static ru.yandex.market.markup3.tasks.mapping_moderation.toloka.HelpersKt.convertOfferInfo;
import static ru.yandex.market.markup3.tasks.mapping_moderation.toloka.HelpersKt.convertSkuInfo;

@ParametersAreNonnullByDefault
public class TolokaConverterTest {
    private static final String MODELS_FILE = "/toloka/models.json";
    private static final String PARAMETERS_FILE = "/toloka/parameters.json";
    private static final String FORMS_FILE = "/toloka/forms.json";

    ObjectMapper mapper = new ObjectMapper();

    ModelStorage.Model model;
    Map<String, MboParameters.Parameter> parameterValueMap;
    ModelForms.ModelForm modelForm;

    @Before
    public void init() throws IOException {
        MboExport.GetCategoryModelsResponse.Builder modelsBuilder = MboExport.GetCategoryModelsResponse
            .newBuilder();
        JsonFormat.merge(
            new InputStreamReader(
                getClass().getResourceAsStream(MODELS_FILE)
            ),
            modelsBuilder
        );
        model = modelsBuilder.getModels(0);

        MboParameters.GetCategoryParametersResponse.Builder categoryBuilder =
            MboParameters.GetCategoryParametersResponse.newBuilder();
        JsonFormat.merge(
            new InputStreamReader(
                getClass().getResourceAsStream(PARAMETERS_FILE)
            ),
            categoryBuilder
        );
        MboParameters.Category categoryParameters = categoryBuilder.getCategoryParameters();
        parameterValueMap = categoryParameters.getParameterList().stream().collect(Collectors.toMap(
            MboParameters.Parameter::getXslName, Function.identity()));

        ModelForms.GetModelFormsResponse.Builder formsBuilder = ModelForms.GetModelFormsResponse.newBuilder();
        JsonFormat.merge(
            new InputStreamReader(
                getClass().getResourceAsStream(FORMS_FILE)
            ),
            formsBuilder
        );
        modelForm = formsBuilder.getModelForms(0);
    }

    @Test
    public void tolokaModelConverterTest() throws JsonProcessingException {
        TolokaMappingModerationSkuInfo mappingModerationSkuInfo = convertSkuInfo(model, modelForm,
            parameterValueMap);

        String json = mapper.writeValueAsString(mappingModerationSkuInfo);

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
        Assert.assertTrue(json.contains("{\"name\":\"Количество предметов\",\"unit\":\"шт.\",\"value\":20"));

        //убрали урл
        Assert.assertFalse(json.contains("\"url\":"));
        Assert.assertTrue(json.contains("\"vendor\":\"Girl's Club\""));
        Assert.assertTrue(json.contains("\"model_id\":705620338"));
        Assert.assertTrue(json.contains("\"title\":\"Набор продуктов Girl's Club IT100307\""));
    }

    @Test
    public void tolokaOfferConverterTest() throws JsonProcessingException {
        TolokaMappingModerationOfferInfo mappingModerationOfferInfo = convertOfferInfo(createOffer().build());

        String json = mapper.writeValueAsString(mappingModerationOfferInfo);

        Assert.assertTrue(json.contains("{\"name\":\"Возраст от\",\"unit\":\"\",\"value\":\"3\"}"));
        Assert.assertTrue(json.contains("\"urls\":[\"tr.tr\",\"yy.yy\"]"));
        Assert.assertTrue(json.contains("\"shop_category_name\":\"shop_cat_nam\""));
        Assert.assertTrue(json.contains("\"offer_vendor\":\"vendor\""));
        Assert.assertTrue(json.contains("\"title\":\"title\""));
        Assert.assertTrue(json.contains("\"offer_model\":\"model\""));
        Assert.assertTrue(json.contains("\"description\":\"d3esc\""));
        Assert.assertFalse(json.contains("\"shop_name\""));
        Assert.assertTrue(json.contains("\"pictures\":[\"pics\",\"sdff\",\"sdf\",\"sdfs\"]"));

        IGNORED_PARAMS.forEach(param -> Assert.assertFalse(json.contains(param)));
    }

    @Test
    public void tolokaOfferConverterSpecificBarcodeAndVendorCodeTest() throws JsonProcessingException {

        TolokaMappingModerationOfferInfo mappingModerationOfferInfo = convertOfferInfo(createOffer()
            .setOfferParams("<?xml version='1.0' encoding='UTF-8'?><offer_params><param name=\"bar_code\" " +
                "unit=\"\">4620000637646,</param><param name=\"vendor_code\" " +
                "unit=\"\">ПТ02</param></offer_params>")
            .build());

        String json = mapper.writeValueAsString(mappingModerationOfferInfo);

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

        IGNORED_PARAMS.forEach(param -> Assert.assertFalse(json.contains(param)));
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
}
