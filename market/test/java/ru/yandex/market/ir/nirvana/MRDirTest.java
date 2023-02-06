package ru.yandex.market.ir.nirvana;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author inenakhov
 */
public class MRDirTest {
    @Test
    public void fromFile() throws Exception {
        MRDir mrDir = MRDir.fromFile("src/test/resources/mrdir.json");
        assertEquals(mrDir.getCluster(), "arnold");
        assertEquals(mrDir.getPath(), "//home/market/development/ir/inenakhov/temp");
    }
}
