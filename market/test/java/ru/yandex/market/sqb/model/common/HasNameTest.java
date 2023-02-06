package ru.yandex.market.sqb.model.common;

import java.util.Collections;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.sqb.test.TestUtils.checkOptional;

/**
 * Unit-тесты для {@link HasName}.
 *
 * @author Vladislav Bauer
 */
class HasNameTest {

    private static final String TEST_NAME = "Hi, my name is ...";


    @Test
    void testGetNames() {
        assertThat(HasName.getNames(Collections.emptyList()), empty());

        assertThat(
                HasName.getNames(Collections.singletonList(createNamed())),
                equalTo(Collections.singletonList(TEST_NAME))
        );
    }

    @Test
    void testByNames() {
        final String name = "test";
        final Predicate<HasName> predicate = HasName.byNames(name);

        assertThat(predicate, notNullValue());
        assertThat(predicate.test(createNamed()), equalTo(false));
        assertThat(predicate.test(() -> name), equalTo(true));
    }

    @Test
    void testSameName() {
        assertThat(HasName.sameName(null, null), equalTo(true));
        assertThat(HasName.sameName(null, ""), equalTo(false));
        assertThat(HasName.sameName("name1", "NAMe1"), equalTo(true));
        assertThat(HasName.sameName("name1", "name2"), equalTo(false));
    }

    @Test
    void testFindByNameNegative() {
        checkOptional(HasName.findByName(null, null), null);
        checkOptional(HasName.findByName(null, StringUtils.EMPTY), null);
        checkOptional(HasName.findByName(Collections.emptySet(), StringUtils.EMPTY), null);
        checkOptional(HasName.findByName(Collections.emptySet(), null), null);

        final HasName named = createNamed();
        checkOptional(HasName.findByName(Collections.singleton(named), null), null);
    }

    @Test
    void testFindByNamePositive() {
        final HasName named = createNamed();

        checkOptional(
                HasName.findByName(Collections.singleton(named), StringUtils.upperCase(named.getName())),
                named
        );
    }

    @Test
    void testHasWithNameNegative() {
        assertThat(HasName.hasWithName(null, null), equalTo(false));
        assertThat(HasName.hasWithName(null, StringUtils.EMPTY), equalTo(false));
        assertThat(HasName.hasWithName(Collections.emptySet(), StringUtils.EMPTY), equalTo(false));
        assertThat(HasName.hasWithName(Collections.emptySet(), null), equalTo(false));

        final HasName named = createNamed();
        assertThat(HasName.hasWithName(Collections.singleton(named), null), equalTo(false));
    }

    @Test
    void testHasWithNamePositive() {
        final HasName named = createNamed();

        assertThat(
                HasName.hasWithName(Collections.singleton(named), StringUtils.upperCase(named.getName())),
                equalTo(true)
        );
    }


    private HasName createNamed() {
        return () -> TEST_NAME;
    }

}
