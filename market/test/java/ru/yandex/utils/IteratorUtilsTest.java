package ru.yandex.utils;

import junit.framework.TestCase;

import java.util.List;

import static ru.yandex.utils.IteratorUtils.*;

public class IteratorUtilsTest extends TestCase {


    public void testFilter() throws Exception {
        List<Integer> list = toList(filter(olist(1, 2, 3), new Accept<Integer>() {
            @Override
            public boolean accept(Integer o) {
                return true;
            }
        }));
        System.out.println(list);
    }
}
