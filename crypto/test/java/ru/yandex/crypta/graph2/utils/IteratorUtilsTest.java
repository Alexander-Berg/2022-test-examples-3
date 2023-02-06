package ru.yandex.crypta.graph2.utils;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.crypta.graph2.utils.IteratorUtils.IteratorSplit;

import static org.junit.Assert.assertTrue;

public class IteratorUtilsTest {
    @Test
    public void takeWhile() throws Exception {

        ListF<String> input = Cf.list("a1", "a2", "a3", "b1", "b2", "c1", "c999");
        IteratorSplit<String> iteratorSplit = IteratorUtils.takeWhile(input.iterator(), el -> el.startsWith("a"));
        ListF<String> head = iteratorSplit.getHead();
        ListF<String> restList = iteratorSplit.getTail().toList();

        ListF<String> expectedHead = Cf.arrayList("a1", "a2", "a3");
        ListF<String> expectedTail = Cf.arrayList("b1", "b2", "c1", "c999");

        ru.yandex.misc.test.Assert.assertListsEqual(expectedHead, head);
        ru.yandex.misc.test.Assert.assertListsEqual(expectedTail, restList);

    }

    @Test
    public void takeWhile1() throws Exception {

        ListF<String> input = Cf.list("a1");
        IteratorSplit<String> iteratorSplit = IteratorUtils.takeWhile(input.iterator(), el -> el.startsWith("a"));
        ListF<String> head = iteratorSplit.getHead();
        ListF<String> restList = iteratorSplit.getTail().toList();

        ListF<String> expectedHead = Cf.arrayList("a1");

        ru.yandex.misc.test.Assert.assertListsEqual(expectedHead, head);
        assertTrue(restList.isEmpty());

    }

    @Test
    public void notTakeWhile1() throws Exception {

        ListF<String> input = Cf.list("a1");
        IteratorSplit<String> iteratorSplit = IteratorUtils.takeWhile(input.iterator(), el -> el.startsWith("b"));
        ListF<String> head = iteratorSplit.getHead();
        ListF<String> restList = iteratorSplit.getTail().toList();

        assertTrue(head.isEmpty());

        ListF<String> expectedTail = Cf.arrayList("a1");
        ru.yandex.misc.test.Assert.assertListsEqual(expectedTail, restList);


    }

}
