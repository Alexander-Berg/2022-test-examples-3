package ru.yandex.market.mbo.gwt.models.sorting;

import org.junit.Test;

import ru.yandex.market.mbo.gwt.models.IdAndName;
import ru.yandex.market.mbo.gwt.models.sorting.ByWordAndIdComparator.Group;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 01.12.2017
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ByWordAndIdComparatorTest {

    @Test
    public void groupTest() {
        ByWordAndIdComparator<IdAndName> comparator = ByWordAndIdComparator.forHasName("milK");

        assertThat("equals filter", comparator.getGroup(item(6, "milk")), is(Group.NAME_STARTS));
        assertThat("by prefix word", comparator.getGroup(item(7, "milkshake with strawberry")), is(Group.NAME_STARTS));
        assertThat("ignoring case", comparator.getGroup(item(9, "Milkshake")), is(Group.NAME_STARTS));
        assertThat("not first word", comparator.getGroup(item(3, "more milk")), is(Group.NAME_CONTAINS));
        assertThat("ignoring case in word", comparator.getGroup(item(4, "another Milkshake")), is(Group.NAME_CONTAINS));
        assertThat("not matches", comparator.getGroup(item(5, "doesn't contain word")), is(Group.NO_MATCHES));
    }

    @Test
    public void doesntContainOneWord() {
        ByWordAndIdComparator<IdAndName> comparator = ByWordAndIdComparator.forHasName("milK and tea");
        assertThat("not matches", comparator.getGroup(item(5, "have some coffee with milk")), is(Group.NO_MATCHES));
    }

    @Test
    public void idWithText() {
        ByWordAndIdComparator<IdAndName> comparator = ByWordAndIdComparator.forHasName("milk 45");
        assertThat("use id", comparator.getGroup(item(45, "coffee with milk")), is(Group.ID_MATCHES));
        assertThat("use id from coffee", comparator.getGroup(item(450, "coffee with milk")), is(Group.ID_STARTS));
        assertThat("find in name, no id", comparator.getGroup(item(47, "coffee with milk")), is(Group.NO_MATCHES));
        assertThat("find in name", comparator.getGroup(item(47, "coffee 45 with milk")), is(Group.NAME_CONTAINS));
    }

    @Test
    public void idWithNullName() {
        ByWordAndIdComparator<IdAndName> comparator = ByWordAndIdComparator.forHasName("17");
        assertThat(comparator.getGroup(item(17, null)), is(Group.ID_MATCHES));
        assertThat(comparator.getGroup(item(179, null)), is(Group.ID_STARTS));
        assertThat(comparator.getGroup(item(13, null)), is(Group.NO_MATCHES));
    }

    @Test
    public void simpleSort() {
        ByWordAndIdComparator<IdAndName> comparator = ByWordAndIdComparator.forHasName("milk");
        List<IdAndName> source = Arrays.asList(
                item(2, "more milk"),
                item(5, "another milkshake"),
                item(3, "some milk"),
                item(0, "doesn't contain word"),
                item(9, "milk"),
                item(4, "milkshake with strawberry"),
                item(1, "Milkshake")
        );

        List<IdAndName> expected = Arrays.asList(
                item(9, "milk"),
                item(1, "Milkshake"),
                item(4, "milkshake with strawberry"),
                item(5, "another milkshake"),
                item(2, "more milk"),
                item(3, "some milk"),
                item(0, "doesn't contain word")
                );

        source.sort(comparator);
        assertThat(source, is(expected));
    }

    @Test
    public void numericFilter() {
        ByWordAndIdComparator<IdAndName> comparator = ByWordAndIdComparator.forHasName("14");
        List<IdAndName> source = Arrays.asList(
                item(1, "more milk 14%"),
                item(147, "some milk"),
                item(14, "doesn't contain word"),
                item(9, "146% milk"),
                item(149, "another milkshake"),
                item(4, "milkshake with strawberry"),
                item(1, "Milkshake")
        );

        List<IdAndName> expected = Arrays.asList(
                item(14, "doesn't contain word"),
                item(149, "another milkshake"),
                item(147, "some milk"),
                item(9, "146% milk"),
                item(1, "more milk 14%"),
                item(1, "Milkshake"),
                item(4, "milkshake with strawberry")
                );

        source.sort(comparator);
        assertThat(source, is(expected));
    }

    private static IdAndName item(int id, String name) {
        return new IdAndName(id, name);
    }
}
