package ru.yandex.direct.utils.json;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JSonSplitterTest {
    @Test
    public void testSplitJsons() {
        String firstJson = "{\"a\":1}";
        String secondJson = "{\"a\":2}";
        String thirdJson = "{\"a\":3}";
        String combinedJson = firstJson + "  \t\n" + secondJson + " \n\t " + thirdJson;
        String[] splitJsons = JSonSplitter.splitJsons(combinedJson);
        assertEquals(splitJsons[0], firstJson);
        assertEquals(splitJsons[1], secondJson);
        assertEquals(splitJsons[2], thirdJson);
    }

    @Test
    public void testCombineJsons() {
        String firstJson = "{\"a\":1}";
        String secondJson = "{\"a\":2}";
        String combinedStr = JSonSplitter.combineJsons(List.of(firstJson, secondJson));
        assertEquals(combinedStr, firstJson + "\n" + secondJson);
        assertEquals(JSonSplitter.splitJsons(combinedStr)[0], firstJson);
        assertEquals(JSonSplitter.splitJsons(combinedStr)[1], secondJson);
    }
}
