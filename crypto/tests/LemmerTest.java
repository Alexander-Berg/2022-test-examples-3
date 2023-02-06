package ru.yandex.crypta.lib.lemmer;

import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LemmerTest {
    @Test
    public void testIsValid() {
        assertEquals(
            Set.of("мама", "мыло", "мыть", "рама", "mazda", "cx", "5", "находить", "приходить"),
            Lemmer.getLemmas("мама мыла раму мылом mazda cx 5 найти прийдём", "ru")
        );
    }
}
