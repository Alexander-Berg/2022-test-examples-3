package ru.yandex.market.mbo.gwt.client.pages.model.editor.test;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Test;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelDataBuilder;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.RulesTriggeredEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.BlockWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamsTab;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValuesWidget;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 04.04.2019
 */
public class ModelRuleSingleEnumTest extends AbstractTest {
    @Test
    public void recoverEmptyEnumValue() {
        // @formatter:off
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
                .hidden(true)
                .option(1, "Vendor1")
            .endParameter()
            .startParameter()
                .xsl("flag").type(Param.Type.BOOLEAN).name("Activate rule flag")
                .option(1, "TRUE")
                .option(2, "FALSE")
            .endParameter()
            .startParameter()
                .xsl("enum-param").type(Param.Type.ENUM).name("Enumeric parameter")
                .option(1, "green")
            .endParameter()
        .endParameters()
        .startModel()
            .id(1).category(2).source(CommonModel.Source.GURU).currentType(CommonModel.Source.GURU)
            .param(XslNames.VENDOR).setOption(1)
        .endModel()
        .startRuleSet()
            .id(1)
            .startRule()
                .name("Rule 1").group("Test")
                ._if()
                    .param("flag").matchesBoolean(true)
                .then()
                    .param("enum-param").isEmpty()
            .endRule()
        .endRuleSet()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("flag", "enum-param")
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

        MutableInt rulesTriggeredCount = new MutableInt();
        bus.subscribe(RulesTriggeredEvent.class, e -> {
            rulesTriggeredCount.increment();
        });

        bus.fireEvent(new PlaceShowEvent(EditorUrlStub.of("modelEditor", "entity-id=1")));

        assertThat(rulesTriggeredCount.getValue()).isOne();

        ParamsTab params = (ParamsTab) view.getTabs().iterator().next();
        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        ValuesWidget<Object> flagWidget = block.getWidget(0).getValuesWidget();
        ValuesWidget<Object> enumWidget = block.getWidget(1).getValuesWidget();

        assertThat(flagWidget.getValues()).containsNull();
        assertThat(enumWidget.getValues()).containsNull();
        assertThat(enumWidget.getFirstValueWidget().getValueField().isEnabled()).isTrue();

        flagWidget.getFirstValueWidget().setValue(true, true);

        assertThat(enumWidget.getValues()).containsNull();
        assertThat(enumWidget.getFirstValueWidget().getValueField().isEnabled()).isFalse();

        flagWidget.getFirstValueWidget().setValue(false, true);

        assertThat(enumWidget.getValues()).containsNull();
        assertThat(enumWidget.getFirstValueWidget().getValueField().isEnabled()).isTrue();
    }

}
