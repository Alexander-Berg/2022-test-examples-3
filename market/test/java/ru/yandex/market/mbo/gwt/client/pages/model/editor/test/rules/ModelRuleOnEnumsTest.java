package ru.yandex.market.mbo.gwt.client.pages.model.editor.test.rules;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelDataBuilder;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.RulesTriggeredEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.AbstractTest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.BlockWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamsTab;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ErrorWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueWidget;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel.Source;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author gilmulla
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ModelRuleOnEnumsTest extends AbstractTest {

    @Test
    @SuppressWarnings("checkstyle:methodlength")
    public void testCorrectInitialStateAndWrongValueEditing() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("str").type(Param.Type.STRING).name("String Parameter1")
            .endParameter()
            .startParameter()
                .xsl("enum").type(Param.Type.ENUM).name("Enum Parameter")
                .option(1, "Option1")
                .option(2, "Option2")
                .option(3, "Option3")
                .option(4, "Option4")
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
            .param("str").setString("ccc")
            .param("enum").setOption(1)
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Rule 1").group("Test")
                ._if()
                    .param("str").matchesString("aaa")
                .then()
                    .param("enum").matchesEnum(2L, 3L)
            .endRule()
        .endRuleSet()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("str", "enum")
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

        int[] rulesTriggeredCount = new int[1];
        bus.subscribe(RulesTriggeredEvent.class, e -> rulesTriggeredCount[0]++);

        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelEditor", "entity-id=1")));

        Assert.assertEquals(1, rulesTriggeredCount[0]);

        ParamsTab params = (ParamsTab) view.getTabs().iterator().next();
        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        ValueWidget<Object> widget1 = (ValueWidget<Object>) block.getWidgets().get(0).getFirstValueWidget();
        ValueWidget<Option> widget2 = (ValueWidget<Option>) block.getWidgets().get(1).getFirstValueWidget();

        // Test initial state
        Assert.assertTrue(view.isSaveButtonEnabled());

        Assert.assertEquals(Arrays.asList("ccc"), widget1.getValue());
        Assert.assertEquals("", block.getWidgets().get(0).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget1.isEnabled());
        Assert.assertFalse(block.getWidgets().get(0).isRuleLinkVisible());
        Assert.assertEquals("", widget1.getErrorMessage());
        Assert.assertEquals(ErrorWidget.Style.INFO, widget1.getErrorStyle());


        Assert.assertEquals(1, widget2.getValue().getId());
        List<Option> domain = widget2.getValueDomain();
        Assert.assertEquals(4, domain.size());
        Assert.assertEquals(1L, domain.get(0).getId());
        Assert.assertEquals(2L, domain.get(1).getId());
        Assert.assertEquals(3L, domain.get(2).getId());
        Assert.assertEquals(4L, domain.get(3).getId());
        Assert.assertEquals("", block.getWidgets().get(1).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget2.isEnabled());
        Assert.assertFalse(block.getWidgets().get(1).isRuleLinkVisible());
        Assert.assertEquals("", widget2.getErrorMessage());
        Assert.assertEquals(ErrorWidget.Style.INFO, widget2.getErrorStyle());

        // Edit string parameter for rule triggering
        widget1.setValue(Arrays.asList("aaa"), true);

        Assert.assertEquals(2, rulesTriggeredCount[0]);

        // Ошибка валидации, сохранение запрещено
        Assert.assertFalse(view.isSaveButtonEnabled());

        Assert.assertEquals(Arrays.asList("aaa"), widget1.getValue());
        Assert.assertEquals("", block.getWidgets().get(0).getValuesWidget().getOldValueMessage());
        Assert.assertEquals(true, widget1.isEnabled());
        Assert.assertEquals(false, block.getWidgets().get(0).isRuleLinkVisible());
        Assert.assertEquals("", widget1.getErrorMessage());
        Assert.assertEquals(ErrorWidget.Style.INFO, widget1.getErrorStyle());


        Assert.assertNull(widget2.getValue());
        domain = widget2.getValueDomain();
        Assert.assertEquals(2, domain.size());
        Assert.assertEquals(2L, domain.get(0).getId());
        Assert.assertEquals(3L, domain.get(1).getId());
        Assert.assertEquals("", block.getWidgets().get(1).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget2.isEnabled());
        Assert.assertTrue(block.getWidgets().get(1).isRuleLinkVisible());
        Assert.assertEquals("Значение должно быть из списка: Option2; Option3", widget2.getErrorMessage());
        Assert.assertEquals(ErrorWidget.Style.ERROR, widget2.getErrorStyle());

        // Исправляем ошибку валидации.
        // Устанавливаем корректное с точки зрения правил значение Option3
        Option option3 = data.getModelData().getParam(2L).getOptions().get(2);
        widget2.setValue(option3, true);

        Assert.assertEquals(3, rulesTriggeredCount[0]); // правила запустились

        // Ошибка должна уйти, сохранение разрешено
        Assert.assertTrue(view.isSaveButtonEnabled());

        // Проверяем состояние виджета с энумом после корректировки
        Assert.assertEquals(3, widget2.getValue().getId());
        domain = widget2.getValueDomain();
        Assert.assertEquals(2, domain.size());
        Assert.assertEquals(2L, domain.get(0).getId());
        Assert.assertEquals(3L, domain.get(1).getId());
        Assert.assertEquals("", block.getWidgets().get(1).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget2.isEnabled());
        // Поскольку значение стало валидным, ссылка на правило исчезает
        Assert.assertFalse(block.getWidgets().get(1).isRuleLinkVisible());
        // Поскольку значение стало валидным, сообщение исчезает
        Assert.assertEquals("", widget2.getErrorMessage());
        // Поскольку значение стало валидным, виджет не подсвечен как ошибочный
        Assert.assertEquals(ErrorWidget.Style.INFO, widget2.getErrorStyle());
    }

    @Test
    public void testWrongInitialStateAndEditingRuleCause() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("str").type(Param.Type.STRING).name("String Parameter1")
            .endParameter()
            .startParameter()
                .xsl("enum").type(Param.Type.ENUM).name("Enum Parameter")
                .option(1, "Option1")
                .option(2, "Option2")
                .option(3, "Option3")
                .option(4, "Option4")
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
            .param("str").setString("aaa")
            .param("enum").setOption(1)
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Rule 1").group("Test")
                ._if()
                    .param("str").matchesString("aaa")
                .then()
                    .param("enum").matchesEnum(2L, 3L)
            .endRule()
        .endRuleSet()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("str", "enum")
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

        int[] rulesTriggeredCount = new int[1];
        bus.subscribe(RulesTriggeredEvent.class, e -> rulesTriggeredCount[0]++);

        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelEditor", "entity-id=2")));

        Assert.assertEquals(1, rulesTriggeredCount[0]);

        ParamsTab params = (ParamsTab) view.getTabs().iterator().next();
        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        ValueWidget<Object> widget1 = (ValueWidget<Object>) block.getWidgets().get(0).getFirstValueWidget();
        ValueWidget<Option> widget2 = (ValueWidget<Option>) block.getWidgets().get(1).getFirstValueWidget();

        // Test initial state
        // Ошибка в начальном состоянии, сохранение запрещено
        Assert.assertFalse(view.isSaveButtonEnabled());

        Assert.assertEquals(Arrays.asList("aaa"), widget1.getValue());
        Assert.assertEquals("", block.getWidgets().get(0).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget1.isEnabled());
        Assert.assertFalse(block.getWidgets().get(0).isRuleLinkVisible());
        Assert.assertEquals("", widget1.getErrorMessage());
        Assert.assertEquals(ErrorWidget.Style.INFO, widget1.getErrorStyle());

        Assert.assertNull(widget2.getValue());
        List<Option> domain = widget2.getValueDomain();
        Assert.assertEquals(2, domain.size());
        Assert.assertEquals(2L, domain.get(0).getId());
        Assert.assertEquals(3L, domain.get(1).getId());
        Assert.assertEquals("", block.getWidgets().get(1).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget2.isEnabled());
        Assert.assertTrue(block.getWidgets().get(1).isRuleLinkVisible());
        Assert.assertEquals("Значение должно быть из списка: Option2; Option3", widget2.getErrorMessage());
        Assert.assertEquals(ErrorWidget.Style.ERROR, widget2.getErrorStyle());

        // Меняем строковый параметр, чтобы правило не срабатывало
        widget1.setValue(Arrays.asList("ccc"), true);

        Assert.assertEquals(2, rulesTriggeredCount[0]);

        // Проверяем, что ошибка ушла
        // Сохранение должно стать доступно
        Assert.assertTrue(view.isSaveButtonEnabled());

        Assert.assertEquals(Arrays.asList("ccc"), widget1.getValue());
        Assert.assertEquals("", block.getWidgets().get(0).getValuesWidget().getOldValueMessage());
        Assert.assertEquals(true, widget1.isEnabled());
        Assert.assertEquals(false, block.getWidgets().get(0).isRuleLinkVisible());

        Assert.assertEquals(1, widget2.getValue().getId());
        domain = widget2.getValueDomain();
        Assert.assertEquals(4, domain.size());
        Assert.assertEquals(1L, domain.get(0).getId());
        Assert.assertEquals(2L, domain.get(1).getId());
        Assert.assertEquals(3L, domain.get(2).getId());
        Assert.assertEquals(4L, domain.get(3).getId());
        Assert.assertEquals("", block.getWidgets().get(1).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget2.isEnabled());
        Assert.assertFalse(block.getWidgets().get(1).isRuleLinkVisible());
        Assert.assertEquals("", widget2.getErrorMessage());
        Assert.assertEquals(ErrorWidget.Style.INFO, widget2.getErrorStyle());
    }

    @Test
    public void testWrongInitialStateAndEditingWrongParameterToCorrect() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("str").type(Param.Type.STRING).name("String Parameter1")
            .endParameter()
            .startParameter()
                .xsl("enum").type(Param.Type.ENUM).name("Enum Parameter")
                .option(1, "Option1")
                .option(2, "Option2")
                .option(3, "Option3")
                .option(4, "Option4")
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
            .id(3).category(2).source(Source.GURU).currentType(Source.GURU)
            .param("str").setString("aaa")
            .param("enum").setOption(1)
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Rule 1").group("Test")
                ._if()
                    .param("str").matchesString("aaa")
                .then()
                    .param("enum").matchesEnum(2L, 3L)
            .endRule()
        .endRuleSet()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("str", "enum")
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

        int[] rulesTriggeredCount = new int[1];
        bus.subscribe(RulesTriggeredEvent.class, e -> rulesTriggeredCount[0]++);

        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelEditor", "entity-id=3")));

        Assert.assertEquals(1, rulesTriggeredCount[0]);

        ParamsTab params = (ParamsTab) view.getTabs().iterator().next();
        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        ValueWidget<?> widget1 = block.getWidgets().get(0).getFirstValueWidget();
        @SuppressWarnings("unchecked")
        ValueWidget<Option> widget2 = (ValueWidget<Option>) block.getWidgets().get(1).getFirstValueWidget();

        // Test initial state
        // Начальное состояние не корректно, сохранение запрещено
        Assert.assertFalse(view.isSaveButtonEnabled());

        Assert.assertEquals(Arrays.asList("aaa"), widget1.getValue());
        Assert.assertEquals("", block.getWidgets().get(0).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget1.isEnabled());
        Assert.assertFalse(block.getWidgets().get(0).isRuleLinkVisible());
        Assert.assertEquals("", widget1.getErrorMessage());
        Assert.assertEquals(ErrorWidget.Style.INFO, widget1.getErrorStyle());

        Assert.assertNull(widget2.getValue());
        List<Option> domain = widget2.getValueDomain();
        Assert.assertEquals(2, domain.size());
        Assert.assertEquals(2L, domain.get(0).getId());
        Assert.assertEquals(3L, domain.get(1).getId());
        Assert.assertEquals("", block.getWidgets().get(1).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget2.isEnabled());
        Assert.assertTrue(block.getWidgets().get(1).isRuleLinkVisible());
        Assert.assertEquals("Значение должно быть из списка: Option2; Option3", widget2.getErrorMessage());
        Assert.assertEquals(ErrorWidget.Style.ERROR, widget2.getErrorStyle());

        // Меняем ошибочный параметр, чтобы убрать ошибку
        Option option2 = data.getModelData().getParam(2L).getOptions().get(1);
        widget2.setValue(option2, true);

        Assert.assertEquals(2, rulesTriggeredCount[0]);

        // Проверяем, что ошибка ушла
        // Сохранение должно стать доступно
        Assert.assertTrue(view.isSaveButtonEnabled());

        Assert.assertEquals(Arrays.asList("aaa"), widget1.getValue());
        Assert.assertEquals("", block.getWidgets().get(0).getValuesWidget().getOldValueMessage());
        Assert.assertEquals(true, widget1.isEnabled());
        Assert.assertEquals(false, block.getWidgets().get(0).isRuleLinkVisible());

        Assert.assertEquals(2, widget2.getValue().getId());
        domain = widget2.getValueDomain();
        Assert.assertEquals(2, domain.size());
        Assert.assertEquals(2L, domain.get(0).getId());
        Assert.assertEquals(3L, domain.get(1).getId());
        Assert.assertEquals("", block.getWidgets().get(1).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget2.isEnabled());
        // Поскольку значение стало валидным, ссылка на правило исчезает
        Assert.assertFalse(block.getWidgets().get(1).isRuleLinkVisible());
        // Поскольку значение стало валидным, сообщение исчезает
        Assert.assertEquals("", widget2.getErrorMessage());
        // Поскольку значение стало валидным, виджет не подсвечен как ошибочный
        Assert.assertEquals(ErrorWidget.Style.INFO, widget2.getErrorStyle());
    }

    @Test
    public void testOpenSeveralModelsInOneEditor() {
        // Тест проверяет последовательные сеансы работы
        // с разными моделями в ОДНОМ редакторе

        // Открыть и провести сеанс работы с моделью 1
        testCorrectInitialStateAndWrongValueEditing();
        // Открыть и провести сеанс работы с моделью 2
        testWrongInitialStateAndEditingRuleCause();
        // Открыть и провести сеанс работы с моделью 3
        testWrongInitialStateAndEditingWrongParameterToCorrect();
    }
}
