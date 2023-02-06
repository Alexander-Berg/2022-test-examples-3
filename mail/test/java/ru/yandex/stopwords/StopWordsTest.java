package ru.yandex.stopwords;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class StopWordsTest extends TestBase {
    public StopWordsTest() {
        super(false, 0L);
    }

    @Test
    public void test() {
        Assert.assertTrue(StopWords.isStopWord("это"));
        Assert.assertTrue(StopWords.isStopWord("то"));
        Assert.assertFalse(StopWords.isStopWord("привет"));
    }
}

