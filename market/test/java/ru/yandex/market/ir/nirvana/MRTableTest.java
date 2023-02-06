package ru.yandex.market.ir.nirvana;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author inenakhov
 */
public class MRTableTest {
    @Test
    public void fromFile() throws Exception {
        MRTable mrTable = MRTable.fromFile("src/test/resources/mrtable.json");
        assertEquals(mrTable.getCluster(), "arnold");
        assertEquals(mrTable.getTable(), "//home/market/development/ir/inenakhov/dummy_watson_results");
    }
}
