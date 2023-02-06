package ru.yandex.market.sqb.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.exception.SqbValidationException;
import ru.yandex.market.sqb.model.common.HasName;
import ru.yandex.market.sqb.model.vo.AliasVO;
import ru.yandex.market.sqb.test.ObjectGenerationUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.market.sqb.test.TestUtils.checkConstructor;

/**
 * Unit-тесты для {@link SqbValidationUtils}.
 *
 * @author Vladislav Bauer
 */
class SqbValidationUtilsTest {

    @Test
    void testConstructorContract() {
        checkConstructor(SqbValidationUtils.class);
    }

    @Test
    void testCheckUniquenessPositive() {
        final List<Integer> expected = Arrays.asList(1, 2, 3);
        final List<Integer> uniqueness = SqbValidationUtils.checkUniqueness(expected);

        Assertions.assertThrows(Exception.class, () -> uniqueness.add(4));

        assertThat(CollectionUtils.isEqualCollection(expected, uniqueness), equalTo(true));
    }

    @Test
    void testCheckUniquenessNegative() {
        Assertions.assertThrows(SqbValidationException.class,
                () -> SqbValidationUtils.checkUniqueness(Arrays.asList(1, 2, 1)));
    }

    @Test
    void testCheckNamePositive() {
        for (final String name : ObjectGenerationUtils.namesLegal()) {
            final String actual = checkName(name);
            final String expected = StringUtils.upperCase(name);

            assertThat(actual, equalTo(expected));
        }
    }

    @Test
    void testCheckNameNegative() {
        for (final String name : ObjectGenerationUtils.namesIllegal()) {
            Assertions.assertThrows(SqbValidationException.class, () -> checkName(name));
        }
    }

    @Test
    void testCheckForbiddenValuesPositive() {
        final Object object = new Object();
        final String value = "value";

        assertThat(
                SqbValidationUtils.checkForbiddenValues(object, value, Collections.emptySet()),
                equalTo(value)
        );

        assertThat(
                SqbValidationUtils.checkForbiddenValues(object, value, ImmutableSet.of("1", "2", "3")),
                equalTo(value)
        );
    }

    @Test
    void testCheckForbiddenValuesNegative() {
        final Object object = new Object();
        final String value = "value";

        Assertions.assertThrows(SqbValidationException.class,
                () -> SqbValidationUtils.checkForbiddenValues(object, value, Collections.singleton(value)));
    }

    @Test
    void testCheckForbiddenPrefixesPositive() {
        final String value = "value";
        final HasName object = () -> value;

        assertThat(
                SqbValidationUtils.checkForbiddenPrefixes(object, value, Collections.emptySet()),
                equalTo(value)
        );

        assertThat(
                SqbValidationUtils.checkForbiddenPrefixes(object, value, ImmutableSet.of("1", "2", "3")),
                equalTo(value)
        );
    }

    @Test
    void testCheckForbiddenPrefixesNegative() {
        final String prefix = "prefix";
        final String value = prefix + ObjectGenerationUtils.createName();
        final HasName object = () -> value;

        Assertions.assertThrows(SqbValidationException.class,
                () -> SqbValidationUtils.checkForbiddenPrefixes(object, value, Collections.singleton(prefix)));
    }


    private String checkName(final String name) {
        final HasName model = () -> name;
        return SqbValidationUtils.checkName(model, AliasVO.NAME);
    }

}
