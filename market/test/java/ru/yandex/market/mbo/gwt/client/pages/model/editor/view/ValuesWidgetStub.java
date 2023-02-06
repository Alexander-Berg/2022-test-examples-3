package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import com.google.gwt.user.client.ui.Widget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ParamMeta;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValuesWidget;
import ru.yandex.market.mbo.gwt.utils.CollectionsTools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class ValuesWidgetStub<T> implements ValuesWidget<T> {

    private final List<ValueWidget<T>> valueWidgets = new ArrayList<>();

    private final List<BiConsumer<ValueWidget<T>, Boolean>> widgetAddConsumers = new ArrayList<>();
    private final List<BiConsumer<ValueWidget<T>, Boolean>> widgetRemoveConsumers = new ArrayList<>();
    private final List<Consumer<List<T>>> valuesChangeConsumers = new ArrayList<>();

    private String originalValuesMessage;

    private final ParamMeta paramMeta;
    private final Supplier<ValueWidget<T>> newValueFieldGenerator;
    private boolean readOnly;

    public ValuesWidgetStub(ParamMeta paramMeta, Supplier<ValueWidget<T>> newValueFieldGenerator) {
        this.paramMeta = paramMeta;
        this.newValueFieldGenerator = newValueFieldGenerator;
    }

    @Override
    public ParamMeta getParamMeta() {
        return paramMeta;
    }

    @Override
    public List<ValueWidget<T>> getValueWidgets() {
        return Collections.unmodifiableList(valueWidgets);
    }

    @Override
    public ValueWidget<T> getValueWidget(int i) {
        return valueWidgets.get(i);
    }

    @Override
    public ValueWidget<T> getFirstValueWidget() {
        return valueWidgets.isEmpty() ? null : valueWidgets.get(0);
    }

    @Override
    public ValueWidget<T> createNewValueWidget(T value, boolean fireValuesChangedEvent) {
        return createNewValueWidget(value, false, fireValuesChangedEvent);
    }

    public ValueWidget<T> createNewValueWidget(T value, boolean userAction, boolean fireValuesChangedEvent) {
        ValueWidget<T> result = newValueFieldGenerator.get();
        result.createStructure();
        result.setValue(value, false);
        return addValueWidget(result, userAction, fireValuesChangedEvent);
    }

    private ValueWidget<T> addValueWidget(ValueWidget<T> widget, boolean userAction, boolean fireEvent) {
        valueWidgets.add(widget);
        widgetAddConsumers.forEach(c -> c.accept(widget, userAction));
        if (fireEvent) {
            valuesChangeConsumers.forEach(c -> c.accept(getValues()));
        }
        return widget;
    }

    @Override
    public void createStructure() {

    }

    @Override
    public String getOldValueMessage() {
        return originalValuesMessage;
    }

    @Override
    public void setOriginalValueMessage(String oldValueMessage) {
        originalValuesMessage = oldValueMessage;
    }

    @Override
    public void removeValueWidget(int index, boolean fireValuesChangedEvent) {
        removeValueWidget(index, false, fireValuesChangedEvent);
    }

    public void removeValueWidget(int index, boolean userAction, boolean fireValuesChangedEvent) {
        ValueWidget<T> result = valueWidgets.remove(index);
        widgetRemoveConsumers.forEach(c -> c.accept(result, userAction));
        if (fireValuesChangedEvent) {
            valuesChangeConsumers.forEach(c -> c.accept(getValues()));
        }
    }

    public void clearValueWidgets(boolean fireValuesChangedEvent) {
        List<ValueWidget<T>> removedWidgets = new ArrayList<>(valueWidgets);
        valueWidgets.clear();
        clear();
        widgetRemoveConsumers.forEach(c -> removedWidgets.forEach(w -> c.accept(w, false)));
        if (fireValuesChangedEvent) {
            valuesChangeConsumers.forEach(c -> c.accept(getValues()));
        }
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public void setReadOnly(boolean value) {
        readOnly = value;
    }

    @Override
    public void addValueWidgetAddedConsumer(BiConsumer<ValueWidget<T>, Boolean> valueWidgetAddedConsumer) {
        widgetAddConsumers.add(valueWidgetAddedConsumer);
    }

    @Override
    public void addValueWidgetRemovedConsumer(BiConsumer<ValueWidget<T>, Boolean> valueWidgetRemovedConsumer) {
        widgetRemoveConsumers.add(valueWidgetRemovedConsumer);
    }

    @Override
    public void addValuesChangeConsumer(Consumer<List<T>> valueChangedConsumer) {
        valuesChangeConsumers.add(valueChangedConsumer);
    }


    public void setNewParamOptionButtonVisible(boolean visible) {
    }

    @Override
    public void addNewParamOptionButtonClickHandler(Runnable function) {
    }

    @Override
    public void setValues(List<T> values, boolean fireValuesChangedEvent) {
        if (CollectionsTools.isEmpty(values)) {
            values = Collections.singletonList(null);
        }
        clearValueWidgets(fireValuesChangedEvent);
        values.forEach(this::createNewValueWidget);
        if (fireValuesChangedEvent) {
            valuesChangeConsumers.forEach(c -> c.accept(getValues()));
        }
    }

    @Override
    public List<T> getValues() {
        return valueWidgets.stream()
            .map(ValueWidget::getValue)
            .collect(Collectors.toList());
    }

    public ValueWidget<T> add(T value) {
        ValueWidget<T> valueWidget = createNewValueWidget(value);
        valueWidgets.add(valueWidget);
        return valueWidget;
    }

    public void clear() {
        valueWidgets.clear();
    }

    private ValueWidget<T> createValueWidget() {
        return newValueFieldGenerator.get();
    }

    @Override
    public boolean isEnabled() {
        return valueWidgets.stream().map(ValueWidget::isEnabled).findFirst().orElse(true);
    }

    @Override
    public void setEnabled(boolean enabled) {
        valueWidgets.forEach(vw -> vw.setEnabled(enabled));
    }

    public void createNewValueWidgetUserAction(T value) {
        createNewValueWidget(value, true, false);
    }

    @Override
    public Widget asWidget() {
        return null;
    }
}
