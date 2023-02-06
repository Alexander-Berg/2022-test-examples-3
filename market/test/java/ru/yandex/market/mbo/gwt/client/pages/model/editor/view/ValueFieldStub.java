package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import com.google.common.base.Objects;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ParamMeta;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueField;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.Param;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author gilmulla
 */
public class ValueFieldStub<T> implements ValueField<T> {

    private ParamMeta paramMeta;
    private Supplier<Long> modificationDateProvider;
    private T value;
    private List<T> valueDomain;
    private boolean enabled;
    private int valueDomainCallsCount = 0;

    private List<Consumer<T>> valueChangedConsumers = new ArrayList<>();

    public ValueFieldStub(ParamMeta paramMeta) {
        this.paramMeta = paramMeta;
    }

    @Override
    public Supplier<Long> getModificationDateProvider() {
        return this.modificationDateProvider;
    }

    @Override
    public void setModificationDateProvider(Supplier<Long> modificationDateProvider) {
        this.modificationDateProvider = modificationDateProvider;
    }

    @Override
    public void createStructure() {

    }

    @Override
    public void addValueChangeConsumer(Consumer<T> valueChangedConsumer) {
        this.valueChangedConsumers.add(valueChangedConsumer);
    }

    @Nullable
    @Override
    public T getValue() {
        return this.value;
    }

    @Override
    public void setValue(@Nullable T value, boolean fireEvent) {
        Param.Type type = getParamMeta().getType();
        if (type == Param.Type.NUMERIC_ENUM || type == Param.Type.ENUM) {
            this.value = valueDomain == null
                ? value
                : valueDomain.stream().filter(opt -> opt.equals(value)).findFirst()
                .orElse(null);
        } else {
            this.value = value;
        }

        if (fireEvent) {
            valueChangedConsumers.forEach(c -> c.accept(this.value));
        }
    }

    public void setValueUserInput(T value) {
        this.value = value;
        this.valueChangedConsumers.forEach(c -> c.accept(this.value));
    }

    @Override
    public List<T> getValueDomain() {
        return this.valueDomain;
    }

    @Override
    public void setValueDomain(List<T> domain) {
        valueDomainCallsCount++;
        List<T> previousOptions = this.valueDomain;
        this.valueDomain = domain;

        if (!Objects.equal(this.valueDomain, previousOptions)) {
            setValue(getValue(), false);
        }
    }

    public void setOptions(List<Option> options) {
        this.valueDomain = (List<T>) options;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public ParamMeta getParamMeta() {
        return paramMeta;
    }

    public int getValueDomainCallsCount() {
        return valueDomainCallsCount;
    }
}
