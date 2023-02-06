package ru.yandex.market.mbo.gwt.client.pages.model.editor.test;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelDataBuilder;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.SaveModelRequest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ViewUtils;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.BlockWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamsTab;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueWidget;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel.Source;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.ArrayList;
import java.util.Date;

/**
 * @author gilmulla
 */
@SuppressWarnings("checkstyle:magicNumber")
public class TestModificationEditing extends AbstractTest {
    @Test
    public void testModificationParamHighlightingOnOpening() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("param").type(Param.Type.NUMERIC).name("Param")
            .endParameter()
            .startParameter()
                .xsl("overriddenParam").type(Param.Type.NUMERIC).name("Overridden Param")
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
            .param("overriddenParam").setNumeric(3).modificationSource(ModificationSource.BACKEND_RULE)
            .param(XslNames.VENDOR).setOption(1)
            .startParentModel()
                .id(2).category(2).source(Source.GURU).currentType(Source.GURU)
                .param("param").setNumeric(1).modificationSource(ModificationSource.BACKEND_RULE)
                .param("overriddenParam").setNumeric(2).modificationSource(ModificationSource.BACKEND_RULE)
            .endModel()
        .endModel()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("param", "overriddenParam")
                .endBlock()
            .endTab()
        .endForm()
        .startVendor()
            .source("http://source1", "ru", new Date())
            .source("http://source2", "en", new Date())
        .endVendor()
        .tovarCategory(1, 2);

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);

        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelEditor", "entity-id=1")));

        ParamsTab params = (ParamsTab) view.getTabs().iterator().next();
        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        ParamWidget<?> widget1 = ViewUtils.getParamWidget(block, 0);
        ParamWidget<?> widget2 = ViewUtils.getParamWidget(block, 1);

        // Значение, заданное в параметре, подсвечивается как модель
        Assert.assertEquals(ParamWidget.BackgroundColor.MODEL, widget1.getBackgroundColor());
        // Значение, заданное в модификации, подсвечивается как модификация
        Assert.assertEquals(ParamWidget.BackgroundColor.MODIFICATION, widget2.getBackgroundColor());
    }

    @Test
    public void testModificationParamHighlightingOnEdit() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("param").type(Param.Type.NUMERIC).name("Param")
            .endParameter()
            .startParameter()
                .xsl("overriddenParam").type(Param.Type.NUMERIC).name("Overridden Param")
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
            .param(XslNames.VENDOR).setOption(1)
            .startParentModel()
                .param("param").setNumeric(1).modificationSource(ModificationSource.BACKEND_RULE)
                .param("overriddenParam").setNumeric(2).modificationSource(ModificationSource.BACKEND_RULE)
                .id(2).category(2).source(Source.GURU).currentType(Source.GURU)
            .endModel()
        .endModel()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("param", "overriddenParam")
                .endBlock()
            .endTab()
        .endForm()
        .startVendor()
            .source("http://source1", "ru", new Date())
            .source("http://source2", "en", new Date())
        .endVendor()
        .tovarCategory(1, 2);

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);

        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelEditor", "entity-id=1")));

        ParamsTab params = (ParamsTab) view.getTabs().iterator().next();
        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        ParamWidget<?> widget1 = ViewUtils.getParamWidget(block, 0);
        ParamWidget<?> widget2 = ViewUtils.getParamWidget(block, 1);
        ValueWidget<Object> valueWidget2 = (ValueWidget<Object>) ViewUtils.getValueWidget(block, 1);

        // Изначально они подсвечены цветом для модели
        Assert.assertEquals(ParamWidget.BackgroundColor.MODEL, widget1.getBackgroundColor());
        Assert.assertEquals(ParamWidget.BackgroundColor.MODEL, widget2.getBackgroundColor());

        // Редактируем параметр overriddenParam, после чего он становится заданным в модификации
        valueWidget2.setValue(String.valueOf(3), true);

        // Параметр param остается подсвечен как модель
        Assert.assertEquals(ParamWidget.BackgroundColor.MODEL, widget1.getBackgroundColor());
        // Параметр param становится подсвечен как модификация
        Assert.assertEquals(ParamWidget.BackgroundColor.MODIFICATION, widget2.getBackgroundColor());

        rpc.setSaveModel(1L, null);
        bus.fireEvent(
                new SaveModelRequest(false, false));

        CommonModel savedModel = rpc.getSavedModel();
        Assert.assertEquals(2, savedModel.getParameterValues().size());
        Assert.assertEquals(1, savedModel.getEffectiveSingleParameterValue("param").get().getNumericValue().intValue());
        Assert.assertEquals(3,
            savedModel.getEffectiveSingleParameterValue("overriddenParam").get().getNumericValue().intValue());

        // Проверить, что после сохранения цвет не изменился
        // Параметр param остается подсвечен как модель
        Assert.assertEquals(ParamWidget.BackgroundColor.MODEL, widget1.getBackgroundColor());
        // Параметр param подсвечен как модификация
        Assert.assertEquals(ParamWidget.BackgroundColor.MODIFICATION, widget2.getBackgroundColor());

    }

    @Test
    public void testModificationParamHighlightingOnBackToModel() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("param").type(Param.Type.NUMERIC).name("Param")
            .endParameter()
            .startParameter()
                .xsl("overriddenParam").type(Param.Type.NUMERIC).name("Overridden Param")
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
            .param("overriddenParam").setNumeric(3).modificationSource(ModificationSource.BACKEND_RULE)
            .param(XslNames.VENDOR).setOption(1)
            .startParentModel()
                .id(2).category(2).source(Source.GURU).currentType(Source.GURU)
                .param("param").setNumeric(1).modificationSource(ModificationSource.BACKEND_RULE)
                .param("overriddenParam").setNumeric(2).modificationSource(ModificationSource.BACKEND_RULE)
            .endModel()
        .endModel()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("param", "overriddenParam")
                .endBlock()
            .endTab()
        .endForm()
        .startVendor()
            .source("http://source1", "ru", new Date())
            .source("http://source2", "en", new Date())
        .endVendor()
        .tovarCategory(1, 2);

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);

        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelEditor", "entity-id=1")));

        ParamsTab params = (ParamsTab) view.getTabs().iterator().next();
        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        ParamWidget<?> widget1 = ViewUtils.getParamWidget(block, 0);
        ParamWidget<?> widget2 = ViewUtils.getParamWidget(block, 1);
        ValueWidget<Object> valueWidget2 = (ValueWidget<Object>) ViewUtils.getValueWidget(block, 1);

        // Значение, заданное в параметре, подсвечивается как модель
        Assert.assertEquals(ParamWidget.BackgroundColor.MODEL, widget1.getBackgroundColor());
        // Значение, заданное в модификации, подсвечивается как модификация
        Assert.assertEquals(ParamWidget.BackgroundColor.MODIFICATION, widget2.getBackgroundColor());

        // Редактируем параметр overriddenParam, устанавливая значение как в модели:
        valueWidget2.setValue("2", true);

        // Подсветка сразу после изменения не должна измениться - потому что генерализация еще не
        // перенесла параметр в родительскую модель
        // Параметр param остается подсвечен как модель
        Assert.assertEquals(ParamWidget.BackgroundColor.MODEL, widget1.getBackgroundColor());
        // Параметр overridedParam становится подсвечен как модификация
        Assert.assertEquals(ParamWidget.BackgroundColor.MODIFICATION, widget2.getBackgroundColor());

        // Эмулируем генерализацию, которая удалит значение в модификации, совпадающее со значением в модели
        CommonModel generalizedModel = CommonModelBuilder.builder(m -> m)
            .parameters(new ArrayList<>(data.getModelData().getParams()))
            .startModel()
                .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
                .param(XslNames.VENDOR).setOption(1)
                .startParentModel()
                    .id(2).category(2).source(Source.GURU).currentType(Source.GURU)
                    .param("param").setNumeric(1).modificationSource(ModificationSource.BACKEND_RULE)
                    .param("overriddenParam").setNumeric(2).modificationSource(ModificationSource.BACKEND_RULE)
                .endModel()
            .endModel();
        rpc.setLoadModel(generalizedModel, null);
        rpc.setRewriteLoadModelOnSave(false);
        // А вот после сохранения модели и отработки генерализации цвет должен измениться
        rpc.setSaveModel(1L, null);
        bus.fireEvent(
                new SaveModelRequest(false, false));

        // Проверить, что после сохранения цвет изменился
        // Параметр param остается подсвечен как модель
        Assert.assertEquals(ParamWidget.BackgroundColor.MODEL, widget1.getBackgroundColor());
        // Параметр overridedParam после генерализации стал подсвечен тоже как модель
        Assert.assertEquals(ParamWidget.BackgroundColor.MODEL, widget2.getBackgroundColor());
    }

}
