package ru.yandex.market.sqb.service.config.converter.common;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.exception.SqbValidationException;
import ru.yandex.market.sqb.model.common.AbstractNameModel;
import ru.yandex.market.sqb.model.common.AbstractVO;
import ru.yandex.market.sqb.service.config.converter.ModelConverter;
import ru.yandex.market.sqb.test.ObjectGenerationUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Базовый класс тестов для именованных {@link ModelConverter}.
 *
 * @author Vladislav Bauer
 */
public abstract class AbstractNameModelConverterTest<IN extends AbstractVO, OUT extends AbstractNameModel>
        extends AbstractModelConverterTest<IN, OUT> {


    @Test
    void testNamesNegative() {
        final ModelConverter<IN, OUT> converter = createConverter();

        for (final String name : getIllegalNames()) {
            final IN object = createObject(name);
            Assertions.assertThrows(SqbValidationException.class, () -> converter.convert(object));
        }
    }

    @Test
    void testNamesPositive() {
        final ModelConverter<IN, OUT> converter = createConverter();

        for (final String name : getLegalNames()) {
            final IN object = createObject(name);
            final OUT converted = converter.convert(object);

            assertThat(converted, notNullValue());
        }
    }


    @Nonnull
    protected Collection<String> getIllegalNames() {
        return Arrays.asList(ObjectGenerationUtils.namesIllegal());
    }

    @Nonnull
    protected Collection<String> getLegalNames() {
        return Arrays.asList(ObjectGenerationUtils.namesLegal());
    }


    private IN createObject(final String name) {
        final IN object = createObject();
        object.setName(name);
        return object;
    }

}
