package ru.yandex.market.mbo.gwt.client.pages.model.editor.test;

import org.junit.Assert;
import org.junit.Before;
import ru.yandex.market.mbo.common.processing.ProcessingResult;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorEventBus;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.ModelEditor;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelDataBuilder;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditableModel;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.ModelData;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.rpc.TestRpc;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ModelEditorViewStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ViewFactoryStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ErrorsWidget;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Date;
import java.util.List;

/**
 * @author gilmulla
 */
@SuppressWarnings("checkstyle:magicNumber")
public abstract class AbstractTest {

    protected TestRpc rpc;
    protected ViewFactoryStub viewFactory = new ViewFactoryStub();

    protected ModelEditorViewStub view;

    protected ModelEditor editor;
    protected EditorEventBus bus;

    protected ModelDataBuilder data;
    protected ModelData modelData;
    protected EditableModel editableModel;

    @Before
    public void commonBefore() {
        rpc = new TestRpc();
        viewFactory = new ViewFactoryStub();
        editor = ModelEditor.createWithoutAddons(rpc, viewFactory, excludedAddons());
        bus = editor.getEventBus();
        view = (ModelEditorViewStub) editor.getView();
        data = ModelDataBuilder.modelData();

        parameters();
        model();
        vendor();
        form();
        run();
    }

    public void parameters() {
        data.startParameters()
            .startParameter()
                .xsl("num1").type(Param.Type.NUMERIC).name("Num1")
            .endParameter()
            .startParameter()
                .xsl("num2").type(Param.Type.NUMERIC).name("Num2")
            .endParameter()
            .startParameter()
                .xsl("num3").type(Param.Type.NUMERIC).name("Num3")
            .endParameter()
            .startParameter()
                .xsl("num4").type(Param.Type.NUMERIC).name("Num4")
            .endParameter()
            .startParameter()
                .xsl("str1").type(Param.Type.STRING).name("Str1")
            .endParameter()
            .startParameter()
                .xsl("str2").type(Param.Type.STRING).name("Str2")
            .endParameter()
            .startParameter()
                .xsl("str3").type(Param.Type.STRING).name("Str3")
            .endParameter()
            .startParameter()
                .xsl("str4").type(Param.Type.STRING).name("Str4")
            .endParameter()
            .startParameter()
                .xsl(XslNames.OPERATOR_SIGN).type(Param.Type.BOOLEAN).name("Подпись оператора")
                .option(1, "TRUE")
                .option(2, "FALSE")
            .endParameter()
            .startParameter()
                .xsl("bool1").type(Param.Type.BOOLEAN).name("Bool1")
                .option(1, "TRUE")
                .option(2, "FALSE")
            .endParameter()
            .startParameter()
                .xsl("bool2").type(Param.Type.BOOLEAN).name("Bool2")
                .option(1, "True")
                .option(2, "False")
            .endParameter()
            .startParameter()
                .xsl("bool3").type(Param.Type.BOOLEAN).name("Bool3")
                .option(1, "TRUE")
                .option(2, "FALSE")
            .endParameter()
            .startParameter()
                .xsl("bool4").type(Param.Type.BOOLEAN).name("Bool4")
                .option(1, "true")
                .option(2, "false")
            .endParameter()
            .startParameter()
                .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
                .option(1, "Vendor1")
                .option(2, "Vendor2")
                .option(3, "Vendor3")
            .endParameter()
            .startParameter()
                .xsl("enum1").type(Param.Type.ENUM).name("Enum1")
                .option(1, "Option1")
                .option(2, "Option2")
                .option(3, "Option3")
            .endParameter()
            .startParameter()
                .xsl("enum2").type(Param.Type.ENUM).name("Enum2")
                .option(1, "Option1")
                .option(2, "Option2")
            .endParameter()
            .startParameter()
                .xsl("enum3").type(Param.Type.ENUM).name("Enum3")
                .option(1, "Option1")
            .endParameter()
            .startParameter()
                .xsl("enum4").type(Param.Type.ENUM).name("Enum4")
            .endParameter()
        .endParameters();
    }

    public void model() {

    }

    public void form() {
        data.startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("num1", "num2")
                .endBlock()
                .startBlock()
                    .name("block")
                    .properties("str1", "str2", XslNames.OPERATOR_SIGN)
                .endBlock()
            .endTab()
            .endForm();
    }

    public void vendor() {
        data.startVendor()
            .source("http://source1", "ru", new Date())
            .source("http://source2", "en", new Date())
            .endVendor()
            .tovarCategory(1, 2);
    }

    public void run() {
    }

    protected Class<?>[] excludedAddons() {
        return new Class[0];
    }

    protected void assertError(String msg) {
        ErrorsWidget errorsWidget = (ErrorsWidget) view.getOperationWidget();
        Assert.assertNotNull(errorsWidget);
        List<ProcessingResult> errors = errorsWidget.getErrors();
        Assert.assertEquals(1, errors.size());
        ProcessingResult error = errors.get(0);
        Assert.assertEquals(error.getText(), msg);
    }

    protected void assertWaitingMessage(String msg) {
        Assert.assertEquals(msg, view.getOperationWaitingText());
    }

    protected void assertWarningMessage(String msg) {
        Assert.assertEquals(msg, view.getOperationWarningText());
    }

    protected void assertSuccessMessage(String msg) {
        Assert.assertEquals(msg, view.getOperationSuccessText());
    }
}
