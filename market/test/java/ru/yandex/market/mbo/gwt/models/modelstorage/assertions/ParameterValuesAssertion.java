package ru.yandex.market.mbo.gwt.models.modelstorage.assertions;

import com.google.common.base.Preconditions;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.utils.MboAssertions;

import javax.annotation.Nonnull;

/**
 * Содержит удобные проверки для {@link ParameterValues}.
 * Вызывать лучше из {@link MboAssertions}.
 * <p>
 * Для проверки {@link ParameterValue} использовать {@link ParameterValueAssertion}.
 *
 * @author s-ermakov
 */
public class ParameterValuesAssertion extends BaseParameterValuesAssertion<ParameterValuesAssertion> {

    public ParameterValuesAssertion(ParameterValues parameterValues) {
        super(parameterValues, ParameterValuesAssertion.class);
        Preconditions.checkNotNull(parameterValues, "Expected not null parameterValues");
    }

    @Nonnull
    @Override
    protected String createSubFailMessage() {
        return "parameter value";
    }
}
