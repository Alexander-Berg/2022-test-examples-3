package ru.yandex.direct.mysql.ytsync.export.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.direct.mysql.ytsync.export.util.queue.IndexedPriorityQueue;

public class IndexedPriorityQueueTest {
    @Test
    public void testSimple() {
        IndexedPriorityQueue<Integer, Integer> queue = new IndexedPriorityQueue<>();
        List<Pair<Integer, Integer>> expected = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            expected.add(new MutablePair<>(i, i));
            queue.put(i, i);
        }
        checkPriorities(queue, expected);
        queue.change(3, i -> 10);
        expected.stream().filter(p -> p.getKey().equals(3)).findFirst().get().setValue(10);
        checkPriorities(queue, expected);
        queue.change(2, i -> -5);
        expected.stream().filter(p -> p.getKey().equals(2)).findFirst().get().setValue(-5);
        checkPriorities(queue, expected);
    }

    private void testPrioritiesPermutations(IndexedPriorityQueue<Integer, Integer> queue,
                                            List<Pair<Integer, Integer>> toAdd, int start, List<Pair<Integer, Integer>> toRemove) {
        // Добавляем по одному элементу из toAdd
        for (int index = start; index < toAdd.size(); index++) {
            Pair<Integer, Integer> next = toAdd.get(index);
            toAdd.set(index, toAdd.get(start));
            toAdd.set(start, next);
            //
            queue.put(next.getKey(), next.getValue());
            toRemove.add(next);
            checkPriorities(queue, toRemove);

            // Рекурсивно продолжаем
            testPrioritiesPermutations(queue, toAdd, start + 1, toRemove);

            // И удаляем после проверки
            queue.delete(next.getKey());
            toRemove.remove(next);
            checkPriorities(queue, toRemove);
            //
            toAdd.set(start, toAdd.get(index));
            toAdd.set(index, next);
        }
    }

    private void checkPriorities(IndexedPriorityQueue<Integer, Integer> queue,
                                 List<Pair<Integer, Integer>> expected) {
        List<Pair<Integer, Integer>> queueSorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            IndexedPriorityQueue<Integer, Integer>.KVPair kvPair = queue.extractMin();
            queueSorted.add(Pair.of(kvPair.key, kvPair.value));
        }
        queueSorted.forEach(i -> queue.put(i.getKey(), i.getValue()));  // Возвращаем в очередь
        expected.sort(Comparator.comparing(Pair::getValue));
        Assert.assertEquals(expected.size(), queueSorted.size());
        for (int i = 0; i < expected.size(); i++) {
            Assert.assertEquals(expected.get(i).getValue().compareTo(queueSorted.get(i).getValue()), 0);
        }
    }

    @Test
    public void testPriorities() {
        IndexedPriorityQueue<Integer, Integer> queue = new IndexedPriorityQueue<>();
        List<Pair<Integer, Integer>> toAdd = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            toAdd.add(Pair.of(i, i));
        }
        List<Pair<Integer, Integer>> toRemove = new ArrayList<>();
        testPrioritiesPermutations(queue, toAdd, 0, toRemove);
    }

    @Test
    public void testPrioritiesWithDuplicateValues() {
        IndexedPriorityQueue<Integer, Integer> queue = new IndexedPriorityQueue<>();
        List<Pair<Integer, Integer>> toAdd = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            toAdd.add(Pair.of(i, i));
            toAdd.add(Pair.of(10 + i, i));
        }
        List<Pair<Integer, Integer>> toRemove = new ArrayList<>();
        testPrioritiesPermutations(queue, toAdd, 0, toRemove);
    }

    @Test
    public void testChangeKey() {
        IndexedPriorityQueue<Integer, Integer> queue = new IndexedPriorityQueue<>();
        List<Pair<Integer, Integer>> all = new ArrayList<>();
        int size = 101;
        for (int i = 0; i < size; i++) {
            all.add(new MutablePair<>(i, i));
            all.add(new MutablePair<>(1000 + i, i));
        }
        all.forEach(p -> queue.put(p.getKey(), p.getValue()));
        for (Pair<Integer, Integer> pair : new ArrayList<>(all)) {
            for (int cnt = 0; cnt < size; cnt++) {
                // increaseKey
                queue.put(pair.getKey(), pair.getValue() + cnt);
                all.stream().filter(p -> p.getKey().equals(pair.getKey()))
                        .findFirst().get().setValue(pair.getValue() + cnt);
                checkPriorities(queue, all);

                // decreaseKey
                queue.put(pair.getKey(), pair.getValue() - cnt);
                all.stream().filter(p -> p.getKey().equals(pair.getKey()))
                        .findFirst().get().setValue(pair.getValue() - cnt);
                checkPriorities(queue, all);

                // Возвращаемся к начальному состоянию
                queue.put(pair.getKey(), pair.getValue());
                all.stream().filter(p -> p.getKey().equals(pair.getKey()))
                        .findFirst().get().setValue(pair.getValue());
                checkPriorities(queue, all);
            }
        }
    }
}
