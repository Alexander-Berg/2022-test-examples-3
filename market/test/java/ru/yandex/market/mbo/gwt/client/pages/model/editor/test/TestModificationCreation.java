package ru.yandex.market.mbo.gwt.client.pages.model.editor.test;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.ModelCreatedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.BlockWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.EditorWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamsTab;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.SourcesWidget;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel.Source;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Arrays;
import java.util.Iterator;

/**
 * @author gilmulla
 */
@SuppressWarnings("checkstyle:magicNumber")
public class TestModificationCreation extends AbstractModelTest {
    CommonModel model;

    private static final int VENDOR_ID3 = 3;

    @Override
    public void parameters() {
        data.startParameters()
            .startParameter()
                .xsl("num1").type(Param.Type.NUMERIC).name("Num1").hidden(false).creation(true)
            .endParameter()
            .startParameter()
                .xsl("num2").type(Param.Type.NUMERIC).name("Num2").hidden(true).creation(true)
            .endParameter()
            .startParameter()
                .xsl("str1").type(Param.Type.STRING).name("Str1").hidden(false).creation(false)
            .endParameter()
            .startParameter()
                .xsl("str2").type(Param.Type.STRING).name("Str2").hidden(false).creation(true)
            .endParameter()
            .startParameter()
                .xsl(XslNames.OPERATOR_SIGN).type(Param.Type.BOOLEAN).name("Подпись оператора")
                .option(1, "TRUE")
                .option(2, "FALSE")
                .hidden(false).creation(false)
            .endParameter()
            .startParameter()
                .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
                .hidden(true)
                .option(1, "Vendor1")
                .option(2, "Vendor2")
                .option(VENDOR_ID3, "Vendor3")
            .endParameter()
        .endParameters();
    }

    @Override
    public void model() {
        data.startModel()
            .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
            .param("num1").setNumeric(1).modificationSource(ModificationSource.BACKEND_RULE)
            .param("str1").setString("aaa").modificationSource(ModificationSource.BACKEND_RULE)
            .param(XslNames.VENDOR).setOption(1)
        .endModel();
    }

    @Override
    public void run() {
        rpc.setSampleNames(Arrays.asList("name1", "name2"), null);
        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);

        bus.subscribe(ModelCreatedEvent.class, event -> {
            model = event.getModel();
        });

        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelCreator", "parent-id=1")));
    }

    @Test
    public void testOpeningNewModel() {
        Assert.assertNotNull(model);
        // У новой модели должна быть категория как у родительской модели
        Assert.assertEquals(2, model.getCategoryId());

        // Панель копирования должна быть скрыта при создании модели
        Assert.assertFalse(view.isCopyPanelVisible());

        // Не должно быть никаких сообщений
        assertSuccessMessage("");
        assertWarningMessage("");
        assertWaitingMessage("");

        // Должно быть 2 вкладки - Параметры и Дополнительно (в которой содержится SourceWidget)
        Assert.assertEquals(2, view.getTabs().size());

        Iterator<EditorWidget> it = view.getTabs().iterator();
        ParamsTab params = (ParamsTab) it.next();
        ParamsTab additional = (ParamsTab) it.next();

        Assert.assertEquals("Параметры модели", params.getTabTitle());
        // При создании модели признак скрытости параметра игнорируется, они участвуют при создании
        Assert.assertEquals(1, params.getWidgetCountAtLeft());
        Assert.assertEquals(1, params.getWidgetCountAtRight());

        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        Assert.assertEquals(2, block.getWidgets().size());

        Assert.assertEquals(1, block.getWidgets().get(0).getParamMeta().getParamId());
        Assert.assertEquals("num1", block.getWidgets().get(0).getParamMeta().getXslName());

        Assert.assertEquals(2, block.getWidgets().get(1).getParamMeta().getParamId());
        Assert.assertEquals("num2", block.getWidgets().get(1).getParamMeta().getXslName());

        block = (BlockWidget) params.getWidgetsAtRight().get(0);
        Assert.assertEquals(1, block.getWidgets().size());

        final int expectedStrParamId = 4;
        Assert.assertEquals(expectedStrParamId, block.getWidgets().get(0).getParamMeta().getParamId());
        Assert.assertEquals("str2", block.getWidgets().get(0).getParamMeta().getXslName());

        Assert.assertEquals("Дополнительно", additional.getTabTitle());
        Assert.assertTrue(SourcesWidget.class.isAssignableFrom(additional.getWidgetsAtLeft().get(0).getClass()));
        Assert.assertEquals(1, additional.getWidgetCountAtLeft());
        Assert.assertEquals(0, additional.getWidgetCountAtRight());
    }

//    @Test
//    public void testSavingNewModel() {
//        ModelDataBuilder data = ModelDataBuilder.modelData()
//        .startParameters()
//            .startParameter()
//                .xsl("num1").type(Param.Type.NUMERIC).name("Num1").creation(true)
//            .endParameter()
//            .startParameter()
//                .xsl("num2").type(Param.Type.NUMERIC).name("Num2").creation(false)
//            .endParameter()
//            .startParameter()
//                .xsl("num3").type(Param.Type.NUMERIC).name("Num3").creation(true)
//            .endParameter()
//            .startParameter()
//                .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
//                .hidden(true)
//                .option(1, "Vendor1")
//                .option(2, "Vendor2")
//                .option(3, "Vendor3")
//            .endParameter()
//        .endParameters()
//        .startForm()
//            .startTab()
//                .name(EditorTabs.PARAMETERS.getDisplayName())
//                .startBlock()
//                    .name("block")
//                    .properties("num1", "num2", "num3")
//                .endBlock()
//            .endTab()
//        .endForm()
//        .startVendor()
//            .source("http://source1", "ru", new Date())
//            .source("http://source2", "en", new Date())
//        .endVendor()
//        .tovarCategory(1, 2);
//
//        rpc.setSampleNames(Arrays.asList("name1", "name2"), null);
//        rpc.setLoadModelData(data.getModelData(), null);
//
//        bus.fireEvent(
//                new PlaceShowEvent(
//                        EditorUrlStub.of("modelCreator", "vendor-id=11&category-id=1")));
//
//        ParamsTab params = (ParamsTab) view.getTabs().iterator().next();
//        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
//        ParamWidget<Object> widget1 = (ParamWidget<Object>) block.getWidgets().get(0);
//
//        // Задаем значение первого параметра, второй оставляем пустым
//        widget1.setValue("5");
//        bus.fireEvent(new WidgetValueChanged(widget1, "5"));
//
//        // Сохраняем модель
//        rpc.setSaveModel(1l, null);
//        bus.fireEvent(
//                new SaveModelRequest(false, false));
//
//        // В сохраненной модели должны быть два параметра - num1 и автоматически проставленный вендор
//        CommonModel savedModel = rpc.getSavedModel();
//        Assert.assertEquals(2, savedModel.getParameterValues().size());
//        ParameterValue num1 = savedModel.getParameterValue("num1");
//        Assert.assertNotNull(num1);
//        Assert.assertEquals(Param.Type.NUMERIC, num1.getType());
//        Assert.assertEquals(5, num1.getNumericValue().intValue());
//        ParameterValue vendor = savedModel.getParameterValue(XslNames.VENDOR);
//        Assert.assertNotNull(vendor);
//        Assert.assertEquals(Param.Type.ENUM, vendor.getType());
//        Assert.assertEquals(11L, vendor.getOptionId().longValue());
//    }
}
