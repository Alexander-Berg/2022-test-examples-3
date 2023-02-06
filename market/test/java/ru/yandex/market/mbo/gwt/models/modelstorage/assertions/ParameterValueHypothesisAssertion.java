package ru.yandex.market.mbo.gwt.models.modelstorage.assertions;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.Iterables;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValueHypothesis;
import ru.yandex.market.mbo.gwt.models.visual.Word;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class ParameterValueHypothesisAssertion
    extends AbstractObjectAssert<ParameterValueHypothesisAssertion, ParameterValueHypothesis> {
    protected Iterables iterables = Iterables.instance();

    public ParameterValueHypothesisAssertion(ParameterValueHypothesis actual) {
        super(actual, ParameterValueHypothesisAssertion.class);
    }

    public ParameterValueHypothesis getParameterValueHypothesis() {
        return super.actual;
    }

    public ParameterValueHypothesisAssertion exists() {
        ParameterValueHypothesis value = getParameterValueHypothesis();
        Assertions.assertThat(value)
            .withFailMessage("Expected parameterValues exist")
            .isNotNull();
        return this;
    }

    public ParameterValueHypothesisAssertion notExists() {
        ParameterValueHypothesis value = getParameterValueHypothesis();
        Assertions.assertThat(value)
            .withFailMessage("Expected parameterValues not exist. PV: " + value)
            .isNull();
        return this;
    }

    public ParameterValueHypothesisAssertion values(String... strings) {
        return values(true, strings);
    }

    private ParameterValueHypothesisAssertion values(boolean ordered, String... strings) {
        List<String> words = actual.getStringValue().stream().map(Word::getWord).collect(Collectors.toList());
        if (ordered) {
            iterables.assertContainsExactly(info, words, strings);
        } else {
            iterables.assertContainsExactlyInAnyOrder(info, words, strings);
        }
        return myself;
    }
}
