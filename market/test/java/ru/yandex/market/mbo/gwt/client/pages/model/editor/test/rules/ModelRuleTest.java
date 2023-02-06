package ru.yandex.market.mbo.gwt.client.pages.model.editor.test.rules;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelDataBuilder;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.RemoteModelChangedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.RemoteModelChangedEvent.Cause;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.RulesTriggeredEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.SaveModelRequest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.ShowRuleInfoPopupEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.AbstractTest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.BlockWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.EditorWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamsTab;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.RuleInfoWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.RuleInfoWidget.Block;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValuesWidget;
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
public class ModelRuleTest extends AbstractTest {

    @Test
    public void testRulesNoEffect() {
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
            .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
            .param("num1").setNumeric(1)
            .param("num2").setNumeric(2)
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Rule 1").group("Test")
                ._if()
                    .param("num1").matchesNumeric(4)
                .then()
                    .param("num2").matchesNumeric(5)
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

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);

        int[] rulesTriggeredCount = new int[1];
        bus.subscribe(RulesTriggeredEvent.class, e -> {
            rulesTriggeredCount[0]++;
        });

        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelEditor", "entity-id=1")));

        Assert.assertEquals(1, rulesTriggeredCount[0]);

        ParamsTab params = (ParamsTab) view.getTabs().iterator().next();
        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        ValuesWidget<Object> widget1 = (ValuesWidget<Object>)
            block.getWidgets().get(0).getValuesWidget();
        ParamWidget<?> widget2 = block.getWidgets().get(1);
        widget1.getFirstValueWidget().setValue("2", true);

        Assert.assertEquals(2, rulesTriggeredCount[0]);

        Assert.assertEquals("2", widget1.getFirstValueWidget().getValue());

        Assert.assertEquals("2", widget2.getValuesWidget().getFirstValueWidget().getValue());
    }

    @Test
    public void testNumeRulesTriggered() {
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
            .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
            .param("num1").setNumeric(1)
            .param("num2").setNumeric(2)
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Rule 1").group("Test")
                ._if()
                    .param("num1").matchesNumeric(4)
                .then()
                    .param("num2").matchesNumeric(5)
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

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);

        int[] rulesTriggeredCount = new int[1];
        bus.subscribe(RulesTriggeredEvent.class, e -> {
            rulesTriggeredCount[0]++;
        });

        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelEditor", "entity-id=1")));

        Assert.assertEquals(1, rulesTriggeredCount[0]);

        ParamsTab params = (ParamsTab) view.getTabs().iterator().next();
        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        ValueWidget<Object> widget1 = (ValueWidget<Object>) block.getWidgets().get(0).getFirstValueWidget();
        ValueWidget<Object> widget2 = (ValueWidget<Object>) block.getWidgets().get(1).getFirstValueWidget();
        widget1.setValue("4", true);

        Assert.assertEquals(2, rulesTriggeredCount[0]);

        Assert.assertEquals("4", widget1.getValue());
        Assert.assertEquals("", block.getWidgets().get(0).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget1.isEnabled());
        Assert.assertFalse(block.getWidgets().get(0).isRuleLinkVisible());

        Assert.assertEquals("5.00", widget2.getValue());
        Assert.assertEquals("предыдущее значение: 2", block.getWidgets().get(1).getValuesWidget().getOldValueMessage());
        Assert.assertFalse(widget2.isEnabled());
        Assert.assertTrue(block.getWidgets().get(1).isRuleLinkVisible());
    }

    @Test
    public void testLastValueWillPrintLastChangedValueNotSavedOne() {
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
            .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
            .param("num1").setNumeric(1)
            .param("num2").setNumeric(2)
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Rule 1").group("Test")
                ._if()
                    .param("num1").matchesNumeric(4)
                .then()
                    .param("num2").matchesNumeric(5)
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

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);

        bus.fireEvent(new PlaceShowEvent(EditorUrlStub.of("modelEditor", "entity-id=1")));

        ParamsTab params = (ParamsTab) view.getTabs().iterator().next();
        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        ValueWidget<Object> widget1 = (ValueWidget<Object>) block.getWidgets().get(0).getFirstValueWidget();
        ValueWidget<Object> widget2 = (ValueWidget<Object>) block.getWidgets().get(1).getFirstValueWidget();
        widget2.setValue("7", true);
        widget1.setValue("4", true);

        Assert.assertEquals("4", widget1.getValue());
        Assert.assertEquals("5.00", widget2.getValue());
        Assert.assertEquals("предыдущее значение: 7", block.getWidgets().get(1).getValuesWidget().getOldValueMessage());
    }

    @Test
    public void testUIStateAfterModelSave() {
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
            .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
            .param("num1").setNumeric(1)
            .param("num2").setNumeric(2)
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Rule 1").group("Test")
                ._if()
                    .param("num1").matchesNumeric(4)
                .then()
                    .param("num2").matchesNumeric(5)
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

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);

        int[] rulesTriggeredCount = new int[1];
        bus.subscribe(RulesTriggeredEvent.class, e -> {
            rulesTriggeredCount[0]++;
        });

        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelEditor", "entity-id=1")));

        Assert.assertEquals(1, rulesTriggeredCount[0]);

        ParamsTab params = (ParamsTab) view.getTabs().iterator().next();
        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        ValueWidget<Object> widget1 = (ValueWidget<Object>) block.getWidgets().get(0).getFirstValueWidget();
        ValueWidget<Object> widget2 = (ValueWidget<Object>) block.getWidgets().get(1).getFirstValueWidget();
        widget1.setValue("4", true);

        Assert.assertEquals(2, rulesTriggeredCount[0]);

        Assert.assertEquals("4", widget1.getValue());
        Assert.assertEquals("", block.getWidgets().get(0).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget1.isEnabled());
        Assert.assertFalse(block.getWidgets().get(0).isRuleLinkVisible());

        Assert.assertEquals("5.00", widget2.getValue());
        Assert.assertEquals("предыдущее значение: 2", block.getWidgets().get(1).getValuesWidget().getOldValueMessage());
        Assert.assertFalse(widget2.isEnabled());
        Assert.assertTrue(block.getWidgets().get(1).isRuleLinkVisible());

        rpc.setSaveModel(data.getModel().getId(), null);

        bus.fireEvent(
                new SaveModelRequest(false, false));

        Assert.assertEquals(3, rulesTriggeredCount[0]);

        Assert.assertEquals("4", widget1.getValue());
        Assert.assertEquals("", block.getWidgets().get(0).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget1.isEnabled());
        Assert.assertFalse(block.getWidgets().get(0).isRuleLinkVisible());

        Assert.assertEquals("5.00", widget2.getValue());
        Assert.assertEquals("", block.getWidgets().get(1).getValuesWidget().getOldValueMessage());
        Assert.assertFalse(widget2.isEnabled());
        Assert.assertTrue(block.getWidgets().get(1).isRuleLinkVisible());
    }

    @Test
    public void testRuleInfoDialog() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("num1").type(Param.Type.NUMERIC).name("First Numeric Parameter")
            .endParameter()
            .startParameter()
                .xsl("num2").type(Param.Type.NUMERIC).name("Second Numeric Parameter")
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
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Rule 1").group("Test")
                ._if()
                    .param("num1").matchesNumeric(4)
                .then()
                    .param("num2").matchesNumeric(5)
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

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);

        int[] rulesTriggeredCount = new int[1];
        bus.subscribe(RulesTriggeredEvent.class, e -> {
            rulesTriggeredCount[0]++;
        });

        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelEditor", "entity-id=1")));

        Assert.assertEquals(1, rulesTriggeredCount[0]);

        ParamsTab params = (ParamsTab) view.getTabs().iterator().next();
        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        ValueWidget<Object> widget1 = (ValueWidget<Object>) block.getWidgets().get(0).getFirstValueWidget();
        ParamWidget<?> widget2 = block.getWidgets().get(1);

        Assert.assertFalse(block.getWidgets().get(0).isRuleLinkVisible());
        Assert.assertFalse(widget2.isRuleLinkVisible());

        widget1.setValue("4", true);

        Assert.assertEquals(2, rulesTriggeredCount[0]);
        Assert.assertFalse(block.getWidgets().get(0).isRuleLinkVisible());
        Assert.assertTrue(widget2.isRuleLinkVisible());

        bus.fireEvent(new ShowRuleInfoPopupEvent(widget2.getParamMeta().getParamId(), 100, 100));

        EditorWidget dlgWidget = view.getDialogWidget();
        Assert.assertNotNull(dlgWidget);
        Assert.assertTrue(dlgWidget instanceof RuleInfoWidget);
        RuleInfoWidget ruleInfo = (RuleInfoWidget) dlgWidget;

        Assert.assertEquals("Значение поля установлено автоматически", ruleInfo.getMessage());
        Assert.assertFalse(ruleInfo.isErrorStyleEnabled());
        Assert.assertEquals(1, ruleInfo.getBlocks().size());
        Block causeBlock = ruleInfo.getBlocks().get(0);

        Assert.assertEquals("0", causeBlock.getRuleId());
        Assert.assertEquals("Test", causeBlock.getRuleGroup());
        Assert.assertEquals("Rule 1", causeBlock.getRuleName());
        Assert.assertEquals("#tovarTree/hyperId=2/rules/rule-id=0", causeBlock.getRuleLink());
        Assert.assertEquals("First Numeric Parameter = 4", causeBlock.getCauseMessage());
    }

    @Test
    public void testRemoteChanges() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("num1").type(Param.Type.NUMERIC).name("First Numeric Parameter")
            .endParameter()
            .startParameter()
                .xsl("num2").type(Param.Type.NUMERIC).name("Second Numeric Parameter")
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
            .param("num1").setNumeric(4)
            .param("num2").setNumeric(2)
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Rule 1").group("Test")
                ._if()
                    .param("num1").matchesNumeric(4)
                .then()
                    .param("num2").matchesNumeric(5)
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

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);

        int[] rulesTriggeredCount = new int[1];
        bus.subscribe(RulesTriggeredEvent.class, e -> {
            rulesTriggeredCount[0]++;
        });

        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelEditor", "entity-id=1")));

        Assert.assertEquals(1, rulesTriggeredCount[0]);

        ParamsTab params = (ParamsTab) view.getTabs().iterator().next();
        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        ValueWidget<Object> widget1 = (ValueWidget<Object>) block.getWidgets().get(0).getFirstValueWidget();
        ValueWidget<?> widget2 = block.getWidgets().get(1).getFirstValueWidget();

        // Эмулируем изменение модели на сервере 4 раза
        for (int i = 0; i < 4; i++) {
            CommonModel newModel = CommonModelBuilder.builder(m -> m)
                    .parameters(new ArrayList<>(data.getModelData().getParams()))
                    .startModel()
                        .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
                        .param("num1").setNumeric(10 + i)
                        .param("num2").setNumeric(20 + i)
                        .param(XslNames.VENDOR).setOption(1)
                    .endModel();

            bus.fireEvent(new RemoteModelChangedEvent(newModel, Cause.COPIED));

            Assert.assertEquals(i + 2, rulesTriggeredCount[0]);

            // Проверим, что эффект от правил исчез, и при этом значения сохранились серверные
            Assert.assertEquals(Integer.toString(10 + i), widget1.getValue());
            Assert.assertEquals("", block.getWidgets().get(0).getValuesWidget().getOldValueMessage());
            Assert.assertTrue(widget1.isEnabled());
            Assert.assertFalse(block.getWidgets().get(0).isRuleLinkVisible());

            Assert.assertEquals(Integer.toString(20 + i), widget2.getValue());
            Assert.assertEquals("", block.getWidgets().get(1).getValuesWidget().getOldValueMessage());
            Assert.assertTrue(widget2.isEnabled());
            Assert.assertFalse(block.getWidgets().get(1).isRuleLinkVisible());
        }
    }

    @Test
    @SuppressWarnings("checkstyle:methodLength")
    public void testRemoteChangesAndUserInput() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("num1").type(Param.Type.NUMERIC).name("1 Numeric Parameter")
            .endParameter()
            .startParameter()
                .xsl("num2").type(Param.Type.NUMERIC).name("2 Numeric Parameter")
            .endParameter()
            .startParameter()
                .xsl("num3").type(Param.Type.NUMERIC).name("3 Numeric Parameter")
            .endParameter()
                .startParameter()
                .xsl("num4").type(Param.Type.NUMERIC).name("4 Numeric Parameter")
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
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Rule 1").group("Test")
                ._if()
                    .param("num1").matchesNumeric(1)
                .then()
                    .param("num2").matchesNumeric(20)
            .endRule()
        .endRuleSet()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("num1", "num2", "num3", "num4")
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

        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelEditor", "entity-id=1")));

        Assert.assertEquals(1, rulesTriggeredCount[0]);

        ParamsTab params = (ParamsTab) view.getTabs().iterator().next();
        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        ValueWidget<Object> widget1 = (ValueWidget<Object>) block.getWidgets().get(0).getFirstValueWidget();
        ValueWidget<?> widget2 = block.getWidgets().get(1).getFirstValueWidget();
        ValueWidget<?> widget3 = block.getWidgets().get(2).getFirstValueWidget();
        ValueWidget<?> widget4 = block.getWidgets().get(3).getFirstValueWidget();

        // Проверка начального состояния виджетов
        Assert.assertEquals("1", widget1.getValue());
        Assert.assertEquals("", block.getWidgets().get(0).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget1.isEnabled());
        Assert.assertFalse(block.getWidgets().get(0).isRuleLinkVisible());

        Assert.assertEquals("20.00", widget2.getValue());
        Assert.assertEquals("предыдущее значение: 2", block.getWidgets().get(1).getValuesWidget().getOldValueMessage());
        Assert.assertFalse(widget2.isEnabled());
        Assert.assertTrue(block.getWidgets().get(1).isRuleLinkVisible());

        Assert.assertEquals("3", widget3.getValue());
        Assert.assertEquals("", block.getWidgets().get(2).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget3.isEnabled());
        Assert.assertFalse(block.getWidgets().get(2).isRuleLinkVisible());

        Assert.assertEquals("4", widget4.getValue());
        Assert.assertEquals("", block.getWidgets().get(3).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget4.isEnabled());
        Assert.assertFalse(block.getWidgets().get(3).isRuleLinkVisible());

        // Меняем значение первого параметра таким образом, чтобы правила не действовало на нем
        widget1.setValue("10", true);

        Assert.assertEquals(2, rulesTriggeredCount[0]);

        // Теперь значение во втором виджете должно откатиться к оригинальному,
        // и прочее состояние виджета - тоже
        Assert.assertEquals("2", widget2.getValue());
        Assert.assertEquals("", block.getWidgets().get(1).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget2.isEnabled());
        Assert.assertFalse(block.getWidgets().get(3).isRuleLinkVisible());

        // Эмулируем изменение модели на сервере 4 раза
        for (int i = 0; i < 4; i++) {
            CommonModel newModel = CommonModelBuilder.builder(m -> m)
                    .parameters(new ArrayList<>(data.getModelData().getParams()))
                    .startModel()
                        .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
                        .param("num1").setNumeric(100 + i) // изменился
                        .param("num2").setNumeric(200 + i) // изменился
                        .param("num3").setNumeric(300 + i) // изменился
                        .param("num4").setNumeric(4) // не изменился
                        .param(XslNames.VENDOR).setOption(1)
                    .endModel();

            bus.fireEvent(new RemoteModelChangedEvent(newModel, Cause.COPIED));

            Assert.assertEquals(i + 3, rulesTriggeredCount[0]);

            // Проверим состояния виджетов после изменения модели на сервере
            // Значение в первом виджете должно сохраниться таким, как ввел его пользователь (10)
            Assert.assertEquals("10", widget1.getValue());
            Assert.assertEquals("", block.getWidgets().get(0).getValuesWidget().getOldValueMessage());
            Assert.assertTrue(widget1.isEnabled());
            Assert.assertFalse(block.getWidgets().get(0).isRuleLinkVisible());

            // Значение во втором виджете должно обновиться на серверное, потому что пользователь его не трогал
            // (оно модифицировалось только правилами)
            Assert.assertEquals(Integer.toString(200 + i), widget2.getValue());
            Assert.assertEquals("", block.getWidgets().get(1).getValuesWidget().getOldValueMessage());
            Assert.assertTrue(widget2.isEnabled());
            Assert.assertFalse(block.getWidgets().get(1).isRuleLinkVisible());

            // Значение в третьем виджете должно обновиться на серверное, потому что оно не менялось вообще
            Assert.assertEquals(Integer.toString(300 + i), widget3.getValue());
            Assert.assertEquals("", block.getWidgets().get(2).getValuesWidget().getOldValueMessage());
            Assert.assertTrue(widget3.isEnabled());
            Assert.assertFalse(block.getWidgets().get(2).isRuleLinkVisible());

            // Значение в четвертом виджете должно сохраниться прежним, потому что оно не менялось на сервере
            Assert.assertEquals("4", widget4.getValue());
            Assert.assertEquals("", block.getWidgets().get(3).getValuesWidget().getOldValueMessage());
            Assert.assertTrue(widget4.isEnabled());
            Assert.assertFalse(block.getWidgets().get(3).isRuleLinkVisible());
        }

        //Сохраним модель. После сохранения пользовательский ввод скинется и не будет препятсвовать обновлению с сервера
        rpc.setSaveModel(1L, null);
        bus.fireEvent(
                new SaveModelRequest(false, false));

        // Пробуем теперь еще раз изменить модель на сервере
        CommonModel newModel = CommonModelBuilder.builder(m -> m)
                .parameters(new ArrayList<>(data.getModelData().getParams()))
                .startModel()
                    .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
                    .param("num1").setNumeric(100)
                    .param("num2").setNumeric(200)
                    .param("num3").setNumeric(300)
                    .param("num4").setNumeric(4)
                    .param(XslNames.VENDOR).setOption(1)
                .endModel();

        bus.fireEvent(new RemoteModelChangedEvent(newModel, Cause.COPIED));

        Assert.assertEquals(8, rulesTriggeredCount[0]);

        // Проверим состояния виджетов после изменения модели на сервере
        // Значение в первом виджете теперь должно быть как на сервере,
        // потому что введенное пользователем значение (10) уже было сохранено
        Assert.assertEquals("100", widget1.getValue());
        Assert.assertEquals("", block.getWidgets().get(0).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget1.isEnabled());
        Assert.assertFalse(block.getWidgets().get(0).isRuleLinkVisible());

        // Значение во втором виджете будет как на сервере
        Assert.assertEquals(Integer.toString(200), widget2.getValue());
        Assert.assertEquals("", block.getWidgets().get(1).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget2.isEnabled());
        Assert.assertFalse(block.getWidgets().get(1).isRuleLinkVisible());

        // Значение в третьем виджете будет как на сервере
        Assert.assertEquals(Integer.toString(300), widget3.getValue());
        Assert.assertEquals("", block.getWidgets().get(2).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget3.isEnabled());
        Assert.assertFalse(block.getWidgets().get(2).isRuleLinkVisible());

        // Значение в четвертом виджете останется прежним
        Assert.assertEquals("4", widget4.getValue());
        Assert.assertEquals("", block.getWidgets().get(3).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget4.isEnabled());
        Assert.assertFalse(block.getWidgets().get(3).isRuleLinkVisible());

        // Теперь меняем модель на сервере таким образом, чтобы сработало правило
        newModel = CommonModelBuilder.builder(m -> m)
                .parameters(new ArrayList<>(data.getModelData().getParams()))
                .startModel()
                    .id(1).category(2).source(Source.GURU).currentType(Source.GURU)
                    .param("num1").setNumeric(1)
                    .param("num2").setNumeric(2000)
                    .param("num3").setNumeric(3000)
                    .param("num4").setNumeric(4)
                    .param(XslNames.VENDOR).setOption(1)
                .endModel();

        bus.fireEvent(new RemoteModelChangedEvent(newModel, Cause.COPIED));

        Assert.assertEquals(9, rulesTriggeredCount[0]);

        // Значение в первом виджете должно быть как на сервере
        Assert.assertEquals("1", widget1.getValue());
        Assert.assertEquals("", block.getWidgets().get(0).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget1.isEnabled());
        Assert.assertFalse(block.getWidgets().get(0).isRuleLinkVisible());

        // Значение во втором виджете будет установлено правилом (то есть 20)
        // Это связано с тем, что правило перезапишет изменение, вызванное копированием (2000)
        Assert.assertEquals("20.00", widget2.getValue());
        // будет показано скопированное значение 2000
        Assert.assertEquals("предыдущее значение: 2000",
            block.getWidgets().get(1).getValuesWidget().getOldValueMessage());
        Assert.assertFalse(widget2.isEnabled());
        Assert.assertTrue(block.getWidgets().get(1).isRuleLinkVisible());

        // Значение в третьем виджете будет как на сервере
        Assert.assertEquals("3000", widget3.getValue());
        Assert.assertEquals("", block.getWidgets().get(2).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget3.isEnabled());
        Assert.assertFalse(block.getWidgets().get(2).isRuleLinkVisible());

        // Значение в четвертом виджете останется прежним
        Assert.assertEquals("4", widget4.getValue());
        Assert.assertEquals("", block.getWidgets().get(3).getValuesWidget().getOldValueMessage());
        Assert.assertTrue(widget4.isEnabled());
        Assert.assertFalse(block.getWidgets().get(3).isRuleLinkVisible());
    }

}
