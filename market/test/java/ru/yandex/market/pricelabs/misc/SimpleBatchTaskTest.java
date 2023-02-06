package ru.yandex.market.pricelabs.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleBatchTaskTest {

    private List<String> list;

    @BeforeEach
    void init() {
        this.list = new ArrayList<>();
    }

    @Test
    void testEmpty() {
        SimpleBatchTask<String> batch = new SimpleBatchTask<>(1, list -> {
            throw new RuntimeException("Not expected");
        });
        batch.flush();
    }

    @Test
    void test1() {
        SimpleBatchTask<String> batch = new SimpleBatchTask<>(1, list::addAll);
        batch.add("1");
        assertEquals(List.of("1"), list);

        batch.add("2");
        assertEquals(List.of("1", "2"), list);

        batch.addAll(List.of("3", "4"));
        assertEquals(List.of("1", "2", "3", "4"), list);

        batch.flush();
        assertEquals(List.of("1", "2", "3", "4"), list);

    }

    @Test
    void test3() {
        SimpleBatchTask<String> batch = new SimpleBatchTask<>(3, list::addAll);
        batch.add("1");
        assertEquals(List.of(), list);

        batch.add("2");
        assertEquals(List.of(), list);

        batch.add("3");
        assertEquals(List.of("1", "2", "3"), list);

        batch.add("4");
        assertEquals(List.of("1", "2", "3"), list);

        batch.addAll(List.of("5", "6", "7"));
        assertEquals(List.of("1", "2", "3", "4", "5", "6"), list);

        batch.addAll(List.of("8"));
        assertEquals(List.of("1", "2", "3", "4", "5", "6"), list);

        batch.flush();
        assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8"), list);
    }


    @Test
    void testStacked() {

        var temp = new Object() {
            int cnt;
            Consumer<List<Integer>> impl;
        };
        temp.impl = items -> assertTrue(items.size() <= 3, "actual size is " + items.size());

        SimpleBatchTask<Integer> batch = new SimpleBatchTask<>(3, items -> temp.impl.accept(items));

        batch.add(temp.cnt++);
        batch.add(temp.cnt++);
        batch.add(temp.cnt++);
        batch.add(temp.cnt++);

        temp.impl = items -> {
            assertTrue(items.size() <= 3, "actual size is " + items.size());
            if (temp.cnt++ < 10) {
                batch.add(temp.cnt++);
                batch.add(temp.cnt++);
                batch.add(temp.cnt++);
            }
        };
        batch.add(temp.cnt++);
        batch.add(temp.cnt++);

    }

}
