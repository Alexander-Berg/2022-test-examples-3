package ru.yandex.market.sqb.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.model.common.HasName;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.sqb.test.TestUtils.checkConstructor;

/**
 * Unit-тесты для {@link SqbCollectionUtils}.
 *
 * @author Vladislav Bauer
 */
class SqbCollectionUtilsTest {

    @Test
    void testConstructorContract() {
        checkConstructor(SqbCollectionUtils.class);
    }

    @Test
    void testFindDuplicatedOne() {
        final Collection<Integer> duplicated = SqbCollectionUtils.findDuplicated(Arrays.asList(1, 2, 1));

        assertThat(duplicated, hasSize(2));
        assertThat(duplicated, containsInAnyOrder(1, 1));
    }

    @Test
    void testFindDuplicatedNoOne() {
        final Collection<Integer> duplicated = SqbCollectionUtils.findDuplicated(Arrays.asList(1, 2, 3));

        assertThat(duplicated, empty());
    }

    @Test
    void testToSafeList() {
        final Object addValue = new Object();

        checkToSafeList(null, addValue);
        checkToSafeList(Collections.emptyList(), addValue);
        checkToSafeList(Collections.singletonList(new Object()), addValue);
    }

    @Test
    void testJoinNotEmpty() {
        final String name = "test";
        final String separator = ",";

        checkJoinNotEmpty(Collections.emptyList(), separator, StringUtils.EMPTY);
        checkJoinNotEmpty(Collections.singletonList(() -> name), separator, name);
        checkJoinNotEmpty(Arrays.asList(() -> name, () -> name), separator, name + separator + name);
    }


    private void checkJoinNotEmpty(final List<HasName> objects, final String separator, final String expected) {
        assertThat(
                SqbCollectionUtils.joinNotEmpty(objects, HasName::getName, separator),
                equalTo(expected)
        );
    }

    private <T> void checkToSafeList(final List<T> collection, final T addValue) {
        final List<T> safeList = SqbCollectionUtils.toSafeList(collection);
        Assertions.assertThrows(Exception.class, () -> safeList.add(addValue));
    }

}
