package ru.yandex.market.mbo.gwt.models.modelstorage.assertions;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;

/**
 * @author s-ermakov
 */
public class ParameterValueAssertion extends AbstractObjectAssert<ParameterValueAssertion, ParameterValue> {
    private final ParameterValue parameterValue;

    public ParameterValueAssertion(ParameterValue parameterValue) {
        super(parameterValue, ParameterValueAssertion.class);
        this.parameterValue = parameterValue;
    }

    public ParameterValue getParameterValue() {
        return parameterValue;
    }

    public ParameterValueAssertion exists() {
        ParameterValue value = getParameterValue();
        Assertions.assertThat(value)
            .withFailMessage("Expected parameterValues exist")
            .isNotNull();
        return this;
    }

    public ParameterValueAssertion notExists() {
        ParameterValue value = getParameterValue();
        Assertions.assertThat(value)
            .withFailMessage("Expected parameterValues not exist. PV: " + value)
            .isNull();
        return this;
    }

    public ParameterValueAssertion hasModificationSource(ModificationSource modificationSource) {
        exists();

        ParameterValue value = getParameterValue();
        Assertions.assertThat(value.getModificationSource())
            .isEqualTo(modificationSource);

        return this;
    }

    public ParameterValueAssertion hasUserId(long userId) {
        exists();

        ParameterValue value = getParameterValue();
        Assertions.assertThat(value.getLastModificationUid())
            .isEqualTo(userId);

        return this;
    }
}
