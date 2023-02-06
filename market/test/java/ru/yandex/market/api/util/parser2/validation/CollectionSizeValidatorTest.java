package ru.yandex.market.api.util.parser2.validation;


import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.common.util.collections.Maybe;
import ru.yandex.market.api.util.parser2.validation.errors.ParsedValueValidationError;

/**
 * @author dimkarp93
 */
public class CollectionSizeValidatorTest {
    public CollectionSizeValidator<IntList> validator = new CollectionSizeValidator<>(10, "error");

    @Test
    public void testNoValue() {
        doTest(Matchers.nullValue(ParsedValueValidationError.class), Maybe.nothing());
    }

    @Test
    public void testEmptyCollection() {
        doTest(Matchers.nullValue(ParsedValueValidationError.class), Maybe.just(IntLists.EMPTY_LIST));
    }

    @Test
    public void testCollectionWith2Elements() {
        IntList list = Stream.of(1, 2).collect(Collectors.toCollection(IntArrayList::new));
        doTest(Matchers.nullValue(ParsedValueValidationError.class), Maybe.just(list));
    }

    @Test
    public void testCollectionWith10Elements() {
        IntList list = Stream.generate(() -> 1).limit(10).collect(Collectors.toCollection(IntArrayList::new));
        doTest(Matchers.nullValue(ParsedValueValidationError.class), Maybe.just(list));
    }

    @Test
    public void testCollectionWith11Elements() {
        IntList list = Stream.generate(() -> 1).limit(11).collect(Collectors.toCollection(IntArrayList::new));
        doTest(Matchers.notNullValue(ParsedValueValidationError.class), Maybe.just(list));
    }

    public void doTest(Matcher<ParsedValueValidationError> errorMatcher, Maybe<IntList> value) {
        Assert.assertThat(validator.validate(value), errorMatcher);
    }
}
