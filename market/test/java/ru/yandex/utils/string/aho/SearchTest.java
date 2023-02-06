package ru.yandex.utils.string.aho;

import org.junit.Test;
import ru.yandex.utils.string.indexed.String4Search;
import ru.yandex.utils.string.indexed.String4SearchSimple;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SearchTest {
    @Test
    public void search2() throws Exception {
        List<String> dict = new ArrayList<>();
        List<String4Search> ind = new ArrayList<>();
        ind.add(new String4SearchSimple(" л ", 1, null));
        for (String4Search ss : ind) {
            dict.add(ss.getPattern());
        }
        Vertex<String4Search> vertex = Builder.buildIndex(dict, ind);
        assertEquals("[[10;  л ]]", Search.search(" общ объ л 355 ", vertex).toString());
    }

    @Test
    public void search() throws Exception {
        List<String> dict = new ArrayList<>();
        dict.add("a");
        dict.add("aa");
        dict.add("b c");
        dict.add("b");
        Vertex<String> vertex = Builder.build(dict);
        assertEquals("[[0; a], [1; aa], [1; a], [3; b], [5; b c]]", Search.search("aa b c", vertex).toString());
    }
}
