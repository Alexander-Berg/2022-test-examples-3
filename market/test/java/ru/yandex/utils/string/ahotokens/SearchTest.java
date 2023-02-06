package ru.yandex.utils.string.ahotokens;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * todo описать предназначение.
 *
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
public class SearchTest {
    @Test
    public void search() throws Exception {
        List<List<String>> dict = new ArrayList<>();
        dict.add(Arrays.asList("a"));
        dict.add(Arrays.asList("aa"));
        dict.add(Arrays.asList("aa", "b"));
        dict.add(Arrays.asList("b", "c"));
        dict.add(Arrays.asList("b"));
        final Vertex<String, List<String>> vertex = Builder.build(dict);
        assertEquals(
            "[[0; [aa]], [1; [aa, b]], [1; [b]], [2; [b, c]]]",
            Search.search(Arrays.asList("aa", "b", "c"), vertex).toString()
        );
        assertEquals(
            "[[aa], [aa, b], [b], [b, c]]",
            Search.searchValues(Arrays.asList("aa", "b", "c"), vertex).toString()
        );
    }
}
