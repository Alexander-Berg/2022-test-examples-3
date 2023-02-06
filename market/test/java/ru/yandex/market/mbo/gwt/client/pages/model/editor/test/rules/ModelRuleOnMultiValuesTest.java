package ru.yandex.market.mbo.gwt.client.pages.model.editor.test.rules;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.ModelRulesAddon;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelDataBuilder;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.RulesTriggeredEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.AbstractTest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.BlockWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamsTab;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValuesWidget;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Тесты проверяют корректность работы {@link ModelRulesAddon} на мультизначениях.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ModelRuleOnMultiValuesTest extends AbstractTest {

    @Test
    public void testRuleWillReturnPreviousValueIfUserRemoveLine() {
        // @formatter:off
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("num1").type(Param.Type.NUMERIC).name("Numeric Parameter 1")
            .endParameter()
            .startParameter()
                .xsl("num2").type(Param.Type.NUMERIC).name("Numeric Parameter 2")
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
            .id(1).category(2).source(CommonModel.Source.GURU).currentType(CommonModel.Source.GURU)
            .param("num1").setNumeric(1)
            .param("num1").setNumeric(2)
            .param("num2").setNumeric(-1)
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Rule 1").group("Test")
                ._if()
                    .param("num1").isNotEmpty()
                .then()
                    .param("num2").matchesNumeric(3)
            .endRule()
        .endRuleSet()
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
        // @formatter:on

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);

        int[] rulesTriggeredCount = new int[1];
        bus.subscribe(RulesTriggeredEvent.class, e -> {
            rulesTriggeredCount[0]++;
        });

        bus.fireEvent(new PlaceShowEvent(EditorUrlStub.of("modelEditor", "entity-id=1")));

        Assert.assertEquals(1, rulesTriggeredCount[0]);

        ParamsTab params = (ParamsTab) view.getTabs().iterator().next();
        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        ValuesWidget<Object> widget1 = block.getWidget(0).getValuesWidget();
        ValuesWidget<Object> widget2 = block.getWidget(1).getValuesWidget();

        // при инициализации тут же должно сработать правило
        Assert.assertEquals("3.00", widget2.getFirstValueWidget().getValue());
        Assert.assertEquals(2, widget1.getValues().size());

        // удаляем второй элемент из списка
        widget1.removeValueWidget(1);
        Assert.assertEquals(1, widget1.getValues().size());

        // правила должны пересчитаться
        Assert.assertEquals(2, rulesTriggeredCount[0]);
        // но значениe так и не должно поменяться
        Assert.assertEquals("3.00", widget2.getFirstValueWidget().getValue());

        // удаляем последний элемент
        widget1.removeValueWidget(0);
        Assert.assertEquals(3, rulesTriggeredCount[0]);
        Assert.assertEquals(0, widget1.getValues().size());

        // значение должно вернуться в первоначальное положение
        Assert.assertEquals("-1", widget2.getFirstValueWidget().getValue());
    }

    @Test
    public void testRuleWillReturnPreviousValueIfUserRemoveLine2() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("enum1").type(Param.Type.ENUM).name("Enum Parameter 1")
                .option(1, "Option1")
                .option(2, "Option2")
                .option(3, "Option3")
            .endParameter()
            .startParameter()
                .xsl("str1").type(Param.Type.STRING).name("String Parameter 1")
            .endParameter()
            .startParameter()
                .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
                .hidden(true)
                .option(1, "Vendor1")
            .endParameter()
        .endParameters()
        .startModel()
            .id(1).category(2).source(CommonModel.Source.GURU).currentType(CommonModel.Source.GURU)
            .param(XslNames.VENDOR).setOption(1)
            .param("str1").setString("initial value")
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Rule 1").group("Test")
                ._if()
                    .param("enum1").isNotEmpty()
                .then()
                    .param("str1").matchesString("rule applied")
            .endRule()
        .endRuleSet()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("enum1", "str1")
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
        bus.subscribe(RulesTriggeredEvent.class, e -> {
            rulesTriggeredCount[0]++;
        });

        bus.fireEvent(new PlaceShowEvent(EditorUrlStub.of("modelEditor", "entity-id=1")));

        Assert.assertEquals(1, rulesTriggeredCount[0]);

        ParamsTab params = (ParamsTab) view.getTabs().iterator().next();
        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        ValuesWidget<Option> widget1 = block.getWidget(0, Option.class).getValuesWidget();
        ValuesWidget<List<String>> widget2 = block.getWidgetList(1, String.class).getValuesWidget();

        // при инициализации правило не срабатывает
        Assert.assertEquals(null, widget1.getFirstValueWidget().getValue());
        Assert.assertEquals(Collections.singletonList("initial value"), widget2.getFirstValueWidget().getValue());

        // добавляем непустое значение
        widget1.getValueWidget(0).setValue(new OptionImpl(1, "Option1"), true);

        // правило должно примениться
        Assert.assertEquals(2, rulesTriggeredCount[0]);
        Assert.assertEquals(Collections.singletonList("rule applied"), widget2.getFirstValueWidget().getValue());

        // удаляем значение
        widget1.removeValueWidget(0);

        // значение должно вернуться в первоначальное состояние
        Assert.assertEquals(3, rulesTriggeredCount[0]);
        Assert.assertEquals(Collections.singletonList("initial value"), widget2.getFirstValueWidget().getValue());
    }

    @Test
    public void testRuleWillReturnPreviousValueIfUserRemoveInput() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("enum1").type(Param.Type.ENUM).name("Enum Parameter 1")
                .option(1, "Option1")
                .option(2, "Option2")
                .option(3, "Option3")
            .endParameter()
            .startParameter()
                .xsl("str1").type(Param.Type.STRING).name("String Parameter 1")
            .endParameter()
            .startParameter()
                .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
                .hidden(true)
                .option(1, "Vendor1")
            .endParameter()
        .endParameters()
        .startModel()
            .id(1).category(2).source(CommonModel.Source.GURU).currentType(CommonModel.Source.GURU)
            .param(XslNames.VENDOR).setOption(1)
            .param("enum1").setOption(1)
            .param("str1").setString("initial value")
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Rule 1").group("Test")
                ._if()
                    .param("enum1").isNotEmpty()
                .then()
                    .param("str1").matchesString("rule applied")
            .endRule()
        .endRuleSet()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("enum1", "str1")
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
        bus.subscribe(RulesTriggeredEvent.class, e -> {
            rulesTriggeredCount[0]++;
        });

        bus.fireEvent(new PlaceShowEvent(EditorUrlStub.of("modelEditor", "entity-id=1")));

        Assert.assertEquals(1, rulesTriggeredCount[0]);

        ParamsTab params = (ParamsTab) view.getTabs().iterator().next();
        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        ValuesWidget<Option> widget1 = block.getWidget(0, Option.class).getValuesWidget();
        ValuesWidget<List<String>> widget2 = block.getWidgetList(1, String.class).getValuesWidget();

        // при инициализации правило срабатывает
        Assert.assertEquals(1, widget1.getFirstValueWidget().getValue().getId());
        Assert.assertEquals(Collections.singletonList("rule applied"), widget2.getFirstValueWidget().getValue());

        // добавляем непустое значение
        widget1.getValueWidget(0).setValue(null, true);

        // значение должно вернуться в первоначальное состояние
        Assert.assertEquals(2, rulesTriggeredCount[0]);
        Assert.assertEquals(Collections.singletonList("initial value"), widget2.getFirstValueWidget().getValue());
    }
}
