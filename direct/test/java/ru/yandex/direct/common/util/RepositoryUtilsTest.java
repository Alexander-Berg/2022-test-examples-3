package ru.yandex.direct.common.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class RepositoryUtilsTest {
    enum TestEnum {
        ITEM,
        HORSE
    }

    @Test
    public void testFromEmptyOnNull() {
        assertThat(RepositoryUtils.setFromDb(null, x -> Enum.valueOf(TestEnum.class, x)))
                .isEmpty();
    }

    @Test
    public void testFromEmptyOnEmptySting() {
        assertThat(RepositoryUtils.setFromDb("", x -> Enum.valueOf(TestEnum.class, x)))
                .isEmpty();
    }

    @Test
    public void testFromEmptyOnWhitespaceSting() {
        assertThat(RepositoryUtils.setFromDb("  ", x -> Enum.valueOf(TestEnum.class, x)))
                .isEmpty();
    }

    @Test
    public void testFromEmptyOnAllValues() {
        assertThat(RepositoryUtils.setFromDb("HORSE,ITEM,HORSE", x -> Enum.valueOf(TestEnum.class, x)))
                .containsExactlyInAnyOrder(
                        TestEnum.ITEM,
                        TestEnum.HORSE
                );
    }

    @Test
    public void testFromEmptyOnOneValue() {
        assertThat(RepositoryUtils.setFromDb("ITEM", x -> Enum.valueOf(TestEnum.class, x)))
                .containsExactlyInAnyOrder(
                        TestEnum.ITEM
                );
    }

    @Test
    public void testToNull() {
        assertThat(RepositoryUtils.setToDb((Set<TestEnum>) null, Enum::toString))
                .isNull();
    }

    @Test
    public void testToEmpty() {
        assertThat(RepositoryUtils.setToDb(Collections.<TestEnum>emptySet(), Enum::toString))
                .isEqualTo("");
    }

    @Test
    public void testToOne() {
        assertThat(RepositoryUtils.setToDb(Collections.singleton(TestEnum.ITEM), Enum::toString))
                .isEqualTo("ITEM");
    }

    @Test
    public void testToAll() {
        assertThat(RepositoryUtils.setToDb(new HashSet<>(Arrays.asList(TestEnum.ITEM, TestEnum.HORSE)), Enum::toString))
                .is(matchedBy(anyOf(equalTo("ITEM,HORSE"), equalTo("HORSE,ITEM"))));
    }
}
