package ru.yandex.vendor.report;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.core.io.InputStreamResource;

import ru.yandex.vendor.brand.Brand;
import ru.yandex.vendor.brand.BrandInfoService;
import ru.yandex.vendor.category.ICategoryService;
import ru.yandex.vendor.exception.ModelNotFoundException;
import ru.yandex.vendor.modeleditor.ModelEditData;
import ru.yandex.vendor.modeleditor.ModelFields;
import ru.yandex.vendor.modeleditor.model.Model;
import ru.yandex.vendor.modeleditor.model.ModelParameter;
import ru.yandex.vendor.report.brand_products.ReportBrandProductsResponseParser;
import ru.yandex.vendor.report.modelinfo.ReportModelInfoResponseParser;
import ru.yandex.vendor.report.shop_info.ReportShopInfoResponseParser;
import ru.yandex.vendor.util.RestTemplateRestClient;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportClientTest {

    private ReportClient client;
    private ReportModelInfoResponseParser reportModelInfoResponseParser;

    @Before
    public void setUp() throws Exception {
        ICategoryService categoryService = mock(ICategoryService.class);

        RestTemplateRestClient restClient = mock(RestTemplateRestClient.class);
        InputStreamResource mockResponse =
                new InputStreamResource(new ByteArrayInputStream(ModelEditData.modelJson.getBytes(StandardCharsets.UTF_8)));
        when(restClient.getForObject(any())).thenReturn(mockResponse);

        BrandInfoService brandInfoService = mock(BrandInfoService.class);
        when(brandInfoService.brandById(any())).thenReturn(
                Optional.of(Brand.fromIdAndName(ModelEditData.expectedModelBrandId, "Brand!")));

        reportModelInfoResponseParser = new ReportModelInfoResponseParser(brandInfoService, categoryService);
        ReportBrandProductsResponseParser reportBrandProductsResponseParser = new ReportBrandProductsResponseParser();
        ReportShopInfoResponseParser reportShopInfoResponseParser = new ReportShopInfoResponseParser();

        client = new ReportClient(restClient, reportModelInfoResponseParser, reportBrandProductsResponseParser,
                reportShopInfoResponseParser);
    }

    @Test
    @DisplayName("Парсинг ответа репорта из place = model_sku_info")
    public void parseModelSku() {
        InputStream inputStream =
                getTestInputStreamResource("/parseModel/model_sku_report.json");
        Optional<Model> models = reportModelInfoResponseParser.parseModelsSkuInfoReportResponse(inputStream);

        Model model = models.orElseThrow(() -> new RuntimeException("Parse error!"));

        assertEquals(651771109L, (long) model.getModelId());
        assertEquals("Кейс мужских носков «Престиж», 30 пар в наборе, черный кейс (размер 41-43)",
                model.getTitle());
        assertEquals("//avatars.mds.yandex.net/get-mpic/1636931/img_id3244127892449708078.jpeg/orig",
                model.getImages().get(0).getUrl());
        assertEquals(100501, (long) model.getBrand().getId());
        assertEquals(100865007024L, (long) model.getSku().get(0).getId());

    }

    /**
     * @throws IOException
     */
    @Test
    public void parseModel() throws Exception {
        InputStream inputStream =
                getTestInputStreamResource("/parseModel/report.json");

        List<Model> models = reportModelInfoResponseParser.parseModelsInfoReportResponse(inputStream,
                Collections.singleton(ModelFields.PARAMETERS));

        List<Long> listOfModelId = new ArrayList<>();
        models.forEach(model -> listOfModelId.add(model.getModelId()));
        List<Integer> listOfGlParameterId = new ArrayList<>();

        models.forEach(model -> model.getParameters().forEach(
                gl_parameter -> listOfGlParameterId.add(gl_parameter.getParamId())));
        assertEquals(7, listOfModelId.size());

        assertEquals(15, listOfGlParameterId.size()); // из двух моделей в json  с одинаковым modelId

        assertEquals(2, listOfGlParameterId.stream().filter(m -> m == 304).count());
        assertEquals(2, listOfGlParameterId.stream().filter(m -> m == 301).count());
        assertEquals(2, listOfGlParameterId.stream().filter(m -> m == 302).count());
        assertEquals(2, listOfGlParameterId.stream().filter(m -> m == 401).count());
        assertEquals(2, listOfGlParameterId.stream().filter(m -> m == 402).count());
        assertEquals(2, listOfGlParameterId.stream().filter(m -> m == 403).count());
        assertEquals(2, listOfGlParameterId.stream().filter(m -> m == 303).count());
        assertEquals(1, listOfGlParameterId.stream().filter(m -> m == 308).count());

        models.forEach(model -> model.getParameters().
                stream().
                filter(p -> p.getParamId() == 308).forEach(p ->
                assertEquals(1, p.getValue().stream().filter(v -> v.equals("val1")).count())));

        models.forEach(model -> model.getParameters().
                stream().
                filter(p -> p.getParamId() == 308).forEach(p ->
                assertEquals(1, p.getValue().stream().filter(v -> v.equals("val2")).count())));

        models.forEach(model -> model.getParameters().
                stream().
                filter(p -> p.getParamId() == 308).forEach(p ->
                assertEquals(1, p.getValue().stream().filter(v -> v.equals("val3")).count())));

        assertEquals(2, listOfModelId.stream().filter(Objects::nonNull).filter(m -> m == 1070000).count());

        assertEquals("//mdata.yandex.net/i?path=b0130135324_img_id6600772400047913164.jpg",
                models.get(0).getImages().get(0).getUrl());
        assertEquals("Grabli2", models.get(0).getTitle());

        assertEquals("//mdata.yandex.net/i?path=ABRAKADABRA.jpg", models.get(1).getImages().get(0).getUrl());
        assertEquals("Grabli21", models.get(1).getTitle());

        // у modelId = 1 нет вендора
        // у modelId = 2 нет titles
        // у modelId = 3 нет categories
        // у modelId = 4 нет pictures
        // у modelId = ? нет modelId
        // Все случаи должны отрабатывать без NPE
        inputStream.close();
    }

    @Test
    public void parseModelTitle() throws Exception {
        Model model = client.getModelById(ModelEditData.expectedModelId)
                .orElseThrow(() -> new ModelNotFoundException(ModelEditData.expectedModelId));

        String actual = model.getTitle();

        assertThat("Model title has a wrong value", actual, is(ModelEditData.expectedModelTitle));
    }

    @Test
    public void parseModelBrandId() throws Exception {
        Model model = client.getModelById(ModelEditData.expectedModelId)
                .orElseThrow(() -> new ModelNotFoundException(ModelEditData.expectedModelId));

        Long actual = model.getBrandId();

        assertThat("Model brandId has a wrong value", actual, is(ModelEditData.expectedModelBrandId));
    }

    @Test
    public void parseModelParams() throws Exception {
        Model model = client.getModelById(ModelEditData.expectedModelId)
                .orElseThrow(() -> new ModelNotFoundException(ModelEditData.expectedModelId));

        Collection<ModelParameter> actual = model.getParameters();

        assertThat("Wrong model parameters count", actual.size(), is(5));
    }

    @Test
    public void parseModelEnumParam() throws Exception {
        Model model = client.getModelById(ModelEditData.expectedModelId)
                .orElseThrow(() -> new ModelNotFoundException(ModelEditData.expectedModelId));

        Collection<ModelParameter> actual = model.getParameters();

        assertNotNull("No params", actual);
        assertFalse("Empty params", actual.isEmpty());
        List<Object> values = model.getParameter(ModelEditData.enumParamId)
                .orElseThrow(() -> new AssertionError("No parameter with id=" + ModelEditData.enumParamId))
                .getValue();
        assertThat("Incorrect values count", values.size(), is(1));
        Object numberParam = values.get(0);
        assertNotNull("No enum param", numberParam);
        assertEquals("Wrong enum param value", 100501, numberParam);
    }

    @Test
    public void parseModelNumberParam() throws Exception {
        Model model = client.getModelById(ModelEditData.expectedModelId)
                .orElseThrow(() -> new ModelNotFoundException(ModelEditData.expectedModelId));

        Collection<ModelParameter> actual = model.getParameters();

        assertNotNull("No params", actual);
        assertFalse("Empty params", actual.isEmpty());
        List<Object> values = model.getParameter(ModelEditData.numberParamId)
                .orElseThrow(() -> new AssertionError("No parameter with id=" + ModelEditData.numberParamId))
                .getValue();
        assertThat("Incorrect values count", values.size(), is(1));
        Object numberParam = values.get(0);
        assertNotNull("No number param", numberParam);
        assertEquals("Wrong number param value", 34.5, numberParam);
    }

    @Test
    public void parseModelBooleanParam() throws Exception {
        Model model = client.getModelById(ModelEditData.expectedModelId)
                .orElseThrow(() -> new ModelNotFoundException(ModelEditData.expectedModelId));

        Collection<ModelParameter> actual = model.getParameters();

        assertNotNull("No params", actual);
        assertFalse("Empty params", actual.isEmpty());
        List<Object> values = model.getParameter(ModelEditData.booleanParamId)
                .orElseThrow(() -> new AssertionError("No parameter with id=" + ModelEditData.booleanParamId))
                .getValue();
        assertThat("Incorrect values count", values.size(), is(1));
        Object booleanParam = values.get(0);
        assertNotNull("No boolean param", booleanParam);
        assertEquals("Wrong boolean param value", false, booleanParam);
    }

    @Test
    public void parseModelStringParam() throws Exception {
        Model model = client.getModelById(ModelEditData.expectedModelId)
                .orElseThrow(() -> new ModelNotFoundException(ModelEditData.expectedModelId));

        Collection<ModelParameter> actual = model.getParameters();

        assertNotNull("No params", actual);
        assertFalse("Empty params", actual.isEmpty());
        List<Object> values = model.getParameter(ModelEditData.stringParamId)
                .orElseThrow(() -> new AssertionError("No parameter with id=" + ModelEditData.stringParamId))
                .getValue();
        assertThat("Incorrect values count", values.size(), is(1));
        Object stringParam = values.get(0);
        assertNotNull("No string param", stringParam);
        assertEquals("Wrong string param value", "Some string", stringParam);
    }

    @Test
    public void parseModelStringMultivalueParam() throws Exception {
        Model model = client.getModelById(ModelEditData.expectedModelId)
                .orElseThrow(() -> new ModelNotFoundException(ModelEditData.expectedModelId));

        Collection<ModelParameter> actual = model.getParameters();
        System.out.println(actual);

        assertNotNull("No params", actual);
        assertFalse("Empty params", actual.isEmpty());
        List<Object> values = model.getParameter(ModelEditData.stringMultiParamId)
                .orElseThrow(() -> new AssertionError("No parameter with id=" + ModelEditData.stringParamId))
                .getValue();
        assertThat("Incorrect values count", values.size(), is(3));
    }

    @Test
    public void checkCorrectModelNameForReport() {
        assertTrue(client.checkSymbolForReport("Sumsung"));
        assertTrue(client.checkSymbolForReport("\"gG\'"));
        assertTrue(client.checkSymbolForReport("Самсунг"));
        assertTrue(client.checkSymbolForReport("\"Самсунг\'"));
        assertTrue(client.checkSymbolForReport("\"п\'"));
        assertTrue(client.checkSymbolForReport("\"П\'"));
        assertTrue(client.checkSymbolForReport("\"П^&jjДД\'"));
    }


    @Test
    public void checkWrongModelNameForReport() {
        assertFalse(client.checkSymbolForReport("\'"));
        assertFalse(client.checkSymbolForReport("\"\'\""));
        assertFalse(client.checkSymbolForReport("^&%"));
        assertFalse(client.checkSymbolForReport("#"));
        assertFalse(client.checkSymbolForReport("\'++++\""));
        assertFalse(client.checkSymbolForReport("++++"));
        assertFalse(client.checkSymbolForReport("@\'\'\'\'\'"));
    }

    private InputStream getTestInputStreamResource(String filename) {
        ClassLoader classLoader = getClass().getClassLoader();
        return classLoader.getResourceAsStream("ru/yandex/vendor/report/" + getClass().getSimpleName() + filename);
    }

}
