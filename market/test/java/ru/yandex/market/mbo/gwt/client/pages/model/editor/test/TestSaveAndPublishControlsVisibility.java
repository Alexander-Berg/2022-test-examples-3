package ru.yandex.market.mbo.gwt.client.pages.model.editor.test;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.common.gwt.shared.User;
import ru.yandex.market.mbo.common.processing.ModelProcessingError;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelDataBuilder;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.events.ForceSaveTestEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.SaveModelRequest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ForceSaveWidgetStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ModelEditorViewStub;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.Role;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Collections;
import java.util.Date;

import static ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ModelEditorViewStub.USER_UPDATE_TIME;

/**
 * @author dmserebr
 * @date 28.04.18
 */
@SuppressWarnings("checkstyle:magicNumber")
public class TestSaveAndPublishControlsVisibility extends AbstractTest {
    @Test
    public void testUnpublishedModel() {
        ModelDataBuilder data = createTestModelData(CommonModel.Source.GURU);

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);
        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", "entity-id=1")));

        Assert.assertTrue("Save & publish button is not enabled", view.isSaveAndPublishButtonEnabled());
        Assert.assertTrue("Save & publish button is not visible", view.isSaveAndPublishButtonVisible());
        Assert.assertEquals(ModelEditorViewStub.TitleBarStatus.UNPUBLISHED_UNSIGNED, view.getTitleBarStatus());

        rpc.setSaveModel(1L, null);

        bus.fireEvent(new SaveModelRequest(false, false, true));

        Assert.assertFalse("Save & publish button is enabled", view.isSaveAndPublishButtonEnabled());
        Assert.assertTrue("Save & publish button is not visible", view.isSaveAndPublishButtonVisible());
        Assert.assertEquals(ModelEditorViewStub.TitleBarStatus.PUBLISHED_UNSIGNED, view.getTitleBarStatus());
    }

    @Test
    public void testUnpublishedSku() {
        ModelDataBuilder data = createTestModelData(CommonModel.Source.SKU);

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);
        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", "entity-id=1")));

        Assert.assertFalse("Save & publish button is visible", view.isSaveAndPublishButtonVisible());
    }

    @Test
    public void testPublishedModel() {
        ModelDataBuilder data = createTestModelData(CommonModel.Source.GURU);
        data.getModel().addParameterValue(new ParameterValue(10L, "DBFilledOK", Param.Type.BOOLEAN,
            ParameterValue.ValueBuilder.newBuilder().setBooleanValue(true)));
        data.getModel().setPublished(true);

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);
        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", "entity-id=1")));

        Assert.assertFalse("Save & publish button is enabled", view.isSaveAndPublishButtonEnabled());
        Assert.assertTrue("Save & publish button is not visible", view.isSaveAndPublishButtonVisible());
        Assert.assertEquals(ModelEditorViewStub.TitleBarStatus.PUBLISHED_SIGNED, view.getTitleBarStatus());
    }

    @Test
    public void testPublishedSku() {
        ModelDataBuilder data = createTestModelData(CommonModel.Source.SKU);
        data.getModel().setPublished(true);

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);
        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", "entity-id=1")));

        Assert.assertFalse("Save & publish button is visible", view.isSaveAndPublishButtonVisible());
        Assert.assertEquals(ModelEditorViewStub.TitleBarStatus.PUBLISHED_UNSIGNED, view.getTitleBarStatus());
    }

    @Test
    public void testNewModel() {
        ModelDataBuilder data = createTestModelData(CommonModel.Source.GURU);
        data.getModel().setId(0L);

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);
        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", "entity-id=1")));

        Assert.assertFalse("Save & publish button is enabled", view.isSaveAndPublishButtonEnabled());
        Assert.assertTrue("Save & publish button is not visible", view.isSaveAndPublishButtonVisible());
        Assert.assertEquals(ModelEditorViewStub.TitleBarStatus.UNPUBLISHED_UNSIGNED, view.getTitleBarStatus());
        Assert.assertFalse("AutoSave CheckBox is not visible", view.isAutoSaveCheckBoxVisible());
    }

    @Test
    public void testUserWithoutAccess() {
        User operator = new User("operator", 1, USER_UPDATE_TIME);
        operator.setRole(Role.OPERATOR);
        view.setUser(operator);

        ModelDataBuilder data = createTestModelData(CommonModel.Source.GURU);
        data.getModel().setPublished(false);

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);
        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", "entity-id=1")));

        Assert.assertFalse("Save & publish button is visible", view.isSaveAndPublishButtonVisible());
        Assert.assertEquals(ModelEditorViewStub.TitleBarStatus.UNPUBLISHED_UNSIGNED, view.getTitleBarStatus());
    }

    @Test
    public void testSaveError() {
        ModelDataBuilder data = createTestModelData(CommonModel.Source.GURU);

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);
        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", "entity-id=1")));

        rpc.setSaveModel(null, new RuntimeException("Failed to save"));

        bus.fireEvent(new SaveModelRequest(false, false, true));

        Assert.assertTrue("Save & publish button is not enabled", view.isSaveAndPublishButtonEnabled());
        Assert.assertTrue("Save & publish button is not visible", view.isSaveAndPublishButtonVisible());
        Assert.assertEquals(ModelEditorViewStub.TitleBarStatus.UNPUBLISHED_UNSIGNED, view.getTitleBarStatus());
    }

    @Test
    public void testSaveErrorThenForceSave() {
        ModelDataBuilder data = createTestModelData(CommonModel.Source.GURU);

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);
        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", "entity-id=1")));

        rpc.setSaveModel(null,
            new OperationException(Collections.singletonList(
                new ModelProcessingError("testGroup", "testTemplate", Collections.emptyMap(),
                    true, true))));

        bus.fireEvent(new SaveModelRequest(false, false, true));

        Assert.assertTrue("Save & publish button is not enabled", view.isSaveAndPublishButtonEnabled());
        Assert.assertTrue("Save & publish button is not visible", view.isSaveAndPublishButtonVisible());
        Assert.assertNotNull("Error widget didn't appear", view.getPopupWidget());
        Assert.assertEquals(ModelEditorViewStub.TitleBarStatus.UNPUBLISHED_UNSIGNED, view.getTitleBarStatus());
        Assert.assertTrue("Error widget is of wrong type", view.getPopupWidget() instanceof ForceSaveWidgetStub);

        ((ForceSaveWidgetStub) view.getPopupWidget()).subscribeToForceSaveEvent(bus);
        rpc.setSaveModel(1L, null);
        bus.fireEvent(new ForceSaveTestEvent());

        Assert.assertFalse("Save & publish button is still enabled", view.isSaveAndPublishButtonEnabled());
        Assert.assertTrue("Save & publish button is not visible", view.isSaveAndPublishButtonVisible());
        Assert.assertEquals(ModelEditorViewStub.TitleBarStatus.PUBLISHED_UNSIGNED, view.getTitleBarStatus());
        Assert.assertNull("Popup widget still appears", view.getPopupWidget());
    }

    @Test
    public void testSaveErrorThenSaveWithoutPublishing() {
        ModelDataBuilder data = createTestModelData(CommonModel.Source.GURU);

        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);
        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", "entity-id=1")));

        rpc.setSaveModel(null, new RuntimeException("Failed to save"));

        bus.fireEvent(new SaveModelRequest(false, false, true));

        Assert.assertTrue("Save & publish button is not enabled", view.isSaveAndPublishButtonEnabled());
        Assert.assertTrue("Save & publish button is not visible", view.isSaveAndPublishButtonVisible());
        Assert.assertEquals(ModelEditorViewStub.TitleBarStatus.UNPUBLISHED_UNSIGNED, view.getTitleBarStatus());

        rpc.setSaveModel(1L, null);

        bus.fireEvent(new SaveModelRequest(false, false, false));

        Assert.assertTrue("Save & publish button is not enabled", view.isSaveAndPublishButtonEnabled());
        Assert.assertTrue("Save & publish button is not visible", view.isSaveAndPublishButtonVisible());
        Assert.assertEquals(ModelEditorViewStub.TitleBarStatus.UNPUBLISHED_UNSIGNED, view.getTitleBarStatus());
    }

    private ModelDataBuilder createTestModelData(CommonModel.Source type) {
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
                .id(1).category(2).source(type).currentType(type)
                .published(false).publishedOnBlue(false)
                .param("num").setNumeric(1).modificationSource(ModificationSource.OPERATOR_FILLED)
                .param(XslNames.VENDOR).setOption(1)
                .computeIf(type == CommonModel.Source.SKU,
                    builder -> builder.startModelRelation()
                        .id(10)
                        .categoryId(2)
                        .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
                        .model(CommonModelBuilder.newBuilder()
                            .id(10).category(2)
                            .getModel())
                    .endModelRelation()
                )
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

        return data;
    }
}
