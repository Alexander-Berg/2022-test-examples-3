package ru.yandex.utils.string.indexed;

import org.junit.Test;
import ru.yandex.utils.string.DeNullingProcessor;

import static org.junit.Assert.*;

/**
 * @author inenakhov
 */
public class AbstractString4SearchTest {
    @Test
    public void isWordsMatched() throws Exception {
        IndexedStringFactory indexedStringFactory = new IndexedStringFactory();
        indexedStringFactory.setStringProcessor(new DeNullingProcessor());
        indexedStringFactory.setSteam(true);

        String4Search string4Search = indexedStringFactory.createString4Search("hello+", false);
        AbstractString4Search abstractString4Search = (AbstractString4Search) string4Search;
        IndexedString indexedString = indexedStringFactory.createIndexedString("hello+ world hello+");
        assertTrue(abstractString4Search.isWordsMatched(indexedString, 0, 0, "hello+"));
        assertTrue(abstractString4Search.isWordsMatched(indexedString, 5, 5, "hello+"));
        assertFalse(abstractString4Search.isWordsMatched(indexedString, 2, 2, "hello+"));
        assertFalse(abstractString4Search.isWordsMatched(indexedString, 4, 4, "hello+"));
    }

    @Test
    public void isWordsMatched2() throws Exception {
        IndexedStringFactory indexedStringFactory = new IndexedStringFactory();
        indexedStringFactory.setStringProcessor(new DeNullingProcessor());
        indexedStringFactory.setSteam(true);

        String4Search string4Search = indexedStringFactory.createString4Search("hello+world", false);
        AbstractString4Search abstractString4Search = (AbstractString4Search) string4Search;
        IndexedString indexedString = indexedStringFactory.createIndexedString("hello+ world hello+world");
        assertTrue(abstractString4Search.isWordsMatched(indexedString, 5, 7, "hello+world"));
        assertTrue(abstractString4Search.isWordsMatched(indexedString, 5, 5, "hello+world"));
        assertTrue(abstractString4Search.isWordsMatched(indexedString, 7, 7, "hello+world"));
        assertTrue(abstractString4Search.isWordsMatched(indexedString, 6, 6, "hello+world"));
    }
}