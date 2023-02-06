package ru.yandex.market.ir.classifier.logic;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Evgeny Anisimoff <a href="mailto:anisimoff@yandex-team.ru"/>
 * @since {18:52}
 */
public class DefaultClassifierControllerTest {
    @Test
    public void timeUnitTest() {
        assertEquals(1000 * 60 * 60 * 24, TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS));
    }
}
