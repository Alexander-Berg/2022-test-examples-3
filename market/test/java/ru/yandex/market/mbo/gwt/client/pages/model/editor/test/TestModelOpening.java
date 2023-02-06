package ru.yandex.market.mbo.gwt.client.pages.model.editor.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelDataBuilder;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.rpc.TestRpc;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.BlockWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.EditorWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamsTab;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.SourcesWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueWidget;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel.Source;
import ru.yandex.market.mbo.gwt.models.params.GuruType;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @author gilmulla
 */
@SuppressWarnings("checkstyle:magicNumber")
@RunWith(Enclosed.class)
public class TestModelOpening {

    public static class ModelOpeningByBarcodeTest extends AbstractTest {
        @Test
        public void testLoadModelUsingBarcodeMoreThanOneModelFailure() {
            List<CommonModel> models = Arrays.asList(
                data.startModel().id(1).getModel(),
                data.startModel().id(2).getModel()
            );
            rpc.setLoadModelsByBarcode(models, null);
            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("modelEditor", "barcode=1")));
            assertError("Найдено более одной модели с barcode=1, modelIds: [1, 2]");
        }

        @Test
        public void testLoadModelUsingBarcodeAndCategoryIdMoreThanOneModelFailure() {
            List<CommonModel> models = Arrays.asList(
                data.startModel().id(1).getModel(),
                data.startModel().id(2).getModel()
            );
            rpc.setLoadModelsByBarcode(models, null);
            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("modelEditor", "barcode=1&category-id=10")));
            assertError("Найдено более одной модели с barcode=1 и categoryId=10, modelIds: [1, 2]");
        }
    }

    @RunWith(Parameterized.class)
    public static class ModelOpeningByUrlKeyCommonTest extends AbstractTest {
        private final String urlKey;
        private final String messageKey;
        private final TestRpcLoadModelSetter rpcModelSetter;

        public ModelOpeningByUrlKeyCommonTest(String urlKey,
                                              String messageKey,
                                              TestRpcLoadModelSetter rpcModelSetter) {
            this.urlKey = urlKey;
            this.messageKey = messageKey;
            this.rpcModelSetter = rpcModelSetter;
        }

        @Parameterized.Parameters(name = "{index}: url key: {0}, message key: {1}")
        public static Iterable<Object[]> testData() {
            return Arrays.asList(new Object[][]{
                {"entity-id", "modelId", (TestRpcLoadModelSetter) TestRpc::setLoadModel},
                {"barcode", "barcode", (TestRpcLoadModelSetter) TestRpc::setLoadModelByBarcode}
            });
        }

        private interface TestRpcLoadModelSetter {
            void setLoadModel(TestRpc rpc, CommonModel model, Throwable error);
        }

        @Test
        public void testNullAnchor() {
            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of(null, urlKey + "=13944146")));
            assertError("Ошибка при переходе по ссылке. Не задан якорь");
        }

        @Test
        public void testEmptyAnchor() {
            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("", urlKey + "=13944146")));
            assertError("Ошибка при переходе по ссылке. Не задан якорь");
        }

        @Test
        public void testUnknownAnchor() {
            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("unknown", urlKey + "=13944146")));

            assertError("Ошибка при переходе по ссылке. Неизвестный якорь: unknown");
        }

        @Test
        public void testNullParametersString() {
            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("modelEditor", null)));

            assertError("Ошибка при переходе по ссылке. Не задан идентификатор или баркод модели");
        }

        @Test
        public void testEmptyParametersString() {
            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("modelEditor", "")));

            assertError("Ошибка при переходе по ссылке. Не задан идентификатор или баркод модели");
        }

        @Test
        public void testMissingEntityIdParametersString() {
            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("modelEditor", "category-id=111")));

            assertError("Ошибка при переходе по ссылке. Не задан идентификатор или баркод модели");
        }

        @Test
        public void testLoadModelMessageUsingEntityId() {
            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("modelEditor", urlKey + "=1")));

            assertWaitingMessage("Подождите, идет загрузка модели...");
        }

        @Test
        public void testLoadModelMessageUsingEntityIdAndCategoryId() {
            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("modelEditor", urlKey + "=1&category-id=2")));
            assertWaitingMessage("Подождите, идет загрузка модели...");
        }

        @Test
        public void testNotExistingEntityId() {
            rpcModelSetter.setLoadModel(rpc, null, null);
            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("modelEditor", urlKey + "=1")));

            assertError("Модель с " + messageKey + "=1 не найдена");
        }

        @Test
        public void testNotExistingEntityIdAndCategoryId() {
            rpcModelSetter.setLoadModel(rpc, null, null);
            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("modelEditor", urlKey + "=1&category-id=2")));
            assertError("Модель с " + messageKey + "=1 и categoryId=2 не найдена");
        }

        @Test
        public void testLoadModelByEntityIdFailed() {
            rpcModelSetter.setLoadModel(rpc, null, new Exception("error information"));
            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("modelEditor", urlKey + "=1")));

            assertError("Ошибка загрузки модели с " + messageKey + "=1. Информация об ошибке: error information");
        }

        @Test
        public void testLoadModelByEntityIdAndCateogryIdFailed() {
            rpcModelSetter.setLoadModel(rpc, null, new Exception("error information"));

            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("modelEditor", urlKey + "=1&category-id=2")));

            assertError("Ошибка загрузки модели с " + messageKey + "=1 и categoryId=2." +
                " Информация об ошибке: error information");
        }

        @Test
        public void testMissingVendor() {
            rpcModelSetter.setLoadModel(rpc,
                data.startModel()
                    .id(1).category(2)
                    .param("num1").setNumeric(1)
                    .param("num2").setEmpty()
                    .endModel().getModel(),
                null);

            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("modelEditor", urlKey + "=1")));

            assertError("Отсутствует производитель (vendor)");
        }

        @Test
        public void testModelDataLoadingFailed() {
            rpcModelSetter.setLoadModel(rpc,
                data.startModel()
                    .id(1).category(2)
                    .param("num1").setNumeric(1)
                    .param("num2").setEmpty()
                    .param(XslNames.VENDOR).setOption(1)
                    .endModel().getModel(),
                null);
            rpc.setLoadModelData(null, new Exception("Error information"));

            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("modelEditor", urlKey + "=1")));


            assertError("Возникла ошибка при получении дополнительных данных модели: Error information");
        }

        @Test
        public void testModelLoadedMessages() {
            data.startModel()
                .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
                .param("num1").setNumeric(1)
                .param("num2").setNumeric(2)
                .param(XslNames.VENDOR).setOption(1)
                .endModel()
                .startForm()
                .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                .name("block")
                .properties("num1", "num2")
                .endBlock()
                .endTab()
                .endForm()
                .startVendor()
                .source("http://source1", "ru", new Date())
                .source("http://source2", "en", new Date())
                .endVendor()
                .tovarCategory(1, 2);

            rpcModelSetter.setLoadModel(rpc, data.getModel(), null);
            rpc.setLoadModelData(data.getModelData(), null);

            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("modelEditor", urlKey + "=1")));

            assertSuccessMessage("Модель загружена");
        }

        @Test
        public void testGeneratedUILayout3Tab1BlocksCase() {
            ModelDataBuilder data = ModelDataBuilder.modelData()
                .startParameters()
                .startParameter()
                .xsl("num1").type(Param.Type.NUMERIC).name("Num1")
                .endParameter()
                .startParameter()
                .xsl(XslNames.OPERATOR_SIGN).type(Param.Type.BOOLEAN).name("Подпись оператора")
                .option(1, "TRUE")
                .option(2, "FALSE")
                .endParameter()
                .startParameter()
                .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
                .hidden(true)
                .option(1, "Vendor1")
                .option(2, "Vendor2")
                .option(3, "Vendor3")
                .endParameter()
                .endParameters()
                .startModel()
                .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
                .param("num1").setNumeric(1)
                .param(XslNames.OPERATOR_SIGN).setBoolean(false).setOption(2)
                .param(XslNames.VENDOR).setOption(1)
                .endModel()
                .startForm()
                .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                .name("block")
                .properties("num1", XslNames.OPERATOR_SIGN)
                .endBlock()
                .endTab()
                .endForm()
                .startVendor()
                .source("http://source1", "ru", new Date())
                .source("http://source2", "en", new Date())
                .endVendor()
                .tovarCategory(1, 2);

            rpcModelSetter.setLoadModel(rpc, data.getModel(), null);
            rpc.setLoadModelData(data.getModelData(), null);

            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("modelEditor", urlKey + "=1")));

            // Параметры, Дополнительно, Совместимость, SKU и привязки SKU,
            // Сматченные оферы
            Assert.assertEquals(6, view.getTabs().size());

            Iterator<EditorWidget> it = view.getTabs().iterator();
            ParamsTab params = (ParamsTab) it.next();
            ParamsTab additional = (ParamsTab) it.next();

            Assert.assertEquals("Параметры модели", params.getTabTitle());
            Assert.assertEquals(1, params.getWidgetCountAtLeft());
            Assert.assertEquals(0, params.getWidgetCountAtRight());

            Assert.assertEquals(1, params.getWidgetsAtLeft().size());
            Assert.assertEquals(0, params.getWidgetsAtRight().size());

            BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
            Assert.assertEquals(2, block.getWidgets().size());

            Assert.assertEquals(1, block.getWidgets().get(0).getParamMeta().getParamId());
            Assert.assertEquals("num1", block.getWidgets().get(0).getParamMeta().getXslName());

            Assert.assertEquals(2, block.getWidgets().get(1).getParamMeta().getParamId());
            Assert.assertEquals(XslNames.OPERATOR_SIGN, block.getWidgets().get(1).getParamMeta().getXslName());

            Assert.assertEquals("Дополнительно", additional.getTabTitle());
            Assert.assertTrue(SourcesWidget.class.isAssignableFrom(additional.getWidgetsAtLeft().get(0).getClass()));
            Assert.assertEquals(1, additional.getWidgetCountAtLeft());
            Assert.assertEquals(0, additional.getWidgetCountAtRight());
        }

        @Test
        public void testGeneratedUILayout5Tab2BlocksCase() {
            ModelDataBuilder data = ModelDataBuilder.modelData()
                .startParameters()
                .startParameter()
                .xsl("num1").type(Param.Type.NUMERIC).name("Num1")
                .endParameter()
                .startParameter()
                .xsl("num2").type(Param.Type.NUMERIC).name("Num2")
                .endParameter()
                .startParameter()
                .xsl("num3").type(Param.Type.NUMERIC).name("Num3")
                .endParameter()
                .startParameter()
                .xsl(XslNames.OPERATOR_SIGN).type(Param.Type.BOOLEAN).name("Подпись оператора")
                .option(1, "TRUE")
                .option(2, "FALSE")
                .endParameter()
                .startParameter()
                .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
                .hidden(true)
                .option(1, "Vendor1")
                .option(2, "Vendor2")
                .option(3, "Vendor3")
                .endParameter()
                .endParameters()
                .startModel()
                .id(2).category(2).source(Source.GURU).currentType(Source.GURU)
                .param("num1").setNumeric(1)
                .param("num2").setNumeric(2)
                .param("num3").setNumeric(3)
                .param(XslNames.OPERATOR_SIGN).setBoolean(false).setOption(2)
                .param(XslNames.VENDOR).setOption(1)
                .endModel()
                .startForm()
                .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                .name("block")
                .properties("num1", "num2")
                .endBlock()
                .startBlock()
                .name("block")
                .properties("num3", XslNames.OPERATOR_SIGN)
                .endBlock()
                .endTab()
                .endForm()
                .startVendor()
                .source("http://source1", "ru", new Date())
                .source("http://source2", "en", new Date())
                .endVendor()
                .tovarCategory(1, 2);

            rpcModelSetter.setLoadModel(rpc, data.getModel(), null);
            rpc.setLoadModelData(data.getModelData(), null);

            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("modelEditor", urlKey + "=2")));

            // Параметры, Дополнительно, Совместимость, SKU и привязки SKU,
            // Сматченные оферы
            Assert.assertEquals(6, view.getTabs().size());

            Iterator<EditorWidget> it = view.getTabs().iterator();
            ParamsTab params = (ParamsTab) it.next();
            ParamsTab additional = (ParamsTab) it.next();

            Assert.assertEquals("Параметры модели", params.getTabTitle());
            Assert.assertEquals(1, params.getWidgetCountAtLeft());
            Assert.assertEquals(1, params.getWidgetCountAtRight());

            Assert.assertEquals(1, params.getWidgetsAtLeft().size());
            Assert.assertEquals(1, params.getWidgetsAtRight().size());

            BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
            Assert.assertEquals(2, block.getWidgets().size());
            Assert.assertEquals("num1", block.getWidgets().get(0).getParamMeta().getXslName());
            Assert.assertEquals("num2", block.getWidgets().get(1).getParamMeta().getXslName());

            block = (BlockWidget) params.getWidgetsAtRight().get(0);
            Assert.assertEquals(2, block.getWidgets().size());
            Assert.assertEquals("num3", block.getWidgets().get(0).getParamMeta().getXslName());
            Assert.assertEquals(XslNames.OPERATOR_SIGN, block.getWidgets().get(1).getParamMeta().getXslName());

            Assert.assertEquals("Дополнительно", additional.getTabTitle());
            Assert.assertTrue(SourcesWidget.class.isAssignableFrom(additional.getWidgetsAtLeft().get(0).getClass()));
            Assert.assertEquals(1, additional.getWidgetCountAtLeft());
            Assert.assertEquals(0, additional.getWidgetCountAtRight());
        }

        @Test
        public void testGeneratedUILayout2Tab3BlocksCase() {
            ModelDataBuilder data = ModelDataBuilder.modelData()
                .startParameters()
                .startParameter()
                .xsl("num1").type(Param.Type.NUMERIC).name("Num1")
                .endParameter()
                .startParameter()
                .xsl("num2").type(Param.Type.NUMERIC).name("Num2")
                .endParameter()
                .startParameter()
                .xsl("num3").type(Param.Type.NUMERIC).name("Num3")
                .endParameter()
                .startParameter()
                .xsl("num4").type(Param.Type.NUMERIC).name("Num4")
                .endParameter()
                .startParameter()
                .xsl(XslNames.OPERATOR_SIGN).type(Param.Type.BOOLEAN).name("Подпись оператора")
                .option(1, "TRUE")
                .option(2, "FALSE")
                .endParameter()
                .startParameter()
                .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
                .hidden(true)
                .option(1, "Vendor1")
                .option(2, "Vendor2")
                .option(3, "Vendor3")
                .endParameter()
                .endParameters()
                .startModel()
                .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
                .param("num1").setNumeric(1)
                .param("num2").setNumeric(2)
                .param("num3").setNumeric(3)
                .param("num4").setNumeric(4)
                .param(XslNames.OPERATOR_SIGN).setBoolean(false).setOption(2)
                .param(XslNames.VENDOR).setOption(1)
                .endModel()
                .startForm()
                .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                .name("block1")
                .properties("num1", "num2")
                .endBlock()
                .startBlock()
                .name("block2")
                .properties("num3", "num4")
                .endBlock()
                .startBlock()
                .name("block3")
                .properties(XslNames.OPERATOR_SIGN)
                .endBlock()
                .endTab()
                .endForm()
                .startVendor()
                .source("http://source1", "ru", new Date())
                .source("http://source2", "en", new Date())
                .endVendor()
                .tovarCategory(1, 2);

            rpcModelSetter.setLoadModel(rpc, data.getModel(), null);
            rpc.setLoadModelData(data.getModelData(), null);

            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("modelEditor", urlKey + "=1")));

            // Параметры, Дополнительно, Совместимость, SKU и привязки SKU,
            // Сматченные оферы
            Assert.assertEquals(6, view.getTabs().size());

            Iterator<EditorWidget> it = view.getTabs().iterator();
            ParamsTab params = (ParamsTab) it.next();
            ParamsTab additional = (ParamsTab) it.next();

            Assert.assertEquals("Параметры модели", params.getTabTitle());
            Assert.assertEquals(2, params.getWidgetCountAtLeft());
            Assert.assertEquals(1, params.getWidgetCountAtRight());

            Assert.assertEquals(2, params.getWidgetsAtLeft().size());
            Assert.assertEquals(1, params.getWidgetsAtRight().size());

            BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
            Assert.assertEquals(2, block.getWidgets().size());
            Assert.assertEquals("num1", block.getWidgets().get(0).getParamMeta().getXslName());
            Assert.assertEquals("num2", block.getWidgets().get(1).getParamMeta().getXslName());

            block = (BlockWidget) params.getWidgetsAtLeft().get(1);
            Assert.assertEquals(1, block.getWidgets().size());
            Assert.assertEquals(XslNames.OPERATOR_SIGN, block.getWidgets().get(0).getParamMeta().getXslName());

            block = (BlockWidget) params.getWidgetsAtRight().get(0);
            Assert.assertEquals(2, block.getWidgets().size());
            Assert.assertEquals("num3", block.getWidgets().get(0).getParamMeta().getXslName());
            Assert.assertEquals("num4", block.getWidgets().get(1).getParamMeta().getXslName());

            Assert.assertEquals("Дополнительно", additional.getTabTitle());
            Assert.assertTrue(SourcesWidget.class.isAssignableFrom(additional.getWidgetsAtLeft().get(0).getClass()));
            Assert.assertEquals(1, additional.getWidgetCountAtLeft());
            Assert.assertEquals(0, additional.getWidgetCountAtRight());
        }

        @Test
        public void testGeneratedUILayout4TabCase() {
            ModelDataBuilder data = ModelDataBuilder.modelData()
                .startParameters()
                .startParameter()
                .xsl("num1").type(Param.Type.NUMERIC).name("Num1")
                .endParameter()
                .startParameter()
                .xsl("num2").type(Param.Type.NUMERIC).name("Num2")
                .endParameter()
                .startParameter()
                .xsl("num3").type(Param.Type.NUMERIC).name("Num3")
                .endParameter()
                .startParameter()
                .xsl("img1").type(Param.Type.STRING).guruType(GuruType.PICTURE).name("Img1")
                .endParameter()
                .startParameter()
                .xsl("img2").type(Param.Type.STRING).guruType(GuruType.PICTURE).name("Img2")
                .endParameter()
                .startParameter()
                .xsl(XslNames.OPERATOR_SIGN).type(Param.Type.BOOLEAN).name("Подпись оператора")
                .option(1, "TRUE")
                .option(2, "FALSE")
                .endParameter()
                .startParameter()
                .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
                .hidden(true)
                .option(1, "Vendor1")
                .option(2, "Vendor2")
                .option(3, "Vendor3")
                .endParameter()
                .endParameters()
                .startModel()
                .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
                .param("num1").setNumeric(1)
                .param("num2").setNumeric(2)
                .param("num3").setNumeric(3)
                .param("img1").setString("http://url1")
                .param("img2").setString("http://url2")
                .param(XslNames.OPERATOR_SIGN).setBoolean(false).setOption(2)
                .param(XslNames.VENDOR).setOption(1)
                .endModel()
                .startForm()
                .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                .name("block1")
                .properties("num1", "num2")
                .endBlock()
                .startBlock()
                .name("block2")
                .properties("num3", XslNames.OPERATOR_SIGN)
                .endBlock()
                .endTab()
                .startTab()
                .name(EditorTabs.PICTURES.getDisplayName())
                .startBlock()
                .name("block3")
                .properties("img1", "img2")
                .endBlock()
                .endTab()
                .endForm()
                .startVendor()
                .source("http://source1", "ru", new Date())
                .source("http://source2", "en", new Date())
                .endVendor()
                .tovarCategory(1, 2);

            rpcModelSetter.setLoadModel(rpc, data.getModel(), null);
            rpc.setLoadModelData(data.getModelData(), null);

            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("modelEditor", urlKey + "=1")));

            // Параметры, Картинки, Дополнительно, Совместимость, SKU и привязки SKU,
            // Сматченные оферы
            Assert.assertEquals(7, view.getTabs().size());

            Iterator<EditorWidget> it = view.getTabs().iterator();
            ParamsTab params = (ParamsTab) it.next();
            ParamsTab pictures = (ParamsTab) it.next();
            ParamsTab additional = (ParamsTab) it.next();

            Assert.assertEquals("Параметры модели", params.getTabTitle());
            Assert.assertEquals(1, params.getWidgetCountAtLeft());
            Assert.assertEquals(1, params.getWidgetCountAtRight());

            Assert.assertEquals(1, params.getWidgetsAtLeft().size());
            Assert.assertEquals(1, params.getWidgetsAtRight().size());

            BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
            Assert.assertEquals(2, block.getWidgets().size());
            Assert.assertEquals("num1", block.getWidgets().get(0).getParamMeta().getXslName());
            Assert.assertEquals("num2", block.getWidgets().get(1).getParamMeta().getXslName());

            block = (BlockWidget) params.getWidgetsAtRight().get(0);
            Assert.assertEquals(2, block.getWidgets().size());
            Assert.assertEquals("num3", block.getWidgets().get(0).getParamMeta().getXslName());
            Assert.assertEquals(XslNames.OPERATOR_SIGN, block.getWidgets().get(1).getParamMeta().getXslName());


            Assert.assertEquals("Картинки", pictures.getTabTitle());
            Assert.assertEquals(1, pictures.getWidgetCountAtLeft());
            Assert.assertEquals(0, pictures.getWidgetCountAtRight());

            Assert.assertEquals(1, pictures.getWidgetsAtLeft().size());
            Assert.assertEquals(0, pictures.getWidgetsAtRight().size());

            block = (BlockWidget) pictures.getWidgetsAtLeft().get(0);
            Assert.assertEquals(2, block.getWidgets().size());
            Assert.assertEquals("img1", block.getWidgets().get(0).getParamMeta().getXslName());
            Assert.assertEquals("img2", block.getWidgets().get(1).getParamMeta().getXslName());

            Assert.assertEquals("Дополнительно", additional.getTabTitle());
            Assert.assertTrue(SourcesWidget.class.isAssignableFrom(additional.getWidgetsAtLeft().get(0).getClass()));
            Assert.assertEquals(1, additional.getWidgetCountAtLeft());
            Assert.assertEquals(0, additional.getWidgetCountAtRight());
        }

        @Test
        public void testGeneratedUILayoutNoFormCase() {
            ModelDataBuilder data = ModelDataBuilder.modelData()
                .startParameters()
                .startParameter()
                .xsl("num1").type(Param.Type.NUMERIC).name("Num1")
                .endParameter()
                .startParameter()
                .xsl("num2").type(Param.Type.NUMERIC).name("Num2")
                .endParameter()
                .startParameter()
                .xsl("str1").type(Param.Type.STRING).name("Str1")
                .endParameter()
                .startParameter()
                .xsl("str2").type(Param.Type.STRING).name("Str2")
                .endParameter()
                .startParameter()
                .xsl(XslNames.OPERATOR_SIGN).type(Param.Type.BOOLEAN).name("Подпись оператора")
                .option(1, "TRUE")
                .option(2, "FALSE")
                .endParameter()
                .startParameter()
                .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
                .hidden(true)
                .option(1, "Vendor1")
                .option(2, "Vendor2")
                .option(3, "Vendor3")
                .endParameter()
                .endParameters()
                .startModel()
                .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
                .param("num1").setNumeric(1)
                .param("num2").setNumeric(2)
                .param("str1").setString("aaa")
                .param("str2").setString("bbb")
                .param(XslNames.OPERATOR_SIGN).setBoolean(false).setOption(2)
                .param(XslNames.VENDOR).setOption(1)
                .endModel()
                .startVendor()
                .source("http://source1", "ru", new Date())
                .source("http://source2", "en", new Date())
                .endVendor()
                .tovarCategory(1, 2);

            rpcModelSetter.setLoadModel(rpc, data.getModel(), null);
            rpc.setLoadModelData(data.getModelData(), null);

            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("modelEditor", urlKey + "=1")));

            // Параметры, Дополнительно, Совместимость, SKU и привязки SKU,
            // Сматченные оферы
            Assert.assertEquals(6, view.getTabs().size());

            Iterator<EditorWidget> it = view.getTabs().iterator();
            ParamsTab params = (ParamsTab) it.next();
            ParamsTab additional = (ParamsTab) it.next();

            Assert.assertEquals("Параметры модели", params.getTabTitle());
            Assert.assertEquals(3, params.getWidgetCountAtLeft());
            Assert.assertEquals(2, params.getWidgetCountAtRight());

            Assert.assertEquals(1,
                ((ParamWidget<?>) params.getWidgetsAtLeft().get(0)).getParamMeta().getParamId());
            Assert.assertEquals("num1",
                ((ParamWidget<?>) params.getWidgetsAtLeft().get(0)).getParamMeta().getXslName());

            Assert.assertEquals(3,
                ((ParamWidget<?>) params.getWidgetsAtLeft().get(1)).getParamMeta().getParamId());
            Assert.assertEquals("str1",
                ((ParamWidget<?>) params.getWidgetsAtLeft().get(1)).getParamMeta().getXslName());

            Assert.assertEquals(5,
                ((ParamWidget<?>) params.getWidgetsAtLeft().get(2)).getParamMeta().getParamId());
            Assert.assertEquals(XslNames.OPERATOR_SIGN,
                ((ParamWidget<?>) params.getWidgetsAtLeft().get(2)).getParamMeta().getXslName());

            Assert.assertEquals(2,
                ((ParamWidget<?>) params.getWidgetsAtRight().get(0)).getParamMeta().getParamId());
            Assert.assertEquals("num2",
                ((ParamWidget<?>) params.getWidgetsAtRight().get(0)).getParamMeta().getXslName());

            Assert.assertEquals(4,
                ((ParamWidget<?>) params.getWidgetsAtRight().get(1)).getParamMeta().getParamId());
            Assert.assertEquals("str2",
                ((ParamWidget<?>) params.getWidgetsAtRight().get(1)).getParamMeta().getXslName());

            Assert.assertEquals("Дополнительно", additional.getTabTitle());
            Assert.assertTrue(SourcesWidget.class.isAssignableFrom(additional.getWidgetsAtLeft().get(0).getClass()));
            Assert.assertEquals(1, additional.getWidgetCountAtLeft());
            Assert.assertEquals(0, additional.getWidgetCountAtRight());
        }

        @Test
        @SuppressWarnings("checkstyle:methodLength")
        public void testInitialUIState() {
            Date date = new Date();
            ModelDataBuilder data = ModelDataBuilder.modelData()
                .startParameters()
                .startParameter()
                .xsl("num").type(Param.Type.NUMERIC).name("Num")
                .endParameter()
                .startParameter()
                .xsl("str").type(Param.Type.STRING).name("Str")
                .endParameter()
                .startParameter()
                .xsl("enum").type(Param.Type.ENUM).name("Enum")
                .endParameter()
                .startParameter()
                .xsl("bool").type(Param.Type.BOOLEAN).name("Bool")
                .option(1, "TRUE")
                .option(2, "FALSE")
                .endParameter()
                .startParameter()
                .xsl(XslNames.OPERATOR_SIGN).type(Param.Type.BOOLEAN).name("Подпись оператора")
                .option(1, "TRUE")
                .option(2, "FALSE")
                .endParameter()
                .startParameter()
                .xsl(XslNames.OPERATOR_COMMENT).type(Param.Type.STRING).name("Комментарий оператора")
                .endParameter()
                .startParameter()
                .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
                .hidden(true)
                .option(1, "Vendor1")
                .option(2, "Vendor2")
                .option(3, "Vendor3")
                .endParameter()
                .endParameters()
                .startModel()
                .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
                .param("num").setNumeric(1)
                .param("str").setString("text")
                .param("enum").setOption(1)
                .param("bool").setOption(1).setBoolean(true)
                .param(XslNames.OPERATOR_SIGN).setBoolean(false).setOption(2)
                .param(XslNames.OPERATOR_COMMENT).setString("Комментарий оператора")
                .param(XslNames.VENDOR).setOption(1)
                .endModel()
                .startForm()
                .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                .name("block1")
                .properties("num", "str")
                .endBlock()
                .startBlock()
                .name("block2")
                .properties("enum", "bool")
                .endBlock()
                .startBlock()
                .name("block3")
                .properties(XslNames.OPERATOR_SIGN)
                .endBlock()
                .endTab()
                .endForm()
                .startVendor()
                .source("http://source1", "ru", date)
                .source("http://source2/", "en", date)
                .source("http://source3", null, date)
                .endVendor()
                .tovarCategory(1, 2);

            rpcModelSetter.setLoadModel(rpc, data.getModel(), null);
            rpc.setLoadModelData(data.getModelData(), null);

            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("modelEditor", urlKey + "=1")));

            // Параметры, Дополнительно, Совместимость, SKU и привязки SKU,
            // Сматченные оферы
            Assert.assertEquals(6, view.getTabs().size());

            Iterator<EditorWidget> it = view.getTabs().iterator();
            ParamsTab params = (ParamsTab) it.next();
            ParamsTab additional = (ParamsTab) it.next();

            Assert.assertEquals("Параметры модели", params.getTabTitle());
            Assert.assertEquals(2, params.getWidgetCountAtLeft());
            Assert.assertEquals(1, params.getWidgetCountAtRight());

            Assert.assertEquals(2, params.getWidgetsAtLeft().size());
            Assert.assertEquals(1, params.getWidgetsAtRight().size());

            BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
            Assert.assertEquals(2, block.getWidgets().size());

            ValueWidget<?> valueWidget = block.getWidgets().get(0).getValuesWidget().getFirstValueWidget();
            Assert.assertEquals(1, block.getWidgets().get(0).getParamMeta().getParamId());
            Assert.assertEquals("num", block.getWidgets().get(0).getParamMeta().getXslName());
            Assert.assertEquals("Num", block.getWidgets().get(0).getNameLabel());
            Assert.assertEquals("", valueWidget.getErrorMessage());
            Assert.assertEquals("", block.getWidgets().get(0).getValuesWidget().getOldValueMessage());
            Assert.assertEquals(true, valueWidget.isEnabled());

            valueWidget = block.getWidgets().get(1).getValuesWidget().getFirstValueWidget();
            Assert.assertEquals(2, block.getWidgets().get(1).getParamMeta().getParamId());
            Assert.assertEquals("str", block.getWidgets().get(1).getParamMeta().getXslName());
            Assert.assertEquals("Str", block.getWidgets().get(1).getNameLabel());
            Assert.assertEquals("", valueWidget.getErrorMessage());
            Assert.assertEquals("", block.getWidgets().get(1).getValuesWidget().getOldValueMessage());
            Assert.assertEquals(true, valueWidget.isEnabled());

            block = (BlockWidget) params.getWidgetsAtRight().get(0);
            Assert.assertEquals(2, block.getWidgets().size());

            valueWidget = block.getWidgets().get(0).getValuesWidget().getFirstValueWidget();
            Assert.assertEquals(3, block.getWidgets().get(0).getParamMeta().getParamId());
            Assert.assertEquals("enum", block.getWidgets().get(0).getParamMeta().getXslName());
            Assert.assertEquals("Enum", block.getWidgets().get(0).getNameLabel());
            Assert.assertEquals("", valueWidget.getErrorMessage());
            Assert.assertEquals("", block.getWidgets().get(0).getValuesWidget().getOldValueMessage());
            Assert.assertEquals(true, valueWidget.isEnabled());

            valueWidget = block.getWidgets().get(1).getValuesWidget().getFirstValueWidget();
            Assert.assertEquals(4, block.getWidgets().get(1).getParamMeta().getParamId());
            Assert.assertEquals("bool", block.getWidgets().get(1).getParamMeta().getXslName());
            Assert.assertEquals("Bool", block.getWidgets().get(1).getNameLabel());
            Assert.assertEquals("", valueWidget.getErrorMessage());
            Assert.assertEquals("", block.getWidgets().get(1).getValuesWidget().getOldValueMessage());
            Assert.assertEquals(true, valueWidget.isEnabled());

            block = (BlockWidget) params.getWidgetsAtLeft().get(1);
            Assert.assertEquals(1, block.getWidgets().size());

            valueWidget = block.getWidgets().get(0).getValuesWidget().getFirstValueWidget();
            Assert.assertEquals(5, block.getWidgets().get(0).getParamMeta().getParamId());
            Assert.assertEquals(XslNames.OPERATOR_SIGN, block.getWidgets().get(0).getParamMeta().getXslName());
            Assert.assertEquals("Подпись оператора", block.getWidgets().get(0).getNameLabel());
            Assert.assertEquals("", valueWidget.getErrorMessage());
            Assert.assertEquals("", block.getWidgets().get(0).getValuesWidget().getOldValueMessage());
            Assert.assertEquals(true, valueWidget.isEnabled());

            Assert.assertEquals("Дополнительно", additional.getTabTitle());
            Assert.assertTrue(SourcesWidget.class.isAssignableFrom(additional.getWidgetsAtRight().get(0).getClass()));
            Assert.assertEquals(1, additional.getWidgetCountAtLeft());
            Assert.assertEquals(1, additional.getWidgetCountAtRight());

            SourcesWidget sourcesWidget = (SourcesWidget) additional.getWidgetsAtRight().get(0);
            Assert.assertEquals(3, sourcesWidget.getRows().size());
            Assert.assertEquals("ru", sourcesWidget.getRows().get(0).getLang());
            Assert.assertEquals(date, sourcesWidget.getRows().get(0).getDate());
            Assert.assertEquals("http://source1", sourcesWidget.getRows().get(0).getSite());
            Assert.assertEquals("en", sourcesWidget.getRows().get(1).getLang());
            Assert.assertEquals(date, sourcesWidget.getRows().get(1).getDate());
            Assert.assertEquals("http://source2", sourcesWidget.getRows().get(1).getSite());
            Assert.assertEquals("Определяется автоматически", sourcesWidget.getRows().get(2).getLang());
            Assert.assertEquals(date, sourcesWidget.getRows().get(2).getDate());
            Assert.assertEquals("http://source3", sourcesWidget.getRows().get(2).getSite());

            BlockWidget additionalBlock = (BlockWidget) additional.getWidgetsAtLeft().get(0);
            ParamWidget<?> commentWidget = additionalBlock.getWidgets().get(0);
            Assert.assertEquals(Arrays.asList("Комментарий оператора"),
                commentWidget.getValuesWidget().getFirstValueWidget().getValue());

            // Поскольку заполнен параметр "Комментарий оператора", вкладка "Дополнительно" - темного цвета
            Assert.assertTrue(view.isTabHeaderDark(EditorTabs.ADDITIONAL.getDisplayName()));
        }

        @Test
        public void testOpenSeveralModelsInOneEditor() {
            // Хитрый тест - запускает последовательно друг за другом тесты
            // с разными urlKey, чтобы проверить, эмулируя
            // переходы по якорям
            //Открыть и протестировать модель с urlKey=1
            testGeneratedUILayout3Tab1BlocksCase();
            //Открыть и протестировать модель с urlKey=2
            testGeneratedUILayout5Tab2BlocksCase();
        }

        @Test
        public void testOpenCluster() {
            ModelDataBuilder data = ModelDataBuilder.modelData()
                .startParameters()
                .startParameter()
                .xsl("num1").type(Param.Type.NUMERIC).name("Num1")
                .endParameter()
                .startParameter()
                .xsl(XslNames.OPERATOR_SIGN).type(Param.Type.BOOLEAN).name("Подпись оператора")
                .option(1, "TRUE")
                .option(2, "FALSE")
                .endParameter()
                .startParameter()
                .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
                .hidden(true)
                .option(1, "Vendor1")
                .option(2, "Vendor2")
                .option(3, "Vendor3")
                .endParameter()
                .endParameters()
                .startModel()
                .id(1).category(2).source(Source.CLUSTER).currentType(Source.CLUSTER)
                .param("num1").setNumeric(1)
                .param(XslNames.VENDOR).setOption(1)
                .endModel()
                .startForm()
                .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                .name("block")
                .properties("num1", XslNames.OPERATOR_SIGN)
                .endBlock()
                .endTab()
                .endForm()
                .startVendor()
                .source("http://source1", "ru", new Date())
                .source("http://source2", "en", new Date())
                .endVendor()
                .tovarCategory(1, 2);

            rpcModelSetter.setLoadModel(rpc, data.getModel(), null);
            rpc.setLoadModelData(data.getModelData(), null);

            bus.fireEvent(
                new PlaceShowEvent(
                    EditorUrlStub.of("modelEditor", urlKey + "=1")));

            // Должно быть 2 вкладки - Параметры, Дополнительно (в которой содержится SourceWidget)
            Assert.assertEquals(2, view.getTabs().size());

            Iterator<EditorWidget> it = view.getTabs().iterator();
            ParamsTab params = (ParamsTab) it.next();
            ParamsTab additional = (ParamsTab) it.next();

            Assert.assertEquals("Параметры модели", params.getTabTitle());
            Assert.assertEquals(1, params.getWidgetCountAtLeft());
            Assert.assertEquals(0, params.getWidgetCountAtRight());

            Assert.assertEquals(1, params.getWidgetsAtLeft().size());
            Assert.assertEquals(0, params.getWidgetsAtRight().size());

            BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
            Assert.assertEquals(2, block.getWidgets().size());

            Assert.assertEquals(1, block.getWidgets().get(0).getParamMeta().getParamId());
            Assert.assertEquals("num1", block.getWidgets().get(0).getParamMeta().getXslName());

            Assert.assertEquals(2, block.getWidgets().get(1).getParamMeta().getParamId());
            Assert.assertEquals(XslNames.OPERATOR_SIGN, block.getWidgets().get(1).getParamMeta().getXslName());

            Assert.assertEquals("Дополнительно", additional.getTabTitle());
            Assert.assertTrue(SourcesWidget.class.isAssignableFrom(additional.getWidgetsAtLeft().get(0).getClass()));
            Assert.assertEquals(1, additional.getWidgetCountAtLeft());
            Assert.assertEquals(0, additional.getWidgetCountAtRight());
        }
    }
}
