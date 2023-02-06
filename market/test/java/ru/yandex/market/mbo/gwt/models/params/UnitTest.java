package ru.yandex.market.mbo.gwt.models.params;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.visual.Word;

import java.math.BigDecimal;

/**
 * @author sergtru
 * @since 02.02.2018
 */
public class UnitTest {

    @Test
    public void testGetOrGenerateReportNameCase1() {
        String expectedAliasName = "Right alias";
        Unit unit = new Unit("Should-not-returned-name", expectedAliasName, BigDecimal.ONE, 1, 1);
        unit.getAliases().add(new UnitAlias(1, "wrong", Word.DEFAULT_LANG_ID, true));
        Assert.assertEquals(expectedAliasName, unit.getOrGenerateReportName());
    }

    @Test
    public void testGetOrGenerateReportNameCase2() {
        String expectedAliasName = "Right alias";
        Unit unit = new Unit("Should-not-returned-name", null, BigDecimal.ONE, 1, 1);
        unit.getAliases().add(new UnitAlias(1, "wrong", Word.DEFAULT_LANG_ID ^ 1, true));
        unit.getAliases().add(new UnitAlias(2, expectedAliasName, Word.DEFAULT_LANG_ID, true));
        Assert.assertEquals(expectedAliasName, unit.getOrGenerateReportName());
    }

    @Test
    public void testGetOrGenerateReportNameCase3() {
        String expectedAliasName = "Right alias";
        Unit unit = new Unit(null, expectedAliasName, BigDecimal.ONE, 1, 1);
        unit.getAliases().add(new UnitAlias(1, "wrong", Word.DEFAULT_LANG_ID ^ 1, true));
        Assert.assertEquals(expectedAliasName, unit.getOrGenerateReportName());
    }
}
