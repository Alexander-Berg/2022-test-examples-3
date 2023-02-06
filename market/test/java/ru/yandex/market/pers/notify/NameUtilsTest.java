package ru.yandex.market.pers.notify;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author misterku
 */
class NameUtilsTest {
    @Test
    void normalName() {
        System.out.println(System.getProperty("java.io.tmpdir"));
        String name = NameUtils.normalizeName("Петя Петров-Водкин");
        assertEquals("Петя Петров-Водкин", name);
    }

    @Test
    void bigName() {
        String name = NameUtils.normalizeName("ПЕТЯ ПЕТРОВ-ВОДКИН");
        assertEquals("ПЕТЯ ПЕТРОВ-ВОДКИН", name);
    }

    @Test
    void lowercaseName() {
        String name = NameUtils.normalizeName("петя петров-водкин");
        assertEquals("Петя Петров-Водкин", name);
    }

    @Test
    void camelName() {
        String name = NameUtils.normalizeName("ПеТя ПетРов-ВодКин");
        assertEquals("ПеТя ПетРов-ВодКин", name);
    }
}
