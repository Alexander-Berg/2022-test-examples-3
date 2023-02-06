package ru.yandex.market.fulfillment.stockstorage.service.system;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SystemPropertySetOfIntKeyTest {

    @Test
    public void testParse() {
        Assertions.assertEquals(Set.of(1, 2, 3), SystemPropertySetOfIntKey.from("1,2,3"));
    }

    @Test
    public void testParseWithExtraSpaces() {
        Assertions.assertEquals(Set.of(1, 2, 3), SystemPropertySetOfIntKey.from("1,  2 ,3"));
    }

    @Test
    public void testParseNull() {
        Assertions.assertEquals(null, SystemPropertySetOfIntKey.from(null));
    }

    @Test
    public void testParseInvalid() {
        Assertions.assertEquals(Set.of(1), SystemPropertySetOfIntKey.from("/%43,1"));
    }
}
