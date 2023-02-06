package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelDataBuilder;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PublishOnBlueRequestedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.SaveModelRequest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.AbstractModelTest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.LinksPanelStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ModelEditorViewStub;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Date;

/**
 * Tests of {@link EditorLinksAddon}.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ChangePublishStatusAddonTest extends AbstractModelTest {

    @Test
    public void testPublishOnBlue() throws Exception {
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
            .id(1).category(2).published(false).publishedOnBlue(false)
            .source(CommonModel.Source.GURU).currentType(CommonModel.Source.GURU)
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

        Assert.assertFalse(data.getModel().isPublished());
        Assert.assertFalse(data.getModel().isBluePublished());

        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", "entity-id=1")));

        Assert.assertTrue(view.getLinksPanel().isVisible());
        LinksPanelStub linksPanelStub = (LinksPanelStub) view.getLinksPanel();
        Assert.assertTrue(linksPanelStub.getPublishOnBlueLinkVisible());
        Assert.assertEquals(ModelEditorViewStub.TitleBarStatus.UNPUBLISHED_UNSIGNED, view.getTitleBarStatus());

        bus.fireEvent(new PublishOnBlueRequestedEvent());

        rpc.setSaveModel(data.getModel().getId(), null);
        bus.fireEvent(
            new SaveModelRequest(false, false));

        CommonModel savedModel = rpc.getSavedModel();
        Assert.assertTrue(savedModel.isBluePublished());

        savedModel.setPublished(true);
        data.getModelData().setModel(savedModel);
        rpc.setLoadModel(savedModel, null);

        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", "entity-id=1")));

        Assert.assertFalse(linksPanelStub.getPublishOnBlueLinkVisible());
        Assert.assertEquals(ModelEditorViewStub.TitleBarStatus.PUBLISHED_UNSIGNED, view.getTitleBarStatus());
    }
}
