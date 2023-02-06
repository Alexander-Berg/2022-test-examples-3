package ru.yandex.ir.common.features.extractors.parameters.valuesExtractors.patternsMatching;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.yandex.ir.common.features.extractors.parameters.valuesExtractors.ValuePosition;
import ru.yandex.ir.common.features.extractors.parameters.valuesExtractors.patternMatching.PatternsTrie;
import ru.yandex.ir.common.features.extractors.parameters.valuesExtractors.patternMatching.StringWithPosition;

import java.util.Arrays;

public class PatternsTrieTest {
    @Test
    public void testAddAndExtract() {
        PatternsTrie trie = new PatternsTrie();
        trie.add("c");
        trie.add("abrac");
        trie.add("buba");
        trie.add("buka");

        Assertions.assertEquals(Arrays.asList(
                        new StringWithPosition("abrac", new ValuePosition(2, 7)),
                        new StringWithPosition("c", new ValuePosition(6, 7))),
                trie.find("ebabracadabra"));
        Assertions.assertEquals(Arrays.asList(
                        new StringWithPosition("buba", new ValuePosition(0, 4)),
                        new StringWithPosition("c", new ValuePosition(4, 5)),
                        new StringWithPosition("abrac", new ValuePosition(5, 10)),
                        new StringWithPosition("c", new ValuePosition(9, 10))),
                trie.find("bubacabrac"));
        Assertions.assertEquals(Arrays.asList(
                        new StringWithPosition("c", new ValuePosition(0, 1)),
                        new StringWithPosition("buka", new ValuePosition(5, 9))),
                trie.find("chupabuka"));
    }
}
