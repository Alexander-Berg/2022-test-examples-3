package ru.yandex.market.mbo.gwt.client.pages.model.editor.model;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ValueFieldStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ValueWidgetStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ValuesWidgetStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ParamMeta;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValuesWidget;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class EditableValuesTest extends BaseParameterValuesTest {
    private ValuesWidget<String> valuesWidget;
    private EditableValues editableValues;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        valuesWidget = new ValuesWidgetStub<>(paramMeta,
            () -> new ValueWidgetStub<>(new ValueFieldStub<>(new ParamMeta())));

        editableValues = new EditableValues(valuesWidget, valueConverter, categoryParam);
    }

    @Test
    public void testNullOrEmptyParameterValuesIfValueIsNull() {
        valuesWidget.setValues(null);

        ParameterValues parameterValues = editableValues.getNotEmptyParameterValues();
        Assert.assertNotNull(parameterValues);
        Assert.assertTrue(parameterValues.isEmpty());
    }

    @Test
    public void testNullOrEmptyParameterValuesIfValueIsEmptyList() {
        valuesWidget.setValues(Collections.emptyList());

        ParameterValues parameterValues = editableValues.getNotEmptyParameterValues();
        Assert.assertNotNull(parameterValues);
        Assert.assertTrue(parameterValues.isEmpty());
    }

    @Test
    public void testParameterValuesIfValueSet() {
        valuesWidget.setValues(Collections.emptyList());
        ParameterValues parameterValues1 = editableValues.getNotEmptyParameterValues();
        Assert.assertEquals(1, editableValues.getEditableValues().size());
        assertParameterValues(parameterValues1);

        valuesWidget.setValues(Arrays.asList("1"));
        ParameterValues parameterValues2 = editableValues.getNotEmptyParameterValues();
        Assert.assertEquals(1, editableValues.getEditableValues().size());
        assertParameterValues(parameterValues2, 1);

        valuesWidget.setValues(Arrays.asList("1", "0"));
        ParameterValues parameterValues3 = editableValues.getNotEmptyParameterValues();
        Assert.assertEquals(2, editableValues.getEditableValues().size());
        assertParameterValues(parameterValues3, 1, 0);

        valuesWidget.setValues(Arrays.asList("0", null, "1"));
        ParameterValues parameterValues4 = editableValues.getNotEmptyParameterValues();
        Assert.assertEquals(3, editableValues.getEditableValues().size());
        assertParameterValues(parameterValues4, 0, 1);

        valuesWidget.setValues(Arrays.asList(null, null, "0"));
        ParameterValues parameterValues5 = editableValues.getNotEmptyParameterValues();
        Assert.assertEquals(3, editableValues.getEditableValues().size());
        assertParameterValues(parameterValues5, 0);

        valuesWidget.setValues(Arrays.asList(null, null, null));
        ParameterValues parameterValues6 = editableValues.getNotEmptyParameterValues();
        Assert.assertEquals(3, editableValues.getEditableValues().size());
        assertParameterValues(parameterValues6);
    }

    @Test
    public void testEqualSetAndGetParameterValues() {
        ParameterValues parameterValues1 = createParameterValues(categoryParam);
        editableValues.setParameterValues(parameterValues1, false);
        assertParameterValues(editableValues.getParameterValues());
        assertDifferentObjects(editableValues.getParameterValues(), parameterValues1);

        ParameterValues parameterValues2 = createParameterValues(categoryParam, 1);
        editableValues.setParameterValues(parameterValues2, false);
        assertParameterValues(editableValues.getParameterValues(), 1);
        assertDifferentObjects(editableValues.getParameterValues(), parameterValues2);

        ParameterValues parameterValues3 = createParameterValues(categoryParam, 1, 0, 1);
        editableValues.setParameterValues(parameterValues3, false);
        assertParameterValues(editableValues.getParameterValues(), 1, 0, 1);
        assertDifferentObjects(editableValues.getParameterValues(), parameterValues3);
    }

    @Test
    public void testValueHandlers() {
        valuesWidget.setValues(Arrays.asList("0", null, "0"));

        editableValues.addOnValuesChangedConsumer((parameterValues, userAction) -> {
            assertParameterValues(parameterValues, 0, 1, 0);
        });
        editableValues.addOnValueChangedConsumer(change -> {
            assertParameterValue(change.getEditableValue().getParameterValue(), 1);
        });

        EditableValue editableValue = editableValues.getEditableValue(1);
        editableValue.getValueWidget().setValue(true, true);
    }

    @Test
    public void testUserRequestedNewValue() {
        Assert.assertEquals(0, editableValues.getEditableValues().size());

        valuesWidget.createNewValueWidget(null);
        Assert.assertEquals(1, editableValues.getEditableValues().size());
    }

    @Test
    public void testUserDeleteSomeValue() {
        valuesWidget.setValues(Arrays.asList(null, null));
        Assert.assertEquals(2, editableValues.getEditableValues().size());

        valuesWidget.removeValueWidget(0);
        Assert.assertEquals(1, editableValues.getEditableValues().size());
    }

    @Test
    public void testShowErrorIfUserTypeIncorrectValue() {
        editableValues.setParameterValues(createParameterValues(categoryParam, 1, 1), false);
        ValueWidget<Object> valueWidget1 = editableValues.getEditableValue(0).getValueWidget();
        ValueWidget<Object> valueWidget2 = editableValues.getEditableValue(1).getValueWidget();

        String incorrectUserInput = "bla-bla-bla";
        Assert.assertEquals(0, editableValues.getFlatErrors().size());

        AtomicInteger eventNumber = new AtomicInteger();
        editableValues.addErrorHandler((value, error) -> {
            eventNumber.incrementAndGet();
        });

        valueWidget1.setValue(incorrectUserInput, true);
        Assert.assertEquals(1, editableValues.getFlatErrors().size());
        Assert.assertEquals(1, eventNumber.get());

        valueWidget2.setValue(incorrectUserInput, true);
        Assert.assertEquals(2, editableValues.getFlatErrors().size());
        Assert.assertEquals(2, eventNumber.get());

        valueWidget1.setValue("0", true);
        Assert.assertEquals(1, editableValues.getFlatErrors().size());
        Assert.assertEquals(3, eventNumber.get());

        valueWidget2.setValue("0", true);
        Assert.assertEquals(0, editableValues.getFlatErrors().size());
        Assert.assertEquals(4, eventNumber.get());
    }

    @Test
    public void testErrorsWillDissapearIfNewParamValuesWasSet() {
        editableValues.setParameterValues(createParameterValues(categoryParam, 1, 1), false);
        ValueWidget<Object> valueWidget1 = editableValues.getEditableValue(0).getValueWidget();
        ValueWidget<Object> valueWidget2 = editableValues.getEditableValue(1).getValueWidget();

        String incorrectUserInput = "bla-bla-bla";
        Assert.assertEquals(0, editableValues.getFlatErrors().size());

        AtomicInteger eventNumber = new AtomicInteger();
        editableValues.addErrorHandler((value, errorChanges) -> {
            eventNumber.incrementAndGet();
        });

        valueWidget1.setValue(incorrectUserInput, true);
        valueWidget2.setValue(incorrectUserInput, true);
        Assert.assertEquals(2, editableValues.getFlatErrors().size());
        Assert.assertEquals(2, eventNumber.get());

        editableValues.setParameterValues(createParameterValues(categoryParam, 1, 1), false);

        Assert.assertEquals(0, editableValues.getFlatErrors().size());
        Assert.assertEquals(4, eventNumber.get());
    }

    @Test
    public void testErrorsWillDissapearIfUserRemoveOneEditableValue() {
        editableValues.setParameterValues(createParameterValues(categoryParam, 1, 1), false);
        ValueWidget<Object> valueWidget1 = editableValues.getEditableValue(0).getValueWidget();
        ValueWidget<Object> valueWidget2 = editableValues.getEditableValue(1).getValueWidget();

        String incorrectUserInput = "bla-bla-bla";
        Assert.assertEquals(0, editableValues.getFlatErrors().size());

        AtomicInteger eventNumber = new AtomicInteger();
        editableValues.addErrorHandler((value, error) -> {
            eventNumber.incrementAndGet();
        });

        valueWidget1.setValue(incorrectUserInput, true);
        valueWidget2.setValue(incorrectUserInput, true);
        Assert.assertEquals(2, editableValues.getFlatErrors().size());
        Assert.assertEquals(2, eventNumber.get());

        editableValues.getValuesWidget().removeValueWidget(1);

        Assert.assertEquals(1, editableValues.getFlatErrors().size());
        Assert.assertEquals(3, eventNumber.get());
    }

    @Test
    public void testSmartSetParameterValuesWillGenerateOnlySingleEvent() {
        AtomicInteger valueChangesCounter = new AtomicInteger(0);
        AtomicInteger valuesChangesCounter = new AtomicInteger(0);

        editableValues.addOnValueChangedConsumer(change -> {
            valueChangesCounter.incrementAndGet();
        });
        editableValues.addOnValuesChangedConsumer((values, userAction) -> {
            valuesChangesCounter.incrementAndGet();
        });

        ParameterValues parameterValues = new ParameterValues(categoryParam,
            BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ONE);
        editableValues.smartSetParameterValues(parameterValues, true);

        Assertions.assertThat(valueChangesCounter.get()).isEqualTo(3);
        Assertions.assertThat(valuesChangesCounter.get()).isEqualTo(1);
    }

    @Test
    public void testSmartSetParameterValuesForNotEmptyWidgetWillGenerateOnlySingleEvent() {
        ParameterValues initialParameterValues = new ParameterValues(categoryParam,
            BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN);
        editableValues.setParameterValues(initialParameterValues, false);

        AtomicInteger valueChangesCounter = new AtomicInteger(0);
        AtomicInteger valuesChangesCounter = new AtomicInteger(0);
        editableValues.addOnValueChangedConsumer(change -> {
            valueChangesCounter.incrementAndGet();
        });
        editableValues.addOnValuesChangedConsumer((values, userAction) -> {
            valuesChangesCounter.incrementAndGet();
        });

        ParameterValues parameterValues = new ParameterValues(categoryParam,
            BigDecimal.ONE, null, BigDecimal.ONE);
        editableValues.smartSetParameterValues(parameterValues, true);

        Assertions.assertThat(valueChangesCounter.get()).isEqualTo(3);
        Assertions.assertThat(valuesChangesCounter.get()).isEqualTo(1);
    }
}
