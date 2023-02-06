package ru.yandex.market.mbo.gwt.client.pages.model.editor.test;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelDataBuilder;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.ModelUIGeneratedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.ParamConfirmEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.SaveModelRequest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditableModel;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditableParameter;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditableValue;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.BlockWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.EditorWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamsTab;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueWidget;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel.Source;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author york
 * @since 05.12.17
 */
@SuppressWarnings("checkstyle:magicNumber")
public class TestParamConfirmation extends AbstractTest {

    //TODO - fix test, it fails after moving to arcadia and didn't start in github because of bad regexp:
    //https://a.yandex-team.ru/arc_vcs/market/mbo/mbo-catalog/build.gradle?rev=r9351105#L178,
    @Test
    @Ignore
    public void testConfirmation() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("num").type(Param.Type.NUMERIC).name("Num")
            .endParameter()
            .startParameter()
                .xsl("num2").type(Param.Type.NUMERIC).name("Num2")
            .endParameter()
            .startParameter()
                .xsl(XslNames.XL_PICTURE).type(Param.Type.STRING).name("XLPicture")
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
            .param("num").setNumeric(1).modificationSource(ModificationSource.AUTO)
            .param("num2").setNumeric(5).modificationSource(ModificationSource.OPERATOR_FILLED)
            .param(XslNames.XL_PICTURE).setString("url1").modificationSource(ModificationSource.AUTO)
            .param(XslNames.OPERATOR_SIGN).setBoolean(false).setOption(2)
                .modificationSource(ModificationSource.OPERATOR_FILLED)
            .param(XslNames.VENDOR).setOption(1).modificationSource(ModificationSource.OPERATOR_FILLED)
        .endModel()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("num", "num2", XslNames.OPERATOR_SIGN)
                .endBlock()
            .endTab()
            .startTab()
            .name(EditorTabs.PICTURES.getDisplayName())
                .startBlock()
                    .name("bloc2k")
                    .properties(XslNames.XL_PICTURE)
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

        AtomicReference<EditableModel> editableModel = new AtomicReference<>();
        bus.subscribe(ModelUIGeneratedEvent.class, event -> {
            editableModel.set(event.getEditableModel());
        });

        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelEditor", "entity-id=1")));

        Assert.assertEquals(8, view.getTabs().size());

        Iterator<EditorWidget> it = view.getTabs().iterator();
        ParamsTab params = (ParamsTab) it.next();

        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        ValueWidget<?> numWidget = null;
        for (ParamWidget<?> widget : block.getWidgets()) {
            if (widget.getParamMeta().getXslName().equals("num")) {
                numWidget = widget.getFirstValueWidget();
                Assert.assertTrue(numWidget.isConfirmCheckboxVisible());
                Assert.assertFalse(numWidget.getConfirmCheckboxStatus());
            } else {
                Assert.assertFalse(widget.getParamMeta().getXslName() + " with confirm",
                    widget.getFirstValueWidget().isConfirmCheckboxVisible());
            }
        }
        Assert.assertNotNull(numWidget);
        numWidget.setConfirmCheckboxStatus(true);

        EditableParameter numParam = editableModel.get().getEditableParameter(numWidget.getParamMeta().getParamId());
        EditableValue editableValue = numParam.getEditableValues().getEditableValue(numWidget);
        Assert.assertEquals(ModificationSource.OPERATOR_CONFIRMED,
            editableValue.getParameterValue().getModificationSource());
        rpc.setSaveModel(editableModel.get().getModel().getId(), null);

        bus.fireEvent(new SaveModelRequest(false, false));


        Assert.assertEquals(false, numWidget.isConfirmCheckboxVisible());
        // update pic
        EditableParameter pictureParam = editableModel.get().getEditableParameters()
            .stream().filter(ep -> ep.getParamMeta().getXslName().equals(XslNames.XL_PICTURE))
            .findFirst().orElse(null);

        Assert.assertNotNull(pictureParam);
        Assert.assertTrue(pictureParam.getFirstEditableValue().isConfirmCheckboxVisible());
        Assert.assertFalse(pictureParam.getFirstEditableValue().getValueWidget().getConfirmCheckboxStatus());

        ParameterValue pv = editableModel.get().getModel()
            .getEffectiveSingleParameterValue(XslNames.XL_PICTURE).get();

        pv.setStringValue(WordUtil.defaultWords("url2"));
        pv.setModificationSource(ModificationSource.OPERATOR_FILLED);
    }

    /**
     * Регресионный тест.
     * 1. Меняем значение у параметра
     * 2. Подтверждаем параметр
     * 3. Снимаем подтверждение параметра
     * 4. Сохраняем параметр
     * Ожидаем что у параметра сохранится исходное значение
     */
    @Test
    public void changeValueThenConfirmationThenRemoveConfrimation() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
            .startParameters()
                .startParameter()
                    .xslAndName("PARAMETER_1").type(Param.Type.ENUM)
                    .option(1, "Option1")
                    .option(2, "Option2")
                .endParameter()
                .startParameter()
                    .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
                    .hidden(true)
                    .option(1, "Vendor1")
                    .option(2, "Vendor2")
                .endParameter()
            .endParameters()
            .startModel()
                .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
                .param("PARAMETER_1").setOption(1).modificationSource(ModificationSource.AUTO)
                .param(XslNames.VENDOR).setOption(1)
            .endModel()
            .startForm()
                .startTab()
                    .name(EditorTabs.PARAMETERS.getDisplayName())
                    .startBlock()
                        .name("block")
                        .properties("PARAMETER_1")
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

        AtomicReference<EditableModel> editableModel = new AtomicReference<>();
        bus.subscribe(ModelUIGeneratedEvent.class, event -> {
            editableModel.set(event.getEditableModel());
        });

        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", "entity-id=1")));

        ParamWidget<Object> parameter1Widget = null;
        ParamsTab params = (ParamsTab) view.getTabs().iterator().next();
        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        for (ParamWidget<?> widget : block.getWidgets()) {
            if (widget.getParamMeta().getXslName().equals("PARAMETER_1")) {
                parameter1Widget = (ParamWidget<Object>) widget;
                break;
            }
        }

        // change value
        Option option2 = data.getModelData().getParam(1).getOptions().get(1);
        parameter1Widget.getFirstValueWidget().setValue(option2, true);

        // confirmation
        parameter1Widget.getFirstValueWidget().setConfirmCheckboxStatus(true);
        EditableParameter parameter1 = editableModel.get().getEditableParameter(
            parameter1Widget.getParamMeta().getParamId());
        EditableValue editableValue = parameter1.getEditableValues()
            .getEditableValue(parameter1Widget.getFirstValueWidget());
        bus.fireEvent(new ParamConfirmEvent(editableValue, true));

        // remove confirmation
        parameter1Widget.getFirstValueWidget().setConfirmCheckboxStatus(false);
        bus.fireEvent(new ParamConfirmEvent(editableValue, false));

        rpc.setSaveModel(1L, null);
        bus.fireEvent(new SaveModelRequest(false, false));

        CommonModel savedModel = rpc.getSavedModel();

        Assert.assertEquals(Long.valueOf(1), savedModel.getSingleParameterValue("PARAMETER_1").getOptionId());

    }
}
