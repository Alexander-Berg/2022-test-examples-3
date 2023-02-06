package ru.yandex.direct.pokazometer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PhraseRequestTest {
    @Test
    public void minusWordsAreEliminated() {
        PhraseRequest phrase = new PhraseRequest("носки -ботинки шерстяные", 1000000L);
        assertEquals("носки шерстяные", phrase.getPhrase());
    }
}
