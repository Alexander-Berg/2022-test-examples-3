package ru.yandex.market.sqb.model.common;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.exception.SqbException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit-тесты для {@link HasValue}.
 *
 * @author Vladislav Bauer
 */
class HasValueTest {

    @Test
    void testGetObjectValuePositive() {
        final String expected = "test value";
        final Supplier<Optional<String>> object = createObject(expected);

        assertThat(HasValue.getObjectValue(object, Supplier::get), equalTo(expected));
    }

    @Test
    void testGetObjectValueNegative() {
        final Supplier<Optional<String>> object = createObject(null);

        Assertions.assertThrows(SqbException.class, () -> HasValue.getObjectValue(object, Supplier::get));
    }


    private Supplier<Optional<String>> createObject(final String value) {
        return () -> Optional.ofNullable(value);
    }

}
