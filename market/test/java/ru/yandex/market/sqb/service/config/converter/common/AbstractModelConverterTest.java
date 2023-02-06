package ru.yandex.market.sqb.service.config.converter.common;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.exception.SqbValidationException;
import ru.yandex.market.sqb.service.config.converter.ModelConverter;

/**
 * Базовый класс тестов для {@link ModelConverter}.
 *
 * @author Vladislav Bauer
 */

public abstract class AbstractModelConverterTest<IN, OUT> {

    @Test
    void testListNegative() {
        final IN object1 = createObject();
        final IN object2 = createObject();
        final List<IN> list = Arrays.asList(object1, object2);

        final ModelConverter<IN, OUT> converter = createConverter();
        Assertions.assertThrows(SqbValidationException.class, () -> converter.convertList(list));
    }


    @Nonnull
    protected abstract IN createObject();

    @Nonnull
    protected abstract ModelConverter<IN, OUT> createConverter();

}
