package ru.yandex.market.api.util.parser2.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.common.util.collections.Maybe;
import ru.yandex.market.api.util.parser2.validation.errors.ParsedValueValidationError;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class CollectionValidatorTest {
    private ParsedValueValidator<IntList, ?> listValidator;

    @Before
    public void setUp() throws Exception {
        listValidator = new CollectionValidator<>(new DescribedByValuesValidator<Integer, Object>() {
            @Override
            public ParsedValueValidationError validate(Maybe<Integer> value, Object o) {
                return value.getValue(1) > 0 ? null : new TestError();
            }

            @Override
            public Collection<Integer> getAllowedValues(Object o) {
                return new IntArrayList(new int[]{1, 2, 3});
            }
        });
    }

    @Test
    public void shouldValidateNull() {
        Assert.assertNull(listValidator.validate(Maybe.nothing(), null));
    }

    @Test
    public void shouldValidateEmptyList() {
        Assert.assertNull(listValidator.validate(Maybe.just(IntLists.EMPTY_LIST), null));
    }

    @Test
    public void shouldValidateCorrectValue() {
        Assert.assertNull(listValidator.validate(Maybe.just(IntLists.singleton(1)), null));
    }

    @Test
    public void shouldValidateCorrectValues() {
        IntList list = new IntArrayList(Arrays.asList(1, 2));
        Assert.assertNull(listValidator.validate(Maybe.just(list), null));
    }

    @Test
    public void shouldNotValidateIncorrectValue() {
        ParsedValueValidationError error = listValidator.validate(Maybe.just(IntLists.singleton(-1)), null);
        Assert.assertTrue(error instanceof TestError);
    }

    @Test
    public void shouldNotValidateIncorrectValues() {
        IntList list = new IntArrayList(Arrays.asList(-1, -2));
        ParsedValueValidationError error = listValidator.validate(Maybe.just(list), null);
        Assert.assertTrue(error instanceof TestError);
    }

    @Test
    public void shouldNotValidateMixedValues() {
        IntList list = new IntArrayList(Arrays.asList(-1, 1));
        ParsedValueValidationError error = listValidator.validate(Maybe.just(list), null);
        Assert.assertTrue(error instanceof TestError);

        list = new IntArrayList(Arrays.asList(1, -1));
        error = listValidator.validate(Maybe.just(list), null);
        Assert.assertTrue(error instanceof TestError);
    }

    @Test
    public void shouldFormatAllowedValues() {
        String description = listValidator.getDescription(
                v -> v.stream().map(String::valueOf).collect(Collectors.joining(";")),
                null);
        assertThat(description, containsString("1;2;3"));
    }

    static class TestError implements ParsedValueValidationError {
        @Override
        public String getMessage(String parserDescription) {
            return null;
        }
    }
}
