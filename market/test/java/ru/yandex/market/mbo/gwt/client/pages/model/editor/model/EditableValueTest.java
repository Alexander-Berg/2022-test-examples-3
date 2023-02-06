package ru.yandex.market.mbo.gwt.client.pages.model.editor.model;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ValueFieldStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ValueWidgetStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ParamMeta;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueField;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueWidget;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author s-ermakov
 */
public class EditableValueTest extends BaseParameterValuesTest {

    private ValueField<Object> valueField;
    private ValueWidget<String> valueWidget;
    private EditableValue editableValue;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        valueField = new ValueFieldStub<>(new ParamMeta());
        valueWidget = new ValueWidgetStub(valueField);

        editableValue = new EditableValue(valueWidget, valueConverter, categoryParam);
    }

    @Test
    public void testEmptyOrNullParamValueIfValueDontSet() {
        valueWidget.setValue(null, true);

        ParameterValue paramValue = editableValue.getParameterValue();
        Assert.assertNotNull(paramValue);
        Assert.assertTrue(paramValue.isEmpty());
    }

    @Test
    public void testParamValueIfValueSet() {
        valueWidget.setValue("1", true);

        ParameterValue paramValue = editableValue.getParameterValue();
        Assert.assertNotNull(paramValue);
        Assert.assertFalse(paramValue.isEmpty());
        Assert.assertEquals(BigDecimal.ONE, paramValue.getNumericValue());
    }

    @Test
    public void testSettingParamValueWillChangeValue() {
        ParameterValue parameterValue = createParameterValue(categoryParam, 1);
        editableValue.setParameterValue(parameterValue, true);

        Assert.assertEquals("1", valueWidget.getValue());
    }

    @Test
    public void testEventFiringWhenValueChange() {
        ParameterValue parameterValue = createParameterValue(categoryParam, 1);

        AtomicInteger eventNumber = new AtomicInteger();
        editableValue.addOnValueChangedConsumer(change -> {
            eventNumber.incrementAndGet();
        });

        editableValue.setParameterValue(new ParameterValue(parameterValue), true);
        Assert.assertEquals(1, eventNumber.get());

        editableValue.setParameterValue(new ParameterValue(parameterValue), true);
        Assert.assertEquals(1, eventNumber.get());
    }

    @Test
    public void testSetParamValueEqualsGetParamValue() {
        ParameterValue setParamValue = createParameterValue(categoryParam, 1);

        editableValue.setParameterValue(setParamValue, true);

        ParameterValue getParamValue = editableValue.getParameterValue();
        Assert.assertEquals(setParamValue, getParamValue);
    }

    @Test
    public void testShowErrorIfUserTypeIncorrectValue() {
        String incorrectUserInput = "bla-bla-bla";
        Assert.assertEquals(0, editableValue.getErrors().size());

        AtomicInteger eventNumber = new AtomicInteger();
        editableValue.addErrorHandler((editableValue, error) -> {
            eventNumber.incrementAndGet();
        });

        valueField.setValue(incorrectUserInput, true);
        Assert.assertEquals(1, editableValue.getErrors().size());
        Assert.assertEquals(1, eventNumber.get());

        valueField.setValue("0", true);
        Assert.assertEquals(0, editableValue.getErrors().size());
        Assert.assertEquals(2, eventNumber.get());
    }

    @Test
    public void testParameterValueWontLostNonValueAttributes() {
        ParameterValue parameterValue = new ParameterValue(categoryParam, BigDecimal.ONE);
        parameterValue.setLastModificationDate(new Date(1));
        parameterValue.setLastModificationUid(2L);
        parameterValue.setModificationSource(ModificationSource.COMPUTER_VISION);

        editableValue.setParameterValue(parameterValue, true);
        Assert.assertEquals(parameterValue, editableValue.getParameterValue());

        valueField.setValue("0", true);

        ParameterValue changedValue = editableValue.getParameterValue();
        Assert.assertEquals(BigDecimal.ZERO, changedValue.getNumericValue());
        Assert.assertEquals(new Date(1), changedValue.getLastModificationDate());
        Assert.assertEquals(new Long(2L), changedValue.getLastModificationUid());
        Assert.assertEquals(ModificationSource.COMPUTER_VISION, changedValue.getModificationSource());
    }
}
