package ru.yandex.travel.hotels.common.pansions;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.travel.hotels.proto.EPansionType;


public class PansionUnifierTests {
    @Test
    public void testNormalizeCase() {
        Assert.assertEquals("MIXED_CASE", PansionUnifier.normalize("MiXeD cAsE"));
    }

    @Test
    public void testNormalizePlus() {
        Assert.assertEquals("", PansionUnifier.normalize("+"));
        Assert.assertEquals("", PansionUnifier.normalize("++"));
        Assert.assertEquals("", PansionUnifier.normalize("+++"));

        Assert.assertEquals("A", PansionUnifier.normalize("a+"));
        Assert.assertEquals("A", PansionUnifier.normalize("+a"));

        Assert.assertEquals("A_B", PansionUnifier.normalize("a+b"));
        Assert.assertEquals("A_B", PansionUnifier.normalize("a + b"));
        Assert.assertEquals("A_B", PansionUnifier.normalize("a   + b"));
    }

    @Test
    public void testNormalizeSpaces() {
        Assert.assertEquals("", PansionUnifier.normalize(" "));
        Assert.assertEquals("", PansionUnifier.normalize("  "));
        Assert.assertEquals("", PansionUnifier.normalize("   "));

        Assert.assertEquals("A_B", PansionUnifier.normalize("a b"));
        Assert.assertEquals("A_B", PansionUnifier.normalize("a  b"));
        Assert.assertEquals("A_B", PansionUnifier.normalize("a   b"));

        Assert.assertEquals("A", PansionUnifier.normalize("a "));
        Assert.assertEquals("A", PansionUnifier.normalize("a  "));
        Assert.assertEquals("A", PansionUnifier.normalize("a   "));

        Assert.assertEquals("A", PansionUnifier.normalize(" a"));
        Assert.assertEquals("A", PansionUnifier.normalize("  a"));
        Assert.assertEquals("A", PansionUnifier.normalize("   a"));
    }

    @Test(expected = PansionUnifier.UnmatchedPansionException.class)
    public void testGetUnmatched() throws PansionUnifier.UnmatchedPansionException {
        PansionUnifier.get("no one knows this pansion type");
    }

    @Test
    public void testGetCase() throws PansionUnifier.UnmatchedPansionException {
        Assert.assertEquals(EPansionType.PT_BB, PansionUnifier.get("завтрак буфет с лечением"));
        Assert.assertEquals(EPansionType.PT_BB, PansionUnifier.get("завтраК буфет с Лечением"));
    }

    @Test
    public void testGetPlus() throws PansionUnifier.UnmatchedPansionException {
        Assert.assertEquals(EPansionType.PT_HB, PansionUnifier.get("hb+beach"));
        Assert.assertEquals(EPansionType.PT_HB, PansionUnifier.get("hb+ beach"));
        Assert.assertEquals(EPansionType.PT_HB, PansionUnifier.get("hb +beach"));
        Assert.assertEquals(EPansionType.PT_HB, PansionUnifier.get("hb + beach"));
    }

    @Test
    public void testGetSpaces() throws PansionUnifier.UnmatchedPansionException {
        Assert.assertEquals(EPansionType.PT_BB, PansionUnifier.get("bb"));
        Assert.assertEquals(EPansionType.PT_BB, PansionUnifier.get(" bb"));
        Assert.assertEquals(EPansionType.PT_BB, PansionUnifier.get("  bb"));

        Assert.assertEquals(EPansionType.PT_BB, PansionUnifier.get("bb "));
        Assert.assertEquals(EPansionType.PT_BB, PansionUnifier.get("bb  "));

        Assert.assertEquals(EPansionType.PT_BB, PansionUnifier.get(" bb "));
        Assert.assertEquals(EPansionType.PT_BB, PansionUnifier.get("  bb  "));
    }

    @Test
    public void testGetFlags() {
        Assert.assertEquals(EPansionType.PT_AI, PansionUnifier.get(true, false, false, false, false, false));
        Assert.assertEquals(EPansionType.PT_AI, PansionUnifier.get(true, true, true, true, true, true));
        Assert.assertEquals(EPansionType.PT_FB, PansionUnifier.get(false, true, false, false, false, false));
        Assert.assertEquals(EPansionType.PT_FB, PansionUnifier.get(false, true, true, true, true, true));
        Assert.assertEquals(EPansionType.PT_HB, PansionUnifier.get(false, false, true, false, false, false));
        Assert.assertEquals(EPansionType.PT_HB, PansionUnifier.get(false, false, true, true, true, true));
        Assert.assertEquals(EPansionType.PT_BB, PansionUnifier.get(false, false, false, true, false, false));
        Assert.assertEquals(EPansionType.PT_FB, PansionUnifier.get(false, false, false, true, true, true));
        Assert.assertEquals(EPansionType.PT_BB, PansionUnifier.get(false, false, false, true, true, false));
        Assert.assertEquals(EPansionType.PT_HB, PansionUnifier.get(false, false, false, true, false, true));
        Assert.assertEquals(EPansionType.PT_BD, PansionUnifier.get(false, false, false, false, false, true));
        Assert.assertEquals(EPansionType.PT_RO, PansionUnifier.get(false, false, false, false, false, false));
    }
}
