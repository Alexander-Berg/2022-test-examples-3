package ru.yandex.crypta.graph2.dao.yt.local.fastyt;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import ru.yandex.crypta.graph2.dao.yt.local.fastyt.recs.MergingIteratorByKeyAndTableIndex;
import ru.yandex.crypta.graph2.dao.yt.local.fastyt.recs.RecWithKeyAndTableIndex;
import ru.yandex.crypta.graph2.utils.IteratorUtils;

import static org.junit.Assert.assertEquals;

public class MergingIteratorByKeyAndTableIndexTest {

    @Test
    public void testConcatTwoSorted() {
        Iterator<RecWithKeyAndTableIndex<String>> iterator1 = List.of(
                new RecWithKeyAndTableIndex<>("sdfsdfdfs4", "b", 0),
                new RecWithKeyAndTableIndex<>("sdfsdfdfs3", "b", 0),
                new RecWithKeyAndTableIndex<>("sdfsdfdfs2", "c", 0),
                new RecWithKeyAndTableIndex<>("sdfsdfdfs", "d", 0)
        ).iterator();

        Iterator<RecWithKeyAndTableIndex<String>> iterator2 = List.of(
                new RecWithKeyAndTableIndex<>("xxxsdfdfs3", "a", 1),
                new RecWithKeyAndTableIndex<>("xxxsdfdfs4", "d", 1),
                new RecWithKeyAndTableIndex<>("xxxsdfdfs", "d", 1),
                new RecWithKeyAndTableIndex<>("xxxsdfdfs", "d", 1),
                new RecWithKeyAndTableIndex<>("xxxsdfdfs", "x", 1)
        ).iterator();

        MergingIteratorByKeyAndTableIndex<String> iter = new MergingIteratorByKeyAndTableIndex<>(
                List.of(iterator1, iterator2)
        );

        List<RecWithKeyAndTableIndex<String>> result = IteratorUtils
                .stream(iter)
                .collect(Collectors.toList());

        assertEquals(9, result.size());
        assertEquals("a1b0b0c0d0d1d1d1x1", result.stream().map(
                r -> r.getKey() + r.getTableIndex()
        ).collect(Collectors.joining("")));
    }

    @Test
    public void testConcatTwoEmpty() {
        Iterator<RecWithKeyAndTableIndex<String>> iterator1 = Collections.emptyIterator();

        Iterator<RecWithKeyAndTableIndex<String>> iterator2 = Collections.emptyIterator();
        MergingIteratorByKeyAndTableIndex<String> iter = new MergingIteratorByKeyAndTableIndex<>(
                List.of(iterator1, iterator2)
        );

        List<RecWithKeyAndTableIndex<String>> result = IteratorUtils
                .stream(iter)
                .collect(Collectors.toList());

        assertEquals(0, result.size());
    }
}
