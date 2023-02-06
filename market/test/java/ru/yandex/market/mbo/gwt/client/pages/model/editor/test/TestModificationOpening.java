package ru.yandex.market.mbo.gwt.client.pages.model.editor.test;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelDataBuilder;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel.Source;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Date;

/**
 * @author gilmulla
 *
 */
@SuppressWarnings("checkstyle:magicNumber")
public class TestModificationOpening extends AbstractTest {

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

        Assert.assertTrue(view.isImageCopyVisible());
        Assert.assertFalse(view.isCopyPanelVisible());
        Assert.assertTrue(view.getNavigationPanel().isParentLinkVisible());
        Assert.assertFalse(view.getNavigationPanel().isModelLinkVisible());
        Assert.assertTrue(view.getNavigationPanel().isModelLabelVisible());
    }
}
