package ru.yandex.market.stat.dicts.common;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static java.util.Collections.emptyList;

/**
 * @author zoom
 */
public class RowMapperHelperTest {

    @Test
    public void shouldReturnEmptyListWhenNull() {
        Assert.assertEquals(emptyList(), RowMapperHelper.resolveIntList(null));
    }

    @Test
    public void shouldReturnEmptyListWhenEmptyString() {
        Assert.assertEquals(emptyList(), RowMapperHelper.resolveIntList(""));
    }

    @Test
    public void shouldReturnOneElementListWhenStringContainsOneInteger() {
        Assert.assertEquals(Collections.singletonList(101), RowMapperHelper.resolveIntList("101"));
    }

    @Test
    public void shouldReturnTwoElementListWhenStringContainsTwoIntegers() {
        Assert.assertEquals(Arrays.asList(101, 202), RowMapperHelper.resolveIntList("101,202"));
    }

    @Test
    public void shouldReturnTwoElementListWhenStringContainsTwoIntegersAndSpaces() {
        Assert.assertEquals(Arrays.asList(101, 202), RowMapperHelper.resolveIntList(" 101, 202"));
    }
}
