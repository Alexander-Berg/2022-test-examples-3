package ru.yandex.market.mbo.gwt.client.pages.model.editor.view.sku.widgets;

import com.google.gwt.user.client.ui.Widget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorEventBus;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.sku.SkuFilterAppliedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.sku.SkuTableSortedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.sku.widget.SkuTableFilter;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class SkuTableFilterStub implements SkuTableFilter {

    private List<String> availableValues = new ArrayList<>();
    private CategoryParam param;
    private EditorEventBus bus;
    Function<CategoryParam, List<String>> valueProviderFunc;

    public SkuTableFilterStub(CategoryParam param, EditorEventBus bus) {
        this.param = param;
        this.bus = bus;
    }

    @Override
    public void setOnFilterOpenValueProvider(Function<CategoryParam, List<String>> valueProviderFunc) {
        this.valueProviderFunc = valueProviderFunc;
    }

    @Override
    public void reset() {
    }

    @Override
    public void updateInvalidBoxesStyle(List<String> badValues) {

    }

    @Override
    public CategoryParam getRelatedParam() {
        return param;
    }

    @Override
    public Widget asWidget() {
        return null;
    }

    public List<String> click() {
        if (valueProviderFunc != null) {
            availableValues = valueProviderFunc.apply(param);
        }
        return availableValues;
    }

    public void applyBooleanFilter(Boolean... values) {
        List<String> constraints = new ArrayList<>();
        for (Boolean value : values) {
            constraints.add(value == null ? null : String.valueOf(value));
        }
        bus.fireEvent(SkuFilterAppliedEvent.withData(param, constraints));
    }

    public void applyStringFilter(String... values) {
        List<String> constraints = Arrays.asList(values);
        bus.fireEvent(SkuFilterAppliedEvent.withData(param, constraints));
    }

    public void applyNumericFilter(int... values) {
        List<String> constraints = new ArrayList<>();
        for (int value : values) {
            constraints.add(String.valueOf(value));
        }
        bus.fireEvent(SkuFilterAppliedEvent.withData(param, constraints));
    }

    public void fullReset() {
        bus.fireEvent(SkuFilterAppliedEvent.fullReset());
    }

    public void partialReset() {
        bus.fireEvent(SkuFilterAppliedEvent.reset(param));
    }

    public void applySorting(SkuTableSortedEvent.Direction direction) {
        bus.fireEvent(new SkuTableSortedEvent(direction, param));
    }
}
