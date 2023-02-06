package ru.yandex.market.mbo.gwt.models.modelstorage;

import org.assertj.core.api.AbstractObjectAssert;
import ru.yandex.market.mbo.gwt.models.modelstorage.assertions.ParameterValuesAssertion;
import ru.yandex.market.mbo.utils.MboAssertions;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 23.05.2018
 */
public class ModelChangesAssertion extends AbstractObjectAssert<ModelChangesAssertion, ModelChanges> {
    private final Set<String> leftParameters;

    public ModelChangesAssertion(ModelChanges changes) {
        super(changes, ModelChangesAssertion.class);
        this.leftParameters = new HashSet<>(changes.getUpdatedParamsNames());
    }

    public ModelChangesAssertion parameterChanged(String xslName, Consumer<ParameterValuesAssertion> assertion) {
        Optional<ParameterValues> first = actual.getChanges().stream()
            .filter(pv -> pv.getXslName().equals(xslName))
            .findFirst();

        if (!first.isPresent()) {
            failWithMessage("not found changes for parameter " + xslName);
        }

        leftParameters.remove(xslName);

        assertion.accept(MboAssertions.assertThat(first.get()));
        return myself;
    }

    public ModelChangesAssertion noMoreChanges() {
        assertThat(leftParameters)
            .withFailMessage("more changed parameters are present %s", leftParameters)
            .isEmpty();
        return myself;
    }
}
