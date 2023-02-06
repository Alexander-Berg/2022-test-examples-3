package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import com.google.gwt.user.client.ui.Focusable;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ParamMeta;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueField;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueWidget;
import ru.yandex.market.mbo.gwt.models.params.PickerImage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author s-ermakov
 */
public class ValueWidgetStub<T> extends EditorWidgetStub implements ValueWidget<T> {
    private final ValueField<T> valueField;
    private PickerImage pickerImage;
    private boolean enumValueAliasesButtonVisible;
    private String errorMessage = "";
    private Style style;

    private final List<Consumer<Boolean>> confirmCheckboxValueChangeHandlers = new ArrayList<>();
    private final List<Runnable> pickerClickHandlers = new ArrayList<>();
    private final List<Runnable> enumValueAliasesClickHandlers = new ArrayList<>();
    private BackgroundColor bgColor;
    private boolean confirmCheckboxVisible;
    private boolean confirmCheckboxValue;

    public ValueWidgetStub(ValueField<T> valueField) {
        this.valueField = valueField;
    }

    @Override
    public boolean isPickerImageButtonVisible() {
        return false;
    }

    @Override
    public void setPickerImageButtonVisible(boolean visible) {

    }

    @Override
    public PickerImage getPickerImage() {
        return pickerImage;
    }

    @Override
    public void setPickerImage(PickerImage pickerImage) {
        this.pickerImage = pickerImage;
    }

    @Override
    public void addPickerImageButtonClickHandler(Runnable pickerClickHandler) {
        this.pickerClickHandlers.add(pickerClickHandler);
    }

    @Override
    public void removePickerImageButtonClickHandler(Runnable pickerClickHandler) {
        this.pickerClickHandlers.remove(pickerClickHandler);
    }

    @Override
    public boolean isEnumValueAliasesButtonVisible() {
        return enumValueAliasesButtonVisible;
    }

    @Override
    public void setEnumValueAliasesButtonVisible(boolean visible) {
        enumValueAliasesButtonVisible = visible;
    }

    @Override
    public void addEnumValueAliasesButtonClickHandler(Runnable enumValueAliasesIconClick) {
        enumValueAliasesClickHandlers.add(enumValueAliasesIconClick);
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void setErrorMessage(String message) {
        errorMessage = message;
    }

    @Override
    public void clearErrorMessage() {
        errorMessage = "";
    }

    @Override
    public Style getErrorStyle() {
        return style;
    }

    @Override
    public void setErrorStyle(Style style) {
        this.style = style;
    }

    @Override
    public ValueField<T> getValueField() {
        return valueField;
    }

    @Override
    public BackgroundColor getBackgroundColor() {
        return bgColor;
    }

    @Override
    public void setBackgroundColor(BackgroundColor source) {
        bgColor = source;
    }

    @Override
    public boolean isConfirmCheckboxVisible() {
        return confirmCheckboxVisible;
    }

    @Override
    public void setConfirmCheckboxVisible(boolean visible) {
        confirmCheckboxVisible = visible;
    }

    @Override
    public boolean getConfirmCheckboxStatus() {
        return confirmCheckboxValue;
    }

    @Override
    public void setConfirmCheckboxStatus(boolean status) {
        confirmCheckboxValue = status;
        this.confirmCheckboxValueChangeHandlers.forEach(h -> h.accept(status));
    }

    @Override
    public void addConfirmCheckboxValueChangeHandler(Consumer<Boolean> confirmCheckboxValueChangeHandler) {
        this.confirmCheckboxValueChangeHandlers.add(confirmCheckboxValueChangeHandler);
    }

    @Override
    public void removeConfirmCheckboxValueChangeHandler(Consumer<Boolean> confirmCheckboxValueChangeHandler) {
        this.confirmCheckboxValueChangeHandlers.remove(confirmCheckboxValueChangeHandler);
    }

    @Override
    public ParamMeta getParamMeta() {
        return valueField.getParamMeta();
    }

    @Nullable
    @Override
    public T getValue() {
        return valueField.getValue();
    }

    @Override
    public void setValue(@Nullable T value, boolean fireEvent) {
        valueField.setValue(value, fireEvent);
    }

    @Override
    public void addValueChangeConsumer(Consumer<T> valueChangedConsumer) {
        valueField.addValueChangeConsumer(valueChangedConsumer);
    }

    @Override
    public List<T> getValueDomain() {
        return valueField.getValueDomain();
    }

    @Override
    public void setValueDomain(List<T> domain) {
        valueField.setValueDomain(domain);
    }

    @Override
    public Supplier<Long> getModificationDateProvider() {
        return valueField.getModificationDateProvider();
    }

    @Override
    public void setModificationDateProvider(Supplier<Long> modificationDateProvider) {
        valueField.setModificationDateProvider(modificationDateProvider);
    }

    @Override
    public void createStructure() {
        valueField.createStructure();
    }

    @Override
    public boolean isEnabled() {
        return valueField.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        valueField.setEnabled(enabled);
    }

    @Override
    public void setFocus(boolean focused) {
        if (valueField instanceof Focusable) {
            ((Focusable) valueField).setFocus(focused);
        }
    }
}
