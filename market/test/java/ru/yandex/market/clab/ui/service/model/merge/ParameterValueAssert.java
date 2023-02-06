package ru.yandex.market.clab.ui.service.model.merge;

import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.AssertFactory;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectAssert;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.ParameterValue;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 12.04.2019
 */
public class ParameterValueAssert extends AbstractObjectAssert<ParameterValueAssert, ParameterValue> {

    private final ParameterValue parameterValue;

    public ParameterValueAssert(ParameterValue parameterValue) {
        super(parameterValue, ParameterValueAssert.class);
        this.parameterValue = parameterValue;
    }

    public ListAssert<ModelStorage.LocalizedString> extractingLocalizedStrings() {
        assertNotNull();
        return assertThat(parameterValue.getStrValueList());
    }

    public AbstractListAssert<?, List<? extends String>, String, ObjectAssert<String>> extractingStrings() {
        assertNotNull();
        return assertThat(parameterValue.getStrValueList()).extracting(ModelStorage.LocalizedString::getValue);
    }

    private void assertNotNull() {
        withFailMessage("Expecting parameterValue not to be null").isNotNull();
    }

    public static AssertFactory<ParameterValue, ParameterValueAssert> parameterValue() {
        return ParameterValueAssert::new;
    }
}
