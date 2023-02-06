package ru.yandex.market.mbo.gwt.client.pages.model.editor.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.processing.ModelProcessingError;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.common.processing.ProcessingResult;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelDataBuilder;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.MoreErrorsRequestedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.SaveModelRequest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ViewUtils;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.BlockWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ErrorsWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ForceSaveWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamsTab;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueWidget;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel.Source;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author gilmulla
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class TestServerErrorHandling extends AbstractTest {

    @Before
    public void begin() {
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
        ValueWidget<Object> widget1 = (ValueWidget<Object>) ViewUtils.getFirstWidget(block);
        ValueWidget<Object> widget3 = (ValueWidget<Object>) ViewUtils.getValueWidget(block, 2);

        widget1.setValue(Arrays.asList("newvalue1"), true);

        widget3.setValue(Arrays.asList("newvalue3"), true);
    }

    @Test
    public void testNoForceError() {
        List<ProcessingResult> details = new ArrayList<>();
        details.add(new ModelProcessingError("Server error", "Error message 1", false, true));
        details.add(new ModelProcessingError("Server error", "Error message 2", false, true));
        OperationException exception = new OperationException(details);
        rpc.setSaveModel(null, exception);

        bus.fireEvent(
                new SaveModelRequest(false, false));

        Assert.assertNotNull(view.getOperationWidget());
        Assert.assertTrue(view.getOperationWidget() instanceof ErrorsWidget);
        assertErrorsWidgetInFooter((ErrorsWidget) view.getOperationWidget());
        Assert.assertNull(view.getPopupWidget()); // проверка, что force-save окно не открылось

        bus.fireEvent(new MoreErrorsRequestedEvent());

        Assert.assertNotNull(view.getPopupWidget());
        // В попапе содержится тоже errors widget, но содержащий все ошибки
        Assert.assertTrue(view.getPopupWidget() instanceof ErrorsWidget);
        assertErrorsWidgetInPopup((ErrorsWidget) view.getPopupWidget());
    }

    @Test
    public void testForceError() {
        List<ProcessingResult> details = new ArrayList<>();
        details.add(new ModelProcessingError("Server error", "Error message 1", true, true));
        details.add(new ModelProcessingError("Server error", "Error message 2", true, true));
        OperationException exception = new OperationException(details);
        rpc.setSaveModel(null, exception);

        bus.fireEvent(
                new SaveModelRequest(false, false));

        Assert.assertNotNull(view.getOperationWidget());
        Assert.assertTrue(view.getOperationWidget() instanceof ErrorsWidget);
        assertForceErrorsWidgetInFooter((ErrorsWidget) view.getOperationWidget());

        Assert.assertNotNull(view.getPopupWidget()); // проверка, что открылось окно force-save
        Assert.assertTrue(view.getPopupWidget() instanceof ForceSaveWidget);
        ForceSaveWidget forceSaveWidget = (ForceSaveWidget) view.getPopupWidget();
        assertForceErrorsWidgetInPopup(forceSaveWidget.getErrorsWidget());
        view.hidePopup();
        Assert.assertNull(view.getPopupWidget()); // проверка, что закрылось окно force-save

        bus.fireEvent(new MoreErrorsRequestedEvent()); // нажатие на кнопку More

        Assert.assertNotNull(view.getPopupWidget()); // проверка, что окно force-save снова открылось
        Assert.assertTrue(view.getPopupWidget() instanceof ForceSaveWidget);
        forceSaveWidget = (ForceSaveWidget) view.getPopupWidget();
        assertForceErrorsWidgetInPopup(forceSaveWidget.getErrorsWidget());
    }

    @Test
    public void testForceErrorForceSaveSuccess() {
        List<ProcessingResult> details = new ArrayList<>();
        details.add(new ModelProcessingError("Server error", "Error message 1", true, true));
        details.add(new ModelProcessingError("Server error", "Error message 2", true, true));
        OperationException exception = new OperationException(details);
        rpc.setSaveModel(null, exception);

        bus.fireEvent(
                new SaveModelRequest(false, false));

        Assert.assertNotNull(view.getOperationWidget());
        Assert.assertTrue(view.getOperationWidget() instanceof ErrorsWidget);
        assertForceErrorsWidgetInFooter((ErrorsWidget) view.getOperationWidget());

        Assert.assertNotNull(view.getPopupWidget()); // проверка, что открылось окно force-save
        Assert.assertTrue(view.getPopupWidget() instanceof ForceSaveWidget);
        ForceSaveWidget forceSaveWidget = (ForceSaveWidget) view.getPopupWidget();
        assertForceErrorsWidgetInPopup(forceSaveWidget.getErrorsWidget());

        rpc.setSaveModel(1L, null);

        // имитация нажатия на кнопку "Сохранить" в попапе
        bus.fireEvent(new SaveModelRequest(false, true));

        Assert.assertNull(view.getOperationWidget()); // проверка что сообщ об ошибке исчезло
        Assert.assertNull(view.getPopupWidget()); // проверка, что попап с force save не открылся
    }

    @Test
    public void testForceErrorForceSaveNotForceError() {
        List<ProcessingResult> details = new ArrayList<>();
        details.add(new ModelProcessingError("Server error", "Error message 1", true, true));
        details.add(new ModelProcessingError("Server error", "Error message 2", true, true));
        OperationException exception = new OperationException(details);
        rpc.setSaveModel(null, exception);

        bus.fireEvent(
                new SaveModelRequest(false, false));

        Assert.assertNotNull(view.getOperationWidget());
        Assert.assertTrue(view.getOperationWidget() instanceof ErrorsWidget);
        assertForceErrorsWidgetInFooter((ErrorsWidget) view.getOperationWidget());

        Assert.assertNotNull(view.getPopupWidget()); // проверка, что открылось окно force-save
        Assert.assertTrue(view.getPopupWidget() instanceof ForceSaveWidget);
        ForceSaveWidget forceSaveWidget = (ForceSaveWidget) view.getPopupWidget();
        assertForceErrorsWidgetInPopup(forceSaveWidget.getErrorsWidget());

        // При втором сохранении - невосстановимые ошибки
        details = new ArrayList<>();
        details.add(new ModelProcessingError("Server error", "Error message 1", false, true));
        details.add(new ModelProcessingError("Server error", "Error message 2", false, true));
        exception = new OperationException(details);
        rpc.setSaveModel(null, exception);

        // имитация нажатия на кнопку "Сохранить" в попапе
        bus.fireEvent(new SaveModelRequest(false, true));

        Assert.assertNotNull(view.getOperationWidget()); // проверка что появилось сообщение об ошибке
        Assert.assertTrue(view.getOperationWidget() instanceof ErrorsWidget);
        assertErrorsWidgetInFooter((ErrorsWidget) view.getOperationWidget());

        Assert.assertNull(view.getPopupWidget()); // проверка, что попап с force save не открылся

        bus.fireEvent(new MoreErrorsRequestedEvent()); // нажатие на кнопку moar

        Assert.assertNotNull(view.getPopupWidget());
        // В попапе содержится тоже errors widget, но содержащий все ошибки
        Assert.assertTrue(view.getPopupWidget() instanceof ErrorsWidget);
        assertErrorsWidgetInPopup((ErrorsWidget) view.getPopupWidget());
    }

    @Test
    public void testForceErrorForceSaveForceError() {
        List<ProcessingResult> details = new ArrayList<>();
        details.add(new ModelProcessingError("Server error", "Error message 1", true, true));
        details.add(new ModelProcessingError("Server error", "Error message 2", true, true));
        OperationException exception = new OperationException(details);
        rpc.setSaveModel(null, exception);

        bus.fireEvent(
                new SaveModelRequest(false, false));

        Assert.assertNotNull(view.getOperationWidget());
        Assert.assertTrue(view.getOperationWidget() instanceof ErrorsWidget);
        assertForceErrorsWidgetInFooter((ErrorsWidget) view.getOperationWidget());

        Assert.assertNotNull(view.getPopupWidget()); // проверка, что открылось окно force-save
        Assert.assertTrue(view.getPopupWidget() instanceof ForceSaveWidget);
        ForceSaveWidget forceSaveWidget = (ForceSaveWidget) view.getPopupWidget();
        assertForceErrorsWidgetInPopup(forceSaveWidget.getErrorsWidget());

        // При втором сохранении - снова восстановимые ошибки
        details = new ArrayList<>();
        details.add(new ModelProcessingError("Server error", "Error message 1", true, true));
        details.add(new ModelProcessingError("Server error", "Error message 2", true, true));
        exception = new OperationException(details);
        rpc.setSaveModel(null, exception);

        // имитация нажатия на кнопку "Сохранить" в попапе
        bus.fireEvent(new SaveModelRequest(false, true));

        Assert.assertNotNull(view.getOperationWidget()); // проверка что появилось сообщение об ошибке
        Assert.assertTrue(view.getOperationWidget() instanceof ErrorsWidget);
        assertForceErrorsWidgetInFooter((ErrorsWidget) view.getOperationWidget());

        Assert.assertNotNull(view.getPopupWidget()); // проверка, что открылся попап с force save

        bus.fireEvent(new MoreErrorsRequestedEvent()); // нажатие на кнопку moar

        Assert.assertNotNull(view.getPopupWidget());
        // В попапе содержится тоже errors widget, но содержащий все ошибки
        Assert.assertTrue(view.getPopupWidget() instanceof ForceSaveWidget);
        forceSaveWidget = (ForceSaveWidget) view.getPopupWidget();
        assertForceErrorsWidgetInPopup(forceSaveWidget.getErrorsWidget());
    }

    private void assertErrorsWidgetInFooter(ErrorsWidget errorsWidget) {
        Assert.assertEquals(1, errorsWidget.getErrors().size());
        Assert.assertEquals("Error message 1", errorsWidget.getErrors().get(0).getText());
        Assert.assertEquals("Server error", errorsWidget.getErrors().get(0).getGroup());
        Assert.assertTrue(errorsWidget.isShowMoreErrorsVisible());
    }

    private void assertErrorsWidgetInPopup(ErrorsWidget embeddedErrorsWidget) {
        Assert.assertEquals(2, embeddedErrorsWidget.getErrors().size());
        Assert.assertEquals("Error message 1", embeddedErrorsWidget.getErrors().get(0).getText());
        Assert.assertEquals("Server error", embeddedErrorsWidget.getErrors().get(0).getGroup());
        Assert.assertEquals("Error message 2", embeddedErrorsWidget.getErrors().get(1).getText());
        Assert.assertEquals("Server error", embeddedErrorsWidget.getErrors().get(1).getGroup());
        Assert.assertFalse(embeddedErrorsWidget.isShowMoreErrorsVisible());
    }

    private void assertForceErrorsWidgetInFooter(ErrorsWidget errorsWidget) {
        Assert.assertEquals(1, errorsWidget.getErrors().size());
        Assert.assertEquals("<b>Error message 1</b>", errorsWidget.getErrors().get(0).getText());
        Assert.assertEquals("Server error", errorsWidget.getErrors().get(0).getGroup());
        Assert.assertTrue(errorsWidget.isShowMoreErrorsVisible());
    }

    private void assertForceErrorsWidgetInPopup(ErrorsWidget embeddedErrorsWidget) {
        Assert.assertEquals(2, embeddedErrorsWidget.getErrors().size());
        Assert.assertEquals("<b>Error message 1</b>", embeddedErrorsWidget.getErrors().get(0).getText());
        Assert.assertEquals("Server error", embeddedErrorsWidget.getErrors().get(0).getGroup());
        Assert.assertEquals("<b>Error message 2</b>", embeddedErrorsWidget.getErrors().get(1).getText());
        Assert.assertEquals("Server error", embeddedErrorsWidget.getErrors().get(1).getGroup());
        Assert.assertFalse(embeddedErrorsWidget.isShowMoreErrorsVisible());
    }
}
