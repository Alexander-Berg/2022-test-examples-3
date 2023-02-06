package ru.yandex.market.mbo.gwt.client.pages.model.editor.test;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelDataBuilder;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.DeleteModelRequestEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.ModelDataLoadedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.ModelUIGeneratedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.RemoteModelChangedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.RemoteModelChangedEvent.Cause;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.SaveModelRequest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.save.PopulateModelSaveSyncEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.SelectModelSuccessorsWidgetStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ValueFieldStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.BlockWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamsTab;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ErrorWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueWidget.BackgroundColor;
import ru.yandex.market.mbo.gwt.models.IdAndName;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel.Source;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRemovalType;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelTransition;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author gilmulla
 */
@SuppressWarnings({"checkstyle:magicNumber", "checkstyle:lineLength"})
public class TestModelEditing extends AbstractTest {
    @Test
    public void testStringParamEditing() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("str").type(Param.Type.STRING).name("String Parameter")
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
            .param("str").setString("value").modificationSource(ModificationSource.OPERATOR_FILLED)
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("str")
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
        ParamWidget<?> textWidget = block.getWidgets().get(0);
        ValueWidget<Object> valueWidget = (ValueWidget<Object>) textWidget.getValuesWidget().getFirstValueWidget();
        Assert.assertEquals(ValueWidget.BackgroundColor.NONE, valueWidget.getBackgroundColor());

        valueWidget.setValue(Arrays.asList("newvalue"), true);

        Assert.assertEquals(ValueWidget.BackgroundColor.NONE, valueWidget.getBackgroundColor());

        rpc.setSaveModel(1L, null);
        bus.fireEvent(
                new SaveModelRequest(false, false));

        CommonModel savedModel = rpc.getSavedModel();
        Assert.assertEquals(2, savedModel.getParameterValues().size());
        List<Word> strValue = savedModel.getSingleParameterValue("str").getStringValue();
        Assert.assertEquals(1, strValue.size());
        Assert.assertEquals("newvalue", strValue.get(0).getWord());
        Assert.assertEquals(1, savedModel.getSingleParameterValue(XslNames.VENDOR).getOptionId().longValue());
    }

    @Test
    public void testNumParamEditing() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("num").type(Param.Type.NUMERIC).name("Numeric Parameter").precision(0)
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
            .param("num").setNumeric(1).modificationSource(ModificationSource.OPERATOR_FILLED)
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("num")
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
        ValueWidget<Object> widget = (ValueWidget<Object>)
            block.getWidgets().get(0).getValuesWidget().getFirstValueWidget();

        Assert.assertEquals(BackgroundColor.NONE, widget.getBackgroundColor());

        widget.setValue("2", true);
        Assert.assertEquals(BackgroundColor.NONE, widget.getBackgroundColor());
        Assert.assertEquals("2", widget.getValue());

        // Проверка округления и замены "," на "."
        widget.setValue("2,2", true);
        Assert.assertEquals("2", widget.getValue());

        // Проверка ошибочного ввода - значение поля с ошибкой должно оставаться как есть
        widget.setValue("ananas", true);
        Assert.assertEquals("ananas", widget.getValue());
        Assert.assertEquals(ErrorWidget.Style.ERROR, widget.getErrorStyle());
        Assert.assertEquals("Ошибка! Значение должно быть числовым!", widget.getErrorMessage());

        rpc.setSaveModel(1L, null);
        bus.fireEvent(
                new SaveModelRequest(false, false));

        // Сохраниться должно правильное значение
        CommonModel savedModel = rpc.getSavedModel();
        Assert.assertEquals(2, savedModel.getParameterValues().size());
        ParameterValue value = savedModel.getSingleParameterValue("num");
        Assert.assertEquals(2, value.getNumericValue().intValue());
        Assert.assertEquals(1, savedModel.getSingleParameterValue(XslNames.VENDOR).getOptionId().longValue());
    }

    @Test
    public void testEnumParamEditing() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("enum").type(Param.Type.ENUM).name("Enum Parameter")
                .option(1, "Option1")
                .option(2, "Option2")
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
            .param("enum").setOption(1).modificationSource(ModificationSource.OPERATOR_FILLED)
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("enum")
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

        Option option2 = data.getModelData().getParam(1).getOptions().get(1);

        ParamsTab params = (ParamsTab) view.getTabs().iterator().next();
        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        ValueWidget<Object> widget = (ValueWidget<Object>)
            block.getWidgets().get(0).getValuesWidget().getFirstValueWidget();
        Assert.assertEquals(BackgroundColor.NONE, widget.getBackgroundColor());
        widget.setValue(option2, true);
        Assert.assertEquals(BackgroundColor.NONE, widget.getBackgroundColor());

        // Эмулируем случай, когда пользователь набрал несуществующий текст в качестве опции
        Option userInput = new OptionImpl(0, "абырвалг");
        ((ValueFieldStub<Object>) widget.getValueField()).setValueUserInput(userInput);
        Assert.assertEquals(0, ((Option) widget.getValue()).getId());
        Assert.assertEquals("абырвалг", ((Option) widget.getValue()).getName());
        Assert.assertEquals(ErrorWidget.Style.ERROR, widget.getErrorStyle());
        Assert.assertEquals("Такой опции нет", widget.getErrorMessage());
        Assert.assertEquals(BackgroundColor.NONE, widget.getBackgroundColor());

        // Эмулируем случай, когда пользователь ничего не выбрал
        widget.setValue(null, true);
        Assert.assertEquals(ErrorWidget.Style.INFO, widget.getErrorStyle());
        // Цвет пустого виджета меняется:
        Assert.assertEquals(BackgroundColor.EMPTY, widget.getBackgroundColor());
        // Сообщение об ошибке уходит
        Assert.assertEquals("", widget.getErrorMessage());

        // Снова возвращаем опцию 2
        widget.setValue(option2, true);
        Assert.assertEquals(BackgroundColor.NONE, widget.getBackgroundColor());

        rpc.setSaveModel(1L, null);
        bus.fireEvent(
                new SaveModelRequest(false, false));

        CommonModel savedModel = rpc.getSavedModel();
        Assert.assertEquals(2, savedModel.getParameterValues().size());
        Assert.assertEquals(2, savedModel.getSingleParameterValue("enum").getOptionId().longValue());
        Assert.assertEquals(1, savedModel.getSingleParameterValue(XslNames.VENDOR).getOptionId().longValue());
    }

    @Test
    public void testBoolParamEditing() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("bool").type(Param.Type.BOOLEAN).name("Bool")
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
            .param("bool").setOption(1).setBoolean(true).modificationSource(ModificationSource.OPERATOR_FILLED)
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("bool")
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
        ValueWidget<Object> widget = (ValueWidget<Object>)
            block.getWidgets().get(0).getValuesWidget().getFirstValueWidget();
        Assert.assertEquals(BackgroundColor.NONE, widget.getBackgroundColor());
        widget.setValue(false, true);
        Assert.assertEquals(BackgroundColor.NONE, widget.getBackgroundColor());

        rpc.setSaveModel(1L, null);
        bus.fireEvent(
                new SaveModelRequest(false, false));

        CommonModel savedModel = rpc.getSavedModel();
        Assert.assertEquals(2, savedModel.getParameterValues().size());
        Assert.assertEquals(2, savedModel.getSingleParameterValue("bool").getOptionId().longValue());
        Assert.assertEquals(false, savedModel.getSingleParameterValue("bool").getBooleanValue());
        Assert.assertEquals(1, savedModel.getSingleParameterValue(XslNames.VENDOR).getOptionId().longValue());
    }

    @Test
    public void testOperatorCommentEditing() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl(XslNames.OPERATOR_COMMENT).type(Param.Type.STRING).name("String Parameter")
            .endParameter()
            .startParameter()
                .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
                .hidden(true)
                .option(1, "Vendor1")
                .option(2, "Vendor2")
                .option(3, "Vendor3")
            .endParameter()
            .startParameter()
                .xsl(XslNames.OPERATOR_COMMENT).type(Param.Type.STRING).name("Комментарий оператора")
            .endParameter()
        .endParameters()
        .startModel()
            .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("str")
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


        ParamsTab additional = (ParamsTab) view.getTabs().iterator().next();
        BlockWidget additionalBlock = (BlockWidget) additional.getWidgetsAtLeft().get(0);
        ValueWidget<Object> commentWidget = (ValueWidget<Object>)
            additionalBlock.getWidgets().get(0).getValuesWidget().getFirstValueWidget();
        Assert.assertEquals(Arrays.asList(), commentWidget.getValue());

        // Поскольку комментарий оператора не заполнен, то вкладка "Дополнительно" - не темная
        Assert.assertFalse(view.isTabHeaderDark(EditorTabs.ADDITIONAL.getDisplayName()));

        Assert.assertEquals(BackgroundColor.EMPTY, commentWidget.getBackgroundColor());
        commentWidget.setValue(Collections.singletonList("Комментарий оператора"), true);
        Assert.assertEquals(BackgroundColor.NONE, commentWidget.getBackgroundColor());

        // После заполнения комментария оператора вкладка "Дополнительно" становится темной
        Assert.assertTrue(view.isTabHeaderDark(EditorTabs.ADDITIONAL.getDisplayName()));
    }

    @Test
    public void testModelSaving() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("str1").type(Param.Type.STRING).name("String Parameter 1")
            .endParameter()
            .startParameter()
                .xsl("str2").type(Param.Type.STRING).name("String Parameter 2")
            .endParameter()
            .startParameter()
                .xsl("str3").type(Param.Type.STRING).name("String Parameter 3")
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
            .param("str1").setString("value1")
            .param("str2").setString("value2")
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("str1", "str2", "str3")
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
        ValueWidget<Object> widget1 = (ValueWidget<Object>)
            block.getWidgets().get(0).getValuesWidget().getFirstValueWidget();
        ValueWidget<Object> widget3 = (ValueWidget<Object>)
            block.getWidgets().get(2).getValuesWidget().getFirstValueWidget();

        Assert.assertEquals(null, view.getLeaveConfirmationMessage());

        widget1.setValue(Arrays.asList("newvalue1"), true);
        widget3.setValue(Arrays.asList("newvalue3"), true);

        Assert.assertEquals(
            "Имеются несохраненные данные. Вы точно хотите продолжить?",
            view.getLeaveConfirmationMessage());

        rpc.setSaveModel(1L, null);

        Assert.assertTrue(view.isSaveButtonEnabled());

        bus.fireEvent(
                new SaveModelRequest(false, false));
        Assert.assertEquals(null, view.getLeaveConfirmationMessage());

        Assert.assertTrue(view.isSaveButtonEnabled());
        Assert.assertNull(view.getOperationWidget());

        CommonModel savedModel = rpc.getSavedModel();
        Assert.assertEquals(4, savedModel.getParameterValues().size()); //+1 параметр (новый)

        List<Word> strValue = savedModel.getSingleParameterValue("str1").getStringValue();
        Assert.assertEquals(1, strValue.size());
        Assert.assertEquals("newvalue1", strValue.get(0).getWord());

        strValue = savedModel.getSingleParameterValue("str2").getStringValue();
        Assert.assertEquals(1, strValue.size());
        Assert.assertEquals("value2", strValue.get(0).getWord());

        strValue = savedModel.getSingleParameterValue("str3").getStringValue();
        Assert.assertEquals(1, strValue.size());
        Assert.assertEquals("newvalue3", strValue.get(0).getWord());


        Assert.assertEquals(1, savedModel.getSingleParameterValue(XslNames.VENDOR).getOptionId().longValue());
    }

    @Test
    public void testModelDeletionNoSuccessors() {
        ModelDataBuilder data = prepareAndClickDelete();

        SelectModelSuccessorsWidgetStub modelSuccessorsWidgetStub =
            viewFactory.getCurrentSelectModelSuccessorsWidget();
        modelSuccessorsWidgetStub.setRemovalType(ModelRemovalType.ERROR);
        modelSuccessorsWidgetStub.clickContinue(Collections.emptyList());

        List<ModelTransition> savedTransitions = rpc.getModelTransitionsToSave();

        assertThat(savedTransitions).containsExactlyInAnyOrder(
            createModelTransition(1L, null, ModelTransition.ModelTransitionType.ERROR, false)
        );

        Assert.assertFalse(view.isDeleteButtonEnabled());
        Assert.assertEquals("Модель: TEST MODEL УДАЛЕНА!!!", view.getWindowTitle());
        Assert.assertTrue(data.getModel().isDeleted());
    }

    @Test
    public void testModelDeletionOneSuccessor() {
        ModelDataBuilder data = prepareAndClickDelete();

        SelectModelSuccessorsWidgetStub modelSuccessorsWidgetStub =
            viewFactory.getCurrentSelectModelSuccessorsWidget();
        modelSuccessorsWidgetStub.setRemovalType(ModelRemovalType.DUPLICATE);
        modelSuccessorsWidgetStub.clickContinue(Collections.singletonList(new IdAndName(2L, "qwerty")));

        List<ModelTransition> savedTransitions = rpc.getModelTransitionsToSave();

        assertThat(savedTransitions).containsExactlyInAnyOrder(
            createModelTransition(1L, 2L, ModelTransition.ModelTransitionType.DUPLICATE, true)
        );

        Assert.assertFalse(view.isDeleteButtonEnabled());
        Assert.assertEquals("Модель: TEST MODEL УДАЛЕНА!!!", view.getWindowTitle());
        Assert.assertTrue(data.getModel().isDeleted());
    }

    @Test
    public void testModelDeletionMultipleSuccessor() {
        ModelDataBuilder data = prepareAndClickDelete();

        SelectModelSuccessorsWidgetStub modelSuccessorsWidgetStub =
            viewFactory.getCurrentSelectModelSuccessorsWidget();
        modelSuccessorsWidgetStub.clickContinue(Arrays.asList(
            new IdAndName(2L, "qwerty"),
            new IdAndName(3L, "qwerty1")
        ));

        List<ModelTransition> savedTransitions = rpc.getModelTransitionsToSave();

        assertThat(savedTransitions).containsExactlyInAnyOrder(
            createModelTransition(1L, 2L, ModelTransition.ModelTransitionType.SPLIT, true),
            createModelTransition(1L, 3L, ModelTransition.ModelTransitionType.SPLIT, false)
        );

        Assert.assertFalse(view.isDeleteButtonEnabled());
        Assert.assertEquals("Модель: TEST MODEL УДАЛЕНА!!!", view.getWindowTitle());
        Assert.assertTrue(data.getModel().isDeleted());
    }

    @Test
    public void testModelDeletionCancelled() {
        ModelDataBuilder data = prepareAndClickDelete();

        SelectModelSuccessorsWidgetStub modelSuccessorsWidgetStub =
            viewFactory.getCurrentSelectModelSuccessorsWidget();
        modelSuccessorsWidgetStub.clickCancel();

        List<ModelTransition> savedTransitions = rpc.getModelTransitionsToSave();

        assertThat(savedTransitions).isNull();
        Assert.assertTrue(view.isDeleteButtonEnabled());
        Assert.assertEquals("Модель: TEST MODEL", view.getWindowTitle());
        Assert.assertFalse(data.getModel().isDeleted());
    }

    @Test
    public void testPreservUserInput() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("str1").type(Param.Type.STRING).name("String Parameter 1")
            .endParameter()
            .startParameter()
                .xsl("str2").type(Param.Type.STRING).name("String Parameter 2")
            .endParameter()
            .startParameter()
                .xsl("str3").type(Param.Type.STRING).name("String Parameter 3")
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
            .param("str1").setString("value1")
            .param("str2").setString("value2")
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("str1", "str2", "str3")
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
        ValueWidget<List<String>> widget1 = (ValueWidget<List<String>>)
            block.getWidgets().get(0).getValuesWidget().getFirstValueWidget();
        ValueWidget<List<String>> widget2 = (ValueWidget<List<String>>)
            block.getWidgets().get(1).getValuesWidget().getFirstValueWidget();
        ValueWidget<List<String>> widget3 = (ValueWidget<List<String>>)
            block.getWidgets().get(2).getValuesWidget().getFirstValueWidget();

        widget1.setValue(Arrays.asList("newvalue1"), true);
        widget3.setValue(Arrays.asList("newvalue3"), true);

        Assert.assertEquals("newvalue1", (widget1.getValue()).get(0));
        Assert.assertEquals("value2", (widget2.getValue()).get(0));
        Assert.assertEquals("newvalue3", (widget3.getValue()).get(0));


        CommonModel newModel = CommonModelBuilder.builder(m -> m)
                .parameters(new ArrayList<>(data.getModelData().getParams()))
                .startModel()
                    .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
                    .param("str1").setString("server_changed_value1").modificationSource(ModificationSource.BACKEND_RULE)
                    .param("str2").setString("server_changed_value2").modificationSource(ModificationSource.BACKEND_RULE)
                    .param(XslNames.VENDOR).setOption(1)
                .endModel();

        bus.fireEvent(new RemoteModelChangedEvent(newModel, Cause.COPIED));

        Assert.assertEquals("newvalue1", (widget1.getValue()).get(0));
        Assert.assertEquals("server_changed_value2", (widget2.getValue()).get(0));
        Assert.assertEquals("newvalue3", (widget3.getValue()).get(0));

        // Теперь пробуем сохранить модели. Ключевой момент здесь - проверить modification source
        // у параметров, так как был баг их невыставления у тех параметров, которые редактировались до
        // изменения модели на сервере
        rpc.setSaveModel(1L, null);
        bus.fireEvent(
                new SaveModelRequest(false, false));

        CommonModel savedModel = rpc.getSavedModel();
        Assert.assertEquals(4, savedModel.getParameterValues().size()); //+1 параметр (новый)

        List<Word> strValue = savedModel.getSingleParameterValue("str1").getStringValue();
        Assert.assertEquals(1, strValue.size());
        Assert.assertEquals("newvalue1", strValue.get(0).getWord());
        Assert.assertEquals(ModificationSource.OPERATOR_FILLED,
            savedModel.getSingleParameterValue("str1").getModificationSource());

        strValue = savedModel.getSingleParameterValue("str2").getStringValue();
        Assert.assertEquals(1, strValue.size());
        Assert.assertEquals("server_changed_value2", strValue.get(0).getWord());
        Assert.assertEquals(ModificationSource.BACKEND_RULE,
            savedModel.getSingleParameterValue("str2").getModificationSource());

        strValue = savedModel.getSingleParameterValue("str3").getStringValue();
        Assert.assertEquals(1, strValue.size());
        Assert.assertEquals("newvalue3", strValue.get(0).getWord());
        Assert.assertEquals(ModificationSource.OPERATOR_FILLED,
            savedModel.getSingleParameterValue("str3").getModificationSource());

        // Пусть после сохранения удаленная модель еще раз изменилась
        newModel = CommonModelBuilder.builder(m -> m)
                .parameters(new ArrayList<>(data.getModelData().getParams()))
                .startModel()
                    .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
                    .param("str1").setString("server_changed_value1").modificationSource(ModificationSource.BACKEND_RULE)
                    .param("str2").setString("server_changed_value2").modificationSource(ModificationSource.BACKEND_RULE)
                    .param(XslNames.VENDOR).setOption(1)
                .endModel();

        bus.fireEvent(new RemoteModelChangedEvent(newModel, Cause.COPIED));

        Assert.assertEquals("server_changed_value1", ((List<String>) widget1.getValue()).get(0));

        // Теперь, после сохранения, нет никакой необходимости далее сохранять введенное пользователем значение!
        // Поэтому оно изменится на серверное - server_changed_value2
        Assert.assertEquals("server_changed_value2", ((List<String>) widget2.getValue()).get(0));
        // Этот параметр станет пустым, поскольку на сервере он пустой
        Assert.assertTrue(((List<String>) widget3.getValue()).isEmpty());
    }

    @Test
    public void testOnPopulateModelSaveSyncEventFiredOnModelSave() {
        AtomicBoolean populateEventFired = new AtomicBoolean();
        bus.subscribe(PopulateModelSaveSyncEvent.class, event -> {
            populateEventFired.set(true);
        });

        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("str").type(Param.Type.STRING).name("String Parameter")
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
            .param("str").setString("value").modificationSource(ModificationSource.OPERATOR_FILLED)
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("str")
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

        rpc.setSaveModel(1L, null);
        bus.fireEvent(
                new SaveModelRequest(false, false));

        Assert.assertTrue("PopulateModelSaveSyncEvent wasn't fire!", populateEventFired.get());
    }

    /**
     * @see EditableValues#testCorrectSetParameterValueIfItNotContainsInAllowedOptions
     */
    @Test
    public void testCorruptedParameterValuesWontAffectCommonModelBeforeSave() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("num").type(Param.Type.NUMERIC).name("Number param")
                .maxValue(1)
            .endParameter()
            .startParameter()
                .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель").hidden(true)
                .option(1, "Vendor1")
            .endParameter()
        .endParameters()
        .startModel()
            .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
            .param("num").setNumeric(2)
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("num")
                .endBlock()
            .endTab()
        .endForm()
        .startVendor()
            .source("http://source1", "ru", new Date())
        .endVendor()
        .tovarCategory(1, 2);

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);

        modelData = data.getModelData();
        bus.subscribe(ModelUIGeneratedEvent.class, event -> {
            editableModel = event.getEditableModel();
        });

        bus.fireEvent(new PlaceShowEvent(EditorUrlStub.of("modelEditor", "entity-id=1")));
        bus.fireEvent(new ModelDataLoadedEvent(modelData));

        // проверяем, что после открытия значение в модели осталось прежним
        CommonModel model = editableModel.getModel();
        ParameterValues parameterValues = model.getParameterValues("num");
        Assert.assertEquals(1, parameterValues.size());
        Assert.assertEquals(2, parameterValues.getValue(0).getNumericValue().intValue());

        // сохраняем модель
        // параметр должен остаться таким же как есть
        rpc.setSaveModel(2L, null);
        bus.fireEvent(new SaveModelRequest());

        CommonModel savedModel = rpc.getSavedModel();
        ParameterValues savedParameterValues = savedModel.getParameterValues("num");
        Assert.assertEquals(1, savedParameterValues.size());
        Assert.assertEquals(2, savedParameterValues.getValue(0).getNumericValue().intValue());
    }

    @Test
    public void testNotEditingModelOnCurrentTypeGenerated() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("num").type(Param.Type.NUMERIC).name("Number param")
                .maxValue(1)
            .endParameter()
            .startParameter()
                .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель").hidden(true)
                .option(1, "Vendor1")
            .endParameter()
        .endParameters()
        .startModel()
            .id(1).category(2).source(Source.GURU).currentType(Source.GENERATED)
            .param("num").setNumeric(2)
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("num")
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

        modelData = data.getModelData();
        bus.subscribe(ModelUIGeneratedEvent.class, event -> {
            editableModel = event.getEditableModel();
        });

        bus.fireEvent(new PlaceShowEvent(EditorUrlStub.of("modelEditor", "entity-id=1")));
        bus.fireEvent(new ModelDataLoadedEvent(modelData));

        Assert.assertTrue(view.isNoEditableModelMessageVisible());
    }

    @Test
    public void testNotEditingModelOnCurrentTypeVendor() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("num").type(Param.Type.NUMERIC).name("Number param")
                .maxValue(1)
            .endParameter()
            .startParameter()
                .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель").hidden(true)
                .option(1, "Vendor1")
            .endParameter()
        .endParameters()
        .startModel()
            .id(1).category(2).source(Source.GURU).currentType(Source.VENDOR)
            .param("num").setNumeric(2)
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("num")
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

        modelData = data.getModelData();
        bus.subscribe(ModelUIGeneratedEvent.class, event -> {
            editableModel = event.getEditableModel();
        });

        bus.fireEvent(new PlaceShowEvent(EditorUrlStub.of("modelEditor", "entity-id=1")));
        bus.fireEvent(new ModelDataLoadedEvent(modelData));

        Assert.assertTrue(view.isNoEditableModelMessageVisible());
    }

    /**
     * @see EditableValues#testCorrectSetParameterValueIfItNotContainsInAllowedOptions
     */
    @Test
    public void testNotShownParameterValueWontChangeDuringModelEditorPipeline() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("num").type(Param.Type.NUMERIC).name("Number param")
                .hidden(true)
                .maxValue(1)
            .endParameter()
            .startParameter()
                .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель").hidden(true)
                .option(1, "Vendor1")
            .endParameter()
        .endParameters()
        .startModel()
            .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
            .param("num").setNumeric(2)
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties()
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

        modelData = data.getModelData();
        bus.subscribe(ModelUIGeneratedEvent.class, event -> {
            editableModel = event.getEditableModel();
        });

        bus.fireEvent(new PlaceShowEvent(EditorUrlStub.of("modelEditor", "entity-id=1")));
        bus.fireEvent(new ModelDataLoadedEvent(modelData));

        // проверяем, что после открытия значение в модели осталось прежним
        CommonModel model = editableModel.getModel();
        ParameterValues parameterValues = model.getParameterValues("num");
        Assert.assertEquals(1, parameterValues.size());
        Assert.assertEquals(2, parameterValues.getValue(0).getNumericValue().intValue());

        // сохраняем модель
        // параметр должен остаться таким же как есть
        rpc.setSaveModel(2L, null);
        bus.fireEvent(new SaveModelRequest());

        CommonModel savedModel = rpc.getSavedModel();
        ParameterValues savedParameterValues = savedModel.getParameterValues("num");
        Assert.assertEquals(1, savedParameterValues.size());
        Assert.assertEquals(2, savedParameterValues.getValue(0).getNumericValue().intValue());
    }

    private ModelTransition createModelTransition(long from,
                                                  Long to,
                                                  ModelTransition.ModelTransitionType type,
                                                  boolean primary) {
        ModelTransition.ModelTransitionReason reason;
        switch (type) {
            case ERROR:
                reason = ModelTransition.ModelTransitionReason.ERROR_REMOVAL;
                break;
            case DUPLICATE:
                reason = ModelTransition.ModelTransitionReason.DUPLICATE_REMOVAL;
                break;
            case SPLIT:
                reason = ModelTransition.ModelTransitionReason.MODEL_SPLIT;
                break;
            default: throw new IllegalArgumentException();
        }
        return new ModelTransition()
            .setType(type)
            .setReason(reason)
            .setEntityType(ModelTransition.EntityType.MODEL)
            .setOldEntityId(from)
            .setOldEntityDeleted(true)
            .setNewEntityId(to)
            .setPrimaryTransition(primary);
    }

    private ModelDataBuilder prepareAndClickDelete() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
            .startParameters()
            .startParameter()
            .xsl(XslNames.NAME).type(Param.Type.STRING).name("ИМЯ")
            .endParameter()
            .startParameter()
            .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
            .hidden(true)
            .option(1, "Vendor1")
            .endParameter()
            .endParameters()
            .startModel()
            .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
            .param(XslNames.VENDOR).setOption(1)
            .param(XslNames.NAME).setString("TEST MODEL")
            .endModel()
            .startForm()
            .startTab()
            .name(EditorTabs.PARAMETERS.getDisplayName())
            .endTab()
            .endForm()
            .startVendor()
            .source("http://source1", "ru", new Date())
            .endVendor()
            .tovarCategory(1, 2);

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);

        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", "entity-id=1")));

        rpc.setSaveModel(1L, null);

        Assert.assertTrue(view.isDeleteButtonEnabled());
        Assert.assertFalse(data.getModel().isDeleted());

        // fired with confirmation
        bus.fireEvent(
            new DeleteModelRequestEvent());

        Assert.assertTrue(view.isDeleteButtonEnabled());
        Assert.assertEquals("Модель: TEST MODEL", view.getWindowTitle());
        Assert.assertFalse(data.getModel().isDeleted());

        return data;
    }
}
