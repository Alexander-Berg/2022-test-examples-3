package ru.yandex.ir.util;

import static org.junit.Assert.assertTrue;
import static ru.yandex.utils.string.CharacterAcceptors.WhitespaceA;
import static ru.yandex.utils.string.CharacterAcceptors.createWhitespaceA;

import org.junit.Before;
import org.junit.Test;

public class StringTest {

    private static final String NBSP = "\u00a0";
    private final WhitespaceA acceptor = (WhitespaceA) createWhitespaceA();

    @Test
    public void NBSPWhitespaceAcceptorTest() {
        assertTrue(acceptor.accept(NBSP.toCharArray()[0]));
    }
}
