package ru.yandex.market.mbo.gwt.client.pages.model.editor.model;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.converter.EnumConverter;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.converter.ValueConverter;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.TestModelEditing;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ValueFieldStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ValueWidgetStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ValuesWidgetStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ViewFactoryStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueWidget;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class EditableValuesAllowOptionsTest extends BaseParameterValuesTest {

    private ValuesWidgetStub<Option> valuesWidget;
    private ValueConverter<Option> valueConverter;
    private EditableValues editableValues;
    private List<Option> options;
    private Option option1;
    private Option option2;
    private Option option3;

    @Before
    public void setUp() throws Exception {
        categoryParam = CategoryParamBuilder.newBuilder(1, "test")
            .setType(Param.Type.ENUM)
            .addOption(new OptionImpl(1, "test1"))
            .addOption(new OptionImpl(2, "test2"))
            .addOption(new OptionImpl(3, "test3"))
            .build();
        options = categoryParam.getOptions();
        option1 = options.get(0);
        option2 = options.get(1);
        option3 = options.get(2);
        model = CommonModelBuilder.newBuilder(1, 1, 1).getModel();
        paramMeta = new ViewFactoryStub().createParamMeta(categoryParam, model);

        valueConverter = new EnumConverter();
        valuesWidget = new ValuesWidgetStub<>(paramMeta,
            () -> new ValueWidgetStub<>(new ValueFieldStub<>(paramMeta)));

        editableValues = new EditableValues(valuesWidget, valueConverter, categoryParam);
    }

    @Test
    public void testAllowedOptionsWillBeFilteredForExistingValues() {
        ParameterValues parameterValues = new ParameterValues(paramMeta.getParamId(), paramMeta.getXslName(),
            paramMeta.getType(), 1L, 2L);

        editableValues.initializeAllOptions(options);
        editableValues.setParameterValues(parameterValues, false);

        EditableValue editableValue1 = editableValues.getEditableValue(0);
        Assertions.assertThat(editableValue1.getOptions()).containsExactly(options.get(0), options.get(2));

        EditableValue editableValue2 = editableValues.getEditableValue(1);
        Assertions.assertThat(editableValue2.getOptions()).containsExactly(options.get(1), options.get(2));
    }

    @Test
    public void testAllowedOptionsWillBeFilteredOutInNewEditableValue() {
        ParameterValues parameterValues = new ParameterValues(paramMeta.getParamId(), paramMeta.getXslName(),
            paramMeta.getType(), 1L);

        editableValues.initializeAllOptions(options);
        editableValues.setParameterValues(parameterValues, false);

        valuesWidget.createNewValueWidgetUserAction(null);

        EditableValue editableValue = editableValues.getEditableValue(1);
        Assertions.assertThat(editableValue.getOptions()).containsExactly(options.get(1), options.get(2));
    }

    @Test
    public void testAllowedOptionsWillBeRefreshedAfterDeletingOneValue() {
        ParameterValues parameterValues = new ParameterValues(paramMeta.getParamId(), paramMeta.getXslName(),
            paramMeta.getType(), 1L, 2L);

        editableValues.initializeAllOptions(options);
        editableValues.setParameterValues(parameterValues, false);

        // создаем пустое поле для ввода
        valuesWidget.createNewValueWidgetUserAction(null);

        // проверяем, что у него только 1 доступный выбор
        EditableValue editableValue = editableValues.getEditableValue(2);
        Assertions.assertThat(editableValue.getOptions()).containsExactly(options.get(2));

        // удалям второй виджет
        valuesWidget.removeValueWidget(1, true, false);

        // убеждаемся, что список сократился после удаления виджета
        Assertions.assertThat(editableValue.getOptions()).containsExactly(options.get(1), options.get(2));
    }

    @Test
    public void testAllowedOptionsWillChangedIfOneValueChanged() {
        ParameterValues parameterValues = new ParameterValues(paramMeta.getParamId(), paramMeta.getXslName(),
            paramMeta.getType(), 1L, 2L);

        editableValues.initializeAllOptions(options);
        editableValues.setParameterValues(parameterValues, false);

        // создаем пустое поле для ввода
        valuesWidget.createNewValueWidgetUserAction(null);

        EditableValue value1 = editableValues.getEditableValue(0);
        EditableValue value2 = editableValues.getEditableValue(1);
        EditableValue value3 = editableValues.getEditableValue(2);

        Assertions.assertThat(value1.getOptions()).containsExactly(options.get(0), options.get(2));
        Assertions.assertThat(value2.getOptions()).containsExactly(options.get(1), options.get(2));
        Assertions.assertThat(value3.getOptions()).containsExactly(options.get(2));

        // меняем второе поле
        value2.getValueWidget().setValue(options.get(2), true);

        Assertions.assertThat(value1.getOptions()).containsExactly(options.get(0), options.get(1));
        Assertions.assertThat(value2.getOptions()).containsExactly(options.get(1), options.get(2));
        Assertions.assertThat(value3.getOptions()).containsExactly(options.get(1));
    }

    /**
     * Логика движка редактора моделей так устроена, что в модели могут содержаться значения,
     * которые не удовлетворяют требованиям параметров.
     * Например, coping-model-source.
     * <p>
     * Согласно настройкам параметра, значения не могут быть больше 1,
     * но на практике это системный параметр, который нужен для разработки
     * и значения его всегда больше 1 (id модели).
     * Чтобы редактор неявным образом не портил данные, добавили костыль,
     * чтобы НЕ ИЗМЕНЯЕМЫЕ пользователем данные сохранились ровно такие, какими они были установлены автоматически.
     * <p>
     * Тест, который проверяет, что неверное значение, также портит {@link ParameterValues}:
     * {@link #testFailedSetParameterValueIfNotContainsInAllowedOptionsByUserAction}.
     * <p>
     * Тесты, которые еще раз воспроизводят текущую ситуцию:
     * {@link TestModelEditing#testCorruptedParameterValuesWontAffectCommonModelBeforeSave}
     * {@link TestModelEditing#testNotShownParameterValueWontChangeDuringModelEditorPipeline}.
     */
    @Test
    public void testCorrectSetParameterValueIfItNotContainsInAllowedOptions() {
        EditableValue editableValue = editableValues.getOrCreateEditableValue(0);
        List<Option> allowedOptions = categoryParam.getOptions().subList(0, 1);

        ParameterValue parameterValue = new ParameterValue(categoryParam, 2L);

        editableValue.setOptions(allowedOptions);
        editableValue.setParameterValue(parameterValue, true);
        Assertions.assertThat(parameterValue.isEmpty()).isFalse();
        Assertions.assertThat(parameterValue.getOptionId()).isEqualTo(2L);

        ParameterValue value = editableValue.getParameterValue();
        Assertions.assertThat(value.isEmpty()).isFalse();
        Assertions.assertThat(value.getOptionId()).isEqualTo(2L);
    }

    @Test
    public void testFailedSetParameterValueIfNotContainsInAllowedOptionsByUserAction() {
        EditableValue editableValue = editableValues.getOrCreateEditableValue(0);
        List<Option> allowedOptions = categoryParam.getOptions().subList(0, 1);

        editableValue.setOptions(allowedOptions);
        editableValue.getValueWidget().setValue(option3, true);
        Assertions.assertThat(editableValue.getParameterValue().isEmpty()).isTrue();

        ParameterValue value = editableValue.getParameterValue();
        Assertions.assertThat(value.isEmpty()).isTrue();
    }

    @Test
    public void testCorrectSetParameterValuesIfSingleOptionsConflictsWithNewParameterValues() {
        // тест проверяет ситуацию, когда пытаемся выставить значения, на те места
        // на которые поставить в следствие ограничений вручную нельзя

        // ограничения в данном случае - это ограничения выставленные EditableValues,
        // чтобы пользователь не мог выбрать значения, выбранные в соседних контролах
        ParameterValues beforeParameterValues = new ParameterValues(categoryParam, 1L, 2L, 3L);
        editableValues.initializeAllOptions(options);
        editableValues.smartSetParameterValues(beforeParameterValues, false);

        ValueWidget<Object> valueWidget1 = editableValues.getValuesWidget().getValueWidget(0);
        ValueWidget<Object> valueWidget2 = editableValues.getValuesWidget().getValueWidget(1);
        ValueWidget<Object> valueWidget3 = editableValues.getValuesWidget().getValueWidget(2);
        Assertions.assertThat(valueWidget1.getValueDomain()).containsExactlyInAnyOrder(option1);
        Assertions.assertThat(valueWidget2.getValueDomain()).containsExactlyInAnyOrder(option2);
        Assertions.assertThat(valueWidget3.getValueDomain()).containsExactlyInAnyOrder(option3);

        // Выставляем те же значения, только в изменнном порядке
        ParameterValues newParameterValues = new ParameterValues(categoryParam, 2L, 3L, 1L);
        editableValues.smartSetParameterValues(newParameterValues, false);

        Assertions.assertThat(valueWidget1.getValueDomain()).containsExactlyInAnyOrder(option2);
        Assertions.assertThat(valueWidget2.getValueDomain()).containsExactlyInAnyOrder(option3);
        Assertions.assertThat(valueWidget3.getValueDomain()).containsExactlyInAnyOrder(option1);

        // проверяем, что значения выставились верные
        Assertions.assertThat(((Option) valueWidget1.getValue()).getId()).isEqualTo(2L);
        Assertions.assertThat(((Option) valueWidget2.getValue()).getId()).isEqualTo(3L);
        Assertions.assertThat(((Option) valueWidget3.getValue()).getId()).isEqualTo(1L);
    }

    @Test
    public void testCorrectSetParameterValuesIfSingleOptionsConflictsWithNewParameterValuesAndAllowedOptions() {
        // тест аналогичен предыдущему случаю, только помимо ограничений EditableValues
        // добавляются ограничения allowedOptions
        ParameterValues beforeParameterValues = new ParameterValues(categoryParam, 1L, 2L, 3L);
        editableValues.initializeAllOptions(options);
        editableValues.setAllowedOptionIds(Arrays.asList(1L, 2L));
        editableValues.smartSetParameterValues(beforeParameterValues, false);

        ValueWidget<Object> valueWidget1 = editableValues.getValuesWidget().getValueWidget(0);
        ValueWidget<Object> valueWidget2 = editableValues.getValuesWidget().getValueWidget(1);
        ValueWidget<Object> valueWidget3 = editableValues.getValuesWidget().getValueWidget(2);

        Assertions.assertThat(((Option) valueWidget1.getValue()).getId()).isEqualTo(1L);
        Assertions.assertThat(((Option) valueWidget2.getValue()).getId()).isEqualTo(2L);
        Assertions.assertThat(((Option) valueWidget3.getValue())).isNull();
        Assertions.assertThat(valueWidget1.getValueDomain()).containsExactlyInAnyOrder(option1);
        Assertions.assertThat(valueWidget2.getValueDomain()).containsExactlyInAnyOrder(option2);
        Assertions.assertThat(valueWidget3.getValueDomain()).isEmpty();

        // Выставляем те же значения, только в изменнном порядке
        ParameterValues newParameterValues = new ParameterValues(categoryParam, 2L, 3L, 1L);
        editableValues.smartSetParameterValues(newParameterValues, false);

        Assertions.assertThat(((Option) valueWidget1.getValue()).getId()).isEqualTo(2L);
        Assertions.assertThat(((Option) valueWidget2.getValue())).isNull();
        Assertions.assertThat(((Option) valueWidget3.getValue()).getId()).isEqualTo(1L);
        Assertions.assertThat(valueWidget1.getValueDomain()).containsExactlyInAnyOrder(option2);
        Assertions.assertThat(valueWidget2.getValueDomain()).isEmpty();
        Assertions.assertThat(valueWidget3.getValueDomain()).containsExactlyInAnyOrder(option1);
    }

    @Test
    public void testAllowedOptionsSetCorrectlyChangesValueDomain() {
        ParameterValues beforeParameterValues = new ParameterValues(categoryParam);
        beforeParameterValues.addValue(new ParameterValue(categoryParam));
        beforeParameterValues.addValue(new ParameterValue(categoryParam));
        beforeParameterValues.addValue(new ParameterValue(categoryParam));
        editableValues.initializeAllOptions(options);
        editableValues.setAllowedOptionIds(Arrays.asList(1L, 2L));
        editableValues.smartSetParameterValues(beforeParameterValues, false);

        EditableValue editableValue1 = editableValues.getEditableValue(0);
        EditableValue editableValue2 = editableValues.getEditableValue(1);
        EditableValue editableValue3 = editableValues.getEditableValue(2);

        Assertions.assertThat(editableValue1.getOptions()).containsExactlyInAnyOrder(option1, option2);
        Assertions.assertThat(editableValue2.getOptions()).containsExactlyInAnyOrder(option1, option2);
        Assertions.assertThat(editableValue3.getOptions()).containsExactlyInAnyOrder(option1, option2);

        // задаем значения, который не противоречат ограничениям и смотрим, что набор опций поменялся
        ((ValueFieldStub<Object>) editableValue1.getValueWidget().getValueField()).setValueUserInput(option1);
        Assertions.assertThat(editableValue1.getOptions()).containsExactlyInAnyOrder(option1, option2);
        Assertions.assertThat(editableValue2.getOptions()).containsExactlyInAnyOrder(option2);
        Assertions.assertThat(editableValue3.getOptions()).containsExactlyInAnyOrder(option2);
    }

    @Test
    public void testDisableOptionsSetCorrectlyChangesValueDomain() {
        ParameterValues beforeParameterValues = new ParameterValues(categoryParam);
        beforeParameterValues.addValue(new ParameterValue(categoryParam));
        beforeParameterValues.addValue(new ParameterValue(categoryParam));
        beforeParameterValues.addValue(new ParameterValue(categoryParam));
        editableValues.initializeAllOptions(options);
        editableValues.setDisabledOptionIds(Collections.singletonList(3L));
        editableValues.smartSetParameterValues(beforeParameterValues, false);

        EditableValue editableValue1 = editableValues.getEditableValue(0);
        EditableValue editableValue2 = editableValues.getEditableValue(1);
        EditableValue editableValue3 = editableValues.getEditableValue(2);

        Assertions.assertThat(editableValue1.getOptions()).containsExactlyInAnyOrder(option1, option2);
        Assertions.assertThat(editableValue2.getOptions()).containsExactlyInAnyOrder(option1, option2);
        Assertions.assertThat(editableValue3.getOptions()).containsExactlyInAnyOrder(option1, option2);

        // задаем значения, который не противоречат ограничениям и смотрим, что набор опций поменялся
        ((ValueFieldStub<Object>) editableValue1.getValueWidget().getValueField()).setValueUserInput(option1);
        Assertions.assertThat(editableValue1.getOptions()).containsExactlyInAnyOrder(option1, option2);
        Assertions.assertThat(editableValue2.getOptions()).containsExactlyInAnyOrder(option2);
        Assertions.assertThat(editableValue3.getOptions()).containsExactlyInAnyOrder(option2);
    }

    @Test
    public void testAllowedAndDisableOptionsSetCorrectlyChangesValueDomain() {
        ParameterValues beforeParameterValues = new ParameterValues(categoryParam);
        beforeParameterValues.addValue(new ParameterValue(categoryParam));
        beforeParameterValues.addValue(new ParameterValue(categoryParam));
        beforeParameterValues.addValue(new ParameterValue(categoryParam));
        editableValues.initializeAllOptions(options);
        editableValues.setAllowedOptionIds(Arrays.asList(1L, 2L));
        editableValues.setDisabledOptionIds(Arrays.asList(2L));
        editableValues.smartSetParameterValues(beforeParameterValues, false);

        EditableValue editableValue1 = editableValues.getEditableValue(0);
        EditableValue editableValue2 = editableValues.getEditableValue(1);
        EditableValue editableValue3 = editableValues.getEditableValue(2);

        Assertions.assertThat(editableValue1.getOptions()).containsExactlyInAnyOrder(option1);
        Assertions.assertThat(editableValue2.getOptions()).containsExactlyInAnyOrder(option1);
        Assertions.assertThat(editableValue3.getOptions()).containsExactlyInAnyOrder(option1);

        // задаем значения, который не противоречат ограничениям и смотрим, что набор опций поменялся
        ((ValueFieldStub<Object>) editableValue1.getValueWidget().getValueField()).setValueUserInput(option1);
        Assertions.assertThat(editableValue1.getOptions()).containsExactlyInAnyOrder(option1);
        Assertions.assertThat(editableValue2.getOptions()).containsExactlyInAnyOrder();
        Assertions.assertThat(editableValue3.getOptions()).containsExactlyInAnyOrder();
    }

    @Test
    public void testOptionInitializationCallsRefreshOnlyOnce() {
        List<ValueWidget<Object>> widgets = editableValues.getValuesWidget().getValueWidgets();
        Assertions.assertThat(widgets).isEmpty();

        ParameterValues beforeParameterValues = new ParameterValues(categoryParam);
        beforeParameterValues.addValue(new ParameterValue(categoryParam));
        beforeParameterValues.addValue(new ParameterValue(categoryParam));
        beforeParameterValues.addValue(new ParameterValue(categoryParam));
        editableValues.smartSetParameterValues(beforeParameterValues, false);
        Assert.assertEquals(3, widgets.size());
        widgets.forEach(w -> { //no options set, nothing to refresh
            ValueFieldStub field = (ValueFieldStub) w.getValueField();
            Assert.assertEquals(0, field.getValueDomainCallsCount());
        });

        editableValues.initializeAllOptions(options);
        widgets.forEach(w -> { //refresh hasn't been explicitly called, still 0 refreshes
            ValueFieldStub field = (ValueFieldStub) w.getValueField();
            Assert.assertEquals(0, field.getValueDomainCallsCount());
        });
        editableValues.refreshOptions();
        widgets.forEach(w -> {
            ValueFieldStub field = (ValueFieldStub) w.getValueField();
            Assert.assertEquals(1, field.getValueDomainCallsCount());
        });

        beforeParameterValues = new ParameterValues(categoryParam);
        beforeParameterValues.addValue(new ParameterValue(categoryParam));
        beforeParameterValues.addValue(new ParameterValue(categoryParam));
        beforeParameterValues.addValue(new ParameterValue(categoryParam));
        editableValues.smartSetParameterValues(beforeParameterValues, false);
        widgets.forEach(w -> { //value setter calls 'setOptions' for its fields 2 times:
            //first to turn off the restrictions
            //second time to turn them back on again after applying changes (real refresh).
            ValueFieldStub field = (ValueFieldStub) w.getValueField();
            Assert.assertEquals(3, field.getValueDomainCallsCount());
        });
    }
}
