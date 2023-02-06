package ru.yandex.market.core.indexer.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class OfferPositionTest {

    @Test
    public void testOf() {
        assertEquals(OfferPosition.of(1, 0), OfferPosition.of("1"));
        assertEquals(OfferPosition.of(0, 0), OfferPosition.of("a"));
        assertEquals(OfferPosition.of(0, 0), OfferPosition.of(""));
        assertEquals(OfferPosition.of(0, 0), OfferPosition.of("a:b"));
        assertEquals(OfferPosition.of(0, 1), OfferPosition.of("b:1"));
        assertEquals(OfferPosition.of(10, 20), OfferPosition.of("10:20"));
    }

}