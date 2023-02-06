package ru.yandex.direct.internaltools.tools.testtool.preprocessors;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;
import ru.yandex.direct.internaltools.core.input.InternalToolInput;
import ru.yandex.direct.internaltools.core.input.InternalToolInputPreProcessor;

import static ru.yandex.direct.validation.constraint.CommonConstraints.inSet;

/**
 * Препроцессор, заменяющий допустимые значения в элементе ввода на список всех существующих на момент вызова пропертей
 */
@Component
public class SetAnyPropertyToolPreProcessor implements InternalToolInputPreProcessor<String> {
    private final PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    public SetAnyPropertyToolPreProcessor(PpcPropertiesSupport ppcPropertiesSupport) {
        this.ppcPropertiesSupport = ppcPropertiesSupport;
    }

    @Override
    public <T extends InternalToolParameter> InternalToolInput.Builder<T, String> preSend(
            InternalToolInput.Builder<T, String> inputBuilder) {
        List<String> strings = new ArrayList<>(ppcPropertiesSupport.getAllProperties().keySet());
        return inputBuilder
                .withDefaultValue(Iterables.getFirst(strings, null))
                .withAllowedValues(strings)
                .addValidator(inSet(ImmutableSet.<String>builder().addAll(strings).build()));
    }
}
