package ru.yandex.calendar.logic.user;

import java.util.EnumSet;
import java.util.Arrays;
import java.util.NoSuchElementException;

import lombok.val;
import org.junit.Test;

import ru.yandex.bolts.collection.CollectorsF;
import ru.yandex.calendar.logic.beans.IntegerArray;
import ru.yandex.misc.test.Assert;


public class GroupTest {
    @Test
    public void arrayOfEmptyEnum() {
        Assert.equals(EnumSet.noneOf(Group.class), Group.integerArrayToEnumSet(new IntegerArray()));
    }

    @Test
    public void arrayOf1Enum() {
        for (Group group : Group.values()) {
            Assert.equals(EnumSet.of(group), Group.integerArrayToEnumSet(new IntegerArray(group.value())));
        }
    }

    @Test
    public void arrayOf2Enum() {
        for (Group group1 : Group.values()) {
            for (Group group2 : Group.values()) {
                Assert.equals(EnumSet.of(group1, group2),
                        Group.integerArrayToEnumSet(new IntegerArray(group1.value(), group2.value())));
            }
        }
    }

    @Test
    public void arrayOfAllEnum() {
        val groups = Arrays.stream(Group.values())
                .map(Group::value)
                .collect(CollectorsF.toList());
        Assert.equals(EnumSet.allOf(Group.class), Group.integerArrayToEnumSet(new IntegerArray(groups)));
    }

    @Test
    public void arrayOfNonexistentEnum() {
        int nonexistentGroupValue = Arrays.stream(Group.values()).mapToInt(Group::value).max().orElse(0) + 1;
        Assert.assertThrows(() -> Group.integerArrayToEnumSet(new IntegerArray(nonexistentGroupValue)),
                NoSuchElementException.class);
    }
}
