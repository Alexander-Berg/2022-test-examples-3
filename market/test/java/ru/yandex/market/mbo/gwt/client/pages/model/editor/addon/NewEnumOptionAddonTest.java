package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.sku.BaseSkuAddonTest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.ParamOptionCreateEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.ParamOptionCreateOpenDialogEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditableParameter;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditableValues;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.NewParamOptionDialogWidgetStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ValueFieldStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueField;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueWidget;
import ru.yandex.market.mbo.gwt.models.params.Option;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("checkstyle:MagicNumber")
public class NewEnumOptionAddonTest extends BaseSkuAddonTest {

    @Test
    public void testNewEnumOptionGoesToPopup() {
        //Проверим, что при создании нового ENUM значения на табе SKU значение ошибочного мультиполя попадёт в попап
        NewParamOptionDialogWidgetStub popup = NewParamOptionDialogWidgetStub.getInstance();
        EditableValues editableValues = editableModel.getEditableParameters().stream()
            .filter(p -> p.getParamMeta().getParamId() == 2L)
            .map(EditableParameter::getEditableValues)
            .findFirst().get();
        ValueFieldStub inputWidget = (ValueFieldStub) editableValues.getEditableValue(editableValues.size() - 1)
            .getValueWidget().getValueField();

        //Пустое поле
        bus.fireEvent(new ParamOptionCreateOpenDialogEvent(editableValues));
        assertEquals("", popup.getStringValue());

        //Пользовательский текст
        inputWidget.setValueUserInput("расторопши семена");
        bus.fireEvent(new ParamOptionCreateOpenDialogEvent(editableValues));
        assertEquals("расторопши семена", popup.getStringValue());
    }

    @Test
    public void testAddNewParamOption() throws Exception {
        EditableValues editableValues = editableModel.getEditableParameters().stream()
            .filter(p -> p.getParamMeta().getParamId() == 2L)
            .map(EditableParameter::getEditableValues)
            .findFirst().get();
        ValueWidget valueWidget = editableValues.getEditableValue(0).getValueWidget();
        assertEquals(3, editableValues.getAllOptions().size());

        rpc.setCreateOptionRelation(true, 7, null);
        userAddsNewOption(editableValues, valueWidget.getValueField(), "test enum value 1");
        assertEquals(4, editableValues.getAllOptions().size());
        assertEquals("", valueWidget.getErrorMessage());
        assertEquals(7, ((Option) valueWidget.getValue()).getId());

        rpc.setCreateOptionRelation(false, 8, null);
        userAddsNewOption(editableValues, valueWidget.getValueField(), "test enum value 2");
        assertEquals(4, editableValues.getAllOptions().size());
        assertTrue(valueWidget.getErrorMessage().startsWith("Не удалось добавить новое значение: test enum value 2"));
    }

    @Test
    public void testAddNewParamOptionToMultivalueField() {
        EditableParameter editableParameter = editableModel.getEditableParameters().stream()
            .filter(p -> p.getParamMeta().getXslName().equals("param4"))
            .findFirst().get();
        EditableValues editableValues = editableParameter.getEditableValues();
        ValueWidget valueWidget = editableValues.getEditableValue(0).getValueWidget();
        assertEquals(1, editableValues.size());
        assertEquals(6, editableValues.getAllOptions().size());

        rpc.setCreateOptionRelation(true, 7, null);
        userAddsNewOption(editableValues, valueWidget.getValueField(), "test enum value 1");
        assertEquals(1, editableValues.size());
        editableValues.getOrCreateEditableValue(1);
        assertEquals(2, editableValues.size());
        assertEquals(7, editableValues.getAllOptions().size());

        ValueWidget valueWidget1 = editableValues.getEditableValue(0).getValueWidget();
        assertEquals("", valueWidget1.getErrorMessage());
        assertEquals(7, ((Option) valueWidget1.getValue()).getId());

        ValueWidget valueWidget2 = editableValues.getEditableValue(1).getValueWidget();
        rpc.setCreateOptionRelation(false, 8, null);
        userAddsNewOption(editableValues, valueWidget2.getValueField(), "test enum value 2");
        assertEquals(7, editableValues.getAllOptions().size());
        assertTrue(valueWidget2.getErrorMessage().startsWith("Не удалось добавить новое значение: test enum value 2"));
        //так как в виджете Т=Object, убедимся, что это либо null, либо не опция
        assertTrue(valueWidget2.getValue() == null || !(valueWidget2.getValue() instanceof Option));
    }

    private void userAddsNewOption(EditableValues editableValues, ValueField textbox, String userInput) {
        ((ValueFieldStub) textbox).setValueUserInput(userInput);
        bus.fireEvent(new ParamOptionCreateOpenDialogEvent(editableValues));
        bus.fireEvent(new ParamOptionCreateEvent(NewParamOptionDialogWidgetStub.getInstance().getStringValue(),
            editableValues.getCategoryParam().getId(), editableValues));
    }

}
