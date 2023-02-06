package ru.yandex.market.mbo.gwt.client.pages.model.editor.test.rules;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.ModelCompatibilityAddon;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelDataBuilder;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.RulesTriggeredEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.AbstractTest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.BlockWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ErrorsWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamsTab;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Date;

/**
 * @author gilmulla
 */
@SuppressWarnings("magicnumber")
public class TestMandatoryRules extends AbstractTest {


    @Override
    protected Class<?>[] excludedAddons() {
        return new Class<?>[] {ModelCompatibilityAddon.class};
    }

    @Test
    public void mandatoryNotApplied() {

        System.out.println(new Date(1488852000150L));

        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("num1").type(Param.Type.NUMERIC).name("Numeric Parameter 1")
            .endParameter()
            .startParameter()
                .xsl("num2").type(Param.Type.NUMERIC).name("Numeric Parameter 2")
            .endParameter()
            .startParameter()
                .xsl(XslNames.OPERATOR_SIGN).type(Param.Type.BOOLEAN)
                .name("Подпись оператора")
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
            .id(1).category(2).source(CommonModel.Source.GURU).currentType(CommonModel.Source.GURU)
            .param(XslNames.OPERATOR_SIGN).setBoolean(false)
            .param("num1").setNumeric(1)
            .param("num2").setEmpty()
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Rule 1").group("Test")
                ._if()
                    .param("num1").matchesNumeric(10)
                .then()
                    .param("num2").mandatory()
            .endRule()
        .endRuleSet()
        .startForm()
            .startTab()
            .name(EditorTabs.PARAMETERS.getDisplayName())
            .startBlock()
            .name("block")
            .properties("num1", "num2", XslNames.OPERATOR_SIGN)
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
        ParamWidget<Object> widget1 = (ParamWidget<Object>) block.getWidgets().get(0);
        ParamWidget<?> widget2 = block.getWidgets().get(1);
        ParamWidget<Object> widget3 = (ParamWidget<Object>) block.getWidgets().get(2);

        Assert.assertFalse(widget2.isRuleLinkVisible());
        Assert.assertTrue(view.isSaveButtonEnabled());

        // Меняем значение первого виджета, чтобы сработало условие правила
        widget1.getFirstValueWidget().setValue("10", true);

        Assert.assertEquals(2, rulesTriggeredCount[0]);

        // Так как подпись оператора не выставлена, это не должно повлиять
        Assert.assertFalse(widget2.isRuleLinkVisible());
        Assert.assertTrue(view.isSaveButtonEnabled());

        // Ставим подпись оператора
        widget3.getFirstValueWidget().setValue(true, true);

        // Правило должно примениться после простановки подписи
        Assert.assertEquals(3, rulesTriggeredCount[0]);

        // У заблокированного правилом виджета должна появиться ссылка на правило,
        // а кнопка сохранения должна быть заблокирована
        Assert.assertTrue(widget2.isRuleLinkVisible());
        Assert.assertFalse(view.isSaveButtonEnabled());
        // Должно появиться оповещение об ошибке
        Assert.assertTrue(view.getOperationWidget() instanceof ErrorsWidget);
        ErrorsWidget errorsWidget = (ErrorsWidget) view.getOperationWidget();
        Assert.assertEquals(1, errorsWidget.getErrors().size());
//        Assert.assertEquals("Error message 1", errorsWidget.getErrors().get(0).getText());
//        Assert.assertEquals("Server error", errorsWidget.getErrors().get(0).getGroup());
        Assert.assertFalse(errorsWidget.isShowMoreErrorsVisible());
    }
}
